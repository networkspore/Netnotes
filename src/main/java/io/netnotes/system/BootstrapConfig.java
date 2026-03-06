package io.netnotes.system;

import io.netnotes.noteBytes.NoteBoolean;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.utils.virtualExecutors.SerializedVirtualExecutor;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BootstrapConfig {
    private static final LogLevel LOG_LEVEL = LogLevel.IMPORTANT;

    private static final NoteBytesReadOnly START_DETACHED = new NoteBytesReadOnly("startDetached");
    private static final NoteBytesReadOnly CLAIMED_KEYBOARD_ID = new NoteBytesReadOnly("claimedKeyboardId");
    private static final NoteBytesReadOnly IO_DAEMON_SOCKET_PATH = new NoteBytesReadOnly("ioDaemonSocketPath");
    private static final NoteBytesReadOnly RECOVERY_MODE = new NoteBytesReadOnly("recoveryMode");
    private static final NoteBytesReadOnly RECOVERY_REASON = new NoteBytesReadOnly("recoveryReason");
    
    private final NoteBytesMap config;
    
    private BootstrapConfig(NoteBytesMap config) {
        this.config = config;
    }

    
    public static CompletableFuture<BootstrapConfig> load() {
        if(!SettingsData.isSystemConfigData()){
            Log.logMsg("[BootstrapConfig] System config not found starting first run", LOG_LEVEL);
            return CompletableFuture.completedFuture((BootstrapConfig)null);
        }

        return SettingsData.loadSystemConfig(VirtualExecutors.getIoExecutor())
            .handle((config, ex)->{
               if(ex == null){
                    return new BootstrapConfig(config);
               }else{
                    Log.logError("[BootstrapConfig]", ex);
                    if(ex.getCause() != null && ex.getCause() instanceof IOException){
                        throw new CompletionException(ex);
                    }else{
                        return null;
                    }
               }
            });
    }

  
    public static CompletableFuture<Void> save(
        SerializedVirtualExecutor ioExecutor,
        NoteBytes keyboardId,
        String socketPath,
        boolean isInRecoveryMode,
        String recoveryReason,
        boolean startDetached
    ) {
        NoteBytesMap map = new NoteBytesMap();
        if (keyboardId != null) {
            map.put(CLAIMED_KEYBOARD_ID, keyboardId);
        }
        map.put(IO_DAEMON_SOCKET_PATH, socketPath);
        
        if (isInRecoveryMode) {
            map.put(RECOVERY_MODE, isInRecoveryMode);
            if (recoveryReason != null) {
                map.put(RECOVERY_REASON, recoveryReason);
            }
        }
        if(startDetached){
            map.put(START_DETACHED, NoteBoolean.TRUE);
        }
    
        return SettingsData.saveSystemConfig(ioExecutor, map)
            .whenComplete((v,ex)->{
                if(ex != null){
                    Log.logError("[BootstrapConfig]", "Save Failed!", ex);
                }else{
                    Log.logMsg("[SystemApplication] Bootstrap saved" + (isInRecoveryMode ? " (recovery=" + 
                        isInRecoveryMode + " reason: "+recoveryReason+")": ""), LOG_LEVEL);
                }
            });
    }
    

    public CompletableFuture<Void> save(){
        return CompletableFuture.completedFuture(null);
    }
    
    public boolean exists() {
        return config != null;
    }

    private void ensureExists(){
        if(config == null){
            throw new IllegalStateException("Config does not exist");
        }
    }
    
    public boolean isDetachedMode() {
        ensureExists();
        return config.getAsBoolean(START_DETACHED, false);
    }
    
    public Optional<NoteBytes> getClaimedKeyboardId() {
        ensureExists();
        return config.containsKey(CLAIMED_KEYBOARD_ID) 
            ? Optional.of(config.get(CLAIMED_KEYBOARD_ID))
            : Optional.empty();
    }
    
    public Optional<String> getIoDaemonSocketPath() {
        ensureExists();
        return config.containsKey(IO_DAEMON_SOCKET_PATH)
            ? Optional.of(config.get(IO_DAEMON_SOCKET_PATH).toString())
            : Optional.empty();
    }
    
    public boolean isRecoveryMode() {
        ensureExists();
        return config.containsKey(RECOVERY_MODE) && 
               Boolean.parseBoolean(config.get(RECOVERY_MODE).toString());
    }
    
    public Optional<String> getRecoveryReason() {
        ensureExists();
        return config.containsKey(RECOVERY_REASON)
            ? Optional.of(config.get(RECOVERY_REASON).toString())
            : Optional.empty();
    }
}