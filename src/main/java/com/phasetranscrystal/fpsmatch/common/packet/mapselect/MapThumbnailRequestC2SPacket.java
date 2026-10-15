package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import com.phasetranscrystal.fpsmatch.common.mapselect.MapThumbnailService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server thumbnail request. {@code force} bypasses the server-side TTL cache so an admin
 * capture re-samples the map immediately.
 */
public record MapThumbnailRequestC2SPacket(String gameType, String mapName, boolean force) {
    private static final int ID_MAX_LENGTH = 128;

    public static void encode(MapThumbnailRequestC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.gameType(), ID_MAX_LENGTH);
        buf.writeUtf(packet.mapName(), ID_MAX_LENGTH);
        buf.writeBoolean(packet.force());
    }

    public static MapThumbnailRequestC2SPacket decode(FriendlyByteBuf buf) {
        return new MapThumbnailRequestC2SPacket(
                buf.readUtf(ID_MAX_LENGTH),
                buf.readUtf(ID_MAX_LENGTH),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            MapThumbnailService.request(player, gameType(), mapName(), force());
        });
        ctx.get().setPacketHandled(true);
    }
}