package modcore.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryEffectsTest {
    FoodRecoveryModifiersTest.Fields fields;
    final float[] original={-.1f,.03f,.02f,-.04f,12f,16f};
    @BeforeEach void isolateBoost(){
        FoodRecoveryEffects.end(new FoodRecoveryModifiersTest.Fields(original));
        fields=new FoodRecoveryModifiersTest.Fields(original);
    }
    @AfterEach void release(){FoodRecoveryEffects.end(fields);}
    @Test void aCleanedRemainderMustNotBeReplacedWithWholeFoodValuesOnEnd(){
        assertTrue(FoodRecoveryEffects.begin(fields,new float[]{-2.8f,4f,-4f,-2.4f,-320f,-240f}));
        float[] remainder={-.075f,.0225f,.015f,-.03f,9f,12f};
        FoodRecoveryEffects.end(fields,remainder);
        assertArrayEquals(remainder,fields.v);
        FoodRecoveryEffects.end(fields);assertArrayEquals(remainder,fields.v);
    }
    @Test void aFailedSendRestoresAllSixOriginalValues(){
        assertFalse(FoodRecoveryEffects.send(fields,new float[]{-2.8f,4f,-4f,-2.4f,-320f,-240f},
                ()->{throw new java.io.IOException("send failed");}));
        assertArrayEquals(original,fields.v);
    }
    @Test void repeatedSubmissionDoesNotCaptureAnEnhancedProfileAsTheOriginal(){
        assertTrue(FoodRecoveryEffects.begin(fields,new float[]{-2.8f,4f,-4f,-2.4f,-320f,-240f}));
        assertTrue(FoodRecoveryEffects.begin(fields,original));FoodRecoveryEffects.end(fields);
        assertArrayEquals(original,fields.v);
    }
}
