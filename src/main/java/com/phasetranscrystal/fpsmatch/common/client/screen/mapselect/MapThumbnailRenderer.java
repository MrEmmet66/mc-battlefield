package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.mapselect.thumbnail.ThumbnailGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * 地图缩略图/预览图渲染器。
 * <p>
 * 渲染优先级：
 * <ol>
 *   <li>显示配置的 iconTexture / backgroundTexture（资源存在时）</li>
 *   <li>服务端生成的真实地图俯视缩略图（{@link MapThumbnailCache}，邻近采样保持像素清晰）</li>
 *   <li>柔和渐变色块兜底，并异步向服务端请求真实缩略图</li>
 * </ol>
 */
public final class MapThumbnailRenderer {
    private MapThumbnailRenderer() {
    }

    /**
     * 8 组柔和渐变色对（顶部深色 → 底部浅色）。
     */
    private static final int[][] GRADIENT_PAIRS = {
            {0xFF2C3E50, 0xFF4A6890}, // 0 深蓝灰
            {0xFF3D2E4A, 0xFF6B4F7D}, // 1 深紫灰
            {0xFF1F3A3D, 0xFF3A6166}, // 2 深青灰
            {0xFF2A3D2A, 0xFF4A6B4A}, // 3 深绿灰
            {0xFF3D2E22, 0xFF6B4F36}, // 4 深棕灰
            {0xFF3D2222, 0xFF6B3636}, // 5 深红灰
            {0xFF2A2A2A, 0xFF4A4A4A}, // 6 深石灰
            {0xFF3D3522, 0xFF6B5E36}, // 7 深金灰
    };

    public static void render(GuiGraphics graphics, int x, int y, int width, int height,
                              String texturePath, String mapName, String gameType,
                              String displayName, boolean showLabel) {
        if (texturePath != null && !texturePath.isEmpty() && tryRenderTexture(graphics, x, y, width, height, texturePath)) {
            if (showLabel) {
                drawBottomLabel(graphics, x, y, width, height, displayName);
            }
            return;
        }
        if (tryRenderGenerated(graphics, x, y, width, height, mapName, gameType, displayName, showLabel)) {
            return;
        }
        renderGradientBlock(graphics, x, y, width, height, mapName, gameType, displayName, showLabel);
        MapThumbnailCache.request(gameType, mapName);
    }

    private static boolean tryRenderTexture(GuiGraphics graphics, int x, int y, int width, int height, String texturePath) {
        try {
            ResourceLocation rl = new ResourceLocation(texturePath);
            Minecraft mc = Minecraft.getInstance();
            if (mc.getResourceManager().getResource(rl).isEmpty()) {
                return false;
            }
            RenderSystem.enableBlend();
            graphics.blit(rl, x, y, 0, 0, width, height, width, height);
            RenderSystem.disableBlend();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 渲染服务端生成的真实地图缩略图：先平铺 CARD 底色（兼容透明像素与黑边），再按比例
     * 居中绘制缩略图，最后叠加模式角标。
     */
    private static boolean tryRenderGenerated(GuiGraphics graphics, int x, int y, int width, int height,
                                              String mapName, String gameType, String displayName, boolean showLabel) {
        if (mapName == null || mapName.isEmpty()) {
            return false;
        }
        Optional<MapThumbnailCache.Thumbnail> thumb = MapThumbnailCache.resolve(gameType, mapName);
        if (thumb.isEmpty()) {
            return false;
        }
        try {
            ResourceLocation location = thumb.get().location();
            int imgW = thumb.get().width();
            int imgH = thumb.get().height();
            graphics.fill(x, y, x + width, y + height, FPSMLdlib2Theme.CARD);
            ThumbnailGeometry.Rect fit = ThumbnailGeometry.aspectFit(imgW, imgH, width, height);
            graphics.blit(location, x + fit.x(), y + fit.y(), fit.width(), fit.height(),
                    0, 0, imgW, imgH, imgW, imgH);
            drawModeBadge(graphics, x, y, width, height, gameType);
            if (showLabel) {
                drawBottomLabel(graphics, x, y, width, height, displayName);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 渲染柔和渐变色块 + 模式角标 + 暗角。
     */
    private static void renderGradientBlock(GuiGraphics graphics, int x, int y, int width, int height,
                                            String mapName, String gameType, String displayName, boolean showLabel) {
        int[] colors = getGradientColors(mapName, gameType);
        int topColor = colors[0];
        int bottomColor = colors[1];

        int segments = 8;
        int segHeight = height / segments;
        int remainder = height % segments;
        int curY = y;
        for (int i = 0; i < segments; i++) {
            int h = segHeight + (i < remainder ? 1 : 0);
            float ratio = segments == 1 ? 0f : (float) i / (segments - 1);
            int color = lerpColor(topColor, bottomColor, ratio);
            graphics.fill(x, curY, x + width, curY + h, color);
            curY += h;
        }

        drawVignette(graphics, x, y, width, height);
        drawModeBadge(graphics, x, y, width, height, gameType);

        if (showLabel) {
            drawBottomLabel(graphics, x, y, width, height, displayName);
        }
    }

    /**
     * 右上角小幅模式角标（"CS"/"DM"）。
     */
    private static void drawModeBadge(GuiGraphics graphics, int x, int y, int width, int height, String gameType) {
        String label = modeLabel(gameType);
        if (label == null || label.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int pad = 3;
        int badgeWidth = mc.font.width(label) + pad * 2;
        int badgeHeight = mc.font.lineHeight + 2;
        int bx = x + width - badgeWidth - 3;
        int by = y + 3;
        graphics.fill(bx, by, bx + badgeWidth, by + badgeHeight, 0xB0000000);
        graphics.drawString(mc.font, label, bx + pad, by + 1, 0xFFFFFFFF, false);
    }

    private static void drawBottomLabel(GuiGraphics graphics, int x, int y, int width, int height, String displayName) {
        if (displayName == null || displayName.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int labelHeight = Math.min(14, mc.font.lineHeight + 4);
        int labelY = y + height - labelHeight;
        graphics.fill(x, labelY, x + width, y + height, 0x99000000);
        int textWidth = mc.font.width(displayName);
        int textX = x + (width - textWidth) / 2;
        int textY = labelY + (labelHeight - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, displayName, textX, textY, 0xFFFFFFFF, false);
    }

    private static void drawVignette(GuiGraphics graphics, int x, int y, int width, int height) {
        int cornerW = Math.max(8, width / 5);
        int cornerH = Math.max(8, height / 5);
        int alpha = 0x55000000;
        graphics.fill(x, y, x + cornerW, y + cornerH, alpha);
        graphics.fill(x + width - cornerW, y, x + width, y + cornerH, alpha);
        graphics.fill(x, y + height - cornerH, x + cornerW, y + height, alpha);
        graphics.fill(x + width - cornerW, y + height - cornerH, x + width, y + height, alpha);
    }

    public static int[] getGradientColors(String mapName, String gameType) {
        int hash = (mapName == null ? 0 : mapName.hashCode()) ^ (gameType == null ? 0 : gameType.hashCode());
        int index = Math.floorMod(hash, GRADIENT_PAIRS.length);
        return GRADIENT_PAIRS[index];
    }

    private static String modeLabel(String gameType) {
        if (gameType == null) return "";
        return switch (gameType) {
            case "cs" -> "CS";
            case "csdm" -> "DM";
            default -> gameType.length() >= 2 ? gameType.substring(0, 2).toUpperCase() : gameType.toUpperCase();
        };
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}