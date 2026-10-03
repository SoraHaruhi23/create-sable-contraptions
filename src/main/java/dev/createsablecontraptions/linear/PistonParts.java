package dev.createsablecontraptions.linear;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.piston.PistonContraption;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import dev.createsablecontraptions.mixin.PistonContraptionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The stationary cylinder contains a virtual rod/head while assembled. It must never be migrated. */
public final class PistonParts {
    private PistonParts() {}
    public static BlockPos controller(Contraption c) {
        return c instanceof PistonContraption ? c.anchor.relative(((PistonContraptionAccessor) c).csc$orientation().getOpposite()) : null;
    }
    public static boolean movingPart(BlockState state) {
        return AllBlocks.PISTON_EXTENSION_POLE.has(state) || AllBlocks.MECHANICAL_PISTON_HEAD.has(state);
    }
    public static boolean cylinderSlot(Contraption c, BlockPos destination, BlockState moving) {
        return destination.equals(controller(c)) && movingPart(moving);
    }
    public static boolean cylinder(BlockState state) {
        return state.getBlock() instanceof MechanicalPistonBlock;
    }
}
