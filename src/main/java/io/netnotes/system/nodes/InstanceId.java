package io.netnotes.system.nodes;

import io.netnotes.engine.utils.noteBytes.NoteUUID;

/**
 * InstanceId - Identifies a RUNNING INode
 * 
 * CREATED AT LOAD TIME, DESTROYED AT UNLOAD
 * 
 * Multiple instances of same package can run if processId differs:
 * - Package "chat-bot" with processId "workspace-1" -> instance-uuid-1
 * - Package "chat-bot" with processId "workspace-2" -> instance-uuid-2
 */
public class InstanceId {
    private final NoteUUID uuid;
    
    private InstanceId(NoteUUID uuid) {
        this.uuid = uuid;
    }
    
    public static InstanceId generate() {
        return new InstanceId(NoteUUID.createLocalUUID128());
    }
    
    public static InstanceId fromString(String str) {
        return new InstanceId(NoteUUID.fromNoteUUIDString(str));
    }
    
    public NoteUUID getUuid() { return uuid; }
    
    @Override
    public String toString() {
        return uuid.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        return uuid.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}


