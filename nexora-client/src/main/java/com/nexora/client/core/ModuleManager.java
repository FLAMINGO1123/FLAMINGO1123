package com.nexora.client.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        add(new Module("Fly", "Movement", "Client flight toggle with adjustable fly speed.", false));
        add(new Module("Sprint", "Movement", "Keeps sprint enabled while moving forward.", false));
        add(new Module("ESP", "Render", "Highlights nearby living entities using Minecraft's glow outline.", false));
        add(new Module("StorageESP", "Render", "Shows nearby loaded storage blocks in the Nexora HUD.", false));
        add(new Module("BaseFinder", "World", "Scores clusters of storage blocks in loaded nearby chunks.", false));
        add(new Module("HUD", "Render", "Shows Nexora status, coordinates and active modules.", true));
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
}
