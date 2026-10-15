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
 * Migrates CS shop data from save version 6 to version 7.
 * <ul>
 *   <li>EXPLOSIVES: replaces uncustomized arrays with the current defaults, which now
 *       append the RPG-7 launcher, its two rocket types, and the TM-62 / PTKM-1R
 *       anti-tank mines after the existing grenade/C4 entries.</li>
 *   <li>VEHICLE_AMMO: replaces uncustomized arrays with the current defaults, which now
 *       append the medium anti-ground missile after the existing shells.</li>
 * </ul>
 * Arrays that were customized are only padded/truncated to the current slot count.
 */
public class CSShopContentV7Fixer implements DataFixer.JsonFixer {

    private static final ItemType[] MIGRATED_TYPES = {
            ItemType.EXPLOSIVES,
            ItemType.VEHICLE_AMMO
    };

    private static final Set<String> PREVIOUS_EXPLOSIVES_IDS = Set.of(
            "superbwarfare:hand_grenade",
            "superbwarfare:rgo_grenade",
            "superbwarfare:m18_smoke_grenade",
            "superbwarfare:claymore_mine",
            "superbwarfare:c4_bomb",
            "superbwarfare:detonator",
            "csgrenades:flashbang",
            "csgrenades:molotov",
            "csgrenades:incendiary",
            "fpsmatch:flash_bomb",
            "fpsmatch:t_incendiary_grenade",
            "fpsmatch:ct_incendiary_grenade",
            "minecraft:air"
    );

    private static final Set<String> PREVIOUS_VEHICLE_AMMO_IDS = Set.of(
            "superbwarfare:heavy_ammo",
            "superbwarfare:rifle_ammo",
            "superbwarfare:small_shell_gs",
            "superbwarfare:small_shell_he",
            "superbwarfare:small_shell_ap",
            "superbwarfare:large_shell_gs",
            "superbwarfare:large_shell_he",
            "superbwarfare:large_shell_ap",
            "minecraft:air"
    );

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Content v7 shop fixer: expected a JSON object");
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
        if (matchesPreviousDefaults(type, existing)) {
            shopData.add(key, serializeDefaults(type));
        } else if (existing.size() != target) {
            padTruncate(existing, target, type);
        }
    }

    private static boolean matchesPreviousDefaults(ItemType type, JsonArray array) {
        Set<String> previousIds = type == ItemType.VEHICLE_AMMO
                ? PREVIOUS_VEHICLE_AMMO_IDS
                : PREVIOUS_EXPLOSIVES_IDS;
        if (array.isEmpty()) {
            return true;
        }
        for (JsonElement element : array) {
            String id = getSlotId(element);
            if (id == null || !previousIds.contains(id)) {
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