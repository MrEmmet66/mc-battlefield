package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.MapThumbnailRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * LDLib2 map preview bridge. Delegates to {@link MapThumbnailRenderer} so that a configured icon or
 * background texture (map settings {@code iconTexture} / {@code backgroundTexture}, e.g.
 * {@code fpsmatch:textures/gui/maps/dust2_icon.png}) is drawn whenever its resource exists, and
 * otherwise a deterministic gradient + mode label preview is rendered. A 1px border is layered on
 * top to match the surrounding card chrome.
 */
final class Ldlib2MapThumbnailElement extends UIElement {
    private String texture;
    private String mapName;
    private String gameType;
    private String displayName;

    Ldlib2MapThumbnailElement(String id, String texture, String mapName, String gameType, String displayName) {
        setId(id);
        setAllowHitTest(false);
        this.texture = texture == null ? "" : texture;
        this.mapName = mapName;
        this.gameType = gameType;
        this.displayName = displayName;
    }

    public void setTexture(String texture) {
        this.texture = texture == null ? "" : texture;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (getSizeWidth() <= 0 || getSizeHeight() <= 0) return;
        MapThumbnailRenderer.render(
                context.graphics,
                Math.round(getPositionX()),
                Math.round(getPositionY()),
                Math.round(getSizeWidth()),
                Math.round(getSizeHeight()),
                texture,
                mapName,
                gameType,
                displayName,
                false
        );
    }

    @Override
    public void drawBackgroundOverlay(GUIContext context) {
        if (getSizeWidth() <= 1 || getSizeHeight() <= 1) return;
        GuiGraphics graphics = context.graphics;
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int w = Math.round(getSizeWidth());
        int h = Math.round(getSizeHeight());
        int border = FPSMLdlib2Theme.BORDER;
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);
    }
}