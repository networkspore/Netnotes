package io.netnotes.system;

import java.util.concurrent.CompletableFuture;

import io.netnotes.terminal.TerminalContainerHandle;
import io.netnotes.terminal.TerminalDeviceManager;
import io.netnotes.engine.io.daemon.ClaimedDevice;
import io.netnotes.engine.io.input.events.EventFilter;
import io.netnotes.engine.io.input.events.keyboardEvents.RoutedKeyboardEvent;
import io.netnotes.engine.messaging.NoteMessaging.ItemTypes;
import io.netnotes.engine.utils.LoggingHelpers.Log;

/**
 * PasswordKeyboardManager - Manages password keyboard with two modes
 * 
 * MODES:
 * 1. EXCLUSIVE (Authentication) - Blocks all other keyboard events
 *    - Used during login/password entry
 *    - Filters out all RoutedKeyboardEvent except from this device
 * 
 * 2. NON_EXCLUSIVE (General Use) - Allows all keyboard events
 *    - Used as primary keyboard when handle is visible
 *    - No filtering applied
 * 
 * USAGE:
 * <pre>
 * // Create manager
 * PasswordKeyboardManager pwdMgr = new PasswordKeyboardManager(
 *     "password-keyboard-id",
 *     ClientSession.Modes.PARSED.toString()
 * );
 * 
 * // Attach to handle
 * handle.addDeviceManager("password-keyboard", pwdMgr);
 * 
 * // Enable in exclusive mode (for authentication)
 * pwdMgr.setExclusiveMode(true);
 * pwdMgr.enable().thenRun(() -> {
 *     // Only password keyboard events pass through
 * });
 * 
 * // Switch to non-exclusive mode (general use)
 * pwdMgr.setExclusiveMode(false);
 * // No need to disable/re-enable, filters update automatically
 * 
 * // Disable when done
 * pwdMgr.disable();
 * </pre>
 */
public class PasswordKeyboardManager extends TerminalDeviceManager {
    
    private boolean exclusiveMode = false;
    private String filterId; // Filter ID for cleanup
    // ===== CONSTRUCTION =====
    
    public PasswordKeyboardManager(String deviceId, String deviceMode) {
        super(deviceId, deviceMode, ItemTypes.KEYBOARD.getAsString());
    }


    // ===== LIFECYCLE HOOKS =====
    
    @Override
    protected CompletableFuture<Void> onAttached(TerminalContainerHandle handle) {
        Log.logMsg("[PasswordKeyboard] Attached to terminal handle: " + handle.getId());
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected CompletableFuture<Void> onDeviceEnabled(ClaimedDevice device) {
        Log.logMsg("[PasswordKeyboard] Device enabled");
        Log.logMsg("[PasswordKeyboard] Source path: " + deviceSourcePath);
        
        if (exclusiveMode) {
            Log.logMsg("[PasswordKeyboard] EXCLUSIVE mode - filtering other keyboards");
        } else {
            Log.logMsg("[PasswordKeyboard] NON-EXCLUSIVE mode - all keyboards allowed");
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> onDeviceDisabled() {
        Log.logMsg("[PasswordKeyboard] Device disabled");
        
        if (exclusiveMode) {
            Log.logMsg("[PasswordKeyboard] Normal keyboard input restored");
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ===== EXCLUSIVITY CONFIGURATION =====
    
    @Override
    protected CompletableFuture<Boolean> requiresExclusiveAccess() {
        return serialExec.submit(() -> exclusiveMode);
    }
    
    @Override
    protected CompletableFuture<Void> configureEventFilters() {
        return serialExec.submit(() -> {
            if (deviceSourcePath == null) {
                throw new IllegalStateException(
                    "[PasswordKeyboard] Cannot configure filters - no source path");
            }
            
            if (!exclusiveMode) {
                // Non-exclusive mode - no filtering needed
                return null;
            }
            
            // Exclusive mode - block all keyboard events except from this device
            EventFilter filter = EventFilter.builder()
                .customPredicate(e -> 
                    e instanceof RoutedKeyboardEvent && 
                    !e.getSourcePath().equals(deviceSourcePath)
                )
                .id(deviceId + "-exclusive-filter")
                .build();
            return filter;
        }).thenCompose(filter->{
            return handle.filterListAddPredicateIfNotExists(filter)
                .thenAccept(added -> {
                    if (added) {
                        filterId = filter.getId();
                        Log.logMsg("[PasswordKeyboard] Exclusive filter configured - " +
                            "blocking all keyboard events except from: " + deviceSourcePath);
                    } else {
                        Log.logMsg("[PasswordKeyboard] Cannot add filter:" +
                            (filterId != null 
                                ? " filterId already exists: " + filterId
                                : " rejected")
                        );
                    }    
                });
        });
    }

    private CompletableFuture<String> getFilterId(){
        return serialExec.submit(()->{
            return filterId;
        });
    }
    
    @Override
    protected CompletableFuture<Void> removeEventFilters() {
        return getFilterId()
            .thenCompose(fId -> {
                if (fId != null) {
                    return handle.filterListRemoveEventFilterById(fId)
                        .thenAccept(removed -> {
                            Log.logMsg("[PasswordKeyboard] Exclusive filter removed");
                            filterId = null;
                        });
                }
                return CompletableFuture.completedFuture(null);
            });
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Exception thrown when password keyboard is required but not configured
     */
    public static class NoPasswordKeyboardException extends IllegalStateException {
        public NoPasswordKeyboardException() {
            super("Password keyboard is not configured");
        }
        
        public NoPasswordKeyboardException(String message) {
            super(message);
        }
    }

    public static class NoSetupPequiredException extends IllegalStateException {
        public NoSetupPequiredException() {
            super("Setup is not required");
        }
        
        public NoSetupPequiredException(String message) {
            super(message);
        }
    }

    /**
     * Set whether this device requires exclusive access
     * 
     * @param exclusive true for authentication mode (blocks other keyboards),
     *                  false for general use (allows all keyboards)
     */
    public CompletableFuture<Void> setExclusiveMode(boolean exclusive) {
        return serialExec.submit(() -> {
            if (this.exclusiveMode == exclusive) {
                return (Boolean) null; // No change
            }
            
            boolean wasExclusive = this.exclusiveMode;
            this.exclusiveMode = exclusive;
            
            Log.logMsg("[PasswordKeyboard] Mode changed: " + 
                (exclusive ? "EXCLUSIVE" : "NON-EXCLUSIVE"));
            
            return wasExclusive;
        }).thenCompose(wasExclusive->{
                if(wasExclusive != null){
                    // If device is currently enabled, update filters
                    if (deviceSourcePath != null && handle != null) {
                        if (exclusive && !wasExclusive) {
                            // Switched to exclusive - add filters
                            return configureEventFilters();
                        } else if (!exclusive && wasExclusive) {
                            // Switched to non-exclusive - remove filters
                            return removeEventFilters();
                        }
                    }
                }
                return CompletableFuture.completedFuture(null);
            });
    }

    /**
     * Check if currently in exclusive mode
     */
    public CompletableFuture<Boolean> isExclusiveMode() {
        return serialExec.submit(() -> exclusiveMode);
    }

  

}