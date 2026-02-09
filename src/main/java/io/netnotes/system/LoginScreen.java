package io.netnotes.system;

import java.util.function.Consumer;

import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle;
import io.netnotes.terminal.TextStyle.BoxStyle;
import io.netnotes.terminal.components.TerminalTextBox;
import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.layout.TerminalLayoutData;
import io.netnotes.engine.io.input.events.keyboardEvents.KeyDownEvent;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.state.ConcurrentBitFlagStateMachine;
import io.netnotes.engine.utils.LoggingHelpers.Log;

/**
 * LoginScreen - Scaffolding screen for authentication
 * 
 * REFACTORED to:
 * - Extend TerminalBorderPanel (not SystemScene)
 * - Implement SystemUIInterface
 * - Own internal state machine
 * - Be single-use (recreated on each show)
 */
class LoginScreen extends TerminalBorderPanel implements SystemUIInterface {
    
    // Internal states for login flow
    private static final int STATE_SHOWING_PROMPT = 0;
    private static final int STATE_PROCESSING = 1;
    private static final int STATE_SHOWING_ERROR = 2;

    private final SystemApplication application;
    private final ConcurrentBitFlagStateMachine stateMachine;
    private final PasswordPrompt passwordPrompt;
    private final TerminalTextBox errorBox;
    private final TerminalTextBox processingBox;

    private volatile String errorMessage = null;
    private volatile boolean timeoutError = false;
    private NoteBytesReadOnly errorKeyHandlerId = null;
    private Consumer<SystemUIInterface> onDisposed;

    public LoginScreen(SystemApplication application) {
        super("login-screen");
        this.application = application;
        this.stateMachine = new ConcurrentBitFlagStateMachine("LoginScreen");
        this.stateMachine.setSerialExecutor(application.getUiExecutor());

        // Create components
        passwordPrompt = new PasswordPrompt("login-auth", SystemApplication.PASSWORD_KEYBOARD_MANAGER_ID, application.getContainerHandle());
        
        errorBox = new TerminalTextBox("login-error");
        errorBox.setBorderStyle(BoxStyle.DOUBLE);
        errorBox.setTitle("Login Failed");
        errorBox.setTextStyle(TextStyle.ERROR);

        processingBox = new TerminalTextBox("login-processing");
        processingBox.setBorderStyle(BoxStyle.SINGLE);
        processingBox.setTitle("Authenticating");
        processingBox.setTextStyle(TextStyle.INFO);

        buildUI();
        
        // Start in prompt state
        stateMachine.addState(STATE_SHOWING_PROMPT);
    }

    private void buildUI() {
        // Use BorderPanel's setPanel method to layout components
        // Password prompt takes full center
        setPanel(Panel.CENTER, passwordPrompt);
        
        // Error and processing boxes overlay on top (initially hidden)
        addChild(errorBox, ctx -> {
            var parent = ctx.getParentRegion();
            int width = Math.min(60, parent.getWidth() - 4);
            int height = 6;
            int x = Math.max(2, (parent.getWidth() - width) / 2);
            int y = Math.max(2, (parent.getHeight() - height) / 2);

            return TerminalLayoutData.getBuilder()
                .setX(x)
                .setY(y)
                .setWidth(width)
                .setHeight(height).build();
        });

        addChild(processingBox, ctx -> {
            var parent = ctx.getParentRegion();
            int width = Math.min(50, parent.getWidth() - 4);
            int height = 5;
            int x = Math.max(2, (parent.getWidth() - width) / 2);
            int y = Math.max(2, (parent.getHeight() - height) / 2);
            return TerminalLayoutData.getBuilder()
                .setX(x).setY(y)
                .setWidth(width).setHeight(height)
                .build();
        });

        errorBox.hide();
        processingBox.hide();
    }

    @Override
    protected void setupStateTransitions() {
        stateMachine.onStateAdded(STATE_SHOWING_PROMPT, (old, now, bit) -> {
            Log.logMsg("[LoginScreen] STATE_SHOWING_PROMPT");
            timeoutError = false;

            String title = application.isAuthenticated() ? "System Locked" : "Netnotes";
            passwordPrompt
                .withTitle(title)
                .withPrompt("Enter password:")
                .withTimeout(30)
                .onPassword(this::handlePassword)
                .onTimeout(this::handleTimeout)
                .onCancel(this::handleCancel);

            passwordPrompt.show();
            passwordPrompt.activate();
            processingBox.hide();
            errorBox.hide();
            removeErrorKeyHandler();
        });

        stateMachine.onStateAdded(STATE_PROCESSING, (old, now, bit) -> {
            Log.logMsg("[LoginScreen] STATE_PROCESSING");
            passwordPrompt.deactivate();
            processingBox.setText("Verifying...");
            processingBox.show();
            errorBox.hide();
        });

        stateMachine.onStateAdded(STATE_SHOWING_ERROR, (old, now, bit) -> {
            Log.logMsg("[LoginScreen] STATE_SHOWING_ERROR: " + errorMessage);
            passwordPrompt.deactivate();
            processingBox.hide();
            errorBox.setText(buildErrorText());
            errorBox.show();
            registerErrorKeyHandler();
        });
    }

    private void handlePassword(NoteBytesEphemeral password) {
        transitionTo(STATE_SHOWING_PROMPT, STATE_PROCESSING);

        application.authenticate(password)
            .thenAccept(valid -> {
                password.close();

                if (!valid) {
                    errorMessage = "Invalid password";
                    application.getIoExecutor().submit(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        transitionTo(STATE_PROCESSING, STATE_SHOWING_ERROR);
                        return null;
                    });
                }
                // On success, SystemApplication will transition state and show new screen
            })
            .exceptionally(ex -> {
                password.close();
                errorMessage = "Login failed: " + ex.getMessage();
                transitionTo(STATE_PROCESSING, STATE_SHOWING_ERROR);
                return null;
            });
    }

    private void handleTimeout() {
        errorMessage = "Authentication timeout";
        timeoutError = true;
        transitionTo(STATE_SHOWING_PROMPT, STATE_SHOWING_ERROR);
    }

    private void handleCancel() {
        // Return to locked state
        application.getStateMachine().removeState(SystemApplication.AUTHENTICATING);
        application.getStateMachine().addState(SystemApplication.LOCKED);
    }

    private String buildErrorText() {
        String msg = errorMessage != null ? errorMessage : "Login failed";
        return msg + "\n\nPress any key to continue...";
    }

    private void registerErrorKeyHandler() {
        if (errorKeyHandlerId != null) {
            return;
        }
        errorKeyHandlerId = addKeyDownHandler(event -> {
            if (event instanceof KeyDownEvent) {
                if (timeoutError) {
                    // Timeout returns to locked
                    application.getStateMachine().removeState(SystemApplication.AUTHENTICATING);
                    application.getStateMachine().addState(SystemApplication.LOCKED);
                } else {
                    // Other errors retry prompt
                    transitionTo(STATE_SHOWING_ERROR, STATE_SHOWING_PROMPT);
                }
            }
        });
    }

    private void removeErrorKeyHandler() {
        if (errorKeyHandlerId != null) {
            removeKeyDownHandler(errorKeyHandlerId);
            errorKeyHandlerId = null;
        }
    }

    @Override
    public TerminalRenderable getUI() {
        return this;
    }

    @Override
    public void setOnDisposed(Consumer<SystemUIInterface> onDisposed) {
        this.onDisposed = onDisposed;
    }

    @Override
    protected void onCleanup() {
        // Clean up resources
        removeErrorKeyHandler();
        passwordPrompt.deactivate();
        
        // Notify disposal
        if (onDisposed != null) {
            onDisposed.accept(this);
        }
        
        super.onCleanup();
    }
}