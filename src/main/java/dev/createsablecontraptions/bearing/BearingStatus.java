package dev.createsablecontraptions.bearing;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.createsablecontraptions.elevator.ElevatorLink;
import dev.createsablecontraptions.physics.SweptBox;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;
import org.joml.Vector3d;

/** Records the existing sweep, never reruns it from a command. */
public final class BearingStatus {
    private static final Map<ControlledContraptionEntity, Sample> LAST = new WeakHashMap<>();
    private static final class Sample {
        long tick;
        float angle, speed;
        String detail = "主结构扫掠通过";
        boolean recorded;
    }
    private BearingStatus() {}
    public static void begin(ControlledContraptionEntity e, float speed) {
        dev.createsablecontraptions.physics.CollisionState.begin(e);
        if (!dev.createsablecontraptions.CscConfig.diagnostics()) { LAST.remove(e); return; }
        var s = new Sample(); s.tick = e.level().getGameTime(); s.angle = e.getAngle(1); s.speed = speed;
        LAST.put(e, s);
    }
    public static boolean fail(ControlledContraptionEntity e, String reason) {
        return fail(e,dev.createsablecontraptions.physics.StructureStatus.UNAVAILABLE,reason);
    }
    public static boolean fail(ControlledContraptionEntity e,int code,String reason) {
        dev.createsablecontraptions.physics.CollisionState.fail(e,code);
        var s = LAST.get(e);
        if (s != null && !s.recorded) { s.detail = "检测未完成：" + reason; s.recorded = true; }
        return true;
    }
    public static void children(ControlledContraptionEntity e, boolean blocked) {
        var s = LAST.get(e);
        if (blocked && s != null && !s.recorded) s.detail = "主结构通过；子结构扫掠阻挡或检测未完成";
    }
    public static void contact(ControlledContraptionEntity e, BlockPos source, BlockPos target,
                               SweptBox.Box local, Vector3d pivot, SweptBox.Box obstacle, double tolerance) {
        dev.createsablecontraptions.physics.CollisionState.contact(e,target);
        var s = LAST.get(e); if (s == null || s.recorded) return;
        s.recorded = true;
        var q = BearingBridge.rotation(e, s.angle);
        var center = q.transform(new Vector3d(local.center().x(), local.center().y(), local.center().z())).add(pivot);
        var now = new SweptBox.Box(v(center), local.half(), v(q.transform(new Vector3d(1,0,0))),
                v(q.transform(new Vector3d(0,1,0))), v(q.transform(new Vector3d(0,0,1))));
        boolean overlap = SweptBox.blocked(now, new SweptBox.V(0,0,0), obstacle, tolerance);
        var sub = dev.ryanhcode.sable.Sable.HELPER.getContaining(e.level(), target);
        var world = dev.ryanhcode.sable.Sable.HELPER.projectOutOfSubLevel(e.level(), target.getCenter());
        s.detail = (overlap ? "当前位置已重叠" : "当前位置无重叠；请求旋转途中命中")
                + "；结构局部[" + source.toShortString() + "] → "
                + BuiltInRegistries.BLOCK.getKey(e.level().getBlockState(target).getBlock())
                + " 世界[" + BlockPos.containing(world).toShortString() + "]"
                + (sub == null ? "（静态世界）" : "（另一物理结构）")
                + "；当前形状中心=" + center + "；半尺寸=" + local.half()
                + "；障碍中心=" + obstacle.center() + "；半尺寸=" + obstacle.half();
    }
    private static SweptBox.V v(Vector3d p) { return new SweptBox.V(p.x,p.y,p.z); }
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("csc").then(Commands.literal("bearing_status").executes(ctx -> {
            var source = ctx.getSource(); var player = source.getPlayerOrException();
            if (!dev.createsablecontraptions.CscConfig.diagnostics()) {
                source.sendFailure(Component.translatable("csc.diagnostic.disabled")); return 0;
            }
            var entries = new ArrayList<>(source.getLevel().getEntitiesOfClass(ControlledContraptionEntity.class,
                    player.getBoundingBox().inflate(16), e -> ElevatorLink.managed(e.getContraption())
                            && ElevatorLink.bearing(e.getContraption())));
            entries.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            if (entries.isEmpty()) { source.sendFailure(Component.translatable("csc.diagnostic.no_bearing")); return 0; }
            for (var e : entries.stream().limit(4).toList()) {
                var s = LAST.get(e);
                source.sendSuccess(() -> Component.literal("轴承实体=" + e.getId() + " 锚点=" + e.blockPosition().toShortString()
                        + " 轴=" + e.getRotationAxis() + " 几何停转=" + ((ElevatorLink)e.getContraption()).csc$isBlocked()), false);
                source.sendSuccess(() -> Component.literal(s == null ? "尚无检测记录" : "记录年龄="
                        + (e.level().getGameTime()-s.tick) + " tick；起始角=" + s.angle + "；请求旋转=" + s.speed + " 度/tick；" + s.detail), false);
            }
            return 1;
        })));
    }
}
