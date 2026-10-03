package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ElevatorPulleyBlockEntity.class, remap = false)
public abstract class ElevatorEffectsMixin {
    @Unique private boolean csc$wasBlocked;
    @Inject(method = "tick", at = @At("RETURN"))
    private void csc$createStopEffect(CallbackInfo ci) {
        var pulley = (ElevatorPulleyBlockEntity) (Object) this;
        if (pulley.getLevel() == null || !pulley.getLevel().isClientSide) return;
        var entity = pulley.movedContraption;
        boolean blocked = entity != null && ElevatorLink.managed(entity.getContraption()) && entity.isStalled();
        if (blocked != csc$wasBlocked) {
            // Reuse Create's own smoke/cloud effect without marking the kinetic network overstressed.
            new KineticEffectHandler(pulley).spawnEffect(blocked ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                    blocked ? .2f : .075f, blocked ? 5 : 2);
            csc$wasBlocked = blocked;
        }
    }
}
