package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class ContraptionColliderMixin {
    @Inject(method = "collideBlocks", at = @At("HEAD"), cancellable = true)
    private static void csc$physicalSweepOwnsBlockCollision(AbstractContraptionEntity entity,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (ElevatorLink.managed(entity.getContraption())) cir.setReturnValue(false);
    }
    @Inject(method = "collideEntities", at = @At("HEAD"), cancellable = true)
    private static void csc$sableOwnsEntityCollision(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (ElevatorLink.managed(entity.getContraption())) ci.cancel();
    }
}
