package dev.createsablecontraptions.elevator;

import java.util.*;

/** Apply only actor-written keys; retain physical inventory edits made through capability views. */
public final class ActorDataPatch {
    private static final Set<String> LOCATION = Set.of("id", "x", "y", "z");
    private ActorDataPatch() {}
    public static <T> Map<String, T> merge(Map<String, T> before, Map<String, T> after, Map<String, T> physical) {
        var result = new HashMap<>(physical);
        var keys = new HashSet<>(before.keySet()); keys.addAll(after.keySet());
        for (String key : keys) {
            if (LOCATION.contains(key) || Objects.equals(before.get(key), after.get(key))) continue;
            if (after.containsKey(key)) result.put(key, after.get(key)); else result.remove(key);
        }
        return result;
    }
}
