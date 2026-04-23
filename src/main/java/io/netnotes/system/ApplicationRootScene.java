package io.netnotes.system;


import io.netnotes.engine.ui.BorderPanel;
import io.netnotes.engine.ui.SizePreference;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
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
    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;
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
        this.scrollPanel.setWidthPreference(SizePreference.FILL);
        this.scrollPanel.setHeightPreference(SizePreference.FILL);
        
        setPanel(BorderPanel.CENTER, scrollPanel);
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


    private void removeOldScaffolding() {
        if (!isScaffoldingActive()) return;
        SystemUIInterface old = currentScaffolding;
        currentScaffolding = null; 
        scrollPanel.removeContent(old.getUI());
    }

    public void clearScaffolding() {
        removeOldScaffolding();
        setContextBarVisible(true);
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
        Log.logMsg("[ApplicationRootScene] showing initProgress", LOG_LEVEL);
        initProgress.getProgressBar().reset();
        swapPanel(BorderPanel.CENTER, initProgress);
    }

    
    public void updatePasswordInitProgress(double percent, String msg) {
        if (initProgress != null) {
            Log.logMsg("[ApplicationRootScene] updating initProgress: " + msg + " " + percent + "%", LOG_LEVEL);
            initProgress.getProgressBar().updatePercentDouble(percent);
            
            //TODO: update label
        }
    }
    
    public void showPasswordPrompt(PasswordPrompt prompt) {
        activePasswordPrompt = prompt;
        if (initProgress != null) {
            removeFromPanel(BorderPanel.CENTER, initProgress);
            initProgress = null;
        }
        Log.logMsg("[ApplicationRootScene] showing password prompt", LOG_LEVEL);
        swapPanel(BorderPanel.CENTER, prompt);
    }
    
    public void closePasswordPrompt() {
        Log.logMsg("[ApplicationRootScene] closing password prompt", LOG_LEVEL);
        if (activePasswordPrompt != null) {
            removeFromPanel(BorderPanel.CENTER, activePasswordPrompt);
            activePasswordPrompt = null;
        }
        swapPanel(BorderPanel.CENTER, scrollPanel);
    }
}