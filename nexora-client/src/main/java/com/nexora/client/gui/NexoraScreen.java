package com.nexora.client.gui;

import com.nexora.client.NexoraClient;
import com.nexora.client.core.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NexoraScreen extends Screen {
    private static final int PANEL = 0xE8121018;
    private static final int PANEL_2 = 0xEE18151F;
    private static final int ROW = 0xE51E1A26;
    private static final int ROW_HOVER = 0xF02A2432;
    private static final int SELECTED = 0xEE352758;
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int PURPLE_LIGHT = 0xFFB89BFF;
    private static final int TEXT = 0xFFF4F1F8;
    private static final int MUTED = 0xFF928C9D;
    private static final int GREEN = 0xFF65E6A5;

    private static final String[] CATEGORIES = {
            "Combat", "Movement", "Render", "World", "Misc", "DonutSMP"
    };

    private static final String[][] BLOCK_PRESETS = {
            {"minecraft:diamond_ore", "Diamond Ore"},
            {"minecraft:deepslate_diamond_ore", "Deep Diamond"},
            {"minecraft:ancient_debris", "Ancient Debris"},
            {"minecraft:spawner", "Spawner"},
            {"minecraft:chest", "Chest"},
            {"minecraft:trapped_chest", "Trapped Chest"},
            {"minecraft:barrel", "Barrel"},
            {"minecraft:hopper", "Hopper"},
            {"minecraft:ender_chest", "Ender Chest"},
            {"minecraft:shulker_box", "Shulker"},
            {"minecraft:emerald_ore", "Emerald Ore"},
            {"minecraft:deepslate_emerald_ore", "Deep Emerald"}
    };

    private final NexoraClient nexora;
    private final Map<String, Float> toggleAnimations = new HashMap<>();

    private String expandedCategory = "DonutSMP";
    private Module selected;
    private int selectedTab;

    private float openAnim;
    private float settingsAnim;
    private float frameDt;
    private long lastFrameNanos = System.nanoTime();

    public NexoraScreen(NexoraClient nexora) {
        super(Text.literal("Nexora Client"));
        this.nexora = nexora;
        this.selected = nexora.modules().get("BaseFinder");
    }

    @Override
    protected void init() {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateAnimations();

        Layout l = layout();
        drawMainMenu(context, mouseX, mouseY, l);

        if (selected != null) {
            drawSettingsWindow(context, mouseX, mouseY, l);
        }
    }

    private void updateAnimations() {
        long now = System.nanoTime();
        frameDt = Math.min(0.05f, Math.max(0.001f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;

        openAnim = approach(openAnim, 1.0f, 12.0f * frameDt);
        settingsAnim = approach(settingsAnim, selected == null ? 0.0f : 1.0f, 14.0f * frameDt);
    }

    private void drawMainMenu(DrawContext context, int mouseX, int mouseY, Layout l) {
        shadow(context, l.menuX, l.menuY, l.menuW, l.menuH);
        context.fill(l.menuX, l.menuY, l.menuX + l.menuW, l.menuY + l.menuH, PANEL);

        int pulse = pulsePurple();
        context.fill(l.menuX, l.menuY, l.menuX + 3, l.menuY + l.menuH, pulse);

        context.drawTextWithShadow(textRenderer, "✦ Nexora Client", l.menuX + 10, l.menuY + 9, PURPLE_LIGHT);
        context.drawTextWithShadow(textRenderer, "0.4", l.menuX + l.menuW - 28, l.menuY + 9, MUTED);

        int y = l.menuY + 34;

        for (String category : CATEGORIES) {
            boolean expanded = category.equals(expandedCategory);
            boolean hover = inside(mouseX, mouseY, l.menuX + 5, y - 3, l.menuW - 10, 20);

            if (hover) {
                context.fill(l.menuX + 5, y - 3, l.menuX + l.menuW - 5, y + 17, 0x55282033);
            }

            String arrow = expanded ? "⌄" : "›";
            context.drawTextWithShadow(textRenderer, arrow, l.menuX + 10, y + 1, expanded ? PURPLE_LIGHT : MUTED);
            context.drawTextWithShadow(textRenderer, category, l.menuX + 24, y + 1, expanded ? TEXT : 0xFFD7D2DC);

            int count = nexora.modules().category(category).size();
            if (count > 0) {
                String c = Integer.toString(count);
                context.drawTextWithShadow(textRenderer, c, l.menuX + l.menuW - 18, y + 1, MUTED);
            }

            y += 21;

            if (expanded) {
                List<Module> modules = nexora.modules().category(category);
                for (Module module : modules) {
                    boolean chosen = selected != null && selected.name().equals(module.name());
                    boolean moduleHover = inside(mouseX, mouseY, l.menuX + 13, y - 2, l.menuW - 20, 19);

                    if (chosen) {
                        context.fill(l.menuX + 13, y - 2, l.menuX + l.menuW - 7, y + 17, SELECTED);
                        context.fill(l.menuX + 13, y - 2, l.menuX + 15, y + 17, pulse);
                    } else if (moduleHover) {
                        context.fill(l.menuX + 13, y - 2, l.menuX + l.menuW - 7, y + 17, ROW_HOVER);
                    }

                    context.drawTextWithShadow(textRenderer, module.name(), l.menuX + 25, y + 2,
                            module.enabled() ? TEXT : MUTED);
                    drawAnimatedToggle(context, "module:" + module.name(),
                            l.menuX + l.menuW - 36, y + 1, module.enabled());

                    y += 20;
                }
            }
        }

        int footerY = l.menuY + l.menuH - 23;
        context.fill(l.menuX + 6, footerY - 4, l.menuX + l.menuW - 6, footerY - 3, 0xFF27222F);
        context.drawTextWithShadow(textRenderer, "Settings", l.menuX + 10, footerY + 2, MUTED);
        context.drawTextWithShadow(textRenderer, "Right Shift", l.menuX + l.menuW - 64, footerY + 2, MUTED);
    }

    private void drawSettingsWindow(DrawContext context, int mouseX, int mouseY, Layout l) {
        if (settingsAnim <= 0.02f) return;

        int sx = l.settingsX;
        int sy = l.settingsY;
        int sw = l.settingsW;
        int sh = settingsHeight();

        shadow(context, sx, sy, sw, sh);
        context.fill(sx, sy, sx + sw, sy + sh, PANEL_2);
        context.fill(sx, sy, sx + sw, sy + 2, pulsePurple());

        context.drawTextWithShadow(textRenderer, selected.name(), sx + 10, sy + 9, TEXT);
        drawAnimatedToggle(context, "selected:" + selected.name(), sx + sw - 39, sy + 7, selected.enabled());

        boolean closeHover = inside(mouseX, mouseY, sx + sw - 18, sy + 7, 10, 10);
        context.drawTextWithShadow(textRenderer, "×", sx + sw - 16, sy + 8, closeHover ? TEXT : MUTED);

        String[] tabs = tabsForSelected();
        int tabY = sy + 30;
        int tabX = sx + 8;
        for (int i = 0; i < tabs.length; i++) {
            int tw = textRenderer.getWidth(tabs[i]) + 14;
            boolean active = selectedTab == i;
            boolean hover = inside(mouseX, mouseY, tabX, tabY, tw, 19);

            if (active) {
                context.fill(tabX, tabY, tabX + tw, tabY + 19, 0x663C2D59);
                context.fill(tabX, tabY + 17, tabX + tw, tabY + 19, PURPLE);
            } else if (hover) {
                context.fill(tabX, tabY, tabX + tw, tabY + 19, 0x442A2334);
            }

            context.drawTextWithShadow(textRenderer, tabs[i], tabX + 7, tabY + 5,
                    active ? TEXT : MUTED);
            tabX += tw + 2;
        }

        int bodyX = sx + 9;
        int bodyY = sy + 57;
        int bodyW = sw - 18;

        drawSelectedTab(context, mouseX, mouseY, bodyX, bodyY, bodyW);
    }

    private void drawSelectedTab(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        switch (selected.name()) {
            case "BaseFinder" -> drawBaseFinderTab(context, mouseX, mouseY, x, y, w);
            case "ESP" -> drawEspTab(context, mouseX, mouseY, x, y, w);
            case "BlockESP" -> drawBlockEspTab(context, mouseX, mouseY, x, y, w);
            case "XRay" -> drawXRayTab(context, mouseX, mouseY, x, y, w);
            case "Freecam" -> {
                numericFloatRow(context, mouseX, mouseY, x, y, w, "Freecam Speed", nexora.freecam().speed());
                info(context, x, y + 27, "WASD move • Space up • Shift down");
                info(context, x, y + 39, "Sprint key = 2x camera speed");
            }
            case "Fly" -> {
                numericFloatRow(context, mouseX, mouseY, x, y, w, "Fly Speed", nexora.flySpeed());
                info(context, x, y + 27, "Use + / − or .flyspeed");
            }
            case "Speed" -> {
                numericFloatRow(context, mouseX, mouseY, x, y, w, "Speed", nexora.speedMultiplier());
                info(context, x, y + 27, "Client movement multiplier");
            }
            case "StorageESP" -> {
                toggleRow(context, mouseX, mouseY, x, y, w, "Storage ESP", selected.enabled(), "storage");
                info(context, x, y + 28, "Gold markers for loaded storage blocks.");
            }
            case "Waypoints" -> {
                toggleRow(context, mouseX, mouseY, x, y, w, "Waypoint ESP", selected.enabled(), "waypoints");
                info(context, x, y + 28, "Use .wp add <name>");
            }
            default -> {
                toggleRow(context, mouseX, mouseY, x, y, w, "Enabled", selected.enabled(), "generic");
                info(context, x, y + 28, selected.description());
            }
        }
    }

    private void drawBaseFinderTab(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        if (selectedTab == 0) {
            numericIntRow(context, mouseX, mouseY, x, y, w, "Scan Range", nexora.baseFinder().scanRange());
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Vertical Range", nexora.baseFinder().verticalRange());
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Min Cluster Size", nexora.baseFinder().minClusterSize());
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Cluster Radius", nexora.baseFinder().clusterRadius());
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Update Delay", nexora.baseFinder().updateDelay());
        } else if (selectedTab == 1) {
            toggleRow(context, mouseX, mouseY, x, y, w, "Chests", nexora.baseFinder().chests(), "bf-chest");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Barrels", nexora.baseFinder().barrels(), "bf-barrel");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Hoppers", nexora.baseFinder().hoppers(), "bf-hopper");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Shulkers", nexora.baseFinder().shulkers(), "bf-shulker");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Furnaces", nexora.baseFinder().furnaces(), "bf-furnace");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Ender Chests", nexora.baseFinder().enderChests(), "bf-ender");
        } else {
            toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "visual-boxes");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "visual-labels");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "StorageESP", nexora.modules().enabled("StorageESP"), "visual-storage");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "HUD Radar", nexora.modules().enabled("HUD"), "visual-hud");
        }
    }

    private void drawEspTab(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        if (selectedTab == 0) {
            toggleRow(context, mouseX, mouseY, x, y, w, "Players", nexora.espPlayers(), "esp-players");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Mobs", nexora.espMobs(), "esp-mobs");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Dropped Items", nexora.modules().enabled("ItemESP"), "esp-items");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "End Crystals", nexora.modules().enabled("CrystalESP"), "esp-crystals");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Tracers", nexora.modules().enabled("Tracers"), "esp-tracers");
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Entity Range", nexora.espRange());
        } else if (selectedTab == 1) {
            toggleRow(context, mouseX, mouseY, x, y, w, "BlockESP", nexora.modules().enabled("BlockESP"), "esp-block");
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Block Range", nexora.blockEsp().scanRange());
            y += 27;
            drawBlockList(context, mouseX, mouseY, x, y, w);
        } else {
            toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "esp-boxes");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "esp-labels");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Fullbright", nexora.modules().enabled("Fullbright"), "esp-fullbright");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "HUD Radar", nexora.modules().enabled("HUD"), "esp-hud");
        }
    }

    private void drawBlockEspTab(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        if (selectedTab == 0) {
            toggleRow(context, mouseX, mouseY, x, y, w, "BlockESP", selected.enabled(), "block-main");
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Scan Range", nexora.blockEsp().scanRange());
        } else if (selectedTab == 1) {
            drawBlockList(context, mouseX, mouseY, x, y, w);
        } else {
            toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "block-boxes");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "block-labels");
        }
    }

    private void drawXRayTab(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        if (selectedTab == 0) {
            toggleRow(context, mouseX, mouseY, x, y, w, "XRay", selected.enabled(), "xray-main");
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Scan Range", nexora.xray().scanRange());
            y += 24;
            numericIntRow(context, mouseX, mouseY, x, y, w, "Vertical Range", nexora.xray().verticalRange());
            y += 24;
            info(context, x, y, "Green boxes mark ores through terrain.");
        } else if (selectedTab == 1) {
            String[][] ores = {
                    {"minecraft:diamond_ore", "Diamond Ore"},
                    {"minecraft:deepslate_diamond_ore", "Deep Diamond"},
                    {"minecraft:ancient_debris", "Ancient Debris"},
                    {"minecraft:emerald_ore", "Emerald Ore"},
                    {"minecraft:deepslate_emerald_ore", "Deep Emerald"},
                    {"minecraft:gold_ore", "Gold Ore"},
                    {"minecraft:deepslate_gold_ore", "Deep Gold"},
                    {"minecraft:iron_ore", "Iron Ore"}
            };
            int rowH = 19;
            for (String[] ore : ores) {
                boolean active = nexora.xray().contains(ore[0]);
                boolean hover = inside(mouseX, mouseY, x, y, w, rowH - 1);
                context.fill(x, y, x + w, y + rowH - 1, hover ? ROW_HOVER : ROW);
                context.drawTextWithShadow(textRenderer, ore[1], x + 6, y + 5, active ? TEXT : MUTED);
                context.drawTextWithShadow(textRenderer, active ? "✓" : "+", x + w - 14, y + 5,
                        active ? GREEN : PURPLE_LIGHT);
                y += rowH;
            }
        } else {
            toggleRow(context, mouseX, mouseY, x, y, w, "World Boxes", nexora.worldBoxes(), "xray-boxes");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "Through-wall Labels", nexora.worldLabels(), "xray-labels");
            y += 22;
            toggleRow(context, mouseX, mouseY, x, y, w, "HUD Radar", nexora.modules().enabled("HUD"), "xray-hud");
        }
    }

    private void drawBlockList(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        int rowH = 19;
        for (int i = 0; i < BLOCK_PRESETS.length; i++) {
            if (i >= 8) break;
            boolean active = nexora.blockEsp().contains(BLOCK_PRESETS[i][0]);
            boolean hover = inside(mouseX, mouseY, x, y, w, rowH - 1);

            context.fill(x, y, x + w, y + rowH - 1, hover ? ROW_HOVER : ROW);
            context.drawTextWithShadow(textRenderer, BLOCK_PRESETS[i][1], x + 6, y + 5,
                    active ? TEXT : MUTED);
            context.drawTextWithShadow(textRenderer, active ? "✓" : "+", x + w - 14, y + 5,
                    active ? GREEN : PURPLE_LIGHT);
            y += rowH;
        }
        info(context, x, y + 2, ".blockesp add minecraft:block");
    }

    private void toggleRow(DrawContext context, int mouseX, int mouseY,
                           int x, int y, int w, String name, boolean enabled, String key) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        context.fill(x, y, x + w, y + 20, hover ? ROW_HOVER : ROW);
        context.drawTextWithShadow(textRenderer, name, x + 6, y + 6, TEXT);
        drawAnimatedToggle(context, "row:" + key, x + w - 31, y + 3, enabled);
    }

    private void numericIntRow(DrawContext context, int mouseX, int mouseY,
                               int x, int y, int w, String name, int value) {
        context.fill(x, y, x + w, y + 21, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 6, y + 6, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 70, y + 2, "−");
        context.drawCenteredTextWithShadow(textRenderer, Integer.toString(value), x + w - 38, y + 6, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 21, y + 2, "+");
    }

    private void numericFloatRow(DrawContext context, int mouseX, int mouseY,
                                 int x, int y, int w, String name, float value) {
        context.fill(x, y, x + w, y + 21, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 6, y + 6, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 70, y + 2, "−");
        context.drawCenteredTextWithShadow(textRenderer,
                String.format(Locale.ROOT, "%.2f", value), x + w - 38, y + 6, TEXT);
        drawMiniButton(context, mouseX, mouseY, x + w - 21, y + 2, "+");
    }

    private void drawMiniButton(DrawContext context, int mouseX, int mouseY, int x, int y, String text) {
        boolean hover = inside(mouseX, mouseY, x, y, 18, 17);
        context.fill(x, y, x + 18, y + 17, hover ? 0xFF503A72 : 0xFF30283D);
        context.drawCenteredTextWithShadow(textRenderer, text, x + 9, y + 5, TEXT);
    }

    private void drawAnimatedToggle(DrawContext context, String key, int x, int y, boolean enabled) {
        float target = enabled ? 1.0f : 0.0f;
        float current = toggleAnimations.getOrDefault(key, target);
        current = approach(current, target, 18.0f * frameDt);
        toggleAnimations.put(key, current);

        int off = 0xFF3B3543;
        int on = PURPLE;
        int track = blend(off, on, current);

        context.fill(x, y, x + 26, y + 13, track);
        int knobX = x + 2 + Math.round(12 * easeOut(current));
        context.fill(knobX, y + 2, knobX + 9, y + 11, 0xFFF7F4FA);
    }

    private void info(DrawContext context, int x, int y, String text) {
        context.drawTextWithShadow(textRenderer, text, x, y, MUTED);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Layout l = layout();
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        int y = l.menuY + 34;

        for (String category : CATEGORIES) {
            if (inside(mx, my, l.menuX + 5, y - 3, l.menuW - 10, 20)) {
                expandedCategory = category.equals(expandedCategory) ? "" : category;
                return true;
            }
            y += 21;

            if (category.equals(expandedCategory)) {
                for (Module module : nexora.modules().category(category)) {
                    if (inside(mx, my, l.menuX + 13, y - 2, l.menuW - 20, 19)) {
                        selected = module;
                        selectedTab = 0;
                        settingsAnim = 0.0f;

                        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            module.toggle();
                            nexora.onModuleToggled(module);
                        }
                        return true;
                    }
                    y += 20;
                }
            }
        }

        if (selected == null) return super.mouseClicked(click, doubled);

        int sx = l.settingsX;
        int sy = l.settingsY;
        int sw = l.settingsW;

        if (inside(mx, my, sx + sw - 18, sy + 7, 10, 12)) {
            selected = null;
            return true;
        }

        String[] tabs = tabsForSelected();
        int tabX = sx + 8;
        int tabY = sy + 30;
        for (int i = 0; i < tabs.length; i++) {
            int tw = textRenderer.getWidth(tabs[i]) + 14;
            if (inside(mx, my, tabX, tabY, tw, 19)) {
                selectedTab = i;
                settingsAnim = 0.55f;
                return true;
            }
            tabX += tw + 2;
        }

        int x = sx + 9;
        int bodyY = sy + 57;
        int w = sw - 18;

        return handleSelectedClick(mx, my, button, x, bodyY, w) || super.mouseClicked(click, doubled);
    }

    private boolean handleSelectedClick(double mx, double my, int button, int x, int y, int w) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        switch (selected.name()) {
            case "BaseFinder" -> {
                if (selectedTab == 0) {
                    if (numericClick(mx, my, x, y, w, () -> nexora.baseFinder().setScanRange(nexora.baseFinder().scanRange() - 8),
                            () -> nexora.baseFinder().setScanRange(nexora.baseFinder().scanRange() + 8))) return true;
                    y += 24;
                    if (numericClick(mx, my, x, y, w, () -> nexora.baseFinder().setVerticalRange(nexora.baseFinder().verticalRange() - 4),
                            () -> nexora.baseFinder().setVerticalRange(nexora.baseFinder().verticalRange() + 4))) return true;
                    y += 24;
                    if (numericClick(mx, my, x, y, w, () -> nexora.baseFinder().setMinClusterSize(nexora.baseFinder().minClusterSize() - 1),
                            () -> nexora.baseFinder().setMinClusterSize(nexora.baseFinder().minClusterSize() + 1))) return true;
                    y += 24;
                    if (numericClick(mx, my, x, y, w, () -> nexora.baseFinder().setClusterRadius(nexora.baseFinder().clusterRadius() - 2),
                            () -> nexora.baseFinder().setClusterRadius(nexora.baseFinder().clusterRadius() + 2))) return true;
                    y += 24;
                    return numericClick(mx, my, x, y, w, () -> nexora.baseFinder().setUpdateDelay(nexora.baseFinder().updateDelay() - 5),
                            () -> nexora.baseFinder().setUpdateDelay(nexora.baseFinder().updateDelay() + 5));
                } else if (selectedTab == 1) {
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setChests(!nexora.baseFinder().chests()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setBarrels(!nexora.baseFinder().barrels()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setHoppers(!nexora.baseFinder().hoppers()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setShulkers(!nexora.baseFinder().shulkers()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setFurnaces(!nexora.baseFinder().furnaces()))) return true;
                    y += 22;
                    return rowClick(mx, my, x, y, w, () -> nexora.baseFinder().setEnderChests(!nexora.baseFinder().enderChests()));
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("StorageESP"))) return true;
                    y += 22;
                    return rowClick(mx, my, x, y, w, () -> toggleModule("HUD"));
                }
            }
            case "ESP" -> {
                if (selectedTab == 0) {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEspPlayers(!nexora.espPlayers()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setEspMobs(!nexora.espMobs()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("ItemESP"))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("CrystalESP"))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("Tracers"))) return true;
                    y += 24;
                    return numericClick(mx, my, x, y, w, () -> nexora.setEspRange(nexora.espRange() - 16),
                            () -> nexora.setEspRange(nexora.espRange() + 16));
                } else if (selectedTab == 1) {
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("BlockESP"))) return true;
                    y += 24;
                    if (numericClick(mx, my, x, y, w, () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 8),
                            () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 8))) return true;
                    y += 27;
                    return blockListClick(mx, my, x, y, w);
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("Fullbright"))) return true;
                    y += 22;
                    return rowClick(mx, my, x, y, w, () -> toggleModule("HUD"));
                }
            }
            case "BlockESP" -> {
                if (selectedTab == 0) {
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("BlockESP"))) return true;
                    y += 24;
                    return numericClick(mx, my, x, y, w, () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 8),
                            () -> nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 8));
                } else if (selectedTab == 1) {
                    return blockListClick(mx, my, x, y, w);
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 22;
                    return rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()));
                }
            }
            case "Fly" -> {
                return numericClick(mx, my, x, y, w, () -> nexora.setFlySpeed(nexora.flySpeed() - 0.05f),
                        () -> nexora.setFlySpeed(nexora.flySpeed() + 0.05f));
            }
            case "Speed" -> {
                return numericClick(mx, my, x, y, w, () -> nexora.setSpeedMultiplier(nexora.speedMultiplier() - 0.10f),
                        () -> nexora.setSpeedMultiplier(nexora.speedMultiplier() + 0.10f));
            }
            case "Freecam" -> {
                return numericClick(mx, my, x, y, w, () -> nexora.freecam().setSpeed(nexora.freecam().speed() - 0.10f),
                        () -> nexora.freecam().setSpeed(nexora.freecam().speed() + 0.10f));
            }
            case "XRay" -> {
                if (selectedTab == 0) {
                    if (rowClick(mx, my, x, y, w, () -> toggleModule("XRay"))) return true;
                    y += 24;
                    if (numericClick(mx, my, x, y, w, () -> nexora.xray().setScanRange(nexora.xray().scanRange() - 8),
                            () -> nexora.xray().setScanRange(nexora.xray().scanRange() + 8))) return true;
                    y += 24;
                    return numericClick(mx, my, x, y, w, () -> nexora.xray().setVerticalRange(nexora.xray().verticalRange() - 4),
                            () -> nexora.xray().setVerticalRange(nexora.xray().verticalRange() + 4));
                } else if (selectedTab == 1) {
                    String[] ores = {
                            "minecraft:diamond_ore",
                            "minecraft:deepslate_diamond_ore",
                            "minecraft:ancient_debris",
                            "minecraft:emerald_ore",
                            "minecraft:deepslate_emerald_ore",
                            "minecraft:gold_ore",
                            "minecraft:deepslate_gold_ore",
                            "minecraft:iron_ore"
                    };
                    int rowH = 19;
                    for (String ore : ores) {
                        if (inside(mx, my, x, y, w, rowH - 1)) {
                            nexora.xray().toggle(ore);
                            return true;
                        }
                        y += rowH;
                    }
                    return false;
                } else {
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldBoxes(!nexora.worldBoxes()))) return true;
                    y += 22;
                    if (rowClick(mx, my, x, y, w, () -> nexora.setWorldLabels(!nexora.worldLabels()))) return true;
                    y += 22;
                    return rowClick(mx, my, x, y, w, () -> toggleModule("HUD"));
                }
            }
            default -> {
                return rowClick(mx, my, x, y, w, () -> {
                    selected.toggle();
                    nexora.onModuleToggled(selected);
                });
            }
        }
    }

    private boolean numericClick(double mx, double my, int x, int y, int w, Runnable minus, Runnable plus) {
        if (inside(mx, my, x + w - 70, y + 2, 18, 17)) {
            minus.run();
            return true;
        }
        if (inside(mx, my, x + w - 21, y + 2, 18, 17)) {
            plus.run();
            return true;
        }
        return false;
    }

    private boolean rowClick(double mx, double my, int x, int y, int w, Runnable action) {
        if (inside(mx, my, x, y, w, 20)) {
            action.run();
            return true;
        }
        return false;
    }

    private boolean blockListClick(double mx, double my, int x, int y, int w) {
        int rowH = 19;
        for (int i = 0; i < BLOCK_PRESETS.length && i < 8; i++) {
            if (inside(mx, my, x, y, w, rowH - 1)) {
                nexora.blockEsp().toggle(BLOCK_PRESETS[i][0]);
                return true;
            }
            y += rowH;
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

    private String[] tabsForSelected() {
        return switch (selected.name()) {
            case "BaseFinder", "ESP", "BlockESP", "XRay" -> new String[]{"General", "Blocks", "Visuals"};
            default -> new String[]{"General"};
        };
    }

    private int settingsHeight() {
        if (selected == null) return 0;
        return switch (selected.name()) {
            case "BaseFinder" -> selectedTab == 1 ? 205 : 190;
            case "ESP" -> selectedTab == 1 ? 245 : 205;
            case "BlockESP" -> selectedTab == 1 ? 245 : 145;
            case "XRay" -> selectedTab == 1 ? 230 : 160;
            case "Freecam" -> 145;
            default -> 125;
        };
    }

    private Layout layout() {
        int menuW = 190;
        int moduleCount = expandedCategory.isEmpty() ? 0 : nexora.modules().category(expandedCategory).size();
        int menuH = 34 + CATEGORIES.length * 21 + moduleCount * 20 + 27;

        float eased = easeOut(openAnim);
        int menuX = 10 - Math.round((1.0f - eased) * (menuW + 20));
        int menuY = 10;

        int settingsW = 310;
        int settingsX = menuX + menuW + 8 + Math.round((1.0f - easeOut(settingsAnim)) * 45);
        int settingsY = 18;

        return new Layout(menuX, menuY, menuW, menuH, settingsX, settingsY, settingsW);
    }

    private int pulsePurple() {
        double t = System.nanoTime() / 1_000_000_000.0;
        float p = 0.82f + (float)(Math.sin(t * 3.0) * 0.10);
        return blend(0xFF7448D5, PURPLE_LIGHT, p);
    }

    private void shadow(DrawContext context, int x, int y, int w, int h) {
        context.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x55000000);
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x66000000);
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

    private record Layout(
            int menuX, int menuY, int menuW, int menuH,
            int settingsX, int settingsY, int settingsW
    ) {}
}
