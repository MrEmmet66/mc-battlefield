package com.ptcrys.blockoffensive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlaySnapshotKeyTest {

    @Test
    void nullPreviousKeyIsChanged() {
        assertTrue(OverlaySnapshotKey.changed(null, new int[]{0, 1}, 2));
    }

    @Test
    void differentLengthIsChanged() {
        int[] prev = new int[]{1, 2, 3};
        assertTrue(OverlaySnapshotKey.changed(prev, new int[]{1, 2}, 2));
        assertTrue(OverlaySnapshotKey.changed(prev, new int[]{1, 2, 3, 4}, 4));
    }

    @Test
    void differentElementIsChanged() {
        int[] prev = new int[]{1, 2, 3};
        int[] cur = new int[]{1, 9, 3};
        assertTrue(OverlaySnapshotKey.changed(prev, cur, 3));
    }

    @Test
    void identicalKeyIsNotChanged() {
        int[] prev = new int[]{1, 2, 3};
        int[] cur = new int[]{1, 2, 3};
        assertFalse(OverlaySnapshotKey.changed(prev, cur, 3));
    }

    @Test
    void extraTrailingElementsInReusableBufferAreIgnored() {
        int[] prev = new int[]{1, 2};
        int[] cur = new int[]{1, 2, 99, 100};
        assertFalse(OverlaySnapshotKey.changed(prev, cur, 2));
    }
}