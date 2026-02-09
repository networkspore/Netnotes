package io.netnotes.system.cmd;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CommandContext - Process command interface
 */
public interface CommandContext {
    String[] getPath();
    CompletableFuture<CommandResult> execute(String command);
    List<String> getCompletions(String partial);
}