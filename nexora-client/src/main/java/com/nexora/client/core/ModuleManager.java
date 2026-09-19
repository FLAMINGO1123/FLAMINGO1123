package com.nexora.client.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        add(new Module("TriggerBot", "Combat", "Attacks the entity under your crosshair when the cooldown is ready.", false));
        add(new Module("CrystalESP", "Combat", "Highlights nearby end crystals.", false));
        add(new Module("AimAssist", "Combat", "Turns toward the nearest living target in range.", false));
        add(new Module("AutoClicker", "Combat", "Automatically attacks the entity under your crosshair.", false));
        add(new Module("AutoShield", "Combat", "Raises an offhand shield when a target is close.", false));
        add(new Module("CriticalJump", "Combat", "Adds a small upward hop while attacking from the ground.", false));
        add(new Module("AutoSwing", "Combat", "Automatically swings your main hand at intervals.", false));
        add(new Module("KillAura", "Combat", "Attacks the nearest living target in melee range.", false));
        add(new Module("Reach", "Combat", "Attempts attacks on targets farther along your crosshair.", false));
        add(new Module("Velocity", "Combat", "Reduces local knockback after taking damage.", false));

        add(new Module("Fly", "Movement", "Client flight with adjustable speed.", false));
        add(new Module("Sprint", "Movement", "Automatically sprints while moving forward.", false));
        add(new Module("AutoWalk", "Movement", "Keeps forward movement held.", false));
        add(new Module("AutoSneak", "Movement", "Keeps sneak held until disabled.", false));
        add(new Module("Speed", "Movement", "Adjustable client movement speed.", false));
        add(new Module("BunnyHop", "Movement", "Automatically jumps while moving on the ground.", false));
        add(new Module("Freecam", "Movement", "Detaches the camera while your player stays anchored.", false));
        add(new Module("HighJump", "Movement", "Higher ground jumps with adjustable power.", false));
        add(new Module("AirJump", "Movement", "Allows a jump impulse while airborne.", false));
        add(new Module("FastFall", "Movement", "Pulls you downward faster while airborne.", false));
        add(new Module("Glide", "Movement", "Caps downward speed for a gentle glide.", false));
        add(new Module("Spider", "Movement", "Pushes you upward while colliding with a wall.", false));
        add(new Module("WaterSpeed", "Movement", "Boosts horizontal movement while swimming.", false));
        add(new Module("LongJump", "Movement", "Adds a stronger forward boost to your jump.", false));
        add(new Module("Jetpack", "Movement", "Applies upward thrust while holding jump.", false));
        add(new Module("SlowFall", "Movement", "Strongly reduces downward fall speed.", false));
        add(new Module("ReverseStep", "Movement", "Pulls you downward quickly after stepping off blocks.", false));
        add(new Module("StrafeBoost", "Movement", "Applies a horizontal strafe boost while moving.", false));
        add(new Module("NoSlow", "Movement", "Counteracts movement slowdown while using items.", false));
        add(new Module("SafeWalk", "Movement", "Stops horizontal movement at unsupported edges.", false));
        add(new Module("Jesus", "Movement", "Keeps you at the water surface with horizontal movement.", false));
        add(new Module("Parkour", "Movement", "Automatically jumps at approaching ledges.", false));
        add(new Module("Phase", "Movement", "Enables client noclip while active.", false));
        add(new Module("Step", "Movement", "Steps upward when colliding horizontally.", false));
        add(new Module("VehicleFly", "Movement", "Lets you steer and lift your current vehicle.", false));
        add(new Module("AntiVoid", "Movement", "Pushes you upward near the bottom of the world.", false));

        add(new Module("AutoRespawn", "Player", "Automatically respawns after death.", false));
        add(new Module("SpinBot", "Player", "Continuously rotates your view.", false));
        add(new Module("PitchLock", "Player", "Locks your pitch to the horizon.", false));
        add(new Module("YawLock", "Player", "Snaps your yaw to the nearest 45 degrees.", false));
        add(new Module("AutoDrop", "Player", "Drops one selected item at intervals.", false));
        add(new Module("HandSwing", "Player", "Periodically swings your hand.", false));
        add(new Module("KeepSprint", "Player", "Keeps sprint enabled while moving forward.", false));
        add(new Module("NoFall", "Player", "Singleplayer-only grounded-state NoFall test; disabled on remote multiplayer.", false));
        add(new Module("FastPlace", "Player", "Rapidly uses the held item while use is pressed.", false));
        add(new Module("FastBreak", "Player", "Sends block-break progress every tick on the targeted block.", false));
        add(new Module("Nuker", "Player", "Attempts to break nearby blocks automatically.", false));
        add(new Module("AutoTool", "Player", "Selects the fastest hotbar tool for the targeted block.", false));

        add(new Module("ESP", "Render", "Entity boxes, labels and glow through terrain.", false));
        add(new Module("ItemESP", "Render", "Highlights dropped items.", false));
        add(new Module("Tracers", "Render", "Draws lines toward enabled ESP targets.", false));
        add(new Module("Breadcrumbs", "Render", "Draws a trail behind your recent movement.", false));
        add(new Module("Fullbright", "Render", "Forces maximum vanilla gamma.", false));
        add(new Module("XRay", "Render", "Highlights selected ores in client-loaded terrain.", false));
        add(new Module("Zoom", "Render", "Adjustable client FOV zoom.", false));
        add(new Module("PlayerESP", "Render", "Targets players for ESP even with the main ESP module off.", false));
        add(new Module("MobESP", "Render", "Targets mobs for ESP even with the main ESP module off.", false));
        add(new Module("BoxESP", "Render", "Enables 3D entity boxes.", false));
        add(new Module("NameTags", "Render", "Shows entity names and distances through terrain.", false));
        add(new Module("GlowESP", "Render", "Uses Minecraft glow outlines for ESP targets.", false));

        add(new Module("Waypoints", "World", "Shows saved waypoint markers and distances.", false));

        add(new Module("HUD", "Misc", "Compact Nexora HUD.", true));
        add(new Module("Radar", "Misc", "Optional top-right loaded-target radar.", false));
        add(new Module("AutoMine", "Misc", "Keeps the attack/mine key held.", false));
        add(new Module("AutoUse", "Misc", "Keeps the use-item key held.", false));
        add(new Module("AntiAFK", "Misc", "Adds small periodic camera movement while idle.", false));

        add(new Module("SneakSpam", "Utility", "Rapidly toggles sneak.", false));
        add(new Module("JumpSpam", "Utility", "Automatically jumps at intervals while grounded.", false));
        add(new Module("UseSpam", "Utility", "Rapidly toggles the use key.", false));
        add(new Module("MineSpam", "Utility", "Rapidly toggles the attack/mine key.", false));
        add(new Module("QuickTurn", "Utility", "Turns your view 180 degrees at intervals.", false));

        add(new Module("BaseFinder", "DonutSMP", "Scores loaded storage clusters as possible bases.", false));
        add(new Module("StorageESP", "DonutSMP", "Finds loaded chests, barrels, hoppers and shulkers.", false));
        add(new Module("BlockESP", "DonutSMP", "Tracks chosen blocks in loaded client chunks.", false));
    }

    private void add(Module module) {
        modules.put(module.name().toLowerCase(Locale.ROOT), module);
    }

    public Module get(String name) {
        return name == null ? null : modules.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean enabled(String name) {
        Module module = get(name);
        return module != null && module.enabled();
    }

    public Collection<Module> all() {
        return modules.values();
    }

    public List<Module> category(String category) {
        return modules.values().stream()
                .filter(m -> m.category().equalsIgnoreCase(category))
                .toList();
    }
}
