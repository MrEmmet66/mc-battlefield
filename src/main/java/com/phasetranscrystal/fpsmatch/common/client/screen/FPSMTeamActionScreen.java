package com.phasetranscrystal.fpsmatch.common.client.screen;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapDetailChildScreen;
import com.phasetranscrystal.fpsmatch.common.client.screen.team.TeamActionModel;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomPlayerInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.UUID;

/** Right-click action box for a player row. */
public final class FPSMTeamActionScreen extends ModularUIScreen implements FPSMMapDetailChildScreen {
    private MapRoomDetail detail;
    private final Screen parent;
    private final UUID player;
    private final UIElement actionPanel;
    private final Label titleLabel;

    public FPSMTeamActionScreen(MapRoomDetail detail, Screen parent, MapRoomPlayerInfo player) {
        this(build(detail, parent, player), detail, parent, player.uuid());
    }

    private FPSMTeamActionScreen(Parts parts, MapRoomDetail detail, Screen parent, UUID player) {
        super(parts.ui(), Component.translatable("gui.fpsm.team_manage.context.title"));
        this.detail = detail;
        this.parent = parent;
        this.player = player;
        this.actionPanel = parts.panel();
        this.titleLabel = parts.title();
    }

    @Override
    public void init() {
        super.init();
        rebuildActions();
    }

    @Override
    public void applyDetail(MapRoomDetail detail) {
        this.detail = detail;
        rebuildActions();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, FPSMLdlib2Theme.SCRIM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        modularUI.onRemoved();
        super.removed();
    }

    private void rebuildActions() {
        actionPanel.clearAllChildren();
        MapRoomPlayerInfo target = detail.players().stream().filter(p -> p.uuid().equals(player)).findFirst().orElse(null);
        if (target == null) {
            onClose();
            return;
        }
        titleLabel.setVisible(true);
        titleLabel.setValue(Component.translatable("gui.fpsm.team_manage.context.player", target.name()));
        int y = 12;
        int index = 0;
        for (String team : TeamActionModel.availableTargetTeams(detail, player)) {
            String targetTeam = team;
            actionPanel.addChild(button(Component.translatable("gui.fpsm.team_manage.context.move", team.toUpperCase()),
                    y + index++ * 28, FPSMLdlib2Theme.ButtonKind.SECONDARY, event -> move(targetTeam)));
        }
        if (TeamActionModel.canKick(detail, player)) {
            actionPanel.addChild(button(Component.translatable("gui.fpsm.map_select.kick"),
                    y + index++ * 28, FPSMLdlib2Theme.ButtonKind.DANGER, event -> kick()));
        }
        actionPanel.addChild(button(Component.translatable("gui.fpsm.team_manage.cancel"), y + index * 28,
                FPSMLdlib2Theme.ButtonKind.QUIET, event -> onClose()));
    }

    private Button button(Component text, int top, FPSMLdlib2Theme.ButtonKind kind,
                          com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener click) {
        Button button = new Button().setText(text).setOnClick(click);
        button.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(12).right(12).top(top).height(22));
        FPSMLdlib2Theme.button(button, kind);
        return button;
    }

    private void move(String team) {
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.SWITCH_TEAM,
                detail.summary().gameType(), detail.summary().mapName(), player, team));
        onClose();
    }

    private void kick() {
        if (!TeamActionModel.canKick(detail, player)) return;
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.KICK,
                detail.summary().gameType(), detail.summary().mapName(), player));
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Parts build(MapRoomDetail detail, Screen parent, MapRoomPlayerInfo player) {
        UIElement root = new UIElement().setId("fpsmatch.team_action.root");
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        FPSMLdlib2Theme.root(root);

        UIElement panel = new UIElement().setId("fpsmatch.team_action.panel");
        panel.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE)
                .left(25).right(25).top(25).height(200));
        FPSMLdlib2Theme.panel(panel);

        Label title = new Label();
        title.setId("fpsmatch.team_action.title");
        title.setValue(Component.translatable("gui.fpsm.team_manage.context.title"));
        title.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE)
                .left(12).right(12).top(8).height(22));
        FPSMLdlib2Theme.title(title);
        panel.addChild(title);

        UIElement actions = new UIElement().setId("fpsmatch.team_action.actions");
        actions.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE)
                .left(0).right(0).top(34).bottom(0));
        panel.addChild(actions);

        root.addChild(panel);
        return new Parts(ModularUI.of(UI.of(root)), actions, title);
    }

    private record Parts(ModularUI ui, UIElement panel, Label title) {
    }
}