package dev.createsablecontraptions.mixin;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import dev.createsablecontraptions.elevator.ElevatorBridge;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WindmillBearingBlockEntity.class, remap = false)
public abstract class WindmillBearingMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void csc$refreshLiveSails(CallbackInfo ci) {
        var self = (WindmillBearingBlockEntity) (Object) this;
        var entity = self.getMovedContraption();
        if (!(self.getLevel() instanceof ServerLevel level) || level.getGameTime() % 5 != 0 || entity == null
                || !ElevatorLink.managed(entity.getContraption()) || !(entity.getContraption() instanceof BearingContraption contraption)) return;
        var sub = ElevatorBridge.resolve(level, contraption);
        if (sub == null) return;
        int count = 0;
        for (long packed : ElevatorBridge.data(sub).getLongArray("Blocks")) {
            var pos = sub.getPlot().getCenterBlock().offset(BlockPos.of(packed));
            if (!level.isLoaded(pos)) return;
            var state = level.getBlockState(pos);
            if (com.simibubi.create.AllBlocks.COPYCAT_PANEL.has(state) && level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat)
                state = copycat.getMaterial();
            if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(state)) count++;
        }
        if (count != contraption.getSailBlocks()) {
            ((BearingContraptionAccessor) contraption).csc$setSails(count);
            self.updateGeneratedRotation(); self.setChanged(); self.sendData();
        }
    }
}
