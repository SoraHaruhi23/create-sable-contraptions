package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClockworkBearingBlockEntity.class, remap = false)
public abstract class ClockworkControllerAngleMixin {
    @Shadow protected ControlledContraptionEntity hourHand;
    @Inject(method = "getInterpolatedAngle", at = @At("HEAD"), cancellable = true)
    private void csc$physicalHead(float partialTicks, CallbackInfoReturnable<Float> cir) {
        var self = (ClockworkBearingBlockEntity) (Object) this;
        if (!self.isVirtual() && hourHand != null && hourHand.level().isClientSide && ElevatorLink.managed(hourHand.getContraption()))
            cir.setReturnValue(hourHand.getAngle(partialTicks));
    }
}
