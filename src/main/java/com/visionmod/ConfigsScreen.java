package com.visionmod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Create, import, activate and remove shared Vision Mod configuration profiles. */
public final class ConfigsScreen extends Screen {
    private static final int ACCENT = 0xFF48E7F6;
    private static final int WHITE = 0xFFF0F6FF;
    private static final int MUTED = 0xFF9BAABD;
    private EditBox nameInput;
    private Button selectedButton;
    private List<String> profiles = List.of();
    private int selected = 0;
    private String status = "Save a profile or place a .json file in the folder";

    public ConfigsScreen() { super(Component.literal("Vision Mod - Configs")); }

    private void refresh() {
        String previously = profiles.isEmpty() ? null : profiles.get(Math.min(selected, profiles.size()-1));
        profiles = VisionProfiles.list();
        selected = Math.max(0, previously == null ? 0 : profiles.indexOf(previously));
        if (selectedButton != null) selectedButton.setMessage(Component.literal("PROFILE: " + current() + "   >"));
    }

    private String current() { return profiles.isEmpty() ? "(none)" : profiles.get(Math.min(selected, profiles.size()-1)); }

    @Override protected void init() {
        int x = (width-278)/2;
        int y = Math.max(8, (height-286)/2);
        nameInput = new EditBox(font, x+16, y+60, 246, 20, Component.literal("Profile name"));
        nameInput.setMaxLength(40);
        nameInput.setHint(Component.literal("Name: my_pvp_config"));
        addRenderableWidget(nameInput);
        selectedButton = addRenderableWidget(Button.builder(Component.literal("PROFILE: (none)   >"), b -> {
            refresh();
            if (!profiles.isEmpty()) selected = (selected+1) % profiles.size();
            selectedButton.setMessage(Component.literal("PROFILE: " + current() + "   >"));
        }).bounds(x+16,y+90,246,20).build());
        addRenderableWidget(Button.builder(Component.literal("SAVE CURRENT SETTINGS"), b -> {
            try { VisionProfiles.save(nameInput.getValue().trim()); status="Saved: " + nameInput.getValue().trim(); refresh(); }
            catch (Exception e) { status="Save failed: " + e.getMessage(); }
        }).bounds(x+16,y+120,246,20).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD SELECTED PROFILE"), b -> {
            try { VisionProfiles.load(current()); status="Loaded: " + current(); }
            catch (Exception e) { status="Load failed: " + e.getMessage(); }
        }).bounds(x+16,y+146,246,20).build());
        addRenderableWidget(Button.builder(Component.literal("DELETE SELECTED PROFILE"), b -> {
            try { VisionProfiles.delete(current()); status="Profile deleted"; refresh(); }
            catch (Exception e) { status="Delete failed: " + e.getMessage(); }
        }).bounds(x+16,y+172,246,20).build());
        addRenderableWidget(Button.builder(Component.literal("OPEN CONFIGS FOLDER"), b -> {
            status = VisionProfiles.openFolder() ? "Folder opened - drop JSON configs here" :
                "Open manually: config/visionmod/profiles";
            refresh();
        }).bounds(x+16,y+204,246,24).build());
        addRenderableWidget(Button.builder(Component.literal("REFRESH LIST"), b -> { refresh();status="Profile list updated"; })
            .bounds(x+16,y+234,120,20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> minecraft.setScreen(new VisionScreen()))
            .bounds(x+142,y+234,120,20).build());
        refresh();
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {}

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        int x = (width-278)/2;
        int y = Math.max(8, (height-286)/2);
        g.fill(x-4,y+4,x+282,y+290,0x88000000);
        g.fill(x,y,x+278,y+286,0xF2111624);
        g.fill(x,y,x+278,y+3,ACCENT);
        g.drawString(font,"VISION / CONFIGS 0.7.0",x+16,y+14,WHITE);
        g.drawString(font,"Share .json profiles with friends",x+16,y+36,MUTED);
        super.render(g,mx,my,delta);
        g.drawCenteredString(font,status,x+139,y+267,ACCENT);
    }

    @Override public void onClose() { minecraft.setScreen(new VisionScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
