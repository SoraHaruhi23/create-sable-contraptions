package dev.createsablecontraptions.elevator;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createsablecontraptions.CreateSableContraptions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;

/** Hard world constraint + authoritative target pose, rebuilt from Sable's saved user data. */
public final class ElevatorPhysics {
    public static final SubLevelLoadingTicketType<Unit> TICKET = SubLevelLoadingTicketType.create(
            ResourceLocation.fromNamespaceAndPath(CreateSableContraptions.ID, "elevator"), Unit.CODEC);
    private static final Map<ServerSubLevel, GenericConstraintHandle> JOINTS = new IdentityHashMap<>();
    private ElevatorPhysics() {}
    public static void registerTicket() { /* initializes the ticket codec before worlds load */ }

    public static void target(ServerSubLevel sub, Vec3 anchor) {
        var data = ElevatorBridge.data(sub);
        data.putDouble("X", anchor.x); data.putDouble("Y", anchor.y); data.putDouble("Z", anchor.z);
        // Queue only. Sable must snapshot the previous pose before we move the body.
        // Teleporting here (during the pulley's BE tick) erases the displacement from
        // lastPose -> logicalPose, leaving riders to be pushed out of an ascending floor.
    }

    public static void hold(ServerSubLevel sub) {
        if (sub.isRemoved()) return;
        var data = ElevatorBridge.data(sub);
        BlockPos plotAnchor = BlockPos.of(data.getLong("PlotAnchor"));
        var pipeline = SubLevelContainer.getContainer(sub.getLevel()).physicsSystem().getPipeline();
        Vector3d localAnchor = new Vector3d(plotAnchor.getX() + .5, plotAnchor.getY() + .5, plotAnchor.getZ() + .5);
        Vector3d worldAnchor = new Vector3d(data.getDouble("X") + .5, data.getDouble("Y") + .5, data.getDouble("Z") + .5);
        Quaterniond rotation = data.contains("QW") ? new Quaterniond(data.getDouble("QX"), data.getDouble("QY"),
                data.getDouble("QZ"), data.getDouble("QW")).normalize() : new Quaterniond();
        var joint = JOINTS.get(sub);
        if (joint == null || !joint.isValid()) {
            joint = pipeline.addConstraint(sub, null, new GenericConstraintConfiguration(localAnchor, worldAnchor,
                    new Quaterniond(), rotation, EnumSet.allOf(ConstraintJointAxis.class)));
            if (joint == null || !joint.isValid()) throw new IllegalStateException("Sable physics backend must support generic constraints");
            JOINTS.put(sub, joint);
        }
        joint.setFrame2(worldAnchor, rotation);
        // Sable stores the body's position at its rotation point (normally the center of mass).
        Vector3d position = dev.createsablecontraptions.physics.RotatingFrame.bodyPosition(
                sub.logicalPose().rotationPoint(), localAnchor, worldAnchor, rotation);
        pipeline.teleport(sub, position, rotation);
        RigidBodyHandle handle = RigidBodyHandle.of(sub);
        handle.addLinearAndAngularVelocity(handle.getLinearVelocity(new Vector3d()).negate(),
                handle.getAngularVelocity(new Vector3d()).negate());
        sub.logicalPose().position().set(position);
        sub.logicalPose().orientation().set(rotation);
        // updatePose ran before the post-physics correction. Publish the corrected
        // displacement too, rather than the constraint solver's intermediate velocity.
        position.sub(sub.lastPose().transformPosition(new Vector3d(sub.logicalPose().rotationPoint())), sub.latestLinearVelocity).mul(20);
        Quaterniond delta = new Quaterniond(rotation).mul(new Quaterniond(sub.lastPose().orientation()).conjugate()).normalize();
        if (delta.w < 0) delta.set(-delta.x, -delta.y, -delta.z, -delta.w);
        double sin = Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
        sub.latestAngularVelocity.set(delta.x, delta.y, delta.z).mul(sin < 1e-12 ? 40 : 40 * Math.atan2(sin, delta.w) / sin);
        sub.updateBoundingBox();
    }

    public static void beforePhysics(ForgeSablePrePhysicsTickEvent event) {
        var container = SubLevelContainer.getContainer(event.getPhysicsSystem().getLevel());
        // All controller/entity ticks have finished: submit one coherent family pose.
        var level = event.getPhysicsSystem().getLevel();
        for (var ref : java.util.List.copyOf(com.simibubi.create.content.contraptions.ContraptionHandler.loadedContraptions.get(level).values())) {
            var entity = ref.get();
            // A ghost still riding a removed cart may no longer receive passenger ticks.
            if (entity instanceof com.simibubi.create.content.contraptions.OrientedContraptionEntity oriented)
                dev.createsablecontraptions.oriented.CartLifecycle.tick(oriented);
            if (entity != null && ElevatorLink.managed(entity.getContraption())
                    && !(entity.getVehicle() instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity)) {
                if (entity instanceof com.simibubi.create.content.contraptions.OrientedContraptionEntity oriented
                        && entity.getVehicle() instanceof net.minecraft.world.entity.vehicle.AbstractMinecart cart && !cart.isRemoved()) {
                    cart.positionRider(entity);
                    dev.createsablecontraptions.oriented.OrientedBridge.target(oriented);
                }
                dev.createsablecontraptions.oriented.OrientedBridge.children(entity);
            }
        }
        JOINTS.entrySet().removeIf(entry -> entry.getKey().isRemoved());
        for (ServerSubLevel sub : container.getAllSubLevels()) if (ElevatorBridge.managed(sub)) hold(sub);
    }

    public static void afterPhysics(ForgeSablePostPhysicsTickEvent event) {
        var container = SubLevelContainer.getContainer(event.getPhysicsSystem().getLevel());
        for (ServerSubLevel sub : container.getAllSubLevels()) if (ElevatorBridge.managed(sub)) hold(sub);
        for(var ref:com.simibubi.create.content.contraptions.ContraptionHandler.loadedContraptions.get(event.getPhysicsSystem().getLevel()).values()) {
            var entity=ref.get();
            if(entity instanceof com.simibubi.create.content.contraptions.OrientedContraptionEntity && ElevatorLink.managed(entity.getContraption()))
                dev.createsablecontraptions.oriented.OrientedBridge.bounds(entity);
        }
    }

    public static void release(ServerSubLevel sub) {
        var joint = JOINTS.remove(sub);
        if (joint != null && joint.isValid()) joint.remove();
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        // Native scene may already be disposed; do not dereference its handles on unload.
        JOINTS.keySet().removeIf(sub -> sub.getLevel() == event.getLevel());
    }
}
