package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The logical Create entity lives outside its Sable plot: upstream cannot infer its owner. */
@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class BlockBreakingMovementMixin {
    @Unique
    private static boolean csc$ownsTarget(MovementContext context, BlockPos target) {
        if (!ElevatorLink.managed(context.contraption)) return false;
        var sub = Sable.HELPER.getContaining(context.world, target);
        return sub != null && sub.getUniqueId().equals(((ElevatorLink) context.contraption).csc$getSubLevel());
    }

    @Inject(method = "visitNewPosition", at = @At("HEAD"), cancellable = true)
    private void csc$excludeOwnPlot(MovementContext context, BlockPos target, CallbackInfo ci) {
        // Also catches the second original.call made by Sable's cross-sublevel search.
        if (csc$ownsTarget(context, target) || !dev.createsablecontraptions.linear.CollisionDrilling.allowedTarget(context, target)) ci.cancel();
    }

    @Inject(method = "tickBreaker", at = @At("HEAD"), cancellable = true)
    private void csc$discardSavedSelfTarget(MovementContext context, CallbackInfo ci) {
        var target = NbtUtils.readBlockPos(context.data, "BreakingPos").orElse(null);
        if (target == null || !csc$ownsTarget(context, target)
                && dev.createsablecontraptions.linear.CollisionDrilling.allowedTarget(context, target)) return;
        ((BlockBreakingMovementBehaviour) (Object) this).cancelStall(context);
        context.data.remove("WaitingTicks");
        context.data.remove("LastPos");
        context.data.remove("ProjectedPos");
        context.data.remove(dev.createsablecontraptions.linear.CollisionDrilling.ACTIVE);
        context.stall = false;
        ci.cancel();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void csc$protectPlatform(MovementContext context, BlockPos target, CallbackInfo ci) {
        if (csc$ownsTarget(context, target) || !dev.createsablecontraptions.linear.CollisionDrilling.allowedTarget(context, target)) ci.cancel();
    }
}
