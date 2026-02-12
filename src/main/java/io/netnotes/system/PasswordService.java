package io.netnotes.system;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.components.input.PasswordPrompt;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

/**
 * PasswordService - Centralized password modal management
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
    
    private final SystemApplication application;
    private TerminalContainerHandle containerHandle;
    
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
    
    public void onHandleAttached(TerminalContainerHandle handle) {
        if (activeRequest != null) {
            throw new IllegalStateException("Cannot attach handle during active password request");
        }
        this.containerHandle = handle;
    }
    
    public void onHandleDetached() {
        if (activeRequest != null) {
            retryContext.finalResult().complete(
                new PasswordResult(false, "System detached")
            );
            cleanup();
        }
        this.containerHandle = null;
    }
    
    // ===== PUBLIC API =====
    

    /**
     * Request password verification with retry control
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
        // Create new result future for this attempt
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
            // First attempt - full modal initialization
            showPasswordModal();
        } else {
            // Retry - reset existing prompt
            activePrompt.reset();
            activePrompt.activate();
        }
        
        // Single listener per attempt
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
            // SUCCESS
            retryContext.finalResult().complete(new PasswordResult(true, null));
            cleanup();
            
        } else if (singleResult.message().contains("Cancelled") || 
                singleResult.message().contains("timeout")) {
            // CANCEL/TIMEOUT - complete immediately
            retryContext.finalResult().complete(singleResult);
            cleanup();
            
        } else {
            // FAILED VERIFICATION - check retry
            retryContext = new RetryContext(
                retryContext.maxAttempts(),
                retryContext.attemptsUsed() + 1,
                retryContext.finalResult()
            );
            
            if (retryContext.hasAttemptsLeft()) {
                // Retry - prompt stays visible
                activeRequest = null;  // Clear for retry
                attemptVerification(title, promptText, verifier);
            } else {
                // MAX ATTEMPTS EXCEEDED
                retryContext.finalResult().complete(
                    new PasswordResult(false, "Maximum authentication attempts exceeded")
                );
                cleanup();
            }
        }
    }
    
     /**
     * Request password creation with retry on mismatch
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
            // SUCCESS
            retryContext.finalResult().complete(new PasswordResult(true, null));
            cleanup();
            
        } else if (singleResult.message().contains("Cancelled") || 
                   singleResult.message().contains("timeout")) {
            // CANCEL/TIMEOUT
            retryContext.finalResult().complete(singleResult);
            cleanup();
            
        } else if (singleResult.message().contains("do not match")) {
            // MISMATCH - retry if attempts left
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
            // OTHER FAILURE (creation handler failed) - don't retry
            retryContext.finalResult().complete(singleResult);
            cleanup();
        }
    }

    // ===== MODAL LIFECYCLE =====
    
    private void showPasswordModal() {
     
        String passwordKeyboardId = application.getClaimedKeyboardId();
        
        if (passwordKeyboardId != null) {
            // Hardware keyboard mode - show progress and initialize
            application.getRootScene().showPasswordInitializing();
            
            ensureIODaemonAvailable()
                .thenCompose(v -> initializeKeyboard())
                .handle((v,ex)->{
                
                    if(ex != null){
                        Log.logError("[PasswordService] Keyboard init failed ", ex);
                    }
                    showPasswordPrompt(true, ex);
                    return null;
                });
                
        } else {
            // GUI keyboard mode - skip progress, show prompt immediately
            showPasswordPrompt(false, null);
        }
  
    }
    
    private CompletableFuture<Void> ensureIODaemonAvailable() {
        updateInitProgress(.1, "Initializing Hardware Keyboard...");
        return application.getIoDaemonManager().ensureAvailable()
            .thenRun(()->updateInitProgress(.7, "Securing event path.."))
            .exceptionally(ex -> {
                Log.logError("[PasswordService] IODaemon not available: " + ex.getMessage());
                throw new RuntimeException("IODaemon required for hardware keyboard", ex);
            });
    }
    
    private CompletableFuture<Void> initializeKeyboard() {
        
        return containerHandle.getDeviceManager(SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID)
            .thenCompose(manager -> {
                if (manager == null) {
                    Log.logMsg("[PasswordService] No password keyboard configured");
                    return CompletableFuture.completedFuture(null);
                }
               
                PasswordKeyboardManager kbdMgr = (PasswordKeyboardManager) manager;
            
               
                return kbdMgr.setExclusiveMode(true)
                    .thenCompose(v -> kbdMgr.enable())
                    .thenRun(()->{
                         updateInitProgress(1, "Hardware Keyboard Enabled");
                    });
            });
    }
    
    private void updateInitProgress(double percent, String msg) {
        VirtualExecutors.getUiExecutor().executeFireAndForget(()->
            application.getRootScene().updatePasswordInitProgress(percent, msg));
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

        if(isHardwareKeyboard){
            if(ex == null){
                builder.title(activeRequest.title() + " - 🔏");
            }else{
                builder.title(activeRequest.title() + " - ⚠️");
                builder.footerText("⚠️ - " + ex.getMessage());
            }
        }else{
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
        // CREATE mode - password confirmed, now verify with caller's onCreate handler
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
        // VERIFY mode - single password entry, verify with caller
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
        
        String passwordKeyboardId = application.getClaimedKeyboardId();
        if (passwordKeyboardId != null) {
            releaseKeyboard(passwordKeyboardId);
        }
    }
    
    private void releaseKeyboard(String keyboardId) {
        containerHandle.getDeviceManager(keyboardId)
            .thenAccept(manager -> {
                if (manager == null) return;
                
                PasswordKeyboardManager kbdMgr = (PasswordKeyboardManager) manager;
                kbdMgr.setExclusiveMode(false)
                    .thenCompose(v -> kbdMgr.disable())
                    .exceptionally(ex -> {
                        Log.logError("[PasswordService] Keyboard release failed: " + ex.getMessage());
                        return null;
                    });
            });
    }
    
    // ===== STATE =====
    
    public boolean isActive() {
        return activeRequest != null;
    }
}