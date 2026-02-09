package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.system.CoreConstants;

/**
 * PackageInfo - Metadata about an AVAILABLE package (from repository)
 * This is NOT installed yet - just information about what's available
 * 
 * SERIALIZATION:
 * - Internal storage: NoteBytes format (via toNoteBytes/fromNoteBytes)
 * - External sources: JSON format (via fromJson when downloading from repos)
 */
public class PackageInfo {
    private final NoteBytesReadOnly packageId;
    private final String name;
    private final String version;
    private final String category;
    private final String description;
    private final String repository;  // Which repo it came from
    private final String downloadUrl;
    private final long size;
    private final PackageManifest manifest;  // JSON manifest

    public PackageInfo(
        NoteBytesReadOnly packageId,
        String name,
        String version,
        String category,
        String description,
        String repository,
        String downloadUrl,
        long size,
        PackageManifest manifest
    ) {
        this.packageId = packageId;
        this.name = name;
        this.version = version;
        this.category = category;
        this.description = description;
        this.repository = repository;
        this.downloadUrl = downloadUrl;
        this.size = size;
        this.manifest = manifest;
    }
    
    // ===== GETTERS =====
    
    public NoteBytesReadOnly getPackageId() { return packageId; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getRepository() { return repository; }
    public String getDownloadUrl() { return downloadUrl; }
    public long getSize() { return size; }
    public PackageManifest getManifest() { return manifest; }

    public ContextPath createInstallPath() {
        return CoreConstants.PACKAGE_STORE_PATH.append(name, version);
    }
    
    // ===== SERIALIZATION =====
    
    /**
     * Serialize to NoteBytes for transmission/storage
     */
    public NoteBytesObject toNoteBytes() {
        return new NoteBytesObject(
            new NoteBytesPair(Keys.PACKAGE_ID, packageId),
            new NoteBytesPair(Keys.NAME, name),
            new NoteBytesPair(Keys.VERSION, version),
            new NoteBytesPair(Keys.CATEGORY, category),
            new NoteBytesPair(Keys.DESCRIPTION, description),
            new NoteBytesPair(NodeConstants.REPOSITORY, repository),
            new NoteBytesPair(NodeConstants.DOWNLOAD_URL, downloadUrl),
            new NoteBytesPair(Keys.SIZE, size),
            new NoteBytesPair(NodeConstants.MANIFEST, manifest.toNoteBytes())
        );
    }
    
    /**
     * Deserialize from NoteBytes
     */
    public static PackageInfo fromNoteBytes(NoteBytesMap map) {
        try {
            NoteBytesReadOnly packageId = map.getReadOnly(Keys.PACKAGE_ID);
            String name = map.get(Keys.NAME).getAsString();
            String version = map.get(Keys.VERSION).getAsString();
            
            // Optional fields with defaults
            NoteBytes categoryBytes = map.get(Keys.CATEGORY);
            String category = categoryBytes != null ? categoryBytes.getAsString() : "uncategorized";
            
            NoteBytes descriptionBytes = map.get(Keys.DESCRIPTION);
            String description = descriptionBytes != null ? descriptionBytes.getAsString() : "";
            
            String repository = map.get(NodeConstants.REPOSITORY).getAsString();
            String downloadUrl = map.get(NodeConstants.DOWNLOAD_URL).getAsString();
            
            NoteBytes sizeBytes = map.get(Keys.SIZE);
            long size = sizeBytes != null ? sizeBytes.getAsLong() : 0;
            
            // Manifest
            PackageManifest manifest = PackageManifest.fromNoteBytes(
                map.get(NodeConstants.MANIFEST).getAsNoteBytesObject()
            );
            
            return new PackageInfo(
                packageId,
                name,
                version,
                category,
                description,
                repository,
                downloadUrl,
                size,
                manifest
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize PackageInfo", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "PackageInfo[%s v%s from %s]",
            name,
            version,
            repository
        );
    }
}