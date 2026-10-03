package dev.createsablecontraptions.linear;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.mixin.ContraptionEntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class LinearMotion {
    private LinearMotion() {}
    public static boolean check(AbstractContraptionEntity entity, Vec3 motion) {
        if (!(entity.level() instanceof ServerLevel level)) return entity.isStalled();
        boolean blocked = ElevatorCollisions.blocked(level, entity.getContraption(), entity.position(), motion);
        if (blocked) ElevatorActors.probeBreakers(entity, motion);
        var link = (ElevatorLink) entity.getContraption();
        boolean previous = link.csc$isBlocked();
        link.csc$setBlocked(blocked);
        boolean stalled = blocked || dev.createsablecontraptions.oriented.PhysicalFamily.actorStalled(entity);
        entity.getContraption().stalled = stalled;
        entity.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(), stalled);
        if (previous != blocked) ((ContraptionEntityAccessor) entity).csc$onStalled();
        return stalled;
    }
    public static void target(AbstractContraptionEntity entity) {
        if (entity == null || !entity.isAlive() || !(entity.level() instanceof ServerLevel level)
                || !ElevatorLink.managed(entity.getContraption()) || !ElevatorLink.linear(entity.getContraption())) return;
        var sub = ElevatorBridge.resolve(level, entity.getContraption());
        if (sub != null) ElevatorPhysics.target(sub, entity.position());
    }
}
