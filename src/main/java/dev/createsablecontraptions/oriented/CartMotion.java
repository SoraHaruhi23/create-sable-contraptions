package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.mixin.ContraptionEntityAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Checks the actual vanilla move request, before either the carrier or its body advances. */
public final class CartMotion {
    public record Frame(Vec3 cartPosition,Vec3 rootPosition,OrientedContraptionEntity root) {}
    private static final Map<AbstractMinecart,Vec3> REJECTED=new WeakHashMap<>();
    private static final Set<AbstractMinecart> RAIL_MOVE=Collections.newSetFromMap(new WeakHashMap<>());
    private CartMotion() {}
    public static boolean enterRailMove(AbstractMinecart cart) { return RAIL_MOVE.add(cart); }
    public static void leaveRailMove(AbstractMinecart cart,boolean entered) { if(entered) RAIL_MOVE.remove(cart); }
    public static Frame begin(AbstractMinecart cart) {
        if(cart.level().isClientSide) return null;
        var root=CartImpulse.structure(cart); if(root==null) return null;
        cart.positionRider(root);
        CartTurning.begin(root,cart);
        CartDocking.tick(root);
        REJECTED.remove(cart);
        return new Frame(cart.position(),root.position(),root);
    }
    public static boolean reject(AbstractMinecart cart,Vec3 motion) {
        if(cart.level().isClientSide || motion.lengthSqr()<1e-16) return false;
        var root=CartImpulse.structure(cart); if(root==null) return false;
        Vec3 previous=root.position();
        boolean blocked;
        try {
            cart.positionRider(root);
            Vec3 testedMotion=motion;
            // Vanilla temporarily snaps Y to the rail block before Entity.move, then
            // restores getPos().y afterwards. That intermediate Y is not the body pose.
            if(RAIL_MOVE.contains(cart)) {
                var start=cart.getPos(cart.getX(),cart.getY(),cart.getZ());
                var end=cart.getPos(cart.getX()+motion.x,cart.getY()+motion.y,cart.getZ()+motion.z);
                if(start!=null) {
                    root.setPos(root.position().add(start.subtract(cart.position())));
                    if(end!=null) testedMotion=end.subtract(start);
                }
            }
            blocked=ChildMotion.cart(root,testedMotion);
        } finally { root.setPos(previous); }
        if(!blocked) return false;
        REJECTED.put(cart,motion);
        CartDocking.impulse(root,motion);
        stall(root);
        return true;
    }
    public static void end(AbstractMinecart cart,Frame frame) {
        if(frame==null || cart.isRemoved() || !frame.root.isAlive() || frame.root.getVehicle()!=cart
                || !ElevatorLink.managed(frame.root.getContraption())) { REJECTED.remove(cart); return; }
        var root=frame.root;
        var rejected=REJECTED.remove(cart);
        // Rails may adjust positions outside Entity.move (slope/track snapping). Check that delta too.
        root.setPos(frame.rootPosition);
        Vec3 displacement=cart.position().subtract(frame.cartPosition);
        if(rejected!=null || displacement.lengthSqr()>1e-16 && ChildMotion.cart(root,displacement)) {
            cart.setPos(frame.cartPosition);
            cart.setDeltaMovement(Vec3.ZERO); cart.hurtMarked=true;
            if(rejected==null) CartDocking.impulse(root,displacement);
            stall(root);
        } else if(displacement.lengthSqr()>1e-16 && ((ElevatorLink)root.getContraption()).csc$isBlocked()) {
            // Rail acceleration may reverse the cart AFTER the cached-cruise precheck.
            // A completed, swept move supersedes that rejected prediction.
            ((ElevatorLink)root.getContraption()).csc$setBlocked(false);
            boolean actorStalled=PhysicalFamily.actorStalled(root);
            root.getContraption().stalled=actorStalled;
            root.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(),actorStalled);
            ((ContraptionEntityAccessor)root).csc$onStalled();
            CartDocking.accepted(root,cart.getDeltaMovement());
        }
        cart.positionRider(root);
        OrientedBridge.target(root);
    }
    private static void stall(OrientedContraptionEntity root) {
        var link=(ElevatorLink)root.getContraption(); boolean previous=link.csc$isBlocked();
        link.csc$setBlocked(true);root.getContraption().stalled=true;
        root.getEntityData().set(ContraptionEntityAccessor.csc$stalledAccessor(),true);
        if(!previous)((ContraptionEntityAccessor)root).csc$onStalled();
    }
}
