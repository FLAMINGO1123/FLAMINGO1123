package com.nexora.client;

import com.nexora.client.command.CommandManager;
import com.nexora.client.core.Module;
import com.nexora.client.core.ModuleManager;
import com.nexora.client.core.ModuleSettings;
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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class NexoraClient implements ClientModInitializer {
    public static final String MOD_ID = "nexora";
    public static NexoraClient INSTANCE;

    private final ModuleManager modules = new ModuleManager();
    private final ModuleSettings settings = new ModuleSettings();
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
    private boolean reachWasPressed;
    private int lastHurtTime;
    private int antiAfkTicks;
    private int autoClickTicks;
    private int autoSwingTicks;
    private int autoDropTicks;
    private int handSwingTicks;
    private int utilityTicks;
    private int quickTurnTicks;
    private int respawnTicks;
    private boolean attackActionUsedThisTick;

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

        attackActionUsedThisTick = false;

        handleFlight(client);
        handleSprint(client);
        handleHeldKeys(client);
        handleSpeed(client);
        handleJumpModules(client);
        handleFastFall(client);
        handleMovementExtras(client);
        handleV11Movement(client);
        handleFullbright(client);
        handleZoom(client);
        handleEsp(client);
        handleTriggerBot(client);
        handleCombatExtras(client);
        handleV11CombatMovement(client);
        handleCoreCheats(client);
        handlePlayerExtras(client);
        handleUtilityExtras(client);
        handleAntiAfk(client);
        if (modules.enabled("Freecam")) freecam.setSpeed((float) settings.number("Freecam", "speed", freecam.speed()));
        freecam.tick(client);

        syncManagerSettings();
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
            abilities.setFlySpeed(flySpeed());
        } else if (flyStateCaptured) {
            abilities.flying = false;
            abilities.allowFlying = rememberedAllowFlying || abilities.creativeMode;
            abilities.setFlySpeed(0.05f);
            flyStateCaptured = false;
        }
    }

    private void handleSprint(MinecraftClient client) {
        if (!modules.enabled("Sprint")) return;

        boolean moving = settings.bool("Sprint", "forwardOnly", true)
                ? client.options.forwardKey.isPressed()
                : client.options.forwardKey.isPressed()
                || client.options.backKey.isPressed()
                || client.options.leftKey.isPressed()
                || client.options.rightKey.isPressed();

        if (moving) client.player.setSprinting(true);
    }

    private void handleHeldKeys(MinecraftClient client) {
        if (modules.enabled("AutoWalk")) {
            client.options.forwardKey.setPressed(true);
            if (settings.bool("AutoWalk", "sprint", false)) client.player.setSprinting(true);
        }

        if (modules.enabled("AutoSneak")) {
            boolean pulse = settings.bool("AutoSneak", "pulse", false);
            client.options.sneakKey.setPressed(!pulse || (utilityTicks / 5) % 2 == 0);
        }

        if (modules.enabled("AutoMine")) {
            boolean onlyTarget = settings.bool("AutoMine", "onlyTarget", false);
            client.options.attackKey.setPressed(!onlyTarget || client.crosshairTarget instanceof BlockHitResult);
        }

        if (modules.enabled("AutoUse")) {
            int interval = Math.max(1, (int) settings.number("AutoUse", "interval", 1));
            client.options.useKey.setPressed(interval == 1 || utilityTicks % interval == 0);
        }
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
        double base = 0.115 * speedMultiplier();
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
                client.player.setVelocity(velocity.x, settings.number("BunnyHop", "jump", 0.42), velocity.z);
            }

            if (pressed && !jumpWasPressed) {
                Vec3d velocity = client.player.getVelocity();

                if (modules.enabled("LongJump") && client.player.isOnGround()) {
                    double yaw = Math.toRadians(client.player.getYaw());
                    double boost = settings.number("LongJump", "boost", 0.72);
                    double jump = settings.number("LongJump", "jump", 0.42);
                    client.player.setVelocity(-Math.sin(yaw) * boost, jump, Math.cos(yaw) * boost);
                } else if (modules.enabled("HighJump") && client.player.isOnGround()) {
                    client.player.setVelocity(velocity.x, highJumpPower(), velocity.z);
                } else if (modules.enabled("AirJump") && !client.player.isOnGround()) {
                    client.player.setVelocity(velocity.x, settings.number("AirJump", "power", 0.42), velocity.z);
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
            client.player.setVelocity(velocity.x, Math.min(velocity.y, -fastFallSpeed()), velocity.z);
        }
    }

    private void handleMovementExtras(MinecraftClient client) {
        if (modules.enabled("Freecam")) return;

        Vec3d velocity = client.player.getVelocity();

        if (modules.enabled("Spider") && client.player.horizontalCollision) {
            client.player.setVelocity(velocity.x, settings.number("Spider", "speed", 0.25), velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("Jetpack") && client.options.jumpKey.isPressed()) {
            client.player.setVelocity(velocity.x, settings.number("Jetpack", "thrust", 0.28), velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("SlowFall") && !client.player.isOnGround() && velocity.y < -settings.number("SlowFall", "fall", 0.03)) {
            client.player.setVelocity(velocity.x, -settings.number("SlowFall", "fall", 0.03), velocity.z);
            velocity = client.player.getVelocity();
        } else if (modules.enabled("Glide") && !client.player.isOnGround() && velocity.y < -settings.number("Glide", "fall", 0.08)) {
            client.player.setVelocity(velocity.x, -settings.number("Glide", "fall", 0.08), velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("ReverseStep") && !client.player.isOnGround()
                && !client.player.isTouchingWater() && velocity.y < -0.12) {
            client.player.setVelocity(velocity.x, Math.min(velocity.y, -settings.number("ReverseStep", "speed", 0.70)), velocity.z);
            velocity = client.player.getVelocity();
        }

        if (modules.enabled("WaterSpeed") && client.player.isTouchingWater()) {
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontal > 0.001 && horizontal < 0.62) {
                double mult = settings.number("WaterSpeed", "multiplier", 1.06);
                client.player.setVelocity(velocity.x * mult, velocity.y, velocity.z * mult);
                velocity = client.player.getVelocity();
            }
        }

        if (modules.enabled("NoSlow") && client.player.isUsingItem()) {
            Vec3d current = client.player.getVelocity();
            double h = Math.sqrt(current.x * current.x + current.z * current.z);
            if (h > 0.001 && h < 0.32) {
                double mult = settings.number("NoSlow", "multiplier", 1.35);
                client.player.setVelocity(current.x * mult, current.y, current.z * mult);
            }
        }

        if (modules.enabled("SafeWalk") && client.player.isOnGround()) {
            Vec3d current = client.player.getVelocity();
            BlockPos ahead = BlockPos.ofFloored(
                    client.player.getX() + current.x * settings.number("SafeWalk", "probe", 2.2),
                    client.player.getY() - 0.6,
                    client.player.getZ() + current.z * settings.number("SafeWalk", "probe", 2.2)
            );
            if (client.world.getBlockState(ahead).isAir()) {
                client.player.setVelocity(0.0, current.y, 0.0);
            }
        }

        if (modules.enabled("Jesus") && client.player.isTouchingWater()) {
            Vec3d current = client.player.getVelocity();
            double waterBoost = settings.number("Jesus", "speed", 1.08);
            double lift = settings.number("Jesus", "lift", 0.08);
            client.player.setVelocity(current.x * waterBoost, Math.max(lift, current.y), current.z * waterBoost);
        }

        if (modules.enabled("Parkour") && client.player.isOnGround() && client.options.forwardKey.isPressed()) {
            double yaw = Math.toRadians(client.player.getYaw());
            double lookAhead = settings.number("Parkour", "lookahead", 0.8);
            double aheadX = client.player.getX() - Math.sin(yaw) * lookAhead;
            double aheadZ = client.player.getZ() + Math.cos(yaw) * lookAhead;
            BlockPos belowAhead = BlockPos.ofFloored(aheadX, client.player.getY() - 0.6, aheadZ);
            if (client.world.getBlockState(belowAhead).isAir()) {
                client.player.jump();
            }
        }

        if (modules.enabled("Step") && client.player.isOnGround() && client.player.horizontalCollision) {
            Vec3d current = client.player.getVelocity();
            client.player.setVelocity(current.x, settings.number("Step", "height", 0.46), current.z);
        }

        client.player.noClip = modules.enabled("Phase");

        if (modules.enabled("VehicleFly") && client.player.getVehicle() != null) {
            Entity vehicle = client.player.getVehicle();
            double yaw = Math.toRadians(client.player.getYaw());
            double speed = settings.number("VehicleFly", "horizontal", 0.55);
            double x = -Math.sin(yaw) * speed;
            double z = Math.cos(yaw) * speed;
            double vertical = settings.number("VehicleFly", "vertical", 0.35);
            double y = client.options.jumpKey.isPressed() ? vertical
                    : client.options.sneakKey.isPressed() ? -vertical : 0.0;
            vehicle.setVelocity(x, y, z);
        }

        if (modules.enabled("AntiVoid")
                && client.player.getY() <= client.world.getBottomY() + settings.number("AntiVoid", "threshold", 5)) {
            Vec3d current = client.player.getVelocity();
            client.player.setVelocity(current.x * 0.2, settings.number("AntiVoid", "boost", 1.0), current.z * 0.2);
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
                double boost = settings.number("StrafeBoost", "speed", 0.31);
                double x = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * boost;
                double z = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * boost;
                client.player.setVelocity(x, client.player.getVelocity().y, z);
            }
        }
    }

    private void handleV11Movement(MinecraftClient client) {
        if (modules.enabled("Freecam")) return;

        Vec3d velocity = client.player.getVelocity();

        if (modules.enabled("AirStrafe") && !client.player.isOnGround()) {
            double forward = 0.0;
            double strafe = 0.0;
            if (client.options.forwardKey.isPressed()) forward += 1.0;
            if (client.options.backKey.isPressed()) forward -= 1.0;
            if (client.options.leftKey.isPressed()) strafe += 1.0;
            if (client.options.rightKey.isPressed()) strafe -= 1.0;

            if (forward != 0.0 || strafe != 0.0) {
                double len = Math.sqrt(forward * forward + strafe * strafe);
                forward /= len;
                strafe /= len;
                double yaw = Math.toRadians(client.player.getYaw());
                double speed = settings.number("AirStrafe", "speed", 0.28);
                double x = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speed;
                double z = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speed;
                client.player.setVelocity(x, client.player.getVelocity().y, z);
            }
        }

        if (modules.enabled("SneakSpeed") && client.player.isSneaking()) {
            Vec3d current = client.player.getVelocity();
            double mult = settings.number("SneakSpeed", "multiplier", 1.35);
            client.player.setVelocity(current.x * mult, current.y, current.z * mult);
        }

        if (modules.enabled("IceSpeed")) {
            BlockPos below = client.player.getBlockPos().down();
            BlockState state = client.world.getBlockState(below);
            if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE)
                    || state.isOf(Blocks.BLUE_ICE) || state.isOf(Blocks.FROSTED_ICE)) {
                Vec3d current = client.player.getVelocity();
                double mult = settings.number("IceSpeed", "multiplier", 1.40);
                client.player.setVelocity(current.x * mult, current.y, current.z * mult);
            }
        }

        if (modules.enabled("LavaSpeed") && client.player.isInLava()) {
            Vec3d current = client.player.getVelocity();
            double mult = settings.number("LavaSpeed", "multiplier", 1.20);
            client.player.setVelocity(current.x * mult, current.y, current.z * mult);
        }

        if (modules.enabled("AutoSwim") && client.player.isTouchingWater()
                && (client.options.forwardKey.isPressed() || client.options.jumpKey.isPressed())) {
            Vec3d current = client.player.getVelocity();
            client.player.setVelocity(current.x,
                    Math.max(current.y, settings.number("AutoSwim", "lift", 0.12)),
                    current.z);
        }

        if (modules.enabled("Hover") && !client.player.isOnGround()) {
            Vec3d current = client.player.getVelocity();
            client.player.setVelocity(current.x,
                    settings.number("Hover", "vertical", 0.0),
                    current.z);
        }

        if (modules.enabled("Anchor")) {
            Vec3d current = client.player.getVelocity();
            double x = settings.bool("Anchor", "horizontal", true) ? 0.0 : current.x;
            double y = settings.bool("Anchor", "vertical", true) ? 0.0 : current.y;
            double z = settings.bool("Anchor", "horizontal", true) ? 0.0 : current.z;
            client.player.setVelocity(x, y, z);
        }

        if (modules.enabled("EdgeJump") && client.player.isOnGround()) {
            double yaw = Math.toRadians(client.player.getYaw());
            double lookAhead = settings.number("EdgeJump", "lookahead", 0.85);
            BlockPos belowAhead = BlockPos.ofFloored(
                    client.player.getX() - Math.sin(yaw) * lookAhead,
                    client.player.getY() - 0.6,
                    client.player.getZ() + Math.cos(yaw) * lookAhead
            );
            if (client.world.getBlockState(belowAhead).isAir()) {
                Vec3d current = client.player.getVelocity();
                client.player.setVelocity(current.x,
                        settings.number("EdgeJump", "power", 0.42),
                        current.z);
            }
        }

        if (modules.enabled("WallBounce") && client.player.horizontalCollision) {
            Vec3d current = client.player.getVelocity();
            double h = settings.number("WallBounce", "horizontal", 0.35);
            double y = settings.number("WallBounce", "vertical", 0.32);
            client.player.setVelocity(-Math.signum(current.x) * h, y, -Math.signum(current.z) * h);
        }
    }

    private void handleFullbright(MinecraftClient client) {
        if (modules.enabled("Fullbright")) {
            if (rememberedGamma == null) rememberedGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(settings.number("Fullbright", "gamma", 1.0));
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
            client.options.getFov().setValue(zoomFov());
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
        double maxSq = (double) espRange() * espRange();

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
        if (target instanceof PlayerEntity && !settings.bool("TriggerBot", "players", true)) return;
        if (target instanceof LivingEntity && !(target instanceof PlayerEntity) && !settings.bool("TriggerBot", "mobs", true)) return;
        double triggerRange = settings.number("TriggerBot", "range", 4.5);
        if (target.squaredDistanceTo(client.player) > triggerRange * triggerRange) return;
        tryAttack(client, target,
                settings.number("TriggerBot", "cooldown", 0.92),
                triggerRange);
    }

    private void handleCombatExtras(MinecraftClient client) {
        if (client.currentScreen != null || client.interactionManager == null) {
            if (modules.enabled("AutoShield")) client.options.useKey.setPressed(false);
            return;
        }

        if (modules.enabled("AimAssist")) {
            LivingEntity nearest = null;
            double aimRange = settings.number("AimAssist", "range", 8.0);
            double best = aimRange * aimRange;

            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof LivingEntity living) || entity == client.player || !entity.isAlive()) continue;
                if (entity instanceof PlayerEntity && !settings.bool("AimAssist", "players", true)) continue;
                if (!(entity instanceof PlayerEntity) && !settings.bool("AimAssist", "mobs", true)) continue;
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
                float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
                float strength = (float) settings.number("AimAssist", "strength", 0.35);
                float nextYaw = client.player.getYaw() + wrapDegrees(targetYaw - client.player.getYaw()) * strength;
                float nextPitch = client.player.getPitch() + (targetPitch - client.player.getPitch()) * strength;
                setRotationIfChanged(client, nextYaw, nextPitch);
            }
        }

        if (modules.enabled("AutoClicker")) {
            autoClickTicks++;
            if (autoClickTicks >= (int) settings.number("AutoClicker", "interval", 4) && client.crosshairTarget instanceof EntityHitResult hit) {
                Entity target = hit.getEntity();
                if (target != client.player && target.isAlive()
                        && client.player.getAttackCooldownProgress(0.0f) >= settings.number("AutoClicker", "cooldown", 0.80)) {
                    if (tryAttack(client, target,
                            settings.number("AutoClicker", "cooldown", 0.80), 4.5)) {
                        autoClickTicks = 0;
                    }
                }
            }
        } else {
            autoClickTicks = 0;
        }

        if (modules.enabled("AutoShield")) {
            boolean threat = client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof LivingEntity
                    && hit.getEntity().squaredDistanceTo(client.player)
                    <= Math.pow(settings.number("AutoShield", "range", 6.0), 2);
            boolean hasShield = client.player.getOffHandStack().isOf(Items.SHIELD);
            client.options.useKey.setPressed(threat && hasShield);
        }

        boolean attacking = client.options.attackKey.isPressed();
        if (modules.enabled("CriticalJump") && attacking && !attackWasPressed && client.player.isOnGround()) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(velocity.x, settings.number("CriticalJump", "height", 0.18), velocity.z);
        }
        attackWasPressed = attacking;

        if (modules.enabled("AutoSwing")) {
            if (++autoSwingTicks >= (int) settings.number("AutoSwing", "interval", 8)) {
                autoSwingTicks = 0;
                client.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            autoSwingTicks = 0;
        }
    }

    private void handleV11CombatMovement(MinecraftClient client) {
        if (modules.enabled("Freecam")) return;

        if (modules.enabled("TargetStrafe")) {
            LivingEntity target = nearestLivingTarget(client,
                    settings.number("TargetStrafe", "range", 6.0),
                    settings.bool("TargetStrafe", "players", true),
                    settings.bool("TargetStrafe", "mobs", true));

            if (target != null) {
                double dx = client.player.getX() - target.getX();
                double dz = client.player.getZ() - target.getZ();
                double dist = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                double desired = settings.number("TargetStrafe", "radius", 3.0);
                double tangentX = -dz / dist;
                double tangentZ = dx / dist;
                double radial = (desired - dist) * 0.08;
                double speed = settings.number("TargetStrafe", "speed", 0.28);
                Vec3d current = client.player.getVelocity();
                client.player.setVelocity(
                        tangentX * speed + (dx / dist) * radial,
                        current.y,
                        tangentZ * speed + (dz / dist) * radial
                );
            }
        }

        if (modules.enabled("AutoChase")) {
            LivingEntity target = nearestLivingTarget(client,
                    settings.number("AutoChase", "range", 10.0),
                    settings.bool("AutoChase", "players", true),
                    settings.bool("AutoChase", "mobs", true));

            if (target != null) {
                double dx = target.getX() - client.player.getX();
                double dz = target.getZ() - client.player.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.25) {
                    double speed = settings.number("AutoChase", "speed", 0.24);
                    Vec3d current = client.player.getVelocity();
                    client.player.setVelocity(dx / len * speed, current.y, dz / len * speed);
                }
            }
        }
    }

    private void handleCoreCheats(MinecraftClient client) {
        if (client.currentScreen == null && client.interactionManager != null) {
            if (modules.enabled("KillAura") && client.player.getAttackCooldownProgress(0.0f)
                    >= settings.number("KillAura", "cooldown", 0.92)) {
                LivingEntity nearest = nearestLivingTarget(client,
                        settings.number("KillAura", "range", 4.5),
                        settings.bool("KillAura", "players", true),
                        settings.bool("KillAura", "mobs", true));
                if (nearest != null) {
                    tryAttack(client, nearest,
                            settings.number("KillAura", "cooldown", 0.92),
                            settings.number("KillAura", "range", 4.5));
                }
            }

            boolean attackPressed = client.options.attackKey.isPressed();

            if (modules.enabled("Reach") && attackPressed && !reachWasPressed
                    && client.player.getAttackCooldownProgress(0.0f) >= settings.number("Reach", "cooldown", 0.80)) {
                LivingEntity target = targetAlongLook(client,
                        settings.number("Reach", "range", 3.0),
                        settings.number("Reach", "radius", 1.35));
                if (target != null) {
                    tryAttack(client, target,
                            settings.number("Reach", "cooldown", 0.80),
                            settings.number("Reach", "range", 3.0));
                }
            } else if (modules.enabled("Hitbox") && attackPressed && !reachWasPressed
                    && client.player.getAttackCooldownProgress(0.0f) >= settings.number("Hitbox", "cooldown", 0.85)) {
                LivingEntity target = targetAlongLook(client,
                        settings.number("Hitbox", "range", 4.5),
                        settings.number("Hitbox", "radius", 1.80));
                if (target != null) {
                    tryAttack(client, target,
                            settings.number("Hitbox", "cooldown", 0.85),
                            settings.number("Hitbox", "range", 4.5));
                }
            }

            reachWasPressed = attackPressed;
        } else {
            reachWasPressed = false;
        }

        if (modules.enabled("Velocity")) {
            int hurt = client.player.hurtTime;
            if (hurt > lastHurtTime) {
                Vec3d velocity = client.player.getVelocity();
                double horizontal = settings.number("Velocity", "horizontal", 25) / 100.0;
                double vertical = settings.number("Velocity", "vertical", 35) / 100.0;
                client.player.setVelocity(velocity.x * horizontal, velocity.y * vertical, velocity.z * horizontal);
            }
            lastHurtTime = hurt;
        } else if (modules.enabled("KnockbackBoost")) {
            int hurt = client.player.hurtTime;
            if (hurt > lastHurtTime) {
                Vec3d velocity = client.player.getVelocity();
                double horizontal = settings.number("KnockbackBoost", "horizontal", 150) / 100.0;
                double vertical = settings.number("KnockbackBoost", "vertical", 125) / 100.0;
                client.player.setVelocity(velocity.x * horizontal, velocity.y * vertical, velocity.z * horizontal);
            }
            lastHurtTime = hurt;
        } else {
            lastHurtTime = client.player.hurtTime;
        }

        if (modules.enabled("NoFall")
                && client.getServer() != null
                && client.player.fallDistance > settings.number("NoFall", "threshold", 2.5)) {
            client.player.setOnGround(true);
            client.player.fallDistance = 0.0f;
        }

        if (modules.enabled("FastPlace") && client.currentScreen == null
                && client.interactionManager != null && client.options.useKey.isPressed()
                && utilityTicks % Math.max(1, (int) settings.number("FastPlace", "interval", 1)) == 0) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }

        if (modules.enabled("FastBreak") && client.currentScreen == null
                && client.interactionManager != null && client.options.attackKey.isPressed()
                && utilityTicks % Math.max(1, (int) settings.number("FastBreak", "interval", 1)) == 0
                && client.crosshairTarget instanceof BlockHitResult hit) {
            client.interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), hit.getSide());
        }

        if (modules.enabled("Nuker") && client.currentScreen == null && client.interactionManager != null
                && utilityTicks % Math.max(1, (int) settings.number("Nuker", "delay", 1)) == 0) {
            BlockPos center = client.player.getBlockPos();
            boolean attacked = false;
            int radius = (int) settings.number("Nuker", "radius", 2);

            for (int dy = -radius; dy <= radius && !attacked; dy++) {
                for (int dx = -radius; dx <= radius && !attacked; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos pos = center.add(dx, dy, dz);
                        BlockState state = client.world.getBlockState(pos);
                        if (!state.isAir()) {
                            client.interactionManager.attackBlock(pos, Direction.UP);
                            attacked = true;
                            break;
                        }
                    }
                }
            }
        }

        if (modules.enabled("AutoTool") && client.crosshairTarget instanceof BlockHitResult hit) {
            BlockState state = client.world.getBlockState(hit.getBlockPos());
            int bestSlot = client.player.getInventory().getSelectedSlot();
            float bestSpeed = client.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = client.player.getInventory().getStack(slot);
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }

            client.player.getInventory().setSelectedSlot(bestSlot);
        }
    }

    private boolean tryAttack(MinecraftClient client, Entity target, double cooldown, double maxRange) {
        if (attackActionUsedThisTick || client.interactionManager == null || client.currentScreen != null) return false;
        if (target == null || target == client.player || !target.isAlive()) return false;
        if (client.player.getAttackCooldownProgress(0.0f) < cooldown) return false;

        double range = Math.max(0.0, maxRange);
        if (target.squaredDistanceTo(client.player) > range * range) return false;

        attackActionUsedThisTick = true;
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    private void setRotationIfChanged(MinecraftClient client, float yaw, float pitch) {
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();
        float yawDelta = Math.abs(wrapDegrees(yaw - currentYaw));
        float clampedPitch = Math.max(-90.0f, Math.min(90.0f, pitch));
        float pitchDelta = Math.abs(clampedPitch - currentPitch);

        if (yawDelta < 0.05f && pitchDelta < 0.05f) return;

        client.player.setYaw(yaw);
        client.player.setPitch(clampedPitch);
    }

    private LivingEntity nearestLivingTarget(MinecraftClient client, double range, boolean players, boolean mobs) {
        LivingEntity nearest = null;
        double best = range * range;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !entity.isAlive()) continue;
            if (entity instanceof PlayerEntity && !players) continue;
            if (!(entity instanceof PlayerEntity) && !mobs) continue;
            double distance = entity.squaredDistanceTo(client.player);
            if (distance < best) {
                best = distance;
                nearest = living;
            }
        }

        return nearest;
    }

    private LivingEntity targetAlongLook(MinecraftClient client, double range, double radius) {
        Vec3d eye = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0f).normalize();

        LivingEntity bestTarget = null;
        double bestProjection = range + 1.0;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !entity.isAlive()) continue;

            Vec3d toTarget = living.getBoundingBox().getCenter().subtract(eye);
            double projection = toTarget.dotProduct(look);
            if (projection < 0.0 || projection > range) continue;

            double perpendicularSq = Math.max(0.0, toTarget.lengthSquared() - projection * projection);
            if (perpendicularSq <= radius * radius && projection < bestProjection) {
                bestProjection = projection;
                bestTarget = living;
            }
        }

        return bestTarget;
    }

    private void handlePlayerExtras(MinecraftClient client) {
        if (modules.enabled("AutoConsume")) {
            int threshold = (int) settings.number("AutoConsume", "hunger", 14);
            int interval = Math.max(1, (int) settings.number("AutoConsume", "interval", 1));
            boolean hungry = client.player.getHungerManager().getFoodLevel() <= threshold;
            client.options.useKey.setPressed(hungry && utilityTicks % interval == 0);
        }

        if (modules.enabled("AutoRespawn") && client.player.isDead()) {
            respawnTicks++;
            if (respawnTicks >= (int) settings.number("AutoRespawn", "delay", 0)) {
                respawnTicks = 0;
                client.player.requestRespawn();
            }
            return;
        } else {
            respawnTicks = 0;
        }

        if (modules.enabled("SpinBot")) {
            setRotationIfChanged(client,
                    client.player.getYaw() + (float) settings.number("SpinBot", "speed", 14),
                    client.player.getPitch());
        }

        if (modules.enabled("PitchLock")) {
            setRotationIfChanged(client,
                    client.player.getYaw(),
                    (float) settings.number("PitchLock", "pitch", 0));
        }

        if (modules.enabled("YawLock")) {
            float step = (float) settings.number("YawLock", "step", 45);
            float snapped = Math.round(client.player.getYaw() / step) * step;
            setRotationIfChanged(client, snapped, client.player.getPitch());
        }

        if (modules.enabled("AutoDrop")) {
            if (++autoDropTicks >= (int) settings.number("AutoDrop", "interval", 20)) {
                autoDropTicks = 0;
                client.player.dropSelectedItem(settings.bool("AutoDrop", "fullStack", false));
            }
        } else {
            autoDropTicks = 0;
        }

        if (modules.enabled("HandSwing")) {
            if (++handSwingTicks >= (int) settings.number("HandSwing", "interval", 10)) {
                handSwingTicks = 0;
                client.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            handSwingTicks = 0;
        }

        if (modules.enabled("KeepSprint")) {
            boolean moving = settings.bool("KeepSprint", "forwardOnly", true)
                    ? client.options.forwardKey.isPressed()
                    : client.options.forwardKey.isPressed()
                    || client.options.backKey.isPressed()
                    || client.options.leftKey.isPressed()
                    || client.options.rightKey.isPressed();
            if (moving) client.player.setSprinting(true);
        }
    }

    private void handleUtilityExtras(MinecraftClient client) {
        utilityTicks++;
        quickTurnTicks++;

        if (modules.enabled("SneakSpam") && !modules.enabled("AutoSneak")) {
            client.options.sneakKey.setPressed((utilityTicks / Math.max(1, (int) settings.number("SneakSpam", "interval", 5))) % 2 == 0);
        }

        if (modules.enabled("UseSpam") && !modules.enabled("AutoUse")) {
            client.options.useKey.setPressed((utilityTicks / Math.max(1, (int) settings.number("UseSpam", "interval", 4))) % 2 == 0);
        }

        if (modules.enabled("MineSpam") && !modules.enabled("AutoMine")) {
            client.options.attackKey.setPressed((utilityTicks / Math.max(1, (int) settings.number("MineSpam", "interval", 4))) % 2 == 0);
        }

        if (modules.enabled("JumpSpam") && client.player.isOnGround() && utilityTicks % Math.max(1, (int) settings.number("JumpSpam", "interval", 10)) == 0) {
            Vec3d velocity = client.player.getVelocity();
            client.player.setVelocity(velocity.x, 0.42, velocity.z);
        }

        if (modules.enabled("QuickTurn") && quickTurnTicks >= (int) settings.number("QuickTurn", "interval", 60)) {
            quickTurnTicks = 0;
            setRotationIfChanged(client,
                    client.player.getYaw() + (float) settings.number("QuickTurn", "degrees", 180),
                    client.player.getPitch());
        }

        if (utilityTicks > 10000) utilityTicks = 0;
    }

    private void handleAntiAfk(MinecraftClient client) {
        if (!modules.enabled("AntiAFK")) {
            antiAfkTicks = 0;
            return;
        }

        if (++antiAfkTicks >= (int) settings.number("AntiAFK", "interval", 100)) {
            antiAfkTicks = 0;
            setRotationIfChanged(client,
                    client.player.getYaw() + (float) settings.number("AntiAFK", "turn", 3),
                    client.player.getPitch());
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
            context.drawTextWithShadow(client.textRenderer, "✦ NEXORA V11", x, y, purple);

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
                            || module.name().equals("Radar")
                            || !settings.bool(module.name(), "showHud", true)) continue;

                    context.drawTextWithShadow(client.textRenderer, module.name(), x, line, purple);
                    line += 11;
                    if (line > 182) break;
                }
            }
        }

        if (modules.enabled("TestMeter")) {
            int mx = 7;
            int my = Math.max(205, context.getScaledWindowHeight() - 66);
            Vec3d v = client.player.getVelocity();
            context.fill(mx - 4, my - 4, mx + 184, my + 58, panel);
            context.drawTextWithShadow(client.textRenderer, "TEST METER", mx, my, purple);

            int line = my + 12;
            if (settings.bool("TestMeter", "velocity", true)) {
                String speed = String.format(Locale.ROOT, "V %.3f %.3f %.3f", v.x, v.y, v.z);
                context.drawTextWithShadow(client.textRenderer, speed, mx, line, white);
                line += 10;
            }
            if (settings.bool("TestMeter", "cooldown", true)) {
                String cd = String.format(Locale.ROOT, "CD %.2f", client.player.getAttackCooldownProgress(0.0f));
                context.drawTextWithShadow(client.textRenderer, cd, mx, line, white);
                line += 10;
            }
            if (settings.bool("TestMeter", "fall", true)) {
                String fall = String.format(Locale.ROOT, "Fall %.2f", client.player.fallDistance);
                context.drawTextWithShadow(client.textRenderer, fall, mx, line, white);
                line += 10;
            }
            if (settings.bool("TestMeter", "target", true)) {
                LivingEntity target = nearestLivingTarget(client, 20.0, true, true);
                String targetText = target == null ? "Target --"
                        : String.format(Locale.ROOT, "Target %.2fm", Math.sqrt(target.squaredDistanceTo(client.player)));
                context.drawTextWithShadow(client.textRenderer, targetText, mx, line, white);
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

    private void syncManagerSettings() {
        blockEsp.setScanRange((int) settings.number("BlockESP", "range", blockEsp.scanRange()));
        xray.setScanRange((int) settings.number("XRay", "range", xray.scanRange()));
        xray.setVerticalRange((int) settings.number("XRay", "vertical", xray.verticalRange()));

        int storageRange = (int) settings.number("StorageESP", "range", baseFinder.scanRange());
        int baseRange = (int) settings.number("BaseFinder", "range", baseFinder.scanRange());
        baseFinder.setScanRange(modules.enabled("BaseFinder") ? baseRange : storageRange);
        baseFinder.setVerticalRange((int) settings.number("BaseFinder", "vertical", baseFinder.verticalRange()));
        baseFinder.setMinClusterSize((int) settings.number("BaseFinder", "minCluster", baseFinder.minClusterSize()));
        baseFinder.setClusterRadius((int) settings.number("BaseFinder", "clusterRadius", baseFinder.clusterRadius()));
        baseFinder.setUpdateDelay((int) settings.number("BaseFinder", "delay", baseFinder.updateDelay()));
    }

    private float wrapDegrees(float degrees) {
        float value = degrees % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
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

    private void isolateConflicts(Module selected) {
        if (!modules.enabled("TestIsolation") || !selected.enabled()) return;

        boolean attack = settings.bool("TestIsolation", "combat", true) && isAttackTest(selected.name());
        boolean movement = settings.bool("TestIsolation", "movement", true) && isMovementTest(selected.name());
        boolean rotation = settings.bool("TestIsolation", "rotation", true) && isRotationTest(selected.name());

        if (!attack && !movement && !rotation) return;

        for (Module other : modules.all()) {
            if (other == selected || !other.enabled()) continue;

            boolean conflict = (attack && isAttackTest(other.name()))
                    || (movement && isMovementTest(other.name()))
                    || (rotation && isRotationTest(other.name()));

            if (!conflict) continue;

            other.setEnabled(false);
            onModuleToggled(other);
        }
    }

    private boolean isAttackTest(String name) {
        return switch (name) {
            case "TriggerBot", "AutoClicker", "KillAura", "Reach", "Hitbox",
                    "AutoSwing", "CriticalJump", "AutoShield" -> true;
            default -> false;
        };
    }

    private boolean isRotationTest(String name) {
        return switch (name) {
            case "AimAssist", "SpinBot", "PitchLock", "YawLock",
                    "QuickTurn", "AntiAFK" -> true;
            default -> false;
        };
    }

    private boolean isMovementTest(String name) {
        return switch (name) {
            case "Fly", "Speed", "BunnyHop", "HighJump", "AirJump",
                    "FastFall", "Glide", "Spider", "WaterSpeed", "LongJump",
                    "Jetpack", "SlowFall", "ReverseStep", "StrafeBoost",
                    "NoSlow", "SafeWalk", "Jesus", "Parkour", "Phase", "Step",
                    "VehicleFly", "AntiVoid", "Velocity", "NoFall",
                    "TargetStrafe", "AutoChase", "KnockbackBoost",
                    "AirStrafe", "SneakSpeed", "IceSpeed", "LavaSpeed",
                    "AutoSwim", "Hover", "Anchor", "EdgeJump", "WallBounce" -> true;
            default -> false;
        };
    }

    public void isolateForTest(Module selected) {
        for (Module module : modules.all()) {
            if (module == selected) continue;
            if (module.name().equals("HUD") || module.name().equals("TestIsolation") || module.name().equals("TestMeter")) continue;
            if (!module.enabled()) continue;

            module.setEnabled(false);
            onModuleToggled(module);
        }

        if (!selected.enabled()) {
            selected.setEnabled(true);
            onModuleToggled(selected);
        }
    }

    public void onModuleToggled(Module module) {
        MinecraftClient client = MinecraftClient.getInstance();

        isolateConflicts(module);

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
                || module.name().equalsIgnoreCase("AutoShield")
                || module.name().equalsIgnoreCase("AutoConsume")) && !module.enabled()) {
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

        if (module.name().equalsIgnoreCase("Phase") && !module.enabled() && client.player != null) {
            client.player.noClip = false;
        }

        if (module.name().equalsIgnoreCase("Freecam")) {
            if (module.enabled()) freecam.enable(client);
            else freecam.disable(client);
        }
    }

    public ModuleManager modules() { return modules; }
    public ModuleSettings settings() { return settings; }
    public WaypointManager waypoints() { return waypoints; }
    public BaseFinder baseFinder() { return baseFinder; }
    public BlockEspManager blockEsp() { return blockEsp; }
    public XRayManager xray() { return xray; }
    public FreecamManager freecam() { return freecam; }
    public RelogManager relog() { return relog; }

    public float flySpeed() { return (float) settings.number("Fly", "speed", flySpeed); }
    public void setFlySpeed(float value) {
        flySpeed = Math.max(0.05f, Math.min(1.0f, value));
        ModuleSettings.Setting s = settings.get("Fly", "speed");
        if (s != null) s.setNumber(flySpeed);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && modules.enabled("Fly")) {
            client.player.getAbilities().setFlySpeed(flySpeed);
        }
    }

    public float speedMultiplier() { return (float) settings.number("Speed", "multiplier", speedMultiplier); }
    public void setSpeedMultiplier(float value) {
        speedMultiplier = Math.max(1.0f, Math.min(3.0f, value));
        ModuleSettings.Setting s = settings.get("Speed", "multiplier");
        if (s != null) s.setNumber(speedMultiplier);
    }

    public float highJumpPower() { return (float) settings.number("HighJump", "power", highJumpPower); }
    public void setHighJumpPower(float value) {
        highJumpPower = Math.max(0.42f, Math.min(1.5f, value));
        ModuleSettings.Setting s = settings.get("HighJump", "power");
        if (s != null) s.setNumber(highJumpPower);
    }

    public float fastFallSpeed() { return (float) settings.number("FastFall", "speed", fastFallSpeed); }
    public void setFastFallSpeed(float value) {
        fastFallSpeed = Math.max(0.10f, Math.min(1.0f, value));
        ModuleSettings.Setting s = settings.get("FastFall", "speed");
        if (s != null) s.setNumber(fastFallSpeed);
    }

    public int zoomFov() { return (int) settings.number("Zoom", "fov", zoomFov); }
    public void setZoomFov(int value) {
        zoomFov = Math.max(10, Math.min(70, value));
        ModuleSettings.Setting s = settings.get("Zoom", "fov");
        if (s != null) s.setNumber(zoomFov);
    }

    public int espRange() { return (int) settings.number("ESP", "range", espRange); }
    public void setEspRange(int value) {
        espRange = Math.max(32, Math.min(256, value));
        ModuleSettings.Setting s = settings.get("ESP", "range");
        if (s != null) s.setNumber(espRange);
    }

    public boolean espPlayers() { return settings.bool("ESP", "players", espPlayers); }
    public void setEspPlayers(boolean value) { espPlayers = value; ModuleSettings.Setting s = settings.get("ESP", "players"); if (s != null) s.setBoolean(value); }

    public boolean espMobs() { return settings.bool("ESP", "mobs", espMobs); }
    public void setEspMobs(boolean value) { espMobs = value; ModuleSettings.Setting s = settings.get("ESP", "mobs"); if (s != null) s.setBoolean(value); }

    public boolean entityBoxes() { return settings.bool("ESP", "boxes", entityBoxes); }
    public void setEntityBoxes(boolean value) { entityBoxes = value; ModuleSettings.Setting s = settings.get("ESP", "boxes"); if (s != null) s.setBoolean(value); }

    public boolean entityLabels() { return settings.bool("ESP", "labels", entityLabels); }
    public void setEntityLabels(boolean value) { entityLabels = value; ModuleSettings.Setting s = settings.get("ESP", "labels"); if (s != null) s.setBoolean(value); }

    public boolean worldLabels() { return worldLabels; }
    public void setWorldLabels(boolean value) { worldLabels = value; }

    public boolean worldBoxes() { return worldBoxes; }
    public void setWorldBoxes(boolean value) { worldBoxes = value; }

    public boolean hudCoordinates() { return settings.bool("HUD", "coordinates", hudCoordinates); }
    public void setHudCoordinates(boolean value) { hudCoordinates = value; ModuleSettings.Setting s = settings.get("HUD", "coordinates"); if (s != null) s.setBoolean(value); }

    public boolean hudActiveModules() { return settings.bool("HUD", "modules", hudActiveModules); }
    public void setHudActiveModules(boolean value) { hudActiveModules = value; ModuleSettings.Setting s = settings.get("HUD", "modules"); if (s != null) s.setBoolean(value); }

    public int radarRows() { return (int) settings.number("Radar", "rows", radarRows); }
    public void setRadarRows(int value) { radarRows = Math.max(4, Math.min(16, value)); ModuleSettings.Setting s = settings.get("Radar", "rows"); if (s != null) s.setNumber(radarRows); }

    public int guiOpacity() { return (int) settings.number("HUD", "opacity", guiOpacity); }
    public void setGuiOpacity(int value) { guiOpacity = Math.max(90, Math.min(235, value)); ModuleSettings.Setting s = settings.get("HUD", "opacity"); if (s != null) s.setNumber(guiOpacity); }
}
