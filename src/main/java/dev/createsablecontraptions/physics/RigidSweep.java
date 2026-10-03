package dev.createsablecontraptions.physics;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

/** Continuous translation + shortest quaternion rotation, including intermediate corners. */
public final class RigidSweep {
    private final SweptBox.Box local;
    private final Vector3d start, end;
    private final Quaterniond from, to;
    private final double radius, angle;
    public RigidSweep(SweptBox.Box local, Vector3dc start, Vector3dc end, Quaterniondc from, Quaterniondc to) {
        this.local=local; this.start=new Vector3d(start); this.end=new Vector3d(end);
        this.from=new Quaterniond(from).normalize(); this.to=new Quaterniond(to).normalize();
        radius=new Vector3d(Math.abs(local.center().x())+local.half().x(),Math.abs(local.center().y())+local.half().y(),
                Math.abs(local.center().z())+local.half().z()).length();
        angle=2*java.lang.Math.acos(java.lang.Math.min(1,java.lang.Math.abs(this.from.dot(this.to))));
    }
    public Vector3d point(Vector3dc localPoint,double t) {
        return new Quaterniond(from).slerp(to,t).transform(new Vector3d(localPoint)).add(new Vector3d(start).lerp(end,t));
    }
    public double angle() { return angle; }
    public Vector3d centerMotion() {
        var center=new Vector3d(local.center().x(),local.center().y(),local.center().z());
        return point(center,1).sub(point(center,0));
    }
    public SweptBox.Box envelope() { return box(.5,2*radius*java.lang.Math.sin(angle/4)+start.distance(end)/2); }
    private SweptBox.Box box(double t,double padding) {
        var q=new Quaterniond(from).slerp(to,t);
        var p=point(new Vector3d(local.center().x(),local.center().y(),local.center().z()),t);
        return new SweptBox.Box(v(p),new SweptBox.V(local.half().x()+padding,local.half().y()+padding,local.half().z()+padding),
                v(q.transform(new Vector3d(1,0,0))),v(q.transform(new Vector3d(0,1,0))),v(q.transform(new Vector3d(0,0,1))));
    }
    public boolean blocked(SweptBox.Box obstacle) {
        var initial=box(0,0);
        var translation=v(new Vector3d(end).sub(start));
        if(angle<1e-10)return SweptBox.blocked(initial,translation,obstacle);
        // Projection on the fixed world rotation axis is invariant during slerp.
        // This rejects an axis-tangent wall without recursively visiting the entire arc.
        var delta=new Quaterniond(to).mul(new Quaterniond(from).conjugate());
        var axis=v(new Vector3d(delta.x,delta.y,delta.z).normalize());
        double distance=obstacle.center().subtract(initial.center()).dot(axis);
        double travel=translation.dot(axis);
        double reach=initial.radius(axis)+obstacle.radius(axis)-SweptBox.SKIN;
        if(java.lang.Math.min(distance,distance-travel)>=reach
                || java.lang.Math.max(distance,distance-travel)<=-reach)return false;
        return refine(obstacle,0,1,0,new int[]{0});
    }
    private boolean refine(SweptBox.Box obstacle,double a,double b,int depth,int[] work) {
        if (++work[0]>32768) throw new IllegalArgumentException("Rigid sweep work budget exceeded");
        double padding=2*radius*java.lang.Math.sin(angle*(b-a)/4)+start.distance(end)*(b-a)/2;
        double mid=(a+b)/2;
        var zero=new SweptBox.V(0,0,0);
        if (!SweptBox.blocked(box(mid,padding),zero,obstacle)) return false;
        if (SweptBox.blocked(box(mid,0),zero,obstacle)) return true;
        if (depth>=24 || padding<SweptBox.SKIN/4) return true;
        return refine(obstacle,a,mid,depth+1,work)||refine(obstacle,mid,b,depth+1,work);
    }
    private static SweptBox.V v(Vector3dc p) { return new SweptBox.V(p.x(),p.y(),p.z()); }
}
