package com.nexora.client;

import com.nexora.client.command.CommandManager;
import com.nexora.client.core.Module;
import com.nexora.client.core.ModuleManager;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.BlockEspManager;
import com.nexora.client.feature.RelogManager;
import com.nexora.client.feature.WaypointManager;
import com.nexora.client.gui.NexoraScreen;
import com.nexora.client.render.WorldEspRenderer;
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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class NexoraClient implements ClientModInitializer {
    public static final String MOD_ID = "nexora";
    public static NexoraClient INSTANCE;

    private final ModuleManager modules = new ModuleManager();
    private final WaypointManager waypoints = new WaypointManager();
    private final BaseFinder baseFinder = new BaseFinder();
    private final BlockEspManager blockEsp = new BlockEspManager();
    private final RelogManager relog = new RelogManager();
    private final WorldEspRenderer worldEspRenderer = new WorldEspRenderer(this);

    private CommandManager commands;
    private KeyBinding openGui;

    private float flySpeed = 0.10f;
    private float speedMultiplier = 1.45f;
    private boolean espPlayers = true;
    private boolean espMobs = true;

    private boolean rememberedAllowFlying;
    private boolean flyStateCaptured;
    private Double rememberedGamma;

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
        worldEspRenderer.register();
    }

    private void tick(MinecraftClient client) {
        while (openGui.wasPressed()) {
            if (client.currentScreen instanceof NexoraScreen) client.setScreen(null);
            else client.setScreen(new NexoraScreen(this));
        }

        relog.tick(client);
        if (client.player == null || client.world == null) {
            restoreFullbright(client);
            return;
        }

        handleFlight(client);
        handleSprint(client);
        handleAutoWalk(client);
        handleSpeed(client);
        handleFullbright(client);
        handleEsp(client);

        blockEsp.tick(client, modules.enabled("BlockESP"));
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
        if (modules.enabled("Sprint") && client.options.forwardKey.isPressed()) {
            client.player.setSprinting(true);
        }
    }

    private void handleAutoWalk(MinecraftClient client) {
        if (modules.enabled("AutoWalk")) {
            client.options.forwardKey.setPressed(true);
        }
    }

    private void handleSpeed(MinecraftClient client) {
        if (!modules.enabled("Speed") || client.player.isClimbing()) return;

        double forward = 0.0;
        double strafe = 0.0;
        if (client.options.forwardKey.isPressed()) forward += 1.0;
        if (client.options.backKey.isPressed()) forward -= 1.0;
        if (client.options.leftKey.isPressed()) strafe += 1.0;
        if (client.options.rightKey.isPressed()) strafe -= 1.0;
        if (forward == 0.0 && strafe == 0.0) return;

        double length = Math.sqrt(forward * forward + strafe * strafe);
        forward /= length;
        strafe /= length;

        double yaw = Math.toRadians(client.player.getYaw());
        double base = 0.115 * speedMultiplier;
        double x = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * base;
        double z = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * base;

        Vec3d old = client.player.getVelocity();
        client.player.setVelocity(x, old.y, z);
    }

    private void handleFullbright(MinecraftClient client) {
        if (modules.enabled("Fullbright")) {
            if (rememberedGamma == null) rememberedGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(1.0);
        } else {
            restoreFullbright(client);
        }
    }

    private void restoreFullbright(MinecraftClient client) {
        if (rememberedGamma != null) {
            client.options.getGamma().setValue(rememberedGamma);
            rememberedGamma = null;
        }
    }

    private void handleEsp(MinecraftClient client) {
        boolean entityEsp = modules.enabled("ESP");
        boolean crystalEsp = modules.enabled("CrystalESP");

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (entity.squaredDistanceTo(client.player) > 160 * 160) continue;

            boolean glow = false;
            if (crystalEsp && entity.getType() == EntityType.END_CRYSTAL) {
                glow = true;
            }
            if (entityEsp && entity instanceof LivingEntity) {
                if (entity instanceof PlayerEntity) glow = espPlayers;
                else glow = espMobs;
            }

            if (glow) entity.setGlowing(true);
            else if (entity.isGlowingLocal()) entity.setGlowing(false);
        }
    }

    private void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !modules.enabled("HUD")) return;

        final int purple = 0xFFB58CFF;
        final int purple2 = 0xFF8B5CF6;
        final int white = 0xFFF4F1FA;
        final int muted = 0xFFA9A3B5;
        final int panel = 0xB5100E16;

        int x = 7;
        int y = 7;
        context.fill(3, 3, 164, 33, panel);
        context.fill(3, 3, 164, 5, purple2);
        context.drawTextWithShadow(client.textRenderer, "NEXORA", x, y, purple);
        context.drawTextWithShadow(client.textRenderer,
                "XYZ " + client.player.getBlockX() + " " + client.player.getBlockY() + " " + client.player.getBlockZ(),
                x, y + 13, white);

        int line = y + 37;
        for (Module module : modules.all()) {
            if (!module.enabled() || module.name().equals("HUD")) continue;
            context.drawTextWithShadow(client.textRenderer, module.name(), x, line, purple);
            line += 11;
            if (line > 170) break;
        }

        boolean showRadar = modules.enabled("BlockESP") || modules.enabled("StorageESP") || modules.enabled("BaseFinder");
        if (!showRadar) return;

        int radarW = 190;
        int rx = Math.max(4, context.getScaledWindowWidth() - radarW - 6);
        int ry = 6;
        int maxRows = 9;
        int radarH = 31 + maxRows * 11;
        context.fill(rx, ry, rx + radarW, ry + radarH, panel);
        context.fill(rx, ry, rx + radarW, ry + 2, purple2);
        context.drawTextWithShadow(client.textRenderer, "NEXORA RADAR", rx + 7, ry + 7, purple);
        context.drawTextWithShadow(client.textRenderer, "loaded client data", rx + 7, ry + 18, muted);

        int row = 0;
        int ty = ry + 33;

        if (modules.enabled("BaseFinder")) {
            for (BaseFinder.BaseCandidate candidate : baseFinder.candidates()) {
                if (row++ >= maxRows) break;
                String s = "Possible Base  " + (int) candidate.distance() + "m  [" + candidate.score() + "]";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFFFF72E8);
                ty += 11;
            }
        }

        if (modules.enabled("BlockESP") && row < maxRows) {
            for (BlockEspManager.BlockHit hit : blockEsp.hits()) {
                if (row++ >= maxRows) break;
                String s = shortBlockName(hit.blockId()) + "  " + (int) hit.distance() + "m";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFF7CEAFF);
                ty += 11;
            }
        }

        if (modules.enabled("StorageESP") && row < maxRows) {
            for (BaseFinder.StorageHit hit : baseFinder.hits()) {
                if (row++ >= maxRows) break;
                String s = hit.type() + "  " + (int) hit.distance() + "m";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFFFFD76A);
                ty += 11;
            }
        }
    }

    private String shortBlockName(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = s.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return out.toString();
    }

    public void onModuleToggled(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();

        if ((module.name().equalsIgnoreCase("ESP") || module.name().equalsIgnoreCase("CrystalESP"))
                && !module.enabled() && client.world != null) {
            for (Entity entity : client.world.getEntities()) {
                if (entity != client.player && entity.isGlowingLocal()) entity.setGlowing(false);
            }
        }

        if (module.name().equalsIgnoreCase("AutoWalk") && !module.enabled()) {
            client.options.forwardKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("Fullbright") && !module.enabled()) {
            restoreFullbright(client);
        }
    }

    public ModuleManager modules() { return modules; }
    public WaypointManager waypoints() { return waypoints; }
    public BaseFinder baseFinder() { return baseFinder; }
    public BlockEspManager blockEsp() { return blockEsp; }
    public RelogManager relog() { return relog; }

    public float flySpeed() { return flySpeed; }
    public void setFlySpeed(float value) {
        flySpeed = Math.max(0.05f, Math.min(1.0f, value));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && modules.enabled("Fly")) {
            client.player.getAbilities().setFlySpeed(flySpeed);
        }
    }

    public float speedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(float value) {
        speedMultiplier = Math.max(1.0f, Math.min(3.0f, value));
    }

    public boolean espPlayers() { return espPlayers; }
    public void setEspPlayers(boolean value) { espPlayers = value; }

    public boolean espMobs() { return espMobs; }
    public void setEspMobs(boolean value) { espMobs = value; }
}
