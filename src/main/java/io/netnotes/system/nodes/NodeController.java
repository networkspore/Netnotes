package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.RoutedPacket;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.NoteMessaging.RoutedMessageExecutor;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesArrayReadOnly;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.NoteSerializable;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.noteFiles.ScopedNoteFileInterface;
import io.netnotes.system.CoreConstants;
import io.netnotes.system.SignatureVerifier;
import io.netnotes.system.nodes.security.NodeSecurityPolicy;
import io.netnotes.system.nodes.security.PolicyManifest;
import io.netnotes.engine.utils.LoggingHelpers.Log;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.crypto.signers.Ed25519Signer;

/**
 * NodeController - 
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * CLEAN RESPONSIBILITIES:
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * 1. Receives fully-formed NodeLoadRequest (all decisions pre-made)
 * 2. Loads INode from package using NodeLoader
 * 3. Creates scoped interfaces (file + process) with security enforcement
 * 4. Initializes INode with interfaces
 * 5. Tracks running instances in NodeInstanceRegistry
 * 6. Handles unload requests
 * 
 * DOES NOT:
 * - Make namespace decisions (done at install)
 * - Make security decisions (done at install)
 * - Make inheritance decisions (done at install)
 * - Manage package installation (that's NodeManager's job)
 */
public class NodeController extends FlowProcess {
    
    private final NoteFileServiceInterface fileService;
    private final NodeLoader nodeLoader;
    private final NodeInstanceRegistry instanceRegistry;
    private InstallationRegistry installationRegistry;
    private InstallationExecutor installationExecutor;
    private final SignatureVerifier signatureVerifier;
    private final Map<NoteBytesReadOnly, RoutedMessageExecutor> m_executorMap = new ConcurrentHashMap<>();

    private final Set<Long> recentTimestamps = ConcurrentHashMap.newKeySet();
    private final long MAX_COMMAND_AGE_MS = 60_000; // 1 minute
 
    
    public NodeController(
        String name,
        NoteFileServiceInterface fileService,
        SignatureVerifier signatureVerifier
    ) {
        super(name, ProcessType.BIDIRECTIONAL);
        this.fileService = fileService;
        this.signatureVerifier = signatureVerifier;
        setupExecutorMap();
        this.nodeLoader = new NodeLoader(name + "-loader", registry, fileService);
        this.instanceRegistry = new NodeInstanceRegistry();
        this.installationExecutor = new InstallationExecutor(fileService);
        
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════
    
    private void setupExecutorMap() {
        m_executorMap.put(NodeConstants.LIST_INSTALLED, this::handleListInstalled);
        m_executorMap.put(NodeConstants.LIST_INSTANCES, this::handleListInstances);
        m_executorMap.put(NodeConstants.INSTALL_PACKAGE, this::handleInstallPackage);
        m_executorMap.put(NodeConstants.UNINSTALL_PACKAGE, this::handleUninstallPackage);
        m_executorMap.put(NodeConstants.LOAD_NODE, this::handleLoadNode);
        m_executorMap.put(NodeConstants.UNLOAD_INSTANCE, this::handleUnloadInstance);
        m_executorMap.put(NodeConstants.UPDATE_CONFIG, this::handleUpdateConfig);
    }

    @Override
    public CompletableFuture<Void> run() {
        return initialize()
            .thenCompose(v -> getCompletionFuture());
    }
    
    private CompletableFuture<Void> initialize() {
        Log.logMsg("[NodeController] Initializing at: " + contextPath);
        
        // Initialize installation registry
        return initializeInstallationRegistry()
            .thenRun(() -> {
                Log.logMsg("[NodeController] Initialization complete");
            });
    }

    private CompletableFuture<Void> initializeInstallationRegistry() {
        // Get registry file path
        ContextPath registryPath = CoreConstants.NODE_DATA_PATH
                .append("installation-registry");
        
        return fileService.getNoteFile(registryPath)
            .thenCompose(registryFile -> {
                this.installationRegistry = new InstallationRegistry(
                    "installation-registry",
                    registryFile
                );
                
                // Register as child process
                io.netnotes.engine.io.ContextPath regPath = registry.registerProcess(
                    installationRegistry,
                    contextPath.append("registry"),
                    contextPath,
                    registry
                );
                
                Log.logMsg("[NodeController] Registered InstallationRegistry at: " + regPath);
                
                // Start and initialize
                return registry.startProcess(regPath)
                    .thenCompose(v -> installationRegistry.initialize());
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LOAD NODE (Simplified)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Load a node from a fully-formed request
     * 
     * ALL DECISIONS ALREADY MADE:
     * - Process configuration (namespace, inheritance)
     * - Security policy (capabilities)
     * - Package metadata
     * 
     * NodeController just applies the configuration
     */
    public CompletableFuture<NodeInstance> loadNode(NodeLoadRequest request) {
        // Validate request
        List<String> errors = request.validate();
        if (!errors.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Invalid request: " + errors));
        }
        
        InstalledPackage pkg = request.getPackage();
        ProcessConfig processConfig = request.getProcessConfig();
 
        // Check if already loaded with this processId
        if (instanceRegistry.isLoaded(pkg.getPackageId(), processConfig.getProcessId())) {
            return CompletableFuture.failedFuture(
                new IllegalStateException(
                    "Package already loaded with processId: " + processConfig.getProcessId()));
        }
        
        Log.logMsg("[NodeController] Loading node:");
        Log.logMsg("  Package: " + pkg.getName() + " v" + pkg.getPackageId().getVersion());
        Log.logMsg("  ProcessId: " + processConfig.getProcessId());
        Log.logMsg("  Data root: " + processConfig.getDataRootPath());
        Log.logMsg("  Flow base: " + processConfig.getFlowBasePath());
        
        // Load INode from package
        return nodeLoader.loadNodeFromPackage(pkg)
            .thenCompose(inode -> initializeAndRegisterNode(pkg, inode));
    }
    
    /**
     * Initialize INode and register in runtime
     */
    private CompletableFuture<NodeInstance> initializeAndRegisterNode(
        InstalledPackage pkg,
        INode inode
    ) {
        // Create instance wrapper
        NodeInstance instance = new NodeInstance(pkg, inode);
        
        // Get configuration from package (already decided at install)
        ProcessConfig processConfig = pkg.getProcessConfig();
        NodeSecurityPolicy securityPolicy = pkg.getSecurityPolicy();
        
        // ===== CREATE SCOPED FILE INTERFACE =====
        // Scope: /data/nodes/{processId}
        NoteFileServiceInterface nodeFileInterface = new ScopedNoteFileInterface(
            fileService,
            processConfig.getDataRootPath()
        );
        
        // ===== CREATE SCOPED PROCESS INTERFACE WITH SECURITY =====
        // Scope: /system/nodes/{processId}
        ProcessRegistryInterface nodeProcessInterface = new NodeProcessInterface(
            registry,
            processConfig.getFlowBasePath(),
            securityPolicy
        );
        
        instance.setState(NodeState.INITIALIZING);
        
        // ===== INITIALIZE INODE =====
        return inode.initialize(nodeFileInterface, nodeProcessInterface)
            .thenApply(v -> {
                instance.setState(NodeState.RUNNING);
                
                // Register in instance registry
                instanceRegistry.register(instance);
                
                Log.logMsg("[NodeController] Node loaded successfully:");
                Log.logMsg("  Instance: " + instance.getInstanceId());
                Log.logMsg("  Package: " + pkg.getName());
                Log.logMsg("  ProcessId: " + processConfig.getProcessId());
                
                return instance;
            })
            .exceptionally(ex -> {
                Log.logError("[NodeController] Failed to initialize node: " + 
                    ex.getMessage());
                instance.setState(NodeState.CRASHED);
                throw new RuntimeException("Node initialization failed", ex);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UNLOAD NODE
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Unload a running instance
     */
    public CompletableFuture<Void> unloadInstance(InstanceId instanceId) {
        NodeInstance instance = instanceRegistry.getInstance(instanceId);
        
        if (instance == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Instance not found: " + instanceId));
        }
        
        Log.logMsg("[NodeController] Unloading instance: " + instanceId);
        
        instance.setState(NodeState.STOPPING);
        
        return instance.getINode().shutdown()
            .thenRun(() -> {
                // Unregister from instance registry
                instanceRegistry.unregister(instanceId);
                
                instance.setState(NodeState.STOPPED);
                
                Log.logMsg("[NodeController] Instance unloaded: " + instanceId);
            })
            .exceptionally(ex -> {
                Log.logError("[NodeController] Error unloading instance: " + 
                    instanceId + " - " + ex.getMessage());
                return null;
            });
    }
    
    /**
     * Unload all instances of a package
     */
    public CompletableFuture<Void> unloadPackage(PackageId packageId) {
        List<NodeInstance> instances = 
            instanceRegistry.getInstancesByPackage(packageId);
        
        if (instances.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        Log.logMsg("[NodeController] Unloading " + instances.size() + 
            " instances of package: " + packageId);
        
        List<CompletableFuture<Void>> futures = instances.stream()
            .map(inst -> unloadInstance(inst.getInstanceId()))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Unload all instances in a process namespace
     */
    public CompletableFuture<Void> unloadProcess(NoteBytesReadOnly processId) {
        List<NodeInstance> instances = 
            instanceRegistry.getInstancesByProcess(processId);
        
        if (instances.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        Log.logMsg("[NodeController] Unloading " + instances.size() + 
            " instances in process: " + processId);
        
        List<CompletableFuture<Void>> futures = instances.stream()
            .map(inst -> unloadInstance(inst.getInstanceId()))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get instance by ID
     */
    public NodeInstance getInstance(InstanceId instanceId) {
        return instanceRegistry.getInstance(instanceId);
    }
    
    /**
     * Get all instances of a package
     */
    public List<NodeInstance> getInstancesByPackage(PackageId packageId) {
        return instanceRegistry.getInstancesByPackage(packageId);
    }
    
    /**
     * Get all instances in a process namespace
     */
    public List<NodeInstance> getInstancesByProcess(NoteBytesReadOnly processId) {
        return instanceRegistry.getInstancesByProcess(processId);
    }
    
    /**
     * Get all running instances
     */
    public List<NodeInstance> getAllInstances() {
        return instanceRegistry.getAllInstances();
    }
    
    /**
     * Get running instances by state
     */
    public List<NodeInstance> getInstancesByState(NodeState state) {
        return getAllInstances().stream()
            .filter(inst -> inst.getState() == state)
            .toList();
    }
    
    /**
     * Check if package is loaded (any processId)
     */
    public boolean isPackageLoaded(PackageId packageId) {
        return instanceRegistry.isPackageLoaded(packageId);
    }
    
    /**
     * Check if specific package+process combination is loaded
     */
    public boolean isLoaded(PackageId packageId, NoteBytesReadOnly processId) {
        return instanceRegistry.isLoaded(packageId, processId);
    }


    /**
     * Install a package
     */
    public CompletableFuture<InstalledPackage> installPackage(InstallationRequest request) {
        Log.logMsg("[NodeController] Installing package: " + 
            request.getPackageInfo().getName());
        
        // Execute installation
        return installationExecutor.executeInstallation(request)
            .thenCompose(installedPackage -> {
                // Register in installation registry
                return installationRegistry.registerPackage(installedPackage)
                    .thenApply(v -> installedPackage);
            })
            .thenCompose(installedPackage -> {
                // If loadImmediately flag is set, load it now
                if (request.shouldLoadImmediately()) {
                    NodeLoadRequest loadRequest = new NodeLoadRequest(installedPackage);
                    return loadNode(loadRequest)
                        .thenApply(instance -> installedPackage);
                }
                return CompletableFuture.completedFuture(installedPackage);
            });
    }

    /**
     * Uninstall a package
     */
    public CompletableFuture<Void> uninstallPackage(PackageId packageId, AsyncNoteBytesWriter progress) {
        Log.logMsg("[NodeController] Uninstalling package: " + packageId);
        
        // Check if package has running instances
        List<NodeInstance> instances = getInstancesByPackage(packageId);
        if (!instances.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException(
                    "Cannot uninstall package with running instances. Stop " + 
                    instances.size() + " instance(s) first."));
        }
        
        // Get installed package info
        InstalledPackage pkg = installationRegistry.getPackage(packageId);
        if (pkg == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Package not found: " + packageId));
        }
        
        // Unregister from installation registry
        return installationRegistry.unregisterPackage(packageId)
            .thenCompose(v -> {
                // Delete package files from storage
                return deletePackageFiles(pkg, progress);
            })
            .thenRun(() -> {
                Log.logMsg("[NodeController] Package uninstalled: " + packageId);
            });
    }

    /**
     * Delete package files from storage
     */
    private CompletableFuture<Void> deletePackageFiles(InstalledPackage pkg, AsyncNoteBytesWriter progress) {
        ContextPath installPath = pkg.getInstallPath();
        
        // Get the NoteFile for the install path
        return fileService.deleteNoteFile(installPath, false, progress)
            .thenAccept((notePath) -> {
                Log.logMsg("[NodeController] Deleted package files at: " + installPath);
            })
            .exceptionally(ex -> {
                Log.logError("[NodeController] Failed to delete package files: " + 
                    ex.getMessage());
                // Don't fail the uninstall if file deletion fails
                return null;
            });
        
    }

    /**
     * Delete package data directory
     */
    public CompletableFuture<Void> deletePackageData(PackageId packageId, AsyncNoteBytesWriter progress) {
        InstalledPackage pkg = installationRegistry.getPackage(packageId);
        if (pkg == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Package not found: " + packageId));
        }
        
        ContextPath dataPath = pkg.getProcessConfig().getDataRootPath();
        
        Log.logMsg("[NodeController] Deleting package data at: " + dataPath);
        
        return fileService.deleteNoteFile(dataPath, false, progress)
            .thenRun(() -> {
                Log.logMsg("[NodeController] Package data deleted");
            })
            .exceptionally(ex -> {
                Log.logError("[NodeController] Failed to delete package data: " + 
                    ex.getMessage());
                // Log but don't fail
                return null;
            });
    }

    /**
     * Update package configuration
     */
    public CompletableFuture<Void> updatePackageConfiguration(
        PackageId packageId,
        io.netnotes.system.nodes.ProcessConfig newProcessConfig
    ) {
        Log.logMsg("[NodeController] Updating package configuration: " + packageId);
        
        // Check if package has running instances
        List<NodeInstance> instances = getInstancesByPackage(packageId);
        if (!instances.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException(
                    "Cannot update configuration with running instances. Stop " + 
                    instances.size() + " instance(s) first."));
        }
        
        // Get current package
        InstalledPackage currentPkg = installationRegistry.getPackage(packageId);
        if (currentPkg == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Package not found: " + packageId));
        }
        
        // Create updated package with new ProcessConfig
        InstalledPackage updatedPkg = new InstalledPackage(
            currentPkg.getPackageId(),
            currentPkg.getName(),
            currentPkg.getDescription(),
            currentPkg.getManifest(),
            newProcessConfig,  // NEW configuration
            currentPkg.getSecurityPolicy(),
            currentPkg.getRepository(),
            currentPkg.getInstalledDate(),
            currentPkg.getInstallPath()
        );
        
        // Update in registry (this will trigger save)
        return installationRegistry.registerPackage(updatedPkg)
            .thenRun(() -> {
                Log.logMsg("[NodeController] Configuration updated for: " + packageId);
                Log.logMsg("  New ProcessId: " + newProcessConfig.getProcessId());
            });
    }


    /**
     * Get installation registry (for RuntimeAccess)
     */
    public InstallationRegistry getInstallationRegistry() {
        return installationRegistry;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MESSAGE HANDLING
    // ═══════════════════════════════════════════════════════════════════════
    private long lastChecked = 0;
    private void checkReplay(long timestamp){
       
        long currentTime = System.currentTimeMillis();
        long cutoff = currentTime - MAX_COMMAND_AGE_MS;

        if((currentTime - lastChecked) > (MAX_COMMAND_AGE_MS * 2)){
            lastChecked = currentTime;    
            Iterator<Long> iter = recentTimestamps.iterator();
            while (iter.hasNext()) {
                Long ts = iter.next();
                if (ts < cutoff) {
                    iter.remove();
                }
            }
        }
        if(timestamp < cutoff){
            throw new SecurityException("[NodeController] Command is stale (> 60s");
        }

        if(!recentTimestamps.add(timestamp)){
            throw new SecurityException("[NodeController] Duplicate timestamp");
        }
        
        

    }

    /**
     * Verify timestamp is recent and not reused
     * 
     * @param timestamp The timestamp to verify
     */
    private void checkTimestamp(long timestamp) throws SecurityException {
        long now = System.currentTimeMillis();
        long age = now - timestamp;
        
        // Check if command is too old
        if (age > MAX_COMMAND_AGE_MS) {
            throw new SecurityException(String.format(
                "[NodeController] SECURITY: Command expired (%dms old, max %dms)",
                age, MAX_COMMAND_AGE_MS));
        }
        
        // Check for future timestamps (clock skew attack)
        if (age < -5000) { // Allow 5 seconds clock skew
            throw new SecurityException(String.format(
                "[NodeController] SECURITY: Future timestamp detected (%dms in future)",
                -age));
        }
        
        // Check for replay (duplicate timestamp)
        checkReplay(timestamp);
           
        
        
    }
    private NoteBytesMap verifySignedMessage(NoteBytesReadOnly payload) throws SecurityException {
        NoteBytesMap signedMessage = payload.getAsMap();

        NoteBytes signatureBytes = signedMessage.get(Keys.SIGNATURE);
        NoteBytes payloadBytes = signedMessage.get(Keys.PAYLOAD);
        NoteBytes timeStampBytes = signedMessage.get(Keys.TIMESTAMP);

        // FIX: These should check if null (not if NOT null)
        if (signatureBytes == null || payloadBytes == null || timeStampBytes == null) {
            throw new IllegalArgumentException("[NodeController] Signed payload required with signature, payload, and timestamp");
        }
        
        if (payloadBytes.getType() != NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE) {
            throw new IllegalArgumentException("[NodeController] NoteBytesObject type required");
        }
        
        checkTimestamp(timeStampBytes.getAsLong());

        Ed25519Signer verifier = new Ed25519Signer();
        verifier.init(false, signatureVerifier.getSigningPublicKey());
        byte[] dataBytes = payloadBytes.getBytes();
        verifier.update(dataBytes, 0, dataBytes.length);
        
        if (verifier.verifySignature(signatureBytes.getBytes())) {
            return payloadBytes.getAsMap();
        }

        throw new SecurityException("[NodeController] Invalid signature");
    }
  
    /**
     * Requires signed node commands
     * 
     * @param packet received packet
     * @return Void or CompletionException
     */
    @Override
    public CompletableFuture<Void> handleMessage(RoutedPacket packet) {
        NoteBytesReadOnly payload = packet.getPayload();
        if(payload.getType() != NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("[NodeController] NoteBytesObject type required"));
        }

        NoteBytesMap message = null;
        try{
            message = verifySignedMessage(payload);
        }catch(Exception e){
            return CompletableFuture.failedFuture(new CompletionException("[NodeController] Signing failed", e));
        }

        NoteBytes cmdBytes = message.get(Keys.CMD);

        if (cmdBytes == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("[NodeController] Missing command field"));
        }
        
        RoutedMessageExecutor msgExec = m_executorMap.get(cmdBytes);

        if(msgExec != null){
            return msgExec.execute(message, packet);
        }else{
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("[NodeController] Unknown command: " + cmdBytes));
        }

    
    }
    
    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        Log.logError("[NodeController] Unexpected stream channel from: " + fromPath);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHUTDOWN
    // ═══════════════════════════════════════════════════════════════════════
    

    public CompletableFuture<Void> shutdown() {
        Log.logMsg("[NodeController] Shutting down - unloading " + 
            instanceRegistry.getAllInstances().size() + " instances");
        
        List<CompletableFuture<Void>> shutdownFutures = new ArrayList<>();
        
        // Unload all instances
        for (NodeInstance instance : instanceRegistry.getAllInstances()) {
            shutdownFutures.add(unloadInstance(instance.getInstanceId()));
        }
        
        // Shutdown installation registry
        if (installationRegistry != null) {
            shutdownFutures.add(installationRegistry.shutdown());
        }
        
        return CompletableFuture.allOf(
            shutdownFutures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                Log.logMsg("[NodeController] Shutdown complete");
            });
    }

    
    
    private CompletableFuture<Void> handleListInstalled(NoteBytesMap msg, RoutedPacket packet) {
        // Get installed packages from registry

        NoteBytes[] noteBytesTmp = installationRegistry
            .getInstalledPackages()
            .stream()
            .map(InstalledPackage::toNoteBytes)
            .toArray(NoteBytes[]::new);

        reply(packet, new NoteBytesPair(NodeConstants.PACKAGES, new NoteBytesArrayReadOnly(noteBytesTmp)));

        return CompletableFuture.completedFuture(null);
    }

    
    
    private CompletableFuture<Void> handleListInstances(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        NoteBytes filter = message.get(Keys.FILTER);
        List<NodeInstance> instances;
        if(filter.equals(ProtocolMesssages.ALL)){
            instances = instanceRegistry.getAllInstances();
        }else if(filter.equals(NodeConstants.BY_PACKAGE)){
            PackageId pkgId = new PackageId(
                message.getReadOnly(Keys.PACKAGE_ID),
                "" // Version not needed for lookup
            );
            instances = instanceRegistry.getInstancesByPackage(pkgId);
        }else if(filter.equals(NodeConstants.BY_PROCESS)){
            NoteBytesReadOnly processId = message.getReadOnly(Keys.PROCESS_ID);
            instances = instanceRegistry.getInstancesByProcess(processId);
        }else{
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unknown filter: " + filter));
        }

        NoteBytes[] values = instances.stream()
            .map(NodeInstance::toNoteBytes)
            .toArray(NoteBytes[]::new);
        
        reply(packet, 
            new NoteBytesPair("instances", new NoteBytesArrayReadOnly(values)));
        
        return CompletableFuture.completedFuture(null);
    }
    
    
    private CompletableFuture<Void> handleUninstallPackage(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        NoteBytesReadOnly pkgIdBytes = message.getReadOnly(Keys.PACKAGE_ID);
        if(pkgIdBytes != null){
        PackageId packageId = new PackageId(
           pkgIdBytes,
            "" // Version handled internally
        );
        boolean deleteData = message.get(NodeConstants.DELETE_DATA).getAsBoolean();
        
        // TODO: progress
        return uninstallPackage(packageId, null)
            .thenCompose(v -> {
                if (deleteData) {
                    return deletePackageData(packageId, null);
                }
                return CompletableFuture.completedFuture(null);
            })
            .thenRun(()->reply(packet, new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS)))
            .exceptionally(ex -> {
                NoteBytes serializableEx = null;
                try{
                    serializableEx = new NoteSerializable(ex);
                }catch(Exception e){
                }
                
                if(serializableEx != null){
                    reply(packet, 
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.ERROR),
                        new NoteBytesPair(Keys.ERROR_MESSAGE, ex.getMessage()),
                        new NoteBytesPair(Keys.EXCEPTION, serializableEx)
                    );
                }else{
                    reply(packet, 
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.ERROR),
                        new NoteBytesPair(Keys.ERROR_MESSAGE, ex.getMessage())
                    );
                }
                return null;
            });
        }else{
            return CompletableFuture.failedFuture(new IllegalArgumentException("package_id required"));
        }
    }
    
    private CompletableFuture<Void> handleInstallPackage(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        try {
            // Parse installation request components
            PackageInfo packageInfo = PackageInfo.fromNoteBytes(
                message.get(NodeConstants.PACKAGE_INFO).getAsNoteBytesMap());
            
            ProcessConfig processConfig = ProcessConfig.fromNoteBytes(
                message.get(NodeConstants.PROCESS_CONFIG).getAsNoteBytesMap());
            
            PolicyManifest policyManifest = PolicyManifest.fromNoteBytes(
                message.get(NodeConstants.POLICY_MANIFEST).getAsNoteBytesMap());
            
            boolean loadImmediately = message.get(NodeConstants.LOAD_IMMEDIATELY).getAsBoolean();
            
            // Build InstallationRequest
            // Note: Password already verified by caller - signature confirmed it
            InstallationRequest request = new InstallationRequest(
                packageInfo,
                processConfig,
                policyManifest,
                null,  // password not needed - already verified
                loadImmediately,
                false, // userReviewedSource
                null   // sourceRepo
            );
            
            // Execute installation
            return installPackage(request)
                .thenAccept(installedPackage -> {
                    reply(packet,
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS),
                        new NoteBytesPair(NodeConstants.INSTALLED_PACKAGE, installedPackage.toNoteBytes())
                    );
                })
                .exceptionally(ex -> {
                    handleCommandError(packet, "Installation failed", ex);
                    return null;
                });
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Failed to parse install request: " + e.getMessage()));
        }
    }
    
    private CompletableFuture<Void> handleLoadNode(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        try {
            // Parse package ID
            NoteBytesReadOnly pkgIdBytes = message.getReadOnly(Keys.PACKAGE_ID);
            if (pkgIdBytes == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("package_id required"));
            }
            
            PackageId packageId = new PackageId(
                pkgIdBytes,
                "" // Version handled internally
            );
            
            // Get installed package from registry
            InstalledPackage pkg = installationRegistry.getPackage(packageId);
            if (pkg == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Package not found: " + packageId));
            }
            
            // Create load request
            NodeLoadRequest loadRequest = new NodeLoadRequest(pkg);
            
            // Load the node
            return loadNode(loadRequest)
                .thenAccept(instance -> {
                    // Serialize instance info for response
                    NoteBytesMap instanceData = serializeInstanceInfo(instance);
                    
                    reply(packet,
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS),
                        new NoteBytesPair(Keys.INSTANCE, instanceData)
                    );
                })
                .exceptionally(ex -> {
                    handleCommandError(packet, "Load failed", ex);
                    return null;
                });
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Failed to parse load request: " + e.getMessage()));
        }
    }
    
    private CompletableFuture<Void> handleUnloadInstance(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        try {
            // Parse instance ID
            String instanceIdStr = message.get(Keys.INSTANCE_ID).getAsString();
            if (instanceIdStr == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("instance_id required"));
            }
            
            InstanceId instanceId = InstanceId.fromString(instanceIdStr);
            
            // Unload the instance
            return unloadInstance(instanceId)
                .thenRun(() -> {
                    reply(packet,
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS)
                    );
                })
                .exceptionally(ex -> {
                    handleCommandError(packet, "Unload failed", ex);
                    return null;
                });
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Failed to parse unload request: " + e.getMessage()));
        }
    }
    
    private CompletableFuture<Void> handleUpdateConfig(
        NoteBytesMap message,
        RoutedPacket packet
    ) {
        try {
            // Parse package ID
            NoteBytesReadOnly pkgIdBytes = message.getReadOnly(Keys.PACKAGE_ID);
            if (pkgIdBytes == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("package_id required"));
            }
            
            PackageId packageId = new PackageId(
                pkgIdBytes,
                "" // Version handled internally
            );
            
            // Parse new process config
            ProcessConfig newProcessConfig = ProcessConfig.fromNoteBytes(
                message.get(NodeConstants.PROCESS_CONFIG).getAsNoteBytesMap());
            
            // Note: Password already verified by caller - signature confirmed it
            return updatePackageConfiguration(packageId, newProcessConfig)
                .thenRun(() -> {
                    reply(packet,
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS)
                    );
                })
                .exceptionally(ex -> {
                    handleCommandError(packet, "Config update failed", ex);
                    return null;
                });
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Failed to parse update request: " + e.getMessage()));
        }
    }

    /**
     * Serialize NodeInstance to NoteBytesMap for transmission
     * 
     * Creates a lightweight representation without the actual INode object
     */
    private NoteBytesMap serializeInstanceInfo(NodeInstance instance) {
        NoteBytesMap data = new NoteBytesMap();
        
        // Instance identification
        data.put(Keys.INSTANCE_ID, instance.getInstanceId().toString());
        data.put(Keys.STATE, instance.getState().name());
        
        // Package information
        data.put(NodeConstants.INSTALLED_PACKAGE, instance.getPackage().toNoteBytes());
        
        // Timing information
        data.put(NodeConstants.LOAD_TIME, instance.getLoadTime());
        data.put(NodeConstants.CRASH_COUNT, instance.getCrashCount());
        
        return data;
    }

    /**
     * Handle command errors with consistent response format
     */
    private void handleCommandError(RoutedPacket packet, String message, Throwable ex) {
        Log.logError("[NodeController] " + message + ": " + ex.getMessage());
        
        // Unwrap CompletionException if present
        Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
        
        // Try to serialize exception
        NoteBytes serializableEx = null;
        try {
            serializableEx = new NoteSerializable(cause);
        } catch (Exception e) {
            // If serialization fails, just use message
        }
        
        // Build error response
        if (serializableEx != null) {
            reply(packet,
                new NoteBytesPair(Keys.STATUS, ProtocolMesssages.ERROR),
                new NoteBytesPair(Keys.ERROR_MESSAGE, message + ": " + cause.getMessage()),
                new NoteBytesPair(Keys.EXCEPTION, serializableEx)
            );
        } else {
            reply(packet,
                new NoteBytesPair(Keys.STATUS, ProtocolMesssages.ERROR),
                new NoteBytesPair(Keys.ERROR_MESSAGE, message + ": " + cause.getMessage())
            );
        }
    }
}