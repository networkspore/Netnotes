package io.netnotes.system;

import io.netnotes.engine.io.ContextPath;
import io.netnotes.engine.ui.SizePreference;
import io.netnotes.terminal.components.text.TerminalLabel;

/**
 * BreadcrumbPanel - Displays context/path/navigation
 */
class BreadcrumbPanel extends TerminalLabel {
    public static final int MAX_SEGMENT_LEN = 8;


    private ContextPath path = ContextPath.ROOT;;

    public BreadcrumbPanel(String name) {
        super(name, "/");
        setWidthPreference(SizePreference.FIT_CONTENT);
        setMaxLines(1);
    }
    
    public void setPath(ContextPath path) {
        path = path == null ? ContextPath.ROOT : path;
        if (!this.path.equals(path)) {
            this.path = path;
            setText(getDisplayString());
            invalidate();
        }
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


 
}