package com.ptcrys.blockoffensive.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopOverlayLayoutTest {

    private static ShopOverlayLayout.CardDesc card(int typeOrdinal, int index, float x, float y,
                                                   float w, float iconH, float fullH) {
        return new ShopOverlayLayout.CardDesc(typeOrdinal, index, x, y, w, iconH, fullH, true);
    }

    @Test
    void categorySwitchChangesTypesInSnapshot() {
        List<ShopOverlayLayout.CardDesc> weapons = Arrays.asList(
                card(0, 0, 0, 0, 120, 68, 112),
                card(0, 1, 130, 0, 120, 68, 112));
        List<ShopOverlayLayout.CardDesc> attachments = Arrays.asList(
                card(2, 0, 0, 0, 120, 68, 112),
                card(2, 1, 130, 0, 120, 68, 112));

        assertEquals(List.of(0, 0),
                ShopOverlayLayout.buildIcons(weapons).stream().map(e -> e.typeOrdinal).toList());
        assertEquals(List.of(2, 2),
                ShopOverlayLayout.buildIcons(attachments).stream().map(e -> e.typeOrdinal).toList());
    }

    @Test
    void firstFrameAfterLayoutIncludesAllCards() {
        List<ShopOverlayLayout.CardDesc> cards = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            cards.add(card(0, i, 0, i * 120, 120, 68, 112));
        }
        List<ShopOverlayLayout.IconEntry> icons = ShopOverlayLayout.buildIcons(cards);
        assertEquals(23, icons.size());
        for (int i = 0; i < 23; i++) {
            assertEquals(i, icons.get(i).index);
        }
    }

    @Test
    void notLaidOutCardsAreSkipped() {
        List<ShopOverlayLayout.CardDesc> cards = Arrays.asList(
                card(0, 0, 0, 0, 120, 68, 112),
                new ShopOverlayLayout.CardDesc(0, 1, 130, 0, 120, 68, 112, false));
        assertEquals(1, ShopOverlayLayout.buildIcons(cards).size());
    }

    @Test
    void hoveredIsNullWhenMouseNotOverCards() {
        List<ShopOverlayLayout.CardDesc> cards = Arrays.asList(
                card(0, 0, 100, 200, 120, 68, 112),
                card(0, 1, 230, 200, 120, 68, 112));
        assertEquals(-1, ShopOverlayLayout.findHovered(cards, 50, 250));
        assertEquals(-1, ShopOverlayLayout.findHovered(cards, 500, 250));
        assertEquals(-1, ShopOverlayLayout.findHovered(cards, 150, 100));
    }

    @Test
    void hoveredMatchesCardUnderMouse() {
        List<ShopOverlayLayout.CardDesc> cards = Arrays.asList(
                card(0, 0, 100, 200, 120, 68, 112),
                card(2, 3, 230, 200, 120, 68, 112));
        int idx = ShopOverlayLayout.findHovered(cards, 240, 300);
        org.junit.jupiter.api.Assertions.assertEquals(1, idx);
        org.junit.jupiter.api.Assertions.assertEquals(2, cards.get(idx).typeOrdinal);
        org.junit.jupiter.api.Assertions.assertEquals(3, cards.get(idx).index);
    }

    @Test
    void changeKeyDetectsTypeChangeWithEqualRects() {
        List<ShopOverlayLayout.CardDesc> weapons = Arrays.asList(
                card(0, 0, 10, 20, 120, 68, 112),
                card(0, 1, 140, 20, 120, 68, 112));
        List<ShopOverlayLayout.CardDesc> attachments = Arrays.asList(
                card(2, 0, 10, 20, 120, 68, 112),
                card(2, 1, 140, 20, 120, 68, 112));

        int[] prev = ShopOverlayLayout.buildChangeKey(weapons, 0, 0, 0);
        int[] cur = ShopOverlayLayout.buildChangeKey(attachments, 2, 0, 0);

        assertEquals(prev.length, cur.length);
        assertTrue(OverlaySnapshotKey.changed(prev, cur, cur.length));
        assertNotEquals(prev[0], cur[0]);
    }

    @Test
    void changeKeyStableForIdenticalSnapshot() {
        List<ShopOverlayLayout.CardDesc> cards = Arrays.asList(
                card(0, 0, 10, 20, 120, 68, 112),
                card(0, 1, 140, 20, 120, 68, 112));
        int[] a = ShopOverlayLayout.buildChangeKey(cards, 0, 0, 0);
        int[] b = ShopOverlayLayout.buildChangeKey(cards, 0, 0, 0);
        assertFalse(OverlaySnapshotKey.changed(a, b, b.length));
    }

    @Test
    void computeCardWidthColumns3Vs2() {
        assertEquals(94, ShopOverlayLayout.computeCardWidth(300, 3, 8));
        assertEquals(146, ShopOverlayLayout.computeCardWidth(300, 2, 8));
    }

    @Test
    void cardRectFirstRowColumns3() {
        assertArrayEquals(new int[]{10, 20, 100, 112},
                ShopOverlayLayout.cardRect(0, 3, 100, 112, 8, 8, 10, 20, 0));
        assertArrayEquals(new int[]{118, 20, 100, 112},
                ShopOverlayLayout.cardRect(1, 3, 100, 112, 8, 8, 10, 20, 0));
        assertArrayEquals(new int[]{226, 20, 100, 112},
                ShopOverlayLayout.cardRect(2, 3, 100, 112, 8, 8, 10, 20, 0));
    }

    @Test
    void cardRectScrollOffsetSecondRow() {
        assertArrayEquals(new int[]{10, 110, 100, 112},
                ShopOverlayLayout.cardRect(3, 3, 100, 112, 8, 8, 10, 20, 30));
    }

    @Test
    void cardRectLastPartialRow() {
        assertArrayEquals(new int[]{10, 140, 100, 112},
                ShopOverlayLayout.cardRect(3, 3, 100, 112, 8, 8, 10, 20, 0));
        assertArrayEquals(new int[]{118, 140, 100, 112},
                ShopOverlayLayout.cardRect(4, 3, 100, 112, 8, 8, 10, 20, 0));
    }

    @Test
    void computeGridIconsAreDeterministicFromGeometry() {
        ShopOverlayLayout.GridGeometry g = new ShopOverlayLayout.GridGeometry(
                5, 7, 316, 232, 0, 0, 3, 100, 112, 8, 8, 68, 5, 2, 40);
        List<ShopOverlayLayout.IconEntry> icons = ShopOverlayLayout.computeGridIcons(g);
        assertEquals(5, icons.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(2, icons.get(i).typeOrdinal);
            assertEquals(i, icons.get(i).index);
            assertEquals(100f, icons.get(i).w);
            assertEquals(68f, icons.get(i).h);
            assertEquals(112f, icons.get(i).fullH);
        }
        assertEquals(5f, icons.get(0).x);
        assertEquals(7f - 40f, icons.get(0).y);
        assertEquals(5f + 108f, icons.get(1).x);
        assertEquals(7f + 120f - 40f, icons.get(3).y);
    }

    @Test
    void resolveSelectionIndexRemembersPerCategoryAndClamps() {
        int[] remembered = new int[]{1, 3, 0, -5, 99, 0, 0, 0};
        assertEquals(1, ShopOverlayLayout.resolveSelectionIndex(remembered, 0, 5));
        assertEquals(3, ShopOverlayLayout.resolveSelectionIndex(remembered, 1, 5));
        assertEquals(0, ShopOverlayLayout.resolveSelectionIndex(remembered, 3, 5));
        assertEquals(0, ShopOverlayLayout.resolveSelectionIndex(remembered, 4, 5));
        assertEquals(-1, ShopOverlayLayout.resolveSelectionIndex(remembered, 0, 0));
        assertEquals(0, ShopOverlayLayout.resolveSelectionIndex(null, 1, 5));
    }

    @Test
    void resolveSelectionCarriesCurrentCategoryOnSwitch() {
        int[] remembered = new int[]{0, 2, 0, 0, 0, 0, 0, 0};
        ShopOverlayLayout.Selection s = ShopOverlayLayout.resolveSelection(remembered, 1, 6);
        assertEquals(1, s.typeOrdinal);
        assertEquals(2, s.index);
        ShopOverlayLayout.Selection clamp = ShopOverlayLayout.resolveSelection(remembered, 1, 1);
        assertEquals(1, clamp.typeOrdinal);
        assertEquals(0, clamp.index);
    }

    @Test
    void lastPartialRowKeepsRegularRowWidth() {
        float cardW = 100f;
        int cardH = 112;
        int gapX = 8;
        int gapY = 8;
        ShopOverlayLayout.GridGeometry g = new ShopOverlayLayout.GridGeometry(
                10, 20, 316, 232, 0, 0, 3, (int) cardW, cardH,
                gapX, gapY, 68, 11, 0, 0);
        List<ShopOverlayLayout.CardDesc> cards = ShopOverlayLayout.computeCards(g);
        assertEquals(11, cards.size());
        for (int i = 0; i < cards.size(); i++) {
            assertEquals(cardW, cards.get(i).w, 0.001f);
        }
        assertEquals(cards.get(0).x, cards.get(9).x, 0.001f);
        assertEquals(cards.get(1).x, cards.get(10).x, 0.001f);
        assertEquals(cards.get(2).x, cards.get(8).x, 0.001f);
    }
}