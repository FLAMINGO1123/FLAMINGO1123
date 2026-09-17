package com.nexora.client.gui;

import com.nexora.client.NexoraClient;
import com.nexora.client.core.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class NexoraScreen extends Screen {
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int PANEL = 0xE5121118;
    private static final int PANEL_2 = 0xE51A1722;
    private static final int TEXT = 0xFFF4F0FF;
    private static final int MUTED = 0xFFAAA2B8;

    private final NexoraClient nexora;
    private final List<ButtonWidget> moduleButtons = new ArrayList<>();

    public NexoraScreen(NexoraClient nexora) {
        super(Text.literal("Nexora Client"));
        this.nexora = nexora;
    }

    @Override
    protected void init() {
        clearChildren();
        moduleButtons.clear();

        int panelW = Math.min(560, width - 40);
        int panelX = (width - panelW) / 2;
        int contentX = panelX + 150;
        int y = 70;
        int buttonW = Math.max(180, panelW - 180);

        for (Module module : nexora.modules().all()) {
            ButtonWidget button = ButtonWidget.builder(label(module), b -> {
                module.toggle();
                nexora.onModuleToggled(module);
                b.setMessage(label(module));
            }).dimensions(contentX, y, buttonW, 22).build();
            addDrawableChild(button);
            moduleButtons.add(button);
            y += 27;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Fly Speed -"), b -> {
            nexora.setFlySpeed(Math.max(0.05f, nexora.flySpeed() - 0.05f));
        }).dimensions(contentX, y + 8, (buttonW - 6) / 2, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Fly Speed +"), b -> {
            nexora.setFlySpeed(Math.min(1.0f, nexora.flySpeed() + 0.05f));
        }).dimensions(contentX + (buttonW + 6) / 2, y + 8, (buttonW - 6) / 2, 20).build());
    }

    private Text label(Module module) {
        return Text.literal(module.name() + "  " + (module.enabled() ? "§aON" : "§cOFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        int panelW = Math.min(560, width - 40);
        int panelH = 285;
        int panelX = (width - panelW) / 2;
        int panelY = 35;

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        context.fill(panelX, panelY, panelX + 130, panelY + panelH, PANEL_2);
        context.fill(panelX, panelY, panelX + panelW, panelY + 3, PURPLE);

        context.drawTextWithShadow(textRenderer, "NEXORA", panelX + 18, panelY + 18, PURPLE);
        context.drawTextWithShadow(textRenderer, "CLIENT 1.21.11", panelX + 18, panelY + 32, MUTED);

        int sy = panelY + 70;
        String[] cats = {"COMBAT", "MOVEMENT", "RENDER", "WORLD", "UTILITY"};
        for (String cat : cats) {
            context.drawTextWithShadow(textRenderer, cat, panelX + 18, sy, cat.equals("RENDER") ? PURPLE : TEXT);
            sy += 25;
        }

        context.drawTextWithShadow(textRenderer, "Right Shift = open/close", panelX + 18, panelY + panelH - 42, MUTED);
        context.drawTextWithShadow(textRenderer, ".help = commands", panelX + 18, panelY + panelH - 28, MUTED);

        context.drawTextWithShadow(textRenderer, "Modules", panelX + 150, panelY + 20, TEXT);
        context.drawTextWithShadow(textRenderer,
                "Fly speed: " + String.format(java.util.Locale.ROOT, "%.2f", nexora.flySpeed()),
                panelX + 150, panelY + panelH - 34, PURPLE);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
