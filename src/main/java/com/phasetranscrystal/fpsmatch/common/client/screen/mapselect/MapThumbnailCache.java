package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapThumbnailRequestC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side cache of server-generated map thumbnails. Received pixel payloads are decoded from
 * packed vanilla map colours into a {@link DynamicTexture} registered under a deterministic resource
 * location, held in a small LRU, and released on eviction. Request sending is rate-limited so the
 * render loop can safely call {@link #request} every frame.
 */
public final class MapThumbnailCache {
    private static final int MAX_ENTRIES = 64;
    private static final long REQUEST_COOLDOWN_MILLIS = 5000L;

    private static final Map<String, Entry> TEXTURES = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, Long> LAST_REQUEST = new HashMap<>();

    private record Entry(ResourceLocation location, int width, int height) {
    }

    public record Thumbnail(ResourceLocation location, int width, int height) {
    }

    private MapThumbnailCache() {
    }

    public static Optional<Thumbnail> resolve(String gameType, String mapName) {
        Entry entry = TEXTURES.get(key(gameType, mapName));
        return entry == null
                ? Optional.empty()
                : Optional.of(new Thumbnail(entry.location(), entry.width(), entry.height()));
    }

    public static void store(String gameType, String mapName, int width, int height, byte[] pixels) {
        if (gameType == null || mapName == null || width <= 0 || height <= 0
                || pixels == null || pixels.length < width * height) {
            return;
        }
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            FPSMatch.LOGGER.warn("MapThumbnailCache.store called off the render thread; ignored");
            return;
        }
        String key = key(gameType, mapName);
        NativeImage image = new NativeImage(width, height, true);
        DynamicTexture texture = null;
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int packed = pixels[y * width + x] & 0xFF;
                    image.setPixelRGBA(x, y, MapColor.getColorFromPackedId(packed));
                }
            }
            texture = new DynamicTexture(image);
            ResourceLocation location = location(key);
            TextureManagerApi.releaseIfPresent(location);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            Entry previous = TEXTURES.put(key, new Entry(location, width, height));
            if (previous != null && !previous.location().equals(location)) {
                Minecraft.getInstance().getTextureManager().release(previous.location());
            }
            evictOverflow();
        } catch (RuntimeException ex) {
            if (texture != null) {
                texture.close();
            } else {
                image.close();
            }
            FPSMatch.LOGGER.warn("Failed to store map thumbnail for {}:{}", gameType, mapName, ex);
        }
    }

    public static void request(String gameType, String mapName) {
        request(gameType, mapName, false);
    }

    public static void requestForce(String gameType, String mapName) {
        request(gameType, mapName, true);
    }

    private static void request(String gameType, String mapName, boolean force) {
        if (gameType == null || mapName == null || gameType.isEmpty() || mapName.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        String key = key(gameType, mapName);
        if (!force && TEXTURES.containsKey(key)) {
            return;
        }
        if (!force) {
            long now = System.currentTimeMillis();
            Long last = LAST_REQUEST.get(key);
            if (last != null && now - last < REQUEST_COOLDOWN_MILLIS) {
                return;
            }
            LAST_REQUEST.put(key, now);
        }
        FPSMatch.sendToServer(new MapThumbnailRequestC2SPacket(gameType, mapName, force));
    }

    private static void evictOverflow() {
        while (TEXTURES.size() > MAX_ENTRIES) {
            Map.Entry<String, Entry> eldest = TEXTURES.entrySet().iterator().next();
            TEXTURES.remove(eldest.getKey());
            Minecraft.getInstance().getTextureManager().release(eldest.getValue().location());
        }
    }

    private static String key(String gameType, String mapName) {
        return gameType + '\n' + mapName;
    }

    private static ResourceLocation location(String key) {
        StringBuilder sb = new StringBuilder("map_thumb/");
        appendSafe(sb, key);
        sb.append('/').append(Integer.toHexString(key.hashCode()));
        return new ResourceLocation(FPSMatch.MODID, sb.toString());
    }

    private static void appendSafe(StringBuilder sb, String value) {
        int length = Math.min(value.length(), 48);
        for (int i = 0; i < length; i++) {
            char c = Character.toLowerCase(value.charAt(i));
            sb.append(isPathAllowed(c) ? c : '_');
        }
    }

    private static boolean isPathAllowed(char c) {
        return c == '/' || c == '.' || c == '_' || c == '-'
                || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static final class TextureManagerApi {
        private TextureManagerApi() {
        }

        static void releaseIfPresent(ResourceLocation location) {
            try {
                if (Minecraft.getInstance().getTextureManager().getTexture(location) != null) {
                    Minecraft.getInstance().getTextureManager().release(location);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }
}