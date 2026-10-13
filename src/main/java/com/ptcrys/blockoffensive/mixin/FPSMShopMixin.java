package com.ptcrys.blockoffensive.mixin;

import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.shop.INamedType;
import com.phasetranscrystal.fpsmatch.core.shop.ShopAction;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = FPSMShop.class, remap = false)
public abstract class FPSMShopMixin {

    @Shadow
    @Final
    public Map<UUID, ShopData<?>> playersData;

    @Shadow
    public abstract void syncShopMoneyData();

    @Shadow
    public abstract ShopData<?> getPlayerShopData(UUID uuid);

    @Shadow
    public abstract void addMoney(UUID uuid, int money);

    /** Баланс покупателя до обработки кнопки (для проверки, что покупка реально прошла). */
    @Unique
    private final Map<UUID, Integer> blockoffensive$preBuyMoney = new HashMap<>();

    @Unique
    private void blockoffensive$syncSharedTeamMoney(int newMoney) {
        for (ShopData<?> data : this.playersData.values()) {
            data.setMoney(newMoney);
        }
        this.syncShopMoneyData();
    }

    @Inject(method = "addMoney(Ljava/util/UUID;I)V", at = @At("HEAD"), cancellable = true)
    private void onAddMoneyUUID(UUID uuid, int money, CallbackInfo ci) {
        // Деньги команды общие: берём баланс именно этого игрока (а не случайного),
        // прибавляем один раз и раздаём всем сокомандникам.
        int currentMoney = 0;
        ShopData<?> self = this.playersData.get(uuid);
        if (self != null) {
            currentMoney = self.getMoney();
        } else if (!this.playersData.isEmpty()) {
            currentMoney = this.playersData.values().iterator().next().getMoney();
        }
        blockoffensive$syncSharedTeamMoney(Math.max(0, currentMoney + money));
        ci.cancel();
    }

    @Inject(method = "addMoney(Lnet/minecraft/server/level/ServerPlayer;I)V", at = @At("HEAD"), cancellable = true)
    private void onAddMoneyPlayer(ServerPlayer player, int money, CallbackInfo ci) {
        this.addMoney(player.getUUID(), money);
        ci.cancel();
    }

    @Inject(method = "reduceMoney", at = @At("HEAD"), cancellable = true)
    private void onReduceMoneyPlayer(ServerPlayer player, int money, CallbackInfo ci) {
        // Списание тоже идёт из общего котла команды.
        int currentMoney = 0;
        ShopData<?> self = this.playersData.get(player.getUUID());
        if (self != null) {
            currentMoney = self.getMoney();
        } else if (!this.playersData.isEmpty()) {
            currentMoney = this.playersData.values().iterator().next().getMoney();
        }
        blockoffensive$syncSharedTeamMoney(Math.max(0, currentMoney - money));
        ci.cancel();
    }

    @Inject(method = "handleButton", at = @At("HEAD"), cancellable = true)
    private void onHandleButtonHead(ServerPlayer player, INamedType type, int slot, ShopAction action, CallbackInfo ci) {
        // Disallow refunds completely
        if (action == ShopAction.RETURN) {
            ci.cancel();
            return;
        }

        // Server-side validation: ensure player can use the shop
        com.phasetranscrystal.fpsmatch.core.FPSMCore core = com.phasetranscrystal.fpsmatch.core.FPSMCore.getInstance();
        com.phasetranscrystal.fpsmatch.core.map.BaseMap map = core.getMapByPlayer(player).orElse(null);
        if (map instanceof com.ptcrys.blockoffensive.map.CSMap csMap) {
            com.phasetranscrystal.fpsmatch.core.team.ServerTeam team = csMap.getMapTeams().getTeamByPlayer(player).orElse(null);
            if (team != null) {
                com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability cap =
                        team.getCapabilityMap().get(com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability.class).orElse(null);
                if (cap != null && !csMap.getPlayerCanOpenShop(cap, player)) {
                    com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.info("[BO-Shop] blocked purchase: player={} not in buy zone",
                            player.getScoreboardName());
                    ci.cancel();
                    return;
                }
            }
        }

        // Запоминаем баланс до покупки, чтобы в TAIL отличить успех от отказа.
        if (action == ShopAction.BUY) {
            ShopData<?> preData = this.getPlayerShopData(player.getUUID());
            if (preData != null) {
                blockoffensive$preBuyMoney.put(player.getUUID(), preData.getMoney());
            }
        }
    }

    @Inject(method = "handleButton", at = @At("TAIL"))
    private void onHandleButtonTail(ServerPlayer player, INamedType type, int slot, ShopAction action, CallbackInfo ci) {
        ShopData<?> buyerData = this.getPlayerShopData(player.getUUID());
        if (buyerData != null) {
            blockoffensive$syncSharedTeamMoney(buyerData.getMoney());
        }
        if (action == ShopAction.BUY) {
            Integer pre = blockoffensive$preBuyMoney.remove(player.getUUID());
            boolean success = buyerData != null && pre != null && buyerData.getMoney() < pre;
            if (success) {
                player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                blockoffensive$notifyTeamPurchase(player, type, slot);
            }
        }
    }

    /**
     * Уведомление команде в чат: игрок купил предмет (деньги-то общие).
     * Бесплатные (цена 0) не анонсируем, чтобы не спамить.
     */
    @Unique
    private void blockoffensive$notifyTeamPurchase(ServerPlayer player, INamedType type, int slot) {
        try {
            com.phasetranscrystal.fpsmatch.core.FPSMCore core = com.phasetranscrystal.fpsmatch.core.FPSMCore.getInstance();
            com.phasetranscrystal.fpsmatch.core.map.BaseMap map = core.getMapByPlayer(player).orElse(null);
            if (!(map instanceof com.ptcrys.blockoffensive.map.CSMap csMap)) return;
            com.phasetranscrystal.fpsmatch.core.team.ServerTeam team =
                    csMap.getMapTeams().getTeamByPlayer(player).orElse(null);
            if (team == null) return;
            ShopData<?> data = this.getPlayerShopData(player.getUUID());
            if (data == null) return;
            com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot bought = null;
            for (Map.Entry<?, ?> entry : data.getData().entrySet()) {
                if (entry.getKey() instanceof Enum<?> en && en.name().equals(type.name())) {
                    Object value = entry.getValue();
                    if (value instanceof List<?> slots && slot >= 0 && slot < slots.size()
                            && slots.get(slot) instanceof com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot s) {
                        bought = s;
                    }
                    break;
                }
            }
            if (bought == null || bought.getCost() <= 0) return;
            net.minecraft.world.item.ItemStack stack = bought.itemSupplier.get();
            if (stack == null || stack.isEmpty()) return;
            String name = stack.getHoverName().getString().replaceAll("§.", "");
            if (name.isBlank()) return;
            team.sendMessage(net.minecraft.network.chat.Component.translatable(
                    "blockoffensive.shop.team.purchase",
                    player.getDisplayName(), name, bought.getCost()));
        } catch (Exception e) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("[BO-Shop] purchase notify failed: {}", e.toString());
        }
    }

    @Inject(method = "getDefaultAndPutData(Ljava/util/UUID;Z)Lcom/phasetranscrystal/fpsmatch/core/shop/ShopData;", at = @At("RETURN"))
    private void onGetDefaultAndPutData(UUID uuid, boolean isReset, CallbackInfoReturnable<ShopData<?>> cir) {
        ShopData<?> created = cir.getReturnValue();
        if (created != null && !this.playersData.isEmpty()) {
            for (Map.Entry<UUID, ShopData<?>> entry : this.playersData.entrySet()) {
                if (!entry.getKey().equals(uuid)) {
                    created.setMoney(entry.getValue().getMoney());
                    break;
                }
            }
        }
    }
}
