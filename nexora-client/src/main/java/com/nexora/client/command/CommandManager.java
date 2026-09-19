package com.nexora.client.command;

import com.nexora.client.NexoraClient;
import com.nexora.client.core.Module;
import com.nexora.client.gui.NexoraScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CommandManager {
    private final NexoraClient nexora;

    public CommandManager(NexoraClient nexora) {
        this.nexora = nexora;
    }

    public boolean handle(String raw) {
        if (raw == null || !raw.startsWith(".")) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        String input = raw.substring(1).trim();

        if (input.isEmpty()) {
            help(client);
            return true;
        }

        String[] args = input.split("\\s+");
        String cmd = args[0].toLowerCase(Locale.ROOT);

        switch (cmd) {
            case "help" -> help(client);
            case "gui" -> client.setScreen(new NexoraScreen(nexora));
            case "modules" -> modules(client);
            case "toggle" -> toggle(client, args);
            case "on" -> setModule(client, args, true);
            case "off" -> setModule(client, args, false);
            case "panic" -> panic(client);

            case "esp" -> simpleModule(client, "ESP", args);
            case "xray" -> simpleModule(client, "XRay", args);
            case "freecam" -> simpleModule(client, "Freecam", args);
            case "radar" -> simpleModule(client, "Radar", args);
            case "hud" -> simpleModule(client, "HUD", args);

            case "flyspeed" -> flySpeed(client, args);
            case "speed" -> speed(client, args);
            case "highjump" -> highJump(client, args);
            case "fastfall" -> fastFall(client, args);
            case "zoom" -> zoom(client, args);
            case "freecamspeed" -> freecamSpeed(client, args);

            case "esprange" -> espRange(client, args);
            case "blockrange" -> blockRange(client, args);
            case "xrayrange" -> xrayRange(client, args);
            case "xrayvertical" -> xrayVertical(client, args);

            case "opacity" -> opacity(client, args);
            case "radarrows" -> radarRows(client, args);
            case "hudcoords" -> hudCoords(client, args);
            case "hudmodules" -> hudModules(client, args);

            case "base" -> base(client, args);
            case "blockesp" -> blockEsp(client, args);
            case "relog" -> relog(client, args);
            case "waypoint", "wp" -> waypoint(client, args);
            default -> chat(client, "§cUnknown command. Try .help");
        }

        return true;
    }

    private void help(MinecraftClient client) {
        chat(client, "§d§lNexora V8 §7commands");
        chat(client, "§f.gui §8| §f.modules §8| §f.panic");
        chat(client, "§f.toggle <module> §8| §f.on <module> §8| §f.off <module>");
        chat(client, "§f.esp on/off §8| §f.xray on/off §8| §f.freecam on/off");
        chat(client, "§f.hud on/off §8| §f.radar on/off §8| §f.opacity <90-235>");
        chat(client, "§f.esprange <32-256> §8| §f.blockrange <8-64> §8| §f.xrayrange <8-64>");
        chat(client, "§f.flyspeed <0.05-1> §8| §f.speed <1-3> §8| §f.freecamspeed <0.1-3>");
        chat(client, "§f.highjump <0.42-1.5> §8| §f.fastfall <0.1-1> §8| §f.zoom <10-70>");
        chat(client, "§f.base range/vertical/min/radius/delay <value>");
        chat(client, "§f.blockesp add/remove/list/defaults/clear <block_id>");
        chat(client, "§f.radarrows <4-16> §8| §f.hudcoords on/off §8| §f.hudmodules on/off");
        chat(client, "§f.relog [seconds] §8| §f.wp add/list/remove");
    }

    private void modules(MinecraftClient client) {
        String list = nexora.modules().all().stream()
                .map(m -> (m.enabled() ? "§a" : "§7") + m.name())
                .collect(Collectors.joining("§8, "));
        chat(client, list);
    }

    private void toggle(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§cUsage: .toggle <module>");
            return;
        }

        Module module = nexora.modules().get(args[1]);
        if (module == null) {
            chat(client, "§cModule not found.");
            return;
        }

        module.toggle();
        nexora.onModuleToggled(module);
        stateMessage(client, module);
    }

    private void setModule(MinecraftClient client, String[] args, boolean enabled) {
        if (args.length < 2) {
            chat(client, "§cUsage: ." + (enabled ? "on" : "off") + " <module>");
            return;
        }

        Module module = nexora.modules().get(args[1]);
        if (module == null) {
            chat(client, "§cModule not found.");
            return;
        }

        setModuleState(module, enabled);
        stateMessage(client, module);
    }

    private void simpleModule(MinecraftClient client, String name, String[] args) {
        Module module = nexora.modules().get(name);
        if (module == null) return;

        if (args.length < 2) {
            chat(client, "§d" + name + " §7is " + (module.enabled() ? "§aON" : "§cOFF"));
            return;
        }

        Boolean value = parseOnOff(args[1]);
        if (value == null) {
            chat(client, "§cUsage: ." + name.toLowerCase(Locale.ROOT) + " on/off");
            return;
        }

        setModuleState(module, value);
        stateMessage(client, module);
    }

    private void setModuleState(Module module, boolean enabled) {
        if (module.enabled() == enabled) return;
        module.setEnabled(enabled);
        nexora.onModuleToggled(module);
    }

    private void stateMessage(MinecraftClient client, Module module) {
        chat(client, "§d" + module.name() + " §7-> " + (module.enabled() ? "§aON" : "§cOFF"));
    }

    private void panic(MinecraftClient client) {
        for (Module module : nexora.modules().all()) {
            if (!module.enabled()) continue;
            module.setEnabled(false);
            nexora.onModuleToggled(module);
        }
        chat(client, "§cAll Nexora modules disabled.");
    }

    private void flySpeed(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Fly speed: §d" + fmt(nexora.flySpeed()));
            return;
        }
        try {
            nexora.setFlySpeed(Float.parseFloat(args[1]));
            chat(client, "§7Fly speed: §d" + fmt(nexora.flySpeed()));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .flyspeed <0.05-1.0>");
        }
    }

    private void speed(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Speed multiplier: §d" + fmt(nexora.speedMultiplier()));
            return;
        }
        try {
            nexora.setSpeedMultiplier(Float.parseFloat(args[1]));
            chat(client, "§7Speed multiplier: §d" + fmt(nexora.speedMultiplier()));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .speed <1.0-3.0>");
        }
    }

    private void highJump(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7HighJump power: §d" + fmt(nexora.highJumpPower()));
            return;
        }
        try {
            nexora.setHighJumpPower(Float.parseFloat(args[1]));
            chat(client, "§7HighJump power: §d" + fmt(nexora.highJumpPower()));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .highjump <0.42-1.5>");
        }
    }

    private void fastFall(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7FastFall speed: §d" + fmt(nexora.fastFallSpeed()));
            return;
        }
        try {
            nexora.setFastFallSpeed(Float.parseFloat(args[1]));
            chat(client, "§7FastFall speed: §d" + fmt(nexora.fastFallSpeed()));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .fastfall <0.1-1.0>");
        }
    }

    private void zoom(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Zoom FOV: §d" + nexora.zoomFov());
            return;
        }
        try {
            nexora.setZoomFov(Integer.parseInt(args[1]));
            chat(client, "§7Zoom FOV: §d" + nexora.zoomFov());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .zoom <10-70>");
        }
    }

    private void freecamSpeed(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Freecam speed: §d" + fmt(nexora.freecam().speed()));
            return;
        }
        try {
            nexora.freecam().setSpeed(Float.parseFloat(args[1]));
            chat(client, "§7Freecam speed: §d" + fmt(nexora.freecam().speed()));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .freecamspeed <0.1-3.0>");
        }
    }

    private void espRange(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Entity ESP range: §d" + nexora.espRange());
            return;
        }
        try {
            nexora.setEspRange(Integer.parseInt(args[1]));
            chat(client, "§7Entity ESP range: §d" + nexora.espRange());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .esprange <32-256>");
        }
    }

    private void blockRange(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Block ESP range: §d" + nexora.blockEsp().scanRange());
            return;
        }
        try {
            nexora.blockEsp().setScanRange(Integer.parseInt(args[1]));
            chat(client, "§7Block ESP range: §d" + nexora.blockEsp().scanRange());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .blockrange <8-64>");
        }
    }

    private void xrayRange(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7XRay range: §d" + nexora.xray().scanRange());
            return;
        }
        try {
            nexora.xray().setScanRange(Integer.parseInt(args[1]));
            chat(client, "§7XRay range: §d" + nexora.xray().scanRange());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .xrayrange <8-64>");
        }
    }

    private void xrayVertical(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7XRay vertical range: §d" + nexora.xray().verticalRange());
            return;
        }
        try {
            nexora.xray().setVerticalRange(Integer.parseInt(args[1]));
            chat(client, "§7XRay vertical range: §d" + nexora.xray().verticalRange());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .xrayvertical <8-48>");
        }
    }

    private void opacity(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7GUI opacity: §d" + nexora.guiOpacity());
            return;
        }
        try {
            nexora.setGuiOpacity(Integer.parseInt(args[1]));
            chat(client, "§7GUI opacity: §d" + nexora.guiOpacity());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .opacity <90-235>");
        }
    }

    private void radarRows(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Radar rows: §d" + nexora.radarRows());
            return;
        }
        try {
            nexora.setRadarRows(Integer.parseInt(args[1]));
            chat(client, "§7Radar rows: §d" + nexora.radarRows());
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .radarrows <4-16>");
        }
    }

    private void hudCoords(MinecraftClient client, String[] args) {
        Boolean value = args.length >= 2 ? parseOnOff(args[1]) : null;
        if (value == null) {
            chat(client, "§7HUD coordinates: " + (nexora.hudCoordinates() ? "§aON" : "§cOFF"));
            return;
        }
        nexora.setHudCoordinates(value);
        chat(client, "§7HUD coordinates: " + (value ? "§aON" : "§cOFF"));
    }

    private void hudModules(MinecraftClient client, String[] args) {
        Boolean value = args.length >= 2 ? parseOnOff(args[1]) : null;
        if (value == null) {
            chat(client, "§7HUD active modules: " + (nexora.hudActiveModules() ? "§aON" : "§cOFF"));
            return;
        }
        nexora.setHudActiveModules(value);
        chat(client, "§7HUD active modules: " + (value ? "§aON" : "§cOFF"));
    }

    private void base(MinecraftClient client, String[] args) {
        if (args.length < 3) {
            chat(client, "§cUsage: .base range/vertical/min/radius/delay <value>");
            return;
        }

        try {
            int value = Integer.parseInt(args[2]);
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "range" -> nexora.baseFinder().setScanRange(value);
                case "vertical" -> nexora.baseFinder().setVerticalRange(value);
                case "min" -> nexora.baseFinder().setMinClusterSize(value);
                case "radius" -> nexora.baseFinder().setClusterRadius(value);
                case "delay" -> nexora.baseFinder().setUpdateDelay(value);
                default -> {
                    chat(client, "§cUsage: .base range/vertical/min/radius/delay <value>");
                    return;
                }
            }
            chat(client, "§aBaseFinder setting updated.");
        } catch (NumberFormatException e) {
            chat(client, "§cValue must be a number.");
        }
    }

    private void blockEsp(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§cUsage: .blockesp add/remove/list/defaults/clear <block_id>");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (nexora.blockEsp().trackedBlocks().isEmpty()) {
                    chat(client, "§7Block ESP list is empty.");
                    return;
                }
                chat(client, "§dTracked blocks:");
                for (String id : nexora.blockEsp().trackedBlocks()) chat(client, "§7• §f" + id);
            }
            case "defaults" -> {
                nexora.blockEsp().resetDefaults();
                chat(client, "§aRestored Block ESP defaults.");
            }
            case "clear" -> {
                nexora.blockEsp().clear();
                chat(client, "§aCleared Block ESP list.");
            }
            case "add" -> {
                if (args.length < 3) {
                    chat(client, "§cUsage: .blockesp add <minecraft:block>");
                    return;
                }
                if (nexora.blockEsp().add(args[2])) chat(client, "§aAdded §f" + args[2]);
                else chat(client, "§eAlready tracked or invalid id.");
            }
            case "remove" -> {
                if (args.length < 3) {
                    chat(client, "§cUsage: .blockesp remove <minecraft:block>");
                    return;
                }
                chat(client, nexora.blockEsp().remove(args[2])
                        ? "§aRemoved §f" + args[2]
                        : "§eNot in tracked list.");
            }
            default -> chat(client, "§cUsage: .blockesp add/remove/list/defaults/clear <block_id>");
        }
    }

    private void relog(MinecraftClient client, String[] args) {
        int seconds = 2;
        if (args.length >= 2) {
            try {
                seconds = Math.max(1, Math.min(30, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {}
        }

        if (nexora.relog().start(client, seconds * 20)) {
            chat(client, "§7Relogging in §d" + seconds + "s§7...");
        } else {
            chat(client, "§cRelog only works while connected to a multiplayer server.");
        }
    }

    private void waypoint(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§cUsage: .wp add/list/remove ...");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        if (sub.equals("list")) {
            if (nexora.waypoints().all().isEmpty()) {
                chat(client, "§7No waypoints.");
                return;
            }
            nexora.waypoints().all().forEach(w -> chat(client,
                    "§d" + w.name() + " §7" + w.pos().getX() + " " + w.pos().getY() + " " + w.pos().getZ()));
            return;
        }

        if (sub.equals("add")) {
            if (client.player == null || args.length < 3) {
                chat(client, "§cUsage: .wp add <name>");
                return;
            }
            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            nexora.waypoints().add(name, client.player.getBlockPos());
            chat(client, "§aAdded waypoint §f" + name);
            return;
        }

        if (sub.equals("remove")) {
            if (args.length < 3) {
                chat(client, "§cUsage: .wp remove <name>");
                return;
            }
            String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            chat(client, nexora.waypoints().remove(name)
                    ? "§aRemoved waypoint."
                    : "§cWaypoint not found.");
            return;
        }

        chat(client, "§cUsage: .wp add/list/remove ...");
    }

    private Boolean parseOnOff(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "1", "yes" -> true;
            case "off", "false", "0", "no" -> false;
            default -> null;
        };
    }

    private String fmt(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void chat(MinecraftClient client, String message) {
        client.inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
