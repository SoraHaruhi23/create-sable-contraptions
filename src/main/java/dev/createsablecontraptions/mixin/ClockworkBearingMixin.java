package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import dev.createsablecontraptions.bearing.BearingBridge;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClockworkBearingBlockEntity.class, remap = false)
public abstract class ClockworkBearingMixin {
    @Inject(method = "getHourArmSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$hourDock(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(dev.createsablecontraptions.docking.DockingMotion.angular(hourHand, cir.getReturnValue()));
    }
    @Inject(method = "getMinuteArmSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$minuteDock(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(dev.createsablecontraptions.docking.DockingMotion.angular(minuteHand, cir.getReturnValue()));
    }
    @Shadow protected ControlledContraptionEntity hourHand;
    @Shadow protected ControlledContraptionEntity minuteHand;
    @Inject(method = "tick", at = @At("HEAD"))
    private void csc$checkBothRotations(CallbackInfo ci) {
        var self = (ClockworkBearingBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide || !self.isRunning()) return;
        dev.createsablecontraptions.bearing.BearingMotion.check(hourHand, self.getHourArmSpeed());
        dev.createsablecontraptions.bearing.BearingMotion.check(minuteHand, self.getMinuteArmSpeed());
    }
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflightBothHands(CallbackInfo ci) {
        var self = (ClockworkBearingBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) return;
        var targets = new java.util.HashSet<BlockPos>();
        if (!BearingBridge.ready(hourHand, true, targets) || !BearingBridge.ready(minuteHand, true, targets)) ci.cancel();
    }
}
