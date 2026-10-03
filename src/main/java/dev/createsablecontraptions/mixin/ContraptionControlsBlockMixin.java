package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlock;
import dev.createsablecontraptions.elevator.ElevatorControlsBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContraptionControlsBlock.class, remap = false)
public abstract class ContraptionControlsBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void csc$noStationaryToggle(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        // Normal input is sent through Create's moving interaction / floor packets.
        // A late ordinary block-use packet must not toggle the physical BE's stationary state.
        if (ElevatorControlsBridge.isPhysicalControl(level, pos)) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
