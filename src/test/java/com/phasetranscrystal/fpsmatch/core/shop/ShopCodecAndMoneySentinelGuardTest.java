package com.phasetranscrystal.fpsmatch.core.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopCodecAndMoneySentinelGuardTest {

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    @Test
    void setMoneyPreservesUnlimitedMinusOneSentinel() throws IOException {
        String source = readSource("src/main/java/com/phasetranscrystal/fpsmatch/core/shop/ShopData.java");

        assertTrue(source.contains("if (money == -1)"));
        assertTrue(source.contains("this.money = -1;"));
        assertTrue(source.contains("this.money = Math.max(0, Math.min(16000, money));"));
    }

    @Test
    void shopCodecSkipsUnknownShopTypesInsteadOfThrowing() throws IOException {
        String source = readSource("src/main/java/com/phasetranscrystal/fpsmatch/core/shop/FPSMShop.java");

        assertTrue(source.contains("catch (IllegalArgumentException e)"));
        assertTrue(source.contains("skipping unknown shop type"));
        assertTrue(source.contains("d.put(E.valueOf(enumClass,t), list);"));
    }
}