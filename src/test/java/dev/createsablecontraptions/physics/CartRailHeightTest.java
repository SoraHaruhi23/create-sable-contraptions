package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Vanilla rail Y and Create's attachment offset, without booting Minecraft. */
class CartRailHeightTest {
    private static SweptBox.Box ordinaryBlock(double cartY) {
        double bottom=cartY+.1875-.19;
        return SweptBox.Box.aligned(0,bottom,0,1,bottom+1,1);
    }
    @Test void temporaryRailSnapFalselyEmbedsAnOrdinaryBlockInFloor() {
        var floor=SweptBox.Box.aligned(-10,-1,-10,10,0,10);
        var motion=new SweptBox.V(.00015,0,0);
        assertTrue(SweptBox.blocked(ordinaryBlock(0),motion,floor));
        assertFalse(SweptBox.blocked(ordinaryBlock(.0625),motion,floor));
    }
    @Test void correctedPoseStillStopsAtFrontWallAtHighSpeed() {
        assertTrue(SweptBox.blocked(ordinaryBlock(.0625),new SweptBox.V(20,0,0),
                SweptBox.Box.aligned(5,0,0,6,1,1)));
    }
    @Test void negativeWorldHeightAndReverseMotionKeepFloorClearance() {
        assertFalse(SweptBox.blocked(ordinaryBlock(-3+.0625),new SweptBox.V(-.2,0,0),
                SweptBox.Box.aligned(-10,-4,-10,10,-3,10)));
    }
    @Test void realDownwardMotionIsNotExemptFromFloorCollision() {
        assertTrue(SweptBox.blocked(ordinaryBlock(.0625),new SweptBox.V(.1,-.2,0),
                SweptBox.Box.aligned(-10,-1,-10,10,0,10)));
    }
}
