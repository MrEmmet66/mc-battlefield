package com.ptcrys.blockoffensive.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared helpers for building TACZ gun {@code ItemStack}s without tripping the
 * "Unknown fire mode, unable to shoot" guard.
 * <p>
 * The TACZ {@code GunItemBuilder} defaults a missing fire mode to
 * {@link FireMode#UNKNOWN}, which is unusable until the player cycles modes.
 * {@link #defaultFireMode(ResourceLocation)} mirrors what TACZ itself does in
 * {@code AbstractGunItem#fillItemCategory}: take the first entry of the gun's
 * supported fire-mode set, falling back to {@link FireMode#SEMI} when the gun
 * index is not available yet.
 */
public final class GunStacks {

    private GunStacks() {
    }

    /**
     * @param gunId the gun resource location, e.g. {@code new ResourceLocation("tacz", "glock_17")}
     * @return the gun's default (first supported) fire mode, or {@link FireMode#SEMI}
     */
    public static FireMode defaultFireMode(ResourceLocation gunId) {
        if (gunId == null) {
            return FireMode.SEMI;
        }
        try {
            return TimelessAPI.getCommonGunIndex(gunId)
                    .map(index -> index.getGunData().getFireModeSet())
                    .filter(set -> !set.isEmpty())
                    .map(set -> set.get(0))
                    .orElse(FireMode.SEMI);
        } catch (Throwable t) {
            return FireMode.SEMI;
        }
    }

    /**
     * @param gunId a gun id string, e.g. {@code "tacz:glock_17"}
     * @return the NBT-ready name of the default fire mode, never {@code null}
     */
    public static String defaultFireModeName(String gunId) {
        try {
            return defaultFireMode(new ResourceLocation(gunId)).name();
        } catch (Throwable t) {
            return FireMode.SEMI.name();
        }
    }
}
