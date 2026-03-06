package io.netnotes.system.nodes;

import java.util.concurrent.CompletableFuture;

import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.system.nodes.osgi.OSGiBundleLoader;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;

/**
 * NodeLoader - Loads nodes from packages (OSGi integration)
 * 
 * Loading strategies:
 * 1. OSGi bundle: Load JAR as OSGi bundle, instantiate via service
 * 2. Script: Load script (JavaScript, Python, etc.) via engine
 * 3. Native: Load native library via JNI
 * 
 * For now, focuses on OSGi bundles
 */
class NodeLoader {
    private final ProcessRegistryInterface processInterface;
    private final NoteFileServiceInterface fileInterface;
    private final OSGiBundleLoader osgiBundleLoader;
    private final String name;
    public NodeLoader(String name, ProcessRegistryInterface processInterface, NoteFileServiceInterface fileInterface) {
        this.name = name;
        this.processInterface = processInterface;
        this.fileInterface = fileInterface;
        this.osgiBundleLoader = new OSGiBundleLoader(fileInterface);
    }
    
    /**
     * Load node from installed package
     * 
     * Process:
     * 1. Read package manifest
     * 2. Determine load strategy (OSGi, script, native)
     * 3. Load code into runtime
     * 4. Instantiate INode implementation
     * 5. Return INode instance
     */
    public CompletableFuture<INode> loadNodeFromPackage(
            InstalledPackage installedPackage) {
        
        PackageManifest manifest = installedPackage.getManifest();
        
        Log.logMsg("[NodeLoader] Loading node: " + 
            installedPackage.getName() + " (type: " + manifest.getType() + ")", LogLevel.IMPORTANT);
        
        switch (manifest.getType()) {
            case "osgi-bundle":
                return loadOSGiBundle(installedPackage);
                
            case "script":
                return loadScript(installedPackage);
                
            case "native":
                return loadNative(installedPackage);
                
            default:
                return CompletableFuture.failedFuture(
                    new UnsupportedOperationException(
                        "Unknown node type: " + manifest.getType()));
        }
    }
    
    /**
     * Load OSGi bundle
     * 
     * Delegates to OSGiBundleLoader which:
     * 1. Initializes OSGi framework (if needed)
     * 2. Extracts bundle JAR from NoteFile
     * 3. Installs bundle in framework
     * 4. Starts bundle
     * 5. Waits for INode service registration
     * 6. Returns INode instance
     */
    private CompletableFuture<INode> loadOSGiBundle(InstalledPackage pkg) {
        return osgiBundleLoader.loadBundle(pkg);
    }
    
    /**
     * Unload OSGi bundle
     */
    public CompletableFuture<Void> unloadOSGiBundle(String packageId) {
        return osgiBundleLoader.unloadBundle(packageId);
    }
    
    /**
     * Load script-based node
     * 
     * TODO: Integrate with script engine (GraalVM, Nashorn)
     * - Read script from NoteFile
     * - Execute in isolated context
     * - Wrap in INode adapter
     * - Return adapter
     */
    private CompletableFuture<INode> loadScript(InstalledPackage pkg) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Script loading not yet implemented"));
    }
    
    /**
     * Load native library node
     * 
     * TODO: Integrate with JNI
     * - Extract native library from NoteFile
     * - Load via System.load()
     * - Wrap in INode adapter
     * - Return adapter
     */
    private CompletableFuture<INode> loadNative(InstalledPackage pkg) {
        return CompletableFuture.failedFuture(
            new UnsupportedOperationException(
                "Native library loading not yet implemented"));
    }
    
    /**
     * Shutdown loader (shuts down OSGi framework)
     */
    public CompletableFuture<Void> shutdown() {
        return osgiBundleLoader.shutdown();
    }
    
    /**
     * Get OSGi bundle loader (for diagnostics)
     */
    public OSGiBundleLoader getOSGiBundleLoader() {
        return osgiBundleLoader;
    }
}