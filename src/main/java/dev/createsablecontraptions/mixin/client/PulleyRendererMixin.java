package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PulleyRenderer.class, remap = false)
public abstract class PulleyRendererMixin {
    @Inject(method = "getBlockEntityOffset", at = @At("RETURN"), cancellable = true)
    private static void csc$followPhysicalAnchor(float partialTicks, PulleyBlockEntity pulley,
            CallbackInfoReturnable<Float> cir) {
        var entity = pulley.getAttachedContraption();
        if (entity == null || !(entity.getContraption() instanceof com.simibubi.create.content.contraptions.pulley.PulleyContraption elevator)
                || !ElevatorLink.managed(elevator)) return;
        var container = SubLevelContainer.getContainer(pulley.getLevel());
        if (container == null) return;
        var sub = container.getSubLevel(((ElevatorLink) elevator).csc$getSubLevel());
        if (!(sub instanceof ClientSubLevel client) || sub.isRemoved()) return;
        var anchor = client.renderPose(partialTicks).transformPosition(Vec3.atLowerCornerOf(sub.getPlot().getCenterBlock()));
        // Shared by Create's ordinary renderer and Flywheel elevator visual.
        cir.setReturnValue((float) (pulley.getBlockPos().getY() - 1 - anchor.y));
    }
}
