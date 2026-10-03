package dev.createsablecontraptions.physics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CartPushTest {
    @Test void convertsSecondsToTicks() { assertEquals(.1,CartPush.velocity(20,10),1e-9); }
    @Test void heavierStructureAcceleratesLess() { assertTrue(CartPush.velocity(20,100)<CartPush.velocity(20,10)); }
    @Test void oppositePunchReversesImpulse() { assertEquals(-CartPush.velocity(20,10),CartPush.velocity(-20,10)); }
    @Test void invalidInputCannotCorruptVelocity() {
        assertEquals(0,CartPush.velocity(Double.NaN,10)); assertEquals(0,CartPush.velocity(20,0));
        assertEquals(0,CartPush.velocity(20,Double.POSITIVE_INFINITY));
    }
}
