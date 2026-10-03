package dev.createsablecontraptions.linear;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinearStepTest {
    @Test void endpointCannotBeOvershotInEitherDirection() {
        var step = new LinearStep();
        assertEquals(.125f, step.accept(.49f, 7.875f, 8, false));
        step.reset();
        assertEquals(-.125f, step.accept(-.49f, .125f, 8, false));
    }
    @Test void repeatedSpeedQueriesDoNotConsumeASecondSequenceStep() {
        var step = new LinearStep();
        assertEquals(.2f, step.accept(.2f, 2, 8, false));
        // Create can decrement its remaining sequence limit before querying speed again.
        assertEquals(.2f, step.accept(0, 2, 8, false));
        assertEquals(.2f, step.take());
    }
    @Test void secondMoveAtTheExtensionLimitDoesNotMoveTwice() {
        var step = new LinearStep();
        step.accept(.1f, 7.9f, 8, false);
        assertEquals(.1f, step.take(), 1e-6f);
        assertEquals(0, step.take());
    }
    @Test void stalledStepDoesNotConsumeProgressAndCanResume() {
        var step = new LinearStep();
        assertEquals(0, step.accept(.3f, 4, 8, true));
        assertEquals(0, step.take());
        step.reset();
        assertEquals(.3f, step.accept(.3f, 4, 8, false));
    }
    @Test void reversalCanEscapeAfterAStalledTick() {
        var step = new LinearStep();
        step.accept(.3f, 4, 8, true);
        step.reset();
        assertFalse(step.checked());
        assertEquals(-.3f, step.accept(-.3f, 4, 8, false));
        assertEquals(-.3f, step.take());
    }
}
