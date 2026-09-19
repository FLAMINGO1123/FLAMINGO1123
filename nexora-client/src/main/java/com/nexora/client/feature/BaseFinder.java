package com.nexora.client.feature;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BaseFinder {
    public record StorageHit(BlockPos pos, String type, double distance) {}
    public record BaseCandidate(BlockPos pos, int score, double distance) {}

    private final List<StorageHit> hits = new ArrayList<>();
    private final List<BaseCandidate> candidates = new ArrayList<>();

    private int scanRange = 32;
    private int verticalRange = 20;
    private int updateDelay = 40;
    private int minClusterSize = 3;
    private int clusterRadius = 16;

    private boolean chests = true;
    private boolean barrels = true;
    private boolean hoppers = true;
    private boolean shulkers = true;
    private boolean furnaces = true;
    private boolean enderChests = true;

    private int tickCounter;

    public List<StorageHit> hits() { return List.copyOf(hits); }
    public List<BaseCandidate> candidates() { return List.copyOf(candidates); }

    public int scanRange() { return scanRange; }
    public void setScanRange(int value) { scanRange = Math.max(8, Math.min(64, value)); }

    public int verticalRange() { return verticalRange; }
    public void setVerticalRange(int value) { verticalRange = Math.max(8, Math.min(48, value)); }

    public int updateDelay() { return updateDelay; }
    public void setUpdateDelay(int value) { updateDelay = Math.max(10, Math.min(100, value)); }

    public int minClusterSize() { return minClusterSize; }
    public void setMinClusterSize(int value) { minClusterSize = Math.max(2, Math.min(20, value)); }

    public int clusterRadius() { return clusterRadius; }
    public void setClusterRadius(int value) { clusterRadius = Math.max(6, Math.min(32, value)); }

    public boolean chests() { return chests; }
    public void setChests(boolean value) { chests = value; }

    public boolean barrels() { return barrels; }
    public void setBarrels(boolean value) { barrels = value; }

    public boolean hoppers() { return hoppers; }
    public void setHoppers(boolean value) { hoppers = value; }

    public boolean shulkers() { return shulkers; }
    public void setShulkers(boolean value) { shulkers = value; }

    public boolean furnaces() { return furnaces; }
    public void setFurnaces(boolean value) { furnaces = value; }

    public boolean enderChests() { return enderChests; }
    public void setEnderChests(boolean value) { enderChests = value; }

    public void tick(MinecraftClient client, boolean storageEsp, boolean baseFinder) {
        if (!storageEsp && !baseFinder) {
            hits.clear();
            candidates.clear();
            return;
        }
        if (client.player == null || client.world == null) return;
        if (++tickCounter % updateDelay != 0) return;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        BlockPos center = client.player.getBlockPos();
        List<StorageHit> found = new ArrayList<>();

        int minY = Math.max(client.world.getBottomY(), center.getY() - verticalRange);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + verticalRange);

        outer:
        for (int x = center.getX() - scanRange; x <= center.getX() + scanRange; x++) {
            for (int z = center.getZ() - scanRange; z <= center.getZ() + scanRange; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    String type = storageType(client.world.getBlockState(pos));
                    if (type != null) {
                        double distance = Math.sqrt(center.getSquaredDistance(pos));
                        found.add(new StorageHit(pos.toImmutable(), type, distance));
                        if (found.size() >= 192) break outer;
                    }
                }
            }
        }

        found.sort(Comparator.comparingDouble(StorageHit::distance));
        hits.clear();
        hits.addAll(found);
        buildCandidates(center, found);
    }

    private void buildCandidates(BlockPos playerPos, List<StorageHit> found) {
        List<BaseCandidate> result = new ArrayList<>();
        boolean[] consumed = new boolean[found.size()];
        int radiusSq = clusterRadius * clusterRadius;

        for (int i = 0; i < found.size(); i++) {
            if (consumed[i]) continue;
            StorageHit seed = found.get(i);
            int count = 0;
            long sx = 0, sy = 0, sz = 0;

            for (int j = i; j < found.size(); j++) {
                if (consumed[j]) continue;
                StorageHit other = found.get(j);
                if (seed.pos().getSquaredDistance(other.pos()) <= radiusSq) {
                    consumed[j] = true;
                    count++;
                    sx += other.pos().getX();
                    sy += other.pos().getY();
                    sz += other.pos().getZ();
                }
            }

            if (count >= minClusterSize) {
                BlockPos avg = new BlockPos((int)(sx / count), (int)(sy / count), (int)(sz / count));
                double distance = Math.sqrt(playerPos.getSquaredDistance(avg));
                result.add(new BaseCandidate(avg, count, distance));
            }
        }

        result.sort(Comparator.comparingInt(BaseCandidate::score).reversed());
        candidates.clear();
        candidates.addAll(result);
    }

    private String storageType(BlockState state) {
        Block block = state.getBlock();
        if (chests && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)) return "Chest";
        if (barrels && block == Blocks.BARREL) return "Barrel";
        if (hoppers && block == Blocks.HOPPER) return "Hopper";
        if (enderChests && block == Blocks.ENDER_CHEST) return "Ender Chest";
        if (furnaces && (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER)) return "Furnace";
        if (shulkers && block instanceof ShulkerBoxBlock) return "Shulker";
        return null;
    }
}
