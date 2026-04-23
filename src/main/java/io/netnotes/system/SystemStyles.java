package io.netnotes.system;

import io.netnotes.terminal.TextStyle;

public class SystemStyles {
    public static final TextStyle CARD_BORDER_STYLE         = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0x666666);
    public static final TextStyle CARD_FOCUSED_BORDER_STYLE = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0x888888);
    public static final TextStyle CARD_FILL_STYLE           = TextStyle.NORMAL.copy().bgRgb(0x111111);
    public static final TextStyle STATUS_FILL_STYLE         = TextStyle.NORMAL.copy().bgRgb(0x1E1E1E);
    public static final TextStyle PROMPT_BASE_STYLE         = TextStyle.NORMAL.copy().bgRgb(0x2A2A2A).fgRgb(0x888888);
    public static final TextStyle PROMPT_TEXT_STYLE         = TextStyle.NORMAL.copy().bgRgb(0x2A2A2A).fgRgb(0x666666);

    // ── Wizard chrome ─────────────────────────────────────────────────────────
    /** Bold app name at the top of the wizard card */
    public static final TextStyle WIZARD_TITLE_STYLE   = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0xDDDDDD).bold();
    /** Dim tagline beneath the title */
    public static final TextStyle WIZARD_TAGLINE_STYLE = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0x555555);
    /** TerminalDivider line characters */
    public static final TextStyle WIZARD_DIVIDER_STYLE = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0x2E2E2E);
    /** Step-progress footer ("Step 2 of 5  ·  Device Detection") */
    public static final TextStyle WIZARD_STEP_STYLE    = TextStyle.NORMAL.copy().bgRgb(0x111111).fgRgb(0x444444);
}