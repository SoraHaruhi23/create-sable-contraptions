package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.mounted.MinecartContraptionItem;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import dev.createsablecontraptions.oriented.PackedStructures;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = MinecartContraptionItem.class, remap = false)
public abstract class MinecartItemMixin {
    @WrapMethod(method="addContraptionToMinecart")
    private static void csc$timePlacement(net.minecraft.world.level.Level level,ItemStack stack,AbstractMinecart cart,
            net.minecraft.core.Direction facing,Operation<Void> original) {
        var previous=dev.createsablecontraptions.oriented.PlacementTrace.begin();
        try { PackedStructures.restoreFamily(() -> original.call(level,stack,cart,facing)); }
        finally { dev.createsablecontraptions.oriented.PlacementTrace.end(cart,previous); }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="addContraptionToMinecart",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean csc$requireRootJoin(net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,
            Operation<Boolean> original) {
        if (!(entity instanceof OrientedContraptionEntity e)
                || !(e.getContraption() instanceof dev.createsablecontraptions.oriented.PackedCarrier packed)
                || packed.csc$packed()==null) return original.call(level,entity);
        // Register before the event: it may be cancelled before restore() is called.
        // The family rollback removes children before discarding their parent.
        PackedStructures.trackPlacementEntity(e);
        if (e.getVehicle()==null) throw new IllegalStateException("Could not attach restored cart structure");
        if (!original.call(level,entity)) throw new IllegalStateException("Restored root join was rejected");
        return true;
    }
    @Inject(method = "create", at = @At("RETURN"))
    private static void csc$liveSnapshot(AbstractMinecart.Type type, OrientedContraptionEntity e, CallbackInfoReturnable<ItemStack> cir) {
        PackedStructures.writeItem(e, cir.getReturnValue());
    }
    @WrapMethod(method = "wrenchCanBeUsedToPickUpMinecartContraptions")
    private static void csc$pickupTransaction(PlayerInteractEvent.EntityInteract event, Operation<Void> original) {
        var family = PackedStructures.beginPickup(event.getTarget());
        try { original.call(event); } finally { PackedStructures.endPickup(family); }
    }
}
