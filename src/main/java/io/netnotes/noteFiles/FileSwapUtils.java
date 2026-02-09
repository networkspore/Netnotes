package io.netnotes.noteFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.NoteSerializable;
import io.netnotes.noteBytes.NoteBytesObject;

public class FileSwapUtils {
    
    public static NoteBytesObject performAtomicFileSwap(File originalFile, File tmpFile, long bytesWritten) {
        File backupFile = new File(originalFile.getAbsolutePath() + ".bak");
        int operationStep = 0;
        Exception lastException = null;
        Path backupPath = backupFile.toPath();
        Path originalPath = originalFile.toPath();
        Path tmpPath = tmpFile.toPath();

        try {
            
            // Step 1: Create backup of original file
            if (originalFile.exists()) {
                Files.move(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                operationStep = 1;
            }
            
            // Step 2: Move new file into place
            Files.move(tmpPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            operationStep = 2;
            
            // Step 3: Clean up backup file
            Files.deleteIfExists(backupPath);
            
            
            // Success case
            NoteBytesObject result = TaskMessages.getTaskMessage(ProtocolMesssages.UPDATED,
                ProtocolMesssages.SUCCESS, "File length:" + bytesWritten);
            result.add("completionStage", 2);
            return result;
            
        } catch (IOException e) {
            lastException = e;
            return handleFileSwapFailure(originalFile, tmpFile, backupFile, 
                                    operationStep, bytesWritten, lastException);
        }
    }

    public static NoteBytesObject handleFileSwapFailure(File originalFile, File tmpFile, File backupFile,
                                                int failedStep, long bytesWritten, Exception originalException) {
        NoteBytesObject errorResult;
        
        switch (failedStep) {
            case 0:
                // Failed to backup original file
                errorResult = TaskMessages.createErrorMessage( originalFile.getAbsolutePath(),
                    "Failed to backup original file", originalException);
                break;
                
            case 1:
                // Failed to move new file into place - attempt recovery
                errorResult = TaskMessages.createErrorMessage(
                    "moveFile:" + tmpFile.getAbsolutePath() + "->" + originalFile.getAbsolutePath(),
                    "Failed to move new file into place", originalException);
                    
                // Attempt to restore from backup
                if (backupFile.exists()) {
                    attemptFileRecovery(originalFile, backupFile, errorResult);
                }
                break;
                
            default:
                // Unexpected failure
                errorResult = TaskMessages.createErrorMessage(
                    originalFile.getAbsolutePath(), "Unexpected failure during file swap", originalException);
                break;
        }
        
        errorResult.add("completionStage", failedStep);
        errorResult.add("bytesWritten", bytesWritten);
        return errorResult;
    }

    public static void attemptFileRecovery(File originalFile, File backupFile, NoteBytesObject errorResult) {
        try {
            Path originalPath = originalFile.toPath();
            Path backupPath = backupFile.toPath();
            // Remove potentially corrupted file
            Files.deleteIfExists(originalPath);
            // Restore from backup
            Files.move(backupPath, originalPath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException recoveryEx) {
            try {
                errorResult.add("recoveryException", new NoteSerializable(recoveryEx));
            } catch (IOException serializationEx) {
                errorResult.add("recoveryException", "File recovery failed - unable to serialize exception");
            }
        }
    }

}
