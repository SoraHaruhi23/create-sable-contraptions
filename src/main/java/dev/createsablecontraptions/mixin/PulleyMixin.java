package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import dev.createsablecontraptions.linear.LinearActuatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = PulleyBlockEntity.class, remap = false)
public abstract class PulleyMixin {
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflight(CallbackInfo ci) {
        if (!((LinearActuatorLink) this).csc$readyToDisassemble(true)) ci.cancel();
    }
}
