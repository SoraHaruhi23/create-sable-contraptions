package dev.createsablecontraptions.elevator;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssemblyUpdatesTest {
    @Test void neighbourCallbacksSeeTheCompleteStructureOnlyAfterCommit() {
        var blocks = new ArrayList<String>();
        try (var scope = AssemblyUpdates.begin()) {
            assertTrue(AssemblyUpdates.defer(() -> assertEquals(List.of("support", "plate", "anchor"), blocks)));
            blocks.addAll(List.of("support", "plate", "anchor"));
            scope.commit();
        }
        assertFalse(AssemblyUpdates.defer(() -> fail("No assembly active")));
    }
    @Test void failedTransferDoesNotReplayDestinationCallbacks() {
        var calls = new ArrayList<String>();
        assertThrows(IllegalStateException.class, () -> {
            try (var scope = AssemblyUpdates.begin()) {
                AssemblyUpdates.defer(() -> calls.add("lost pressure plate"));
                throw new IllegalStateException("verification failure");
            }
        });
        assertTrue(calls.isEmpty());
        assertFalse(AssemblyUpdates.defer(() -> fail("Scope leaked")));
    }
    @Test void nestedTransferWaitsForOuterCommitAndRestoresScopeOnCallbackFailure() {
        var calls = new ArrayList<Integer>();
        try (var outer = AssemblyUpdates.begin()) {
            AssemblyUpdates.defer(() -> calls.add(1));
            try (var inner = AssemblyUpdates.begin()) {
                AssemblyUpdates.defer(() -> calls.add(2));
                inner.commit();
            }
            assertTrue(calls.isEmpty());
            outer.commit();
        }
        assertEquals(List.of(1, 2), calls);
        assertThrows(IllegalArgumentException.class, () -> {
            try (var scope = AssemblyUpdates.begin()) {
                AssemblyUpdates.defer(() -> { throw new IllegalArgumentException(); });
                scope.commit();
            }
        });
        assertFalse(AssemblyUpdates.defer(() -> {}));
    }
}
