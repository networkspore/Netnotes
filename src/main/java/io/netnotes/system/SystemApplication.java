package io.netnotes.system;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.daemon.ClientSession;
import io.netnotes.engine.io.daemon.IODaemonManager;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.FlowProcessService;
import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.engine.io.process.StreamChannel;

import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.renderer.ConsoleUIRenderer;
import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.TerminalRectangle;
import io.netnotes.terminal.layout.TerminalLayoutData;
import io.netnotes.engine.state.ConcurrentBitFlagStateMachine;
import io.netnotes.engine.ui.UIRenderer;
import io.netnotes.engine.ui.containers.Container;
import io.netnotes.engine.ui.containers.RenderingService;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.engine.utils.virtualExecutors.SerializedVirtualExecutor;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

/**
 * SystemApplication - Process-based terminal system with authentication
 * 
 * Lifecycle:
 * 1. Bootstrap: Create infrastructure (RenderingService, ProcessService)
 * 2. Initialize: Load config, check settings
 * 3. Authenticate: First run / login / unlock
 * 4. Runtime: Manage processes, handle detachment
 */
public class SystemApplication extends FlowProcess {
   
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
    
    private final ConsoleUIRenderer uiRenderer;
    private final RenderingService renderingService;
    private final FlowProcessService processService;
    private final IODaemonManager ioDaemonManager;

    // ===== UI STATE =====
    protected TerminalContainerHandle containerHandle;
    private ApplicationRootScene rootScene;

    
    // ===== AUTHENTICATION =====
    private PasswordKeyboardManager passwordKeyboardManager = null;
    private String claimedKeyboardId;
    private SystemRuntime systemRuntime;
    private RuntimeAccess systemAccess;
    private CompletableFuture<Void> authTimeoutFuture;

    // ===== LIFECYCLE =====
    private volatile boolean running = false;
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private volatile boolean shutdownInProgress = false;
    
    // ===== MODE FLAGS =====
    private boolean daemonMode = false;
    private boolean allowRemoteAccess = false;
    private boolean isInRecoveryMode = false;
    private String recoveryReason;
    
    private static final long AUTH_TIMEOUT_SECONDS = 30;
    public static final String DEFAULT_IO_DAEMON_SOCKET_PATH = "/var/run/io-daemon.sock";
    public static final String PASSWORD_KEYBOARD_MANAGER_ID = "password-keyboard";
    public static final String RESOURCE_IO_EXECUTOR = "io-executor";
    
    // ===== CONFIG KEYS =====
    private static class ConfigKeys {
        public static final NoteBytesReadOnly CLAIMED_KEYBOARD_ID = 
            new NoteBytesReadOnly("claimedKeyboardId");
        public static final NoteBytesReadOnly IO_DAEMON_SOCKET_PATH = 
            new NoteBytesReadOnly("ioDaemonSocketPath");
        public static final NoteBytesReadOnly RECOVERY_MODE = 
            new NoteBytesReadOnly("recoveryMode");
        public static final NoteBytesReadOnly RECOVERY_REASON = 
            new NoteBytesReadOnly("recoveryReason");
    }
    
    // ===== CONSTRUCTION =====
    
    private SystemApplication(
        ConsoleUIRenderer uiRenderer,
        RenderingService renderingService,
        FlowProcessService processService,
        ProcessRegistryInterface registry,
        boolean daemonMode
    ) {
        super("SystemApplication", ProcessType.BIDIRECTIONAL);
        this.id = NoteUUID.createSafeUUID128();
        this.stateMachine = new ConcurrentBitFlagStateMachine("SystemApplication:" + id);
        this.stateMachine.setSerialExecutor(uiExecutor);
        this.uiRenderer = uiRenderer;
        this.renderingService = renderingService;
        this.processService = processService;
        this.daemonMode = daemonMode;
        this.ioDaemonManager = new IODaemonManager(
            CoreConstants.IO_DAEMON, 
            CoreConstants.IO_DAEMON_PATH,
            registry, 
            DEFAULT_IO_DAEMON_SOCKET_PATH
        );
        this.uiRenderer.setOnCtrlC(this::handleCtrlC);
        setupStateTransitions();
        setupShutdownHook();
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shutdownInProgress) {
            }
        }, "shutdown-hook"));
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[SystemApplication] Uncaught exception in " + 
                thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });
    }

    private void handleCtrlC(){
        requestShutdown();
    }

    public CompletableFuture<SystemApplication> start(){
        Log.logMsg("[SystemApplication] Bootstrap starting...");
        
        return TerminalInitializer.createAndInitialize()
            .thenCompose(renderer -> bootstrapInfrastructure(renderer, daemonMode))
            .whenComplete((app,ex) -> {
                if(ex != null){
                    Log.logError("[SystemApplication]", "Startup failed", ex);
                    ex.printStackTrace();
                    System.exit(1);
                }else{
                    app.running = true;
                    Log.logMsg("[SystemApplication] Bootstrap complete");
                }
            });
           
            

    }

    private static CompletableFuture<SystemApplication> bootstrapInfrastructure(
        ConsoleUIRenderer uiRenderer,
        boolean daemonMode
    ) {
        FlowProcessService processService = new FlowProcessService();
        ProcessRegistryInterface registry = processService.getRegistryInterface();
        
        return uiRenderer.initialize()
            .thenCompose(v -> startRenderingService(uiRenderer, registry))
            .thenCompose(renderingService -> {
                SystemApplication app = new SystemApplication(
                    uiRenderer,
                    renderingService,
                    processService,
                    registry,
                    daemonMode
                );
                
                registry.registerProcess(app, CoreConstants.SYSTEM_PATH, null, registry);
                
                return registry.startProcess(CoreConstants.SYSTEM_PATH)
                    .thenApply(v2 -> {
                        registry.connect(CoreConstants.SYSTEM_PATH, CoreConstants.RENDERING_SERVICE_PATH);
                        registry.connect(CoreConstants.RENDERING_SERVICE_PATH, CoreConstants.SYSTEM_PATH);
                        
                        Log.logMsg("[SystemApplication] Infrastructure operational");
                        return app;
                    });
            });
    }

    
    // ===== BOOTSTRAP =====
    
    
    private static CompletableFuture<RenderingService> startRenderingService(
        ConsoleUIRenderer uiRenderer,
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
    
    // ===== INITIALIZATION =====
    
    @Override
    public CompletableFuture<Void> run() {
        stateMachine.addState(INITIALIZING);
        if (needsBootstrap()) {
            stateMachine.removeState(INITIALIZING);
            stateMachine.addState(SETUP_NEEDED);
            return attachLocalTerminal();
        }
        
        return loadBootstrapConfig()
            .thenCompose(v -> {
                stateMachine.removeState(INITIALIZING);
                stateMachine.addState(INITIALIZED);
                return decideHandleAttachment();
            })
            .thenCompose(v -> setupIODaemon())
            .whenComplete((v, ex) -> {
                if (ex != null) {
                    Log.logError("[SystemApplication] Init failed: " + ex.getMessage());
                    stateMachine.addState(ERROR);
                }
            });
    }
    
    private boolean needsBootstrap() {
        return !SettingsData.isSystemConfigData();
    }
    
    private CompletableFuture<Void> loadBootstrapConfig() {
        if (!SettingsData.isSystemConfigData()) {
            return CompletableFuture.failedFuture(new IOException("Config does not exist"));
        }
        
        return SettingsData.loadSystemConfig(ioExecutor)
            .thenCompose(map -> uiExecutor.submit(() -> {
                NoteBytes keyboardBytes = map.get(ConfigKeys.CLAIMED_KEYBOARD_ID);
                NoteBytes socketBytes = map.get(ConfigKeys.IO_DAEMON_SOCKET_PATH);
                NoteBytes recoveryBytes = map.get(ConfigKeys.RECOVERY_MODE);
                NoteBytes reasonBytes = map.get(ConfigKeys.RECOVERY_REASON);
                
                claimedKeyboardId = keyboardBytes != null
                    ? keyboardBytes.getAsString()
                    : null;
                
                String ioDaemonSocketPath = socketBytes != null
                    ? socketBytes.getAsString()
                    : DEFAULT_IO_DAEMON_SOCKET_PATH;
                
                isInRecoveryMode = recoveryBytes != null
                    ? recoveryBytes.getAsBoolean()
                    : false;
                recoveryReason = reasonBytes != null
                    ? reasonBytes.getAsString()
                    : null;
                
                ioDaemonManager.setIODaemonSocketPath(ioDaemonSocketPath);
                
                Log.logMsg("[SystemApplication] Bootstrap loaded: passwordKeyboard=" + claimedKeyboardId);
                return null;
            }));
    }
    
    private CompletableFuture<Void> saveBootstrapConfig() {
        return uiExecutor.submit(() -> {
            NoteBytesMap map = new NoteBytesMap();
            
            String keyboardId = this.claimedKeyboardId;
            if (keyboardId != null) {
                map.put(ConfigKeys.CLAIMED_KEYBOARD_ID, keyboardId);
            }
            map.put(ConfigKeys.IO_DAEMON_SOCKET_PATH, ioDaemonManager.getIODaemonSocketPath());
            
            if (isInRecoveryMode) {
                map.put(ConfigKeys.RECOVERY_MODE, isInRecoveryMode);
                if (recoveryReason != null) {
                    map.put(ConfigKeys.RECOVERY_REASON, recoveryReason);
                }
            }
            return map;
        }).thenCompose(map -> SettingsData.saveSystemConfig(ioExecutor, map))
            .thenRun(() -> Log.logMsg("[SystemApplication] Bootstrap saved (recovery=" + isInRecoveryMode + ")"));
    }
    
    private CompletableFuture<Void> setupIODaemon() {
        if (!daemonMode) {
            return CompletableFuture.completedFuture(null);
        }
        
        stateMachine.addState(INFRASTRUCTURE_INITIALIZING);
        return ioDaemonManager.ensureAvailable()
            .thenAccept(v -> {
                stateMachine.removeState(INFRASTRUCTURE_INITIALIZING);
                stateMachine.addState(INFRASTRUCTURE_READY);
            });
    }

    private CompletableFuture<Void> attachLocalTerminal() {
        if (isHandleAttached()) {
            Log.logMsg("[SystemApplication] Handle already attached");
            return CompletableFuture.completedFuture(null);
        }

        Log.logMsg("[SystemApplication] Attaching local terminal");
        TerminalContainerHandle handle = TerminalContainerHandle.builder(
                CoreConstants.SYSTEM_CONTAINER_NAME, 
                CoreConstants.RENDERING_SERVICE_PATH, 
                CoreConstants.TERMINAL_RENDERER_ID
            ).build();

        ContextPath terminalPath = registerChildAt(handle, CoreConstants.SYSTEM_CONTAINER_HANDLE_PATH);
        return startProcess(terminalPath)
            .thenRun(() -> setHandle(handle));
    }

    private CompletableFuture<Void> decideHandleAttachment() {
        if (daemonMode) {
            Log.logMsg("[SystemApplication] Daemon mode - staying headless");
            return CompletableFuture.completedFuture(null);
        }
        return attachLocalTerminal();
    }
    
    // ===== STATE TRANSITIONS =====
    
    protected void setupStateTransitions() {
        stateMachine.onStateAdded(ATTACHED, (old, now, bit) -> {
            stateMachine.removeState(DETACHED);
            onHandleAttached();
        });
        
        stateMachine.onStateRemoved(ATTACHED, (old, now, bit) -> {
            stateMachine.addState(DETACHED);
            onHandleDetached();
        });

        stateMachine.onStateAdded(INITIALIZED, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] INITIALIZED");
            stateMachine.addState(SETUP_COMPLETE);
            stateMachine.addState(CHECKING_SETTINGS);
        });

        stateMachine.onStateAdded(SETUP_NEEDED, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] SETUP_NEEDED");
            syncScaffoldingToState();
        });

        stateMachine.onStateAdded(CHECKING_SETTINGS, (old, now, bit) -> {
            Log.logMsg("[SystemApplication] CHECKING_SETTINGS");

            if (isInRecoveryMode) {
                Log.logMsg("[SystemApplication] Recovery mode active: " + recoveryReason);
                stateMachine.removeState(CHECKING_SETTINGS);
                stateMachine.addState(FAILED_SETTINGS);
                return;
            }

            checkSettingsExist()
                .thenAccept(exists -> {
                    stateMachine.removeState(CHECKING_SETTINGS);
                    if (exists) {
                        stateMachine.addState(AUTHENTICATING);
                    } else {
                        if (!SettingsData.isIdDataFile()) {
                            stateMachine.addState(FIRST_RUN);
                        } else {
                            stateMachine.addState(FAILED_SETTINGS);
                        }
                    }
                })
                .exceptionally(ex -> {
                    Log.logError("[SystemApplication] Settings check failed", ex);
                    stateMachine.removeState(CHECKING_SETTINGS);
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
            if (containerHandle != null && claimedKeyboardId != null) {
                setupClaimedKeyboard(containerHandle);
            }
            syncScaffoldingToState();
        });

        stateMachine.onStateRemoved(AUTHENTICATING, (old, now, bit) -> {
            cancelAuthTimeout();
            if (passwordKeyboardManager != null) {
                passwordKeyboardManager.setExclusiveMode(false)
                    .thenCompose(v -> passwordKeyboardManager.disable())
                    .exceptionally(ex -> {
                        Log.logError("[SystemApplication] Password keyboard release failed", ex);
                        return null;
                    });
            }
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
    
    protected void onHandleAttached() {
        Log.logMsg("[SystemApplication] Handle attached");
        
        if (rootScene == null) {
            rootScene = createRootScene();
            registerProcessFactories();
        }
        
        containerHandle.setRenderable(rootScene, (ctx ->{
            TerminalRectangle newRegion = ctx.getRequestedRegion();
            if(newRegion.getWidth() < 40){
                newRegion.setWidth(40);
            }
            if(newRegion.getHeight() < 40){
                newRegion.setHeight(40);
            }
            return TerminalLayoutData.getBuilder().setBounds(newRegion).build();
        }));
        if (claimedKeyboardId != null) {
            setupClaimedKeyboard(containerHandle);
        }
        syncScaffoldingToState();
    }
    
    protected void onHandleDetached() {
        Log.logMsg("[SystemApplication] Handle detached");
        TerminalContainerHandle handle = containerHandle;
        if (passwordKeyboardManager != null && handle != null) {
            handle.removeDeviceManager(PASSWORD_KEYBOARD_MANAGER_ID)
                .whenComplete((v, ex) -> {
                    passwordKeyboardManager = null;
                    if (ex != null) {
                        Log.logError("[SystemApplication] Password keyboard cleanup failed", ex);
                    }
                });
        } else if (passwordKeyboardManager != null) {
            passwordKeyboardManager.disable()
                .whenComplete((v, ex) -> {
                    passwordKeyboardManager = null;
                    if (ex != null) {
                        Log.logError("[SystemApplication] Password keyboard cleanup failed", ex);
                    }
                });
        }
    }
    
    protected ApplicationRootScene createRootScene() {
        return new ApplicationRootScene("root", this);
    }
    
    protected void registerProcessFactories() {
    
    }

    private volatile TerminalContainerHandle newHandle = null;
    
    protected CompletableFuture<Void> setHandle(TerminalContainerHandle handle) {
        if (containerHandle == handle) return CompletableFuture.completedFuture(null);
        newHandle = handle;
        return detachHandle()
            .thenCompose(v->{
                TerminalContainerHandle attach = newHandle;
                newHandle = null;
                if(attach == null){
                    return CompletableFuture.completedFuture(null);
                }else{
                    return attachHandle(attach);
                }
            });
        
    }

    private CompletableFuture<Void> deattachHandleFuture = null;

    private CompletableFuture<Void> detachHandle(){
        if(deattachHandleFuture != null) return deattachHandleFuture;
        deattachHandleFuture = new CompletableFuture<>();
    
        uiExecutor.executeFireAndForget(()->{
            if(containerHandle != null){
                TerminalContainerHandle oldHandle = containerHandle;
                containerHandle = null;

                if (stateMachine.hasState(ATTACHED) && oldHandle != null) {
                    stateMachine.removeState(ATTACHED);
                    oldHandle.close()
                        .thenRun(()->deattachHandleFuture.complete(null));
                }else{
                    deattachHandleFuture.complete(null);
                }
            }else{
                deattachHandleFuture.complete(null);
            }
        });
        return deattachHandleFuture;
    }

    private CompletableFuture<Void> attachHandle(TerminalContainerHandle handle){
        return uiExecutor.execute(()->{
            this.containerHandle = handle;
            deattachHandleFuture = null;
            if (handle != null) {
                stateMachine.addState(ATTACHED);
            }
        });
    }


    // ===== BOOTSTRAP COMPLETION =====

    public CompletableFuture<Void> completeBootstrap(String selectedKeyboardId) {
        String previousKeyboardId = this.claimedKeyboardId;
        this.claimedKeyboardId = selectedKeyboardId;

        return saveBootstrapConfig()
            .thenCompose(v -> updatePasswordKeyboard(previousKeyboardId))
            .thenRun(() -> {
                Log.logMsg("[SystemApplication] Bootstrap complete");
                stateMachine.removeState(SETUP_NEEDED);
                stateMachine.addState(SETUP_COMPLETE);
                stateMachine.addState(CHECKING_SETTINGS);
            });
    }

    private CompletableFuture<Void> updatePasswordKeyboard(String previousKeyboardId) {
        TerminalContainerHandle handle = containerHandle;
        String currentKeyboardId = claimedKeyboardId;

        if (handle == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (currentKeyboardId == null) {
            return removePasswordKeyboard(handle);
        }

        if (previousKeyboardId == null || !previousKeyboardId.equals(currentKeyboardId)) {
            return removePasswordKeyboard(handle)
                .thenCompose(v -> setupClaimedKeyboard(handle));
        }

        if (passwordKeyboardManager == null) {
            return setupClaimedKeyboard(handle);
        }

        return CompletableFuture.completedFuture(null);
    }

    // ===== PASSWORD KEYBOARD MANAGEMENT =====

    private CompletableFuture<Void> setupClaimedKeyboard(TerminalContainerHandle handle) {
        String keyboardId = claimedKeyboardId;
        if (keyboardId == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (passwordKeyboardManager != null) {
            return CompletableFuture.completedFuture(null);
        }

        PasswordKeyboardManager keyboardManager = new PasswordKeyboardManager(
            keyboardId,
            ClientSession.Modes.PARSED.toString()
        );
        passwordKeyboardManager = keyboardManager;

        return handle.addDeviceManager(PASSWORD_KEYBOARD_MANAGER_ID, keyboardManager)
            .thenRun(() -> Log.logMsg("[SystemApplication] Password keyboard attached: " + keyboardId))
            .exceptionally(ex -> {
                Log.logError("[SystemApplication] Password keyboard setup failed", ex);
                passwordKeyboardManager = null;
                return null;
            });
    }

    private CompletableFuture<Void> removePasswordKeyboard(TerminalContainerHandle handle) {
        if (passwordKeyboardManager == null) {
            return CompletableFuture.completedFuture(null);
        }

        return handle.removeDeviceManager(PASSWORD_KEYBOARD_MANAGER_ID)
            .exceptionally(ex -> {
                Log.logError("[SystemApplication] Password keyboard removal failed", ex);
                return null;
            })
            .thenRun(() -> passwordKeyboardManager = null);
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
    
    private CompletableFuture<Boolean> checkSettingsExist() {
        if (SettingsData.isSettingsData()) {
            return SettingsData.loadSettingsMap(ioExecutor).thenApply(map -> map != null);
        }
        return CompletableFuture.completedFuture(false);
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
        Log.logMsg("[SystemApplication] Locking system");
        
        if (isAuthenticated()) {
            stateMachine.addState(LOCKED);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ===== RECOVERY MODE =====
    
    CompletableFuture<Void> enterRecoveryMode(String reason) {
        this.isInRecoveryMode = true;
        this.recoveryReason = reason;
        
        Log.logMsg("[SystemApplication] Entering recovery mode: " + reason);
        
        return saveBootstrapConfig();
    }
    
    CompletableFuture<Void> exitRecoveryMode() {
        this.isInRecoveryMode = false;
        this.recoveryReason = null;
        
        Log.logMsg("[SystemApplication] Exiting recovery mode");
        
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
        
        // Create fresh instance (screens cannot be reused)
        SystemUIInterface screen = switch (screenId) {
            case "locked" -> new LockedScreen(this);
            case "login" -> new LoginScreen(this);
            case "setup" -> new SystemSetupScreen(this);
            default -> {
                Log.logError("[SystemApplication] Unknown scaffolding screen: " + screenId);
                yield null;
            }
        };
        
        if (screen != null) {
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
        if (rootScene == null) return;
        
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
    

    public String getClaimedKeyboardId() {
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
    
    UIRenderer<?> getUIRenderer() {
        return uiRenderer;
    }
    
    boolean isDaemonMode() {
        return daemonMode;
    }
    
    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        throw new UnsupportedOperationException("handleStreamChannel");
    }
    
    public void shutdown() {
        if (shutdownInProgress) return;
        shutdownInProgress = true;
        running = false;
        
        Log.logMsg("[SystemApplication] Shutdown initiated...");
        
        detachHandle().orTimeout(3, TimeUnit.SECONDS)
            .thenCompose((v)->{
                return CompletableFuture.runAsync(()->{
                    TerminalContainerHandle handle =  containerHandle;
                    if(handle != null && !handle.getStateMachine().hasState(Container.STATE_DESTROYED)){
                        //Kills process
                        unregisterProcess(handle.getContextPath());
                    }
                    //Kills Process
                    unregisterProcess(contextPath);
                });
            })
            .orTimeout(1, TimeUnit.SECONDS)
            .whenComplete((v,ex)->TerminalInitializer.shutdown(uiRenderer))
            .thenCompose(v->renderingService.shutdown())
            .thenRun(()->{
                shutdownFuture.complete(null);
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        System.exit(0);
                    } catch (InterruptedException e) {
                        System.exit(0);
                    }
                }, "exit-thread").start();
            });
    }
}
