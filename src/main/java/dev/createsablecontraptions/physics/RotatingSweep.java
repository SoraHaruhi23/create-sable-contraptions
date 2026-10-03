package dev.createsablecontraptions.physics;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/** Conservative arc envelopes. Inflate only perpendicular to the bearing axis. */
public final class RotatingSweep {
    /** Trial clearance for bearing sweeps against static world blocks, in block units. */
    public static final double WORLD_CONTACT_TOLERANCE = 1.0 / 1024;
    private RotatingSweep() {}
    public static List<SweptBox.Box> envelopes(SweptBox.Box local, Vector3dc pivot, int axis,
                                              double radians, DoubleFunction<Quaterniondc> rotation) {
        double[] reach = {Math.abs(local.center().x()) + local.half().x(),
                Math.abs(local.center().y()) + local.half().y(), Math.abs(local.center().z()) + local.half().z()};
        double radiusSquared = 0;
        for (int i = 0; i < 3; i++) if (i != axis) radiusSquared += reach[i] * reach[i];
        double radius = Math.sqrt(radiusSquared);
        int steps = Math.max(1, (int) Math.ceil(radius * Math.abs(radians) * 32));
        if (steps > 2048) throw new IllegalArgumentException("Rotation sweep exceeds work budget");
        double padding = 2 * radius * Math.sin(Math.abs(radians) / (4 * steps));
        var half = new SweptBox.V(local.half().x() + (axis == 0 ? 0 : padding),
                local.half().y() + (axis == 1 ? 0 : padding), local.half().z() + (axis == 2 ? 0 : padding));
        var result = new ArrayList<SweptBox.Box>(steps);
        for (int i = 0; i < steps; i++) {
            var q = rotation.apply((i + .5) / steps);
            var center = q.transform(new Vector3d(local.center().x(), local.center().y(), local.center().z())).add(pivot);
            result.add(new SweptBox.Box(vector(center), half, vector(q.transform(new Vector3d(1, 0, 0))),
                    vector(q.transform(new Vector3d(0, 1, 0))), vector(q.transform(new Vector3d(0, 0, 1)))));
        }
        return result;
    }

    /** Refine broad arc envelopes near contact so their padding does not lock reverse travel. */
    public static boolean blocked(SweptBox.Box local, Vector3dc pivot, int axis, double radians,
                                  DoubleFunction<Quaterniondc> rotation, SweptBox.Box obstacle) {
        return blocked(local, pivot, axis, radians, rotation, obstacle, SweptBox.SKIN);
    }

    public static boolean blocked(SweptBox.Box local, Vector3dc pivot, int axis, double radians,
                                  DoubleFunction<Quaterniondc> rotation, SweptBox.Box obstacle, double tolerance) {
        double[] reach = {Math.abs(local.center().x()) + local.half().x(), Math.abs(local.center().y()) + local.half().y(),
                Math.abs(local.center().z()) + local.half().z()};
        double r2 = 0;
        for (int i = 0; i < 3; i++) if (i != axis) r2 += reach[i] * reach[i];
        double radius = Math.sqrt(r2);
        int sections = Math.max(1, (int) Math.ceil(Math.abs(radians) / (Math.PI / 2)));
        if (sections > 2048) return true;
        for (int i = 0; i < sections; i++)
            if (refine(local, pivot, axis, radians, rotation, obstacle, (double) i / sections, (double) (i + 1) / sections, 0, tolerance, radius)) return true;
        return false;
    }

    private static boolean refine(SweptBox.Box local, Vector3dc pivot, int axis, double radians,
                                   DoubleFunction<Quaterniondc> rotation, SweptBox.Box obstacle, double from, double to, int depth, double tolerance, double radius) {
        double padding = 2 * radius * Math.sin(Math.abs(radians) * (to - from) / 4);
        var q = rotation.apply((from + to) / 2);
        var center = q.transform(new Vector3d(local.center().x(), local.center().y(), local.center().z())).add(pivot);
        var half = new SweptBox.V(local.half().x() + (axis == 0 ? 0 : padding), local.half().y() + (axis == 1 ? 0 : padding),
                local.half().z() + (axis == 2 ? 0 : padding));
        var envelope = new SweptBox.Box(vector(center), half, vector(q.transform(new Vector3d(1, 0, 0))),
                vector(q.transform(new Vector3d(0, 1, 0))), vector(q.transform(new Vector3d(0, 0, 1))));
        if (!SweptBox.blocked(envelope, new SweptBox.V(0, 0, 0), obstacle, tolerance)) return false;
        // A real midpoint overlap proves collision; no need to refine an already proven hit.
        var midpoint = new SweptBox.Box(envelope.center(), local.half(), envelope.x(), envelope.y(), envelope.z());
        if (SweptBox.blocked(midpoint, new SweptBox.V(0, 0, 0), obstacle, tolerance)) return true;
        // Keep numerical refinement independent of the gameplay clearance.
        if (depth >= 22 || padding < SweptBox.SKIN / 4) return true;
        double mid = (from + to) / 2;
        return refine(local, pivot, axis, radians, rotation, obstacle, from, mid, depth + 1, tolerance, radius)
                || refine(local, pivot, axis, radians, rotation, obstacle, mid, to, depth + 1, tolerance, radius);
    }
    private static SweptBox.V vector(Vector3dc v) { return new SweptBox.V(v.x(), v.y(), v.z()); }
}
