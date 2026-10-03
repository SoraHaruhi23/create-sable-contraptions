package dev.createsablecontraptions.physics;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RotatingSweepTest {
    @Test void provenMidpointCollisionDoesNotNeedRecursiveRefinement() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        assertTrue(RotatingSweep.blocked(ARM, ZERO, 2, Math.PI / 2, t -> {
            calls.incrementAndGet(); return new Quaterniond().rotationZ(t * Math.PI / 2);
        }, SweptBox.Box.aligned(2, 2, -.5, 2.2, 2.2, .5)));
        assertEquals(1, calls.get());
    }
    @Test void worldClearanceAllowsTinyOverlapButKeepsRealCornerCollisions() {
        double radius = Math.hypot(3.5, .25);
        for (double direction : new double[] {-1, 1}) {
            double radians = direction * Math.PI * 2;
            var grazing = SweptBox.Box.aligned(-5, radius - .0005, -1, 5, radius + 1, 1);
            assertTrue(hits(radians, grazing));
            assertFalse(RotatingSweep.blocked(ARM, ZERO, 2, radians,
                    t -> new Quaterniond().rotationZ(radians * t), grazing, RotatingSweep.WORLD_CONTACT_TOLERANCE));
            var embedded = SweptBox.Box.aligned(-5, radius - .002, -1, 5, radius + 1, 1);
            assertTrue(RotatingSweep.blocked(ARM, ZERO, 2, radians,
                    t -> new Quaterniond().rotationZ(radians * t), embedded, RotatingSweep.WORLD_CONTACT_TOLERANCE));
        }
    }
    @Test void worldClearanceStillCatchesThinObstaclesBetweenEndpoints() {
        assertTrue(RotatingSweep.blocked(ARM, ZERO, 2, Math.PI * 2,
                t -> new Quaterniond().rotationZ(t * Math.PI * 2),
                SweptBox.Box.aligned(-.01, 3, -.1, .01, 3.2, .1), RotatingSweep.WORLD_CONTACT_TOLERANCE));
    }
    @Test void outerCornerTangentToFrameDoesNotBlockFullRotation() {
        double radius = Math.hypot(3.5, .25);
        assertFalse(hits(2 * Math.PI, SweptBox.Box.aligned(-5, radius, -1, 5, radius + 1, 1)));
        assertTrue(hits(2 * Math.PI, SweptBox.Box.aligned(-5, radius - .002, -1, 5, radius + 1, 1)));
    }
    @Test void coarsePaddingIsNotUsedAsTheFinalCollision() {
        double radius = Math.hypot(3.5, .25);
        var wall = SweptBox.Box.aligned(-5, radius + .0001, -1, 5, radius + 1, 1);
        assertTrue(RotatingSweep.envelopes(ARM, ZERO, 2, Math.PI * 2,
                t -> new Quaterniond().rotationZ(t * Math.PI * 2)).stream()
                .anyMatch(b -> SweptBox.blocked(b, new SweptBox.V(0, 0, 0), wall)));
        assertFalse(hits(Math.PI * 2, wall));
    }
    private static final Vector3d ZERO = new Vector3d();
    private static final SweptBox.Box ARM = SweptBox.Box.aligned(2.5, -.25, -.25, 3.5, .25, .25);
    private static boolean hits(double radians, SweptBox.Box obstacle) {
        return RotatingSweep.blocked(ARM, ZERO, 2, radians, t -> new Quaterniond().rotationZ(radians * t), obstacle);
    }
    @Test void quarterArcHitsAnObstacleMissedByBothEndpoints() {
        assertTrue(hits(Math.PI / 2, SweptBox.Box.aligned(2, 2, -.5, 2.15, 2.15, .5)));
    }
    @Test void fullRevolutionCannotBeReducedToIdenticalEndpoints() {
        assertTrue(hits(2 * Math.PI, SweptBox.Box.aligned(-3.1, -.1, -.1, -2.9, .1, .1)));
    }
    @Test void clockwiseArcHasItsOwnObstacles() {
        var obstacle = SweptBox.Box.aligned(2, -2.15, -.5, 2.15, -2, .5);
        assertTrue(hits(-Math.PI / 2, obstacle));
        assertFalse(hits(Math.PI / 2, obstacle));
    }
    @Test void tangencyAlongTheRotationAxisDoesNotJamClockHands() {
        assertFalse(hits(2 * Math.PI, SweptBox.Box.aligned(-4, -4, .25, 4, 4, .5)));
    }
    @Test void attachedWorldBearingDoesNotBlockItsOwnRotor() {
        var rotor = SweptBox.Box.aligned(-.5, -.5, -.5, .5, .5, .5);
        var controller = SweptBox.Box.aligned(-.5, -.5, -1.5, .5, .5, -.5);
        assertFalse(RotatingSweep.blocked(rotor, ZERO, 2, Math.PI * 2,
                t -> new Quaterniond().rotationZ(t * Math.PI * 2), controller));
    }
    @Test void canReverseAwayFromContact() {
        var obstacle = SweptBox.Box.aligned(2.5, .25, -.25, 3.5, 1.25, .25);
        assertTrue(hits(.2, obstacle));
        assertFalse(hits(-.2, obstacle));
    }
    @Test void removedOrDistantObstacleDoesNotStall() {
        assertFalse(hits(2 * Math.PI, SweptBox.Box.aligned(8, 8, -1, 9, 9, 1)));
    }
    @Test void xAxisRotationFindsVerticalArc() {
        var arm = SweptBox.Box.aligned(-.25, 2.5, -.25, .25, 3.5, .25);
        var obstacle = SweptBox.Box.aligned(-.5, 2, 2, .5, 2.2, 2.2);
        assertTrue(RotatingSweep.blocked(arm, ZERO, 0, Math.PI / 2,
                t -> new Quaterniond(Math.sin(t * Math.PI / 4), 0, 0, Math.cos(t * Math.PI / 4)), obstacle));
    }
    @Test void yAxisRotationUsesWorldPivot() {
        var pivot = new Vector3d(-30, 62, 15);
        var obstacle = SweptBox.Box.aligned(-28, 61.5, 12.8, -27.8, 62.5, 13);
        assertTrue(RotatingSweep.blocked(ARM, pivot, 1, Math.PI / 2, t -> new Quaterniond().rotationY(t * Math.PI / 2), obstacle));
    }
    @Test void workBudgetIsBounded() {
        var huge = SweptBox.Box.aligned(1000, 0, 0, 1001, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> RotatingSweep.envelopes(huge, ZERO, 2, Math.PI,
                t -> new Quaterniond().rotationZ(t * Math.PI)));
    }
}
