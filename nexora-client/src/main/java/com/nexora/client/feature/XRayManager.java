package com.nexora.client.feature;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class XRayManager {
    public record Hit(BlockPos pos, String blockId, double distance) {}

    private final Set<String> tracked = new LinkedHashSet<>();
    private final List<Hit> hits = new ArrayList<>();
    private int scanRange = 32;
    private int verticalRange = 32;
    private int tickCounter;

    public XRayManager() {
        tracked.add("minecraft:diamond_ore");
        tracked.add("minecraft:deepslate_diamond_ore");
        tracked.add("minecraft:ancient_debris");
        tracked.add("minecraft:emerald_ore");
        tracked.add("minecraft:deepslate_emerald_ore");
        tracked.add("minecraft:gold_ore");
        tracked.add("minecraft:deepslate_gold_ore");
        tracked.add("minecraft:iron_ore");
        tracked.add("minecraft:deepslate_iron_ore");
        tracked.add("minecraft:lapis_ore");
        tracked.add("minecraft:deepslate_lapis_ore");
        tracked.add("minecraft:redstone_ore");
        tracked.add("minecraft:deepslate_redstone_ore");
    }

    public List<Hit> hits() { return List.copyOf(hits); }
    public Set<String> tracked() { return Set.copyOf(tracked); }

    public boolean contains(String id) { return tracked.contains(id); }
    public boolean toggle(String id) {
        if (tracked.contains(id)) {
            tracked.remove(id);
            return false;
        }
        tracked.add(id);
        return true;
    }

    public int scanRange() { return scanRange; }
    public void setScanRange(int value) { scanRange = Math.max(8, Math.min(64, value)); }

    public int verticalRange() { return verticalRange; }
    public void setVerticalRange(int value) { verticalRange = Math.max(8, Math.min(48, value)); }

    public void tick(MinecraftClient client, boolean enabled) {
        if (!enabled) {
            hits.clear();
            return;
        }
        if (client.player == null || client.world == null || tracked.isEmpty()) return;
        if (++tickCounter % 20 != 0) return;

        BlockPos center = client.player.getBlockPos();
        int minY = Math.max(client.world.getBottomY(), center.getY() - verticalRange);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + verticalRange);
        List<Hit> found = new ArrayList<>();

        for (int x = center.getX() - scanRange; x <= center.getX() + scanRange; x++) {
            for (int z = center.getZ() - scanRange; z <= center.getZ() + scanRange; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    String id = Registries.BLOCK.getId(state.getBlock()).toString();
                    if (tracked.contains(id)) {
                        double distance = Math.sqrt(center.getSquaredDistance(pos));
                        found.add(new Hit(pos.toImmutable(), id, distance));
                    }
                }
            }
        }

        found.sort(Comparator.comparingDouble(Hit::distance));
        hits.clear();
        hits.addAll(found.subList(0, Math.min(found.size(), 2048)));
    }
}
