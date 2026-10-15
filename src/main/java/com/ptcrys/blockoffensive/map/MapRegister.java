package com.ptcrys.blockoffensive.map;

import com.ptcrys.blockoffensive.BlockOffensive;

import com.ptcrys.blockoffensive.data.persistence.CSGameMapFixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopArmorFixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopDefaultsFixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopContentV6Fixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopContentV7Fixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopSuperbWarfareFixer;
import com.ptcrys.blockoffensive.data.persistence.CSShopWeaponFireModeFixer;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.event.register.RegisterFPSMSaveDataEvent;
import com.phasetranscrystal.fpsmatch.common.event.register.RegisterFPSMapEvent;
import com.phasetranscrystal.fpsmatch.core.persistence.SaveHolder;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MapRegister {

    @SubscribeEvent
    public static void onMapRegister(RegisterFPSMapEvent event){
        FPSMShop.registerShopType("cs", ItemType.class);
        FPSMShop.registerShopType("csdm", ItemType.class);

        event.registerGameType("cs", CSGameMap::new);
        event.registerGameType("csdm", CSDeathMatchMap::new);
    }

    @SubscribeEvent
    public static void onDataRegister(RegisterFPSMSaveDataEvent event){
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,0, new CSGameMapFixer.OldV0());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,1, new CSShopDefaultsFixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,1, new CSShopDefaultsFixer());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,2, new CSShopSuperbWarfareFixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,2, new CSShopSuperbWarfareFixer());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,3, new CSShopArmorFixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,3, new CSShopArmorFixer());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,4, new CSShopWeaponFireModeFixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,4, new CSShopWeaponFireModeFixer());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,5, new CSShopContentV6Fixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,5, new CSShopContentV6Fixer());
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,6, new CSShopContentV7Fixer());
        DataFixer.getInstance().registerJsonFixer(CSDeathMatchMap.class,6, new CSShopContentV7Fixer());

        event.registerData(CSGameMap.class,"CSGameMaps",
                new SaveHolder.Builder<>(CSGameMap.CODEC)
                        .withVersion(7)
                        .withLoadHandler(CSGameMap::load)
                        .withSaveHandler(CSGameMap::save)
                        .build()
        );

        event.registerData(CSDeathMatchMap.class,"CSDeathMatchMaps",
                new SaveHolder.Builder<>(CSDeathMatchMap.CODEC)
                        .withVersion(7)
                        .withLoadHandler(CSDeathMatchMap::load)
                        .withSaveHandler(CSDeathMatchMap::save)
                        .build()
        );
    }
}
