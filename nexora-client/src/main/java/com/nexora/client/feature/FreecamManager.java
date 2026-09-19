package com.nexora.client.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class FreecamManager {
    private boolean active;
    private Vec3d anchorPos;
    private Vec3d cameraPos;
    private float speed = 0.65f;

    public boolean active() { return active; }
    public Vec3d cameraPos() { return cameraPos; }
    public float speed() { return speed; }

    public void setSpeed(float value) {
        speed = Math.max(0.10f, Math.min(3.0f, value));
    }

    public void enable(MinecraftClient client) {
        if (client.player == null) return;
        active = true;
        anchorPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        cameraPos = client.player.getEyePos();
    }

    public void disable(MinecraftClient client) {
        if (client.player != null && anchorPos != null) {
            client.player.setPosition(anchorPos.x, anchorPos.y, anchorPos.z);
            client.player.setVelocity(Vec3d.ZERO);
        }
        active = false;
        anchorPos = null;
        cameraPos = null;
    }

    public void tick(MinecraftClient client) {
        if (!active || client.player == null || cameraPos == null || anchorPos == null) return;

        if (client.currentScreen == null) {
            double yaw = Math.toRadians(client.player.getYaw());
            double forward = 0.0;
            double strafe = 0.0;
            double vertical = 0.0;

            if (client.options.forwardKey.isPressed()) forward += 1.0;
            if (client.options.backKey.isPressed()) forward -= 1.0;
            if (client.options.leftKey.isPressed()) strafe += 1.0;
            if (client.options.rightKey.isPressed()) strafe -= 1.0;
            if (client.options.jumpKey.isPressed()) vertical += 1.0;
            if (client.options.sneakKey.isPressed()) vertical -= 1.0;

            double horizontalLength = Math.sqrt(forward * forward + strafe * strafe);
            if (horizontalLength > 0.0) {
                forward /= horizontalLength;
                strafe /= horizontalLength;
            }

            double moveSpeed = speed * (client.options.sprintKey.isPressed() ? 2.0 : 1.0);
            double dx = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * moveSpeed;
            double dz = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * moveSpeed;
            double dy = vertical * moveSpeed;

            cameraPos = cameraPos.add(dx, dy, dz);
        }

        client.player.setPosition(anchorPos.x, anchorPos.y, anchorPos.z);
        client.player.setVelocity(Vec3d.ZERO);
    }
}
