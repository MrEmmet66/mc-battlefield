package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.math.Size;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.screen.FPSMTeamActionScreen;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapSelectScreens;
import com.phasetranscrystal.fpsmatch.common.client.screen.team.TeamActionModel;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomPlayerInfo;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomSummary;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomTeamInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Lobby team management drawn as a compact centred translucent window (matching the map browser).
 * Server remains authoritative; this UI only requests actions. Drag-drop is intentionally omitted.
 */
public final class Ldlib2TeamManageScreen extends Ldlib2MapChildScreen {

    private static final int TEAM_CT = 0xFF4080FF;
    private static final int TEAM_T = 0xFFFFC040;

    private static final int PADDING = 10;
    private static final int GAP = 8;
    private static final int INFO_HEIGHT = 24;
    private static final int TAB_HEIGHT = 24;
    private static final int BOTTOM_BAR_HEIGHT = 36;
    private static final int BASE_ROW_HEIGHT = 22;
    private static final int MIN_ROW_HEIGHT = 16;
    private static final int COLUMN_PANEL_PAD = 4;
    private static final int TEAM_TAB_WIDTH = 96;
    private static final int MAX_VISIBLE_SLOTS = 5;

    private static final int INFO_TOP = MapSelectionLayoutModel.HEADER_HEIGHT + PADDING;
    private static final int TABS_TOP = INFO_TOP + INFO_HEIGHT + GAP;
    private static final int COLUMNS_TOP = TABS_TOP + TAB_HEIGHT + GAP;

    private final Label modeCaption;
    private final UIElement playersChip;
    private final Label playersChipValue;
    private final UIElement readyChip;
    private final Label readyChipValue;
    private final UIElement countdownBadge;
    private final Label countdownBadgeLabel;
    private final UIElement teamButtonsPanel;
    private final UIElement selectionBar;
    private final Label selectedLabel;
    private final Button cancelButton;
    private final UIElement leftPanel;
    private final UIElement rightPanel;
    private final VirtualScrollerView<Row> leftColumn;
    private final VirtualScrollerView<Row> rightColumn;
    private final VirtualScrollerView<Row> spectatorList;
    private final Ldlib2MapThumbnailElement mapThumb;
    private final Label mapName;
    private final Label mapMode;
    private final Button backButton;
    private final Button readyButton;
    private final UIElement mapCard;

    private final Set<UUID> syncedReadyPlayers = new HashSet<>();
    private int syncedCountdownSeconds;
    private int rowHeight = BASE_ROW_HEIGHT;
    private UUID selectedPlayer;
    private String selectedPlayerTeam;
    private String lastSnapshotKey;
    private boolean wasStarted;
    private boolean closedForMatchStart;

    public Ldlib2TeamManageScreen(MapRoomDetail detail, Screen parent) {
        this(build(), detail, parent);
    }

    private Ldlib2TeamManageScreen(Parts parts, MapRoomDetail detail, Screen parent) {
        super(parts.ui(), FPSMLdlib2Theme.uppercase(Component.translatable("gui.fpsm.team_manage.title")), detail, parent);
        this.modeCaption = parts.modeCaption();
        this.playersChip = parts.playersChip();
        this.playersChipValue = parts.playersChipValue();
        this.readyChip = parts.readyChip();
        this.readyChipValue = parts.readyChipValue();
        this.countdownBadge = parts.countdownBadge();
        this.countdownBadgeLabel = parts.countdownBadgeLabel();
        this.teamButtonsPanel = parts.teamButtonsPanel();
        this.selectionBar = parts.selectionBar();
        this.selectedLabel = parts.selectedLabel();
        this.cancelButton = parts.cancelButton();
        this.leftPanel = parts.leftPanel();
        this.rightPanel = parts.rightPanel();
        this.leftColumn = parts.leftColumn();
        this.rightColumn = parts.rightColumn();
        this.spectatorList = parts.spectatorList();
        this.mapThumb = parts.mapThumb();
        this.mapName = parts.mapName();
        this.mapMode = parts.mapMode();
        this.backButton = parts.backButton();
        this.readyButton = parts.readyButton();
        this.mapCard = parts.mapCard();

        this.syncedReadyPlayers.addAll(detail.readyPlayers());
        this.syncedCountdownSeconds = detail.summary().readyCountdownSeconds();
        this.wasStarted = detail.summary().started();

        readyButton.setOnClick(e -> toggleReady());
        cancelButton.setOnClick(e -> clearSelection());
        backButton.setOnClick(e -> onClose());
        parts.close().setOnClick(e -> onClose());
        leftColumn.setItemUIProvider(this::buildColumnRow);
        rightColumn.setItemUIProvider(this::buildColumnRow);
        spectatorList.setItemUIProvider(this::buildSpectatorRow);
    }

    @Override
    public void init() {
        super.init();
        applyLayout();
        updateMapCard();
        refresh();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        lastSnapshotKey = null;
        applyLayout();
        refresh();
    }

    @Override
    protected void onDetailApplied() {
        boolean started = detail.summary().started();
        if (started && !wasStarted) {
            wasStarted = true;
            closeForMatchStart();
            return;
        }
        wasStarted = started;

        syncedReadyPlayers.clear();
        syncedReadyPlayers.addAll(detail.readyPlayers());
        syncedCountdownSeconds = detail.summary().readyCountdownSeconds();
        if (selectedPlayer != null && detail.players().stream().noneMatch(p -> p.uuid().equals(selectedPlayer))) {
            selectedPlayer = null;
            selectedPlayerTeam = null;
        }
        applyLayout();
        refresh();
    }

    public void applyReadyState(String gameType, String mapName, int countdownSeconds, Set<UUID> readyPlayers) {
        if (detail == null
                || !detail.summary().gameType().equals(gameType)
                || !detail.summary().mapName().equals(mapName)) {
            return;
        }
        this.syncedCountdownSeconds = countdownSeconds;
        this.syncedReadyPlayers.clear();
        this.syncedReadyPlayers.addAll(readyPlayers);
        if (countdownSeconds <= 0) {
            closeForMatchStart();
            return;
        }
        refresh();
    }

    private void closeForMatchStart() {
        if (closedForMatchStart) {
            return;
        }
        closedForMatchStart = true;
        Minecraft.getInstance().setScreen(null);
    }

    private void refresh() {
        String key = snapshotKey();
        if (!Objects.equals(key, lastSnapshotKey)) {
            lastSnapshotKey = key;
            rebuildTeamButtons();
            rebuildColumns();
        }
        updateStatChips();
        updateCountdownBadge();
        updateReadyButton();
        updateModeCaption();
        updateSelectedBar();
    }

    private String snapshotKey() {
        MapRoomSummary s = detail.summary();
        StringBuilder sb = new StringBuilder(256);
        sb.append(s.gameType()).append('|').append(s.mapName()).append('|')
                .append(s.started()).append('|').append(s.allowJoinInProgress()).append('|')
                .append(s.joinedPlayers()).append('|').append(s.maxPlayers()).append('|')
                .append(s.currentPlayerJoined()).append('|').append(s.currentPlayerSpectating()).append('|')
                .append(s.currentPlayerOp()).append('|')
                .append(selfTeamName()).append('|')
                .append(selectedPlayer == null ? "" : selectedPlayer).append('|')
                .append(selectedPlayerTeam == null ? "" : selectedPlayerTeam).append('|');
        detail.teams().stream()
                .sorted(Comparator.comparing(MapRoomTeamInfo::name))
                .forEach(t -> sb.append(t.name()).append(',').append(t.currentPlayers()).append(',')
                        .append(t.playerLimit()).append(',').append(t.spectator()).append(';'));
        sb.append('|');
        detail.players().stream()
                .sorted(Comparator.comparing(MapRoomPlayerInfo::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(p -> sb.append(p.uuid()).append(',').append(p.teamName()).append(',')
                        .append(p.spectator()).append(',').append(isReady(p.uuid())).append(';'));
        return sb.toString();
    }

    private void applyLayout() {
        Size win = MapSelectionLayoutModel.windowSize(width, height);
        int w = win.getWidth();
        int h = win.getHeight();

        int specCount = spectatorCount();
        int specH = specCount == 0 ? 0 : Math.min(BASE_ROW_HEIGHT * specCount + 2, 66);
        int bottomBarTop = h - PADDING - BOTTOM_BAR_HEIGHT;
        int columnsBottom = bottomBarTop - GAP - (specH == 0 ? 0 : specH + GAP);
        int columnsH = Math.max(MIN_ROW_HEIGHT, columnsBottom - COLUMNS_TOP);

        modeCaption.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).top(INFO_TOP + 5).width(150).height(14));
        countdownBadge.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING + 158).top(INFO_TOP).width(106).height(INFO_HEIGHT));
        readyChip.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(PADDING).top(INFO_TOP).width(90).height(INFO_HEIGHT));
        playersChip.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(PADDING + 98).top(INFO_TOP).width(130).height(INFO_HEIGHT));

        selectionBar.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).right(PADDING).top(COLUMNS_TOP).height(22));

        int contentW = Math.max(1, w - PADDING * 2);
        int columnW = Math.max(1, (contentW - GAP) / 2);

        leftPanel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).top(COLUMNS_TOP).width(columnW).height(columnsH));
        rightPanel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(PADDING).top(COLUMNS_TOP).width(columnW).height(columnsH));

        int contentRows = 1 + maxTeamSlots();
        int innerH = Math.max(1, columnsH - COLUMN_PANEL_PAD * 2);
        int scrollerH;
        if (contentRows * BASE_ROW_HEIGHT <= innerH) {
            rowHeight = BASE_ROW_HEIGHT;
            scrollerH = contentRows * rowHeight;
        } else {
            rowHeight = Math.max(MIN_ROW_HEIGHT, innerH / contentRows);
            scrollerH = innerH;
        }

        leftColumn.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(COLUMN_PANEL_PAD).top(COLUMN_PANEL_PAD).right(COLUMN_PANEL_PAD).height(scrollerH));
        leftColumn.virtualScrollerViewStyle(style -> style.estimatedItemHeight(rowHeight).overscanPixels(60));
        rightColumn.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(COLUMN_PANEL_PAD).top(COLUMN_PANEL_PAD).right(COLUMN_PANEL_PAD).height(scrollerH));
        rightColumn.virtualScrollerViewStyle(style -> style.estimatedItemHeight(rowHeight).overscanPixels(60));

        spectatorList.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).right(PADDING).top(columnsBottom + GAP).height(Math.max(0, specH)));
        spectatorList.virtualScrollerViewStyle(style -> style.estimatedItemHeight(rowHeight).overscanPixels(60));

        int mapCardW = Math.min(260, Math.max(140, w - PADDING * 2 - GAP - 208));
        mapCard.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).top(bottomBarTop).width(mapCardW).height(BOTTOM_BAR_HEIGHT));
        mapThumb.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(4).top(4).width(48).height(28));
        mapName.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(60).top(6).right(8).height(12));
        mapMode.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(60).top(22).right(8).height(10));

        int buttonTop = bottomBarTop + (BOTTOM_BAR_HEIGHT - 28) / 2;
        readyButton.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(PADDING).top(buttonTop).width(120).height(28));
        backButton.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(PADDING + 128).top(buttonTop).width(72).height(28));
    }

    private void rebuildTeamButtons() {
        teamButtonsPanel.clearAllChildren();
        List<MapRoomTeamInfo> teams = detail.teams().stream()
                .filter(t -> !t.spectator())
                .sorted(Comparator.comparing(MapRoomTeamInfo::name))
                .toList();
        teamButtonsPanel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(PADDING).top(TABS_TOP).width(Math.max(0, teams.size() * TEAM_TAB_WIDTH)).height(TAB_HEIGHT));
        String selfTeam = selfTeamName();
        boolean canSwitch = canSwitchTeam();
        for (int i = 0; i < teams.size(); i++) {
            MapRoomTeamInfo team = teams.get(i);
            String teamName = team.name();
            boolean current = teamName.equals(selfTeam);
            boolean full = team.isFull();
            boolean switchable = !current && !full && canSwitch;
            int left = i * TEAM_TAB_WIDTH;
            Button button = new Button();
            button.setId("fpsmatch.team_manage.team." + teamName);
            button.setText(FPSMLdlib2Theme.uppercase(Component.literal(teamName)));
            button.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                    .left(left).top(0).width(TEAM_TAB_WIDTH).height(TAB_HEIGHT));
            button.setActive(switchable);
            if (current) {
                styleTeamButton(button, i, teams.size(), TeamButtonKind.SELECTED);
            } else if (switchable) {
                styleTeamButton(button, i, teams.size(), TeamButtonKind.SWITCHABLE);
            } else {
                styleTeamButton(button, i, teams.size(), TeamButtonKind.DISABLED);
            }
            button.setOnClick(e -> {
                if (selectedPlayer != null && detail.summary().currentPlayerOp()) {
                    moveSelectedPlayer(teamName);
                } else {
                    switchSelfTeam(teamName);
                }
            });
            UIElement dot = rect("fpsmatch.team_manage.team_dot." + teamName, teamColor(teamName));
            dot.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                    .left(10).top((TAB_HEIGHT - 6) / 2).width(6).height(6));
            button.addChild(dot);
            teamButtonsPanel.addChild(button);
        }
    }

    private void rebuildColumns() {
        List<Row> left = new ArrayList<>();
        List<Row> right = new ArrayList<>();
        List<MapRoomTeamInfo> teams = detail.teams().stream()
                .filter(t -> !t.spectator())
                .sorted(Comparator.comparing(MapRoomTeamInfo::name))
                .toList();
        for (int i = 0; i < teams.size(); i++) {
            MapRoomTeamInfo team = teams.get(i);
            List<Row> target = (i % 2 == 0) ? left : right;
            target.add(Row.header(team));
            List<MapRoomPlayerInfo> players = playersInTeam(team.name());
            int limit = team.playerLimit();
            int slots = Math.max(players.size(), limit < 0 ? players.size() : limit);
            for (int s = 0; s < slots; s++) {
                target.add(Row.slot(s, s < players.size() ? players.get(s) : null));
            }
        }
        leftColumn.setItems(left);
        leftColumn.refreshVisibleItems();
        leftColumn.setVisible(!left.isEmpty());
        rightColumn.setItems(right);
        rightColumn.refreshVisibleItems();
        rightColumn.setVisible(!right.isEmpty());

        List<Row> spectators = new ArrayList<>();
        for (MapRoomPlayerInfo player : spectatorPlayers()) {
            spectators.add(Row.slot(spectators.size(), player));
        }
        spectatorList.setItems(spectators);
        spectatorList.refreshVisibleItems();
        spectatorList.setVisible(!spectators.isEmpty());
    }

    private int maxTeamSlots() {
        int max = 0;
        for (MapRoomTeamInfo team : detail.teams()) {
            if (team.spectator()) {
                continue;
            }
            int limit = team.playerLimit();
            int players = playersInTeam(team.name()).size();
            int slots = Math.max(players, limit < 0 ? players : limit);
            max = Math.max(max, Math.min(slots, MAX_VISIBLE_SLOTS));
        }
        return max;
    }

    private int topFor(int contentHeight) {
        return Math.max(0, (rowHeight - contentHeight) / 2);
    }

    private UIElement buildColumnRow(Row row) {
        if (row.header()) {
            return buildTeamHeaderRow(row.team());
        }
        return row.player() != null
                ? buildPlayerRow(row.player(), row.slotIndex(), false)
                : buildEmptySlotRow(row.slotIndex());
    }

    private UIElement buildSpectatorRow(Row row) {
        if (row.player() == null) {
            return buildEmptySlotRow(row.slotIndex());
        }
        return buildPlayerRow(row.player(), row.slotIndex(), true);
    }

    private UIElement buildTeamHeaderRow(MapRoomTeamInfo team) {
        UIElement header = new UIElement().setId("fpsmatch.team_manage.team_header." + team.name());
        header.layout(l -> l.widthPercent(100).height(rowHeight));

        int barH = Math.max(4, rowHeight - 8);
        UIElement dot = rect("fpsmatch.team_manage.team_dot_column." + team.name(), teamColor(team.name()));
        dot.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(6).top((rowHeight - barH) / 2).width(3).height(barH));

        Label name = label("fpsmatch.team_manage.team_name." + team.name(),
                FPSMLdlib2Theme.uppercase(Component.literal(team.name())));
        name.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));
        name.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(16).top(topFor(14)).right(64).height(14));

        Label count = label("fpsmatch.team_manage.team_count." + team.name(),
                Component.literal(team.currentPlayers() + " / " + (team.playerLimit() < 0 ? "?" : team.playerLimit())));
        count.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        count.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(8).top(topFor(12)).width(56).height(12));

        header.addChildren(dot, name, count);
        return header;
    }

    private UIElement buildPlayerRow(MapRoomPlayerInfo player, int index, boolean spectator) {
        Minecraft mc = Minecraft.getInstance();
        UUID localUuid = mc.player == null ? null : mc.player.getUUID();
        boolean local = localUuid != null && localUuid.equals(player.uuid());
        boolean selected = selectedPlayer != null && selectedPlayer.equals(player.uuid());
        boolean ready = !spectator && isReady(player.uuid());
        boolean kickable = !spectator && canActOnPlayer(player);

        UIElement row = new UIElement().setId("fpsmatch.team_manage.player." + player.uuid());
        row.layout(l -> l.widthPercent(100).height(rowHeight));
        int fill = local ? FPSMLdlib2Theme.ACCENT_SOFT : FPSMLdlib2Theme.CARD;
        int border = selected ? FPSMLdlib2Theme.ACCENT : FPSMLdlib2Theme.BORDER;
        row.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(fill, border, 3)));

        Button select = new Button();
        select.setId("fpsmatch.team_manage.select." + player.uuid());
        select.noText();
        select.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0).right(0).bottom(0));
        select.buttonStyle(s -> s
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));
        select.setOnClick(e -> {
            if (Minecraft.getInstance().screen instanceof Ldlib2TeamManageScreen screen) {
                if (e.button == 1) {
                    screen.openPlayerActions(player);
                } else {
                    screen.selectPlayer(player);
                }
            }
        });

        Label indexLabel = label("fpsmatch.team_manage.player.index." + player.uuid(),
                Component.literal(Integer.toString(index + 1)));
        indexLabel.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
        indexLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(6).top(topFor(12)).width(16).height(12));

        UIElement avatar = new PlayerFaceElement(
                "fpsmatch.team_manage.player.face." + player.uuid(), player.uuid(), player.name());
        avatar.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(24).top(topFor(16)).width(16).height(16));

        Label name = label("fpsmatch.team_manage.player.name." + player.uuid(), Component.literal(player.name()));
        name.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));
        name.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(46).top(topFor(14)).right(112).height(14));

        Label status = label("fpsmatch.team_manage.player.status." + player.uuid(),
                spectator ? Component.literal("--")
                        : FPSMLdlib2Theme.uppercase(Component.translatable(
                                ready ? "gui.fpsm.team_manage.ready_mark" : "gui.fpsm.team_manage.not_ready_mark")));
        int statusColor = spectator ? FPSMLdlib2Theme.DISABLED_TEXT : (ready ? FPSMLdlib2Theme.SUCCESS : FPSMLdlib2Theme.DANGER);
        status.textStyle(s -> s.fontSize(9).textColor(statusColor).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        status.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .right(52).top(topFor(12)).width(56).height(12));

        row.addChildren(select, indexLabel, avatar, name, status);

        if (local) {
            UIElement bar = rect("fpsmatch.team_manage.player.local." + player.uuid(), FPSMLdlib2Theme.ACCENT);
            bar.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0).width(2).height(rowHeight));
            row.addChild(bar);
        }

        if (kickable) {
            Button kick = new Button();
            kick.setId("fpsmatch.team_manage.kick." + player.uuid());
            kick.setText(Component.translatable("gui.fpsm.map_select.kick"));
            int kickH = Math.max(10, rowHeight - 6);
            kick.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(6).top(topFor(kickH)).width(40).height(kickH));
            int danger50 = withAlpha(FPSMLdlib2Theme.DANGER, 0x80);
            kick.buttonStyle(s -> s
                    .baseTexture(FPSMLdlib2Theme.roundedTexture(0, danger50, FPSMLdlib2Theme.BUTTON_RADIUS))
                    .hoverTexture(FPSMLdlib2Theme.roundedTexture(
                            withAlpha(FPSMLdlib2Theme.EXIT_BG, 0x40), FPSMLdlib2Theme.DANGER, FPSMLdlib2Theme.BUTTON_RADIUS))
                    .pressedTexture(FPSMLdlib2Theme.roundedTexture(
                            withAlpha(FPSMLdlib2Theme.EXIT_BG, 0x55), FPSMLdlib2Theme.DANGER, FPSMLdlib2Theme.BUTTON_RADIUS)));
            kick.textStyle(s -> s.fontSize(8).textColor(FPSMLdlib2Theme.DANGER).textShadow(false));
            kick.setOnClick(e -> {
                if (Minecraft.getInstance().screen instanceof Ldlib2TeamManageScreen screen) {
                    screen.sendAction(MapRoomActionC2SPacket.Action.KICK, player.uuid());
                }
            });
            row.addChild(kick);
        }
        return row;
    }

    private UIElement buildEmptySlotRow(int index) {
        UIElement row = new UIElement().setId("fpsmatch.team_manage.empty." + index);
        row.layout(l -> l.widthPercent(100).height(rowHeight));
        row.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(
                withAlpha(FPSMLdlib2Theme.CARD, 0x50), withAlpha(FPSMLdlib2Theme.BORDER, 0x66), 3)));

        Label indexLabel = label("fpsmatch.team_manage.empty.index." + index,
                Component.literal(Integer.toString(index + 1)));
        indexLabel.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.DISABLED_TEXT).textShadow(false));
        indexLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(6).top(topFor(12)).width(16).height(12));

        UIElement avatar = rect("fpsmatch.team_manage.empty.face." + index, withAlpha(FPSMLdlib2Theme.BORDER, 0x66));
        avatar.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(24).top(topFor(16)).width(16).height(16));

        Label name = label("fpsmatch.team_manage.empty.name." + index,
                Component.translatable("gui.fpsm.team_manage.empty_slot"));
        name.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.DISABLED_TEXT).textShadow(false));
        name.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(46).top(topFor(14)).right(112).height(14));

        Label status = label("fpsmatch.team_manage.empty.status." + index, Component.literal("--"));
        status.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.DISABLED_TEXT).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        status.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(52).top(topFor(12)).width(56).height(12));

        row.addChildren(indexLabel, avatar, name, status);
        return row;
    }
    private void updateStatChips() {
        MapRoomSummary s = detail.summary();
        playersChipValue.setValue(Component.literal(s.joinedPlayers() + "/" + maxPlayers(s)));
        int ready = readyNormalPlayerCount();
        int total = normalPlayerCount();
        readyChipValue.setValue(Component.literal(ready + "/" + total));
        int readyColor = (ready == total && total > 0) ? FPSMLdlib2Theme.SUCCESS : FPSMLdlib2Theme.WARNING;
        readyChipValue.textStyle(st -> st.fontSize(10).textColor(readyColor).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
    }

    private void updateCountdownBadge() {
        int ready = readyNormalPlayerCount();
        int total = normalPlayerCount();
        if (syncedCountdownSeconds > 0) {
            countdownBadge.setVisible(true);
            countdownBadgeLabel.setValue(FPSMLdlib2Theme.uppercase(
                    Component.translatable("gui.fpsm.team_manage.countdown_short", syncedCountdownSeconds)));
            styleCountdownBadge(FPSMLdlib2Theme.WARNING);
        } else if (total > 0 && ready == total) {
            countdownBadge.setVisible(true);
            countdownBadgeLabel.setValue(FPSMLdlib2Theme.uppercase(
                    Component.translatable("gui.fpsm.team_manage.all_ready")));
            styleCountdownBadge(FPSMLdlib2Theme.SUCCESS);
        } else {
            countdownBadge.setVisible(false);
        }
    }

    private void styleCountdownBadge(int color) {
        countdownBadge.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(
                withAlpha(color, 0x26), withAlpha(color, 0xB3), FPSMLdlib2Theme.BUTTON_RADIUS)));
        countdownBadgeLabel.textStyle(st -> st.fontSize(9).textColor(color).textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER));
    }

    private void updateReadyButton() {
        Minecraft mc = Minecraft.getInstance();
        boolean joined = detail.summary().currentPlayerJoined();
        boolean active = joined && mc.player != null;
        readyButton.setActive(active);
        boolean ready = mc.player != null && isReady(mc.player.getUUID());
        readyButton.setText(Component.translatable(ready ? "gui.fpsm.team_manage.ready.off" : "gui.fpsm.team_manage.ready.on"));
        if (!active) {
            FPSMLdlib2Theme.buttonDisabled(readyButton);
        } else if (ready) {
            FPSMLdlib2Theme.button(readyButton, FPSMLdlib2Theme.ButtonKind.SECONDARY);
            readyButton.textStyle(st -> st.fontSize(9).textColor(FPSMLdlib2Theme.SUCCESS).textShadow(false));
        } else {
            FPSMLdlib2Theme.button(readyButton, FPSMLdlib2Theme.ButtonKind.PRIMARY);
        }
    }

    private void updateModeCaption() {
        MapRoomSummary s = detail.summary();
        modeCaption.setValue(FPSMLdlib2Theme.uppercase(
                Component.literal(s.gameType() + " \u00B7 " + s.mapName())));
    }

    private void updateSelectedBar() {
        boolean show = hasAdminPanel();
        selectionBar.setVisible(show);
        if (show) {
            MapRoomPlayerInfo player = findPlayer(selectedPlayer);
            if (player != null) {
                selectedLabel.setValue(Component.translatable("gui.fpsm.team_manage.selected",
                        player.name(), selectedPlayerTeam == null ? "?" : selectedPlayerTeam.toUpperCase(Locale.ROOT)));
            } else {
                selectedLabel.setValue(Component.empty());
            }
        }
    }

    private void updateMapCard() {
        MapRoomSummary s = detail.summary();
        String texture = firstNonEmpty(detail.backgroundTexture(), detail.iconTexture());
        mapThumb.setTexture(texture);
        mapThumb.setMapName(s.mapName());
        mapThumb.setGameType(s.gameType());
        mapThumb.setDisplayName(s.displayName());
        String name = (s.displayName() == null || s.displayName().isEmpty()) ? s.mapName() : s.displayName();
        mapName.setValue(Component.literal(name));
        mapMode.setValue(FPSMLdlib2Theme.uppercase(Component.literal(s.gameType())));
    }

    private void openPlayerActions(MapRoomPlayerInfo player) {
        FPSMMapSelectScreens.openChild(new FPSMTeamActionScreen(detail, this, player));
    }

    private void selectPlayer(MapRoomPlayerInfo player) {
        if (!detail.summary().currentPlayerOp() || player.spectator()) {
            return;
        }
        if (selectedPlayer != null && selectedPlayer.equals(player.uuid())) {
            clearSelection();
            return;
        }
        selectedPlayer = player.uuid();
        selectedPlayerTeam = player.teamName();
        refresh();
    }

    private void clearSelection() {
        selectedPlayer = null;
        selectedPlayerTeam = null;
        refresh();
    }

    private void switchSelfTeam(String teamName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        sendAction(MapRoomActionC2SPacket.Action.SWITCH_TEAM, mc.player.getUUID(), teamName);
    }

    private void moveSelectedPlayer(String targetTeam) {
        if (selectedPlayer == null) {
            return;
        }
        sendAction(MapRoomActionC2SPacket.Action.SWITCH_TEAM, selectedPlayer, targetTeam);
        clearSelection();
    }

    private void toggleReady() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        sendAction(MapRoomActionC2SPacket.Action.READY, mc.player.getUUID());
    }

    private void sendAction(MapRoomActionC2SPacket.Action action, UUID target) {
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(action,
                detail.summary().gameType(), detail.summary().mapName(), target));
    }

    private void sendAction(MapRoomActionC2SPacket.Action action, UUID target, String data) {
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(action,
                detail.summary().gameType(), detail.summary().mapName(), target, data));
    }

    private boolean hasAdminPanel() {
        return detail.summary().currentPlayerOp() && selectedPlayer != null;
    }

    private boolean canActOnPlayer(MapRoomPlayerInfo player) {
        return TeamActionModel.canKick(detail, player.uuid());
    }

    private boolean canSwitchTeam() {
        return !detail.summary().started() || detail.summary().allowJoinInProgress();
    }

    private boolean isReady(UUID uuid) {
        return syncedReadyPlayers.contains(uuid) || detail.players().stream()
                .anyMatch(p -> p.uuid().equals(uuid) && p.ready());
    }

    private String selfTeamName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "";
        }
        return detail.players().stream()
                .filter(p -> p.uuid().equals(mc.player.getUUID()))
                .map(MapRoomPlayerInfo::teamName)
                .findFirst()
                .orElse("");
    }

    private List<MapRoomPlayerInfo> playersInTeam(String teamName) {
        return detail.players().stream()
                .filter(p -> !p.spectator() && p.teamName().equalsIgnoreCase(teamName))
                .sorted(Comparator.comparing(MapRoomPlayerInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<MapRoomPlayerInfo> spectatorPlayers() {
        return detail.players().stream()
                .filter(MapRoomPlayerInfo::spectator)
                .sorted(Comparator.comparing(MapRoomPlayerInfo::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private int spectatorCount() {
        return (int) detail.players().stream().filter(MapRoomPlayerInfo::spectator).count();
    }

    private MapRoomPlayerInfo findPlayer(UUID uuid) {
        return detail.players().stream().filter(p -> p.uuid().equals(uuid)).findFirst().orElse(null);
    }

    private int normalPlayerCount() {
        return (int) detail.players().stream().filter(p -> !p.spectator()).count();
    }

    private int readyNormalPlayerCount() {
        return (int) detail.players().stream()
                .filter(p -> !p.spectator())
                .filter(p -> isReady(p.uuid()))
                .count();
    }

    private static String maxPlayers(MapRoomSummary summary) {
        return summary.maxPlayers() < 0 ? "?" : Integer.toString(summary.maxPlayers());
    }

    private static String firstNonEmpty(String first, String fallback) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        return fallback == null ? "" : fallback;
    }

    private static int teamColor(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ct" -> TEAM_CT;
            case "t" -> TEAM_T;
            default -> FPSMLdlib2Theme.ACCENT;
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private enum TeamButtonKind {
        SELECTED, SWITCHABLE, DISABLED
    }

    private static void styleTeamButton(Button button, int index, int count, TeamButtonKind kind) {
        int fill;
        int border;
        int textColor;
        switch (kind) {
            case SELECTED -> {
                fill = FPSMLdlib2Theme.ACCENT_SOFT;
                border = FPSMLdlib2Theme.ACCENT;
                textColor = FPSMLdlib2Theme.TEXT;
            }
            case SWITCHABLE -> {
                fill = FPSMLdlib2Theme.CARD;
                border = FPSMLdlib2Theme.BORDER;
                textColor = FPSMLdlib2Theme.TEXT;
            }
            default -> {
                fill = 0x00000000;
                border = (0x80 << 24) | (FPSMLdlib2Theme.BORDER & 0xFFFFFF);
                textColor = FPSMLdlib2Theme.DISABLED_TEXT;
            }
        }
        boolean leftEdge = index == 0;
        boolean rightEdge = index == count - 1;
        int hoverFill = kind == TeamButtonKind.SELECTED ? FPSMLdlib2Theme.ACCENT_SOFT
                : kind == TeamButtonKind.SWITCHABLE ? FPSMLdlib2Theme.CARD_HOVER
                : (0x40 << 24) | (FPSMLdlib2Theme.CARD & 0xFFFFFF);
        int hoverBorder = kind == TeamButtonKind.SELECTED ? FPSMLdlib2Theme.ACCENT
                : kind == TeamButtonKind.SWITCHABLE ? FPSMLdlib2Theme.BORDER_STRONG
                : (0x80 << 24) | (FPSMLdlib2Theme.BORDER & 0xFFFFFF);
        float radius = FPSMLdlib2Theme.BUTTON_RADIUS;
        button.buttonStyle(style -> style
                .baseTexture(FPSMLdlib2Theme.segmentedTexture(fill, border, radius, leftEdge, rightEdge))
                .hoverTexture(FPSMLdlib2Theme.segmentedTexture(hoverFill, hoverBorder, radius, leftEdge, rightEdge))
                .pressedTexture(FPSMLdlib2Theme.segmentedTexture(fill, border, radius, leftEdge, rightEdge)));
        button.textStyle(style -> style.fontSize(9).textColor(textColor).textShadow(false));
    }

    private record Row(boolean header, MapRoomTeamInfo team, int slotIndex, MapRoomPlayerInfo player) {
        static Row header(MapRoomTeamInfo team) {
            return new Row(true, team, 0, null);
        }

        static Row slot(int index, MapRoomPlayerInfo player) {
            return new Row(false, null, index, player);
        }
    }
    private static Parts build() {
        Chrome chrome = chrome("fpsmatch.team_manage.root",
                FPSMLdlib2Theme.uppercase(Component.translatable("gui.fpsm.team_manage.title")));

        chrome.title().layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(120).top(8).right(40).height(14));
        chrome.subtitle().setValue(Component.translatable("gui.fpsm.brand.subtitle"));
        chrome.subtitle().layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(12).top(19).right(140).height(8));
        Label brand = label("fpsmatch.team_manage.brand", buildBrandComponent());
        brand.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));
        brand.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(12).top(4).right(140).height(12));
        chrome.headerBar().addChild(brand);

        UIElement root = chrome.root();

        Label modeCaption = label("fpsmatch.team_manage.mode_caption", Component.empty());
        modeCaption.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        UIElement countdownBadge = new UIElement().setId("fpsmatch.team_manage.countdown_badge");
        Label countdownBadgeLabel = label("fpsmatch.team_manage.countdown_badge.label", Component.empty());
        countdownBadgeLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(4).right(4).top(5).height(14));
        countdownBadge.addChild(countdownBadgeLabel);
        countdownBadge.setVisible(false);

        UIElement readyChip = new UIElement().setId("fpsmatch.team_manage.ready_chip");
        readyChip.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(FPSMLdlib2Theme.CARD, FPSMLdlib2Theme.BORDER, 3)));
        Label readyChipLabel = label("fpsmatch.team_manage.ready_chip.label", Component.literal("READY"));
        readyChipLabel.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
        readyChipLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(8).top(5).width(34).height(14));
        Label readyChipValue = label("fpsmatch.team_manage.ready_chip.value", Component.empty());
        readyChipValue.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.WARNING).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        readyChipValue.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(8).top(5).width(36).height(14));
        readyChip.addChildren(readyChipLabel, readyChipValue);

        UIElement playersChip = new UIElement().setId("fpsmatch.team_manage.players_chip");
        playersChip.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(FPSMLdlib2Theme.CARD, FPSMLdlib2Theme.BORDER, 3)));
        Label playersChipLabel = label("fpsmatch.team_manage.players_chip.label", Component.literal("PLAYERS"));
        playersChipLabel.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
        playersChipLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(8).top(5).width(48).height(14));
        Label playersChipValue = label("fpsmatch.team_manage.players_chip.value", Component.empty());
        playersChipValue.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.TEXT).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        playersChipValue.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(8).top(5).width(62).height(14));
        playersChip.addChildren(playersChipLabel, playersChipValue);

        UIElement teamButtonsPanel = new UIElement().setId("fpsmatch.team_manage.teams");

        UIElement selectionBar = new UIElement().setId("fpsmatch.team_manage.selection");
        selectionBar.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(FPSMLdlib2Theme.CARD_BG, FPSMLdlib2Theme.BORDER, 3)));
        selectionBar.setVisible(false);

        Label selectedLabel = label("fpsmatch.team_manage.selected", Component.empty());
        FPSMLdlib2Theme.muted(selectedLabel);
        selectedLabel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(8).top(4).right(56).height(14));

        Button cancelButton = new Button();
        cancelButton.setId("fpsmatch.team_manage.cancel");
        cancelButton.setText(Component.translatable("gui.fpsm.team_manage.cancel"));
        cancelButton.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(0).top(1).width(44).height(20));
        FPSMLdlib2Theme.button(cancelButton, FPSMLdlib2Theme.ButtonKind.QUIET);

        selectionBar.addChildren(selectedLabel, cancelButton);

        UIElement leftPanel = new UIElement().setId("fpsmatch.team_manage.column_left_panel");
        FPSMLdlib2Theme.panel(leftPanel);
        UIElement rightPanel = new UIElement().setId("fpsmatch.team_manage.column_right_panel");
        FPSMLdlib2Theme.panel(rightPanel);

        VirtualScrollerView<Row> leftColumn = new VirtualScrollerView<>();
        leftColumn.setId("fpsmatch.team_manage.column_left");
        FPSMLdlib2Theme.clearScrollerViewport(leftColumn);
        FPSMLdlib2Theme.slimScroller(leftColumn);
        leftColumn.virtualScrollerViewStyle(style -> style.estimatedItemHeight(BASE_ROW_HEIGHT).overscanPixels(60));

        VirtualScrollerView<Row> rightColumn = new VirtualScrollerView<>();
        rightColumn.setId("fpsmatch.team_manage.column_right");
        FPSMLdlib2Theme.clearScrollerViewport(rightColumn);
        FPSMLdlib2Theme.slimScroller(rightColumn);
        rightColumn.virtualScrollerViewStyle(style -> style.estimatedItemHeight(BASE_ROW_HEIGHT).overscanPixels(60));

        VirtualScrollerView<Row> spectatorList = new VirtualScrollerView<>();
        spectatorList.setId("fpsmatch.team_manage.spectators");
        FPSMLdlib2Theme.clearScrollerViewport(spectatorList);
        FPSMLdlib2Theme.slimScroller(spectatorList);
        spectatorList.virtualScrollerViewStyle(style -> style.estimatedItemHeight(BASE_ROW_HEIGHT).overscanPixels(60));
        spectatorList.setVisible(false);

        leftPanel.addChild(leftColumn);
        rightPanel.addChild(rightColumn);

        UIElement mapCard = new UIElement().setId("fpsmatch.team_manage.map_card");
        mapCard.style(s -> s.background(FPSMLdlib2Theme.roundedTexture(
                FPSMLdlib2Theme.CARD_BG, FPSMLdlib2Theme.BORDER, FPSMLdlib2Theme.CARD_RADIUS)));

        Ldlib2MapThumbnailElement mapThumb = new Ldlib2MapThumbnailElement(
                "fpsmatch.team_manage.map_thumb", "", "", "", "");
        Label mapNameLabel = label("fpsmatch.team_manage.map_name", Component.empty());
        mapNameLabel.textStyle(s -> s.fontSize(10).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));
        Label mapModeLabel = label("fpsmatch.team_manage.map_mode", Component.empty());
        mapModeLabel.textStyle(s -> s.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        mapCard.addChildren(mapThumb, mapNameLabel, mapModeLabel);

        Button readyButton = new Button();
        readyButton.setId("fpsmatch.team_manage.ready");
        readyButton.setText(Component.translatable("gui.fpsm.team_manage.ready.on"));
        FPSMLdlib2Theme.button(readyButton, FPSMLdlib2Theme.ButtonKind.PRIMARY);

        Button backButton = new Button();
        backButton.setId("fpsmatch.team_manage.back");
        backButton.setText(Component.translatable("gui.back"));
        FPSMLdlib2Theme.button(backButton, FPSMLdlib2Theme.ButtonKind.SECONDARY);

        root.addChildren(modeCaption, countdownBadge, playersChip, readyChip, teamButtonsPanel,
                leftPanel, rightPanel, spectatorList, selectionBar, mapCard, readyButton, backButton);

        return new Parts(ModularUI.of(UI.of(root,
                        screenSize -> MapSelectionLayoutModel.windowSize(screenSize.getWidth(), screenSize.getHeight()))),
                modeCaption, playersChip, playersChipValue, readyChip, readyChipValue,
                countdownBadge, countdownBadgeLabel,
                teamButtonsPanel, selectionBar, selectedLabel, cancelButton,
                leftPanel, rightPanel, leftColumn, rightColumn, spectatorList,
                mapCard, mapThumb, mapNameLabel, mapModeLabel, backButton, readyButton, chrome.close());
    }

    private static Component buildBrandComponent() {
        String raw = Component.translatable("gui.fpsm.brand.title").getString();
        int cut = raw.indexOf("//");
        String accentPart = cut >= 0 ? raw.substring(0, cut).trim() + " //" : raw.trim();
        String textPart = cut >= 0 ? " " + raw.substring(cut + 2).trim() : "";
        MutableComponent component = Component.literal(accentPart)
                .withStyle(style -> style.withColor(TextColor.fromRgb(FPSMLdlib2Theme.ACCENT & 0xFFFFFF)));
        if (!textPart.trim().isEmpty()) {
            component = component.append(Component.literal(textPart)
                    .withStyle(style -> style.withColor(TextColor.fromRgb(FPSMLdlib2Theme.TEXT & 0xFFFFFF))));
        }
        return component;
    }

    private static UIElement rect(String id, int color) {
        UIElement element = new UIElement().setId(id);
        element.setAllowHitTest(false);
        element.style(s -> s.background(new ColorRectTexture(color)));
        return element;
    }

    private static Label label(String id, Component text) {
        Label label = new Label();
        label.setId(id);
        label.setAllowHitTest(false);
        label.setValue(text);
        return label;
    }

    private static final class PlayerFaceElement extends UIElement {
        private final UUID uuid;
        private final String name;
        private ResourceLocation skin;
        private boolean resolved;

        PlayerFaceElement(String id, UUID uuid, String name) {
            setId(id);
            setAllowHitTest(false);
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            if (getSizeWidth() <= 0 || getSizeHeight() <= 0) {
                return;
            }
            ResourceLocation location = skinLocation();
            if (location == null) {
                return;
            }
            int x = Math.round(getPositionX());
            int y = Math.round(getPositionY());
            int size = Math.round(getSizeWidth());
            context.graphics.pose().pushPose();
            context.graphics.pose().translate(x, y, 0);
            float scale = size / 8.0f;
            context.graphics.pose().scale(scale, scale, 1.0f);
            context.graphics.blit(location, 0, 0, 8, 8, 8, 8, 64, 64);
            context.graphics.blit(location, 0, 0, 40, 8, 8, 8, 64, 64);
            context.graphics.pose().popPose();
        }

        private ResourceLocation skinLocation() {
            if (resolved) {
                return skin;
            }
            resolved = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    skin = info.getSkinLocation();
                }
            }
            if (skin == null && name != null && !name.isEmpty() && uuid != null) {
                skin = mc.getSkinManager().getInsecureSkinLocation(
                        new com.mojang.authlib.GameProfile(uuid, name));
            }
            return skin;
        }
    }

    private record Parts(ModularUI ui, Label modeCaption, UIElement playersChip, Label playersChipValue,
                         UIElement readyChip, Label readyChipValue, UIElement countdownBadge, Label countdownBadgeLabel,
                         UIElement teamButtonsPanel, UIElement selectionBar, Label selectedLabel, Button cancelButton,
                         UIElement leftPanel, UIElement rightPanel, VirtualScrollerView<Row> leftColumn,
                         VirtualScrollerView<Row> rightColumn, VirtualScrollerView<Row> spectatorList,
                         UIElement mapCard, Ldlib2MapThumbnailElement mapThumb, Label mapName, Label mapMode,
                         Button backButton, Button readyButton, Button close) {}
}