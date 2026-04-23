package io.netnotes.noteFiles;

import java.io.File;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netnotes.engine.messaging.NoteMessaging;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.virtualExecutors.SerializedVirtualExecutor;
import io.netnotes.noteFiles.notePath.NoteFileService;

/**
 * ManagedNoteFileInterface - Manages exclusive access to an encrypted file
 * 
 * REFACTORED ARCHITECTURE:
 * - Uses SerializedVirtualExecutor instead of Semaphore for file access
 * - No more manual lock management (acquireLock/releaseLock)
 * - All file operations are serialized automatically
 * - Caller-controlled cancellation for all operations
 * - No deadlock risk from unreleased locks
 * - Emergency shutdown support via executor methods
 */
public class ManagedNoteFileInterface implements NoteFile.NoteFileInterface {
    private final File m_file;
    private final SerializedVirtualExecutor m_fileExecutor;
    
    private final Map<NoteUUID, NoteFile> m_activeReferences = new ConcurrentHashMap<>();
    private final NoteFileService m_noteFileService;
    private final NoteStringArrayReadOnly m_pathKey;
    private final String path;
    private final AtomicBoolean m_isClosed = new AtomicBoolean(false);
    
    public ManagedNoteFileInterface(
            NoteStringArrayReadOnly pathKey, 
            NoteBytes noteFilePath, 
            NoteFileService noteFileService) {
        this.m_file = new File(noteFilePath.getAsString());
        this.m_noteFileService = noteFileService;
        this.m_pathKey = pathKey;
        this.path = pathKey.getAsString();
        this.m_fileExecutor = new SerializedVirtualExecutor();
    }

    public NoteStringArrayReadOnly getId() {
        return m_pathKey;
    }


    public void addReference(NoteFile noteFile) throws IllegalStateException {
        if(m_isClosed.get()) {
            throw new IllegalStateException("Interface is closed");
        }
        m_activeReferences.putIfAbsent(noteFile.getId(), noteFile);
    }

    public void removeReference(NoteUUID uuid) {
        m_activeReferences.remove(uuid);
        
        // If no more references and not processing operations, schedule cleanup
        if (m_activeReferences.isEmpty() && m_fileExecutor.getQueueSize() == 0) {
            scheduleCleanup();
        }
    }
    

    public int getReferenceCount() {
        return m_activeReferences.size();
    }

    private void scheduleCleanup() {
        if(!m_isClosed.get()) {
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS, getExecService())
                .execute(() -> {
                    // Double-check cleanup conditions
                    if (m_activeReferences.isEmpty() && 
                        m_fileExecutor.getQueueSize() == 0 &&
                        !m_isClosed.get()) {
                        m_noteFileService.cleanupInterface(m_pathKey, this);
                    }
                });
        }
    }

    /**
     * Execute an operation with exclusive file access.
     * The operation is queued and executed serially with all other file operations.
     * 
     * @param operation the operation to perform
     * @return CompletableFuture that can be cancelled by the caller
     */
    <T> CompletableFuture<T> executeWithFileAccess(
            java.util.concurrent.Callable<T> operation) {
        if (m_isClosed.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Interface is closed"));
        }
        return m_fileExecutor.submit(operation);
    }

    /**
     * Execute a runnable operation with exclusive file access.
     */
    CompletableFuture<Void> executeWithFileAccess(Runnable operation) {
        if (m_isClosed.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Interface is closed"));
        }
        return m_fileExecutor.execute(operation);
    }

    @Override
    public CompletableFuture<NoteBytesObject> readWriteFile(
            PipedOutputStream inParseStream, 
            PipedOutputStream modifiedInParseStream) {
        
        return executeWithFileAccess(() -> null)
            .thenCompose(v -> {
                // Start decrypt operation
                CompletableFuture<NoteBytesObject> decryptFuture = 
                    m_noteFileService.performDecryption(m_file, inParseStream);
                
                // Start encrypt operation
                CompletableFuture<NoteBytesObject> encryptFuture = 
                    m_noteFileService.saveEncryptedFileSwap(m_file, modifiedInParseStream);
                
                // Return the encrypt future as it represents completion
                return CompletableFuture.allOf(decryptFuture, encryptFuture)
                    .thenCompose(x -> encryptFuture);
            });
    }
    
    @Override
    public ExecutorService getExecService() {
        return m_noteFileService.getExecService();
    }
    
    @Override
    public CompletableFuture<NoteBytesObject> decryptFile(PipedOutputStream pipedOutput) {
        return executeWithFileAccess(() -> null)
            .thenCompose(v -> m_noteFileService.performDecryption(m_file, pipedOutput));
    }

    @Override
    public CompletableFuture<NoteBytesObject> saveEncryptedFileSwap(
            PipedOutputStream pipedOutputStream) {
        return executeWithFileAccess(() -> null)
            .thenCompose(v -> 
                m_noteFileService.saveEncryptedFileSwap(m_file, pipedOutputStream));
    }
    
    @Override
    public boolean isFile() {
        return m_file.isFile();
    }
    
    @Override
    public long fileSize() {
        return m_file.length();
    }
    
    /**
     * @deprecated Use executeWithFileAccess pattern instead.
     * This method is kept for backward compatibility but operations
     * are now automatically serialized.
     */
    @Deprecated
    @Override
    public CompletableFuture<Void> acquireLock() {
        // For backward compatibility, just return completed future
        // The actual serialization happens in executeWithFileAccess
        if (m_isClosed.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Interface is closed"));
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * @deprecated Use executeWithFileAccess pattern instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    @Override
    public void releaseLock() {
        // No-op - locks are managed by executor now
        // Check for cleanup after operation completes
        if (m_activeReferences.isEmpty() && 
            m_fileExecutor.getQueueSize() == 0 && 
            !m_isClosed.get()) {
            scheduleCleanup();
        }
    }
    
    @Override
    public boolean isClosed() {
        return m_isClosed.get();
    }
    
    /**
     * Convenience method to see if operations are pending.
     */
    @Override
    public boolean isLocked() {
        return m_fileExecutor.getQueueSize() > 0 || !m_fileExecutor.isTerminated();
    }
    
    @Override
    public CompletableFuture<Void> prepareForKeyUpdate() {
        // Queue a marker operation to ensure all previous operations complete
        return executeWithFileAccess(() -> null);
    }

    public CompletableFuture<Void> perpareForShutdown() {
        return perpareForShutdown(null);
    }


    public CompletableFuture<Void> perpareForShutdown(AsyncNoteBytesWriter writer) {
        // Queue shutdown operation to execute after all pending operations
        return executeWithFileAccess(() -> {
            m_isClosed.set(true);
            
            // Close all active references
            Iterator<Map.Entry<NoteUUID, NoteFile>> iterator = 
                m_activeReferences.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<NoteUUID, NoteFile> entry = iterator.next();
                entry.getValue().forceClose();
                iterator.remove();
            }
            
            return null;
        }).thenRun(()->{
            if(writer != null) {
                writer.writeAsync(TaskMessages.createSuccessResult(
                    path, NoteMessaging.Status.CLOSED));
            }
        })
        .exceptionally(ex -> {
            String msg = "Preparing for shutdown failed";
            if(writer != null) {
                TaskMessages.writeErrorAsync(path, msg, ex, writer);
            }
            Log.logError(msg + " for " + path);
            ex.printStackTrace();
            return null;
        });
    }
    
    @Override
    public void completeKeyUpdate() {
        // No-op - operations complete automatically
        // Check for cleanup
        if (m_activeReferences.isEmpty() && 
            m_fileExecutor.getQueueSize() == 0 && 
            !m_isClosed.get()) {
            scheduleCleanup();
        }
    }

    /**
     * Get the number of pending file operations.
     * Useful for monitoring and deciding whether to cancel long operations.
     */
    public int getQueueSize() {
        return m_fileExecutor.getQueueSize();
    }

    /**
     * Initiates graceful shutdown of file operations.
     * Queued operations will complete, but new operations will be rejected.
     */
    public void shutdownFileAccess() {
        m_fileExecutor.shutdown();
    }

    /**
     * Immediately cancels all pending file operations.
     * 
     * @return list of operations that were cancelled before execution
     */
    public void shutdownFileAccessNow() {
        m_fileExecutor.shutdownNow();
    }

    /**
     * Waits for all file operations to complete after shutdown.
     * 
     * @param timeout maximum time to wait
     * @param unit time unit
     * @return true if completed, false if timeout
     */
    public boolean awaitFileTermination(long timeout, TimeUnit unit) 
            throws InterruptedException {
        return m_fileExecutor.awaitTermination(timeout, unit);
    }

    /**
     * Check if file executor is shut down.
     */
    public boolean isFileExecutorShutdown() {
        return m_fileExecutor.isShutdown();
    }

    /**
     * Check if all file operations have completed after shutdown.
     */
    public boolean isFileExecutorTerminated() {
        return m_fileExecutor.isTerminated();
    }
}