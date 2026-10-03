package dev.createsablecontraptions.client;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.ElevatorActors;
import dev.createsablecontraptions.oriented.CartImpulse;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3d;

/** Align the client collision entity as well as its renderer to the authoritative carried body. */
@EventBusSubscriber(modid="create_sable_contraptions",value=Dist.CLIENT)
public final class CartClientSync {
    private CartClientSync() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        for(var e:level.entitiesForRendering()) {
            if(!(e instanceof AbstractMinecart cart))continue;
            var root=CartImpulse.structure(cart);
            if(root==null || !(ElevatorActors.sub(root) instanceof ClientSubLevel sub) || sub.isRemoved())continue;
            var offset=cart.getPassengerRidingPosition(root).subtract(cart.position()).subtract(root.getVehicleAttachmentPoint(cart));
            var previous=position(sub,0).subtract(offset);
            var current=position(sub,1).subtract(offset);
            cart.setPos(current);
            cart.xOld=cart.xo=previous.x;cart.yOld=cart.yo=previous.y;cart.zOld=cart.zo=previous.z;
            for(var passenger:cart.getPassengers())cart.positionRider(passenger);
            dev.createsablecontraptions.oriented.OrientedBridge.bounds(root);
        }
    }
    private static Vec3 position(ClientSubLevel sub,float partialTick) {
        var anchor=sub.getPlot().getCenterBlock();
        var p=sub.renderPose(partialTick).transformPosition(new Vector3d(anchor.getX()+.5,anchor.getY()+.5,anchor.getZ()+.5));
        return new Vec3(p.x,p.y-.5,p.z);
    }
}
