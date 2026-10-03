package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CartMountTest {
    @Test void mountedBlocksFitBetweenFloorAndCeilingOnTheOriginalGrid() {
        double bottom=.0625+.1875-CartMount.ATTACHMENT_Y;
        assertEquals(0,bottom,1e-12);
        var block=SweptBox.Box.aligned(0,bottom,0,1,bottom+1,1);
        var motion=new SweptBox.V(.2,0,0);
        assertFalse(SweptBox.blocked(block,motion,SweptBox.Box.aligned(-2,-1,-2,2,0,2)));
        assertFalse(SweptBox.blocked(block,motion,SweptBox.Box.aligned(-2,1,-2,2,2,2)));
    }
    @Test void correctedMountStillCollidesWithAnActualFrontWall() {
        double bottom=-3+.0625+.1875-CartMount.ATTACHMENT_Y;
        assertEquals(-3,bottom,1e-12);
        assertTrue(SweptBox.blocked(SweptBox.Box.aligned(0,bottom,0,1,bottom+1,1),new SweptBox.V(2,0,0),
                SweptBox.Box.aligned(2,-3,0,3,-2,1)));
    }
}
