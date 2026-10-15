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

import java.util.List;
import java.util.Set;

/**
 * Migrates the CS shop ARMOR category from the pre-Superb-Warfare-armor defaults
 * (save version 3, only the two FPSMatch kevlar slots) to the Superb-Warfare-era
 * defaults (save version 4, kevlar plus the Superb Warfare ballistic armor).
 * ARMOR arrays that were never customized (empty, all-air, or consisting solely
 * of the two FPSMatch kevlar ids) are replaced with the current defaults, which
 * preserves the fpsmatch listener modules through ShopSlot.CODEC. A customized
 * array is only padded/truncated to the current slot count.
 */
public class CSShopArmorFixer implements DataFixer.JsonFixer {

    private static final ItemType[] MIGRATED_TYPES = {
            ItemType.ARMOR
    };

    private static final Set<String> PREVIOUS_DEFAULT_IDS = Set.of(
            "fpsmatch:bulletproof_armor",
            "fpsmatch:bulletproof_with_helmet",
            "minecraft:air"
    );

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Armor shop fixer: expected a JSON object");
        }
        JsonObject root = jsonElement.getAsJsonObject();
        if (!root.has("teams") || !root.get("teams").isJsonObject()) {
            return root;
        }
        JsonObject teams = root.get("teams").getAsJsonObject();
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
        if (isEmptyOrAllPreviousDefaults(existing)) {
            shopData.add(key, serializeDefaults(type));
        } else if (existing.size() != target) {
            padTruncate(existing, target, type);
        }
    }

    private static boolean isEmptyOrAllPreviousDefaults(JsonArray array) {
        if (array.isEmpty()) {
            return true;
        }
        for (JsonElement element : array) {
            String id = getSlotId(element);
            if (id == null || !PREVIOUS_DEFAULT_IDS.contains(id)) {
                return false;
            }
        }
        return true;
    }

    private static String getSlotId(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonElement itemStackElement = element.getAsJsonObject().get("ItemStack");
        if (itemStackElement == null || !itemStackElement.isJsonObject()) {
            return null;
        }
        JsonElement idElement = itemStackElement.getAsJsonObject().get("id");
        if (idElement == null || !idElement.isJsonPrimitive()) {
            return null;
        }
        return idElement.getAsString();
    }

    private static void padTruncate(JsonArray array, int target, ItemType type) {
        while (array.size() > target) {
            array.remove(array.size() - 1);
        }
        List<ShopSlot> defaults = type.defaultSlots();
        while (array.size() < target) {
            int index = array.size();
            if (index < defaults.size()) {
                array.add(serializeSlot(defaults.get(index)));
            } else {
                array.add(serializeSlot(new ShopSlot(ItemStack.EMPTY, 0)));
            }
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