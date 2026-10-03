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
        var data = ElevatorBridge.data(own);
        BlockPos plotAnchor = BlockPos.of(data.getLong("PlotAnchor"));
        List<AABB> boxes = new ArrayList<>();
        List<BlockPos> locals = new ArrayList<>();
        AABB swept = null;
        for (long packed : data.getLongArray("Blocks")) {
            var position = BlockPos.of(packed);
            BlockPos plotPos = plotAnchor.offset(position);
            var state = level.getBlockState(plotPos);
            for (AABB local : state.getCollisionShape(level, plotPos).toAabbs()) {
                AABB box = local.move(position).move(anchor);
                boxes.add(box);
                locals.add(position);
                AABB path = box.expandTowards(motion);
                swept = swept == null ? path : swept.minmax(path);
            }
        }
        if (swept == null) return false;
        if (swept.minY < level.getMinBuildHeight() || swept.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(swept)) return CollisionState.fail(entity,StructureStatus.BOUNDARY);
        if (volume(swept) > MAX_QUERY_BLOCKS) return CollisionState.fail(entity,StructureStatus.BUDGET);
        SweptBox.V velocity = vector(motion);
        boolean blocked = false;
        long comparisons = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(swept.minX, swept.minY, swept.minZ),
                BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) {
            if (!level.isLoaded(pos)) return CollisionState.fail(entity,StructureStatus.UNLOADED);
            for (AABB obstacle : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                SweptBox.Box stationary = aligned(obstacle.move(pos));
                for (int i = 0; i < boxes.size(); i++) {
                    if (++comparisons > 2_000_000) return CollisionState.fail(entity,StructureStatus.BUDGET);
                    var movingState = level.getBlockState(plotAnchor.offset(locals.get(i)));
                    if (dev.createsablecontraptions.linear.PistonParts.cylinderSlot(elevator, pos, movingState)
                            && dev.createsablecontraptions.linear.PistonParts.cylinder(level.getBlockState(pos))) continue;
                    if (SweptBox.blocked(aligned(boxes.get(i)), velocity, stationary)) {
                        blocked = true;
                        CollisionState.contact(entity,pos);
                        dev.createsablecontraptions.linear.CollisionDrilling.offer(elevator, locals.get(i), pos, motion);
                    }
                }
            }
        }
        var container = SubLevelContainer.getContainer(level);
        for (SubLevel other : container.getAllSubLevels()) {
            if (other == own || other.isRemoved()) continue;
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
                        if (++comparisons > 2_000_000) return CollisionState.fail(entity,StructureStatus.BUDGET);
                        if (SweptBox.blocked(aligned(boxes.get(i)), velocity, obstacle)) {
                            blocked = true;
                            CollisionState.contact(entity,pos);
                            dev.createsablecontraptions.linear.CollisionDrilling.offer(elevator, locals.get(i), pos, motion);
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
