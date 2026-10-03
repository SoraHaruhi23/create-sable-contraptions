package dev.createsablecontraptions.mixin.client;

import dev.createsablecontraptions.client.ElevatorActorClient;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.engine_room.flywheel.lib.visualization.VisualizationHelper", remap = false)
public abstract class ActorVisualizerMixin {
    @Inject(method = "getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Ldev/engine_room/flywheel/api/visualization/BlockEntityVisualizer;",
            at = @At("HEAD"), cancellable = true)
    private static void csc$movingVisualOwns(BlockEntity be, CallbackInfoReturnable<?> cir) {
        if (ElevatorActorClient.movingRendererOwns(be)) cir.setReturnValue(null);
    }
}
