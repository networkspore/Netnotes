package io.netnotes.noteFiles.notePath;

import java.io.File;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import io.netnotes.noteFiles.DiskSpaceValidation;
import io.netnotes.noteFiles.FileStreamUtils;
import io.netnotes.system.SettingsData;
import io.netnotes.system.SettingsData.InvalidPasswordException;
import io.netnotes.engine.crypto.HashServices;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.ProgressMessage;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.IntCounter;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.NoteBytesReader;

import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.engine.utils.virtualExecutors.SerializedVirtualExecutor;

/**
 * Factory for managing NoteFile operations with serialized access to the ledger file.
 * Uses SerializedVirtualExecutor to ensure exclusive, ordered access to the ledger
 * while supporting caller-controlled cancellation for long-running operations.
 */
public class NotePathFactory {
   
    private final ExecutorService m_execService;
    private final ScheduledExecutorService m_schedualedExecutor;
    private final SerializedVirtualExecutor m_ledgerExecutor;
    private final SettingsData m_settingsData;

    public NotePathFactory(SettingsData settingsData){
        m_execService = Executors.newVirtualThreadPerTaskExecutor();
        m_schedualedExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
        m_ledgerExecutor = new SerializedVirtualExecutor();
        m_settingsData = settingsData;
    }

    public ScheduledExecutorService getScheduledExecutor(){
        return m_schedualedExecutor;
    }

    public ExecutorService getExecService(){
        return m_execService;
    }

    /**
     * Returns the ledger executor for monitoring or advanced control.
     * Callers can check queue size or setup custom cancellation logic.
     */
    public SerializedVirtualExecutor getLedgerExecutor() {
        return m_ledgerExecutor;
    }

    /**
     * Execute an operation with exclusive access to the ledger.
     * The operation runs serially with all other ledger operations.
     * 
     * @param operation the operation to perform with ledger access
     * @return CompletableFuture that can be cancelled by the caller
     */
    public <T> CompletableFuture<T> executeWithLedgerAccess(java.util.concurrent.Callable<T> operation) {
        return m_ledgerExecutor.submit(operation);
    }

    /**
     * Execute a runnable operation with exclusive access to the ledger.
     * 
     * @param operation the operation to perform with ledger access
     * @return CompletableFuture that can be cancelled by the caller
     */
    public CompletableFuture<Void> executeWithLedgerAccess(Runnable operation) {
        return m_ledgerExecutor.execute(operation);
    }

    /**
     * Initiates graceful shutdown of ledger operations.
     * Queued operations will complete, but new operations will be rejected.
     */
    public void shutdownLedgerAccess() {
        m_ledgerExecutor.shutdown();
    }

    /**
     * Immediately cancels all pending ledger operations and attempts to
     * interrupt the currently executing operation.
     * 
     * @return list of operations that were cancelled before execution
     */
    public void shutdownLedgerAccessNow() {
        m_ledgerExecutor.shutdownNow();
    }

    /**
     * Waits for all ledger operations to complete after shutdown.
     * 
     * @param timeout maximum time to wait
     * @param unit time unit
     * @return true if completed, false if timeout
     */
    public boolean awaitLedgerTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return m_ledgerExecutor.awaitTermination(timeout, unit);
    }

    /**
     * Returns the number of operations waiting for ledger access.
     * Useful for monitoring and deciding whether to cancel long operations.
     */
    public int getLedgerQueueSize() {
        return m_ledgerExecutor.getQueueSize();
    }

    /**
     * Check if ledger executor is shut down.
     */
    public boolean isLedgerShutdown() {
        return m_ledgerExecutor.isShutdown();
    }

    /**
     * Check if all ledger operations have completed after shutdown.
     */
    public boolean isLedgerTerminated() {
        return m_ledgerExecutor.isTerminated();
    }

    public static File generateNewDataFile(File dataDir) {     
        String encodedUUID = NoteUUID.createSafeUUID128();
        File dataFile = new File(dataDir.getAbsolutePath() + "/" + encodedUUID + ".dat");
        return dataFile;
    }

    public File getFilePathLedger() {
        return SettingsData.getIdDataFile();
    }

    public SettingsData getSettingsData(){
        return m_settingsData;
    }

    private SecretKey getSecretKey(){
        return getSettingsData().getSecretKey();
    }

    public CompletableFuture<NoteBytesObject> performDecryption(
        File file, 
        PipedOutputStream pipedOutput
    ){
        return FileStreamUtils.performDecryption(file, pipedOutput, getSecretKey(), getExecService());
    }
  
    public CompletableFuture<NoteBytesObject> saveEncryptedFileSwap(
        File file,
        PipedOutputStream pipedOutputStream
    ) {
        if(file.exists() && file.isFile()){
            return FileStreamUtils.saveEncryptedFileSwap(file, getSecretKey(), pipedOutputStream, getExecService());
        }else{
            return FileStreamUtils.saveEncryptedFile(file, getSecretKey(), pipedOutputStream, getExecService());
        }
    }

    /**
     * Get or create a note file path with serialized ledger access.
     * Returns a CompletableFuture that the caller can cancel if needed.
     */
    protected CompletableFuture<NoteBytes> getNoteFilePath(NoteStringArrayReadOnly path) {
        return executeWithLedgerAccess(() -> {
            if (path == null) {
                throw new IllegalArgumentException("Path cannot be null");
            }
            
            File notePathLedger = getFilePathLedger();
            NotePath notePath = new NotePath(notePathLedger, path);
            notePath.checkDataDir();
            
            return notePath;
        }).thenCompose(notePath -> 
            NotePathGet.getOrCreateNoteFilePath(notePath, getSecretKey(), getExecService())
        );
    }

    /**
     * Update file path ledger encryption with serialized access.
     * The caller can cancel this operation if it takes too long.
     */
    protected CompletableFuture<Boolean> updateFilePathLedgerEncryption(
        AsyncNoteBytesWriter progressWriter,
        NoteBytesEphemeral oldPassword,
        NoteBytesEphemeral newPassword,
        int batchSize
    ) { 
        return executeWithLedgerAccess(() -> {
            try {
                getSettingsData().updatePassword(oldPassword, newPassword);
                ProgressMessage.writeAsync(ProtocolMesssages.STARTING, 3, 4, 
                    "Created new key", progressWriter);
                return getFilePathLedger();
            } catch (IOException | InvalidPasswordException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to update password", e);
            } 
        }).thenCompose(filePathLedger -> {
            ProgressMessage.writeAsync(ProtocolMesssages.STARTING, 4, 4, 
                "Opening file path ledger", progressWriter);
            return NotePathReEncryption.updatePathLedgerEncryption(
                filePathLedger, 
                getSettingsData().getOldKey(), 
                getSecretKey(), 
                batchSize, 
                progressWriter, 
                getExecService()
            );
        });
    }

    public CompletableFuture<Void> verifyPassword(NoteBytesEphemeral password){
        return CompletableFuture.runAsync(() -> {
            HashServices.verifyBCryptPassword(password, getSettingsData().getBCryptKey());
        });
    }
    
    /**
     * Delete a note file path with serialized ledger access.
     * The caller can cancel this operation through the returned CompletableFuture.
     */
    protected CompletableFuture<NotePath> deleteNoteFilePath(NotePath notePath){
        return executeWithLedgerAccess(() -> {
            notePath.progressMsg(ProtocolMesssages.STARTING, 3, 4, "Initializing pipeline");
            
            File ledger = notePath.getPathLedger();
            SecretKey secretKey = getSecretKey();
            ExecutorService execService = getExecService();
            PipedOutputStream decryptedOutput = new PipedOutputStream();
            PipedOutputStream parsedOutput = new PipedOutputStream();

            CompletableFuture<NoteBytesObject> decryptFuture = 
                FileStreamUtils.performDecryption(ledger, decryptedOutput, secretKey, execService);
            
            CompletableFuture<Void> parseFuture = 
                NotePathDelete.deleteFromPath(notePath, secretKey, decryptedOutput, parsedOutput, execService);
            
            CompletableFuture<NoteBytesObject> saveFuture = 
                FileStreamUtils.saveEncryptedFileSwap(ledger, secretKey, parsedOutput, execService);
            
            return CompletableFuture.allOf(decryptFuture, parseFuture, saveFuture)
                .whenComplete((v, ex) -> {
                    StreamUtils.safeClose(decryptedOutput);
                    StreamUtils.safeClose(parsedOutput);
                })
                .thenCompose(v -> 
                    CompletableFuture.allOf(notePath.getCompletableList().toArray(new CompletableFuture[0]))
                )
                .thenApply(nv -> notePath);
        }).thenCompose(future -> future);
    }

    /**
     * Parse bucket (nested structure) to collect file paths
     */
    public static void parseBucketForFiles(
            NoteBytesReader reader, 
            List<File> files, 
            IntCounter byteCounter,
            int bucketEnd) throws IOException {
        
        while (byteCounter.get() < bucketEnd) {
            NoteBytes key = reader.nextNoteBytes();
            
            if (key == null) {
                break;
            }
            
            byteCounter.add(key.byteLength());
            
            if (key.equals(NotePath.FILE_PATH)) {
                // Found a file path
                NoteBytes filePath = reader.nextNoteBytes();
                if (filePath != null) {
                    byteCounter.add(filePath.byteLength());
                    
                    File file = new File(filePath.getAsString());
                    if (file.exists() && file.isFile()) {
                        files.add(file);
                    }
                }
                
            } else if (key.getType() == NoteBytesMetaData.STRING_TYPE) {
                // Nested bucket - recurse
                NoteBytesMetaData metaData = reader.nextMetaData();
                
                if (metaData != null && 
                    metaData.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE) {
                    
                    byteCounter.add(NoteBytesMetaData.STANDARD_META_DATA_SIZE);
                    int nestedEnd = byteCounter.get() + metaData.getLength();
                    
                    parseBucketForFiles(reader, files, byteCounter, nestedEnd);
                }
            }
        }
    }

    /**
     * Calculate disk space requirements based on collected files
     */
    public static DiskSpaceValidation calculateDiskSpaceRequirements(
            List<File> files, 
            File ledgerFile) {
        
        int fileCount = files.size();
        long totalSize = 0;
        long[] largestBatchSizes = new long[10];
        
        for (File file : files) {
            long size = file.length();
            totalSize += size;
            DiskSpaceValidation.updateLargestBatchSizes(largestBatchSizes, size);
        }
        
        long availableSpace = ledgerFile.getUsableSpace();
        long requiredSpace = totalSize;
        long bufferSpace = Math.min(
            (long) (requiredSpace * 0.20),
            1024L * 1024 * 1024 // 1GB
        );
        
        boolean isValid = availableSpace >= (requiredSpace + bufferSpace);
        
        Log.logMsg(String.format(
            "[NoteFileService] Disk space validation:\n" +
            "  Files found: %d\n" +
            "  Total size: %.2f MB\n" +
            "  Required space: %.2f MB\n" +
            "  Available space: %.2f MB\n" +
            "  Buffer space: %.2f MB\n" +
            "  Validation: %s",
            fileCount,
            totalSize / (1024.0 * 1024.0),
            requiredSpace / (1024.0 * 1024.0),
            availableSpace / (1024.0 * 1024.0),
            bufferSpace / (1024.0 * 1024.0),
            isValid ? "PASSED" : "FAILED"
        ));
        
        return new DiskSpaceValidation(
            isValid,
            fileCount,
            totalSize,
            requiredSpace,
            availableSpace,
            bufferSpace
        );
    }

    /**
     * Parse root level of ledger to collect file paths
     * Similar to NotePathReEncryption.rootLevelParse but only collecting paths
     */
    public static void parseRootLevelForFiles(NoteBytesReader reader, List<File> files) 
            throws IOException {
        
        NoteBytes rootKey = reader.nextNoteBytes();
        
        while (rootKey != null) {
            if (rootKey.equals(NotePath.FILE_PATH)) {
                // Found a file path entry at root level
                NoteBytes filePath = reader.nextNoteBytes();
                if (filePath != null) {
                    
                    File file = new File(filePath.getAsString());
                    if (file.exists() && file.isFile()) {
                        files.add(file);
                    }
                }
                
            } else if (rootKey.getType() == NoteBytesMetaData.STRING_TYPE) {
                // This is a directory/bucket entry - recurse into it
                NoteBytesMetaData metaData = reader.nextMetaData();
                
                if (metaData != null && 
                    metaData.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE) {
                    
                    // Track position to know bucket boundaries
                    IntCounter byteCounter = new IntCounter(0);
                    byteCounter.add(rootKey.byteLength());
                    byteCounter.add(NoteBytesMetaData.STANDARD_META_DATA_SIZE);
                    
                    int bucketEnd = byteCounter.get() + metaData.getLength();
                    
                    // Recursively collect file paths within bucket
                    parseBucketForFiles(reader, files, byteCounter, bucketEnd);
                }
            }
            
            rootKey = reader.nextNoteBytes();
        }
    }
}