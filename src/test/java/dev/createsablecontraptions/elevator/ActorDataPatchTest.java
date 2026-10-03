package dev.createsablecontraptions.elevator;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ActorDataPatchTest {
    @Test void capabilityInventoryChangesAreNotOverwrittenByStaleActorSnapshot() {
        var before = Map.of("Items", "64 stone", "Progress", "0");
        var after = Map.of("Items", "64 stone", "Progress", "1");
        var physical = Map.of("Items", "32 stone", "Progress", "0");
        assertEquals(Map.of("Items", "32 stone", "Progress", "1"), ActorDataPatch.merge(before, after, physical));
    }
    @Test void actorRemovingOwnedInventoryDoesNotRemoveUnrelatedCapabilityData() {
        assertEquals(Map.of("OtherModInventory", "tools"), ActorDataPatch.merge(
                Map.of("Inventory", "deployer hand"), Map.of(),
                Map.of("Inventory", "deployer hand", "OtherModInventory", "tools")));
    }
    @Test void plotCoordinatesAndBlockEntityIdentityCannotBeReplacedByLocalSnapshot() {
        var physical = Map.of("id", "actual", "x", "1000000", "y", "64", "z", "2000000");
        assertEquals(physical, ActorDataPatch.merge(Map.of(), Map.of("id", "wrong", "x", "1", "y", "2", "z", "3"), physical));
    }
    @Test void stopMovingRestoresActorOwnedDataWithoutMutatingTheInput() {
        var physical = Map.of("Other", "keep");
        assertEquals(Map.of("Inventory", "remaining tools", "Other", "keep"),
                ActorDataPatch.merge(Map.of(), Map.of("Inventory", "remaining tools"), physical));
        assertEquals(Map.of("Other", "keep"), physical);
    }
}
