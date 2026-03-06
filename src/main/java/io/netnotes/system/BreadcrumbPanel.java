package io.netnotes.system;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.ui.SizePreference;
import io.netnotes.terminal.TerminalBatchBuilder;
import io.netnotes.terminal.TextStyle;
import io.netnotes.terminal.components.TerminalRegion;

/**
 * BreadcrumbPanel - Displays context/path/navigation
 */
class BreadcrumbPanel extends TerminalRegion {
    public static final int MAX_SEGMENT_LEN = 8;


    private ContextPath path = ContextPath.ROOT;
    private String displayString = "/";
    private int len = 1;
  

    public BreadcrumbPanel(String name) {
        super(name);
        setHeightPreference(SizePreference.STATIC);
        setWidthPreference(SizePreference.FIT_CONTENT);
    }
    
    public void setPath(ContextPath path) {
        path = path == null ? ContextPath.ROOT : path;
        if (!this.path.equals(path)) {
            this.path = path;
            displayString = getDisplayString();
            len = displayString.length();
            invalidate();
        }
    }
    
    @Override
    protected void renderSelf(TerminalBatchBuilder batch) {
        printAt(batch, 0, 0, displayString, TextStyle.NORMAL);
    }

    public String getDisplayString() {

        String[] segments = path.isEmpty()
                ? new String[]{}
                : path.getStringSegments();

        final int MAX_SEGMENTS = 5; // total visible segments
        StringBuilder display = new StringBuilder("/");

        if (segments.length <= MAX_SEGMENTS) {

            for (String segment : segments) {
                display.append(truncate(segment)).append("/");
            }

        } else {

            // Always show first segment
            display.append(truncate(segments[0])).append("/");

            // Collapse middle
            display.append("…/");

            // Show last (MAX_SEGMENTS - 2) segments
            int tailCount = MAX_SEGMENTS - 2;
            for (int i = segments.length - tailCount; i < segments.length; i++) {
                display.append(truncate(segments[i])).append("/");
            }
        }

        return display.toString();
    }

    private String truncate(String segment) {
        return segment.length() > MAX_SEGMENT_LEN
                ? segment.substring(0, MAX_SEGMENT_LEN - 1) + "…"
                : segment;
    }

    @Override
    public int getMinWidth(){
        return len;
    }

    @Override
    public int getPreferredWidth(){
        return Math.max(getMinWidth(), len + getInsets().getHorizontal());
    }
}