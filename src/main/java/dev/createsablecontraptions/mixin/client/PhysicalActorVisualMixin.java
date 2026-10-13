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

@Mixin(value = ContraptionVisual.class, remap = false, priority = 1100)
public abstract class PhysicalActorVisualMixin {
    @Shadow @Final protected VisualEmbedding embedding;
    @Shadow @Final private PoseStack contraptionMatrix;
    @Unique private AbstractContraptionEntity csc$entity;
    @Unique private VisualizationContext csc$context;
    @Unique private boolean csc$physicalBlockEntities;

    @Inject(method = "setupChildren", at = @At("HEAD"))
    private void csc$selectRealBlockEntities(com.simibubi.create.content.contraptions.Contraption contraption,
            com.simibubi.create.content.contraptions.render.ClientContraption client, float pt, CallbackInfo ci) {
        // Called during construction, before csc$entity is captured at constructor TAIL.
        csc$physicalBlockEntities = dev.createsablecontraptions.elevator.ElevatorLink.managed(contraption);
    }

    @Inject(method = "setupVisualizer", at = @At("HEAD"), cancellable = true)
    private void csc$realBlockEntityOwnsVisual(net.minecraft.world.level.block.entity.BlockEntity be,
                                             float pt, CallbackInfo ci) {
        // Real Sable BEs own kinetic/pulley/tank visuals. Create's setupActor remains
        // enabled for drills, deployers and other MovementBehaviour visuals.
        if (csc$physicalBlockEntities) ci.cancel();
    }

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

    @Inject(method = "beginFrame", at = @At("TAIL"))
    private void csc$finalPhysicalPose(dev.engine_room.flywheel.api.visual.DynamicVisual.Context context, CallbackInfo ci) {
        // Reassert after Sable's own setEmbeddingMatrices hook, before actor plans run.
        csc$updatePhysicalEmbedding(context.partialTick());
    }

    @Unique private boolean csc$updatePhysicalEmbedding(float pt) {
        if (csc$entity == null || csc$context == null) return false;
        var origin = csc$context.renderOrigin();
        var corrected = new PoseStack();
        if (!PhysicalActorTransform.apply(corrected, csc$entity, pt, origin.getX(), origin.getY(), origin.getZ(), false)) return false;
        contraptionMatrix.setIdentity();
        com.simibubi.create.content.contraptions.render.ContraptionMatrices.transform(contraptionMatrix, corrected);
        embedding.transforms(contraptionMatrix.last().pose(), contraptionMatrix.last().normal());
        return true;
    }
}
