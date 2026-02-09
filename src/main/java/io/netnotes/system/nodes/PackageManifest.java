package io.netnotes.system.nodes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesArray;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;

import java.util.ArrayList;
import java.util.List;

/**
 * PackageManifest - Metadata describing a package
 * 
 * Example manifest.json:
 * {
 *   "name": "example-node",
 *   "version": "1.0.0",
 *   "type": "osgi-bundle",
 *   "entry": "com.example.ExampleNode",
 *   "dependencies": ["org.osgi.core"],
 *   "autoload": false,
 *   "namespace": {
 *     "required": "system-core",      // MUST install in this namespace
 *     "default": "myapp-workspace",   // SUGGESTED namespace (user can override)
 *     "flexible": true                // Can be installed anywhere (default)
 *   }
 * }
 * 
 * Namespace Modes:
 * 1. REQUIRED: Package MUST be in specific namespace (e.g., system components)
 * 2. DEFAULT: Package suggests a namespace but user can override
 * 3. FLEXIBLE: User chooses freely (most common, default if namespace not specified)
 */
public class PackageManifest {
    public static final NoteBytesReadOnly OSGI_BUNDLE = new NoteBytesReadOnly("osgi-bundle");
    
    private final String name;
    private final String version;
    private final String type;          // osgi-bundle, script, native, etc.
    private final String entry;         // Entry point class/file
    private final List<String> dependencies;
    private final boolean autoload;
    private final NamespaceRequirement namespaceRequirement;
    private final NoteBytesMap metadata; // Additional metadata
    
    public PackageManifest(
        String name,
        String version,
        String type,
        String entry,
        List<String> dependencies,
        boolean autoload,
        NamespaceRequirement namespaceRequirement,
        JsonObject metadata
    ) { 
        this(name, version, type, entry, dependencies, autoload, 
             namespaceRequirement, NoteBytes.fromJson(metadata).getAsMap()); 
    }

    public PackageManifest(
        String name,
        String version,
        String type,
        String entry,
        List<String> dependencies,
        boolean autoload,
        NamespaceRequirement namespaceRequirement,
        NoteBytesMap metadata
    ) {
        this.name = name;
        this.version = version;
        this.type = type;
        this.entry = entry;
        this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        this.autoload = autoload;
        this.namespaceRequirement = namespaceRequirement != null ? 
            namespaceRequirement : NamespaceRequirement.flexible();
        this.metadata = metadata;
    }
    
    // ===== GETTERS =====
    
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getType() { return type; }
    public String getEntry() { return entry; }
    public List<String> getDependencies() { return dependencies; }
    public boolean isAutoload() { return autoload; }
    public NamespaceRequirement getNamespaceRequirement() { return namespaceRequirement; }
    public NoteBytesMap getMetadata() { return metadata; }
    
    // ===== NAMESPACE HELPERS =====
    
    /**
     * Check if user can choose namespace freely
     */
    public boolean allowsNamespaceChoice() {
        return namespaceRequirement.mode() != NamespaceMode.REQUIRED;
    }
    
    /**
     * Get the namespace this package must/should use
     */
    public NoteBytesReadOnly getNamespace() {
        return namespaceRequirement.namespace();
    }
    
    /**
     * Check if this package requires a specific namespace
     */
    public boolean requiresSpecificNamespace() {
        return namespaceRequirement.mode() == NamespaceMode.REQUIRED;
    }
    
    // ===== SERIALIZATION =====
    
    /**
     * Parse manifest from JSON
     */
    public static PackageManifest fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        String version = json.get("version").getAsString();
        String type = json.has("type") ? 
            json.get("type").getAsString() : "osgi-bundle";
        String entry = json.has("entry") ? 
            json.get("entry").getAsString() : null;
       
        List<String> dependencies = new ArrayList<>();
        if (json.has("dependencies") && json.get("dependencies").isJsonArray()) {
            json.getAsJsonArray("dependencies").forEach(e -> 
                dependencies.add(e.getAsString()));
        }
        
        boolean autoload = json.has("autoload") && 
            json.get("autoload").getAsBoolean();

        // Parse namespace requirement
        NamespaceRequirement namespaceReq = NamespaceRequirement.flexible();
        if (json.has("namespace")) {
            JsonObject nsJson = json.getAsJsonObject("namespace");
            
            if (nsJson.has("required")) {
                // Required namespace - no choice
                String required = nsJson.get("required").getAsString();
                namespaceReq = NamespaceRequirement.required(
                    new NoteBytesReadOnly(required)
                );
            } else if (nsJson.has("default")) {
                // Default namespace - can override
                String defaultNs = nsJson.get("default").getAsString();
                namespaceReq = NamespaceRequirement.withDefault(
                    new NoteBytesReadOnly(defaultNs)
                );
            } else if (nsJson.has("flexible") && nsJson.get("flexible").getAsBoolean()) {
                // Explicitly flexible
                namespaceReq = NamespaceRequirement.flexible();
            }
        }

        return new PackageManifest(
            name, 
            version, 
            type, 
            entry, 
            dependencies, 
            autoload,
            namespaceReq,
            json
        );
    }
    
    /**
     * Convert to JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("version", version);
        json.addProperty("type", type);
        if (entry != null) json.addProperty("entry", entry);
        
        if (!dependencies.isEmpty()) {
            JsonArray deps = new JsonArray();
            dependencies.forEach(deps::add);
            json.add("dependencies", deps);
        }
        
        json.addProperty("autoload", autoload);
        
        // Add namespace requirement
        JsonObject nsJson = new JsonObject();
        switch (namespaceRequirement.mode()) {
            case REQUIRED:
                nsJson.addProperty("required", namespaceRequirement.namespace().toString());
                break;
            case DEFAULT:
                nsJson.addProperty("default", namespaceRequirement.namespace().toString());
                break;
            case FLEXIBLE:
                nsJson.addProperty("flexible", true);
                break;
        }
        json.add("namespace", nsJson);
        
        // Merge additional metadata
        if (metadata != null) {
            for (NoteBytes keyBytes : metadata.keySet()) {
                String key = keyBytes.getAsString();
                if (!json.has(key)) {
                    json.add(key, NoteBytes.toJson(metadata.get(key)));
                }
            }
        }
        
        return json;
    }

    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        map.put(Keys.NAME, name);
        map.put(Keys.VERSION, version);
        map.put(Keys.TYPE, type);
        if (entry != null) map.put(Keys.ENTRY, entry);
        
        if (!dependencies.isEmpty()) {
            map.put(Keys.DEPENDENCIES, NoteStringArrayReadOnly.fromList(dependencies));
        }
        
        map.put(Keys.AUTOLOAD, autoload);
        map.put("namespace_requirement", namespaceRequirement.toNoteBytes());
        
        // Merge additional metadata
        if (metadata != null) {
            for (NoteBytes key : metadata.keySet()) {
                if (!map.has(key)) {
                    map.put(key, metadata.get(key));
                }
            }
        }
        
        return map.toNoteBytes();
    }

    public static PackageManifest fromNoteBytes(NoteBytesObject obj) {
        return fromNoteBytes(obj.getAsMap());
    }

    public static PackageManifest fromNoteBytes(NoteBytesMap map) {
        String name = map.get(Keys.NAME).getAsString();
        String version = map.get(Keys.VERSION).getAsString();
        String type = map.getOrDefault(Keys.TYPE, OSGI_BUNDLE).getAsString();
        NoteBytes entryBytes = map.get(Keys.ENTRY);
        String entry = entryBytes != null ? entryBytes.getAsString() : null;
        
        List<String> dependencies = new ArrayList<>();
        NoteBytes dependenciesBytes = map.get(Keys.DEPENDENCIES);

        if (dependenciesBytes != null && 
            dependenciesBytes.getType() == NoteBytesMetaData.NOTE_BYTES_ARRAY_TYPE) {
            NoteBytesArray noteBytesArray = dependenciesBytes.getAsNoteBytesArray();
            NoteBytes[] array = noteBytesArray.getAsArray();

            for (NoteBytes v : array) {
                dependencies.add(v.getAsString());
            }
        }
        
        NoteBytes autoloadBytes = map.get(Keys.AUTOLOAD);
        boolean autoload = autoloadBytes != null ? autoloadBytes.getAsBoolean() : false;

        // Parse namespace requirement
        NamespaceRequirement namespaceReq = NamespaceRequirement.flexible();
        NoteBytes nsReqBytes = map.get("namespace_requirement");
        if (nsReqBytes != null) {
            namespaceReq = NamespaceRequirement.fromNoteBytes(
                nsReqBytes.getAsNoteBytesMap()
            );
        }

        return new PackageManifest(
            name, 
            version, 
            type, 
            entry, 
            dependencies, 
            autoload,
            namespaceReq,
            map
        );
    }
    
    // ===== UTILITY =====
    
    @Override
    public String toString() {
        return String.format(
            "PackageManifest[name=%s, version=%s, type=%s, namespace=%s]",
            name, version, type, namespaceRequirement.mode()
        );
    }
    
    // ===== NAMESPACE REQUIREMENT =====
    
    /**
     * Describes namespace requirements for a package
     */
    public static record NamespaceRequirement(
        NamespaceMode mode,
        NoteBytesReadOnly namespace
    ) {
        /**
         * Package MUST be installed in specific namespace
         */
        public static NamespaceRequirement required(NoteBytesReadOnly namespace) {
            return new NamespaceRequirement(NamespaceMode.REQUIRED, namespace);
        }
        
        /**
         * Package suggests a namespace but user can override
         */
        public static NamespaceRequirement withDefault(NoteBytesReadOnly namespace) {
            return new NamespaceRequirement(NamespaceMode.DEFAULT, namespace);
        }
        
        /**
         * User can choose any namespace freely
         */
        public static NamespaceRequirement flexible() {
            return new NamespaceRequirement(NamespaceMode.FLEXIBLE, null);
        }
        
        public NoteBytesObject toNoteBytes() {
            NoteBytesMap map = new NoteBytesMap();
            map.put("mode", mode.name());
            if (namespace != null) {
                map.put("namespace", namespace);
            }
            return map.toNoteBytes();
        }
        
        public static NamespaceRequirement fromNoteBytes(NoteBytesMap map) {
            NamespaceMode mode = NamespaceMode.valueOf(
                map.get("mode").getAsString()
            );
            
            NoteBytesReadOnly namespace = map.getReadOnly("namespace");

            
            return new NamespaceRequirement(mode, namespace);
        }
    }
    
    public enum NamespaceMode {
        /**
         * Package MUST be in specific namespace (no choice)
         * Example: system components that coordinate
         */
        REQUIRED,
        
        /**
         * Package suggests namespace but user can override
         * Example: workspace apps that work better together
         */
        DEFAULT,
        
        /**
         * User chooses freely (most packages)
         */
        FLEXIBLE
    }
}