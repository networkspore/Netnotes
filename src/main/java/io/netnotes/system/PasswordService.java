package io.netnotes.system;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.netnotes.engine.io.daemon.ClientSession;
import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.components.input.PasswordPrompt;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.virtualExecutors.VirtualExecutors;

/**
 * PasswordService - Centralized password modal management
 *
 * Owns the FULL lifecycle of the PasswordKeyboardManager:
 *   - Registration onto the handle  (onHandleAttached)
 *   - enable / exclusive-mode / disable during password sessions
 *   - Removal from the handle       (onHandleDetached)
 *
 * SystemApplication should NOT hold a PasswordKeyboardManager field, call
 * addDeviceManager / removeDeviceManager for the password keyboard, or call
 * enable/disable/setExclusiveMode on it directly.
 *
 * MODES:
 * - CREATE: Double-entry with confirmation (password setup)
 * - VERIFY: Single-entry (authentication)
 *
 * HARDWARE KEYBOARD FLOW:
 * - Ensures IODaemon available
 * - Shows init progress during keyboard claim
 * - Initializes PasswordKeyboardManager
 *
 * SOFTWARE KEYBOARD FLOW:
 * - Skips initialization progress
 * - Shows prompt immediately
 */
public class PasswordService {
    private final static LogLevel LOG_LEVEL = LogLevel.IMPORTANT;

    private final SystemApplication application;
    private TerminalContainerHandle containerHandle;

    // Owned exclusively by PasswordService
    private PasswordKeyboardManager passwordKeyboardManager = null;

    private PasswordRequest activeRequest;
    private PasswordPrompt activePrompt;
    private RetryContext retryContext;

    record PasswordRequest(
        PasswordPrompt.Mode mode,
        String title,
        String promptText,
        String confirmPromptText,
        int timeoutSeconds,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> handler,
        CompletableFuture<PasswordResult> result
    ) {}

    public record PasswordResult(boolean success, String message) {}

    private record RetryContext(
        int maxAttempts,
        int attemptsUsed,
        CompletableFuture<PasswordResult> finalResult
    ) {
        boolean hasAttemptsLeft() {
            return maxAttempts < 1 || attemptsUsed < maxAttempts;
        }
    }

    public PasswordService(SystemApplication application) {
        this.application = application;
    }

    // ===== LIFECYCLE =====

    /**
     * Called by SystemApplication when a TerminalContainerHandle is attached.
     *
     * If a claimedKeyboardId is configured, this registers the
     * PasswordKeyboardManager onto the handle but does NOT enable it yet.
     * Enabling happens only when a password session is started.
     */
    public CompletableFuture<Void> onHandleAttached(TerminalContainerHandle handle) {
        if (activeRequest != null) {
            throw new IllegalStateException("Cannot attach handle during active password request");
        }
        this.containerHandle = handle;

        NoteBytes keyboardId = application.getClaimedKeyboardId();
        if (keyboardId == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Register the manager on the handle. The manager is NOT enabled here;
        // it will be enabled on-demand when a password session begins.
        PasswordKeyboardManager mgr = new PasswordKeyboardManager(
            keyboardId,
            ClientSession.Modes.PARSED
        );
        passwordKeyboardManager = mgr;

        return handle.addDeviceManager(SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID, mgr)
            .handle((deviceManager,ex) -> {
                if(ex != null){
                    Log.logError("[PasswordService] Failed to register new keyboard manager", ex);
                    passwordKeyboardManager = null;
                }else{
                    Log.logMsg("[PasswordService] device manager added: " 
                        + SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID, LOG_LEVEL);
                }
                return null;
            });
    }

    /**
     * Called by SystemApplication when the handle is being detached.
     *
     * Cancels any active request, removes the keyboard manager from the handle
     * (which internally disables it if active), and clears local state.
     *
     * SystemApplication.detachHandle() should call this BEFORE calling
     * handle.removeDeviceManager or handle.close() directly — this method
     * handles both.
     */
    public CompletableFuture<Void> onHandleDetached() {
        if (activeRequest != null) {
            retryContext.finalResult().complete(
                new PasswordResult(false, "System detached")
            );
            // Clear request state but leave keyboard removal to the block below
            activeRequest = null;
            retryContext = null;
            if (activePrompt != null) {
                activePrompt.deactivate();
                activePrompt = null;
            }
        }

        TerminalContainerHandle handle = this.containerHandle;
        this.containerHandle = null;

        if (handle == null || passwordKeyboardManager == null) {
            passwordKeyboardManager = null;
            return CompletableFuture.completedFuture(null);
        }

        // removeDeviceManager calls DeviceManager.detach() internally, which
        // calls disable() → cleanupEventRouting() → removeEventFilters() + releaseDevice().
        // We do not need to call disable() or setExclusiveMode(false) ourselves.
        return handle.removeDeviceManager(SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID)
            .whenComplete((v, ex) -> {
                if (ex != null) {
                    Log.logError("[PasswordService] Keyboard removal on detach failed", ex);
                }
                passwordKeyboardManager = null;
            });
    }

    /**
     * Called by SystemApplication when the claimedKeyboardId changes
     * (e.g. after completeBootstrap selects a different keyboard).
     *
     * Replaces the registered manager on the current handle if one is attached.
     * SystemApplication should no longer call updatePasswordKeyboard / removePasswordKeyboard.
     */
    public CompletableFuture<Void> onKeyboardIdChanged(NoteBytes newKeyboardId) {
        TerminalContainerHandle handle = this.containerHandle;

        // Remove the old manager (detach handles disable internally)
        CompletableFuture<Void> removal;
        if (handle != null && passwordKeyboardManager != null) {
            removal = handle.removeDeviceManager(SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID)
                .whenComplete((v, ex) -> passwordKeyboardManager = null);
        } else {
            if (passwordKeyboardManager != null) {
                passwordKeyboardManager = null;
            }
            removal = CompletableFuture.completedFuture(null);
        }

        if (newKeyboardId == null || handle == null) {
            return removal;
        }

        return removal.thenCompose(v -> {
            PasswordKeyboardManager mgr = new PasswordKeyboardManager(
                newKeyboardId,
                ClientSession.Modes.PARSED
            );
            passwordKeyboardManager = mgr;
            return handle.addDeviceManager(SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID, mgr);
        }).handle((deviceManager,ex) -> {
            if(ex != null){
                Log.logError("[PasswordService] Failed to register new keyboard manager", ex);
                passwordKeyboardManager = null;
            }else{
                Log.logMsg("[PasswordService] device manager added: " 
                    + SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID, LOG_LEVEL);
            }
            return null;
        });
    }

    // ===== PUBLIC API =====

    /**
     * Request password verification with retry control.
     *
     * @param maxAttempts -1 for unlimited (only completes on success/cancel/timeout)
     *                    N for N attempts (completes on success OR max exceeded)
     * @return Future completes with:
     *         - success=true when verified
     *         - success=false when maxAttempts exceeded (if limited)
     *         - success=false on cancel/timeout (immediate, doesn't retry)
     */
    public CompletableFuture<PasswordResult> requestVerification(
        String title,
        String promptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> verifier,
        int maxAttempts
    ) {
        if (containerHandle == null) {
            return CompletableFuture.completedFuture(
                new PasswordResult(false, "No container handle attached")
            );
        }

        if (activeRequest != null) {
            return CompletableFuture.completedFuture(
                new PasswordResult(false, "Password request already in progress")
            );
        }

        CompletableFuture<PasswordResult> finalResult = new CompletableFuture<>();
        retryContext = new RetryContext(maxAttempts, 0, finalResult);

        attemptVerification(title, promptText, verifier);
        return finalResult;
    }

    private void attemptVerification(
        String title,
        String promptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> verifier
    ) {
        CompletableFuture<PasswordResult> singleAttemptResult = new CompletableFuture<>();

        activeRequest = new PasswordRequest(
            PasswordPrompt.Mode.VERIFY,
            title,
            promptText,
            null,
            30,
            verifier,
            singleAttemptResult
        );

        if (activePrompt == null) {
            showPasswordModal();
        } else {
            activePrompt.reset();
            activePrompt.activate();
        }

        singleAttemptResult.thenAccept(singleResult -> {
            handleVerificationResult(singleResult, title, promptText, verifier);
        });
    }

    private void handleVerificationResult(
        PasswordResult singleResult,
        String title,
        String promptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> verifier
    ) {
        if (singleResult.success()) {
            retryContext.finalResult().complete(new PasswordResult(true, null));
            cleanup();

        } else if (singleResult.message().contains("Cancelled") ||
                singleResult.message().contains("timeout")) {
            retryContext.finalResult().complete(singleResult);
            cleanup();

        } else {
            retryContext = new RetryContext(
                retryContext.maxAttempts(),
                retryContext.attemptsUsed() + 1,
                retryContext.finalResult()
            );

            if (retryContext.hasAttemptsLeft()) {
                activeRequest = null;
                attemptVerification(title, promptText, verifier);
            } else {
                retryContext.finalResult().complete(
                    new PasswordResult(false, "Maximum authentication attempts exceeded")
                );
                cleanup();
            }
        }
    }

    /**
     * Request password creation with retry on mismatch.
     *
     * @param maxAttempts -1 for unlimited, N for N mismatch attempts before giving up
     */
    public CompletableFuture<PasswordResult> requestCreation(
        String title,
        String promptText,
        String confirmPromptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> onCreate,
        int maxAttempts
    ) {
        if (containerHandle == null) {
            return CompletableFuture.completedFuture(
                new PasswordResult(false, "No container handle attached")
            );
        }

        if (activeRequest != null) {
            return CompletableFuture.completedFuture(
                new PasswordResult(false, "Password request already in progress")
            );
        }

        CompletableFuture<PasswordResult> finalResult = new CompletableFuture<>();
        retryContext = new RetryContext(maxAttempts, 0, finalResult);

        attemptCreation(title, promptText, confirmPromptText, onCreate);
        return finalResult;
    }

    private void attemptCreation(
        String title,
        String promptText,
        String confirmPromptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> onCreate
    ) {
        CompletableFuture<PasswordResult> singleAttemptResult = new CompletableFuture<>();

        activeRequest = new PasswordRequest(
            PasswordPrompt.Mode.CREATE,
            title,
            promptText,
            confirmPromptText,
            30,
            onCreate,
            singleAttemptResult
        );

        if (activePrompt == null) {
            showPasswordModal();
        } else {
            activePrompt.reset();
            activePrompt.activate();
        }

        singleAttemptResult.thenAccept(singleResult -> {
            handleCreationResult(singleResult, title, promptText, confirmPromptText, onCreate);
        });
    }

    private void handleCreationResult(
        PasswordResult singleResult,
        String title,
        String promptText,
        String confirmPromptText,
        Function<NoteBytesEphemeral, CompletableFuture<Boolean>> onCreate
    ) {
        if (singleResult.success()) {
            retryContext.finalResult().complete(new PasswordResult(true, null));
            cleanup();

        } else if (singleResult.message().contains("Cancelled") ||
                singleResult.message().contains("timeout")) {
            retryContext.finalResult().complete(singleResult);
            cleanup();

        } else if (singleResult.message().contains("do not match")) {
            RetryContext nextContext = new RetryContext(
                retryContext.maxAttempts(),
                retryContext.attemptsUsed() + 1,
                retryContext.finalResult()
            );

            if (nextContext.hasAttemptsLeft()) {
                retryContext = nextContext;
                activeRequest = null;
                attemptCreation(title, promptText, confirmPromptText, onCreate);
            } else {
                retryContext.finalResult().complete(
                    new PasswordResult(false, "Maximum creation attempts exceeded")
                );
                cleanup();
            }

        } else {
            retryContext.finalResult().complete(singleResult);
            cleanup();
        }
    }

    // ===== MODAL LIFECYCLE =====

    private void showPasswordModal() {
        NoteBytes passwordKeyboardId = application.getClaimedKeyboardId();

        if (passwordKeyboardId != null) {
            application.getRootScene().showPasswordInitializing();

            ensureIODaemonAndKeyboard()
                .handle((v, ex) -> {
                    if (ex != null) {
                        Log.logError("[PasswordService] Keyboard init failed", ex);
                    }
                    showPasswordPrompt(true, ex);
                    return null;
                });
        } else {
            showPasswordPrompt(false, null);
        }
    }

    /**
     * Ensures the IODaemon is available and the PasswordKeyboardManager is
     * enabled in exclusive mode.
     *
     * NOTE on ordering: exclusive mode is set BEFORE enable() so that
     * DeviceManager.setupEventRouting() picks it up via requiresExclusiveAccess()
     * in the same operation. This avoids the double-filter-setup that would occur
     * if setExclusiveMode(true) were called after enable().
     */
    private CompletableFuture<Void> ensureIODaemonAndKeyboard() {
        updateInitProgress(0.1, "Initializing Hardware Keyboard...");

        return application.getIoDaemonManager().ensureAvailable()
            .thenRun(() -> updateInitProgress(0.7, "Securing event path.."))
            .thenCompose(v -> {
                if (passwordKeyboardManager == null) {
                    Log.logMsg("[PasswordService] No password keyboard registered", LOG_LEVEL);
                    return CompletableFuture.completedFuture(null);
                }

                return passwordKeyboardManager.isEnabled()
                    .thenCompose(isEnabled -> {
                        if (isEnabled) {
                            // Already enabled from a previous session in this attach cycle —
                            // just ensure exclusive mode is on. setExclusiveMode is idempotent
                            // and only updates filters if the mode actually changes.
                            return passwordKeyboardManager.setExclusiveMode(true);
                        }

                        // Set exclusive mode first so enable() configures filters correctly
                        // via requiresExclusiveAccess() during setupEventRouting().
                        return passwordKeyboardManager.setExclusiveMode(true)
                            .thenCompose(v2 -> passwordKeyboardManager.enable())
                            .thenAccept((claimedDevice)->{
                                Log.logMsg("[PasswordService] password keyboard->enabled: " 
                                    + claimedDevice.getDeviceId(), LOG_LEVEL);
                            });
                    });
            })
            .thenRun(() -> updateInitProgress(1.0, "Hardware Keyboard Enabled"))
            .exceptionally(ex -> {
                Log.logError("[PasswordService] Could not enable keyboard manager", ex);
                throw new RuntimeException(ex);
            });
    }

    private void updateInitProgress(double percent, String msg) {
        if(!VirtualExecutors.getUiExecutor().isCurrentThread()){
            VirtualExecutors.getUiExecutor().runLater(()->updateInitProgress(percent, msg));
            return;
        }
      
        application.getRootScene().updatePasswordInitProgress(percent, msg);
    }

    private void showPasswordPrompt(boolean isHardwareKeyboard, Throwable ex) {
        PasswordPrompt.Builder builder = activeRequest.mode() == PasswordPrompt.Mode.CREATE
            ? PasswordPrompt.createBuilder("sys-pass-service-pass-modal")
            : PasswordPrompt.verifyBuilder("sys-pass-service-pass-modal");

        builder
            .prompt(activeRequest.promptText())
            .timeoutSeconds(activeRequest.timeoutSeconds())
            .onTimeout(this::handleTimeout)
            .onCancel(this::handleCancel);

        if (isHardwareKeyboard) {
            if (ex == null) {
                builder.title(activeRequest.title() + " - 🔏");
            } else {
                builder.title(activeRequest.title() + " - ⚠️");
                builder.footerText("⚠️ - " + ex.getMessage());
            }
        } else {
            builder.title(activeRequest.title());
        }

        if (activeRequest.mode() == PasswordPrompt.Mode.CREATE) {
            builder
                .confirmPrompt(activeRequest.confirmPromptText())
                .onPassword(this::handlePasswordCreated)
                .onMismatch(this::handleMismatch);
        } else {
            builder.onVerify(this::handlePasswordEntered);
        }

        activePrompt = builder.build();
        application.getRootScene().showPasswordPrompt(activePrompt);
        activePrompt.activate();
    }

    private void hidePasswordPrompt() {
        if (activePrompt != null) {
            activePrompt.deactivate();
            application.getRootScene().closePasswordPrompt();
            activePrompt = null;
        }
    }

    // ===== HANDLERS =====

    private void handlePasswordCreated(NoteBytesEphemeral password) {
        activeRequest.handler().apply(password)
            .thenAccept(valid -> {
                password.close();
                if (valid) {
                    completeCurrentAttempt(true, null);
                } else {
                    completeCurrentAttempt(false, "Password creation failed");
                }
            })
            .exceptionally(ex -> {
                password.close();
                completeCurrentAttempt(false, "Creation failed: " + ex.getMessage());
                return null;
            });
    }

    private void handlePasswordEntered(NoteBytesEphemeral password) {
        activeRequest.handler().apply(password)
            .thenAccept(valid -> {
                password.close();
                if (valid) {
                    completeCurrentAttempt(true, null);
                } else {
                    completeCurrentAttempt(false, "Invalid password");
                }
            })
            .exceptionally(ex -> {
                password.close();
                completeCurrentAttempt(false, "Verification failed: " + ex.getMessage());
                return null;
            });
    }

    private void handleTimeout() {
        completeCurrentAttempt(false, "Authentication timeout");
    }

    private void handleCancel() {
        completeCurrentAttempt(false, "Cancelled");
    }

    private void handleMismatch() {
        completeCurrentAttempt(false, "Passwords do not match");
    }

    // ===== COMPLETION =====

    private void completeCurrentAttempt(boolean success, String message) {
        if (activeRequest != null) {
            activeRequest.result().complete(new PasswordResult(success, message));
        }
    }

    private void cleanup() {
        activeRequest = null;
        retryContext = null;
        hidePasswordPrompt();
        releaseKeyboard();
    }

    /**
     * Releases exclusive mode and disables the keyboard manager after a
     * password session ends.
     *
     * We call disable() directly (not removeDeviceManager) because the manager
     * stays registered on the handle for the next session — it just isn't
     * actively claiming the device between sessions.
     *
     * disable() internally calls cleanupEventRouting() which removes the
     * exclusive filter and releases the device via IODaemon. There is no need
     * to call setExclusiveMode(false) first — disable() handles filter removal.
     */
    private void releaseKeyboard() {
        if (passwordKeyboardManager == null) return;

        passwordKeyboardManager.disable()
            .exceptionally(ex -> {
                Log.logError("[PasswordService] Keyboard release failed: " + ex.getMessage());
                return null;
            });
    }

    // ===== STATE =====

    public boolean isActive() {
        return activeRequest != null;
    }
}