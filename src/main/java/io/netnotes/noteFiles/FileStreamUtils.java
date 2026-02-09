package io.netnotes.noteFiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;
import io.netnotes.noteBytes.NoteBytesReadOnly;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.noteBytes.collections.NoteBytesPair;
import io.netnotes.engine.crypto.CryptoService;

import io.netnotes.engine.messaging.NoteMessaging.ProtocolMesssages;
import io.netnotes.engine.messaging.task.ProgressMessage;
import io.netnotes.engine.messaging.task.TaskMessages;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;
import io.netnotes.noteBytes.processing.RandomService;
import io.netnotes.engine.utils.streams.StreamUtils;

public class FileStreamUtils {
 
    

    public static String getNewUUIDFilePath(File dataDir){
        return dataDir.getAbsolutePath() + "/" + NoteUUID.createSafeUUID128();
    }

    public static CompletableFuture<Void> fileToPipe(File file, PipedOutputStream pipedOutputStream, ExecutorService execService){
        return CompletableFuture.runAsync(()->{
            try(
                PipedOutputStream outputStream = pipedOutputStream;
                InputStream inputStream = Files.newInputStream(file.toPath());
            ){
                inputStream.transferTo(outputStream);
            }catch(IOException e){
                throw new CompletionException("Failed to read file", e);
            }
        },execService);
    }

    public static CompletableFuture<Boolean> pipeToFile( PipedOutputStream pipedOutputStream, File file, ExecutorService execService){
        return CompletableFuture.supplyAsync(()->{
            try(
                PipedInputStream inputStream = new PipedInputStream(pipedOutputStream, StreamUtils.PIPE_BUFFER_SIZE);
                OutputStream outputStream = Files.newOutputStream(file.toPath())
            ){
                inputStream.transferTo(outputStream);
                return true;
            }catch(IOException e){
                throw new CompletionException("Failed to write file", e);
            }
        },execService);
    }


    public static void writeFileBytes(File file, byte[] bytes) throws IOException{
        try(
            OutputStream outputStream = Files.newOutputStream(file.toPath());
        ){
            int offset = 0;
            int remaining = bytes.length;
            while(remaining > 0){
                int length = Math.min(remaining, StreamUtils.BUFFER_SIZE);
                outputStream.write(bytes, offset, length);
                offset += length;
                remaining -= length;
            }
        }
    }

    public static void writeFileNoteBytes(File file, NoteBytes noteBytes) throws IOException{
        try(
           NoteBytesWriter writer = new NoteBytesWriter(Files.newOutputStream(file.toPath()));
        ){
            writer.write(noteBytes);
        }
    }
    

    public static void saveEncryptedFile(File file, byte[] bytes, SecretKey secretKey) throws Exception{

        try(
            OutputStream fileOutputStream = Files.newOutputStream(file.toPath());
        ){
            
            byte[] outIV = RandomService.getIV();
            Cipher encryptCipher = CryptoService.getAESEncryptCipher(outIV, secretKey);


            fileOutputStream.write(outIV);

            try(
                CipherOutputStream outputStream = new CipherOutputStream(fileOutputStream, encryptCipher);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ){
                byte[] buffer = new byte[StreamUtils.BUFFER_SIZE > bytes.length ? bytes.length : StreamUtils.BUFFER_SIZE];
                int length = 0;
                while((length = inputStream.read(buffer)) != -1){
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    
    }

    public static boolean updateFileEncryption(SecretKey oldAppKey, SecretKey newAppKey, File file, File tmpFile) throws Exception {
        return updateFileEncryption(oldAppKey, newAppKey, file, tmpFile, null);
    }

    public static boolean updateFileEncryption(SecretKey oldAppKey, SecretKey newAppKey, File file, File tmpFile, 
        AsyncNoteBytesWriter progressWriter
    ) throws Exception {
        if(file != null && file.isFile()){
            
            Path filePath = file.toPath();
            Path tmpPath = tmpFile.toPath();
            if(progressWriter != null){
                progressWriter.writeAsync(ProgressMessage.getProgressMessage(ProtocolMesssages.UPDATED_ENCRYPTION,
                    new NoteBytesReadOnly( 0), new NoteBytesReadOnly( -1), ProtocolMesssages.STARTED, new NoteBytesPair[]{
                        new NoteBytesPair("file", file.getAbsolutePath()),
                        new NoteBytesPair("tmpFile", tmpFile.getAbsolutePath())
                    }
                ));
            }
            try(
                InputStream fileInputStream = Files.newInputStream(filePath);
                OutputStream fileOutputStream = Files.newOutputStream(tmpPath);
                NoteBytesReader initialReader = new NoteBytesReader(fileInputStream);
            ){
                
                byte[] oldIV = initialReader.readNextBytes(CryptoService.AES_IV_SIZE);
                byte[] outIV = RandomService.getIV();

                Cipher decryptCipher = CryptoService.getAESDecryptCipher(oldIV, newAppKey);
                Cipher encryptCipher = CryptoService.getAESEncryptCipher(outIV, newAppKey);

                fileOutputStream.write(outIV);
                long fileSize = file.length();
                
                try(
                    CipherInputStream inputStream = new CipherInputStream(fileInputStream, decryptCipher);
                    CipherOutputStream outputStream = new CipherOutputStream(fileOutputStream, encryptCipher);
                ){
                    byte[] buffer = new byte[StreamUtils.BUFFER_SIZE >  (fileSize -12)? (int) (fileSize-12) : StreamUtils.BUFFER_SIZE];
                    int length = 0;
                    long total = 12;
                    while((length = inputStream.read(buffer)) != -1){
                        outputStream.write(buffer, 0, length);
                        total += length;
                        if(progressWriter != null){
                        progressWriter.writeAsync(ProgressMessage.getProgressMessage(ProtocolMesssages.UPDATED_ENCRYPTION,
                            total, fileSize, ProtocolMesssages.UPDATED, new NoteBytesPair[]{
                                new NoteBytesPair("file", file.getAbsolutePath()),
                                new NoteBytesPair("tmpFile", tmpFile.getAbsolutePath())
                            }
                        ));
                    }
                    }
                }
            }
        
            Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            if(progressWriter != null){
                progressWriter.writeAsync(ProgressMessage.getProgressMessage(ProtocolMesssages.UPDATED_ENCRYPTION,
                    0, -1, ProtocolMesssages.SUCCESS, new NoteBytesPair[]{
                        new NoteBytesPair("file", file.getAbsolutePath()),
                        new NoteBytesPair("tmpFile", tmpFile.getAbsolutePath())
                    }
                ));
            }
            return true;
        }
        return false;
    }



    public static boolean decryptFileToFile(SecretKey appKey, File encryptedFile, File decryptedFile) throws Exception{
        if(encryptedFile != null && encryptedFile.isFile() && encryptedFile.length() > 12){
            
            try(
                InputStream inputStream = Files.newInputStream(encryptedFile.toPath());
                NoteBytesReader reader = new NoteBytesReader(inputStream);
                OutputStream outStream = Files.newOutputStream(decryptedFile.toPath());
            ){
                
                byte[] iV = reader.readNextBytes(CryptoService.AES_IV_SIZE);

                Cipher cipher = CryptoService.getAESDecryptCipher(iV, appKey);

                long fileSize = encryptedFile.length();
                int bufferSize = fileSize < (8 * 1024) ? (int) fileSize : (8 * 1024);

                byte[] buffer = new byte[bufferSize];
                byte[] decryptedBuffer;
                int length = 0;
                long decrypted = 0;

                while ((length = inputStream.read(buffer)) != -1) {
                    decryptedBuffer = cipher.update(buffer, 0, length);
                    if(decryptedBuffer != null){
                        outStream.write(decryptedBuffer);
                    }
                    decrypted += length;
                }

                decryptedBuffer = cipher.doFinal();

                if(decryptedBuffer != null){
                    outStream.write(decryptedBuffer);
                }

                if(decrypted == fileSize){
                    return true;
                }
            }

      
        }
        return false;
    }

  
    
    public static byte[] decryptFileToBytes(SecretKey appKey, File file) throws Exception{
        if(file != null && file.isFile()){
            
            try(
                InputStream inputStream = Files.newInputStream(file.toPath());
                NoteBytesReader reader = new NoteBytesReader(inputStream);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ){

                if(!file.exists() || !file.isFile()){
                    return null;
                }
                long fileSize = file.length();
                if(fileSize < CryptoService.AES_IV_SIZE){
                    return new byte[0];
                }

                byte[] iV = reader.readNextBytes(CryptoService.AES_IV_SIZE);

               
                Cipher cipher = CryptoService.getAESDecryptCipher(iV, appKey);

                
                int bufferSize = fileSize < (long) StreamUtils.BUFFER_SIZE ? (int) fileSize : StreamUtils.BUFFER_SIZE;

                byte[] buffer = new byte[bufferSize];
             
                int length = 0;

                try(CipherInputStream inputCipher = new CipherInputStream(inputStream, cipher)){

                    while ((length = inputCipher.read(buffer)) != -1) {
                        outStream.write(buffer, 0, length);
                    }

                    return outStream.toByteArray();
               }
            }
        }
        return null;

    }

    public static CompletableFuture<NoteBytesObject> saveEncryptedFile( File file, SecretKey secretKey, PipedOutputStream pipedOutputStream,
        ExecutorService execService
    ){
        return CompletableFuture.supplyAsync(() -> {
            try(
                PipedInputStream inputStream = new PipedInputStream(pipedOutputStream);
                OutputStream fileOutputStream = Files.newOutputStream(file.toPath());
            ){
                
                byte[] outIV = RandomService.getIV();
                Cipher encryptCipher = CryptoService.getAESEncryptCipher(outIV, secretKey);

                fileOutputStream.write(outIV);
                
                try(CipherOutputStream outputStream = new CipherOutputStream(fileOutputStream, encryptCipher)){
                    long bytesWritten = 0;
                    int length = 0;  
                    byte[] readBuffer = new byte[StreamUtils.BUFFER_SIZE];
                    while((length = inputStream.read(readBuffer)) != -1){
                        outputStream.write(readBuffer, 0, length);
                        bytesWritten += length;
                    }

                    return TaskMessages.createSuccessResult(file.getAbsolutePath(), "File length:" + bytesWritten);
                }
            }catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, execService);
    
    }

    public static CompletableFuture<NoteBytesObject> saveEncryptedFileSwap(
        File file,
        SecretKey secretKey,
        PipedOutputStream pipedOutputStream,
        ExecutorService execService
    ) {
        
        return CompletableFuture.supplyAsync(() -> {
            final File tmpFile = new File(getNewUUIDFilePath(file.getParentFile()) + ".tmp");
            final Path tmpPath = tmpFile.toPath();

            long bytesWritten = 0;
            try (PipedInputStream inputStream = new PipedInputStream(pipedOutputStream)){
                try(
                    OutputStream fileOutputStream = Files.newOutputStream(tmpPath);
                ){
                    byte[] outIV = RandomService.getIV();
                    Cipher encryptCipher = CryptoService.getAESEncryptCipher(outIV, secretKey);
                    fileOutputStream.write(outIV);
                    
                    try (CipherOutputStream outputStream = new CipherOutputStream(fileOutputStream, encryptCipher)) {
                        int length;
                        byte[] readBuffer = new byte[StreamUtils.BUFFER_SIZE];
                        while ((length = inputStream.read(readBuffer)) != -1) {
                            outputStream.write(readBuffer, 0, length);
                            bytesWritten += length;
                        }
                    }
                } 
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(tmpPath);
                } catch (IOException cleanupEx) {
                    e.addSuppressed(cleanupEx);
                }
                throw new RuntimeException("Failed to write encrypted data", e);
            }
            return FileSwapUtils.performAtomicFileSwap(file, tmpFile, bytesWritten);
        }, execService);
    }

    public static CompletableFuture<NoteBytesObject> performDecryption(
        File file, 
        PipedOutputStream pipedOutput,
        SecretKey secretKey,
        ExecutorService execService
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (
                InputStream fileInputStream = Files.newInputStream(file.toPath());
                NoteBytesReader reader = new NoteBytesReader(fileInputStream);
            ) {
           
                if (file.exists() && file.isFile() && file.length() > CryptoService.AES_IV_SIZE - 1) {
                    byte[] iV = reader.readNextBytes(CryptoService.AES_IV_SIZE);
                    
                    Cipher decryptCipher = CryptoService.getAESDecryptCipher(iV, secretKey);
                    
                    byte[] readBuffer = new byte[StreamUtils.BUFFER_SIZE];
                    int length;
                    int dataLength = 0;
                    try(CipherInputStream inputStream = new CipherInputStream(fileInputStream, decryptCipher)){

                        while ((length = inputStream.read(readBuffer)) != -1) {
                            pipedOutput.write(readBuffer, 0, length);
                        }
                    
                        return TaskMessages.createSuccessResult(file.getAbsolutePath(), "Data length:" + dataLength);
                    }
                }
                
                return TaskMessages.createSuccessResult(file.getAbsolutePath(), "File length: 0");
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    pipedOutput.close();
                } catch (IOException e) {
                    // Log close error
                }
            }
        }, execService);
    }
            

    public static void encryptPairToFile(File file, SecretKey secretKey, NoteBytesPair pair) throws Exception{
        try( 
            OutputStream fileOutputStream = Files.newOutputStream(file.toPath());
        ){
            byte[] outIV = RandomService.getIV();
            Cipher encryptCipher = CryptoService.getAESEncryptCipher(outIV, secretKey);

            fileOutputStream.write(outIV);
            
            try(NoteBytesWriter writer = new NoteBytesWriter(new CipherOutputStream(fileOutputStream, encryptCipher))){
                writer.write(pair);
            }
        }
    }


    public static void readFileToWriter(File tmpFile, NoteBytesWriter writer, SecretKey key) throws Exception {
        try (
           InputStream fileIn = Files.newInputStream(tmpFile.toPath());
        ) {
            byte[] iV = StreamUtils.readByteAmount(CryptoService.AES_IV_SIZE, fileIn);
            
            Cipher decryptCipher = CryptoService.getAESDecryptCipher(iV, key);
            try(CipherInputStream tmpIn = new CipherInputStream(fileIn, decryptCipher) ){
                byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = tmpIn.read(buffer)) != -1) {
                    writer.write(buffer, 0, bytesRead);
                }
            }
        }
    }


    public static NoteBytes readFileNextNoteBytes(File file) throws FileNotFoundException, IOException{
      
        try(
            NoteBytesReader reader = new NoteBytesReader(new FileInputStream(file));    
        ){
            return reader.nextNoteBytes();
        }
       
    }

}