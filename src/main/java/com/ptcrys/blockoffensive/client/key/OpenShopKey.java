package com.ptcrys.blockoffensive.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.ptcrys.blockoffensive.client.data.CSClientData;
import com.ptcrys.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import icyllis.modernui.mc.forge.MuiForgeApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class OpenShopKey {
    public static final KeyMapping OPEN_SHOP_KEY = new KeyMapping("key.blockoffensive.open.shop.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.category.blockoffensive");

    @SubscribeEvent
    public static void onInspectPress(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && OPEN_SHOP_KEY.matches(event.getKey(), event.getScanCode())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }

            if (FPSMClient.getGlobalData().isCurrentMap("fpsm_none")) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("key.blockoffensive.open.shop.failed.no_map"));
                return;
            }

            // Время закупки бесконечно — единственная причина отказа: вне радиуса базы.
            if (!CSClientData.canOpenShop) {
                com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.info(
                        "[BO-Shop] open denied (client flag): player at ({}, {}, {})",
                        String.format(java.util.Locale.US, "%.1f", player.getX()),
                        String.format(java.util.Locale.US, "%.1f", player.getY()),
                        String.format(java.util.Locale.US, "%.1f", player.getZ()));
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("key.blockoffensive.open.shop.failed.left.purchase_area"));
                return;
            }

            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.info(
                    "[BO-Shop] open allowed (client flag): player at ({}, {}, {})",
                    String.format(java.util.Locale.US, "%.1f", player.getX()),
                    String.format(java.util.Locale.US, "%.1f", player.getY()),
                    String.format(java.util.Locale.US, "%.1f", player.getZ()));
            openShop();
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        if (Minecraft.getInstance().screen instanceof icyllis.modernui.mc.MuiScreen muiScreen && muiScreen.getFragment() instanceof CSGameShopScreen) {
            net.minecraft.world.item.ItemStack hovered = CSGameShopScreen.getHoveredItemStack();
            if (hovered != null && !hovered.isEmpty()) {
                event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, hovered, event.getMouseX(), event.getMouseY());
            }
        }
    }

    private static void openShop(){
        MuiForgeApi.openScreen(CSGameShopScreen.getInstance());
    }
}
