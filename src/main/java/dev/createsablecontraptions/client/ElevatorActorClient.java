package dev.createsablecontraptions.client;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.createsablecontraptions.elevator.ElevatorActors;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "create_sable_contraptions", value = Dist.CLIENT)
public final class ElevatorActorClient {
    private static final java.util.Set<BlockEntity> REMOVED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private ElevatorActorClient() {}
    public static boolean movingRendererOwns(BlockEntity be) {
        var behaviour = MovementBehaviour.REGISTRY.get(be.getBlockState());
        return behaviour != null && behaviour.disableBlockEntityRendering() && ElevatorActors.physicalActor(be);
    }
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() % 5 != 0 || !VisualizationManager.supportsVisualization(level)) return;
        for (var candidate : level.entitiesForRendering()) {
            if (!(candidate instanceof AbstractContraptionEntity entity) || !ElevatorLink.managed(entity.getContraption())) continue;
            var sub = ElevatorActors.sub(entity); if (sub == null) continue;
            for (var actor : entity.getContraption().getActors()) {
                var be = level.getBlockEntity(sub.getPlot().getCenterBlock().offset(actor.left.pos()));
                if (be != null && movingRendererOwns(be) && REMOVED.add(be))
                    VisualizationManager.get(level).blockEntities().queueRemove(be);
            }
        }
    }
}
