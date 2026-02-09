package io.netnotes.system;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netnotes.engine.crypto.AsymmetricPairs;
import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteFiles.DiskSpaceValidation;
import io.netnotes.noteFiles.notePath.NoteFileService.FileEncryptionAnalysis;


/**
 * AppDataSystemAccess - Capability container filled by SystemRuntime
 * 
 * Pattern:
 * 1. SystemSessionProcess creates: new RuntimeAccess()
 * 2. Passes to SystemRuntime: new SystemRuntime(settingsData, processService, systemAccess)
 * 3. SystemRuntime fills with closures over private fields
 * 4. SystemSessionProcess uses: systemAccess.changePassword(...)
 * 
 * Result: Capabilities WITHOUT SystemRuntime exposing getters
 */
public class RuntimeAccess {
    
    // Closures stored as functional interfaces
    private PasswordChanger passwordChanger;
    private PasswordVerifier passwordVerifier;
    private PasswordVerifier oldPasswordVerifier;
    private AsymetricPairsVerifier asymVerifier;
    private InvestigationProvider investigator;
    private RecoveryPerformer recoveryPerformer;
    private RollbackPerformer rollbackPerformer;
    private ComprehensiveRecoveryPerformer comprehensiveRecoveryPerformer;
    private SwapPerformer swapPerformer;
    private CleanupPerformer cleanupPerformer;
    private SettingsRollback settingsRollback;
    private OldKeyChecker oldKeyChecker;
    private OldKeyClearer oldKeyClearer;
    private DiskSpaceValidator diskSpaceValidator;
    private CorruptedFilesDeleter corruptedFilesDeleter;
    private FileCounter fileCounter;

    
    // === PACKAGE-PRIVATE SETTERS (called by SystemRuntime.grantSystemAccess) ===

    void setPasswordChanger(PasswordChanger changer) {
        this.passwordChanger = changer;
    }
    
    void setPasswordVerifier(PasswordVerifier verifier) {
        this.passwordVerifier = verifier;
    }

    void setAsymVerifier(AsymetricPairsVerifier asymVerifier){
        this.asymVerifier = asymVerifier;
    }
    
    void setOldPasswordVerifier(PasswordVerifier oldVerifier) {
        this.oldPasswordVerifier = oldVerifier;
    }
    
    void setInvestigator(InvestigationProvider investigator) {
        this.investigator = investigator;
    }
    
    void setRecoveryPerformer(RecoveryPerformer recovery) {
        this.recoveryPerformer = recovery;
    }
    
    void setRollbackPerformer(RollbackPerformer rollback) {
        this.rollbackPerformer = rollback;
    }
    
    void setComprehensiveRecoveryPerformer(ComprehensiveRecoveryPerformer comprehensive) {
        this.comprehensiveRecoveryPerformer = comprehensive;
    }
    
    void setSwapPerformer(SwapPerformer swapper) {
        this.swapPerformer = swapper;
    }
    
    void setCleanupPerformer(CleanupPerformer cleanup) {
        this.cleanupPerformer = cleanup;
    }
    
    void setSettingsRollback(SettingsRollback rollback) {
        this.settingsRollback = rollback;
    }
    
    void setOldKeyChecker(OldKeyChecker checker) {
        this.oldKeyChecker = checker;
    }
    
    void setOldKeyClearer(OldKeyClearer clearer) {
        this.oldKeyClearer = clearer;
    }
    
    void setDiskSpaceValidator(DiskSpaceValidator validator) {
        this.diskSpaceValidator = validator;
    }
    
    void setCorruptedFilesDeleter(CorruptedFilesDeleter deleter) {
        this.corruptedFilesDeleter = deleter;
    }
    
    void setFileCounter(FileCounter counter) {
        this.fileCounter = counter;
    }

        
    // === PUBLIC GETTERS (called by SystemSessionProcess) ===
    
    CompletableFuture<Boolean> changePassword(
        NoteBytesEphemeral currentPassword,
        NoteBytesEphemeral newPassword,
        int batchSize,
        AsyncNoteBytesWriter progressWriter
    ) {
        if (passwordChanger == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Password changer capability not granted"));
        }
        return passwordChanger.change(currentPassword, newPassword, batchSize, progressWriter);
    }
    
    CompletableFuture<Boolean> verifyPassword(NoteBytesEphemeral password) {
        if (passwordVerifier == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Password verifier capability not granted"));
        }
        return passwordVerifier.verify(password);
    }

    CompletableFuture<AsymmetricPairs> getAsymmetricPairs(NoteBytesEphemeral password){
        if (asymVerifier == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("AsymmetricPairs verifier capability not granted"));
        }
        return asymVerifier.verify(password);
    }
    
    CompletableFuture<Boolean> verifyOldPassword(NoteBytesEphemeral oldPassword) {
        if (oldPasswordVerifier == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Old password verifier capability not granted"));
        }
        return oldPasswordVerifier.verify(oldPassword);
    }
    
    CompletableFuture<FileEncryptionAnalysis> investigateFileEncryption() {
        if (investigator == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Investigator capability not granted"));
        }
        return investigator.investigate();
    }
    
    CompletableFuture<Boolean> performRecovery(
        FileEncryptionAnalysis analysis,
        AsyncNoteBytesWriter progressWriter,
        int batchSize
    ) {
        if (recoveryPerformer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Recovery performer capability not granted"));
        }
        return recoveryPerformer.perform(analysis, progressWriter, batchSize);
    }
    
    CompletableFuture<Boolean> performRollback(
        FileEncryptionAnalysis analysis,
        AsyncNoteBytesWriter progressWriter,
        int batchSize
    ) {
        if (rollbackPerformer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Rollback performer capability not granted"));
        }
        return rollbackPerformer.perform(analysis, progressWriter, batchSize);
    }
    
    CompletableFuture<Boolean> performComprehensiveRecovery(
        FileEncryptionAnalysis analysis,
        AsyncNoteBytesWriter progressWriter,
        int batchSize
    ) {
        if (comprehensiveRecoveryPerformer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Comprehensive recovery performer capability not granted"));
        }
        return comprehensiveRecoveryPerformer.perform(analysis, progressWriter, batchSize);
    }
    
    CompletableFuture<Boolean> performSwap(
        FileEncryptionAnalysis analysis,
        AsyncNoteBytesWriter progressWriter
    ) {
        if (swapPerformer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Swap performer capability not granted"));
        }
        return swapPerformer.perform(analysis, progressWriter);
    }
    
    CompletableFuture<Boolean> performTempFileCleanup(FileEncryptionAnalysis analysis) {
        if (cleanupPerformer == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Cleanup performer capability not granted"));
        }
        return cleanupPerformer.cleanup(analysis);
    }
    
    CompletableFuture<Void> rollbackSettingsData() {
        if (settingsRollback == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Settings rollback capability not granted"));
        }
        return settingsRollback.rollback();
    }
    
    boolean hasOldKeyForRecovery() {
        if (oldKeyChecker == null) {
            throw new IllegalStateException("Old key checker capability not granted");
        }
        return oldKeyChecker.hasOldKey();
    }
    
    void clearOldKey() {
        if (oldKeyClearer == null) {
            throw new IllegalStateException("Old key clearer capability not granted");
        }
        oldKeyClearer.clear();
    }
    
    CompletableFuture<DiskSpaceValidation> validateDiskSpaceForReEncryption() {
        if (diskSpaceValidator == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Disk space validator capability not granted"));
        }
        return diskSpaceValidator.validate();
    }
    
    CompletableFuture<Boolean> deleteCorruptedFiles(List<String> files) {
        if (corruptedFilesDeleter == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Corrupted files deleter capability not granted"));
        }
        return corruptedFilesDeleter.delete(files);
    }
    
    CompletableFuture<Integer> getFileCount() {
        if (fileCounter == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("File counter capability not granted"));
        }
        return fileCounter.count();
    }
    
    // === FUNCTIONAL INTERFACES ===
    
    @FunctionalInterface
    interface PasswordChanger {
        CompletableFuture<Boolean> change(
            NoteBytesEphemeral current,
            NoteBytesEphemeral newPassword,
            int batchSize,
            AsyncNoteBytesWriter progressWriter
        );
    }
    
    @FunctionalInterface
    interface PasswordVerifier {
        CompletableFuture<Boolean> verify(NoteBytesEphemeral password);
    }

    @FunctionalInterface
    interface AsymetricPairsVerifier {
        CompletableFuture<AsymmetricPairs> verify(NoteBytesEphemeral password);
    }
    
    @FunctionalInterface
    interface InvestigationProvider {
        CompletableFuture<FileEncryptionAnalysis> investigate();
    }
    
    @FunctionalInterface
    interface RecoveryPerformer {
        CompletableFuture<Boolean> perform(
            FileEncryptionAnalysis analysis,
            AsyncNoteBytesWriter progressWriter,
            int batchSize
        );
    }
    
    @FunctionalInterface
    interface RollbackPerformer {
        CompletableFuture<Boolean> perform(
            FileEncryptionAnalysis analysis,
            AsyncNoteBytesWriter progressWriter,
            int batchSize
        );
    }
    
    @FunctionalInterface
    interface ComprehensiveRecoveryPerformer {
        CompletableFuture<Boolean> perform(
            FileEncryptionAnalysis analysis,
            AsyncNoteBytesWriter progressWriter,
            int batchSize
        );
    }
    
    @FunctionalInterface
    interface SwapPerformer {
        CompletableFuture<Boolean> perform(
            FileEncryptionAnalysis analysis,
            AsyncNoteBytesWriter progressWriter
        );
    }
    
    @FunctionalInterface
    interface CleanupPerformer {
        CompletableFuture<Boolean> cleanup(FileEncryptionAnalysis analysis);
    }
    
    @FunctionalInterface
    interface SettingsRollback {
        CompletableFuture<Void> rollback();
    }
    
    @FunctionalInterface
    interface OldKeyChecker {
        boolean hasOldKey();
    }
    
    @FunctionalInterface
    interface OldKeyClearer {
        void clear();
    }
    
    @FunctionalInterface
    interface DiskSpaceValidator {
        CompletableFuture<DiskSpaceValidation> validate();
    }
    
    @FunctionalInterface
    interface CorruptedFilesDeleter {
        CompletableFuture<Boolean> delete(List<String> files);
    }
    
    @FunctionalInterface
    interface FileCounter {
        CompletableFuture<Integer> count();
    }

}