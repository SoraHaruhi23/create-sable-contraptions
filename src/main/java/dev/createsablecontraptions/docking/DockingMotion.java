package dev.createsablecontraptions.docking;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.actors.psi.*;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.physics.DockingSpeed;
import net.minecraft.core.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class DockingMotion {
    @FunctionalInterface public interface Path { Vec3 at(Vec3 local, double ticks); }
    private static final Map<AbstractContraptionEntity, State> STATES = new WeakHashMap<>();
    private static final class State {
        final DockingSpeed ramp = new DockingSpeed();
        final Set<BlockPos> departing = new HashSet<>();
        final Set<BlockPos> held = new HashSet<>();
        long tick = Long.MIN_VALUE;
        double factor = 1;
    }
    private DockingMotion() {}
    public static double linear(AbstractContraptionEntity e, Vec3 velocity) {
        return scale(e, (p, t) -> e.toGlobalVector(p, 1).add(velocity.scale(t)));
    }
    public static float angular(ControlledContraptionEntity e, float speed) {
        if (e == null || e.getRotationAxis() == null) return speed;
        return (float) (speed * scale(e, (p, t) -> {
            var v = p.subtract(.5, .5, .5);
            var q = dev.createsablecontraptions.bearing.BearingBridge.rotation(e, e.getAngle(1) + speed * (float) t);
            var result = q.transform(new org.joml.Vector3d(v.x, v.y, v.z));
            return new Vec3(result.x, result.y, result.z).add(.5, .5, .5).add(e.getAnchorVec());
        }));
    }
    public static double scale(AbstractContraptionEntity e, Path path) {
        if (e == null || e.level().isClientSide || !ElevatorLink.managed(e.getContraption())) return 1;
        var state = STATES.computeIfAbsent(e, ignored -> new State());
        long tick = e.level().getGameTime();
        if (state.tick == tick) return state.factor;
        state.tick = tick;
        Set<BlockPos> nowHeld = new HashSet<>();
        List<AbstractContraptionEntity> owners = new ArrayList<>(); owners.add(e);
        for (int index = 0; index < owners.size(); index++)
            for (var passenger : owners.get(index).getPassengers())
                if (passenger instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption())) owners.add(child);
        double nearest = Double.POSITIVE_INFINITY, nominal = 0;
        for (var owner : owners) for (var pair : owner.getContraption().getActors()) {
            var ctx = pair.right;
            if (ctx == null || ctx.disabled || !(MovementBehaviour.REGISTRY.get(pair.left.state()) instanceof PortableStorageInterfaceMovement psi)) continue;
            var working = NbtUtils.readBlockPos(ctx.data, "WorkingPos").orElse(null);
            if (ctx.stall && working != null) nowHeld.add(working);
        }
        for (var pos : state.held) if (!nowHeld.contains(pos)) state.departing.add(pos);
        state.held.clear(); state.held.addAll(nowHeld);
        // Ignore a just-serviced dock until the moving structure has left its vicinity.
        state.departing.removeIf(p -> owners.stream().flatMap(owner -> owner.getContraption().getActors().stream())
                .noneMatch(pair -> pair.right != null && NbtUtils.readBlockPos(pair.right.data, "WorkingPos").filter(p::equals).isPresent()));
        for (var owner : owners) for (var pair : owner.getContraption().getActors()) {
            var ctx = pair.right;
            if (ctx == null || ctx.disabled || !(MovementBehaviour.REGISTRY.get(pair.left.state()) instanceof PortableStorageInterfaceMovement psi)) continue;
            Vec3 active = Vec3.atCenterOf(ctx.localPos).add(psi.getActiveAreaOffset(ctx));
            Vec3 base = owner.toGlobalVector(active, 1);
            Vec3 parentPoint = e.toLocalVector(base, 1);
            Path actorPath = owner == e ? path : (p, t) -> base.add(path.at(parentPoint, t).subtract(path.at(parentPoint, 0)));
            double speed = actorPath.at(active, 1).distanceTo(actorPath.at(active, 0));
            nominal = Math.max(nominal, speed);
            if (speed < 1e-8) continue;
            double horizon = Math.min(80, 4.5 / speed);
            Vec3 previous = base; double distance = 0;
            for (int i = 0; i <= 48; i++) {
                double t = horizon * i / 48;
                Vec3 point = actorPath.at(active, t);
                distance += point.distanceTo(previous); previous = point;
                Vec3 unit = Vec3.atLowerCornerOf(ctx.state.getValue(PortableStorageInterfaceBlock.FACING).getNormal());
                Vec3 normal = owner == e ? path.at(unit, t).subtract(path.at(Vec3.ZERO, t)) : owner.applyRotation(unit, 1);
                Direction facing = Direction.getNearest(normal.x, normal.y, normal.z);
                if (normal.distanceTo(Vec3.atLowerCornerOf(facing.getNormal())) > .5) continue;
                for (int ahead = 0; ahead <= 1; ahead++) {
                    BlockPos pos = BlockPos.containing(point).relative(facing, ahead);
                    if (state.departing.contains(pos) || !e.level().isLoaded(pos)) continue;
                    if (!(e.level().getBlockEntity(pos) instanceof PortableStorageInterfaceBlockEntity stationary)
                            || stationary.isPowered() || stationary.getBlockState().getBlock() != ctx.state.getBlock()
                            || stationary.getBlockState().getValue(PortableStorageInterfaceBlock.FACING) != facing.getOpposite()) continue;
                    var containing = dev.ryanhcode.sable.Sable.HELPER.getContaining(e.level(), pos);
                    if (containing != null) continue; // Native stationary-world docking; do not mistake our own plot for a dock.
                    var connected = ((dev.createsablecontraptions.mixin.PortableInterfaceAccessor) stationary).csc$connectedEntity();
                    if (connected != null && connected != owner && connected.isAlive()) continue;
                    nearest = Math.min(nearest, distance + .25);
                }
            }
        }
        return state.factor = state.ramp.update(nearest, nominal, !nowHeld.isEmpty());
    }
}
