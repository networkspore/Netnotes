package io.netnotes.system.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.RoutedPacket;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteFiles.NoteFile;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;

/**
 * InstallationRegistry - Tracks INSTALLED packages (FlowProcess)
 * 
 * RESPONSIBILITIES:
 * - Track which packages are installed
 * - Persist to NoteFile
 * - Emit events on install/uninstall
 * - Answer queries about installed packages
 * 
 * LIFECYCLE:
 * - Created by SystemRuntime during initialization
 * - Registered as child process at /system/nodes/registry
 * - Initialized (loads from NoteFile)
 * - Lives for entire session
 * - Closed during shutdown
 * 
 * STORAGE:
 * - Path: /node-packages/registry
 * - Format: NoteBytesMap { packageId -> InstalledPackage }
 * - NoteFile cached as instance field
 */
public class InstallationRegistry extends FlowProcess {
    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;
    
    private final ConcurrentHashMap<PackageId, InstalledPackage> installed;
    private final NoteFile registryFile;
    
    public InstallationRegistry(String name, NoteFile registryFile) {
        super(name, ProcessType.BIDIRECTIONAL);
        
        if (registryFile == null) {
            throw new IllegalArgumentException("Registry file cannot be null");
        }
        
        this.registryFile = registryFile;
        this.installed = new ConcurrentHashMap<>();
    }
    
    @Override
    public CompletableFuture<Void> run() {
        // Initialize will be called separately by SystemRuntime
        return getCompletionFuture();
    }
    
    /**
     * Initialize - load installation registry from NoteFile
     */
    public CompletableFuture<Void> initialize() {
        Log.logMsg("[InstallationRegistry] Initializing at: " + contextPath, LOG_LEVEL);
        
        return loadInstalledPackages()
            .exceptionally(ex -> {
                // First run - create empty registry
                Log.logMsg("[InstallationRegistry] First run - creating empty registry", LOG_LEVEL);
                return saveToFile().join(); // Create empty file
            });
    }
    
    /**
     * Load installed packages from cached NoteFile
     */
    private CompletableFuture<Void> loadInstalledPackages() {
        return registryFile.readNoteBytes()
            .thenAccept(noteBytesObj -> {
                if (noteBytesObj == null) {
                    throw new IllegalStateException("Registry file does not exist");
                }
                
                NoteBytesMap packagesMap = noteBytesObj.getAsNoteBytesMap();
                
                Log.logMsg("[InstallationRegistry] Loading " + 
                    packagesMap.size() + " installed packages", LOG_LEVEL);
                
                for (NoteBytes pkgIdBytes : packagesMap.keySet()) {
                    try {
                        NoteBytesMap pkgData = packagesMap.get(pkgIdBytes).getAsNoteBytesMap();
                        InstalledPackage pkg = 
                            InstalledPackage.fromNoteBytes(pkgData);
                        
                        installed.put(pkg.getPackageId(), pkg);
                        
                        Log.logMsg("[InstallationRegistry]   Loaded: " + 
                            pkg.getName() + " v" + pkg.getVersion(), LOG_LEVEL);
                            
                    } catch (Exception e) {
                        Log.logError("[InstallationRegistry]   Failed to load package " + 
                            pkgIdBytes.getAsString() + ": " + e.getMessage());
                    }
                }
                
                Log.logMsg("[InstallationRegistry] Successfully loaded " + 
                    installed.size() + " packages", LOG_LEVEL);
            });
    }
    
    /**
     * Register a newly installed package
     */
    public CompletableFuture<Void> registerPackage(InstalledPackage pkg) {
        installed.put(pkg.getPackageId(), pkg);
        
        Log.logMsg("[InstallationRegistry] Registered: " + pkg.getName() + 
            " (processId: " + pkg.getProcessId() + ")", LOG_LEVEL);
        
        // Emit event
        emitPackageEvent("package_installed", pkg.getPackageId().getId());
        
        return saveToFile()
            .thenRun(() -> {
                Log.logMsg("[InstallationRegistry] Registry saved (" + 
                    installed.size() + " packages)", LOG_LEVEL);
            });
    }
    
    /**
     * Unregister a package (before deletion)
     */
    public CompletableFuture<Void> unregisterPackage(PackageId packageId) {
        InstalledPackage removed = installed.remove(packageId);
        
        if (removed != null) {
            Log.logMsg("[InstallationRegistry] Unregistered: " + removed.getName(), LOG_LEVEL);
            
            // Emit event
            emitPackageEvent("package_uninstalled", packageId.getId());
            
            return saveToFile()
                .thenRun(() -> {
                    Log.logMsg("[InstallationRegistry] Registry saved after removal", LOG_LEVEL);
                });
        }
        
        Log.logMsg("[InstallationRegistry] Package not found: " + packageId, LOG_LEVEL);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Get all installed packages
     */
    public List<InstalledPackage> getInstalledPackages() {
        return new ArrayList<>(installed.values());
    }
    
    /**
     * Get specific package
     */
    public InstalledPackage getPackage(PackageId packageId) {
        return installed.get(packageId);
    }
    
    /**
     * Check if package is installed
     */
    public boolean isInstalled(PackageId packageId) {
        return installed.containsKey(packageId);
    }
    
    /**
     * Save registry to cached NoteFile
     */
    private CompletableFuture<Void> saveToFile() {
        if (registryFile == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Registry not initialized"));
        }
        
        NoteBytesMap packagesMap = new NoteBytesMap();
        
        // Serialize each package
        for (InstalledPackage pkg : installed.values()) {
            packagesMap.put(
                pkg.getPackageId().getId(),
                pkg.toNoteBytes()
            );
        }
        
        return registryFile.write(packagesMap.toNoteBytes())
            .thenRun(() -> {
                Log.logMsg("[InstallationRegistry] Saved " + 
                    installed.size() + " packages to file", LOG_LEVEL);
            })
            .exceptionally(ex -> {
                Log.logError("[InstallationRegistry] Failed to save: " + 
                    ex.getMessage());
                throw new RuntimeException("Failed to save installation registry", ex);
            });
    }
    
    /**
     * Emit package event (for subscribers like NodeController)
     */
    private void emitPackageEvent(String eventType, NoteBytes packageId) {
        NoteBytesMap event = new NoteBytesMap();
        event.put("event", new NoteBytes(eventType));
        event.put(Keys.PACKAGE_ID, packageId);
        event.put(Keys.TIMESTAMP, new NoteBytes(System.currentTimeMillis()));
        
        emit(event.toNoteBytes());
    }
    
    @Override
    public CompletableFuture<Void> handleMessage(RoutedPacket packet) {
        // Could handle queries like:
        // - list_installed
        // - get_package
        // - is_installed
        // For now, not implemented (direct method calls used)
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void handleStreamChannel(StreamChannel channel, ContextPath fromPath) {
        throw new UnsupportedOperationException(
            "InstallationRegistry does not support stream channels");
    }
    
    /**
     * Shutdown - ensure registry is saved and NoteFile closed
     */
    public CompletableFuture<Void> shutdown() {
        Log.logMsg("[InstallationRegistry] Shutting down", LOG_LEVEL);
        
        return saveToFile()
            .whenComplete((v, ex) -> {
                if (ex != null) {
                    Log.logError("[InstallationRegistry] Error during shutdown: " + 
                        ex.getMessage());
                }
                
                if (registryFile != null) {
                    registryFile.close();
                    Log.logMsg("[InstallationRegistry] NoteFile closed", LOG_LEVEL);
                }
            })
            .thenRun(() -> {
                complete();
                Log.logMsg("[InstallationRegistry] Shutdown complete", LOG_LEVEL);
            });
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("Installed packages: %d", installed.size());
    }
}