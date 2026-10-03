package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.oriented.ChildAssemblyScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

@Mixin(value = Contraption.class, remap = false)
public abstract class ChildAssemblyMixin {
    @WrapMethod(method = "onEntityCreated")
    private void csc$createChildren(AbstractContraptionEntity entity, Operation<Void> original) {
        ChildAssemblyScope.push(ElevatorLink.managed((Contraption) (Object) this));
        try { original.call(entity); } finally { ChildAssemblyScope.pop(); }
    }
}
