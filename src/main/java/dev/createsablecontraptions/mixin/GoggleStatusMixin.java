package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.oriented.GoggleStatus;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AbstractContraptionEntity.class,remap=false)
public abstract class GoggleStatusMixin implements GoggleStatus {
    @Unique private static final EntityDataAccessor<Integer> CSC_STATUS=SynchedEntityData.defineId(AbstractContraptionEntity.class,EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<java.util.Optional<net.minecraft.core.BlockPos>> CSC_TARGET=SynchedEntityData.defineId(AbstractContraptionEntity.class,EntityDataSerializers.OPTIONAL_BLOCK_POS);
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void csc$defineStatus(SynchedEntityData.Builder builder,CallbackInfo ci) { builder.define(CSC_STATUS,0); builder.define(CSC_TARGET,java.util.Optional.empty()); }
    @Inject(method="tick",at=@At("TAIL"))
    private void csc$updateStatus(CallbackInfo ci) {
        var e=(AbstractContraptionEntity)(Object)this;
        if(!e.level().isClientSide && ElevatorLink.managed(e.getContraption())) {
            int status=GoggleStatus.compute(e);
            e.getEntityData().set(CSC_STATUS,status);
            var root=GoggleStatus.root(e);
            var hit=dev.createsablecontraptions.physics.CollisionState.get(root);
            e.getEntityData().set(CSC_TARGET,status==dev.createsablecontraptions.physics.StructureStatus.COLLISION && hit!=null
                    ? java.util.Optional.ofNullable(hit.target()):java.util.Optional.empty());
        }
    }
    public int csc$status() { return ((AbstractContraptionEntity)(Object)this).getEntityData().get(CSC_STATUS); }
    public java.util.Optional<net.minecraft.core.BlockPos> csc$collisionTarget() { return ((AbstractContraptionEntity)(Object)this).getEntityData().get(CSC_TARGET); }
}
