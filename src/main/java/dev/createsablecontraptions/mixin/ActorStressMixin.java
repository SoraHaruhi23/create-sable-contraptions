package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorActors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class ActorStressMixin {
    @Inject(method = "calculateStressApplied", at = @At("HEAD"), cancellable = true)
    private void csc$movementActorsNeedNoStress(CallbackInfoReturnable<Float> cir) {
        if (ElevatorActors.physicalActor((KineticBlockEntity) (Object) this)) cir.setReturnValue(0f);
    }
}
