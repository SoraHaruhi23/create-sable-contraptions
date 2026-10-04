package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import org.joml.Quaterniond;
import static org.junit.jupiter.api.Assertions.*;

class CartCouplingAnchorTest {
    @Test void secondaryAnchorMatchesAssemblyInAllFourDirections() {
        double[][] cases = {{0,4,0},{90,0,4},{180,-4,0},{270,0,-4}};
        for (var c : cases) {
            var p = CartCouplingAnchor.offset(c[0],4);
            assertEquals(c[1],p.x,1e-9);
            assertEquals(0,p.y,1e-9);
            assertEquals(c[2],p.z,1e-9);
        }
    }
    @Test void turningAndSlopesPreserveTheSeparateAttachmentAndCouplingLength() {
        for (double yaw : new double[] {0,90,180,270}) {
            var local = CartCouplingAnchor.offset(yaw,5);
            var rotation = new Quaterniond().rotateY(.73).rotateZ(.28);
            var world = rotation.transform(local);
            assertEquals(5,world.length(),1e-9);
            var recovered = rotation.conjugate().transform(world);
            assertTrue(recovered.distance(CartCouplingAnchor.offset(yaw,5))<1e-9);
        }
    }
}
