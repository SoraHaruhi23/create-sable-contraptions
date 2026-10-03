package dev.createsablecontraptions.physics;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RigidSweepTest {
    private final SweptBox.Box arm=SweptBox.Box.aligned(2.5,-.2,-.2,3.5,.2,.2);
    @Test void turningCornerHitsBetweenClearEndpoints() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(),new Quaterniond(),new Quaterniond().rotationY(Math.PI/2));
        assertTrue(sweep.blocked(SweptBox.Box.aligned(2,-.2,-2.2,2.2,.2,-2)));
    }
    @Test void translationAndRotationAreCheckedTogether() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(2,0,0),new Quaterniond(),new Quaterniond().rotationY(Math.PI/2));
        assertTrue(sweep.blocked(SweptBox.Box.aligned(3,-.2,-2.2,3.2,.2,-2)));
        assertFalse(sweep.blocked(SweptBox.Box.aligned(-3,-.2,2,-2,.2,3)));
    }
    @Test void axisTangencyAndNearbyEmptySpaceRemainClear() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(),new Quaterniond(),new Quaterniond().rotationY(Math.PI/2));
        assertFalse(sweep.blocked(SweptBox.Box.aligned(-5,.2,-5,5,1,5)));
    }
    @Test void pureTranslationStillDetectsThinObstacles() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(0,0,8),new Quaterniond(),new Quaterniond());
        assertTrue(sweep.blocked(SweptBox.Box.aligned(2,-1,4,4,1,4.01)));
    }
    @Test void quaternionSignDoesNotIntroduceAFullTurn() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(),new Quaterniond(),new Quaterniond(0,0,0,-1));
        assertEquals(0,sweep.angle(),1e-10);
        assertFalse(sweep.blocked(SweptBox.Box.aligned(-4,-1,-1,-2,1,1)));
    }
    @Test void worldRotationAxisIsRespectedAfterAnInitialTilt() {
        var from=new Quaterniond().rotationX(.7).rotateZ(.3);
        var to=new Quaterniond().rotationY(.8).mul(from);
        var sweep=new RigidSweep(arm,new Vector3d(600,70,-400),new Vector3d(600,70,-400),from,to);
        var center=sweep.point(new Vector3d(3,0,0),.5);
        assertTrue(sweep.blocked(SweptBox.Box.aligned(center.x-.02,center.y-.02,center.z-.02,center.x+.02,center.y+.02,center.z+.02)));
        assertFalse(sweep.blocked(SweptBox.Box.aligned(590,80,-410,610,81,-390)));
    }
    @Test void translationAlongRotationAxisCanCloseAnInitiallyClearGap() {
        var sweep=new RigidSweep(arm,new Vector3d(),new Vector3d(0,2,0),new Quaterniond(),new Quaterniond().rotationY(Math.PI/2));
        assertTrue(sweep.blocked(SweptBox.Box.aligned(-4,1,-4,4,1.1,4)));
        assertFalse(sweep.blocked(SweptBox.Box.aligned(-4,2.2,-4,4,3,4)));
    }
    @Test void reverseTurnHasTheSameSweptVolume() {
        var a=new Vector3d();var b=new Vector3d(2,0,1);var q=new Quaterniond().rotationY(1.2);
        var forward=new RigidSweep(arm,a,b,new Quaterniond(),q);
        var backward=new RigidSweep(arm,b,a,q,new Quaterniond());
        var point=forward.point(new Vector3d(3,0,0),.4);
        var obstacle=SweptBox.Box.aligned(point.x-.05,-.1,point.z-.05,point.x+.05,.1,point.z+.05);
        assertTrue(forward.blocked(obstacle));assertTrue(backward.blocked(obstacle));
        assertTrue(forward.centerMotion().add(backward.centerMotion()).length()<1e-9);
    }
}
