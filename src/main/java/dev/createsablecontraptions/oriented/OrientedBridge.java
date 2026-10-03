package dev.createsablecontraptions.oriented;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.*;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.physics.RotatingFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class OrientedBridge {
    private OrientedBridge() {}
    public static boolean virtualAnchor(Contraption c, BlockPos local) {
        var info = c.getBlocks().get(local);
        return c instanceof com.simibubi.create.content.contraptions.mounted.MountedContraption
                && info != null && AllBlocks.MINECART_ANCHOR.has(info.state());
    }
    public static void target(OrientedContraptionEntity e) {
        if (!(e.level() instanceof ServerLevel level) || !e.isAlive() || !ElevatorLink.managed(e.getContraption())) return;
        var sub = ElevatorBridge.resolve(level, e.getContraption());
        if (sub == null) return;
        var x = e.applyRotation(new Vec3(1, 0, 0), 1);
        var y = e.applyRotation(new Vec3(0, 1, 0), 1);
        var z = e.applyRotation(new Vec3(0, 0, 1), 1);
        var q = RotatingFrame.fromBasis(new Vector3d(x.x, x.y, x.z), new Vector3d(y.x, y.y, y.z), new Vector3d(z.x, z.y, z.z));
        var data = ElevatorBridge.data(sub);
        data.putDouble("QX", q.x); data.putDouble("QY", q.y); data.putDouble("QZ", q.z); data.putDouble("QW", q.w);
        // Riding entities may tick before their parent controller. Derive the attachment
        // from the parent's CURRENT transform, rather than last tick's passenger position.
        Vec3 anchor = e.getAnchorVec();
        if (e.getVehicle() instanceof AbstractContraptionEntity parent && ElevatorLink.managed(parent.getContraption())) {
            var bearing = parent.getContraption().getBearingPosOf(e.getUUID());
            if (bearing != null) anchor = parent.toGlobalVector(Vec3.atCenterOf(bearing), 1).subtract(.5, .5, .5);
        }
        ElevatorPhysics.target(sub, anchor);
        children(e);
    }
    public static void children(AbstractContraptionEntity parent) {
        for (var passenger : parent.getPassengers())
            if (passenger instanceof OrientedContraptionEntity child && ElevatorLink.managed(child.getContraption())) {
                parent.positionRider(child);
                target(child);
            }
    }
    public static void bounds(AbstractContraptionEntity entity) {
        var sub=ElevatorActors.sub(entity);
        if(sub==null || sub.isRemoved())return;
        var b=sub.boundingBox();
        entity.setBoundingBox(new net.minecraft.world.phys.AABB(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()));
        for(var p:entity.getPassengers())if(p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption()))bounds(child);
    }
}
