package dev.createsablecontraptions.client;

import com.simibubi.create.content.contraptions.actors.psi.*;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.*;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.*;
import org.joml.Vector3d;

/** The actor owns both renderer backends; use Sable's pose and the dock's synced timer. */
public final class PortableAnimation {
    private PortableAnimation() {}
    public static boolean tick(MovementContext ctx) {
        if (!ctx.world.isClientSide || ctx.contraption.entity == null || !ElevatorLink.managed(ctx.contraption)) return false;
        if (!(ElevatorActors.sub(ctx.contraption.entity) instanceof ClientSubLevel sub)) return false;
        var pose = sub.renderPose(1);
        var localFacing = ctx.state.getValue(PortableStorageInterfaceBlock.FACING).getNormal();
        var normal = pose.orientation().transform(new Vector3d(localFacing.getX(), localFacing.getY(), localFacing.getZ()));
        var facing = Direction.getNearest(normal.x, normal.y, normal.z);
        var plot = sub.getPlot().getCenterBlock().offset(ctx.localPos);
        var point = pose.transformPosition(new Vector3d(plot.getX() + .5 + localFacing.getX() * 1.85,
                plot.getY() + .5 + localFacing.getY() * 1.85, plot.getZ() + .5 + localFacing.getZ() * 1.85));
        var animation = PortableStorageInterfaceMovement.getAnimation(ctx);
        if (!ctx.disabled && normal.distance(new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ())) <= .5)
            for (int i = 0; i < 2; i++) {
                var pos = BlockPos.containing(point.x, point.y, point.z).relative(facing, i);
                if (ctx.world.getBlockEntity(pos) instanceof PortableStorageInterfaceBlockEntity dock
                        && dock.getBlockState().getBlock() == ctx.state.getBlock()
                        && dock.getBlockState().getValue(PortableStorageInterfaceBlock.FACING) == facing.getOpposite()) {
                    // Transfer timeout does not imply disconnection: Create can hold at
                    // ANIMATION while the actor keeps the stationary interface alive.
                    // Mirror its actual extension, including connection/retraction phases.
                    float extension = ((dev.createsablecontraptions.mixin.PortableInterfaceAccessor) dock)
                            .csc$extensionDistance(1);
                    animation.setValue(extension);
                    return true;
                }
            }
        animation.chase(0, .25f, Chaser.LINEAR);
        animation.tickChaser();
        return true;
    }
}
