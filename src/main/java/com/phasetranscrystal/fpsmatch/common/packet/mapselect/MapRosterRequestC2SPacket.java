package com.phasetranscrystal.fpsmatch.common.packet.mapselect;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.mapselect.MapRoomQueryService;
import com.phasetranscrystal.fpsmatch.common.mapselect.MapSelectionAccessSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server request for a room roster while the map browser is showing the inline detail
 * preview. Unlike {@link MapRoomActionC2SPacket.Action#REQUEST_DETAIL}, the reply is passive, so
 * the browser stays open and the roster is rendered in place (name + team + ready/spectator mark).
 */
public record MapRosterRequestC2SPacket(String gameType, String mapName) {
    private static final int ID_MAX_LENGTH = 128;

    public static void encode(MapRosterRequestC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.gameType(), ID_MAX_LENGTH);
        buf.writeUtf(packet.mapName(), ID_MAX_LENGTH);
    }

    public static MapRosterRequestC2SPacket decode(FriendlyByteBuf buf) {
        return new MapRosterRequestC2SPacket(buf.readUtf(ID_MAX_LENGTH), buf.readUtf(ID_MAX_LENGTH));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (!MapSelectionAccessSync.canUseMapSelection(player)) {
                FPSMatch.sendToPlayer(player, new MapSelectionAccessS2CPacket(false));
                FPSMatch.sendToPlayer(player, new MapRoomToastS2CPacket(Component.translatable("gui.fpsm.map_select.action.no_permission"), true));
                return;
            }
            MapRoomQueryService.findMap(gameType(), mapName()).ifPresentOrElse(
                    map -> FPSMatch.sendToPlayer(player, new MapRoomDetailS2CPacket(MapRoomQueryService.detail(player, map), true)),
                    () -> FPSMatch.sendToPlayer(player, new MapRoomToastS2CPacket(Component.translatable("gui.fpsm.map_select.action.map_not_found"), true))
            );
        });
        ctx.get().setPacketHandled(true);
    }
}