package dev.createsablecontraptions.elevator;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Assembly collection must not run arbitrary storage mount callbacks against live inventories. */
public final class PendingPhysicalStorage extends MountedStorageManager {
    @Override public void addBlock(Level level, BlockState state, BlockPos global, BlockPos local, BlockEntity be) { }
}
