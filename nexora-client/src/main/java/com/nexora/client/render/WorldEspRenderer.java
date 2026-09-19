package com.nexora.client.render;

import com.nexora.client.NexoraClient;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.BlockEspManager;
import com.nexora.client.feature.WaypointManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public final class WorldEspRenderer {
    private final NexoraClient nexora;

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

        if (!block && !storage && !bases && !waypoints && !tracers) return;

        MatrixStack matrices = context.matrices();
        if (matrices == null || context.consumers() == null) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (block && nexora.worldBoxes()) {
            int shown = 0;
            for (BlockEspManager.BlockHit hit : nexora.blockEsp().hits()) {
                if (shown++ >= 96) break;
                drawBlockBox(matrices, lines, hit.pos(), 0.15f, 0.90f, 1.00f, 0.95f);
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

        matrices.pop();

        // Floating SEE_THROUGH text renders with Minecraft's no-depth text layer.
        // This keeps target names/distances readable even when blocks are between
        // the camera and the target.
        if (bases && nexora.worldLabels()) {
            int shown = 0;
            for (BaseFinder.BaseCandidate candidate : nexora.baseFinder().candidates()) {
                if (shown++ >= 8) break;
                BlockPos p = candidate.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 2.7, p.getZ() + 0.5,
                        "Possible Base  " + (int)candidate.distance() + "m",
                        0xFFFF75EE);
            }
        }

        if (storage && nexora.worldLabels()) {
            int shown = 0;
            for (BaseFinder.StorageHit hit : nexora.baseFinder().hits()) {
                if (shown++ >= 14) break;
                BlockPos p = hit.pos();
                drawLabel(context, client,
                        p.getX() + 0.5, p.getY() + 1.25, p.getZ() + 0.5,
                        hit.type() + "  " + (int)hit.distance() + "m",
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
                        shortName(hit.blockId()) + "  " + (int)hit.distance() + "m",
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
                        waypoint.name() + "  " + (int)distance + "m",
                        0xFFB99CFF);
            }
        }
    }

    private void drawTracers(MinecraftClient client, MatrixStack matrices, VertexConsumer lines) {
        Vec3d start = client.player.getEyePos();
        double maxSq = (double)nexora.espRange() * nexora.espRange();
        int shown = 0;

        for (Entity entity : client.world.getEntities()) {
            if (shown >= 40) break;
            if (entity == client.player) continue;
            if (entity.squaredDistanceTo(client.player) > maxSq) continue;

            boolean target = false;
            if (entity instanceof PlayerEntity && nexora.espPlayers()) target = true;
            else if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && nexora.espMobs()) target = true;
            else if (entity instanceof ItemEntity && nexora.modules().enabled("ItemESP")) target = true;

            if (!target) continue;
            shown++;

            Vec3d end = entity.getBoundingBox().getCenter();
            line(matrices, lines,
                    start.x, start.y, start.z,
                    end.x, end.y, end.z,
                    0.62f, 0.35f, 1.00f, 0.80f);
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
