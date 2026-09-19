package com.nexora.client.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

public final class RelogManager {
    private ServerInfo pendingServer;
    private int remainingTicks = -1;

    public boolean start(MinecraftClient client, int delayTicks) {
        ServerInfo current = client.getCurrentServerEntry();
        if (current == null || client.isInSingleplayer()) return false;
        pendingServer = current;
        remainingTicks = Math.max(10, delayTicks);
        client.disconnect(Text.literal("Nexora relog"));
        return true;
    }

    public void tick(MinecraftClient client) {
        if (pendingServer == null || remainingTicks < 0) return;
        if (client.world != null) return;
        if (--remainingTicks > 0) return;

        ServerInfo target = pendingServer;
        pendingServer = null;
        remainingTicks = -1;

        MultiplayerScreen parent = new MultiplayerScreen(new TitleScreen());
        ConnectScreen.connect(parent, client, ServerAddress.parse(target.address), target, false, null);
    }
}
