package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.linear.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = LinearActuatorBlockEntity.class, remap = false)
public abstract class LinearActuatorMixin implements LinearActuatorLink {
    @Shadow protected abstract Vec3 toMotionVector(float speed);
    @Shadow protected abstract Vec3 toPosition(float offset);
    @Shadow protected abstract int getExtensionRange();
    @Shadow protected abstract int getGridOffset(float offset);
    @Unique private final LinearStep csc$step = new LinearStep();
    @Unique private boolean csc$ticking;
    @Unique private boolean csc$managed() {
        var self = (LinearActuatorBlockEntity) (Object) this;
        return self.movedContraption != null && ElevatorLink.managed(self.movedContraption.getContraption())
                && !(self.movedContraption.getContraption() instanceof ElevatorContraption);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void csc$begin(CallbackInfo ci) { csc$step.reset(); csc$ticking = true; }
    @Inject(method = "tick", at = @At("RETURN"))
    private void csc$follow(CallbackInfo ci) {
        csc$ticking = false;
        var self = (LinearActuatorBlockEntity) (Object) this;
        if (csc$managed()) { LinearMotion.target(self.movedContraption); if (!self.getLevel().isClientSide) self.setChanged(); }
    }
    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$sweep(CallbackInfoReturnable<Float> cir) { cir.setReturnValue(csc$checkSpeed(cir.getReturnValue())); }
    public float csc$checkSpeed(float speed) {
        var self = (LinearActuatorBlockEntity) (Object) this;
        if (!csc$managed()) return speed;
        if (self.getLevel().isClientSide) return self.movedContraption.isStalled() ? 0 : speed;
        if (!csc$ticking) return speed;
        if (csc$step.checked()) return csc$step.speed();
        float clamped = Math.max(-self.offset, Math.min(getExtensionRange() - self.offset, speed));
        clamped *= (float) dev.createsablecontraptions.docking.DockingMotion.linear(self.movedContraption, toMotionVector(clamped));
        return csc$step.accept(clamped, self.offset, getExtensionRange(), LinearMotion.check(self.movedContraption, toMotionVector(clamped)));
    }
    @Inject(method = "moveAndCollideContraption", at = @At("HEAD"), cancellable = true)
    private void csc$acceptedMove(CallbackInfoReturnable<Boolean> cir) {
        var self = (LinearActuatorBlockEntity) (Object) this;
        if (!csc$managed()) return;
        Vec3 motion = self.movedContraption.isStalled() ? Vec3.ZERO : toMotionVector(self.getLevel().isClientSide
                ? self.getMovementSpeed() : csc$step.take());
        self.movedContraption.setContraptionMotion(motion);
        self.movedContraption.move(motion.x, motion.y, motion.z);
        cir.setReturnValue(false);
    }
    @Inject(method = "resetContraptionToOffset", at = @At("RETURN"))
    private void csc$resetTarget(CallbackInfo ci) {
        if (csc$managed()) LinearMotion.target(((LinearActuatorBlockEntity) (Object) this).movedContraption);
    }
    public boolean csc$readyToDisassemble(boolean grid) {
        var self = (LinearActuatorBlockEntity) (Object) this;
        if (!csc$managed() || !(self.getLevel() instanceof ServerLevel level)) return true;
        var position = toPosition(grid ? getGridOffset(self.offset) : self.offset);
        return dev.createsablecontraptions.bearing.BearingBridge.canDisassemble(level, self.movedContraption.getContraption(),
                new StructureTransform(BlockPos.containing(position.add(.5, .5, .5)), 0, 0, 0), new java.util.HashSet<>());
    }
}
