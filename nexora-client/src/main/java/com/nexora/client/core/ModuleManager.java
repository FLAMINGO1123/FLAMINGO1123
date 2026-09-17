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
        add(new Module("AutoWalk", "Movement", "Keeps the forward key held for you.", false));
        add(new Module("Speed", "Movement", "Client movement speed boost.", false));

        add(new Module("ESP", "Render", "Player/mob ESP. Right-click for block ESP settings.", false));
        add(new Module("BlockESP", "Render", "Scans loaded nearby blocks for your tracked block list.", false));
        add(new Module("StorageESP", "Render", "Finds loaded chests, barrels, hoppers and shulkers.", false));
        add(new Module("Fullbright", "Render", "Forces maximum vanilla gamma while enabled.", false));
        add(new Module("HUD", "Render", "Nexora HUD, radar and active module list.", true));

        add(new Module("BaseFinder", "World", "Scores loaded storage clusters as possible bases.", false));
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
