package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = ClientContraption.class, remap = false)
public abstract class ClientContraptionMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="setupRenderLevelAndRenderedBlockEntities")
    private void csc$timeVirtualSetup(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        long start=ElevatorLink.managed(contraption)?dev.createsablecontraptions.client.ClientPlacementProfile.start():0;
        try { original.call(); } finally { dev.createsablecontraptions.client.ClientPlacementProfile.end("Create虚拟结构初始化",start); }
    }
    @Shadow @Final private Contraption contraption;

    @Inject(method = "getAndAdjustShouldRenderBlockEntities", at = @At("HEAD"), cancellable = true)
    private void csc$physicalBlockEntitiesRender(CallbackInfoReturnable<java.util.BitSet> cir) {
        if (ElevatorLink.managed(contraption)) cir.setReturnValue(new java.util.BitSet());
    }

    // Keep the virtual block entities: Create's controls actor needs its button/indicator
    // and virtual render world for floor labels. Only the static mesh belongs to Sable.
    @Inject(method = "getRenderedBlocks", at = @At("HEAD"), cancellable = true)
    private void csc$noProxyMesh(CallbackInfoReturnable<ClientContraption.RenderedBlocks> cir) {
        if (ElevatorLink.managed(contraption))
            cir.setReturnValue(new ClientContraption.RenderedBlocks(pos -> Blocks.AIR.defaultBlockState(), List.of()));
    }
}
