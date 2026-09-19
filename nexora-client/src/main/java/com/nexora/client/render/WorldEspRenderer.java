package com.nexora.client.render;

import com.nexora.client.NexoraClient;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.BlockEspManager;
import com.nexora.client.feature.WaypointManager;
import com.nexora.client.feature.XRayManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class WorldEspRenderer {
    private final NexoraClient nexora;
    private final Deque<Vec3d> breadcrumbPoints = new ArrayDeque<>();
    private Vec3d lastBreadcrumb;

    public WorldEspRenderer(NexoraClient nexora) {
        this.nexora = nexora;
    }

    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    private void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        boolean block = nexora.modules().enabled("BlockESP");
        boolean storage = nexora.modules().enabled("StorageESP");
        boolean bases = nexora.modules().enabled("BaseFinder");
        boolean waypoints = nexora.modules().enabled("Waypoints");
        boolean tracers = nexora.modules().enabled("Tracers");
        boolean xray = nexora.modules().enabled("XRay");
        boolean breadcrumbs = nexora.modules().enabled("Breadcrumbs");
        boolean entityEsp = nexora.modules().enabled("ESP")
                || nexora.modules().enabled("ItemESP")
                || nexora.modules().enabled("CrystalESP");

        updateBreadcrumbs(client, breadcrumbs);

        if (!block && !storage && !bases && !waypoints && !tracers && !xray && !entityEsp && !breadcrumbs) return;

        MatrixStack matrices = context.matrices();
        if (matrices == null || context.consumers() == null) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (entityEsp && nexora.entityBoxes()) {
            drawEntityBoxes(client, matrices, lines);
        }

        if (block && nexora.worldBoxes()) {
            int shown = 0;
            for (BlockEspManager.BlockHit hit : nexora.blockEsp().hits()) {
                if (shown++ >= 96) break;
                drawBlockBox(matrices, lines, hit.pos(), 0.15f, 0.90f, 1.00f, 0.95f);
            }
        }

        if (xray && nexora.worldBoxes()) {
            int shown = 0;
            for (XRayManager.Hit hit : nexora.xray().hits()) {
                if (shown++ >= 128) break;
                drawBlockBox(matrices, lines, hit.pos(), 0.35f, 1.00f, 0.55f, 0.95f);
            }
        }

        if (storage && nexora.worldBoxes()) {
            int shown = 0;
            for (BaseFinder.StorageHit hit : nexora.baseFinder().hits()) {
                if (shown++ >= 80) break;
                drawBlockBox(matrices, lines, hit.pos(), 1.00f, 0.70f, 0.15f, 0.95f);
            }
        }

        if (bases && nexora.worldBoxes()) {
            int shown = 0;
            for (BaseFinder.BaseCandidate candidate : nexora.baseFinder().candidates()) {
                if (shown++ >= 12) break;
                BlockPos p = candidate.pos();
                double radius = Math.min(3.5, 1.0 + candidate.score() * 0.12);
                drawBox(
                        matrices, lines,
                        p.getX() + 0.5 - radius, p.getY() - 0.25, p.getZ() + 0.5 - radius,
                        p.getX() + 0.5 + radius, p.getY() + 2.25, p.getZ() + 0.5 + radius,
                        0.72f, 0.28f, 1.00f, 0.95f
                );
            }
        }

        if (tracers) {
            drawTracers(client, matrices, lines);
        }

        if (breadcrumbs) {
            drawBreadcrumbs(matrices, lines);
        }

        matrices.pop();

        if (entityEsp && nexora.entityLabels()) {
            drawEntityLabels(context, client);
        }

        if (bases && nexora.worldLabels()) {
            int shown = 0;
            for (BaseFinder.BaseCandidate candidate : nexora.baseFinder().candidates()) {
                if (shown++ >= 8) break;
                BlockPos p = candidate.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 2.7, p.getZ() + 0.5,
                        "Possible Base  " + (int) candidate.distance() + "m",
                        0xFFFF75EE);
            }
        }

        if (xray && nexora.worldLabels()) {
            int shown = 0;
            for (XRayManager.Hit hit : nexora.xray().hits()) {
                if (shown++ >= 24) break;
                BlockPos p = hit.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 1.25, p.getZ() + 0.5,
                        "XRay " + shortName(hit.blockId()) + "  " + (int) hit.distance() + "m",
                        0xFF7CFF9D);
            }
        }

        if (storage && nexora.worldLabels()) {
            int shown = 0;
            for (BaseFinder.StorageHit hit : nexora.baseFinder().hits()) {
                if (shown++ >= 14) break;
                BlockPos p = hit.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 1.25, p.getZ() + 0.5,
                        hit.type() + "  " + (int) hit.distance() + "m",
                        0xFFFFD76A);
            }
        }

        if (block && nexora.worldLabels()) {
            int shown = 0;
            for (BlockEspManager.BlockHit hit : nexora.blockEsp().hits()) {
                if (shown++ >= 18) break;
                BlockPos p = hit.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 1.25, p.getZ() + 0.5,
                        shortName(hit.blockId()) + "  " + (int) hit.distance() + "m",
                        0xFF65ECFF);
            }
        }

        if (waypoints && nexora.worldLabels()) {
            int shown = 0;
            for (WaypointManager.Waypoint waypoint : nexora.waypoints().all()) {
                if (shown++ >= 12) break;
                BlockPos p = waypoint.pos();
                double distance = Math.sqrt(client.player.getBlockPos().getSquaredDistance(p));
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 2.0, p.getZ() + 0.5,
                        waypoint.name() + "  " + (int) distance + "m",
                        0xFFB99CFF);
            }
        }
    }

    private void updateBreadcrumbs(MinecraftClient client, boolean enabled) {
        if (!enabled) {
            breadcrumbPoints.clear();
            lastBreadcrumb = null;
            return;
        }

        Vec3d pos = new Vec3d(client.player.getX(), client.player.getY() + 0.1, client.player.getZ());
        if (lastBreadcrumb == null || pos.squaredDistanceTo(lastBreadcrumb) >= 0.20) {
            breadcrumbPoints.addLast(pos);
            lastBreadcrumb = pos;
            while (breadcrumbPoints.size() > 140) breadcrumbPoints.removeFirst();
        }
    }

    private void drawBreadcrumbs(MatrixStack matrices, VertexConsumer lines) {
        Vec3d previous = null;
        int index = 0;
        int total = Math.max(1, breadcrumbPoints.size());

        for (Vec3d point : breadcrumbPoints) {
            if (previous != null) {
                float t = (float) index / total;
                line(
                        matrices, lines,
                        previous.x, previous.y, previous.z,
                        point.x, point.y, point.z,
                        0.45f + t * 0.25f, 0.28f, 1.0f, 0.75f
                );
            }
            previous = point;
            index++;
        }
    }

    private void drawEntityBoxes(MinecraftClient client, MatrixStack matrices, VertexConsumer lines) {
        double maxSq = (double) nexora.espRange() * nexora.espRange();
        int shown = 0;

        for (Entity entity : client.world.getEntities()) {
            if (shown >= 80) break;
            if (!isEntityTarget(client, entity)) continue;
            if (entity.squaredDistanceTo(client.player) > maxSq) continue;

            Box box = entity.getBoundingBox().expand(0.04);
            float[] color = entityColor(entity);

            drawBox(
                    matrices, lines,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    color[0], color[1], color[2], 0.95f
            );
            shown++;
        }
    }

    private void drawEntityLabels(WorldRenderContext context, MinecraftClient client) {
        double maxSq = (double) nexora.espRange() * nexora.espRange();
        int shown = 0;

        for (Entity entity : client.world.getEntities()) {
            if (shown >= 40) break;
            if (!isEntityTarget(client, entity)) continue;
            if (entity.squaredDistanceTo(client.player) > maxSq) continue;

            double distance = Math.sqrt(entity.squaredDistanceTo(client.player));
            String name;

            if (entity instanceof ItemEntity item) {
                name = item.getStack().getName().getString();
            } else if (entity.getType() == EntityType.END_CRYSTAL) {
                name = "End Crystal";
            } else {
                name = entity.getName().getString();
            }

            int color = entity instanceof PlayerEntity
                    ? 0xFFB99CFF
                    : entity instanceof ItemEntity
                    ? 0xFF65ECFF
                    : entity.getType() == EntityType.END_CRYSTAL
                    ? 0xFFFF72E8
                    : 0xFFFFD76A;

            drawLabel(
                    context,
                    client,
                    entity.getX(),
                    entity.getBoundingBox().maxY + 0.35,
                    entity.getZ(),
                    name + "  " + (int) distance + "m",
                    color
            );
            shown++;
        }
    }

    private boolean isEntityTarget(MinecraftClient client, Entity entity) {
        if (entity == client.player) return false;

        if (entity instanceof PlayerEntity) {
            return nexora.modules().enabled("ESP") && nexora.espPlayers();
        }

        if (entity instanceof ItemEntity) {
            return nexora.modules().enabled("ItemESP");
        }

        if (entity.getType() == EntityType.END_CRYSTAL) {
            return nexora.modules().enabled("CrystalESP");
        }

        if (entity instanceof LivingEntity) {
            return nexora.modules().enabled("ESP") && nexora.espMobs();
        }

        return false;
    }

    private float[] entityColor(Entity entity) {
        if (entity instanceof PlayerEntity) return new float[]{0.72f, 0.50f, 1.00f};
        if (entity instanceof ItemEntity) return new float[]{0.20f, 0.90f, 1.00f};
        if (entity.getType() == EntityType.END_CRYSTAL) return new float[]{1.00f, 0.35f, 0.85f};
        return new float[]{1.00f, 0.78f, 0.28f};
    }

    private void drawTracers(MinecraftClient client, MatrixStack matrices, VertexConsumer lines) {
        Vec3d start = client.gameRenderer.getCamera().getCameraPos();
        double maxSq = (double) nexora.espRange() * nexora.espRange();
        int shown = 0;

        for (Entity entity : client.world.getEntities()) {
            if (shown >= 40) break;
            if (!isEntityTarget(client, entity)) continue;
            if (entity.squaredDistanceTo(client.player) > maxSq) continue;

            Vec3d end = entity.getBoundingBox().getCenter();
            float[] color = entityColor(entity);

            line(
                    matrices, lines,
                    start.x, start.y, start.z,
                    end.x, end.y, end.z,
                    color[0], color[1], color[2], 0.82f
            );
            shown++;
        }
    }

    private void drawLabel(WorldRenderContext context, MinecraftClient client,
                           double x, double y, double z,
                           String label, int color) {
        MatrixStack matrices = context.matrices();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();

        matrices.push();
        matrices.translate(x - camera.x, y - camera.y, z - camera.z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.scale(0.025f, -0.025f, 0.025f);

        float textX = -client.textRenderer.getWidth(label) / 2.0f;
        client.textRenderer.draw(
                label,
                textX,
                0.0f,
                color,
                false,
                matrices.peek().getPositionMatrix(),
                context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH,
                0x66000000,
                LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        matrices.pop();
    }

    private void drawBlockBox(MatrixStack matrices, VertexConsumer lines, BlockPos p,
                              float r, float g, float b, float a) {
        double e = 0.003;
        drawBox(
                matrices, lines,
                p.getX() + e, p.getY() + e, p.getZ() + e,
                p.getX() + 1.0 - e, p.getY() + 1.0 - e, p.getZ() + 1.0 - e,
                r, g, b, a
        );
    }

    private void drawBox(MatrixStack matrices, VertexConsumer out,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         float r, float g, float b, float a) {
        line(matrices, out, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(matrices, out, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(matrices, out, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(matrices, out, x1,y1,z2, x1,y1,z1, r,g,b,a);

        line(matrices, out, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(matrices, out, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(matrices, out, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(matrices, out, x1,y2,z2, x1,y2,z1, r,g,b,a);

        line(matrices, out, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(matrices, out, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(matrices, out, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(matrices, out, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private void line(MatrixStack matrices, VertexConsumer out,
                      double x1, double y1, double z1,
                      double x2, double y2, double z2,
                      float r, float g, float b, float a) {
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 0.0001f) return;

        dx /= len;
        dy /= len;
        dz /= len;

        MatrixStack.Entry entry = matrices.peek();

        out.vertex(entry, (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .normal(entry, dx, dy, dz)
                .lineWidth(2.0f);

        out.vertex(entry, (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .normal(entry, dx, dy, dz)
                .lineWidth(2.0f);
    }

    private String shortName(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = s.split("_");
        StringBuilder out = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }

        return out.toString();
    }
}
