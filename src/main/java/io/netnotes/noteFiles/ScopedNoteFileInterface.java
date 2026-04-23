package io.netnotes.noteFiles;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.engine.virtualExecutors.VirtualExecutors;

/**
 * ScopedAppDataInterface - Path-based access control
 * 
 * - Interface lives at specific paths
 * - Access control via hop validation
 * - Paths determine what's reachable
 * 
 * Example:
 *   Interface at: /system/nodes/runtime/database-node
 *   
 *   Request: config/settings (relative)
 *   → Resolves to: /system/nodes/runtime/database-node/config/settings
 *   → canReach? Yes (within basePath)
 *   
 *   Request: /user/nodes/database-node/exports/data
 *   → canReach from basePath? Check hops...
 *   → ALLOWED (reachable from altPath)
 *   
 *   Request: /system/nodes/runtime/other-node/data
 *   → canReach from basePath? Check hops...
 *     → Ownership check fails (database-node ≠ other-node)
 *   → DENIED
 */
public class ScopedNoteFileInterface implements NoteFileServiceInterface {
    
    private final ContextPath dataRootPath;     // DataRoot
    private final NoteFileServiceInterface fileInterface;
    /**
     * Constructor - paths determine access
     * 
     * @param noteFileService File service for actual file operations
     * @param dataRootPath Primary path where this interface lives
     */
    public ScopedNoteFileInterface(
        NoteFileServiceInterface fileInterface,
        ContextPath dataRootPath
    ) {
        
        if (fileInterface == null) {
            throw new IllegalArgumentException("NoteFileServiceInterface cannot be null");
        }
        
        if (dataRootPath == null) {
            throw new IllegalArgumentException("basePath cannot be null");
        }

        if(fileInterface.getDataRootPath().size() > 0 && !dataRootPath.startsWith(fileInterface.getDataRootPath())){
            throw new IllegalArgumentException("basePath must be within parent interface base path");
        }
     
        this.fileInterface = fileInterface;
        this.dataRootPath = dataRootPath;
    }
    
    // =========================================================================
    // PUBLIC API
    // =========================================================================
    
    @Override
    public CompletableFuture<NoteFile> getNoteFile(ContextPath requestedPath) {
        if (requestedPath == null) {
            return  CompletableFuture.failedFuture(new CompletionException("Invalid path",
                new NullPointerException("Path is null")));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            // Resolve and validate path via hop validation
            PathResolution resolution = resolvePath(requestedPath);
            
            if (resolution == null || !resolution.allowed) {
                throw new CompletionException("Access denied", new SecurityException(String.format(
                    "Cannot access '%s' from base '%s'%s",
                    requestedPath,
                    dataRootPath
                )));
            }
            
            return resolution.resolvedPath;
            
        }, VirtualExecutors.getVirtualExecutor())
        .thenCompose(path -> fileInterface.getNoteFile(path));
    }

   @Override
    public CompletableFuture<Void> deleteNoteFile(ContextPath deletePath, boolean recurrsive, AsyncNoteBytesWriter progress) {
        if (deletePath == null) {
            return  CompletableFuture.failedFuture(new CompletionException("Invalid path",
                new NullPointerException("Path is null")));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            
            // Resolve and validate path via hop validation
            PathResolution resolution = resolvePath(deletePath);
            
            if (resolution == null || !resolution.allowed) {
                throw new CompletionException("Access denied", new SecurityException(String.format(
                    "Cannot access '%s' from base '%s'%s",
                    deletePath,
                    dataRootPath
                )));
            }
            
            return resolution.resolvedPath;
            
        }, VirtualExecutors.getVirtualExecutor())
        .thenCompose(path -> fileInterface.deleteNoteFile(deletePath, recurrsive, progress));
    }
    

    // =========================================================================
    // PATH RESOLUTION & HOP VALIDATION
    // =========================================================================
    
    /**
     * Resolve requested path to absolute path
     * 
     * 
     * @param requestedPath Path requested by caller
     * @return PathResolution with resolved path, or null if denied
     */
    private PathResolution resolvePath(ContextPath requestedPath) {
        
        // === ABSOLUTE PATH ===
        // Check if reachable from basePath
        if (dataRootPath.canReach(requestedPath)) {
            return new PathResolution(requestedPath, true);
        }

        
        return new PathResolution(requestedPath, false);
    }
    
    // =========================================================================
    // HELPER CLASSES
    // =========================================================================
    
    /**
     * Result of path resolution
     */
    private static class PathResolution {
        final ContextPath resolvedPath;
        final boolean allowed;
        
        PathResolution(ContextPath resolvedPath, boolean allowed) {
            this.resolvedPath = resolvedPath;
            this.allowed = allowed;
        }
    }
    

    public ContextPath getDataRootPath() {
        return dataRootPath;
    }

    @Override
    public String toString() {
        return String.format(
            "ScopedInterface[base=%s]",
            dataRootPath
        );
    }

    
}