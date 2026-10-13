package dev.createsablecontraptions.elevator;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;

/** Create controllers stay in their source plot; physical bodies use world coordinates. */
public final class AssemblyFrame {
    private AssemblyFrame() {}
    public static boolean usable(SubLevel parent) {
        return parent == null || (!parent.isRemoved()
                && parent.logicalPose().scale().distanceSquared(new org.joml.Vector3d(1, 1, 1)) <= 1e-12);
    }

    /** Check before transferring blocks: a live controller cannot change plots safely. */
    public static void validateControllers(net.minecraft.server.level.ServerLevel level,
            com.simibubi.create.content.contraptions.Contraption contraption, BlockPos offset)
            throws com.simibubi.create.content.contraptions.AssemblyException {
        var controllers = new java.util.ArrayList<com.simibubi.create.content.contraptions.IControlContraption>();
        for (var local : contraption.getBlocks().keySet()) {
            var be = level.getBlockEntity(contraption.anchor.offset(offset).offset(local));
            if (be instanceof com.simibubi.create.content.contraptions.IControlContraption controller)
                controllers.add(controller);
        }
        if (controllers.isEmpty()) return;
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity child)
                    || entity.isRemoved()) continue;
            for (var controller : controllers) if (controller.isAttachedTo(child))
                throw new com.simibubi.create.content.contraptions.AssemblyException(
                        net.minecraft.network.chat.Component.translatable("csc.assembly.active_inner"));
        }
    }
    public static SubLevel at(Level level, Vec3 point) {
        return Sable.HELPER.getContaining(level, BlockPos.containing(point));
    }
    public static Vec3 world(SubLevel parent, Vec3 point) {
        return parent == null ? point : parent.logicalPose().transformPosition(point);
    }
    public static Vec3 direction(SubLevel parent, Vec3 direction) {
        if (parent == null) return direction;
        var v=parent.logicalPose().orientation().transform(new org.joml.Vector3d(direction.x,direction.y,direction.z));
        return new Vec3(v.x,v.y,v.z);
    }
    public static Vec3 localDirection(SubLevel parent, Vec3 direction) {
        if (parent == null) return direction;
        var v = new Quaterniond(parent.logicalPose().orientation()).conjugate()
                .transform(new org.joml.Vector3d(direction.x, direction.y, direction.z));
        return new Vec3(v.x, v.y, v.z);
    }
    public static Quaterniond rotation(SubLevel parent, Quaterniond local) {
        return parent == null ? local : new Quaterniond(parent.logicalPose().orientation()).mul(local);
    }
    public static ServerSubLevel parent(ServerSubLevel child) {
        if (!ElevatorBridge.managed(child)) return null;
        var data=ElevatorBridge.data(child);
        if (!data.hasUUID("ParentSubLevel")) return null;
        var parent=SubLevelContainer.getContainer(child.getLevel()).getSubLevel(data.getUUID("ParentSubLevel"));
        return parent instanceof ServerSubLevel server && !server.isRemoved() ? server : null;
    }
    public static boolean available(ServerSubLevel child) {
        return dev.createsablecontraptions.physics.ParentFrames.validChain(child, AssemblyFrame::parent,
                current -> usable(current) && (!ElevatorBridge.managed(current)
                        || !ElevatorBridge.data(current).hasUUID("ParentSubLevel") || parent(current) != null));
    }
    public static boolean descendant(SubLevel candidate, ServerSubLevel ancestor) {
        return candidate instanceof ServerSubLevel server && ElevatorBridge.managed(server)
                && dev.createsablecontraptions.physics.ParentFrames.descendant(server, ancestor, AssemblyFrame::parent);
    }
    public static boolean hasChildren(ServerSubLevel parent) {
        return SubLevelContainer.getContainer(parent.getLevel()).getAllSubLevels().stream()
                .anyMatch(child -> !child.isRemoved() && descendant(child, parent));
    }
    public static boolean destination(ServerSubLevel child, BlockPos pos) {
        if (!available(child)) return false;
        return Sable.HELPER.getContaining(child.getLevel(),pos)==parent(child);
    }
    public static boolean withinWorld(ServerSubLevel child, BlockPos pos) {
        var worldPos = BlockPos.containing(world(parent(child), pos.getCenter()));
        return !child.getLevel().isOutsideBuildHeight(worldPos)
                && child.getLevel().getWorldBorder().isWithinBounds(worldPos);
    }
}
