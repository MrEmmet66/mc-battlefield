package com.ptcrys.blockoffensive.mixin;

import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShopSlot.class, remap = false)
public abstract class ShopSlotMixin {

    @Shadow private int cost;

    @Inject(method = "canBuy", at = @At("HEAD"), cancellable = true)
    private void blockoffensive$onCanBuy(int money, CallbackInfoReturnable<Boolean> cir) {
        // Allow buying repeatedly as long as the player has enough money
        cir.setReturnValue(money >= this.cost);
    }

    @Inject(method = "canReturn", at = @At("HEAD"), cancellable = true)
    private void blockoffensive$onCanReturn(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Disallow refunds completely
        cir.setReturnValue(false);
    }

    @Inject(method = "getMaxBuyCount", at = @At("HEAD"), cancellable = true)
    private void blockoffensive$onGetMaxBuyCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Integer.MAX_VALUE);
    }
}
