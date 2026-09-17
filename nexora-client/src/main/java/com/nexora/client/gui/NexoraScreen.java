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
    private static final int BG = 0x99000000;
    private static final int PANEL = 0xF0111017;
    private static final int SIDEBAR = 0xF016141D;
    private static final int CARD = 0xE91B1923;
    private static final int CARD_HOVER = 0xF2252230;
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int PURPLE_SOFT = 0xFFB99CFF;
    private static final int TEXT = 0xFFF4F1FA;
    private static final int MUTED = 0xFF9D96A8;
    private static final int GREEN = 0xFF63E6A6;
    private static final int RED = 0xFFFF6B7A;

    private static final String[] CATEGORIES = {
            "Combat", "Movement", "Render", "World", "Misc"
    };

    private static final String[][] BLOCK_PRESETS = {
            {"minecraft:diamond_ore", "Diamond Ore"},
            {"minecraft:deepslate_diamond_ore", "Deepslate Diamond"},
            {"minecraft:ancient_debris", "Ancient Debris"},
            {"minecraft:spawner", "Spawner"},
            {"minecraft:chest", "Chest"},
            {"minecraft:shulker_box", "Shulker Box"}
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
        // Custom-drawn UI: no vanilla gray widgets.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, BG);

        Layout l = layout();

        shadow(context, l.x, l.y, l.w, l.h);
        context.fill(l.x, l.y, l.x + l.w, l.y + l.h, PANEL);
        context.fill(l.x, l.y, l.x + l.sidebarW, l.y + l.h, SIDEBAR);
        context.fill(l.x, l.y, l.x + l.w, l.y + 2, PURPLE);

        drawSidebar(context, mouseX, mouseY, l);
        drawModules(context, mouseX, mouseY, l);
        drawSettings(context, mouseX, mouseY, l);
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY, Layout l) {
        int x = l.x + 12;
        int y = l.y + 13;

        context.drawTextWithShadow(textRenderer, "✦ NEXORA", x, y, PURPLE_SOFT);
        context.drawTextWithShadow(textRenderer, "CLIENT 0.2", x, y + 14, MUTED);

        y += 48;
        for (String cat : CATEGORIES) {
            boolean active = cat.equals(category);
            boolean hover = inside(mouseX, mouseY, l.x + 6, y - 5, l.sidebarW - 12, 22);
            if (active) {
                context.fill(l.x + 5, y - 5, l.x + l.sidebarW - 5, y + 17, 0x552F2450);
                context.fill(l.x + 5, y - 5, l.x + 8, y + 17, PURPLE);
            } else if (hover) {
                context.fill(l.x + 5, y - 5, l.x + l.sidebarW - 5, y + 17, 0x332B2733);
            }
            context.drawTextWithShadow(textRenderer, cat, x, y, active ? PURPLE_SOFT : TEXT);
            y += 26;
        }

        int fy = l.y + l.h - 37;
        context.drawTextWithShadow(textRenderer, "Right Shift", x, fy, MUTED);
        context.drawTextWithShadow(textRenderer, "Right-click = settings", x, fy + 12, MUTED);
    }

    private void drawModules(DrawContext context, int mouseX, int mouseY, Layout l) {
        int x = l.x + l.sidebarW + 10;
        int y = l.y + 12;
        int w = l.moduleW - 20;

        context.drawTextWithShadow(textRenderer, category.toUpperCase(Locale.ROOT), x, y, TEXT);
        context.drawTextWithShadow(textRenderer, "left toggle • right settings", x, y + 13, MUTED);

        y += 39;
        List<Module> list = nexora.modules().category(category);
        if (list.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No modules here yet.", x, y, MUTED);
            return;
        }

        for (Module module : list) {
            boolean hover = inside(mouseX, mouseY, x, y, w, 31);
            boolean chosen = selected != null && selected.name().equals(module.name());
            int color = chosen ? 0xFF262032 : (hover ? CARD_HOVER : CARD);
            context.fill(x, y, x + w, y + 31, color);
            if (chosen) context.fill(x, y, x + 3, y + 31, PURPLE);

            context.drawTextWithShadow(textRenderer, module.name(), x + 9, y + 7, TEXT);
            drawToggle(context, x + w - 35, y + 7, module.enabled());

            y += 36;
        }
    }

    private void drawSettings(DrawContext context, int mouseX, int mouseY, Layout l) {
        int x = l.x + l.sidebarW + l.moduleW + 10;
        int y = l.y + 12;
        int w = l.w - l.sidebarW - l.moduleW - 20;

        context.drawTextWithShadow(textRenderer, "SETTINGS", x, y, TEXT);

        if (selected == null) {
            context.drawTextWithShadow(textRenderer, "Right-click a module.", x, y + 27, MUTED);
            return;
        }

        context.drawTextWithShadow(textRenderer, selected.name(), x, y + 20, PURPLE_SOFT);
        drawWrapped(context, selected.description(), x, y + 34, w, MUTED);
        y += 60;

        switch (selected.name()) {
            case "ESP", "BlockESP" -> drawEspSettings(context, mouseX, mouseY, x, y, w);
            case "Fly" -> drawFloatSetting(context, mouseX, mouseY, x, y, w,
                    "Fly speed", nexora.flySpeed(), 0.05f, 1.0f);
            case "Speed" -> drawFloatSetting(context, mouseX, mouseY, x, y, w,
                    "Multiplier", nexora.speedMultiplier(), 1.0f, 3.0f);
            case "BaseFinder" -> {
                settingRow(context, mouseX, mouseY, x, y, w, "Storage cluster scan", selected.enabled());
                context.drawTextWithShadow(textRenderer, "Uses loaded nearby blocks only.", x, y + 29, MUTED);
            }
            case "StorageESP" -> {
                settingRow(context, mouseX, mouseY, x, y, w, "Storage scan", selected.enabled());
                context.drawTextWithShadow(textRenderer, "Chest • Barrel • Hopper • Shulker", x, y + 29, MUTED);
            }
            default -> {
                settingRow(context, mouseX, mouseY, x, y, w, "Enabled", selected.enabled());
                context.drawTextWithShadow(textRenderer, "Left-click module to toggle.", x, y + 29, MUTED);
            }
        }
    }

    private void drawEspSettings(DrawContext context, int mouseX, int mouseY, int x, int y, int w) {
        settingRow(context, mouseX, mouseY, x, y, w, "Players", nexora.espPlayers());
        y += 25;
        settingRow(context, mouseX, mouseY, x, y, w, "Mobs", nexora.espMobs());
        y += 25;
        settingRow(context, mouseX, mouseY, x, y, w, "Block ESP", nexora.modules().enabled("BlockESP"));
        y += 27;

        context.drawTextWithShadow(textRenderer, "Scan range", x, y + 4, MUTED);
        drawSmallButton(context, mouseX, mouseY, x + w - 86, y, 22, 18, "−");
        context.drawCenteredTextWithShadow(textRenderer, String.valueOf(nexora.blockEsp().scanRange()), x + w - 52, y + 5, TEXT);
        drawSmallButton(context, mouseX, mouseY, x + w - 24, y, 22, 18, "+");
        y += 28;

        context.drawTextWithShadow(textRenderer, "TRACKED BLOCKS", x, y, MUTED);
        context.drawTextWithShadow(textRenderer, "click to add/remove", x + Math.max(76, w - 96), y, 0xFF777080);
        y += 15;

        int rowH = 18;
        for (String[] preset : BLOCK_PRESETS) {
            if (y + rowH > height - 14) break;
            boolean active = nexora.blockEsp().contains(preset[0]);
            boolean hover = inside(mouseX, mouseY, x, y, w, rowH - 1);
            context.fill(x, y, x + w, y + rowH - 1, hover ? CARD_HOVER : 0xAA181620);
            context.drawTextWithShadow(textRenderer, preset[1], x + 6, y + 5, active ? TEXT : MUTED);
            context.drawTextWithShadow(textRenderer, active ? "✓" : "+", x + w - 14, y + 5, active ? GREEN : PURPLE_SOFT);
            y += rowH;
        }
    }

    private void drawFloatSetting(DrawContext context, int mouseX, int mouseY, int x, int y, int w,
                                  String name, float value, float min, float max) {
        context.fill(x, y, x + w, y + 34, CARD);
        context.drawTextWithShadow(textRenderer, name, x + 8, y + 7, TEXT);
        context.drawTextWithShadow(textRenderer,
                String.format(Locale.ROOT, "%.2f", value), x + 8, y + 20, PURPLE_SOFT);

        drawSmallButton(context, mouseX, mouseY, x + w - 54, y + 8, 20, 18, "−");
        drawSmallButton(context, mouseX, mouseY, x + w - 27, y + 8, 20, 18, "+");

        int barX = x;
        int barY = y + 40;
        int barW = w;
        context.fill(barX, barY, barX + barW, barY + 4, 0xFF2D2935);
        float t = (value - min) / (max - min);
        context.fill(barX, barY, barX + (int) (barW * t), barY + 4, PURPLE);
    }

    private void settingRow(DrawContext context, int mouseX, int mouseY, int x, int y, int w,
                            String name, boolean enabled) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 22);
        context.fill(x, y, x + w, y + 22, hover ? CARD_HOVER : CARD);
        context.drawTextWithShadow(textRenderer, name, x + 7, y + 7, TEXT);
        drawToggle(context, x + w - 35, y + 4, enabled);
    }

    private void drawToggle(DrawContext context, int x, int y, boolean on) {
        context.fill(x, y, x + 28, y + 14, on ? PURPLE : 0xFF3A3542);
        int knobX = on ? x + 16 : x + 2;
        context.fill(knobX, y + 2, knobX + 10, y + 12, 0xFFF3F0F8);
    }

    private void drawSmallButton(DrawContext context, int mouseX, int mouseY,
                                 int x, int y, int w, int h, String label) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        context.fill(x, y, x + w, y + h, hover ? 0xFF493762 : 0xFF30283E);
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 5, TEXT);
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int width, int color) {
        int maxChars = Math.max(18, width / 6);
        String remaining = text;
        int line = 0;
        while (!remaining.isEmpty() && line < 2) {
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

        int cy = l.y + 60;
        for (String cat : CATEGORIES) {
            if (inside(mx, my, l.x + 5, cy - 5, l.sidebarW - 10, 22)) {
                category = cat;
                List<Module> list = nexora.modules().category(category);
                selected = list.isEmpty() ? null : list.getFirst();
                return true;
            }
            cy += 26;
        }

        int moduleX = l.x + l.sidebarW + 10;
        int moduleY = l.y + 51;
        int moduleW = l.moduleW - 20;
        for (Module module : nexora.modules().category(category)) {
            if (inside(mx, my, moduleX, moduleY, moduleW, 31)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    selected = module;
                    return true;
                }
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    module.toggle();
                    nexora.onModuleToggled(module);
                    selected = module;
                    return true;
                }
            }
            moduleY += 36;
        }

        if (selected != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int sx = l.x + l.sidebarW + l.moduleW + 10;
            int sy = l.y + 72;
            int sw = l.w - l.sidebarW - l.moduleW - 20;

            if (selected.name().equals("ESP") || selected.name().equals("BlockESP")) {
                if (inside(mx, my, sx, sy, sw, 22)) {
                    nexora.setEspPlayers(!nexora.espPlayers());
                    return true;
                }
                sy += 25;
                if (inside(mx, my, sx, sy, sw, 22)) {
                    nexora.setEspMobs(!nexora.espMobs());
                    return true;
                }
                sy += 25;
                if (inside(mx, my, sx, sy, sw, 22)) {
                    Module block = nexora.modules().get("BlockESP");
                    block.toggle();
                    nexora.onModuleToggled(block);
                    return true;
                }
                sy += 27;

                if (inside(mx, my, sx + sw - 86, sy, 22, 18)) {
                    nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() - 4);
                    return true;
                }
                if (inside(mx, my, sx + sw - 24, sy, 22, 18)) {
                    nexora.blockEsp().setScanRange(nexora.blockEsp().scanRange() + 4);
                    return true;
                }

                sy += 43;
                for (String[] preset : BLOCK_PRESETS) {
                    if (inside(mx, my, sx, sy, sw, 17)) {
                        nexora.blockEsp().toggle(preset[0]);
                        return true;
                    }
                    sy += 18;
                }
            }

            if (selected.name().equals("Fly")) {
                if (inside(mx, my, sx + sw - 54, sy + 8, 20, 18)) {
                    nexora.setFlySpeed(nexora.flySpeed() - 0.05f);
                    return true;
                }
                if (inside(mx, my, sx + sw - 27, sy + 8, 20, 18)) {
                    nexora.setFlySpeed(nexora.flySpeed() + 0.05f);
                    return true;
                }
            }

            if (selected.name().equals("Speed")) {
                if (inside(mx, my, sx + sw - 54, sy + 8, 20, 18)) {
                    nexora.setSpeedMultiplier(nexora.speedMultiplier() - 0.10f);
                    return true;
                }
                if (inside(mx, my, sx + sw - 27, sy + 8, 20, 18)) {
                    nexora.setSpeedMultiplier(nexora.speedMultiplier() + 0.10f);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private Layout layout() {
        int w = Math.min(760, Math.max(420, width - 16));
        int h = Math.min(410, Math.max(220, height - 16));
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        int sidebarW = w < 560 ? 105 : 125;
        int moduleW = w < 560 ? 145 : 185;
        return new Layout(x, y, w, h, sidebarW, moduleW);
    }

    private void shadow(DrawContext context, int x, int y, int w, int h) {
        context.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0x44000000);
        context.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x55000000);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Layout(int x, int y, int w, int h, int sidebarW, int moduleW) {}
}
