package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import net.minecraft.world.item.ItemStack;

/**
 * Migrates CS shop data from the Superb-Warfare-era defaults to CS-style content.
 * For each team, replaces the ARMOR / EXPLOSIVES / SUPPORT / VEHICLE / VEHICLE_AMMO
 * arrays that were never customized (all slots air) with the current defaults, and
 * pads/truncates customized arrays so the slot count always matches ItemType.slotCount().
 */
public class CSShopDefaultsFixer implements DataFixer.JsonFixer {

    private static final ItemType[] MIGRATED_TYPES = {
            ItemType.ARMOR,
            ItemType.EXPLOSIVES,
            ItemType.SUPPORT,
            ItemType.VEHICLE,
            ItemType.VEHICLE_AMMO
    };

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Shop defaults fixer: expected a JSON object");
        }
        JsonObject root = jsonElement.getAsJsonObject();
        if (!root.has("teams") || !root.get("teams").isJsonObject()) {
            return root;
        }
        JsonObject teams = root.getAsJsonObject("teams");
        for (String teamName : teams.keySet()) {
            migrateTeam(teams.get(teamName));
        }
        return root;
    }

    private static void migrateTeam(JsonElement teamElement) {
        if (teamElement == null || !teamElement.isJsonObject()) {
            return;
        }
        JsonObject team = teamElement.getAsJsonObject();
        JsonElement capabilitiesElement = team.get("capabilities");
        if (capabilitiesElement == null || !capabilitiesElement.isJsonObject()) {
            return;
        }
        JsonElement shopElement = capabilitiesElement.getAsJsonObject().get("ShopCapability");
        if (shopElement == null || !shopElement.isJsonObject()) {
            return;
        }
        JsonElement shopDataElement = shopElement.getAsJsonObject().get("shopData");
        if (shopDataElement == null || !shopDataElement.isJsonObject()) {
            return;
        }
        JsonObject shopData = shopDataElement.getAsJsonObject();
        for (ItemType type : MIGRATED_TYPES) {
            migrateType(shopData, type);
        }
    }

    private static void migrateType(JsonObject shopData, ItemType type) {
        String key = type.name();
        JsonElement existingElement = shopData.get(key);
        if (existingElement == null) {
            shopData.add(key, serializeDefaults(type));
            return;
        }
        if (!existingElement.isJsonArray()) {
            return;
        }
        JsonArray existing = existingElement.getAsJsonArray();
        int target = type.slotCount();
        if (isAllAir(existing)) {
            shopData.add(key, serializeDefaults(type));
        } else if (existing.size() != target) {
            padTruncate(existing, target);
        }
    }

    private static boolean isAllAir(JsonArray array) {
        for (JsonElement element : array) {
            if (!isAirSlot(element)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAirSlot(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return true;
        }
        JsonElement itemStackElement = element.getAsJsonObject().get("ItemStack");
        if (itemStackElement == null || !itemStackElement.isJsonObject()) {
            return true;
        }
        JsonElement idElement = itemStackElement.getAsJsonObject().get("id");
        if (idElement == null || !idElement.isJsonPrimitive()) {
            return true;
        }
        return "minecraft:air".equals(idElement.getAsString());
    }

    private static void padTruncate(JsonArray array, int target) {
        while (array.size() > target) {
            array.remove(array.size() - 1);
        }
        while (array.size() < target) {
            array.add(serializeSlot(new ShopSlot(ItemStack.EMPTY, 0)));
        }
    }

    private static JsonArray serializeDefaults(ItemType type) {
        JsonArray array = new JsonArray();
        for (ShopSlot slot : type.defaultSlots()) {
            array.add(serializeSlot(slot));
        }
        return array;
    }

    private static JsonElement serializeSlot(ShopSlot slot) {
        return ShopSlot.CODEC.encodeStart(JsonOps.INSTANCE, slot)
                .getOrThrow(false, error -> new JsonParseException("Failed to encode ShopSlot: " + error));
    }
}