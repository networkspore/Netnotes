package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.system.nodes.security.NodeSecurityPolicy;


/**
 * InstalledPackage - Metadata about an INSTALLED package
 * 
 * SERIALIZATION:
 * - Internal storage: NoteBytes format (via toNoteBytes/fromNoteBytes)
 * - External sources: JSON format (via fromJson when downloading)
 * 
 * This is stored in /system/nodes/registry/installed as NoteBytesMap
 */
public class InstalledPackage {
    private final PackageId packageId;
    private final String name;
    private final String description;
    private final PackageManifest manifest;
    
    // Process configuration (decided at install)
    private final ProcessConfig processConfig;
    
    // Security (decided at install)
    private final NodeSecurityPolicy securityPolicy;
    
    // Install metadata
    private final String repository;
    private final long installedDate;

    private final ContextPath installPath;
    
    public InstalledPackage(
        PackageId packageId,
        String name,
        String description,
        PackageManifest manifest,
        ProcessConfig processConfig,
        NodeSecurityPolicy securityPolicy,
        String repository,
        long installedDate,
        ContextPath installedPath
    ) {
        this.packageId = packageId;
        this.name = name;
        this.description = description;
        this.manifest = manifest;
        this.processConfig = processConfig;
        this.securityPolicy = securityPolicy;
        this.repository = repository;
        this.installedDate = installedDate;
        this.installPath = installedPath;
    }
    
    public PackageId getPackageId() { return packageId; }
    public NoteBytesReadOnly getProcessId() { return processConfig.getProcessId(); }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public PackageManifest getManifest() { return manifest; }
    public ProcessConfig getProcessConfig() { return processConfig; }
    public NodeSecurityPolicy getSecurityPolicy() { return securityPolicy; }
    public String getRepository() { return repository; }
    public long getInstalledDate() { return installedDate; }
    public String getVersion() { return packageId.getVersion(); }
    public ContextPath getInstallPath() { return installPath; }
    public String getPluginUrl() { return "notefile://" + installPath.toString(); }

    /**
     * Serialize to NoteBytes for storage
     */
    public NoteBytesObject toNoteBytes() {

        //Alternative use NoteBytesMap.put then NoteBytesObject obj = NoteBytesMap.toNoteBytes() 

        return new NoteBytesObject(
            new NoteBytesPair(Keys.PACKAGE_ID, packageId.getId()),
            new NoteBytesPair(Keys.NAME, name),
            new NoteBytesPair(Keys.VERSION, packageId.getVersion()),
            new NoteBytesPair(Keys.DESCRIPTION, description),
            // Manifest
            new NoteBytesPair(NodeConstants.MANIFEST, manifest.toNoteBytes()),
            // Process configuration
            new NoteBytesPair(NodeConstants.PROCESS_CONFIG, processConfig.toNoteBytes()),
            // Security policy
            new NoteBytesPair(NodeConstants.SECURITY_POLICY, securityPolicy.toNoteBytes()),
            // Install metadata
            new NoteBytesPair(NodeConstants.REPOSITORY, repository),
            new NoteBytesPair(NodeConstants.INSTALLED_DATE, installedDate),
            new NoteBytesPair(NodeConstants.INSTALLED_PATH, installPath.getSegments()) //ContextPath
        );
    }
    
    /**
     * Deserialize from NoteBytes
     */
    public static InstalledPackage fromNoteBytes(NoteBytesMap map) {
        try {
            // Package identity
            NoteBytesReadOnly pkgIdBytes = map.getReadOnly(Keys.PACKAGE_ID);
            String name = map.get(Keys.NAME).getAsString();
            String version = map.get(Keys.VERSION).getAsString();
            String description = map.get(Keys.DESCRIPTION).getAsString();
            
            PackageId packageId = new PackageId(pkgIdBytes, version);
            
            // Manifest
            PackageManifest manifest = PackageManifest.fromNoteBytes(
                map.get(NodeConstants.MANIFEST).getAsNoteBytesObject()
            );
            
            // Process configuration
            ProcessConfig processConfig = ProcessConfig.fromNoteBytes(
                map.get(NodeConstants.PROCESS_CONFIG).getAsNoteBytesMap()
            );
            
            // Security policy
            NodeSecurityPolicy securityPolicy = NodeSecurityPolicy.fromNoteBytes(
                map.get(NodeConstants.SECURITY_POLICY).getAsNoteBytesMap()
            );
            
            // Install metadata
            String repository = map.get(NodeConstants.REPOSITORY).getAsString();
            long installedDate = map.get(NodeConstants.INSTALLED_DATE).getAsLong();
            ContextPath installPath = ContextPath.of(map.get("install_path").getAsNoteBytesArrayReadOnly());
            
            return new InstalledPackage(
                packageId,
                name,
                description,
                manifest,
                processConfig,
                securityPolicy,
                repository,
                installedDate,
                installPath
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize RefactoredInstalledPackage", e);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "InstalledPackage[%s v%s, process=%s]",
            name,
            packageId.getVersion(),
            processConfig.getProcessId()
        );
    }
}


