package dev.createsablecontraptions.physics;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RotatingFrameTest {
    private static final Vector3d PLOT = new Vector3d(30_000_000.5, 64.5, 30_000_000.5);
    private static final Vector3d WORLD = new Vector3d(-23.5, 77.5, 19.5);
    private static void assertVector(Vector3d expected, Vector3d actual) {
        assertTrue(expected.distance(actual) < 1e-7, expected + " != " + actual);
    }
    @Test void xAxisQuarterTurn() {
        // Explicit reference quaternion, independent of the dependency's axis factories.
        var body = RotatingFrame.bodyPosition(new Vector3d(PLOT).add(0, 3, 0), PLOT, WORLD,
                new Quaterniond(Math.sin(Math.PI / 4), 0, 0, Math.cos(Math.PI / 4)));
        assertVector(new Vector3d(WORLD).add(0, 0, 3), body);
    }
    @Test void yAxisQuarterTurn() {
        var body = RotatingFrame.bodyPosition(new Vector3d(PLOT).add(3, 0, 0), PLOT, WORLD, new Quaterniond().rotationY(Math.PI / 2));
        assertVector(new Vector3d(WORLD).add(0, 0, -3), body);
    }
    @Test void zAxisReverseTurn() {
        var body = RotatingFrame.bodyPosition(new Vector3d(PLOT).add(3, 0, 0), PLOT, WORLD, new Quaterniond().rotationZ(-Math.PI / 2));
        assertVector(new Vector3d(WORLD).add(0, -3, 0), body);
    }
    @Test void plotPivotAlwaysMapsToBearingCenter() {
        var mass = new Vector3d(PLOT).add(8, -4, 3);
        for (double angle : new double[] {0, .3, Math.PI / 2, Math.PI, 2 * Math.PI, -7}) {
            var q = new Quaterniond().rotationY(angle);
            var body = RotatingFrame.bodyPosition(mass, PLOT, WORLD, q);
            var pivot = q.transform(new Vector3d(PLOT).sub(mass)).add(body);
            assertVector(WORLD, pivot);
        }
    }
    @Test void editedMassCenterDoesNotMoveExistingBlocks() {
        var q = new Quaterniond().rotationZ(.73);
        var block = new Vector3d(PLOT).add(5, 2, -1);
        var first = new Vector3d(PLOT).add(4, 0, 1);
        var second = new Vector3d(PLOT).add(-3, 6, 2);
        var a = q.transform(new Vector3d(block).sub(first)).add(RotatingFrame.bodyPosition(first, PLOT, WORLD, q));
        var b = q.transform(new Vector3d(block).sub(second)).add(RotatingFrame.bodyPosition(second, PLOT, WORLD, q));
        assertVector(a, b);
    }
    @Test void zeroRotationPreservesElevatorTranslation() {
        assertVector(new Vector3d(WORLD).add(2, 5, -7),
                RotatingFrame.bodyPosition(new Vector3d(PLOT).add(2, 5, -7), PLOT, WORLD, new Quaterniond()));
    }
    @Test void basisConversionPreservesCreateXAxisRotation() {
        var q = RotatingFrame.fromBasis(new Vector3d(1, 0, 0), new Vector3d(0, 0, 1), new Vector3d(0, -1, 0));
        assertVector(new Vector3d(0, 0, 1), q.transform(new Vector3d(0, 1, 0)));
        assertVector(new Vector3d(0, -1, 0), q.transform(new Vector3d(0, 0, 1)));
        assertVector(new Vector3d(1, 0, 0), q.transform(new Vector3d(1, 0, 0)));
    }
}
