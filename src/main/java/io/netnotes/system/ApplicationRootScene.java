package io.netnotes.system;


import io.netnotes.engine.ui.BorderPanel;
import io.netnotes.terminal.components.input.PasswordPrompt;
import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.components.panels.TerminalScrollPanel;

/**
 * ApplicationRootScene - Desktop + scaffolding container
 * 
 * Modes:
 * - Scaffolding: System screens (setup, lock, login, main menu)
 * - Desktop: Process stack + context bar
 */
public class ApplicationRootScene extends TerminalBorderPanel {
    
    private final SystemApplication application;

    private PasswordPrompt activePasswordPrompt;
    private final TerminalScrollPanel scrollPanel;
  //  private final ContextBar contextBar;
 
    private SystemUIInterface currentScaffolding;
    private ApplicationRootInitProgress initProgress = null;
    
    public ApplicationRootScene(SystemApplication application) {
        super("system-root-scene");
        this.application = application;
        this.scrollPanel = new TerminalScrollPanel(name + "-scroll");
       // this.contextBar = new ContextBar(name + "-context", this);
        
        setPanel(BorderPanel.CENTER, scrollPanel);
      //  layout.setPanel(TerminalBorderPanel.Panel.BOTTOM, contextBar.getUI());
    }
    
    @Override
    protected void setupStateTransitions() {}
    
    // ===== SCAFFOLDING MODE =====
    
    public void showScaffolding(SystemUIInterface scaffolding) {
        if(scaffolding == currentScaffolding) return;
        removeOldScaffolding();
        currentScaffolding = scaffolding;

        scrollPanel.swapContent(scaffolding.getUI());
        setContextBarVisible(false);
    }


    private void removeOldScaffolding(){
        if(isScaffoldingActive()){
            scrollPanel.removeContent(currentScaffolding.getUI());
        }
    }


    private void setContextBarVisible(boolean isContextVisible){

    }
    
    public boolean isScaffoldingActive() {
        return currentScaffolding != null;
    }
    
    // === Password Mode ===

    public void showPasswordInitializing() {
        if (initProgress == null) {
            initProgress = new ApplicationRootInitProgress();
        }
        initProgress.getProgressBar().reset();
        swapPanel(BorderPanel.CENTER, initProgress);
    }

    
    public void updatePasswordInitProgress(double percent, String msg) {
        if (initProgress != null) {
            initProgress.getProgressBar().updatePercent(percent * 100);
            initProgress.getProgressBar().setLabel(msg);
        }
    }
    
    public void showPasswordPrompt(PasswordPrompt prompt) {
        activePasswordPrompt = prompt;
        if(initProgress != null){
            removeFromPanel(BorderPanel.CENTER, initProgress);
            initProgress = null;
        }
        swapPanel(BorderPanel.CENTER, prompt);
    }
    
    public void closePasswordPrompt() {
        if (activePasswordPrompt != null) {
            removeFromPanel(BorderPanel.CENTER, activePasswordPrompt);
            swapPanel(BorderPanel.CENTER, scrollPanel);
            activePasswordPrompt = null;
        }
    }
}