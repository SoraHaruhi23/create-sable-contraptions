package dev.createsablecontraptions.physics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DockingSpeedTest {
    @Test void noInterfaceDoesNotAlterCruisingSpeed() { assertEquals(1, new DockingSpeed().update(Double.POSITIVE_INFINITY, .4, false)); }
    @Test void approachDeceleratesBeforeTheHandshake() {
        var speed = new DockingSpeed(); double previous = 1;
        for (double distance = 3.5; distance >= .25; distance -= .25) {
            double next = speed.update(distance, .3, false);
            assertTrue(next <= previous); assertTrue(next > 0); previous = next;
        }
        assertTrue(previous <= .4);
    }
    @Test void transferHoldsAtZero() {
        var speed = new DockingSpeed();
        for (int i = 0; i < 50; i++) assertEquals(0, speed.update(.1, .5, true));
    }
    @Test void departureAcceleratesInsteadOfJumpingToCruise() {
        var speed = new DockingSpeed(); speed.update(0, .5, true);
        double previous = 0;
        for (int i = 0; i < 3; i++) {
            double next = speed.update(Double.POSITIVE_INFINITY, .5, false);
            assertTrue(next >= previous && next - previous <= .400001); previous = next;
        }
        assertEquals(1, previous, 1e-6);
    }
    @Test void removedInterfaceReleasesApproachSlowdown() {
        var speed = new DockingSpeed(); speed.update(.1, .5, false);
        for (int i = 0; i < 25; i++) speed.update(Double.POSITIVE_INFINITY, .5, false);
        assertEquals(1, speed.update(Double.POSITIVE_INFINITY, .5, false));
    }
    @Test void stationaryMechanismDoesNotProduceNaN() {
        assertTrue(Double.isFinite(new DockingSpeed().update(0, 0, false)));
    }
    @Test void distantDockDoesNotCauseLongSlowCruising() {
        var speed = new DockingSpeed();
        for (int i = 0; i < 20; i++) assertEquals(1, speed.update(2, .3, false));
    }
    @Test void nearDockBrakesWithinThreeTicks() {
        var speed = new DockingSpeed();
        for (int i = 0; i < 3; i++) speed.update(.15, .3, false);
        assertEquals(.15, speed.update(.15, .3, false), 1e-6);
        assertEquals(0, speed.update(.15, .3, true));
    }
}
