package dev.createsablecontraptions.elevator;

import java.util.LinkedHashSet;

/** Copy-on-write membership: transfer snapshots survive the removal callbacks they trigger. */
public final class LiveBlockIndex {
    private LiveBlockIndex() {}
    public static long[] update(long[] saved, long position, boolean present) {
        // Redstone, animation and other state-only changes do not change membership.
        boolean exists = false;
        for (long packed : saved) if (packed == position) { exists = true; break; }
        if (exists == present) return saved;
        var blocks = new LinkedHashSet<Long>();
        for (long packed : saved) blocks.add(packed);
        if (present) blocks.add(position); else blocks.remove(position);
        return blocks.stream().mapToLong(Long::longValue).toArray();
    }
}
