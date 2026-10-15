package com.ptcrys.blockoffensive.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, dependency-free layout math for the BlockOffensive buy-menu overlay.
 *
 * <p>The overlay is rendered via {@code ScreenEvent.Render.Post} from a snapshot that is
 * rebuilt when the grid geometry or selection changes. Keeping the card -&gt; icon / hit-test /
 * change-key math in this class (no Minecraft, no FPSMatch, no ModernUI imports) means the exact
 * logic that decides which slot icon goes on which card can be unit-tested without bootstrapping
 * a game.</p>
 */
public final class ShopOverlayLayout {

    private ShopOverlayLayout() {
    }

    /**
     * A laid-out card in window (physical) pixel coordinates.
     *
     * @param typeOrdinal ordinal of the {@code ItemType} this card belongs to
     * @param index       slot index within that type
     * @param x           card left (window space)
     * @param y           card top (window space)
     * @param w           card width
     * @param iconH       icon-area height (used for drawing the icon)
     * @param fullH       full card height (used for hover hit-testing)
     * @param laidOut     whether the card has a valid, laid-out rect this frame
     */
    public static final class CardDesc {
        public final int typeOrdinal;
        public final int index;
        public final float x;
        public final float y;
        public final float w;
        public final float iconH;
        public final float fullH;
        public final boolean laidOut;

        public CardDesc(int typeOrdinal, int index, float x, float y, float w,
                        float iconH, float fullH, boolean laidOut) {
            this.typeOrdinal = typeOrdinal;
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.iconH = iconH;
            this.fullH = fullH;
            this.laidOut = laidOut;
        }
    }

    /**
     * A card that passed layout and should have its icon drawn.
     */
    public static final class IconEntry {
        public final int typeOrdinal;
        public final int index;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public final float fullH;

        public IconEntry(int typeOrdinal, int index, float x, float y, float w, float h, float fullH) {
            this.typeOrdinal = typeOrdinal;
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.fullH = fullH;
        }
    }

    /**
     * Returns one icon entry per card that is laid out and has a positive icon rect.
     */
    public static List<IconEntry> buildIcons(List<CardDesc> cards) {
        List<IconEntry> out = new ArrayList<>(cards.size());
        for (CardDesc c : cards) {
            if (c.laidOut && c.w > 0f && c.iconH > 0f) {
                out.add(new IconEntry(c.typeOrdinal, c.index, c.x, c.y, c.w, c.iconH, c.fullH));
            }
        }
        return out;
    }

    /**
     * Returns the index into {@code cards} of the card whose full rect contains the given
     * window-space mouse position, or {@code -1} when the mouse is not over any card.
     */
    public static int findHovered(List<CardDesc> cards, double mouseX, double mouseY) {
        for (int i = 0; i < cards.size(); i++) {
            CardDesc c = cards.get(i);
            if (c.laidOut && c.w > 0f && c.fullH > 0f
                    && mouseX >= c.x && mouseX < c.x + c.w
                    && mouseY >= c.y && mouseY < c.y + c.fullH) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Builds a change-detection key whose per-card {@code typeOrdinal} is included, so a
     * category switch is detected even when the card rects (and thus the icon geometry)
     * are identical between the two categories.
     */
    public static int[] buildChangeKey(List<CardDesc> cards, int selectedTypeOrdinal,
                                       int selectedIndex, int scrollY) {
        int need = 4 + cards.size() * 6;
        int[] key = new int[need];
        key[0] = selectedTypeOrdinal;
        key[1] = selectedIndex;
        key[2] = cards.size();
        key[3] = scrollY;
        int p = 4;
        for (CardDesc c : cards) {
            key[p++] = c.typeOrdinal;
            key[p++] = c.index;
            key[p++] = Float.floatToIntBits(c.x);
            key[p++] = Float.floatToIntBits(c.y);
            key[p++] = Float.floatToIntBits(c.w);
            key[p++] = Float.floatToIntBits(c.fullH);
        }
        return key;
    }

    /**
     * Immutable description of the buy-menu grid, in window (physical) pixel coordinates.
     *
     * <p>The overlay renderer derives every card rect from these numbers, so the snapshot only
     * needs to carry geometry — never a per-card list whose positions depend on per-frame
     * {@code View} reads. {@code cardW} is measured once from the first laid-out card; the rest
     * are fixed by the layout constants.</p>
     */
    public static final class GridGeometry {
        public final int viewportX;
        public final int viewportY;
        public final int viewportWidth;
        public final int viewportHeight;
        public final int padLeft;
        public final int padTop;
        public final int columns;
        public final int cardW;
        public final int cardH;
        public final int gapX;
        public final int gapY;
        public final int iconH;
        public final int count;
        public final int typeOrdinal;
        public final int scrollY;

        public GridGeometry(int viewportX, int viewportY, int viewportWidth, int viewportHeight,
                            int padLeft, int padTop, int columns, int cardW, int cardH,
                            int gapX, int gapY, int iconH, int count, int typeOrdinal, int scrollY) {
            this.viewportX = viewportX;
            this.viewportY = viewportY;
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;
            this.padLeft = padLeft;
            this.padTop = padTop;
            this.columns = columns;
            this.cardW = cardW;
            this.cardH = cardH;
            this.gapX = gapX;
            this.gapY = gapY;
            this.iconH = iconH;
            this.count = count;
            this.typeOrdinal = typeOrdinal;
            this.scrollY = scrollY;
        }
    }

    /**
     * The card width for {@code columns} equally sized cards separated by {@code gap} pixels
     * within {@code availWidth} pixels. Kept in one place so the grid layout and
     * overlay always agree on the number.
     */
    public static int computeCardWidth(int availWidth, int columns, int gap) {
        if (columns <= 0) return 0;
        int totalGap = gap * (columns - 1);
        return Math.max(0, (availWidth - totalGap) / columns);
    }

    /**
     * Window-space rectangle of card {@code index}, given the grid origin ({@code originX},
     * {@code originY} already includes any content padding) and a {@code scrollY} scroll offset.
     * Returns {@code [left, top, width, height]}.
     */
    public static int[] cardRect(int index, int columns, int cardW, int cardH, int gapX, int gapY,
                                 int originX, int originY, int scrollY) {
        int col = index % columns;
        int row = index / columns;
        return new int[]{
                originX + col * (cardW + gapX),
                originY + row * (cardH + gapY) - scrollY,
                cardW,
                cardH
        };
    }

    /**
     * Builds the deterministic card list for a grid geometry. Card {@code i} has
     * {@code typeOrdinal= g.typeOrdinal} and {@code index= i}.
     */
    public static List<CardDesc> computeCards(GridGeometry g) {
        List<CardDesc> out = new ArrayList<>(g.count);
        for (int i = 0; i < g.count; i++) {
            int[] r = cardRect(i, g.columns, g.cardW, g.cardH, g.gapX, g.gapY,
                    g.viewportX + g.padLeft, g.viewportY + g.padTop, g.scrollY);
            out.add(new CardDesc(g.typeOrdinal, i, r[0], r[1], g.cardW, g.iconH, g.cardH, true));
        }
        return out;
    }

    /**
     * Builds the icon entries for a grid geometry (see {@link #computeCards}).
     */
    public static List<IconEntry> computeGridIcons(GridGeometry g) {
        return buildIcons(computeCards(g));
    }

    /**
     * The selected slot described by a category ordinal and a slot index.
     */
    public static final class Selection {
        public final int typeOrdinal;
        public final int index;

        public Selection(int typeOrdinal, int index) {
            this.typeOrdinal = typeOrdinal;
            this.index = index;
        }
    }

    /**
     * Resolves the remembered selection index for a category switch: the per-category remembered
     * index when valid, clamped to {@code [0, slotCount)}; {@code 0} otherwise. Returns {@code -1}
     * when the category has no slots.
     */
    public static int resolveSelectionIndex(int[] remembered, int categoryOrdinal, int slotCount) {
        if (slotCount <= 0) return -1;
        int index = 0;
        if (remembered != null && categoryOrdinal >= 0 && categoryOrdinal < remembered.length) {
            index = remembered[categoryOrdinal];
        }
        if (index < 0 || index >= slotCount) {
            index = 0;
        }
        return index;
    }

    /**
     * Resolves the full selection descriptor ({@code typeOrdinal}+{@code index}) after a category
     * switch, guaranteeing the descriptor always carries the current category, never a stale one.
     */
    public static Selection resolveSelection(int[] remembered, int categoryOrdinal, int slotCount) {
        return new Selection(categoryOrdinal, resolveSelectionIndex(remembered, categoryOrdinal, slotCount));
    }
}