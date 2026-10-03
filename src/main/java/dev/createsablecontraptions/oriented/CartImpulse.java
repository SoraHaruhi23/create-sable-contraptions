package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.minecart.MinecartSim2020;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.physics.CartPush;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CartImpulse {
    private CartImpulse() {}
    public static OrientedContraptionEntity structure(AbstractMinecart cart) {
        for (var p : cart.getPassengers())
            if (p instanceof OrientedContraptionEntity e && e.getContraption() instanceof MountedContraption && ElevatorLink.managed(e.getContraption())) return e;
        return null;
    }
    public static boolean apply(ServerSubLevel sub,Vector3dc localImpulse) {
        if (!ElevatorBridge.managed(sub)) return false;
        for (var ref:ContraptionHandler.loadedContraptions.get(sub.getLevel()).values()) {
            var entity=ref.get();
            if (entity==null || !ElevatorLink.managed(entity.getContraption())
                    || !sub.getUniqueId().equals(((ElevatorLink)entity.getContraption()).csc$getSubLevel())) continue;
            while (entity.getVehicle() instanceof AbstractContraptionEntity parent) entity=parent;
            if (!(entity instanceof OrientedContraptionEntity root) || !(root.getVehicle() instanceof AbstractMinecart cart) || cart.isRemoved()) return false;
            var impulse=sub.logicalPose().orientation().transform(new Vector3d(localImpulse));
            double mass=Math.max(1,mass(root));
            var delta=new Vec3(CartPush.velocity(impulse.x,mass),0,CartPush.velocity(impulse.z,mass));
            var coupled=root.getCoupledCartsIfPresent();
            if (coupled == null) push(cart,delta);
            else coupled.forEach(c -> push(c.cart(),delta));
            CartDocking.impulse(root,cart.getDeltaMovement());
            return true;
        }
        return false;
    }
    private static double mass(AbstractContraptionEntity e) {
        var sub=ElevatorActors.sub(e); double mass=sub instanceof ServerSubLevel server?server.getMassTracker().getMass():0;
        for(var p:e.getPassengers()) if(p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption())) mass+=mass(child);
        return mass;
    }
    private static void push(AbstractMinecart cart,Vec3 delta) {
        var pos=cart.getCurrentRailPosition(); var state=cart.level().getBlockState(pos);
        if(state.getBlock() instanceof BaseRailBlock rail) {
            var direction=MinecartSim2020.getRailVec(rail.getRailDirection(state,cart.level(),pos,cart));
            delta=direction.scale(delta.dot(direction));
        }
        cart.setDeltaMovement(cart.getDeltaMovement().add(delta)); cart.hurtMarked=true;
    }
}
