package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.createsablecontraptions.physics.SweptBox.*;

class SweptBoxTest {
    private final Box platform = Box.aligned(0, 0, 0, 1, 1, 1);
    @Test void clearPath() { assertFalse(blocked(platform, new V(0, 1, 0), Box.aligned(2, 0, 0, 3, 3, 1))); }
    @Test void sweepCatchesThinObstacleBetweenEndpoints() {
        assertTrue(blocked(platform, new V(0, 8, 0), Box.aligned(0, 4, 0, 1, 4.01, 1)));
    }
    @Test void downwardCollision() { assertTrue(blocked(platform, new V(0, -2, 0), Box.aligned(0, -2, 0, 1, -1, 1))); }
    @Test void touchingWallDoesNotBlockParallelMotion() {
        assertFalse(blocked(platform, new V(0, 3, 0), Box.aligned(1, 0, 0, 2, 5, 1)));
    }
    @Test void reverseAwayFromContact() {
        Box ceiling = Box.aligned(0, 1, 0, 1, 2, 1);
        assertTrue(blocked(platform, new V(0, .1, 0), ceiling));
        assertFalse(blocked(platform, new V(0, -.1, 0), ceiling));
    }
    @Test void restingAtFloorIsNotBlocked() {
        assertFalse(blocked(platform, new V(0, 0, 0), Box.aligned(0, -1, 0, 1, 0, 1)));
    }
    @Test void occupiedDisassemblyIsBlocked() { assertTrue(blocked(platform, new V(0, 0, 0), platform)); }
    @Test void rotatedObstacle() {
        double c = Math.sqrt(.5);
        Box rotated = new Box(new V(.5, 2, .5), new V(.5, .1, .5), new V(c, c, 0), new V(-c, c, 0), new V(0, 0, 1));
        assertTrue(blocked(platform, new V(0, 3, 0), rotated));
        assertFalse(blocked(platform, new V(0, -3, 0), rotated));
    }
    @Test void removingObstacleAllowsNextStep() {
        V motion = new V(0, .25, 0);
        assertTrue(blocked(platform, motion, Box.aligned(0, 1, 0, 1, 2, 1)));
        assertFalse(blocked(platform, motion, Box.aligned(2, 1, 0, 3, 2, 1)));
    }
    @Test void sideOfDrillCanHitAnObliqueObstacleOutsideItsCenterLine() {
        double c = Math.sqrt(.5);
        var side = new Box(new V(1.02, 1.2, .5), new V(.2, .1, .5),
                new V(c, c, 0), new V(-c, c, 0), new V(0, 0, 1));
        assertTrue(blocked(platform, new V(0, .4, 0), side));
        var centerLine = Box.aligned(.49, 0, .49, .51, 1, .51);
        assertFalse(blocked(centerLine, new V(0, .4, 0), side));
    }
    @Test void aChassisHitMustNotBecomeADrillHit() {
        var obstacle = Box.aligned(1, 1, 0, 2, 2, 1);
        var chassis = Box.aligned(1, 0, 0, 2, 1, 1);
        assertTrue(blocked(chassis, new V(0, .2, 0), obstacle));
        assertFalse(blocked(platform, new V(0, .2, 0), obstacle));
    }
}
