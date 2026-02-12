package io.netnotes.system;

import io.netnotes.terminal.components.TerminalProgressBar;
import io.netnotes.terminal.components.panels.TerminalPanel;

public class ApplicationRootInitProgress extends TerminalPanel {
    private final TerminalPanel leftPanel;
    private final TerminalProgressBar passwordInitProgress;
    private final TerminalPanel rightPanel;

    public ApplicationRootInitProgress(){
        super("app-init-progress-panel");

        setAxis(Axis.HORIZONTAL);
        leftPanel = new TerminalPanel("app-init-left-panel");
        leftPanel.setWidthPreference(SizePreference.FILL);
        
        rightPanel = new TerminalPanel("app-init-right-panel");    
        rightPanel.setWidthPreference(SizePreference.FILL);

        passwordInitProgress = TerminalProgressBar.builder()
                .name("password-init-progress")
                .label("Initializing secure keyboard...")
                .style(TerminalProgressBar.Style.SMOOTH)
                .bounds(0, 0, 36, 4)
                .build();
        passwordInitProgress.setMinWidth(36);
        passwordInitProgress.setWidthPreference(SizePreference.FILL);
        
        init();
    }

    public TerminalProgressBar getProgressBar(){
        return passwordInitProgress;
    }

    private void init(){
        addChild(leftPanel);
        addChild(passwordInitProgress);
        addChild(rightPanel);
    }
}
