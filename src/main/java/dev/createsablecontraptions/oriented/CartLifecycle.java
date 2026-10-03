package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.createsablecontraptions.elevator.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.util.Unit;
import java.util.*;

/** A destroyed carrier must not leave an immortal proxy or delete its live inventories. */
public final class CartLifecycle {
    private static final Set<AbstractContraptionEntity> RELEASING = Collections.newSetFromMap(new IdentityHashMap<>());
    private CartLifecycle() {}
    public static boolean releasing(AbstractContraptionEntity e) { return RELEASING.contains(e); }
    public static void tick(OrientedContraptionEntity e) {
        if (e.level().isClientSide || !e.isAlive() || !(e.getContraption() instanceof MountedContraption)
                || !ElevatorLink.managed(e.getContraption()) || releasing(e)) return;
        if (e.getVehicle() instanceof AbstractMinecart cart && cart.isRemoved()
                || e.getVehicle() == null && e.tickCount > 100) release(e);
    }
    public static void release(AbstractContraptionEntity root) {
        if (!(root.level() instanceof ServerLevel level) || releasing(root)) return;
        var family = new ArrayList<AbstractContraptionEntity>(); collect(root,family);
        RELEASING.addAll(family);
        try {
            for (var e : family) {
                var sub = ElevatorBridge.resolve(level,e.getContraption());
                if (sub != null && !sub.isRemoved()) {
                    // Flush actor-held tools only while their real storage is still available.
                    ElevatorActors.prepare(e);
                    e.getContraption().stop(level);
                    ElevatorActors.commit(e);
                    removeVirtualAnchors(e,sub);
                    ElevatorPhysics.release(sub);
                    SubLevelContainer.getContainer(level).removeForceLoadTicket(sub,ElevatorPhysics.TICKET,Unit.INSTANCE);
                    var data = sub.getUserDataTag().copy(); data.remove(ElevatorBridge.TAG); sub.setUserDataTag(data);
                }
                ((dev.createsablecontraptions.mixin.ContraptionEntityAccessor)e).csc$skipActorStop(true);
                ((ElevatorLink)e.getContraption()).csc$setSubLevel(null);
            }
            // Never place a stale proxy snapshot or delete the retained Sable bodies.
            for (var e : family) e.discard();
        } finally { RELEASING.removeAll(family); }
    }
    private static void removeVirtualAnchors(AbstractContraptionEntity e,dev.ryanhcode.sable.sublevel.ServerSubLevel sub) {
        for (var local : java.util.List.copyOf(e.getContraption().getBlocks().keySet()))
            if (OrientedBridge.virtualAnchor(e.getContraption(),local))
                e.level().removeBlock(sub.getPlot().getCenterBlock().offset(local),false);
    }
    private static void collect(AbstractContraptionEntity e,List<AbstractContraptionEntity> family) {
        family.add(e);
        for (var p:e.getPassengers()) if (p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption())) collect(child,family);
    }
}
