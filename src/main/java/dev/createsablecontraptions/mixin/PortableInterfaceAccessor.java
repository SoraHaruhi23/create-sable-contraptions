package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PortableStorageInterfaceBlockEntity.class, remap = false)
public interface PortableInterfaceAccessor {
    @org.spongepowered.asm.mixin.gen.Invoker("getExtensionDistance") float csc$extensionDistance(float partialTicks);
    @Accessor("connectedEntity") Entity csc$connectedEntity();
}
