package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import dev.createsablecontraptions.linear.LinearActuatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = MechanicalPistonBlockEntity.class, remap = false)
public abstract class MechanicalPistonMixin {
    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$sweep(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(((LinearActuatorLink) this).csc$checkSpeed(cir.getReturnValue()));
    }
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflight(CallbackInfo ci) {
        if (!((LinearActuatorLink) this).csc$readyToDisassemble(false)) ci.cancel();
    }
}
