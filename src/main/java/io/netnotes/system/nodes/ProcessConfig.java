package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.system.CoreConstants;

/**
 * ProcessConfig - Defines namespace and paths for a node installation
 * 
 * SIMPLIFIED: Just namespace identification, no prescriptive behavior
 * 
 * ProcessId determines:
 * - Data root path: /data/nodes/{processId}
 * - Flow base path: /system/runtime/nodes/{processId}
 * 
 * Conceptual Understanding:
 * - Every installation lives in a namespace (processId)
 * - Multiple packages can share a namespace (grouping)
 * - Single package in namespace = effectively "standalone"
 * - Multiple packages in namespace = shared/grouped
 * - No prescriptive "modes" - just paths
 * 
 * Examples:
 * 1. Default: processId = packageId (most common)
 * 2. Grouped: processId = "my-workspace" (shared by multiple packages)
 * 3. Versioned: processId = "myapp-v2" (custom namespace)
 */
public class ProcessConfig {
    private final NoteBytesReadOnly processId;  // Namespace identifier
    private final ContextPath dataRootPath;     // /data/nodes/{processId}
    private final ContextPath flowBasePath;     // /system/runtime/nodes/{processId}

    /**
     * Private constructor - use factory methods
     */
    private ProcessConfig(
        NoteBytesReadOnly processId, 
        ContextPath dataRootPath, 
        ContextPath flowBasePath
    ) {
        this.processId = processId;
        this.dataRootPath = dataRootPath;
        this.flowBasePath = flowBasePath;
    }
    
    /**
     * Standard constructor - derives paths from processId
     */
    public ProcessConfig(NoteBytesReadOnly processId) {
        this.processId = processId;
        this.dataRootPath = CoreConstants.NODE_DATA_PATH.append(processId);
        this.flowBasePath = CoreConstants.NODES_PATH.append(processId);
    }
    
    // ===== GETTERS =====
    
    public NoteBytesReadOnly getProcessId() { 
        return processId; 
    }
    
    public ContextPath getDataRootPath() { 
        return dataRootPath; 
    }
    
    public ContextPath getFlowBasePath() { 
        return flowBasePath; 
    }
    
    // ===== FACTORY METHODS =====
    
    /**
     * Create config with default processId (packageId)
     * This is the most common case
     */
    public static ProcessConfig create(NoteBytesReadOnly processId) {
        return new ProcessConfig(processId);
    }
    
    /**
     * Create config for joining existing namespace
     */
    public static ProcessConfig forExistingNamespace(NoteBytesReadOnly existingProcessId) {
        return new ProcessConfig(existingProcessId);
    }
    
    /**
     * Create config with custom processId
     */
    public static ProcessConfig withCustomId(NoteBytesReadOnly customProcessId) {
        return new ProcessConfig(customProcessId);
    }
    
    // ===== SERIALIZATION =====
    
    public NoteBytesObject toNoteBytes() {
        return new NoteBytesObject(new NoteBytesPair[]{
            new NoteBytesPair(Keys.PROCESS_ID, processId),
            new NoteBytesPair("data_root_path", dataRootPath.getSegments()),
            new NoteBytesPair("flow_base_path", flowBasePath.getSegments())
        });
    }

    public static ProcessConfig fromNoteBytes(NoteBytesMap map) {
        NoteBytesReadOnly processId = map.getReadOnly(Keys.PROCESS_ID);
        ContextPath dataRootPath = ContextPath.of(
            map.get("data_root_path").getAsNoteBytesArrayReadOnly()
        );
        ContextPath flowBasePath = ContextPath.of(
            map.get("flow_base_path").getAsNoteBytesArrayReadOnly()
        );
       
        return new ProcessConfig(processId, dataRootPath, flowBasePath);
    }
    
    // ===== UTILITY =====
    
    @Override
    public String toString() {
        return String.format(
            "ProcessConfig[processId=%s, data=%s, flow=%s]",
            processId,
            dataRootPath,
            flowBasePath
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProcessConfig other)) return false;
        return processId.equals(other.processId);
    }
    
    @Override
    public int hashCode() {
        return processId.hashCode();
    }
}