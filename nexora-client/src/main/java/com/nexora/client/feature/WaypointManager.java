package com.nexora.client.feature;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WaypointManager {
    public record Waypoint(String name, BlockPos pos) {}

    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();

    public void add(String name, BlockPos pos) {
        waypoints.put(name.toLowerCase(), new Waypoint(name, pos.toImmutable()));
    }

    public boolean remove(String name) {
        return waypoints.remove(name.toLowerCase()) != null;
    }

    public Collection<Waypoint> all() {
        return waypoints.values();
    }
}
