package io.netnotes.noteFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;

import io.netnotes.noteBytes.NoteBytes;
import io.netnotes.noteBytes.NoteBytesObject;

import io.netnotes.noteBytes.collections.NoteBytesMap;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;
import io.netnotes.noteBytes.processing.NoteBytesReader;
import io.netnotes.noteBytes.processing.NoteBytesWriter;
import io.netnotes.noteBytes.NoteStringArrayReadOnly;

import io.netnotes.engine.utils.CollectionHelpers;
import io.netnotes.engine.utils.LoggingHelpers.Log;
import io.netnotes.engine.utils.noteBytes.NoteUUID;
import io.netnotes.engine.utils.streams.StreamUtils;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;

/**
 * NoteFile - Handle to an encrypted file with automatic serialized access
 * 
 * REFACTORED ARCHITECTURE:
 * - All file operations are automatically serialized via ManagedNoteFileInterface
 * - No manual lock management needed - operations queue automatically
 * - Caller-controlled cancellation via CompletableFuture
 * - Simplified API - no acquire/release patterns
 * - Better resource safety - no lock leaks possible
 */
public class NoteFile implements AutoCloseable {

    private final NoteStringArrayReadOnly m_notePath;
    private final NoteFileInterface m_noteFileInterface;
    private final NoteUUID noteUUID;
    private final String pathString;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    public NoteFile(NoteStringArrayReadOnly notePath, ManagedNoteFileInterface noteFileInterface) 
            throws IllegalStateException {
        this.m_notePath = notePath;
        this.m_noteFileInterface = noteFileInterface;
        this.noteUUID = NoteUUID.createLocalUUID128();
        this.pathString = notePath.getAsString();
        noteFileInterface.addReference(this);
    }

    public String getUrlPathString() {
        return pathString;
    }

    public NoteUUID getId() {
        return noteUUID;
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("NoteFile has been closed");
        }
    }

    public void shutdown() {
        close();
    }

    @Override
    public void close() {
        if (!closed.getAndSet(true)) {
            m_noteFileInterface.removeReference(noteUUID);
        }
    }

    public void forceClose() {
        closed.set(true);
    }

    /**
     * Read-only operation - decrypt, duplicate for caller, re-encrypt
     * Operations are automatically serialized by the interface
     */
    public CompletableFuture<Void> readOnly(PipedOutputStream readOutput) {
        checkNotClosed();
        
        PipedOutputStream decryptedOutput = new PipedOutputStream();
        CompletableFuture<NoteBytesObject> decryptFuture = 
            m_noteFileInterface.decryptFile(decryptedOutput);

        PipedOutputStream encryptOutput = new PipedOutputStream();
        CompletableFuture<Void> voidFuture = 
            StreamUtils.duplicateEntireStream(decryptedOutput, readOutput, encryptOutput, null);
            
        CompletableFuture<NoteBytesObject> encryptFuture = 
            m_noteFileInterface.saveEncryptedFileSwap(encryptOutput);
   
        return CompletableFuture.allOf(decryptFuture, voidFuture, encryptFuture);
    }

    /**
     * Read entire NoteBytes object from file
     */
    public CompletableFuture<NoteBytes> readNoteBytes() {
        PipedOutputStream outputStream = new PipedOutputStream();
        CompletableFuture<Void> readFuture = readOnly(outputStream);

        CompletableFuture<NoteBytes> readNoteBytesFuture = CompletableFuture.supplyAsync(() -> {
            try(NoteBytesReader reader = new NoteBytesReader(
                    new PipedInputStream(outputStream, StreamUtils.PIPE_BUFFER_SIZE))) {
                return reader.nextNoteBytes();
            } catch (IOException e) {
                throw new CompletionException("Failed to read", e);
            }
        }, VirtualExecutors.getVirtualExecutor());

        return CompletableFuture.allOf(readFuture, readNoteBytesFuture)
            .thenCompose(v -> readNoteBytesFuture);
    }

    /**
     * Read all bytes from file
     */
    public CompletableFuture<byte[]> readBytes() {
        PipedOutputStream outputStream = new PipedOutputStream();
        CompletableFuture<Void> readFuture = readOnly(outputStream);

        CompletableFuture<byte[]> readByteFuture = CompletableFuture.supplyAsync(() -> {
            try(
                PipedInputStream inputStream = new PipedInputStream(
                    outputStream, StreamUtils.PIPE_BUFFER_SIZE);
                UnsynchronizedByteArrayOutputStream byteStream = UnsynchronizedByteArrayOutputStream.builder()
                    .setBufferSize((int)m_noteFileInterface.fileSize()).get();
            ){
                inputStream.transferTo(byteStream);
                return byteStream.toByteArray();
            } catch (IOException e) {
                throw new CompletionException("Failed to read", e);
            }
        }, VirtualExecutors.getVirtualExecutor());

        return CompletableFuture.allOf(readFuture, readByteFuture)
            .thenCompose(v -> readByteFuture);
    }

    /**
     * Write-only operation - automatically serialized
     */
    public CompletableFuture<NoteBytesObject> writeOnly(PipedOutputStream pipedOutput) {
        checkNotClosed();
        return m_noteFileInterface.saveEncryptedFileSwap(pipedOutput);
    }

    public CompletableFuture<Void> write(NoteBytesMap noteBytesMap) {
        return write(noteBytesMap.toNoteBytes());
    }

    /**
     * Write NoteBytes with metadata
     */
    public CompletableFuture<Void> write(NoteBytes noteBytes) {
        PipedOutputStream outputStream = new PipedOutputStream();
        
        CompletableFuture<Void> writeFuture = CompletableFuture.runAsync(() -> {
            try(NoteBytesWriter writer = new NoteBytesWriter(outputStream)) {
                writer.write(noteBytes);
            } catch(IOException e) {
                throw new CompletionException(e);
            }
        }, VirtualExecutors.getVirtualExecutor());

        CompletableFuture<NoteBytesObject> writeOnlyFuture = writeOnly(outputStream);

        return CompletableFuture.allOf(writeFuture, writeOnlyFuture);
    }

    public CompletableFuture<Void> write(byte[] bytes) {
        PipedOutputStream outputStream = new PipedOutputStream();
        
        CompletableFuture<Void> writeFuture = CompletableFuture.runAsync(() -> {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, VirtualExecutors.getVirtualExecutor());

        CompletableFuture<NoteBytesObject> writeOnlyFuture = writeOnly(outputStream);

        return CompletableFuture.allOf(writeFuture, writeOnlyFuture);
    }

    /**
     * Read-write without explicit locking - automatically serialized
     */
    public CompletableFuture<NoteBytesObject> readWriteNoLock(
            PipedOutputStream inParseStream, 
            PipedOutputStream modifiedInParseStream) {
        checkNotClosed();
        return m_noteFileInterface.readWriteFile(inParseStream, modifiedInParseStream);
    }
    
    /**
     * Functional approach for custom operations
     * Operations are automatically serialized
     */
    public <T> CompletableFuture<T> withExclusiveAccess(
            Function<NoteFileInterface, CompletableFuture<T>> operation) {
        checkNotClosed();
        return operation.apply(m_noteFileInterface);
    }
    
    /**
     * Read-write operation - automatically serialized
     */
    public CompletableFuture<NoteBytesObject> readWrite(
            PipedOutputStream inParseStream, 
            PipedOutputStream modifiedInParseStream) {
        return withExclusiveAccess(fileInterface -> 
            fileInterface.readWriteFile(inParseStream, modifiedInParseStream));
    }
    
    public static CompletableFuture<NoteBytesObject> readWriteNoteFile(
            NoteFile noteFile, 
            PipedOutputStream outStream, 
            PipedOutputStream modifiedStream) {
        return noteFile.readWriteNoLock(outStream, modifiedStream);
    }
    
    /**
     * Fluent builder API for reusable operations
     */
    public OperationBuilder newOperation() {
        checkNotClosed();
        return new OperationBuilder(this);
    }
    
    /**
     * Lightweight builder for composing file operations
     */
    public static class OperationBuilder {
        private final NoteFile noteFile;
        
        OperationBuilder(NoteFile noteFile) {
            this.noteFile = noteFile;
        }
        
        public CompletableFuture<NoteBytesObject> readWrite(
                PipedOutputStream inParseStream,
                PipedOutputStream modifiedInParseStream) {
            return noteFile.readWriteNoLock(inParseStream, modifiedInParseStream);
        }
        
        public <T> CompletableFuture<T> execute(
                Function<NoteFileInterface, CompletableFuture<T>> operation) {
            return noteFile.withExclusiveAccess(operation);
        }
        
        public OperationBuilder reset() {
            return this;
        }
    }
  
    public CompletableFuture<NoteBytesObject> performComplexOperation(
            Function<PipedOutputStream, CompletableFuture<Void>> readProcessor,
            Function<PipedOutputStream, CompletableFuture<NoteBytesObject>> writeProcessor) {
        
        return withExclusiveAccess(fileInterface -> {
            PipedOutputStream inStream = new PipedOutputStream();
            PipedOutputStream outStream = new PipedOutputStream();
            
            fileInterface.decryptFile(inStream);
            readProcessor.apply(inStream);
            
            return writeProcessor.apply(outStream);
        });
    }

    /**
     * Get InputStream for reading file contents
     * Stream operations are automatically serialized
     */
    public InputStream getInputStream() throws IOException {
        checkNotClosed();
        
        PipedOutputStream decryptOutput = new PipedOutputStream();
        PipedInputStream decryptInput = new PipedInputStream(
            decryptOutput,
            StreamUtils.PIPE_BUFFER_SIZE
        );
        
        CompletableFuture<Void> decryptFuture = readOnly(decryptOutput)
            .exceptionally(ex -> {
                StreamUtils.safeClose(decryptInput);
                StreamUtils.safeClose(decryptOutput);
                Log.logError("[NoteFile] Decryption error for " + 
                    pathString + ": " + ex.getMessage());
                return null;
            });
        
        return new InputStream() {
            private final AtomicBoolean streamClosed = new AtomicBoolean(false);
            
            @Override
            public int read() throws IOException {
                if (streamClosed.get()) {
                    throw new IOException("Stream closed");
                }
                
                try {
                    return decryptInput.read();
                } catch (IOException e) {
                    if (decryptFuture.isCompletedExceptionally()) {
                        throw new IOException("Decryption failed", 
                            decryptFuture.handle((v, ex) -> ex).join());
                    }
                    throw e;
                }
            }
            
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (streamClosed.get()) {
                    throw new IOException("Stream closed");
                }
                
                try {
                    return decryptInput.read(b, off, len);
                } catch (IOException e) {
                    if (decryptFuture.isCompletedExceptionally()) {
                        throw new IOException("Decryption failed", 
                            decryptFuture.handle((v, ex) -> ex).join());
                    }
                    throw e;
                }
            }
            
            @Override
            public void close() throws IOException {
                if (streamClosed.compareAndSet(false, true)) {
                    try {
                        decryptInput.close();
                        decryptOutput.close();
                        
                        try {
                            decryptFuture.join();
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            throw new IOException("Decryption did not complete successfully", cause);
                        }
                    } catch (IOException e) {
                        StreamUtils.safeClose(decryptInput);
                        StreamUtils.safeClose(decryptOutput);
                        throw e;
                    }
                }
            }
            
            @Override
            public int available() throws IOException {
                if (streamClosed.get()) {
                    return 0;
                }
                return decryptInput.available();
            }
            
            @Override
            public long skip(long n) throws IOException {
                if (streamClosed.get()) {
                    throw new IOException("Stream closed");
                }
                return decryptInput.skip(n);
            }
            
            @Override
            public boolean markSupported() {
                return false;
            }
        };
    }

    /**
     * Get OutputStream for writing file contents
     * Write operations are automatically serialized
     */
    public OutputStream getOutputStream() throws IOException {
        checkNotClosed();

        PipedOutputStream pipeOut = new PipedOutputStream();

        CompletableFuture<NoteBytesObject> writeFuture = writeOnly(pipeOut)
            .exceptionally(ex -> {
                StreamUtils.safeClose(pipeOut);
                Log.logError("[NoteFile] Write/encrypt error for " +
                    pathString + ": " + ex.getMessage());
                return null;
            });

        return new OutputStream() {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void write(int b) throws IOException {
                ensureOpen();
                try {
                    pipeOut.write(b);
                } catch (IOException e) {
                    rethrowWriteError(e);
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                ensureOpen();
                try {
                    pipeOut.write(b, off, len);
                } catch (IOException e) {
                    rethrowWriteError(e);
                }
            }

            private void rethrowWriteError(IOException e) throws IOException {
                if (writeFuture.isCompletedExceptionally()) {
                    Throwable cause = writeFuture.handle((v, ex) -> ex).join();
                    throw new IOException("Async write failed", cause);
                }
                throw e;
            }

            private void ensureOpen() throws IOException {
                if (closed.get()) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public void flush() throws IOException {
                ensureOpen();
                pipeOut.flush();
            }

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    try {
                        pipeOut.close();
                        try {
                            writeFuture.join();
                        } catch (Exception e) {
                            Throwable cause = e.getCause() != null ? e.getCause() : e;
                            throw new IOException("Async write did not complete successfully", cause);
                        }
                    } finally {
                        StreamUtils.safeClose(pipeOut);
                    }
                }
            }
        };
    }

    public boolean isClosed() { 
        return closed.get(); 
    }

    public NoteStringArrayReadOnly getPath() { 
        return m_notePath; 
    }

    public boolean isLocked() {
        return m_noteFileInterface.isLocked();
    }

    public ExecutorService getExecService() {
        return m_noteFileInterface.getExecService();
    }

    public boolean isFile() {
        return m_noteFileInterface.isFile();
    }

    public static URL getResourceURL(String resourceLocation) {
        return resourceLocation != null ? 
            CollectionHelpers.class.getResource(resourceLocation) : null;
    }

    /**
     * NoteFileInterface - abstraction for file operations
     * All operations are automatically serialized by the implementation
     */
    public interface NoteFileInterface {
        ExecutorService getExecService();
        boolean isFile();
        long fileSize();
        void addReference(NoteFile noteFile);
        void removeReference(NoteUUID noteUUID);
        int getReferenceCount();
        
        // All operations are automatically serialized
        CompletableFuture<NoteBytesObject> decryptFile(PipedOutputStream pipedOutput);
        CompletableFuture<NoteBytesObject> saveEncryptedFileSwap(PipedOutputStream pipedOutputStream);
        CompletableFuture<NoteBytesObject> readWriteFile(
            PipedOutputStream inParseStream, 
            PipedOutputStream modifiedInParseStream);
        
        // Legacy methods - deprecated
        @Deprecated CompletableFuture<Void> acquireLock();
        @Deprecated void releaseLock();
        @Deprecated boolean isLocked();
        
        boolean isClosed();
        CompletableFuture<Void> perpareForShutdown(AsyncNoteBytesWriter progressWriter);
        CompletableFuture<Void> prepareForKeyUpdate();
        void completeKeyUpdate();
    }
}