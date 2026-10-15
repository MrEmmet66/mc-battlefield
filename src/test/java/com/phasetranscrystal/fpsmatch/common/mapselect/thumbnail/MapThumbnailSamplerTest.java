package com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapThumbnailSamplerTest {

    @Test
    void emptyColumnSentinelIsZero() {
        assertEquals(0, MapThumbnailSampler.EMPTY);
    }

    @Test
    void nullLevelProducesDegenerateSample() {
        MapThumbnailSampler.Sample sample = MapThumbnailSampler.sample(null, null);
        assertEquals(0, sample.width());
        assertEquals(0, sample.height());
        assertEquals(0, sample.pixels().length);
    }
}