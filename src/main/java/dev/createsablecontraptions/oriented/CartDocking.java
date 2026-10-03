package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.createsablecontraptions.docking.DockingMotion;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class CartDocking {
    private static final Map<OrientedContraptionEntity, State> STATES = new WeakHashMap<>();
    private static final class State { Vec3 cruise = Vec3.ZERO; double factor = 1; int resumeTicks; }
    private CartDocking() {}
    public static String describe(OrientedContraptionEntity e) {
        var s=STATES.get(e);
        return s==null?"无速度控制记录":"速度控制倍率="+s.factor+"；巡航速度="+s.cruise+"；恢复计时="+s.resumeTicks;
    }
    public static void impulse(OrientedContraptionEntity e, Vec3 velocity) {
        var state = STATES.computeIfAbsent(e, ignored -> new State());
        state.cruise = velocity;
    }
    public static void accepted(OrientedContraptionEntity e,Vec3 velocity) {
        var state=STATES.computeIfAbsent(e,ignored->new State());
        state.cruise=velocity; state.factor=1; state.resumeTicks=0;
    }
    public static void tick(OrientedContraptionEntity e) {
        if (e.level().isClientSide || !ElevatorLink.managed(e.getContraption()) || !(e.getVehicle() instanceof AbstractMinecart cart)) return;
        var s = STATES.computeIfAbsent(e, ignored -> new State());
        Vec3 incoming = cart.getDeltaMovement();
        var link = (ElevatorLink) e.getContraption();
        boolean wasBlocked = link.csc$isBlocked();
        if (!wasBlocked && s.factor >= .999 && s.resumeTicks == 0 || incoming.length() > s.cruise.length()
                || dev.createsablecontraptions.physics.CartDirection.reversed(incoming.x,incoming.y,incoming.z,s.cruise.x,s.cruise.y,s.cruise.z)) s.cruise = incoming;
        else if (incoming.lengthSqr() > 1e-8) s.cruise = incoming.normalize().scale(s.cruise.length());
        double factor = DockingMotion.linear(e, s.cruise);
        boolean blocked = ChildMotion.cart(e, s.cruise.scale(factor));
        // Create's cart controller may publish the previous stalled velocity for one more tick.
        if (wasBlocked && !blocked) s.resumeTicks = 2;
        link.csc$setBlocked(blocked);
        if (blocked || wasBlocked) {
            boolean stalled = blocked || PhysicalFamily.actorStalled(e);
            e.getContraption().stalled = stalled;
            e.getEntityData().set(dev.createsablecontraptions.mixin.ContraptionEntityAccessor.csc$stalledAccessor(), stalled);
            if (blocked != wasBlocked) ((dev.createsablecontraptions.mixin.ContraptionEntityAccessor) e).csc$onStalled();
        }
        if (blocked) factor = 0;
        if (factor < .999 || s.factor < .999 || s.resumeTicks > 0) {
            Vec3 motion = s.cruise.scale(factor);
            cart.setDeltaMovement(motion);
            cart.hurtMarked = true;
            var coupled = e.getCoupledCartsIfPresent();
            if (coupled != null) coupled.forEach(controller -> {
                controller.cart().setDeltaMovement(motion); controller.cart().hurtMarked = true;
            });
        }
        s.factor = factor;
        if (s.resumeTicks > 0) s.resumeTicks--;
    }
}
