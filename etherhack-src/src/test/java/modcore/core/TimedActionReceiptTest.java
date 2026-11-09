package modcore.core;

import org.junit.jupiter.api.Test;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.ActionManager;
import zombie.core.NetTimedAction;
import zombie.core.Transaction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads real game action entries without creating a world or sending packets. */
class TimedActionReceiptTest {
    @Test
    void readsPendingDoneAndRejectedFromTheRealStaticQueue() throws Exception {
        KahluaTable table = J2SEPlatform.getInstance().newTable();
        NetTimedAction action = new NetTimedAction();
        action.action = table;
        var queue = actions();
        VarHandle state = state();
        queue.add(action);
        try {
            state.set(action, Transaction.TransactionState.Request);
            assertEquals(0, TimedActionReceipt.state(table));
            state.set(action, Transaction.TransactionState.Accept);
            assertEquals(0, TimedActionReceipt.state(table));
            state.set(action, Transaction.TransactionState.Done);
            assertEquals(1, TimedActionReceipt.state(table));
            state.set(action, Transaction.TransactionState.Reject);
            assertEquals(2, TimedActionReceipt.state(table));
        } finally {
            queue.remove(action);
        }
    }

    @Test
    void aMissingActionIsUnknownRatherThanRejected() {
        assertEquals(3, TimedActionReceipt.state(J2SEPlatform.getInstance().newTable()));
        assertEquals(3, TimedActionReceipt.state(null));
    }

    @Test
    void removingAnOwnedActionLeavesOtherActionsInTheQueue() throws Exception {
        NetTimedAction owned = new NetTimedAction();
        NetTimedAction other = new NetTimedAction();
        owned.action = J2SEPlatform.getInstance().newTable();
        other.action = J2SEPlatform.getInstance().newTable();
        var queue = actions();
        queue.add(owned);
        queue.add(other);
        try {
            TimedActionReceipt.remove(owned.action);
            assertFalse(queue.contains(owned));
            assertTrue(queue.contains(other));
            TimedActionReceipt.remove(owned.action);
            assertTrue(queue.contains(other));
        } finally {
            queue.remove(owned);
            queue.remove(other);
        }
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentLinkedQueue<Object> actions() throws Exception {
        var lookup = MethodHandles.privateLookupIn(ActionManager.class, MethodHandles.lookup());
        return (ConcurrentLinkedQueue<Object>) lookup.findStaticVarHandle(ActionManager.class,
                "actions", ConcurrentLinkedQueue.class).get();
    }

    private static VarHandle state() throws Exception {
        Class<?> action = Class.forName("zombie.core.Action");
        var lookup = MethodHandles.privateLookupIn(action, MethodHandles.lookup());
        return lookup.findVarHandle(action, "state", Transaction.TransactionState.class);
    }
}
