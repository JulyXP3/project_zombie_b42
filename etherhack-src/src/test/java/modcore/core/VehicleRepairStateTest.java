package modcore.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleRepairStateTest {
    @Test
    void reservationFollowsTheItemIdWhenTheInventoryObjectIsReplaced() {
        VehicleRepairState state = connected();
        assertTrue(state.claim(new Object(), 77, "Base.NormalCarSeat"));
        assertTrue(state.blocks(77, "Base.NormalCarSeat"));
        assertFalse(state.blocks(78, "Base.NormalCarSeat"));
        assertFalse(state.blocks(77, "Base.Screwdriver"));
    }

    @Test
    void anotherPartCannotReplaceAnActiveReservation() {
        VehicleRepairState state = connected();
        Object part = new Object();
        assertTrue(state.claim(part, 77, "Base.NormalCarSeat"));
        assertFalse(state.claim(new Object(), 78, "Base.CarBattery1"));
        assertTrue(state.owns(part, 77, "Base.NormalCarSeat"));
    }

    @Test
    void pendingActionPreventsBothASecondSendAndAnEarlyRelease() {
        VehicleRepairState state = claimed();
        assertTrue(state.startAction((byte) 1, new Object()));
        assertFalse(state.startAction((byte) 2, new Object()));
        assertFalse(state.release());
        assertTrue(state.blocks(77, "Base.NormalCarSeat"));
    }

    @Test
    void actionMustExistAndMatchBeforeARejectionIsAccepted() {
        VehicleRepairState state = claimed();
        state.startAction((byte) 1, new Object());
        assertEquals(VehicleRepairState.Status.MISSING, state.observeAction(null, false, true));
        assertFalse(state.completeAction());
        assertFalse(state.release());
    }

    @Test
    void reusedNativeIdDoesNotTurnAnUnrelatedActionIntoOurReceipt() {
        VehicleRepairState state = claimed();
        state.startAction((byte) 1, new Object());
        assertEquals(VehicleRepairState.Status.MISSING,
                state.observeAction(new Object(), true, false));
        assertFalse(state.completeAction());
    }

    @Test
    void doneReceiptAloneDoesNotReleaseTheItemBeforeTheControllerChecksOwnership() {
        VehicleRepairState state = claimed();
        Object action = new Object();
        state.startAction((byte) -128, action);
        assertEquals(VehicleRepairState.Status.DONE, state.observeAction(action, true, false));
        assertFalse(state.release());
        assertTrue(state.completeAction());
        assertTrue(state.blocks(77, "Base.NormalCarSeat"));
        assertTrue(state.release());
        assertFalse(state.blocks(77, "Base.NormalCarSeat"));
    }

    @Test
    void quarantineCannotBeClearedByAnUnverifiedStopOrAnotherClaim() {
        VehicleRepairState state = claimed();
        state.quarantine();
        assertFalse(state.release());
        assertFalse(state.claim(new Object(), 78, "Base.CarBattery1"));
        assertTrue(state.blocks(77, "Base.NormalCarSeat"));
    }

    @Test
    void reconnectDropsTheOldLeaseAndItsReceipt() {
        VehicleRepairState state = claimed();
        state.startAction((byte) 1, new Object());
        state.quarantine();
        state.connect(new Object(), new Object());
        assertFalse(state.blocks(77, "Base.NormalCarSeat"));
        assertEquals(VehicleRepairState.Status.IDLE, state.status());
        assertTrue(state.claim(new Object(), 78, "Base.CarBattery1"));
    }

    @Test
    void sameConnectionDoesNotClearAnUnresolvedReservation() {
        Object owner = new Object();
        Object connection = new Object();
        VehicleRepairState state = new VehicleRepairState();
        state.connect(owner, connection);
        state.claim(new Object(), 77, "Base.NormalCarSeat");
        state.quarantine();
        state.connect(owner, connection);
        assertTrue(state.blocks(77, "Base.NormalCarSeat"));
        assertTrue(state.isQuarantined());
    }

    @Test
    void offlineStateCannotClaimAnItem() {
        VehicleRepairState state = claimed();
        state.connect(null, null);
        assertFalse(state.blocks(77, "Base.NormalCarSeat"));
        assertFalse(state.claim(new Object(), 77, "Base.NormalCarSeat"));
    }

    private static VehicleRepairState connected() {
        VehicleRepairState state = new VehicleRepairState();
        state.connect(new Object(), new Object());
        return state;
    }

    private static VehicleRepairState claimed() {
        VehicleRepairState state = connected();
        state.claim(new Object(), 77, "Base.NormalCarSeat");
        return state;
    }
}
