package com.nexora.client.gui;

import com.nexora.client.NexoraClient;
import com.nexora.client.core.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NexoraScreen extends Screen {
    private static final int PANEL = 0xC0121018;
    private static final int PANEL_2 = 0xCB18151F;
    private static final int ROW = 0x941E1A26;
    private static final int ROW_HOVER = 0xC12A2432;
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int PURPLE_LIGHT = 0xFFB89BFF;
    private static final int TEXT = 0xFFF4F1F8;
    private static final int MUTED = 0xFF928C9D;
    private static final int GREEN = 0xFF65E6A5;

    private static final String[] CATEGORIES = {
            "Combat", "Movement", "Render", "World", "Misc", "DonutSMP"
    };

    private static final String[][] BLOCK_PRESETS = {
            {"minecraft:diamond_ore", "Diamond"},
            {"minecraft:deepslate_diamond_ore", "Deep Diamond"},
            {"minecraft:ancient_debris", "Ancient Debris"},
            {"minecraft:spawner", "Spawner"},
            {"minecraft:chest", "Chest"},
            {"minecraft:trapped_chest", "Trapped Chest"},
            {"minecraft:barrel", "Barrel"},
            {"minecraft:hopper", "Hopper"},
            {"minecraft:ender_chest", "Ender Chest"},
            {"minecraft:shulker_box", "Shulker"}
    };

    private static final String[][] XRAY_PRESETS = {
            {"minecraft:diamond_ore", "Diamond"},
            {"minecraft:deepslate_diamond_ore", "Deep Diamond"},
            {"minecraft:ancient_debris", "Ancient Debris"},
            {"minecraft:emerald_ore", "Emerald"},
            {"minecraft:deepslate_emerald_ore", "Deep Emerald"},
            {"minecraft:gold_ore", "Gold"},
            {"minecraft:deepslate_gold_ore", "Deep Gold"},
            {"minecraft:iron_ore", "Iron"},
            {"minecraft:deepslate_iron_ore", "Deep Iron"},
            {"minecraft:lapis_ore", "Lapis"}
    };

    private final NexoraClient nexora;
    private final Set<String> collapsedCategories = new LinkedHashSet<>();
    private final Set<String> openSettings = new LinkedHashSet<>();
    private final Map<String, Integer> tabs = new HashMap<>();
    private final Map<String, Float> toggleAnimations = new HashMap<>();
    private final Map<String, Float> panelAnimations = new HashMap<>();

    private float openAnim;
    private float frameDt;
    private long lastFrameNanos = System.nanoTime();

    public NexoraScreen(NexoraClient nexora) {
        super(Text.literal("Nexora Client"));
        this.nexora = nexora;
    }

    @Override
    protected void init() {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateAnimations();

        for (PanelPos panel : categoryPanels()) {
            drawCategoryPanel(context, mouseX, mouseY, panel);
        }

        for (SettingsPos panel : settingsPanels()) {
            drawSettingsPanel(context, mouseX, mouseY, panel);
        }

        context.drawTextWithShadow(textRenderer, "Nexora V8  •  L toggle  •  R settings  •  RShift close",
                8, Math.max(4, height - 14), 0xC0B9B1C4);
    }

    private void updateAnimations() {
        long now = System.nanoTime();
        frameDt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;
        openAnim = approach(openAnim, 1.0f, 14.0f * frameDt);

        for (String category : CATEGORIES) {
            String key = "cat:" + category;
            panelAnimations.put(key, approach(panelAnimations.getOrDefault(key, 0.0f), 1.0f, 16.0f * frameDt));
        }

        for (String module : openSettings) {
            String key = "set:" + module;
            panelAnimations.put(key, approach(panelAnimations.getOrDefault(key, 0.0f), 1.0f, 16.0f * frameDt));
        }
    }

    private void drawCategoryPanel(DrawContext context, int mouseX, int mouseY, PanelPos panel) {
        float anim = panelAnimations.getOrDefault("cat:" + panel.category, openAnim);
        int x = panel.x;
        int y = panel.y + Math.round((1.0f - easeOut(anim)) * 12);
        int w = panel.w;
        int h = panel.h;

        shadow(context, x, y, w, h);
        glassPanel(context, x, y, w, h, PANEL);

        boolean collapsed = collapsedCategories.contains(panel.category);
        context.drawTextWithShadow(textRenderer, collapsed ? "›" : "⌄", x + 7, y + 7, PURPLE_LIGHT);
        context.drawTextWithShadow(textRenderer, panel.category, x + 18, y + 7, TEXT);

        if (collapsed) return;

        int ry = y + 24;
        for (Module module : nexora.modules().category(panel.category)) {
            boolean hover = inside(mouseX, mouseY, x + 4, ry, w - 8, 17);
            roundedRect(context, x + 4, ry, w - 8, 17, 5, hover ? ROW_HOVER : ROW);

            context.drawTextWithShadow(textRenderer, module.name(), x + 8, ry + 4,
                    module.enabled() ? TEXT : MUTED);
            drawAnimatedToggle(context, "module:" + module.name(), x + w - 29, ry + 3, module.enabled());
            ry += 18;
        }
    }

    private void drawSettingsPanel(DrawContext context, int mouseX, int mouseY, SettingsPos panel) {
        Module module = nexora.modules().get(panel.module);
        if (module == null) return;

        float anim = panelAnimations.getOrDefault("set:" + panel.module, 1.0f);
        int x = panel.x;
        int y = panel.y + Math.round((1.0f - easeOut(anim)) * 14);
        int w = panel.w;
        int h = panel.h;

        shadow(context, x, y, w, h);
        glassPanel(context, x, y, w, h, PANEL_2);

        context.drawTextWithShadow(textRenderer, module.name(), x + 8, y + 7, TEXT);
        drawAnimatedToggle(context, "settings:" + module.name(), x + w - 43, y + 5, module.enabled());
        context.drawTextWithShadow(textRenderer, "×", x + w - 13, y + 7,
                inside(mouseX, mouseY, x + w - 16, y + 4, 11, 12) ? TEXT : MUTED);

        String[] moduleTabs = tabsFor(module.name());
        int tab = tabs.getOrDefault(module.name(), 0);
        int tx = x + 6;
        int ty = y + 25;

        for (int i = 0; i < moduleTabs.length; i++) {
            int tw = textRenderer.getWidth(moduleTabs[i]) + 10;
            boolean active = i == tab;
            boolean hover = inside(mouseX, mouseY, tx, ty, tw, 17);

            if (active) {
                roundedRect(context, tx, ty, tw, 17, 5, 0x663C2D59);
                roundedRect(context, tx + 4, ty + 15, Math.max(1, tw - 8), 2, 1, PURPLE);
            } else if (hover) {
                roundedRect(context, tx, ty, tw, 17, 5, 0x442A2334);
            }

            context.drawTextWithShadow(textRenderer, moduleTabs[i], tx + 5, ty + 4,
                    active ? TEXT : MUTED);
            tx += tw + 2;
        }

        drawModuleSettings(context, mouseX, mouseY, module, tab, x + 6, y + 47, w - 12);
    }

    private void drawModuleSettings(DrawContext context, int mouseX, int mouseY,
                                    Module module, int tab, int x, int y, int w) {
        switch (module.name()) {
            case "ESP" -> {
                if (tab == 0) {
                    toggleRow(context, mouseX, mouseY, x, y, w, "Players", nexora.espPlayers(), "esp-players");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Mobs", nexora.espMobs(), "esp-mobs");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Items", nexora.modules().enabled("ItemESP"), "esp-items");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Crystals", nexora.modules().enabled("CrystalESP"), "esp-crystals");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Tracers", nexora.modules().enabled("Tracers"), "esp-tracers");
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Range", nexora.espRange());
                } else {
                    toggleRow(context, mouseX, mouseY, x, y, w, "Entity Boxes", nexora.entityBoxes(), "esp-boxes");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Name + Distance", nexora.entityLabels(), "esp-labels");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Glow Outline", module.enabled(), "esp-glow");
                }
            }
            case "BaseFinder" -> {
                if (tab == 0) {
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Scan Range", nexora.baseFinder().scanRange());
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Vertical", nexora.baseFinder().verticalRange());
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Min Cluster", nexora.baseFinder().minClusterSize());
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Radius", nexora.baseFinder().clusterRadius());
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Delay", nexora.baseFinder().updateDelay());
                } else if (tab == 1) {
                    toggleRow(context, mouseX, mouseY, x, y, w, "Chests", nexora.baseFinder().chests(), "bf-chest");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Barrels", nexora.baseFinder().barrels(), "bf-barrel");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Hoppers", nexora.baseFinder().hoppers(), "bf-hopper");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Shulkers", nexora.baseFinder().shulkers(), "bf-shulker");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Furnaces", nexora.baseFinder().furnaces(), "bf-furnace");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Ender Chests", nexora.baseFinder().enderChests(), "bf-ender");
                } else {
                    toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "bf-boxes");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "bf-labels");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "StorageESP", nexora.modules().enabled("StorageESP"), "bf-storage");
                }
            }
            case "BlockESP" -> {
                if (tab == 0) {
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Range", nexora.blockEsp().scanRange());
                } else if (tab == 1) {
                    drawPresetRows(context, mouseX, mouseY, x, y, w, false);
                } else {
                    toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "block-boxes");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "block-labels");
                }
            }
            case "XRay" -> {
                if (tab == 0) {
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Range", nexora.xray().scanRange());
                    y += 22;
                    numericIntRow(context, mouseX, mouseY, x, y, w, "Vertical", nexora.xray().verticalRange());
                } else if (tab == 1) {
                    drawPresetRows(context, mouseX, mouseY, x, y, w, true);
                } else {
                    toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "xray-boxes");
                    y += 20;
                    toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "xray-labels");
                }
            }
            case "HUD" -> {
                toggleRow(context, mouseX, mouseY, x, y, w, "Coordinates", nexora.hudCoordinates(), "hud-coords");
                y += 20;
                toggleRow(context, mouseX, mouseY, x, y, w, "Active Modules", nexora.hudActiveModules(), "hud-mods");
                y += 22;
                numericIntRow(context, mouseX, mouseY, x, y, w, "GUI Opacity", nexora.guiOpacity());
            }
            case "Radar" -> numericIntRow(context, mouseX, mouseY, x, y, w, "Rows", nexora.radarRows());
            case "Freecam" -> {
                numericFloatRow(context, mouseX, mouseY, x, y, w, "Camera Speed", nexora.freecam().speed());
                info(context, x, y + 24, "WASD • Space up • Shift down");
            }
            case "Fly" -> numericFloatRow(context, mouseX, mouseY, x, y, w, "Fly Speed", nexora.flySpeed());
            case "Speed" -> numericFloatRow(context, mouseX, mouseY, x, y, w, "Multiplier", nexora.speedMultiplier());
            case "HighJump" -> numericFloatRow(context, mouseX, mouseY, x, y, w, "Jump Power", nexora.highJumpPower());
            case "FastFall" -> numericFloatRow(context, mouseX, mouseY, x, y, w, "Fall Speed", nexora.fastFallSpeed());
            case "Zoom" -> numericIntRow(context, mouseX, mouseY, x, y, w, "Zoom FOV", nexora.zoomFov());
            default -> {
                toggleRow(context, mouseX, mouseY, x, y, w, "Enabled", module.enabled(), "generic-" + module.name());
                info(context, x, y + 24, module.description());
            }
        }
    }

    private void drawPresetRows(DrawContext context, int mouseX, int mouseY, int x, int y, int w, boolean xray) {
        String[][] source = xray ? XRAY_PRESETS : BLOCK_PRESETS;

        for (String[] item : source) {
            boolean active = xray ? nexora.xray().contains(item[0]) : nexora.blockEsp().contains(item[0]);
            boolean hover = inside(mouseX, mouseY, x, y, w, 17);

            roundedRect(context, x, y, w, 17, 5, hover ? ROW_HOVER : ROW);
            context.drawTextWithShadow(textRenderer, item[1], x + 5, y + 4, active ? TEXT : MUTED);
            context.drawTextWithShadow(textRenderer, active ? "✓" : "+", x + w - 12, y + 4,
                    active ? GREEN : PURPLE_LIGHT);
            y += 18;
        }
    }

    private void toggleRow(DrawContext context, int mouseX, int mouseY,
                           int x, int y, int w, String name, boolean enabled, String key) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 18);
        roundedRect(context, x, y, w, 18, 5, hover ? ROW_HOVER : ROW);
        context.drawTextWithShadow(textRenderer, name, x + 5, y + 5, TEXT);
        drawAnimatedToggle(context, "row:" + key, x + w - 27, y + 3, enabled);
    }

    private void numericIntRow(DrawContext context, int mouseX, int mouseY,
                               int x, int y, int w, String name, int value) {
        roundedRect(context, x, y, w, 19, 5, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 5, y + 5, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 62, y + 2, "−");
        context.drawCenteredTextWithShadow(textRenderer, Integer.toString(value), x + w - 35, y + 5, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 17, y + 2, "+");
    }

    private void numericFloatRow(DrawContext context, int mouseX, int mouseY,
                                 int x, int y, int w, String name, float value) {
        roundedRect(context, x, y, w, 19, 5, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 5, y + 5, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 62, y + 2, "−");
        context.drawCenteredTextWithShadow(textRenderer,
                String.format(Locale.ROOT, "%.2f", value), x + w - 35, y + 5, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 17, y + 2, "+");
    }

    private void drawMiniButton(DrawContext context, int mouseX, int mouseY, int x, int y, String label) {
        boolean hover = inside(mouseX, mouseY, x, y, 16, 15);
        roundedRect(context, x, y, 16, 15, 5, hover ? 0xDD503A72 : 0xCC30283D);
        context.drawCenteredTextWithShadow(textRenderer, label, x + 8, y + 4, TEXT);
    }

    private void drawAnimatedToggle(DrawContext context, String key, int x, int y, boolean enabled) {
        float target = enabled ? 1.0f : 0.0f;
        float current = toggleAnimations.getOrDefault(key, target);
        current = approach(current, target, 18.0f * frameDt);
        toggleAnimations.put(key, current);

        int track = blend(0xB03B3543, PURPLE, current);
        roundedRect(context, x, y, 22, 11, 6, track);

        int knobX = x + 2 + Math.round(9 * easeOut(current));
        roundedRect(context, knobX, y + 2, 7, 7, 4, 0xFFF7F4FA);
    }

    private void info(DrawContext context, int x, int y, String text) {
        context.drawTextWithShadow(textRenderer, text, x, y, MUTED);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        for (PanelPos panel : categoryPanels()) {
            if (handleCategoryClick(mx, my, button, panel)) return true;
        }

        for (SettingsPos panel : settingsPanels()) {
            if (handleSettingsClick(mx, my, button, panel)) return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleCategoryClick(double mx, double my, int button, PanelPos panel) {
        if (inside(mx, my, panel.x, panel.y, panel.w, 22)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (collapsedCategories.contains(panel.category)) collapsedCategories.remove(panel.category);
                else collapsedCategories.add(panel.category);
                return true;
            }
        }

        if (collapsedCategories.contains(panel.category)) return false;

        int ry = panel.y + 24;
        for (Module module : nexora.modules().category(panel.category)) {
            if (inside(mx, my, panel.x + 4, ry, panel.w - 8, 17)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (openSettings.contains(module.name())) openSettings.remove(module.name());
                    else {
                        openSettings.add(module.name());
                        tabs.putIfAbsent(module.name(), 0);
                    }
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    module.toggle();
                    nexora.onModuleToggled(module);
                }
                return true;
            }
            ry += 18;
        }

        return false;
    }

    private boolean handleSettingsClick(double mx, double my, int button, SettingsPos panel) {
        Module module = nexora.modules().get(panel.module);
        if (module == null) return false;

        if (inside(mx, my, panel.x + panel.w - 16, panel.y + 4, 11, 12)) {
            openSettings.remove(module.name());
            return true;
        }

        if (inside(mx, my, panel.x + panel.w - 45, panel.y + 4, 24, 14)
                && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            module.toggle();
            nexora.onModuleToggled(module);
            return true;
        }

        String[] moduleTabs = tabsFor(module.name());
        int tx = panel.x + 6;
        int ty = panel.y + 25;

        for (int i = 0; i < moduleTabs.length; i++) {
            int tw = textRenderer.getWidth(moduleTabs[i]) + 10;
            if (inside(mx, my, tx, ty, tw, 17)) {
                tabs.put(module.name(), i);
                return true;
            }
            tx += tw + 2;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        return handleModuleSettingClick(mx, my, module, tabs.getOrDefault(module.name(), 0),
                panel.x + 6, panel.y + 47, panel.w - 12);
    }

    private boolean handleModuleSettingClick(double mx, double my, Module module, int tab,
                                             int x, int y, int w) {
        switch (module.name()) {
            case "ESP" -> {
                if (tab == 0) {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEspPlayers(!nexora.espPlayers()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEspMobs(!nexora.espMobs()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("ItemESP"))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("CrystalESP"))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("Tracers"))) return true;
                    y += 22;
                    return numericClick(mx, my, x, y, w,
                            () -> nexora.setEspRange(nexora.espRange() - 16),
                            () -> nexora.setEspRange(nexora.espRange() + 16));
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEntityBoxes(!nexora.entityBoxes()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEntityLabels(!nexora.entityLabels()))) return true;
                    y += 20;
                    return rowClick(mx, my, x, y, w, () -> {
                        module.toggle();
                        nexora.onModuleToggled(module);
                    });
                }
            }
            case "BaseFinder" -> {
                if (tab == 0) {
                    if (numericClick(mx, my, x, y, w,
                            () -> nexora.baseFinder().setScanRange(nexora.baseFinder().scanRange() - 8),
                            () -> nexora.baseFinder().setScanRange(nexora.baseFinder().scanRange() + 8))) return true;
                    y += 22;
                    if (numericClick(mx, my, x, y, w,
                            () -> nexora.baseFinder().setVerticalRange(nexora.baseFinder().verticalRange() - 4),
                            () -> nexora.baseFinder().setVerticalRange(nexora.baseFinder().verticalRange() + 4))) return true;
                    y += 22;
                    if (numericClick(mx, my, x, y, w,
                            () -> nexora.baseFinder().setMinClusterSize(nexora.baseFinder().minClusterSize() - 1),
                            () -> nexora.baseFinder().setMinClusterSize(nexora.baseFinder().minClusterSize() + 1))) return true;
                    y += 22;
                    if (numericClick(mx, my, x, y, w,
                            () -> nexora.baseFinder().setClusterRadius(nexora.baseFinder().clusterRadius() - 2),
                            () -> nexora.baseFinder().setClusterRadius(nexora.baseFinder().clusterRadius() + 2))) return true;
                    y += 22;
                    return numericClick(mx, my, x, y, w,
                            () -> nexora.baseFinder().setUpdateDelay(nexora.baseFinder().updateDelay() - 5),
                            () -> nexora.baseFinder().setUpdateDelay(nexora.baseFinder().updateDelay() + 5));
                } else if (tab == 1) {
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setChests(!nexora.baseFinder().chests()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setBarrels(!nexora.baseFinder().barrels()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setHoppers(!nexora.baseFinder().hoppers()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setShulkers(!nexora.baseFinder().shulkers()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setFurnaces(!nexora.baseFinder().furnaces()))) return true;
                    y += 20;
                    return rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setEnderChests(!nexora.baseFinder().enderChests()));
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 20;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()))) return true;
                    y += 20;
                    return rowClick(mx, my, x, y, w, () -> toggleModule("StorageESP"));
                }
            }
            case "BlockESP" -> {
                if (tab == 0) {
                    return numericClick(mx, my, x, y, w,
                            () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 8),
                            () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 8));
                } else if (tab == 1) {
                    return presetClick(mx, my, x, y, w, false);
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 20;
                    return rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()));
                }
            }
            case "XRay" -> {
                if (tab == 0) {
                    if (numericClick(mx, my, x, y, w,
                            () -> nexora.xray().setScanRange(nexora.xray().scanRange() - 8),
                            () -> nexora.xray().setScanRange(nexora.xray().scanRange() + 8))) return true;
                    y += 22;
                    return numericClick(mx, my, x, y, w,
                            () -> nexora.xray().setVerticalRange(nexora.xray().verticalRange() - 4),
                            () -> nexora.xray().setVerticalRange(nexora.xray().verticalRange() + 4));
                } else if (tab == 1) {
                    return presetClick(mx, my, x, y, w, true);
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 20;
                    return rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()));
                }
            }
            case "HUD" -> {
                if (rowClick(mx, my, x, y, w, () -> nexora.setHudCoordinates(!nexora.hudCoordinates()))) return true;
                y += 20;
                if (rowClick(mx, my, x, y, w, () -> nexora.setHudActiveModules(!nexora.hudActiveModules()))) return true;
                y += 22;
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setGuiOpacity(nexora.guiOpacity() - 10),
                        () -> nexora.setGuiOpacity(nexora.guiOpacity() + 10));
            }
            case "Radar" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setRadarRows(nexora.radarRows() - 1),
                        () -> nexora.setRadarRows(nexora.radarRows() + 1));
            }
            case "Freecam" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.freecam().setSpeed(nexora.freecam().speed() - 0.10f),
                        () -> nexora.freecam().setSpeed(nexora.freecam().speed() + 0.10f));
            }
            case "Fly" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setFlySpeed(nexora.flySpeed() - 0.05f),
                        () -> nexora.setFlySpeed(nexora.flySpeed() + 0.05f));
            }
            case "Speed" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setSpeedMultiplier(nexora.speedMultiplier() - 0.10f),
                        () -> nexora.setSpeedMultiplier(nexora.speedMultiplier() + 0.10f));
            }
            case "HighJump" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setHighJumpPower(nexora.highJumpPower() - 0.05f),
                        () -> nexora.setHighJumpPower(nexora.highJumpPower() + 0.05f));
            }
            case "FastFall" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setFastFallSpeed(nexora.fastFallSpeed() - 0.05f),
                        () -> nexora.setFastFallSpeed(nexora.fastFallSpeed() + 0.05f));
            }
            case "Zoom" -> {
                return numericClick(mx, my, x, y, w,
                        () -> nexora.setZoomFov(nexora.zoomFov() - 5),
                        () -> nexora.setZoomFov(nexora.zoomFov() + 5));
            }
            default -> {
                return rowClick(mx, my, x, y, w, () -> {
                    module.toggle();
                    nexora.onModuleToggled(module);
                });
            }
        }
    }

    private boolean presetClick(double mx, double my, int x, int y, int w, boolean xray) {
        String[][] source = xray ? XRAY_PRESETS : BLOCK_PRESETS;
        for (String[] item : source) {
            if (inside(mx, my, x, y, w, 17)) {
                if (xray) nexora.xray().toggle(item[0]);
                else nexora.blockEsp().toggle(item[0]);
                return true;
            }
            y += 18;
        }
        return false;
    }

    private boolean numericClick(double mx, double my, int x, int y, int w, Runnable minus, Runnable plus) {
        if (inside(mx, my, x + w - 62, y + 2, 16, 15)) {
            minus.run();
            return true;
        }
        if (inside(mx, my, x + w - 17, y + 2, 16, 15)) {
            plus.run();
            return true;
        }
        return false;
    }

    private boolean rowClick(double mx, double my, int x, int y, int w, Runnable action) {
        if (inside(mx, my, x, y, w, 18)) {
            action.run();
            return true;
        }
        return false;
    }

    private void toggleModule(String name) {
        Module module = nexora.modules().get(name);
        if (module != null) {
            module.toggle();
            nexora.onModuleToggled(module);
        }
    }

    private String[] tabsFor(String module) {
        return switch (module) {
            case "ESP" -> new String[]{"Targets", "Visuals"};
            case "BaseFinder", "BlockESP", "XRay" -> new String[]{"General", "Blocks", "Visuals"};
            default -> new String[]{"General"};
        };
    }

    private int settingsHeight(String module) {
        int tab = tabs.getOrDefault(module, 0);
        return switch (module) {
            case "ESP" -> tab == 0 ? 175 : 125;
            case "BaseFinder" -> tab == 1 ? 185 : 165;
            case "BlockESP", "XRay" -> tab == 1 ? 235 : 115;
            case "HUD" -> 135;
            case "Freecam" -> 105;
            default -> 95;
        };
    }

    private List<PanelPos> categoryPanels() {
        List<PanelPos> result = new ArrayList<>();

        int w = 112;
        int gap = 5;
        int x = 8;
        int y = 8;
        int rowHeight = 0;

        for (String category : CATEGORIES) {
            int h = collapsedCategories.contains(category)
                    ? 22
                    : 24 + nexora.modules().category(category).size() * 18 + 5;

            if (x + w > width - 8 && x > 8) {
                x = 8;
                y += rowHeight + gap;
                rowHeight = 0;
            }

            result.add(new PanelPos(category, x, y, w, h));
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }

        return result;
    }

    private int categoryGridBottom() {
        int bottom = 8;
        for (PanelPos p : categoryPanels()) bottom = Math.max(bottom, p.y + p.h);
        return bottom;
    }

    private List<SettingsPos> settingsPanels() {
        List<SettingsPos> result = new ArrayList<>();

        int w = 205;
        int gap = 5;
        int x = 8;
        int y = categoryGridBottom() + 6;
        int rowHeight = 0;

        for (String module : openSettings) {
            int h = settingsHeight(module);

            if (x + w > width - 8 && x > 8) {
                x = 8;
                y += rowHeight + gap;
                rowHeight = 0;
            }

            result.add(new SettingsPos(module, x, y, w, h));
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }

        return result;
    }

    private void shadow(DrawContext context, int x, int y, int w, int h) {
        roundedRect(context, x - 4, y - 3, w + 8, h + 8, 8, 0x1C000000);
        roundedRect(context, x - 2, y - 1, w + 4, h + 4, 7, 0x30000000);
    }

    private void glassPanel(DrawContext context, int x, int y, int w, int h, int color) {
        int alpha = nexora.guiOpacity();
        roundedRect(context, x - 1, y - 1, w + 2, h + 2, 7, withAlpha(0x473759, Math.min(150, alpha)));
        roundedRect(context, x, y, w, h, 7, withAlpha(color, alpha));
        roundedRect(context, x + 7, y + 1, Math.max(1, w - 14), 2, 1, pulsePurple());
    }

    private int pulsePurple() {
        double t = System.nanoTime() / 1_000_000_000.0;
        float p = 0.82f + (float)(Math.sin(t * 3.0) * 0.10);
        return blend(0xFF7448D5, PURPLE_LIGHT, p);
    }

    private int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private void roundedRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 1) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }

        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            int inset = 0;
            if (edge < r) {
                double dy = r - edge - 0.5;
                inset = r - (int)Math.floor(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            }
            context.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private float approach(float current, float target, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        return current + (target - current) * amount;
    }

    private float easeOut(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private int blend(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));

        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;

        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int ca = (int)(aa + (ba - aa) * t);
        int cr = (int)(ar + (br - ar) * t);
        int cg = (int)(ag + (bg - ag) * t);
        int cb = (int)(ab + (bb - ab) * t);

        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record PanelPos(String category, int x, int y, int w, int h) {}
    private record SettingsPos(String module, int x, int y, int w, int h) {}
}
