package dev.createsablecontraptions.mixin;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import dev.createsablecontraptions.oriented.CartMotion;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value=AbstractMinecart.class,remap=false)
public abstract class MinecartMotionMixin {
    @WrapMethod(method="moveMinecartOnRail")
    private void csc$railMovementFrame(net.minecraft.core.BlockPos pos,Operation<Void> original) {
        var cart=(AbstractMinecart)(Object)this;
        boolean entered=CartMotion.enterRailMove(cart);
        try { original.call(pos); } finally { CartMotion.leaveRailMove(cart,entered); }
    }
    @WrapMethod(method="tick")
    private void csc$checkedCartTick(Operation<Void> original) {
        var cart=(AbstractMinecart)(Object)this;
        var frame=CartMotion.begin(cart);
        try { original.call(); } finally { CartMotion.end(cart,frame); }
    }
}
