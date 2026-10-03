package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CartDirectionTest {
    @Test void slowReverseReplacesFastForwardCruise() {
        assertTrue(CartDirection.reversed(-.00015,0,0,.4,0,0));
        assertTrue(CartDirection.reversed(0,0,.01,0,0,-.4));
    }
    @Test void stoppedAndSameDirectionDoNotInventAReversal() {
        assertFalse(CartDirection.reversed(0,0,0,.4,0,0));
        assertFalse(CartDirection.reversed(.01,0,0,.4,0,0));
    }
    @Test void retreatCanPassWhenForwardPredictionIsBlocked() {
        var body=SweptBox.Box.aligned(0,0,0,1,1,1);
        var obstacle=SweptBox.Box.aligned(1.2,0,0,2.2,1,1);
        assertTrue(SweptBox.blocked(body,new SweptBox.V(.4,0,0),obstacle));
        assertFalse(SweptBox.blocked(body,new SweptBox.V(-.01,0,0),obstacle));
    }
}
