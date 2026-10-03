package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import dev.createsablecontraptions.elevator.PhysicalStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MovementContext.class, remap = false)
public abstract class MovementContextMixin {
    @Inject(method = "getItemStorage", at = @At("HEAD"), cancellable = true)
    private void csc$liveItemStorage(CallbackInfoReturnable<MountedItemStorage> cir) {
        var ctx = (MovementContext) (Object) this;
        if (ctx.contraption.getStorage() instanceof PhysicalStorage storage) cir.setReturnValue(storage.itemAt(ctx.localPos));
    }
    @Inject(method = "getFluidStorage", at = @At("HEAD"), cancellable = true)
    private void csc$liveFluidStorage(CallbackInfoReturnable<MountedFluidStorage> cir) {
        var ctx = (MovementContext) (Object) this;
        if (ctx.contraption.getStorage() instanceof PhysicalStorage storage) cir.setReturnValue(storage.fluidAt(ctx.localPos));
    }
}
