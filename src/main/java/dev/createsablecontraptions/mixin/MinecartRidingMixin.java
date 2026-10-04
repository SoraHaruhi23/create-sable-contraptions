package dev.createsablecontraptions.mixin;

import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecart.class, remap = false)
public abstract class MinecartRidingMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void csc$rideAlongsideProxy(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        var cart = (Minecart)(Object)this;
        if (player.isSecondaryUseActive() || dev.createsablecontraptions.oriented.CartImpulse.attachedStructure(cart) == null) return;
        // Native Minecart rejects every occupied cart, even when its sole occupant is our invisible carrier.
        if (cart.getPassengers().stream().anyMatch(p -> !(p instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity))) return;
        if (cart.level().isClientSide) cir.setReturnValue(InteractionResult.SUCCESS);
        else cir.setReturnValue(player.startRiding(cart,true) ? InteractionResult.CONSUME : InteractionResult.PASS);
    }
}
