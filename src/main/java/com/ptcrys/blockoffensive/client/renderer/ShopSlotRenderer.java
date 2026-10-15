package com.ptcrys.blockoffensive.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.ptcrys.blockoffensive.client.data.CSClientData;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import com.phasetranscrystal.fpsmatch.compat.gun.GunTabTypeEnum;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Pure render utility for drawing shop item / TACZ gun icons into a vanilla {@link GuiGraphics}.
 *
 * <p>This class is deliberately free of ModernUI surface-view state. Icons are drawn from a
 * {@code ScreenEvent.Render.Post} overlay pass on the Minecraft render thread, using the same
 * well-tested {@link GuiGraphics} path that vanilla tooltips use. Every slot is rendered inside a
 * try/catch that logs at most once per second, so a bad item/gun can never crash the game.</p>
 */
public final class ShopSlotRenderer {
    public static final float GUN_ICON_WIDTH = 58.5F;
    public static final float GUN_ICON_HEIGHT = 19.5F;
    public static final float ITEM_ICON_SIZE = 16.0F;
    public static final float FILL_RATIO = 0.8f;
    public static final float ICON_SCALE = 20f;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile long lastErrorLogTime = 0L;

    private ShopSlotRenderer() {
    }

    /**
     * Draws one shop slot's icon centered inside the rectangle {@code (windowX, windowY, w, h)},
     * given in window (physical) pixels. {@code guiScale} is {@code Window#getGuiScale()}; it is
     * used to convert the window-space rectangle into GUI coordinates.
     *
     * <p>Safe to call from the render thread. Never throws: all failures are caught and logged
     * with a one-second throttle.</p>
     */
    public static void drawSlot(GuiGraphics gr, ItemType type, int index,
                                float windowX, float windowY, float w, float h, double guiScale) {
        if (type == null || index < 0 || guiScale <= 0) return;
        try {
            ClientShopSlot slot = FPSMClient.getGlobalData().getSlotData(type.name(), index);
            if (slot == null) return;
            ItemStack itemStack = slot.itemStack();
            if (itemStack.isEmpty()) return;
            boolean enable = CSClientData.getMoney() >= slot.cost() && !slot.isLocked();
            float gx = (float) (windowX / guiScale);
            float gy = (float) (windowY / guiScale);
            float gw = (float) (w / guiScale);
            float gh = (float) (h / guiScale);
            drawSlotGui(gr, itemStack, enable, gx, gy, gw, gh, ICON_SCALE);
        } catch (RuntimeException e) {
            logRenderError("slot render failed; skipping", e);
        }
    }

    private static void drawSlotGui(GuiGraphics gr, ItemStack itemStack, boolean enable,
                                    float x, float y, float w, float h, float iconScale) {
        gr.pose().pushPose();
        try {
            Optional<GunDisplayInstance> display = ModList.get().isLoaded("tacz")
                    ? TimelessAPI.getGunDisplay(itemStack)
                    : Optional.empty();
            if (display.isPresent()) {
                float offset = 0;
                if (GunCompatManager.isGun(itemStack)) {
                    Optional<GunTabTypeEnum> gunType = FPSMUtil.getGunTypeByGunId(
                            GunCompatManager.findProvider(itemStack).getGunId(itemStack));
                    if (gunType.isPresent() && gunType.get() == GunTabTypeEnum.PISTOL) {
                        offset = 10f;
                    }
                }
                renderIcon(gr, display.get(), enable, offset, x, y, w, h, iconScale);
            } else {
                renderItem(gr, itemStack, enable, x, y, w, h, iconScale);
            }
        } finally {
            gr.pose().popPose();
            gr.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableDepthTest();
        }
    }

    private static void renderItem(GuiGraphics gr, ItemStack itemStack, boolean enable,
                                   float x, float y, float w, float h, float iconScale) {
        setItemColor(gr, enable);
        float s = fitScale(iconScale, ITEM_ICON_SIZE, ITEM_ICON_SIZE, w, h);
        float iw = ITEM_ICON_SIZE * s;
        float ih = ITEM_ICON_SIZE * s;
        float ix = w > 0 ? x + (w - iw) / 2f : x;
        float iy = h > 0 ? y + (h - ih) / 2f : y;
        gr.pose().pushPose();
        try {
            gr.pose().translate(ix, iy, 0);
            gr.pose().scale(s, s, s);
            gr.renderItem(itemStack, 0, 0);
        } finally {
            gr.pose().popPose();
        }
    }

    private static void renderIcon(GuiGraphics gr, GunDisplayInstance display, boolean enable, float offset,
                                   float x, float y, float w, float h, float iconScale) {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        setIconColor(gr, enable);
        float s = fitScale(iconScale, GUN_ICON_WIDTH, GUN_ICON_HEIGHT, w, h);
        int iw = Math.max(1, (int) (GUN_ICON_WIDTH * s));
        int ih = Math.max(1, (int) (GUN_ICON_HEIGHT * s));
        int ix = w > 0 ? (int) (x + (w - iw) / 2f) : (int) x;
        int iy = h > 0 ? (int) (y + (h - ih) / 2f) : (int) y;
        gr.blit(display.getHUDTexture(), ix, iy, offset * s, 0.0F, iw, ih, iw, ih);
    }

    private static float fitScale(float desired, float baseW, float baseH, float surfaceW, float surfaceH) {
        if (surfaceW <= 0 || surfaceH <= 0) {
            return desired;
        }
        return Math.min(desired, Math.min(surfaceW * FILL_RATIO / baseW, surfaceH * FILL_RATIO / baseH));
    }

    private static void setIconColor(GuiGraphics gr, boolean enable) {
        if (enable) {
            if (FPSMClient.getGlobalData().isCurrentTeam("ct")) {
                gr.setColor((float) 150 / 255, (float) 200 / 255, (float) 250 / 255, 1);
            } else {
                gr.setColor((float) 234 / 255, (float) 192 / 255, (float) 85 / 255, 1);
            }
        } else {
            gr.setColor(125 / 255F, 125 / 255F, 125 / 255F, 1);
        }
    }

    private static void setItemColor(GuiGraphics gr, boolean enable) {
        if (!enable) {
            gr.setColor(125 / 255F, 125 / 255F, 125 / 255F, 1);
        } else {
            gr.setColor(1, 1, 1, 1);
        }
    }

    private static void logRenderError(String msg, RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime > 1000L) {
            lastErrorLogTime = now;
            LOGGER.warn("[BO-Shop] " + msg, e);
        }
    }
}