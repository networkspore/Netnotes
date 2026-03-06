package io.netnotes.system.nodes;


import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteFiles.NoteFile;
import io.netnotes.noteFiles.NoteFileServiceInterface;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.LoggingHelpers.LogLevel;
import io.netnotes.engine.utils.streams.UrlStreamHelpers;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

/**
 * PackageInstaller - Downloads and installs packages
 * 
 * Install process:
 * 1. Download package files
 * 2. Parse and verify manifest.json
 * 3. Store files in encrypted NoteFiles
 * 4. Register in InstallationRegistry
 * 
 * Does NOT load into runtime
 */
public class PackageInstaller {

    private final NoteFileServiceInterface appDataInterface;
    public PackageInstaller(NoteFileServiceInterface appDataInteface) {
        this.appDataInterface = appDataInteface;
    }
    
    public CompletableFuture<ContextPath> installPackage(PackageInfo pkgInfo) {
        Log.logMsg("[PackageInstaller] Installing " + pkgInfo.getName(), LogLevel.IMPORTANT);
        
        ContextPath installPath = pkgInfo.createInstallPath();
        
        return appDataInterface
            .getNoteFile( installPath)
            .thenCompose(noteFile -> downloadToNoteFile(pkgInfo.getDownloadUrl(), noteFile))
            .thenApply(v -> {
       
                return installPath;
            });
    }
    
    private CompletableFuture<NoteBytesObject> downloadToNoteFile(
            String downloadUrl,
            NoteFile noteFile) {
        
        PipedOutputStream outputStream = new PipedOutputStream();
        
        CompletableFuture<Void> downloadFuture = UrlStreamHelpers.transferUrlStream(
            downloadUrl, outputStream, null, VirtualExecutors.getVirtualExecutor());
        
        CompletableFuture<NoteBytesObject> writeFuture = 
            noteFile.writeOnly(outputStream);
        
        return CompletableFuture.allOf(downloadFuture, writeFuture)
            .thenCompose(v -> writeFuture);
    }
}