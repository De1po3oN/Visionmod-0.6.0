package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Each control changes a single, reversible performance/visual setting. */
public final class PerformanceScreen extends Screen {
    private static final int CYAN = 0xFF48E7F6;
    private static final int TEXT = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9BAABD;
    private static final int WIDTH = 336;

    private Button presetButton, oreButton, radiusButton, budgetButton, limitButton;
    private Button cosmeticsButton, remoteButton, heartsButton, particleButton, allParticleButton, rangeButton;

    public PerformanceScreen() { super(Component.literal("Vision Mod / Performance")); }
    private static Component label(String s) { return Component.literal(s); }
    private static String enabled(boolean value) { return value ? "ON" : "OFF"; }
    private static int next(int current, int... variants) {
        for (int i = 0; i < variants.length; i++) {
            if (current == variants[i]) return variants[(i + 1) % variants.length];
        }
        return variants[0];
    }

    @Override protected void init() {
        int x = (width - WIDTH) / 2;
        int y = Math.max(6, (height - 254) / 2);
        presetButton = addRenderableWidget(Button.builder(label("PRESET"), b -> {
            switch (VisionPerformance.preset) {
                case "MAX_FPS" -> VisionPerformance.balanced();
                case "BALANCED" -> VisionPerformance.visual();
                default -> VisionPerformance.maxFps();
            }
            refresh();
        }).bounds(x + 12, y + 50, WIDTH - 24, 20).build());

        oreButton = addRenderableWidget(Button.builder(label("ORE"), b -> {
            VisionModClient.oreHighlight = !VisionModClient.oreHighlight;
            changed();
        }).bounds(x + 12, y + 78, 152, 20).build());
        radiusButton = addRenderableWidget(Button.builder(label("RADIUS"), b -> {
            VisionPerformance.oreRadius = next(VisionPerformance.oreRadius, 4, 6, 8, 12, 16);
            changed();
        }).bounds(x + 172, y + 78, 152, 20).build());

        budgetButton = addRenderableWidget(Button.builder(label("BUDGET"), b -> {
            VisionPerformance.checksPerTick = next(VisionPerformance.checksPerTick, 40, 80, 160, 320);
            changed();
        }).bounds(x + 12, y + 104, 152, 20).build());
        limitButton = addRenderableWidget(Button.builder(label("OUTLINES"), b -> {
            VisionPerformance.outlineLimit = next(VisionPerformance.outlineLimit, 32, 48, 96, 160, 240);
            changed();
        }).bounds(x + 172, y + 104, 152, 20).build());

        cosmeticsButton = addRenderableWidget(Button.builder(label("COSMETICS"), b -> {
            VisionModClient.cosmetics = !VisionModClient.cosmetics;
            changed();
        }).bounds(x + 12, y + 130, 152, 20).build());
        remoteButton = addRenderableWidget(Button.builder(label("OTHER PLAYERS"), b -> {
            VisionModClient.showOtherCosmetics = !VisionModClient.showOtherCosmetics;
            changed();
        }).bounds(x + 172, y + 130, 152, 20).build());

        heartsButton = addRenderableWidget(Button.builder(label("HEARTS"), b -> {
            VisionModClient.playerHearts = !VisionModClient.playerHearts;
            changed();
        }).bounds(x + 12, y + 156, 152, 20).build());
        rangeButton = addRenderableWidget(Button.builder(label("COSMETIC RANGE"), b -> {
            VisionPerformance.cosmeticsRange = next(VisionPerformance.cosmeticsRange, 12, 24, 48);
            changed();
        }).bounds(x + 172, y + 156, 152, 20).build());

        particleButton = addRenderableWidget(Button.builder(label("PARTICLE FILTER"), b -> {
            VisionModClient.particles = !VisionModClient.particles;
            changed();
        }).bounds(x + 12, y + 182, 152, 20).build());
        allParticleButton = addRenderableWidget(Button.builder(label("HIDE ALL"), b -> {
            VisionModClient.hideAllParticles = !VisionModClient.hideAllParticles;
            changed();
        }).bounds(x + 172, y + 182, 152, 20).build());

        addRenderableWidget(Button.builder(label("BACK TO VISION"), b -> minecraft.setScreen(new VisionScreen()))
            .bounds(x + 12, y + 214, WIDTH - 24, 20).build());
        refresh();
    }

    private void changed() { VisionPerformance.custom(); refresh(); }

    private void refresh() {
        presetButton.setMessage(label("PRESET: " + VisionPerformance.preset.replace('_', ' ') + "   >"));
        oreButton.setMessage(label("ORE OUTLINES: " + enabled(VisionModClient.oreHighlight)));
        radiusButton.setMessage(label("ORE RADIUS: " + VisionPerformance.oreRadius));
        budgetButton.setMessage(label("BLOCKS / TICK: " + VisionPerformance.checksPerTick));
        limitButton.setMessage(label("MAX OUTLINES: " + VisionPerformance.outlineLimit));
        cosmeticsButton.setMessage(label("COSMETICS: " + enabled(VisionModClient.cosmetics)));
        remoteButton.setMessage(label("OTHERS: " + enabled(VisionModClient.showOtherCosmetics)));
        heartsButton.setMessage(label("PLAYER HEARTS: " + enabled(VisionModClient.playerHearts)));
        rangeButton.setMessage(label("COSMETIC DIST: " + VisionPerformance.cosmeticsRange));
        particleButton.setMessage(label("FILTER PARTICLES: " + enabled(VisionModClient.particles)));
        allParticleButton.setMessage(label("HIDE ALL: " + enabled(VisionModClient.hideAllParticles)));
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = (width - WIDTH) / 2;
        int y = Math.max(6, (height - 254) / 2);
        g.fill(x - 4, y + 5, x + WIDTH + 4, y + 253, 0x99000000);
        g.fill(x, y, x + WIDTH, y + 248, 0xF2111624);
        g.fill(x, y, x + WIDTH / 2, y + 3, CYAN);
        g.fill(x + WIDTH / 2, y, x + WIDTH, y + 3, 0xFFAA74FF);
        g.drawString(font, "VISION / PERFORMANCE 0.7.0", x + 12, y + 13, TEXT);
        g.drawString(font, "Click any setting to cycle / toggle", x + 12, y + 31, MUTED);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() { minecraft.setScreen(new VisionScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
