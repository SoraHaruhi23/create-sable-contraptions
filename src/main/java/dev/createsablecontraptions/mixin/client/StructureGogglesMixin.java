package dev.createsablecontraptions.mixin.client;

import java.util.List;
import net.minecraft.network.chat.Component;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.api.equipment.goggles.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.createsablecontraptions.client.StructureGoggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds information through native providers, retaining Create's full overlay rendering path. */
@Mixin(value=GoggleOverlayRenderer.class,remap=false)
public abstract class StructureGogglesMixin {
    @WrapOperation(method="renderOverlay",at=@At(value="INVOKE",target="Lcom/simibubi/create/api/equipment/goggles/IHaveGoggleInformation;addToGoggleTooltip(Ljava/util/List;Z)Z"))
    private static boolean csc$goggles(IHaveGoggleInformation provider,List<Component> tooltip,boolean shift,Operation<Boolean> original) {
        boolean nativeInfo=original.call(provider,tooltip,shift);return StructureGoggles.append(tooltip)||nativeInfo;
    }
    @WrapOperation(method="renderOverlay",at=@At(value="INVOKE",target="Lcom/simibubi/create/api/equipment/goggles/IHaveHoveringInformation;addToTooltip(Ljava/util/List;Z)Z"))
    private static boolean csc$hover(IHaveHoveringInformation provider,List<Component> tooltip,boolean shift,Operation<Boolean> original) {
        boolean nativeInfo=original.call(provider,tooltip,shift);return StructureGoggles.append(tooltip)||nativeInfo;
    }
    @WrapOperation(method="renderOverlay",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/trains/entity/TrainRelocator;addToTooltip(Ljava/util/List;Z)Z"))
    private static boolean csc$plainBlock(List<Component> tooltip,boolean shift,Operation<Boolean> original) {
        boolean nativeInfo=original.call(tooltip,shift);return StructureGoggles.append(tooltip)||nativeInfo;
    }
}
