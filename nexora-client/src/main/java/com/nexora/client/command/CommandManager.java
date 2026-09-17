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
            case "toggle" -> toggle(client, args);
            case "flyspeed" -> flySpeed(client, args);
            case "relog" -> relog(client, args);
            case "waypoint", "wp" -> waypoint(client, args);
            case "modules" -> modules(client);
            default -> chat(client, "§cUnknown command. Try .help");
        }
        return true;
    }

    private void help(MinecraftClient client) {
        chat(client, "§d§lNexora §7commands");
        chat(client, "§f.help §8| §f.gui §8| §f.modules");
        chat(client, "§f.toggle <module> §8| §f.flyspeed <0.05-1.0>");
        chat(client, "§f.relog [seconds] §8| §f.wp add <name> §8| §f.wp list §8| §f.wp remove <name>");
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
        chat(client, "§d" + module.name() + " §7-> " + (module.enabled() ? "§aON" : "§cOFF"));
    }

    private void flySpeed(MinecraftClient client, String[] args) {
        if (args.length < 2) {
            chat(client, "§7Fly speed: §d" + String.format(Locale.ROOT, "%.2f", nexora.flySpeed()));
            return;
        }
        try {
            float value = Float.parseFloat(args[1]);
            value = Math.max(0.05f, Math.min(1.0f, value));
            nexora.setFlySpeed(value);
            chat(client, "§7Fly speed set to §d" + String.format(Locale.ROOT, "%.2f", value));
        } catch (NumberFormatException e) {
            chat(client, "§cUsage: .flyspeed <0.05-1.0>");
        }
    }

    private void relog(MinecraftClient client, String[] args) {
        int seconds = 2;
        if (args.length >= 2) {
            try { seconds = Math.max(1, Math.min(30, Integer.parseInt(args[1]))); }
            catch (NumberFormatException ignored) {}
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
            chat(client, nexora.waypoints().remove(name) ? "§aRemoved waypoint." : "§cWaypoint not found.");
            return;
        }
        chat(client, "§cUsage: .wp add/list/remove ...");
    }

    private void chat(MinecraftClient client, String message) {
        client.inGameHud.getChatHud().addMessage(Text.literal(message));
    }
}
