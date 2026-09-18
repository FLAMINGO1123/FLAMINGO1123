package com.nexora.client.render;

import com.nexora.client.NexoraClient;
import com.nexora.client.feature.BaseFinder;
import com.nexora.client.feature.BlockEspManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
        if (!block && !storage && !bases) return;

        MatrixStack matrices = context.matrices();
        if (matrices == null || context.consumers() == null) return;

        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.linesTranslucent());

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (block) {
            int shown = 0;
            for (BlockEspManager.BlockHit hit : nexora.blockEsp().hits()) {
                if (shown++ >= 72) break;
                drawBlockBox(matrices, lines, hit.pos(), 0.15f, 0.90f, 1.00f, 0.95f);
            }
        }

        if (storage) {
            int shown = 0;
            for (BaseFinder.StorageHit hit : nexora.baseFinder().hits()) {
                if (shown++ >= 64) break;
                drawBlockBox(matrices, lines, hit.pos(), 1.00f, 0.70f, 0.15f, 0.95f);
            }
        }

        if (bases) {
            int shown = 0;
            for (BaseFinder.BaseCandidate candidate : nexora.baseFinder().candidates()) {
                if (shown++ >= 10) break;
                BlockPos p = candidate.pos();
                double radius = Math.min(3.0, 1.0 + candidate.score() * 0.12);
                drawBox(
                        matrices, lines,
                        p.getX() + 0.5 - radius, p.getY() - 0.25, p.getZ() + 0.5 - radius,
                        p.getX() + 0.5 + radius, p.getY() + 2.0, p.getZ() + 0.5 + radius,
                        0.72f, 0.28f, 1.00f, 0.95f
                );
            }
        }

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
}
