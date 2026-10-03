package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.piston.PistonContraption;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PistonContraption.class, remap = false)
public interface PistonContraptionAccessor {
    @Accessor("orientation") Direction csc$orientation();
}
