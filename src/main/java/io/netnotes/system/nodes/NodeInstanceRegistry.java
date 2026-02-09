package io.netnotes.system.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.netnotes.noteBytes.NoteBytesReadOnly;

public class NodeInstanceRegistry {
    // Primary index: instanceId -> instance
    private final Map<InstanceId, NodeInstance> byInstanceId = new ConcurrentHashMap<>();
    
    // Secondary indexes for queries
    private final Map<PackageId, Set<InstanceId>> byPackageId = new ConcurrentHashMap<>();
    private final Map<NoteBytesReadOnly, Set<InstanceId>> byProcessId = new ConcurrentHashMap<>();
    
    /**
     * Register a new instance
     */
    public void register(NodeInstance instance) {
        InstanceId instanceId = instance.getInstanceId();
        PackageId packageId = instance.getPackageId();
        NoteBytesReadOnly processId = instance.getProcessId();
        
        // Primary index
        byInstanceId.put(instanceId, instance);
        
        // Secondary indexes
        byPackageId.computeIfAbsent(packageId, k -> ConcurrentHashMap.newKeySet())
            .add(instanceId);
        byProcessId.computeIfAbsent(processId, k -> ConcurrentHashMap.newKeySet())
            .add(instanceId);
    }
    
    /**
     * Unregister an instance
     */
    public void unregister(InstanceId instanceId) {
        NodeInstance instance = byInstanceId.remove(instanceId);
        if (instance != null) {
            byPackageId.get(instance.getPackageId()).remove(instanceId);
            byProcessId.get(instance.getProcessId()).remove(instanceId);
        }
    }
    
    /**
     * Get instance by ID
     */
    public NodeInstance getInstance(InstanceId instanceId) {
        return byInstanceId.get(instanceId);
    }
    
    /**
     * Get all instances of a package (across different processIds)
     */
    public List<NodeInstance> getInstancesByPackage(PackageId packageId) {
        Set<InstanceId> instanceIds = byPackageId.get(packageId);
        if (instanceIds == null) return List.of();
        
        return instanceIds.stream()
            .map(byInstanceId::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    /**
     * Get all instances in a process namespace
     */
    public List<NodeInstance> getInstancesByProcess(NoteBytesReadOnly processId) {
        Set<InstanceId> instanceIds = byProcessId.get(processId);
        if (instanceIds == null) return List.of();
        
        return instanceIds.stream()
            .map(byInstanceId::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    /**
     * Get all running instances
     */
    public List<NodeInstance> getAllInstances() {
        return new ArrayList<>(byInstanceId.values());
    }
    
    /**
     * Check if package is loaded (any processId)
     */
    public boolean isPackageLoaded(PackageId packageId) {
        Set<InstanceId> instances = byPackageId.get(packageId);
        return instances != null && !instances.isEmpty();
    }
    
    /**
     * Check if specific package+process combination is loaded
     */
    public boolean isLoaded(PackageId packageId, NoteBytesReadOnly processId) {
        return getInstancesByPackage(packageId).stream()
            .anyMatch(inst -> inst.getProcessId().equals(processId));
    }
}