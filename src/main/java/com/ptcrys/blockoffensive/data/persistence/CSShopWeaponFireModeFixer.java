package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.ptcrys.blockoffensive.util.GunStacks;

/**
 * Migrates CS shop weapon slots (save version 4) so every saved TACZ gun carries a
 * usable {@code GunFireMode} NBT tag.
 * <p>
 * Guns built with {@code GunItemBuilder} default to {@code GunFireMode: "UNKNOWN"},
 * and TACZ refuses to fire them until the player cycles fire modes. This fixer walks
 * each team's {@code ShopCapability.shopData} (every category, especially WEAPON) and
 * the {@code StartKitsCapability} item list, and for each {@code tacz:modern_kinetic_gun}
 * stack whose {@code tag.GunFireMode} is missing or "UNKNOWN" it writes the first fire
 * mode supported by {@code tag.GunId} (via TimelessAPI when the gun index is available,
 * otherwise "SEMI"). Slot prices and other NBT are left untouched.
 */
public class CSShopWeaponFireModeFixer implements DataFixer.JsonFixer {

    private static final String GUN_ITEM_ID = "tacz:modern_kinetic_gun";
    private static final String UNKNOWN = "UNKNOWN";

    @Override
    public JsonElement apply(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("Weapon fire mode fixer: expected a JSON object");
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
        JsonObject capabilities = capabilitiesElement.getAsJsonObject();

        // 1. Shop gun slots: ShopCapability -> shopData -> <category> -> [slot].ItemStack
        JsonElement shopElement = capabilities.get("ShopCapability");
        if (shopElement != null && shopElement.isJsonObject()) {
            JsonElement shopDataElement = shopElement.getAsJsonObject().get("shopData");
            if (shopDataElement != null && shopDataElement.isJsonObject()) {
                JsonObject shopData = shopDataElement.getAsJsonObject();
                for (String category : shopData.keySet()) {
                    JsonElement categoryElement = shopData.get(category);
                    if (categoryElement == null || !categoryElement.isJsonArray()) {
                        continue;
                    }
                    for (JsonElement slot : categoryElement.getAsJsonArray()) {
                        if (slot == null || !slot.isJsonObject()) {
                            continue;
                        }
                        fixItemStack(slot.getAsJsonObject().get("ItemStack"));
                    }
                }
            }
        }

        // 2. Start kits: StartKitsCapability -> [ItemStack, ...] (also covers any future
        //    list-of-ItemStack capability that might hold serialized guns).
        JsonElement kitsElement = capabilities.get("StartKitsCapability");
        if (kitsElement != null && kitsElement.isJsonArray()) {
            for (JsonElement itemStack : kitsElement.getAsJsonArray()) {
                fixItemStack(itemStack);
            }
        }
    }

    private static void fixItemStack(JsonElement itemStackElement) {
        if (itemStackElement == null || !itemStackElement.isJsonObject()) {
            return;
        }
        JsonObject itemStack = itemStackElement.getAsJsonObject();
        JsonElement idElement = itemStack.get("id");
        if (idElement == null || !idElement.isJsonPrimitive() || !GUN_ITEM_ID.equals(idElement.getAsString())) {
            return;
        }
        JsonElement tagElement = itemStack.get("tag");
        if (tagElement == null || !tagElement.isJsonObject()) {
            return;
        }
        JsonObject tag = tagElement.getAsJsonObject();
        JsonElement gunIdElement = tag.get("GunId");
        if (gunIdElement == null || !gunIdElement.isJsonPrimitive()) {
            return;
        }
        String gunId = gunIdElement.getAsString();

        JsonElement fireModeElement = tag.get("GunFireMode");
        if (fireModeElement != null && fireModeElement.isJsonPrimitive()
                && !UNKNOWN.equals(fireModeElement.getAsString())) {
            return;
        }

        tag.addProperty("GunFireMode", GunStacks.defaultFireModeName(gunId));
    }
}
