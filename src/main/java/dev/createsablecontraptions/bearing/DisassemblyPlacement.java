package dev.createsablecontraptions.bearing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Create's destination replacement rules, retaining physical inventories when placement is impossible. */
public final class DisassemblyPlacement {
    private DisassemblyPlacement() {}
    public static boolean canReplace(ServerLevel level,BlockPos pos,BlockState incoming) {
        var existing=level.getBlockState(pos);
        return existing.isAir() || existing.getDestroySpeed(level,pos)!=-1
                && !(incoming.getCollisionShape(level,pos).isEmpty() && !existing.getCollisionShape(level,pos).isEmpty());
    }
    public static void clear(ServerLevel level,BlockPos pos,BlockState incoming) {
        if(!canReplace(level,pos,incoming))throw new IllegalStateException("Destination changed during physical disassembly: "+pos);
        if(level.getBlockState(pos).isAir())return;
        boolean drops=!com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.noDropWhenContraptionReplaceBlocks.get();
        // A waterlogged obstacle may leave its fluid behind, just as in native Create.
        if(!level.destroyBlock(pos,drops) && !level.getBlockState(pos).isAir())
            throw new IllegalStateException("Destination removal failed: "+pos);
    }
}
