package dev.createsablecontraptions.mixin;

import dev.createsablecontraptions.elevator.AssemblerProtection;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Simulated integration: guard packets and deployers before any alignment or transfer. */
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity", remap = false)
public abstract class PhysicsAssemblerProtectionMixin {
    @Inject(method = "assembleOrDisassemble()V", at = @At("HEAD"), cancellable = true)
    private void csc$protectStructure(CallbackInfo ci) {
        var be = (BlockEntity) (Object) this;
        if (AssemblerProtection.managed(be.getLevel(), be.getBlockPos())) ci.cancel();
    }
}
