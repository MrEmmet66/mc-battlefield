package com.phasetranscrystal.fpsmatch.common.mapselect;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail.MapThumbnailSampler;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapThumbnailS2CPacket;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side map thumbnail generator. Samples each map area with vanilla
 * {@link net.minecraft.world.level.material.MapColor} on demand and replies with a compact
 * (<= 128 px on the longer side, one packed byte per pixel) payload, cached for a short TTL.
 * Runs entirely on the server thread inside the C2S packet handler.
 */
public final class MapThumbnailService {
    private static final long CACHE_TTL_MILLIS = 300_000L;
    private static final Map<String, CachedThumbnail> CACHE = new ConcurrentHashMap<>();

    private record CachedThumbnail(int width, int height, byte[] pixels, long createdAt) {
    }

    private MapThumbnailService() {
    }

    public static void request(ServerPlayer player, String gameType, String mapName, boolean force) {
        if (player == null || gameType == null || mapName == null) {
            return;
        }
        String key = gameType + '\n' + mapName;
        if (!force) {
            CachedThumbnail cached = CACHE.get(key);
            if (cached != null && System.currentTimeMillis() - cached.createdAt() < CACHE_TTL_MILLIS) {
                send(player, gameType, mapName, cached);
                return;
            }
        }

        Optional<BaseMap> found = MapRoomQueryService.findMap(gameType, mapName);
        if (found.isEmpty()) {
            return;
        }
        BaseMap map = found.get();
        try {
            MapThumbnailSampler.Sample sample =
                    MapThumbnailSampler.sample(map.getServerLevel(), map.getMapArea());
            if (sample.width() <= 0 || sample.height() <= 0) {
                return;
            }
            CachedThumbnail computed = new CachedThumbnail(
                    sample.width(),
                    sample.height(),
                    sample.pixels().clone(),
                    System.currentTimeMillis()
            );
            CACHE.put(key, computed);
            send(player, gameType, mapName, computed);
        } catch (RuntimeException ex) {
            FPSMatch.LOGGER.warn("Failed to generate map thumbnail for {}:{}", gameType, mapName, ex);
        }
    }

    private static void send(ServerPlayer player, String gameType, String mapName, CachedThumbnail cached) {
        FPSMatch.sendToPlayer(player, new MapThumbnailS2CPacket(
                gameType, mapName, cached.width(), cached.height(), cached.pixels()
        ));
    }
}