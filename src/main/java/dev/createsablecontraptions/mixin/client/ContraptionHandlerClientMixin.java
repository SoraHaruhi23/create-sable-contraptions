package dev.createsablecontraptions.mixin.client;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;

@Mixin(value = ContraptionHandlerClient.class, remap = false)
public abstract class ContraptionHandlerClientMixin {
    @Inject(method = "rightClickingOnContraptionsGetsHandledLocally", at = @At("HEAD"), cancellable = true)
    private static void csc$allowPhysicalBlockUse(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;
        var sub = Sable.HELPER.getContaining(mc.level, hit.getBlockPos());
        if (sub == null) return;
        for (var candidate : mc.level.entitiesForRendering()) {
            if (candidate instanceof AbstractContraptionEntity entity && ElevatorLink.managed(entity.getContraption())
                    && sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) {
                ci.cancel(); // Skip only Create's proxy handler; leave the input event for vanilla/Sable placement.
                return;
            }
        }
    }

    @Inject(method = "rayTraceContraption", at = @At("HEAD"), cancellable = true)
    private static void csc$physicalHitToActor(Vec3 origin, Vec3 target, AbstractContraptionEntity entity,
            CallbackInfoReturnable<BlockHitResult> cir) {
        if (!ElevatorLink.managed(entity.getContraption())) return;
        // Sable's clip returns plot-space coordinates, already occluded by the world and other ships.
        // Both Create's scrolling and right-click handlers use this entry point.
        // Create truncates the ray exactly at the picked face. Include that face despite
        // endpoint rounding; the extra distance is only 0.1 mm and still respects occlusion.
        var end = target.add(target.subtract(origin).normalize().scale(1e-4));
        var hit = entity.level().clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, entity));
        var sub = hit.getType() == HitResult.Type.BLOCK
                ? Sable.HELPER.getContaining(entity.level(), hit.getBlockPos()) : null;
        if (sub == null || !sub.getUniqueId().equals(((ElevatorLink) entity.getContraption()).csc$getSubLevel())) {
            cir.setReturnValue(null);
            return;
        }
        var anchor = sub.getPlot().getCenterBlock();
        var local = hit.getBlockPos().subtract(anchor);
        cir.setReturnValue(entity.getContraption().getBlocks().containsKey(local)
                ? new BlockHitResult(hit.getLocation().subtract(Vec3.atLowerCornerOf(anchor)),
                        hit.getDirection(), local, hit.isInside()) : null);
    }
}
