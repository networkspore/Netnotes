package io.netnotes.system;

import io.netnotes.terminal.TextStyle;

import java.util.function.Consumer;

import io.netnotes.terminal.TerminalCommands;
import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.components.panels.TerminalVStack;
import io.netnotes.terminal.components.panels.TerminalHStack.HAlignment;
import io.netnotes.terminal.components.text.TerminalLabel;
import io.netnotes.engine.io.input.ephemeralEvents.EphemeralKeyDownEvent;
import io.netnotes.engine.io.input.events.keyboardEvents.KeyDownEvent;
import io.netnotes.engine.ui.SizePreference;

/**
 * LockedScreen - Shows when system is locked
 * Press any key → transition to AUTHENTICATING
 * 
 * REFACTORED to use TerminalVStack for simplified layout management
 */
class LockedScreen extends TerminalVStack implements SystemUIInterface {
    private final TerminalLabel titleLabel;
    private final TerminalLabel promptLabel;
    private SystemApplication application;

    private Consumer<SystemUIInterface> onDisposed;

    public LockedScreen(SystemApplication application) {
        super("system-locked-screen");
        this.setWidthPreference(SizePreference.FILL);
        this.setHeightPreference(SizePreference.FILL);
        this.setVAlignment(VAlignment.CENTER);
        this.setHAlignment(HAlignment.CENTER);
        this.setSpacing(2);

        // Create labels
        titleLabel = new TerminalLabel("locked-title", "System Locked");
        titleLabel.setTextStyle(TextStyle.BOLD);

        promptLabel = new TerminalLabel("locked-prompt", TerminalCommands.PRESS_ANY_KEY);
        promptLabel.setTextStyle(TextStyle.NORMAL);

        buildUi();
    }

    @Override
    public void setOnDisposed(Consumer<SystemUIInterface> onDisposed){ this.onDisposed = onDisposed; }

    
    private void buildUi() {
        // Add labels to VStack - it will handle their positioning
        addChild(titleLabel);
        addChild(promptLabel);
    }
    
    @Override
    protected void setupEventHandlers() {
        addKeyDownHandler(event -> {
            if (event instanceof KeyDownEvent || event instanceof EphemeralKeyDownEvent) {
                if (event instanceof EphemeralKeyDownEvent e) {
                    e.close();
                }
                application.getStateMachine().removeState(SystemApplication.LOCKED);
                application.getStateMachine().addState(SystemApplication.AUTHENTICATING);
            }
        });
    }

    public TerminalRenderable getUI(){
        return this;
    }

    /*
     * Called when removed from the layout (Cannot be reused)    
     */
    @Override
    protected void onRemovedFromLayout(){
        if(onDisposed != null){
            onDisposed.accept(this);
        }
        destroy();
    }
}