package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapThumbnailRendererTest {

    @Test
    void gradientColorsAreDeterministicOpaquePairs() {
        int[] first = MapThumbnailRenderer.getGradientColors("de_dust2", "cs");
        int[] second = MapThumbnailRenderer.getGradientColors("de_dust2", "cs");

        assertEquals(2, first.length);
        assertArrayEquals(first, second);
        for (int color : first) {
            assertEquals(0xFF000000, color & 0xFF000000, "gradient colors must be opaque");
        }
    }

    @Test
    void gradientColorsStayWithinThePresetPalette() {
        int[] dust2 = MapThumbnailRenderer.getGradientColors("de_dust2", "cs");
        int[] inferno = MapThumbnailRenderer.getGradientColors("de_inferno", "csdm");
        assertEquals(2, dust2.length);
        assertEquals(2, inferno.length);
        for (int color : inferno) {
            assertEquals(0xFF000000, color & 0xFF000000, "gradient colors must be opaque");
        }
    }
}