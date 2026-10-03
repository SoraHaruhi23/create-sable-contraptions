package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionMixin {
    @Inject(method = "addBlock", at = @At("HEAD"))
    private void csc$doNotSnapshotInventories(CallbackInfo ci) {
        var self = (Contraption) (Object) this;
        if (ElevatorLink.supported(self) && !(self.getStorage() instanceof dev.createsablecontraptions.elevator.PendingPhysicalStorage)
                && !ElevatorLink.managed(self))
            ((ContraptionAccessor) self).csc$setStorage(new dev.createsablecontraptions.elevator.PendingPhysicalStorage());
    }

    @Inject(method = "writeStorage", at = @At("HEAD"), cancellable = true)
    private void csc$saveOnlyRealInventories(CallbackInfo ci) {
        if (ElevatorLink.managed((Contraption) (Object) this)) ci.cancel();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void csc$beforeActorsStop(CallbackInfo ci) {
        var self = (Contraption) (Object) this;
        if (ElevatorLink.managed(self) && self.entity != null) dev.createsablecontraptions.elevator.ElevatorActors.checkpoint(self.entity);
    }

    @Inject(method = "stop", at = @At("RETURN"))
    private void csc$afterActorsStop(CallbackInfo ci) {
        var self = (Contraption) (Object) this;
        if (ElevatorLink.managed(self) && self.entity != null) dev.createsablecontraptions.elevator.ElevatorActors.commit(self.entity);
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"), cancellable = true)
    private void csc$assemble(Level level, BlockPos offset, CallbackInfo ci) throws AssemblyException {
        var elevator = (Contraption) (Object) this;
        if (ElevatorLink.supported(elevator) && level instanceof ServerLevel server) {
            try {
                ElevatorBridge.assemble(server, elevator, offset);
            } catch (RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger("create_sable_contraptions").error("Elevator assembly failed", failure);
                throw new AssemblyException(net.minecraft.network.chat.Component.translatable("csc.assembly.failed"));
            }
            ci.cancel();
        }
    }

    @Inject(method = "addBlocksToWorld", at = @At("HEAD"), cancellable = true)
    private void csc$disassemble(Level level, StructureTransform transform, CallbackInfo ci) {
        Contraption self = (Contraption) (Object) this;
        if (ElevatorLink.managed(self) && level instanceof ServerLevel server) {
            if (self instanceof ElevatorContraption elevator) ElevatorBridge.disassemble(server, elevator, transform);
            else dev.createsablecontraptions.bearing.BearingBridge.disassemble(server, self, transform);
            ci.cancel();
        }
    }
}
