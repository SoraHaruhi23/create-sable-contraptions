package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.bearing.StabilizedContraption;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.oriented.ChildAssemblyScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StabilizedContraption.class, remap = false)
public abstract class StabilizedContraptionMixin {
    @Inject(method = "assemble", at = @At("HEAD"))
    private void csc$inheritPhysicalParent(CallbackInfoReturnable<Boolean> cir) {
        ((ElevatorLink) this).csc$setPhysicalChild(ChildAssemblyScope.physicalParent());
    }
}
