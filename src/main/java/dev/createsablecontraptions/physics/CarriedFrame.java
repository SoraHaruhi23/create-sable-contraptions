package dev.createsablecontraptions.physics;

import org.joml.Quaterniondc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Carry a child's world box from the published parent pose to its requested pose. */
public final class CarriedFrame {
    private CarriedFrame() {}
    public static SweptBox.Box move(SweptBox.Box box, Vector3dc oldPivot, Quaterniondc oldRotation,
                                    Vector3dc newPivot, Quaterniondc newRotation) {
        var rotation = new Quaterniond(newRotation).mul(new Quaterniond(oldRotation).conjugate());
        var center = rotation.transform(vector(box.center()).sub(oldPivot)).add(newPivot);
        return new SweptBox.Box(v(center), box.half(), v(rotation.transform(vector(box.x()))),
                v(rotation.transform(vector(box.y()))), v(rotation.transform(vector(box.z()))));
    }
    private static Vector3d vector(SweptBox.V v) { return new Vector3d(v.x(), v.y(), v.z()); }
    private static SweptBox.V v(Vector3d v) { return new SweptBox.V(v.x, v.y, v.z); }
}
