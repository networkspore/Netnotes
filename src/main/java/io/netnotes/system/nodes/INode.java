package io.netnotes.system.nodes;

import java.util.concurrent.CompletableFuture;

import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.noteFiles.NoteFileServiceInterface;


/**
 * INode - Interface for Node initialization
 */
public interface INode {
    
   
    /**
     * Initialize Node with interfaces for both capabilities
     * 
     * @param nodeDataInterface File access 
     * @param processInterface Process network access 
     */
    CompletableFuture<Void> initialize(
        NoteFileServiceInterface nodeDataInterfacem,
        ProcessRegistryInterface processInterface
    );
    
    
    CompletableFuture<Void> shutdown();
}