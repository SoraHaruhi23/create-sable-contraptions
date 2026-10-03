package dev.createsablecontraptions.client;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.mixin.ControlledContraptionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.Map;

@EventBusSubscriber(modid = "create_sable_contraptions", value = Dist.CLIENT)
public final class BearingStallEffects {
    private static final Map<KineticBlockEntity, Boolean> LAST = new java.util.WeakHashMap<>();
    private BearingStallEffects() {}
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) { LAST.clear(); return; }
        var current = new java.util.IdentityHashMap<KineticBlockEntity, Boolean>();
        for (var candidate : level.entitiesForRendering()) {
            if (!(candidate instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity entity) || !entity.isAlive()
                    || !ElevatorLink.managed(entity.getContraption())
                    || entity.getContraption() instanceof com.simibubi.create.content.contraptions.elevator.ElevatorContraption) continue;
            net.minecraft.core.BlockPos pos;
            if (entity instanceof ControlledContraptionEntity) pos = ((ControlledContraptionAccessor) entity).csc$controllerPos();
            else if (entity.getContraption() instanceof com.simibubi.create.content.contraptions.gantry.GantryContraption gantry)
                pos = net.minecraft.core.BlockPos.containing(entity.getAnchorVec().add(.5, .5, .5)).relative(gantry.getFacing().getOpposite());
            else continue;
            if (pos == null) continue;
            var be = level.getBlockEntity(pos);
            if (!(be instanceof KineticBlockEntity)) continue;
            // A clock has two entities but one controller: either blocked hand produces one effect.
            current.merge((KineticBlockEntity) be, entity.isStalled(), (a, b) -> a || b);
        }
        LAST.keySet().removeIf(be -> be.getLevel() != level || be.isRemoved() || !current.containsKey(be));
        current.forEach((be, blocked) -> {
            boolean before = LAST.getOrDefault(be, false);
            if (before != blocked)
                new KineticEffectHandler(be).spawnEffect(blocked ? ParticleTypes.SMOKE : ParticleTypes.CLOUD,
                        blocked ? .2f : .075f, blocked ? 5 : 2);
            LAST.put(be, blocked);
        });
    }
}
