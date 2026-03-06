package io.netnotes.system;

import org.jline.terminal.Terminal;

import io.netnotes.consoleRenderer.ConsoleRenderer;
import io.netnotes.engine.utils.virtualExecutors.VirtualExecutors;


import org.jline.terminal.Attributes;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TerminalInitializer {
    public static Attributes ORIGINAL_TERMINAL_ATTRIBUTES = null;
    public static CompletableFuture<ConsoleRenderer> createAndInitialize() {
        return VirtualExecutors.getIoExecutor().submit(()->{
            try {
                ConsoleRenderer renderer = new ConsoleRenderer();
                Terminal terminal = renderer.getTerminal();
                setRawMode(terminal);
                return renderer;
            } catch (IOException e) {
                throw new CompletionException("Failed to initialize", e);
            }
        })
        .thenCompose(renderer->{
            return renderer.initialize()
                .thenApply(v->renderer);
        });

    }
    
    private static void setRawMode(Terminal terminal) {
        ORIGINAL_TERMINAL_ATTRIBUTES = terminal.getAttributes();

        Attributes raw = new Attributes(ORIGINAL_TERMINAL_ATTRIBUTES);
        
        raw.setLocalFlag(Attributes.LocalFlag.ICANON, false);  // char-by-char
        raw.setLocalFlag(Attributes.LocalFlag.ECHO, false);    // manual render
        raw.setLocalFlag(Attributes.LocalFlag.ISIG, false);     // SIGINT on Ctrl+C
        raw.setLocalFlag(Attributes.LocalFlag.IEXTEN, false);  // no special chars
        raw.setControlChar(Attributes.ControlChar.VMIN, 0);    // non-blocking
        raw.setControlChar(Attributes.ControlChar.VTIME, 1);   // 100ms poll
        
        terminal.setAttributes(raw);
    }
    
    public static void shutdown(ConsoleRenderer renderer) {
        if (renderer == null) return;
        try {
            Terminal terminal = renderer.getTerminal();
            if (terminal != null) {
                terminal.writer().print("\033[2J\033[H");       
                terminal.writer().print("\033[?25h");  // Show cursor
                terminal.writer().print("\033[?1049l"); // Exit alt buffer
                terminal.flush();
                terminal.setAttributes(ORIGINAL_TERMINAL_ATTRIBUTES);
            }
            renderer.shutdown();
        } catch (Exception e) {
            System.err.println("[ConsoleTerminalInitializer] Emergency restore failed: " + e.getMessage());
        }
    }
}