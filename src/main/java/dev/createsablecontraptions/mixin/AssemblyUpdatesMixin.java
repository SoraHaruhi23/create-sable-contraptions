package dev.createsablecontraptions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.createsablecontraptions.elevator.AssemblyUpdates;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class AssemblyUpdatesMixin {
    @WrapOperation(method = "moveBlocks", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper;markAndNotifyBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;II)V"))
    private static void csc$notifyAfterCommit(Level level, BlockPos pos, LevelChunk chunk, BlockState oldState,
            BlockState state, int flags, int recursion, Operation<Void> original) {
        var stablePos = pos.immutable();
        if (AssemblyUpdates.defer(() -> {
            try {
                SubLevelAssemblyHelper.markAndNotifyBlock(level, stablePos, chunk, oldState, state, flags, recursion);
            } catch (Exception failure) {
                // Sable's original moveBlocks also logs each notification failure and continues.
                // The transfer is committed: do not orphan it by reporting a new assembly failure.
                dev.ryanhcode.sable.Sable.LOGGER.error("Failed to notify committed assembly block {}", stablePos, failure);
            }
        })) {
            // Still publish geometry/mass and client updates. Support/redstone callbacks wait for all blocks.
            original.call(level, pos, chunk, oldState, state, (flags & ~Block.UPDATE_NEIGHBORS) | Block.UPDATE_KNOWN_SHAPE, recursion);
        } else original.call(level, pos, chunk, oldState, state, flags, recursion);
    }
}
