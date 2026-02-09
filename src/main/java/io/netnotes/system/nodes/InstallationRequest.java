package io.netnotes.system.nodes;

import java.util.ArrayList;
import java.util.List;

import io.netnotes.noteBytes.NoteBytesEphemeral;
import io.netnotes.system.nodes.security.PolicyManifest;
import io.netnotes.engine.utils.github.GitHubInfo;

/**
 * InstallationRequest - Everything needed to install a package
 * 
 * Created during installation flow after user makes all decisions
 */
public class InstallationRequest {
    private final PackageInfo packageInfo;
    private final ProcessConfig processConfig;
    private final PolicyManifest policyManifest;
    private final NoteBytesEphemeral password;
    private final boolean loadImmediately;
    
    // Optional: GitHub browsing metadata
    private final boolean userReviewedSource;
    private final GitHubInfo sourceRepo;
    
    public InstallationRequest(
        PackageInfo packageInfo,
        ProcessConfig processConfig,
        PolicyManifest policyManifest,
        NoteBytesEphemeral password,
        boolean loadImmediately,
        boolean userReviewedSource,
        GitHubInfo sourceRepo
    ) {
        this.packageInfo = packageInfo;
        this.processConfig = processConfig;
        this.policyManifest = policyManifest;
        this.password = password;
        this.loadImmediately = loadImmediately;
        this.userReviewedSource = userReviewedSource;
        this.sourceRepo = sourceRepo;
    }
    
    public PackageInfo getPackageInfo() { return packageInfo; }
    public ProcessConfig getProcessConfig() { return processConfig; }
    public PolicyManifest getPolicyManifest() { return policyManifest; }
    public NoteBytesEphemeral getPassword() { return password; }
    public boolean shouldLoadImmediately() { return loadImmediately; }
    public boolean hasSourceReview() { return userReviewedSource; }
    public GitHubInfo getSourceRepo() { return sourceRepo; }
    
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (packageInfo == null) errors.add("Package info is null");
        if (processConfig == null) errors.add("Process config is null");
        if (policyManifest == null) errors.add("Policy manifest is null");
        if (password == null) errors.add("Password is null");
        
        return errors;
    }
}