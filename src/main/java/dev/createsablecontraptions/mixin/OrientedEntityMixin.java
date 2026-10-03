package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.createsablecontraptions.oriented.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OrientedContraptionEntity.class, remap = false)
public abstract class OrientedEntityMixin {
    @Inject(method="updateOrientation",at=@At("RETURN"),cancellable=true)
    private void csc$turnSweep(boolean rotationLock,boolean wasStalled,net.minecraft.world.entity.Entity riding,
            boolean coupled,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        // The rejected turn did not rotate. Let stalled actors continue working,
        // including drills when the cart is configured to pause while rotating.
        if (CartTurning.check((OrientedContraptionEntity)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method = "getVehicleAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void csc$gridAlignedCartAttachment(net.minecraft.world.entity.Entity vehicle,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.phys.Vec3> cir) {
        var e = (OrientedContraptionEntity) (Object) this;
        if (vehicle instanceof net.minecraft.world.entity.vehicle.AbstractMinecart
                && e.getContraption() instanceof com.simibubi.create.content.contraptions.mounted.MountedContraption
                && (dev.createsablecontraptions.elevator.ElevatorLink.managed(e.getContraption())
                    || e.getContraption() instanceof PackedCarrier packed && packed.csc$packed()!=null)) {
            var previous = cir.getReturnValue();
            cir.setReturnValue(new net.minecraft.world.phys.Vec3(previous.x,
                    dev.createsablecontraptions.physics.CartMount.ATTACHMENT_Y, previous.z));
        }
    }
    @Inject(method = "tickContraption", at = @At("RETURN"))
    private void csc$follow(CallbackInfo ci) {
        var entity = (OrientedContraptionEntity) (Object) this;
        CartLifecycle.tick(entity);
        OrientedBridge.target(entity);
    }
    @Inject(method = "stopRiding", at = @At("HEAD"), cancellable = true)
    private void csc$keepAttachmentIfPlacementBlocked(CallbackInfo ci) {
        var e = (OrientedContraptionEntity) (Object) this;
        if (CartLifecycle.releasing(e)) return;
        if (PackedStructures.suppressDisassembly(e)) return;
        if (!e.level().isClientSide && e.isAlive() && e.getVehicle() != null && e.getVehicle().isRemoved()
                && dev.createsablecontraptions.elevator.ElevatorLink.managed(e.getContraption())) {
            CartLifecycle.release(e);
            return;
        }
        if (e.isAlive() && !e.level().isClientSide && dev.createsablecontraptions.elevator.ElevatorLink.managed(e.getContraption())
                && !PhysicalFamily.ready(e, ((ContraptionEntityAccessor) e).csc$structureTransform(), new java.util.HashSet<>())) ci.cancel();
    }
}
