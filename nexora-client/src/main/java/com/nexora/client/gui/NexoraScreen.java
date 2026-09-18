package com.nexora.client.gui;

import com.nexora.client.NexoraClient;
import com.nexora.client.core.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public final class NexoraScreen extends Screen {
    private static final int PANEL = 0xE813111A;
    private static final int PANEL_2 = 0xEE18151F;
    private static final int ROW = 0xE51E1A26;
    private static final int ROW_HOVER = 0xF02A2336;
    private static final int SELECTED = 0xFF2B2240;
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int PURPLE_LIGHT = 0xFFB89BFF;
    private static final int TEXT = 0xFFF5F2FA;
    private static final int MUTED = 0xFF9A93A6;
    private static final int GREEN = 0xFF65E6A5;
    private static final int RED = 0xFFFF6D7C;
    private static final int CYAN = 0xFF65ECFF;
    private static final int GOLD = 0xFFFFD76A;

    private static final String[] CATEGORIES = {
            "Combat", "Movement", "Render", "World", "DonutSMP", "Misc"
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
            {"minecraft:shulker_box", "Shulker"},
            {"minecraft:emerald_ore", "Emerald"},
            {"minecraft:deepslate_emerald_ore", "Deep Emerald"}
    };

    private final NexoraClient nexora;
    private String category = "Render";
    private Module selected;

    public NexoraScreen(NexoraClient nexora) {
        super(Text.literal("Nexora Client"));
        this.nexora = nexora;
        this.selected = nexora.modules().get("ESP");
    }

    @Override
    protected void init() {
        // Fully custom compact click GUI.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        Layout l = layout();

        // No full-screen dim. The world remains visible like Meteor.
        shadow(context, l.x, l.y, l.leftW, l.h);
        context.fill(l.x, l.y, l.x + l.leftW, l.y + l.h, PANEL);
        context.fill(l.x, l.y, l.x + l.leftW, l.y + 2, PURPLE);

        drawSidebar(context, mouseX, mouseY, l);
        drawModuleColumn(context, mouseX, mouseY, l);

        if (selected != null) {
            int sx = l.x + l.leftW + 7;
            shadow(context, sx, l.y, l.settingsW, l.h);
            context.fill(sx, l.y, sx + l.settingsW, l.y + l.h, PANEL_2);
            context.fill(sx, l.y, sx + l.settingsW, l.y + 2, PURPLE);
            drawSettings(context, mouseX, mouseY, l, sx);
        }
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY, Layout l) {
        int x = l.x + 10;
        int y = l.y + 10;

        context.drawTextWithShadow(textRenderer, "✦ Nexora Client", x, y, PURPLE_LIGHT);
        context.drawTextWithShadow(textRenderer, "0.3 • Fabric 1.21.11", x, y + 13, MUTED);

        y += 43;
        for (String cat : CATEGORIES) {
            boolean active = cat.equals(category);
            boolean hover = inside(mouseX, mouseY, l.x + 5, y - 4, l.sidebarW - 10, 21);

            if (active) {
                context.fill(l.x + 5, y - 4, l.x + l.sidebarW - 5, y + 17, 0x8832254D);
                context.fill(l.x + 5, y - 4, l.x + 8, y + 17, PURPLE);
            } else if (hover) {
                context.fill(l.x + 5, y - 4, l.x + l.sidebarW - 5, y + 17, 0x55231E2B);
            }

            int color = active ? PURPLE_LIGHT : TEXT;
            context.drawTextWithShadow(textRenderer, cat, x, y, color);

            int count = nexora.modules().category(cat).size();
            if (count > 0) {
                String badge = String.valueOf(count);
                context.drawTextWithShadow(textRenderer, badge,
                        l.x + l.sidebarW - 17, y, active ? PURPLE_LIGHT : MUTED);
            }
            y += 24;
        }

        int fy = l.y + l.h - 44;
        context.drawTextWithShadow(textRenderer, "Right Shift  close", x, fy, MUTED);
        context.drawTextWithShadow(textRenderer, "L-click toggle", x, fy + 11, MUTED);
        context.drawTextWithShadow(textRenderer, "R-click settings", x, fy + 22, MUTED);
    }

    private void drawModuleColumn(DrawContext context, int mouseX, int mouseY, Layout l) {
        int x = l.x + l.sidebarW + 7;
        int y = l.y + 10;
        int w = l.moduleW - 14;

        context.drawTextWithShadow(textRenderer, category.toUpperCase(Locale.ROOT), x, y, TEXT);
        context.drawTextWithShadow(textRenderer, "modules", x, y + 12, MUTED);

        y += 34;
        List<Module> modules = nexora.modules().category(category);
        if (modules.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "Nothing here yet.", x, y, MUTED);
            return;
        }

        for (Module module : modules) {
            boolean hover = inside(mouseX, mouseY, x, y, w, 25);
            boolean chosen = selected != null && selected.name().equals(module.name());

            context.fill(x, y, x + w, y + 25, chosen ? SELECTED : (hover ? ROW_HOVER : ROW));
            if (chosen) context.fill(x, y, x + 2, y + 25, PURPLE);

            context.drawTextWithShadow(textRenderer, module.name(), x + 7, y + 8,
                    module.enabled() ? TEXT : 0xFFD5CFDD);
            drawToggle(context, x + w - 31, y + 6, module.enabled());

            y += 29;
        }
    }

    private void drawSettings(DrawContext context, int mouseX, int mouseY, Layout l, int x) {
        int y = l.y + 10;
        int w = l.settingsW - 20;
        int cx = x + 10;

        context.drawTextWithShadow(textRenderer, selected.name(), cx, y, PURPLE_LIGHT);
        context.drawTextWithShadow(textRenderer, selected.category(), x + l.settingsW - 60, y, MUTED);
        y += 15;
        drawWrapped(context, selected.description(), cx, y, w, MUTED, 2);
        y += 29;

        switch (selected.name()) {
            case "ESP" -> drawEspSettings(context, mouseX, mouseY, cx, y, w);
            case "BlockESP" -> drawBlockSettings(context, mouseX, mouseY, cx, y, w, true);
            case "Fly" -> drawFloatSetting(context, mouseX, mouseY, cx, y, w,
                    "Fly Speed", nexora.flySpeed(), 0.05f, 1.0f);
            case "Speed" -> drawFloatSetting(context, mouseX, mouseY, cx, y, w,
                    "Speed Multiplier", nexora.speedMultiplier(), 1.0f, 3.0f);
            case "StorageESP" -> {
                settingRow(context, mouseX, mouseY, cx, y, w, "Storage ESP", selected.enabled());
                y += 24;
                info(context, cx, y, "Gold boxes + through-wall labels");
                info(context, cx, y + 12, "Chest / barrel / hopper / shulker");
            }
            case "BaseFinder" -> {
                settingRow(context, mouseX, mouseY, cx, y, w, "Base Finder", selected.enabled());
                y += 24;
                info(context, cx, y, "Clusters loaded storage locations.");
                info(context, cx, y + 12, "Purple markers = stronger cluster score.");
            }
            case "Waypoints" -> {
                settingRow(context, mouseX, mouseY, cx, y, w, "Waypoint ESP", selected.enabled());
                y += 24;
                info(context, cx, y, "Use .wp add <name> at your position.");
                info(context, cx, y + 12, "Labels stay visible through walls.");
            }
            case "Tracers" -> {
                settingRow(context, mouseX, mouseY, cx, y, w, "Tracers", selected.enabled());
                y += 24;
                info(context, cx, y, "Tracks enabled ESP targets.");
            }
            default -> {
                settingRow(context, mouseX, mouseY, cx, y, w, "Enabled", selected.enabled());
                y += 24;
                info(context, cx, y, "Left-click the module to toggle it.");
            }
        }
    }

    private void drawEspSettings(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        section(context, x, y, "ENTITY TARGETS");
        y += 14;

        settingRow(context, mouseX, mouseY, x, y, w, "Players • wall glow", nexora.espPlayers());
        y += 22;
        settingRow(context, mouseX, mouseY, x, y, w, "Mobs • wall glow", nexora.espMobs());
        y += 22;
        settingRow(context, mouseX, mouseY, x, y, w, "Dropped Items", nexora.modules().enabled("ItemESP"));
        y += 22;
        settingRow(context, mouseX, mouseY, x, y, w, "End Crystals", nexora.modules().enabled("CrystalESP"));
        y += 22;
        settingRow(context, mouseX, mouseY, x, y, w, "Tracers", nexora.modules().enabled("Tracers"));
        y += 24;

        numericRow(context, mouseX, mouseY, x, y, w, "Entity Range", nexora.espRange());
        y += 26;

        section(context, x, y, "BLOCK ESP");
        y += 14;
        settingRow(context, mouseX, mouseY, x, y, w, "Enable Block ESP", nexora.modules().enabled("BlockESP"));
        y += 22;
        numericRow(context, mouseX, mouseY, x, y, w, "Block Range", nexora.blockEsp().scanRange());
        y += 27;

        drawBlockGrid(context, mouseX, mouseY, x, y, w);
    }

    private void drawBlockSettings(DrawContext context, int mouseX, int mouseY, int x, int y, int w, boolean title) {
        if (title) {
            settingRow(context, mouseX, mouseY, x, y, w, "Block ESP", nexora.modules().enabled("BlockESP"));
            y += 22;
            numericRow(context, mouseX, mouseY, x, y, w, "Scan Range", nexora.blockEsp().scanRange());
            y += 27;
        }
        drawBlockGrid(context, mouseX, mouseY, x, y, w);
    }

    private void drawBlockGrid(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        section(context, x, y, "CLICK BLOCKS TO ADD / REMOVE");
        y += 14;

        int gap = 4;
        int colW = (w - gap) / 2;
        int rowH = 19;

        for (int i = 0; i < BLOCK_PRESETS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + col * (colW + gap);
            int by = y + row * rowH;
            boolean active = nexora.blockEsp().contains(BLOCK_PRESETS[i][0]);
            boolean hover = inside(mouseX, mouseY, bx, by, colW, rowH - 2);

            context.fill(bx, by, bx + colW, by + rowH - 2, hover ? ROW_HOVER : ROW);
            context.drawTextWithShadow(textRenderer, BLOCK_PRESETS[i][1], bx + 5, by + 5,
                    active ? TEXT : MUTED);
            context.drawTextWithShadow(textRenderer, active ? "✓" : "+",
                    bx + colW - 12, by + 5, active ? GREEN : PURPLE_LIGHT);
        }

        int bottom = y + 6 * rowH + 3;
        context.drawTextWithShadow(textRenderer, ".blockesp add minecraft:block",
                x, bottom, MUTED);
    }

    private void drawFloatSetting(DrawContext context, int mouseX, int mouseY, int x, int y, int w,
                                  String name, float value, float min, float max) {
        context.fill(x, y, x + w, y + 36, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 7, y + 7, TEXT);
        context.drawTextWithShadow(textRenderer,
                String.format(Locale.ROOT, "%.2f", value), x + 7, y + 20, PURPLE_LIGHT);

        drawSmallButton(context, mouseX, mouseY, x + w - 51, y + 9, 20, 18, "−");
        drawSmallButton(context, mouseX, mouseY, x + w - 24, y + 9, 20, 18, "+");

        int barY = y + 42;
        context.fill(x, barY, x + w, barY + 3, 0xFF302A39);
        float t = (value - min) / (max - min);
        context.fill(x, barY, x + (int)(w * t), barY + 3, PURPLE);
    }

    private void numericRow(DrawContext context, int mouseX, int mouseY, int x, int y, int w,
                            String name, int value) {
        context.fill(x, y, x + w, y + 22, ROW);
        context.drawTextWithShadow(textRenderer, name, x + 7, y + 7, TEXT);
        drawSmallButton(context, mouseX, mouseY, x + w - 72, y + 2, 20, 18, "−");
        context.drawCenteredTextWithShadow(textRenderer, String.valueOf(value), x + w - 40, y + 7, TEXT);
        drawSmallButton(context, mouseX, mouseY, x + w - 22, y + 2, 20, 18, "+");
    }

    private void settingRow(DrawContext context, int mouseX, int mouseY, int x, int y, int w,
                            String name, boolean enabled) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 20);
        context.fill(x, y, x + w, y + 20, hover ? ROW_HOVER : ROW);
        context.drawTextWithShadow(textRenderer, name, x + 6, y + 6, TEXT);
        drawToggle(context, x + w - 30, y + 3, enabled);
    }

    private void drawToggle(DrawContext context, int x, int y, boolean on) {
        context.fill(x, y, x + 25, y + 13, on ? PURPLE : 0xFF403A48);
        int knobX = on ? x + 14 : x + 2;
        context.fill(knobX, y + 2, knobX + 9, y + 11, 0xFFF5F2F9);
    }

    private void drawSmallButton(DrawContext context, int mouseX, int mouseY,
                                 int x, int y, int w, int h, String label) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        context.fill(x, y, x + w, y + h, hover ? 0xFF4C3A68 : 0xFF342B43);
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 5, TEXT);
    }

    private void section(DrawContext context, int x, int y, String label) {
        context.drawTextWithShadow(textRenderer, label, x, y, MUTED);
    }

    private void info(DrawContext context, int x, int y, String label) {
        context.drawTextWithShadow(textRenderer, label, x, y, MUTED);
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int width, int color, int maxLines) {
        int maxChars = Math.max(20, width / 6);
        String remaining = text;
        int line = 0;
        while (!remaining.isEmpty() && line < maxLines) {
            if (remaining.length() <= maxChars) {
                context.drawTextWithShadow(textRenderer, remaining, x, y + line * 11, color);
                break;
            }
            int cut = remaining.lastIndexOf(' ', maxChars);
            if (cut <= 0) cut = maxChars;
            context.drawTextWithShadow(textRenderer, remaining.substring(0, cut), x, y + line * 11, color);
            remaining = remaining.substring(Math.min(remaining.length(), cut + 1));
            line++;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Layout l = layout();
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        int cy = l.y + 53;
        for (String cat : CATEGORIES) {
            if (inside(mx, my, l.x + 5, cy - 4, l.sidebarW - 10, 21)) {
                category = cat;
                List<Module> list = nexora.modules().category(category);
                selected = list.isEmpty() ? null : list.getFirst();
                return true;
            }
            cy += 24;
        }

        int moduleX = l.x + l.sidebarW + 7;
        int moduleY = l.y + 44;
        int moduleW = l.moduleW - 14;
        for (Module module : nexora.modules().category(category)) {
            if (inside(mx, my, moduleX, moduleY, moduleW, 25)) {
                selected = module;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    module.toggle();
                    nexora.onModuleToggled(module);
                }
                return true;
            }
            moduleY += 29;
        }

        if (selected == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }

        int sx = l.x + l.leftW + 17;
        int sy = l.y + 54;
        int sw = l.settingsW - 20;

        if (selected.name().equals("ESP")) {
            if (inside(mx, my, sx, sy, sw, 20)) {
                nexora.setEspPlayers(!nexora.espPlayers());
                return true;
            }
            sy += 22;
            if (inside(mx, my, sx, sy, sw, 20)) {
                nexora.setEspMobs(!nexora.espMobs());
                return true;
            }
            sy += 22;
            if (inside(mx, my, sx, sy, sw, 20)) {
                toggleModule("ItemESP");
                return true;
            }
            sy += 22;
            if (inside(mx, my, sx, sy, sw, 20)) {
                toggleModule("CrystalESP");
                return true;
            }
            sy += 22;
            if (inside(mx, my, sx, sy, sw, 20)) {
                toggleModule("Tracers");
                return true;
            }
            sy += 24;

            if (inside(mx, my, sx + sw - 72, sy + 2, 20, 18)) {
                nexora.setEspRange(nexora.espRange() - 16);
                return true;
            }
            if (inside(mx, my, sx + sw - 22, sy + 2, 20, 18)) {
                nexora.setEspRange(nexora.espRange() + 16);
                return true;
            }
            sy += 40;

            if (inside(mx, my, sx, sy, sw, 20)) {
                toggleModule("BlockESP");
                return true;
            }
            sy += 22;
            if (inside(mx, my, sx + sw - 72, sy + 2, 20, 18)) {
                nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 8);
                return true;
            }
            if (inside(mx, my, sx + sw - 22, sy + 2, 20, 18)) {
                nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 8);
                return true;
            }
            sy += 41;

            if (handleBlockGridClick(mx, my, sx, sy, sw)) return true;
        }

        if (selected.name().equals("BlockESP")) {
            if (inside(mx, my, sx, sy, sw, 20)) {
                toggleModule("BlockESP");
                return true;
            }
            sy += 22;

            if (inside(mx, my, sx + sw - 72, sy + 2, 20, 18)) {
                nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 8);
                return true;
            }
            if (inside(mx, my, sx + sw - 22, sy + 2, 20, 18)) {
                nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 8);
                return true;
            }
            sy += 41;
            if (handleBlockGridClick(mx, my, sx, sy, sw)) return true;
        }

        if (selected.name().equals("Fly")) {
            if (inside(mx, my, sx + sw - 51, sy + 9, 20, 18)) {
                nexora.setFlySpeed(nexora.flySpeed() - 0.05f);
                return true;
            }
            if (inside(mx, my, sx + sw - 24, sy + 9, 20, 18)) {
                nexora.setFlySpeed(nexora.flySpeed() + 0.05f);
                return true;
            }
        }

        if (selected.name().equals("Speed")) {
            if (inside(mx, my, sx + sw - 51, sy + 9, 20, 18)) {
                nexora.setSpeedMultiplier(nexora.speedMultiplier() - 0.10f);
                return true;
            }
            if (inside(mx, my, sx + sw - 24, sy + 9, 20, 18)) {
                nexora.setSpeedMultiplier(nexora.speedMultiplier() + 0.10f);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean handleBlockGridClick(double mx, double my, int x, int y, int w) {
        int gap = 4;
        int colW = (w - gap) / 2;
        int rowH = 19;

        for (int i = 0; i < BLOCK_PRESETS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + col * (colW + gap);
            int by = y + row * rowH;
            if (inside(mx, my, bx, by, colW, rowH - 2)) {
                nexora.blockEsp().toggle(BLOCK_PRESETS[i][0]);
                return true;
            }
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

    private Layout layout() {
        int sidebarW = 145;
        int moduleW = 155;
        int leftW = sidebarW + moduleW;
        int settingsW = 300;
        int h = Math.min(390, Math.max(300, height - 20));

        int x = 10;
        int y = 10;

        return new Layout(x, y, h, sidebarW, moduleW, leftW, settingsW);
    }

    private void shadow(DrawContext context, int x, int y, int w, int h) {
        context.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x55000000);
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x66000000);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Layout(int x, int y, int h, int sidebarW, int moduleW, int leftW, int settingsW) {}
}
