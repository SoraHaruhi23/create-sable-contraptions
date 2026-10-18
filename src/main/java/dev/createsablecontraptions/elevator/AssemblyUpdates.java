package dev.createsablecontraptions.elevator;

import java.util.ArrayList;
import java.util.List;

/** Keep neighbour callbacks outside the incomplete assembly/rollback transaction. */
public final class AssemblyUpdates implements AutoCloseable {
    private static final ThreadLocal<AssemblyUpdates> CURRENT = new ThreadLocal<>();
    private final AssemblyUpdates parent;
    private final List<Runnable> pending = new ArrayList<>();
    private boolean closed;
    private AssemblyUpdates() { parent = CURRENT.get(); CURRENT.set(this); }
    public static AssemblyUpdates begin() { return new AssemblyUpdates(); }
    public static boolean defer(Runnable notification) {
        var scope = CURRENT.get();
        if (scope == null) return false;
        scope.pending.add(notification);
        return true;
    }
    public void commit() {
        if (closed) throw new IllegalStateException("Assembly notifications already closed");
        close();
        if (parent != null) parent.pending.addAll(pending);
        else for (var notification : pending) notification.run();
        pending.clear();
    }
    @Override public void close() {
        if (closed) return;
        if (CURRENT.get() != this) throw new IllegalStateException("Assembly scope closed out of order");
        if (parent == null) CURRENT.remove(); else CURRENT.set(parent);
        closed = true;
    }
}
