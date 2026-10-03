package dev.createsablecontraptions.physics;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Map Sable's center of mass around Create's block-center pivot. */
public final class RotatingFrame {
    private RotatingFrame() {}
    public static org.joml.Quaterniond fromBasis(Vector3dc x, Vector3dc y, Vector3dc z) {
        return new org.joml.Quaterniond().setFromNormalized(new org.joml.Matrix3d(x, y, z));
    }
    public static Vector3d bodyPosition(Vector3dc rotationPoint, Vector3dc plotPivot,
                                        Vector3dc worldPivot, Quaterniondc rotation) {
        return rotation.transform(new Vector3d(rotationPoint).sub(plotPivot)).add(worldPivot);
    }
}
