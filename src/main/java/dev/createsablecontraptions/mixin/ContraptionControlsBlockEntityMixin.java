package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorControlsBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionControlsBlockEntity.class, remap = false)
public abstract class ContraptionControlsBlockEntityMixin {
    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void csc$disableStationaryFilter(CallbackInfo ci) {
        var self = (ContraptionControlsBlockEntity) (Object) this;
        self.filtering.onlyActiveWhen(() -> !ElevatorControlsBridge.isPhysicalControl(self.getLevel(), self.getBlockPos()));
    }

    @Inject(method = {"tick", "updatePoweredState"}, at = @At("HEAD"), cancellable = true)
    private void csc$actorOwnsMovingControls(CallbackInfo ci) {
        var self = (ContraptionControlsBlockEntity) (Object) this;
        if (ElevatorControlsBridge.isPhysicalControl(self.getLevel(), self.getBlockPos())) ci.cancel();
    }
}
