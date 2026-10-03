package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class ContraptionEntityMixin {
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflightDirectBearingDisassembly(CallbackInfo ci) {
        var entity = (AbstractContraptionEntity) (Object) this;
        if (dev.createsablecontraptions.oriented.CartLifecycle.releasing(entity)) { ci.cancel(); return; }
        if (dev.createsablecontraptions.oriented.PackedStructures.suppressDisassembly(entity)) { ci.cancel(); return; }
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel server)
                || !ElevatorLink.managed(entity.getContraption())
                || entity.getContraption() instanceof com.simibubi.create.content.contraptions.elevator.ElevatorContraption) return;
        if (!dev.createsablecontraptions.oriented.PhysicalFamily.ready(entity,
                ((ContraptionEntityAccessor) entity).csc$structureTransform(), new java.util.HashSet<>())) ci.cancel();
    }
    @Inject(method = "tickActors", at = @At("HEAD"), cancellable = true)
    private void csc$preparePhysicalActors(CallbackInfo ci) {
        var entity = (AbstractContraptionEntity) (Object) this;
        if (ElevatorLink.managed(entity.getContraption())
                && !dev.createsablecontraptions.elevator.ElevatorActors.prepare(entity)) ci.cancel();
    }

    @Inject(method = "setBlock", at = @At("HEAD"))
    private void csc$mirrorActorState(net.minecraft.core.BlockPos local,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo info, CallbackInfo ci) {
        var entity = (AbstractContraptionEntity) (Object) this;
        if (!ElevatorLink.managed(entity.getContraption()) || entity.level().isClientSide) return;
        var sub = dev.createsablecontraptions.elevator.ElevatorActors.sub(entity);
        if (sub != null) {
            var state = info.state();
            if (state.hasProperty(com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock.VISIBLE)
                    && state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN))
                state = state.setValue(com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock.VISIBLE, false);
            entity.level().setBlock(sub.getPlot().getCenterBlock().offset(local), state,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
        }
    }
    @Inject(method = "collisionEnabled", at = @At("HEAD"), cancellable = true)
    private void csc$singleCollisionOwner(CallbackInfoReturnable<Boolean> cir) {
        if (ElevatorLink.managed(((AbstractContraptionEntity) (Object) this).getContraption())) cir.setReturnValue(false);
    }

    @Inject(method = "tickActors", at = @At("RETURN"))
    private void csc$retainCollisionStall(CallbackInfo ci) {
        AbstractContraptionEntity entity = (AbstractContraptionEntity) (Object) this;
        if (ElevatorLink.managed(entity.getContraption())) dev.createsablecontraptions.elevator.ElevatorActors.commit(entity);
        if (!entity.level().isClientSide && ElevatorLink.managed(entity.getContraption())
                && ((ElevatorLink) entity.getContraption()).csc$isBlocked()) {
            entity.getContraption().stalled = true;
            entity.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(), true);
        }
    }
}
