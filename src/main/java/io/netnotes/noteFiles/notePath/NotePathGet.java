package io.netnotes.noteFiles.notePath;

import java.io.EOFException;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.crypto.SecretKey;

import io.netnotes.noteFiles.FileStreamUtils;

import io.netnotes.engine.crypto.CryptoService;
import io.netnotes.engine.messaging.NoteMessaging;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.engine.utils.streams.AESBackedInputStream;
import io.netnotes.engine.utils.streams.AESBackedOutputStream;
import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.noteBytes.processing.IntCounter;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;

public class NotePathGet {
    
    
    public static CompletableFuture<NoteBytes> getOrCreateNoteFilePath(NotePath notePath, 
        SecretKey secretKey, ExecutorService execService
    ) {
        File pathLedger = notePath.getPathLedger();
        if(
            pathLedger.exists() && 
            pathLedger.isFile() && 
            pathLedger.length() > CryptoService.AES_IV_SIZE
        ){
            PipedOutputStream decryptedOutput = new PipedOutputStream();
            PipedOutputStream parsedOutput = new PipedOutputStream();
        
            // Chain the operations: decrypt -> parse -> encrypt -> return file
            CompletableFuture<NoteBytesObject> decryptFuture = 
                FileStreamUtils.performDecryption(pathLedger, decryptedOutput, secretKey, execService);
            
            CompletableFuture<NoteBytes> parseFuture = 
                parseStreamToOutputGetOrAddPath(notePath, secretKey, decryptedOutput, parsedOutput, execService);
            
            CompletableFuture<NoteBytesObject> saveFuture = 
                FileStreamUtils.saveEncryptedFileSwap(pathLedger, secretKey, parsedOutput, execService);
            
            // Wait for all operations to complete and return the result file
            return CompletableFuture.allOf(decryptFuture, parseFuture, saveFuture)
                .thenCompose(v -> parseFuture)
                .whenComplete((v, ex)->{
                    StreamUtils.safeClose(decryptedOutput);
                    StreamUtils.safeClose(parsedOutput);
                });
        }else{
            return CompletableFuture.supplyAsync(()->notePath.createNotePathLedger(secretKey));
        }
    }

 

    public static CompletableFuture<NoteBytes> parseStreamToOutputGetOrAddPath(NotePath notePath, SecretKey secretKey,
            PipedOutputStream decryptedInputStream, PipedOutputStream parsedOutputStream, ExecutorService execService) 
    {
        return CompletableFuture
            .supplyAsync(() -> {


                try (
                    NoteBytesReader reader = new NoteBytesReader(new PipedInputStream(decryptedInputStream, StreamUtils.PIPE_BUFFER_SIZE));
                    NoteBytesWriter writer = new NoteBytesWriter(parsedOutputStream)
                ) {
                    
                    NoteBytesReadOnly targetPathKey = notePath.getCurrentPathKey();
                
                    boolean foundRootKey = false;
                    IntCounter byteCounter = new IntCounter();
                    NoteBytes key = reader.nextNoteBytes();
                    if(key != null){
                        byteCounter.add(writer.write(key));
                    }
                    while ( key != null && !(foundRootKey = key.equals(targetPathKey))) {
                        //not found
                        NoteBytesMetaData valueMetaData = reader.nextMetaData();
                        if(valueMetaData != null){
                            byteCounter.add(writer.write(valueMetaData));
                            byteCounter.add(StreamUtils.readWriteNextBytes(valueMetaData.getLength(), reader, writer));
                        }else{
                            throw new IllegalStateException("Corrupted data format found at: " + byteCounter.get() );
                        }
                    }
                    
                    if (foundRootKey) {
                        return processFoundRootKey(notePath, secretKey, reader, writer);
                    } else {
                        return notePath.createNewRootPath(writer);
                    } 
                    
                } catch (Exception e) {
                    throw new RuntimeException(NoteMessaging.Error.INVALID, e);
                }
            }, execService);
    }

    private static NoteBytes processFoundRootKey(NotePath notePath, SecretKey secretKey, 
            NoteBytesReader reader, NoteBytesWriter writer) throws Exception {
        
        // Read metadata for the root key's value
        NoteBytesMetaData rootValueMetaData = reader.nextMetaData();
       
        int rootMetadataLength = rootValueMetaData.getLength();
                
        AESBackedOutputStream divertedRoot = new AESBackedOutputStream(notePath.getDataDir(), secretKey, rootMetadataLength,(int)(StreamUtils.PIPE_BUFFER_SIZE * 0.9));

        IntCounter depthCounter = new IntCounter(notePath.getSize() == 1 ? 0 : 1);
        notePath.setCurrentLevel(depthCounter);
   

        try(NoteBytesWriter tmpWriter = new NoteBytesWriter(divertedRoot)) {
            parseContentWhileWritingToTemp(notePath, rootMetadataLength, reader, tmpWriter);
        }
        NoteBytes filePathResult = notePath.getTargetFilePath();

        if (filePathResult != null) {
            writer.write(rootValueMetaData);

            // File exists - stream temp file back unchanged
            try(NoteBytesReader backedReader = new NoteBytesReader( new AESBackedInputStream(divertedRoot, secretKey))){
                StreamUtils.readWriteBytes(backedReader, writer);
            }
    
            // Continue streaming remaining top-level content
            StreamUtils.readWriteBytes(reader, writer);
            return filePathResult;
        } else {

            NoteBytes newFile = NotePath.generateNewDataFilePath(notePath.getDataDir());
            NoteBytesPair insertionPair = notePath.createFilePath( depthCounter.get(), newFile);

            NoteBytesMetaData newRootValueMetadata = new NoteBytesMetaData(rootValueMetaData.getType(), rootValueMetaData.getLength() + insertionPair.byteLength());
            writer.write(newRootValueMetadata);

            try (
                NoteBytesReader backedReader = new NoteBytesReader(new AESBackedInputStream(divertedRoot, secretKey))
            ){  
                notePath.getByteCounter().set(0);
                int insertionDepth = notePath.getCurrentLevel().get();
                notePath.getCurrentLevel().set(1);
                streamBackWithSizeAdjustments(notePath, insertionPair, insertionDepth, backedReader, writer);
                StreamUtils.readWriteBytes(backedReader, writer);
            }
            // Continue streaming remaining top-level content  
            StreamUtils.readWriteBytes(reader, writer);

            return newFile;
        }
    }

    private static boolean parseContentWhileWritingToTemp(NotePath notePath, int contentLength, NoteBytesReader reader, 
        NoteBytesWriter tmpWriter) throws Exception 
    {
        IntCounter depthCounter = notePath.getCurrentLevel();
        int currentDepth = depthCounter.get();
        int maxLevel = notePath.getSize();
        IntCounter byteCounter = notePath.getByteCounter();

        while(byteCounter.get() < contentLength){
            if(currentDepth == 0){
                notePath.getCurrentLevel().increment();
                return notePath.searchForFilePathKey(contentLength, reader, tmpWriter);
                
            }else if(
                currentDepth < maxLevel
            ){
                NoteBytesReadOnly targetKey = notePath.getCurrentPathKey();
                NoteBytes key = reader.nextNoteBytes();

                int keySize = tmpWriter.write(key);
                byteCounter.add(keySize);
                if(key != null){
                    if(targetKey.equals(key)){
                        depthCounter.increment();
                        NoteBytesMetaData metaData = reader.nextMetaData();
                        if(metaData != null ){
                            byteCounter.add( tmpWriter.write(metaData));
                            if(currentDepth == maxLevel -1){
                                return notePath.searchForFilePathKey(metaData.getLength(), reader, tmpWriter); 
                            }else{
                                return parseContentWhileWritingToTemp(notePath, byteCounter.get() + metaData.getLength(), 
                                    reader, tmpWriter);
                            }
                        }else{
                            throw new EOFException(
                                "Unexpected end of file found at: " + 
                                notePath.getTargetPath(0) + " - " + byteCounter.get());
                        }
                    }else{
                        NoteBytes value = reader.nextNoteBytes();
                        byteCounter.add(tmpWriter.write(value));
                    }
                }else{ return false; }
            
            }else{ return false; }
        }
        return false;
    }

    private static boolean streamBackWithSizeAdjustments(NotePath notePath, NoteBytesPair insertionPair, int insertionDepth, 
        NoteBytesReader tmpReader, NoteBytesWriter writer
    ) throws Exception {
        int insertionSizeIncrease = insertionPair.byteLength();
        int currentDepth = notePath.getCurrentLevel().get();
        IntCounter bytesProcessed = notePath.getByteCounter();

        if(currentDepth == insertionDepth && insertionDepth == notePath.getSize()){
            writer.write(insertionPair); 
            return true;
        }else{
            NoteBytes key = tmpReader.nextNoteBytes();
            if (key == null){
                throw new IllegalStateException("Unexpected end of file found");
            }
            NoteBytesReadOnly currentPathKey = notePath.getCurrentPathKey();

            bytesProcessed.add(writer.write(key));
            
            // Read value metadata explicitly
            NoteBytesMetaData valueMetaData = tmpReader.nextMetaData();
            if (valueMetaData == null){
                throw new IllegalStateException("Corrupted end of file found");
            }
            
            if(key.equals(currentPathKey)){
                int newSize = valueMetaData.getLength() + insertionSizeIncrease;
                NoteBytesMetaData adjustedMetaData = new NoteBytesMetaData(valueMetaData.getType(), newSize);
                bytesProcessed.add(writer.write(adjustedMetaData));
                
                if (currentDepth < insertionDepth) {
                    notePath.getCurrentLevel().increment();
                    return streamBackWithSizeAdjustments(notePath, insertionPair, insertionDepth,  tmpReader, writer);

                } else{
                    writer.write(insertionPair);
                    return true;
                } 
            }else{
                bytesProcessed.add(writer.write(valueMetaData));
                bytesProcessed.add(StreamUtils.readWriteNextBytes(valueMetaData.getLength(), tmpReader, writer));

                return streamBackWithSizeAdjustments(notePath, insertionPair, insertionDepth, tmpReader, writer);
            }
        }
      

    }



   
}
