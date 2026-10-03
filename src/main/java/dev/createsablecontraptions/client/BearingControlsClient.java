package dev.createsablecontraptions.client;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.sync.ContraptionInteractionPacket;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;

/** Non-elevator physical controls toggle Create's filtered movement actors. */
public final class BearingControlsClient {
    private BearingControlsClient() {}
    private record Control(AbstractContraptionEntity entity, BlockPos local, ContraptionControlsBlockEntity display) {}
    private static Control resolve(BlockPos physical) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !AllBlocks.CONTRAPTION_CONTROLS.has(mc.level.getBlockState(physical))) return null;
        var sub = Sable.HELPER.getContaining(mc.level, physical);
        if (sub == null) return null;
        for (var candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof AbstractContraptionEntity entity) || !entity.isAlive()
                    || !ElevatorLink.managed(entity.getContraption())
                    || entity.getContraption() instanceof com.simibubi.create.content.contraptions.elevator.ElevatorContraption
                    || !sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) continue;
            var local = physical.subtract(sub.getPlot().getCenterBlock());
            if (entity.getContraption().getActorAt(local) == null) return null;
            var be = entity.getContraption().getOrCreateClientContraptionLazy().getBlockEntity(local);
            if (be instanceof ContraptionControlsBlockEntity controls) return new Control(entity, local, controls);
        }
        return null;
    }
    public static boolean use(BlockHitResult hit, InputEvent.InteractionKeyMappingTriggered event) {
        var control = resolve(hit.getBlockPos());
        if (control == null) return false;
        if (control.entity.handlePlayerInteraction(Minecraft.getInstance().player, control.local, hit.getDirection(), event.getHand()))
            CatnipServices.NETWORK.sendToServer(new ContraptionInteractionPacket(control.entity, event.getHand(), control.local, hit.getDirection()));
        event.setCanceled(true); event.setSwingHand(false);
        return true;
    }
    public static void prepareDisplay(ContraptionControlsBlockEntity physical, float partialTick) {
        var control = resolve(physical.getBlockPos());
        if (control == null) return;
        physical.button.setValue(control.display.button.getValue(partialTick));
        physical.indicator.setValue(control.display.indicator.getValue(partialTick));
    }
}
