package io.netnotes.noteFiles.notePath;

import io.netnotes.noteFiles.DiskSpaceValidation;
import io.netnotes.noteFiles.FileStreamUtils;
import io.netnotes.noteFiles.ManagedNoteFileInterface;
import io.netnotes.noteFiles.NoteFile;
import io.netnotes.system.SettingsData;
import io.netnotes.engine.crypto.CryptoService;
import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.ProgressMessage;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * NoteFileService - Encrypted file registry and operations
 * 
 * REFACTORED ARCHITECTURE:
 * - Uses SerializedVirtualExecutor through NotePathFactory for ledger access
 * - No more manual lock management (acquireLock/releaseLock)
 * - All ledger operations are serialized automatically
 * - Caller-controlled cancellation for all operations
 * - Provides recovery operations for AppData
 */
public class NoteFileService extends NotePathFactory {
    private final static LogLevel LOG_LEVEL = LogLevel.IMPORTANT;

    private final Map<NoteStringArrayReadOnly, ManagedNoteFileInterface> m_registry = new ConcurrentHashMap<>();

    public NoteFileService(SettingsData settingsData) {
        super(settingsData);
    }
    
    // ===== STANDARD FILE OPERATIONS =====

    public CompletableFuture<NoteFile> getNoteFile(ContextPath notePath) {
        return getNoteFile(notePath.getSegments());
    }
    
    public CompletableFuture<NoteFile> getNoteFile(NoteStringArrayReadOnly notePath) {
        ManagedNoteFileInterface existing = m_registry.get(notePath);
        if (existing != null) {
            return CompletableFuture.completedFuture(new NoteFile(notePath, existing));
        }

        // Execute synchronous registry check with ledger access, then chain async operations
        return executeWithLedgerAccess(() -> notePath)
            .thenCompose(path -> getNoteFilePath(path))
            .thenApply(filePath -> {
                ManagedNoteFileInterface noteFileInterface = m_registry.computeIfAbsent(notePath,
                    k -> new ManagedNoteFileInterface(k, filePath, this));
                return new NoteFile(notePath, noteFileInterface);
            });
    }
    
    // Called by ManagedNoteFileInterface when it has no more references
    public void cleanupInterface(NoteStringArrayReadOnly path, ManagedNoteFileInterface expectedInterface) {
        m_registry.remove(path, expectedInterface);
    }
    
    // ===== KEY UPDATE COORDINATION =====
    
    public CompletableFuture<File> prepareAllForKeyUpdate() {
        return executeWithLedgerAccess(() -> getFilePathLedger())
            .thenCompose(ledgerFile -> {
                List<CompletableFuture<Void>> lockFutures = m_registry.values().stream()
                    .map(ManagedNoteFileInterface::prepareForKeyUpdate)
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(lockFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> ledgerFile);
            });
    }

    public CompletableFuture<File> prepareAllForShutdown() {
        return prepareAllForShutdown(null);
    }

    public CompletableFuture<File> prepareAllForShutdown(AsyncNoteBytesWriter progressWriter) {
        if(progressWriter != null){
            ProgressMessage.writeAsync("[NoteFileService]", 0, -1, "Preparing for shutdown", 
                progressWriter);
        }
        
        return executeWithLedgerAccess(() -> getFilePathLedger())
            .thenCompose(ledgerFile -> {
                List<CompletableFuture<Void>> lockFutures = m_registry.values().stream()
                    .map(managedInterface -> managedInterface.perpareForShutdown(progressWriter))
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(lockFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> ledgerFile);
            });
    }
    
    public void completeKeyUpdateForAll() {
        m_registry.values().forEach(ManagedNoteFileInterface::completeKeyUpdate);
    }
    
    // ===== PASSWORD CHANGE OPERATIONS =====
    
    /**
     * Update file path ledger encryption (normal password change)
     * 
     * This is the PRIMARY password change operation.
     * Coordinates with SettingsData to:
     * 1. Update SettingsData (BCrypt, salt, keys)
     * 2. Re-encrypt all files in ledger
     * 
     * Called by AppData.changePassword()
     */
    public CompletableFuture<Boolean> updateFilePathLedgerEncryption(
        AsyncNoteBytesWriter progressWriter,
        NoteBytesEphemeral oldPassword,
        NoteBytesEphemeral newPassword,
        int batchSize
    ) {
        ProgressMessage.writeAsync(ProtocolMesssages.STARTING, 0, 4, 
            "Acquiring file locks", progressWriter);

        return prepareAllForKeyUpdate()
            .thenCompose(filePathLedger ->
                super.updateFilePathLedgerEncryption(progressWriter, 
                    oldPassword, newPassword, batchSize))
            .whenComplete((result, throwable) -> {
                ProgressMessage.writeAsync(ProtocolMesssages.STOPPING, 0, -1, 
                    "Releasing locks", progressWriter);
                completeKeyUpdateForAll();
                StreamUtils.safeClose(progressWriter);
            });
    }

    // ===== RECOVERY OPERATIONS =====

    /**
     * Investigate file encryption state with automatic ledger access management
     */
    public CompletableFuture<FileEncryptionAnalysis> investigateFileEncryptionState() {
        SecretKey currentKey = getSettingsData().getSecretKey();
        SecretKey oldKey = getSettingsData().getOldKey();
        
        if (oldKey == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Old key not available for investigation"));
        }
        
        return executeWithLedgerAccess(() -> getFilePathLedger())
            .thenCompose(ledgerFile -> collectFilePathsFromLedger(ledgerFile))
            .thenCompose(filePaths -> analyzeFiles(filePaths, currentKey, oldKey));
    }

    private CompletableFuture<FileEncryptionAnalysis> analyzeFiles(
            List<File> filePaths,
            SecretKey currentKey,
            SecretKey oldKey) {
        
        return CompletableFuture.supplyAsync(() -> {
            FileEncryptionAnalysis analysis = new FileEncryptionAnalysis();
            
            for (File file : filePaths) {
                File tmpFile = new File(file.getAbsolutePath() + ".tmp");
                
                FileState state = determineFileState(file, tmpFile, currentKey, oldKey);
                analysis.addFileState(file.getAbsolutePath(), state);
            }
            
            return analysis;
            
        }, getExecService());
    }

    private FileState determineFileState(File file, File tmpFile, 
                                        SecretKey currentKey, SecretKey oldKey) {
        
        // Check if file exists
        if (!file.exists()) {
            // File doesn't exist - check for tmp
            if (tmpFile.exists()) {
                // Tmp exists - check if it's valid
                if (canDecryptFile(tmpFile, currentKey)) {
                    return FileState.TMP_READY_CURRENT_KEY;
                } else if (canDecryptFile(tmpFile, oldKey)) {
                    return FileState.TMP_READY_OLD_KEY;
                } else {
                    return FileState.TMP_CORRUPT;
                }
            } else {
                // Neither exists - path created but never used
                return FileState.NEVER_CREATED;
            }
        }
        
        // File exists - check encryption
        if (canDecryptFile(file, currentKey)) {
            // File accessible with current key
            if (tmpFile.exists()) {
                // Tmp also exists - cleanup needed
                return FileState.CURRENT_KEY_WITH_TMP;
            } else {
                // File OK
                return FileState.CURRENT_KEY_OK;
            }
        }
        
        if (canDecryptFile(file, oldKey)) {
            // File accessible with old key - needs re-encryption
            if (tmpFile.exists()) {
                // Check if tmp has current key
                if (canDecryptFile(tmpFile, currentKey)) {
                    // Tmp is re-encrypted version, ready to replace
                    return FileState.OLD_KEY_WITH_CURRENT_TMP;
                } else {
                    // Tmp exists but unclear state
                    return FileState.OLD_KEY_WITH_TMP;
                }
            } else {
                // Needs re-encryption
                return FileState.OLD_KEY_NEEDS_UPDATE;
            }
        }
        
        // Neither key works
        if (tmpFile.exists()) {
            return FileState.CORRUPT_WITH_TMP;
        } else {
            return FileState.CORRUPT;
        }
    }

    private boolean canDecryptFile(File file, SecretKey key) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        if (file.length() < CryptoService.AES_IV_SIZE) {
            return false;
        }
        
        try {
            int headerSize = CryptoService.AES_IV_SIZE + 1024;
            headerSize = headerSize > file.length() ? (int) file.length() : headerSize;
            // Read just enough to verify decryption
            byte[] header = new byte[headerSize];
            int read = 0;
            try (NoteBytesReader reader = new NoteBytesReader(new FileInputStream(file))) {
                read = reader.read(header, 0, headerSize);
                if (read < CryptoService.AES_IV_SIZE) {
                    return false;
                }
            }
            
            // Try to decrypt header
            byte[] iv = Arrays.copyOfRange(header, 0, CryptoService.AES_IV_SIZE);
            byte[] encryptedChunk = Arrays.copyOfRange(header, 
                CryptoService.AES_IV_SIZE, Math.min(header.length, read));
            
            Cipher cipher = CryptoService.getAESDecryptCipher(iv, key);
            cipher.doFinal(encryptedChunk);
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    // File state enum
    public enum FileState {
        // Good states
        CURRENT_KEY_OK,              // ✓ File OK with current key
        NEVER_CREATED,               // ○ Path in ledger but no file (normal)
        
        // Needs action
        OLD_KEY_NEEDS_UPDATE,        // ⚠ File encrypted with old key
        OLD_KEY_WITH_CURRENT_TMP,    // ⚠ File old key, tmp has current key (finish swap)
        TMP_READY_CURRENT_KEY,       // ⚠ No file, but tmp has current key (finish swap)
        
        // Cleanup needed
        CURRENT_KEY_WITH_TMP,        // ⚠ File OK but tmp exists (delete tmp)
        OLD_KEY_WITH_TMP,            // ⚠ File old key, tmp unclear (investigate)
        TMP_READY_OLD_KEY,           // ⚠ No file, tmp has old key (old state)
        
        // Bad states
        CORRUPT,                     // ✗ File exists but neither key works
        CORRUPT_WITH_TMP,            // ✗ Both file and tmp corrupt
        TMP_CORRUPT                  // ✗ Only tmp exists and corrupt
    }

    // Analysis result
    public static class FileEncryptionAnalysis {
        private final Map<String, FileState> fileStates = new LinkedHashMap<>();
        private final Set<String> completedFiles = Collections.synchronizedSet(new HashSet<>());
        
        public void addFileState(String path, FileState state) {
            fileStates.put(path, state);
        }
        
        public Map<String, FileState> getFileStates() {
            return Collections.unmodifiableMap(fileStates);
        }
        
        public synchronized void markFileCompleted(String path) {
            completedFiles.add(path);
        }
        
        public synchronized void markFilesCompleted(List<String> paths) {
            completedFiles.addAll(paths);
        }
        
        public boolean isFileCompleted(String path) {
            return completedFiles.contains(path);
        }
        
        public List<String> getFilesNeedingUpdate() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.OLD_KEY_NEEDS_UPDATE)
                .map(Map.Entry::getKey)
                .filter(path -> !completedFiles.contains(path))
                .collect(Collectors.toList());
        }
        
        public List<String> getAllFilesNeedingUpdate() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.OLD_KEY_NEEDS_UPDATE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        public List<String> getFilesNeedingSwap() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.OLD_KEY_WITH_CURRENT_TMP ||
                            e.getValue() == FileState.TMP_READY_CURRENT_KEY)
                .map(Map.Entry::getKey)
                .filter(path -> !completedFiles.contains(path))
                .collect(Collectors.toList());
        }
        
        public List<String> getAllFilesNeedingSwap() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.OLD_KEY_WITH_CURRENT_TMP ||
                            e.getValue() == FileState.TMP_READY_CURRENT_KEY)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        public List<String> getFilesNeedingCleanup() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.CURRENT_KEY_WITH_TMP)
                .map(Map.Entry::getKey)
                .filter(path -> !completedFiles.contains(path))
                .collect(Collectors.toList());
        }
        
        public List<String> getAllFilesNeedingCleanup() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.CURRENT_KEY_WITH_TMP)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        public List<String> getCorruptedFiles() {
            return fileStates.entrySet().stream()
                .filter(e -> e.getValue() == FileState.CORRUPT ||
                            e.getValue() == FileState.CORRUPT_WITH_TMP ||
                            e.getValue() == FileState.TMP_CORRUPT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
        
        public boolean needsRecovery() {
            return !getFilesNeedingUpdate().isEmpty() ||
                !getFilesNeedingSwap().isEmpty() ||
                !getFilesNeedingCleanup().isEmpty();
        }
        
        public String getProgressSummary() {
            int totalUpdate = getAllFilesNeedingUpdate().size();
            int completedUpdate = (int) getAllFilesNeedingUpdate().stream()
                .filter(completedFiles::contains)
                .count();
            
            int totalSwap = getAllFilesNeedingSwap().size();
            int completedSwap = (int) getAllFilesNeedingSwap().stream()
                .filter(completedFiles::contains)
                .count();
            
            int totalCleanup = getAllFilesNeedingCleanup().size();
            int completedCleanup = (int) getAllFilesNeedingCleanup().stream()
                .filter(completedFiles::contains)
                .count();
            
            return String.format(
                "Recovery Progress:\n" +
                "  Re-encryption: %d/%d completed\n" +
                "  File swaps: %d/%d completed\n" +
                "  Cleanup: %d/%d completed\n" +
                "  Total completed: %d files",
                completedUpdate, totalUpdate,
                completedSwap, totalSwap,
                completedCleanup, totalCleanup,
                completedFiles.size()
            );
        }
        
        public String getSummary() {
            long ok = fileStates.values().stream()
                .filter(s -> s == FileState.CURRENT_KEY_OK).count();
            long neverCreated = fileStates.values().stream()
                .filter(s -> s == FileState.NEVER_CREATED).count();
            
            int remainingUpdate = getFilesNeedingUpdate().size();
            int remainingSwap = getFilesNeedingSwap().size();
            int remainingCleanup = getFilesNeedingCleanup().size();
            int corrupted = getCorruptedFiles().size();
            
            StringBuilder summary = new StringBuilder();
            summary.append("File Analysis:\n");
            summary.append(String.format("  ✓ OK: %d\n", ok));
            summary.append(String.format("  ○ Never created: %d\n", neverCreated));
            
            if (completedFiles.size() > 0) {
                summary.append(String.format("  ✓ Completed: %d\n", completedFiles.size()));
            }
            
            if (remainingUpdate > 0) {
                summary.append(String.format("  ⚠ Need update: %d\n", remainingUpdate));
            }
            
            if (remainingSwap > 0) {
                summary.append(String.format("  ⚠ Need swap: %d\n", remainingSwap));
            }
            
            if (remainingCleanup > 0) {
                summary.append(String.format("  ⚠ Need cleanup: %d\n", remainingCleanup));
            }
            
            if (corrupted > 0) {
                summary.append(String.format("  ✗ Corrupted: %d\n", corrupted));
            }
            
            return summary.toString();
        }
        
        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            
            report.append("═══════════════════════════════════════\n");
            report.append("  File Encryption State Report\n");
            report.append("═══════════════════════════════════════\n\n");
            
            report.append(getSummary()).append("\n");
            
            if (completedFiles.size() > 0) {
                report.append("───────────────────────────────────────\n");
                report.append(getProgressSummary()).append("\n");
            }
            
            report.append("═══════════════════════════════════════\n");
            
            return report.toString();
        }
        
        public void resetCompletionTracking() {
            completedFiles.clear();
        }
        
        public double getCompletionPercentage() {
            int totalIssues = getAllFilesNeedingUpdate().size() +
                            getAllFilesNeedingSwap().size() +
                            getAllFilesNeedingCleanup().size();
            
            if (totalIssues == 0) {
                return 100.0;
            }
            
            return (completedFiles.size() / (double) totalIssues) * 100.0;
        }
    }

    public CompletableFuture<Boolean> cleanupFiles(List<String> files) {
        return CompletableFuture.supplyAsync(() -> {
            boolean succeeded = true;
            for (String pathStr : files) {
                File tmpFile = new File(pathStr + ".tmp");
                try {
                    if (tmpFile.exists()) {
                        Files.delete(tmpFile.toPath());
                    }
                } catch (IOException e) {
                    Log.logError("Failed to delete tmp: " + tmpFile + " - " + 
                        e.getMessage());
                    succeeded = false;
                }
            }
            return succeeded;
        });
    }

    public CompletableFuture<Boolean> reEncryptFiles(
        List<String> filePaths,
        SecretKey decryptKey,
        SecretKey encryptKey,
        String operation,
        int batchSize,
        AsyncNoteBytesWriter progressWriter
    ) {
        return reEncryptFilesAdaptive(
            filePaths, decryptKey, encryptKey, operation, 
            batchSize, progressWriter, null);
    }

    public CompletableFuture<Boolean> performFinishSwaps(List<String> files) {
        return CompletableFuture.supplyAsync(() -> {
            boolean isSuccess = true;
            for (String pathStr : files) {
                File file = new File(pathStr);
                File tmpFile = new File(pathStr + ".tmp");
                
                try {
                    if (!file.exists() && tmpFile.exists()) {
                        Files.move(tmpFile.toPath(), file.toPath(), 
                            StandardCopyOption.REPLACE_EXISTING);
                    } else if (file.exists() && tmpFile.exists()) {
                        SecretKey currentKey = getSettingsData().getSecretKey();
                        if (canDecryptFile(tmpFile, currentKey)) {
                            Files.move(tmpFile.toPath(), file.toPath(), 
                                StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            isSuccess = false;
                        }
                    }
                } catch (IOException e) {
                    isSuccess = false;
                    Log.logError("Perform finish swaps failed: " + e.toString());
                }
            }
            return isSuccess;
        });
    }

    // ===== DISK SPACE VALIDATION =====
    
    /**
     * Validate disk space for re-encryption with automatic ledger access
     */
    public CompletableFuture<DiskSpaceValidation> validateDiskSpaceForReEncryption() {
        return executeWithLedgerAccess(() -> getFilePathLedger())
            .thenCompose(ledgerFile -> collectFilePathsFromLedger(ledgerFile)
                .thenApply(files -> calculateDiskSpaceRequirements(files, ledgerFile)));
    }
        
    /**
     * Collect all file paths from the encrypted ledger
     */
    private CompletableFuture<List<File>> collectFilePathsFromLedger(File ledgerFile) {
        if (!ledgerFile.exists() || !ledgerFile.isFile()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            List<File> files = new ArrayList<>();
            PipedOutputStream decryptedOutput = new PipedOutputStream();
            
            try {
                CompletableFuture<NoteBytesObject> decryptFuture = 
                    performDecryption(ledgerFile, decryptedOutput);
                
                PipedInputStream decryptedInput = new PipedInputStream(decryptedOutput);
                
                try (NoteBytesReader reader = new NoteBytesReader(decryptedInput)) {
                    parseRootLevelForFiles(reader, files);
                }
                
                decryptFuture.join();
                
            } catch (IOException e) {
                Log.logError("[NoteFileService] Error reading ledger: " + e.getMessage());
            } finally {
                StreamUtils.safeClose(decryptedOutput);
            }
            
            return files;
            
        }, getExecService());
    }

    // ===== FILE DELETION =====
    
    public CompletableFuture<Void> deleteNoteFilePath(
        ContextPath contextPath, 
        boolean recursive, 
        AsyncNoteBytesWriter progressWriter
    ) {
        NoteStringArrayReadOnly path = contextPath.getSegments();
        if(path == null || path.byteLength() == 0 || path.size() == 0) {
            StreamUtils.safeClose(progressWriter);
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Null or empty path provided"));
        }

        if(progressWriter != null) {
            ProgressMessage.writeAsync(ProtocolMesssages.STARTING,
                0, 4, "Acquiring lock", progressWriter);
        }
        
        List<ManagedNoteFileInterface> interfaceList = new ArrayList<>();

        return executeWithLedgerAccess(() -> {
            File filePathLedger = getFilePathLedger();
            
            if(!filePathLedger.exists() || !filePathLedger.isFile() || 
                filePathLedger.length() <= CryptoService.AES_IV_SIZE) {
                throw new IllegalArgumentException(
                    "Invalid ledger, inaccessible or insufficient size provided");
            }

            return new NotePath(filePathLedger, path, recursive, progressWriter);
        }).thenCompose(notePath -> {
            notePath.progressMsg(ProtocolMesssages.STARTING, 2, 4,
                "Initial lock acquired, preparing registry interfaces");
            
            return prepareForShutdown(path, recursive, interfaceList)
                .thenCompose(v -> super.deleteNoteFilePath(notePath))
                .whenComplete((result, ex) -> {
                    int toRemoveSize = interfaceList.size();
                    for(int i = 0; i < toRemoveSize; i++) {
                        ManagedNoteFileInterface managedInterface = interfaceList.get(i);
                        boolean isDeleted = !managedInterface.isFile();
                            
                        result.progressMsg(ProtocolMesssages.STOPPING, i, toRemoveSize, 
                            managedInterface.getId().getAsString(), new NoteBytesPair[]{
                                new NoteBytesPair(Keys.STATUS, 
                                    !managedInterface.isLocked() && isDeleted ? 
                                        ProtocolMesssages.SUCCESS : ProtocolMesssages.FAILED
                                )
                            });
                    }
                    
                    StreamUtils.safeClose(progressWriter);
                })
                .thenApply(notePath2 -> null);
        });
    }
    
    private CompletableFuture<Void> prepareForShutdown(
            NoteStringArrayReadOnly path, 
            boolean recursive,
            List<ManagedNoteFileInterface> interfaceList) {
   
        if(recursive) {
            List<CompletableFuture<Void>> list = new ArrayList<>();
            
            Iterator<Map.Entry<NoteStringArrayReadOnly,ManagedNoteFileInterface>> iterator = 
                m_registry.entrySet().iterator();
            
            while(iterator.hasNext()) {
                Map.Entry<NoteStringArrayReadOnly,ManagedNoteFileInterface> entry = 
                    iterator.next();
                list.add(entry.getValue().perpareForShutdown());
                if(interfaceList != null) {
                    interfaceList.add(entry.getValue());
                }
                iterator.remove(); 
            }
            
            return CompletableFuture.allOf(list.toArray(new CompletableFuture[0]));
        } else {
            ManagedNoteFileInterface managedInterface = m_registry.remove(path);
            if(managedInterface != null) {
                if(interfaceList != null) {
                    interfaceList.add(managedInterface);
                }
                return managedInterface.perpareForShutdown();
            } else {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    // ===== MAINTENANCE =====
    
    public int cleanupUnusedInterfaces() {
        int removed = 0;
        Iterator<Map.Entry<NoteStringArrayReadOnly, ManagedNoteFileInterface>> iterator = 
            m_registry.entrySet().iterator();
            
        while (iterator.hasNext()) {
            Map.Entry<NoteStringArrayReadOnly, ManagedNoteFileInterface> entry = 
                iterator.next();
            ManagedNoteFileInterface noteInterface = entry.getValue();
            
            if (noteInterface.getReferenceCount() == 0 && !noteInterface.isLocked()) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
    
    public int getRegistrySize() {
        return m_registry.size();
    }
    
    public Map<NoteStringArrayReadOnly, Integer> getReferenceCounts() {
        return m_registry.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getReferenceCount()
            ));
    }

    public CompletableFuture<Integer> getFileCount() {
        return validateDiskSpaceForReEncryption()
            .thenApply(DiskSpaceValidation::getNumberOfFiles);
    }

    /**
     * Adaptive re-encryption with resource monitoring
     */
    public CompletableFuture<Boolean> reEncryptFilesAdaptive(
            List<String> filePaths,
            SecretKey decryptKey,
            SecretKey encryptKey,
            String operation,
            int initialBatchSize,
            AsyncNoteBytesWriter progressWriter,
            FileEncryptionAnalysis analysis) {
        
        if (filePaths == null || filePaths.isEmpty()) {
            Log.logMsg("[NoteFileService] No files to re-encrypt", LOG_LEVEL);
            return CompletableFuture.completedFuture(true);
        }
        
        Log.logMsg(String.format(
            "[NoteFileService] %s: Adaptive processing of %d files (initial batch: %d)",
            operation, filePaths.size(), initialBatchSize), LOG_LEVEL);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                AtomicInteger completed = new AtomicInteger(0);
                AtomicInteger failed = new AtomicInteger(0);
                AtomicInteger currentBatchSize = new AtomicInteger(initialBatchSize);
                
                File dataDir = SettingsData.getDataDir();
                List<String> remainingFiles = new ArrayList<>(filePaths);
                
                while (!remainingFiles.isEmpty()) {
                    ResourceCheck resourceCheck = checkResourceAvailability(
                        dataDir, remainingFiles, currentBatchSize.get());
                    
                    if (!resourceCheck.canProceed) {
                        Log.logError("[NoteFileService] Insufficient resources: " + 
                            resourceCheck.reason);
                        
                        if (progressWriter != null) {
                            TaskMessages.writeErrorAsync(operation,
                                "Resource check failed: " + resourceCheck.reason,
                                new RuntimeException(resourceCheck.reason),
                                progressWriter);
                        }
                        
                        return completed.get() > 0;
                    }
                    
                    if (resourceCheck.recommendedBatchSize < currentBatchSize.get()) {
                        Log.logMsg(String.format(
                            "[NoteFileService] Adjusting batch size: %d -> %d",
                            currentBatchSize.get(), resourceCheck.recommendedBatchSize), LOG_LEVEL);
                        currentBatchSize.set(resourceCheck.recommendedBatchSize);
                    }
                    
                    int batchSize = Math.min(currentBatchSize.get(), remainingFiles.size());
                    List<String> currentBatch = remainingFiles.subList(0, batchSize);
                    List<String> batchCopy = new ArrayList<>(currentBatch);
                    currentBatch.clear();
                    
                    Semaphore semaphore = new Semaphore(batchSize, true);
                    List<CompletableFuture<Boolean>> batchFutures = new ArrayList<>();
                    
                    for (String filePath : batchCopy) {
                        File file = new File(filePath);
                        
                        if (!file.exists() || !file.isFile()) {
                            Log.logError("[NoteFileService] File not found: " + filePath);
                            failed.incrementAndGet();
                            continue;
                        }
                        
                        CompletableFuture<Boolean> fileFuture = CompletableFuture.supplyAsync(() -> {
                            try {
                                semaphore.acquire();
                                try {
                                    File tmpFile = new File(file.getAbsolutePath() + ".tmp");
                                    
                                    if (progressWriter != null) {
                                        ProgressMessage.writeAsync(ProtocolMesssages.ITEM_INFO,
                                            completed.get(), filePaths.size(),
                                            operation + ": " + filePath,
                                            progressWriter);
                                    }
                                    
                                    FileStreamUtils.updateFileEncryption(
                                        decryptKey, encryptKey, file, tmpFile, progressWriter);
                                    
                                    completed.incrementAndGet();
                                    
                                    if (analysis != null) {
                                        analysis.markFileCompleted(filePath);
                                    }
                                    
                                    if (progressWriter != null) {
                                        ProgressMessage.writeAsync(ProtocolMesssages.SUCCESS,
                                            completed.get(), filePaths.size(),
                                            operation + ": " + filePath,
                                            progressWriter);
                                    }
                                    
                                    return true;
                                    
                                } finally {
                                    semaphore.release();
                                }
                            } catch (Exception e) {
                                failed.incrementAndGet();
                                Log.logError("[NoteFileService] " + operation +
                                    " failed: " + filePath + " - " + e.getMessage());
                                
                                if (progressWriter != null) {
                                    TaskMessages.writeErrorAsync(operation,
                                        filePath, e, progressWriter);
                                }
                                
                                return false;
                            }
                        }, getExecService());
                        
                        batchFutures.add(fileFuture);
                    }
                    
                    CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                    
                    Log.logMsg(String.format(
                        "[NoteFileService] Batch complete: %d/%d files remaining",
                        remainingFiles.size(), filePaths.size()), LOG_LEVEL);
                }
                
                boolean success = failed.get() == 0;
                
                Log.logMsg(String.format(
                    "[NoteFileService] %s complete: %d succeeded, %d failed",
                    operation, completed.get(), failed.get()), LOG_LEVEL);
                
                return success;
                
            } catch (Exception e) {
                Log.logError("[NoteFileService] " + operation +
                    " operation failed: " + e.getMessage());
                return false;
            }
        }, VirtualExecutors.getVirtualExecutor());
    }

    private ResourceCheck checkResourceAvailability(
            File dataDir,
            List<String> remainingFiles,
            int proposedBatchSize) {
        
        ResourceCheck check = new ResourceCheck();
        
        long availableDisk = dataDir.getUsableSpace();
        long bufferSpace = 100 * 1024 * 1024; // 100MB buffer
        long safeDiskSpace = availableDisk - bufferSpace;
        
        if (safeDiskSpace < 0) {
            check.canProceed = false;
            check.reason = "Insufficient disk space (less than 100MB available)";
            return check;
        }
        
        long batchSpaceNeeded = 0;
        int filesInBatch = Math.min(proposedBatchSize, remainingFiles.size());
        
        for (int i = 0; i < filesInBatch; i++) {
            File file = new File(remainingFiles.get(i));
            if (file.exists()) {
                batchSpaceNeeded += file.length();
            }
        }
        
        if (batchSpaceNeeded > safeDiskSpace) {
            long spaceAccumulator = 0;
            int maxFiles = 0;
            
            for (int i = 0; i < filesInBatch; i++) {
                File file = new File(remainingFiles.get(i));
                if (file.exists()) {
                    long fileSize = file.length();
                    if (spaceAccumulator + fileSize <= safeDiskSpace) {
                        spaceAccumulator += fileSize;
                        maxFiles++;
                    } else {
                        break;
                    }
                }
            }
            
            if (maxFiles == 0) {
                check.canProceed = false;
                check.reason = String.format(
                    "Insufficient disk space. Need %.2f MB, have %.2f MB available",
                    batchSpaceNeeded / (1024.0 * 1024.0),
                    safeDiskSpace / (1024.0 * 1024.0));
                return check;
            }
            
            check.canProceed = true;
            check.recommendedBatchSize = maxFiles;
            check.reason = String.format(
                "Disk space constrained. Reducing batch from %d to %d files",
                proposedBatchSize, maxFiles);
            
            Log.logMsg("[NoteFileService] " + check.reason, LOG_LEVEL);
            return check;
        }
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long availableMemory = maxMemory - allocatedMemory + freeMemory;
        
        long memoryNeeded = (long) (batchSpaceNeeded * 0.1);
        
        if (memoryNeeded > availableMemory * 0.5) {
            int memoryBasedBatch = (int) ((availableMemory * 0.5) / 
                (batchSpaceNeeded / (double) filesInBatch * 0.1));
            
            if (memoryBasedBatch < 1) {
                System.gc();
                
                long newAvailable = runtime.maxMemory() - runtime.totalMemory() + 
                    runtime.freeMemory();
                
                if (newAvailable < memoryNeeded) {
                    check.canProceed = false;
                    check.reason = String.format(
                        "Insufficient memory. Need ~%.2f MB, have ~%.2f MB available",
                        memoryNeeded / (1024.0 * 1024.0),
                        availableMemory / (1024.0 * 1024.0));
                    return check;
                }
            }
            
            check.canProceed = true;
            check.recommendedBatchSize = Math.max(1, memoryBasedBatch);
            check.reason = String.format(
                "Memory constrained. Reducing batch from %d to %d files",
                proposedBatchSize, memoryBasedBatch);
            
            Log.logMsg("[NoteFileService] " + check.reason, LOG_LEVEL);
            return check;
        }
        
        check.canProceed = true;
        check.recommendedBatchSize = proposedBatchSize;
        check.reason = "Resources OK";
        return check;
    }

    private static class ResourceCheck {
        boolean canProceed = true;
        int recommendedBatchSize = 1;
        String reason = "";
    }

    public CompletableFuture<Boolean> deleteCorruptedFiles(List<String> corruptedFiles) {
        if (corruptedFiles == null || corruptedFiles.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            boolean allSucceeded = true;
            
            for (String filePath : corruptedFiles) {
                File file = new File(filePath);
                File tmpFile = new File(filePath + ".tmp");
                
                try {
                    if (file.exists()) {
                        Files.delete(file.toPath());
                        Log.logMsg("[NoteFileService] Deleted corrupted file: " + filePath, LOG_LEVEL);
                    }
                    
                    if (tmpFile.exists()) {
                        Files.delete(tmpFile.toPath());
                        Log.logMsg("[NoteFileService] Deleted corrupted tmp: " + 
                            tmpFile.getName(), LOG_LEVEL);
                    }
                } catch (IOException e) {
                    Log.logError("[NoteFileService] Failed to delete: " + filePath + 
                        " - " + e.getMessage());
                    allSucceeded = false;
                }
            }
            
            return allSucceeded;
            
        }, VirtualExecutors.getVirtualExecutor());
    }

    public CompletableFuture<Void> shutdown(AsyncNoteBytesWriter progressWriter) {
        return prepareAllForShutdown().whenComplete((v, ex) -> {
            if(ex != null) {
                Throwable cause = ex.getCause();
                String msg = "Error shutting down note file service: " + 
                    (cause == null ? ex.getMessage() : ex.getMessage() + ": " + cause.toString());
                Log.logError(msg);
                ex.printStackTrace();
                if(progressWriter != null) {
                    TaskMessages.writeErrorAsync("AppData", msg, ex, progressWriter);
                }
            }
        }).thenApply((v) -> null);
    }
}