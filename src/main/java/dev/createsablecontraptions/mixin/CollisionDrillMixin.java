package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.createsablecontraptions.linear.CollisionDrilling;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = DrillMovementBehaviour.class, remap = false)
public abstract class CollisionDrillMixin {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void csc$allContactDirections(MovementContext ctx, CallbackInfoReturnable<Boolean> cir) {
        if (ElevatorLink.managed(ctx.contraption) && ctx.data.getBoolean(CollisionDrilling.ACTIVE)
                && ctx.data.contains("BreakingPos")) cir.setReturnValue(!ctx.disabled);
    }
}
