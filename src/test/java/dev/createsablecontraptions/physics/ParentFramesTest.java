package dev.createsablecontraptions.physics;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ParentFramesTest {
    @Test void invalidAncestorAndCyclesRejectTheWholeChain() {
        var parents = Map.of("inner", "outer", "outer", "ship");
        assertTrue(ParentFrames.validChain("inner", parents::get, item -> true));
        assertFalse(ParentFrames.validChain("inner", parents::get, item -> !item.equals("ship")));
        var cycle = Map.of("a", "b", "b", "a");
        assertFalse(ParentFrames.validChain("a", cycle::get, item -> true));
        assertFalse(ParentFrames.validChain("a", item -> item, item -> true));
    }
    @Test void orderingKeepsSiblingOrderAndResolvesDepthOnlyOncePerItem() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        assertEquals(List.of("c", "a", "b"), ParentFrames.ordered(List.of("c", "a", "b"), item -> {
            calls.incrementAndGet(); return null;
        }));
        assertEquals(3, calls.get());
    }
    @Test void parentIsUpdatedBeforeChildRegardlessOfAllocationOrder() {
        var parents=Map.of("inner","outer","outer","ship");
        assertEquals(List.of("ship","outer","inner"), ParentFrames.ordered(List.of("inner","ship","outer"),parents::get));
    }
    @Test void descendantsAreNotConfusedWithAncestorsOrSiblings() {
        var parents=Map.of("inner","outer","sibling","outer","outer","ship");
        assertTrue(ParentFrames.descendant("inner","ship",parents::get));
        assertFalse(ParentFrames.descendant("outer","inner",parents::get));
        assertFalse(ParentFrames.descendant("inner","sibling",parents::get));
        assertFalse(ParentFrames.descendant("inner","inner",parents::get));
    }
    @Test void malformedCyclesTerminate() {
        var parents=Map.of("a","b","b","a");
        assertFalse(ParentFrames.descendant("a","missing",parents::get));
        assertEquals(2,ParentFrames.ordered(List.of("a","b"),parents::get).size());
    }
}
