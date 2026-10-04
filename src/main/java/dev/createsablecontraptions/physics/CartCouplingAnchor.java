package dev.createsablecontraptions.physics;

import org.joml.Vector3d;

/** Create chooses initial yaw with atan2(second.z - first.z, second.x - first.x). */
public final class CartCouplingAnchor {
    private CartCouplingAnchor() {}
    public static Vector3d offset(double initialYaw, double length) {
        double radians = Math.toRadians(initialYaw);
        return new Vector3d(Math.cos(radians) * length, 0, Math.sin(radians) * length);
    }
}
