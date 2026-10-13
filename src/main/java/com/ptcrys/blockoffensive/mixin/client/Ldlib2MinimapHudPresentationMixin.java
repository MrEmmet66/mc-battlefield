package com.ptcrys.blockoffensive.mixin.client;

import com.phasetranscrystal.fpsmatch.common.client.minimap.render.MinimapClientSettings;
import com.phasetranscrystal.fpsmatch.common.client.minimap.render.MinimapFrame;
import com.phasetranscrystal.fpsmatch.common.client.minimap.render.MinimapViewerPose;
import com.phasetranscrystal.fpsmatch.common.client.minimap.tactical.TacticalMapPresentation;
import com.phasetranscrystal.fpsmatch.common.client.minimap.tactical.TacticalMapState;
import com.phasetranscrystal.fpsmatch.common.client.minimap.ui.ldlib2.Ldlib2MinimapHudPresentation;
import com.phasetranscrystal.fpsmatch.core.minimap.hud.HudPlacement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = Ldlib2MinimapHudPresentation.class, remap = false)
public abstract class Ldlib2MinimapHudPresentationMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight, HudPlacement placement, MinimapViewerPose viewerPose, MinimapClientSettings settings, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "prepareFrame", at = @At("HEAD"), cancellable = true)
    private void onPrepareFrame(MinimapViewerPose viewerPose, MinimapClientSettings settings, int width, int height, CallbackInfoReturnable<Optional<MinimapFrame>> cir) {
        cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "prepareTactical", at = @At("HEAD"), cancellable = true)
    private void onPrepareTactical(MinimapViewerPose viewerPose, MinimapClientSettings settings, TacticalMapState tacticalMapState, CallbackInfoReturnable<Optional<TacticalMapPresentation>> cir) {
        cir.setReturnValue(Optional.empty());
    }
}
