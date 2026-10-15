package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapRoomSummaryCodecTest {

    @Test
    void roundTripPreservesIconTexture() {
        MapRoomSummary summary = new MapRoomSummary(
                "cs", "de_test", "Test", "overworld", "0-0",
                true, false, true, 2, 5, true, false, true, 3,
                "fpsmatch:textures/gui/maps/de_test_icon.png");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MapRoomSummary.encode(summary, buf);
        MapRoomSummary decoded = MapRoomSummary.decode(buf);

        assertEquals(summary, decoded);
        assertEquals("fpsmatch:textures/gui/maps/de_test_icon.png", decoded.iconTexture());
    }
}