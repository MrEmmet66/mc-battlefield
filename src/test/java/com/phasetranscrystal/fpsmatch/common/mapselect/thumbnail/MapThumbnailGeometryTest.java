package com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapThumbnailGeometryTest {

    @Test
    void computeOutputCapsLongerSideAndPreservesAspect() {
        assertArrayEquals(new int[]{128, 64}, ThumbnailGeometry.computeOutput(400, 200, 128));
        assertArrayEquals(new int[]{64, 128}, ThumbnailGeometry.computeOutput(200, 400, 128));
    }

    @Test
    void computeOutputNeverUpscalesSmallSources() {
        assertArrayEquals(new int[]{38, 38}, ThumbnailGeometry.computeOutput(38, 38, 128));
        assertArrayEquals(new int[]{10, 4}, ThumbnailGeometry.computeOutput(10, 4, 128));
        assertArrayEquals(new int[]{1, 1}, ThumbnailGeometry.computeOutput(1, 1, 128));
    }

    @Test
    void computeOutputClampsVeryThinSources() {
        int[] out = ThumbnailGeometry.computeOutput(1000, 1, 128);
        assertEquals(128, out[0]);
        assertEquals(1, out[1]);
    }

    @Test
    void computeOutputRejectsDegenerateCases() {
        assertArrayEquals(new int[]{0, 0}, ThumbnailGeometry.computeOutput(0, 10, 128));
        assertThrows(IllegalArgumentException.class, () -> ThumbnailGeometry.computeOutput(10, 10, 0));
    }

    @Test
    void mapOutputToSourceUsesNearestSampleCoordinates() {
        assertEquals(0, ThumbnailGeometry.mapOutputToSource(0, 8, 0, 40));
        assertEquals(35, ThumbnailGeometry.mapOutputToSource(7, 8, 0, 40));
        assertEquals(100, ThumbnailGeometry.mapOutputToSource(0, 4, 100, 40));
        assertEquals(130, ThumbnailGeometry.mapOutputToSource(3, 4, 100, 40));
    }

    @Test
    void aspectFitCentersAndFits() {
        ThumbnailGeometry.Rect fit = ThumbnailGeometry.aspectFit(128, 64, 64, 64);
        assertEquals(0, fit.x());
        assertEquals(16, fit.y());
        assertEquals(64, fit.width());
        assertEquals(32, fit.height());
    }

    @Test
    void aspectFitFillsBoxForMatchingAspect() {
        ThumbnailGeometry.Rect fit = ThumbnailGeometry.aspectFit(64, 64, 64, 64);
        assertEquals(0, fit.x());
        assertEquals(0, fit.y());
        assertEquals(64, fit.width());
        assertEquals(64, fit.height());
    }
}