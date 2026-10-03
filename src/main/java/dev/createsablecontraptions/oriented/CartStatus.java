package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** Records existing checks; querying status never repeats a sweep or starts a working actor. */
public final class CartStatus {
    private record Sample(long tick,Vec3 motion,String reason) {}
    private static final Map<AbstractContraptionEntity,Sample> LAST=new WeakHashMap<>();
    private static final Map<AbstractContraptionEntity,Sample> LAST_FAILURE=new WeakHashMap<>();
    private CartStatus() {}
    public static void begin(AbstractContraptionEntity root,Vec3 motion) {
        dev.createsablecontraptions.physics.CollisionState.begin(root);
        if (!dev.createsablecontraptions.CscConfig.diagnostics()) { LAST.remove(root); LAST_FAILURE.remove(root); return; }
        LAST.put(root,new Sample(root.level().getGameTime(),motion,"本次扫掠通过"));
    }
    public static boolean fail(AbstractContraptionEntity root,String reason) {
        return fail(root,dev.createsablecontraptions.physics.StructureStatus.UNAVAILABLE,reason);
    }
    public static boolean fail(AbstractContraptionEntity root,int code,String reason) {
        dev.createsablecontraptions.physics.CollisionState.fail(root,code);
        if (!dev.createsablecontraptions.CscConfig.diagnostics()) return true;
        var old=LAST.get(root);
        LAST.put(root,new Sample(root.level().getGameTime(),old==null?Vec3.ZERO:old.motion,reason));
        LAST_FAILURE.put(root,LAST.get(root));
        return true;
    }
    public static void contact(AbstractContraptionEntity root,AbstractContraptionEntity owner,BlockPos local,BlockPos target) {
        dev.createsablecontraptions.physics.CollisionState.contact(root,target);
        var old=LAST.get(root);
        if(old==null || !old.reason.equals("本次扫掠通过"))return;
        var block=owner.getContraption().getBlocks().get(local);
        String moving=block==null?"未知":BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString();
        String obstacle=BuiltInRegistries.BLOCK.getKey(root.level().getBlockState(target).getBlock()).toString();
        var sub=dev.ryanhcode.sable.Sable.HELPER.getContaining(root.level(),target);
        var world=dev.ryanhcode.sable.Sable.HELPER.projectOutOfSubLevel(root.level(),target.getCenter());
        var detail="碰撞："+moving+" 局部["+local.toShortString()+"] → "+obstacle+" 世界["
                +BlockPos.containing(world).toShortString()+"]"+(sub==null?"":"（另一物理结构）");
        LAST.put(root,new Sample(root.level().getGameTime(),old.motion,detail));
        LAST_FAILURE.put(root,LAST.get(root));
    }
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csc").then(Commands.literal("cart_status").executes(context->{
            var source=context.getSource();var player=source.getPlayerOrException();
            if (!dev.createsablecontraptions.CscConfig.diagnostics()) {
                source.sendFailure(Component.translatable("csc.diagnostic.disabled")); return 0;
            }
            var carts=source.getLevel().getEntitiesOfClass(AbstractMinecart.class,player.getBoundingBox().inflate(16),
                    c->CartImpulse.structure(c)!=null);
            carts.sort(Comparator.comparingDouble(c->c.distanceToSqr(player)));
            if(carts.isEmpty()){source.sendFailure(Component.translatable("csc.diagnostic.no_cart"));return 0;}
            var cart=carts.getFirst();var root=CartImpulse.structure(cart);
            var lines=new ArrayList<String>();
            lines.add("CSC "+dev.createsablecontraptions.CreateSableContraptions.version()+" 矿车诊断：实体="+cart.getId()+" 坐标="+cart.blockPosition().toShortString());
            lines.add("速度="+cart.getDeltaMovement()+"；几何停转="+((ElevatorLink)root.getContraption()).csc$isBlocked()
                    +"；结构停转="+root.isStalled()+"；方块数="+root.getContraption().getBlocks().size());
            var sample=LAST.get(root);
            lines.add(sample==null?"尚无碰撞检测记录":"最近检测（"+(source.getLevel().getGameTime()-sample.tick)+" tick 前），请求位移="+sample.motion+"；"+sample.reason);
            actors(root,lines);
            if(lines.size()==3)lines.add("没有工作部件请求停转；请同时检查轨道动力、耦合和装配站模式。");
            var failure=LAST_FAILURE.get(root);
            if(failure!=null)lines.add("最近阻挡（"+(source.getLevel().getGameTime()-failure.tick)+" tick 前）："+failure.reason);
            lines.add(CartDocking.describe(root));
            lines.add(PlacementTrace.describe(cart));
            lines.add(DisassemblyStatus.describe(root));
            var rail=cart.getCurrentRailPosition(); var railState=cart.level().getBlockState(rail);
            lines.add("当前轨道="+BuiltInRegistries.BLOCK.getKey(railState.getBlock())+"，位置="+rail.toShortString());
            if(railState.getBlock() instanceof com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock)
                lines.add("装配站动作="+com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock.getActionForCart(railState,cart));
            for(var line:lines)source.sendSuccess(()->Component.literal(line),false);
            return 1;
        })));
    }
    private static void actors(AbstractContraptionEntity e,List<String> lines) {
        for(var actor:e.getContraption().getActors()) {
            var ctx=actor.right;
            if(ctx==null || !ctx.stall)continue;
            if(lines.size()>=19){lines.add("更多停转部件已省略。");return;}
            var breaking=net.minecraft.nbt.NbtUtils.readBlockPos(ctx.data,"BreakingPos").map(BlockPos::toShortString).orElse("无");
            lines.add("工作停转："+BuiltInRegistries.BLOCK.getKey(actor.left.state().getBlock())+" 局部["+actor.left.pos().toShortString()
                    +"]，禁用="+ctx.disabled+"，破坏目标="+breaking+"，计时="+ctx.data.getInt("Timer"));
        }
        for(var p:e.getPassengers())if(p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption()))actors(child,lines);
    }
}
