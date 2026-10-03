package dev.createsablecontraptions.oriented;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.contraptions.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.mixin.ContraptionAccessor;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import java.util.*;

/** Portable snapshots contain block/BE data, never references to live physics bodies. */
public final class PackedStructures {
    public static final String KEY = "CSCPackedPhysical";
    /** Copy mutable logical data without recursively duplicating the read-only physical snapshot. */
    public static CompoundTag logicalCopy(CompoundTag source) {
        var copy = new CompoundTag();
        for (var key : source.getAllKeys()) {
            var value = source.get(key);
            copy.put(key, key.equals(KEY) ? value : value.copy());
        }
        return copy;
    }
    private static final ThreadLocal<List<AbstractContraptionEntity>> PICKUP = new ThreadLocal<>();
    private static final ThreadLocal<dev.createsablecontraptions.physics.RestoreTransaction> RESTORING = new ThreadLocal<>();
    private PackedStructures() {}
    public static List<AbstractContraptionEntity> beginPickup(Entity target) {
        if (target instanceof AbstractContraptionEntity e) target = e.getVehicle();
        var list = new ArrayList<AbstractContraptionEntity>();
        if (target != null) for (var passenger : target.getPassengers())
            if (passenger instanceof AbstractContraptionEntity e && ElevatorLink.managed(e.getContraption())) collect(e, list);
        PICKUP.set(list);
        return list;
    }
    private static void collect(AbstractContraptionEntity e, List<AbstractContraptionEntity> list) {
        list.add(e);
        for (var p : e.getPassengers()) if (p instanceof AbstractContraptionEntity c && ElevatorLink.managed(c.getContraption())) collect(c, list);
    }
    public static boolean suppressDisassembly(AbstractContraptionEntity e) {
        var list = PICKUP.get();
        return list != null && !list.isEmpty() && list.getFirst().isRemoved() && list.contains(e);
    }
    public static void endPickup(List<AbstractContraptionEntity> list) {
        try {
            if (list.isEmpty() || !list.getFirst().isRemoved()) return; // Native size/permission failure leaves every body intact.
            for (var e : list) {
                if (!(e.level() instanceof ServerLevel level)) continue;
                var sub = ElevatorBridge.resolve(level, e.getContraption());
                if (sub != null) remove(sub);
                if (e.isAlive()) e.discard();
            }
        } finally { PICKUP.remove(); }
    }
    public static void writeItem(OrientedContraptionEntity e, ItemStack stack) {
        if (stack.isEmpty() || !(e.level() instanceof ServerLevel) || !ElevatorLink.managed(e.getContraption())) return;
        var tag = stack.get(AllDataComponents.MINECRAFT_CONTRAPTION_DATA);
        if (tag == null) return;
        tag = tag.copy();
        tag.put(KEY, snapshot(e));
        tag.remove("CSCSubLevel"); tag.remove("CSCBlocked"); tag.remove("SubContraptions");
        stack.set(AllDataComponents.MINECRAFT_CONTRAPTION_DATA, tag);
    }
    private static CompoundTag snapshot(AbstractContraptionEntity e) {
        var level = (ServerLevel) e.level();
        var sub = ElevatorBridge.resolve(level, e.getContraption());
        if (sub == null || !ElevatorEditing.reconcile(sub)) throw new IllegalStateException("Physical storage unavailable for cart pickup");
        e.getContraption().stop(level);
        var result = new CompoundTag();
        result.put("PhysicsData", ElevatorBridge.data(sub).copy());
        var blocks = new ListTag();
        var anchor = sub.getPlot().getCenterBlock();
        for (long packed : ElevatorBridge.data(sub).getLongArray("Blocks")) {
            var pos = anchor.offset(BlockPos.of(packed));
            var row = new CompoundTag(); row.putLong("Pos", packed);
            row.put("State", NbtUtils.writeBlockState(level.getBlockState(pos)));
            var be = level.getBlockEntity(pos);
            if (be != null) row.put("BE", be.saveWithFullMetadata(level.registryAccess()));
            blocks.add(row);
        }
        result.put("Blocks", blocks);
        var children = new ListTag();
        for (var p : e.getPassengers()) {
            if (!(p instanceof OrientedContraptionEntity child) || !ElevatorLink.managed(child.getContraption())) continue;
            var face = ((ContraptionAccessor) e.getContraption()).csc$children().get(child.getUUID());
            if (face == null) continue;
            var row = new CompoundTag();
            var childData = snapshot(child);
            var logical = child.getContraption().writeNBT(level.registryAccess(), false);
            logical.remove("CSCSubLevel"); logical.remove("CSCBlocked"); logical.remove("SubContraptions");
            logical.put(KEY, childData);
            row.put("Contraption", logical); row.put("Face", face.serializeNBT());
            row.putInt("Initial", child.getInitialOrientation().get3DDataValue());
            row.putFloat("Yaw", child.yaw); row.putFloat("Pitch", child.pitch);
            children.add(row);
        }
        result.put("Children", children);
        return result;
    }
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if(!event.isCanceled() && !event.getLevel().isClientSide && event.getEntity() instanceof AbstractContraptionEntity e) restore(e);
    }
    public static void restore(AbstractContraptionEntity e) {
        restoreFamily(() -> restoreBody(e, RESTORING.get()));
    }
    public static void trackPlacementEntity(AbstractContraptionEntity e) {
        var tx=RESTORING.get();
        if(tx==null)throw new IllegalStateException("Missing placement transaction");
        tx.undo(() -> {
            ((dev.createsablecontraptions.mixin.ContraptionEntityAccessor)e).csc$skipActorStop(true);
            ((ElevatorLink)e.getContraption()).csc$setSubLevel(null);
            e.discard();
        });
    }
    /** Item placement also keeps the transaction open until the root join is accepted. */
    public static void restoreFamily(Runnable action) {
        var tx = RESTORING.get();
        boolean owner = tx == null;
        if (owner) { tx = new dev.createsablecontraptions.physics.RestoreTransaction(); RESTORING.set(tx); }
        try {
            action.run();
            if (owner) tx.commit();
        } catch (RuntimeException failure) {
            if (owner) tx.rollback(failure);
            throw failure;
        } finally { if (owner) RESTORING.remove(); }
    }
    private static void restoreBody(AbstractContraptionEntity e, dev.createsablecontraptions.physics.RestoreTransaction tx) {
        if (!(e.level() instanceof ServerLevel level) || !(e.getContraption() instanceof PackedCarrier carrier)) return;
        var packed = carrier.csc$packed();
        if (packed == null || packed.isEmpty()) return;
        tx.undo(() -> {
            carrier.csc$packed(packed);
            ((dev.createsablecontraptions.mixin.ContraptionEntityAccessor)e).csc$skipActorStop(true);
            ((ElevatorLink)e.getContraption()).csc$setSubLevel(null);
            e.discard();
        });
        // onEntityCreated runs before the item sets position/yaw and mounts the cart.
        // EntityJoinLevelEvent runs after those choices, before first tracking/physics.
        if(e.getVehicle()!=null)e.getVehicle().positionRider(e);
        long phaseStarted=System.nanoTime();
        var container = SubLevelContainer.getContainer(level);
        var pose = new Pose3d();
        var initialAnchor=e.getAnchorVec();
        pose.position().set(initialAnchor.x+.5,initialAnchor.y+.5,initialAnchor.z+.5);
        var sub = (ServerSubLevel) container.allocateNewSubLevel(pose);
        tx.undo(() -> { ((ElevatorLink)e.getContraption()).csc$setSubLevel(null); remove(sub); });
        PlacementTrace.phase("物理结构分配",phaseStarted);
        try {
            phaseStarted=System.nanoTime();
            var anchor = sub.getPlot().getCenterBlock();
            var data = packed.getCompound("PhysicsData").copy();
            data.putLong("PlotAnchor", anchor.asLong());
            data.putDouble("X", e.getAnchorVec().x); data.putDouble("Y", e.getAnchorVec().y); data.putDouble("Z", e.getAnchorVec().z);
            var root = new CompoundTag(); root.put(ElevatorBridge.TAG, data); sub.setUserDataTag(root);
            var chunks = new HashSet<ChunkPos>();
            chunks.add(sub.getPlot().getCenterChunk());
            var blocks = packed.getList("Blocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocks.size(); i++) chunks.add(new ChunkPos(anchor.offset(BlockPos.of(blocks.getCompound(i).getLong("Pos")))));
            for (var chunk : chunks) sub.getPlot().newEmptyChunk(chunk);
            PlacementTrace.phase("区块准备",phaseStarted);
            var states = new HashMap<CompoundTag, net.minecraft.world.level.block.state.BlockState>();
            var blockLookup = level.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
            for (int i = 0; i < blocks.size(); i++) {
                var row = blocks.getCompound(i); var pos = anchor.offset(BlockPos.of(row.getLong("Pos")));
                phaseStarted=System.nanoTime();
                var state = states.computeIfAbsent(row.getCompound("State"), tag -> NbtUtils.readBlockState(blockLookup, tag));
                level.setBlock(pos, state, 82);
                var be = level.getBlockEntity(pos);
                PlacementTrace.phase("方块创建",phaseStarted);
                if (row.contains("BE")) {
                    phaseStarted=System.nanoTime();
                    if (be == null) throw new IllegalStateException("Missing restored block entity");
                    var tag = row.getCompound("BE").copy(); tag.putInt("x", pos.getX()); tag.putInt("y", pos.getY()); tag.putInt("z", pos.getZ());
                    be.loadWithComponents(tag, level.registryAccess()); be.setChanged();
                    PlacementTrace.phase("方块实体NBT加载",phaseStarted);
                    // setBlock already queued the update; the newly allocated plot's initial
                    // chunk stream includes the loaded BE. Do not queue the same block twice.
                }
            }
            ((ElevatorLink) e.getContraption()).csc$setSubLevel(sub.getUniqueId());
            phaseStarted=System.nanoTime();
            ((ContraptionAccessor) e.getContraption()).csc$setStorage(new PhysicalStorage(sub));
            // Overwrite the pickup-time rotation before the first hold, not one tick later.
            if(e instanceof OrientedContraptionEntity oriented) OrientedBridge.target(oriented);
            ElevatorPhysics.hold(sub);
            // This is initial placement, not movement from the allocation pose. The first
            // client interpolation pair must not sweep the cart through an obsolete pose.
            sub.updateLastPose();
            sub.latestLinearVelocity.zero(); sub.latestAngularVelocity.zero();
            sub.forceUpdateGlobalBounds();
            container.addForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
            PlacementTrace.phase("物理初始化及库存视图",phaseStarted);
            tx.onCommit(() -> carrier.csc$packed(null));
            var children = packed.getList("Children", Tag.TAG_COMPOUND);
            for (int i = 0; i < children.size(); i++) {
                var row = children.getCompound(i);
                var c = Contraption.fromNBT(level, logicalCopy(row.getCompound("Contraption")), false);
                var child = OrientedContraptionEntity.create(level, c, Direction.from3DDataValue(row.getInt("Initial")));
                tx.undo(() -> {
                    ((ContraptionAccessor)e.getContraption()).csc$children().remove(child.getUUID());
                    ((dev.createsablecontraptions.mixin.ContraptionEntityAccessor)child).csc$skipActorStop(true);
                    ((ElevatorLink)c).csc$setSubLevel(null);
                    child.discard();
                });
                child.startAtYaw(row.getFloat("Yaw")); child.pitch = row.getFloat("Pitch");
                var face = BlockFace.fromNBT(row.getCompound("Face"));
                ((ContraptionAccessor) e.getContraption()).csc$children().put(child.getUUID(), face);
                if (!child.startRiding(e)) throw new IllegalStateException("Could not attach restored child");
                e.positionRider(child);
                if (!level.addFreshEntity(child)) throw new IllegalStateException("Restored child join was rejected");
                OrientedBridge.target(child);
            }
        } catch (RuntimeException failure) { throw failure; } // Family owner performs rollback in reverse order.
    }
    private static void remove(ServerSubLevel sub) {
        ElevatorPhysics.release(sub);
        var container = SubLevelContainer.getContainer(sub.getLevel());
        container.removeForceLoadTicket(sub, ElevatorPhysics.TICKET, net.minecraft.util.Unit.INSTANCE);
        if (!sub.isRemoved()) container.removeSubLevel(sub, SubLevelRemovalReason.REMOVED);
    }
}
