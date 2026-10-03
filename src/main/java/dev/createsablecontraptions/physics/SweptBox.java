package dev.createsablecontraptions.physics;

/** Continuous SAT for a translating box against an oriented box. No Minecraft/native dependency. */
public final class SweptBox {
    public static final double SKIN = 1e-5;
    private SweptBox() {}

    public record V(double x, double y, double z) {
        public V subtract(V v) { return new V(x - v.x, y - v.y, z - v.z); }
        public double dot(V v) { return x * v.x + y * v.y + z * v.z; }
        public V cross(V v) { return new V(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
        public V scale(double s) { return new V(x * s, y * s, z * s); }
    }
    public record Box(V center, V half, V x, V y, V z) {
        public static Box aligned(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            return new Box(new V((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2),
                    new V((maxX - minX) / 2, (maxY - minY) / 2, (maxZ - minZ) / 2),
                    new V(1, 0, 0), new V(0, 1, 0), new V(0, 0, 1));
        }
        double radius(V axis) {
            return Math.abs(x.dot(axis)) * half.x + Math.abs(y.dot(axis)) * half.y + Math.abs(z.dot(axis)) * half.z;
        }
    }

    /** True when interiors overlap anywhere along the displacement; tangent contact is allowed. */
    public static boolean blocked(Box moving, V displacement, Box obstacle) {
        return blocked(moving, displacement, obstacle, SKIN);
    }

    public static boolean blocked(Box moving, V displacement, Box obstacle, double tolerance) {
        V[] a = {moving.x, moving.y, moving.z};
        V[] b = {obstacle.x, obstacle.y, obstacle.z};
        V[] axes = new V[15];
        System.arraycopy(a, 0, axes, 0, 3);
        System.arraycopy(b, 0, axes, 3, 3);
        int n = 6;
        for (V av : a) for (V bv : b) axes[n++] = av.cross(bv);
        V delta = obstacle.center.subtract(moving.center);
        double enter = 0, exit = 1;
        for (V raw : axes) {
            double length = Math.sqrt(raw.dot(raw));
            if (length < 1e-10) continue;
            V axis = raw.scale(1 / length);
            double radius = Math.max(0, moving.radius(axis) + obstacle.radius(axis) - tolerance);
            double distance = delta.dot(axis), speed = displacement.dot(axis);
            if (Math.abs(speed) < 1e-12) {
                if (Math.abs(distance) >= radius) return false;
            } else {
                double t1 = (distance - radius) / speed, t2 = (distance + radius) / speed;
                enter = Math.max(enter, Math.min(t1, t2));
                exit = Math.min(exit, Math.max(t1, t2));
                if (enter >= exit) return false;
            }
        }
        return enter < exit;
    }
}
