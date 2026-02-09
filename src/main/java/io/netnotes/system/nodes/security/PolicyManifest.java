package io.netnotes.system.nodes.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.*;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.system.CoreConstants;
import io.netnotes.system.nodes.security.PathCapability.MatchMode;

import java.util.*;

/**
 * PolicyManifest - Security policy declared in package manifest
 * 
 * Simplified to just request PathCapabilities.
 * No required/optional distinction - user approves all or denies installation.
 * 
 * Example manifest.json:
 * {
 *   "name": "example-node",
 *   "version": "1.0.0",
 *   "security_policy": {
 *     "requested_capabilities": [
 *       {
 *         "path": "/system/services/*",
 *         "service_names": ["io-daemon"],
 *         "operations": ["MESSAGE", "STREAM"],
 *         "reason": "Secure password input"
 *       },
 *       {
 *         "path": "/system/nodes/*",
 *         "operations": ["MESSAGE"],
 *         "reason": "Communicate with other nodes"
 *       }
 *     ]
 *   }
 * }
 */
public class PolicyManifest {
    
    private final List<PathCapability> requestedCapabilities;

    public PolicyManifest(
            List<PathCapability> requestedCapabilities) {
        this.requestedCapabilities = new ArrayList<>(requestedCapabilities);
    }
    
    // ===== GETTERS =====
    
    public List<PathCapability> getRequestedCapabilities() {
        return Collections.unmodifiableList(requestedCapabilities);
    }
    


    /**
     * Get capabilities that require user approval (not default granted)
     */
    public List<PathCapability> getApprovalRequiredCapabilities() {
        return requestedCapabilities.stream()
            .filter(cap -> !cap.isDefaultGranted())
            .toList();
    }
    
    /**
     * Get capabilities that are granted by default
     */
    public List<PathCapability> getDefaultCapabilities() {
        return requestedCapabilities.stream()
            .filter(PathCapability::isDefaultGranted)
            .toList();
    }
    
    // ===== VALIDATION =====
    
    /**
     * Validate manifest structure
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Validate each capability
        for (PathCapability cap : requestedCapabilities) {
            if (cap.getPathPattern() == null || cap.getPathPattern().isEmpty()) {
                errors.add("Capability missing path pattern");
            }
            if (cap.getOperations().isEmpty()) {
                errors.add("Capability missing operations: " + cap.getPathPattern());
            }
            if (cap.getReason() == null || cap.getReason().isEmpty()) {
                errors.add("Capability missing reason: " + cap.getPathPattern());
            }
        }
    
        
        return errors;
    }
    
    /**
     * Check if manifest requests sensitive paths
     */
    public boolean hasSensitivePaths() {
        for (PathCapability cap : requestedCapabilities) {
            ContextPath pattern = cap.getPathPattern();
            
            // System services are sensitive
            if (pattern.startsWith(CoreConstants.SERVICES_PATH)) {
                return true;
            }

            if (pattern.startsWith(CoreConstants.NODES_PATH)){
                return true;
            }
        }
        return false;
    }
    
    // ===== SERIALIZATION: JSON (External - in package manifest) =====
    
    /**
     * Parse from JSON in package manifest
     */
    public static PolicyManifest fromJson(JsonObject json) {
        List<PathCapability> capabilities = new ArrayList<>();
        
        // Parse requested capabilities
        if (json.has("requested_capabilities") && json.get("requested_capabilities").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("requested_capabilities");
            for (JsonElement elem : arr) {
                JsonObject capJson = elem.getAsJsonObject();
                capabilities.add(parseCapabilityFromJson(capJson));
            }
        }
    
        return new PolicyManifest(capabilities);
    }
    
    private static PathCapability parseCapabilityFromJson(JsonObject json) {
        String pathPattern = json.get("path").getAsString();
        String reason = json.has("reason") ? json.get("reason").getAsString() : "";
        String matchModeString = json.has("matchMode") ? json.get("matchMode").getAsString() : "PREFIX";
        // Parse operations
        Set<PathCapability.Operation> operations = new HashSet<>();
        if (json.has("operations") && json.get("operations").isJsonArray()) {
            JsonArray opsArr = json.getAsJsonArray("operations");
            for (JsonElement elem : opsArr) {
                operations.add(PathCapability.Operation.valueOf(elem.getAsString()));
            }
        }
        
        // Parse service names (for /system/services/* paths)
        List<String> serviceNames = null;
        if (json.has("service_names") && json.get("service_names").isJsonArray()) {
            serviceNames = new ArrayList<>();
            JsonArray namesArr = json.getAsJsonArray("service_names");
            for (JsonElement elem : namesArr) {
                serviceNames.add(elem.getAsString());
            }
        }

        MatchMode matchMode = MatchMode.valueOf(matchModeString.toUpperCase());
        
        return new PathCapability(ContextPath.parse(pathPattern), operations, false, reason, serviceNames, matchMode);
    }
    
    /**
     * Convert to JSON for package manifest
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        
        // Requested capabilities
        JsonArray capsArr = new JsonArray();
        for (PathCapability cap : requestedCapabilities) {
            capsArr.add(capabilityToJson(cap));
        }
        json.add("requested_capabilities", capsArr);
        
        return json;
    }
    
    private JsonObject capabilityToJson(PathCapability cap) {
        JsonObject json = new JsonObject();
        json.addProperty("path", cap.getPathPattern().toString());
        
        // Operations
        JsonArray opsArr = new JsonArray();
        cap.getOperations().forEach(op -> opsArr.add(op.name()));
        json.add("operations", opsArr);
        
        json.addProperty("reason", cap.getReason());
        
        // Service names (if applicable)
        if (cap.getServiceNames() != null && !cap.getServiceNames().isEmpty()) {
            JsonArray namesArr = new JsonArray();
            cap.getServiceNames().forEach(namesArr::add);
            json.add("service_names", namesArr);
        }
        
        return json;
    }
    
    // ===== SERIALIZATION: NoteBytes (Internal storage) =====
    
    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        
        // Requested capabilities
        NoteBytesArray capsArr = new NoteBytesArray();
        requestedCapabilities.forEach(cap -> capsArr.add(cap.toNoteBytes()));
        map.put("requested_capabilities", capsArr);
        
        return map.toNoteBytes();
    }
    
    public static PolicyManifest fromNoteBytes(NoteBytesMap map) {
  
        
        List<PathCapability> capabilities = new ArrayList<>();
        
        // Parse capabilities
        NoteBytes capsBytes = map.get("requested_capabilities");
        if (capsBytes != null) {
            NoteBytesArray arr = capsBytes.getAsNoteBytesArray();
            for (NoteBytes b : arr.getAsArray()) {
                capabilities.add(PathCapability.fromNoteBytes(b.getAsNoteBytesObject()));
            }
        }
        
        
        return new PolicyManifest(capabilities);
    }
    
    // ===== CLUSTER CONFIG =====
    
    
    // ===== BUILDER =====
    
    public static class Builder {
        private final List<PathCapability> capabilities = new ArrayList<>();

        public Builder requestCapability(PathCapability capability) {
            capabilities.add(capability);
            return this;
        }
        
        public Builder requestServiceAccess(Set<PathCapability.Operation> operations, String reason, String... serviceNames) {
            capabilities.add(PathCapability.accessServices(operations, serviceNames));
            return this;
        }
        
        public Builder requestNodeMessaging(String reason) {
            capabilities.add(new PathCapability(
                CoreConstants.NODES_PATH,
                Set.of(PathCapability.Operation.MESSAGE),
                false,
                reason,null,MatchMode.PREFIX
            ));
            return this;
        }
        

        
        public PolicyManifest build() {
            return new PolicyManifest(capabilities);
        }
    }
}