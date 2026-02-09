package io.netnotes.system.cmd;

import io.netnotes.system.ApplicationRootScene;
import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.components.TerminalLabel;
import io.netnotes.terminal.input.TerminalTextInput;

/**
 * CommandInterpreter - Breadcrumb + input line
 * Phase 2 implementation
 */
public class CommandInterpreter {
    
    private final String name;
    private final ApplicationRootScene rootScene;
    private final TerminalBorderPanel ui;
    private final TerminalLabel breadcrumb;
    private final TerminalTextInput input;
    
    private CommandContext currentContext;
    
    public CommandInterpreter(String name, ApplicationRootScene rootScene) {
        this.name = name;
        this.rootScene = rootScene;
        this.ui = new TerminalBorderPanel(name + "-ui");
        this.breadcrumb = new TerminalLabel(name + "-breadcrumb", "");
        this.input = new TerminalTextInput(name + "-input", 0, 0, 256);
        
        ui.setPanel(TerminalBorderPanel.Panel.LEFT, breadcrumb);
        ui.setPanel(TerminalBorderPanel.Panel.RIGHT, input);
        
        input.setOnComplete(this::handleCommand);
    }
    
    public TerminalRenderable getUI() {
        return ui;
    }
    
    public void updateContext(CommandContext context) {
        this.currentContext = context;
        updateBreadcrumb();
    }
    
    private void updateBreadcrumb() {
        if (currentContext == null) {
            breadcrumb.setText("");
        } else {
            String[] path = currentContext.getPath();
            breadcrumb.setText(String.join(" > ", path));
        }
    }
    
    private void handleCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) {
            input.clear();
            return;
        }
        
      /*  rootScene.executeCommand(cmd)
            .thenRun(() -> input.clear())
            .exceptionally(ex -> {
                input.setText("Error: " + ex.getMessage());
                return null;
            });*/
    }
}