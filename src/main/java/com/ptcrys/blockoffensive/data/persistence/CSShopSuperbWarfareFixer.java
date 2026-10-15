package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.phasetranscrystal.fpsmatch.core.capability.CapabilityMap;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migrates CS shop data from the pre-Superb-Warfare defaults (save version 2)
 * to the Superb-Warfare-era defaults (save version 3). Categories that were
 * never customized (empty, all-air, or consisting solely of the version-2
 * default item ids) are replaced with the current defaults; anything else is
 * only padded/truncated to the current slot count.
 */
public class CSShopSuperbWarfareFixer implements DataFixer.JsonFixer {

    private static final ItemType[] MIGRATED_TYPES = {
            ItemType.EXPLOSIVES,
            ItemType.SUPPORT,
            ItemType.VEHICLE,
            ItemType.VEHICLE_AMMO
    };

    private static final Set<String> PREVIOUS_DEFAULT_IDS = Set.of(
            "csgrenades:hegrenade", "csgrenades:flashbang", "csgrenades:smokegrenade",
            "csgrenades:molotov", "csgrenades:incendiary", "csgrenades:decoy",
            "fpsmatch:grenade", "fpsmatch:flash_bomb", "fpsmatch:smoke_shell",
            "fpsmatch:t_incendiary_grenade", "fpsmatch:ct_incendiary_grenade",
            "blockoffensive:bomb_disposal_kit",
            "minecraft:air"
    );

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Superb Warfare shop fixer: expected a JSON object");
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

    /**
     * Safety net applied while decoding a save-version-3 map. Categories that
     * were saved empty or all-air (for example when Superb Warfare was not
     * installed when the version-2 to version-3 migration first ran, leaving
     * VEHICLE / VEHICLE_AMMO collapsed to zero slots) are repopulated from the
     * current defaults instead of being padded with air, so they do not stay
     * empty forever once Superb Warfare is installed. Customized categories are
     * only padded/truncated to the current slot count. Call before
     * {@code writeData} in the map decode constructor.
     */
    public static void normalizeShopDataSlotCounts(Map<String, CapabilityMap.Wrapper> teams) {
        if (teams == null) {
            return;
        }
        for (CapabilityMap.Wrapper wrapper : teams.values()) {
            if (wrapper == null) {
                continue;
            }
            JsonElement shopElement = wrapper.data().get("ShopCapability");
            if (shopElement == null || !shopElement.isJsonObject()) {
                continue;
            }
            JsonElement shopDataElement = shopElement.getAsJsonObject().get("shopData");
            if (shopDataElement == null || !shopDataElement.isJsonObject()) {
                continue;
            }
            JsonObject shopData = shopDataElement.getAsJsonObject();
            for (ItemType type : ItemType.values()) {
                JsonElement element = shopData.get(type.name());
                if (element == null) {
                    shopData.add(type.name(), serializeDefaults(type));
                    continue;
                }
                if (!element.isJsonArray()) {
                    continue;
                }
                JsonArray array = element.getAsJsonArray();
                int target = type.slotCount();
                if (isAllAir(array)) {
                    shopData.add(type.name(), serializeDefaults(type));
                } else if (array.size() != target) {
                    padTruncate(array, target, type);
                }
            }
        }
    }
}
