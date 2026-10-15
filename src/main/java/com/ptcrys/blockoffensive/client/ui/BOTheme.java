package com.ptcrys.blockoffensive.client.ui;

import com.phasetranscrystal.fpsmatch.util.RenderUtil;

/**
 * Единая палитра и правила оформления для клиентских UI (Block Offensive CS).
 *
 * <p>Содержит один и тот же набор цветов в двух кодировках:
 * <ul>
 *   <li><b>RGB</b> ({@code 0xRRGGBB}, без альфы) — для собственных вычислений
 *       (смешивание, {@link #argb(int, int)}, {@link #mui(int)});</li>
 *   <li><b>ARGB</b> (непрозрачные {@code 0xFFRRGGBB}) — для vanilla
 *       {@code GuiGraphics} и любых вызовов, ожидающих альфа-канал.</li>
 * </ul>
 *
 * <p>Палитра:</p>
 * <table border="1">
 *   <caption>Цвета BOTheme</caption>
 *   <tr><th>Константа</th><th>RGB</th><th>Назначение</th></tr>
 *   <tr><td>{@code PANEL}</td><td>{@code 0x11111A}</td><td>фон панели</td></tr>
 *   <tr><td>{@code BACKGROUND}</td><td>{@code 0x1A232A}</td><td>общий фон</td></tr>
 *   <tr><td>{@code BORDER}</td><td>{@code 0x2A2F3F}</td><td>рамка 1px</td></tr>
 *   <tr><td>{@code ACCENT}</td><td>{@code 0xB18CFF}</td><td>цены, выделение</td></tr>
 *   <tr><td>{@code TEXT_PRIMARY}</td><td>{@code 0xE2DFF0}</td><td>основной текст</td></tr>
 *   <tr><td>{@code TEXT_SECONDARY}</td><td>{@code 0x8C95AB}</td><td>второстепенный текст</td></tr>
 *   <tr><td>{@code STATE_OK}</td><td>{@code 0x7EDB8F}</td><td>статус "успех/доступно"</td></tr>
 *   <tr><td>{@code STATE_INFO}</td><td>{@code 0xB18CFF}</td><td>статус "инфо" (= ACCENT)</td></tr>
 *   <tr><td>{@code STATE_WARN}</td><td>{@code 0xFFB547}</td><td>статус "предупреждение"</td></tr>
 *   <tr><td>{@code STATE_ERROR}</td><td>{@code 0xFF4D5A}</td><td>статус "ошибка"</td></tr>
 *   <tr><td>{@code EXIT_BG}</td><td>{@code 0x3A1420}</td><td>фон кнопки выхода</td></tr>
 * </table>
 *
 * <p>Правила стиля:</p>
 * <ul>
 *   <li>панели — плоские, без объёмных градиентов и теней;</li>
 *   <li>рамка — {@code 1px} цвета {@code BORDER} по контуру панелей и кнопок;</li>
 *   <li>заголовки — ВЕРХНИМ РЕГИСТРОМ;</li>
 *   <li>цены и активное/выбранное выделение — {@code ACCENT};</li>
 *   <li>состояния (успех/инфо/предупреждение/ошибка) — {@code STATE_*}.</li>
 * </ul>
 */
public final class BOTheme {

    /* ============================ RGB (0xRRGGBB) ============================ */

    /** Фоновый цвет панелей. */
    public static final int PANEL_RGB = 0x11111A;
    /** Общий цвет фона. */
    public static final int BACKGROUND_RGB = 0x1A232A;
    /** Цвет рамки (1px). */
    public static final int BORDER_RGB = 0x2A2F3F;
    /** Акцентный цвет (цены, выделение активных элементов). */
    public static final int ACCENT_RGB = 0xB18CFF;
    /** Цвет основного текста. */
    public static final int TEXT_PRIMARY_RGB = 0xE2DFF0;
    /** Цвет второстепенного текста. */
    public static final int TEXT_SECONDARY_RGB = 0x8C95AB;
    /** Цвет статуса "успех/доступно". */
    public static final int STATE_OK_RGB = 0x7EDB8F;
    /** Цвет статуса "инфо" (совпадает с акцентом). */
    public static final int STATE_INFO_RGB = ACCENT_RGB;
    /** Цвет статуса "предупреждение". */
    public static final int STATE_WARN_RGB = 0xFFB547;
    /** Цвет статуса "ошибка". */
    public static final int STATE_ERROR_RGB = 0xFF4D5A;
    /** Цвет фона кнопки выхода. */
    public static final int EXIT_BG_RGB = 0x3A1420;
    /** Card background color. */
    public static final int CARD_RGB = 0x1B1F2D;
    /** Card hover background color. */
    public static final int CARD_HOVER_RGB = 0x222739;
    /** Strong border color. */
    public static final int BORDER_STRONG_RGB = 0x3A4058;
    /** Strong accent color (button fills / selection). */
    public static final int ACCENT_STRONG_RGB = 0x6F4FD8;
    /** Soft accent color (selected category backing). */
    public static final int ACCENT_SOFT_RGB = 0x2A2540;
    /** Disabled text color. */
    public static final int DISABLED_TEXT_RGB = 0x565D70;
    /* ==================== ARGB (0xFF..., для vanilla GUI) =================== */

    /** Непрозрачный {@link #PANEL_RGB} для vanilla {@code GuiGraphics}. */
    public static final int PANEL = 0xFF11111A;
    /** Непрозрачный {@link #BACKGROUND_RGB} для vanilla {@code GuiGraphics}. */
    public static final int BACKGROUND = 0xFF1A232A;
    /** Непрозрачный {@link #BORDER_RGB} для vanilla {@code GuiGraphics}. */
    public static final int BORDER = 0xFF2A2F3F;
    /** Непрозрачный {@link #ACCENT_RGB} для vanilla {@code GuiGraphics}. */
    public static final int ACCENT = 0xFFB18CFF;
    /** Непрозрачный {@link #TEXT_PRIMARY_RGB} для vanilla {@code GuiGraphics}. */
    public static final int TEXT_PRIMARY = 0xFFE2DFF0;
    /** Непрозрачный {@link #TEXT_SECONDARY_RGB} для vanilla {@code GuiGraphics}. */
    public static final int TEXT_SECONDARY = 0xFF8C95AB;
    /** Непрозрачный {@link #STATE_OK_RGB} для vanilla {@code GuiGraphics}. */
    public static final int STATE_OK = 0xFF7EDB8F;
    /** Непрозрачный {@link #STATE_INFO_RGB} для vanilla {@code GuiGraphics}. */
    public static final int STATE_INFO = 0xFFB18CFF;
    /** Непрозрачный {@link #STATE_WARN_RGB} для vanilla {@code GuiGraphics}. */
    public static final int STATE_WARN = 0xFFFFB547;
    /** Непрозрачный {@link #STATE_ERROR_RGB} для vanilla {@code GuiGraphics}. */
    public static final int STATE_ERROR = 0xFFFF4D5A;
    /** Непрозрачный {@link #EXIT_BG_RGB} для vanilla {@code GuiGraphics}. */
    public static final int EXIT_BG = 0xFF3A1420;
    /** Полупрозрачная панель (alpha {@code 0xE6}) surface {@link #PANEL_RGB}. */
    public static final int PANEL_TRANSLUCENT = 0xE611111A;

    /* ============================ Размеры =================================== */

    /** Малый радиус скругления углов. */
    public static final int RADIUS_SMALL = 3;
    /** Стандартный радиус скругления углов. */
    public static final int RADIUS = 4;
    /** Большой радиус скругления углов. */
    public static final int RADIUS_LARGE = 6;
    /** Толщина рамки. */
    public static final int BORDER_WIDTH = 1;

    private BOTheme() {
    }

    /* ============================== Утилиты ================================= */

    /**
     * Собирает ARGB-цвет из 8-битного альфа-канала и 24-битных RGB-компонент
     * ({@code 0xRRGGBB}, без альфы).
     *
     * @param alpha альфа-канал 0..255 (0 — полностью прозрачный, 255 — непрозрачный)
     * @param rgb   цвет {@code 0xRRGGBB}
     * @return {@code 0xAARRGGBB}
     */
    public static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * Подменяет альфа-канал у готового ARGB-цвета {@code 0xAARRGGBB},
     * не трогая RGB-компоненты.
     *
     * @param argb  исходный цвет {@code 0xAARRGGBB}
     * @param alpha новый альфа-канал 0..255
     * @return {@code 0xAARRGGBB} с новым альфа
     */
    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0xFFFFFF);
    }

    /**
     * Осветляет RGB-цвет смешиванием к белому. Используется для состояния
     * наведения (hover): {@code amount} 0..1 означает долю белого в результате.
     *
     * @param rgb    исходный цвет {@code 0xRRGGBB} (альфа не учитывается)
     * @param amount доля осветления 0..1
     * @return результирующий цвет {@code 0xRRGGBB}
     */
    public static int lighten(int rgb, float amount) {
        if (amount <= 0.0f) return rgb;
        if (amount >= 1.0f) return 0xFFFFFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int nr = r + (int) ((255 - r) * amount);
        int ng = g + (int) ((255 - g) * amount);
        int nb = b + (int) ((255 - b) * amount);
        return (nr << 16) | (ng << 8) | nb;
    }

    /**
     * Цвет для Modern UI ({@code icyllis.modernui}) — переводит {@code 0xRRGGBB}
     * в ARGB-представление, принятое в {@code ShapeDrawable}, {@code TextView} и т.п.
     *
     * @param rgb цвет {@code 0xRRGGBB}
     * @return непрозрачный ARGB-цвет ({@code 0xFFRRGGBB})
     */
    public static int mui(int rgb) {
        return RenderUtil.color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Цвет для Modern UI с явным альфа-каналом: {@code ShapeDrawable.setColor}
     * принимает ARGB, поэтому итог собирается как {@code 0xAARRGGBB}.
     *
     * @param alpha альфа-канал 0..255
     * @param rgb   цвет {@code 0xRRGGBB}
     * @return {@code 0xAARRGGBB}
     */
    public static int muiA(int alpha, int rgb) {
        return argb(alpha, rgb);
    }
}