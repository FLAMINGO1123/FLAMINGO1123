package com.nexora.client.mixin;

import com.nexora.client.NexoraClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void nexora$freecam(World area, Entity focusedEntity, boolean thirdPerson,
                                boolean inverseView, float tickProgress, CallbackInfo ci) {
        NexoraClient client = NexoraClient.INSTANCE;
        if (client == null || !client.modules().enabled("Freecam") || !client.freecam().active()) return;

        Vec3d pos = client.freecam().cameraPos();
        if (pos != null) setPos(pos.x, pos.y, pos.z);
    }
}
