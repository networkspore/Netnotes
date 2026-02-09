package io.netnotes.system.nodes;

import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;

/**
 * Repository - A source of packages (like apt repository)
 * 
 * STORAGE:
 * Stored in /system/nodes/repositories/sources as NoteBytesMap
 * 
 * Examples:
 * - Official Netnotes repo (GitHub)
 * - User's personal GitHub repo
 * - Local file repository
 * - Third-party community repo
 * 
 * The repository URL points to a packages.json file with this structure:
 * {
 *   "repository": "Official Netnotes",
 *   "packages": [
 *     {
 *       "id": "example-node",
 *       "name": "Example Node",
 *       "version": "1.0.0",
 *       "download_url": "https://github.com/.../package.jar",
 *       "manifest": { ... }
 *     }
 *   ]
 * }
 */
public class Repository {
    private final NoteBytesReadOnly id;
    private final String name;
    private final String url;
    private final String keyUrl;  // Optional: GPG key URL for verification
    private boolean enabled;
    
    public Repository(NoteBytesReadOnly id, String name, String url, String keyUrl, boolean enabled) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.keyUrl = keyUrl;
        this.enabled = enabled;
    }
    
    // ===== GETTERS =====
    
    public NoteBytesReadOnly getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getKeyUrl() { return keyUrl; }
    public boolean isEnabled() { return enabled; }
    public boolean hasKey() { return keyUrl != null && !keyUrl.isEmpty(); }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // ===== INTERNAL SERIALIZATION (NoteBytes) =====
    
    /**
     * Serialize to NoteBytes format for internal storage
     */
    public NoteBytesObject toNoteBytes() {
        NoteBytesMap map = new NoteBytesMap();
        
        map.put(Keys.ID, id);
        map.put(Keys.NAME, name);
        map.put(Keys.URL, url);
        
        if (keyUrl != null && !keyUrl.isEmpty()) {
            map.put(new NoteBytes("key_url"), new NoteBytes(keyUrl));
        }
        
        map.put(Keys.ENABLED, new NoteBytes(enabled));
        
        return map.toNoteBytes();
    }
    
    /**
     * Deserialize from NoteBytes format
     */
    public static Repository fromNoteBytes(NoteBytesMap map) {
        try {
            NoteBytesReadOnly id = map.getReadOnly(Keys.ID);
            String name = map.get(Keys.NAME).getAsString();
            String url = map.get(Keys.URL).getAsString();
            
            String keyUrl = null;
            var keyUrlBytes = map.get(new NoteBytes("key_url"));
            if (keyUrlBytes != null) {
                keyUrl = keyUrlBytes.getAsString();
            }
            
            boolean enabled = map.get(Keys.ENABLED).getAsBoolean();
            
            return new Repository(id, name, url, keyUrl, enabled);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Repository from NoteBytes", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "Repository[id=%s, name=%s, enabled=%s]",
            id, name, enabled
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Repository)) return false;
        Repository other = (Repository) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}