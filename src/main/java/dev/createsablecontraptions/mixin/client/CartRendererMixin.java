package dev.createsablecontraptions.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EntityRenderer.class,remap=false)
public abstract class CartRendererMixin {
    @Inject(method="getRenderOffset",at=@At("RETURN"),cancellable=true)
    private void csc$matchPhysicalInterpolation(Entity entity,float partialTick,CallbackInfoReturnable<Vec3> cir) {
        if(entity instanceof AbstractMinecart cart)
            cir.setReturnValue(cir.getReturnValue().add(dev.createsablecontraptions.client.CartRenderAlignment.offset(cart,partialTick)));
    }
}
