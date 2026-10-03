package dev.createsablecontraptions.physics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DrillHemisphereTest {
    @Test void forwardTargetIsAccepted() { assertTrue(DrillHemisphere.inFront(0, 0, 1, 0, 0, 1)); }
    @Test void frontDiagonalIsAccepted() { assertTrue(DrillHemisphere.inFront(1, .5, .1, 0, 0, 1)); }
    @Test void pureSideIsExcluded() { assertFalse(DrillHemisphere.inFront(1, 0, 0, 0, 0, 1)); }
    @Test void rearIsExcluded() { assertFalse(DrillHemisphere.inFront(.5, 0, -.1, 0, 0, 1)); }
    @Test void downwardFacingUsesItsOwnHemisphere() {
        assertTrue(DrillHemisphere.inFront(.7, -.2, .3, 0, -1, 0));
        assertFalse(DrillHemisphere.inFront(.7, 0, .3, 0, -1, 0));
    }
    @Test void rotatingDrillUsesWorldFacing() {
        double s = Math.sqrt(.5);
        assertTrue(DrillHemisphere.inFront(1, 1, 0, s, s, 0));
        assertFalse(DrillHemisphere.inFront(-1, 1, 0, s, s, 0));
    }
}
