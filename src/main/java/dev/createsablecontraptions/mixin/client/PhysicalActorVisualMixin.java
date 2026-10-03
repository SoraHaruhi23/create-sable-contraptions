package dev.createsablecontraptions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.createsablecontraptions.client.PhysicalActorTransform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionVisual.class, remap = false)
public abstract class PhysicalActorVisualMixin {
    @Shadow @Final protected VisualEmbedding embedding;
    @Shadow @Final private PoseStack contraptionMatrix;
    @Unique private AbstractContraptionEntity csc$entity;
    @Unique private VisualizationContext csc$context;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void csc$captureRenderContext(VisualizationContext context, AbstractContraptionEntity entity, float pt, CallbackInfo ci) {
        csc$entity = entity;
        csc$context = context;
        csc$updatePhysicalEmbedding(pt);
    }

    @Inject(method = "setEmbeddingMatrices", at = @At("HEAD"), cancellable = true)
    private void csc$physicalEmbedding(float pt, CallbackInfo ci) {
        if (csc$updatePhysicalEmbedding(pt)) ci.cancel();
    }

    @Unique private boolean csc$updatePhysicalEmbedding(float pt) {
        if (csc$entity == null || csc$context == null) return false;
        var origin = csc$context.renderOrigin();
        var corrected = new PoseStack();
        if (!PhysicalActorTransform.apply(corrected, csc$entity, pt, origin.getX(), origin.getY(), origin.getZ())) return false;
        contraptionMatrix.setIdentity();
        com.simibubi.create.content.contraptions.render.ContraptionMatrices.transform(contraptionMatrix, corrected);
        embedding.transforms(contraptionMatrix.last().pose(), contraptionMatrix.last().normal());
        return true;
    }
}
