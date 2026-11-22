package modcore.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryDemandTest {
    private static RecoveryDemand bad(){return new RecoveryDemand(.7f,.2f,.8f,.6f,80f,60f);}
    @Test void thirstReductionKeepsItsNegativeSign(){assertEquals(-2.8f,bad().perUnit(Restored.THIRST,.25f),1e-5f);}
    @Test void stressReductionKeepsItsNegativeSign(){assertEquals(-2.4f,bad().perUnit(Restored.STRESS,.25f),1e-5f);}
    @Test void boredomUsesHundredPointUnitsAndANegativeDelta(){assertEquals(-320f,bad().perUnit(Restored.BOREDOM,.25f),1e-4f);}
    @Test void unhappinessUsesHundredPointUnitsAndANegativeDelta(){assertEquals(-240f,bad().perUnit(Restored.UNHAPPY,.25f),1e-4f);}
    @Test void reducingStatesAlreadyAtZeroDoNotNeedAnyDelta(){
        RecoveryDemand good=new RecoveryDemand(0f,1f,0f,0f,0f,0f);
        for(Restored stat:Restored.values())assertEquals(0f,good.perUnit(stat,.25f));
    }
    @Test void enduranceAndFatigueUseTheirOppositeGoodEnds(){
        assertEquals(3.2f,bad().perUnit(Restored.ENDURANCE,.25f),1e-5f);
        assertEquals(-3.2f,bad().perUnit(Restored.FATIGUE,.25f),1e-5f);
    }
    @Test void invalidFractionsCannotCreateInfiniteDoses(){
        assertTrue(Float.isNaN(bad().perUnit(Restored.THIRST,0f)));
        assertTrue(Float.isNaN(bad().perUnit(Restored.THIRST,Float.NaN)));
        assertTrue(Float.isNaN(bad().perUnit(Restored.THIRST,1.01f)));
    }
    @Test void outOfRangeServerReadingsAreRejectedRatherThanUsedAsHealingDoses(){
        assertThrows(IllegalArgumentException.class,()->new RecoveryDemand(-.1f,.2f,.8f,.6f,80f,60f));
        assertThrows(IllegalArgumentException.class,()->new RecoveryDemand(.7f,.2f,.8f,.6f,101f,60f));
        assertThrows(IllegalArgumentException.class,()->new RecoveryDemand(.7f,.2f,.8f,Float.NaN,80f,60f));
    }
}
