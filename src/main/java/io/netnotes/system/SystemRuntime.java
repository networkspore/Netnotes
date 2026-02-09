package io.netnotes.system;

import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteFiles.NoteFile;
import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.noteFiles.notePath.NoteFileService;
import io.netnotes.system.nodes.NodeController;
import io.netnotes.system.nodes.RepositoryManager;
import io.netnotes.engine.utils.LoggingHelpers.Log;


public class SystemRuntime {

    private final NoteFileService noteFileService;
    private final ProcessRegistryInterface registryInterface;
    
    // Node registry: instanceId → INode (runtime cache)
   // private final Map<NoteBytesReadOnly, INode> m_nodeRegistry = new ConcurrentHashMap<>();
    
    // System Services (FlowProcesses)
    private RepositoryManager repositoryManager;
    private NodeController nodeController;

    /**
     * @param settingsData
     * @param registryInterface
     * @param systemAccess
     */
    public SystemRuntime(
        SettingsData settingsData,
        ProcessRegistryInterface registryInterface,
        RuntimeAccess systemAccess
    ) {
     
        if (settingsData == null) {
            throw new IllegalArgumentException("SettingsData cannot be null");
        }
        if (registryInterface == null) {
            throw new IllegalArgumentException("ProcessRegistryInterface cannot be null");
        }
        
        this.noteFileService = new NoteFileService(settingsData);
        this.registryInterface = registryInterface;
        if(systemAccess != null){
            grantSystemAccess(systemAccess);
        }
        
    }

     /**
     * Grant system access by filling with closures
     * 
     * Each closure captures private fields (settingsData, noteFileService)
     * SystemSessionProcess can call these WITHOUT AppData exposing getters
     */
    /**
     * Grant system access by filling with closures
     * 
     * Each closure captures private fields (settingsData, noteFileService)
     * SystemSessionProcess can call these WITHOUT AppData exposing getters
     */
    private void grantSystemAccess(RuntimeAccess access) {
        
        // Password change - coordinates SettingsData + NoteFileService
        access.setPasswordChanger((currentPassword, newPassword, batchSize, progressWriter) -> {
            // Closure captures this.settingsData and this.noteFileService
            return SystemRuntime.this.noteFileService.updateFilePathLedgerEncryption(
                progressWriter,
                currentPassword,
                newPassword,
                batchSize
            );
        });
        
        // Password verification
        access.setPasswordVerifier(password -> {
            return SystemRuntime.this.noteFileService.getSettingsData().verifyPassword(password);
        });

        access.setAsymVerifier(password ->{
            return SystemRuntime.this.noteFileService.getSettingsData().getAsymmetricPairs(password);
        });
        
        access.setOldPasswordVerifier(oldPassword -> {
            return SystemRuntime.this.noteFileService.getSettingsData().verifyOldPassword(oldPassword);
        });
        
        // Recovery operations
        access.setInvestigator(() -> {
            return SystemRuntime.this.noteFileService.investigateFileEncryptionState();
        });
        
        access.setRecoveryPerformer((analysis, progressWriter, batchSize) -> {
            SecretKey currentKey = SystemRuntime.this.noteFileService.getSettingsData().getSecretKey();
            SecretKey oldKey = SystemRuntime.this.noteFileService.getSettingsData().getOldKey();
            
            return SystemRuntime.this.noteFileService.reEncryptFilesAdaptive(
                analysis.getFilesNeedingUpdate(),
                oldKey,           // Decrypt with old
                currentKey,       // Encrypt with current
                "RECOVERY",
                batchSize,
                progressWriter,
                analysis          // Track completion
            );
        });
        
        access.setRollbackPerformer((analysis, progressWriter, batchSize) -> {
            // First rollback settings (swap keys)
            return CompletableFuture.runAsync(() -> {
                try {
                    SystemRuntime.this.noteFileService.getSettingsData().rollbackToOldPassword();
                } catch (Exception e) {
                    throw new RuntimeException("Settings rollback failed", e);
                }
            })
            .thenCompose(v -> {
                // Now keys are swapped: current=old, old=current
                SecretKey nowCurrent = SystemRuntime.this.noteFileService.getSettingsData().getSecretKey(); // Was old
                SecretKey nowOld = SystemRuntime.this.noteFileService.getSettingsData().getOldKey();        // Was current
                
                // Re-encrypt files that were updated back to "old" (now current) key
                return SystemRuntime.this.noteFileService.reEncryptFilesAdaptive(
                    analysis.getAllFilesNeedingUpdate(), // All files that were changed
                    nowOld,        // Decrypt with new key (was current)
                    nowCurrent,    // Encrypt with old key (now current)
                    "ROLLBACK",
                    batchSize,
                    progressWriter,
                    analysis
                );
            });
        });
        
        access.setComprehensiveRecoveryPerformer((analysis, progressWriter, batchSize) -> {
            SecretKey currentKey = SystemRuntime.this.noteFileService.getSettingsData().getSecretKey();
            SecretKey oldKey = SystemRuntime.this.noteFileService.getSettingsData().getOldKey();
            
            // Step 1: Re-encrypt files needing update
            return SystemRuntime.this.noteFileService.reEncryptFilesAdaptive(
                analysis.getFilesNeedingUpdate(),
                oldKey,
                currentKey,
                "COMPREHENSIVE_REENCRYPT",
                batchSize,
                progressWriter,
                analysis
            )
            .thenCompose(reencryptSuccess -> {
                // Step 2: Finish swaps
                return SystemRuntime.this.noteFileService.performFinishSwaps(
                    analysis.getFilesNeedingSwap()
                );
            })
            .thenCompose(swapSuccess -> {
                // Step 3: Cleanup tmp files
                return SystemRuntime.this.noteFileService.cleanupFiles(
                    analysis.getFilesNeedingCleanup()
                );
            });
        });
        
        access.setSwapPerformer((analysis, progressWriter) -> {
            return SystemRuntime.this.noteFileService.performFinishSwaps(
                analysis.getFilesNeedingSwap()
            );
        });
        
        access.setCleanupPerformer(analysis -> {
            return SystemRuntime.this.noteFileService.cleanupFiles(
                analysis.getFilesNeedingCleanup()
            );
        });
        
        // Settings operations
        access.setSettingsRollback(() -> {
            return CompletableFuture.runAsync(() -> {
                try {
                    SystemRuntime.this.noteFileService.getSettingsData().rollbackToOldPassword();
                } catch (Exception e) {
                    throw new RuntimeException("Settings rollback failed", e);
                }
            });
        });
        
        access.setOldKeyChecker(() -> {
            return SystemRuntime.this.noteFileService.getSettingsData().hasOldKey();
        });
        
        access.setOldKeyClearer(() -> {
            SystemRuntime.this.noteFileService.getSettingsData().clearOldKey();
        });
        
        // File service operations
        access.setDiskSpaceValidator(() -> {
            return SystemRuntime.this.noteFileService.validateDiskSpaceForReEncryption();
        });
        
        access.setCorruptedFilesDeleter(files -> {
            return SystemRuntime.this.noteFileService.deleteCorruptedFiles(files);
        });
        
        access.setFileCounter(() -> {
            return SystemRuntime.this.noteFileService.getFileCount();
        });

   
    }

    /**
     * Initialize system services
     * 
     * Spawns child processes:
     * - InstallationRegistry
     * - RepositoryManager
     * - NodeController
     */
    public CompletableFuture<Void> initialize() {
        Log.logMsg("[SystemRuntime] Initializing services...");
        
        return initializeRepositoryManager()
            .thenCompose(v -> initializeNodeController())
            .thenRun(() -> {
                Log.logMsg("[SystemRuntime] All services initialized");
            });
    }

    

    private CompletableFuture<Void> initializeRepositoryManager() {
       
        // Create child interface scoped to repositories subtree
        ContextPath repoPath = ContextPath.of(CoreConstants.REPOSITORIES);

        ContextPath repoFilePath = repoPath.append("repository-list");
        return SystemRuntime.this.noteFileService.getNoteFile(repoFilePath.getSegments())
            .thenCompose(repoFile -> {
               this.repositoryManager = new RepositoryManager(CoreConstants.REPOSITORIES, repoFile);
                ContextPath path = registryInterface.registerProcess(
                    repositoryManager, 
                    CoreConstants.REPOSITORIES_PATH,
                    CoreConstants.SYSTEM_PATH,
                    registryInterface
                );
                Log.logMsg("[SystemRuntime] Registered RepositoryManager at: " + path);
                return registryInterface.startProcess(path)
                    .thenCompose(v -> repositoryManager.initialize());
            });
    }

    private CompletableFuture<Void> initializeNodeController() {



        NoteFileServiceInterface noteFileServiceInterface = new NoteFileServiceInterface(){

            @Override
            public CompletableFuture<NoteFile> getNoteFile(ContextPath path) {
                return SystemRuntime.this.noteFileService.getNoteFile(path);
            }

            @Override
            public ContextPath getDataRootPath() {
                return CoreConstants.NODE_DATA_PATH;
            }

            @Override
            public CompletableFuture<Void> deleteNoteFile(ContextPath path, boolean recurrsive, AsyncNoteBytesWriter progress){
                return SystemRuntime.this.noteFileService.deleteNoteFilePath(path, false, progress);
            }

        };

        SignatureVerifier sigVerifier = new SignatureVerifier() {

            @Override
            public Ed25519PublicKeyParameters getSigningPublicKey() {
                return SystemRuntime.this.noteFileService.getSettingsData().getSigningPublicKey();
            }
            
        };
       

        // Create controller
        this.nodeController = new NodeController(
            CoreConstants.NODE_CONTROLLER,
            noteFileServiceInterface,
            sigVerifier
        );
        ContextPath path = registryInterface.registerProcess(
                    nodeController, 
                    CoreConstants.NODE_CONTROLLER_PATH,
                    CoreConstants.SYSTEM_PATH,
                    registryInterface
                );

        Log.logMsg("[SystemRuntime] Registered NodeController at: " + path);


        return registryInterface.startProcess(path);
           
    }

    /**
     * Shutdown - stop all services
     */
    public CompletableFuture<Void> shutdown() {
        Log.logMsg("[SystemRuntime] Shutting down services");
        
        return nodeController.shutdown()
            .thenCompose(v -> repositoryManager.shutdown())
            .thenCompose(v -> noteFileService.shutdown(null))
            .thenRun(() -> {
                Log.logMsg("[SystemRuntime] Shutdown complete");
            });
    }

  
}
