package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.AssemblyException;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

/** Shared save link; activation remains restricted to explicitly supported contraption types. */
@Mixin(value = Contraption.class, remap = false)
public abstract class PhysicalContraptionMixin implements ElevatorLink, dev.createsablecontraptions.oriented.PackedCarrier {
    @Unique private CompoundTag csc$packedData;
    public CompoundTag csc$packed() { return csc$packedData; }
    public void csc$packed(CompoundTag tag) { csc$packedData = tag; }
    @Unique private UUID csc$subLevel;
    @Unique private boolean csc$blocked;
    @Unique private boolean csc$physicalChild;
    public boolean csc$isPhysicalChild() { return csc$physicalChild; }
    public void csc$setPhysicalChild(boolean value) { csc$physicalChild = value; }
    public UUID csc$getSubLevel() { return csc$subLevel; }
    public void csc$setSubLevel(UUID id) { csc$subLevel = id; }
    public boolean csc$isBlocked() { return csc$blocked; }
    public void csc$setBlocked(boolean blocked) { csc$blocked = blocked; }

    @Inject(method = "searchMovedStructure", at = @At("HEAD"))
    private void csc$worldAnchorOnly(Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) throws AssemblyException {
        if (ElevatorLink.supported((Contraption) (Object) this) && Sable.HELPER.getContaining(level, pos) != null)
            throw new AssemblyException(net.minecraft.network.chat.Component.translatable("csc.assembly.nested"));
    }

    @Inject(method = "writeNBT", at = @At("RETURN"))
    private void csc$write(HolderLookup.Provider registries, boolean spawn, CallbackInfoReturnable<CompoundTag> cir) {
        if (csc$subLevel != null && ElevatorLink.supported((Contraption) (Object) this)) {
            cir.getReturnValue().putUUID("CSCSubLevel", csc$subLevel);
            cir.getReturnValue().putBoolean("CSCBlocked", csc$blocked);
            cir.getReturnValue().putBoolean("CSCPhysicalChild", csc$physicalChild);
        }
    }

    @Inject(method = "readNBT", at = @At("TAIL"))
    private void csc$read(Level level, CompoundTag tag, boolean spawn, CallbackInfo ci) {
        csc$physicalChild = tag.getBoolean("CSCPhysicalChild");
        if (!ElevatorLink.supported((Contraption) (Object) this)) return;
        csc$packedData = tag.contains(dev.createsablecontraptions.oriented.PackedStructures.KEY)
                // Borrow immutable packed input. Restore copies only the individual mutable payloads.
                ? tag.getCompound(dev.createsablecontraptions.oriented.PackedStructures.KEY) : null;
        csc$subLevel = tag.hasUUID("CSCSubLevel") ? tag.getUUID("CSCSubLevel") : null;
        csc$blocked = tag.getBoolean("CSCBlocked");
        if (csc$subLevel != null && level instanceof net.minecraft.server.level.ServerLevel server) {
            var sub = ElevatorBridge.resolve(server, (Contraption) (Object) this);
            if (sub != null) ((ContraptionAccessor) this).csc$setStorage(new dev.createsablecontraptions.elevator.PhysicalStorage(sub));
        }
    }
}
