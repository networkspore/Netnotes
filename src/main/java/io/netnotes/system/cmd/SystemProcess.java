package io.netnotes.system.cmd;

import java.util.concurrent.CompletableFuture;

import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.RoutedPacket;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.system.SystemApplication;
import io.netnotes.system.SystemUIInterface;

/**
 * SystemProcess - FlowProcess with TerminalUI + CommandContext
 * 
 * Process lifecycle independent of UI display:
 * - run() starts computational work
 * - UI can attach/detach without affecting process state
 * - Registered in process tree on startup
 */
public abstract class SystemProcess extends FlowProcess {
    
    protected final SystemApplication application;
    private SystemUIInterface ui;
    private CommandContext commandContext;
    
    protected SystemProcess(String name, SystemApplication application) {
        super(name, ProcessType.BIDIRECTIONAL);
        this.application = application;
    }
    
    /**
     * Build UI - called when process UI needs to be displayed
     * May be called multiple times if UI detaches/reattaches
     */
    protected abstract SystemUIInterface buildUI();
    
    /**
     * Build command context - called on UI build
     */
    protected abstract CommandContext buildCommandContext();
    
    /**
     * Process execution - independent of UI
     * Override for long-running processes
     */
    @Override
    public CompletableFuture<Void> run() {
        onStart();
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Message handling - standard FlowProcess behavior
     */
    @Override
    public CompletableFuture<Void> handleMessage(RoutedPacket packet) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        // Default: no streaming
    }
    
    // ===== UI LIFECYCLE =====
    
    /**
     * Renderable cannot be re-used, must call onDisposed onCleanup() 
     * (when removal from layoutManager)
     * @return
     */
    public TerminalRenderable getUI() {
        if (ui == null) {
            ui = buildUI();
            ui.setOnDisposed(this::disposeUI);
        }
        return ui.getUI();
    }
    
    public CommandContext getCommandContext() {
        if (commandContext == null) {
            commandContext = buildCommandContext();
        }
        return commandContext;
    }
    
    /* 
     */
    private void disposeUI(SystemUIInterface uiInterface) {
        uiInterface.setOnDisposed(null);
        if (ui != null) {
            ui = null;
        }
    }

    //TODO: Verify can this be externally disposed, or should it dispose when finished? 
    public void dispose(){
        commandContext = null;
    }
    
    // ===== NAVIGATION =====
    
    protected void pushProcess(String processId) {
     //   application.startProcess(processId)
          //  .thenAccept(application::pushProcess);
    }
    
    protected void goBack() {
        //application.goBack();
    }
    
    // ===== RESOURCE REQUIREMENTS =====
    
    /**
     * Declare resource requirements
     * Override to specify resources this process needs
     */
    public String[] getRequiredResources() {
        return new String[0];
    }
    
    public SystemApplication getApplication() {
        return application;
    }
}