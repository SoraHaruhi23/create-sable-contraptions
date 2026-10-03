package dev.createsablecontraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createsablecontraptions.CreateSableContraptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CreateSableContraptions.ID)
public final class ElevatorEditing {
    private static final java.util.Map<ServerSubLevel, Boolean> RECONCILED = new java.util.WeakHashMap<>();
    private ElevatorEditing() {}

    @SubscribeEvent
    public static void beforeBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) ElevatorActors.beforePlayerBreak(level, event.getPos());
    }

    public static void changed(ServerSubLevel sub, BlockPos pos, BlockState state) {
        if (!ElevatorBridge.managed(sub)) return;
        var tag = ElevatorBridge.data(sub);
        var local = pos.subtract(BlockPos.of(tag.getLong("PlotAnchor")));
        tag.putLongArray("Blocks", LiveBlockIndex.update(tag.getLongArray("Blocks"), local.asLong(), !state.isAir()));
    }

    public static boolean valid(ServerLevel level, ServerSubLevel sub, com.simibubi.create.content.contraptions.Contraption elevator) {
        if (!RECONCILED.containsKey(sub) && !reconcile(sub)) return false;
        return ElevatorBridge.data(sub).getLongArray("Blocks").length != 0;
    }

    public static boolean contactIntact(ServerLevel level, ElevatorContraption elevator) {
        var sub = ElevatorBridge.resolve(level, elevator);
        if (sub == null) return false;
        var anchor = BlockPos.of(ElevatorBridge.data(sub).getLong("PlotAnchor"));
        // Ordinary redstone/state changes are valid; only losing the original column contact stops travel.
        for (var info : elevator.getBlocks().values())
            if (AllBlocks.REDSTONE_CONTACT.has(info.state()) && !ElevatorBridge.samePlatformState(level.getBlockState(anchor.offset(info.pos())), info.state())) return false;
        return true;
    }

    /** Rebuild once after loading, and before dismantling, from real nonempty plot sections. */
    public static boolean reconcile(ServerSubLevel sub) {
        var tag = ElevatorBridge.data(sub);
        var anchor = BlockPos.of(tag.getLong("PlotAnchor"));
        for (long packed : tag.getLongArray("Blocks"))
            if (!sub.getLevel().isLoaded(anchor.offset(BlockPos.of(packed)))) return false;
        var positions = new java.util.ArrayList<Long>();
        for (var holder : sub.getPlot().getLoadedChunks()) {
            var chunk = holder.getChunk();
            if (chunk == null) return false;
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i];
                if (section.hasOnlyAir()) continue;
                int baseY = chunk.getSectionYFromSectionIndex(i) * 16;
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    if (section.getBlockState(x, y, z).isAir()) continue;
                    var local = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y,
                            chunk.getPos().getMinBlockZ() + z).subtract(anchor);

                    positions.add(local.asLong());
                }
            }
        }
        if (positions.isEmpty()) return false;
        tag.putLongArray("Blocks", positions);
        RECONCILED.put(sub, true);
        return true;
    }

}

