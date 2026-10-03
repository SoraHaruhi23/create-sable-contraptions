package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The sub-level already has a body; do not register a second overlapping proxy body. */
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class RapierPipelineMixin {
    @Inject(method = "applyImpulse", at = @At("HEAD"), cancellable = true)
    private void csc$driveMinecart(dev.ryanhcode.sable.api.physics.PhysicsPipelineBody body,
            org.joml.Vector3dc position, org.joml.Vector3dc force, CallbackInfo ci) {
        if (body instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel sub
                && dev.createsablecontraptions.oriented.CartImpulse.apply(sub,force)) ci.cancel();
    }
    @Inject(method = "add(Ldev/ryanhcode/sable/api/sublevel/KinematicContraption;)V", at = @At("HEAD"), cancellable = true)
    private void csc$skipProxy(KinematicContraption contraption, CallbackInfo ci) {
        if (contraption instanceof AbstractContraptionEntity entity && ElevatorLink.managed(entity.getContraption())) ci.cancel();
    }
}
