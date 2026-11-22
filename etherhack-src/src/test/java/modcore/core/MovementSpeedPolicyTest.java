package modcore.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementSpeedPolicyTest {
    // Replacing the accepted preset or any eligibility gate must fail these tests.
    @Test void keepsOnlyTheThreeApprovedPresets() {
        for (int value : new int[]{1, 2, 3}) assertEquals(value, MovementSpeedPolicy.normalizePreset(value));
    }
    @Test void invalidPresetsFallBackToOriginalSpeed() {
        for (int value : new int[]{-1, 0, 4, Integer.MIN_VALUE, Integer.MAX_VALUE})
            assertEquals(1, MovementSpeedPolicy.normalizePreset(value));
    }
    @Test void approvedManualGroundMovementUsesTheRequestedFactor() {
        for (int value : new int[]{1, 2, 3})
            assertEquals((float)value, factor(value, true, true, true, false, false, false));
    }
    @Test void anotherOrDeadPlayerCannotBeAccelerated() {
        assertEquals(1f, factor(3, false, true, true, false, false, false));
    }
    @Test void unknownGroundStatesKeepOriginalMovement() {
        assertEquals(1f, factor(3, true, false, true, false, false, false));
    }
    @Test void movementWithoutManualInputKeepsOriginalSpeed() {
        assertEquals(1f, factor(3, true, true, false, false, false, false));
    }
    @Test void specialAnimationMotionCannotBeAccelerated() {
        assertEquals(1f, factor(3, true, true, true, true, false, false));
    }
    @Test void aimingOrPathingKeepsOriginalSpeed() {
        assertEquals(1f, factor(3, true, true, true, false, true, false));
    }
    @Test void existingExtraMovementModesAreNotMultipliedAgain() {
        assertEquals(1f, factor(3, true, true, true, false, false, true));
    }
    @Test void invalidRequestedFactorsCannotSlipThroughTheRuntimeGate() {
        assertEquals(1f, factor(4, true, true, true, false, false, false));
    }
    @Test void twoAndThreeTimesScaleBothDirectionsExactly() {
        assertEquals(2.5f, MovementSpeedPolicy.scaledComponent(1.25f, 2f));
        assertEquals(-5f, MovementSpeedPolicy.scaledComponent(-2.5f, 2f));
        assertEquals(3.75f, MovementSpeedPolicy.scaledComponent(1.25f, 3f));
        assertEquals(-7.5f, MovementSpeedPolicy.scaledComponent(-2.5f, 3f));
    }
    @Test void zeroInputDoesNotGenerateMovement() {
        assertTrue(MovementSpeedPolicy.canScale(0f, 0f, 3f));
        assertEquals(0f, MovementSpeedPolicy.scaledComponent(0f, 3f));
    }
    @Test void nonFiniteOrOverflowingVectorsAreNotWrittenBack() {
        assertFalse(MovementSpeedPolicy.canScale(Float.NaN, 1f, 2f));
        assertFalse(MovementSpeedPolicy.canScale(1f, Float.POSITIVE_INFINITY, 3f));
        assertFalse(MovementSpeedPolicy.canScale(Float.MAX_VALUE, 1f, 3f));
        assertFalse(MovementSpeedPolicy.canScale(1f, Float.MAX_VALUE, 2f));
    }
    @Test void nonPresetFloatFactorsCannotBeUsedAsMotionMultipliers() {
        for (float value : new float[]{0f, -1f, 2.5f, 4f, Float.NaN, Float.POSITIVE_INFINITY})
            assertFalse(MovementSpeedPolicy.canScale(1f, 1f, value));
    }
    @Test void everyFreshReadScalesItsOwnInputInsteadOfAccumulating() {
        assertEquals(3.75f, MovementSpeedPolicy.scaledComponent(1.25f, 3f));
        assertEquals(3.75f, MovementSpeedPolicy.scaledComponent(1.25f, 3f));
        assertEquals(1.25f, MovementSpeedPolicy.scaledComponent(1.25f, 1f));
    }
    @Test void walkingStateFlagIsRequiredForGroundScaling() {
        // Mirrors MovementSpeedRuntime: a moving local player is in PlayerMovementState, not IdleState.
        assertEquals(2f, factor(2, true, true, true, false, false, false));
        assertEquals(1f, factor(2, true, false, true, false, false, false));
    }
    private static float factor(int value, boolean current, boolean ground, boolean manual,
                                boolean special, boolean aimOrPath, boolean extra) {
        return MovementSpeedPolicy.effectiveMultiplier(value, current, ground, manual, special, aimOrPath, extra);
    }
}
