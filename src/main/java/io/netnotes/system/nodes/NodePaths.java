package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;

/**
 * NodePaths - Centralized path structure for node system
 * 
 * PATH HIERARCHY:
 * 
 * /system/
 *   └─ nodes/
 *       ├─ registry/           # Installation metadata (InstallationRegistry)
 *       │   └─ installed.json  # List of installed packages
 *       │
 *       ├─ packages/           # Package files (read-only after install)
 *       │   └─ {packageId}/
 *       │       ├─ {version}/
 *       │       │   ├─ manifest.json
 *       │       │   ├─ package.jar
 *       │       │   └─ resources/
 *       │       └─ current -> {version}  # Symlink to active version
 *       │
 *       ├─ repositories/       # Repository configuration
 *       │   └─ sources.json    # List of package sources
 *       │
 *       └─ runtime/            # Runtime node data (per-instance)
 *           └─ {packageId}/
 *               ├─ config/     # Node configuration
 *               ├─ data/       # Node persistent data
 *               └─ cache/      # Node temporary data
 * 
 * /user/
 *   └─ nodes/                  # User-accessible node data
 *       └─ {packageId}/
 *           └─ (node-specific user data)
 * 
 * FLOWPROCESS NETWORK PATHS:
 * /system/controller/nodes/{packageId}  # FlowProcess routing path
 * 
 * ACCESS CONTROL:
 * - System paths (/system/*): Read-only to nodes, write-only to system
 * - Runtime paths (/system/nodes/runtime/{packageId}): Read-write to owning node only
 * - User paths (/user/nodes/{packageId}): Read-write to owning node only
 * - Inter-node: Via explicit permission grants only
 */
public class NodePaths {
    
    // ===== ROOT PATHS =====
    
    private static final ContextPath SYSTEM_ROOT = ContextPath.of("system");
    private static final ContextPath USER_ROOT = ContextPath.of("user");
    
    // ===== SYSTEM NODE PATHS =====
    
    /** Base path for all node system data: /system/nodes */
    public static final ContextPath SYSTEM_NODES = SYSTEM_ROOT.append("nodes");
    
    /** Installation registry: /system/nodes/registry */
    public static final ContextPath REGISTRY = SYSTEM_NODES.append("registry");
    
    /** Installed packages metadata file */
    public static final ContextPath INSTALLED_PACKAGES = REGISTRY.append("installed.json");
    
    /** Package files storage: /system/nodes/packages */
    public static final ContextPath PACKAGES = SYSTEM_NODES.append("packages");
    
    /** Repository configuration: /system/nodes/repositories */
    public static final ContextPath REPOSITORIES = SYSTEM_NODES.append("repositories");
    
    /** Repository sources list */
    public static final ContextPath REPOSITORY_SOURCES = REPOSITORIES.append("sources.json");
    
    /** Runtime node data: /system/nodes/runtime */
    public static final ContextPath RUNTIME = SYSTEM_NODES.append("runtime");
    
    // ===== USER NODE PATHS =====
    
    /** User-accessible node data: /user/nodes */
    public static final ContextPath USER_NODES = USER_ROOT.append("nodes");
    
    // ===== PATH BUILDERS =====
    
    /**
     * Get package storage path
     * /system/nodes/packages/{packageId}/{version}
     */
    public static ContextPath getPackagePath(String packageId, String version) {
        return PACKAGES.append(packageId, version);
    }
    
    /**
     * Get package JAR path
     * /system/nodes/packages/{packageId}/{version}/package.jar
     */
    public static ContextPath getPackageJarPath(String packageId, String version) {
        return getPackagePath(packageId, version).append("package.jar");
    }
    
    /**
     * Get package manifest path
     * /system/nodes/packages/{packageId}/{version}/manifest.json
     */
    public static ContextPath getPackageManifestPath(String packageId, String version) {
        return getPackagePath(packageId, version).append("manifest.json");
    }
    
    /**
     * Get runtime data path for a node instance
     * /system/nodes/runtime/{packageId}
     * 
     * This is the node's SYSTEM data area (config, state, etc.)
     */
    public static ContextPath getRuntimePath(String packageId) {
        return RUNTIME.append(packageId);
    }
    
    /**
     * Get runtime config path
     * /system/nodes/runtime/{packageId}/config
     */
    public static ContextPath getRuntimeConfigPath(String packageId) {
        return getRuntimePath(packageId).append("config");
    }
    
    /**
     * Get runtime data path
     * /system/nodes/runtime/{packageId}/data
     */
    public static ContextPath getRuntimeDataPath(String packageId) {
        return getRuntimePath(packageId).append("data");
    }
    
    /**
     * Get runtime cache path
     * /system/nodes/runtime/{packageId}/cache
     */
    public static ContextPath getRuntimeCachePath(String packageId) {
        return getRuntimePath(packageId).append("cache");
    }
    
    /**
     * Get user data path for a node
     * /user/nodes/{packageId}
     * 
     * This is where nodes store USER-VISIBLE data
     */
    public static ContextPath getUserNodePath(String packageId) {
        return USER_NODES.append(packageId);
    }
    
    /**
     * Get FlowProcess network path for a node
     * /system/controller/nodes/{packageId}
     * 
     * This is for routing, not file storage
     */
    public static ContextPath getFlowProcessPath(String packageId) {
        return ContextPath.of("system", "controller", "nodes", packageId);
    }
    
    // ===== PATH VALIDATION =====
    
    /**
     * Check if path is within system nodes area
     */
    public static boolean isSystemNodePath(ContextPath path) {
        return path.startsWith(SYSTEM_NODES);
    }
    
    /**
     * Check if path is within user nodes area
     */
    public static boolean isUserNodePath(ContextPath path) {
        return path.startsWith(USER_NODES);
    }
    
    /**
     * Check if path is a node's runtime data path
     */
    public static boolean isRuntimePath(ContextPath path) {
        return path.startsWith(RUNTIME);
    }
    
    /**
     * Check if path is in package storage (read-only)
     */
    public static boolean isPackagePath(ContextPath path) {
        return path.startsWith(PACKAGES);
    }
    
    /**
     * Extract package ID from a runtime or user path
     * Returns null if path doesn't match expected structure
     */
    public static String extractPackageId(ContextPath path) {
        if (path.startsWith(RUNTIME) && path.size() > RUNTIME.size()) {
            return path.getSegment(RUNTIME.size());
        }
        if (path.startsWith(USER_NODES) && path.size() > USER_NODES.size()) {
            return path.getSegment(USER_NODES.size());
        }
        return null;
    }
    
    // ===== ACCESS CONTROL HELPERS =====
    
    /**
     * Check if a node with given packageId can access the given path
     * 
     * Rules:
     * 1. Nodes can READ from /system/nodes/packages (their own package files)
     * 2. Nodes can READ/WRITE to /system/nodes/runtime/{their-packageId}
     * 3. Nodes can READ/WRITE to /user/nodes/{their-packageId}
     * 4. Nodes CANNOT access other nodes' paths without explicit permission
     * 5. Nodes CANNOT write to /system/nodes/registry or /system/nodes/repositories
     */
    public static boolean canAccess(String packageId, ContextPath path, boolean write) {
        // Allow access to own runtime data
        ContextPath ownRuntime = getRuntimePath(packageId);
        if (path.startsWith(ownRuntime)) {
            return true;
        }
        
        // Allow access to own user data
        ContextPath ownUserPath = getUserNodePath(packageId);
        if (path.startsWith(ownUserPath)) {
            return true;
        }
        
        // Allow READ from package storage (read-only)
        if (!write && isPackagePath(path)) {
            return true;
        }
        
        // Deny everything else by default
        return false;
    }
    
    /**
     * Create a sandboxed AppDataInterface for a node
     * This enforces the access control rules above
     */
    public static class NodeSandbox {
        private final String packageId;
        private final ContextPath runtimePath;
        private final ContextPath userPath;
        
        public NodeSandbox(String packageId) {
            this.packageId = packageId;
            this.runtimePath = NodePaths.getRuntimePath(packageId);
            this.userPath = NodePaths.getUserNodePath(packageId);
        }
        
        /**
         * Validate and resolve a path request from the node
         * 
         * @param requestedPath Path the node wants to access
         * @param write Whether this is a write operation
         * @return Resolved absolute path, or null if access denied
         */
        public ContextPath validateAndResolve(ContextPath requestedPath, boolean write) {
            // If path is already absolute and allowed, use it
            if (NodePaths.canAccess(packageId, requestedPath, write)) {
                return requestedPath;
            }
            
            // Otherwise, try to scope it to runtime path
            ContextPath scopedToRuntime = runtimePath.append(requestedPath);
            if (NodePaths.canAccess(packageId, scopedToRuntime, write)) {
                return scopedToRuntime;
            }
            
            // Try user path
            ContextPath scopedToUser = userPath.append(requestedPath);
            if (NodePaths.canAccess(packageId, scopedToUser, write)) {
                return scopedToUser;
            }
            
            // Access denied
            return null;
        }
        
        public String getPackageId() {
            return packageId;
        }
        
        public ContextPath getRuntimePath() {
            return runtimePath;
        }
        
        public ContextPath getUserPath() {
            return userPath;
        }
    }
}