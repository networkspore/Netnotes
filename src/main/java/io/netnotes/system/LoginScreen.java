package io.netnotes.system;

import java.util.function.Consumer;

import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle;
import io.netnotes.terminal.TextStyle.BoxStyle;
import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.components.text.TerminalTextBox;
import io.netnotes.terminal.layout.TerminalLayoutData;
import io.netnotes.engine.io.input.events.keyboardEvents.KeyDownEvent;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.state.ConcurrentBitFlagStateMachine;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;

class LoginScreen extends TerminalBorderPanel implements SystemUIInterface {
    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;
    private static final int STATE_SHOWING_PROMPT = 0;
    private static final int STATE_SHOWING_ERROR = 1;

    private final SystemApplication application;
    private final ConcurrentBitFlagStateMachine stateMachine;
    private final TerminalTextBox errorBox;

    private volatile String errorMessage = null;
    private volatile boolean timeoutError = false;
    private NoteBytesReadOnly errorKeyHandlerId = null;
    private Consumer<SystemUIInterface> onDisposed;

    public LoginScreen(SystemApplication application) {
        super("login-screen");
        this.application = application;
        this.stateMachine = new ConcurrentBitFlagStateMachine("LoginScreen");
        this.stateMachine.setSerialExecutor(application.getUiExecutor());

        errorBox = new TerminalTextBox("login-error");
        errorBox.setBorderStyle(BoxStyle.DOUBLE);
        errorBox.setTitle("Login Failed");
        errorBox.setTextStyle(TextStyle.ERROR);

        buildUI();
        stateMachine.addState(STATE_SHOWING_PROMPT);
    }

    private void buildUI() {
        addChild(errorBox, ctx -> {
            var parent = ctx.getParentRegion();
            int width = Math.min(60, parent.getWidth() - 4);
            int height = 6;
            int x = Math.max(2, (parent.getWidth() - width) / 2);
            int y = Math.max(2, (parent.getHeight() - height) / 2);
            return TerminalLayoutData.getBuilder()
                .setX(x).setY(y)
                .setWidth(width).setHeight(height)
                .build();
        });

        errorBox.hide();
    }

    @Override
    protected void setupStateTransitions() {
        stateMachine.onStateAdded(STATE_SHOWING_PROMPT, (old, now, bit) -> {
            Log.logMsg("[LoginScreen] STATE_SHOWING_PROMPT", LOG_LEVEL);
            timeoutError = false;

            String title = application.isAuthenticated() ? "System Locked" : "Netnotes";
            
            application.getPasswordService()
                .requestVerification(title, "Enter password:", pw -> application.authenticate(pw), 3)
                .thenAccept(result -> {
                    if (result.success()) {
                        // Success handled by SystemApplication state transitions
                    } else {
                        errorMessage = result.message();
                        if (errorMessage.contains("timeout")) {
                            timeoutError = true;
                        }
                        application.getUiExecutor().submit(() -> {
                            try { Thread.sleep(1000); } 
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                            transitionTo(STATE_SHOWING_PROMPT, STATE_SHOWING_ERROR);
                            return null;
                        });
                    }
                });
            
            errorBox.hide();
            removeErrorKeyHandler();
        });

        stateMachine.onStateAdded(STATE_SHOWING_ERROR, (old, now, bit) -> {
            Log.logError("[LoginScreen] STATE_SHOWING_ERROR: " + errorMessage);
            errorBox.setText(buildErrorText());
            errorBox.show();
            registerErrorKeyHandler();
        });
    }

    private String buildErrorText() {
        String msg = errorMessage != null ? errorMessage : "Login failed";
        return msg + "\n\nPress any key to continue...";
    }

    private void registerErrorKeyHandler() {
        if (errorKeyHandlerId != null) return;
        
        errorKeyHandlerId = addKeyDownHandler(event -> {
            if (event instanceof KeyDownEvent) {
                if (timeoutError) {
                    application.getStateMachine().removeState(SystemApplication.AUTHENTICATING);
                    application.getStateMachine().addState(SystemApplication.LOCKED);
                } else {
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
    protected void onRemovedFromLayout() {
        removeErrorKeyHandler();
        
        if (onDisposed != null) {
            onDisposed.accept(this);
        }
        
        destroy();
    }
}