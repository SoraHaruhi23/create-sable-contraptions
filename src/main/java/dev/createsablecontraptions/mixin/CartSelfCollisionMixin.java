package dev.createsablecontraptions.mixin;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.createsablecontraptions.oriented.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The carrier cannot collide with the physical body it carries. External bodies stay solid. */
@Mixin(value=SubLevelEntityCollision.class,remap=false)
public abstract class CartSelfCollisionMixin {
    @Inject(method="getSubLevelEntityCollisionShape",at=@At("HEAD"),cancellable=true)
    private static void csc$excludeCarriedBody(Entity entity,Vector3dc center,Pose3dc pose,BlockState state,
            LevelAccelerator level,BlockPos pos,LevelReusedVectors sink,CallbackInfoReturnable<VoxelShape> cir) {
        if(!(entity instanceof AbstractMinecart cart)) return;
        var root=CartImpulse.structure(cart); if(root==null)return;
        var sub=Sable.HELPER.getContaining(cart.level(),pos);
        if(sub!=null && PhysicalFamily.related(root,sub.getUniqueId())) cir.setReturnValue(Shapes.empty());
    }
}
