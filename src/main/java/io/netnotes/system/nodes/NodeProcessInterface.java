package io.netnotes.system.nodes;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.io.RoutedPacket;
import io.netnotes.engine.io.process.FlowProcess;
import io.netnotes.engine.io.process.ProcessRegistryInterface;
import io.netnotes.engine.io.process.StreamChannel;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.system.nodes.security.NodeSecurityPolicy;
import io.netnotes.system.nodes.security.PathCapability;
import io.netnotes.engine.utils.LoggingHelpers.Log;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
/**
 * NodeProcessInterface - Simplified path-based security enforcement
 * 
 * Single responsibility: Check if node can access target path with operation.
 * No command-specific logic, just path + operation validation.
 * 
 * Architecture:
 * Node → NodeProcessInterface (path + operation check) → Delegate
 */
public class NodeProcessInterface implements ProcessRegistryInterface {
    
    private final ProcessRegistryInterface delegate;
    private final ContextPath nodePath;
    private final NodeSecurityPolicy policy;
    
    public NodeProcessInterface(
        ProcessRegistryInterface delegate,
        ContextPath nodePath,
        NodeSecurityPolicy policy
    ) {
        this.delegate = delegate;
        this.nodePath = nodePath;
        this.policy = policy;
    }
    
    // ===== CORE ACCESS CHECK =====
    
    /**
     * Single source of truth: Can this node access target with operation?
     */
    private boolean canAccess(ContextPath caller, ContextPath target, PathCapability.Operation operation) {
        // Verify caller is within node's subtree
        if (!caller.startsWith(nodePath)) {
            logViolation(caller, target, operation, "Caller not within node subtree");
            return false;
        }
        
        // Check policy
        if (policy.canAccess(target, operation)) {
            // Log successful access for audit trail
            policy.addRuntimeGrant(target.toString(), operation, "Access granted");
            return true;
        }
        
        logViolation(caller, target, operation, "No matching capability");
        return false;
    }
    
    /**
     * Log security violation
     */
    private void logViolation(ContextPath caller, ContextPath target, 
                              PathCapability.Operation operation, String reason) {
        Log.logError(String.format(
            "[Security] VIOLATION: Node '%s' (caller: %s) attempted %s on '%s': %s",
            nodePath.getLastSegment(),
            caller,
            operation,
            target,
            reason
        ));
    }
    
    // ===== ProcessRegistryInterface IMPLEMENTATION =====
    
    @Override
    public ContextPath registerChild(ContextPath callerPath, FlowProcess process) {
        if (!callerPath.startsWith(nodePath)) {
            throw new SecurityException("Caller not within node subtree");
        }
        
        // Registering children is always within node's own subtree
        return delegate.registerChild(callerPath, process);
    }
    

    
    @Override
    public FlowProcess getProcess(ContextPath path) {
        // Getting process reference requires MESSAGE capability to that path
        if (!canAccess(nodePath, path, PathCapability.Operation.MESSAGE)) {
            throw new SecurityException("Access denied: cannot get process at " + path);
        }
        
        return delegate.getProcess(path);
    }
    
    @Override
    public boolean exists(ContextPath path) {
        // Checking existence is less sensitive, only require caller verification
        return delegate.exists(path);
    }
    
    @Override
    public CompletableFuture<StreamChannel> requestStreamChannel(ContextPath callerPath, ContextPath target) {
        // Verify caller
        if (!callerPath.startsWith(nodePath)) {
            return CompletableFuture.failedFuture(
                new SecurityException("Caller not within node subtree"));
        }
        
        // Check STREAM capability
        if (!canAccess(callerPath, target, PathCapability.Operation.STREAM)) {
            return CompletableFuture.failedFuture(
                new SecurityException("Access denied: cannot stream to " + target));
        }
        
        return delegate.requestStreamChannel(callerPath, target);
    }
    
    @Override
    public CompletableFuture<RoutedPacket> request(
            ContextPath targetPath,
            NoteBytesReadOnly payload,
            Duration timeout) {
        
        // Check MESSAGE capability
        if (!canAccess(nodePath, targetPath, PathCapability.Operation.MESSAGE)) {
            return CompletableFuture.failedFuture(
                new SecurityException("Access denied: cannot message " + targetPath));
        }
        
        return delegate.request(targetPath, payload, timeout);
    }
    
    @Override
    public List<ContextPath> getChildren() {
        // Getting own children is always allowed
        return delegate.getChildren();
    }
    
    @Override
    public <T extends FlowProcess> List<T> findChildrenByType(ContextPath callerPath, Class<T> type) {
        if (!callerPath.startsWith(nodePath)) {
            throw new SecurityException("Caller not within node subtree");
        }
        
        return delegate.findChildrenByType(callerPath, type);
    }
    
    @Override
    public void connect(ContextPath upstreamPath, ContextPath downstreamPath) {
        // Both paths must be within node's subtree or explicitly allowed
        if (!upstreamPath.startsWith(nodePath) || !downstreamPath.startsWith(nodePath)) {
            // Check if node has capability to connect external processes
            if (!canAccess(nodePath, upstreamPath, PathCapability.Operation.MESSAGE) ||
                !canAccess(nodePath, downstreamPath, PathCapability.Operation.MESSAGE)) {
                throw new SecurityException("Access denied: cannot connect " + 
                    upstreamPath + " to " + downstreamPath);
            }
        }
        
        delegate.connect(upstreamPath, downstreamPath);
    }
    
    @Override
    public CompletableFuture<Void> startProcess(ContextPath path) {
        if (!path.startsWith(nodePath)) {
            throw new SecurityException("Cannot start process outside node subtree");
        }
        
        return delegate.startProcess(path);
    }
    
    @Override
    public void unregisterProcess(ContextPath path) {
        if (!path.startsWith(nodePath)) {
            throw new SecurityException("Cannot unregister process outside node subtree");
        }
        
        delegate.unregisterProcess(path);
    }
    
    @Override
    public FlowProcess getChildProcess(ContextPath callerPath, String childName) {
        ContextPath path = callerPath.append(childName);
        if (!path.startsWith(nodePath)) {
            throw new SecurityException("Child path not within node subtree");
        }
        
        return delegate.getChildProcess(callerPath, childName);
    }
    
    /**
     *  Child interfaces inherit the same security policy
     *  Additional policy can only further restrict, not expand
     * @param basePath
     * @param additionalPolicy
     * @return
     */

    public ProcessRegistryInterface createChildInterface(
            ContextPath basePath,
            NodeSecurityPolicy additionalPolicy) {
        
        if (!basePath.startsWith(nodePath)) {
            throw new SecurityException("Child interface basePath must be within node's subtree");
        }
        
        return new NodeProcessInterface(this, basePath, additionalPolicy);
    }
    
    // ===== GETTERS =====
    
    public NodeSecurityPolicy getPolicy() {
        return policy;
    }
    
    public ContextPath getNodePath() {
        return nodePath;
    }

   @Override
    public ContextPath registerProcess(
            FlowProcess process,
            ContextPath path,
            ContextPath parentPath,
            ProcessRegistryInterface interfaceForNewProcess
    ) {
        // Must register only inside subtree
        if (!path.startsWith(nodePath)) {
            throw new SecurityException("Cannot register process outside node subtree: " + path);
        }

        return delegate.registerProcess(process, path, parentPath, interfaceForNewProcess);
    }

    @Override
    public List<FlowProcess> findByPathPrefix(ContextPath prefix) {
        // Caller should only see processes within allowed subtree
        if (!prefix.startsWith(nodePath)) {
            throw new SecurityException("Cannot query processes outside node subtree: " + prefix);
        }

        return delegate.findByPathPrefix(prefix);
    }


    @Override
    public Set<ContextPath> getAllPaths() {
        // Filter out anything outside the subtree to avoid leaking topology
        return delegate.getAllPaths()
                .stream()
                .filter(p -> p.startsWith(nodePath))
                .collect(java.util.stream.Collectors.toSet());
    }


    @Override
    public ContextPath getParent(ContextPath childPath) {
        // Parent lookup allowed only within subtree
        if (!childPath.startsWith(nodePath)) {
            throw new SecurityException("Cannot get parent outside node subtree: " + childPath);
        }

        return delegate.getParent(childPath);
    }

    @Override
    public Set<ContextPath> getChildren(ContextPath parentPath) {
        if (!parentPath.startsWith(nodePath)) {
            throw new SecurityException("Cannot get children of a node outside subtree: " + parentPath);
        }

        return delegate.getChildren(parentPath)
                .stream()
                .filter(path -> path.startsWith(nodePath))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<FlowProcess> findChildrenByName(ContextPath parentPath, String name) {
        if (!parentPath.startsWith(nodePath)) {
            throw new SecurityException("Cannot list children outside node subtree: " + parentPath);
        }

        return delegate.findChildrenByName(parentPath, name);
    }

    @Override
    public void killProcess(ContextPath path) {
        // Killing a process is sensitive → require MESSAGE capability
        if (!canAccess(nodePath, path, PathCapability.Operation.MESSAGE)) {
            throw new SecurityException("Access denied: cannot kill " + path);
        }

        delegate.killProcess(path);
    }

    @Override
    public void disconnect(ContextPath upstreamPath, ContextPath downstreamPath) {
        // Same pattern as connect()
        boolean upstreamOK = upstreamPath.startsWith(nodePath)
            || canAccess(nodePath, upstreamPath, PathCapability.Operation.MESSAGE);

        boolean downstreamOK = downstreamPath.startsWith(nodePath)
            || canAccess(nodePath, downstreamPath, PathCapability.Operation.MESSAGE);

        if (!upstreamOK || !downstreamOK) {
            throw new SecurityException("Access denied: cannot disconnect "
                    + upstreamPath + " from " + downstreamPath);
        }

        delegate.disconnect(upstreamPath, downstreamPath);
    }

    @Override
    public Set<ContextPath> getUpstreams(ContextPath processPath) {
        if (!processPath.startsWith(nodePath)) {
            throw new SecurityException("Cannot get upstreams outside node subtree: " + processPath);
        }

        // Filter topology relative only to subtree visibility
        return delegate.getUpstreams(processPath)
                .stream()
                .filter(path -> path.startsWith(nodePath))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<ContextPath> getDownstreams(ContextPath processPath) {
        if (!processPath.startsWith(nodePath)) {
            throw new SecurityException("Cannot get downstreams outside node subtree: " + processPath);
        }

        return delegate.getDownstreams(processPath)
                .stream()
                .filter(path -> path.startsWith(nodePath))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean isStreamCapable(ContextPath path) {
        // This reports capability of a process, but should not leak info outside subtree
        if (!path.startsWith(nodePath)) {
            return false;
        }

        return delegate.isStreamCapable(path);
    }
}