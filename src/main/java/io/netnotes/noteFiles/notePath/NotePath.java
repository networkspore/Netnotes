package io.netnotes.noteFiles.notePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import io.netnotes.noteFiles.FileStreamUtils;

import io.netnotes.engine.messaging.NoteMessaging.Keys;
import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.ProgressMessage;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.IntCounter;
import io.netnotes.noteBytes.processing.NoteBytesMetaData;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;

import io.netnotes.engine.utils.streams.StreamUtils;

public class NotePath{

    public static final NoteBytes FILE_PATH = new NoteBytes(new byte[]{ 0x01 });
    public static final int FILE_PATH_TOTAL_BYTE_LENGTH = 
        NotePath.FILE_PATH.byteLength() + NoteBytesMetaData.STANDARD_META_DATA_SIZE;
    public static final int PATH_LENGTH_WARNING = 512; //excessive size

    private final boolean recursive;
    private final NoteBytesReadOnly[] targetPath;
    private final int size;
    
    private NoteBytes targetFilePath = null;
    private IntCounter byteCounter = new IntCounter();
    private IntCounter deletedFilePathLength = new IntCounter();
    private IntCounter currentLevel = new IntCounter();
    private int[] pathSize;
    private final File pathLedger;
    private final AsyncNoteBytesWriter progressWriter;
    private ArrayList<CompletableFuture<Void>> asyncFileDeletions = null;
    
    
    public NotePath( File pathLedger, NoteBytesReadOnly[] targetPath, boolean recursive, AsyncNoteBytesWriter progressWriter){
        this.pathLedger = pathLedger;
        this.targetPath = targetPath;
        this.size = targetPath.length;
        this.recursive = recursive;
        pathSize = new int[targetPath.length];
        this.progressWriter = progressWriter;
    }

    public NotePath(File pathLedger, NoteStringArrayReadOnly path, boolean recursive, AsyncNoteBytesWriter progressWriter){
        this(pathLedger, path.getAsArray(), recursive, progressWriter);
    }

    public NotePath(File pathLedger, NoteStringArrayReadOnly path, AsyncNoteBytesWriter progressWriter){
        this(pathLedger, path.getAsArray(), progressWriter);
    }

    public NotePath(File pathLedger, NoteStringArrayReadOnly path){
        this(pathLedger, path.getAsArray());
    }

    public NotePath( File pathLedger, NoteBytesReadOnly[] targetPath, AsyncNoteBytesWriter progressWriter){
        this(pathLedger, targetPath, false, progressWriter);
    }

    public NotePath( File pathLedger, NoteBytesReadOnly[] targetPath){
        this(pathLedger, targetPath, false, null);
    }
    public void addMetaData(NoteBytesMetaData metaData){

        if(currentLevel.get() < pathSize.length){
            pathSize[currentLevel.get()] = metaData.getLength();
        }
        currentLevel.increment();
    }

    public int getCurrentPathSize(){
        int level = currentLevel.get();
        if(level < pathSize.length){
            return pathSize[level];
        } else {
            return 0;
        }
    }

    public ArrayList<CompletableFuture<Void>> getCompletableList(){
        if( asyncFileDeletions == null){
            asyncFileDeletions = new ArrayList<>();
        }
        return asyncFileDeletions;
    }

    public boolean isProgressWriter(){
        return progressWriter != null;
    }

    public AsyncNoteBytesWriter getProgressWriter(){
        return progressWriter;
    }

    public void setTargetFilePath(NoteBytes filePath){
        targetFilePath = filePath;
    }

        public boolean isRecursive() {
        return recursive;
    }
    public NoteBytesReadOnly[] getTargetPath() {
        return targetPath;
    }

    public NoteBytesReadOnly getTargetPath(int level){
        return targetPath[level];
    }
    public int getSize() {
        return size;
    }
    public NoteBytes getTargetFilePath() {
        return targetFilePath;
    }
    public IntCounter getByteCounter() {
        return byteCounter;
    }
    public void setByteCounter(IntCounter byteCounter) {
        this.byteCounter = byteCounter;
    }
    
    public IntCounter getDeletedFilePathLength() {
        return deletedFilePathLength;
    }
    public void setDeletedFilePathLength(IntCounter deletedFilePathLength) {
        this.deletedFilePathLength = deletedFilePathLength;
    }
    public IntCounter getCurrentLevel() {
        return currentLevel;
    }
    public NoteBytesReadOnly getCurrentPathKey(){
        if(currentLevel.get() < size){
            return targetPath[currentLevel.get()];
        }else{
            return null;
        }
    }
    public void setCurrentLevel(IntCounter currentLevel) {
        this.currentLevel = currentLevel;
    }
    
    public int[] getPathSize() {
        return pathSize;
    }
    
    public int getPathSize(int level){
        return pathSize[level];
    }

    public int getRootPathSize(){
        return pathSize[0];
    }

    public void setPathSize(int[] pathSize) {
        this.pathSize = pathSize;
    }

    public void skipKeyValue(NoteBytes nextNoteBytes, NoteBytesReader reader, NoteBytesWriter writer) throws IOException{
        byteCounter.add(writer.write(nextNoteBytes));
        NoteBytesMetaData metaData = reader.nextMetaData();
        if(metaData == null){
            throw new IllegalStateException("Unexpected end of file found at: " + byteCounter.get());
        }
        byteCounter.add(writer.write(metaData));
        byteCounter.add(StreamUtils.readWriteNextBytes(metaData.getLength(), reader, writer));
    }

    public boolean searchForFilePathKey(int bucketEnd, NoteBytesReader reader, NoteBytesWriter tmpWriter) throws Exception {
  
        while(byteCounter.get() < bucketEnd){
            NoteBytes key = reader.nextNoteBytes();
    
            if(key != null){
                byteCounter.add(tmpWriter.write(key));
                if(key.equals(NotePath.FILE_PATH)){
                    NoteBytes filePath = reader.nextNoteBytes();
                    byteCounter.add(tmpWriter.write(filePath));
                    setTargetFilePath(filePath);
                    return true;
                }else{
                    skipKeyValue(key, reader, tmpWriter);
                }
            }else{
                return false;
            }
        }

        return false;
    }

    public File getPathLedger(){
        return pathLedger;
    }

    public File getDataDir(){
        return getPathLedger().getParentFile();
    }

    public static NoteBytes generateNewDataFilePath(File dataDir){
        File newFile = NotePathFactory.generateNewDataFile(dataDir);
        return new NoteBytes(newFile.getAbsolutePath());
    }

    public NoteBytesPair createFilePath(int pathIndex, NoteBytes resultPath) {
        if (pathIndex == size) {
            return new NoteBytesPair(NotePath.FILE_PATH, resultPath);
        }else if (pathIndex > targetPath.length) {
            return null;
        }
        return new NoteBytesPair(targetPath[pathIndex], new NoteBytesObject(
            createFilePath(pathIndex + 1, resultPath)
        ));
    }

    



    public NoteBytes createNewRootPath(NoteBytesWriter writer) throws Exception {
        NoteBytes resultPath = generateNewDataFilePath(getDataDir());
        writer.write(createFilePath(0, resultPath));
        return resultPath;
    }

    public NoteBytes createNotePathLedger(SecretKey secretKey){
        try{
            NoteBytes noteFilePath = generateNewDataFilePath(getDataDir());
            // Build the complete path structure
            FileStreamUtils.encryptPairToFile(getPathLedger(), secretKey, 
                createFilePath( 0, noteFilePath)
            );
            return noteFilePath;
        }catch(Exception e){
            throw new RuntimeException("Failed to create initial data file", e);
        }
    }

    public void checkDataDir(){
        File dataDir = getDataDir();

        if(!dataDir.isDirectory()){
            try{
                Files.createDirectories(dataDir.toPath());
            }catch(IOException e){
                throw new RuntimeException("Cannot access data directory", e);
            }
        }
    }

    public void deleteFilePathValue(NoteBytes filePathValue, ArrayList<CompletableFuture<Void>> completableList){
        completableList.add(CompletableFuture.runAsync(()->{
            String filePath = filePathValue.getAsString();

            progressMsg(ProtocolMesssages.UPDATED,new NoteBytesReadOnly(0), new NoteBytesReadOnly(-1), new NoteBytesReadOnly(filePath),new NoteBytesPair[]{
                new NoteBytesPair(Keys.STATUS, ProtocolMesssages.STARTED) 
            });
            try{
                
                Files.deleteIfExists(new File(filePath).toPath());
               
                progressMsg(ProtocolMesssages.UPDATED, new NoteBytesReadOnly(0), new NoteBytesReadOnly(-1), new NoteBytesReadOnly( filePath), new NoteBytesPair[]{
                    new NoteBytesPair(Keys.STATUS, ProtocolMesssages.SUCCESS)});

            }catch(IOException e){

                progressMsg(ProtocolMesssages.UPDATED,new NoteBytesReadOnly(0), new NoteBytesReadOnly(-1), 
                    new NoteBytesReadOnly(filePath),  
                    new NoteBytesPair[]{
                        new NoteBytesPair(Keys.STATUS, ProtocolMesssages.ERROR),
                        new NoteBytesPair(Keys.EXCEPTION, e)});
        
                throw new RuntimeException(filePath, e);
            }
        }));
    }

    public CompletableFuture<Integer> progressMsg(NoteBytesReadOnly scope, long completed, long total, String message){
        return progressMsg(scope, 
            new NoteBytesReadOnly(completed), 
            new NoteBytesReadOnly(total), 
            new NoteBytesReadOnly(message));
    }

    public CompletableFuture<Integer> progressMsg(String scope, long completed, long total, String message){
        return progressMsg(new NoteBytesReadOnly(scope), new NoteBytesReadOnly(completed), new NoteBytesReadOnly(total), new NoteBytesReadOnly(message));
    }


    public CompletableFuture<Integer> progressMsg(NoteBytesReadOnly scope,NoteBytesReadOnly completed,  NoteBytesReadOnly total, NoteBytesReadOnly message){
        return progressWriter != null ? ProgressMessage.writeAsync(scope,completed, total,  message, progressWriter) : 
            CompletableFuture.failedFuture(new NullPointerException("progress writer is null"));
    }

    public CompletableFuture<Integer> progressMsg(NoteBytesReadOnly scope, long completed, long total, String message, 
        NoteBytesPair... pairs){
        return progressWriter != null ? ProgressMessage.writeAsync(scope, 
            new NoteBytesReadOnly(completed), 
            new NoteBytesReadOnly(total),
            new NoteBytesReadOnly(message), progressWriter, pairs) : 
            CompletableFuture.failedFuture(new NullPointerException("progress writer is null"));
    }

    public CompletableFuture<Integer> progressMsg(NoteBytesReadOnly scope,  NoteBytesReadOnly completed, NoteBytesReadOnly total, NoteBytesReadOnly message, 
        NoteBytesPair... pairs){
        return progressWriter != null ? ProgressMessage.writeAsync(scope,completed, total, message, progressWriter, pairs) : 
            CompletableFuture.failedFuture(new NullPointerException("progress writer is null"));
    }

     public CompletableFuture<Integer> errorMsg(NoteBytesReadOnly scope, String message, Throwable e){
        return progressWriter != null ? progressWriter.writeAsync(TaskMessages.createErrorMessage(scope, message, e)): 
            CompletableFuture.failedFuture(new NullPointerException("progress writer is null"));
    }

    public CompletableFuture<Integer> errorMsg(String scope, String message, Throwable e){
        return progressWriter != null ? progressWriter.writeAsync(TaskMessages.createErrorMessage(scope, message, e)): 
            CompletableFuture.failedFuture(new NullPointerException("progress writer is null"));
    }

    
}