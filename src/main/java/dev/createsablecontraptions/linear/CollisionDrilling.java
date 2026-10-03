package dev.createsablecontraptions.linear;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;

/** Collision code provides the exact world/plot target and the local block that hit it. */
public final class CollisionDrilling {
    public static final String ACTIVE = "CSCCollisionDrill";
    private CollisionDrilling() {}
    public static void offer(Contraption c, BlockPos local, BlockPos target, Vec3 motion) {
        if (!ElevatorLink.managed(c) || c.entity == null || c.entity.level().isClientSide || motion.lengthSqr() < 1e-12) return;
        if (AssemblySourceProtection.protects(c, target)) return;
        var level = c.entity.level();
        if (!level.isLoaded(target)) return;
        var own = dev.createsablecontraptions.elevator.ElevatorActors.sub(c.entity);
        if (own == null || !AllBlocks.MECHANICAL_DRILL.has(level.getBlockState(own.getPlot().getCenterBlock().offset(local)))) return;
        var containing = Sable.HELPER.getContaining(level, target);
        if (containing != null && containing.getUniqueId().equals(((ElevatorLink) c).csc$getSubLevel())) return;
        for (var actor : c.getActors()) {
            var ctx = actor.right;
            if (!actor.left.pos().equals(local) || ctx == null || ctx.disabled || ctx.stall
                    || !AllBlocks.MECHANICAL_DRILL.has(actor.left.state())) continue;
            if (!(MovementBehaviour.REGISTRY.get(actor.left.state()) instanceof DrillMovementBehaviour drill)
                    || !drill.canBreak(level, target, level.getBlockState(target)) || !inFront(ctx, target)) continue;
            ctx.position = c.entity.toGlobalVector(Vec3.atCenterOf(local).add(drill.getActiveAreaOffset(ctx)), 1);
            ctx.motion = motion;
            ctx.relativeMotion = c.entity.reverseRotation(motion, 1);
            ctx.rotation = v -> c.entity.applyRotation(v, 1);
            // Keep Create's hardness/progress/drops pipeline, without its forward-only target search.
            ctx.data.remove("Progress");
            ctx.data.remove("TicksUntilNextProgress");
            ctx.data.remove("ProjectedPos");
            ctx.data.put("BreakingPos", NbtUtils.writeBlockPos(target));
            ctx.data.putBoolean(ACTIVE, true);
            ctx.stall = true;
            return;
        }
    }

    public static boolean targetInReach(com.simibubi.create.content.contraptions.behaviour.MovementContext ctx) {
        var own = dev.createsablecontraptions.elevator.ElevatorActors.sub(ctx.contraption.entity);
        var target = NbtUtils.readBlockPos(ctx.data, "BreakingPos").orElse(null);
        if (own == null || target == null || AssemblySourceProtection.protects(ctx.contraption, target)) return false;
        var drill = own.getPlot().getCenterBlock().offset(ctx.localPos);
        return ctx.world.isLoaded(target) && inFront(ctx, target) && AllBlocks.MECHANICAL_DRILL.has(ctx.world.getBlockState(drill))
                && Sable.HELPER.distanceSquaredWithSubLevels(ctx.world, drill.getCenter(), target.getCenter())
                <= Math.pow(2 + ctx.motion.length(), 2);
    }
    private static boolean inFront(com.simibubi.create.content.contraptions.behaviour.MovementContext ctx, BlockPos target) {
        var entity = ctx.contraption.entity;
        var center = entity.toGlobalVector(Vec3.atCenterOf(ctx.localPos), 1);
        var facing = entity.applyRotation(Vec3.atLowerCornerOf(ctx.state.getValue(
                com.simibubi.create.content.kinetics.drill.DrillBlock.FACING).getNormal()), 1);
        var worldTarget = Sable.HELPER.projectOutOfSubLevel(ctx.world, target.getCenter());
        var diff = worldTarget.subtract(center);
        return dev.createsablecontraptions.physics.DrillHemisphere.inFront(diff.x, diff.y, diff.z, facing.x, facing.y, facing.z);
    }
    public static boolean allowedTarget(com.simibubi.create.content.contraptions.behaviour.MovementContext ctx, BlockPos target) {
        if (AssemblySourceProtection.protects(ctx.contraption, target)) return false;
        return !ElevatorLink.managed(ctx.contraption) || !AllBlocks.MECHANICAL_DRILL.has(ctx.state)
                || ctx.contraption.entity != null && inFront(ctx, target);
    }
}
