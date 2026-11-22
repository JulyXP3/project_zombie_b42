package modcore.core;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryModifiersTest {
    @Test void cookedAndStaleModifiersStillClearAllFourStatesInOneQuarter() throws Exception {
        FoodRecoveryModifiers m=new FoodRecoveryModifiers(.5f,1.3f,10f,12f);
        RecoveryDemand need=new RecoveryDemand(.7f,.2f,.8f,.6f,80f,60f);
        FoodRecoveryPortions.Dose d=FoodRecoveryPortions.dose(-.2f,-.2f,-.1f,2f,.25f,need,m);
        assertNotNull(d);assertEquals(-5.6f,d.thirst(),1e-5f);
        assertEquals(-2.4f/1.3f,d.stress(),1e-5f);assertEquals(-330f,d.boredom(),1e-4f);
        assertEquals(-252f,d.unhappy(),1e-4f);
        Fields f=new Fields(new float[]{d.thirst(),d.endurance(),d.fatigue(),d.stress(),d.boredom(),d.unhappy()});
        float[] wire=new float[6];m.capture(f,()->copyItemStatsValues(f,wire));
        assertEquals(0f,.7f+wire[0]*.5f*.25f,1e-6f);
        assertEquals(0f,.6f+wire[3]*1.3f*.25f,1e-6f);
        assertEquals(0f,80f+(wire[4]+10f)*.25f,1e-5f);
        assertEquals(0f,60f+(wire[5]+12f)*.25f,1e-5f);
    }
    @Test void aZeroNeedNeutralisesFoodAgeAndTemperaturePenalties() throws Exception {
        FoodRecoveryModifiers m=new FoodRecoveryModifiers(.5f,1.3f,10f,12f);
        FoodRecoveryPortions.Dose d=FoodRecoveryPortions.dose(-.2f,-.2f,-.1f,2f,.25f,
                new RecoveryDemand(0f,.2f,.8f,0f,0f,0f),m);
        assertNotNull(d);assertEquals(-10f,d.boredom());assertEquals(-12f,d.unhappy());
        Fields f=new Fields(new float[]{d.thirst(),d.endurance(),d.fatigue(),d.stress(),d.boredom(),d.unhappy()});
        float[] wire=new float[6];m.capture(f,()->copyItemStatsValues(f,wire));
        assertEquals(0f,wire[4]+10f);assertEquals(0f,wire[5]+12f);
    }
    @Test void aRollbackPacketCarriesTheOriginalStoredValuesWithoutScalingThemAgain() throws Exception {
        float[] original={-.1f,.03f,.02f,-.04f,12f,16f};Fields fields=new Fields(original);
        float[] wire=new float[6];new FoodRecoveryModifiers(.5f,1.3f,10f,12f).capture(fields,
                ()->copyItemStatsValues(fields,wire));
        assertArrayEquals(original,wire,1e-6f);assertArrayEquals(original,fields.v);
    }
    @Test void failedPacketCaptureRestoresTheLocalValues() {
        float[] original={-.1f,.03f,.02f,-.04f,12f,16f};Fields fields=new Fields(original);
        assertThrows(IOException.class,()->new FoodRecoveryModifiers(.5f,1.3f,10f,12f).capture(fields,
                ()->{throw new IOException("capture failed");}));
        assertArrayEquals(original,fields.v);
    }
    private static void copyItemStatsValues(Fields f,float[] w){
        // These are the actual getter corrections used by ItemStatsPacket.setData and Food.
        w[0]=f.thirst()*.5f;w[1]=f.endurance();w[2]=f.fatigue();w[3]=f.stress()*1.3f;
        w[4]=f.boredom()+10f;w[5]=f.unhappy()+12f;
    }
    static final class Fields implements FoodRecoveryEffects.Fields {
        final float[] v;Fields(float[] values){v=values.clone();}
        public float thirst(){return v[0];}public void thirst(float x){v[0]=x;}
        public float endurance(){return v[1];}public void endurance(float x){v[1]=x;}
        public float fatigue(){return v[2];}public void fatigue(float x){v[2]=x;}
        public float stress(){return v[3];}public void stress(float x){v[3]=x;}
        public float boredom(){return v[4];}public void boredom(float x){v[4]=x;}
        public float unhappy(){return v[5];}public void unhappy(float x){v[5]=x;}
    }
}
