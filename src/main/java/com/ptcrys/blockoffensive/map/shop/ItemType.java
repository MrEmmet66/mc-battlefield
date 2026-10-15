package com.ptcrys.blockoffensive.map.shop;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.shop.INamedType;
import com.phasetranscrystal.fpsmatch.core.shop.functional.ListenerModule;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import com.ptcrys.blockoffensive.BlockOffensive;
import com.ptcrys.blockoffensive.util.GunStacks;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public enum ItemType implements INamedType {
    WEAPON(0) {
        @Override
        public int slotCount() {
            return getWeaponTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getWeaponTemplates());
        }
    },
    AMMO(1) {
        @Override
        public int slotCount() {
            return getAmmoTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getAmmoTemplates());
        }
    },
    ATTACHMENT(4) {
        @Override
        public int slotCount() {
            return getAttachmentTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getAttachmentTemplates());
        }
    },
    ARMOR(2) {
        @Override
        public int slotCount() {
            return getArmorTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getArmorTemplates());
        }
    },
    EXPLOSIVES(5) {
        @Override
        public int slotCount() {
            return getExplosivesTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getExplosivesTemplates());
        }
    },
    SUPPORT(6) {
        @Override
        public int slotCount() {
            return getSupportTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getSupportTemplates());
        }
    },
    VEHICLE(3) {
        @Override
        public int slotCount() {
            return getVehicleTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getVehicleTemplates());
        }
    },
    VEHICLE_AMMO(7) {
        @Override
        public int slotCount() {
            return getVehicleAmmoTemplates().size();
        }

        @Override
        public ArrayList<ShopSlot> defaultSlots() {
            return copySlots(getVehicleAmmoTemplates());
        }
    };

    public final int typeIndex;
    public final boolean dropUnlock;

    private static List<ShopSlot> weaponTemplates;
    private static List<ShopSlot> ammoTemplates;
    private static List<ShopSlot> armorTemplates;
    private static List<ShopSlot> vehicleTemplates;
    private static List<ShopSlot> attachmentTemplates;
    private static List<ShopSlot> explosivesTemplates;
    private static List<ShopSlot> supportTemplates;
    private static List<ShopSlot> vehicleAmmoTemplates;

    ItemType(int typeIndex) {
        this(typeIndex, true);
    }

    ItemType(int typeIndex, boolean dropUnlock) {
        this.typeIndex = typeIndex;
        this.dropUnlock = dropUnlock;
    }

    @Override
    public boolean dorpUnlock() {
        return dropUnlock;
    }

    private static ArrayList<ShopSlot> copySlots(List<ShopSlot> source) {
        ArrayList<ShopSlot> list = new ArrayList<>(source.size());
        for (ShopSlot slot : source) {
            ShopSlot copied = slot.copy();
            copied.setMaxBuyCount(Integer.MAX_VALUE);
            list.add(copied);
        }
        return list;
    }

    private static boolean swLoaded() {
        return ModList.get().isLoaded("superbwarfare");
    }

    private static synchronized List<ShopSlot> getWeaponTemplates() {
        if (weaponTemplates == null) {
            weaponTemplates = new ArrayList<>();
            if (ModList.get().isLoaded("tacz")) {
                // РџРёСЃС‚РѕР»РµС‚С‹: РґРµС€С‘РІС‹Р№ РІС‚РѕСЂРѕР№ СЃС‚РІРѕР», Glock Р±РµСЃРїР»Р°С‚РµРЅ РґР»СЏ СЃС‚Р°СЂС‚Р°
                addGun(weaponTemplates, "glock_17", 0);
                addGun(weaponTemplates, "m1911", 350);
                addGun(weaponTemplates, "cz75", 500);
                addGun(weaponTemplates, "b93r", 600);
                addGun(weaponTemplates, "deagle", 700);
                // ПП: дешёвый мид-гейм, 1-2 килла на ствол
                addGun(weaponTemplates, "uzi", 500);
                addGun(weaponTemplates, "hk_mp5a5", 700);
                addGun(weaponTemplates, "ump45", 800);
                addGun(weaponTemplates, "p90", 900);
                addGun(weaponTemplates, "vector45", 1000);
                // Дробовики: высокий урон вблизи (награда 900)
                addGun(weaponTemplates, "db_short", 1100);
                addGun(weaponTemplates, "m870", 1700);
                addGun(weaponTemplates, "m1014", 2200);
                addGun(weaponTemplates, "spas_12", 2500);
                addGun(weaponTemplates, "aa12", 2000);
                // Винтовки: ядро. Стартовые 3000 = 2 АК или АК + броник + патроны
                addGun(weaponTemplates, "ak47", 1500);
                addGun(weaponTemplates, "m16a4", 1200);
                addGun(weaponTemplates, "m4a1", 1600);
                addGun(weaponTemplates, "aug", 1300);
                addGun(weaponTemplates, "hk416d", 1600);
                addGun(weaponTemplates, "scar_l", 1700);
                addGun(weaponTemplates, "g36k", 1400);
                addGun(weaponTemplates, "fn_fal", 1900);
                // Снайперы: налог на кемп (награда всего 100), цены сдержанные
                addGun(weaponTemplates, "m700", 3200);
                addGun(weaponTemplates, "ai_awp", 4750);
                addGun(weaponTemplates, "m95", 5500);
                addGun(weaponTemplates, "m107", 6200);
                // РџСѓР»РµРјС‘С‚С‹: РґРѕСЂРѕРіРёРµ, РґР»СЏ СѓРґРµСЂР¶Р°РЅРёСЏ РїРѕР·РёС†РёР№ РІСЃРµР№ РєРѕРјР°РЅРґРѕР№
                addGun(weaponTemplates, "rpk", 4200);
                addGun(weaponTemplates, "m249", 5200);
                addGun(weaponTemplates, "fn_evolys", 5800);
            } else {
                for (int i = 0; i < 5; i++) {
                    weaponTemplates.add(new ShopSlot(ItemStack.EMPTY, 0));
                }
            }
        }
        return weaponTemplates;
    }

    private static synchronized List<ShopSlot> getAmmoTemplates() {
        if (ammoTemplates == null) {
            ammoTemplates = new ArrayList<>();
            if (ModList.get().isLoaded("tacz")) {
                // РљР°Р»РёР±СЂС‹ СЃС‚СЂРѕРіРѕ РїРѕРґ СЃС‚РІРѕР»С‹ РёР· РјР°РіР°Р·РёРЅР°; С†РµРЅР° РїР°С‡РєРё ~5-10% С†РµРЅС‹ СЃС‚РІРѕР»Р°
                addAmmo(ammoTemplates, "9mm", 30, 50);
                addAmmo(ammoTemplates, "45acp", 24, 60);
                addAmmo(ammoTemplates, "50ae", 14, 80);
                addAmmo(ammoTemplates, "57x28", 50, 100);
                addAmmo(ammoTemplates, "12g", 16, 100);
                addAmmo(ammoTemplates, "762x39", 30, 150);
                addAmmo(ammoTemplates, "556x45", 30, 150);
                addAmmo(ammoTemplates, "308", 20, 200);
                addAmmo(ammoTemplates, "30_06", 15, 200);
                addAmmo(ammoTemplates, "338", 10, 250);
                addAmmo(ammoTemplates, "50bmg", 10, 300);
            } else {
                for (int i = 0; i < 5; i++) {
                    ammoTemplates.add(new ShopSlot(ItemStack.EMPTY, 0));
                }
            }
        }
        return ammoTemplates;
    }

    private static synchronized List<ShopSlot> getArmorTemplates() {
        if (armorTemplates == null) {
            armorTemplates = new ArrayList<>();
            addArmorItem(armorTemplates, "bulletproof_armor", 650, "bulletproof_without_helmet");
            addArmorItem(armorTemplates, "bulletproof_with_helmet", 1000, "bulletproof_with_helmet");

            // Superb Warfare ballistic armor (vanilla HEAD/CHEST slots, adds the
            // superbwarfare:bullet_resistance attribute); no NBT or Curios needed.
            // Prices track BULLET_RESISTANCE (helmet 0.1/0.2, chest 0.5) and the
            // armor_plate consumable (+15 armor points to the worn chest armor).
            if (swLoaded()) {
                addItem(armorTemplates, "superbwarfare", "us_helmet_pasgt", 350);
                addItem(armorTemplates, "superbwarfare", "ru_helmet_6b47", 400);
                addItem(armorTemplates, "superbwarfare", "ge_helmet_m_35", 300);
                addItem(armorTemplates, "superbwarfare", "us_chest_iotv", 900);
                addItem(armorTemplates, "superbwarfare", "ru_chest_6b43", 1000);
                addItem(armorTemplates, "superbwarfare", "armor_plate", 300);
            }
        }
        return armorTemplates;
    }

    private static synchronized List<ShopSlot> getExplosivesTemplates() {
        if (explosivesTemplates == null) {
            explosivesTemplates = new ArrayList<>();
            if (swLoaded()) {
                addSWItem(explosivesTemplates, "hand_grenade", 1, 300);
                addSWItem(explosivesTemplates, "rgo_grenade", 1, 400);
                addSWItem(explosivesTemplates, "m18_smoke_grenade", 1, 300);
                addSWItem(explosivesTemplates, "claymore_mine", 1, 600);
                addC4Item(explosivesTemplates, false, 900);
                addC4Item(explosivesTemplates, true, 1100);
                addSWItem(explosivesTemplates, "detonator", 1, 150);
                addNonSWGrenades(explosivesTemplates);
            } else if (ModList.get().isLoaded("csgrenades")) {
                addItem(explosivesTemplates, "csgrenades", "hegrenade", 300);
                addItem(explosivesTemplates, "csgrenades", "flashbang", 200);
                addItem(explosivesTemplates, "csgrenades", "smokegrenade", 300);
                addItem(explosivesTemplates, "csgrenades", "molotov", 400);
                addItem(explosivesTemplates, "csgrenades", "incendiary", 500);
                addItem(explosivesTemplates, "csgrenades", "decoy", 50);
            } else {
                addItem(explosivesTemplates, "fpsmatch", "grenade", 300);
                addItem(explosivesTemplates, "fpsmatch", "flash_bomb", 200);
                addItem(explosivesTemplates, "fpsmatch", "smoke_shell", 300);
                addItem(explosivesTemplates, "fpsmatch", "t_incendiary_grenade", 400);
                addItem(explosivesTemplates, "fpsmatch", "ct_incendiary_grenade", 500);
            }
        }
        return explosivesTemplates;
    }

    private static synchronized List<ShopSlot> getSupportTemplates() {
        if (supportTemplates == null) {
            supportTemplates = new ArrayList<>();
            addItem(supportTemplates, "blockoffensive", "bomb_disposal_kit", 400);
            if (swLoaded()) {
                addSWItem(supportTemplates, "mortar_deployer", 1, 2500);
                addSWItem(supportTemplates, "mortar_shell", 1, 200);
                addSWItem(supportTemplates, "mortar_shell_smoke", 1, 150);
                addSWItem(supportTemplates, "drone", 1, 1800);
                addSWItem(supportTemplates, "monitor", 1, 350);
                addSWItem(supportTemplates, "artillery_indicator", 1, 600);
                addSWItem(supportTemplates, "firing_parameters", 1, 200);
            }
        }
        return supportTemplates;
    }

    private static synchronized List<ShopSlot> getVehicleTemplates() {
        if (vehicleTemplates == null) {
            vehicleTemplates = new ArrayList<>();
            if (swLoaded()) {
                addSWItem(vehicleTemplates, "crowbar", 1, 50);
                addVehicle(vehicleTemplates, "sodayo_pick_up_hmg", 1500);
                addVehicle(vehicleTemplates, "tow", 2200);
                addVehicle(vehicleTemplates, "lav_150", 3500);
                addVehicle(vehicleTemplates, "lav_25", 4800);
                addVehicle(vehicleTemplates, "bmp_2", 5500);
                addVehicle(vehicleTemplates, "bradley", 6000);
                addVehicle(vehicleTemplates, "plz_05", 6500);
                addVehicle(vehicleTemplates, "t_90a", 8000);
                addVehicle(vehicleTemplates, "ztz_99a", 8500);
                addVehicle(vehicleTemplates, "m_1a_2", 9500);
            }
        }
        return vehicleTemplates;
    }

    private static synchronized List<ShopSlot> getVehicleAmmoTemplates() {
        if (vehicleAmmoTemplates == null) {
            vehicleAmmoTemplates = new ArrayList<>();
            if (swLoaded()) {
                addSWItem(vehicleAmmoTemplates, "heavy_ammo", 64, 200);
                addSWItem(vehicleAmmoTemplates, "rifle_ammo", 64, 250);
                addSWItem(vehicleAmmoTemplates, "small_shell_gs", 20, 250);
                addSWItem(vehicleAmmoTemplates, "small_shell_he", 30, 350);
                addSWItem(vehicleAmmoTemplates, "small_shell_ap", 30, 400);
                addSWItem(vehicleAmmoTemplates, "large_shell_gs", 6, 350);
                addSWItem(vehicleAmmoTemplates, "large_shell_he", 6, 500);
                addSWItem(vehicleAmmoTemplates, "large_shell_ap", 6, 600);
            }
        }
        return vehicleAmmoTemplates;
    }

    private static synchronized List<ShopSlot> getAttachmentTemplates() {
        if (attachmentTemplates == null) {
            attachmentTemplates = new ArrayList<>();
            if (ModList.get().isLoaded("tacz")) {
                // РљРѕР»Р»РёРјР°С‚РѕСЂС‹
                addAttachment(attachmentTemplates, "sight_t1", 300);
                addAttachment(attachmentTemplates, "sight_rmr_dot", 400);
                addAttachment(attachmentTemplates, "sight_t2", 500);
                addAttachment(attachmentTemplates, "sight_552", 550);
                addAttachment(attachmentTemplates, "sight_uh1", 650);
                addAttachment(attachmentTemplates, "sight_exp3", 750);
                // РћРїС‚РёРєР°
                addAttachment(attachmentTemplates, "scope_retro_2x", 850);
                addAttachment(attachmentTemplates, "scope_acog_ta31", 900);
                addAttachment(attachmentTemplates, "scope_elcan_4x", 1000);
                addAttachment(attachmentTemplates, "scope_lpvo_1_6", 1100);
                addAttachment(attachmentTemplates, "scope_hamr", 1300);
                addAttachment(attachmentTemplates, "scope_vudu", 1500);
                // РћР±РІРµСЃ
                addAttachment(attachmentTemplates, "stock_moe", 400);
                addAttachment(attachmentTemplates, "stock_tactical_ar", 450);
                addAttachment(attachmentTemplates, "grip_vertical_ranger", 450);
                addAttachment(attachmentTemplates, "grip_td", 500);
                addAttachment(attachmentTemplates, "grip_cobra", 500);
                addAttachment(attachmentTemplates, "laser_peq15", 550);
                addAttachment(attachmentTemplates, "laser_lopro", 600);
                addAttachment(attachmentTemplates, "muzzle_compensator_trident", 700);
                addAttachment(attachmentTemplates, "muzzle_silencer_phantom_s1", 800);
                addAttachment(attachmentTemplates, "muzzle_silencer_mirage", 1000);
                addAttachment(attachmentTemplates, "muzzle_silencer_ursus", 1100);
            } else {
                for (int i = 0; i < 5; i++) {
                    attachmentTemplates.add(new ShopSlot(ItemStack.EMPTY, 0));
                }
            }
        }
        return attachmentTemplates;
    }

    private static void addAttachment(List<ShopSlot> list, String attachmentId, int cost) {
        try {
            ResourceLocation rl = new ResourceLocation("tacz", attachmentId);
            ItemStack stack = AttachmentItemBuilder.create()
                    .setId(rl)
                    .build();
            list.add(new ShopSlot(stack, cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create default TACZ attachment {}: {}", attachmentId, t.getMessage());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }

    private static void addGun(List<ShopSlot> list, String gunId, int cost) {
        try {
            ResourceLocation rl = new ResourceLocation("tacz", gunId);
            ItemStack stack = GunItemBuilder.create()
                    .setId(rl)
                    .setFireMode(GunStacks.defaultFireMode(rl))
                    .setAmmoInBarrel(true)
                    .forceBuild();
            list.add(new ShopSlot(stack, cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create default TACZ gun {}: {}", gunId, t.getMessage());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }

    private static void addAmmo(List<ShopSlot> list, String ammoId, int count, int cost) {
        try {
            ResourceLocation rl = new ResourceLocation("tacz", ammoId);
            ItemStack stack = AmmoItemBuilder.create()
                    .setId(rl)
                    .setCount(count)
                    .build();
            list.add(new ShopSlot(stack, cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create default TACZ ammo {}: {}", ammoId, t.getMessage());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }

    private static void addItem(List<ShopSlot> list, String modId, String itemId, int cost) {
        try {
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modId, itemId));
            if (item == null || item == Items.AIR) {
                FPSMatch.LOGGER.warn("Missing shop item {}:{}, skipping from template", modId, itemId);
                return;
            }
            list.add(new ShopSlot(new ItemStack(item), cost));
        } catch (Throwable t) {
            FPSMatch.LOGGER.warn("Failed to create shop item {}:{}: {}", modId, itemId, t.toString());
        }
    }

    private static void addArmorItem(List<ShopSlot> list, String itemId, int cost, String listenerName) {
        try {
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fpsmatch", itemId));
            if (item == null || item == Items.AIR) {
                FPSMatch.LOGGER.warn("Missing armor item fpsmatch:{}, skipping from template", itemId);
                return;
            }
            ShopSlot slot = new ShopSlot(new ItemStack(item), cost);
            ListenerModule lm = FPSMCore.getInstance().getListenerModuleManager().getListenerModule(listenerName);
            if (lm != null) {
                slot.addListener(lm);
            } else {
                FPSMatch.LOGGER.warn("Listener module '{}' not found for armor item {}", listenerName, itemId);
            }
            list.add(slot);
        } catch (Throwable t) {
            FPSMatch.LOGGER.warn("Failed to create armor item fpsmatch:{}: {}", itemId, t.toString());
        }
    }

    private static void addSWItem(List<ShopSlot> list, String itemId, int count, int cost) {
        if (!swLoaded()) {
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
            return;
        }
        try {
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", itemId));
            if (item == null || item == Items.AIR) {
                throw new IllegalStateException("missing item superbwarfare:" + itemId);
            }
            list.add(new ShopSlot(new ItemStack(item, count), cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create SuperbWarfare shop item {}: {}", itemId, t.toString());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }

    private static void addC4Item(List<ShopSlot> list, boolean remote, int cost) {
        if (!swLoaded()) {
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
            return;
        }
        try {
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "c4_bomb"));
            if (item == null || item == Items.AIR) {
                throw new IllegalStateException("missing item superbwarfare:c4_bomb");
            }
            ItemStack stack = new ItemStack(item);
            if (remote) {
                stack.getOrCreateTag().putBoolean("Control", true);
            }
            list.add(new ShopSlot(stack, cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create SuperbWarfare C4 item (remote={}): {}", remote, t.toString());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }

    private static void addNonSWGrenades(List<ShopSlot> list) {
        if (ModList.get().isLoaded("csgrenades")) {
            addItem(list, "csgrenades", "flashbang", 200);
            addItem(list, "csgrenades", "molotov", 400);
            addItem(list, "csgrenades", "incendiary", 500);
        } else {
            addItem(list, "fpsmatch", "flash_bomb", 200);
            addItem(list, "fpsmatch", "t_incendiary_grenade", 400);
            addItem(list, "fpsmatch", "ct_incendiary_grenade", 500);
        }
    }

    /**
     * РўРµС…РЅРёРєР°: СЏС‰РёРє-РєРѕРЅС‚РµР№РЅРµСЂ superbwarfare:container СЃ NBT EntityType.
     * РўРѕС‚ Р¶Рµ С„РѕСЂРјР°С‚, С‡С‚Рѕ РєР»Р°РґС‘С‚ ContainerBlockItem.createInstance(entityType):
     * BlockEntityTag { EntityType: "superbwarfare:<id>" }.
     */
    private static void addVehicle(List<ShopSlot> list, String entityId, int cost) {
        if (!swLoaded()) {
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
            return;
        }
        try {
            net.minecraft.world.item.Item container = ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "container"));
            if (container == null || container == Items.AIR) {
                throw new IllegalStateException("missing item superbwarfare:container");
            }
            ItemStack stack = new ItemStack(container);
            CompoundTag tag = new CompoundTag();
            tag.putString("EntityType", "superbwarfare:" + entityId);
            stack.getOrCreateTag().put("BlockEntityTag", tag);
            list.add(new ShopSlot(stack, cost));
        } catch (Throwable t) {
            com.phasetranscrystal.fpsmatch.FPSMatch.LOGGER.warn("Failed to create vehicle shop item {}: {}", entityId, t.toString());
            list.add(new ShopSlot(ItemStack.EMPTY, cost));
        }
    }
}
