package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.mixin.ContraptionEntityAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.util.*;

/** Captures the carrier's pre-move pose, checks orientation before actors run. */
public final class CartTurning {
    private record Carrier(java.lang.ref.WeakReference<AbstractMinecart> cart,Vec3 position) {}
    private record Frame(Vec3 position,List<Carrier> carriers,float yaw,float pitch,Quaterniond rotation) {}
    private record Pending(float yaw,float pitch) {}
    private static final Map<OrientedContraptionEntity,Frame> FRAMES=new WeakHashMap<>();
    private static final Map<OrientedContraptionEntity,Pending> PENDING=new WeakHashMap<>();
    private CartTurning() {}
    public static void begin(OrientedContraptionEntity e,AbstractMinecart cart) {
        var carriers=new ArrayList<Carrier>();
        carriers.add(new Carrier(new java.lang.ref.WeakReference<>(cart),cart.position()));
        var coupled=e.getCoupledCartsIfPresent();
        if(coupled!=null)coupled.forEach(controller -> {
            var other=controller.cart();
            if(other!=cart)carriers.add(new Carrier(new java.lang.ref.WeakReference<>(other),other.position()));
        });
        FRAMES.put(e,new Frame(e.position(),carriers,e.yaw,e.pitch,rotation(e)));
    }
    public static boolean check(OrientedContraptionEntity e) {
        if (e.level().isClientSide || !ElevatorLink.managed(e.getContraption()) || !(e.getVehicle() instanceof AbstractMinecart cart)) return false;
        var frame=FRAMES.remove(e); if(frame==null)return false;
        var pending=PENDING.get(e);
        if(pending!=null && e.yaw==frame.yaw && e.pitch==frame.pitch) { e.yaw=pending.yaw;e.pitch=pending.pitch; }
        var to=rotation(e);
        if(pending==null && java.lang.Math.abs(frame.rotation.dot(to))>1-1e-12)return false;
        var prior=dev.createsablecontraptions.physics.CollisionState.get(e);
        boolean alreadyBlocked=((ElevatorLink)e.getContraption()).csc$isBlocked();
        if(ChildMotion.cartTurn(e,frame.position,frame.rotation,to)) {
            PENDING.put(e,new Pending(e.yaw,e.pitch));
            e.yaw=frame.yaw;e.pitch=frame.pitch;
            if(cart.getDeltaMovement().lengthSqr()>1e-12)CartDocking.impulse(e,cart.getDeltaMovement());
            for(var carrier:frame.carriers) {
                var member=carrier.cart.get();
                if(member==null || member.isRemoved())continue;
                member.setPos(carrier.position);member.setDeltaMovement(Vec3.ZERO);member.hurtMarked=true;
            }
            cart.positionRider(e);
            setBlocked(e,true);
            OrientedBridge.target(e);
            return true;
        }
        if(alreadyBlocked && prior!=null) {
            // A successful angular retry must not erase a translation/budget stop.
            dev.createsablecontraptions.physics.CollisionState.restore(e,prior);
            PENDING.remove(e);
        } else if(PENDING.remove(e)!=null)setBlocked(e,false);
        return false;
    }
    private static void setBlocked(OrientedContraptionEntity e,boolean blocked) {
        ((ElevatorLink)e.getContraption()).csc$setBlocked(blocked);
        boolean stalled=blocked||PhysicalFamily.actorStalled(e);
        e.getContraption().stalled=stalled;
        e.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(),stalled);
        ((ContraptionEntityAccessor)e).csc$onStalled();
    }
    private static Quaterniond rotation(OrientedContraptionEntity e) {
        var x=e.applyRotation(new Vec3(1,0,0),1);var y=e.applyRotation(new Vec3(0,1,0),1);var z=e.applyRotation(new Vec3(0,0,1),1);
        return dev.createsablecontraptions.physics.RotatingFrame.fromBasis(new Vector3d(x.x,x.y,x.z),new Vector3d(y.x,y.y,y.z),new Vector3d(z.x,z.y,z.z)).normalize();
    }
}
