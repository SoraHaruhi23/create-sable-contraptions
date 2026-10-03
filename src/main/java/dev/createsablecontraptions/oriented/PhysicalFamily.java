package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.*;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.mixin.ContraptionAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import java.util.Set;

public final class PhysicalFamily {
    private PhysicalFamily() {}
    public static boolean actorStalled(AbstractContraptionEntity entity) {
        if (entity.getContraption().getActors().stream().anyMatch(a -> a.right != null && a.right.stall)) return true;
        for (var p : entity.getPassengers())
            if (p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption()) && actorStalled(child)) return true;
        return false;
    }
    public static boolean ready(AbstractContraptionEntity entity, StructureTransform transform, Set<BlockPos> destinations) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (!dev.createsablecontraptions.bearing.BearingBridge.canDisassemble(level, entity.getContraption(), transform, destinations)) return false;
        for (var passenger : entity.getPassengers()) {
            if (!(passenger instanceof OrientedContraptionEntity child) || !ElevatorLink.managed(child.getContraption())) continue;
            var face = ((ContraptionAccessor) entity.getContraption()).csc$children().get(child.getUUID());
            if (face == null) continue;
            var childTransform = new StructureTransform(transform.apply(face.getConnectedPos()), 0, -child.yaw + child.getInitialYaw(), 0);
            if (!ready(child, childTransform, destinations)) return false;
        }
        return true;
    }
    public static boolean related(AbstractContraptionEntity entity, java.util.UUID subId) {
        var root = entity;
        while (root.getVehicle() instanceof AbstractContraptionEntity parent && ElevatorLink.managed(parent.getContraption())) root = parent;
        return contains(root, subId);
    }
    private static boolean contains(AbstractContraptionEntity e, java.util.UUID id) {
        if (ElevatorLink.managed(e.getContraption()) && id.equals(((ElevatorLink) e.getContraption()).csc$getSubLevel())) return true;
        for (var p : e.getPassengers()) if (p instanceof AbstractContraptionEntity c && contains(c, id)) return true;
        return false;
    }
}
