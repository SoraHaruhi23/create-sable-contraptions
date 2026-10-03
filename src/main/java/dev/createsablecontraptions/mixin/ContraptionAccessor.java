package dev.createsablecontraptions.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Set;

@Mixin(value = Contraption.class, remap = false)
public interface ContraptionAccessor {
    @Accessor("stabilizedSubContraptions") java.util.Map<java.util.UUID, net.createmod.catnip.math.BlockFace> csc$children();
    @Accessor("storage") void csc$setStorage(com.simibubi.create.content.contraptions.MountedStorageManager storage);
    @Accessor("glueToRemove") Set<SuperGlueEntity> csc$glueToRemove();
    @org.spongepowered.asm.mixin.gen.Invoker("expandBoundsAroundAxis") void csc$expandAroundAxis(net.minecraft.core.Direction.Axis axis);
}
