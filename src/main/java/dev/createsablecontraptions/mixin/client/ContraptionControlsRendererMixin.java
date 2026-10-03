package dev.createsablecontraptions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsRenderer;
import dev.createsablecontraptions.client.ElevatorControlsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionControlsRenderer.class, remap = false)
public abstract class ContraptionControlsRendererMixin {
    @Inject(method = "renderSafe(Lcom/simibubi/create/content/contraptions/actors/contraptionControls/ContraptionControlsBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"))
    private void csc$renderMovingControlsOnly(ContraptionControlsBlockEntity be, float pt, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (be.getLevel() == Minecraft.getInstance().level) ElevatorControlsClient.prepareDisplay(be, pt);
    }

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/contraptions/actors/contraptionControls/ContraptionControlsBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("TAIL"))
    private void csc$floorOnPhysicalControls(ContraptionControlsBlockEntity be, float pt, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (be.getLevel() == Minecraft.getInstance().level) ElevatorControlsClient.renderFloor(be, pose, buffers);
    }
}
