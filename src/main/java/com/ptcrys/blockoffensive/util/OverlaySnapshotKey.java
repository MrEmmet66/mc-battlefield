package com.ptcrys.blockoffensive.util;

public final class OverlaySnapshotKey {

    private OverlaySnapshotKey() {
    }

    public static boolean changed(int[] previousKey, int[] currentKey, int length) {
        if (previousKey == null || previousKey.length != length) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (previousKey[i] != currentKey[i]) {
                return true;
            }
        }
        return false;
    }
}