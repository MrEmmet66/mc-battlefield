package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.minimap.ui.ldlib2.editor.MinimapEditorScreens;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapSelectScreens;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.MapThumbnailCache;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.UUID;

public final class Ldlib2MapManageScreen extends Ldlib2MapChildScreen {
    private final Label subtitleLabel;
    private final Label permissionLabel;
    private final Button startButton;
    private final Button resetButton;
    private final Button newRoundButton;
    private final Button cleanupButton;
    private final Button switchButton;
    private final Button settingsButton;
    private final Button shopButton;
    private final Button minimapButton;
    private final Button captureButton;

    public Ldlib2MapManageScreen(MapRoomDetail detail, Screen parent) {
        this(build(), detail, parent);
    }

    private Ldlib2MapManageScreen(Parts parts, MapRoomDetail detail, Screen parent) {
        super(parts.ui(), Component.translatable("gui.fpsm.map_select.manage.title"), detail, parent);
        this.subtitleLabel = parts.subtitle();
        this.permissionLabel = parts.permission();
        this.startButton = parts.start();
        this.resetButton = parts.reset();
        this.newRoundButton = parts.newRound();
        this.cleanupButton = parts.cleanup();
        this.switchButton = parts.switchBtn();
        this.settingsButton = parts.settings();
        this.shopButton = parts.shop();
        this.minimapButton = parts.minimap();
        this.captureButton = parts.capture();
        startButton.setOnClick(e -> send(MapRoomActionC2SPacket.Action.DEBUG_START));
        resetButton.setOnClick(e -> send(MapRoomActionC2SPacket.Action.DEBUG_RESET));
        newRoundButton.setOnClick(e -> send(MapRoomActionC2SPacket.Action.DEBUG_NEW_ROUND));
        cleanupButton.setOnClick(e -> send(MapRoomActionC2SPacket.Action.DEBUG_CLEANUP));
        switchButton.setOnClick(e -> send(MapRoomActionC2SPacket.Action.DEBUG_SWITCH));
        settingsButton.setOnClick(e -> FPSMMapSelectScreens.openChild(new Ldlib2MapSettingsScreen(this.detail, this)));
        shopButton.setOnClick(e -> FPSMMapSelectScreens.openChild(new Ldlib2MapShopScreen(this.detail, this)));
        minimapButton.setOnClick(e -> MinimapEditorScreens.open(this.detail, this));
        captureButton.setOnClick(e -> captureThumbnail());
        parts.back().setOnClick(e -> onClose());
        parts.close().setOnClick(e -> onClose());
        refreshContent();
    }

    @Override
    protected void onDetailApplied() {
        refreshContent();
    }

    private void refreshContent() {
        subtitleLabel.setValue(Component.literal(detail.summary().gameType() + " / " + detail.summary().mapName()));
        boolean op = detail.summary().currentPlayerOp();
        permissionLabel.setVisible(!op);
        permissionLabel.setValue(Component.translatable("gui.fpsm.map_select.manage.no_permission"));
        startButton.setActive(op);
        resetButton.setActive(op);
        newRoundButton.setActive(op);
        cleanupButton.setActive(op);
        switchButton.setActive(op);
        settingsButton.setActive(op);
        shopButton.setActive(op && !detail.editableShops().isEmpty());
        minimapButton.setActive(op);
        captureButton.setActive(op);
    }

    private void captureThumbnail() {
        MapThumbnailCache.requestForce(detail.summary().gameType(), detail.summary().mapName());
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.fpsm.map_select.capture_thumbnail"), false);
        }
    }

    private void send(MapRoomActionC2SPacket.Action action) {
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(action, detail.summary().gameType(), detail.summary().mapName(), new UUID(0L, 0L)));
    }

    private static Parts build() {
        Chrome chrome = chrome("fpsmatch.map_manage.root", Component.translatable("gui.fpsm.map_select.manage.title"));

        Label subtitle = label("fpsmatch.map_manage.subtitle", Component.empty());
        subtitle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(12).right(12).top(32).height(14));
        FPSMLdlib2Theme.muted(subtitle);

        UIElement panel = new UIElement().setId("fpsmatch.map_manage.panel");
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).right(10).top(50).bottom(50));
        panel.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(
                FPSMLdlib2Theme.CARD_BG, FPSMLdlib2Theme.BORDER, FPSMLdlib2Theme.CARD_RADIUS)));

        Label debugTitle = label("fpsmatch.map_manage.debug_title", Component.translatable("gui.fpsm.map_select.manage.title"));
        debugTitle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(14).top(8).height(14));
        FPSMLdlib2Theme.sectionTitle(debugTitle);

        Button start = medium("fpsmatch.map_manage.start", "gui.fpsm.map_select.debug.start", 10, 28);
        Button reset = medium("fpsmatch.map_manage.reset", "gui.fpsm.map_select.debug.reset", 108, 28);
        Button newRound = medium("fpsmatch.map_manage.new_round", "gui.fpsm.map_select.debug.new_round", 206, 28);
        Button cleanup = medium("fpsmatch.map_manage.cleanup", "gui.fpsm.map_select.debug.cleanup", 304, 28);
        Button switchBtn = medium("fpsmatch.map_manage.switch", "gui.fpsm.map_select.debug.switch", 402, 28);
        Button settings = medium("fpsmatch.map_manage.settings", "gui.fpsm.map_select.settings", 10, 58);
        Button shop = medium("fpsmatch.map_manage.shop", "gui.fpsm.map_detail.edit_shop", 108, 58);
        Button minimap = medium("fpsmatch.map_manage.minimap", "gui.fpsm.map_detail.edit_minimap", 206, 58);
        Button capture = medium("fpsmatch.map_manage.capture", "gui.fpsm.map_select.capture_thumbnail", 304, 58);

        FPSMLdlib2Theme.button(start, FPSMLdlib2Theme.ButtonKind.PRIMARY);
        for (Button b : new Button[]{reset, newRound, cleanup, switchBtn, settings, shop, minimap, capture}) {
            FPSMLdlib2Theme.button(b, FPSMLdlib2Theme.ButtonKind.SECONDARY);
        }

        Label permission = label("fpsmatch.map_manage.permission", Component.empty());
        permission.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(14).right(14).top(92).height(18));
        FPSMLdlib2Theme.status(permission, FPSMLdlib2Theme.WARNING);
        panel.addChildren(debugTitle, start, reset, newRound, cleanup, switchBtn, settings, shop, minimap, capture, permission);

        Button back = new Button();
        back.setId("fpsmatch.map_manage.back");
        back.setText(Component.translatable("gui.back"));
        back.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).bottom(14).width(96).height(24));
        FPSMLdlib2Theme.button(back, FPSMLdlib2Theme.ButtonKind.QUIET);

        chrome.root().addChildren(subtitle, panel, back);
        return new Parts(ModularUI.of(UI.of(chrome.root(),
                        screenSize -> MapSelectionLayoutModel.windowSize(screenSize.getWidth(), screenSize.getHeight()))),
                subtitle, permission, start, reset, newRound, cleanup, switchBtn, settings, shop, minimap, capture, back, chrome.close());
    }

    private static Label label(String id, Component text) {
        Label label = new Label();
        label.setId(id);
        label.setValue(text);
        return label;
    }

    private static Button medium(String id, String key, int left, int top) {
        Button button = new Button();
        button.setId(id);
        button.setText(Component.translatable(key));
        button.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(left).top(top).width(92).height(24));
        return button;
    }

    private record Parts(ModularUI ui, Label subtitle, Label permission, Button start, Button reset, Button newRound,
                         Button cleanup, Button switchBtn, Button settings, Button shop, Button minimap, Button capture, Button back,
                         Button close) {}
}