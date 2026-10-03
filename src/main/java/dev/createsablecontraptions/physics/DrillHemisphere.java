package dev.createsablecontraptions.physics;

public final class DrillHemisphere {
    private DrillHemisphere() {}
    /** The equator is excluded: a purely lateral or rear target is not a cutting target. */
    public static boolean inFront(double dx, double dy, double dz, double fx, double fy, double fz) {
        return dx * fx + dy * fy + dz * fz > 1e-6;
    }
}
