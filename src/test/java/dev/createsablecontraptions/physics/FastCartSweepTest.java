package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FastCartSweepTest {
    private static final SweptBox.Box BODY=SweptBox.Box.aligned(0,0,0,1,1,1);
    @Test void thinWallCannotBeSkippedWhenBothEndpointsAreClear() {
        var wall=SweptBox.Box.aligned(9,.1,.1,9.01,.9,.9);
        assertFalse(SweptBox.blocked(BODY,new SweptBox.V(0,0,0),wall));
        assertTrue(SweptBox.blocked(BODY,new SweptBox.V(30,0,0),wall));
        assertFalse(SweptBox.blocked(SweptBox.Box.aligned(30,0,0,31,1,1),new SweptBox.V(0,0,0),wall));
    }
    @Test void negativeHighSpeedAlsoSweepsTheWholePath() {
        assertTrue(SweptBox.blocked(BODY,new SweptBox.V(-30,0,0),SweptBox.Box.aligned(-10,0,0,-9,1,1)));
    }
    @Test void diagonalTravelHitsIntermediateBlock() {
        assertTrue(SweptBox.blocked(BODY,new SweptBox.V(20,0,20),SweptBox.Box.aligned(10,0,10,11,1,11)));
    }
    @Test void overheadClearanceDoesNotCauseFalseStops() {
        assertFalse(SweptBox.blocked(BODY,new SweptBox.V(40,0,0),SweptBox.Box.aligned(10,1,0,11,2,1)));
    }
}
