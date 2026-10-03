package dev.createsablecontraptions.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.createsablecontraptions.elevator.ElevatorEditing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelPlot.class, remap = false)
public abstract class LevelPlotMixin {
    @Inject(method = "getBlockEntityActors", at = @At("RETURN"), cancellable = true)
    private void csc$noDuplicateSableWork(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Iterable<dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor>> cir) {
        var plot = (LevelPlot) (Object) this;
        if (!(plot.getSubLevel() instanceof ServerSubLevel server)
                || !dev.createsablecontraptions.elevator.ElevatorBridge.managed(server)) return;
        var filtered = new java.util.ArrayList<dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor>();
        for (var actor : cir.getReturnValue())
            if (!(actor instanceof net.minecraft.world.level.block.entity.BlockEntity be)
                    || com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.get(be.getBlockState()) == null) filtered.add(actor);
        cir.setReturnValue(filtered);
    }
    @Inject(method = "onBlockChange", at = @At("TAIL"))
    private void csc$trackLiveBlocks(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (((LevelPlot) (Object) this).getSubLevel() instanceof ServerSubLevel sub)
            ElevatorEditing.changed(sub, pos, state);
    }
}
