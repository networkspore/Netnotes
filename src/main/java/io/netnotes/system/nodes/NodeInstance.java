package io.netnotes.system.nodes;


import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;

/**
 * NodeInstance - Wrapper around INode with lifecycle state
 * 
 * Tracks:
 * - Node implementation (INode)
 * - Installed package metadata
 * - Current state (loading, running, stopping, etc.)
 * - Sandboxed interface
 * - FlowProcess adapter
 * - Load time
 * - Crash count
 */
public class NodeInstance {
    private final InstanceId instanceId;           // Unique per load
    private final InstalledPackage pkg;             // What package
    private final ProcessConfig processConfig;     // What namespace
    private final INode inode;                     // The actual node
    
    private volatile NodeState state;
    private final long loadTime;
    private int crashCount;
    
    public NodeInstance(
        InstalledPackage pkg,
        INode inode
    ) {
        this.instanceId = InstanceId.generate();
        this.pkg = pkg;
        this.processConfig = pkg.getProcessConfig();
        this.inode = inode;
        this.state = NodeState.LOADING;
        this.loadTime = System.currentTimeMillis();
        this.crashCount = 0;
    }
    
    // ===== IDENTITY =====
    
    public InstanceId getInstanceId() { return instanceId; }
    public PackageId getPackageId() { return pkg.getPackageId(); }
    public NoteBytesReadOnly getProcessId() { return processConfig.getProcessId(); }
    
    // ===== CONFIGURATION =====
    
    public InstalledPackage getPackage() { return pkg; }
    public ProcessConfig getProcessConfig() { return processConfig; }
    public ContextPath getDataRootPath() { return processConfig.getDataRootPath(); }
    public ContextPath getFlowBasePath() { return processConfig.getFlowBasePath(); }
    
    // ===== RUNTIME =====
    
    public INode getINode() { return inode; }
    public NodeState getState() { return state; }
    public void setState(NodeState state) { this.state = state; }
    public long getLoadTime() { return loadTime; }
    public long getUptime() { return System.currentTimeMillis() - loadTime; }
    public int getCrashCount() { return crashCount; }
    public void incrementCrashCount() { crashCount++; }




    /**
     * Serialize to NoteBytes for transmission
     * 
     * Note: INode cannot be serialized, so this creates a metadata-only
     * representation suitable for queries and UI display
     */
    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        
        // Identity
        map.put(Keys.INSTANCE_ID, instanceId.toString());
        
        // Package info (full InstalledPackage)
        map.put(NodeConstants.INSTALLED_PACKAGE, pkg.toNoteBytes());
        
        // State
        map.put(Keys.STATE, state.name());
        
        // Runtime info
        map.put(NodeConstants.LOAD_TIME, loadTime);
        map.put(NodeConstants.UPTIME, getUptime());
        map.put(NodeConstants.CRASH_COUNT, crashCount);
        
        return map.toNoteBytes();
    }

    /**
     * Partial deserialization from NoteBytes
     * 
     * Creates a metadata-only NodeInstance (without actual INode)
     * Used for remote queries and UI display
     */
    public static NodeInstance fromNoteBytesMetadata(NoteBytesMap map) {
        try {
            // Parse installed package
            InstalledPackage pkg = InstalledPackage.fromNoteBytes(
                map.get(NodeConstants.INSTALLED_PACKAGE).getAsNoteBytesMap()
            );
            
            // Create instance without INode (null)
            NodeInstance instance = new NodeInstance(pkg, null);
            
            // Set state
            String stateStr = map.get(Keys.STATE).getAsString();
            instance.setState(NodeState.valueOf(stateStr));
            
            // Note: instanceId from map is ignored - generate() was called in constructor
            // For full reconstruction, would need additional constructor
            
            return instance;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize NodeInstance metadata", e);
        }
    }

    /**
     * Create metadata-only instance from full instance
     * Used when transmitting instance info over network
     */
    public NodeInstance toMetadataOnly() {
        NodeInstance metadata = new NodeInstance(pkg, null);
        metadata.setState(this.state);
        // Copy other fields as needed
        return metadata;
    }

    @Override
    public String toString() {
        return String.format(
            "Instance[id=%s, package=%s, process=%s, state=%s]",
            instanceId,
            pkg.getPackageId(),
            processConfig.getProcessId(),
            state
        );
    }


}
