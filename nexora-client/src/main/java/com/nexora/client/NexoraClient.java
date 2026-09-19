package com.nexora.client;

import com.nexora.client.command.CommandManager;
import com.nexora.client.core.Module;
import com.nexora.client.core.ModuleManager;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.BlockEspManager;
import com.nexora.client.feature.FreecamManager;
import com.nexora.client.feature.RelogManager;
import com.nexora.client.feature.WaypointManager;
import com.nexora.client.feature.XRayManager;
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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
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
    private final XRayManager xray = new XRayManager();
    private final FreecamManager freecam = new FreecamManager();
    private final RelogManager relog = new RelogManager();
    private final WorldEspRenderer worldEspRenderer = new WorldEspRenderer(this);

    private CommandManager commands;
    private KeyBinding openGui;

    private float flySpeed = 0.10f;
    private float speedMultiplier = 1.45f;
    private float highJumpPower = 0.72f;
    private float fastFallSpeed = 0.30f;
    private int zoomFov = 30;

    private boolean espPlayers = true;
    private boolean espMobs = true;
    private int espRange = 160;
    private boolean entityBoxes = true;
    private boolean entityLabels = true;
    private boolean worldLabels = true;
    private boolean worldBoxes = true;

    private boolean hudCoordinates = true;
    private boolean hudActiveModules = true;
    private int radarRows = 8;
    private int guiOpacity = 175;

    private boolean rememberedAllowFlying;
    private boolean flyStateCaptured;
    private Double rememberedGamma;
    private Integer rememberedFov;
    private boolean jumpWasPressed;
    private boolean attackWasPressed;
    private int antiAfkTicks;
    private int autoClickTicks;
    private int autoSwingTicks;
    private int autoDropTicks;
    private int handSwingTicks;
    private int utilityTicks;
    private int quickTurnTicks;

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
            restoreZoom(client);
            return;
        }

        handleFlight(client);
        handleSprint(client);
        handleHeldKeys(client);
        handleSpeed(client);
        handleJumpModules(client);
        handleFastFall(client);
        handleMovementExtras(client);
        handleFullbright(client);
        handleZoom(client);
        handleEsp(client);
        handleTriggerBot(client);
        handleCombatExtras(client);
        handlePlayerExtras(client);
        handleUtilityExtras(client);
        handleAntiAfk(client);
        freecam.tick(client);

        blockEsp.tick(client, modules.enabled("BlockESP"));
        xray.tick(client, modules.enabled("XRay"));
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

    private void handleHeldKeys(MinecraftClient client) {
        if (modules.enabled("AutoWalk")) client.options.forwardKey.setPressed(true);
        if (modules.enabled("AutoSneak")) client.options.sneakKey.setPressed(true);
        if (modules.enabled("AutoMine")) client.options.attackKey.setPressed(true);
        if (modules.enabled("AutoUse")) client.options.useKey.setPressed(true);
    }

    private void handleSpeed(MinecraftClient client) {
        if (!modules.enabled("Speed") || client.player.isClimbing() || modules.enabled("Freecam")) return;

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

    private void handleJumpModules(MinecraftClient client) {
        boolean pressed = client.options.jumpKey.isPressed();

        if (!modules.enabled("Freecam")) {
            if (modules.enabled("BunnyHop") && client.player.isOnGround()
                    && (client.options.forwardKey.isPressed()
                    || client.options.backKey.isPressed()
                    || client.options.leftKey.isPressed()
                    || client.options.rightKey.isPressed())) {
                Vec3d velocity = client.player.getVelocity();
                client.player.setVelocity(velocity.x, 0.42, velocity.z);
            }

            if (pressed && !jumpWasPressed) {
                Vec3d velocity = client.player.getVelocity();

                if (modules.enabled("LongJump") && client.player.isOnGround()) {
                    double yaw = Math.toRadians(client.player.getYaw());
                    double boost = 0.72;
                    client.player.setVelocity(-Math.sin(yaw) * boost, 0.42, Math.cos(yaw) * boost);
                } else if (modules.enabled("HighJump") && client.player.isOnGround()) {
                    client.player.setVelocity(velocity.x, highJumpPower, velocity.z);
                } else if (modules.enabled("AirJump") && !client.player.isOnGround()) {
                    client.player.setVelocity(velocity.x, 0.42, velocity.z);
                }
            }
        }

        jumpWasPressed = pressed;
    }

    private void handleFastFall(MinecraftClient client) {
        if (!modules.enabled("FastFall") || modules.enabled("Freecam")) return;
        if (client.player.isOnGround() || client.player.isClimbing() || client.player.isTouchingWater()) return;

        Vec3d velocity = client.player.getVelocity();
        if (velocity.y < 0.0) {
            client.player.setVelocity(velocity.x, Math.min(velocity.y, -fastFallSpeed), velocity.z);
        }
    }

    private void handleMovementExtras(MinecraftClient client) {
        if (modules.enabled("Freecam")) return;

        Vec3d velocity = client.player.getVelocity();

        if (modules.enabled("Spider") && client.player.horizontalCollision) {
            client.player.setVelocity(velocity.x, 0.25, velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("Jetpack") && client.options.jumpKey.isPressed()) {
            client.player.setVelocity(velocity.x, 0.28, velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("SlowFall") && !client.player.isOnGround() && velocity.y < -0.03) {
            client.player.setVelocity(velocity.x, -0.03, velocity.z);
            velocity = client.player.getVelocity();
        } else if (modules.enabled("Glide") && !client.player.isOnGround() && velocity.y < -0.08) {
            client.player.setVelocity(velocity.x, -0.08, velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("ReverseStep") && !client.player.isOnGround()
                && !client.player.isTouchingWater() && velocity.y < -0.12) {
            client.player.setVelocity(velocity.x, Math.min(velocity.y, -0.70), velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("WaterSpeed") && client.player.isTouchingWater()) {
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontal > 0.001 && horizontal < 0.62) {
                client.player.setVelocity(velocity.x * 1.06, velocity.y, velocity.z * 1.06);
                velocity = client.player.getVelocity();
            }
        }

        if (modules.enabled("StrafeBoost")) {
            double forward = 0.0;
            double strafe = 0.0;
            if (client.options.forwardKey.isPressed()) forward += 1.0;
            if (client.options.backKey.isPressed()) forward -= 1.0;
            if (client.options.leftKey.isPressed()) strafe += 1.0;
            if (client.options.rightKey.isPressed()) strafe -= 1.0;

            if (forward != 0.0 || strafe != 0.0) {
                double length = Math.sqrt(forward * forward + strafe * strafe);
                forward /= length;
                strafe /= length;
                double yaw = Math.toRadians(client.player.getYaw());
                double boost = 0.31;
                double x = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * boost;
                double z = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * boost;
                client.player.setVelocity(x, client.player.getVelocity().y, z);
            }
        }
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

    private void handleZoom(MinecraftClient client) {
        if (modules.enabled("Zoom")) {
            if (rememberedFov == null) rememberedFov = client.options.getFov().getValue();
            client.options.getFov().setValue(zoomFov);
        } else {
            restoreZoom(client);
        }
    }

    private void restoreZoom(MinecraftClient client) {
        if (rememberedFov != null) {
            client.options.getFov().setValue(rememberedFov);
            rememberedFov = null;
        }
    }

    private void handleEsp(MinecraftClient client) {
        boolean livingEsp = modules.enabled("ESP")
                || modules.enabled("PlayerESP")
                || modules.enabled("MobESP")
                || modules.enabled("GlowESP");
        boolean crystalEsp = modules.enabled("CrystalESP");
        boolean itemEsp = modules.enabled("ItemESP");
        double maxSq = (double) espRange * espRange;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;

            if (entity.squaredDistanceTo(client.player) > maxSq) {
                if (entity.isGlowingLocal()) entity.setGlowing(false);
                continue;
            }

            boolean glow = false;

            if (crystalEsp && entity.getType() == EntityType.END_CRYSTAL) glow = true;
            if (itemEsp && entity instanceof ItemEntity) glow = true;

            if (livingEsp && entity instanceof LivingEntity) {
                if (entity instanceof PlayerEntity) {
                    glow = (modules.enabled("ESP") && espPlayers)
                            || modules.enabled("PlayerESP")
                            || (modules.enabled("GlowESP") && espPlayers);
                } else {
                    glow = (modules.enabled("ESP") && espMobs)
                            || modules.enabled("MobESP")
                            || (modules.enabled("GlowESP") && espMobs);
                }
            }

            if (glow) entity.setGlowing(true);
            else if (entity.isGlowingLocal()) entity.setGlowing(false);
        }
    }

    private void handleTriggerBot(MinecraftClient client) {
        if (!modules.enabled("TriggerBot") || client.currentScreen != null || client.interactionManager == null) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return;

        Entity target = hit.getEntity();
        if (target == client.player || !target.isAlive()) return;
        if (client.player.getAttackCooldownProgress(0.0f) < 0.92f) return;

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private void handleCombatExtras(MinecraftClient client) {
        if (client.currentScreen != null || client.interactionManager == null) {
            if (modules.enabled("AutoShield")) client.options.useKey.setPressed(false);
            return;
        }

        if (modules.enabled("AimAssist")) {
            LivingEntity nearest = null;
            double best = 64.0;

            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof LivingEntity living) || entity == client.player || !entity.isAlive()) continue;
                double dist = entity.squaredDistanceTo(client.player);
                if (dist < best) {
                    best = dist;
                    nearest = living;
                }
            }

            if (nearest != null) {
                Vec3d from = client.player.getEyePos();
                Vec3d to = nearest.getBoundingBox().getCenter();
                double dx = to.x - from.x;
                double dy = to.y - from.y;
                double dz = to.z - from.z;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                client.player.setYaw((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
                client.player.setPitch((float)(-Math.toDegrees(Math.atan2(dy, horizontal))));
            }
        }

        if (modules.enabled("AutoClicker")) {
            autoClickTicks++;
            if (autoClickTicks >= 4 && client.crosshairTarget instanceof EntityHitResult hit) {
                Entity target = hit.getEntity();
                if (target != client.player && target.isAlive()
                        && client.player.getAttackCooldownProgress(0.0f) >= 0.80f) {
                    autoClickTicks = 0;
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else {
            autoClickTicks = 0;
        }

        if (modules.enabled("AutoShield")) {
            boolean threat = client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity
                    && hit.getEntity().squaredDistanceTo(client.player) <= 36.0;
            boolean hasShield = client.player.getOffHandStack().isOf(Items.SHIELD);
            client.options.useKey.setPressed(threat && hasShield);
        }

        boolean attacking = client.options.attackKey.isPressed();
        if (modules.enabled("CriticalJump") && attacking && !attackWasPressed && client.player.isOnGround()) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(velocity.x, 0.18, velocity.z);
        }
        attackWasPressed = attacking;

        if (modules.enabled("AutoSwing")) {
            if (++autoSwingTicks >= 8) {
                autoSwingTicks = 0;
                client.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            autoSwingTicks = 0;
        }
    }

    private void handlePlayerExtras(MinecraftClient client) {
        if (modules.enabled("AutoRespawn") && client.player.isDead()) {
            client.player.requestRespawn();
            return;
        }

        if (modules.enabled("SpinBot")) {
            client.player.setYaw(client.player.getYaw() + 14.0f);
        }

        if (modules.enabled("PitchLock")) {
            client.player.setPitch(0.0f);
        }

        if (modules.enabled("YawLock")) {
            float snapped = Math.round(client.player.getYaw() / 45.0f) * 45.0f;
            client.player.setYaw(snapped);
        }

        if (modules.enabled("AutoDrop")) {
            if (++autoDropTicks >= 20) {
                autoDropTicks = 0;
                client.player.dropSelectedItem(false);
            }
        } else {
            autoDropTicks = 0;
        }

        if (modules.enabled("HandSwing")) {
            if (++handSwingTicks >= 10) {
                handSwingTicks = 0;
                client.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            handSwingTicks = 0;
        }

        if (modules.enabled("KeepSprint") && client.options.forwardKey.isPressed()) {
            client.player.setSprinting(true);
        }
    }

    private void handleUtilityExtras(MinecraftClient client) {
        utilityTicks++;
        quickTurnTicks++;

        if (modules.enabled("SneakSpam") && !modules.enabled("AutoSneak")) {
            client.options.sneakKey.setPressed((utilityTicks / 5) % 2 == 0);
        }

        if (modules.enabled("UseSpam") && !modules.enabled("AutoUse")) {
            client.options.useKey.setPressed((utilityTicks / 4) % 2 == 0);
        }

        if (modules.enabled("MineSpam") && !modules.enabled("AutoMine")) {
            client.options.attackKey.setPressed((utilityTicks / 4) % 2 == 0);
        }

        if (modules.enabled("JumpSpam") && client.player.isOnGround() && utilityTicks % 10 == 0) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(velocity.x, 0.42, velocity.z);
        }

        if (modules.enabled("QuickTurn") && quickTurnTicks >= 60) {
            quickTurnTicks = 0;
            client.player.setYaw(client.player.getYaw() + 180.0f);
        }

        if (utilityTicks > 10000) utilityTicks = 0;
    }

    private void handleAntiAfk(MinecraftClient client) {
        if (!modules.enabled("AntiAFK")) {
            antiAfkTicks = 0;
            return;
        }

        if (++antiAfkTicks >= 100) {
            antiAfkTicks = 0;
            client.player.setYaw(client.player.getYaw() + 3.0f);
        }
    }

    private void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean hud = modules.enabled("HUD");
        boolean radar = modules.enabled("Radar");
        if (!hud && !radar) return;

        final int purple = 0xFFB58CFF;
        final int purple2 = 0xFF8B5CF6;
        final int white = 0xFFF4F1FA;
        final int muted = 0xFFA9A3B5;
        final int panel = 0x90100E16;

        if (hud) {
            int x = 7;
            int y = 7;
            int boxH = hudCoordinates ? 34 : 22;

            context.fill(3, 3, 166, boxH, panel);
            context.fill(3, 3, 166, 5, purple2);
            context.drawTextWithShadow(client.textRenderer, "✦ NEXORA V9", x, y, purple);

            if (hudCoordinates) {
                context.drawTextWithShadow(client.textRenderer,
                        "XYZ " + client.player.getBlockX() + " " + client.player.getBlockY() + " " + client.player.getBlockZ(),
                        x, y + 13, white);
            }

            if (hudActiveModules) {
                int line = boxH + 6;
                for (Module module : modules.all()) {
                    if (!module.enabled()
                            || module.name().equals("HUD")
                            || module.name().equals("Radar")) continue;

                    context.drawTextWithShadow(client.textRenderer, module.name(), x, line, purple);
                    line += 11;
                    if (line > 182) break;
                }
            }
        }

        if (!radar) return;

        int radarW = 175;
        int rx = Math.max(4, context.getScaledWindowWidth() - radarW - 6);
        int ry = 6;
        int maxRows = radarRows;
        int radarH = 32 + maxRows * 11;

        context.fill(rx, ry, rx + radarW, ry + radarH, panel);
        context.fill(rx, ry, rx + radarW, ry + 2, purple2);
        context.drawTextWithShadow(client.textRenderer, "NEXORA RADAR", rx + 7, ry + 7, purple);
        context.drawTextWithShadow(client.textRenderer, "loaded targets", rx + 7, ry + 18, muted);

        int row = 0;
        int ty = ry + 34;

        if (modules.enabled("BaseFinder")) {
            for (BaseFinder.BaseCandidate candidate : baseFinder.candidates()) {
                if (row++ >= maxRows) break;
                String s = "BASE? " + (int) candidate.distance() + "m [" + candidate.score() + "]";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFFFF72E8);
                ty += 11;
            }
        }

        if (modules.enabled("XRay") && row < maxRows) {
            for (XRayManager.Hit hit : xray.hits()) {
                if (row++ >= maxRows) break;
                String s = "XRAY " + shortBlockName(hit.blockId()) + " " + (int) hit.distance() + "m";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFF7CFF9D);
                ty += 11;
            }
        }

        if (modules.enabled("BlockESP") && row < maxRows) {
            for (BlockEspManager.BlockHit hit : blockEsp.hits()) {
                if (row++ >= maxRows) break;
                String s = shortBlockName(hit.blockId()) + " " + (int) hit.distance() + "m";
                context.drawTextWithShadow(client.textRenderer, s, rx + 7, ty, 0xFF7CEAFF);
                ty += 11;
            }
        }

        if (modules.enabled("StorageESP") && row < maxRows) {
            for (BaseFinder.StorageHit hit : baseFinder.hits()) {
                if (row++ >= maxRows) break;
                String s = hit.type() + " " + (int) hit.distance() + "m";
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

        if ((module.name().equalsIgnoreCase("ESP")
                || module.name().equalsIgnoreCase("CrystalESP")
                || module.name().equalsIgnoreCase("ItemESP")
                || module.name().equalsIgnoreCase("PlayerESP")
                || module.name().equalsIgnoreCase("MobESP")
                || module.name().equalsIgnoreCase("GlowESP"))
                && !module.enabled() && client.world != null) {
            handleEsp(client);
        }

        if (module.name().equalsIgnoreCase("AutoWalk") && !module.enabled()) {
            client.options.forwardKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("AutoSneak") && !module.enabled()) {
            client.options.sneakKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("AutoMine") && !module.enabled()) {
            client.options.attackKey.setPressed(false);
        }

        if ((module.name().equalsIgnoreCase("AutoUse")
                || module.name().equalsIgnoreCase("UseSpam")
                || module.name().equalsIgnoreCase("AutoShield")) && !module.enabled()) {
            client.options.useKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("SneakSpam") && !module.enabled()
                && !modules.enabled("AutoSneak")) {
            client.options.sneakKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("MineSpam") && !module.enabled()
                && !modules.enabled("AutoMine")) {
            client.options.attackKey.setPressed(false);
        }

        if (module.name().equalsIgnoreCase("Fullbright") && !module.enabled()) {
            restoreFullbright(client);
        }

        if (module.name().equalsIgnoreCase("Zoom") && !module.enabled()) {
            restoreZoom(client);
        }

        if (module.name().equalsIgnoreCase("Freecam")) {
            if (module.enabled()) freecam.enable(client);
            else freecam.disable(client);
        }
    }

    public ModuleManager modules() { return modules; }
    public WaypointManager waypoints() { return waypoints; }
    public BaseFinder baseFinder() { return baseFinder; }
    public BlockEspManager blockEsp() { return blockEsp; }
    public XRayManager xray() { return xray; }
    public FreecamManager freecam() { return freecam; }
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

    public float highJumpPower() { return highJumpPower; }
    public void setHighJumpPower(float value) {
        highJumpPower = Math.max(0.42f, Math.min(1.5f, value));
    }

    public float fastFallSpeed() { return fastFallSpeed; }
    public void setFastFallSpeed(float value) {
        fastFallSpeed = Math.max(0.10f, Math.min(1.0f, value));
    }

    public int zoomFov() { return zoomFov; }
    public void setZoomFov(int value) {
        zoomFov = Math.max(10, Math.min(70, value));
    }

    public int espRange() { return espRange; }
    public void setEspRange(int value) {
        espRange = Math.max(32, Math.min(256, value));
    }

    public boolean espPlayers() { return espPlayers; }
    public void setEspPlayers(boolean value) { espPlayers = value; }

    public boolean espMobs() { return espMobs; }
    public void setEspMobs(boolean value) { espMobs = value; }

    public boolean entityBoxes() { return entityBoxes; }
    public void setEntityBoxes(boolean value) { entityBoxes = value; }

    public boolean entityLabels() { return entityLabels; }
    public void setEntityLabels(boolean value) { entityLabels = value; }

    public boolean worldLabels() { return worldLabels; }
    public void setWorldLabels(boolean value) { worldLabels = value; }

    public boolean worldBoxes() { return worldBoxes; }
    public void setWorldBoxes(boolean value) { worldBoxes = value; }

    public boolean hudCoordinates() { return hudCoordinates; }
    public void setHudCoordinates(boolean value) { hudCoordinates = value; }

    public boolean hudActiveModules() { return hudActiveModules; }
    public void setHudActiveModules(boolean value) { hudActiveModules = value; }

    public int radarRows() { return radarRows; }
    public void setRadarRows(int value) { radarRows = Math.max(4, Math.min(16, value)); }

    public int guiOpacity() { return guiOpacity; }
    public void setGuiOpacity(int value) { guiOpacity = Math.max(90, Math.min(235, value)); }
}
