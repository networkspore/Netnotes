package io.netnotes.system.nodes.security;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesArray;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.system.CoreConstants;

import java.util.*;

/**
 * PathCapability - Permission to access a path pattern with specific operations
 * 
 * REFACTORED:
 * - Uses ContextPath constants instead of hardcoded strings
 * - Supports path prefix matching for flexible topology
 * - Handles logical groupings (e.g., "services" in path, not parent)
 */
public class PathCapability {
    public static final NoteBytesReadOnly SELF = new NoteBytesReadOnly("{self}");

    
    private final ContextPath pathPattern;  // Changed from String to ContextPath
    private final Set<Operation> operations;
    private final boolean defaultGranted;
    private final String reason;
    
    // For service access with multiple names
    private final List<String> serviceNames;
    
    // Path matching mode
    private final MatchMode matchMode;
    
    public PathCapability(
            ContextPath pathPattern,
            Set<Operation> operations,
            boolean defaultGranted,
            String reason) {
        this(pathPattern, operations, defaultGranted, reason, null, MatchMode.EXACT);
    }
    
    public PathCapability(
            ContextPath pathPattern,
            Set<Operation> operations,
            boolean defaultGranted,
            String reason,
            List<String> serviceNames,
            MatchMode matchMode) {
        this.pathPattern = pathPattern;
        this.operations = new HashSet<>(operations);
        this.defaultGranted = defaultGranted;
        this.reason = reason;
        this.serviceNames = serviceNames != null ? new ArrayList<>(serviceNames) : null;
        this.matchMode = matchMode;
    }
    
    /**
     * Operations that can be performed on paths
     */
    public enum Operation {
        MESSAGE,    // Send messages via process registry
        STREAM,     // Open stream channels
        READ,       // Read files (for data paths)
        WRITE       // Write files (for data paths)
    }
    
    /**
     * Path matching modes
     */
    public enum MatchMode {
        EXACT,          // Exact path match
        PREFIX,         // Match all under prefix (e.g., "system/services/*")
        WITH_SELF       // Pattern with {self} placeholder
    }
    
    // ===== MATCHING =====
    
    /**
     * Check if this capability allows access to target path with operation
     */
    public boolean allows(ContextPath target, Operation operation) {
        if (!operations.contains(operation)) {
            return false;
        }
        
        return matchesPath(target);
    }
    
    /**
     * Check if target matches this capability's path pattern
     */
    private boolean matchesPath(ContextPath target) {
        switch (matchMode) {
            case EXACT:
                return target.equals(pathPattern);
                
            case PREFIX:
                // Match all under prefix
                if (serviceNames != null && !serviceNames.isEmpty()) {
                    // Service-specific matching
                    return matchesService(target);
                }
                return target.startsWith(pathPattern);
                
            case WITH_SELF:
                // Handled by allowsWithSelfSubstitution
                return false;
        }
        return false;
    }
    
    /**
     * Match specific services under a path
     */
    private boolean matchesService(ContextPath target) {
        // Check if target is under the pattern path
        if (!target.startsWith(pathPattern)) {
            return false;
        }
        
        // Check if "all" services allowed
        if (serviceNames.contains("all")) {
            return true;
        }
        
        // Extract service name from target path
        // e.g., "system/services/io-daemon" -> "io-daemon"
        String[] targetSegments = target.getSegments().getAsStringArray();
        String[] patternSegments = pathPattern.getSegments().getAsStringArray();
        
        if (targetSegments.length <= patternSegments.length) {
            return false;
        }
        
        String serviceName = targetSegments[patternSegments.length];
        
        // Check if this service is in the allowed list
        return serviceNames.contains(serviceName);
    }
    
    /**
     * Check if this capability matches pattern with {self} substitution
     */
    public boolean allowsWithSelfSubstitution(
            ContextPath target, 
            Operation operation, 
            NoteBytesReadOnly selfPackageId) {
        
        if (!operations.contains(operation)) {
            return false;
        }
        
        if (matchMode != MatchMode.WITH_SELF) {
            return false;
        }
        
        // Replace {self} in pattern with package ID
        ContextPath substitutedPattern = substituteSelf(pathPattern, selfPackageId);
        
        return target.startsWith(substitutedPattern);
    }
    
    
    /**
     * Substitute {self} placeholder in path
     */
    private ContextPath substituteSelf(ContextPath pattern, NoteBytesReadOnly selfValue) {
        NoteBytesReadOnly[] segments = pattern.getSegments().getAsArray();
        NoteBytesReadOnly[] newSegments = new NoteBytesReadOnly[segments.length];
        
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].equals(SELF)) {
                newSegments[i] = selfValue;
            } else {
                newSegments[i] = segments[i];
            }
        }
        
        return ContextPath.of(newSegments);
    }
    
    // ===== FACTORY METHODS (Using Path Constants) =====
    
    /**
     * Message NodeController (always granted)
     */
    public static PathCapability messageController() {
        return new PathCapability(
            CoreConstants.NODE_CONTROLLER_PATH,
            Set.of(Operation.MESSAGE),
            true,
            "Communicate with NodeController for lifecycle and discovery",
            null,
            MatchMode.EXACT
        );
    }
    
    /**
     * Access own runtime data (always granted)
     * 
     * Pattern: /runtime/nodes/{self}
     * 
     * TODO: packageId is not used
     */
    public static PathCapability ownRuntimeData(NoteBytesReadOnly packageId) {
        ContextPath pattern = CoreConstants.NODES_PATH.append(SELF);
        
        return new PathCapability(
            pattern,
            Set.of(Operation.READ, Operation.WRITE),
            true,
            "Read and write node's runtime data",
            null,
            MatchMode.WITH_SELF
        );
    }
    

    /**
     * Access system services
     * 
     * @param operations Operations allowed (MESSAGE, STREAM, etc.)
     * @param serviceNames List of service names, or "all" for all services
     */
    public static PathCapability accessServices(
            Set<Operation> operations, 
            String... serviceNames) {
        
        List<String> names = Arrays.asList(serviceNames);
        
        return new PathCapability(
            CoreConstants.SERVICES_PATH,  // Use constant!
            operations,
            false,
            "Access system services: " + String.join(", ", serviceNames),
            names,
            MatchMode.PREFIX
        );
    }
    
    /**
     * Access specific service by path constant
     */
    public static PathCapability accessService(
            ContextPath servicePath,
            Set<Operation> operations,
            String reason) {
        
        return new PathCapability(
            servicePath,
            operations,
            false,
            reason,
            null,
            MatchMode.EXACT
        );
    }
    
    /**
     * Access IO Daemon specifically
     */
    public static PathCapability accessIODaemon(Set<Operation> operations) {
        return accessService(
            CoreConstants.IO_DAEMON_PATH,
            operations,
            "Access IO Service for hardware and system IO"
        );
    }
    
    /**
     * Message other nodes (requires approval)
     */
    public static PathCapability messageNodes() {
        return new PathCapability(
            CoreConstants.NODES_PATH,
            Set.of(Operation.MESSAGE),
            false,
            "Send messages to other nodes",
            null,
            MatchMode.PREFIX
        );
    }
    
    /**
     * Stream to other nodes (requires approval)
     */
    public static PathCapability streamNodes() {
        return new PathCapability(
            CoreConstants.NODES_PATH,
            Set.of(Operation.STREAM),
            false,
            "Open streaming channels to other nodes",
            null,
            MatchMode.PREFIX
        );
    }
    
    
    /**

    /**
     * Access all system services (dangerous - requires explicit approval)
     */
    public static PathCapability accessAllServices(Set<Operation> operations) {
        return accessServices(operations, "all");
    }
    
    // ===== GETTERS =====
    
    public ContextPath getPathPattern() { return pathPattern; }
    public Set<Operation> getOperations() { return Collections.unmodifiableSet(operations); }
    public boolean isDefaultGranted() { return defaultGranted; }
    public String getReason() { return reason; }
    public MatchMode getMatchMode() { return matchMode; }
    public List<String> getServiceNames() { 
        return serviceNames != null ? Collections.unmodifiableList(serviceNames) : null; 
    }
    
    /**
     * Get user-friendly description
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(pathPattern.toString());
        
        if (matchMode == MatchMode.PREFIX) {
            sb.append("/*");
        }
        
        if (serviceNames != null && !serviceNames.isEmpty()) {
            sb.append(" [").append(String.join(", ", serviceNames)).append("]");
        }
        
        sb.append(" - ");
        sb.append(String.join(", ", operations.stream()
            .map(Enum::name)
            .toList()));
        
        return sb.toString();
    }
    
    // ===== SERIALIZATION =====
    
    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        
        map.put("path_pattern", pathPattern.toString());
        map.put("match_mode", matchMode.name());
        
        // Operations
        NoteBytesArray opsArr = new NoteBytesArray();
        operations.forEach(op -> opsArr.add(new NoteBytesReadOnly(op.name())));
        map.put("operations", opsArr);
        
        map.put("default_granted", defaultGranted);
        map.put("reason", reason);
        
        // Service names (if applicable)
        if (serviceNames != null && !serviceNames.isEmpty()) {
            NoteBytesArray namesArr = new NoteBytesArray();
            serviceNames.forEach(name -> namesArr.add(new NoteBytesReadOnly(name)));
            map.put("service_names", namesArr);
        }
        
        return map.toNoteBytes();
    }
    
    public static PathCapability fromNoteBytes(NoteBytesObject obj) {
        NoteBytesMap map = obj.getAsNoteBytesMap();
        
        ContextPath pathPattern = ContextPath.parse(map.get("path_pattern").getAsString());
        
        MatchMode matchMode = MatchMode.valueOf(
            map.get("match_mode").getAsString()
        );
        
        // Parse operations
        Set<Operation> operations = new HashSet<>();
        NoteBytesArray opsArr = map.get("operations").getAsNoteBytesArray();
        for (NoteBytes b : opsArr.getAsArray()) {
            operations.add(Operation.valueOf(b.getAsString()));
        }
        
        boolean defaultGranted = map.get("default_granted").getAsBoolean();
        String reason = map.get("reason").getAsString();
        
        // Parse service names (if present)
        List<String> serviceNames = null;
        NoteBytes namesBytes = map.get("service_names");
        if (namesBytes != null) {
            serviceNames = new ArrayList<>();
            NoteBytesArray namesArr = namesBytes.getAsNoteBytesArray();
            for (NoteBytes b : namesArr.getAsArray()) {
                serviceNames.add(b.getAsString());
            }
        }
        
        return new PathCapability(
            pathPattern, operations, defaultGranted, reason, serviceNames, matchMode
        );
    }
    
    // ===== EQUALITY =====
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathCapability)) return false;
        PathCapability other = (PathCapability) obj;
        return pathPattern.equals(other.pathPattern) &&
               operations.equals(other.operations) &&
               matchMode == other.matchMode &&
               Objects.equals(serviceNames, other.serviceNames);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pathPattern, operations, matchMode, serviceNames);
    }
    
    @Override
    public String toString() {
        return "PathCapability{" + getDescription() + "}";
    }
}