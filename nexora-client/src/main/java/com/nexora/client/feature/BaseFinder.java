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
    private int tickCounter;

    public List<StorageHit> hits() { return List.copyOf(hits); }
    public List<BaseCandidate> candidates() { return List.copyOf(candidates); }

    public void tick(MinecraftClient client, boolean storageEsp, boolean baseFinder) {
        if (!storageEsp && !baseFinder) {
            hits.clear();
            candidates.clear();
            return;
        }
        if (client.player == null || client.world == null) return;
        if (++tickCounter % 40 != 0) return;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        BlockPos center = client.player.getBlockPos();
        int horizontal = 24;
        int vertical = 16;

        List<StorageHit> found = new ArrayList<>();
        int minY = Math.max(client.world.getBottomY(), center.getY() - vertical);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + vertical);

        outer:
        for (int x = center.getX() - horizontal; x <= center.getX() + horizontal; x++) {
            for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    String type = storageType(state);
                    if (type != null) {
                        double distance = Math.sqrt(center.getSquaredDistance(pos));
                        found.add(new StorageHit(pos.toImmutable(), type, distance));
                        if (found.size() >= 96) break outer;
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

        for (int i = 0; i < found.size(); i++) {
            if (consumed[i]) continue;
            StorageHit seed = found.get(i);
            int count = 0;
            long sx = 0, sy = 0, sz = 0;

            for (int j = i; j < found.size(); j++) {
                if (consumed[j]) continue;
                StorageHit other = found.get(j);
                if (seed.pos().getSquaredDistance(other.pos()) <= 16 * 16) {
                    consumed[j] = true;
                    count++;
                    sx += other.pos().getX();
                    sy += other.pos().getY();
                    sz += other.pos().getZ();
                }
            }

            if (count >= 3) {
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
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) return "Chest";
        if (block == Blocks.BARREL) return "Barrel";
        if (block == Blocks.HOPPER) return "Hopper";
        if (block == Blocks.ENDER_CHEST) return "Ender Chest";
        if (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER) return "Furnace";
        if (block instanceof ShulkerBoxBlock) return "Shulker";
        return null;
    }
}
