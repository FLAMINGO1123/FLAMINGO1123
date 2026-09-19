package com.nexora.client.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        add(new Module("CrystalESP", "Combat", "Highlights nearby end crystals.", false));

        add(new Module("Fly", "Movement", "Client flight with adjustable speed.", false));
        add(new Module("Sprint", "Movement", "Automatically sprints while moving forward.", false));
        add(new Module("AutoWalk", "Movement", "Keeps forward movement held.", false));
        add(new Module("Speed", "Movement", "Adjustable client movement speed.", false));
        add(new Module("Freecam", "Movement", "Detaches the camera while your player stays anchored.", false));
        add(new Module("HighJump", "Movement", "Higher ground jumps with adjustable power.", false));
        add(new Module("AirJump", "Movement", "Allows a jump impulse while airborne.", false));

        add(new Module("ESP", "Render", "Entity boxes, labels and glow through terrain.", false));
        add(new Module("ItemESP", "Render", "Highlights dropped items.", false));
        add(new Module("Tracers", "Render", "Draws lines toward enabled ESP targets.", false));
        add(new Module("Fullbright", "Render", "Forces maximum vanilla gamma.", false));
        add(new Module("XRay", "Render", "Highlights selected ores in client-loaded terrain.", false));
        add(new Module("Zoom", "Render", "Adjustable client FOV zoom.", false));

        add(new Module("Waypoints", "World", "Shows saved waypoint markers and distances.", false));

        add(new Module("HUD", "Misc", "Compact Nexora HUD and radar.", true));

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
