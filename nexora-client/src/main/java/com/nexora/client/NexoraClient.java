package com.nexora.client;

import com.nexora.client.command.CommandManager;
import com.nexora.client.core.Module;
import com.nexora.client.core.ModuleManager;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.RelogManager;
import com.nexora.client.feature.WaypointManager;
import com.nexora.client.gui.NexoraScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class NexoraClient implements ClientModInitializer {
    public static final String MOD_ID = "nexora";
    public static NexoraClient INSTANCE;

    private final ModuleManager modules = new ModuleManager();
    private final WaypointManager waypoints = new WaypointManager();
    private final BaseFinder baseFinder = new BaseFinder();
    private final RelogManager relog = new RelogManager();
    private CommandManager commands;
    private KeyBinding openGui;

    private float flySpeed = 0.10f;
    private boolean rememberedAllowFlying;
    private boolean flyStateCaptured;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        commands = new CommandManager(this);

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nexora.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> !commands.handle(message));
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context));
    }

    private void tick(MinecraftClient client) {
        while (openGui.wasPressed()) {
            if (client.currentScreen instanceof NexoraScreen) client.setScreen(null);
            else client.setScreen(new NexoraScreen(this));
        }

        relog.tick(client);
        if (client.player == null || client.world == null) return;

        handleFlight(client);
        handleSprint(client);
        handleEsp(client);
        baseFinder.tick(client, modules.enabled("StorageESP"), modules.enabled("BaseFinder"));
    }

    private void handleFlight(MinecraftClient client) {
        PlayerAbilities abilities = client.player.getAbilities();
        if (modules.enabled("Fly")) {
            if (!flyStateCaptured) {
                rememberedAllowFlying = abilities.allowFlying;
                flyStateCaptured = true;
            }
            abilities.allowFlying = true;
            abilities.flying = true;
            abilities.setFlySpeed(flySpeed);
        } else if (flyStateCaptured) {
            abilities.flying = false;
            abilities.allowFlying = rememberedAllowFlying || abilities.creativeMode;
            abilities.setFlySpeed(0.05f);
            flyStateCaptured = false;
        }
    }

    private void handleSprint(MinecraftClient client) {
        if (!modules.enabled("Sprint")) return;
        if (client.options.forwardKey.isPressed()) client.player.setSprinting(true);
    }

    private void handleEsp(MinecraftClient client) {
        boolean enabled = modules.enabled("ESP");
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == client.player) continue;
            if (entity.squaredDistanceTo(client.player) > 128 * 128) continue;
            if (enabled) entity.setGlowing(true);
            else if (entity.isGlowingLocal()) entity.setGlowing(false);
        }
    }

    private void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !modules.enabled("HUD")) return;

        int x = 6;
        int y = 6;
        int purple = 0xFFB48CFF;
        int white = 0xFFF5F1FF;
        int muted = 0xFFB7AFC5;

        context.fill(3, 3, 158, 31, 0x99100D16);
        context.drawTextWithShadow(client.textRenderer, "NEXORA", x, y, purple);
        context.drawTextWithShadow(client.textRenderer,
                "XYZ " + client.player.getBlockX() + " " + client.player.getBlockY() + " " + client.player.getBlockZ(),
                x, y + 12, white);

        int line = y + 31;
        for (Module module : modules.all()) {
            if (!module.enabled() || module.name().equals("HUD")) continue;
            context.drawTextWithShadow(client.textRenderer, module.name(), x, line, purple);
            line += 11;
        }

        if (modules.enabled("StorageESP")) {
            int shown = 0;
            for (BaseFinder.StorageHit hit : baseFinder.hits()) {
                if (shown++ >= 5) break;
                String s = hit.type() + "  " + hit.pos().getX() + " " + hit.pos().getY() + " " + hit.pos().getZ()
                        + "  " + (int) hit.distance() + "m";
                context.drawTextWithShadow(client.textRenderer, s, x, line, muted);
                line += 11;
            }
        }

        if (modules.enabled("BaseFinder")) {
            int shown = 0;
            for (BaseFinder.BaseCandidate candidate : baseFinder.candidates()) {
                if (shown++ >= 3) break;
                String s = "BASE? score " + candidate.score() + " @ "
                        + candidate.pos().getX() + " " + candidate.pos().getY() + " " + candidate.pos().getZ();
                context.drawTextWithShadow(client.textRenderer, s, x, line, 0xFFFF7BEF);
                line += 11;
            }
        }

        if (!waypoints.all().isEmpty()) {
            line += 4;
            context.drawTextWithShadow(client.textRenderer, "Waypoints", x, line, purple);
            line += 11;
            for (WaypointManager.Waypoint w : waypoints.all()) {
                if (line > 180) break;
                double dist = Math.sqrt(client.player.getBlockPos().getSquaredDistance(w.pos()));
                context.drawTextWithShadow(client.textRenderer,
                        w.name() + " " + (int) dist + "m", x, line, white);
                line += 11;
            }
        }
    }

    public void onModuleToggled(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (module.name().equalsIgnoreCase("ESP") && !module.enabled() && client.world != null) {
            for (Entity entity : client.world.getEntities()) {
                if (entity != client.player && entity.isGlowingLocal()) entity.setGlowing(false);
            }
        }
    }

    public ModuleManager modules() { return modules; }
    public WaypointManager waypoints() { return waypoints; }
    public BaseFinder baseFinder() { return baseFinder; }
    public RelogManager relog() { return relog; }
    public float flySpeed() { return flySpeed; }

    public void setFlySpeed(float flySpeed) {
        this.flySpeed = Math.max(0.05f, Math.min(1.0f, flySpeed));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && modules.enabled("Fly")) {
            client.player.getAbilities().setFlySpeed(this.flySpeed);
        }
    }
}
