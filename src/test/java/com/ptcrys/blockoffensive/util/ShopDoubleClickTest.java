package com.ptcrys.blockoffensive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopDoubleClickTest {
    @Test
    void buysOncePerPairAndDoesNotTreatTripleClickAsTwoPurchases() {
        ShopDoubleClick clicks = new ShopDoubleClick();
        assertFalse(clicks.click(0, 2, 1000));
        assertTrue(clicks.click(0, 2, 1200));
        assertFalse(clicks.click(0, 2, 1250));
        assertTrue(clicks.click(0, 2, 1300));
    }

    @Test
    void requiresSameItemAndCategoryWithinTimeout() {
        ShopDoubleClick clicks = new ShopDoubleClick();
        assertFalse(clicks.click(0, 2, 1000));
        assertFalse(clicks.click(0, 3, 1050));
        assertFalse(clicks.click(1, 3, 1100));
        assertFalse(clicks.click(1, 3, 1500));
        assertTrue(clicks.click(1, 3, 1800));
    }

    @Test
    void switchingTabsOrRightClickingBreaksThePair() {
        ShopDoubleClick clicks = new ShopDoubleClick();
        assertFalse(clicks.click(0, 2, 1000));
        clicks.reset();
        assertFalse(clicks.click(0, 2, 1100));
    }
}
