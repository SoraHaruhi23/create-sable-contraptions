package dev.createsablecontraptions.mixin;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Entity.class,remap=false)
public abstract class CartMoveMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void csc$wholeStructureSweep(MoverType type,Vec3 motion,CallbackInfo ci) {
        if((Object)this instanceof AbstractMinecart cart && dev.createsablecontraptions.oriented.CartMotion.reject(cart,motion)) ci.cancel();
    }
}
