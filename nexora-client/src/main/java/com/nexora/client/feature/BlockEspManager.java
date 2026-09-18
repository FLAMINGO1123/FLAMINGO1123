package com.nexora.client.feature;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BlockEspManager {
    public record BlockHit(BlockPos pos, String blockId, double distance) {}

    private final Set<String> tracked = new LinkedHashSet<>();
    private final List<BlockHit> hits = new ArrayList<>();
    private int scanRange = 32;
    private int tickCounter;

    public BlockEspManager() {
        resetDefaults();
    }

    public void resetDefaults() {
        tracked.clear();
        tracked.add("minecraft:diamond_ore");
        tracked.add("minecraft:deepslate_diamond_ore");
        tracked.add("minecraft:ancient_debris");
        tracked.add("minecraft:spawner");
        tracked.add("minecraft:chest");
        tracked.add("minecraft:trapped_chest");
        tracked.add("minecraft:barrel");
        tracked.add("minecraft:hopper");
        tracked.add("minecraft:ender_chest");
        tracked.add("minecraft:shulker_box");
    }

    public boolean add(String id) {
        String normalized = normalize(id);
        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) return false;
        return tracked.add(normalized);
    }

    public boolean remove(String id) {
        return tracked.remove(normalize(id));
    }

    public boolean toggle(String id) {
        String normalized = normalize(id);
        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) return false;

        if (tracked.contains(normalized)) {
            tracked.remove(normalized);
            return false;
        }
        tracked.add(normalized);
        return true;
    }

    public boolean contains(String id) {
        return tracked.contains(normalize(id));
    }

    public void clear() {
        tracked.clear();
        hits.clear();
    }

    public List<String> trackedBlocks() {
        return List.copyOf(tracked);
    }

    public List<BlockHit> hits() {
        return List.copyOf(hits);
    }

    public int scanRange() {
        return scanRange;
    }

    public void setScanRange(int value) {
        scanRange = Math.max(8, Math.min(64, value));
    }

    public void tick(MinecraftClient client, boolean enabled) {
        if (!enabled) {
            hits.clear();
            return;
        }
        if (client.player == null || client.world == null || tracked.isEmpty()) return;
        if (++tickCounter % 20 != 0) return;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        BlockPos center = client.player.getBlockPos();
        int horizontal = scanRange;
        int vertical = Math.min(32, Math.max(12, scanRange / 2));

        List<BlockHit> found = new ArrayList<>();
        int minY = Math.max(client.world.getBottomY(), center.getY() - vertical);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + vertical);

        outer:
        for (int x = center.getX() - horizontal; x <= center.getX() + horizontal; x++) {
            for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    String id = Registries.BLOCK.getId(state.getBlock()).toString();
                    if (tracked.contains(id)) {
                        double distance = Math.sqrt(center.getSquaredDistance(pos));
                        found.add(new BlockHit(pos.toImmutable(), id, distance));
                        if (found.size() >= 256) break outer;
                    }
                }
            }
        }

        found.sort(Comparator.comparingDouble(BlockHit::distance));
        hits.clear();
        hits.addAll(found);
    }

    private String normalize(String id) {
        String s = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (!s.contains(":")) s = "minecraft:" + s;
        return s;
    }
}
