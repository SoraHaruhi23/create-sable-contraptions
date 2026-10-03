package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlockEntity;
import dev.createsablecontraptions.linear.GantrySupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = GantryCarriageBlockEntity.class, remap = false)
public abstract class GantryCarriageBlockEntityMixin {
    @Inject(method = "checkValidGantryShaft", at = @At("HEAD"), cancellable = true)
    private void csc$proxyOwnsShaftTracking(CallbackInfo ci) {
        var self = (GantryCarriageBlockEntity) (Object) this;
        if (GantrySupport.retained(self.getLevel(), self.getBlockPos())) ci.cancel();
    }
}
