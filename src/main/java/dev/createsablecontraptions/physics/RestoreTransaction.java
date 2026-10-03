package dev.createsablecontraptions.physics;

import java.util.ArrayDeque;
import java.util.ArrayList;

/** One family commit; undo every acquired resource even if one cleanup fails. */
public final class RestoreTransaction {
    private final ArrayDeque<Runnable> undo = new ArrayDeque<>();
    private final ArrayList<Runnable> commit = new ArrayList<>();
    public void undo(Runnable action) { undo.push(action); }
    public void onCommit(Runnable action) { commit.add(action); }
    public void commit() {
        for (var action : commit) action.run();
        undo.clear(); commit.clear();
    }
    public void rollback(RuntimeException original) {
        while (!undo.isEmpty()) {
            try { undo.pop().run(); } catch (RuntimeException cleanup) { original.addSuppressed(cleanup); }
        }
        commit.clear();
    }
}
