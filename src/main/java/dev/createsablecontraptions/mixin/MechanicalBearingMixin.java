package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import dev.createsablecontraptions.bearing.BearingBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MechanicalBearingMixin {
    @Inject(method = "getAngularSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$dockApproach(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        var self = (MechanicalBearingBlockEntity) (Object) this;
        cir.setReturnValue(dev.createsablecontraptions.docking.DockingMotion.angular(self.getMovedContraption(), cir.getReturnValue()));
    }
    @org.spongepowered.asm.mixin.Shadow protected double sequencedAngleLimit;
    @Inject(method = "tick", at = @At("HEAD"))
    private void csc$checkRotation(CallbackInfo ci) {
        var self = (MechanicalBearingBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide || !self.isRunning()) return;
        float speed = self.getAngularSpeed();
        if (sequencedAngleLimit >= 0) speed = (float) net.minecraft.util.Mth.clamp(speed, -sequencedAngleLimit, sequencedAngleLimit);
        dev.createsablecontraptions.bearing.BearingMotion.check(self.getMovedContraption(), speed);
    }
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflight(CallbackInfo ci) {
        var self = (MechanicalBearingBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) return;
        if (!BearingBridge.ready(self.getMovedContraption(), self instanceof WindmillBearingBlockEntity, new java.util.HashSet<>())) ci.cancel();
    }
}
