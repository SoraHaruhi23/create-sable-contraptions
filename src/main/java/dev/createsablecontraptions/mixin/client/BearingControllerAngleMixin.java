package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class BearingControllerAngleMixin {
    @Inject(method = "getInterpolatedAngle", at = @At("HEAD"), cancellable = true)
    private void csc$physicalHead(float partialTicks, CallbackInfoReturnable<Float> cir) {
        var self = (MechanicalBearingBlockEntity) (Object) this;
        var entity = self.getMovedContraption();
        if (!self.isVirtual() && entity != null && entity.level().isClientSide && ElevatorLink.managed(entity.getContraption()))
            cir.setReturnValue(entity.getAngle(partialTicks));
    }
}
