package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Compact main page with controls reachable on a small GUI-scale window. */
public final class VisionScreen extends Screen {
    private static final int BG = 0xF2111624;
    private static final int ROW = 0xF0242E43;
    private static final int HOVER = 0xFF36455F;
    private static final int TEXT = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9BAABD;
    private static final int CYAN = 0xFF48E7F6;
    private static final int PURPLE = 0xFFAA74FF;
    private static final String[] MODULES = {
        "FullBright", "Ore Highlight", "Clear Water", "Clear Lava",
        "Player Hearts", "Particles", "Free Look"
    };

    private boolean orePage;
    private long openedAt = System.nanoTime();

    public VisionScreen() { this(false); }
    public VisionScreen(boolean showOres) {
        super(Component.literal("Vision Mod"));
        orePage = showOres;
    }

    private int panelW() { return 336; }
    private int panelH() { return orePage ? 236 : 228; }
    private int x() { return (width - panelW()) / 2; }
    private int y() {
        float p = Math.max(0f, Math.min(1f, (System.nanoTime() - openedAt) / 230_000_000f));
        p = 1f - (1f - p) * (1f - p) * (1f - p);
        int destination = Math.max(4, (height - panelH()) / 2);
        return Math.round((height - 32) * (1f - p) + destination * p);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean enabled(int index) {
        return switch (index) {
            case 0 -> VisionModClient.fullBright;
            case 1 -> VisionModClient.oreHighlight;
            case 2 -> VisionModClient.clearWater;
            case 3 -> VisionModClient.clearLava;
            case 4 -> VisionModClient.playerHearts;
            case 5 -> VisionModClient.particles;
            case 6 -> FreeLook.enabled;
            default -> false;
        };
    }

    private void toggle(int index) {
        switch (index) {
            case 0 -> VisionModClient.fullBright = !VisionModClient.fullBright;
            case 1 -> VisionModClient.oreHighlight = !VisionModClient.oreHighlight;
            case 2 -> VisionModClient.clearWater = !VisionModClient.clearWater;
            case 3 -> VisionModClient.clearLava = !VisionModClient.clearLava;
            case 4 -> VisionModClient.playerHearts = !VisionModClient.playerHearts;
            case 5 -> VisionModClient.particles = !VisionModClient.particles;
            case 6 -> FreeLook.enabled = !FreeLook.enabled;
            default -> { return; }
        }
        VisionPerformance.custom();
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x = x(), y = y(), w = panelW(), h = panelH();
        g.fill(x - 3, y + 5, x + w + 3, y + h + 6, 0x88000000);
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w / 2, y + 3, CYAN);
        g.fill(x + w / 2, y, x + w, y + 3, PURPLE);
        g.drawString(font, orePage ? "VISION / ORES" : "VISION MOD   0.7.0", x + 12, y + 13, TEXT);
        g.drawString(font, orePage ? "LMB: toggle ore  |  RMB: edit color" :
            "Right Shift: menu | Left Alt: Free Look", x + 12, y + 30, MUTED);
        g.fill(x + 10, y + 45, x + w - 10, y + 46, 0xFF415069);
        if (orePage) renderOres(g, mx, my, x, y);
        else renderModules(g, mx, my, x, y);
    }

    private void renderModules(GuiGraphics g, int mx, int my, int x, int y) {
        for (int i = 0; i < MODULES.length; i++) {
            int bx = x + 10 + (i % 2) * 160;
            int by = y + 52 + (i / 2) * 30;
            boolean hover = in(mx, my, bx, by, 155, 25);
            boolean on = enabled(i);
            g.fill(bx, by, bx + 155, by + 25, hover ? HOVER : ROW);
            g.fill(bx + 2, by + 3, bx + 5, by + 22, on ? CYAN : 0xFF54647D);
            g.drawString(font, MODULES[i], bx + 9, by + 8, TEXT);
            g.drawString(font, on ? "ON" : "OFF", bx + 130, by + 8, on ? CYAN : MUTED);
        }
        button(g, mx, my, x + 10, y + 175, 155, 21, "PERFORMANCE / FPS");
        button(g, mx, my, x + 170, y + 175, 155, 21, "CONFIGS / PROFILES");
        g.drawCenteredString(font, "RMB Ore Highlight: ore selection", x + 168, y + 209, MUTED);
    }

    private void renderOres(GuiGraphics g, int mx, int my, int x, int y) {
        for (int i = 0; i < OreColors.NAMES.length; i++) {
            int bx = x + 10 + (i / 5) * 160;
            int by = y + 52 + (i % 5) * 29;
            g.fill(bx, by, bx + 155, by + 25, in(mx, my, bx, by, 155, 25) ? HOVER : ROW);
            g.fill(bx + 5, by + 7, bx + 17, by + 19, OreColors.getColor(i));
            g.drawString(font, OreColors.NAMES[i], bx + 22, by + 8, TEXT);
            boolean on = OreSelection.enabled(i);
            g.drawString(font, on ? "ON" : "OFF", bx + 130, by + 8, on ? CYAN : MUTED);
        }
        button(g, mx, my, x + 10, y + 202, 315, 22, "BACK TO MODULES");
    }

    private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label) {
        g.fill(x, y, x + w, y + h, in(mx, my, x, y, w, h) ? HOVER : 0xFF205469);
        g.drawCenteredString(font, label, x + w / 2, y + 7, TEXT);
    }

    @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int x = x(), y = y();
        double mx = click.x(), my = click.y();
        boolean left = click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if (orePage) {
            for (int i = 0; i < OreColors.NAMES.length; i++) {
                int bx = x + 10 + (i / 5) * 160;
                int by = y + 52 + (i % 5) * 29;
                if (in(mx, my, bx, by, 155, 25)) {
                    if (left) { OreSelection.toggle(i); return true; }
                    if (right) { minecraft.setScreen(new OreColorPickerScreen(i)); return true; }
                }
            }
            if (left && in(mx, my, x + 10, y + 202, 315, 22)) {
                orePage = false;
                openedAt = System.nanoTime();
                return true;
            }
        } else {
            if (left && in(mx, my, x + 10, y + 175, 155, 21)) {
                minecraft.setScreen(new PerformanceScreen());
                return true;
            }
            if (left && in(mx, my, x + 170, y + 175, 155, 21)) {
                minecraft.setScreen(new ConfigsScreen());
                return true;
            }
            for (int i = 0; i < MODULES.length; i++) {
                int bx = x + 10 + (i % 2) * 160;
                int by = y + 52 + (i / 2) * 30;
                if (!in(mx, my, bx, by, 155, 25)) continue;
                if (left) { toggle(i); return true; }
                if (right && i == 1) { orePage = true; openedAt = System.nanoTime(); return true; }
                if (right && i == 5) { minecraft.setScreen(new ParticleSettingsScreen()); return true; }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean isPauseScreen() { return false; }
}
