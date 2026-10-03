package dev.createsablecontraptions.linear;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.mixin.ControlledContraptionAccessor;
import dev.createsablecontraptions.oriented.PhysicalFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Prevent movement breakers from destroying their own carrier/control infrastructure. */
public final class AssemblySourceProtection {
    private AssemblySourceProtection() {}

    public static boolean protects(Contraption contraption, BlockPos target) {
        if (!ElevatorLink.managed(contraption) || contraption.entity == null) return false;
        var entity = contraption.entity;
        var level = entity.level();
        var containing = Sable.HELPER.getContaining(level, target);
        if (containing != null && PhysicalFamily.related(entity, containing.getUniqueId())) return true;

        // Walk upwards so a stabilized child's drill cannot destroy its parent's controller.
        for (var current = entity; current != null;) {
            if (current instanceof ControlledContraptionEntity
                    && target.equals(((ControlledContraptionAccessor) current).csc$controllerPos())) return true;
            if (level.isLoaded(target)) {
                var state = level.getBlockState(target);
                // Protect destination stations too, including carts restored from an item
                // which have no meaningful original assembly coordinate.
                if (current.getContraption() instanceof MountedContraption
                        && state.getBlock() instanceof CartAssemblerBlock) return true;
                if (current.getContraption() instanceof GantryContraption gantry && AllBlocks.GANTRY_SHAFT.has(state)) {
                    var shaft = BlockPos.containing(current.getAnchorVec().add(.5, .5, .5))
                            .relative(gantry.getFacing().getOpposite());
                    if (level.isLoaded(shaft)) {
                        var support = level.getBlockState(shaft);
                        if (AllBlocks.GANTRY_SHAFT.has(support)) {
                            var axis = support.getValue(BlockStateProperties.FACING).getAxis();
                            var delta = target.subtract(shaft);
                            if (state.getValue(BlockStateProperties.FACING).getAxis() == axis
                                    && switch (axis) {
                                        case X -> delta.getY() == 0 && delta.getZ() == 0;
                                        case Y -> delta.getX() == 0 && delta.getZ() == 0;
                                        case Z -> delta.getX() == 0 && delta.getY() == 0;
                                    }) return true;
                        }
                    }
                }
            }
            current = current.getVehicle() instanceof AbstractContraptionEntity parent
                    && ElevatorLink.managed(parent.getContraption()) ? parent : null;
        }
        return false;
    }
}
