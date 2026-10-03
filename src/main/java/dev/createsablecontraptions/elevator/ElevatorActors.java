package dev.createsablecontraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.createsablecontraptions.mixin.ContraptionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;
import java.util.*;

public final class ElevatorActors {
    private static final Map<MovementContext, CompoundTag> BEFORE = new WeakHashMap<>();
    private ElevatorActors() {}

    public static SubLevel sub(AbstractContraptionEntity entity) {
        if (!ElevatorLink.managed(entity.getContraption())) return null;
        var container = SubLevelContainer.getContainer(entity.level());
        return container == null ? null : container.getSubLevel(((ElevatorLink) entity.getContraption()).csc$getSubLevel());
    }

    public static boolean physicalActor(BlockEntity be) {
        if (be == null || be.getLevel() == null || MovementBehaviour.REGISTRY.get(be.getBlockState()) == null) return false;
        var sub = Sable.HELPER.getContaining(be.getLevel(), be.getBlockPos());
        if (sub instanceof ServerSubLevel server) return ElevatorBridge.managed(server);
        if (sub == null) return false;
        for (var ref : ContraptionHandler.loadedContraptions.get(be.getLevel()).values()) {
            var entity = ref.get();
            if (entity != null && ElevatorLink.managed(entity.getContraption())
                    && sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) return true;
        }
        return false;
    }

    public static boolean prepare(AbstractContraptionEntity entity) {
        var sub = sub(entity);
        if (sub == null || sub.isRemoved()) return false;
        var elevator = entity.getContraption();
        if (sub instanceof ServerSubLevel server && !(elevator.getStorage() instanceof PhysicalStorage))
            ((ContraptionAccessor) elevator).csc$setStorage(new PhysicalStorage(server));
        // Derive runtime actor membership on both sides from Sable's authoritative block stream.
        // Existing contexts retain their progress and disabled state; new ones start exactly once.
        if (entity.tickCount % 5 == 0) synchronize(entity, sub);
        checkpoint(entity);
        return true;
    }

    private static void synchronize(AbstractContraptionEntity entity, SubLevel sub) {
        var contraption = entity.getContraption();
        var level = entity.level();
        var anchor = sub.getPlot().getCenterBlock();
        for (var local : contraption.getBlocks().keySet())
            if (!level.isLoaded(anchor.offset(local))) return;
        var found = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (var holder : sub.getPlot().getLoadedChunks()) {
            var chunk = holder.getChunk();
            if (chunk == null) return;
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i];
                if (section.hasOnlyAir()) continue;
                int baseY = chunk.getSectionYFromSectionIndex(i) * 16;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    var state = section.getBlockState(x, y, z);
                    if (!state.isAir()) found.put(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y,
                            chunk.getPos().getMinBlockZ() + z).subtract(anchor), state);
                }
            }
        }
        if (found.isEmpty()) return; // wait for Sable's initial chunk stream
        boolean changed = false;
        var positions = new HashSet<>(contraption.getBlocks().keySet()); positions.addAll(found.keySet());
        for (var local : positions) {
            var previous = contraption.getBlocks().get(local);
            var state = found.get(local);
            if (contraption instanceof ElevatorContraption && previous != null && AllBlocks.REDSTONE_CONTACT.has(previous.state())) continue; // fixed elevator column reference
            if (previous != null && previous.state().equals(state)) continue;
            var actor = contraption.getActorAt(local);
            boolean sameBlock = previous != null && state != null && previous.state().getBlock() == state.getBlock();
            if (actor != null && !sameBlock) {
                var behaviour = MovementBehaviour.REGISTRY.get(actor.left.state());
                if (behaviour != null && actor.right != null) behaviour.stopMoving(actor.right);
                contraption.getActors().remove(actor);
                BEFORE.remove(actor.right);
            }
            contraption.getInteractors().remove(local);
            if (state == null) { contraption.getBlocks().remove(local); changed = true; continue; }
            var be = level.getBlockEntity(anchor.offset(local));
            var nbt = sameBlock ? previous.nbt() : be == null ? null : be.saveWithFullMetadata(level.registryAccess());
            var info = new StructureBlockInfo(local, state, nbt);
            contraption.getBlocks().put(local, info);
            var interaction = MovingInteractionBehaviour.REGISTRY.get(state);
            if (interaction != null) contraption.getInteractors().put(local, interaction);
            var behaviour = MovementBehaviour.REGISTRY.get(state);
            if (actor != null && sameBlock) { actor.setLeft(info); actor.right.state = state; }
            else if (behaviour != null) {
                var context = new MovementContext(level, info, contraption);
                if (!level.isClientSide) behaviour.startMoving(context);
                var filter = behaviour.canBeDisabledVia(context);
                if (filter != null) context.disabled = contraption.isActorTypeDisabled(filter)
                        || contraption.isActorTypeDisabled(net.minecraft.world.item.ItemStack.EMPTY);
                contraption.getActors().add(MutablePair.of(info, context));
            }
            changed = true;
        }
        if (changed) {
            AABB bounds = null;
            for (var pos : found.keySet()) bounds = bounds == null ? new AABB(pos) : bounds.minmax(new AABB(pos));
            contraption.bounds = bounds;
            if (ElevatorLink.bearing(contraption) && entity instanceof com.simibubi.create.content.contraptions.ControlledContraptionEntity controlled
                    && controlled.getRotationAxis() != null)
                ((ContraptionAccessor) contraption).csc$expandAroundAxis(controlled.getRotationAxis());
            if (ElevatorLink.oriented(contraption)) ((ContraptionAccessor) contraption).csc$expandAroundAxis(net.minecraft.core.Direction.Axis.Y);
            contraption.invalidateColliders();
            entity.setPos(entity.getX(), entity.getY(), entity.getZ());
            if (level.isClientSide) contraption.getOrCreateClientContraptionLazy().resetRenderLevel();
        }
    }

    /** Flush actor-owned hands/tools before vanilla calculates drops from a player-broken BE. */
    public static void beforePlayerBreak(ServerLevel level, BlockPos pos) {
        var sub = Sable.HELPER.getContaining(level, pos);
        if (!(sub instanceof ServerSubLevel server) || !ElevatorBridge.managed(server)) return;
        var local = pos.subtract(sub.getPlot().getCenterBlock());
        for (var ref : ContraptionHandler.loadedContraptions.get(level).values()) {
            var entity = ref.get();
            if (entity == null || !ElevatorLink.managed(entity.getContraption())
                    || !sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) continue;
            var actor = entity.getContraption().getActorAt(local);
            if (actor == null || actor.right == null) return;
            var behaviour = MovementBehaviour.REGISTRY.get(actor.left.state());
            if (behaviour == null) return;
            checkpoint(entity);
            behaviour.stopMoving(actor.right);
            commit(entity);
            // Also leave a usable actor if another mod cancels the break after this callback.
            var replacement = new MovementContext(level, new StructureBlockInfo(local, actor.left.state(),
                    actor.right.blockEntityData == null ? null : actor.right.blockEntityData.copy()), entity.getContraption());
            behaviour.startMoving(replacement);
            replacement.disabled = actor.right.disabled;
            actor.setRight(replacement);
            return;
        }
    }

    public static void checkpoint(AbstractContraptionEntity entity) {
        if (entity.level().isClientSide || !ElevatorLink.managed(entity.getContraption())) return;
        for (var actor : entity.getContraption().getActors())
            if (actor.right != null && actor.right.blockEntityData != null) BEFORE.put(actor.right, actor.right.blockEntityData.copy());
    }

    public static void commit(AbstractContraptionEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var sub = sub(entity); if (sub == null) return;
        for (var actor : entity.getContraption().getActors()) {
            var context = actor.right;
            var before = BEFORE.remove(context);
            if (before == null || context.blockEntityData == null || before.equals(context.blockEntityData)) continue;
            var be = level.getBlockEntity(sub.getPlot().getCenterBlock().offset(actor.left.pos()));
            if (be == null || be.getBlockState().getBlock() != actor.left.state().getBlock()) continue;
            var physical = be.saveWithFullMetadata(level.registryAccess());
            var merged = new CompoundTag();
            ActorDataPatch.merge(entries(before), entries(context.blockEntityData), entries(physical))
                    .forEach((key, value) -> merged.put(key, value.copy()));
            be.loadWithComponents(merged, level.registryAccess()); be.setChanged();
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static Map<String, net.minecraft.nbt.Tag> entries(CompoundTag tag) {
        var map = new HashMap<String, net.minecraft.nbt.Tag>();
        for (var key : tag.getAllKeys()) map.put(key, tag.get(key));
        return map;
    }

    public static void probeBreakers(AbstractContraptionEntity entity, Vec3 motion) {
        if (motion.lengthSqr() < 1e-12 || !(entity.level() instanceof ServerLevel level)) return;
        for (var pair : entity.getContraption().getActors()) {
            var ctx = pair.right;
            if (ctx == null || ctx.stall || ctx.disabled) continue;
            if (!(MovementBehaviour.REGISTRY.get(pair.left.state()) instanceof BlockBreakingMovementBehaviour breaker)) continue;
            var tip = entity.toGlobalVector(Vec3.atCenterOf(ctx.localPos).add(breaker.getActiveAreaOffset(ctx)), 1);
            // Sable's visitNewPosition wrapper also reads position, including before
            // Create has performed the first actor tick after assembly.
            ctx.position = tip;
            ctx.motion = motion; ctx.relativeMotion = motion; ctx.rotation = v -> v;
            if (!breaker.isActive(ctx)) continue;
            int steps = Math.max(1, (int) Math.ceil(motion.length() * 8));
            for (int i = 0; i <= steps; i++) {
                var target = BlockPos.containing(tip.add(motion.scale(i / (double) steps)));
                if (!level.isLoaded(target) || !breaker.canBreak(level, target, level.getBlockState(target))) continue;
                breaker.visitNewPosition(ctx, target);
                break;
            }
        }
    }
}
