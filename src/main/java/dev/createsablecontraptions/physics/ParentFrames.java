package dev.createsablecontraptions.physics;

import java.util.*;
import java.util.function.Function;

/** Parent relationships are independent of Create's entity-passenger tree. */
public final class ParentFrames {
    private ParentFrames() {}
    public static <T> boolean validChain(T item, Function<T, T> parent, java.util.function.Predicate<T> valid) {
        var seen = new HashSet<T>();
        for (T current = item; current != null; current = parent.apply(current))
            if (!seen.add(current) || !valid.test(current)) return false;
        return true;
    }
    public static <T> boolean descendant(T candidate, T ancestor, Function<T, T> parent) {
        var seen = new HashSet<T>();
        seen.add(candidate);
        for (T next = parent.apply(candidate); next != null && seen.add(next); next = parent.apply(next))
            if (next.equals(ancestor)) return true;
        return false;
    }
    public static <T> List<T> ordered(Collection<T> items, Function<T, T> parent) {
        var result = new ArrayList<>(items);
        // Physics sorts twice per step. Resolve each depth once, not on every comparison.
        var depths = new HashMap<T, Integer>();
        for (var item : result) depths.put(item, depth(item, parent));
        result.sort(Comparator.comparingInt(depths::get));
        return result;
    }
    private static <T> int depth(T item, Function<T, T> parent) {
        var seen = new HashSet<T>();
        for (T next = item; next != null && seen.add(next); next = parent.apply(next)) {}
        return seen.size();
    }
}
