package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import dev.createsablecontraptions.linear.GantrySupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = GantryCarriageBlock.class, remap = false)
public abstract class GantryCarriageBlockMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void csc$shaftRemainsInWorld(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (GantrySupport.retained(world, pos)) cir.setReturnValue(true);
    }
}
