package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorPulleyBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import dev.createsablecontraptions.elevator.ElevatorCollisions;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.elevator.ElevatorPhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ElevatorPulleyBlockEntity.class, remap = false)
public abstract class ElevatorPulleyMixin {
    @Shadow private float prevSpeed;
    @Shadow private boolean arrived;
    @Shadow private int getTargetOffset() { throw new AssertionError(); }
    @Unique private float csc$acceptedSpeed;

    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void csc$stopBeforeObstacle(CallbackInfoReturnable<Float> cir) {
        var pulley = (ElevatorPulleyBlockEntity) (Object) this;
        AbstractContraptionEntity entity = pulley.movedContraption;
        if (entity == null || !ElevatorLink.managed(entity.getContraption())) return;
        if (!(pulley.getLevel() instanceof ServerLevel server)) {
            if (entity.isStalled()) { prevSpeed = 0; arrived = false; cir.setReturnValue(0f); }
            return;
        }
        ElevatorContraption elevator = (ElevatorContraption) entity.getContraption();
        float speed = cir.getReturnValue();
        float factor = (float) dev.createsablecontraptions.docking.DockingMotion.linear(entity, new Vec3(0, -speed, 0));
        speed *= factor;
        if (factor < .999f) { arrived = false; prevSpeed = speed; cir.setReturnValue(speed); }
        csc$acceptedSpeed = speed;
        double delta = arrived ? -(getTargetOffset() - pulley.offset) : -speed;
        boolean blocked = !dev.createsablecontraptions.elevator.ElevatorEditing.contactIntact(server, elevator)
                || ElevatorCollisions.blocked(server, elevator, entity.position(), new Vec3(0, delta, 0));
        if (blocked) dev.createsablecontraptions.elevator.ElevatorActors.probeBreakers(entity, new Vec3(0, delta, 0));
        csc$setStall(entity, blocked);
        if (entity.isStalled()) {
            prevSpeed = 0;
            csc$acceptedSpeed = 0;
            arrived = false;
            entity.setContraptionMotion(Vec3.ZERO);
            cir.setReturnValue(0f);
        }
    }

    @Inject(method = "moveAndCollideContraption", at = @At("HEAD"), cancellable = true)
    private void csc$useCheckedStep(CallbackInfoReturnable<Boolean> cir) {
        var pulley = (ElevatorPulleyBlockEntity) (Object) this;
        var entity = pulley.movedContraption;
        if (!(pulley.getLevel() instanceof ServerLevel) || entity == null || !ElevatorLink.managed(entity.getContraption())) return;
        // Create otherwise asks getMovementSpeed several times, accelerating between the sweep
        // and the move. Integrate exactly the step used for both the sweep and newOffset.
        Vec3 motion = arrived || entity.isStalled() ? Vec3.ZERO : new Vec3(0, -csc$acceptedSpeed, 0);
        entity.setContraptionMotion(motion);
        entity.move(motion.x, motion.y, motion.z);
        cir.setReturnValue(false); // A collision stalls; it never invokes Create's auto-disassembly path.
    }

    @Unique private void csc$setStall(AbstractContraptionEntity entity, boolean blocked) {
        ElevatorLink link = (ElevatorLink) entity.getContraption();
        boolean wasBlocked = link.csc$isBlocked();
        link.csc$setBlocked(blocked);
        boolean actorStall = entity.getContraption().getActors().stream().anyMatch(pair -> pair.right != null && pair.right.stall);
        entity.getContraption().stalled = blocked || actorStall;
        entity.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(), blocked || actorStall);
        if (blocked && !wasBlocked) ((ContraptionEntityAccessor) entity).csc$onStalled();
        if (blocked != wasBlocked) {
            var pulley = (ElevatorPulleyBlockEntity) (Object) this;
            pulley.setChanged();
            pulley.sendData();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void csc$followController(CallbackInfo ci) {
        var pulley = (ElevatorPulleyBlockEntity) (Object) this;
        if (!(pulley.getLevel() instanceof ServerLevel server) || pulley.movedContraption == null
                || !ElevatorLink.managed(pulley.movedContraption.getContraption())) return;
        var sub = ElevatorBridge.resolve(server, (ElevatorContraption) pulley.movedContraption.getContraption());
        if (sub != null) {
            var elevator = (ElevatorContraption) pulley.movedContraption.getContraption();
            pulley.offset = (float) (elevator.getInitialOffset() + elevator.anchor.getY() - pulley.movedContraption.getY());
            ElevatorPhysics.target(sub, pulley.movedContraption.position());
        }
        // Offset is progress, not elapsed time. Persist downward AND upward movement.
        pulley.setChanged();
    }

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void csc$preflightDisassembly(CallbackInfo ci) {
        var pulley = (ElevatorPulleyBlockEntity) (Object) this;
        if (!(pulley.getLevel() instanceof ServerLevel server) || pulley.movedContraption == null
                || !ElevatorLink.managed(pulley.movedContraption.getContraption())) return;
        var elevator = (ElevatorContraption) pulley.movedContraption.getContraption();
        Vec3 target = Vec3.atLowerCornerOf(elevator.anchor).add(0, elevator.getInitialOffset() - Math.round(pulley.offset), 0);
        if (!ElevatorBridge.canDisassemble(server, elevator, target)) {
            csc$setStall(pulley.movedContraption, true);
            ci.cancel();
        }
    }
}
