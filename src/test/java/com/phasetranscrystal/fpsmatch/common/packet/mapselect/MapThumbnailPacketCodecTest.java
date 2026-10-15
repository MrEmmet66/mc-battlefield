package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MapThumbnailPacketCodecTest {

    @Test
    void s2cRoundTripPreservesDimensionsAndPixels() {
        byte[] pixels = new byte[4 * 4];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte) (i * 7);
        }
        MapThumbnailS2CPacket packet = new MapThumbnailS2CPacket("cs", "de_dust2", 4, 4, pixels);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MapThumbnailS2CPacket.encode(packet, buf);
        MapThumbnailS2CPacket decoded = MapThumbnailS2CPacket.decode(buf);

        assertEquals("cs", decoded.gameType());
        assertEquals("de_dust2", decoded.mapName());
        assertEquals(4, decoded.width());
        assertEquals(4, decoded.height());
        assertArrayEquals(pixels, decoded.pixels());
    }

    @Test
    void s2cEncodeCapsOversizedPayloads() {
        MapThumbnailS2CPacket packet = new MapThumbnailS2CPacket("csdm", "de_aztec", 200, 200, new byte[40000]);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MapThumbnailS2CPacket.encode(packet, buf);
        MapThumbnailS2CPacket decoded = MapThumbnailS2CPacket.decode(buf);

        assertEquals(0, decoded.pixels().length, "oversized payload must be clamped to empty");
    }

    @Test
    void c2sRoundTripPreservesIdentityAndForce() {
        MapThumbnailRequestC2SPacket packet = new MapThumbnailRequestC2SPacket("csdm", "de_aztec", true);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MapThumbnailRequestC2SPacket.encode(packet, buf);
        MapThumbnailRequestC2SPacket decoded = MapThumbnailRequestC2SPacket.decode(buf);

        assertEquals(packet, decoded);
        assertEquals(true, decoded.force());
    }
}