package dev.createsablecontraptions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import dev.createsablecontraptions.client.PhysicalActorTransform;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContraptionMatrices.class, remap = false)
public abstract class PhysicalActorMatricesMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void csc$physicalModel(PoseStack view, AbstractContraptionEntity entity, CallbackInfo ci) {
        float pt = AnimationTickHolder.getPartialTicks();
        var corrected = new PoseStack();
        if (!PhysicalActorTransform.apply(corrected, entity, pt, Mth.lerp(pt, entity.xOld, entity.getX()),
                Mth.lerp(pt, entity.yOld, entity.getY()), Mth.lerp(pt, entity.zOld, entity.getZ()))) return;
        var matrices = (ContraptionMatrices) (Object) this;
        matrices.getModel().setIdentity();
        ContraptionMatrices.transform(matrices.getModel(), corrected);
        matrices.getModelViewProjection().setIdentity();
        ContraptionMatrices.transform(matrices.getModelViewProjection(), view);
        ContraptionMatrices.transform(matrices.getModelViewProjection(), corrected);
        matrices.getLight().set(matrices.getWorld()).mul(corrected.last().pose());
    }
}
