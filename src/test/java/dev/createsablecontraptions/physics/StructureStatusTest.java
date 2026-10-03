package dev.createsablecontraptions.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StructureStatusTest {
    @Test void incompleteChecksHaveSpecificReasons() {
        for(int code:new int[]{StructureStatus.UNLOADED,StructureStatus.BUDGET,StructureStatus.BOUNDARY,StructureStatus.UNAVAILABLE}) {
            assertEquals("stopped",StructureStatus.stateKey(code));
            assertFalse(StructureStatus.reasonKey(code).isEmpty());
            assertNotEquals("collision",StructureStatus.reasonKey(code));
        }
    }
    @Test void stationaryDoesNotInventAStallOrPowerFailure() {
        assertEquals(StructureStatus.IDLE,StructureStatus.choose(false,0,false,false));
        assertEquals("",StructureStatus.reasonKey(StructureStatus.IDLE));
    }
    @Test void collisionWinsOverResidualMotion() {
        assertEquals(StructureStatus.COLLISION,StructureStatus.choose(true,0,true,true));
    }
    @Test void activeWorkExplainsAStopWhileTouchingAnObstacle() {
        for(int actor:new int[]{StructureStatus.DRILL,StructureStatus.DEPLOYER,StructureStatus.TRANSFER})
            assertEquals(actor,StructureStatus.choose(true,actor,true,false));
    }
    @Test void releaseClearsThePreviousReason() {
        assertEquals(StructureStatus.MOVING,StructureStatus.choose(false,0,false,true));
        assertEquals("",StructureStatus.reasonKey(StructureStatus.MOVING));
    }
    @Test void unknownStopIsReportedWithoutGuessing() {
        assertEquals(StructureStatus.STALLED,StructureStatus.choose(false,0,true,false));
        assertEquals("unspecified",StructureStatus.reasonKey(StructureStatus.STALLED));
        assertEquals("waiting",StructureStatus.stateKey(StructureStatus.UNKNOWN));
    }
}
