package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.math.Size;

/**
 * Pure responsive layout calculation for the LDLib2 map-room browser window.
 * <p>
 * The browser is drawn as a compact, centred floating window rather than a full-screen page. All
 * rects are window-local GUI coordinates (0,0 = top-left of the window). The window follows a
 * fixed vertical rhythm: a 30px header (brand/title/close/rooms-count + 1px divider), a 22px
 * toolbar (filter tabs + search), a side-by-side list/detail content band (58%/42%), and a 26px
 * bottom action row, all padded by {@link #PADDING}.
 */
public record MapSelectionLayoutModel(
        Rect header,
        Rect filters,
        Rect roomList,
        Rect detail,
        Rect actions,
        boolean compact
) {
    public static final int PADDING = 10;
    public static final int HEADER_HEIGHT = 30;
    public static final int TOOLBAR_HEIGHT = 22;
    public static final int ACTIONS_HEIGHT = 26;
    /** Gap between the header divider and the toolbar row. */
    public static final int TOOLBAR_GAP = 6;
    /** Gap between the toolbar and the content band, and between content and the action row. */
    public static final int CONTENT_GAP = 8;
    /** Percentage of the content width given to the room list (remainder is the detail panel). */
    public static final int LIST_RATIO = 58;

    public static final int MIN_WINDOW_WIDTH = 520;
    public static final int MAX_WINDOW_WIDTH = 860;
    public static final int MIN_WINDOW_HEIGHT = 300;
    public static final int MAX_WINDOW_HEIGHT = 520;
    /** Width factor applied to the screen width before clamping. */
    public static final float WIDTH_FACTOR = 0.72f;
    /** Height factor applied to the screen height before clamping. */
    public static final float HEIGHT_FACTOR = 0.78f;

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("layout dimensions must be non-negative");
            }
        }

        public boolean intersects(Rect other) {
            return x < other.x + other.width && other.x < x + width
                    && y < other.y + other.height && other.y < y + height;
        }
    }

    /** Compute the centred floating-window size for a given screen size (already clamped). */
    public static Size windowSize(int screenWidth, int screenHeight) {
        int w = Math.round(screenWidth * WIDTH_FACTOR);
        int h = Math.round(screenHeight * HEIGHT_FACTOR);
        w = Math.max(MIN_WINDOW_WIDTH, Math.min(MAX_WINDOW_WIDTH, w));
        h = Math.max(MIN_WINDOW_HEIGHT, Math.min(MAX_WINDOW_HEIGHT, h));
        return Size.of(Math.min(w, screenWidth), Math.min(h, screenHeight));
    }

    public static MapSelectionLayoutModel responsive(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("window dimensions must be positive");
        }
        int contentWidth = Math.max(1, width - PADDING * 2);
        int toolbarY = HEADER_HEIGHT + TOOLBAR_GAP;
        int contentTop = toolbarY + TOOLBAR_HEIGHT + CONTENT_GAP;
        int actionsY = height - PADDING - ACTIONS_HEIGHT;
        int contentBottom = actionsY - CONTENT_GAP;
        int contentHeight = Math.max(1, contentBottom - contentTop);

        int listWidth = contentWidth * LIST_RATIO / 100;
        int detailWidth = Math.max(1, contentWidth - listWidth - CONTENT_GAP);

        return new MapSelectionLayoutModel(
                new Rect(0, 0, width, HEADER_HEIGHT),
                new Rect(PADDING, toolbarY, contentWidth, TOOLBAR_HEIGHT),
                new Rect(PADDING, contentTop, listWidth, contentHeight),
                new Rect(PADDING + listWidth + CONTENT_GAP, contentTop, detailWidth, contentHeight),
                new Rect(PADDING, actionsY, contentWidth, ACTIONS_HEIGHT),
                width < 620
        );
    }
}