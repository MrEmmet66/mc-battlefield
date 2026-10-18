package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CSShopContentV8RulesTest {
    private static JsonObject slot(String id, int cost) {
        JsonObject slot = new JsonObject();
        JsonObject item = new JsonObject();
        item.addProperty("id", id);
        item.addProperty("Count", 1);
        slot.add("ItemStack", item);
        slot.addProperty("defaultCost", cost);
        return slot;
    }

    @Test
    void removesRetiredItemsAcrossCategoriesWithoutLosingCustomItems() {
        JsonObject shop = new JsonObject();
        JsonArray armor = new JsonArray();
        armor.add(slot("fpsmatch:bulletproof_armor", 650));
        armor.add(slot("superbwarfare:us_helmet_pasgt", 350));
        armor.add(slot("fpsmatch:bulletproof_with_helmet", 1000));
        JsonArray support = new JsonArray();
        support.add(slot("blockoffensive:bomb_disposal_kit", 400));
        JsonObject custom = slot("minecraft:diamond", 1234);
        support.add(custom);
        shop.add("ARMOR", armor);
        shop.add("SUPPORT", support);
        CSShopContentV8Rules.migrateExistingEntries(shop);
        assertEquals(1, armor.size());
        assertEquals(1, support.size());
        assertSame(custom, support.get(0));
    }

    @Test
    void updatesStockPricesButPreservesCustomPricesAndIsIdempotent() {
        JsonObject shop = new JsonObject();
        JsonArray explosives = new JsonArray();
        JsonObject stock = slot("superbwarfare:rpg", 3000);
        JsonObject custom = slot("superbwarfare:rpg", 1234);
        explosives.add(stock);
        explosives.add(custom);
        shop.add("EXPLOSIVES", explosives);
        CSShopContentV8Rules.migrateExistingEntries(shop);
        assertEquals(3200, stock.get("defaultCost").getAsInt());
        assertEquals(1234, custom.get("defaultCost").getAsInt());
        JsonObject migrated = shop.deepCopy();
        CSShopContentV8Rules.migrateExistingEntries(shop);
        assertEquals(migrated, shop);
    }

    @Test
    void vehiclePricingUsesEntityTypeAndShellPacksHaveConsistentQuantities() {
        JsonObject shop = new JsonObject();
        JsonArray vehicles = new JsonArray();
        JsonObject vehicle = slot("superbwarfare:container", 8000);
        JsonObject tag = new JsonObject();
        JsonObject container = new JsonObject();
        container.addProperty("EntityType", "superbwarfare:t_90a");
        tag.add("BlockEntityTag", container);
        vehicle.getAsJsonObject("ItemStack").add("tag", tag);
        vehicles.add(vehicle);
        shop.add("VEHICLE", vehicles);
        JsonArray ammo = new JsonArray();
        JsonObject shell = slot("superbwarfare:small_shell_ap", 400);
        shell.getAsJsonObject("ItemStack").addProperty("Count", 30);
        ammo.add(shell);
        shop.add("VEHICLE_AMMO", ammo);
        CSShopContentV8Rules.migrateExistingEntries(shop);
        assertEquals(9000, vehicle.get("defaultCost").getAsInt());
        assertEquals(20, shell.getAsJsonObject("ItemStack").get("Count").getAsInt());
    }
}
