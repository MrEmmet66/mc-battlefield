package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaPositionType;

/** Invite online players into the current room, drawn as a compact floating window. */
public final class Ldlib2MapInviteScreen extends Ldlib2MapChildScreen {
    private final Label subtitleLabel;
    private final VirtualScrollerView<MapRoomPlayerInfo> list;
    private final Label emptyLabel;

    public Ldlib2MapInviteScreen(MapRoomDetail detail, Screen parent) {
        this(build(), detail, parent);
    }

    private Ldlib2MapInviteScreen(Parts parts, MapRoomDetail detail, Screen parent) {
        super(parts.ui(), Component.translatable("gui.fpsm.map_select.invite.title"), detail, parent);
        this.subtitleLabel = parts.subtitle();
        this.list = parts.list();
        this.emptyLabel = parts.empty();
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
        list.setItems(detail.availableInviteTargets());
        list.refreshVisibleItems();
        emptyLabel.setVisible(detail.availableInviteTargets().isEmpty());
    }

    private static Parts build() {
        Chrome chrome = chrome("fpsmatch.map_invite.root", Component.translatable("gui.fpsm.map_select.invite.title"));

        Label subtitle = label("fpsmatch.map_invite.subtitle", Component.empty());
        subtitle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(12).right(12).top(32).height(14));
        FPSMLdlib2Theme.muted(subtitle);

        UIElement panel = new UIElement().setId("fpsmatch.map_invite.panel");
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).right(10).top(50).bottom(50));
        panel.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(
                FPSMLdlib2Theme.CARD_BG, FPSMLdlib2Theme.BORDER, FPSMLdlib2Theme.CARD_RADIUS)));

        Label empty = label("fpsmatch.map_invite.empty", Component.translatable("gui.fpsm.map_select.invite.empty"));
        empty.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(14).right(14).top(16).height(18));
        FPSMLdlib2Theme.muted(empty);

        VirtualScrollerView<MapRoomPlayerInfo> list = new VirtualScrollerView<>();
        list.setId("fpsmatch.map_invite.list");
        list.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(8).right(8).top(8).bottom(8));
        list.virtualScrollerViewStyle(style -> style.estimatedItemHeight(28f));
        list.setItemUIProvider(player -> {
            UIElement row = new UIElement().setId("fpsmatch.map_invite.row." + player.uuid());
            row.layout(layout -> layout.widthPercent(100).height(26));
            FPSMLdlib2Theme.elevated(row);
            Label name = label("fpsmatch.map_invite.name." + player.uuid(), Component.literal(player.name()));
            name.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(10).top(6).widthPercent(50).height(14));
            FPSMLdlib2Theme.body(name);
            Label online = label("fpsmatch.map_invite.online." + player.uuid(), Component.translatable("gui.fpsm.map_select.online"));
            online.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).right(92).top(6).width(80).height(14));
            FPSMLdlib2Theme.status(online, FPSMLdlib2Theme.SUCCESS);
            Button invite = new Button();
            invite.setId("fpsmatch.map_invite.btn." + player.uuid());
            invite.setText(Component.translatable("gui.fpsm.map_select.invite"));
            invite.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).right(8).top(2).width(72).height(22));
            FPSMLdlib2Theme.button(invite, FPSMLdlib2Theme.ButtonKind.PRIMARY);
            invite.setOnClick(e -> {
                if (Minecraft.getInstance().screen instanceof Ldlib2MapInviteScreen screen) {
                    FPSMatch.sendToServer(new MapRoomActionC2SPacket(
                            MapRoomActionC2SPacket.Action.INVITE,
                            screen.detail.summary().gameType(),
                            screen.detail.summary().mapName(),
                            player.uuid()));
                }
            });
            row.addChildren(name, online, invite);
            return row;
        });

        panel.addChildren(empty, list);

        Button back = new Button();
        back.setId("fpsmatch.map_invite.back");
        back.setText(Component.translatable("gui.back"));
        back.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).bottom(14).width(96).height(24));
        FPSMLdlib2Theme.button(back, FPSMLdlib2Theme.ButtonKind.QUIET);

        chrome.root().addChildren(subtitle, panel, back);
        return new Parts(ModularUI.of(UI.of(chrome.root(),
                        screenSize -> MapSelectionLayoutModel.windowSize(screenSize.getWidth(), screenSize.getHeight()))),
                subtitle, list, empty, back, chrome.close());
    }

    private static Label label(String id, Component text) {
        Label label = new Label();
        label.setId(id);
        label.setValue(text);
        return label;
    }

    private record Parts(ModularUI ui, Label subtitle, VirtualScrollerView<MapRoomPlayerInfo> list, Label empty,
                         Button back, Button close) {}
}