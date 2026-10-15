package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import com.phasetranscrystal.fpsmatch.common.packet.ClientPacketExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client map thumbnail payload. {@code pixels} holds one packed vanilla map-colour byte
 * per pixel ({@code 0} = empty/transparent), row-major, {@code width * height} bytes.
 */
public record MapThumbnailS2CPacket(String gameType, String mapName, int width, int height, byte[] pixels) {
    private static final int ID_MAX_LENGTH = 128;
    private static final int MAX_PIXELS = 128 * 128;

    public static void encode(MapThumbnailS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.gameType(), ID_MAX_LENGTH);
        buf.writeUtf(packet.mapName(), ID_MAX_LENGTH);
        buf.writeVarInt(packet.width());
        buf.writeVarInt(packet.height());
        byte[] safe = packet.pixels() == null ? new byte[0] : packet.pixels();
        if (safe.length > MAX_PIXELS) {
            safe = new byte[0];
        }
        buf.writeByteArray(safe);
    }

    public static MapThumbnailS2CPacket decode(FriendlyByteBuf buf) {
        return new MapThumbnailS2CPacket(
                buf.readUtf(ID_MAX_LENGTH),
                buf.readUtf(ID_MAX_LENGTH),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readByteArray()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ClientPacketExecutor.execute(ctx, this);
    }
}