package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.math.Size;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapDetailChildScreen;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapSelectScreens;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.CloseMapViewC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomPlayerInfo;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomSummary;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapSelectionSnapshotS2CPacket;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.OpenMapSelectionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** LDLib2 map-room browser and detail view, HMS // FIELD SYSTEMS style, drawn as a compact floating window. */
public final class Ldlib2MapSelectionScreen extends ModularUIScreen implements FPSMMapDetailChildScreen {
    private static final FPSMLdlib2Theme.ButtonKind[] ACTION_KINDS = {
            FPSMLdlib2Theme.ButtonKind.QUIET,     // refresh
            FPSMLdlib2Theme.ButtonKind.SECONDARY, // manage
            FPSMLdlib2Theme.ButtonKind.SECONDARY, // details
            FPSMLdlib2Theme.ButtonKind.DANGER,    // leave
            FPSMLdlib2Theme.ButtonKind.PRIMARY,   // enter lobby
            FPSMLdlib2Theme.ButtonKind.QUIET      // done
    };

    private final Screen parent;
    private final VirtualScrollerView<MapRoomSummary> roomList;
    private final Label listEmpty;
    private final UIElement listPanel;
    private final UIElement detailPanel;
    private final VirtualScrollerView<MapRoomPlayerInfo> playerList;
    private final TextField search;
    private final UIElement filters;
    private final UIElement actions;
    private final UIElement toast;
    private final List<Button> actionButtons;
    private final List<Button> filterButtons;
    private final UIElement headerBar;
    private final UIElement headerLine;
    private final UIElement highlight;
    private final Label brand;
    private final Label brandSub;
    private final Label roomCount;
    private final Label screenTitle;
    private final Button closeButton;
    private final Label updatingLabel;
    private final Ldlib2MapThumbnailElement detailThumbnail;
    private final Label detailName;
    private final Label detailMode;
    private final Button detailJoin;
    private final Label selectPrompt;
    private final Label playersTitle;
    private final Label noPlayers;
    private final List<InfoRow> infoRows;
    private final UIElement divider;
    private MapSelectionSnapshotS2CPacket snapshot;
    private MapRoomDetail detail;
    private MapRoomSummary selected;
    private String query = "";
    private String stateFilter = "all";
    private PendingOpen pendingOpen = PendingOpen.NONE;
    private boolean detailThumbVisible;
    private boolean refreshing;

    private enum PendingOpen {
        NONE,
        MANAGE,
        TEAM
    }

    private record InfoRow(Label key, Label value) {
    }

    public Ldlib2MapSelectionScreen(MapSelectionSnapshotS2CPacket snapshot, Screen parent) {
        this(build(snapshot), snapshot, parent);
    }

    private Ldlib2MapSelectionScreen(Parts parts, MapSelectionSnapshotS2CPacket snapshot, Screen parent) {
        super(parts.ui(), Component.translatable("gui.fpsm.map_select.title"));
        this.parent = parent;
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.roomList = parts.roomList();
        this.listEmpty = parts.listEmpty();
        this.listPanel = parts.listPanel();
        this.detailPanel = parts.detailPanel();
        this.playerList = parts.playerList();
        this.search = parts.search();
        this.filters = parts.filters();
        this.actions = parts.actions();
        this.toast = parts.toast();
        this.actionButtons = parts.actionButtons();
        this.filterButtons = parts.filterButtons();
        this.headerBar = parts.headerBar();
        this.headerLine = parts.headerLine();
        this.highlight = parts.highlight();
        this.brand = parts.brand();
        this.brandSub = parts.brandSub();
        this.roomCount = parts.roomCount();
        this.screenTitle = parts.screenTitle();
        this.closeButton = parts.closeButton();
        this.updatingLabel = parts.updatingLabel();
        this.detailThumbnail = parts.detailThumbnail();
        this.detailName = parts.detailName();
        this.detailMode = parts.detailMode();
        this.detailJoin = parts.detailJoin();
        this.selectPrompt = parts.selectPrompt();
        this.playersTitle = parts.playersTitle();
        this.noPlayers = parts.noPlayers();
        this.infoRows = parts.infoRows();
        this.divider = parts.divider();
        parts.refresh().setOnClick(event -> refresh());
        parts.close().setOnClick(event -> onClose());
        closeButton.setOnClick(event -> onClose());
        parts.detail().setOnClick(event -> requestDetail());
        parts.join().setOnClick(event -> joinOrOpenManage());
        parts.leave().setOnClick(event -> leave());
        parts.manage().setOnClick(event -> openMapManage());
        parts.detailJoin().setOnClick(event -> joinOrOpenManage());
        parts.filterAll().setOnClick(event -> setStateFilter("all"));
        parts.filterWaiting().setOnClick(event -> setStateFilter("waiting"));
        parts.filterRunning().setOnClick(event -> setStateFilter("running"));
        parts.filterOpen().setOnClick(event -> setStateFilter("open"));
        refreshList();
        refreshDetail();
        refreshActionState();
        refreshFilterState();
        refreshToast();
    }

    @Override
    public void init() {
        super.init();
        bindRequiredWidgets();
        applyResponsiveLayout();
        refreshDetail();
    }

    @Override
    public void tick() {
        super.tick();
        modularUI.tick();
        refreshToast();
    }

    @Override
    public void applyDetail(MapRoomDetail detail) {
        this.detail = detail;
        refreshing = false;
        applyUpdating();
        if (selected == null || !sameRoom(selected, detail.summary())) {
            selected = summaryFor(detail.summary());
        }
        refreshList();
        refreshDetail();
        refreshActionState();
    }

    public void applySnapshot(MapSelectionSnapshotS2CPacket snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        refreshing = false;
        applyUpdating();
        if (selected != null) {
            selected = summaryFor(selected);
        }
        if (selected == null || (detail != null && summaryFor(detail.summary()) == null)) {
            detail = null;
        }
        refreshList();
        refreshDetail();
        refreshActionState();
        refreshFilterState();
        refreshToast();
    }

    @Override
    public void onClose() {
        FPSMatch.sendToServer(new CloseMapViewC2SPacket());
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, FPSMLdlib2Theme.SCRIM);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        FPSMLdlib2Theme.drawWindowShadow(graphics,
                Math.round(modularUI.getLeftPos()), Math.round(modularUI.getTopPos()),
                Math.round(modularUI.getWidth()), Math.round(modularUI.getHeight()));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        modularUI.onRemoved();
        super.removed();
    }

    private void refreshList() {
        List<MapRoomSummary> filtered = snapshot.maps().stream()
                .filter(this::matchesQuery)
                .filter(this::matchesStateFilter)
                .toList();
        roomList.setItems(filtered);
        roomList.refreshVisibleItems();
        listEmpty.setVisible(filtered.isEmpty());
        roomCount.setValue(Component.translatable("gui.fpsm.map_select.rooms", snapshot.maps().size()));
        applyUpdating();
        refreshFilterCounts();
    }

    private void refreshFilterCounts() {
        String[] filterIds = {"all", "waiting", "running", "open"};
        String[] keys = {"gui.fpsm.map_select.filter.all", "gui.fpsm.map_select.filter.waiting",
                "gui.fpsm.map_select.filter.running", "gui.fpsm.map_select.filter.open"};
        for (int i = 0; i < filterButtons.size() && i < filterIds.length; i++) {
            int count = filterCount(filterIds[i]);
            filterButtons.get(i).setText(Component.translatable(keys[i]).append(" ").append(String.valueOf(count)));
        }
    }

    private int filterCount(String filter) {
        return (int) snapshot.maps().stream().filter(summary -> matchesStateFilter(summary, filter)).count();
    }

    private void refreshDetail() {
        boolean hasSummary = selected != null || detail != null;
        selectPrompt.setVisible(!hasSummary);
        detailThumbnail.setVisible(hasSummary && detailThumbVisible);
        detailName.setVisible(hasSummary);
        detailMode.setVisible(hasSummary);
        divider.setVisible(hasSummary);
        detailJoin.setVisible(hasSummary);
        if (!hasSummary) {
            for (InfoRow row : infoRows) {
                row.key().setVisible(false);
                row.value().setVisible(false);
            }
            playersTitle.setVisible(false);
            playerList.setVisible(false);
            noPlayers.setVisible(false);
            playerList.setItems(List.of());
            playerList.refreshVisibleItems();
            return;
        }
        MapRoomSummary summary = detail == null ? selected : detail.summary();
        String texture = detail == null ? "" : firstNonEmpty(detail.backgroundTexture(), detail.iconTexture());
        detailThumbnail.setTexture(texture);
        detailThumbnail.setMapName(summary.mapName());
        detailThumbnail.setGameType(summary.gameType());
        detailThumbnail.setDisplayName(summary.displayName());
        detailName.setValue(FPSMLdlib2Theme.ellipsize(summary.displayName(), 200));
        detailMode.setValue(Component.literal(summary.gameType().toUpperCase(Locale.ROOT) + "  /  " + summary.mapName()));

        setInfoRow(0, "PLAYERS", summary.joinedPlayers() + "/" + maxPlayers(summary), FPSMLdlib2Theme.TEXT);
        setInfoRow(1, "STATUS", statusText(summary).getString(), roomStatusColor(summary));
        setInfoRow(2, "DIMENSION", summary.dimension(), FPSMLdlib2Theme.TEXT, !summary.dimension().isEmpty());

        playerList.setItems(detail == null ? List.of() : detail.players());
        playerList.refreshVisibleItems();
        boolean hasPlayers = detail != null && !detail.players().isEmpty();
        playersTitle.setVisible(true);
        playerList.setVisible(hasPlayers);
        noPlayers.setVisible(!hasPlayers);
    }

    private void setInfoRow(int index, String key, String value, int color) {
        setInfoRow(index, key, value, color, true);
    }

    private void setInfoRow(int index, String key, String value, int color, boolean visible) {
        InfoRow row = infoRows.get(index);
        row.key().setVisible(visible);
        row.value().setVisible(visible);
        row.key().setValue(Component.literal(key));
        row.value().setValue(Component.literal(value));
        row.value().textStyle(style -> style.fontSize(8).textColor(color).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        row.key().textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT));
    }

    private static String firstNonEmpty(String first, String fallback) {
        if (first != null && !first.isEmpty()) return first;
        return fallback == null ? "" : fallback;
    }
    private void applyResponsiveLayout() {
        Size win = MapSelectionLayoutModel.windowSize(width, height);
        int w = win.getWidth();
        int h = win.getHeight();
        MapSelectionLayoutModel layout = MapSelectionLayoutModel.responsive(w, h);
        MapSelectionLayoutModel.Rect hdr = layout.header();
        MapSelectionLayoutModel.Rect toolbarRect = layout.filters();
        MapSelectionLayoutModel.Rect listRect = layout.roomList();
        MapSelectionLayoutModel.Rect detailRect = layout.detail();
        MapSelectionLayoutModel.Rect actionsRect = layout.actions();

        headerBar.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(0).top(0).width(w).height(MapSelectionLayoutModel.HEADER_HEIGHT));
        highlight.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(1).top(1).width(Math.max(1, w - 2)).height(1));
        headerLine.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(0).top(MapSelectionLayoutModel.HEADER_HEIGHT).width(w).height(1));
        brand.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(12).top(4).width(Math.max(60, w - 160)).height(12));
        brandSub.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(12).top(19).width(Math.max(60, w - 220)).height(8));
        screenTitle.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(120).top(8).width(Math.max(80, w - 300)).height(14));
        roomCount.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .right(40).top(9).width(90).height(12));
        closeButton.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .right(8).top(4).width(22).height(22));
        updatingLabel.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .right(40).top(20).width(90).height(8));

        layoutToolbar(toolbarRect);
        place(listPanel, listRect);
        int listInnerW = Math.max(1, listRect.width() - 8);
        int listInnerH = Math.max(1, listRect.height() - 8);
        roomList.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(4).top(4).width(listInnerW).height(listInnerH));
        listEmpty.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(4).top(4).width(listInnerW).height(listInnerH));
        place(detailPanel, detailRect);
        layoutDetailPanel(detailRect.width(), detailRect.height());
        place(actions, actionsRect);
        layoutActions(actionsRect.width());
        toast.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(12).top(hdr.height() + 4).width(Math.max(1, w - 24)).height(26));
    }

    private void layoutToolbar(MapSelectionLayoutModel.Rect toolbar) {
        filters.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(toolbar.x()).top(toolbar.y()).width(toolbar.width()).height(toolbar.height()));
        int searchWidth = Math.min(180, Math.max(120, toolbar.width() - 280));
        int x = 0;
        for (int i = 0; i < filterButtons.size(); i++) {
            int index = i;
            int tabW = tabPixelWidth(filterButtons.get(index));
            final int left = x;
            filterButtons.get(index).layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                    .left(left).top(0).width(tabW).height(toolbar.height()));
            x += tabW + 4;
        }
        search.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(toolbar.width() - searchWidth).top(toolbar.y()).width(searchWidth).height(toolbar.height()));
    }

    private static int tabPixelWidth(Button button) {
        return Math.max(48, Minecraft.getInstance().font.width(button.text.getText().getString()) + 24);
    }

    private void layoutDetailPanel(int w, int h) {
        int pad = 8;
        int thumbW = Math.max(1, w - pad * 2);
        boolean showThumb = h >= 200;
        detailThumbVisible = showThumb;
        int thumbH = showThumb ? Math.min(96, Math.max(48, thumbW * 9 / 16)) : 0;
        int nameTop;
        if (showThumb) {
            detailThumbnail.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                    .left(pad).top(pad).width(thumbW).height(thumbH));
            nameTop = pad + thumbH + 8;
        } else {
            nameTop = pad;
        }
        detailName.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(nameTop).width(thumbW).height(14));
        detailMode.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(nameTop + 16).width(thumbW).height(10));
        divider.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(nameTop + 28).width(thumbW).height(1));
        int infoTop = nameTop + 33;
        int keyW = thumbW / 2;
        for (int i = 0; i < infoRows.size(); i++) {
            InfoRow row = infoRows.get(i);
            int rowTop = infoTop + i * 15;
            row.key().layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                    .left(pad).top(rowTop).width(keyW).height(15));
            row.value().layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                    .left(pad + keyW).top(rowTop).width(thumbW - keyW).height(15));
        }
        int playersTop = infoTop + infoRows.size() * 15 + 4;
        playersTitle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(playersTop).width(thumbW).height(9));
        int buttonTop = h - pad - 26;
        int listBottom = buttonTop - 4;
        int listTop = playersTop + 10;
        int listH = Math.max(1, listBottom - listTop);
        playerList.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(listTop).width(thumbW).height(listH));
        noPlayers.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(playersTop + 10).width(thumbW).height(12));
        detailJoin.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(buttonTop).width(thumbW).height(26));
        selectPrompt.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pad).top(pad).width(thumbW).height(Math.max(20, h - pad * 2 - 34)));
    }

    private void layoutActions(int barWidth) {
        int count = actionButtons.size();
        int gap = 6;
        int available = barWidth;
        int buttonWidth = 76;
        int total = count * buttonWidth + Math.max(0, count - 1) * gap;
        if (total > available) {
            buttonWidth = Math.max(48, (available - Math.max(0, count - 1) * gap) / count);
            total = count * buttonWidth + Math.max(0, count - 1) * gap;
        }
        int start = available - total;
        final int bw = buttonWidth;
        for (int i = 0; i < count; i++) {
            int index = i;
            int left = start + index * (bw + gap);
            actionButtons.get(index).layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                    .left(left).top(0).width(bw).height(26));
        }
    }

    private static void place(UIElement element, MapSelectionLayoutModel.Rect rect) {
        element.layout(s -> s.positionType(YogaPositionType.ABSOLUTE)
                .left(rect.x()).top(rect.y()).width(rect.width()).height(rect.height()));
    }

    private void select(MapRoomSummary summary) {
        selected = summary;
        detail = null;
        pendingOpen = PendingOpen.NONE;
        refreshList();
        refreshDetail();
        refreshActionState();
    }

    private void requestDetail() {
        if (selected == null) {
            return;
        }
        pendingOpen = PendingOpen.NONE;
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.REQUEST_DETAIL,
                selected.gameType(), selected.mapName(), new UUID(0L, 0L)));
    }

    private void requestDetailFor(PendingOpen next) {
        if (selected == null) {
            return;
        }
        pendingOpen = next;
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.REQUEST_DETAIL,
                selected.gameType(), selected.mapName(), new UUID(0L, 0L)));
    }

    public boolean consumePendingManageOpen() {
        if (pendingOpen != PendingOpen.MANAGE) {
            return false;
        }
        pendingOpen = PendingOpen.NONE;
        return true;
    }

    public boolean consumePendingTeamOpen() {
        if (pendingOpen != PendingOpen.TEAM) {
            return false;
        }
        pendingOpen = PendingOpen.NONE;
        return true;
    }

    private void leave() {
        MapRoomSummary summary = selected;
        if (summary == null) return;
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.LEAVE,
                summary.gameType(), summary.mapName(), new UUID(0L, 0L)));
    }

    private void refresh() {
        refreshing = true;
        applyUpdating();
        FPSMatch.sendToServer(new OpenMapSelectionC2SPacket());
    }

    private void applyUpdating() {
        updatingLabel.setVisible(refreshing);
    }

    private void joinOrOpenManage() {
        if (selected == null) {
            return;
        }
        MapRoomSummary summary = summaryFor(selected);
        if (summary == null) {
            return;
        }
        boolean joined = summary.currentPlayerJoined() || summary.currentPlayerSpectating();
        if (joined) {
            if (!"csdm".equalsIgnoreCase(summary.gameType())) {
                openTeamManage();
            }
            return;
        }
        FPSMatch.sendToServer(new MapRoomActionC2SPacket(MapRoomActionC2SPacket.Action.JOIN,
                summary.gameType(), summary.mapName(), new UUID(0L, 0L)));
        if (!"csdm".equalsIgnoreCase(summary.gameType())) {
            requestDetailFor(PendingOpen.TEAM);
        }
    }

    private boolean matchesQuery(MapRoomSummary summary) {
        if (query.isBlank()) return true;
        String normalized = query.toLowerCase(Locale.ROOT);
        return summary.displayName().toLowerCase(Locale.ROOT).contains(normalized)
                || summary.mapName().toLowerCase(Locale.ROOT).contains(normalized)
                || summary.gameType().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesStateFilter(MapRoomSummary summary) {
        return matchesStateFilter(summary, stateFilter);
    }

    private static boolean matchesStateFilter(MapRoomSummary summary, String filter) {
        return switch (filter) {
            case "waiting" -> !summary.started();
            case "running" -> summary.started();
            case "open" -> !summary.full() && (!summary.started() || summary.allowJoinInProgress());
            default -> true;
        };
    }

    private void setStateFilter(String filter) {
        stateFilter = filter;
        refreshList();
        refreshFilterState();
    }

    private void refreshFilterState() {
        String[] filterIds = {"all", "waiting", "running", "open"};
        for (int i = 0; i < filterButtons.size() && i < filterIds.length; i++) {
            boolean selectedFilter = filterIds[i].equals(stateFilter);
            Button button = filterButtons.get(i);
            button.setActive(true);
            FPSMLdlib2Theme.button(button, selectedFilter
                    ? FPSMLdlib2Theme.ButtonKind.PRIMARY
                    : FPSMLdlib2Theme.ButtonKind.QUIET);
        }
    }

    private void refreshActionState() {
        MapRoomSummary summary = selected == null ? null : summaryFor(selected);
        boolean hasSelection = summary != null;
        boolean joined = summary != null && (summary.currentPlayerJoined() || summary.currentPlayerSpectating());
        boolean canJoin = hasSelection && !joined && !summary.full()
                && (!summary.started() || summary.allowJoinInProgress());
        boolean canMapManage = hasSelection && (snapshot.viewerOp()
                || (summary != null && summary.currentPlayerOp()));
        boolean canTeamLobby = joined && !"csdm".equalsIgnoreCase(summary.gameType());
        actionButtons.get(0).setActive(true);
        actionButtons.get(1).setActive(canMapManage);
        actionButtons.get(2).setActive(hasSelection);
        actionButtons.get(3).setActive(joined);
        actionButtons.get(4).setActive(canJoin || canTeamLobby);
        actionButtons.get(5).setActive(true);
        for (int i = 0; i < actionButtons.size() && i < ACTION_KINDS.length; i++) {
            FPSMLdlib2Theme.button(actionButtons.get(i), ACTION_KINDS[i]);
        }
        detailJoin.setActive(canJoin || canTeamLobby);
        FPSMLdlib2Theme.button(detailJoin, FPSMLdlib2Theme.ButtonKind.PRIMARY);
    }

    public void applyToast() {
        refreshToast();
    }

    private void refreshToast() {
        FPSMClient.getGlobalData().getMapRoomToast().ifPresentOrElse(packet -> {
            toast.setVisible(true);
            if (toast.getChildren().isEmpty()) {
                Label label = label(MapSelectionWidgetCatalog.TOAST + ".text", packet.message());
                label.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(8).right(8).top(5).height(18));
                FPSMLdlib2Theme.status(label, packet.error() ? FPSMLdlib2Theme.DANGER : FPSMLdlib2Theme.SUCCESS);
                toast.addChild(label);
            } else if (toast.getChildren().get(0) instanceof Label label) {
                label.setValue(packet.message());
                FPSMLdlib2Theme.status(label, packet.error() ? FPSMLdlib2Theme.DANGER : FPSMLdlib2Theme.SUCCESS);
            }
        }, () -> toast.setVisible(false));
    }

    private void openTeamManage() {
        if (selected == null) {
            return;
        }
        MapRoomSummary summary = summaryFor(selected);
        if (summary != null && "csdm".equalsIgnoreCase(summary.gameType())) {
            return;
        }
        if (detail != null && sameRoom(detail.summary(), selected) && !"csdm".equalsIgnoreCase(detail.summary().gameType())) {
            FPSMMapSelectScreens.openChild(new Ldlib2TeamManageScreen(detail, this));
            return;
        }
        requestDetailFor(PendingOpen.TEAM);
    }

    private void openMapManage() {
        if (selected == null) {
            return;
        }
        if (detail != null && sameRoom(detail.summary(), selected)) {
            FPSMMapSelectScreens.openChild(new Ldlib2MapManageScreen(detail, this));
            return;
        }
        requestDetailFor(PendingOpen.MANAGE);
    }

    private MapRoomSummary summaryFor(MapRoomSummary source) {
        return snapshot.maps().stream().filter(summary -> sameRoom(summary, source)).findFirst().orElse(null);
    }

    private static boolean sameRoom(MapRoomSummary first, MapRoomSummary second) {
        return first != null && second != null && first.gameType().equals(second.gameType()) && first.mapName().equals(second.mapName());
    }

    private static String maxPlayers(MapRoomSummary summary) {
        return summary.maxPlayers() < 0 ? "?" : Integer.toString(summary.maxPlayers());
    }

    private static int roomStatusColor(MapRoomSummary summary) {
        if (summary.full()) return FPSMLdlib2Theme.DANGER;
        if (summary.debug()) return FPSMLdlib2Theme.WARNING;
        if (summary.started()) return summary.allowJoinInProgress() ? FPSMLdlib2Theme.STATUS_RUNNING_JOINABLE : FPSMLdlib2Theme.DANGER;
        return FPSMLdlib2Theme.SUCCESS;
    }

    private static Component statusText(MapRoomSummary summary) {
        if (summary.full()) return Component.translatable("gui.fpsm.map_select.full");
        if (summary.debug()) return Component.translatable("gui.fpsm.map_select.status.debug");
        if (summary.started()) return Component.translatable(summary.allowJoinInProgress()
                ? "gui.fpsm.map_select.status.started_joinable" : "gui.fpsm.map_select.status.started");
        return Component.translatable("gui.fpsm.map_select.status.waiting");
    }

    private void bindRequiredWidgets() {
        if (!modularUI.hasElementWithId(MapSelectionWidgetCatalog.ROOT)
                || !modularUI.hasElementWithId(MapSelectionWidgetCatalog.ROOM_LIST)
                || !modularUI.hasElementWithId(MapSelectionWidgetCatalog.ROOM_DETAIL)
                || !modularUI.hasElementWithId(MapSelectionWidgetCatalog.PLAYERS)
                || !modularUI.hasElementWithId(MapSelectionWidgetCatalog.FILTERS)
                || !modularUI.hasElementWithId(MapSelectionWidgetCatalog.TOAST)) {
            throw new IllegalStateException("Incomplete LDLib2 map selection UI");
        }
    }

    private static Label label(String id, Component text) {
        Label label = new Label();
        label.setId(id);
        label.setValue(text);
        return label;
    }

    private static Button button(String id, String key, UIEventListener listener) {
        Button button = new Button();
        button.setId(id);
        button.setText(Component.translatable(key));
        button.setOnClick(listener);
        return button;
    }
    private static Parts build(MapSelectionSnapshotS2CPacket snapshot) {
        UIElement root = new UIElement().setId(MapSelectionWidgetCatalog.ROOT);
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        FPSMLdlib2Theme.window(root);

        UIElement highlight = new UIElement().setId("fpsmatch.map_selection.highlight");
        highlight.setAllowHitTest(false);
        FPSMLdlib2Theme.windowHighlight(highlight);

        UIElement headerBar = new UIElement().setId("fpsmatch.map_selection.header_bar");
        headerBar.setAllowHitTest(false);
        UIElement headerLine = new UIElement().setId("fpsmatch.map_selection.header_line");
        headerLine.setAllowHitTest(false);
        headerLine.style(style -> style.background(new ColorRectTexture(FPSMLdlib2Theme.BORDER)));

        String brandText = Component.translatable("gui.fpsm.brand.title").getString();
        int cut = brandText.indexOf("//");
        String accentPart = (cut >= 0 ? brandText.substring(0, cut).trim() : brandText) + (cut >= 0 ? " //" : "");
        String textPart = cut >= 0 ? brandText.substring(cut + 2).trim() : "";
        Component brandComponent = Component.literal(accentPart)
                .withStyle(style -> style.withColor(TextColor.fromRgb(FPSMLdlib2Theme.ACCENT & 0xFFFFFF)))
                .append(Component.literal(" "))
                .append(Component.literal(textPart)
                        .withStyle(style -> style.withColor(TextColor.fromRgb(FPSMLdlib2Theme.TEXT & 0xFFFFFF))));
        Label brand = label("fpsmatch.map_selection.brand", brandComponent);
        brand.textStyle(style -> style.fontSize(9).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));

        Label brandSub = label("fpsmatch.map_selection.brand_sub", Component.translatable("gui.fpsm.brand.subtitle"));
        brandSub.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        Label roomCount = label("fpsmatch.map_selection.room_count", Component.literal(""));
        roomCount.textStyle(style -> style.fontSize(8).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));

        Label screenTitle = label(MapSelectionWidgetCatalog.HEADER,
                FPSMLdlib2Theme.uppercase(Component.translatable("gui.fpsm.map_select.title")));
        screenTitle.textStyle(style -> style.fontSize(11).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));

        Button closeButton = new Button();
        closeButton.setId("fpsmatch.map_selection.close");
        FPSMLdlib2Theme.iconButton(closeButton, "\u00D7");

        Label updatingLabel = label("fpsmatch.map_selection.updating",
                Component.translatable("gui.fpsm.map_select.updating"));
        updatingLabel.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        updatingLabel.setVisible(false);

        TextField search = new TextField();
        search.setId(MapSelectionWidgetCatalog.SEARCH);
        search.setAnyString();
        search.setText("");
        FPSMLdlib2Theme.input(search, Component.translatable("gui.fpsm.map_select.search"));

        UIElement listPanel = new UIElement().setId("fpsmatch.map_selection.list_panel");
        listPanel.style(style -> style.background(IGuiTexture.EMPTY));

        VirtualScrollerView<MapRoomSummary> roomList = new VirtualScrollerView<>();
        roomList.setId(MapSelectionWidgetCatalog.ROOM_LIST);
        FPSMLdlib2Theme.clearScrollerViewport(roomList);
        roomList.virtualScrollerViewStyle(style -> style.estimatedItemHeight(50).overscanPixels(100));

        Label listEmpty = label("fpsmatch.map_selection.list_empty", Component.translatable("gui.fpsm.map_select.empty"));
        listEmpty.textStyle(style -> style.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        listPanel.addChildren(roomList, listEmpty);

        UIElement detailPanel = new UIElement().setId(MapSelectionWidgetCatalog.ROOM_DETAIL);
        detailPanel.style(style -> style.background(
                FPSMLdlib2Theme.roundedTexture(FPSMLdlib2Theme.CARD_BG, FPSMLdlib2Theme.BORDER, FPSMLdlib2Theme.CARD_RADIUS)));

        Ldlib2MapThumbnailElement detailThumbnail = new Ldlib2MapThumbnailElement(
                "fpsmatch.map_selection.detail.thumbnail", "", "", "", "");

        Label detailName = label("fpsmatch.map_selection.detail.name", Component.literal(""));
        detailName.textStyle(style -> style.fontSize(11).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));

        Label detailMode = label("fpsmatch.map_selection.detail.mode", Component.literal(""));
        detailMode.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        UIElement divider = new UIElement().setId("fpsmatch.map_selection.detail.divider");
        divider.setAllowHitTest(false);
        divider.style(style -> style.background(new ColorRectTexture(FPSMLdlib2Theme.BORDER)));

        List<InfoRow> infoRows = new ArrayList<>();
        infoRows.add(infoRow("fpsmatch.map_selection.detail.row.players"));
        infoRows.add(infoRow("fpsmatch.map_selection.detail.row.status"));
        infoRows.add(infoRow("fpsmatch.map_selection.detail.row.dimension"));

        Label playersTitle = label("fpsmatch.map_selection.detail.players_title", Component.literal("PLAYERS"));
        playersTitle.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        VirtualScrollerView<MapRoomPlayerInfo> playerList = new VirtualScrollerView<>();
        playerList.setId(MapSelectionWidgetCatalog.PLAYERS);
        FPSMLdlib2Theme.clearScrollerViewport(playerList);
        playerList.virtualScrollerViewStyle(style -> style.estimatedItemHeight(20).overscanPixels(40));
        playerList.setItemUIProvider(info -> {
            Label row = new Label();
            row.setId(MapSelectionWidgetCatalog.PLAYERS + "." + info.uuid());
            row.setValue(Component.literal(info.name() + "  " + info.teamName()
                    + (info.spectator() ? "  SPEC" : "") + (info.ready() ? "  *" : "")));
            row.layout(layout -> layout.height(20).widthPercent(100).paddingLeft(4));
            row.textStyle(style -> style.fontSize(8).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
            return row;
        });

        Label noPlayers = label("fpsmatch.map_selection.detail.no_players",
                Component.translatable("gui.fpsm.map_select.no_players"));
        noPlayers.textStyle(style -> style.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));

        Button detailJoin = button("fpsmatch.map_selection.detail.join", "gui.fpsm.map_select.join", event -> {});
        FPSMLdlib2Theme.button(detailJoin, FPSMLdlib2Theme.ButtonKind.PRIMARY);

        Label selectPrompt = label("fpsmatch.map_selection.detail.prompt",
                Component.translatable("gui.fpsm.map_select.preview.none"));
        selectPrompt.textStyle(style -> style.fontSize(9).textColor(FPSMLdlib2Theme.MUTED).textShadow(false)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));

        List<UIElement> detailChildren = new ArrayList<>();
        detailChildren.add(detailThumbnail);
        detailChildren.add(detailName);
        detailChildren.add(detailMode);
        detailChildren.add(divider);
        for (InfoRow row : infoRows) {
            detailChildren.add(row.key());
            detailChildren.add(row.value());
        }
        detailChildren.add(playersTitle);
        detailChildren.add(playerList);
        detailChildren.add(noPlayers);
        detailChildren.add(detailJoin);
        detailChildren.add(selectPrompt);
        detailPanel.addChildren(detailChildren.toArray(new UIElement[0]));

        UIElement filters = new UIElement().setId(MapSelectionWidgetCatalog.FILTERS);

        UIElement toast = new UIElement().setId(MapSelectionWidgetCatalog.TOAST).setVisible(false);
        FPSMLdlib2Theme.panel(toast);

        Button filterAll = button(MapSelectionWidgetCatalog.FILTERS + ".all", "gui.fpsm.map_select.filter.all", event -> {});
        Button filterWaiting = button(MapSelectionWidgetCatalog.FILTERS + ".waiting", "gui.fpsm.map_select.filter.waiting", event -> {});
        Button filterRunning = button(MapSelectionWidgetCatalog.FILTERS + ".running", "gui.fpsm.map_select.filter.running", event -> {});
        Button filterOpen = button(MapSelectionWidgetCatalog.FILTERS + ".open", "gui.fpsm.map_select.filter.open", event -> {});
        List<Button> filterButtons = List.of(filterAll, filterWaiting, filterRunning, filterOpen);
        filters.addChildren(filterAll, filterWaiting, filterRunning, filterOpen);

        Button refreshBtn = button(MapSelectionWidgetCatalog.ACTIONS + ".refresh", "gui.fpsm.map_select.refresh", event -> {});
        Button manage = button(MapSelectionWidgetCatalog.ACTIONS + ".manage", "gui.fpsm.map_select.manage", event -> {});
        Button detailBtn = button(MapSelectionWidgetCatalog.ACTIONS + ".detail", "gui.fpsm.map_select.detail", event -> {});
        Button leave = button(MapSelectionWidgetCatalog.ACTIONS + ".leave", "gui.fpsm.map_select.leave", event -> {});
        Button join = button(MapSelectionWidgetCatalog.ACTIONS + ".join", "gui.fpsm.map_select.join", event -> {});
        Button close = button(MapSelectionWidgetCatalog.ACTIONS + ".close", "gui.done", event -> {});
        UIElement actions = new UIElement().setId(MapSelectionWidgetCatalog.ACTIONS);
        actions.addChildren(refreshBtn, manage, detailBtn, leave, join, close);

        root.addChildren(headerBar, highlight, headerLine, brand, brandSub, screenTitle, roomCount, closeButton,
                updatingLabel, filters, search, listPanel, detailPanel, actions, toast);

        List<Button> actionButtons = List.of(refreshBtn, manage, detailBtn, leave, join, close);

        Parts parts = new Parts(ModularUI.of(UI.of(root,
                        screenSize -> MapSelectionLayoutModel.windowSize(screenSize.getWidth(), screenSize.getHeight()))),
                roomList, listEmpty, listPanel, detailPanel, playerList, search, filters, actions, toast,
                actionButtons, filterButtons,
                headerBar, headerLine, highlight, brand, brandSub, roomCount, screenTitle, closeButton, updatingLabel,
                detailThumbnail, detailName, detailMode, detailJoin, selectPrompt, playersTitle, noPlayers, infoRows, divider,
                refreshBtn, close, detailBtn, join, leave, manage,
                filterAll, filterWaiting, filterRunning, filterOpen);
        roomList.setItemUIProvider((UIElementProvider<MapRoomSummary>) summary -> {
            Button row = new Button().noText();
            row.setId(MapSelectionWidgetCatalog.ROOM_LIST + "." + summary.gameType() + "." + summary.mapName());
            row.setOnClick(event -> parts.screenSelect(summary));
            row.layout(layout -> layout.height(44).widthPercent(100).marginBottom(6));
            FPSMLdlib2Theme.roomRow(row, roomStatusColor(summary), parts.isSelected(summary));
            Ldlib2MapThumbnailElement thumbnail = new Ldlib2MapThumbnailElement(
                    row.getElementName() + ".preview", summary.iconTexture(), summary.mapName(), summary.gameType(), summary.displayName());
            thumbnail.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(6).top(6).width(32).height(32));
            Label name = label(row.getElementName() + ".name",
                    FPSMLdlib2Theme.ellipsize(summary.displayName(), 180));
            name.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(46).right(92).top(7).height(12));
            name.textStyle(style -> style.fontSize(9).textColor(FPSMLdlib2Theme.TEXT).textShadow(false));
            Label meta = label(row.getElementName() + ".meta",
                    FPSMLdlib2Theme.ellipsize(summary.gameType().toUpperCase(Locale.ROOT) + "  " + summary.mapName(), 180));
            meta.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(46).right(92).top(23).height(10));
            meta.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
            Label players = label(row.getElementName() + ".players",
                    Component.literal(summary.joinedPlayers() + " / " + maxPlayers(summary)));
            players.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).right(10).top(9).width(68).height(11));
            players.textStyle(style -> style.fontSize(8)
                    .textColor(summary.full() ? FPSMLdlib2Theme.DANGER : FPSMLdlib2Theme.TEXT)
                    .textShadow(false).textAlignHorizontal(Horizontal.RIGHT));
            Label state = label(row.getElementName() + ".status", statusText(summary));
            state.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).right(10).top(24).width(68).height(9));
            state.textStyle(style -> style.fontSize(7).textColor(roomStatusColor(summary))
                    .textShadow(false).textAlignHorizontal(Horizontal.RIGHT));
            row.addChildren(thumbnail, name, meta, players, state);
            if (parts.isSelected(summary)) {
                UIElement stripe = new UIElement();
                stripe.setId(row.getElementName() + ".selected");
                stripe.setAllowHitTest(false);
                stripe.layout(layout -> layout.positionType(YogaPositionType.ABSOLUTE).left(2).top(8).width(3).height(28));
                stripe.style(style -> style.background(new ColorRectTexture(FPSMLdlib2Theme.ACCENT)));
                row.addChild(stripe);
            }
            return row;
        });
        search.setTextResponder(value -> {
            if (parts.ui().getScreen() instanceof Ldlib2MapSelectionScreen screen) {
                screen.query = value == null ? "" : value.trim();
                screen.refreshList();
            }
        });
        return parts;
    }

    private static InfoRow infoRow(String id) {
        Label key = label(id + ".key", Component.literal(""));
        key.textStyle(style -> style.fontSize(7).textColor(FPSMLdlib2Theme.MUTED).textShadow(false));
        Label value = label(id + ".value", Component.literal(""));
        value.textStyle(style -> style.fontSize(8).textColor(FPSMLdlib2Theme.TEXT).textShadow(false)
                .textAlignHorizontal(Horizontal.RIGHT));
        return new InfoRow(key, value);
    }

    private record Parts(ModularUI ui, VirtualScrollerView<MapRoomSummary> roomList, Label listEmpty,
                         UIElement listPanel, UIElement detailPanel, VirtualScrollerView<MapRoomPlayerInfo> playerList,
                         TextField search, UIElement filters, UIElement actions, UIElement toast,
                         List<Button> actionButtons, List<Button> filterButtons,
                         UIElement headerBar, UIElement headerLine, UIElement highlight, Label brand, Label brandSub, Label roomCount,
                         Label screenTitle, Button closeButton, Label updatingLabel, Ldlib2MapThumbnailElement detailThumbnail,
                         Label detailName, Label detailMode, Button detailJoin, Label selectPrompt, Label playersTitle, Label noPlayers,
                         List<InfoRow> infoRows, UIElement divider,
                         Button refresh, Button close, Button detail, Button join, Button leave, Button manage,
                         Button filterAll, Button filterWaiting, Button filterRunning, Button filterOpen) {
        private void screenSelect(MapRoomSummary summary) {
            if (ui.getScreen() instanceof Ldlib2MapSelectionScreen screen) screen.select(summary);
        }

        private boolean isSelected(MapRoomSummary summary) {
            return ui.getScreen() instanceof Ldlib2MapSelectionScreen screen && sameRoom(screen.selected, summary);
        }
    }
}