package dev.createsablecontraptions.client;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.ElevatorActors;
import dev.createsablecontraptions.oriented.CartImpulse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Vector3d;

/** Render cart and carried blocks from one interpolation clock, without moving gameplay entities. */
public final class CartRenderAlignment {
    private CartRenderAlignment() {}
    public static Vec3 offset(AbstractMinecart cart,float pt) {
        var entity=CartImpulse.attachedStructure(cart);
        if(entity==null || !(ElevatorActors.sub(entity) instanceof ClientSubLevel sub) || sub.isRemoved()) return Vec3.ZERO;
        var local=CartImpulse.anchorOffset(entity,cart);
        if(local==null)return Vec3.ZERO;
        var anchor=sub.getPlot().getCenterBlock();
        var point=sub.renderPose(pt).transformPosition(new Vector3d(anchor.getX()+.5+local.x,anchor.getY()+.5+local.y,anchor.getZ()+.5+local.z));
        var mountOffset=cart.getPassengerRidingPosition(entity).subtract(cart.position()).subtract(entity.getVehicleAttachmentPoint(cart));
        var desired=new Vec3(point.x,point.y-.5,point.z).subtract(mountOffset);
        var interpolated=new Vec3(Mth.lerp(pt,cart.xOld,cart.getX()),Mth.lerp(pt,cart.yOld,cart.getY()),Mth.lerp(pt,cart.zOld,cart.getZ()));
        var nativeOrigin=interpolated;
        var rail=cart.getPos(interpolated.x,interpolated.y,interpolated.z);
        if(rail!=null) {
            var front=cart.getPosOffs(interpolated.x,interpolated.y,interpolated.z,.3);
            var back=cart.getPosOffs(interpolated.x,interpolated.y,interpolated.z,-.3);
            nativeOrigin=new Vec3(rail.x,((front==null?rail:front).y+(back==null?rail:back).y)/2,rail.z);
        }
        return desired.subtract(nativeOrigin);
    }
}
