package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.createsablecontraptions.linear.CollisionDrilling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public abstract class CollisionDrillProgressMixin {
    @Inject(method = "tickBreaker", at = @At("HEAD"), cancellable = true)
    private void csc$releaseDistantTarget(MovementContext ctx, CallbackInfo ci) {
        if (!ctx.world.isClientSide && dev.createsablecontraptions.elevator.ElevatorLink.managed(ctx.contraption)
                && ctx.data.getBoolean(CollisionDrilling.ACTIVE) && !CollisionDrilling.targetInReach(ctx)) {
            ((BlockBreakingMovementBehaviour) (Object) this).cancelStall(ctx);
            ctx.data.remove("WaitingTicks");
            ctx.data.remove("LastPos");
            ctx.data.remove("ProjectedPos");
            ctx.data.remove(CollisionDrilling.ACTIVE);
            ctx.stall = false;
            ci.cancel();
        }
    }
    @Inject(method = {"tickBreaker", "cancelStall"}, at = @At("RETURN"))
    private void csc$clearContactMode(MovementContext ctx, CallbackInfo ci) {
        if (!ctx.data.contains("BreakingPos")) ctx.data.remove(CollisionDrilling.ACTIVE);
    }
}
