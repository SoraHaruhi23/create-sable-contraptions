package dev.createsablecontraptions.oriented;

import com.simibubi.create.content.contraptions.*;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.createsablecontraptions.bearing.BearingBridge;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.physics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import org.joml.Vector3d;
import java.util.*;

/** Check child shapes BEFORE accepting the parent's movement, including stable orientations. */
public final class ChildMotion {
    @FunctionalInterface private interface Path { Vec3 at(Vec3 p, double t); }
    private record Shape(AbstractContraptionEntity owner, BlockPos local, List<TranslatedArc.Step> steps, RigidSweep rigid) {
        Shape(AbstractContraptionEntity owner, BlockPos local, List<TranslatedArc.Step> steps) { this(owner,local,steps,null); }
    }
    private record Turn(Vector3d start, Vector3d end, org.joml.Quaterniond from, org.joml.Quaterniond to) {}
    private static final class CollectionFailure extends RuntimeException {
        final int reason;
        CollectionFailure(int reason,String message) { super(message); this.reason=reason; }
    }
    private ChildMotion() {}
    public static boolean angular(ControlledContraptionEntity root, float speed) {
        float angle = root.getAngle(1);
        return check(root, (p, t) -> {
            var q = BearingBridge.rotation(root, angle + speed * (float) t);
            var r = q.transform(new Vector3d(p.x - .5, p.y - .5, p.z - .5));
            return new Vec3(r.x, r.y, r.z).add(root.getAnchorVec()).add(.5, .5, .5);
        }, Math.toRadians(speed));
    }
    public static boolean linear(AbstractContraptionEntity root, Vec3 motion) {
        return root != null && check(root, (p, t) -> root.toGlobalVector(p, 1).add(motion.scale(t)), 0);
    }
    public static boolean cart(AbstractContraptionEntity root, Vec3 motion) {
        return root != null && check(root,(p,t)->root.toGlobalVector(p,1).add(motion.scale(t)),0,true);
    }
    public static boolean cartTurn(OrientedContraptionEntity root, Vec3 start, org.joml.Quaterniond from, org.joml.Quaterniond to) {
        // OrientedContraptionEntity's anchor differs from its entity position by
        // (-.5, 0, -.5). Match toGlobalVector and the physical target exactly.
        var end=root.getAnchorVec();
        var beginning=start.add(end.subtract(root.position()));
        var turn=new Turn(new Vector3d(beginning.x+.5,beginning.y+.5,beginning.z+.5),new Vector3d(end.x+.5,end.y+.5,end.z+.5),from,to);
        var path=new RigidSweep(SweptBox.Box.aligned(0,0,0,0,0,0),turn.start,turn.end,from,to);
        return check(root,(p,t)-> {
            var v=path.point(new Vector3d(p.x-.5,p.y-.5,p.z-.5),t); return new Vec3(v.x,v.y,v.z);
        },path.angle(),true,turn);
    }
    private static void collect(AbstractContraptionEntity parent, Path path, double radians, double orbitRadius,
                                   List<Shape> shapes, int[] budget) {
        for (var passenger : parent.getPassengers()) {
            if (!(passenger instanceof OrientedContraptionEntity child) || !ElevatorLink.managed(child.getContraption())) continue;
            var attachment = parent.getContraption().getBearingPosOf(child.getUUID());
            if (attachment == null) continue;
            var sub = ElevatorActors.sub(child);
            if (sub == null) throw new CollectionFailure(StructureStatus.UNAVAILABLE,"子结构物理体不可用");
            Vec3 localAttachment = Vec3.atCenterOf(attachment);
            double radius = orbitRadius < 0 ? localAttachment.subtract(.5, .5, .5).length() : orbitRadius;
            Path childPath = (p, t) -> path.at(localAttachment, t).add(child.applyRotation(p.subtract(.5, .5, .5), 1));
            var x = v(child.applyRotation(new Vec3(1, 0, 0), 1));
            var y = v(child.applyRotation(new Vec3(0, 1, 0), 1));
            var z = v(child.applyRotation(new Vec3(0, 0, 1), 1));
            if (!(sub instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel server)) throw new CollectionFailure(StructureStatus.UNAVAILABLE,"子结构物理体不可用");
            for (long packed : ElevatorBridge.data(server).getLongArray("Blocks")) {
                var local = BlockPos.of(packed); var pos = sub.getPlot().getCenterBlock().offset(local);
                if (!child.level().isLoaded(pos)) throw new CollectionFailure(StructureStatus.UNLOADED,"子结构区块未加载");
                for (var box : child.level().getBlockState(pos).getCollisionShape(child.level(), pos).toAabbs()) {
                    var center = box.move(local).getCenter();
                    var shape = new SweptBox.Box(v(center), new SweptBox.V(box.getXsize()/2, box.getYsize()/2, box.getZsize()/2), x, y, z);
                    var steps = TranslatedArc.steps(shape, t -> {
                        var p = childPath.at(center, t); return new Vector3d(p.x, p.y, p.z);
                    }, radius, radians);
                    budget[0] += steps.size();
                    if (budget[0] > 32768) throw new CollectionFailure(StructureStatus.BUDGET,"子结构扫掠分段超过 32768");
                    shapes.add(new Shape(child, local, steps));
                }
            }
            collect(child, childPath, radians, radius, shapes, budget);
        }
    }
    private static boolean check(AbstractContraptionEntity root, Path path, double radians) {
        return check(root,path,radians,false);
    }
    private static boolean check(AbstractContraptionEntity root, Path path, double radians, boolean includeRoot) {
        return check(root,path,radians,includeRoot,null);
    }
    private static boolean check(AbstractContraptionEntity root, Path path, double radians, boolean includeRoot, Turn turn) {
        if (!(root.level() instanceof ServerLevel level) || !ElevatorLink.managed(root.getContraption())) return false;
        CartStatus.begin(root,path.at(Vec3.ZERO,1).subtract(path.at(Vec3.ZERO,0)));
        var shapes = new ArrayList<Shape>();
        try {
            int[] budget = {0};
            if (includeRoot) collectCartBody(root,path,shapes,budget,turn);
            collect(root, path, radians, -1, shapes, budget);
        }
        catch (CollectionFailure failure) { return CartStatus.fail(root,failure.reason,failure.getMessage()); }
        catch (IllegalArgumentException tooLarge) { return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"单条旋转扫掠超过 2048 段"); }
        AABB search = null;
        for (var shape : shapes) for (var step : shape.steps) {
            var b = bounds(step.box()).expandTowards(step.motion().x(), step.motion().y(), step.motion().z());
            search = search == null ? b : search.minmax(b);
        }
        if (search == null) return false;
        if (search.minY < level.getMinBuildHeight() || search.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(search)) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BOUNDARY,"扫掠超出世界高度或边界");
        if(volume(search)>32768)return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"世界查询体积超过 32768，体积="+volume(search));
        long[] comparisons = {0}; boolean blocked = false;
        for (var pos : positions(search)) {
            if (!level.isLoaded(pos)) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.UNLOADED,"世界区块未加载："+pos.toShortString());
            var state = level.getBlockState(pos);
            var context = includeRoot && state.getBlock() instanceof com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock
                    && root.getVehicle() instanceof net.minecraft.world.entity.vehicle.AbstractMinecart cart
                    ? net.minecraft.world.phys.shapes.CollisionContext.of(cart) : net.minecraft.world.phys.shapes.CollisionContext.empty();
            for (var box : state.getCollisionShape(level, pos, context).toAabbs()) {
                var b = box.move(pos);
                blocked |= hit(root,shapes, SweptBox.Box.aligned(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ), pos, comparisons);
                if (comparisons[0] > 2_000_000) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"碰撞比较次数超过 2000000");
            }
        }
        for (var other : SubLevelContainer.getContainer(level).getAllSubLevels()) {
            if (other.isRemoved() || PhysicalFamily.related(root, other.getUniqueId())) continue;
            var b = other.boundingBox();
            if (!search.intersects(new AABB(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()))) continue;
            var plot = other.getPlot().getBoundingBox();
            var query = ElevatorCollisions.inverseBounds(search, other.logicalPose()).intersect(
                    new AABB(plot.minX(),plot.minY(),plot.minZ(),plot.maxX()+1,plot.maxY()+1,plot.maxZ()+1));
            if (volume(query) > 32768) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"另一物理结构的查询体积超过 32768");
            for (var pos : positions(query)) {
                if (!level.isLoaded(pos)) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.UNLOADED,"另一物理结构的区块未加载");
                for (var box : level.getBlockState(pos).getCollisionShape(level,pos).toAabbs()) {
                    blocked |= hit(root,shapes, ElevatorCollisions.oriented(box.move(pos),other.logicalPose()),pos,comparisons);
                    if (comparisons[0] > 2_000_000) return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"碰撞比较次数超过 2000000");
                }
            }
        }
        return blocked;
    }
    private static void collectCartBody(AbstractContraptionEntity root,Path path,List<Shape> shapes,int[] budget,Turn turn) {
        if (!(ElevatorActors.sub(root) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel sub)) throw new CollectionFailure(StructureStatus.UNAVAILABLE,"主结构物理体不可用");
        var x=v(root.applyRotation(new Vec3(1,0,0),1));
        var y=v(root.applyRotation(new Vec3(0,1,0),1));
        var z=v(root.applyRotation(new Vec3(0,0,1),1));
        for(long packed:ElevatorBridge.data(sub).getLongArray("Blocks")) {
            var local=BlockPos.of(packed);
            if(OrientedBridge.virtualAnchor(root.getContraption(),local)) continue;
            var pos=sub.getPlot().getCenterBlock().offset(local);
            if(!root.level().isLoaded(pos)) throw new CollectionFailure(StructureStatus.UNLOADED,"主结构区块未加载");
            for(var b:root.level().getBlockState(pos).getCollisionShape(root.level(),pos).toAabbs()) {
                var center=b.move(local).getCenter();
                var box=new SweptBox.Box(v(center),new SweptBox.V(b.getXsize()/2,b.getYsize()/2,b.getZsize()/2),x,y,z);
                RigidSweep rigid=null;
                List<TranslatedArc.Step> steps;
                if (turn!=null) {
                    var localBox=b.move(local).move(-.5,-.5,-.5);
                    rigid=new RigidSweep(SweptBox.Box.aligned(localBox.minX,localBox.minY,localBox.minZ,localBox.maxX,localBox.maxY,localBox.maxZ),turn.start,turn.end,turn.from,turn.to);
                    steps=List.of(new TranslatedArc.Step(rigid.envelope(),new SweptBox.V(0,0,0)));
                } else steps=TranslatedArc.steps(box,t->{var p=path.at(center,t);return new Vector3d(p.x,p.y,p.z);},0,0);
                if(++budget[0]>32768) throw new CollectionFailure(StructureStatus.BUDGET,"主结构形状数量超过 32768");
                shapes.add(new Shape(root,local,steps,rigid));
            }
        }
    }
    private static boolean hit(AbstractContraptionEntity root,List<Shape> shapes, SweptBox.Box obstacle, BlockPos pos, long[] budget) {
        boolean blocked = false;
        for (var shape : shapes) for (var step : shape.steps) {
            if (++budget[0] > 2_000_000) return true;
            if (!SweptBox.blocked(step.box(),step.motion(),obstacle)) continue;
            if (shape.rigid!=null) {
                try { if (!shape.rigid.blocked(obstacle)) continue; }
                catch (IllegalArgumentException budgetExceeded) { return CartStatus.fail(root,dev.createsablecontraptions.physics.StructureStatus.BUDGET,"矿车转弯精检超过预算"); }
            }
            blocked = true;
            CartStatus.contact(root,shape.owner,shape.local,pos);
            var motion=shape.rigid==null ? new Vec3(step.motion().x(),step.motion().y(),step.motion().z())
                    : vec(shape.rigid.centerMotion());
            dev.createsablecontraptions.linear.CollisionDrilling.offer(shape.owner.getContraption(),shape.local,pos,
                    motion);
            break;
        }
        return blocked;
    }
    private static Iterable<BlockPos> positions(AABB b) { return BlockPos.betweenClosed(BlockPos.containing(b.minX,b.minY,b.minZ),BlockPos.containing(b.maxX,b.maxY,b.maxZ)); }
    private static double volume(AABB b) { return (b.getXsize()+2)*(b.getYsize()+2)*(b.getZsize()+2); }
    private static SweptBox.V v(Vec3 p) { return new SweptBox.V(p.x,p.y,p.z); }
    private static Vec3 vec(Vector3d p) { return new Vec3(p.x,p.y,p.z); }
    private static AABB bounds(SweptBox.Box b) {
        double x = Math.abs(b.x().x())*b.half().x()+Math.abs(b.y().x())*b.half().y()+Math.abs(b.z().x())*b.half().z();
        double y = Math.abs(b.x().y())*b.half().x()+Math.abs(b.y().y())*b.half().y()+Math.abs(b.z().y())*b.half().z();
        double z = Math.abs(b.x().z())*b.half().x()+Math.abs(b.y().z())*b.half().y()+Math.abs(b.z().z())*b.half().z();
        return new AABB(b.center().x()-x,b.center().y()-y,b.center().z()-z,b.center().x()+x,b.center().y()+y,b.center().z()+z);
    }
}
