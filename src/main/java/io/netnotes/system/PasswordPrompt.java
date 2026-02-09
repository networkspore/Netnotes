package io.netnotes.system;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netnotes.terminal.Position;
import io.netnotes.terminal.TerminalBatchBuilder;
import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle;
import io.netnotes.terminal.TextStyle.BoxStyle;
import io.netnotes.engine.io.input.Keyboard.KeyCodeBytes;
import io.netnotes.engine.io.input.events.keyboardEvents.KeyDownEvent;
import io.netnotes.engine.ui.PasswordReader;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

/**
 * PasswordPrompt - Async state-driven password capture
 * 
 * CRITICAL: No async chaining from transitionTo()
 * - transitionTo() is synchronous state update
 * - Async operations listen to state transitions via stateMachine callbacks
 * - Prevents async interference with layout/render deferral
 * 
 * ASYNC PATTERN:
 * activate() → transitionTo(INACTIVE, ACTIVE) 
 *           → STATE_ACTIVE handler fires (async)
 *           → claimKeyboard() + startCapture() + startTimeout()
 * 
 * deactivate() → transitionTo(ACTIVE, INACTIVE)
 *             → STATE_INACTIVE handler fires (async)
 *             → cleanupResources() (all cleanup here)
 */
public class PasswordPrompt extends TerminalRenderable {
    
    private static final int STATE_INACTIVE = 40;
    private static final int STATE_ACTIVE = 41;
    private static final int STATE_CONFIRMING = 42;
    
    private final TerminalContainerHandle containerHandle;
    private PasswordKeyboardManager keyboardManager;
    
    // Config
    private String title = "Authentication";
    private String promptText = "Enter password:";
    private String confirmPromptText = null;
    private int timeoutSeconds = 30;
    private int boxWidth = 50;
    private int boxHeight = 11;
    
    // Callbacks
    private Consumer<NoteBytesEphemeral> onPassword;
    private Runnable onTimeout;
    private Runnable onCancel;
    private Runnable onMismatch;
    
    // Mutable state
    private volatile String currentPrompt = null;
    private volatile String statusMessage = null;
    private volatile NoteBytesEphemeral firstPassword = null;
    
    // Components
    private PasswordReader passwordReader;
    private CompletableFuture<Void> timeoutFuture;
    private NoteBytesReadOnly cancelHandlerId;
    
    // Layout
    private static class Layout {
        int boxX, boxY, promptRow, promptCol, inputRow, inputCol, statusRow, statusCol, footerRow;
    }
    private final Layout layout = new Layout();

    private final String passwordKeyboardManagerId;
    
    public PasswordPrompt(String name, String passwordKeyboardManagerId, TerminalContainerHandle containerHandle) {
        super(name);
        this.containerHandle = containerHandle;
        this.passwordKeyboardManagerId = passwordKeyboardManagerId;
    }
    
    @Override
    protected void setupStateTransitions() {
        // INACTIVE: cleanup resources, release keyboard
        stateMachine.onStateAdded(STATE_INACTIVE, (old, now, bit) -> {
            Log.logMsg("[PasswordPrompt] STATE_INACTIVE");
            cleanupResources(); // Async operations happen here
        });
        
        // ACTIVE: claim keyboard, start capture
        stateMachine.onStateAdded(STATE_ACTIVE, (old, now, bit) -> {
            Log.logMsg("[PasswordPrompt] STATE_ACTIVE");
            currentPrompt = promptText;
            statusMessage = null;
            
            // Async chain - NOT from transitionTo()
            claimKeyboard()
                .thenRun(this::startPasswordCapture)
                .thenRun(this::startTimeout)
                .thenRun(this::invalidate)
                .exceptionally(ex -> {
                    Log.logError("[PasswordPrompt] Activation failed: " + ex.getMessage());
                    // Rollback: transition to INACTIVE
                    // This transition will defer if layout in progress
                    transitionTo(STATE_ACTIVE, STATE_INACTIVE);
                    return null;
                });
        });
        
        // CONFIRMING: restart capture with confirmation prompt
        stateMachine.onStateAdded(STATE_CONFIRMING, (old, now, bit) -> {
            Log.logMsg("[PasswordPrompt] STATE_CONFIRMING");
            currentPrompt = confirmPromptText;
            statusMessage = null;
            
            startPasswordCapture();
            startTimeout();
            invalidate();
        });
        
        stateMachine.addState(STATE_INACTIVE);
    }
    
    @Override
    protected void setupEventHandlers() {
        cancelHandlerId = addKeyDownHandler(event -> {
            if (event instanceof KeyDownEvent kd && kd.getKeyCodeBytes().equals(KeyCodeBytes.ESCAPE)) {
                handleCancel();
            }
        });
    }
    
    // Device management
    private CompletableFuture<Void> claimKeyboard() {
        return containerHandle.getDeviceManager(passwordKeyboardManagerId)
            .thenCompose(manager -> {
                if (manager == null) {
                    Log.logMsg("[PasswordPrompt] Password keyboard not configured; using default input");
                    keyboardManager = null;
                    return CompletableFuture.completedFuture(null);
                }
                
                keyboardManager = (PasswordKeyboardManager) manager;
                return keyboardManager.setExclusiveMode(true)
                    .thenCompose(v -> keyboardManager.enable())
                    .thenApply(v -> null);
            })
            .handle((v,ex)->{
                if (ex != null) {
                    Log.logError("[PasswordPrompt] Keyboard claim failed: " + ex.getMessage());
                    keyboardManager = null;
                    throw new RuntimeException("Failed to claim keyboard", ex);
                }
                return null;
            });
    }
    
    private void releaseKeyboard() {
        if (keyboardManager == null) return;
        
        // Async but fire-and-forget
        keyboardManager.setExclusiveMode(false)
            .thenCompose(v -> keyboardManager.disable())
            .thenRun(() -> keyboardManager = null)
            .exceptionally(ex -> {
                Log.logError("[PasswordPrompt] Keyboard release failed: " + ex.getMessage());
                keyboardManager = null;
                return null;
            });
    }
    
    // Rendering
    @Override
    protected void renderSelf(TerminalBatchBuilder batch) {
        if (stateMachine.hasState(STATE_INACTIVE)) return;
        
        calculateLayout();
        clear(batch);

        TextStyle titleStyle = hasFocus() ? TextStyle.BOLD : TextStyle.DIM;

        drawBox(batch,layout.boxY, layout.boxX, boxWidth, boxHeight, title,  Position.TOP_CENTER, BoxStyle.DOUBLE, titleStyle);
        
        
        if (currentPrompt != null) {
            printAt(batch, layout.promptRow, layout.promptCol, currentPrompt, TextStyle.NORMAL);
        }
        
        printAt(batch, layout.inputRow, layout.inputCol, "> ", TextStyle.NORMAL);
        
        if (statusMessage != null) {
            int msgCol = layout.statusCol - (statusMessage.length() / 2);
            printAt(batch, layout.statusRow, msgCol, statusMessage, TextStyle.WARNING);
        }
        
        drawHLine(batch, layout.footerRow - 1, layout.boxX, boxWidth);
        String footer = "ESC: Cancel";
        int footerCol = layout.boxX + (boxWidth - footer.length()) / 2;
        printAt(batch, layout.footerRow, footerCol, footer, TextStyle.INFO);
    }
    
    private void calculateLayout() {
        int parentWidth = getWidth();
        int parentHeight = getHeight();
        
        layout.boxX = (parentWidth - boxWidth) / 2;
        layout.boxY = (parentHeight - boxHeight) / 2;
        layout.promptRow = layout.boxY + 3;
        layout.promptCol = layout.boxX + (boxWidth - (currentPrompt != null ? currentPrompt.length() : 0)) / 2;
        layout.inputRow = layout.boxY + 5;
        layout.inputCol = layout.boxX + (boxWidth / 2) - 10;
        layout.statusRow = layout.boxY + 8;
        layout.statusCol = layout.boxX + boxWidth / 2;
        layout.footerRow = layout.boxY + boxHeight - 1;
    }
    
    // Configuration
    public PasswordPrompt withTitle(String title) { this.title = title; return this; }
    public PasswordPrompt withPrompt(String prompt) { this.promptText = prompt; return this; }
    public PasswordPrompt withConfirmPrompt(String confirmPrompt) { this.confirmPromptText = confirmPrompt; return this; }
    public PasswordPrompt withTimeout(int seconds) { this.timeoutSeconds = seconds; return this; }
    public PasswordPrompt withBoxSize(int width, int height) { this.boxWidth = width; this.boxHeight = height; return this; }
    public PasswordPrompt onPassword(Consumer<NoteBytesEphemeral> handler) { this.onPassword = handler; return this; }
    public PasswordPrompt onTimeout(Runnable handler) { this.onTimeout = handler; return this; }
    public PasswordPrompt onCancel(Runnable handler) { this.onCancel = handler; return this; }
    public PasswordPrompt onMismatch(Runnable handler) { this.onMismatch = handler; return this; }
    
    // Lifecycle - synchronous state transitions
    public void activate() {
        if (!stateMachine.hasState(STATE_INACTIVE)) {
            Log.logError("[PasswordPrompt] Already active");
            return;
        }
        transitionTo(STATE_INACTIVE, STATE_ACTIVE);
    }
    
    public void deactivate() {
        if (stateMachine.hasState(STATE_INACTIVE)) return;
        
        int current = stateMachine.hasState(STATE_CONFIRMING) ? STATE_CONFIRMING : STATE_ACTIVE;
        transitionTo(current, STATE_INACTIVE);
    }
    
    // Password capture
    private void startPasswordCapture() {
        if (passwordReader == null) {
            passwordReader = new PasswordReader(eventRegistry);
        } else {
            passwordReader.escape();
        }
        
        passwordReader.setOnPassword(this::handlePasswordEntered);
    }
    
    private void handlePasswordEntered(NoteBytesEphemeral password) {
        cancelTimeout();
        
        if (confirmPromptText != null && stateMachine.hasState(STATE_ACTIVE)) {
            handleFirstPassword(password);
        } else if (stateMachine.hasState(STATE_CONFIRMING)) {
            handleConfirmPassword(password);
        } else {
            completeWithPassword(password);
        }
    }
    
    private void handleFirstPassword(NoteBytesEphemeral password) {
        firstPassword = password.copy();
        password.close();
        transitionTo(STATE_ACTIVE, STATE_CONFIRMING);
    }


    private void handleConfirmPassword(NoteBytesEphemeral password) {
        boolean match = firstPassword.equals(password);
        
        if (match) {
            password.close();
            NoteBytesEphemeral confirmedPassword = firstPassword;
            firstPassword = null;
            completeWithPassword(confirmedPassword);
        } else {
            firstPassword.close();
            firstPassword = null;
            password.close();
            
            if (onMismatch != null) {
                deactivate();
            } else {
                showMismatchError();
            }
        }
    }
    
    private void showMismatchError() {
        statusMessage = "Passwords do not match";
        invalidateRegion(layout.statusRow, layout.boxX, 1, boxWidth);
        
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(2000); } 
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, VirtualExecutors.getVirtualExecutor())
        .thenRun(() -> {
            statusMessage = null;
            transitionTo(STATE_CONFIRMING, STATE_ACTIVE);
        });
    }
    
    private void completeWithPassword(NoteBytesEphemeral password) {
        if (onPassword != null) {
            // Listen to INACTIVE state for callback
        } else {
            password.close();
        }
        deactivate();
    }
    
    // Timeout
    private void startTimeout() {
        cancelTimeout();
        if (timeoutSeconds <= 0) return;
        
        timeoutFuture = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(timeoutSeconds);
                if (!stateMachine.hasState(STATE_INACTIVE)) {
                    handleTimeout();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, VirtualExecutors.getVirtualExecutor());
    }
    
    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
            timeoutFuture = null;
        }
    }
    
    private void handleTimeout() {
        if (onTimeout != null) {
            stateMachine.onStateAdded(STATE_INACTIVE, (old, now, bit) -> {
                onTimeout.run();
            });
        }
        deactivate();
    }
    
    private void handleCancel() {
        if (onCancel != null) {
            stateMachine.onStateAdded(STATE_INACTIVE, (old, now, bit) -> {
                onCancel.run();
            });
        }
        deactivate();
    }
    
    // Cleanup - called by STATE_INACTIVE handler
    private void cleanupResources() {
        cancelTimeout();
        
        if (passwordReader != null) {
            passwordReader.close();
            passwordReader = null;
        }
        
        if (firstPassword != null) {
            firstPassword.close();
            firstPassword = null;
        }
        
        releaseKeyboard(); // Fire-and-forget async
    }
    
    public boolean isActive() {
        return !stateMachine.hasState(STATE_INACTIVE);
    }

    protected void onCleanup() {
        if(cancelHandlerId != null){
            removeKeyDownHandler(cancelHandlerId);
        }
        deactivate();
    }
}
