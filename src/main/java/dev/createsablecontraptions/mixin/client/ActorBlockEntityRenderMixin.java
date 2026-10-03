package dev.createsablecontraptions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.createsablecontraptions.client.ElevatorActorClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntityRenderDispatcher.class, remap = false)
public abstract class ActorBlockEntityRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void csc$movingRendererOwns(BlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        if (ElevatorActorClient.movingRendererOwns(be)) ci.cancel();
    }
}
