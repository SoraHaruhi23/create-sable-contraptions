package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionControlsMovement.class, remap = false)
public abstract class ContraptionControlsMovementMixin {
    @Inject(method = "renderInContraption", at = @At("HEAD"), cancellable = true)
    private void csc$physicalDisplayOwnsRendering(MovementContext ctx, VirtualRenderWorld world,
            ContraptionMatrices matrices, MultiBufferSource buffers, CallbackInfo ci) {
        if (ElevatorLink.managed(ctx.contraption)) ci.cancel();
    }
}
