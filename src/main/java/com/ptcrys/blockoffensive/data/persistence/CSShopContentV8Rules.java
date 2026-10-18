package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

/** Registry-independent rules for upgrading saved shop entries. */
public final class CSShopContentV8Rules {
    private CSShopContentV8Rules() {}

    // Only replace the previous stock price. Administrator overrides are preserved.
    private static final Map<String, PriceChange> PRICES = Map.ofEntries(
            price("rpg", 3000, 3200), price("rpg_rocket_standard", 400, 450),
            price("rpg_rocket_tbg", 550, 650), price("tm_62", 700, 800),
            price("ptkm_1r", 1200, 1400), price("mortar_deployer", 2500, 2800),
            price("drone", 1800, 2200), price("crowbar", 50, 100),
            price("sodayo_pick_up_hmg", 1500, 1800), price("tow", 2200, 2400),
            price("lav_150", 3500, 4000), price("lav_25", 4800, 5200),
            price("bmp_2", 5500, 6000), price("bradley", 6000, 6500),
            price("plz_05", 6500, 7500), price("t_90a", 8000, 9000),
            price("ztz_99a", 8500, 9500), price("m_1a_2", 9500, 10500));

    private record PriceChange(int previous, int current) {}

    private static Map.Entry<String, PriceChange> price(String id, int previous, int current) {
        return Map.entry("superbwarfare:" + id, new PriceChange(previous, current));
    }

    static void migrateExistingEntries(JsonObject shop) {
        removeRetiredItems(shop);
        for (JsonElement category : shop.asMap().values()) {
            if (!category.isJsonArray()) continue;
            for (JsonElement entry : category.getAsJsonArray()) {
                if (!entry.isJsonObject()) continue;
                JsonObject slot = entry.getAsJsonObject();
                String id = itemId(entry);
                JsonObject stack = object(slot, "ItemStack");
                String priceId = id;
                if (id.equals("superbwarfare:container")) {
                    priceId = object(object(stack, "tag"), "BlockEntityTag").has("EntityType")
                            ? object(object(stack, "tag"), "BlockEntityTag").get("EntityType").getAsString() : "";
                }
                PriceChange price = PRICES.get(priceId);
                if (price != null && slot.has("defaultCost") && slot.get("defaultCost").getAsInt() == price.previous) {
                    slot.addProperty("defaultCost", price.current);
                }
                if ((id.equals("superbwarfare:small_shell_he") || id.equals("superbwarfare:small_shell_ap"))
                        && slot.has("defaultCost")
                        && slot.get("defaultCost").getAsInt() == (id.endsWith("_he") ? 350 : 400)
                        && stack.has("Count") && stack.get("Count").getAsInt() == 30) {
                    stack.addProperty("Count", 20);
                }
            }
        }
    }

    public static void removeRetiredItems(JsonObject shop) {
        for (JsonElement category : shop.asMap().values()) {
            if (!category.isJsonArray()) continue;
            JsonArray slots = category.getAsJsonArray();
            for (int i = slots.size() - 1; i >= 0; i--) {
                String id = itemId(slots.get(i));
                if (id.startsWith("fpsmatch:bulletproof") || id.equals("blockoffensive:bomb_disposal_kit")
                        || id.equals("fpsmatch:defusal_kit")) {
                    slots.remove(i);
                }
            }
        }
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
