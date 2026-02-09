package io.netnotes.noteFiles.notePath;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import javax.crypto.SecretKey;

import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.utils.streams.AESBackedInputStream;
import io.netnotes.engine.utils.streams.AESBackedOutputStream;
import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;

public class NotePathDelete {
    
    private static final int  metaDataSize  = NoteBytesMetaData.STANDARD_META_DATA_SIZE;
    

    public static CompletableFuture<Void> deleteFromPath(NotePath notePath, SecretKey secretKey, 
        PipedOutputStream decryptionOutput, PipedOutputStream parsedOutputStream, ExecutorService execService
    ) {
        return CompletableFuture
            .runAsync(() -> {
                notePath.progressMsg(ProtocolMesssages.STARTING, 4, 4, 
                "Initialized, ledger parse starting");
                

                try (
                    NoteBytesReader reader = new NoteBytesReader(new PipedInputStream(decryptionOutput, StreamUtils.PIPE_BUFFER_SIZE));
                    NoteBytesWriter writer = new NoteBytesWriter(parsedOutputStream)
                ) {      
                    NoteBytes rootPathKey = notePath.getTargetPath(0);
                    NoteBytes key = reader.nextNoteBytes();
                   
                    while(key != null){
                        NoteBytesMetaData metaData = reader.nextMetaData();
                        if(metaData == null){
                            throw new IllegalArgumentException("parseStreamForRoot: Unexpected end of file after key: " + key );
                        }
                        if(key.equals(rootPathKey)){
                            
                            //increment level and add metaData to path
                            notePath.addMetaData(metaData);
                            int rootBucketSize = metaData.getLength();
                            if(notePath.getSize() == 1 && notePath.isRecursive()){

                                notePath.progressMsg(ProtocolMesssages.PROCESSING,0, rootBucketSize,
                                    "Path found: recursive pruning from file");

                                //entire root path and all children deleted
                                recursiveDeleteFull(notePath, rootBucketSize, reader);
                                StreamUtils.readWriteBytes(reader, writer);
                             
                            }else{
                                notePath.progressMsg(ProtocolMesssages.PROCESSING,0, rootBucketSize,
                                    "Path found: Finding end point");

                                processFoundRootKey(notePath, secretKey, reader, writer);
                            }
                        }else{
                            writer.write(key);
                            writer.write(metaData);
                            StreamUtils.readWriteNextBytes(metaData.getLength(), reader, writer);
                        }

                    }
                    
                } catch (Exception e) {
                    String msg = "Critical error, failed before completion";
                    notePath.errorMsg(ProtocolMesssages.STOPPING,  msg, e);

                    throw new RuntimeException(msg, e);
                }
                
            }, execService);
    }

    

    private static void processFoundRootKey(NotePath notePath,  SecretKey secretKey, NoteBytesReader reader, 
        NoteBytesWriter writer) throws Exception 
    {
        
        AESBackedOutputStream aesbos = new AESBackedOutputStream(notePath.getDataDir(), secretKey, notePath.getRootPathSize(), 
            (int)(StreamUtils.PIPE_BUFFER_SIZE * 0.9));

        try(
            NoteBytesWriter tmpWriter = new NoteBytesWriter(aesbos);
        ){
            processNotePathToStream(notePath, notePath.getRootPathSize(), reader, tmpWriter);
        }

        if(notePath.getByteCounter().get() != 0){

            try(NoteBytesReader tmpReader = new NoteBytesReader(
                new AESBackedInputStream(aesbos, secretKey)
            )){
                notePath.getCurrentLevel().set(0);
                notePath.getByteCounter().set(0);
              
                readBackPathFromTmp(notePath, notePath.getRootPathSize() - notePath.getDeletedFilePathLength().get(), 
                    tmpReader, writer);
         
                
            }
        }
        StreamUtils.readWriteBytes(reader, writer);
        return;
    }

    private static boolean readBackPathFromTmp( NotePath notePath, int bucketSize, NoteBytesReader tmpReader, 
        NoteBytesWriter writer
    ) throws IOException{
        
        while(notePath.getByteCounter().get() < bucketSize){
            
            int currentLevel = notePath.getCurrentLevel().get();
            boolean recursive = notePath.isRecursive();
            int pathSize =  notePath.getSize();

            //if recursive prior recursive delete removed path at pathSize - 1
            //-path of size 1 and recursive can be discounted as this case is handled earlier
            //-otherwise if not recursive process to pathSize, to determine if path can be hidden
            if(currentLevel < (recursive ? pathSize - 1 : pathSize)){
                NoteBytes nextNoteBytes = tmpReader.nextNoteBytes();
               
                if(nextNoteBytes == null){
                    throw new IllegalArgumentException("readBackPathFromTmp: Unexpected end of file:" + notePath.getByteCounter().get() + " expected:" + bucketSize);
                }

                if(nextNoteBytes.equals(notePath.getCurrentPathKey())){
                    //add keyByteSize to counter
                    int keyByteSize = (nextNoteBytes.byteLength() + metaDataSize);
                    notePath.getByteCounter().add(keyByteSize);
                    
                    
                    //read value metadata and add to byteCounter
                    NoteBytesMetaData valueMetaData = tmpReader.nextMetaData();
                    notePath.getByteCounter().add(metaDataSize);

                    //ensure prior path level size matches stored size - to ensure sync
                    int currentPathBucketSize = notePath.getCurrentPathSize();
                    if(valueMetaData.getLength() != currentPathBucketSize){
                        throw new IllegalArgumentException("readBackPathFromTmp: Mismatched bucket size found: " + valueMetaData.getLength() + " expected: " + currentPathBucketSize);
                    }
                    
                    notePath.getCurrentLevel().increment();
                    int minTargetPathSize =  minTargetPathSizeBelowDeletion(notePath, currentLevel);

                    int adjustedBucketSize = currentPathBucketSize - notePath.getDeletedFilePathLength().get();
                    
                    //Check if path has been hidden by having no further file paths below it
                    if(adjustedBucketSize > minTargetPathSize){
                        //path is greater than minimum size so it cannot be hidden
                        writer.write(nextNoteBytes);
                        
                        //adjust metadata length to new size
                        NoteBytesMetaData adjustedMetaData = new NoteBytesMetaData(valueMetaData.getType(), 
                            adjustedBucketSize);
                        writer.write(adjustedMetaData);
    
                        //no further reading on this level is required, just move to next level and continue processing
                        return readBackPathFromTmp(notePath, notePath.getByteCounter().get() + currentPathBucketSize, 
                            tmpReader, writer);
                       
                    }else{
                        //path has been collpapsed so skip entire bucket less prior deleted length
                        tmpReader.skipData(adjustedBucketSize);
                        notePath.getByteCounter().add(adjustedBucketSize);
                        return true;
                    }
                }else{
                    //key is not on the current path, add to bytecounter and write to output stream
                    notePath.skipKeyValue(nextNoteBytes, tmpReader, writer);
                }
            
            }else {
               StreamUtils.readWriteBytes(tmpReader, writer);
            }

        }

        return true;
    }


    private static boolean recursiveDeleteFull(NotePath notePath, int bucketEnd, NoteBytesReader reader) throws IOException 
    {

        while(notePath.getByteCounter().get() < bucketEnd){
            NoteBytes key = reader.nextNoteBytes();
      
            if(key != null){
                int keyByteSize = (key.byteLength() + metaDataSize);

                notePath.getDeletedFilePathLength().add(keyByteSize);
                notePath.getByteCounter().add(keyByteSize);


                byte keyType = key.getType();

                if(key.equals(NotePath.FILE_PATH)){

                    NoteBytes filePathValue = reader.nextNoteBytes();
                    int filePathValueSize = (filePathValue.byteLength() + metaDataSize);
                    notePath.getByteCounter().add( filePathValueSize);
                    boolean isTarget = notePath.getTargetFilePath() == null;
                    if(isTarget){
                        notePath.setTargetFilePath(filePathValue);
                        
                    }
                    notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(),isTarget ? "Target file path found" : "File path found");
                    
                    notePath.getDeletedFilePathLength().add(filePathValueSize);
                    notePath.deleteFilePathValue(filePathValue, notePath.getCompletableList());

                }else if(keyType == NoteBytesMetaData.STRING_TYPE){
                    NoteBytesMetaData metaData = reader.nextMetaData();
                
                    notePath.getByteCounter().add(metaDataSize);
                    notePath.getDeletedFilePathLength().add( metaDataSize);

                    notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(), "Nested path found: " + key.getAsString());
                        
                    recursiveDeleteFull(notePath, notePath.getByteCounter().get() + metaData.getLength(), reader);
                }else{
                    String msg = "key: " + key.getAsString() + " type: " + keyType + " at: " + notePath.getByteCounter().get();
                    notePath.errorMsg("recursiveDeleteFull: " + notePath.getByteCounter().get(),
                        msg, null);

                    notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(), msg);
                    throw new IllegalArgumentException(msg);
                }
            }else{
                String msg = "Unexpected end of file:" + notePath.getByteCounter().get() + " expected:" + bucketEnd;
                notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(), msg);
                 throw new IllegalArgumentException(msg);
            }
        }
        return true;
    }




   
    private static boolean processNotePathToStream( NotePath notePath, int bucketSize, NoteBytesReader reader, 
        NoteBytesWriter writer) throws IOException
    {
        
        while(notePath.getByteCounter().get() < bucketSize){
            NoteBytes nextNoteBytes = reader.nextNoteBytes();
            
            if(nextNoteBytes == null){
                if(notePath.getByteCounter().get() < bucketSize){
                    String msg = "Unexpected end of file";
                    notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(), msg);
                    throw new IllegalArgumentException(msg);
                }else{
                    return false;
                }
            }

            NoteBytesReadOnly currentPath = notePath.getCurrentPathKey();
        
            if(notePath.getByteCounter().get() < bucketSize){
                if(nextNoteBytes.equals(NotePath.FILE_PATH)){
                    //isTargetFilePath non-recursive delete
                    if( notePath.getCurrentLevel().get() == notePath.getSize()){
                        NoteBytes filePathValue = reader.nextNoteBytes();

                        notePath.setTargetFilePath(filePathValue);
                        notePath.deleteFilePathValue(filePathValue, notePath.getCompletableList());
                     
                        int filePathSize = metaDataSize + nextNoteBytes.byteLength() + 
                            metaDataSize + filePathValue.byteLength();

                        notePath.getByteCounter().add(filePathSize);
                        notePath.getDeletedFilePathLength().add(filePathSize);

                        notePath.progressMsg(ProtocolMesssages.PROCESSING, new NoteBytesReadOnly(notePath.getByteCounter().get()), 
                            new NoteBytesReadOnly(notePath.getByteCounter().get()), ProtocolMesssages.SUCCESS);
                        return true;
                    }else{
                        notePath.skipKeyValue(nextNoteBytes, reader, writer);

                        notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                            notePath.getRootPathSize(), ProtocolMesssages.INFO + ": found non target file path");
                    }
                }else if( currentPath != null && nextNoteBytes.equals(currentPath)){
                    NoteBytesMetaData metaData = reader.nextMetaData();
                    //increment current level, and add metadata size
                    notePath.addMetaData(metaData);

                    //process next level or recursive delete
                    if(notePath.getCurrentLevel().get() == notePath.getSize() -1 && notePath.isRecursive()){
                        
                        //document deleted key + metadata + value metadata + all children length 
                        notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                            notePath.getRootPathSize(), ProtocolMesssages.INFO + ": found recursive delete point");

                        notePath.getDeletedFilePathLength().add((metaDataSize * 2) + nextNoteBytes.byteLength());
                        int remainingPathSize = notePath.getByteCounter().get() + metaData.getLength();

                        return recursiveDeleteFull(notePath, remainingPathSize, reader);
                    }else{
                        notePath.getByteCounter().add(writer.write(nextNoteBytes));
                        notePath.getByteCounter().add(writer.write(metaData));

                        notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                            notePath.getRootPathSize(), ProtocolMesssages.INFO + ": found target path node " + 
                                nextNoteBytes.getAsString()
                        );

                        return processNotePathToStream(notePath, notePath.getByteCounter().get() + metaData.getLength(), 
                            reader, writer);
                    }
                    
                }else{
                    notePath.skipKeyValue(nextNoteBytes, reader, writer);

                    notePath.progressMsg(ProtocolMesssages.PROCESSING, notePath.getByteCounter().get(), 
                        notePath.getRootPathSize(), ProtocolMesssages.INFO + ": found non target path node " + 
                            nextNoteBytes.getAsString()
                    );
                }
                
            }
        }
        return false;
    }


    private static int minTargetPathSizeBelowDeletion(NotePath notePath, int pathIndex) {
        int pathSize = notePath.getSize();
        boolean recursiveDelete = notePath.isRecursive();

        if (pathIndex == (recursiveDelete ?pathSize - 1 : pathSize)) {
            return notePath.getDeletedFilePathLength().get();
        }else if (pathIndex > pathSize) {
            return 0;
        }
        //key metaData + value MetaData + value size
        return metaDataSize + notePath.getTargetPath(pathIndex).byteLength() + metaDataSize +  
            minTargetPathSizeBelowDeletion(notePath, pathIndex + 1);
       
    }


   

    
}
