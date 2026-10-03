package dev.createsablecontraptions.client;

import java.util.List;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.oriented.GoggleStatus;
import dev.createsablecontraptions.physics.StructureStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

public final class StructureGoggles {
    private StructureGoggles() {}
    private static boolean hasTitle(Component line) {
        if(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tr
                && tr.getKey().equals("csc.goggles.title"))return true;
        return line.getSiblings().stream().anyMatch(StructureGoggles::hasTitle);
    }
    public static boolean append(List<Component> tooltip) {
        if (!dev.createsablecontraptions.CscConfig.HUD.get()) return false;
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || !GogglesItem.isWearingGoggles(mc.player)
                || !(mc.hitResult instanceof BlockHitResult hit))return false;
        var sub=Sable.HELPER.getContaining(mc.level,hit.getBlockPos());
        if(sub==null)return false;
        for(var ref:ContraptionHandler.loadedContraptions.get(mc.level).values()) {
            var e=ref.get();
            if(e==null || !e.isAlive() || !ElevatorLink.managed(e.getContraption())
                    || !sub.getUniqueId().equals(((ElevatorLink)e.getContraption()).csc$getSubLevel()))continue;
            if(dev.createsablecontraptions.CscConfig.HIGHLIGHT.get())
                ((GoggleStatus)e).csc$collisionTarget().ifPresent(StructureGoggles::highlight);
            // Native overlay can invoke several providers in one frame. Append just once.
            if(tooltip.stream().anyMatch(StructureGoggles::hasTitle)) {
                // Native hover providers may have appended a separator before returning no text.
                if(!tooltip.isEmpty() && tooltip.getLast().getString().isBlank())tooltip.removeLast();
                return true;
            }
            if(!tooltip.isEmpty())tooltip.add(Component.empty());
            CreateLang.builder().add(Component.translatable("csc.goggles.title"))
                    .style(ChatFormatting.GOLD).forGoggles(tooltip);
            int status=((GoggleStatus)e).csc$status();
            CreateLang.builder().add(Component.translatable("csc.goggles.state."+StructureStatus.stateKey(status)))
                    .style(status==StructureStatus.MOVING?ChatFormatting.GREEN:ChatFormatting.GRAY).forGoggles(tooltip);
            var reason=StructureStatus.reasonKey(status);
            if(dev.createsablecontraptions.CscConfig.REASONS.get() && !reason.isEmpty())CreateLang.builder().add(Component.translatable("csc.goggles.reason."+reason))
                    .style(ChatFormatting.YELLOW).forGoggles(tooltip);
            return true;
        }
        return false;
    }
    private static void highlight(net.minecraft.core.BlockPos pos) {
        var level=Minecraft.getInstance().level;
        if(level==null || !level.isLoaded(pos) || level.getBlockState(pos).isAir())return;
        var shape=level.getBlockState(pos).getCollisionShape(level,pos);
        if(shape.isEmpty())return;
        var bounds=shape.bounds().move(pos);
        var sub=Sable.HELPER.getContaining(level,pos);
        if(sub instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel client) {
            net.minecraft.world.phys.AABB world=null;
            for(int i=0;i<8;i++) {
                var point=client.renderPose(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)).transformPosition(new org.joml.Vector3d((i&1)==0?bounds.minX:bounds.maxX,
                        (i&2)==0?bounds.minY:bounds.maxY,(i&4)==0?bounds.minZ:bounds.maxZ));
                var corner=new net.minecraft.world.phys.AABB(point.x,point.y,point.z,point.x,point.y,point.z);
                world=world==null?corner:world.minmax(corner);
            }
            bounds=world;
        }
        net.createmod.catnip.outliner.Outliner.getInstance().showAABB("csc_collision",bounds.inflate(.002))
                .colored(0xff5555).lineWidth(1/32f);
    }
}
