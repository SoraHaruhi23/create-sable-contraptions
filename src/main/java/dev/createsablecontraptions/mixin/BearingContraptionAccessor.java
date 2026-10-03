package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BearingContraption.class, remap = false)
public interface BearingContraptionAccessor {
    @Accessor("sailBlocks") void csc$setSails(int sails);
}
