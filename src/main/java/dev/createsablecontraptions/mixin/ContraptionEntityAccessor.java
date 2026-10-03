package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public interface ContraptionEntityAccessor {
    @Accessor("skipActorStop") void csc$skipActorStop(boolean skip);
    @Accessor("STALLED") static EntityDataAccessor<Boolean> csc$stalledAccessor() { throw new AssertionError(); }
    @Invoker("onContraptionStalled") void csc$onStalled();
    @Invoker("makeStructureTransform") com.simibubi.create.content.contraptions.StructureTransform csc$structureTransform();
}
