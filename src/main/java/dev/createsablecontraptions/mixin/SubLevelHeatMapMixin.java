package dev.createsablecontraptions.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A Create-assembled platform remains one structure, including glue-connected blocks. */
@Mixin(value = SubLevelHeatMapManager.class, remap = false)
public abstract class SubLevelHeatMapMixin {
    @Shadow @Final private ServerSubLevel subLevel;
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void csc$keepPlatformTogether(CallbackInfo ci) {
        if (ElevatorBridge.managed(subLevel)) ci.cancel();
    }
}
