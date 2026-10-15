package com.ptcrys.blockoffensive.data.persistence;

import com.google.gson.JsonObject;
import com.phasetranscrystal.fpsmatch.core.persistence.DataPersistenceException;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the DataFixer engine that drives the CS shop save migration chain
 * 0 -> 1 -> 2 -> 3 -> 4 -> 5. The BlockOffensive fixer content itself
 * (CSShopDefaultsFixer / CSShopSuperbWarfareFixer / CSShopArmorFixer / CSShopWeaponFireModeFixer) needs a live Minecraft
 * bootstrap (registries, ModList, TACZ/SuperbWarfare item builders) and is
 * reasoned about in the QA audit rather than unit-tested here.
 *
 * Each test registers against a fresh, unique Class key so the singleton
 * {@link DataFixer} registry does not leak state between tests.
 */
class DataFixerVersionChainTest {

    private static final class ChainA {}
    private static final class ChainB {}
    private static final class ChainMissing {}
    private static final class ChainC {}

    @Test
    void appliesFixersInAscendingVersionOrder() {
        DataFixer fixer = DataFixer.getInstance();
        JsonObject root = new JsonObject();

        fixer.registerJsonFixer(ChainA.class, 0, json -> { json.getAsJsonObject().addProperty("0to1", true); return json; });
        fixer.registerJsonFixer(ChainA.class, 1, json -> { json.getAsJsonObject().addProperty("1to2", true); return json; });
        fixer.registerJsonFixer(ChainA.class, 2, json -> { json.getAsJsonObject().addProperty("2to3", true); return json; });
        fixer.registerJsonFixer(ChainA.class, 3, json -> { json.getAsJsonObject().addProperty("3to4", true); return json; });

        JsonObject result = fixer.fixJson(ChainA.class, root, 0, 4).getAsJsonObject();

        assertTrue(result.has("0to1"), "version 0 -> 1 fixer must run");
        assertTrue(result.has("1to2"), "version 1 -> 2 fixer must run");
        assertTrue(result.has("2to3"), "version 2 -> 3 fixer must run");
        assertTrue(result.has("3to4"), "version 3 -> 4 fixer must run");
    }

    @Test
    void appliesVersionFourToFiveFixer() {
        DataFixer fixer = DataFixer.getInstance();
        JsonObject root = new JsonObject();

        fixer.registerJsonFixer(ChainC.class, 3, json -> { json.getAsJsonObject().addProperty("3to4", true); return json; });
        fixer.registerJsonFixer(ChainC.class, 4, json -> { json.getAsJsonObject().addProperty("4to5", true); return json; });

        JsonObject result = fixer.fixJson(ChainC.class, root, 3, 5).getAsJsonObject();

        assertTrue(result.has("3to4"), "version 3 -> 4 fixer must run");
        assertTrue(result.has("4to5"), "version 4 -> 5 fixer must run");
    }

    @Test
    void atTargetVersionIsIdempotent() {
        DataFixer fixer = DataFixer.getInstance();
        JsonObject alreadyCurrent = new JsonObject();
        alreadyCurrent.addProperty("migrated", true);

        JsonObject result = fixer.fixJson(ChainB.class, alreadyCurrent, 4, 4).getAsJsonObject();

        assertSame(alreadyCurrent, result, "no-op migration must return the same JsonObject");
        assertTrue(result.has("migrated"));
    }

    @Test
    void downgradeThrows() {
        DataFixer fixer = DataFixer.getInstance();
        assertThrows(DataPersistenceException.class,
                () -> fixer.fixJson(ChainA.class, new JsonObject(), 4, 3));
    }

    @Test
    void missingFixerThrows() {
        DataFixer fixer = DataFixer.getInstance();
        // Only a version 1 -> 2 fixer is registered; migrating from 0 must fail.
        fixer.registerJsonFixer(ChainMissing.class, 1,
                json -> { json.getAsJsonObject().addProperty("1to2", true); return json; });
        assertThrows(DataPersistenceException.class,
                () -> fixer.fixJson(ChainMissing.class, new JsonObject(), 0, 2));
    }
}