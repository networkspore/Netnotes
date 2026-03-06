package io.netnotes.system.nodes.osgi;

import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.tracker.ServiceTracker;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.system.nodes.INode;
import io.netnotes.system.nodes.InstalledPackage;
import io.netnotes.system.nodes.PackageId;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSGiBundleLoader - Loads and manages OSGi bundles as nodes
 * 
 * ARCHITECTURE:
 * - Single OSGi Framework instance for all nodes
 * - Each package JAR is installed as an OSGi bundle
 * - Bundles export INode service
 * - Loader discovers and returns INode instances
 * 
 * LIFECYCLE:
 * 1. Initialize framework (once)
 * 2. Install bundle from NoteFile
 * 3. Start bundle
 * 4. Get INode service reference
 * 5. Return INode instance
 * 
 * SECURITY:
 * - Bundles run in same JVM but isolated classloaders
 * - No direct file system access (via AppDataInterface only)
 * - Service registry controls inter-bundle communication
 * 
 * RESOURCE MANAGEMENT:
 * - Bundles share framework instance
 * - Framework shutdown cleans all bundles
 * - Bundle stop releases INode service
 */
public class OSGiBundleLoader {
    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;
    
    private final NoteFileServiceInterface appDataInterface;
    private Framework framework;
    private final Map<String, Bundle> installedBundles = new ConcurrentHashMap<>();
    
    // OSGi framework initialization state
    private volatile boolean frameworkInitialized = false;
    private final Object frameworkLock = new Object();
    
    public OSGiBundleLoader(NoteFileServiceInterface fileServiceInterface) {
        this.appDataInterface = fileServiceInterface;
    }
    
    /**
     * Initialize OSGi framework (lazy - called on first bundle load)
     * TODO: this should be using steaming note file services rather than a direct file
     */
    private CompletableFuture<Void> initializeFramework() {
        if (frameworkInitialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            synchronized (frameworkLock) {
                if (frameworkInitialized) {
                    return;
                }
                
                try {
                    Log.logMsg("[OSGiBundleLoader] Initializing OSGi framework", LOG_LEVEL);
                    
                    // Get FrameworkFactory via ServiceLoader
                    ServiceLoader<FrameworkFactory> factoryLoader = 
                        ServiceLoader.load(FrameworkFactory.class);
                    
                    FrameworkFactory factory = factoryLoader.iterator().next();
                    
                    // Configure framework
                    Map<String, String> config = new HashMap<>();
                    
                    // Storage location for framework state
                    File frameworkStorage = new File(
                        io.netnotes.system.SettingsData.getDataDir(),
                        "osgi-framework"
                    );
                    frameworkStorage.mkdirs();
                    
                    config.put(Constants.FRAMEWORK_STORAGE, 
                        frameworkStorage.getAbsolutePath());
                    
                    // Clean storage on startup (optional - for testing)
                    config.put(Constants.FRAMEWORK_STORAGE_CLEAN, 
                        Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
                    
                    // Create and start framework
                    framework = factory.newFramework(config);
                    framework.start();
                    
                    frameworkInitialized = true;
                    
                    Log.logMsg("[OSGiBundleLoader] OSGi framework started", LOG_LEVEL);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize OSGi framework", e);
                }
            }
        });
    }
    
    /**
     * Load INode from OSGi bundle package
     * 
     * Process:
     * 1. Ensure framework initialized
     * 2. Install bundle in framework
     * 5. Wait for INode service registration
     * 6. Return INode service instance
     */
    public CompletableFuture<INode> loadBundle(InstalledPackage pkg) {
        return initializeFramework()
            .thenCompose(v -> installBundleFromNoteFile(pkg))
            .thenCompose(bundle -> waitForNodeService(pkg.getPackageId(), bundle))
            .whenComplete((inode, ex) -> {
                if (ex != null) {
                    Log.logError("[OSGiBundleLoader] Failed to load bundle: " + 
                        pkg.getName() + " - " + ex.getMessage());
                }
            });
    }
    
    /**
     * Read bundle from stream location
     */
    private CompletableFuture<Bundle> installBundleFromNoteFile(InstalledPackage pkg) {

        ContextPath jarPath = pkg.getInstallPath();

        return appDataInterface
            .getNoteFile(jarPath).thenCompose(jarFile->
            CompletableFuture.supplyAsync(() -> {
                try {
                    Log.logMsg("[OSGiBundleLoader] Installing bundle: " + 
                        pkg.getName() + " from " + jarPath, LOG_LEVEL);
                    if(!jarFile.isFile()){
                        throw new CompletionException("Note file is not written", new FileNotFoundException("Bundle JAR not found: " + jarPath));
                    }
                    // Load directly from stream
                    BundleContext context = framework.getBundleContext();
                    
                    String location = pkg.getPluginUrl();
                    
                    // Install from encrypted stream
                    try (InputStream stream = jarFile.getInputStream()) {
                        Bundle bundle = context.installBundle(location, stream);
                        //verify if we should start() here
                        bundle.start();
                        return bundle;
                    }
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to install bundle", e);
                }
            }).whenComplete((bundle, ex)->{
                jarFile.close();
            }));
    }
    
    /**
     * Wait for INode service registration
     * 
     * Bundle's BundleActivator should register INode service:
     * 
     * public class Activator implements BundleActivator {
     *     public void start(BundleContext context) {
     *         INode node = new MyNodeImpl();
     *         context.registerService(INode.class, node, null);
     *     }
     * }
     */
    private CompletableFuture<INode> waitForNodeService(
            PackageId packageId, 
            Bundle bundle) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.logMsg("[OSGiBundleLoader] Waiting for INode service from: " + 
                    packageId, LOG_LEVEL);
                
                BundleContext context = framework.getBundleContext();
                
                // Create service filter for this bundle
                String filter = String.format(
                    "(&(objectClass=%s)(bundle.id=%d))",
                    INode.class.getName(),
                    bundle.getBundleId()
                );
                
                // Wait for service (with timeout)
                ServiceReference<?>[] refs = context.getServiceReferences(
                    (String) null, 
                    filter
                );
                
                if (refs != null && refs.length > 0) {
                    INode node = (INode) context.getService(refs[0]);
                    
                    if (node != null) {
                        Log.logMsg("[OSGiBundleLoader] Found INode service", LOG_LEVEL);
                        return node;
                    }
                }
                
                // If not found immediately, use ServiceTracker for async discovery
                return waitForServiceWithTracker(context, bundle);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to get INode service", e);
            }
        });
    }
    
    /**
     * Wait for service using ServiceTracker (with timeout)
     */
    private INode waitForServiceWithTracker(
            BundleContext context, 
            Bundle bundle) throws Exception {
        
        ServiceTracker<INode, INode> tracker = new ServiceTracker<>(
            context,
            INode.class,
            null
        );
        
        tracker.open();
        
        try {
            // Wait up to 30 seconds for service
            INode node = tracker.waitForService(30000);
            
            if (node == null) {
                throw new RuntimeException(
                    "INode service not registered within timeout: " + 
                    bundle.getSymbolicName()
                );
            }
            
            Log.logMsg("[OSGiBundleLoader] INode service registered: " + node.getClass().getSimpleName(), LOG_LEVEL);
            
            return node;
            
        } finally {
            tracker.close();
        }
    }
    
    /**
     * Unload bundle (stop and uninstall)
     */
    public CompletableFuture<Void> unloadBundle(String packageId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Bundle bundle = installedBundles.remove(packageId);
                
                if (bundle != null) {
                    Log.logMsg("[OSGiBundleLoader] Unloading bundle: " + 
                        bundle.getSymbolicName(), LOG_LEVEL);
                    
                    // Stop bundle (deactivates BundleActivator)
                    bundle.stop();
                    
                    // Uninstall bundle
                    bundle.uninstall();
                    
                    Log.logMsg("[OSGiBundleLoader] Bundle unloaded: " + packageId, LOG_LEVEL);
                }
                
            } catch (BundleException e) {
                throw new RuntimeException("Failed to unload bundle", e);
            }
        });
    }
    
    /**
     * Shutdown OSGi framework and all bundles
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            synchronized (frameworkLock) {
                if (framework != null && frameworkInitialized) {
                    try {
                        Log.logMsg("[OSGiBundleLoader] Shutting down OSGi framework", LOG_LEVEL);
                        
                        // Stop framework (stops all bundles)
                        framework.stop();
                        
                        // Wait for framework to stop
                        framework.waitForStop(10000);
                        
                        Log.logMsg("[OSGiBundleLoader] OSGi framework stopped", LOG_LEVEL);
                        
                    } catch (Exception e) {
                        Log.logError("[OSGiBundleLoader] Error shutting down framework: " + 
                            e.getMessage());
                    } finally {
                        framework = null;
                        frameworkInitialized = false;
                        installedBundles.clear();
                    }
                }
            }
        });
    }
    
    /**
     * Get installed bundle
     */
    public Bundle getBundle(String packageId) {
        return installedBundles.get(packageId);
    }
    
    /**
     * Check if framework is running
     */
    public boolean isFrameworkActive() {
        return frameworkInitialized && 
               framework != null && 
               framework.getState() == Bundle.ACTIVE;
    }
    
    /**
     * Get bundle count
     */
    public int getBundleCount() {
        return installedBundles.size();
    }
}