package io.netnotes.system.nodes;

/**
 * NodeState - Lifecycle states for nodes
 */
public enum NodeState {
    LOADING,        // Being loaded from package
    INITIALIZING,   // INode.initialize() in progress
    RUNNING,        // Active and ready
    STOPPING,       // Shutdown requested
    STOPPED,        // Fully stopped
    CRASHED,        // Unexpected termination
    SUSPENDED       // Temporarily paused (future feature)
}