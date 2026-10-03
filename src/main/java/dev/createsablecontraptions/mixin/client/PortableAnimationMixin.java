package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PortableStorageInterfaceMovement.class, remap = false)
public abstract class PortableAnimationMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void csc$physicalAnimation(MovementContext context, CallbackInfo ci) {
        if (dev.createsablecontraptions.client.PortableAnimation.tick(context)) ci.cancel();
    }
}
