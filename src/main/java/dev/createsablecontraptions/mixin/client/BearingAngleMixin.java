package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.ElevatorActors;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.bearing.BearingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ControlledContraptionEntity.class, remap = false)
public abstract class BearingAngleMixin {
    @Inject(method = "getAngle", at = @At("HEAD"), cancellable = true)
    private void csc$renderAtPhysicalAngle(float partialTicks, CallbackInfoReturnable<Float> cir) {
        var entity = (ControlledContraptionEntity) (Object) this;
        if (!entity.level().isClientSide || !ElevatorLink.bearing(entity.getContraption()) || entity.getRotationAxis() == null) return;
        if (!(ElevatorActors.sub(entity) instanceof ClientSubLevel sub)) return;
        var q = sub.renderPose(partialTicks).orientation();
        var unit = BearingBridge.rotation(entity, 1);
        var axis = entity.getRotationAxis();
        double component = axis == net.minecraft.core.Direction.Axis.X ? q.x() : axis == net.minecraft.core.Direction.Axis.Y ? q.y() : q.z();
        double reference = axis == net.minecraft.core.Direction.Axis.X ? unit.x : axis == net.minecraft.core.Direction.Axis.Y ? unit.y : unit.z;
        cir.setReturnValue((float) (Math.toDegrees(2 * Math.atan2(component, q.w())) * Math.signum(reference)));
    }
}
