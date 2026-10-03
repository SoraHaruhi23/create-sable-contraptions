package dev.createsablecontraptions.physics;

import java.util.*;
import java.util.function.DoubleFunction;
import org.joml.Vector3d;

/** A stabilized box keeps its orientation while its attachment follows a circular arc. */
public final class TranslatedArc {
    public record Step(SweptBox.Box box, SweptBox.V motion) {}
    private TranslatedArc() {}
    public static List<Step> steps(SweptBox.Box shape, DoubleFunction<Vector3d> center, double radius, double radians) {
        double tolerance = SweptBox.SKIN / 8;
        int count = Math.max(1, (int) Math.ceil(Math.max(Math.abs(radians) * 4 / Math.PI,
                Math.sqrt(Math.max(0, radius) * radians * radians / (8 * tolerance)))));
        if (count > 2048) throw new IllegalArgumentException("Child sweep exceeds work budget");
        // r * theta^2 / 8 bounds the sagitta between the chord and the actual arc.
        double padding = radius * radians * radians / (8 * count * count);
        var half = new SweptBox.V(shape.half().x() + padding, shape.half().y() + padding, shape.half().z() + padding);
        var result = new ArrayList<Step>(count);
        var previous = center.apply(0);
        for (int i = 1; i <= count; i++) {
            var next = center.apply((double) i / count);
            result.add(new Step(new SweptBox.Box(v(previous), half, shape.x(), shape.y(), shape.z()), v(new Vector3d(next).sub(previous))));
            previous = next;
        }
        return result;
    }
    private static SweptBox.V v(Vector3d p) { return new SweptBox.V(p.x, p.y, p.z); }
}
