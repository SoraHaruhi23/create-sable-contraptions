package dev.createsablecontraptions.bearing;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.physics.*;
import dev.createsablecontraptions.mixin.ContraptionEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import java.util.ArrayList;

/** The whole requested rotation is checked against world blocks and other physical structures. */
public final class BearingMotion {
    private record Arc(BlockPos block, SweptBox.Box local, java.util.List<SweptBox.Box> envelopes) {}
    private BearingMotion() {}
    public static void check(ControlledContraptionEntity entity, float speed) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)
                || !ElevatorLink.managed(entity.getContraption()) || !ElevatorLink.bearing(entity.getContraption())) return;
        BearingStatus.begin(entity, speed);
        boolean blocked = blocked(level, entity, speed);
        var parentHit=CollisionState.get(entity);
        boolean childBlocked = dev.createsablecontraptions.oriented.ChildMotion.angular(entity, speed);
        if(blocked)CollisionState.restore(entity,parentHit);
        BearingStatus.children(entity, childBlocked);
        blocked |= childBlocked;
        var link = (ElevatorLink) entity.getContraption();
        boolean old = link.csc$isBlocked();
        link.csc$setBlocked(blocked);
        boolean stall = blocked || dev.createsablecontraptions.oriented.PhysicalFamily.actorStalled(entity);
        entity.getContraption().stalled = stall;
        entity.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(), stall);
        if (old != blocked) ((ContraptionEntityAccessor) entity).csc$onStalled();
    }

    private static boolean blocked(ServerLevel level, ControlledContraptionEntity entity, float speed) {
        var own = ElevatorBridge.resolve(level, entity.getContraption());
        if (own == null || entity.getRotationAxis() == null) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.UNAVAILABLE,"物理结构或旋转轴不可用");
        var sublevels = SubLevelContainer.getContainer(level).getAllSubLevels();
        // Cull distant physical structures, but never skip the ordinary world query.
        var plotBounds = own.getPlot().getBoundingBox();
        var plotPivot = own.getPlot().getCenterBlock().getCenter();
        double rx = Math.max(Math.abs(plotBounds.minX() - plotPivot.x), Math.abs(plotBounds.maxX() + 1 - plotPivot.x));
        double ry = Math.max(Math.abs(plotBounds.minY() - plotPivot.y), Math.abs(plotBounds.maxY() + 1 - plotPivot.y));
        double rz = Math.max(Math.abs(plotBounds.minZ() - plotPivot.z), Math.abs(plotBounds.maxZ() + 1 - plotPivot.z));
        double radius = Math.sqrt(rx * rx + ry * ry + rz * rz);
        var broad = new AABB(entity.position().add(.5, .5, .5), entity.position().add(.5, .5, .5)).inflate(radius);
        var nearby = sublevels.stream().filter(s -> {
            if (s == own || s.isRemoved()) return false;
            if (dev.createsablecontraptions.oriented.PhysicalFamily.related(entity, s.getUniqueId())) return false;
            var b = s.boundingBox();
            return broad.intersects(new AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()));
        }).toList();
        var arcs = new ArrayList<Arc>();
        int envelopes = 0;
        AABB search = null;
        var anchor = own.getPlot().getCenterBlock();
        var pivot = new Vector3d(entity.getX() + .5, entity.getY() + .5, entity.getZ() + .5);
        float start = entity.getAngle(1);
        double worldClearance = dev.createsablecontraptions.CscConfig.rotationClearance();
        for (long packed : ElevatorBridge.data(own).getLongArray("Blocks")) {
            var local = BlockPos.of(packed);
            var pos = anchor.offset(local);
            if (!level.isLoaded(pos)) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.UNLOADED,"结构区块未加载：" + pos.toShortString());
            for (var shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                var box = shape.move(local).move(-.5, -.5, -.5);
                java.util.List<SweptBox.Box> sweep;
                var localBox = SweptBox.Box.aligned(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                try {
                    sweep = RotatingSweep.envelopes(localBox,
                            pivot, entity.getRotationAxis().ordinal(), Math.toRadians(speed),
                            t -> BearingBridge.rotation(entity, start + speed * (float) t));
                } catch (IllegalArgumentException tooLarge) { return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"单条扫掠超过 2048 段"); }
                envelopes += sweep.size();
                if (envelopes > 32768) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"扫掠总数超过 32768");
                for (var volume : sweep) { var b = bounds(volume); search = search == null ? b : search.minmax(b); }
                arcs.add(new Arc(local, localBox, sweep));
            }
        }
        if (search == null) return false;
        var zero = new SweptBox.V(0, 0, 0);
        if (search.minY < level.getMinBuildHeight() || search.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(search)
                || (search.getXsize() + 2) * (search.getYsize() + 2) * (search.getZsize() + 2) > 32768) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BOUNDARY,"查询超出世界边界/高度或体积预算");
        long worldComparisons = 0;
        boolean blocked = false;
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(search.minX, search.minY, search.minZ),
                BlockPos.containing(search.maxX, search.maxY, search.maxZ))) {
            if (!level.isLoaded(pos)) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.UNLOADED,"世界区块未加载：" + pos.toShortString());
            for (var shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                var box = shape.move(pos);
                var obstacle = SweptBox.Box.aligned(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                for (var arc : arcs) {
                    boolean candidate = false;
                    for (var moving : arc.envelopes) {
                        if (++worldComparisons > 2_000_000) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"世界碰撞比较超过 2000000 次");
                        if (SweptBox.blocked(moving, zero, obstacle)) { candidate = true; break; }
                    }
                    if (candidate && RotatingSweep.blocked(arc.local, pivot, entity.getRotationAxis().ordinal(), Math.toRadians(speed),
                            t -> BearingBridge.rotation(entity, start + speed * (float) t), obstacle, worldClearance)) {
                        blocked = true;
                        BearingStatus.contact(entity, arc.block, pos, arc.local, pivot, obstacle, worldClearance);
                        csc$drill(entity, arc.block, pos, start, speed);
                    }
                }
            }
        }
        for (var other : nearby) {
            if (other == own || other.isRemoved()) continue;
            var b = other.boundingBox();
            if (!search.intersects(new AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()))) continue;
            var localSearch = ElevatorCollisions.inverseBounds(search, other.logicalPose());
            var plot = other.getPlot().getBoundingBox();
            localSearch = localSearch.intersect(new AABB(plot.minX(), plot.minY(), plot.minZ(), plot.maxX() + 1, plot.maxY() + 1, plot.maxZ() + 1));
            double volume = (localSearch.getXsize() + 2) * (localSearch.getYsize() + 2) * (localSearch.getZsize() + 2);
            if (volume > 32768) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"另一物理结构查询体积超过 32768");
            long comparisons = 0;
            for (var pos : BlockPos.betweenClosed(BlockPos.containing(localSearch.minX, localSearch.minY, localSearch.minZ),
                    BlockPos.containing(localSearch.maxX, localSearch.maxY, localSearch.maxZ))) {
                if (!level.isLoaded(pos)) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.UNLOADED,"另一物理结构区块未加载：" + pos.toShortString());
                for (var shape : level.getBlockState(pos).getCollisionShape(level, pos).toAabbs()) {
                    var obstacle = ElevatorCollisions.oriented(shape.move(pos), other.logicalPose());
                    for (var arc : arcs) {
                        boolean candidate = false;
                        for (var moving : arc.envelopes) {
                            if (++comparisons > 2_000_000) return BearingStatus.fail(entity,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"结构间碰撞比较超过 2000000 次");
                            if (SweptBox.blocked(moving, zero, obstacle)) { candidate = true; break; }
                        }
                        if (candidate && RotatingSweep.blocked(arc.local, pivot, entity.getRotationAxis().ordinal(), Math.toRadians(speed),
                                t -> BearingBridge.rotation(entity, start + speed * (float) t), obstacle)) {
                            blocked = true;
                            BearingStatus.contact(entity, arc.block, pos, arc.local, pivot, obstacle, SweptBox.SKIN);
                            csc$drill(entity, arc.block, pos, start, speed);
                        }
                    }
                }
            }
        }
        return blocked;
    }

    private static void csc$drill(ControlledContraptionEntity entity, BlockPos local, BlockPos target, float start, float speed) {
        var point = new Vector3d(local.getX() + .25, local.getY() + .25, local.getZ() + .25);
        var previous = BearingBridge.rotation(entity, start).transform(new Vector3d(point));
        // A small tangent segment remains nonzero even for full revolutions.
        var next = BearingBridge.rotation(entity, start + Math.max(-30, Math.min(30, speed))).transform(point);
        next.sub(previous);
        dev.createsablecontraptions.linear.CollisionDrilling.offer(entity.getContraption(), local, target,
                new net.minecraft.world.phys.Vec3(next.x, next.y, next.z));
    }

    private static AABB bounds(SweptBox.Box b) {
        double x = Math.abs(b.x().x()) * b.half().x() + Math.abs(b.y().x()) * b.half().y() + Math.abs(b.z().x()) * b.half().z();
        double y = Math.abs(b.x().y()) * b.half().x() + Math.abs(b.y().y()) * b.half().y() + Math.abs(b.z().y()) * b.half().z();
        double z = Math.abs(b.x().z()) * b.half().x() + Math.abs(b.y().z()) * b.half().y() + Math.abs(b.z().z()) * b.half().z();
        return new AABB(b.center().x() - x, b.center().y() - y, b.center().z() - z,
                b.center().x() + x, b.center().y() + y, b.center().z() + z);
    }
}
