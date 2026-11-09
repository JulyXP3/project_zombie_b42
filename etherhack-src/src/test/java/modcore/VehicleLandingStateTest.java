package modcore;

import modcore.core.VehicleLandingState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VehicleLandingStateTest {

    @Test
    void highFrameRateDescentDoesNotBecomeSettledWhenVelocityIsStale() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        for (int ms = 0; ms <= 208; ms += 8) {
            assertNotEquals("ok", state.sample(ms, 3.7f - 1.3f * ms / 1000.0f, 0.0f, true, true),
                    "small per-frame drops must not hide continuous descent");
        }
    }

    @Test
    void nativeVerticalSpeedRejectsAnApparentlyStationaryHeight() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        for (int ms = 0; ms <= 400; ms += 20) {
            assertEquals("falling", state.sample(ms, 0.27f, -0.8f, true, true));
        }
    }

    @Test
    void supportedAndAlignedPoseNeedsAnObservedTimeWindow() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        assertEquals("settling", state.sample(0, 0.27f, 0.0f, true, true));
        assertEquals("settling", state.sample(100, 0.27f, 0.0f, true, true));
        assertEquals("ok", state.sample(200, 0.27f, 0.0f, true, true));
    }

    @Test
    void airWithoutSupportTimesOutInsteadOfArriving() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        for (int ms = 0; ms < 3000; ms += 20) {
            assertEquals("unsupported", state.sample(ms, 3.97f, 0.0f, false, true));
        }
        assertEquals("timeout-unsupported", state.sample(3000, 3.97f, 0.0f, false, true));
    }

    @Test
    void displayAndNativeSplitCannotCompleteTheObservation() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        for (int ms = 0; ms < 3000; ms += 20) {
            assertEquals("syncing", state.sample(ms, 0.27f, 0.0f, true, false));
        }
        assertEquals("timeout-syncing", state.sample(3000, 0.27f, 0.0f, true, false));
    }

    @Test
    void lostSupportRestartsTheWindow() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        state.sample(0, 0.27f, 0.0f, true, true);
        state.sample(100, 0.27f, 0.0f, true, true);
        assertEquals("unsupported", state.sample(200, 0.27f, 0.0f, false, true));
        assertEquals("settling", state.sample(220, 0.27f, 0.0f, true, true));
        assertEquals("settling", state.sample(320, 0.27f, 0.0f, true, true));
        assertEquals("ok", state.sample(420, 0.27f, 0.0f, true, true));
    }

    @Test
    void longSampleGapIsNotCountedAsObservedStillness() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        state.sample(0, 0.27f, 0.0f, true, true);
        assertEquals("settling", state.sample(1000, 0.27f, 0.0f, true, true));
        state.sample(1100, 0.27f, 0.0f, true, true);
        assertEquals("ok", state.sample(1200, 0.27f, 0.0f, true, true));
    }

    @Test
    void settlingAtTheDeadlineDoesNotOverrideTimeout() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        state.sample(2800, 0.27f, 0.0f, true, true);
        state.sample(2900, 0.27f, 0.0f, true, true);
        assertEquals("timeout-settling", state.sample(3000, 0.27f, 0.0f, true, true));
    }

    @Test
    void stalePhysicsFrameCannotAccumulateObservationTime() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        assertEquals("settling", state.sample(0, 0.27f, 0.0f, true, true));
        assertEquals("waiting-physics-frame", state.awaitingSample(200));
        assertEquals("timeout-waiting-physics-frame", state.awaitingSample(3000));
    }

    @Test
    void missingHeightNeverFallsThroughToSuccessAtTheDeadline() {
        VehicleLandingState state = new VehicleLandingState();
        state.begin(0);
        assertEquals("no physics", state.sample(0, Float.NaN, 0.0f, true, true));
        assertEquals("timeout-no physics", state.sample(3000, Float.NaN, 0.0f, true, true));
    }
}
