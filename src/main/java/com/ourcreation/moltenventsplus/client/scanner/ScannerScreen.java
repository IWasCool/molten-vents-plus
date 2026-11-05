package com.ourcreation.moltenventsplus.client.scanner;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.scanner.ScannerTarget;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** A small mouse-drag material wheel with an uninterrupted background. */
public final class ScannerScreen extends Screen {
    private static final int RADIUS = 50, INNER = 18, ICON_RADIUS = 34;
    private static final double SECTOR = Math.PI * 2 / 6;
    private static final ResourceLocation WHEEL = ResourceLocation.fromNamespaceAndPath(
            MoltenVentsPlus.MODID, "textures/gui/surveyor_wheel.png");
    private static final ResourceLocation WHEEL_SELECTED = ResourceLocation.fromNamespaceAndPath(
            MoltenVentsPlus.MODID, "textures/gui/surveyor_wheel_selected.png");
    private final VentTier tier;
    private final List<ScannerTarget> targets;
    private final Button[] choices = new Button[6];
    private int cx, cy, hovered = -1;
    private boolean keyboard;

    public ScannerScreen(VentTier tier) {
        super(Component.translatable("item.create_water_vents." + switch (tier) {
            case WATER -> "vent_surveyor";
            case MOLTEN -> "molten_vent_surveyor";
            case SUPERHEATED -> "superheated_vent_surveyor";
        }));
        this.tier = tier;
        this.targets = ScannerTarget.forTier(tier);
    }

    @Override protected void init() {
        ScannerClient.stopScan();
        cx = width / 2;
        cy = height / 2;
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get(i);
            double angle = -Math.PI / 2 + i * SECTOR;
            int x = cx + (int) Math.round(Math.cos(angle) * ICON_RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * ICON_RADIUS);
            var label = Component.translatable("scanner.create_water_vents.tier_option", target.label(),
                    Component.translatable("scanner.create_water_vents.distance", target.radius()));
            choices[i] = addRenderableWidget(new Button(x - 17, y - 14, 34, 28, label,
                    b -> confirm(target), narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {}
            });
        }
    }

    private static int sector(double x, double y) {
        double angle = (Math.atan2(y, x) + Math.PI / 2 + SECTOR / 2 + Math.PI * 2) % (Math.PI * 2);
        return Math.min(5, (int) (angle / SECTOR));
    }

    private void point(double x, double y) {
        keyboard = false;
        double dx = x - cx, dy = y - cy;
        double radius = dx * dx + dy * dy;
        hovered = radius < INNER * INNER ? -1 : sector(dx, dy);
    }

    private void confirm(ScannerTarget target) {
        if (target.tier() == tier && ScannerClient.canScan()) ScannerClient.scan(target);
    }

    @Override public void mouseMoved(double x, double y) { point(x, y); super.mouseMoved(x, y); }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        point(x, y);
        return button == 0 || button == 1 || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 1 && hasShiftDown()) { ScannerClient.stopScan(); onClose(); return true; }
        if (button == 0 || button == 1) { point(x, y); return true; }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 1 && hasShiftDown()) { ScannerClient.stopScan(); onClose(); return true; }
        if (button == 0 || button == 1) {
            point(x, y);
            if (hovered >= 0) confirm(targets.get(hovered));
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_6) {
            hovered = key - GLFW.GLFW_KEY_1; keyboard = true;
            setFocused(choices[hovered]);
            confirm(targets.get(hovered));
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            int step = key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP ? -1 : 1;
            hovered = hovered < 0 ? 0 : Math.floorMod(hovered + step, 6);
            keyboard = true; setFocused(choices[hovered]); return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) && hovered >= 0) {
            confirm(targets.get(hovered)); return true;
        }
        boolean handled = super.keyPressed(key, scanCode, modifiers);
        if (key == GLFW.GLFW_KEY_TAB) {
            keyboard = true;
            for (int i = 0; i < choices.length; i++) if (getFocused() == choices[i]) hovered = i;
        }
        return handled;
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (!keyboard) point(mouseX, mouseY);
        int size = RADIUS * 2 + 1;
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, 1);
        g.blit(hovered < 0 ? WHEEL : WHEEL_SELECTED, cx - RADIUS, cy - RADIUS,
                0, hovered < 0 ? 0 : hovered * size, size, size, size, hovered < 0 ? size : size * 6);
        RenderSystem.disableBlend();
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get(i);
            double angle = -Math.PI / 2 + i * SECTOR;
            int x = cx + (int) Math.round(Math.cos(angle) * ICON_RADIUS) - 8;
            int y = cy + (int) Math.round(Math.sin(angle) * ICON_RADIUS) - 8;
            g.renderItem(target.icon(), x, y);
        }
        line(g, title, cy - RADIUS - 18, 0xFFDCDFD9);
    }

    private void line(GuiGraphics g, Component text, int y, int color) {
        var clipped = font.substrByWidth(text, Math.max(40, width - 20));
        g.drawCenteredString(font, Language.getInstance().getVisualOrder(clipped), cx, y, color);
    }

    @Override public boolean isPauseScreen() { return false; }
}
