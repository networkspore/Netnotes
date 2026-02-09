package io.netnotes.noteFiles.notePath;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.SecretKey;

import io.netnotes.noteFiles.FileStreamUtils;

import io.netnotes.engine.crypto.CryptoService;
import io.netnotes.engine.messaging.NoteMessaging;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.ProgressMessage;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;

import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.LongCounter;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;

public class NotePathReEncryption {

    public static CompletableFuture<Boolean> updatePathLedgerEncryption(File pathLedger, SecretKey oldSecretKey,
        SecretKey newSecretKey, int batchSize, AsyncNoteBytesWriter progressWriter, ExecutorService execService
    ) {
    
        if(oldSecretKey == null || newSecretKey == null){
            return CompletableFuture.failedFuture(new RuntimeException("Valid keys required"));
        }
        if(
            !pathLedger.exists() || 
            !pathLedger.isFile() || 
            pathLedger.length() < CryptoService.AES_IV_SIZE
        ){
            return CompletableFuture.failedFuture(new RuntimeException("Path ledger file is not valid"));
        }
        if(progressWriter == null){
            return CompletableFuture.failedFuture(new RuntimeException("Progress update callback cannot be null"));
        }
            
        PipedOutputStream decryptedOutput = new PipedOutputStream();
        PipedOutputStream parsedOutput = new PipedOutputStream();
    
        CompletableFuture<NoteBytesObject> decryptFuture = 
            FileStreamUtils.performDecryption(pathLedger, decryptedOutput, oldSecretKey, execService);
        
        CompletableFuture<CompletableFuture<Void>> parseFuture = parseStreamUpdateEncryption(batchSize, pathLedger.length(), 
            oldSecretKey, newSecretKey, decryptedOutput, parsedOutput, progressWriter, execService);
        
        CompletableFuture<NoteBytesObject> saveFuture = 
            FileStreamUtils.saveEncryptedFileSwap(pathLedger, newSecretKey, parsedOutput, execService);
        
        return CompletableFuture.allOf(decryptFuture, parseFuture, saveFuture)
            .whenComplete((v,ex) ->{
                //close streams as soon as they complete
                StreamUtils.safeClose(decryptedOutput);
                StreamUtils.safeClose(parsedOutput);
            })    
            .thenCompose(v -> parseFuture) // wait for all files to finish updating
            .thenCompose(v-> saveFuture)
            .thenApply(v->true);
    }

    public static CompletableFuture<CompletableFuture<Void>> parseStreamUpdateEncryption(int batchSize, long fileSize,
        SecretKey oldKey, SecretKey newKey, PipedOutputStream decryptedOutput, PipedOutputStream parsedOutput,
        AsyncNoteBytesWriter progressWriter, ExecutorService execService
    ){
        return CompletableFuture
            .supplyAsync(() -> {
                Semaphore fileUpdateSemaphore = new Semaphore(batchSize, true);
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                try (PipedInputStream decryptedInput = new PipedInputStream(decryptedOutput);
                    NoteBytesReader reader = new NoteBytesReader(decryptedInput);
                    NoteBytesWriter writer = new NoteBytesWriter(parsedOutput)
                ) {

                    ProgressMessage.writeAsync(ProtocolMesssages.INFO,
                        12, fileSize, "Finding file paths", progressWriter);
                    
                    rootLevelParse(futures, fileSize, fileUpdateSemaphore, oldKey, newKey, reader, writer, 
                        progressWriter, execService);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse and update data structure", e);
                }
                
                AtomicInteger completedCount = new AtomicInteger(0);
                int totalFutures = futures.size();

                return CompletableFuture.allOf(
                    futures.stream()
                        .map(future -> future.whenComplete((result, ex) -> {
                            int completed = completedCount.incrementAndGet();
                            ProgressMessage.writeAsync(ProtocolMesssages.UPDATED,
                               completed, totalFutures, ProtocolMesssages.SUCCESS, progressWriter);
                    })).toArray(CompletableFuture[]::new)
                );

        }, execService);

    }

    public static void rootLevelParse( List<CompletableFuture<Void>> futures, long fileSize, Semaphore fileUpdateSemaphore, 
        SecretKey oldKey, SecretKey newKey, NoteBytesReader reader, NoteBytesWriter writer, 
        AsyncNoteBytesWriter progressWriter, ExecutorService execService
    ) throws Exception {

        LongCounter byteCounter = new LongCounter(0);
        
        NoteBytes rootKey = reader.nextNoteBytes();

        while(rootKey != null){
            byteCounter.add(writer.write(rootKey));
            if(rootKey.equals(NotePath.FILE_PATH)){
                NoteBytes filePath = reader.nextNoteBytes();
                byteCounter.add(writer.write(filePath));
                futures.add(queueEncryptionUpdate(fileUpdateSemaphore, byteCounter, fileSize, oldKey, newKey, filePath, 
                    progressWriter, execService));
            }else if(rootKey.getType() == NoteBytesMetaData.STRING_TYPE){
                NoteBytesMetaData metaData = reader.nextMetaData();
                if(metaData != null && metaData.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
                    byteCounter.add(writer.write(metaData));
                    if(!recursivelyParseQueueEncryption(futures, fileUpdateSemaphore, byteCounter, fileSize, 
                        byteCounter.get() + metaData.getLength(), oldKey, newKey, reader, writer, progressWriter,
                        execService)
                    ){
                        return;
                    }
                }else{
                     throw new IllegalStateException("Invalid value data detected: " + metaData == null ? 
                        "metadata null" : "type: " + (int) metaData.getType());
                }
            }else{
                throw new IllegalStateException("Invalid key type: " + (int) rootKey.getType());
            }

            rootKey = reader.nextNoteBytes();
        }
    }

    public static boolean recursivelyParseQueueEncryption( List<CompletableFuture<Void>> futures, 
        Semaphore fileUpdateSemaphore, LongCounter byteCounter, long fileSize, long bucketEnd, SecretKey oldKey, 
        SecretKey newKey, NoteBytesReader reader, NoteBytesWriter writer, AsyncNoteBytesWriter progressWriter, 
        ExecutorService execService
    ) throws Exception {
       
        while(byteCounter.get() < bucketEnd){
            
            NoteBytes foundKey = reader.nextNoteBytes();
            if(foundKey == null){
                return false;
            }
            byteCounter.add(writer.write(foundKey));
            if(foundKey.equals(NotePath.FILE_PATH)){
                NoteBytes filePath = reader.nextNoteBytes();
                byteCounter.add(writer.write(filePath));
                futures.add(queueEncryptionUpdate(fileUpdateSemaphore, byteCounter, fileSize, oldKey, newKey, filePath, 
                    progressWriter, execService));
            }else if (foundKey.getType() == NoteBytesMetaData.STRING_TYPE){
                NoteBytesMetaData metaData = reader.nextMetaData();
                if(metaData != null && metaData.getType() == NoteBytesMetaData.NOTE_BYTES_OBJECT_TYPE){
                    byteCounter.add(writer.write(metaData));
                    recursivelyParseQueueEncryption(futures, fileUpdateSemaphore, byteCounter, fileSize,
                        byteCounter.get() + metaData.getLength(), oldKey, newKey, reader, writer, progressWriter, 
                        execService);
                }else{
                    throw new IllegalStateException("Invalid value data detected: " + metaData == null ? 
                        "metadata null" : "type: " + (int) metaData.getType());
                }
            }else{
                throw new IllegalStateException("Invalid key type: " + (int) foundKey.getType());
            }
        }
        return true;
    }

    private static CompletableFuture<Void> queueEncryptionUpdate(Semaphore fileUpdateSemaphore, LongCounter byteCounter, 
        long fileSize, SecretKey oldKey, SecretKey newKey, NoteBytes filePath, AsyncNoteBytesWriter progressWriter,
        ExecutorService execService
    ) throws InterruptedException {
        return  CompletableFuture.runAsync(() -> {
            String filePathString = filePath.getAsString();
            File file = new File(filePathString);
            if(file.exists() && file.isFile() && file.length() > 12){
                try{
                    File tmpFile = new File(file.getAbsolutePath() + ".tmp");
                    fileUpdateSemaphore.acquire();
                    try{
                        ProgressMessage.writeAsync(ProtocolMesssages.INFO,
                            byteCounter.get(), fileSize, file.getAbsolutePath(), progressWriter);
                        
                        FileStreamUtils.updateFileEncryption(oldKey, newKey, file, tmpFile, progressWriter);

                    }catch(Exception e){
                        TaskMessages.writeErrorAsync(ProtocolMesssages.ERROR, file.getAbsolutePath() , e, progressWriter);
                    }finally{
                        try{
                            Files.deleteIfExists(tmpFile.toPath());
                        }catch(IOException e){
                            TaskMessages.writeErrorAsync(NoteMessaging.Error.IO_DELETION, tmpFile.getAbsolutePath() , e, progressWriter);
                        }
                        fileUpdateSemaphore.release();
                    }
                    return;
                }catch(InterruptedException e){
                    return;    
                }
              
            }else{
                ProgressMessage.writeAsync(ProtocolMesssages.INFO,
                    byteCounter.get(), fileSize, file.getAbsolutePath(), progressWriter);
            }
        }, execService);

    } 
    
}
