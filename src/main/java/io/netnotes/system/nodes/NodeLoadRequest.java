package io.netnotes.system.nodes;

import java.util.ArrayList;
import java.util.List;

import io.netnotes.system.nodes.security.NodeSecurityPolicy;

/**
 * NodeLoadRequest - Everything needed to load a node
 * 
 * NodeController receives this fully-formed request with all decisions made:
 * - Which package to load
 * - What process configuration to use
 * - What security policy to apply
 * 
 * NO decisions made at load time - all pre-determined at install
 */
public class NodeLoadRequest {
    private final InstalledPackage pkg;
    
    public NodeLoadRequest(InstalledPackage pkg) {
        this.pkg = pkg;
    }
    
    public InstalledPackage getPackage() { return pkg; }
    public PackageId getPackageId() { return pkg.getPackageId(); }
    public ProcessConfig getProcessConfig() { return pkg.getProcessConfig(); }
    public NodeSecurityPolicy getSecurityPolicy() { return pkg.getSecurityPolicy(); }
    
    /**
     * Validate request before loading
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (pkg == null) {
            errors.add("Package is null");
        }
        if (pkg.getProcessConfig() == null) {
            errors.add("Process configuration is null");
        }
        if (pkg.getSecurityPolicy() == null) {
            errors.add("Security policy is null");
        }
        
        return errors;
    }
}