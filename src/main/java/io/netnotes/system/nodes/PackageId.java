package io.netnotes.system.nodes;

import io.netnotes.noteBytes.NoteBytesReadOnly;



/**
 * 1. PACKAGE = Installed code + metadata (immutable after install)
 * 2. PROCESS CONFIG = Namespace + inheritance decisions (set at install)
 * 3. INSTANCE = Running INode (created at load, destroyed at unload)
 * 4. FLOW PATH = Communication routing address (dynamic)
 */


/**
 * PackageId - Identifies installed package
 * This is what goes in InstallationRegistry
 */
public class PackageId {
    private final NoteBytesReadOnly id;
    private final String version;
    
    public PackageId(NoteBytesReadOnly id, String version) {
        this.id = id;
        this.version = version;
    }
    
    public NoteBytesReadOnly getId() { return id; }
    public String getVersion() { return version; }

    
    @Override
    public String toString() {
        return id.getAsString() + "@" + version;
    }

    @Override
    public int hashCode(){
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        return id.equals(obj);
    }
}

