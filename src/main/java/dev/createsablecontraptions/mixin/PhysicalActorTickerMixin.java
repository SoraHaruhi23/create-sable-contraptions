package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.AllBlocks;
import dev.createsablecontraptions.elevator.ElevatorActors;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity", remap = false)
public abstract class PhysicalActorTickerMixin {
    @Shadow @Final private BlockEntity blockEntity;
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void csc$oneWorkExecutor(CallbackInfo ci) {
        // Doors animate physically; Sable's contact BE powers the REAL moving contact
        // and notifies adjacent plot wiring. Create's actor only handles the visited contact.
        // Tank movement behaviour only animates the client proxy. The real tank must
        // maintain connectivity/capabilities and flush its queued fluid synchronization.
        if (!(blockEntity instanceof SlidingDoorBlockEntity)
                && !(blockEntity instanceof FluidTankBlockEntity)
                && !AllBlocks.REDSTONE_CONTACT.has(blockEntity.getBlockState())
                && ElevatorActors.physicalActor(blockEntity)) ci.cancel();
    }
}
