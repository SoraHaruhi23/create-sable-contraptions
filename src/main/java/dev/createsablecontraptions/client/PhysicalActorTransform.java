package dev.createsablecontraptions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.ElevatorActors;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/** Both renderer backends use the same physical local-to-world transform as Sable's blocks. */
public final class PhysicalActorTransform {
    private PhysicalActorTransform() {}
    public static boolean apply(PoseStack stack, AbstractContraptionEntity entity, float partialTick,
                                 double originX, double originY, double originZ) {
        if (!(ElevatorActors.sub(entity) instanceof ClientSubLevel sub) || sub.isRemoved()) return false;
        var pose = sub.renderPose(partialTick);
        var anchor = sub.getPlot().getCenterBlock();
        var position = pose.transformPosition(new Vector3d(anchor.getX(), anchor.getY(), anchor.getZ()));
        stack.translate(position.x - originX, position.y - originY, position.z - originZ);
        stack.mulPose(new Quaternionf(pose.orientation()));
        var scale = pose.scale();
        stack.scale((float) scale.x(), (float) scale.y(), (float) scale.z());
        return true;
    }
}
