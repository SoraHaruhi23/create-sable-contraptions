package dev.createsablecontraptions.physics;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CarriedFrameTest {
    @Test void childFollowsUpdatedParentAndStillHitsAnExternalCeiling() {
        var box=SweptBox.Box.aligned(1,3,0,2,4,1);
        var moved=CarriedFrame.move(box,new Vector3d(),new Quaterniond(),new Vector3d(0,5,0),new Quaterniond());
        assertEquals(8.5,moved.center().y(),1e-9);
        assertTrue(SweptBox.blocked(moved,new SweptBox.V(0,2,0),SweptBox.Box.aligned(1,10,0,2,11,1)));
        assertFalse(SweptBox.blocked(moved,new SweptBox.V(0,-2,0),SweptBox.Box.aligned(1,10,0,2,11,1)));
    }
    @Test void tiltedParentCarriesOrientationAsWellAsPosition() {
        var moved=CarriedFrame.move(SweptBox.Box.aligned(2,-.25,-.25,3,.25,.25),new Vector3d(),new Quaterniond(),
                new Vector3d(10,20,30),new Quaterniond().rotationZ(Math.PI/2));
        assertEquals(10,moved.center().x(),1e-8);
        assertEquals(22.5,moved.center().y(),1e-8);
        assertEquals(1,moved.x().y(),1e-8);
    }
}
