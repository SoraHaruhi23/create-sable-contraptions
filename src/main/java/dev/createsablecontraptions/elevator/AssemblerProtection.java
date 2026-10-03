package dev.createsablecontraptions.elevator;

import com.simibubi.create.content.contraptions.ContraptionHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Only CSC-owned plots are protected; ordinary Sable bodies keep their assembler. */
public final class AssemblerProtection {
    private AssemblerProtection() {}

    public static boolean managed(Level level, BlockPos pos) {
        if (level == null) return false;
        var sub = Sable.HELPER.getContaining(level, pos);
        if (sub == null) return false;
        // Persistent ownership also protects plots while their logical entity is loading.
        if (sub instanceof ServerSubLevel server && ElevatorBridge.managed(server)) return true;
        for (var ref : ContraptionHandler.loadedContraptions.get(level).values()) {
            var entity = ref.get();
            if (entity != null && ElevatorLink.managed(entity.getContraption())
                    && sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) return true;
        }
        return false;
    }

    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock());
        if (!id.toString().equals("simulated:physics_assembler") || !managed(level, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!level.isClientSide) event.getEntity().displayClientMessage(
                Component.translatable("csc.assembly.assembler_protected"), true);
    }
}
