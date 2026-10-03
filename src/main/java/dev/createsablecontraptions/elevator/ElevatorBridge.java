package dev.createsablecontraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import dev.createsablecontraptions.mixin.ContraptionAccessor;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class ElevatorBridge {

    public static final String TAG = "CreateSableStructure";
    private ElevatorBridge() {}

    public static void validate(ElevatorContraption elevator, Level level) throws AssemblyException {
        if (Sable.HELPER.getContaining(level, elevator.anchor) != null) fail("nested");
    }

    private static void fail(String key) throws AssemblyException {
        throw new AssemblyException(Component.translatable("csc.assembly." + key));
    }

    public static ServerSubLevel resolve(ServerLevel level, Contraption elevator) {
        var container = SubLevelContainer.getContainer(level);
        var id = ((ElevatorLink) elevator).csc$getSubLevel();
        if (container == null || id == null) return null;
        return container.getSubLevel(id) instanceof ServerSubLevel sub && !sub.isRemoved() ? sub : null;
    }

    public static void assemble(ServerLevel level, Contraption elevator, BlockPos offset) {
        if (ElevatorLink.managed(elevator)) throw new IllegalStateException("Elevator already assembled");
        BlockPos origin = elevator.anchor.offset(offset);
        BlockPos cylinder = dev.createsablecontraptions.linear.PistonParts.controller(elevator);
        List<BlockPos> source = elevator.getBlocks().keySet().stream()
                .filter(p -> !dev.createsablecontraptions.oriented.OrientedBridge.virtualAnchor(elevator, p)).map(origin::offset)
                .filter(p -> !p.equals(cylinder)).toList();
        BoundingBox3i bounds = bounds(source);
        // Snapshot actual world state, rather than Create's potentially transformed capture state.
        List<StructureBlockInfo> backup = new ArrayList<>();
        for (BlockPos pos : source) {
            var be = level.getBlockEntity(pos);
            backup.add(new StructureBlockInfo(pos, level.getBlockState(pos),
                    be == null ? null : be.saveWithFullMetadata(level.registryAccess())));
        }
        ServerSubLevel sub = null;
        try {
            boolean before = dev.createsablecontraptions.linear.GantrySupport.beginTransfer();
            try { sub = SubLevelAssemblyHelper.assembleBlocks(level, origin, source, bounds); }
            finally { dev.createsablecontraptions.linear.GantrySupport.endTransfer(before); }
            BlockPos plotAnchor = sub.getPlot().getCenterBlock();
            for (var info : elevator.getBlocks().values())
                if (dev.createsablecontraptions.oriented.OrientedBridge.virtualAnchor(elevator, info.pos()))
                    level.setBlock(plotAnchor.offset(info.pos()), info.state(), 82);
            for (StructureBlockInfo info : backup) {
                BlockPos dest = plotAnchor.offset(info.pos().subtract(origin));
                if (level.getBlockState(dest).getBlock() != info.state().getBlock() || !level.getBlockState(info.pos()).isAir())
                    throw new IllegalStateException("Sable block transfer verification failed at " + info.pos());
                if (info.nbt() != null && level.getBlockEntity(dest) == null)
                    throw new IllegalStateException("Sable block entity transfer failed at " + info.pos());
            }
            if (cylinder != null) {
                // Create captures a synthetic rod/head at the cylinder position, not the cylinder BE.
                var local = cylinder.subtract(origin);
                var captured = elevator.getBlocks().get(local);
                if (captured == null || !dev.createsablecontraptions.linear.PistonParts.movingPart(captured.state()))
                    throw new IllegalStateException("Missing synthetic piston part");
                level.setBlock(plotAnchor.offset(local), captured.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            CompoundTag data = new CompoundTag();
            data.putInt("Schema", 1);
            data.putBoolean("Bearing", ElevatorLink.bearing(elevator));
            data.putLong("PlotAnchor", plotAnchor.asLong());
            data.putLongArray("Blocks", elevator.getBlocks().keySet().stream().mapToLong(BlockPos::asLong).toArray());
            data.putDouble("X", origin.getX());
            data.putDouble("Y", origin.getY());
            data.putDouble("Z", origin.getZ());
            ListTag glueData = new ListTag();
            for (SuperGlueEntity glue : ((ContraptionAccessor) elevator).csc$glueToRemove()) {
                CompoundTag glueTag = new CompoundTag();
                SuperGlueEntity.writeBoundingBox(glueTag, glue.getBoundingBox().intersect(bounds.toAABB())
                        .move(Vec3.atLowerCornerOf(origin).scale(-1)));
                glueData.add(glueTag);
            }
            data.put("Glue", glueData);
            CompoundTag root = new CompoundTag();
            root.put(TAG, data);
            sub.setUserDataTag(root);
            ElevatorPhysics.hold(sub);
            SubLevelContainer.getContainer(level).addForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
            ((ElevatorLink) elevator).csc$setSubLevel(sub.getUniqueId());
            ((ContraptionAccessor) elevator).csc$setStorage(new PhysicalStorage(sub));
            ((ContraptionAccessor) elevator).csc$glueToRemove().forEach(SuperGlueEntity::discard);
            if (cylinder != null) level.setBlock(cylinder, level.getBlockState(cylinder)
                    .setValue(com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.STATE,
                            com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState.MOVING), 82);
        } catch (RuntimeException failure) {
            // A late failure may occur after ownership and the force-load ticket were published.
            ((ElevatorLink) elevator).csc$setSubLevel(null);
            if (sub != null && !sub.isRemoved()) {
                ElevatorPhysics.release(sub);
                SubLevelContainer.getContainer(level).removeForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
                SubLevelContainer.getContainer(level).removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
            }
            for (StructureBlockInfo info : backup) {
                level.setBlock(info.pos(), info.state(), Block.UPDATE_ALL);
                var be = level.getBlockEntity(info.pos());
                if (be != null && info.nbt() != null) be.loadWithComponents(info.nbt(), level.registryAccess());
            }
            throw failure;
        }
    }

    public static boolean canDisassemble(ServerLevel level, ElevatorContraption elevator, Vec3 target) {
        ServerSubLevel sub = resolve(level, elevator);
        if (sub == null) return false; // Never reconstruct from the stale Create snapshot.
        if (!ElevatorEditing.reconcile(sub) || !ElevatorEditing.valid(level, sub, elevator)) return false;
        BlockPos anchor = BlockPos.containing(target.add(.5, .5, .5));
        BlockPos plotAnchor = BlockPos.of(data(sub).getLong("PlotAnchor"));
        for (long packed : data(sub).getLongArray("Blocks")) {
            BlockPos local = BlockPos.of(packed), dest = anchor.offset(local);
            if (level.isOutsideBuildHeight(dest) || !level.getWorldBorder().isWithinBounds(dest) || !level.isLoaded(dest)) return false;
            if (!level.getBlockState(dest).isAir()) return false;
            if (!ElevatorControlsBridge.hasActorData(level, elevator, local, plotAnchor.offset(local))) return false;
        }
        return !ElevatorCollisions.blocked(level, elevator, target, Vec3.ZERO);
    }

    public static void disassemble(ServerLevel level, ElevatorContraption elevator, StructureTransform transform) {
        ServerSubLevel sub = resolve(level, elevator);
        BlockPos target = transform.apply(BlockPos.ZERO);
        if (sub == null || !canDisassemble(level, elevator, Vec3.atLowerCornerOf(target)))
            throw new IllegalStateException("Unsafe elevator disassembly refused; Sable platform retained");
        BlockPos plotAnchor = BlockPos.of(data(sub).getLong("PlotAnchor"));
        ListTag glue = data(sub).getList("Glue", Tag.TAG_COMPOUND).copy();
        List<BlockPos> source = new ArrayList<>();
        for (long packed : data(sub).getLongArray("Blocks")) source.add(plotAnchor.offset(BlockPos.of(packed)));
        List<StructureBlockInfo> live = source.stream().map(pos -> {
            var be = level.getBlockEntity(pos);
            return new StructureBlockInfo(pos.subtract(plotAnchor), level.getBlockState(pos),
                    be == null ? null : be.saveWithFullMetadata(level.registryAccess()));
        }).toList();
        ElevatorControlsBridge.restoreActorData(level, elevator, plotAnchor);
        var transfer = new SubLevelAssemblyHelper.AssemblyTransform(plotAnchor, target, 0, Rotation.NONE, level);
        // moveOtherStuff transfers supported hanging entities. Glue is separately retained in user data.
        SubLevelAssemblyHelper.moveOtherStuff(level, transfer, source, bounds(source));
        SubLevelAssemblyHelper.moveBlocks(level, transfer, source);
        for (var info : live) {
            if (level.getBlockState(target.offset(info.pos())).getBlock() != info.state().getBlock()
                    || info.nbt() != null && level.getBlockEntity(target.offset(info.pos())) == null)
                throw new IllegalStateException("Elevator disassembly verification failed; recovery required");
        }
        ElevatorPhysics.release(sub);
        var container = SubLevelContainer.getContainer(level);
        container.removeForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
        if (!sub.isRemoved()) container.removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
        for (int i = 0; i < glue.size(); i++) {
            level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.readBoundingBox(glue.getCompound(i)).move(target)));
        }
        // Keep the UUID until the logical entity is discarded: no transient proxy rendering/collision.
    }

    public static CompoundTag data(ServerSubLevel sub) {
        return sub.getUserDataTag().getCompound(TAG);
    }

    public static boolean managed(ServerSubLevel sub) {
        return sub.getUserDataTag() != null && sub.getUserDataTag().contains(TAG);
    }


    public static boolean samePlatformState(BlockState actual, BlockState expected) {
        if (AllBlocks.REDSTONE_CONTACT.has(expected) && AllBlocks.REDSTONE_CONTACT.has(actual))
            return actual.setValue(RedstoneContactBlock.POWERED, false).equals(expected.setValue(RedstoneContactBlock.POWERED, false));
        return actual.equals(expected);
    }

    private static BoundingBox3i bounds(List<BlockPos> positions) {
        BlockPos first = positions.getFirst();
        BoundingBox3i result = new BoundingBox3i(first.getX(), first.getY(), first.getZ(), first.getX(), first.getY(), first.getZ());
        for (BlockPos pos : positions) result.expandTo(pos.getX(), pos.getY(), pos.getZ());
        return result;
    }
}

