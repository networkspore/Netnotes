package io.netnotes.system.nodes.security;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.*;
import io.netnotes.noteBytes.collections.NoteBytesMap;

import java.util.*;

/**
 * NodeSecurityPolicy - Simplified path-based security policy
 * 
 * Stores granted PathCapabilities for a node.
 * No command-based checking, just: "Can this node access this path with this operation?"
 */
public class NodeSecurityPolicy {
    
    private final NoteBytesReadOnly nodeId;
    private final NoteBytesReadOnly packageId;
    private final Set<PathCapability> grantedCapabilities;
    
    // Approval metadata
    private final long createdAt;
    private boolean approved;
    
    // Runtime permission grants (audit trail)
    private final List<RuntimePermissionGrant> runtimeGrants;
    
    public NodeSecurityPolicy(NoteBytesReadOnly nodeId, NoteBytesReadOnly packageId) {
        this.nodeId = nodeId;
        this.packageId = packageId;
        this.grantedCapabilities = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
        this.approved = false;
        this.runtimeGrants = new ArrayList<>();
        
        // Initialize with default capabilities (always granted)
        initializeDefaults();
    }
    
    /**
     * Initialize with default capabilities (no password needed)
     */
    private void initializeDefaults() {
        grantCapability(PathCapability.messageController());
        grantCapability(PathCapability.ownRuntimeData(packageId));
    }
    
    // ===== CAPABILITY MANAGEMENT =====
    
    /**
     * Grant a capability to the node
     */
    public void grantCapability(PathCapability capability) {
        grantedCapabilities.add(capability);
    }
    
    /**
     * Revoke a capability from the node
     */
    public void revokeCapability(PathCapability capability) {
        grantedCapabilities.remove(capability);
    }
    
    /**
     * Check if node can access path with operation
     */
    public boolean canAccess(ContextPath target, PathCapability.Operation operation) {
        for (PathCapability cap : grantedCapabilities) {
            // Try direct match
            if (cap.allows(target, operation)) {
                return true;
            }
            
            // Try with {self} substitution
            if (cap.allowsWithSelfSubstitution(target, operation, packageId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all granted capabilities
     */
    public Set<PathCapability> getGrantedCapabilities() {
        return Collections.unmodifiableSet(grantedCapabilities);
    }
    
    /**
     * Get capabilities that require approval (not default granted)
     */
    public Set<PathCapability> getApprovalRequiredCapabilities() {
        return grantedCapabilities.stream()
            .filter(cap -> !cap.isDefaultGranted())
            .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Get default capabilities
     */
    public Set<PathCapability> getDefaultCapabilities() {
        return grantedCapabilities.stream()
            .filter(PathCapability::isDefaultGranted)
            .collect(java.util.stream.Collectors.toSet());
    }
    
    // ===== RUNTIME EXPANSION =====
    
    /**
     * Record a runtime permission grant (for audit trail)
     */
    public void addRuntimeGrant(String target, PathCapability.Operation operation, String reason) {
        RuntimePermissionGrant grant = new RuntimePermissionGrant(
            target, operation.name(), reason, System.currentTimeMillis()
        );
        runtimeGrants.add(grant);
    }
    
    /**
     * Get runtime grants
     */
    public List<RuntimePermissionGrant> getRuntimeGrants() {
        return Collections.unmodifiableList(runtimeGrants);
    }
    
    // ===== APPROVAL =====
    
    /**
     * Mark policy as approved by user
     */
    public void approve() {
        this.approved = true;
    }
    
    /**
     * Revoke approval
     */
    public void revoke() {
        this.approved = false;
    }
    
    /**
     * Check if approved
     */
    public boolean isApproved() {
        return approved;
    }
    
    // ===== VALIDATION =====
    
    /**
     * Validate policy consistency
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Check for conflicts
        Map<String, Set<PathCapability.Operation>> pathOperations = new HashMap<>();
        
        for (PathCapability cap : grantedCapabilities) {
            String pattern = cap.getPathPattern().toString();
            pathOperations.computeIfAbsent(pattern, k -> new HashSet<>())
                .addAll(cap.getOperations());
        }
        
        // Could add more validation here if needed
        
        return errors;
    }
    
    // ===== SERIALIZATION =====
    
    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        
        map.put(Keys.NODE_ID, nodeId);
        map.put("package_id", packageId);
        map.put("approved", approved);
        map.put("created_at", createdAt);
        
        // Granted capabilities
        NoteBytesArray capsArr = new NoteBytesArray();
        for (PathCapability cap : grantedCapabilities) {
            capsArr.add(cap.toNoteBytes());
        }
        map.put("granted_capabilities", capsArr);
        
        // Runtime grants
        if (!runtimeGrants.isEmpty()) {
            NoteBytesArray grantsArr = new NoteBytesArray();
            runtimeGrants.forEach(g -> grantsArr.add(g.toNoteBytes()));
            map.put("runtime_grants", grantsArr);
        }
        
        return map.toNoteBytes();
    }
    
    public static NodeSecurityPolicy fromNoteBytes(NoteBytesMap map) {
        
        NoteBytesReadOnly nodeId = map.getReadOnly(Keys.NODE_ID);
        NoteBytesReadOnly packageId = map.getReadOnly(Keys.PACKAGE_ID);
        
        NodeSecurityPolicy policy = new NodeSecurityPolicy(nodeId, packageId);
        
        // Restore approval state
        policy.approved = map.get(Keys.APPROVED).getAsBoolean();
        
        // Restore capabilities (clear defaults first)
        policy.grantedCapabilities.clear();
        NoteBytesArray capsArr = map.get("granted_capabilities").getAsNoteBytesArray();
        for (NoteBytes b : capsArr.getAsArray()) {
            PathCapability cap = PathCapability.fromNoteBytes(b.getAsNoteBytesObject());
            policy.grantedCapabilities.add(cap);
        }
        
        // Restore runtime grants
        NoteBytes grantsBytes = map.get("runtime_grants");
        if (grantsBytes != null) {
            NoteBytesArray arr = grantsBytes.getAsNoteBytesArray();
            for (NoteBytes b : arr.getAsArray()) {
                policy.runtimeGrants.add(RuntimePermissionGrant.fromNoteBytes(
                    b.getAsNoteBytesObject()
                ));
            }
        }
        
        return policy;
    }
    
    // ===== GETTERS =====
    
    public NoteBytesReadOnly getNodeId() { return nodeId; }
    public NoteBytesReadOnly getPackageId() { return packageId; }
    public long getCreatedAt() { return createdAt; }
    
    // ===== NESTED RECORD =====
    
    public record RuntimePermissionGrant(
        String target,      // Path that was accessed
        String operation,   // Operation performed
        String reason,
        long grantedAt
    ) {
        public NoteBytesObject toNoteBytes() {
            NoteBytesMap map = new NoteBytesMap();
            map.put("target", target);
            map.put("operation", operation);
            map.put("reason", reason);
            map.put("granted_at", grantedAt);
            return map.toNoteBytes();
        }
        
        public static RuntimePermissionGrant fromNoteBytes(NoteBytesObject obj) {
            NoteBytesMap map = obj.getAsNoteBytesMap();
            return new RuntimePermissionGrant(
                map.get("target").getAsString(),
                map.get("operation").getAsString(),
                map.get("reason").getAsString(),
                map.get("granted_at").getAsLong()
            );
        }
    }
}