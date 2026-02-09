package io.netnotes.system;

import io.netnotes.terminal.TerminalBatchBuilder;
import io.netnotes.terminal.TerminalRenderable;
import io.netnotes.terminal.TextStyle;

/**
 * BreadcrumbPanel - Displays context/path/navigation
 */
class BreadcrumbPanel extends TerminalRenderable {
    private String path = "";
    
    public BreadcrumbPanel(String name) {
        super(name);
    }
    
    public void setPath(String path) {
        if (path == null) path = "";
        if (!this.path.equals(path)) {
            this.path = path;
            invalidate();
        }
    }
    
    @Override
    protected void renderSelf(TerminalBatchBuilder batch) {
        int width = getWidth();
        if (width <= 0) return;
        
        String display = path.isEmpty() ? "~" : path;
        if (display.length() > width) {
            display = "..." + display.substring(display.length() - width + 3);
        }
        
        printAt(batch, 0, 0, display, TextStyle.NORMAL);
    }
}