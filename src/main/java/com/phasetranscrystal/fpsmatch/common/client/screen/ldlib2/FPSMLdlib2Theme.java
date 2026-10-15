package com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Vector4f;

import java.util.Locale;

/**
 * Shared visual language for FPSMatch LDLib2 work surfaces.
 * <p>
 * Dark tactical "HMS // FIELD SYSTEMS" palette with a single purple accent. Values are ARGB ints
 * ({@code 0xAARRGGBB}); the {@code // Palette} block is authoritative and the {@code // Derived}
 * block only provides hover/pressed/selection variants for the few interactive states that need
 * them.
 */
public final class FPSMLdlib2Theme {
    // ===== Palette (authoritative; base palette fixed) =====
    /** Full-screen backdrop (darkest, opaque). */
    public static final int PAGE_BG = 0xFF0F1018;
    /** Legacy app backdrop. Superseded by {@link #PAGE_BG}; kept for API compatibility. */
    public static final int BG = 0xFF1A232A;
    /** Panel surface fill. */
    public static final int SURFACE = 0xFF151722;
    /** Card fill (room rows, secondary/surface controls). */
    public static final int CARD = 0xFF1B1F2D;
    /** Card hover fill. */
    public static final int CARD_HOVER = 0xFF222739;
    /** Standard 1px border. */
    public static final int BORDER = 0xFF2A2F3F;
    /** Stronger border for hover/emphasis. */
    public static final int BORDER_STRONG = 0xFF3A4058;
    /** Brand accent (text, borders, selection). */
    public static final int ACCENT = 0xFFB18CFF;
    /** Primary action fill. */
    public static final int ACCENT_STRONG = 0xFF6F4FD8;
    /** Primary action hover fill. */
    public static final int ACCENT_STRONG_HOVER = 0xFF7E5FD9;
    /** Primary text. */
    public static final int TEXT = 0xFFE2DFF0;
    /** Secondary / muted text. */
    public static final int MUTED = 0xFF8C95AB;
    /** Disabled text. */
    public static final int DISABLED_TEXT = 0xFF565D70;
    /** Success / waiting status. */
    public static final int SUCCESS = 0xFF7EDB8F;
    public static final int WARNING = 0xFFFFB547;
    public static final int DANGER = 0xFFFF4D5A;
    /** Danger button background. */
    public static final int DANGER_BG = 0xFF3A1420;
    /** Desaturated cyan for the running-joinable room status. */
    public static final int STATUS_RUNNING_JOINABLE = 0xFF6FC3D0;
    /** Muted lavender for the spectator status. */
    public static final int STATUS_SPECTATOR = 0xFFC9B6FF;

    // ===== Derived (hover/pressed/selection variants; keep few, documented) =====
    /** Accent at 14% over CARD (selected room-card / segmented selection fill). */
    public static final int ACCENT_SOFT = 0xFF2A2540;
    /** Elevated surface; one notch above SURFACE (=== CARD). */
    public static final int ELEVATED = CARD;
    /** Recessed border for inner frames and separators. */
    public static final int BORDER_SOFT = 0xFF1E2230;
    /** Accent pressed state. */
    public static final int ACCENT_DARK = 0xFF3B2F5C;
    /** Accent lightened ~12% for legacy text-hover uses. */
    public static final int ACCENT_HOVER = 0xFFBA9AFF;
    /** Legacy disabled text alias (dimmed one notch). */
    public static final int DISABLED = DISABLED_TEXT;
    /** Legacy danger button background alias. */
    public static final int EXIT_BG = DANGER_BG;
    /** Danger button hover fill. */
    public static final int DANGER_HOVER = 0xFF573741;
    /** Danger button press fill. */
    public static final int DANGER_PRESSED = 0xFF250D17;
    /** World-dim backdrop drawn behind the floating window (translucent scrim over the game world). */
    public static final int SCRIM = 0xC0101418;

    // ===== Floating window chrome =====
    /** Window surface alpha (SURFACE RGB at ~90%). */
    public static final int WINDOW_ALPHA = 0xE6;
    /** Card fill alpha (~80%) so the window texture shows through slightly. */
    public static final int CARD_ALPHA = 0xCC;
    /** Window surface: SURFACE colour + {@link #WINDOW_ALPHA}. */
    public static final int WINDOW_BG = (WINDOW_ALPHA << 24) | (SURFACE & 0xFFFFFF);
    /** Card fill: CARD colour + {@link #CARD_ALPHA}. */
    public static final int CARD_BG = (CARD_ALPHA << 24) | (CARD & 0xFFFFFF);
    /** 1px white @6% top highlight line. */
    public static final int WINDOW_HIGHLIGHT = 0x0FFFFFFF;

    /** Corner radius (px) for panels. */
    public static final float RADIUS = 4f;
    /** Corner radius (px) for buttons/inputs/cards. */
    public static final float BUTTON_RADIUS = 3f;
    /** Corner radius (px) for cards. */
    public static final float CARD_RADIUS = 3f;
    /** Corner radius (px) for the floating window. */
    public static final float WINDOW_RADIUS = 6f;

    private FPSMLdlib2Theme() {
    }

    /**
     * Button appearance kinds.
     * <ul>
     *   <li>{@link #PRIMARY} - filled accent action.</li>
     *   <li>{@link #SECONDARY} - neutral filled action.</li>
     *   <li>{@link #QUIET} - transparent outline action (muted until hover).</li>
     *   <li>{@link #DANGER} - destructive action.</li>
     *   <li>{@link #SELECTED} - segmented/tab "selected" look that wins even when inactive.</li>
     * </ul>
     */
    public enum ButtonKind {
        PRIMARY, SECONDARY, DANGER, QUIET, SELECTED
    }

    // ===== Surfaces =====

    /**
     * Transparent app backdrop (no square fill). Screens that need a dimmed backdrop render their
     * own scrim in {@code renderBackground}, so painting an opaque square here would show a black
     * rectangle behind every rounded panel/card/button layered on top of it.
     */
    public static void root(UIElement element) {
        element.style(style -> style.background(IGuiTexture.EMPTY));
    }

    /** Flat panel: SURFACE fill + 1px BORDER frame, radius 4. */
    public static void panel(UIElement element) {
        element.style(style -> style.background(roundedTexture(SURFACE, BORDER, RADIUS)));
        clearScrollerViewport(element);
    }

    /** Elevated flat panel (content wells, secondary cards). */
    public static void elevated(UIElement element) {
        element.style(style -> style.background(roundedTexture(ELEVATED, BORDER, RADIUS)));
        clearScrollerViewport(element);
    }

    /** Header band: flat SURFACE fill, no outer frame. */
    public static void headerBar(UIElement element) {
        element.style(style -> style.background(new ColorRectTexture(SURFACE)));
    }

    /** Rounded card surface: CARD fill + 1px BORDER frame, radius 3. */
    public static void card(UIElement element) {
        element.style(style -> style.background(roundedTexture(CARD, BORDER, CARD_RADIUS)));
    }

    /** Floating window: translucent SURFACE + 1px BORDER frame, radius {@value #WINDOW_RADIUS}. */
    public static void window(UIElement element) {
        element.style(style -> style.background(roundedTexture(WINDOW_BG, BORDER, WINDOW_RADIUS)));
    }

    /** Full-screen translucent scrim (kept for screens that use an in-UI backdrop element). */
    public static void scrim(UIElement element) {
        element.style(style -> style.background(new ColorRectTexture(SCRIM)));
    }

    /** 1px top highlight line drawn just inside a window's top edge. */
    public static void windowHighlight(UIElement element) {
        element.style(style -> style.background(new ColorRectTexture(WINDOW_HIGHLIGHT)));
    }

    /** Square icon button (transparent until hover); glyph supplied as literal text (e.g. "\u00D7"). */
    public static void iconButton(Button button, String glyph) {
        button.setText(Component.literal(glyph));
        button.buttonStyle(style -> style
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(roundedTexture(CARD_HOVER, BORDER, BUTTON_RADIUS))
                .pressedTexture(roundedTexture(ACCENT_SOFT, BORDER, BUTTON_RADIUS)));
        button.textStyle(style -> style.fontSize(12).textColor(MUTED).textShadow(false));
    }

    /** Cheap soft drop shadow under a window (stacked low-alpha black, offset 2px). */
    public static void drawWindowShadow(GuiGraphics graphics, int x, int y, int width, int height) {
        int[] alphas = {0x1A000000, 0x14000000, 0x0E000000};
        int radius = Math.round(WINDOW_RADIUS);
        for (int i = 0; i < alphas.length; i++) {
            int offset = 2 + i;
            graphics.fill(x + offset + radius, y + height + 2, x + width + offset - radius, y + height + 4, alphas[i]);
            graphics.fill(x + width + 2, y + offset + radius, x + width + 4, y + height + offset - radius, alphas[i]);
        }
    }

    /**
     * Clear a {@link ScrollerView}'s inner viewport default background so the parent panel colour
     * shows through the empty area instead of LDLib2's light-gray default.
     */
    public static void clearScrollerViewport(UIElement element) {
        if (element instanceof ScrollerView scroller) {
            scroller.viewPort(port -> port.style(style -> style.background(IGuiTexture.EMPTY)));
        }
    }

    // ===== Text =====

    /** Large screen/panel title. */
    public static void title(Label label) {
        label.textStyle(style -> style.fontSize(15).textColor(TEXT).textShadow(true));
    }

    /** Window title (compact floating-window header). */
    public static void windowTitle(Label label) {
        label.textStyle(style -> style.fontSize(11).textColor(TEXT).textShadow(false));
    }

    /** Medium section heading. */
    public static void sectionTitle(Label label) {
        label.textStyle(style -> style.fontSize(11).textColor(TEXT).textShadow(false));
    }

    /** Accent brand word-mark (used for the "HMS //" half of the header logo). */
    public static void brandAccent(Label label) {
        label.textStyle(style -> style.fontSize(13).textColor(ACCENT).textShadow(true));
    }

    /** Regular half of the header logo and body copy. */
    public static void body(Label label) {
        label.textStyle(style -> style.fontSize(10).textColor(TEXT).lineSpacing(2));
    }

    /** Muted body copy. */
    public static void muted(Label label) {
        label.textStyle(style -> style.fontSize(9).textColor(MUTED).textShadow(false));
    }

    /** Tiny secondary caption (subtitle / hint text). */
    public static void caption(Label label) {
        label.textStyle(style -> style.fontSize(8).textColor(MUTED).textShadow(false));
    }

    /** Status text in the given colour. */
    public static void status(Label label, int color) {
        label.textStyle(style -> style.fontSize(9).textColor(color).textShadow(false));
    }

    /** Resolve a translatable component and uppercase it (decorative display only). */
    public static Component uppercase(Component component) {
        return Component.literal(component.getString().toUpperCase(Locale.ROOT));
    }

    /** Truncate a plain string to a pixel budget, appending an ellipsis when it overflows. */
    public static Component ellipsize(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return Component.literal("");
        }
        Font font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) {
            return Component.literal(text);
        }
        return Component.literal(font.plainSubstrByWidth(text, Math.max(1, maxWidth)));
    }

    // ===== Inputs / controls =====

    /** Rounded search/input field: CARD fill, BORDER border, ACCENT border when focused. */
    public static void input(TextField field, Component placeholder) {
        field.style(style -> style.background(roundedTexture(CARD, BORDER, BUTTON_RADIUS)));
        field.textFieldStyle(style -> style
                .fontSize(10)
                .textColor(TEXT)
                .cursorColor(ACCENT)
                .errorColor(DANGER)
                .placeholder(placeholder)
                .focusOverlay(new ColorBorderTexture(1, ACCENT)));
    }

    /**
     * Apply a button kind. When the button is currently inactive the disabled style is applied
     * instead, so call this again after {@link Button#setActive(boolean)} changes. {@link ButtonKind#SELECTED}
     * deliberately ignores the inactive state so a selected segmented control keeps its look.
     */
    public static void button(Button button, ButtonKind kind) {
        if (kind != ButtonKind.SELECTED && !button.isActive()) {
            buttonDisabled(button);
            return;
        }
        int base;
        int hover;
        int pressed;
        int text;
        int border;
        switch (kind) {
            case PRIMARY -> {
                base = ACCENT_STRONG;
                hover = ACCENT_STRONG_HOVER;
                pressed = ACCENT_STRONG;
                text = TEXT;
                border = ACCENT;
            }
            case SECONDARY -> {
                base = CARD;
                hover = CARD_HOVER;
                pressed = CARD;
                text = TEXT;
                border = BORDER;
            }
            case QUIET -> {
                base = 0;
                hover = CARD_HOVER;
                pressed = CARD;
                text = MUTED;
                border = BORDER;
            }
            case DANGER -> {
                base = DANGER_BG;
                hover = DANGER_HOVER;
                pressed = DANGER_PRESSED;
                text = DANGER;
                border = DANGER;
            }
            default -> { // SELECTED
                base = ACCENT_SOFT;
                hover = ACCENT_SOFT;
                pressed = ACCENT_SOFT;
                text = TEXT;
                border = ACCENT;
            }
        }
        int hoverBorder = kind == ButtonKind.SECONDARY || kind == ButtonKind.QUIET ? BORDER_STRONG
                : kind == ButtonKind.DANGER ? DANGER : border;
        button.buttonStyle(style -> style
                .baseTexture(roundedTexture(base, border, BUTTON_RADIUS))
                .hoverTexture(roundedTexture(hover, hoverBorder, BUTTON_RADIUS))
                .pressedTexture(roundedTexture(pressed, border, BUTTON_RADIUS)));
        button.textStyle(style -> style.fontSize(9).textColor(text).textShadow(false));
    }

    /** Disabled control: transparent fill + 50% BORDER border + DISABLED_TEXT text, no hover. */
    public static void buttonDisabled(Button button) {
        int weakBorder = (0x80 << 24) | (BORDER & 0xFFFFFF);
        IGuiTexture texture = roundedTexture(0, weakBorder, BUTTON_RADIUS);
        button.buttonStyle(style -> style
                .baseTexture(texture)
                .hoverTexture(texture)
                .pressedTexture(texture));
        button.textStyle(style -> style.fontSize(9).textColor(DISABLED_TEXT).textShadow(false));
    }

    /** Primary (accent) action button. */
    public static void accentButton(Button button) {
        button(button, ButtonKind.PRIMARY);
    }

    /** Danger (exit/leave) action button. */
    public static void dangerButton(Button button) {
        button(button, ButtonKind.DANGER);
    }

    /** Selected segmented/tab button (wins even when inactive). */
    public static void selectedButton(Button button) {
        button(button, ButtonKind.SELECTED);
    }

    /**
     * Room card row. Selected rows get an accent fill+border; otherwise the card is a translucent
     * CARD fill with a neutral border. Hover lifts to CARD_HOVER with a stronger border.
     */
    public static void roomRow(Button button, int statusColor, boolean selected) {
        int base = selected ? ACCENT_SOFT : CARD_BG;
        int border = selected ? ACCENT : BORDER;
        int hover = selected ? ACCENT_SOFT : CARD_HOVER;
        int hoverBorder = selected ? ACCENT : BORDER_STRONG;
        button.buttonStyle(style -> style
                .baseTexture(roundedTexture(base, border, CARD_RADIUS))
                .hoverTexture(roundedTexture(hover, hoverBorder, CARD_RADIUS))
                .pressedTexture(roundedTexture(ACCENT_SOFT, ACCENT, CARD_RADIUS)));
    }

    /** Neutral tab control. */
    public static void tab(Tab tab) {
        tab.tabStyle(style -> style
                .baseTexture(roundedTexture(CARD, BORDER, BUTTON_RADIUS))
                .hoverTexture(roundedTexture(CARD_HOVER, BORDER_STRONG, BUTTON_RADIUS))
                .selectedTexture(roundedTexture(ACCENT_SOFT, ACCENT, BUTTON_RADIUS)));
        tab.textStyle(style -> style.fontSize(9).textColor(TEXT).textShadow(false));
    }

    /** Item slot. */
    public static void slot(ItemSlot slot) {
        slot.style(style -> style.background(panelTexture(CARD, BORDER)));
        slot.slotStyle(style -> style
                .slotOverlay(new ColorBorderTexture(1, BORDER))
                .hoverOverlay(new ColorRectTexture((0x30 << 24) | (ACCENT & 0xFFFFFF)))
                .showItemTooltips(true));
    }

    // ===== Textures =====

    /** Flat fill + 1px border frame. */
    public static IGuiTexture panelTexture(int fill, int border) {
        return IGuiTexture.group(new ColorRectTexture(fill), new ColorBorderTexture(1, border));
    }

    /** Rounded fill + 1px border frame (SDF; {@code radius} in px). */
    public static IGuiTexture roundedTexture(int fill, int border, float radius) {
        return SDFRectTexture.of(fill).setBorderColor(border).setStroke(1f).setRadius(radius);
    }

    /**
     * Joined segmented-control segment background. Rounds only the corners facing the control's
     * outer edge so adjacent segments share a seamless seam while the outer corners stay rounded.
     */
    public static IGuiTexture segmentedTexture(int fill, int border, float radius, boolean leftEdge, boolean rightEdge) {
        SDFRectTexture texture = SDFRectTexture.of(fill).setBorderColor(border).setStroke(1f);
        if (leftEdge && rightEdge) {
            texture.setRadius(radius);
        } else {
            texture.setRadius(new Vector4f(leftEdge ? radius : 0, rightEdge ? radius : 0,
                    rightEdge ? radius : 0, leftEdge ? radius : 0));
        }
        return texture;
    }

    /**
     * Style a {@link ScrollerView} scrollbar for content-hiding use: hidden when content fits
     * (auto display), and a slim accent bar with no arrows when it scrolls.
     */
    public static void slimScroller(ScrollerView scroller) {
        scroller.scrollerStyle(style -> style
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .mode(ScrollerMode.VERTICAL));
        int accent = (0x99 << 24) | (ACCENT & 0xFFFFFF);
        IGuiTexture bar = SDFRectTexture.of(accent).setRadius(2f);
        scroller.verticalScroller(s -> {
            s.headButton(b -> b.setVisible(false));
            s.tailButton(b -> b.setVisible(false));
            s.scrollerStyle(st -> st.scrollBarSize(3f));
            s.scrollBar(b -> b.buttonStyle(bs -> bs
                    .baseTexture(bar).hoverTexture(bar).pressedTexture(bar)));
            s.scrollContainer(c -> c.style(st -> st.background(IGuiTexture.EMPTY)));
        });
    }
}