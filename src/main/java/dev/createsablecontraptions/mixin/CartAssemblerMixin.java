package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.*;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.oriented.PhysicalFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CartAssemblerBlockEntity.class, remap = false)
public abstract class CartAssemblerMixin {
    @org.spongepowered.asm.mixin.Shadow protected com.simibubi.create.content.contraptions.AssemblyException lastException;

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "assemble")
    private void csc$reportTransferFailure(Level world, BlockPos pos, AbstractMinecart cart,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var previousPosition = cart.position();
        try {
            original.call(world, pos, cart);
        } catch (Exception failure) {
            if (!(failure instanceof com.simibubi.create.content.contraptions.AssemblyException assembly)) throw failure;
            // Create only catches exceptions from the scan, but our physical transfer can also fail.
            // A cart with an existing contraption coupling exits before assembly, so any such link
            // here was just created by this failed attempt. Leave ordinary coupling chains intact.
            var controller = cart.getData(com.simibubi.create.AllAttachmentTypes.MINECART_CONTROLLER);
            for (boolean forward : new boolean[]{true, false}) if (controller.hasContraptionCoupling(forward)) {
                var other = com.simibubi.create.content.contraptions.minecart.CouplingHandler.getNextInCouplingChain(world, controller, forward);
                controller.removeConnection(forward);
                if (other != null) other.removeConnection(!forward);
            }
            cart.setPos(previousPosition);
            lastException = assembly;
            ((CartAssemblerBlockEntity) (Object) this).sendData();
        }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="disassemble",
            at=@At(value="INVOKE",target="Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object csc$findPhysicalPassenger(java.util.List<?> passengers,int index,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Object> original) {
        if(index==0)for(var passenger:passengers)
            if(passenger instanceof OrientedContraptionEntity e
                    && e.getContraption() instanceof MountedContraption && ElevatorLink.managed(e.getContraption()))return e;
        return original.call(passengers,index);
    }
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$checkBeforeUncoupling(Level level, BlockPos pos, AbstractMinecart cart, CallbackInfo ci) {
        if (level.isClientSide) return;
        for (var passenger : cart.getPassengers()) {
            if (!(passenger instanceof OrientedContraptionEntity e) || !ElevatorLink.managed(e.getContraption())) continue;
            float yaw = e.yaw;
            try {
                if (e.getCouplingId() == null) e.yaw = CartAssemblerBlock.getHorizontalDirection(
                        ((CartAssemblerBlockEntity) (Object) this).getBlockState()).toYRot();
                if (!PhysicalFamily.ready(e, ((ContraptionEntityAccessor) e).csc$structureTransform(), new java.util.HashSet<>())) ci.cancel();
            } finally { e.yaw = yaw; }
        }
    }
}
