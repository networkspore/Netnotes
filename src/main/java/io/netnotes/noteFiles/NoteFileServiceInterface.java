package io.netnotes.noteFiles;

import java.util.concurrent.CompletableFuture;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.noteBytes.processing.AsyncNoteBytesWriter;

public interface NoteFileServiceInterface {

    CompletableFuture<NoteFile> getNoteFile(ContextPath path);
    ContextPath getDataRootPath();
    CompletableFuture<Void> deleteNoteFile(ContextPath path, boolean recurrsive, AsyncNoteBytesWriter progress);
}
