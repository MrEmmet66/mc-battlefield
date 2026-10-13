package com.ptcrys.blockoffensive.client.screen;

import com.ptcrys.blockoffensive.BlockOffensive;
import com.ptcrys.blockoffensive.client.data.CSClientData;
import com.ptcrys.blockoffensive.client.renderer.ShopSlotRenderer;
import com.ptcrys.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.music.FPSClientMusicManager;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMSoundPlayC2SPacket;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.packet.shop.ShopActionC2SPacket;
import com.phasetranscrystal.fpsmatch.common.sound.FPSMSoundRegister;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.shop.ShopAction;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.ScreenCallback;
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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CSGameShopScreen extends Fragment implements ScreenCallback {
    public static final Map<ItemType, List<GunButtonLayout>> shopButtons = new HashMap<>();
    public static final String BACKGROUND = "ui/cs/background.png";
    public static final int DISABLE_TEXT_COLOR = RenderUtil.color(100, 100, 100);
    private static final String[] TOP_NAME_KEYS = new String[]{
            "blockoffensive.shop.title.weapon",
            "blockoffensive.shop.title.ammo",
            "blockoffensive.shop.title.attachment",
            "blockoffensive.shop.title.armor",
            "blockoffensive.shop.title.explosives",
            "blockoffensive.shop.title.support",
            "blockoffensive.shop.title.vehicle",
            "blockoffensive.shop.title.vehicle_ammo"
    };
    public static boolean refreshFlag = false;
    private static CSGameShopScreen INSTANCE;
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong();
    private final Map<Long, PendingShopAction> pendingActions = new HashMap<>();
    private AutoCloseable resultSubscription;
    /** Выбранная категория (сохраняется между открытиями). */
    private static int selectedCategory = 0;

    public CSGameShopScreen() {
    }

    public static float calculateScaleFactor(int w, int h) {
        return Math.min((float) w / 1920, (float) h / 1080);
    }

    public static CSGameShopScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CSGameShopScreen();
        }
        return INSTANCE;
    }

    public static String getCategoryTitle(int index) {
        ItemType[] values = ItemType.values();
        if (index < 0 || index >= values.length) index = 0;
        return I18n.get(TOP_NAME_KEYS[index]);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
        return new WindowLayout(getContext());
    }

    @Override
    public boolean isBackKey(int keyCode, KeyEvent event) {
        if (keyCode == GLFW.GLFW_KEY_B) {
            return true;
        }
        return ScreenCallback.super.isBackKey(keyCode, event);
    }

    private static GunButtonLayout hoveredButton = null;

    public static GunButtonLayout getHoveredButton() {
        return hoveredButton;
    }

    public static ItemStack getHoveredItemStack() {
        if (hoveredButton != null && hoveredButton.isShown()) {
            ClientShopSlot slot = hoveredButton.getSlot();
            if (slot != null && !slot.itemStack().isEmpty()) {
                return slot.itemStack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void onDestroyView() {
        hoveredButton = null;
        closeResultSubscription();
        pendingActions.clear();
        super.onDestroyView();
    }

    private void closeResultSubscription() {
        if (resultSubscription == null) return;
        try {
            resultSubscription.close();
        } catch (Exception ignored) {
        }
        resultSubscription = null;
    }

    private void sendShopAction(ItemType type, int index, ShopAction action) {
        String mapName = FPSMClient.getGlobalData().getCurrentMap();
        NetworkPacketRegister.getChannelFromCache(ShopActionC2SPacket.class)
                .sendToServer(new ShopActionC2SPacket(mapName, type, index, action));
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

    public static class WindowLayout extends RelativeLayout {
        private static final int CONTENT_WIDTH = 1040;
        private static final int CONTENT_HEIGHT = 580;
        private static final int HEAD_HEIGHT = 40;
        private static final int SIDEBAR_WIDTH = 250;

        private float scale = 1.0f;
        private ImageView background;
        private RelativeLayout headBar;
        private LinearLayout content;
        private LinearLayout sidebar;
        private LinearLayout mainPanel;
        private TextView categoryTitle;
        private ScrollView scrollView;
        private LinearLayout itemsContainer;

        public TextView moneyText;
        public TextView cooldownText;
        public TextView nextRoundMinMoneyText;
        private long lastRefreshMs = 0;

        public List<CategoryButton> categoryButtons = new ArrayList<>();

        public WindowLayout(Context context) {
            super(context);
            init();
        }

        private void init() {
            // 设置布局参数
            setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            background = new ImageView(getContext());
            ShapeDrawable backgroundShape = new ShapeDrawable();
            backgroundShape.setShape(ShapeDrawable.RECTANGLE);
            backgroundShape.setColor(RenderUtil.color(14, 14, 16));
            backgroundShape.setAlpha(235);
            backgroundShape.setCornerRadius(6);
            background.setImageDrawable(backgroundShape);
            addView(background);

            // 创建头部栏
            headBar = new RelativeLayout(getContext());

            // 头部栏背景: непрозрачная плашка, чтобы текст читался поверх мира
            ImageView titleBarBackground = new ImageView(getContext());
            ShapeDrawable titleBarBackgroundShape = new ShapeDrawable();
            titleBarBackgroundShape.setShape(ShapeDrawable.RECTANGLE);
            titleBarBackgroundShape.setColor(RenderUtil.color(24, 24, 28));
            titleBarBackgroundShape.setAlpha(245);
            titleBarBackgroundShape.setCornerRadius(6);
            titleBarBackground.setImageDrawable(titleBarBackgroundShape);
            titleBarBackground.setScaleType(ImageView.ScaleType.FIT_XY);
            headBar.addView(titleBarBackground, new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

            // 金钱文本
            moneyText = new TextView(getContext());
            moneyText.setTextColor(FPSMClient.getGlobalData().isCurrentTeam("ct") ? RenderUtil.color(150, 200, 250) : RenderUtil.color(234, 192, 85));
            moneyText.setTextSize(18);
            RelativeLayout.LayoutParams moneyParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            moneyParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            moneyParams.addRule(RelativeLayout.CENTER_VERTICAL);
            moneyParams.leftMargin = 25;
            headBar.addView(moneyText, moneyParams);

            // 冷却时间文本
            cooldownText = new TextView(getContext());
            cooldownText.setText(I18n.get("blockoffensive.shop.title.cooldown", CSClientData.shopCloseTime));
            cooldownText.setTextSize(18);
            RelativeLayout.LayoutParams cooldownParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            cooldownParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            headBar.addView(cooldownText, cooldownParams);

            addView(headBar);

            // Контент: сайдбар категорий + панель товаров
            content = new LinearLayout(getContext());
            content.setOrientation(LinearLayout.HORIZONTAL);

            sidebar = new LinearLayout(getContext());
            sidebar.setOrientation(LinearLayout.VERTICAL);
            for (int i = 0; i < ItemType.values().length; i++) {
                CategoryButton button = new CategoryButton(getContext(), i);
                final int index = i;
                button.setOnClickListener(v -> selectCategory(index));
                categoryButtons.add(button);
                sidebar.addView(button, new LinearLayout.LayoutParams(MATCH_PARENT, (int) CategoryButton.BUTTON_HEIGHT));
            }
            content.addView(sidebar, new LinearLayout.LayoutParams(SIDEBAR_WIDTH, MATCH_PARENT));

            mainPanel = new LinearLayout(getContext());
            mainPanel.setOrientation(LinearLayout.VERTICAL);

            categoryTitle = new TextView(getContext());
            categoryTitle.setTextColor(RenderUtil.color(203, 203, 203));
            categoryTitle.setTextSize(18);
            categoryTitle.setMaxLines(1);
            categoryTitle.setGravity(Gravity.LEFT);
            mainPanel.addView(categoryTitle, new LinearLayout.LayoutParams(MATCH_PARENT, 52));

            scrollView = new ScrollView(getContext());
            scrollView.setFillViewport(true);
            scrollView.setVerticalScrollBarEnabled(true);
            scrollView.setScrollbarFadingEnabled(false);
            ShapeDrawable thumb = new ShapeDrawable();
            thumb.setShape(ShapeDrawable.RECTANGLE);
            thumb.setColor(RenderUtil.color(140, 140, 140));
            thumb.setCornerRadius(3);
            scrollView.setVerticalScrollbarThumbDrawable(thumb);
            scrollView.setScrollBarSize(5);

            itemsContainer = new LinearLayout(getContext());
            itemsContainer.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(itemsContainer, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            mainPanel.addView(scrollView, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f));

            content.addView(mainPanel, new LinearLayout.LayoutParams(CONTENT_WIDTH - SIDEBAR_WIDTH, MATCH_PARENT));
            addView(content);

            if (selectedCategory < 0 || selectedCategory >= ItemType.values().length) {
                selectedCategory = 0;
            }
            refreshSidebarSelection();
            rebuildItems();

            // 初始更新文本
            updateText();
        }

        /** Выбор категории: подсветка + перестройка списка товаров. */
        public void selectCategory(int index) {
            if (index < 0 || index >= ItemType.values().length) return;
            selectedCategory = index;
            hoveredButton = null;
            refreshSidebarSelection();
            rebuildItems();
            if (scrollView != null) {
                scrollView.scrollTo(0, 0);
            }
        }

        private void refreshSidebarSelection() {
            for (int i = 0; i < categoryButtons.size(); i++) {
                categoryButtons.get(i).setSelected(i == selectedCategory);
            }
        }

        private void rebuildItems() {
            itemsContainer.removeAllViews();
            ItemType type = ItemType.values()[selectedCategory];
            int count = type.slotCount();
            categoryTitle.setText(getCategoryTitle(selectedCategory) + " (" + count + ")");
            List<GunButtonLayout> buttons = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                GunButtonLayout button = new GunButtonLayout(getContext(), type, j);
                buttons.add(button);
                itemsContainer.addView(button, new LinearLayout.LayoutParams(MATCH_PARENT, (int) (48 * scale)));
                button.setScale(scale);
            }
            shopButtons.put(type, buttons);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            // 计算缩放因子
            scale = calculateScaleFactor(width, height);

            int contentWidth = (int) (CONTENT_WIDTH * scale);
            int contentHeight = (int) (CONTENT_HEIGHT * scale);

            // 测量背景 - 与内容区域相同大小
            background.measure(
                    MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
            );

            // Сайдбар: кнопки категорий
            for (CategoryButton button : categoryButtons) {
                button.setScale(scale);
            }
            // Товары текущей категории
            for (int i = 0; i < itemsContainer.getChildCount(); i++) {
                View child = itemsContainer.getChildAt(i);
                if (child instanceof GunButtonLayout gunButton) {
                    gunButton.setScale(scale);
                }
            }
            categoryTitle.setTextSize(18 * scale);

            mainPanel.setPadding((int) (14 * scale), 0, 0, 0);

            LinearLayout.LayoutParams sidebarParams = (LinearLayout.LayoutParams) sidebar.getLayoutParams();
            sidebarParams.width = (int) (SIDEBAR_WIDTH * scale);
            sidebar.setLayoutParams(sidebarParams);

            LinearLayout.LayoutParams mainParams = (LinearLayout.LayoutParams) mainPanel.getLayoutParams();
            mainParams.width = contentWidth - (int) (SIDEBAR_WIDTH * scale);
            mainPanel.setLayoutParams(mainParams);

            if (scrollView != null) {
                scrollView.setScrollBarSize((int) Math.max(3, 5 * scale));
            }
            // Нижний отступ: иначе цена последнего ряда упирается в край контейнера и режется.
            itemsContainer.setPadding(0, 0, (int) (10 * scale), (int) (12 * scale));

            // 测量内容区域
            content.measure(
                    MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
            );

            // 测量头部栏
            int headBarWidth = contentWidth;
            int headBarHeight = (int) (HEAD_HEIGHT * scale);
            headBar.measure(
                    MeasureSpec.makeMeasureSpec(headBarWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(headBarHeight, MeasureSpec.EXACTLY)
            );

            // 更新文本大小
            moneyText.setTextSize(18 * scale);
            cooldownText.setTextSize(18 * scale);
            if(nextRoundMinMoneyText != null){
                nextRoundMinMoneyText.setTextSize(15 * scale);

                RelativeLayout.LayoutParams minMoneyParams = (RelativeLayout.LayoutParams) nextRoundMinMoneyText.getLayoutParams();
                minMoneyParams.rightMargin = (int) (20 * scale);
                nextRoundMinMoneyText.setLayoutParams(minMoneyParams);
            }

            // 更新边距
            RelativeLayout.LayoutParams moneyParams = (RelativeLayout.LayoutParams) moneyText.getLayoutParams();
            moneyParams.leftMargin = (int) (25 * scale);
            moneyText.setLayoutParams(moneyParams);

            // 设置自身尺寸
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;

            int contentWidth = content.getMeasuredWidth();
            int contentHeight = content.getMeasuredHeight();
            int headBarHeight = headBar.getMeasuredHeight();
            int contentLeft = (width - contentWidth) / 2;
            int headBarTop = (int) (150 * scale);
            int contentTop = headBarTop + headBarHeight + (int) (10 * scale);

            // Фон покрывает шапку + контент
            background.layout(contentLeft, headBarTop, contentLeft + contentWidth, contentTop + contentHeight);
            headBar.layout(contentLeft, headBarTop, contentLeft + contentWidth, headBarTop + headBarHeight);
            content.layout(contentLeft, contentTop, contentLeft + contentWidth, contentTop + contentHeight);
        }

        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateText();
            // Деньги и доступность кнопок прилетают с сервера асинхронно после покупки.
            // Без периодической инвалидации экран показывает stale-данные, пока мышь не двинется.
            long now = System.currentTimeMillis();
            if (now - lastRefreshMs > 250) {
                lastRefreshMs = now;
                invalidate();
            }
        }

        public void updateText() {
            int money = CSClientData.getMoney() == -1 ? 16000 : CSClientData.getMoney();
            moneyText.setText("$ " + money);
            moneyText.setTextColor(FPSMClient.getGlobalData().isCurrentTeam("ct") ? RenderUtil.color(150, 200, 250) : RenderUtil.color(234, 192, 85));
            if (nextRoundMinMoneyText != null) {
                nextRoundMinMoneyText.setText(I18n.get("blockoffensive.shop.title.min.money", CSClientData.getNextRoundMinMoney()));
            }

            if (CSClientData.shopCloseTime <= 0 || CSClientData.shopCloseTime == -1) {
                cooldownText.setText("");
            } else {
                cooldownText.setText(I18n.get("blockoffensive.shop.title.cooldown", CSClientData.shopCloseTime));
            }
        }
    }

    /** Кнопка категории в сайдбаре. */
    public static class CategoryButton extends LinearLayout {
        public static final float BUTTON_HEIGHT = 56f;
        private static final float NUM_WIDTH = 36f;

        public final int index;
        private final ShapeDrawable bg;
        private final TextView numText;
        private final TextView nameText;
        private boolean selected = false;

        public CategoryButton(Context context, int index) {
            super(context);
            this.index = index;
            setOrientation(LinearLayout.HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            bg = new ShapeDrawable();
            bg.setShape(ShapeDrawable.RECTANGLE);
            bg.setColor(RenderUtil.color(30, 30, 34));
            bg.setCornerRadius(4);
            bg.setAlpha(230);
            setBackground(bg);

            numText = new TextView(getContext());
            numText.setTextSize(14);
            numText.setMaxLines(1);
            numText.setGravity(Gravity.LEFT);
            numText.setText(String.valueOf(index + 1));
            numText.setPadding(12, 0, 0, 0);
            addView(numText, new LinearLayout.LayoutParams((int) (NUM_WIDTH + 12), WRAP_CONTENT));

            nameText = new TextView(getContext());
            nameText.setTextSize(15);
            nameText.setMaxLines(1);
            nameText.setGravity(Gravity.LEFT);
            nameText.setText(getCategoryTitle(index));
            nameText.setPadding(4, 0, 8, 0);
            addView(nameText, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.0f));
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                bg.setColor(RenderUtil.color(58, 64, 84));
                nameText.setTextColor(RenderUtil.color(234, 192, 85));
                numText.setTextColor(RenderUtil.color(234, 192, 85));
            } else {
                bg.setColor(RenderUtil.color(30, 30, 34));
                nameText.setTextColor(RenderUtil.color(203, 203, 203));
                numText.setTextColor(RenderUtil.color(140, 140, 140));
            }
            invalidate();
        }

        public void setScale(float scale) {
            numText.setTextSize(14 * scale);
            nameText.setTextSize(15 * scale);
            LinearLayout.LayoutParams numParams = (LinearLayout.LayoutParams) numText.getLayoutParams();
            numParams.width = (int) ((NUM_WIDTH + 12) * scale);
            numText.setLayoutParams(numParams);
            numText.setPadding((int) (12 * scale), 0, 0, 0);
            nameText.setPadding((int) (4 * scale), 0, (int) (8 * scale), 0);
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
        public final TextView numText;
        public final TextView itemNameText;
        public final TextView costText;
        private boolean hoverState = false;
        // Строка — горизонтальный LinearLayout: номер (фикс.) | название (вес 1) | цена (wrap).
        // Цена всегда измеряется полностью и не обрезается, название занимает остаток.
        private static final float NUM_WIDTH = 34f;

        /** Чистит §-коды форматирования (TACZ пишет цвета прямо в имя: "§a9mm"). */
        private static String stripFormatting(String s) {
            if (s == null) return "";
            return s.replaceAll("§.", "");
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

        public GunButtonLayout(Context context, ItemType type, int index) {
            super(context);
            this.type = type;
            this.index = index;

            setOrientation(LinearLayout.HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            // 设置背景
            this.background = new ShapeDrawable();
            background.setShape(ShapeDrawable.RECTANGLE);
            background.setColor(RenderUtil.color(36, 36, 36));
            background.setCornerRadius(3);
            background.setAlpha(220);
            setBackground(background);

            ClientShopSlot currentSlot = getSlot();

            // 数字文本 (слева, фиксированная ширина)
            numText = new TextView(getContext());
            numText.setTextSize(12);
            numText.setMaxLines(1);
            numText.setGravity(Gravity.LEFT);
            numText.setText(String.valueOf(this.index + 1));
            numText.setPadding(6, 0, 0, 0);
            addView(numText, new LinearLayout.LayoutParams((int) NUM_WIDTH, WRAP_CONTENT));

            // 物品名称文本 (занимает всё свободное место, одна строка)
            itemNameText = new TextView(getContext());
            itemNameText.setTextSize(12);
            itemNameText.setMaxLines(1);
            itemNameText.setGravity(Gravity.LEFT);
            itemNameText.setText(displayName(currentSlot));
            itemNameText.setPadding(4, 0, 4, 0);
            addView(itemNameText, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.0f));

            // 价格文本 (справа, wrap — всегда видна целиком)
            costText = new TextView(getContext());
            costText.setText(displayCost(currentSlot));
            costText.setTextSize(12);
            costText.setMaxLines(1);
            costText.setGravity(Gravity.RIGHT);
            costText.setPadding(0, 0, 6, 0);
            addView(costText, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

            // 背景动画
            backgroundAnimeFadeIn = ValueAnimator.ofInt(36, 68);
            backgroundAnimeFadeIn.setDuration(160);
            backgroundAnimeFadeIn.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeIn.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.background.setColor(RenderUtil.color(color, color, color));
            });

            backgroundAnimeFadeOut = ValueAnimator.ofInt(68, 36);
            backgroundAnimeFadeOut.setDuration(160);
            backgroundAnimeFadeOut.setInterpolator(TimeInterpolator.SINE);
            backgroundAnimeFadeOut.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                this.background.setColor(RenderUtil.color(color, color, color));
            });

            // 点击事件：无限购买 + 音效
            setOnClickListener((v) -> {
                ClientShopSlot slot = getSlot();
                boolean enable = CSClientData.canOpenShop && CSClientData.getMoney() >= slot.cost() && !slot.itemStack().isEmpty();
                if (enable) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getSoundManager() != null) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                    }
                    CSGameShopScreen.getInstance().sendShopAction(this.type, this.index, ShopAction.BUY);
                    // Сразу перерисоваться: деньги/доступность обновятся с сервера в течение ~250мс
                    // через авто-refresh в WindowLayout.draw, а кликнутая кнопка — мгновенно.
                    invalidate();
                }
            });
        }

        public void setElements(boolean enable) {
            if (enable) {
                int color = FPSMClient.getGlobalData().isCurrentTeam("ct") ? RenderUtil.color(150, 200, 250) : RenderUtil.color(234, 192, 85);
                numText.setTextColor(color);
                itemNameText.setTextColor(color);
                costText.setTextColor(RenderUtil.color(120, 215, 120));
            } else {
                numText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                itemNameText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
                costText.setTextColor(CSGameShopScreen.DISABLE_TEXT_COLOR);
            }
        }

        public ClientShopSlot getSlot() {
            return FPSMClient.getGlobalData().getSlotData(this.type.name(), this.index);
        }

        public void updateButtonState() {
            ClientShopSlot currentSlot = this.getSlot();
            boolean enable = CSClientData.canOpenShop && CSClientData.getMoney() >= currentSlot.cost() && !currentSlot.itemStack().isEmpty();
            this.setElements(enable);

            boolean hovered = this.isHovered();
            if (hovered != hoverState) {
                hoverState = hovered;
                if (hovered) {
                    backgroundAnimeFadeIn.start();
                } else {
                    backgroundAnimeFadeOut.start();
                }
            }

            if (refreshFlag) {
                ClientShopSlot data = getSlot();
                this.itemNameText.setText(displayName(data));
                this.costText.setText(displayCost(data));
                this.invalidate();

                ItemType[] values = ItemType.values();
                if (this.type == values[values.length - 1] && this.index == this.type.slotCount() - 1) {
                    refreshFlag = false;
                }
            }
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            super.onHoverChanged(hovered);
            if (hovered) {
                hoveredButton = this;
            } else if (hoveredButton == this) {
                hoveredButton = null;
            }
        }

        public void setScale(float scale) {
            // 更新文本尺寸
            numText.setTextSize(12 * scale);
            itemNameText.setTextSize(12 * scale);
            costText.setTextSize(12 * scale);

            // Номер — фиксированная ширина, название — вес (трогать не надо), цена — wrap
            LinearLayout.LayoutParams numParams = (LinearLayout.LayoutParams) numText.getLayoutParams();
            numParams.width = (int) (NUM_WIDTH * scale);
            numText.setLayoutParams(numParams);

            numText.setPadding((int) (6 * scale), 0, 0, 0);
            itemNameText.setPadding((int) (4 * scale), 0, (int) (4 * scale), 0);
            costText.setPadding(0, 0, (int) (6 * scale), 0);

            // Строка на всю ширину списка
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
            if (params != null) {
                params.width = MATCH_PARENT;
                params.height = (int) (48 * scale);
                setLayoutParams(params);
            }
        }

        @Override
        public void draw(@NotNull Canvas canvas) {
            super.draw(canvas);
            updateButtonState();
        }
    }
}
