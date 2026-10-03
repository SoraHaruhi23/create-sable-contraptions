package dev.createsablecontraptions.physics;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RestoreTransactionTest {
    @Test void childFailureRollsBackTheEntireFamilyInReverseOrder() {
        var tx=new RestoreTransaction();var live=new ArrayList<>(List.of("root","child","grandchild"));var removed=new ArrayList<String>();
        for(var name:List.copyOf(live))tx.undo(()->{live.remove(name);removed.add(name);});
        boolean[] snapshot={true};tx.onCommit(()->snapshot[0]=false);
        tx.rollback(new IllegalStateException("child BE failed"));
        assertTrue(live.isEmpty());assertEquals(List.of("grandchild","child","root"),removed);assertTrue(snapshot[0]);
    }
    @Test void cleanupFailureDoesNotPreventOtherBodiesBeingRemoved() {
        var tx=new RestoreTransaction();boolean[] removed={false};
        tx.undo(()->removed[0]=true);tx.undo(()->{throw new IllegalStateException("cleanup");});
        var failure=new IllegalArgumentException("original");tx.rollback(failure);
        assertTrue(removed[0]);assertEquals(1,failure.getSuppressed().length);
    }
    @Test void commitClearsSnapshotsOnlyAfterTheWholeFamilySucceeds() {
        var tx=new RestoreTransaction();boolean[] live={true},snapshot={true};
        tx.undo(()->live[0]=false);tx.onCommit(()->snapshot[0]=false);
        assertTrue(snapshot[0]);tx.commit();tx.rollback(new IllegalStateException());
        assertTrue(live[0]);assertFalse(snapshot[0]);
    }
    @Test void partiallyFailedCommitCanRestoreItsSnapshot() {
        var tx=new RestoreTransaction();boolean[] snapshot={true};
        tx.undo(()->snapshot[0]=true);tx.onCommit(()->snapshot[0]=false);
        tx.onCommit(()->{throw new IllegalStateException();});
        var ex=assertThrows(IllegalStateException.class,tx::commit);tx.rollback(ex);assertTrue(snapshot[0]);
    }
}
