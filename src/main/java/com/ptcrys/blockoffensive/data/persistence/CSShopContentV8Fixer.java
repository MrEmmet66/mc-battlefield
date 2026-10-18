package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import com.ptcrys.blockoffensive.map.shop.ItemType;

/** Removes retired equipment and updates stock prices without resetting custom shops. */
public final class CSShopContentV8Fixer implements DataFixer.JsonFixer {
    @Override
    public JsonElement apply(JsonElement json) {
        JsonObject root = json.getAsJsonObject();
        JsonObject teams = object(root, "teams");
        for (JsonElement team : teams.asMap().values()) {
            if (!team.isJsonObject()) continue;
            JsonObject capabilities = object(team.getAsJsonObject(), "capabilities");
            JsonObject shop = object(object(capabilities, "ShopCapability"), "shopData");
            CSShopContentV8Rules.migrateExistingEntries(shop);
            // Append once, after existing support slots, so custom entries keep their order.
            if (shop.get("SUPPORT") instanceof JsonArray support && !contains(support, "superbwarfare:repair_tool")) {
                for (ShopSlot slot : ItemType.SUPPORT.defaultSlots()) {
                    JsonElement encoded = ShopSlot.CODEC.encodeStart(JsonOps.INSTANCE, slot).result().orElse(null);
                    if (encoded != null && "superbwarfare:repair_tool".equals(itemId(encoded))) {
                        support.add(encoded);
                        break;
                    }
                }
            }
        }
        return root;
    }

    private static boolean contains(JsonArray slots, String id) {
        for (JsonElement slot : slots) if (id.equals(itemId(slot))) return true;
        return false;
    }

    private static String itemId(JsonElement entry) {
        if (!entry.isJsonObject()) return "";
        JsonObject stack = object(entry.getAsJsonObject(), "ItemStack");
        return stack.has("id") ? stack.get("id").getAsString() : "";
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.get(key) instanceof JsonObject value ? value : new JsonObject();
    }
}
