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
 * Migrates CS shop data from save version 5 to version 6.
 * <ul>
 *   <li>SUPPORT: replaces uncustomized arrays with the current defaults, which now
 *       append the LR Tactical healing consumables after the Superb Warfare items.</li>
 *   <li>MELEE: adds the new MELEE category from the current defaults.</li>
 *   <li>VEHICLE: replaces uncustomized arrays with the current defaults so vehicle
 *       container stacks carry the corrected {@code BlockEntityTag}
 *       ({@code EntityType} plus the {@code id} key).</li>
 * </ul>
 * Arrays that were customized are only padded/truncated to the current slot count.
 */
public class CSShopContentV6Fixer implements DataFixer.JsonFixer {

    private static final ItemType[] MIGRATED_TYPES = {
            ItemType.SUPPORT,
            ItemType.MELEE,
            ItemType.VEHICLE
    };

    private static final Set<String> PREVIOUS_SUPPORT_IDS = Set.of(
            "blockoffensive:bomb_disposal_kit",
            "superbwarfare:mortar_deployer",
            "superbwarfare:mortar_shell",
            "superbwarfare:mortar_shell_smoke",
            "superbwarfare:drone",
            "superbwarfare:monitor",
            "superbwarfare:artillery_indicator",
            "superbwarfare:firing_parameters",
            "minecraft:air"
    );

    private static final Set<String> PREVIOUS_VEHICLE_ENTITY_TYPES = Set.of(
            "superbwarfare:sodayo_pick_up_hmg",
            "superbwarfare:tow",
            "superbwarfare:lav_150",
            "superbwarfare:lav_25",
            "superbwarfare:bmp_2",
            "superbwarfare:bradley",
            "superbwarfare:plz_05",
            "superbwarfare:t_90a",
            "superbwarfare:ztz_99a",
            "superbwarfare:m_1a_2"
    );

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Content v6 shop fixer: expected a JSON object");
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
        if (existingElement == null || !existingElement.isJsonArray()) {
            shopData.add(key, serializeDefaults(type));
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
        if (type == ItemType.VEHICLE) {
            return matchesPreviousVehicleDefaults(array);
        }
        if (type == ItemType.MELEE) {
            return isAllAir(array);
        }
        return isEmptyOrAllPreviousSupportIds(array);
    }

    private static boolean isEmptyOrAllPreviousSupportIds(JsonArray array) {
        if (array.isEmpty()) {
            return true;
        }
        for (JsonElement element : array) {
            String id = getSlotId(element);
            if (id == null || !PREVIOUS_SUPPORT_IDS.contains(id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesPreviousVehicleDefaults(JsonArray array) {
        for (JsonElement element : array) {
            String id = getSlotId(element);
            if (id == null || "minecraft:air".equals(id)) {
                continue;
            }
            if ("superbwarfare:crowbar".equals(id)) {
                continue;
            }
            if ("superbwarfare:container".equals(id)) {
                String entityType = getEntityType(element);
                if (entityType != null && PREVIOUS_VEHICLE_ENTITY_TYPES.contains(entityType)) {
                    continue;
                }
                return false;
            }
            return false;
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

    private static String getEntityType(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonElement itemStackElement = element.getAsJsonObject().get("ItemStack");
        if (itemStackElement == null || !itemStackElement.isJsonObject()) {
            return null;
        }
        JsonElement tagElement = itemStackElement.getAsJsonObject().get("tag");
        if (tagElement == null || !tagElement.isJsonObject()) {
            return null;
        }
        JsonElement blockEntityTagElement = tagElement.getAsJsonObject().get("BlockEntityTag");
        if (blockEntityTagElement == null || !blockEntityTagElement.isJsonObject()) {
            return null;
        }
        JsonElement entityTypeElement = blockEntityTagElement.getAsJsonObject().get("EntityType");
        if (entityTypeElement == null || !entityTypeElement.isJsonPrimitive()) {
            return null;
        }
        return entityTypeElement.getAsString();
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
}