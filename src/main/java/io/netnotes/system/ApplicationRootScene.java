package io.netnotes.system;


import io.netnotes.terminal.components.panels.TerminalBorderPanel;
import io.netnotes.terminal.components.panels.TerminalScrollPanel;
import io.netnotes.terminal.components.panels.TerminalStackPanel;

/**
 * ApplicationRootScene - Desktop + scaffolding container
 * 
 * Modes:
 * - Scaffolding: System screens (setup, lock, login, main menu)
 * - Desktop: Process stack + context bar
 */
public class ApplicationRootScene extends TerminalBorderPanel {
    
    private final SystemApplication application;
   
    private final TerminalScrollPanel scrollPanel;
  //  private final ContextBar contextBar;
 
    private SystemUIInterface currentScaffolding;

    
    public ApplicationRootScene(String name, SystemApplication application) {
        super(name);
        this.application = application;
       
        this.scrollPanel = new TerminalScrollPanel(name + "-scroll");
       // this.contextBar = new ContextBar(name + "-context", this);
        
        setPanel(TerminalBorderPanel.Panel.CENTER, scrollPanel);
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
            TerminalStackPanel stackPanel = getRegionStack(Panel.CENTER);
            stackPanel.removeFromStack(currentScaffolding.getUI());
        }
    }


    private void setContextBarVisible(boolean isContextVisible){

    }
    
    public boolean isScaffoldingActive() {
        return currentScaffolding != null;
    }
    
}