package dev.createsablecontraptions.bearing;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.createsablecontraptions.elevator.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import java.util.*;

public final class BearingBridge {
    private BearingBridge() {}

    public static void target(ControlledContraptionEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !ElevatorLink.bearing(entity.getContraption())
                || !ElevatorLink.managed(entity.getContraption()) || entity.getRotationAxis() == null) return;
        var sub = ElevatorBridge.resolve(level, entity.getContraption());
        if (sub == null) return;
        // Use Create's actual basis; do not assume Minecraft's axis-angle sign conventions.
        var q = rotation(entity, entity.getAngle(1));
        var tag = ElevatorBridge.data(sub);
        tag.putDouble("QX", q.x); tag.putDouble("QY", q.y); tag.putDouble("QZ", q.z); tag.putDouble("QW", q.w);
        ElevatorPhysics.target(sub, entity.position());
        dev.createsablecontraptions.oriented.OrientedBridge.children(entity);
    }

    public static Quaterniond rotation(ControlledContraptionEntity entity, float angle) {
        var axis = entity.getRotationAxis();
        Vec3 x = net.createmod.catnip.math.VecHelper.rotate(new Vec3(1, 0, 0), angle, axis),
                y = net.createmod.catnip.math.VecHelper.rotate(new Vec3(0, 1, 0), angle, axis),
                z = net.createmod.catnip.math.VecHelper.rotate(new Vec3(0, 0, 1), angle, axis);
        return dev.createsablecontraptions.physics.RotatingFrame.fromBasis(new org.joml.Vector3d(x.x, x.y, x.z),
                new org.joml.Vector3d(y.x, y.y, y.z), new org.joml.Vector3d(z.x, z.y, z.z));
    }

    public static StructureTransform transform(ControlledContraptionEntity entity, boolean returnToOrigin) {
        float angle = returnToOrigin ? 0 : entity.getAngle(1);
        Axis axis = entity.getRotationAxis();
        return new StructureTransform(BlockPos.containing(entity.position().add(.5, .5, .5)),
                axis == Axis.X ? angle : 0, axis == Axis.Y ? angle : 0, axis == Axis.Z ? angle : 0);
    }

    public static boolean canDisassemble(ServerLevel level, Contraption contraption, StructureTransform transform,
                                          Set<BlockPos> destinations) {
        var sub = ElevatorBridge.resolve(level, contraption);
        if (sub == null || !ElevatorEditing.reconcile(sub)) return dev.createsablecontraptions.oriented.DisassemblyStatus.record(contraption,"物理结构不可用或方块索引未就绪",false);
        for (long packed : ElevatorBridge.data(sub).getLongArray("Blocks")) {
            if (dev.createsablecontraptions.oriented.OrientedBridge.virtualAnchor(contraption, BlockPos.of(packed))) continue;
            var dest = transform.apply(BlockPos.of(packed));
            var state = level.getBlockState(sub.getPlot().getCenterBlock().offset(BlockPos.of(packed)));
            boolean cylinderSlot = dev.createsablecontraptions.linear.PistonParts.cylinderSlot(contraption, dest, state);
            if (!destinations.add(dest)) return dev.createsablecontraptions.oriented.DisassemblyStatus.record(contraption,"主/子结构目标重叠："+dest.toShortString(),false);
            if(level.isOutsideBuildHeight(dest) || !level.getWorldBorder().isWithinBounds(dest) || !level.isLoaded(dest))
                return dev.createsablecontraptions.oriented.DisassemblyStatus.record(contraption,"目标超出边界或未加载："+dest.toShortString(),false);
            if(!DisassemblyPlacement.canReplace(level,dest,transform.apply(state))
                    && !(cylinderSlot && dev.createsablecontraptions.linear.PistonParts.cylinder(level.getBlockState(dest))))
                return dev.createsablecontraptions.oriented.DisassemblyStatus.record(contraption,"目标被占用："+dest.toShortString()+"，障碍="
                        +net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(dest).getBlock())+"，待放置="
                        +net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()),false);
        }
        return dev.createsablecontraptions.oriented.DisassemblyStatus.record(contraption,"目标空间检查通过",true);
    }

    public static boolean ready(ControlledContraptionEntity entity, boolean returnToOrigin, Set<BlockPos> destinations) {
        if (entity == null || !ElevatorLink.managed(entity.getContraption())) return true;
        return entity.level() instanceof ServerLevel level
                && dev.createsablecontraptions.oriented.PhysicalFamily.ready(entity, transform(entity, returnToOrigin), destinations);
    }

    public static void disassemble(ServerLevel level, Contraption contraption, StructureTransform transform) {
        if (!canDisassemble(level, contraption, transform, new HashSet<>()))
            throw new IllegalStateException("Unsafe bearing disassembly refused; physical structure retained");
        var sub = ElevatorBridge.resolve(level, contraption);
        var data = ElevatorBridge.data(sub);
        BlockPos anchor = BlockPos.of(data.getLong("PlotAnchor"));
        var riderTargets = new HashMap<net.minecraft.world.entity.Entity, Vec3>();
        var physicalBounds = sub.boundingBox();
        for (var rider : level.getEntities((net.minecraft.world.entity.Entity) null,
                new AABB(physicalBounds.minX(), physicalBounds.minY(), physicalBounds.minZ(), physicalBounds.maxX(), physicalBounds.maxY(), physicalBounds.maxZ()).inflate(2),
                e -> !e.isPassenger() && !(e instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)
                        && dev.ryanhcode.sable.Sable.HELPER.getTrackingSubLevel(e) == sub)) {
            Vec3 local = sub.logicalPose().transformPositionInverse(rider.position()).subtract(Vec3.atLowerCornerOf(anchor));
            riderTargets.put(rider, transform.apply(local).add(0, 1 / 16.0, 0));
        }
        var glue = data.getList("Glue", Tag.TAG_COMPOUND).copy();
        List<BlockPos> all = Arrays.stream(data.getLongArray("Blocks")).mapToObj(p -> anchor.offset(BlockPos.of(p))).toList();
        List<BlockPos> cylinderParts = all.stream().filter(p ->
                dev.createsablecontraptions.linear.PistonParts.cylinderSlot(contraption, transform.apply(p.subtract(anchor)), level.getBlockState(p))).toList();
        List<BlockPos> source = all.stream().filter(p -> !cylinderParts.contains(p)
                && !dev.createsablecontraptions.oriented.OrientedBridge.virtualAnchor(contraption, p.subtract(anchor))).toList();
        var bounds = new BoundingBox3i(all.getFirst().getX(), all.getFirst().getY(), all.getFirst().getZ(),
                all.getFirst().getX(), all.getFirst().getY(), all.getFirst().getZ());
        var states = new HashMap<BlockPos, BlockState>();
        var entities = new HashSet<BlockPos>();
        for (var pos : source) {
            bounds.expandTo(pos.getX(), pos.getY(), pos.getZ());
            states.put(pos, level.getBlockState(pos));
            if (level.getBlockEntity(pos) != null) entities.add(pos);
        }
        // Sable normally only rotates about Y. Delegate all three axes, block-state
        // transformers and the exact 90-degree placement rounding to Create.
        var transfer = new SubLevelAssemblyHelper.AssemblyTransform(anchor, transform.offset, 0, Rotation.NONE, level) {
            @Override public Vec3 apply(Vec3 pos) { return transform.apply(pos.subtract(Vec3.atLowerCornerOf(anchor))); }
            @Override public BlockPos apply(BlockPos pos) { return transform.apply(pos.subtract(anchor)); }
            @Override public BlockState apply(BlockState state) { return transform.apply(state); }
        };
        if (!source.isEmpty()) {
            // All destinations were preflighted before uncoupling. Clear only actual
            // placement cells; the virtual minecart anchor/assembler and cylinders are excluded.
            for(var pos:source)DisassemblyPlacement.clear(level,transfer.apply(pos),transform.apply(states.get(pos)));
            SubLevelAssemblyHelper.moveOtherStuff(level, transfer, source, bounds);
            SubLevelAssemblyHelper.moveBlocks(level, transfer, source);
        }
        for (var pos : cylinderParts) {
            var dest = transfer.apply(pos);
            boolean head = com.simibubi.create.AllBlocks.MECHANICAL_PISTON_HEAD.has(level.getBlockState(pos));
            if (dev.createsablecontraptions.linear.PistonParts.cylinder(level.getBlockState(dest)))
                level.setBlock(dest, level.getBlockState(dest).setValue(
                    com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.STATE,
                    head ? com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState.RETRACTED
                            : com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState.EXTENDED), 82);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 82);
        }
        for (var pos : source) {
            var dest = transfer.apply(pos);
            var be = level.getBlockEntity(dest);
            if (level.getBlockState(dest).getBlock() != transform.apply(states.get(pos)).getBlock()
                    || !level.getBlockState(pos).isAir() || entities.contains(pos) && be == null)
                throw new IllegalStateException("Bearing transfer verification failed; recovery required");
            if (be != null) { transform.apply(be); be.setChanged(); }
        }
        SubLevelAssemblyHelper.moveTrackingPoints(level, bounds, null, transfer);
        riderTargets.forEach((rider, pos) -> {
            ((dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension) rider).sable$setTrackingSubLevel(null);
            rider.teleportTo(pos.x, pos.y, pos.z);
        });
        ElevatorPhysics.release(sub);
        var container = SubLevelContainer.getContainer(level);
        container.removeForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
        if (!sub.isRemoved()) container.removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
        for (int i = 0; i < glue.size(); i++)
            level.addFreshEntity(new SuperGlueEntity(level, transformBounds(SuperGlueEntity.readBoundingBox(glue.getCompound(i)), transform)));
    }

    private static AABB transformBounds(AABB box, StructureTransform transform) {
        AABB result = null;
        for (int i = 0; i < 8; i++) {
            Vec3 p = transform.apply(new Vec3((i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ));
            result = result == null ? new AABB(p, p) : result.minmax(new AABB(p, p));
        }
        return result;
    }
}
