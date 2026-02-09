package io.netnotes.system;

import java.util.function.Consumer;

import io.netnotes.terminal.TerminalRenderable;



public interface SystemUIInterface {
    TerminalRenderable getUI();

    /**
     * Renderables cannot be resused must call onDisposed onCleanup (removed from layout)
     * @param onDisposed
     */
    void setOnDisposed(Consumer<SystemUIInterface> onDisposed);
}
