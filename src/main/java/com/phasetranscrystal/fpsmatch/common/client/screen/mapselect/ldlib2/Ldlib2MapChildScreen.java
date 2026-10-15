package com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.ldlib2;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.phasetranscrystal.fpsmatch.common.client.screen.ldlib2.FPSMLdlib2Theme;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.phasetranscrystal.fpsmatch.common.client.screen.mapselect.FPSMMapDetailChildScreen;
import com.phasetranscrystal.fpsmatch.common.packet.mapselect.MapRoomDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.Objects;

/** Shared lifecycle for LDLib2 map-room child pages, drawn as compact centred translucent windows. */
public abstract class Ldlib2MapChildScreen extends ModularUIScreen implements FPSMMapDetailChildScreen {
    protected final Screen parent;
    protected MapRoomDetail detail;

    protected Ldlib2MapChildScreen(ModularUI ui, Component title, MapRoomDetail detail, Screen parent) {
        super(ui, title);
        this.detail = Objects.requireNonNull(detail, "detail");
        this.parent = parent;
    }

    @Override
    public void applyDetail(MapRoomDetail detail) {
        this.detail = Objects.requireNonNull(detail, "detail");
        onDetailApplied();
    }

    protected void onDetailApplied() {
    }

    @Override
    public void onClose() {
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

    /** Compact window chrome: translucent floating root with a 30px header (title + subtitle + close) + 1px divider. */
    protected record Chrome(UIElement root, UIElement headerBar, Label title, Label subtitle, Button close,
                            UIElement divider) {
    }

    /** Build a compact window and its header chrome. Positions are window-relative and size-independent. */
    protected static Chrome chrome(String rootId, Component titleText) {
        UIElement root = new UIElement().setId(rootId);
        root.layout(l -> l.widthPercent(100).heightPercent(100));
        FPSMLdlib2Theme.window(root);

        UIElement highlight = new UIElement().setId(rootId + ".highlight");
        highlight.setAllowHitTest(false);
        FPSMLdlib2Theme.windowHighlight(highlight);
        highlight.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(1).top(1).right(1).height(1));

        UIElement headerBar = new UIElement().setId(rootId + ".header");
        headerBar.setAllowHitTest(false);
        headerBar.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(0).top(0).right(0).height(MapSelectionLayoutModel.HEADER_HEIGHT));

        Label title = new Label();
        title.setId(rootId + ".header.title");
        title.setValue(titleText);
        FPSMLdlib2Theme.windowTitle(title);
        title.setAllowHitTest(false);
        title.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).top(5).right(40).height(15));

        Label subtitle = new Label();
        subtitle.setId(rootId + ".header.subtitle");
        subtitle.setValue(Component.empty());
        FPSMLdlib2Theme.caption(subtitle);
        subtitle.setAllowHitTest(false);
        subtitle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(10).top(20).right(40).height(9));

        Button close = new Button();
        close.setId(rootId + ".header.close");
        FPSMLdlib2Theme.iconButton(close, "\u00D7");
        close.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(8).top(4).width(22).height(22));

        UIElement divider = new UIElement().setId(rootId + ".header.divider");
        divider.setAllowHitTest(false);
        divider.style(style -> style.background(new ColorRectTexture(FPSMLdlib2Theme.BORDER)));
        divider.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(0).right(0).top(MapSelectionLayoutModel.HEADER_HEIGHT).height(1));

        headerBar.addChildren(title, subtitle, close, divider);
        root.addChildren(highlight, headerBar);
        return new Chrome(root, headerBar, title, subtitle, close, divider);
    }
}