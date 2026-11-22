package modcore.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryPortionsTest {
    @Test void quarterOfCurrentRemainderIsNotQuarterOfTheOriginalWhole() {
        assertEquals(.125f,FoodRecoveryPortions.smallRequest(-.4f,-.2f));
        assertEquals(.25f,FoodRecoveryPortions.effectiveFraction(-.4f,-.2f,0f,.125f));
    }
    @Test void tinyRemaindersFollowVanillaEatEverythingRule() {
        assertEquals(1f,FoodRecoveryPortions.effectiveFraction(-.01f,-.01f,0f,.25f));
    }
    @Test void zeroHungerFoodUsesAWholeRequest() {
        assertEquals(1f,FoodRecoveryPortions.smallRequest(0f,0f));
        assertEquals(1f,FoodRecoveryPortions.effectiveFraction(0f,0f,-.2f,1f));
    }
    @Test void cookedEnduranceMultiplierIsIncludedInTheDose() {
        FoodRecoveryPortions.Dose raw=FoodRecoveryPortions.dose(-.2f,-.2f,0f,1f,.25f);
        assertEquals(.25f,raw.fraction());assertEquals(4f,raw.endurance());assertEquals(-4f,raw.fatigue());
        FoodRecoveryPortions.Dose cooked=FoodRecoveryPortions.dose(-.2f,-.2f,0f,2f,.25f);
        assertEquals(2f,cooked.endurance());assertEquals(-4f,cooked.fatigue());
    }
    @Test void invalidAndZeroFractionsCannotProduceInfiniteFoodEffects() {
        assertNull(FoodRecoveryPortions.dose(-.2f,-.2f,0f,1f,0f));
        assertNull(FoodRecoveryPortions.dose(-.2f,-.2f,0f,Float.NaN,.25f));
        assertTrue(Float.isNaN(FoodRecoveryPortions.smallRequest(Float.NaN,-.2f)));
    }
    @Test void originalPercentageClampingAndBaseHungerRatioAreRespected() {
        assertEquals(.5f,FoodRecoveryPortions.effectiveFraction(-.1f,-.2f,0f,2f));
        assertEquals(0f,FoodRecoveryPortions.effectiveFraction(-.1f,-.2f,0f,-1f));
    }
}
