package com.ptcrys.blockoffensive.util;

/** Tracks pairs of consecutive clicks on the same shop card. */
public final class ShopDoubleClick {
    private int type = -1;
    private int index = -1;
    private long lastClick;

    public boolean click(int type, int index, long now) {
        boolean buy = this.type == type && this.index == index
                && now >= lastClick && now - lastClick <= 300;
        if (buy) {
            reset();
        } else {
            this.type = type;
            this.index = index;
            lastClick = now;
        }
        return buy;
    }

    public void reset() {
        type = -1;
        index = -1;
    }
}
