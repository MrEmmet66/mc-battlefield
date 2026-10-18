package com.ptcrys.blockoffensive.client.data;

import com.ptcrys.blockoffensive.BOConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Local preferences shared across maps; quantities and battery charge are not identities. */
public final class ShopFavorites {
    private ShopFavorites() {}

    public static boolean contains(ItemStack stack) {
        return !stack.isEmpty() && BOConfig.client.shopFavorites.get().contains(key(stack));
    }

    public static void toggle(ItemStack stack) {
        if (stack.isEmpty()) return;
        List<String> favorites = new ArrayList<>(BOConfig.client.shopFavorites.get());
        String key = key(stack);
        if (!favorites.remove(key)) favorites.add(key);
        BOConfig.client.shopFavorites.set(favorites);
        BOConfig.clientSpec.save();
    }

    private static String key(ItemStack stack) {
        CompoundTag tag = stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        tag.remove("Energy");
        return ForgeRegistries.ITEMS.getKey(stack.getItem()) + "|" + canonical(tag);
    }

    private static String canonical(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            StringBuilder key = new StringBuilder("{");
            compound.getAllKeys().stream().sorted().forEach(name -> key.append(name.length()).append(':')
                    .append(name).append('=').append(canonical(compound.get(name))).append(';'));
            return key.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder key = new StringBuilder("[");
            for (Tag element : list) key.append(canonical(element)).append(';');
            return key.append(']').toString();
        }
        return tag.toString();
    }
}
