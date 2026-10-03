package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.createsablecontraptions.bearing.BearingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ControlledContraptionEntity.class, remap = false)
public abstract class BearingEntityMixin {
    @Inject(method = {"setAngle", "setRotationAxis"}, at = @At("TAIL"))
    private void csc$queuePhysicalPose(CallbackInfo ci) {
        BearingBridge.target((ControlledContraptionEntity) (Object) this);
    }
}
