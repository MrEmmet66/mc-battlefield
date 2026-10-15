package com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2;

import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2.MapSelectionLayoutModel;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2.MapSelectionWidgetCatalog;
import com.phasetranscrystal.fpsmatch.common.client.screen.shop.ldlib2.ShopEditorLayoutModel;
import com.phasetranscrystal.fpsmatch.common.client.screen.shop.ldlib2.ShopEditorWidgetCatalog;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ldlib2ScreenContractTest {
    @Test
    void mapAndShopWidgetCatalogsAreStableAndUnique() {
        assertEquals(MapSelectionWidgetCatalog.ids().size(),
                new HashSet<>(MapSelectionWidgetCatalog.ids()).size());
        assertEquals(ShopEditorWidgetCatalog.ids().size(),
                new HashSet<>(ShopEditorWidgetCatalog.ids()).size());
        assertTrue(MapSelectionWidgetCatalog.ids().contains("fpsmatch.map_selection.room_list"));
        assertTrue(ShopEditorWidgetCatalog.ids().contains("fpsmatch.shop_editor.item"));
    }

    @Test
    void mapSelectionFitsBothWideAndNarrowWindows() {
        MapSelectionLayoutModel wide = MapSelectionLayoutModel.responsive(860, 520);
        MapSelectionLayoutModel narrow = MapSelectionLayoutModel.responsive(520, 300);
        assertFalse(wide.compact());
        assertTrue(narrow.compact());
        assertFalse(wide.filters().intersects(wide.roomList()));
        assertFalse(wide.roomList().intersects(wide.detail()));
        assertFalse(narrow.filters().intersects(narrow.roomList()));
        assertFalse(narrow.roomList().intersects(narrow.detail()));
        assertEquals(860 - MapSelectionLayoutModel.PADDING * 2, wide.actions().width());
        assertEquals(520 - MapSelectionLayoutModel.PADDING * 2, narrow.actions().width());
    }


    @Test
    void shopEditorFitsBothDesktopAndCompactLayouts() {
        ShopEditorLayoutModel desktop = ShopEditorLayoutModel.responsive(960, 540);
        ShopEditorLayoutModel compact = ShopEditorLayoutModel.responsive(480, 360);
        assertFalse(desktop.compact());
        assertTrue(compact.compact());
        assertFalse(desktop.categories().intersects(desktop.slots()));
        assertFalse(desktop.slots().intersects(desktop.properties()));
        assertEquals(960, desktop.actions().width());
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> MapSelectionLayoutModel.responsive(0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> ShopEditorLayoutModel.responsive(100, -1));
    }
}