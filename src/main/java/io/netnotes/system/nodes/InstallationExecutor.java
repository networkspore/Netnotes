package io.netnotes.system.nodes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.system.nodes.security.NodeSecurityPolicy;
import io.netnotes.system.nodes.security.PathCapability;
import io.netnotes.system.nodes.security.PolicyManifest;
import io.netnotes.engine.utils.LoggingHelpers.Log;

/**
 * InstallationExecutor - Actually performs the installation
 * 
 * Separated from flow coordinator for clean separation of concerns.
 * 
 * PROCESS:
 * 1. Validate InstallationRequest
 * 2. Use PackageInstaller to download and store files
 * 3. Create security policy from manifest + user decisions
 * 4. Build RefactoredInstalledPackage with all metadata
 * 5. Return for registration (caller registers in InstallationRegistry)
 */
class InstallationExecutor {
    private final NoteFileServiceInterface fileService;
    private final PackageInstaller packageInstaller;
    
    public InstallationExecutor(NoteFileServiceInterface fileService) {
        this.fileService = fileService;
        this.packageInstaller = new PackageInstaller(fileService);
    }
    
    /**
     * Execute installation from request
     * 
     * ALL DECISIONS ALREADY MADE:
     * - Package selected
     * - Process configuration chosen
     * - Capabilities approved
     * - Password verified
     * 
     * This method just executes the installation.
     */
    public CompletableFuture<InstalledPackage> executeInstallation(
        InstallationRequest request
    ) {
        // Validate request
        List<String> errors = request.validate();
        if (!errors.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Invalid request: " + errors));
        }
        
        PackageInfo pkgInfo = request.getPackageInfo();
        ProcessConfig processConfig = request.getProcessConfig();
        PolicyManifest policyManifest = request.getPolicyManifest();
        
        Log.logMsg("[InstallationExecutor] Installing: " + pkgInfo.getName());
        Log.logMsg("  ProcessId: " + processConfig.getProcessId());
        
        // Step 1: Download and store package files
        return packageInstaller.installPackage(pkgInfo)
            .thenApply(installPath -> {
                Log.logMsg("[InstallationExecutor] Files downloaded and stored");
                
                // Step 2: Create security policy from manifest
                NodeSecurityPolicy policy = createSecurityPolicy(
                    pkgInfo,
                    policyManifest,
                    processConfig
                );
                
                Log.logMsg("[InstallationExecutor] Security policy created with " + 
                    policy.getGrantedCapabilities().size() + " capabilities");
                
                // Step 3: Create RefactoredInstalledPackage with all metadata
                InstalledPackage pkg = new InstalledPackage(
                    new PackageId(pkgInfo.getPackageId(), pkgInfo.getVersion()),
                    pkgInfo.getName(),
                    pkgInfo.getDescription(),
                    pkgInfo.getManifest(),
                    processConfig,
                    policy,
                    pkgInfo.getRepository(),
                    System.currentTimeMillis(),
                    installPath
                );

                Log.logMsg("[InstallationExecutor] Installation complete: " + 
                    pkgInfo.getName());
                
                return pkg;
            })
            .exceptionally(ex -> {
                Log.logError("[InstallationExecutor] Installation failed: " + 
                    ex.getMessage());
                throw new RuntimeException("Installation failed", ex);
            });
    }
    
    /**
     * Create security policy from manifest and user approval
     * 
     * The user has already reviewed and approved the capabilities in the flow.
     * We just need to grant them and mark as approved.
     */
    private NodeSecurityPolicy createSecurityPolicy(
        PackageInfo pkgInfo,
        PolicyManifest policyManifest,
        ProcessConfig processConfig
    ) {
        NodeSecurityPolicy policy = new NodeSecurityPolicy(
            pkgInfo.getPackageId(),
            processConfig.getProcessId()
        );
        
        // Grant all requested capabilities (user approved them)
        for (PathCapability cap : policyManifest.getRequestedCapabilities()) {
            policy.grantCapability(cap);
            
            Log.logMsg("[InstallationExecutor]   Granted: " + 
                cap.getPathPattern() + " (" + cap.getReason() + ")");
        }
        
        // Mark as approved (password was verified in SystemSessionProcess)
        policy.approve();
        
        return policy;
    }
}
