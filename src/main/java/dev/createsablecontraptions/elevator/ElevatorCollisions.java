package dev.createsablecontraptions.elevator;

import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.createsablecontraptions.physics.SweptBox;
import dev.createsablecontraptions.physics.CollisionState;
import dev.createsablecontraptions.physics.StructureStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.List;

public final class ElevatorCollisions {
    private static final int MAX_QUERY_BLOCKS = 32768;
    private ElevatorCollisions() {}

    public static boolean blocked(ServerLevel level, com.simibubi.create.content.contraptions.Contraption elevator, Vec3 anchor, Vec3 motion) {
        var entity=elevator.entity;
        CollisionState.begin(entity);
        var own = ElevatorBridge.resolve(level, elevator);
        if (own == null) return CollisionState.fail(entity,StructureStatus.UNAVAILABLE);
        if (!ElevatorEditing.valid(level, own, elevator)) return CollisionState.fail(entity,StructureStatus.UNAVAILABLE);
        if(!AssemblyFrame.available(own))return CollisionState.fail(entity,StructureStatus.UNAVAILABLE);
        var parent=AssemblyFrame.parent(own);
        var worldMotion=AssemblyFrame.direction(parent,motion);
        var data = ElevatorBridge.data(own);
        BlockPos plotAnchor = BlockPos.of(data.getLong("PlotAnchor"));
        List<SweptBox.Box> boxes = new ArrayList<>();
        List<BlockPos> locals = new ArrayList<>();
        AABB swept = null;
        for (long packed : data.getLongArray("Blocks")) {
            var position = BlockPos.of(packed);
            BlockPos plotPos = plotAnchor.offset(position);
            var state = level.getBlockState(plotPos);
            for (AABB local : state.getCollisionShape(level, plotPos).toAabbs()) {
                AABB box = local.move(position).move(anchor);
                var oriented=parent==null?aligned(box):oriented(box,parent.logicalPose());
                boxes.add(oriented);
                locals.add(position);
                AABB path = bounds(oriented).expandTowards(worldMotion);
                swept = swept == null ? path : swept.minmax(path);
            }
        }
        // Sublevel-mounted controllers are not Create passengers. Carry their real
        // bodies in the parent's sweep, rather than colliding with them as obstacles.
        var oldPivot = own.logicalPose().transformPosition(new Vector3d(plotAnchor.getX()+.5, plotAnchor.getY()+.5, plotAnchor.getZ()+.5));
        var targetPivot = AssemblyFrame.world(parent, anchor.add(.5,.5,.5));
        var targetRotation = AssemblyFrame.rotation(parent, new org.joml.Quaterniond());
        for (var child : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            if (child.isRemoved() || !AssemblyFrame.descendant(child, own)) continue;
            if (!ElevatorEditing.valid(level, child, null)) return CollisionState.fail(entity, StructureStatus.UNAVAILABLE);
            var childAnchor = BlockPos.of(ElevatorBridge.data(child).getLong("PlotAnchor"));
            for (long packed : ElevatorBridge.data(child).getLongArray("Blocks")) {
                var pos = childAnchor.offset(BlockPos.of(packed));
                for (var shape : level.getBlockState(pos).getCollisionShape(level,pos).toAabbs()) {
                    var box = dev.createsablecontraptions.physics.CarriedFrame.move(oriented(shape.move(pos),child.logicalPose()),
                            oldPivot, own.logicalPose().orientation(), new Vector3d(targetPivot.x,targetPivot.y,targetPivot.z), targetRotation);
                    boxes.add(box);
                    locals.add(null); // inherited movement; this is not an actor of the parent
                    var path = bounds(box).expandTowards(worldMotion);
                    swept = swept == null ? path : swept.minmax(path);
                }
            }
        }
        if (swept == null) return false;
        if (swept.minY < level.getMinBuildHeight() || swept.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(swept)) return CollisionState.fail(entity,StructureStatus.BOUNDARY);
        if (volume(swept) > MAX_QUERY_BLOCKS) return CollisionState.fail(entity,StructureStatus.BUDGET);
        SweptBox.V velocity = vector(worldMotion);
        boolean blocked = false;
        long comparisons = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(swept.minX, swept.minY, swept.minZ),
                BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) {
            if (!level.isLoaded(pos)) return CollisionState.fail(entity,StructureStatus.UNLOADED);
            for (AABB obstacle : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                SweptBox.Box stationary = aligned(obstacle.move(pos));
                for (int i = 0; i < boxes.size(); i++) {
                    if (++comparisons > 2_000_000) return CollisionState.fail(entity,StructureStatus.BUDGET);
                    var movingState = locals.get(i)==null ? null : level.getBlockState(plotAnchor.offset(locals.get(i)));
                    if (movingState != null && dev.createsablecontraptions.linear.PistonParts.cylinderSlot(elevator, pos, movingState)
                            && dev.createsablecontraptions.linear.PistonParts.cylinder(level.getBlockState(pos))) continue;
                    if (SweptBox.blocked(boxes.get(i), velocity, stationary)) {
                        blocked = true;
                        CollisionState.contact(entity,pos);
                        if (locals.get(i)!=null) dev.createsablecontraptions.linear.CollisionDrilling.offer(elevator, locals.get(i), pos, motion);
                    }
                }
            }
        }
        var container = SubLevelContainer.getContainer(level);
        for (SubLevel other : container.getAllSubLevels()) {
            if (other == own || other.isRemoved() || AssemblyFrame.descendant(other, own)) continue;
            if (elevator.entity != null && dev.createsablecontraptions.oriented.PhysicalFamily.related(elevator.entity, other.getUniqueId())) continue;
            var bounds = other.boundingBox();
            AABB global = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
            if (!swept.inflate(SweptBox.SKIN).intersects(global)) continue;
            Pose3dc pose = other.logicalPose();
            AABB localSearch = inverseBounds(swept, pose);
            var plotBounds = other.getPlot().getBoundingBox();
            localSearch = localSearch.intersect(new AABB(plotBounds.minX(), plotBounds.minY(), plotBounds.minZ(),
                    plotBounds.maxX() + 1, plotBounds.maxY() + 1, plotBounds.maxZ() + 1));
            if (volume(localSearch) > MAX_QUERY_BLOCKS) return CollisionState.fail(entity,StructureStatus.BUDGET);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(localSearch.minX, localSearch.minY, localSearch.minZ),
                    BlockPos.containing(localSearch.maxX, localSearch.maxY, localSearch.maxZ))) {
                if (!level.isLoaded(pos)) return CollisionState.fail(entity,StructureStatus.UNLOADED);
                for (AABB shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                    SweptBox.Box obstacle = oriented(shape.move(pos), pose);
                    for (int i = 0; i < boxes.size(); i++) {
                        var movingState=locals.get(i)==null ? null : level.getBlockState(plotAnchor.offset(locals.get(i)));
                        if(movingState != null && dev.createsablecontraptions.linear.PistonParts.cylinderSlot(elevator,pos,movingState)
                                && dev.createsablecontraptions.linear.PistonParts.cylinder(level.getBlockState(pos)))continue;
                        if (++comparisons > 2_000_000) return CollisionState.fail(entity,StructureStatus.BUDGET);
                        if (SweptBox.blocked(boxes.get(i), velocity, obstacle)) {
                            blocked = true;
                            CollisionState.contact(entity,pos);
                            if (locals.get(i)!=null) dev.createsablecontraptions.linear.CollisionDrilling.offer(elevator, locals.get(i), pos, motion);
                        }
                    }
                }
            }
        }
        var parentHit=CollisionState.get(entity);
        boolean children=dev.createsablecontraptions.oriented.ChildMotion.linear(entity, motion);
        if(blocked)CollisionState.restore(entity,parentHit);
        return blocked | children;
    }

    public static AABB bounds(SweptBox.Box b) {
        double x=Math.abs(b.x().x())*b.half().x()+Math.abs(b.y().x())*b.half().y()+Math.abs(b.z().x())*b.half().z();
        double y=Math.abs(b.x().y())*b.half().x()+Math.abs(b.y().y())*b.half().y()+Math.abs(b.z().y())*b.half().z();
        double z=Math.abs(b.x().z())*b.half().x()+Math.abs(b.y().z())*b.half().y()+Math.abs(b.z().z())*b.half().z();
        return new AABB(b.center().x()-x,b.center().y()-y,b.center().z()-z,b.center().x()+x,b.center().y()+y,b.center().z()+z);
    }
    private static long volume(AABB b) {
        return (long) Math.ceil(b.getXsize() + 2) * (long) Math.ceil(b.getYsize() + 2) * (long) Math.ceil(b.getZsize() + 2);
    }
    private static SweptBox.V vector(Vec3 v) { return new SweptBox.V(v.x, v.y, v.z); }
    private static SweptBox.V vector(Vector3d v) { return new SweptBox.V(v.x, v.y, v.z); }
    private static SweptBox.Box aligned(AABB b) {
        return SweptBox.Box.aligned(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
    public static SweptBox.Box oriented(AABB b, Pose3dc pose) {
        Vector3d center = new Vector3d((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, (b.minZ + b.maxZ) / 2);
        pose.transformPosition(center);
        var scale = pose.scale();
        return new SweptBox.Box(vector(center), new SweptBox.V(b.getXsize() * Math.abs(scale.x()) / 2,
                b.getYsize() * Math.abs(scale.y()) / 2, b.getZsize() * Math.abs(scale.z()) / 2),
                vector(pose.orientation().transform(new Vector3d(1, 0, 0))),
                vector(pose.orientation().transform(new Vector3d(0, 1, 0))),
                vector(pose.orientation().transform(new Vector3d(0, 0, 1))));
    }
    public static AABB inverseBounds(AABB b, Pose3dc pose) {
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (int i = 0; i < 8; i++) {
            Vector3d p = new Vector3d((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
            pose.transformPositionInverse(p);
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); maxZ = Math.max(maxZ, p.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
