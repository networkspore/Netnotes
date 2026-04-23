package io.netnotes.system;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netnotes.consoleRenderer.ConsoleRenderer;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.daemon.IODaemonManager;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.FlowProcessService;
import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesReadOnly;

import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.TerminalRectangle;
import io.netnotes.terminal.layout.TerminalLayoutData;
import io.netnotes.engine.state.ConcurrentBitFlagStateMachine;
import io.netnotes.engine.ui.renderer.RenderingService;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.engine.virtualExecutors.SerializedVirtualExecutor;
import io.netnotes.engine.virtualExecutors.VirtualExecutors;

/**
 * SystemApplication - Process-based terminal system with authentication
 * Startup States: These four are mutually exclusive 
 *      FIRST_RUN - syscfg.dat does not exist. User has never completed setup. 
 *          Show full SystemSetupScreen (keyboard selection + password creation).
 *      SETUP_NEEDED - syscfg.dat exists but settings.dat does not. 
 *          Keyboard was selected but password was never created. Show 
 *          SystemSetupScreen resuming at the password creation step.
 *      FAILED_SETTING - Ssettings.dat exists and contains OLD_BCRYPT_KEY / OLD_SALT_KEY fields. 
 *          A password change was started but not completed. Show recovery UI.
 *      AUTHENTICATING - Both files exist and settings.dat has no old key fields. Normal login.
 * 
 *  — CHECKING_SETTINGS handler should set exactly one of them and nothing else.
 */
public class SystemApplication extends FlowProcess {
    private final static LogLevel LOG_LEVEL = LogLevel.IMPORTANT;
   
    // ===== HANDLE STATES =====
    public static final int DETACHED = 0;
    public static final int ATTACHED = 1;
    
    // ===== APPLICATION STATES =====
    public static final int INITIALIZING = 2;
    public static final int SETUP_NEEDED = 3;
    public static final int SETUP_COMPLETE = 4;
    public static final int LOCKED = 5;
    public static final int CHECKING_SETTINGS = 6;
    public static final int FIRST_RUN = 7;
    public static final int AUTHENTICATING = 8;
    public static final int AUTH_TIMEOUT = 9;
    public static final int ERROR = 10;
    public static final int FAILED_SETTINGS = 11;
    public static final int INITIALIZED = 12;
    public static final int AUTHENTICATED = 13;
    public static final int INFRASTRUCTURE_INITIALIZING = 14;
    public static final int INFRASTRUCTURE_READY = 15;
    public static final int SHUTDOWN_REQUESTED = 16;
    public static final int SHUTTING_DOWN = 17;
    public static final int SHUTDOWN_READY = 18;
    
    // ===== INFRASTRUCTURE =====
    protected final String id;
    protected final ConcurrentBitFlagStateMachine stateMachine;
    protected final SerializedVirtualExecutor uiExecutor = VirtualExecutors.getUiExecutor();
    protected final SerializedVirtualExecutor ioExecutor = VirtualExecutors.getIoExecutor();
    
    private final ConsoleRenderer uiRenderer;
    private final RenderingService renderingService;
    private final FlowProcessService processService;
    private final IODaemonManager ioDaemonManager;
    private final PasswordService passwordService;
    // ===== UI STATE =====
    protected TerminalContainerHandle containerHandle;
    private ApplicationRootScene rootScene;

    
    // ===== AUTHENTICATION =====
    
    private NoteBytes claimedKeyboardId;
    private SystemRuntime systemRuntime;
    private RuntimeAccess systemAccess;
    private CompletableFuture<Void> authTimeoutFuture;

    // ===== LIFECYCLE =====

    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private volatile boolean shutdownInProgress = false;
    private CompletableFuture<Void> deattachHandleFuture = null;
    
    // ===== MODE FLAGS =====
    protected boolean startDetached = false;
    private boolean allowRemoteAccess = false;
    private boolean isInRecoveryMode = false;
    private String recoveryReason = null;
    
    private static final long AUTH_TIMEOUT_SECONDS = 30;
    public static final String DEFAULT_IO_DAEMON_SOCKET_PATH = "/var/run/io-daemon.sock";
    public static final NoteBytesReadOnly PASSWORD_KEYBOARD_MANAGER_ID = 
        new NoteBytesReadOnly("password-keyboard");

    
    // ===== CONSTRUCTION =====

    /**
     * Constructor 1: FIRST RUN / RECOVERY MODE
     * Used when bootstrap config doesn't exist or is invalid.
     * Initializes minimal infrastructure to show SystemSetupScreen.
     */
    private SystemApplication(
        ConsoleRenderer uiRenderer,
        RenderingService renderingService,
        FlowProcessService processService,
        ProcessRegistryInterface registry
    ) {
        super("SystemApplication", ProcessType.BIDIRECTIONAL);
        this.id = NoteUUID.createSafeUUID128();
        this.stateMachine = new ConcurrentBitFlagStateMachine("SystemApplication:" + id);
        this.stateMachine.setSerialExecutor(uiExecutor);
        this.uiRenderer = uiRenderer;
        this.renderingService = renderingService;
        this.processService = processService;
        this.passwordService = new PasswordService(this);
        this.ioDaemonManager = new IODaemonManager(
            CoreConstants.IO_DAEMON, 
            CoreConstants.IO_DAEMON_PATH,
            registry, 
            SystemApplication.DEFAULT_IO_DAEMON_SOCKET_PATH
        );
        this.uiRenderer.setOnCtrlC(this::handleCtrlC);
        setupStateTransitions();
        setupShutdownHook();
    }
    
    /**
     * Constructor 2: NORMAL BOOTSTRAP
     * Used when valid bootstrap config exists.
     * Initializes with config values - can skip setup, go straight to auth/recovery.
     */
    private SystemApplication(
        ConsoleRenderer uiRenderer,
        RenderingService renderingService,
        FlowProcessService processService,
        ProcessRegistryInterface registry,
        BootstrapConfig config
    ) {
        super("SystemApplication", ProcessType.BIDIRECTIONAL);
        this.id = NoteUUID.createSafeUUID128();
        this.stateMachine = new ConcurrentBitFlagStateMachine("SystemApplication:" + id);
        this.stateMachine.setSerialExecutor(uiExecutor);
        this.uiRenderer = uiRenderer;
        this.renderingService = renderingService;
        this.processService = processService;
        this.passwordService = new PasswordService(this);
        this.startDetached = config.isDetachedMode();
        this.claimedKeyboardId = config.getClaimedKeyboardId().orElse(null);
        this.isInRecoveryMode = config.isRecoveryMode();
        this.recoveryReason = config.getRecoveryReason().orElse(null);

        String socketPath = config.getIoDaemonSocketPath()
            .orElse(SystemApplication.DEFAULT_IO_DAEMON_SOCKET_PATH);
        
        this.ioDaemonManager = new IODaemonManager(
            CoreConstants.IO_DAEMON, 
            CoreConstants.IO_DAEMON_PATH,
            registry, 
            socketPath
        );
        
        this.uiRenderer.setOnCtrlC(this::handleCtrlC);
        setupStateTransitions();
        setupShutdownHook();
    }

    private void setupShutdownHook() {
        /* Not required for now
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shutdownInProgress) {
            }
        }, "shutdown-hook"));
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[SystemApplication] Uncaught exception in " + 
                thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        }); */
    }

    private void handleCtrlC(){
        requestShutdown();
    }

    public static CompletableFuture<Void> start() {
        Log.logMsg("[SystemApplication] Loading bootstrap config...", LOG_LEVEL);

        return BootstrapConfig.load()
            .thenCompose(config -> TerminalInitializer.createAndInitialize()
                .thenCompose(renderer -> bootstrap(renderer, config)))  // config may be null
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        Log.logError("[SystemApplication]", "Application error", ex);
                        ex.printStackTrace();
                        System.exit(1);
                    } else {
                        Log.logMsg("[SystemApplication] Clean shutdown", LOG_LEVEL);
                    }
                });
    }

    private static CompletableFuture<Void> bootstrap(
        ConsoleRenderer uiRenderer,
        BootstrapConfig config          // null when syscfg.dat does not exist
    ) {
        FlowProcessService processService = new FlowProcessService();
        ProcessRegistryInterface registry = processService.getRegistryInterface();

        return startRenderingService(uiRenderer, registry)
            .thenCompose(renderingService -> {
                SystemApplication app = config != null
                    ? new SystemApplication(uiRenderer, renderingService, processService, registry, config)
                    : new SystemApplication(uiRenderer, renderingService, processService, registry);

                registry.registerProcess(app, CoreConstants.SYSTEM_PATH, null, registry);

                return registry.startProcess(CoreConstants.SYSTEM_PATH)
                    .thenRun(() -> {
                        registry.connect(CoreConstants.SYSTEM_PATH, CoreConstants.RENDERING_SERVICE_PATH);
                        registry.connect(CoreConstants.RENDERING_SERVICE_PATH, CoreConstants.SYSTEM_PATH);
                    })
                    .thenCompose(v -> {
                        if (app.startDetached) {
                            app.stateMachine.addState(DETACHED);
                            return CompletableFuture.completedFuture(null);
                        }
                        return app.attachLocalTerminal();
                    })
                    .thenRun(() -> {
                        Log.logMsg("[SystemApplication] Bootstrap complete - entering state machine", LOG_LEVEL);
                        app.stateMachine.addState(INITIALIZED);  // ← single entry point
                    })
                    .thenCompose(v -> app.shutdownFuture);
            });
}


    private static CompletableFuture<RenderingService> startRenderingService(
        ConsoleRenderer uiRenderer,
        ProcessRegistryInterface registry
    ) {
        RenderingService renderingService = new RenderingService(
            CoreConstants.RENDERING_SERVICE,
            uiRenderer
        );
        
        ContextPath servicePath = registry.registerProcess(
            renderingService,
            CoreConstants.RENDERING_SERVICE_PATH,
            CoreConstants.SYSTEM_PATH,
            registry
        );
        
        return registry.startProcess(servicePath)
            .thenApply(v -> renderingService);
    }


    private CompletableFuture<Void> attachLocalTerminal() {
        if (isHandleAttached()) {
            Log.logMsg("[SystemApplication] Handle already attached", LOG_LEVEL);
            return CompletableFuture.completedFuture(null);
        }

        Log.logMsg("[SystemApplication] Attaching local terminal", LOG_LEVEL);
        TerminalContainerHandle handle = TerminalContainerHandle.builder(
                CoreConstants.SYSTEM_CONTAINER_NAME, 
                CoreConstants.RENDERING_SERVICE_PATH, 
                ConsoleRenderer.DEFAULT_RENDERER_ID
            ).build();

        ContextPath terminalPath = registerChildAt(handle, CoreConstants.SYSTEM_CONTAINER_HANDLE_PATH);
        Log.logMsg("[SystemApplication] registered handle at: " + terminalPath, LOG_LEVEL);
        return startProcess(terminalPath)
            .thenCompose((v) -> {
                Log.logMsg("[SystemApplication] handle started", LOG_LEVEL);
                return setHandle(handle);
            });
    }


    // ===== STATE TRANSITIONS =====
    
    protected void setupStateTransitions() {
        Log.logMsg("[SystemApplication] setupStateTransitions", LOG_LEVEL);
        stateMachine.onStateAdded(ATTACHED, (old, now, bit) -> {
            stateMachine.removeState(DETACHED);
        });
        
        stateMachine.onStateRemoved(ATTACHED, (old, now, bit) -> {
            stateMachine.addState(DETACHED);
        });

        stateMachine.onStateAdded(INITIALIZED, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] INITIALIZED — checking system state", LOG_LEVEL);
            stateMachine.addState(CHECKING_SETTINGS);
        });

        stateMachine.onStateAdded(SETUP_NEEDED, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] SETUP_NEEDED", LOG_LEVEL);
            syncScaffoldingToState();
        });

        stateMachine.onStateAdded(CHECKING_SETTINGS, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] CHECKING_SETTINGS", LOG_LEVEL);
            stateMachine.removeState(CHECKING_SETTINGS);

            // Recovery mode is set from BootstrapConfig — highest priority check
            if (isInRecoveryMode) {
                Log.logMsg("[SystemApplication] Recovery mode active: " + recoveryReason, LOG_LEVEL);
                stateMachine.addState(FAILED_SETTINGS);
                return;
            }

            // syscfg.dat absent → user has never completed setup
            if (!SettingsData.isSystemConfigData()) {
                Log.logMsg("[SystemApplication] No system config — FIRST_RUN", LOG_LEVEL);
                stateMachine.addState(FIRST_RUN);
                return;
            }

            // syscfg.dat present but settings.dat absent → keyboard selected, no password yet
            if (!SettingsData.isSettingsData()) {
                Log.logMsg("[SystemApplication] System config exists, no password — SETUP_NEEDED", LOG_LEVEL);
                stateMachine.addState(SETUP_NEEDED);
                return;
            }

            // Both files exist — load settings map to check for interrupted password change
            SettingsData.loadSettingsMap(ioExecutor)
                .thenAccept(map -> {
                    boolean hasOldKey = map.containsKey(SettingsData.OLD_BCRYPT_KEY);
                    if (hasOldKey) {
                        Log.logMsg("[SystemApplication] Incomplete password change detected — FAILED_SETTINGS", LOG_LEVEL);
                        stateMachine.addState(FAILED_SETTINGS);
                    } else {
                        Log.logMsg("[SystemApplication] Settings valid — AUTHENTICATING", LOG_LEVEL);
                        stateMachine.addState(AUTHENTICATING);
                    }
                })
                .exceptionally(ex -> {
                    Log.logError("[SystemApplication] Settings load failed", ex);
                    stateMachine.addState(FAILED_SETTINGS);
                    return null;
                });
        });
        
        stateMachine.onStateAdded(AUTHENTICATED, (old, now, bit) -> {
            cancelAuthTimeout();
            stateMachine.removeState(LOCKED);
            stateMachine.removeState(AUTHENTICATING);
            stateMachine.removeState(AUTH_TIMEOUT);
            syncScaffoldingToState();
        });
        
        stateMachine.onStateAdded(LOCKED, (old, now, bit) -> {
            stateMachine.removeState(AUTHENTICATED);
            syncScaffoldingToState();
        });
        
        stateMachine.onStateAdded(FIRST_RUN, (old, now, bit) -> {
            syncScaffoldingToState();
        });
        
        stateMachine.onStateAdded(AUTHENTICATING, (old, now, bit) -> {
            startAuthTimeout();
            syncScaffoldingToState();
        });

        stateMachine.onStateRemoved(AUTHENTICATING, (old, now, bit) -> {
            cancelAuthTimeout();
        });

        stateMachine.onStateAdded(FAILED_SETTINGS, (old, now, bit) -> {
            syncScaffoldingToState();
        });

        stateMachine.onStateAdded(AUTH_TIMEOUT, (old, now, bit) -> {
            syncScaffoldingToState();
        });

        stateMachine.onStateAdded(SHUTDOWN_REQUESTED, (old,now,bit)->{
            shutdown();
        });

        
        stateMachine.addState(DETACHED);
    }
    
  

    protected void registerSceneFactories() {
    
    }

    private volatile TerminalContainerHandle newHandle = null;
    
    protected CompletableFuture<Void> setHandle(TerminalContainerHandle handle) {
        if (containerHandle == handle) return CompletableFuture.completedFuture(null);
        newHandle = handle;
        return detachHandle()
            .thenCompose(v->{
                TerminalContainerHandle attach = newHandle;
                newHandle = null;
                if(attach == null || attach == containerHandle){
                    Log.logMsg("[SystemApplication] no handle to attach", LOG_LEVEL);
                    return CompletableFuture.completedFuture(null);
                }else{
                    return attachHandle(attach);
                }
            });
        
    }

    

    private CompletableFuture<Void> detachHandle(){
        if(deattachHandleFuture != null) return deattachHandleFuture;
        deattachHandleFuture = new CompletableFuture<>();
    
        if(!uiExecutor.isCurrentThread()){
           uiExecutor.runLater(this::detachHandleInternal);
        }else{
            detachHandleInternal();
        }

        return deattachHandleFuture;
    }

    private void detachHandleInternal(){
        if(containerHandle != null){
            TerminalContainerHandle oldHandle = containerHandle;
            containerHandle = null;
            passwordService.onHandleDetached()         
                .thenCompose(v -> oldHandle.close())
                .handle((v, ex) -> {
                    if (ex != null) {
                        Log.logError("[SystemApplication] Handle detached with error", ex);
                    }
                    deattachHandleFuture.complete(null);
                    stateMachine.removeState(ATTACHED);
                    return null;
                });
        
        }else{
            if(stateMachine.hasState(ATTACHED)){
                stateMachine.removeState(ATTACHED); 
            }
            deattachHandleFuture.complete(null);
        }
    }

    private CompletableFuture<Void> attachHandle(TerminalContainerHandle handle) {
        return uiExecutor.execute(() -> {
            Log.logMsg("[SystemApplication] handle attached:" + handle.getName(), LOG_LEVEL);
            this.containerHandle = handle;
            deattachHandleFuture = null;
        })
        .thenCompose(v -> passwordService.onHandleAttached(handle))  // now returns future
        .thenAccept(v -> {
            // rootScene setup and containerHandle.setRenderable(...) remain here unchanged
            if (rootScene == null) {
                Log.logMsg("[SystemApplication] creating root scene", LOG_LEVEL);
                rootScene = new ApplicationRootScene(this);
                registerSceneFactories();
            }
           
            containerHandle.setRenderable(rootScene, (ctx ->{
                TerminalRectangle newRegion = ctx.getRequestedRegion();

                  Log.logMsg("[SystemApplication] root scene callback, updating"
                    +"\n\tsize:" + newRegion, LOG_LEVEL);
                return TerminalLayoutData.getBuilder().setBounds(newRegion).build();
            }));
            syncScaffoldingToState();
        });
    }

    /*
     */


    // ===== BOOTSTRAP COMPLETION =====

    public CompletableFuture<Void> completeBootstrap(NoteBytes selectedKeyboardId) {
        this.claimedKeyboardId = selectedKeyboardId;

        return saveBootstrapConfig()
            .thenCompose(v -> passwordService.onKeyboardIdChanged(selectedKeyboardId))
            .thenRun(() -> {
                Log.logMsg("[SystemApplication] Bootstrap complete", LOG_LEVEL);
                stateMachine.removeState(SETUP_NEEDED);
                stateMachine.addState(SETUP_COMPLETE);
                stateMachine.addState(CHECKING_SETTINGS);
            });
    }

    private CompletableFuture<Void> saveBootstrapConfig(){
        return BootstrapConfig.save(
            this.ioExecutor,
            this.claimedKeyboardId,
            this.ioDaemonManager.getIODaemonSocketPath(),
            this.isInRecoveryMode,
            this.recoveryReason, 
            this.startDetached
        );
    }

    
    // ===== AUTHENTICATION =====
    
    private void startAuthTimeout() {
        cancelAuthTimeout();
        
        authTimeoutFuture = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(AUTH_TIMEOUT_SECONDS);
                if (stateMachine.hasState(AUTHENTICATING)) {
                    stateMachine.removeState(AUTHENTICATING);
                    stateMachine.addState(AUTH_TIMEOUT);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, VirtualExecutors.getVirtualExecutor());
    }
    
    private void cancelAuthTimeout() {
        if (authTimeoutFuture != null) {
            authTimeoutFuture.cancel(true);
            authTimeoutFuture = null;
        }
    }
    
    public CompletableFuture<Void> createNewSystem(NoteBytesEphemeral password) {
        return SettingsData.createSettings(ioExecutor, password)
            .thenAccept(settingsData -> {
                password.close();
                
                RuntimeAccess access = new RuntimeAccess();
                SystemRuntime runtime = new SystemRuntime(settingsData, registry, access);
                
                this.systemRuntime = runtime;
                this.systemAccess = access;
                
                stateMachine.removeState(FIRST_RUN);
                stateMachine.removeState(AUTHENTICATING);
                cancelAuthTimeout();
                stateMachine.addState(AUTHENTICATED);
            });
    }
    
    public CompletableFuture<Boolean> authenticate(NoteBytesEphemeral password) {
        if (systemAccess != null) {
            return systemAccess.verifyPassword(password)
                .thenApply(valid -> {
                    if (valid) {
                        stateMachine.removeState(AUTHENTICATING);
                        if (!stateMachine.hasState(AUTHENTICATED)) {
                            stateMachine.addState(AUTHENTICATED);
                        }
                    }
                    return valid;
                });
        } else {
            return SettingsData.loadSettingsMap(ioExecutor)
                .thenCompose(settingsMap ->
                    SettingsData.verifyPassword(ioExecutor, password, settingsMap)
                        .thenCompose(valid -> {
                            if (valid) {
                                return SettingsData.loadSettingsData(ioExecutor, password, settingsMap)
                                    .thenApply(settingsData -> {
                                        RuntimeAccess access = new RuntimeAccess();
                                        SystemRuntime runtime = new SystemRuntime(
                                            settingsData, registry, access);
                                        
                                        this.systemRuntime = runtime;
                                        this.systemAccess = access;
                                        
                                        stateMachine.removeState(AUTHENTICATING);
                                        stateMachine.addState(AUTHENTICATED);
                                        return true;
                                    });
                            }
                            return CompletableFuture.completedFuture(false);
                        })
                );
        }
    }
    
    public CompletableFuture<Void> lock() {
        Log.logMsg("[SystemApplication] Locking system", LOG_LEVEL);
        
        if (isAuthenticated()) {
            stateMachine.addState(LOCKED);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ===== RECOVERY MODE =====
    
    CompletableFuture<Void> enterRecoveryMode(String reason) {
        this.isInRecoveryMode = true;
        this.recoveryReason = reason;
        
        Log.logMsg("[SystemApplication] Entering recovery mode: " + reason, LOG_LEVEL);
        
        return saveBootstrapConfig();
    }
    
    CompletableFuture<Void> exitRecoveryMode() {
        this.isInRecoveryMode = false;
        this.recoveryReason = null;
        
        Log.logMsg("[SystemApplication] Exiting recovery mode", LOG_LEVEL);
        
        return saveBootstrapConfig();
    }
    
    CompletableFuture<Void> recoverSystem(SettingsData settingsData) {
        if (!stateMachine.hasState(FAILED_SETTINGS)) {
            throw new SecurityException("Recovery only allowed in FAILED_SETTINGS state");
        }
        
        RuntimeAccess access = new RuntimeAccess();
        SystemRuntime runtime = new SystemRuntime(settingsData, registry, access);
        
        this.systemRuntime = runtime;
        this.systemAccess = access;
        
        stateMachine.removeState(FAILED_SETTINGS);
        stateMachine.addState(AUTHENTICATED);
        
        return CompletableFuture.completedFuture(null);
    }
    
    
    /**
     * Show a scaffolding screen (pre-process bootstrap UI)
     * These screens are shown BEFORE process infrastructure is available
     * 
     * Scaffolding screens:
     * - Cannot be reused (must create fresh instances)
     * - Don't participate in process stack
     * - Used for: LOCKED, AUTHENTICATING, FIRST_RUN, SETUP_NEEDED
     */
    private void showScaffoldingScreen(String screenId) {
        if (rootScene == null) {
            Log.logError("[SystemApplication] Cannot show scaffolding - no root scene");
            return;
        }
        Log.logMsg("[SystemApplication.showScaffoldingScreen]", LOG_LEVEL);
        // Create fresh instance (screens cannot be reused)
        SystemUIInterface screen = switch (screenId) {
            case "locked" -> new LockedScreen(this);
            case "login" -> new LoginScreen(this);
            case "setup" -> new SystemSetupWizardScreen(this);
            default -> {
                Log.logError("[SystemApplication] Unknown scaffolding screen: " + screenId);
                yield null;
            }
        };
        
        if (screen != null) {
            screen.setOnDisposed(disposed -> {
                // ApplicationRootScene already nulls currentScaffolding during removeOldScaffolding(),
                // but this closes the contract so external observers (e.g. diagnostics, future
                // extension) get notification that the scaffolding screen has cleaned up.
                Log.logMsg("[SystemApplication] Scaffolding disposed: " + screenId, LOG_LEVEL);
            });
            rootScene.showScaffolding(screen);
        }
    }


    /**
     * Check if currently showing scaffolding screen
     */
    public boolean isShowingScaffolding() {
        return rootScene != null && rootScene.isScaffoldingActive();
    }

    /**
     * Check if currently in process mode
     */
    public boolean isInProcessMode() {
        return rootScene != null && !rootScene.isScaffoldingActive() && stateMachine.hasState(AUTHENTICATED);
    }

    // ===== STATE SYNC =====
    
    /**
     * Sync UI to current state machine state
     * 
     * TWO MODES:
     * 1. SCAFFOLDING MODE: Pre-authentication screens (locked, login, setup)
     *    - Direct UI replacement (not process stack)
     *    - Fresh screen instances on each show
     *    
     * 2. PROCESS MODE: Post-authentication screens (main menu, etc.)
     *    - Process stack navigation
     *    - Full process lifecycle
     */
    private void syncScaffoldingToState() {
        if (rootScene == null){ 
            Log.logMsg("[SystemApplication] syncScaffoldingToState - rootScene null canceling", LOG_LEVEL);    
            return;
        }
        Log.logMsg("[SystemApplication.syncScaffoldingToState]", LOG_LEVEL);
        // ===== SCAFFOLDING STATES (Pre-Process) =====
        
        // Setup overlay takes priority over everything
        if (stateMachine.hasState(SETUP_NEEDED) || stateMachine.hasState(FIRST_RUN)) {
            showScaffoldingScreen("setup");
            return;
        }
        
        // Lock screen - system is locked
        if (stateMachine.hasState(LOCKED)) {
            showScaffoldingScreen("locked");
            return;
        }
        
        // Login screen - authenticating user
        if (stateMachine.hasState(AUTHENTICATING)) {
            showScaffoldingScreen("login");
            return;
        }
        
        // Auth timeout - show timeout message (could be scaffolding or process)
        if (stateMachine.hasState(AUTH_TIMEOUT)) {
            showScaffoldingScreen("auth-timeout");
            return;
        }
        
        // ===== PROCESS STATES (Post-Process) =====
        
        // Once authenticated, hide scaffolding
        if (stateMachine.hasState(AUTHENTICATED)) {
            rootScene.clearScaffolding();
            return;
        }
        
        // Settings recovery - process-based recovery UI
        if (stateMachine.hasState(FAILED_SETTINGS)) {
            
           // "settings-recovery"
             
            return;
        }
    }



    
    public void requestShutdown() {
        stateMachine.addState(SHUTDOWN_REQUESTED);
    }
    
    // ===== QUERIES =====
    
    public boolean isAuthenticated() {
        return systemAccess != null;
    }
    
    public boolean isLocked() {
        return stateMachine.hasState(LOCKED);
    }
    
    public boolean isInRecoveryMode() {
        return isInRecoveryMode;
    }
    
    public String getRecoveryReason() {
        return recoveryReason;
    }
    
    public boolean isHandleAttached() {
        return stateMachine.hasState(ATTACHED);
    }
    
    public TerminalContainerHandle getContainerHandle() {
        if (containerHandle == null) {
            throw new IllegalStateException("No handle attached");
        }
        return containerHandle;
    }
    
    public ApplicationRootScene getRootScene() {
        return rootScene;
    }
    
    public String getId() {
        return id;
    }
    
    public ConcurrentBitFlagStateMachine getStateMachine() {
        return stateMachine;
    }
    
    public SerializedVirtualExecutor getUiExecutor() {
        return uiExecutor;
    }
    
    public SerializedVirtualExecutor getIoExecutor() {
        return ioExecutor;
    }
    
    RuntimeAccess getSystemAccess() {
        return systemAccess;
    }
    
    public RenderingService getRenderingService() {
        return renderingService;
    }
    
    public FlowProcessService getProcessService() {
        return processService;
    }

    public IODaemonManager getIoDaemonManager() {
        return ioDaemonManager;
    }
    

    public NoteBytes getClaimedKeyboardId() {
        return claimedKeyboardId;
    }

    public String getIODaemonSocketPath() {
        return ioDaemonManager.getIODaemonSocketPath();
    }
    
    boolean allowRemoteAccess() {
        return allowRemoteAccess;
    }
    
    SystemRuntime getSystemRuntime() {
        return systemRuntime;
    }

    public PasswordService getPasswordService() {
        return passwordService;
    }
    
    ConsoleRenderer getUIRenderer() {
        return uiRenderer;
    }
    
    boolean isStartDetached() {
        return startDetached;
    }
    
    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        throw new UnsupportedOperationException("handleStreamChannel");
    }
    
    public void shutdown() {
        if (shutdownInProgress) return;
        shutdownInProgress = true;
        cancelAuthTimeout();
        stateMachine.addState(SHUTTING_DOWN);
        
        Log.logMsg("[SystemApplication] Shutdown initiated...", LOG_LEVEL);

        shutdownRuntime()
            .thenCompose(v -> detachHandle().orTimeout(3, TimeUnit.SECONDS))
            .handle((v, ex) -> {
                if (ex != null) {
                    Log.logError("[SystemApplication] Handle detach failed during shutdown", ex);
                }
                return null;
            })
            .whenComplete((v, ex) -> {
                try {
                    processService.shutdown();
                } catch (Exception shutdownEx) {
                    Log.logError("[SystemApplication] Process service shutdown failed", shutdownEx);
                    if (ex == null) {
                        ex = shutdownEx;
                    }
                }

                TerminalInitializer.shutdown(uiRenderer);

                if (ex != null) {
                    Log.logError("[SystemApplication] Shutdown finalization failed", ex);
                    shutdownFuture.completeExceptionally(ex);
                    return;
                }

                stateMachine.addState(SHUTDOWN_READY);
                shutdownFuture.complete(null);
            });
    }

    private CompletableFuture<Void> shutdownRuntime() {
        if (systemRuntime == null) {
            return CompletableFuture.completedFuture(null);
        }

        return systemRuntime.shutdown()
            .orTimeout(5, TimeUnit.SECONDS)
            .handle((v, ex) -> {
                if (ex != null) {
                    Log.logError("[SystemApplication] Runtime shutdown failed", ex);
                }
                return null;
            });
    }
}
