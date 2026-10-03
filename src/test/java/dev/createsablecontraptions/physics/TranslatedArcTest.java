package dev.createsablecontraptions.physics;
import org.junit.jupiter.api.Test;
import org.joml.Vector3d;
import static org.junit.jupiter.api.Assertions.*;

class TranslatedArcTest {
    private static final SweptBox.Box BOX = SweptBox.Box.aligned(-.2,-.2,-.2,.2,.2,.2);
    @Test void stableChildKeepsOrientationWhileAttachmentOrbits() {
        var steps = TranslatedArc.steps(BOX,t -> new Vector3d(2*Math.cos(t),2*Math.sin(t),0),2,1);
        for (var s : steps) { assertEquals(BOX.x(),s.box().x()); assertEquals(BOX.y(),s.box().y()); }
        assertEquals(2,steps.getFirst().box().center().x(),1e-9);
        var last=steps.getLast();
        assertEquals(2*Math.sin(1),last.box().center().y()+last.motion().y(),1e-9);
    }
    @Test void curvedPathHitsObstacleAwayFromEndpointChord() {
        var obstacle = SweptBox.Box.aligned(1.35,1.35,-.3,1.5,1.5,.3);
        var steps=TranslatedArc.steps(BOX,t -> new Vector3d(2*Math.cos(t*Math.PI/2),2*Math.sin(t*Math.PI/2),0),2,Math.PI/2);
        assertTrue(steps.stream().anyMatch(s -> SweptBox.blocked(s.box(),s.motion(),obstacle)));
        assertFalse(SweptBox.blocked(new SweptBox.Box(new SweptBox.V(2,0,0),BOX.half(),BOX.x(),BOX.y(),BOX.z()),new SweptBox.V(-2,2,0),obstacle));
    }
    @Test void separatedObstacleAllowsResume() {
        var obstacle=SweptBox.Box.aligned(4,4,4,5,5,5);
        assertTrue(TranslatedArc.steps(BOX,t -> new Vector3d(2*Math.cos(t),2*Math.sin(t),0),2,1).stream()
                .noneMatch(s -> SweptBox.blocked(s.box(),s.motion(),obstacle)));
    }
    @Test void linearChildUsesContinuousSweep() {
        var steps=TranslatedArc.steps(BOX,t -> new Vector3d(5*t,0,0),0,0);
        assertEquals(1,steps.size());
        assertTrue(SweptBox.blocked(steps.getFirst().box(),steps.getFirst().motion(),SweptBox.Box.aligned(2,-1,-1,3,1,1)));
    }
    @Test void tangentAndReverseMotionRemainFree() {
        var obstacle=SweptBox.Box.aligned(.2,-1,-1,1,1,1);
        assertTrue(TranslatedArc.steps(BOX,t -> new Vector3d(-t,0,0),0,0).stream()
                .noneMatch(s -> SweptBox.blocked(s.box(),s.motion(),obstacle)));
    }
    @Test void excessiveSweepFailsClosed() {
        assertThrows(IllegalArgumentException.class,() -> TranslatedArc.steps(BOX,t -> new Vector3d(),1e6,10));
    }
}
