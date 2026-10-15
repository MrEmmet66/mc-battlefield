package com.ptcrys.blockoffensive.client.screen;

import com.ptcrys.blockoffensive.BlockOffensive;
import com.ptcrys.blockoffensive.client.data.CSClientData;
import com.ptcrys.blockoffensive.client.renderer.ShopSlotRenderer;
import com.ptcrys.blockoffensive.client.ui.BOTheme;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import com.ptcrys.blockoffensive.util.OverlaySnapshotKey;
import com.ptcrys.blockoffensive.util.ShopOverlayLayout;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.music.FPSClientMusicManager;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.packet.shop.ShopActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.sound.FPSMSoundRegister;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.shop.ShopAction;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.TextUtils;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.RelativeLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CSGameShopScreen extends Fragment implements ScreenCallback {
    public static final Map<ItemType, List<GunButtonLayout>> shopButtons = new HashMap<>();
    public static final String BACKGROUND = "ui/cs/background.png";
    public static final int DISABLE_TEXT_COLOR = BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB);
    public static volatile boolean refreshFlag = false;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean SHOP_DEBUG = Boolean.getBoolean("bo.shopdebug");
    private static volatile long lastOverlayErrorLogTime = 0L;
    private static volatile long lastSnapshotDebugLogTime = 0L;
    private final Map<Long, PendingShopAction> pendingActions = new HashMap<>();
    private static int selectedCategory = 0;
    private static volatile OverlaySnapshot activeOverlay = null;
    private WindowLayout window;

    public CSGameShopScreen() {
    }

    public static float calculateScaleFactor(int w, int h) {
        return Math.min((float) w / 1920, (float) h / 1080);
    }

    public static CSGameShopScreen getInstance() {
        return new CSGameShopScreen();
    }

    public static String getCategoryTitle(int index) {
        ItemType[] values = ItemType.values();
        if (index < 0 || index >= values.length) index = 0;
        ItemType type = values[index];
        String key = "blockoffensive.shop.title." + type.name().toLowerCase(Locale.ROOT);
        return I18n.exists(key) ? I18n.get(key) : prettifyEnumName(type.name());
    }

    private static String prettifyEnumName(String name) {
        String[] parts = name.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }

    private static List<Integer> getVisibleOrdinals() {
        ItemType[] values = ItemType.values();
        List<Integer> ordinals = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (values[i].slotCount() > 0) {
                ordinals.add(i);
            }
        }
        if (ordinals.isEmpty()) {
            ordinals.add(0);
        }
        return ordinals;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        window = new WindowLayout(getContext(), this);
        return window;
    }

    @Override
    public boolean isBackKey(int keyCode, KeyEvent event) {
        if (keyCode == GLFW.GLFW_KEY_B) {
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            WindowLayout wl = window;
            if (wl != null && wl.handleHotkey(keyCode)) {
                return false;
            }
        }
        return ScreenCallback.super.isBackKey(keyCode, event);
    }

    public static ItemStack getHoveredItemStack() {
        OverlaySnapshot snap = activeOverlay;
        if (snap == null || snap.cards == null) {
            return ItemStack.EMPTY;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.mouseHandler == null) {
            return ItemStack.EMPTY;
        }
        int idx = ShopOverlayLayout.findHovered(snap.cards, mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
        if (idx < 0) {
            return ItemStack.EMPTY;
        }
        ShopOverlayLayout.CardDesc card = snap.cards.get(idx);
        if (card.typeOrdinal < 0 || card.typeOrdinal >= ItemType.values().length) {
            return ItemStack.EMPTY;
        }
        ClientShopSlot slot = FPSMClient.getGlobalData().getSlotData(ItemType.values()[card.typeOrdinal].name(), card.index);
        if (slot == null || slot.itemStack().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return slot.itemStack();
    }

    @Override
    public void onDestroyView() {
        window = null;
        pendingActions.clear();
        activeOverlay = null;
        super.onDestroyView();
    }

    private void sendShopAction(ItemType type, int index, ShopAction action) {
        String mapName = FPSMClient.getGlobalData().getCurrentMap();
        NetworkPacketRegister.getChannelFromCache(ShopActionC2SPacket.class)
                .sendToServer(new ShopActionC2SPacket(mapName, type, index, action));
    }

    public void selectItem(ItemType type, int index) {
        WindowLayout wl = window;
        if (wl != null) {
            wl.selectItem(type, index);
        }
    }

    public void attemptBuy(ItemType type, int index) {
        ClientShopSlot slot = FPSMClient.getGlobalData().getSlotData(type.name(), index);
        if (slot == null) return;
        boolean enable = CSClientData.canOpenShop && CSClientData.getMoney() >= slot.cost() && !slot.itemStack().isEmpty();
        if (!enable) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        }
        sendShopAction(type, index, ShopAction.BUY);
        WindowLayout wl = window;
        if (wl != null) {
            wl.invalidate();
        }
    }

    private static void playPurchaseSound(ItemType type, int index) {
        ItemStack itemStack = FPSMClient.getGlobalData().getSlotData(type.name(), index).itemStack();
        if (GunCompatManager.isGun(itemStack)) {
            FPSMUtil.getGunTypeByGunId(GunCompatManager.findProvider(itemStack).getGunId(itemStack))
                    .ifPresent(gunType -> FPSClientMusicManager.playSound(FPSMSoundRegister.getGunDropSound(gunType)));
            return;
        }
        SoundEvent sound = FPSMImpl.findLrtacticalMod() && LrtacticalCompat.isKnife(itemStack.getItem())
                ? FPSMSoundRegister.getKnifeDropSound()
                : FPSMSoundRegister.getItemDropSound(itemStack.getItem());
        FPSClientMusicManager.playSound(sound);
    }

    private record PendingShopAction(ItemType type, int index, ShopAction action) {
        private boolean matches(ItemType candidateType, int candidateIndex, ShopAction candidateAction) {
            return type == candidateType && index == candidateIndex && action == candidateAction;
        }

        private boolean matches(String candidateType, int candidateIndex, ShopAction candidateAction) {
            return type.name().equals(candidateType) && index == candidateIndex && action == candidateAction;
        }
    }

    private static void renderShopOverlay(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof MuiScreen muiScreen) || !(muiScreen.getFragment() instanceof CSGameShopScreen)) {
            return;
        }
        OverlaySnapshot snap = activeOverlay;
        if (snap == null) return;
        GuiGraphics gr = event.getGuiGraphics();
        double guiScale = mc.getWindow().getGuiScale();
        if (guiScale <= 0) return;
        try {
            gr.flush();
            SlotIcon[] grid = snap.gridIcons;
            if (grid != null && grid.length > 0 && snap.viewportWidth > 0 && snap.viewportHeight > 0) {
                int sx = (int) Math.floor(snap.viewportX / guiScale);
                int sy = (int) Math.floor(snap.viewportY / guiScale);
                int ex = (int) Math.ceil((snap.viewportX + snap.viewportWidth) / guiScale);
                int ey = (int) Math.ceil((snap.viewportY + snap.viewportHeight) / guiScale);
                gr.enableScissor(sx, sy, ex, ey);
                gr.flush();
                for (SlotIcon s : grid) {
                    if (s == null) continue;
                    ShopSlotRenderer.drawSlot(gr, s.type, s.index, s.x, s.y, s.w, s.h, guiScale);
                }
                gr.flush();
                gr.disableScissor();
            }
            SlotIcon details = snap.detailsIcon;
            if (details != null) {
                ShopSlotRenderer.drawSlot(gr, details.type, details.index, details.x, details.y, details.w, details.h, guiScale);
            }
            gr.flush();
        } catch (RuntimeException e) {
            long now = System.currentTimeMillis();
            if (now - lastOverlayErrorLogTime > 1000L) {
                lastOverlayErrorLogTime = now;
                LOGGER.warn("[BO-Shop] overlay render failed; skipping", e);
            }
        } finally {
            gr.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static final class SlotIcon {
        public final ItemType type;
        public final int index;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public final float fullH;

        SlotIcon(ItemType type, int index, float x, float y, float w, float h, float fullH) {
            this.type = type;
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.fullH = fullH;
        }
    }

    public static final class OverlaySnapshot {
        final SlotIcon[] gridIcons;
        final int viewportX;
        final int viewportY;
        final int viewportWidth;
        final int viewportHeight;
        final SlotIcon detailsIcon;
        final List<ShopOverlayLayout.CardDesc> cards;

        OverlaySnapshot(SlotIcon[] gridIcons, int viewportX, int viewportY, int viewportWidth, int viewportHeight,
                        SlotIcon detailsIcon, List<ShopOverlayLayout.CardDesc> cards) {
            this.gridIcons = gridIcons;
            this.viewportX = viewportX;
            this.viewportY = viewportY;
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;
            this.detailsIcon = detailsIcon;
            this.cards = cards;
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT)
    public static final class ShopScreenEvents {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onRenderPost(ScreenEvent.Render.Post event) {
            CSGameShopScreen.renderShopOverlay(event);
        }
    }
    public static class WindowLayout extends RelativeLayout {
        private static final int CONTENT_WIDTH = 1080;
        private static final int CONTENT_HEIGHT = 620;
        private static final int HEAD_HEIGHT = 52;
        private static final int SIDEBAR_WIDTH = 210;
        private static final int DETAILS_WIDTH = 280;
        private static final int CARD_HEIGHT = 112;
        private static final int CARD_GAP = 8;
        private static final int BODY_GAP = 12;
        private static final int BODY_PADDING = 12;
        private static final int TITLE_ROW_H = 36;
        private static final int DETAILS_ICON_H = 130;
        private static final int BUY_BUTTON_H = 36;

        private static final int MIN_CARD_WIDTH = 140;

        private float scale = 1.0f;
        private ImageView background;
        private RelativeLayout headBar;
        private LinearLayout body;
        private LinearLayout sidebar;
        private LinearLayout gridPanel;
        private LinearLayout titleRow;
        private TextView categoryTitleText;
        private TextView categoryCountChip;
        private FrameLayout gridScrollContainer;
        private ScrollView gridScroll;
        private LinearLayout itemsContainer;
        private LinearLayout detailsPanel;
        private ImageView headerDivider;

        private LinearLayout titleBlock;
        private TextView titleText;
        private TextView teamCaptionText;
        private LinearLayout moneyBlock;
        public TextView moneyText;
        public TextView cooldownChip;
        public TextView nextRoundMinMoneyText;

        private View detailsIconView;
        private TextView detailsNameText;
        private TextView detailsCategoryText;
        private TextView detailsPriceText;
        private TextView detailsOwnedText;
        private TextView detailsDescText;
        private TextView buyButton;

        private ShapeDrawable buyButtonBg;

        private final CSGameShopScreen host;

        private int windowLeft;
        private int windowTop;
        private int windowWidth;
        private int windowHeight;

        private int cardColumns = 3;
        private List<GunButtonLayout> currentButtons = new ArrayList<>();
        private ItemType selectedType = null;
        private int selectedIndex = -1;
        private final int[] selectedByOrdinal = new int[ItemType.values().length];
        private int cardH = CARD_HEIGHT;
        private int cardGap = CARD_GAP;
        private int cardIconH = (int) GunButtonLayout.ICON_AREA_HEIGHT;
        private int gridPadLeft;
        private int gridPadRight;
        private int gridPadTop;
        private int gridPadBottom;
        private ItemType renderedType = null;
        private int renderedIndex = -1;

        private boolean frameLoopRunning = false;
        private int cachedMoney = Integer.MIN_VALUE;
        private int cachedTeam = -1;
        private int cachedNextMin = Integer.MIN_VALUE;
        private int cachedCooldown = Integer.MIN_VALUE;
        private int cachedPriceCost = Integer.MIN_VALUE;
        private int cachedPriceColorKey = -1;
        private int cachedOwnedCount = -1;
        private int cachedBuyCost = Integer.MIN_VALUE;
        private int cachedBuyState = -1;
        private boolean cachedDetailsEmpty = false;
        private final int[] tmpLocation = new int[2];

        private int[] lastSnapKey = null;

        public final List<CategoryButton> categoryButtons = new ArrayList<>();
        private final List<TextView> hintTexts = new ArrayList<>();

        public WindowLayout(Context context, CSGameShopScreen host) {
            super(context);
            this.host = host;
            init();
        }

        private static ShapeDrawable shape(int color, int radius, int strokeW, int strokeColor) {
            ShapeDrawable d = new ShapeDrawable();
            d.setShape(ShapeDrawable.RECTANGLE);
            d.setColor(color);
            d.setCornerRadius(radius);
            if (strokeW > 0) d.setStroke(strokeW, strokeColor);
            return d;
        }

        private static ShapeDrawable shape(int color, int radius) {
            return shape(color, radius, 0, 0);
        }

        private void init() {
            setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            background = new ImageView(getContext());
            ShapeDrawable panelShape = shape(BOTheme.mui(BOTheme.PANEL_RGB), BOTheme.RADIUS_LARGE, BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB));
            panelShape.setAlpha(235);
            background.setImageDrawable(panelShape);
            addView(background);

            buildHeader();
            buildBody();

            ItemType[] values = ItemType.values();
            if (selectedCategory < 0 || selectedCategory >= values.length || values[selectedCategory].slotCount() == 0) {
                selectedCategory = getVisibleOrdinals().get(0);
            }
            refreshSidebarSelection();
            rebuildItems();
            ItemType type = ItemType.values()[selectedCategory];
            int sel = ShopOverlayLayout.resolveSelectionIndex(selectedByOrdinal, selectedCategory, type.slotCount());
            selectedType = type;
            selectedIndex = sel;
            applySelectionToCards();
            updateText();
            refreshDetails();
        }

        private void buildHeader() {
            headBar = new RelativeLayout(getContext());

            ImageView headerBg = new ImageView(getContext());
            ShapeDrawable headerShape = shape(BOTheme.mui(BOTheme.BACKGROUND_RGB), BOTheme.RADIUS_LARGE);
            headerShape.setAlpha(245);
            headerBg.setImageDrawable(headerShape);
            headerBg.setScaleType(ImageView.ScaleType.FIT_XY);
            headBar.addView(headerBg, new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            titleBlock = new LinearLayout(getContext());
            titleBlock.setOrientation(LinearLayout.VERTICAL);
            titleBlock.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            titleText = new TextView(getContext());
            titleText.setText(I18n.get("blockoffensive.shop.header.title").toUpperCase(Locale.ROOT));
            titleText.setTextSize(18);
            titleText.setTextColor(BOTheme.mui(BOTheme.TEXT_PRIMARY_RGB));
            titleText.setMaxLines(1);
            titleBlock.addView(titleText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            teamCaptionText = new TextView(getContext());
            teamCaptionText.setTextSize(11);
            teamCaptionText.setMaxLines(1);
            titleBlock.addView(teamCaptionText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            titleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
            titleParams.leftMargin = (int) (20 * scale);
            headBar.addView(titleBlock, titleParams);

            cooldownChip = new TextView(getContext());
            cooldownChip.setBackground(shape(BOTheme.mui(BOTheme.CARD_RGB), BOTheme.RADIUS_SMALL));
            cooldownChip.setTextSize(12);
            cooldownChip.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            cooldownChip.setGravity(Gravity.CENTER);
            cooldownChip.setMaxLines(1);
            cooldownChip.setPadding((int) (9 * scale), (int) (2 * scale), (int) (9 * scale), (int) (2 * scale));
            RelativeLayout.LayoutParams cooldownParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            cooldownParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            headBar.addView(cooldownChip, cooldownParams);

            moneyBlock = new LinearLayout(getContext());
            moneyBlock.setOrientation(LinearLayout.VERTICAL);
            moneyBlock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            moneyText = new TextView(getContext());
            moneyText.setTextSize(22);
            moneyText.setTextColor(BOTheme.mui(BOTheme.ACCENT_RGB));
            moneyText.setMaxLines(1);
            moneyText.setGravity(Gravity.RIGHT);
            moneyBlock.addView(moneyText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            nextRoundMinMoneyText = new TextView(getContext());
            nextRoundMinMoneyText.setTextSize(11);
            nextRoundMinMoneyText.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            nextRoundMinMoneyText.setMaxLines(1);
            nextRoundMinMoneyText.setGravity(Gravity.RIGHT);
            moneyBlock.addView(nextRoundMinMoneyText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            RelativeLayout.LayoutParams moneyParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            moneyParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            moneyParams.addRule(RelativeLayout.CENTER_VERTICAL);
            moneyParams.rightMargin = (int) (20 * scale);
            headBar.addView(moneyBlock, moneyParams);

            headerDivider = new ImageView(getContext());
            ShapeDrawable dividerShape = shape(BOTheme.mui(BOTheme.BORDER_RGB), 0);
            headerDivider.setImageDrawable(dividerShape);
            headerDivider.setScaleType(ImageView.ScaleType.FIT_XY);
            RelativeLayout.LayoutParams dividerParams = new RelativeLayout.LayoutParams(MATCH_PARENT, BOTheme.BORDER_WIDTH);
            dividerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            headBar.addView(headerDivider, dividerParams);

            addView(headBar);
        }
        private void buildBody() {
            body = new LinearLayout(getContext());
            body.setOrientation(LinearLayout.HORIZONTAL);
            body.setPadding((int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale));
            buildSidebar();
            buildGridPanel();
            buildDetailsPanel();
            addView(body);
        }

        private void buildSidebar() {
            sidebar = new LinearLayout(getContext());
            sidebar.setOrientation(LinearLayout.VERTICAL);
            List<Integer> visible = getVisibleOrdinals();
            for (int pos = 0; pos < visible.size(); pos++) {
                int ordinal = visible.get(pos);
                CategoryButton b = new CategoryButton(getContext(), ordinal, pos + 1);
                final int idx = ordinal;
                b.setOnClickListener(v -> selectCategory(idx));
                categoryButtons.add(b);
                sidebar.addView(b, new LinearLayout.LayoutParams(MATCH_PARENT, (int) CategoryButton.BUTTON_HEIGHT));
            }

            View spacer = new View(getContext());
            sidebar.addView(spacer, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));

            hintTexts.add(makeHint("blockoffensive.shop.hint.category"));
            hintTexts.add(makeHint("blockoffensive.shop.hint.buy"));
            hintTexts.add(makeHint("blockoffensive.shop.hint.close"));
            for (TextView h : hintTexts) {
                sidebar.addView(h, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            }

            body.addView(sidebar, new LinearLayout.LayoutParams((int) SIDEBAR_WIDTH, MATCH_PARENT));
        }

        private TextView makeHint(String key) {
            TextView t = new TextView(getContext());
            t.setText(I18n.get(key));
            t.setTextSize(11);
            t.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            t.setMaxLines(1);
            t.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            t.setPadding((int) (10 * scale), (int) (4 * scale), (int) (8 * scale), 0);
            return t;
        }

        private void buildGridPanel() {
            gridPanel = new LinearLayout(getContext());
            gridPanel.setOrientation(LinearLayout.VERTICAL);

            titleRow = new LinearLayout(getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            categoryTitleText = new TextView(getContext());
            categoryTitleText.setTextSize(16);
            categoryTitleText.setTextColor(BOTheme.mui(BOTheme.TEXT_PRIMARY_RGB));
            categoryTitleText.setMaxLines(1);
            titleRow.addView(categoryTitleText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            categoryCountChip = new TextView(getContext());
            categoryCountChip.setBackground(shape(BOTheme.mui(BOTheme.CARD_RGB), BOTheme.RADIUS_SMALL));
            categoryCountChip.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            categoryCountChip.setTextSize(11);
            categoryCountChip.setGravity(Gravity.CENTER);
            categoryCountChip.setMaxLines(1);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            chipLp.leftMargin = (int) (8 * scale);
            titleRow.addView(categoryCountChip, chipLp);
            gridPanel.addView(titleRow, new LinearLayout.LayoutParams(MATCH_PARENT, (int) TITLE_ROW_H));

            gridScrollContainer = new FrameLayout(getContext());

            gridScroll = new ScrollView(getContext());
            gridScroll.setFillViewport(true);
            gridScroll.setVerticalScrollBarEnabled(true);
            gridScroll.setScrollbarFadingEnabled(false);
            ShapeDrawable thumb = shape(BOTheme.muiA(0x99, BOTheme.ACCENT_RGB), BOTheme.RADIUS_SMALL);
            gridScroll.setVerticalScrollbarThumbDrawable(thumb);
            gridScroll.setScrollBarSize(5);
            gridScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> publishOverlaySnapshot("scroll"));

            itemsContainer = new LinearLayout(getContext());
            itemsContainer.setOrientation(LinearLayout.VERTICAL);
            gridScroll.addView(itemsContainer, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            gridScrollContainer.addView(gridScroll, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            gridPanel.addView(gridScrollContainer, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));

            body.addView(gridPanel, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1.0f));
        }

        private void buildDetailsPanel() {
            detailsPanel = new LinearLayout(getContext());
            detailsPanel.setOrientation(LinearLayout.VERTICAL);
            detailsPanel.setBackground(shape(BOTheme.mui(BOTheme.CARD_RGB), BOTheme.RADIUS, BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB)));
            detailsPanel.setPadding((int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale), (int) (BODY_PADDING * scale));

            detailsIconView = new View(getContext());
            detailsIconView.setVisibility(View.INVISIBLE);
            detailsPanel.addView(detailsIconView, new LinearLayout.LayoutParams(MATCH_PARENT, (int) DETAILS_ICON_H));

            detailsNameText = makeText(16, BOTheme.TEXT_PRIMARY_RGB, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            detailsPanel.addView(detailsNameText, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            detailsCategoryText = makeText(11, BOTheme.TEXT_SECONDARY_RGB, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            detailsPanel.addView(detailsCategoryText, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            detailsPriceText = makeText(20, BOTheme.ACCENT_RGB, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            detailsPanel.addView(detailsPriceText, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            detailsOwnedText = makeText(12, BOTheme.STATE_OK_RGB, Gravity.LEFT | Gravity.CENTER_VERTICAL);
            detailsOwnedText.setVisibility(View.GONE);
            detailsPanel.addView(detailsOwnedText, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            detailsDescText = new TextView(getContext());
            detailsDescText.setTextSize(12);
            detailsDescText.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            detailsDescText.setGravity(Gravity.LEFT | Gravity.TOP);
            detailsDescText.setMaxLines(6);
            detailsDescText.setEllipsize(TextUtils.TruncateAt.END);
            detailsPanel.addView(detailsDescText, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

            View spacer = new View(getContext());
            detailsPanel.addView(spacer, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));

            buyButton = new TextView(getContext());
            buyButtonBg = shape(BOTheme.mui(BOTheme.ACCENT_STRONG_RGB), BOTheme.RADIUS);
            buyButton.setBackground(buyButtonBg);
            buyButton.setTextSize(14);
            buyButton.setTextColor(BOTheme.mui(BOTheme.TEXT_PRIMARY_RGB));
            buyButton.setGravity(Gravity.CENTER);
            buyButton.setMaxLines(1);
            buyButton.setClickable(true);
            buyButton.setOnClickListener(v -> {
                if (selectedType != null && selectedIndex >= 0) {
                    host.attemptBuy(selectedType, selectedIndex);
                }
            });
            LinearLayout.LayoutParams buyLp = new LinearLayout.LayoutParams(MATCH_PARENT, (int) BUY_BUTTON_H);
            buyLp.topMargin = (int) (10 * scale);
            detailsPanel.addView(buyButton, buyLp);

            body.addView(detailsPanel, new LinearLayout.LayoutParams((int) DETAILS_WIDTH, MATCH_PARENT));
        }

        private TextView makeText(float size, int rgb, int gravity) {
            TextView t = new TextView(getContext());
            t.setTextSize(size);
            t.setTextColor(BOTheme.mui(rgb));
            t.setGravity(gravity);
            t.setMaxLines(1);
            return t;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            addOnLayoutChangeListener(this::onWindowLayoutChange);
            if (!frameLoopRunning) {
                frameLoopRunning = true;
                postOnAnimation(mFrameLoop);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            frameLoopRunning = false;
            super.onDetachedFromWindow();
        }

        private final Runnable mFrameLoop = new Runnable() {
            @Override
            public void run() {
                if (!frameLoopRunning) return;
                invalidate();
                updateText();
                refreshDetails();
                postOnAnimation(this);
            }
        };

        public void selectCategory(int index) {
            if (index < 0 || index >= ItemType.values().length) return;
            selectedCategory = index;
            refreshSidebarSelection();
            rebuildItems();
            if (gridScroll != null) {
                gridScroll.scrollTo(0, 0);
            }
            ItemType type = ItemType.values()[selectedCategory];
            int sel = ShopOverlayLayout.resolveSelectionIndex(selectedByOrdinal, selectedCategory, type.slotCount());
            selectedType = type;
            selectedIndex = sel;
            applySelectionToCards();
            refreshDetails();
            publishOverlaySnapshot("category");
        }

        private void refreshSidebarSelection() {
            for (CategoryButton button : categoryButtons) {
                button.setSelected(button.index == selectedCategory);
            }
        }

        private int computeColumns() {
            int avail;
            if (gridPanel != null && gridPanel.getWidth() > 0) {
                avail = gridPanel.getWidth();
            } else {
                avail = (int) ((CONTENT_WIDTH - SIDEBAR_WIDTH - DETAILS_WIDTH - 2 * BODY_GAP - 2 * BODY_PADDING) * scale);
            }
            return (avail / 3) >= MIN_CARD_WIDTH ? 3 : 2;
        }

        private void rebuildItems() {
            itemsContainer.removeAllViews();
            ItemType type = ItemType.values()[selectedCategory];
            int count = type.slotCount();
            cardColumns = computeColumns();
            categoryTitleText.setText(getCategoryTitle(selectedCategory).toUpperCase(Locale.ROOT));
            categoryCountChip.setText(String.valueOf(count));
            List<GunButtonLayout> buttons = new ArrayList<>();
            final int gap = (int) (CARD_GAP * scale);
            final int rowHeight = (int) (CARD_HEIGHT * scale);
            int paddedCount = ((count + cardColumns - 1) / cardColumns) * cardColumns;
            LinearLayout row = null;
            for (int j = 0; j < paddedCount; j++) {
                if (j % cardColumns == 0) {
                    row = new LinearLayout(getContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH_PARENT, rowHeight);
                    rp.bottomMargin = gap;
                    itemsContainer.addView(row, rp);
                }
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, MATCH_PARENT, 1.0f);
                if (j % cardColumns == 0) {
                    cp.rightMargin = gap / 2;
                } else {
                    cp.leftMargin = gap / 2;
                }
                if (j < count) {
                    GunButtonLayout button = new GunButtonLayout(getContext(), type, j, host);
                    buttons.add(button);
                    button.setScale(scale);
                    row.addView(button, cp);
                } else {
                    View spacer = new View(getContext());
                    spacer.setVisibility(View.INVISIBLE);
                    row.addView(spacer, cp);
                }
            }
            shopButtons.put(type, buttons);
            currentButtons = buttons;
            post(() -> publishOverlaySnapshot("rebuild"));
        }

        public void selectItem(ItemType type, int index) {
            if (type == null || index < 0) return;
            selectedType = type;
            selectedIndex = index;
            if (ItemType.values()[selectedCategory] == type) {
                selectedByOrdinal[type.ordinal()] = index;
            }
            applySelectionToCards();
            refreshDetails();
            publishOverlaySnapshot("item");
        }

        private void applySelectionToCards() {
            if (currentButtons == null) return;
            for (GunButtonLayout button : currentButtons) {
                boolean sel = button.type == selectedType && button.index == selectedIndex;
                button.setSelectState(sel);
            }
        }

        private void publishOverlaySnapshot(String trigger) {
            if (gridScroll == null || currentButtons == null || currentButtons.isEmpty()
                    || gridScroll.getWidth() <= 0 || gridScroll.getHeight() <= 0) {
                if (activeOverlay != null) {
                    activeOverlay = null;
                    lastSnapKey = null;
                }
                return;
            }
            int scrollY = gridScroll.getScrollY();

            gridScroll.getLocationInWindow(tmpLocation);
            int gridX0 = tmpLocation[0];
            int gridY0 = tmpLocation[1];
            int viewportW = gridScroll.getWidth();
            int viewportH = gridScroll.getHeight();

            List<GunButtonLayout> buttons = currentButtons;
            int count = buttons.size();
            int typeOrdinal = buttons.get(0).type.ordinal();

            GunButtonLayout first = buttons.get(0);
            int cardW = (first.isLaidOut() && first.getWidth() > 0)
                    ? first.getWidth()
                    : ShopOverlayLayout.computeCardWidth(viewportW - gridPadLeft - gridPadRight, cardColumns, cardGap);
            if (cardW <= 0) {
                activeOverlay = null;
                lastSnapKey = null;
                return;
            }
            int iconH = Math.round(first.getIconAreaHeight());
            if (iconH <= 0) {
                iconH = cardIconH;
            }

            ShopOverlayLayout.GridGeometry geometry = new ShopOverlayLayout.GridGeometry(
                    gridX0, gridY0, viewportW, viewportH,
                    gridPadLeft, gridPadTop,
                    cardColumns, cardW, cardH, cardGap, cardGap, iconH,
                    count, typeOrdinal, scrollY);

            List<ShopOverlayLayout.IconEntry> iconEntries = ShopOverlayLayout.computeGridIcons(geometry);
            SlotIcon[] grid = new SlotIcon[iconEntries.size()];
            for (int i = 0; i < iconEntries.size(); i++) {
                ShopOverlayLayout.IconEntry e = iconEntries.get(i);
                grid[i] = new SlotIcon(ItemType.values()[e.typeOrdinal], e.index, e.x, e.y, e.w, e.h, e.fullH);
            }
            List<ShopOverlayLayout.CardDesc> cards = ShopOverlayLayout.computeCards(geometry);

            SlotIcon detailsIcon = computeDetailsIcon();

            activeOverlay = new OverlaySnapshot(grid, gridX0, gridY0, viewportW, viewportH, detailsIcon, cards);

            if (SHOP_DEBUG) {
                int selOrd = selectedType != null ? selectedType.ordinal() : -1;
                int[] key = ShopOverlayLayout.buildChangeKey(cards, selOrd, selectedIndex, scrollY);
                if (OverlaySnapshotKey.changed(lastSnapKey, key, key.length)) {
                    lastSnapKey = Arrays.copyOf(key, key.length);
                    logSnapshotChange(key, trigger);
                }
            }
        }

        private SlotIcon computeDetailsIcon() {
            if (selectedType == null || selectedIndex < 0 || detailsIconView == null) return null;
            if (!detailsIconView.isLaidOut() || detailsIconView.getWidth() <= 0) return null;
            detailsIconView.getLocationInWindow(tmpLocation);
            float dw = detailsIconView.getWidth();
            float dh = detailsIconView.getHeight();
            return new SlotIcon(selectedType, selectedIndex, tmpLocation[0], tmpLocation[1], dw, dh, dh);
        }

        private void logSnapshotChange(int[] key, String trigger) {
            long now = System.currentTimeMillis();
            if (now - lastSnapshotDebugLogTime < 500L) {
                return;
            }
            lastSnapshotDebugLogTime = now;
            StringBuilder sb = new StringBuilder("[BO-Shop] snapshot changed trigger=").append(trigger).append(" sel=").append(key[0])
                    .append(" idx=").append(key[1]).append(" count=").append(key[2])
                    .append(" scroll=").append(key[3]).append(" types=[");
            int p = 4;
            int count = key[2];
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(key[p]).append(':').append(key[p + 1]);
                p += 6;
            }
            sb.append(']');
            LOGGER.info(sb.toString());
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            scale = calculateScaleFactor(width, height);

            int contentWidth = (int) (CONTENT_WIDTH * scale);
            int contentHeight = (int) (CONTENT_HEIGHT * scale);

            background.measure(
                    MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
            );

            scaleAll(scale);

            int headH = (int) (HEAD_HEIGHT * scale);
            headBar.measure(
                    MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(headH, MeasureSpec.EXACTLY)
            );

            int bodyH = contentHeight - headH;
            body.measure(
                    MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(bodyH, MeasureSpec.EXACTLY)
            );

            setMeasuredDimension(width, height);
        }

        private void scaleAll(float s) {
            int pad = (int) (BODY_PADDING * s);
            int gap = (int) (BODY_GAP * s);
            body.setPadding(pad, pad, pad, pad);

            titleText.setTextSize(18 * s);
            teamCaptionText.setTextSize(11 * s);
            moneyText.setTextSize(22 * s);
            nextRoundMinMoneyText.setTextSize(11 * s);
            cooldownChip.setTextSize(12 * s);
            cooldownChip.setPadding((int) (9 * s), (int) (2 * s), (int) (9 * s), (int) (2 * s));
            ((ViewGroup.MarginLayoutParams) titleBlock.getLayoutParams()).leftMargin = (int) (20 * s);
            ((ViewGroup.MarginLayoutParams) moneyBlock.getLayoutParams()).rightMargin = (int) (20 * s);
            if (headerDivider != null) {
                RelativeLayout.LayoutParams dlp = (RelativeLayout.LayoutParams) headerDivider.getLayoutParams();
                dlp.height = Math.max(1, (int) (BOTheme.BORDER_WIDTH * s));
                headerDivider.setLayoutParams(dlp);
            }

            LinearLayout.LayoutParams sidebarLp = (LinearLayout.LayoutParams) sidebar.getLayoutParams();
            sidebarLp.width = (int) (SIDEBAR_WIDTH * s);
            sidebarLp.rightMargin = gap;
            sidebar.setLayoutParams(sidebarLp);
            for (CategoryButton b : categoryButtons) b.setScale(s);
            for (TextView h : hintTexts) {
                h.setTextSize(11 * s);
                h.setPadding((int) (10 * s), (int) (4 * s), (int) (8 * s), 0);
            }

            LinearLayout.LayoutParams gridLp = (LinearLayout.LayoutParams) gridPanel.getLayoutParams();
            gridLp.rightMargin = gap;
            gridPanel.setLayoutParams(gridLp);
            LinearLayout.LayoutParams trLp = (LinearLayout.LayoutParams) titleRow.getLayoutParams();
            trLp.height = (int) (TITLE_ROW_H * s);
            titleRow.setLayoutParams(trLp);
            categoryTitleText.setTextSize(16 * s);
            categoryCountChip.setTextSize(11 * s);
            categoryCountChip.setPadding((int) (6 * s), (int) (2 * s), (int) (6 * s), (int) (2 * s));
            LinearLayout.LayoutParams chipLp = (LinearLayout.LayoutParams) categoryCountChip.getLayoutParams();
            chipLp.leftMargin = (int) (8 * s);
            categoryCountChip.setLayoutParams(chipLp);

            for (int r = 0; r < itemsContainer.getChildCount(); r++) {
                View rowView = itemsContainer.getChildAt(r);
                if (rowView instanceof LinearLayout rowLayout) {
                    LinearLayout.LayoutParams rp = (LinearLayout.LayoutParams) rowLayout.getLayoutParams();
                    rp.height = (int) (CARD_HEIGHT * s);
                    rp.bottomMargin = (int) (CARD_GAP * s);
                    rowLayout.setLayoutParams(rp);
                    for (int c = 0; c < rowLayout.getChildCount(); c++) {
                        View child = rowLayout.getChildAt(c);
                        if (child instanceof GunButtonLayout gunButton) {
                            gunButton.setScale(s);
                        }
                    }
                }
            }
            if (gridScroll != null) {
                gridScroll.setScrollBarSize((int) Math.max(3, 5 * s));
            }
            this.cardH = (int) (CARD_HEIGHT * s);
            this.cardGap = (int) (CARD_GAP * s);
            this.cardIconH = (int) (GunButtonLayout.ICON_AREA_HEIGHT * s);
            gridPadLeft = 0;
            gridPadTop = 0;
            gridPadRight = (int) (10 * s);
            gridPadBottom = (int) (12 * s);
            itemsContainer.setPadding(gridPadLeft, gridPadTop, gridPadRight, gridPadBottom);

            scaleDetails(s);
        }

        private void scaleDetails(float s) {
            LinearLayout.LayoutParams detailsLp = (LinearLayout.LayoutParams) detailsPanel.getLayoutParams();
            detailsLp.width = (int) (DETAILS_WIDTH * s);
            detailsPanel.setLayoutParams(detailsLp);
            detailsPanel.setPadding((int) (BODY_PADDING * s), (int) (BODY_PADDING * s), (int) (BODY_PADDING * s), (int) (BODY_PADDING * s));

            LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) detailsIconView.getLayoutParams();
            iconLp.height = (int) (DETAILS_ICON_H * s);
            detailsIconView.setLayoutParams(iconLp);

            detailsNameText.setTextSize(16 * s);
            detailsCategoryText.setTextSize(11 * s);
            detailsPriceText.setTextSize(20 * s);
            detailsOwnedText.setTextSize(12 * s);
            detailsDescText.setTextSize(12 * s);
            buyButton.setTextSize(14 * s);

            LinearLayout.LayoutParams buyLp = (LinearLayout.LayoutParams) buyButton.getLayoutParams();
            buyLp.height = (int) (BUY_BUTTON_H * s);
            buyLp.topMargin = (int) (10 * s);
            buyButton.setLayoutParams(buyLp);

            margin(detailsIconView, 0, 0, 0, (int) (8 * s));
            margin(detailsNameText, 0, (int) (4 * s), 0, 0);
            margin(detailsCategoryText, 0, (int) (2 * s), 0, 0);
            margin(detailsPriceText, 0, (int) (4 * s), 0, 0);
            margin(detailsOwnedText, 0, (int) (4 * s), 0, 0);
            margin(detailsDescText, 0, (int) (4 * s), 0, (int) (4 * s));
        }

        private void margin(View v, int l, int t, int r, int b) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams mlp) {
                mlp.leftMargin = l;
                mlp.topMargin = t;
                mlp.rightMargin = r;
                mlp.bottomMargin = b;
                v.setLayoutParams(mlp);
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            int contentWidth = (int) (CONTENT_WIDTH * scale);
            int contentHeight = (int) (CONTENT_HEIGHT * scale);
            int headH = headBar.getMeasuredHeight();

            windowLeft = (width - contentWidth) / 2;
            windowTop = (height - contentHeight) / 2;
            windowWidth = contentWidth;
            windowHeight = contentHeight;

            background.layout(windowLeft, windowTop, windowLeft + contentWidth, windowTop + contentHeight);
            headBar.layout(windowLeft, windowTop, windowLeft + contentWidth, windowTop + headH);
            body.layout(windowLeft, windowTop + headH, windowLeft + contentWidth, windowTop + contentHeight);

            int cols = computeColumns();
            if (cols != cardColumns) {
                cardColumns = cols;
                post(this::rebuildItems);
                post(this::applySelectionToCards);
            }
            post(() -> publishOverlaySnapshot("layout"));
        }



        public void updateText() {
            int money = CSClientData.getMoney();
            if (money == -1) money = 16000;
            if (money != cachedMoney) {
                cachedMoney = money;
                moneyText.setText("$ " + money);
                moneyText.setTextColor(BOTheme.mui(BOTheme.ACCENT_RGB));
            }

            boolean ct = FPSMClient.getGlobalData().isCurrentTeam("ct");
            int team = ct ? 1 : 0;
            if (team != cachedTeam) {
                cachedTeam = team;
                teamCaptionText.setText(I18n.get(ct ? "blockoffensive.shop.header.team.ct" : "blockoffensive.shop.header.team.t"));
                teamCaptionText.setTextColor(BOTheme.mui(ct ? BOTheme.ACCENT_RGB : BOTheme.STATE_WARN_RGB));
            }

            int nextMin = CSClientData.getNextRoundMinMoney();
            if (nextRoundMinMoneyText != null && nextMin != cachedNextMin) {
                cachedNextMin = nextMin;
                if (nextMin > 0) {
                    nextRoundMinMoneyText.setText(I18n.get("blockoffensive.shop.header.next_round_min", nextMin));
                    nextRoundMinMoneyText.setVisibility(View.VISIBLE);
                } else {
                    nextRoundMinMoneyText.setVisibility(View.GONE);
                }
            }

            int t = CSClientData.shopCloseTime;
            if (t != cachedCooldown) {
                cachedCooldown = t;
                if (t <= 0 || t == -1) {
                    cooldownChip.setText("");
                    cooldownChip.setVisibility(View.GONE);
                } else {
                    cooldownChip.setText(I18n.get("blockoffensive.shop.title.cooldown", t));
                    cooldownChip.setVisibility(View.VISIBLE);
                    cooldownChip.setTextColor(BOTheme.mui(t <= 10 ? BOTheme.STATE_WARN_RGB : BOTheme.TEXT_SECONDARY_RGB));
                }
            }
        }

        private void onWindowLayoutChange(View v, int left, int top, int right, int bottom,
                                         int oldLeft, int oldTop, int oldRight, int oldBottom) {
            publishOverlaySnapshot("layout-change");
        }

        private void refreshDetails() {
            boolean has = selectedType != null && selectedIndex >= 0;
            ClientShopSlot slot = has ? FPSMClient.getGlobalData().getSlotData(selectedType.name(), selectedIndex) : null;
            if (!has || slot == null || slot.itemStack().isEmpty()) {
                renderedType = selectedType;
                renderedIndex = selectedIndex;
                if (!cachedDetailsEmpty) {
                    cachedDetailsEmpty = true;
                    detailsIconView.setVisibility(View.INVISIBLE);
                    detailsNameText.setText("");
                    detailsCategoryText.setText("");
                    detailsPriceText.setText("");
                    detailsDescText.setText("");
                    detailsOwnedText.setVisibility(View.GONE);
                    buyButton.setVisibility(View.INVISIBLE);
                }
                return;
            }
            if (cachedDetailsEmpty) {
                cachedDetailsEmpty = false;
                cachedPriceCost = Integer.MIN_VALUE;
                cachedPriceColorKey = -1;
                cachedOwnedCount = -1;
                cachedBuyCost = Integer.MIN_VALUE;
                cachedBuyState = -1;
            }

            detailsIconView.setVisibility(View.VISIBLE);

            boolean changed = (selectedType != renderedType || selectedIndex != renderedIndex);
            if (changed || refreshFlag) {
                detailsNameText.setText(GunButtonLayout.displayName(slot));
                detailsCategoryText.setText(getCategoryTitle(selectedType.ordinal()));
                detailsDescText.setText(buildTooltipDesc(slot.itemStack()));
                renderedType = selectedType;
                renderedIndex = selectedIndex;
            }

            int money = CSClientData.getMoney();
            int cost = slot.cost();
            boolean affordable = money >= cost;
            boolean locked = slot.isLocked();
            boolean canBuy = CSClientData.canOpenShop && affordable && !slot.itemStack().isEmpty();

            if (cost != cachedPriceCost) {
                cachedPriceCost = cost;
                detailsPriceText.setText("$ " + cost);
            }
            int priceColorKey = locked ? 0 : (affordable ? 1 : 2);
            if (priceColorKey != cachedPriceColorKey) {
                cachedPriceColorKey = priceColorKey;
                detailsPriceText.setTextColor(BOTheme.mui(locked ? BOTheme.DISABLED_TEXT_RGB : (affordable ? BOTheme.ACCENT_RGB : BOTheme.STATE_ERROR_RGB)));
            }

            int ownedCount = slot.boughtCount();
            if (ownedCount != cachedOwnedCount) {
                cachedOwnedCount = ownedCount;
                if (ownedCount > 0) {
                    detailsOwnedText.setVisibility(View.VISIBLE);
                    detailsOwnedText.setText(I18n.get("blockoffensive.shop.details.owned", ownedCount));
                } else {
                    detailsOwnedText.setVisibility(View.GONE);
                }
            }

            buyButton.setVisibility(View.VISIBLE);
            if (cost != cachedBuyCost) {
                cachedBuyCost = cost;
                buyButton.setText(I18n.get("blockoffensive.shop.action.buy", cost));
            }
            int buyState = canBuy ? 1 : 0;
            if (buyState != cachedBuyState) {
                cachedBuyState = buyState;
                buyButton.setTextColor(BOTheme.mui(canBuy ? BOTheme.TEXT_PRIMARY_RGB : BOTheme.DISABLED_TEXT_RGB));
                if (canBuy) {
                    buyButtonBg.setColor(BOTheme.mui(BOTheme.ACCENT_STRONG_RGB));
                    buyButtonBg.setStroke(0, 0);
                } else {
                    buyButtonBg.setColor(BOTheme.muiA(0, 0x000000));
                    buyButtonBg.setStroke(Math.max(1, (int) (BOTheme.BORDER_WIDTH * scale)), BOTheme.mui(BOTheme.BORDER_STRONG_RGB));
                }
                buyButton.invalidate();
            }
        }

        private static String buildTooltipDesc(ItemStack stack) {
            if (stack.isEmpty()) return "";
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return "";
            List<Component> lines = stack.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL);
            StringBuilder sb = new StringBuilder();
            int added = 0;
            for (int i = 1; i < lines.size() && added < 6; i++) {
                String s = GunButtonLayout.stripFormatting(lines.get(i).getString());
                if (s.isBlank()) continue;
                if (added > 0) sb.append('\n');
                sb.append(s);
                added++;
            }
            return sb.toString();
        }

        public boolean handleHotkey(int keyCode) {
            if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
                selectVisibleCategory(keyCode - GLFW.GLFW_KEY_1 + 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                buyDetailsItem();
                return true;
            }
            return false;
        }

        private void selectVisibleCategory(int n) {
            List<Integer> ordinals = getVisibleOrdinals();
            if (n >= 1 && n <= ordinals.size()) {
                selectCategory(ordinals.get(n - 1));
            }
        }

        private void buyDetailsItem() {
            if (selectedType != null && selectedIndex >= 0) {
                host.attemptBuy(selectedType, selectedIndex);
            }
        }
    }
    public static class CategoryButton extends LinearLayout {
        public static final float BUTTON_HEIGHT = 44f;
        private static final float NUM_SIZE = 22f;
        private static final float BAR_WIDTH = 3f;

        public final int index;
        private final ShapeDrawable bg;
        private final ShapeDrawable numBg;
        private final View accentBar;
        private final TextView numText;
        private final TextView nameText;
        private final TextView countText;
        private boolean selected = false;
        private boolean hovered = false;

        public CategoryButton(Context context, int index, int displayNumber) {
            super(context);
            this.index = index;
            setOrientation(LinearLayout.HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            bg = new ShapeDrawable();
            bg.setShape(ShapeDrawable.RECTANGLE);
            bg.setCornerRadius(BOTheme.RADIUS_SMALL);
            bg.setColor(0);
            setBackground(bg);

            accentBar = new View(getContext());
            ShapeDrawable barShape = new ShapeDrawable();
            barShape.setShape(ShapeDrawable.RECTANGLE);
            barShape.setColor(BOTheme.mui(BOTheme.ACCENT_STRONG_RGB));
            accentBar.setBackground(barShape);
            accentBar.setVisibility(View.INVISIBLE);
            addView(accentBar, new LinearLayout.LayoutParams((int) BAR_WIDTH, MATCH_PARENT));

            numText = new TextView(getContext());
            numText.setTextSize(12);
            numText.setGravity(Gravity.CENTER);
            numText.setMaxLines(1);
            numText.setText(String.valueOf(displayNumber));
            numBg = new ShapeDrawable();
            numBg.setShape(ShapeDrawable.RECTANGLE);
            numBg.setCornerRadius(BOTheme.RADIUS_SMALL);
            numBg.setColor(BOTheme.mui(BOTheme.CARD_RGB));
            numText.setBackground(numBg);
            LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams((int) NUM_SIZE, (int) NUM_SIZE);
            numLp.leftMargin = (int) 10;
            addView(numText, numLp);

            nameText = new TextView(getContext());
            nameText.setTextSize(14);
            nameText.setMaxLines(1);
            nameText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            nameText.setTextColor(BOTheme.mui(BOTheme.TEXT_PRIMARY_RGB));
            nameText.setText(getCategoryTitle(index));
            nameText.setPadding((int) 8, 0, (int) 6, 0);
            addView(nameText, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1.0f));

            countText = new TextView(getContext());
            countText.setTextSize(12);
            countText.setMaxLines(1);
            countText.setGravity(Gravity.CENTER | Gravity.RIGHT);
            countText.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            countText.setText(String.valueOf(ItemType.values()[index].slotCount()));
            countText.setPadding(0, 0, (int) 10, 0);
            addView(countText, new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            applyState();
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            super.onHoverChanged(hovered);
            this.hovered = hovered;
            applyState();
        }

        private void applyState() {
            if (selected) {
                bg.setColor(BOTheme.mui(BOTheme.ACCENT_SOFT_RGB));
                accentBar.setVisibility(View.VISIBLE);
                numBg.setColor(BOTheme.mui(BOTheme.ACCENT_STRONG_RGB));
                numText.setTextColor(BOTheme.mui(0xFFFFFF));
            } else if (hovered) {
                bg.setColor(BOTheme.mui(BOTheme.CARD_HOVER_RGB));
                accentBar.setVisibility(View.INVISIBLE);
                numBg.setColor(BOTheme.mui(BOTheme.CARD_RGB));
                numText.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            } else {
                bg.setColor(0);
                accentBar.setVisibility(View.INVISIBLE);
                numBg.setColor(BOTheme.mui(BOTheme.CARD_RGB));
                numText.setTextColor(BOTheme.mui(BOTheme.TEXT_SECONDARY_RGB));
            }
            invalidate();
        }

        public void setScale(float scale) {
            numText.setTextSize(12 * scale);
            nameText.setTextSize(14 * scale);
            countText.setTextSize(12 * scale);

            LinearLayout.LayoutParams barLp = (LinearLayout.LayoutParams) accentBar.getLayoutParams();
            barLp.width = (int) (BAR_WIDTH * scale);
            accentBar.setLayoutParams(barLp);

            LinearLayout.LayoutParams numLp = (LinearLayout.LayoutParams) numText.getLayoutParams();
            numLp.width = (int) (NUM_SIZE * scale);
            numLp.height = (int) (NUM_SIZE * scale);
            numLp.leftMargin = (int) (10 * scale);
            numText.setLayoutParams(numLp);

            nameText.setPadding((int) (8 * scale), 0, (int) (6 * scale), 0);
            countText.setPadding(0, 0, (int) (10 * scale), 0);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
            if (params != null) {
                params.height = (int) (BUTTON_HEIGHT * scale);
                setLayoutParams(params);
            }
        }
    }

    public static class GunButtonLayout extends LinearLayout {
        public final ItemType type;
        public final int index;
        public final ShapeDrawable background;
        public final ValueAnimator backgroundAnimeFadeIn;
        public final ValueAnimator backgroundAnimeFadeOut;
        public final TextView itemNameText;
        public final TextView costText;
        private final FrameLayout iconFrame;
        private final TextView badgeText;
        private final LinearLayout bottomRow;
        private final CSGameShopScreen host;
        private boolean hoverState = false;
        private boolean selectState = false;
        private float iconAreaHeight;
        private static final float ICON_AREA_HEIGHT = 68f;
        private static final float BOTTOM_AREA_HEIGHT = 44f;

        private static String stripFormatting(String s) {
            if (s == null) return "";
            return s.replaceAll("\u00a7.", "");
        }

        private static String displayName(ClientShopSlot slot) {
            ItemStack stack = slot.itemStack();
            if (stack.isEmpty()) {
                return I18n.get("blockoffensive.shop.slot.empty");
            }
            try {
                String hover = stripFormatting(stack.getHoverName().getString());
                if (!hover.isBlank()) {
                    return hover;
                }
            } catch (Exception ignored) {
            }
            return stripFormatting(slot.name());
        }

        private static String displayCost(ClientShopSlot slot) {
            if (slot.itemStack().isEmpty()) {
                return "";
            }
            return "$ " + slot.cost();
        }

        private static int lerpRgb(int a, int b, float t) {
            int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
            int r = ar + (int) ((br - ar) * t);
            int g = ag + (int) ((bg - ag) * t);
            int bl = ab + (int) ((bb - ab) * t);
            return (r << 16) | (g << 8) | bl;
        }

        public GunButtonLayout(Context context, ItemType type, int index, CSGameShopScreen host) {
            super(context);
            this.type = type;
            this.index = index;
            this.host = host;
            this.iconAreaHeight = ICON_AREA_HEIGHT;

            setOrientation(LinearLayout.VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);

            this.background = new ShapeDrawable();
            background.setShape(ShapeDrawable.RECTANGLE);
            background.setColor(BOTheme.mui(BOTheme.CARD_RGB));
            background.setCornerRadius(BOTheme.RADIUS);
            background.setAlpha(230);
            background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB));
            setBackground(background);

            ClientShopSlot currentSlot = getSlot();

            iconFrame = new FrameLayout(getContext());

            badgeText = new TextView(getContext());
            badgeText.setTextSize(10);
            badgeText.setGravity(Gravity.CENTER);
            badgeText.setMaxLines(1);
            ShapeDrawable badgeShape = new ShapeDrawable();
            badgeShape.setShape(ShapeDrawable.RECTANGLE);
            badgeShape.setCornerRadius(BOTheme.RADIUS_SMALL);
            badgeShape.setColor(BOTheme.mui(BOTheme.CARD_RGB));
            badgeShape.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB));
            badgeText.setBackground(badgeShape);
            badgeText.setVisibility(View.GONE);
            FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
            badgeLp.topMargin = 4;
            badgeLp.rightMargin = 4;
            iconFrame.addView(badgeText, badgeLp);

            addView(iconFrame, new LinearLayout.LayoutParams(MATCH_PARENT, (int) ICON_AREA_HEIGHT));

            bottomRow = new LinearLayout(getContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);

            itemNameText = new TextView(getContext());
            itemNameText.setTextSize(13);
            itemNameText.setMaxLines(1);
            itemNameText.setSingleLine(true);
            itemNameText.setEllipsize(TextUtils.TruncateAt.END);
            itemNameText.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            itemNameText.setText(displayName(currentSlot));
            itemNameText.setPadding(8, 2, 4, 0);
            bottomRow.addView(itemNameText, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1.0f));

            costText = new TextView(getContext());
            costText.setText(displayCost(currentSlot));
            costText.setTextSize(13);
            costText.setMaxLines(1);
            costText.setSingleLine(true);
            costText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            costText.setPadding(4, 2, 8, 0);
            bottomRow.addView(costText, new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT));

            addView(bottomRow, new LinearLayout.LayoutParams(MATCH_PARENT, (int) BOTTOM_AREA_HEIGHT));

            backgroundAnimeFadeIn = ValueAnimator.ofInt(0, 100);
            backgroundAnimeFadeIn.setDuration(160);
            backgroundAnimeFadeIn.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeIn.addUpdateListener(animation -> {
                float t = (int) animation.getAnimatedValue() / 100f;
                this.background.setColor(BOTheme.mui(lerpRgb(BOTheme.CARD_RGB, BOTheme.CARD_HOVER_RGB, t)));
            });

            backgroundAnimeFadeOut = ValueAnimator.ofInt(100, 0);
            backgroundAnimeFadeOut.setDuration(160);
            backgroundAnimeFadeOut.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeOut.addUpdateListener(animation -> {
                float t = (int) animation.getAnimatedValue() / 100f;
                this.background.setColor(BOTheme.mui(lerpRgb(BOTheme.CARD_RGB, BOTheme.CARD_HOVER_RGB, t)));
            });

            setOnClickListener(v -> host.selectItem(this.type, this.index));
        }

        public ClientShopSlot getSlot() {
            return FPSMClient.getGlobalData().getSlotData(this.type.name(), this.index);
        }

        public float getIconAreaHeight() {
            return iconAreaHeight;
        }

        public void setSelectState(boolean selected) {
            if (selectState == selected) return;
            selectState = selected;
            backgroundAnimeFadeIn.cancel();
            backgroundAnimeFadeOut.cancel();
            if (selected) {
                background.setColor(BOTheme.muiA(230, BOTheme.ACCENT_SOFT_RGB));
                background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.ACCENT_RGB));
            } else {
                background.setColor(BOTheme.mui(BOTheme.CARD_RGB));
                background.setAlpha(230);
                background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB));
                hoverState = false;
            }
            invalidate();
        }

        public void updateButtonState() {
            ClientShopSlot currentSlot = this.getSlot();
            boolean affordable = CSClientData.getMoney() >= currentSlot.cost();
            boolean locked = currentSlot.isLocked();
            boolean hasItem = !currentSlot.itemStack().isEmpty();
            boolean enable = CSClientData.canOpenShop && affordable && hasItem;

            itemNameText.setTextColor(BOTheme.mui((enable && !locked) ? BOTheme.TEXT_PRIMARY_RGB : BOTheme.TEXT_SECONDARY_RGB));
            if (locked) {
                costText.setTextColor(BOTheme.mui(BOTheme.DISABLED_TEXT_RGB));
            } else {
                costText.setTextColor(BOTheme.mui(affordable ? BOTheme.ACCENT_RGB : BOTheme.STATE_ERROR_RGB));
            }

            boolean hovered = this.isHovered();
            if (selectState) {
                background.setColor(BOTheme.muiA(230, BOTheme.ACCENT_SOFT_RGB));
                background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.ACCENT_RGB));
            } else if (hovered != hoverState) {
                hoverState = hovered;
                if (hovered) {
                    background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.ACCENT_RGB));
                    backgroundAnimeFadeIn.start();
                } else {
                    background.setStroke(BOTheme.BORDER_WIDTH, BOTheme.mui(BOTheme.BORDER_RGB));
                    backgroundAnimeFadeOut.start();
                }
            }

            if (currentSlot.boughtCount() > 0) {
                badgeText.setText("\u00d7" + currentSlot.boughtCount());
                badgeText.setTextColor(BOTheme.mui(BOTheme.STATE_OK_RGB));
                badgeText.setVisibility(View.VISIBLE);
            } else if (locked) {
                badgeText.setText(I18n.get("blockoffensive.shop.badge.locked"));
                badgeText.setTextColor(BOTheme.mui(BOTheme.DISABLED_TEXT_RGB));
                badgeText.setVisibility(View.VISIBLE);
            } else {
                badgeText.setVisibility(View.GONE);
            }
        }

        public void setScale(float scale) {
            itemNameText.setTextSize(13 * scale);
            costText.setTextSize(13 * scale);
            badgeText.setTextSize(10 * scale);

            LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) iconFrame.getLayoutParams();
            iconLp.height = (int) (ICON_AREA_HEIGHT * scale);
            iconFrame.setLayoutParams(iconLp);
            iconAreaHeight = iconLp.height;

            LinearLayout.LayoutParams bottomLp = (LinearLayout.LayoutParams) bottomRow.getLayoutParams();
            bottomLp.height = (int) (BOTTOM_AREA_HEIGHT * scale);
            bottomRow.setLayoutParams(bottomLp);

            itemNameText.setPadding((int) (8 * scale), (int) (2 * scale), (int) (4 * scale), 0);
            costText.setPadding((int) (4 * scale), (int) (2 * scale), (int) (8 * scale), 0);
            badgeText.setPadding((int) (4 * scale), (int) (2 * scale), (int) (4 * scale), (int) (2 * scale));
        }

        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateButtonState();
        }
    }
}