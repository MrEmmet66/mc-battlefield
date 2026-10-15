package com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail;

import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapThumbnailColorEncodingTest {

    @Test
    void packedMapColourByteRoundTripsThroughVanillaCodec() {
        MapColor[] colors = {
                MapColor.STONE, MapColor.GRASS, MapColor.WATER, MapColor.WOOD,
                MapColor.SAND, MapColor.COLOR_RED, MapColor.GOLD, MapColor.NONE
        };
        for (MapColor color : colors) {
            int packed = color.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
            assertEquals(color.calculateRGBColor(MapColor.Brightness.NORMAL),
                    MapColor.getColorFromPackedId(packed),
                    "packed colour must decode back to the original map colour");
        }
    }

    @Test
    void emptySentinelDecodesToTransparent() {
        assertEquals(0, MapColor.getColorFromPackedId(0));
        assertEquals(0, MapColor.getColorFromPackedId(0) & 0xFF000000, "top byte (alpha) must be zero");
    }

    @Test
    void solidMapColoursDecodeToOpaqueArgb() {
        MapColor[] colors = {MapColor.STONE, MapColor.GRASS, MapColor.WATER, MapColor.WOOD,
                MapColor.SAND, MapColor.COLOR_RED, MapColor.GOLD};
        for (MapColor color : colors) {
            int packed = color.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
            int argb = MapColor.getColorFromPackedId(packed);
            assertEquals(0xFF000000, argb & 0xFF000000,
                    color + " must decode to opaque (alpha 0xFF), got 0x" + Integer.toHexString(argb));
        }
    }
}