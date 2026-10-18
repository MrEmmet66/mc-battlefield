package com.ptcrys.blockoffensive.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

/** Charges purchased items through Forge Energy without linking the optional SW mod. */
public final class SuperbWarfareShopItems {
    private SuperbWarfareShopItems() {}

    public static ItemStack prepare(ItemStack source, ServerPlayer player) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(source.getItem());
        if (id == null || !id.getNamespace().equals("superbwarfare")) return source;
        ItemStack stack = source.copy();
        if (id.getPath().equals("repair_tool")) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy ->
                    stack.getOrCreateTag().putInt("Energy", energy.getMaxEnergyStored()));
            // Recreate the capability after writing NBT; ItemEnergyStorage caches its energy.
            return stack.copy();
        }
        if (!id.getPath().equals("container") || stack.getTag() == null) return stack;
        CompoundTag container = stack.getTag().getCompound("BlockEntityTag");
        ResourceLocation vehicleId = ResourceLocation.tryParse(container.getString("EntityType"));
        if (vehicleId == null || !vehicleId.getNamespace().equals("superbwarfare")) return stack;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(vehicleId);
        if (type == null) return stack;
        try {
            Entity vehicle = type.create(player.serverLevel());
            if (vehicle == null) return stack;
            if (container.contains("Entity", Tag.TAG_COMPOUND)) {
                CompoundTag saved = container.getCompound("Entity").copy();
                saved.remove("UUID");
                vehicle.load(saved);
            }
            vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy ->
                    energy.receiveEnergy(energy.getMaxEnergyStored(), false));
            CompoundTag saved = vehicle.saveWithoutId(new CompoundTag());
            saved.remove("UUID");
            container.put("Entity", saved);
            stack.getOrCreateTag().put("BlockEntityTag", container);
            vehicle.invalidateCaps();
        } catch (RuntimeException e) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Unable to charge shop vehicle {}", vehicleId, e);
        }
        return stack;
    }
}
