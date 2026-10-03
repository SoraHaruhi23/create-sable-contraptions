package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.linear.LinearMotion;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = GantryContraptionEntity.class, remap = false)
public abstract class GantryEntityMixin {
    @Inject(method = "checkPinionShaft", at = @At("RETURN"))
    private void csc$checkStep(CallbackInfo ci) {
        var entity = (GantryContraptionEntity) (Object) this;
        if (entity.isAlive() && entity.level() instanceof ServerLevel && ElevatorLink.managed(entity.getContraption()))
        {
            entity.setContraptionMotion(entity.getDeltaMovement().scale(dev.createsablecontraptions.docking.DockingMotion.linear(entity, entity.getDeltaMovement())));
            LinearMotion.check(entity, entity.getDeltaMovement());
        }
    }
    @Inject(method = "tickContraption", at = @At("RETURN"))
    private void csc$follow(CallbackInfo ci) { LinearMotion.target((GantryContraptionEntity) (Object) this); }
    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflight(CallbackInfo ci) {
        var entity = (GantryContraptionEntity) (Object) this;
        if (entity.level() instanceof ServerLevel level && ElevatorLink.managed(entity.getContraption())
                && !dev.createsablecontraptions.bearing.BearingBridge.canDisassemble(level, entity.getContraption(),
                ((ContraptionEntityAccessor) entity).csc$structureTransform(), new java.util.HashSet<>())) ci.cancel();
    }
}
