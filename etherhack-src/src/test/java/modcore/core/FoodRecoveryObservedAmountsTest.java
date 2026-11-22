package modcore.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryObservedAmountsTest {
    private static FoodRecoveryAmounts food(float h,float e,float f,float calories){return new FoodRecoveryAmounts(h,-.1f,e,f,-.03f,0f,0f,calories,10f,5f,2f,0f,0,0,0);}
    @Test void aKnownServerPortionCorrectsOnlyRecognizedUnscaledOrScaledClientAmounts(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts before=food(-.2f,.02f,.04f,400f);
        c.observe(owner,1,before,.25f);FoodRecoveryAmounts stale=before.withHunger(-.15f);
        assertEquals(300f,c.normalize(owner,1,stale).calories());assertEquals(.02f*.75f,c.normalize(owner,1,stale).endurance());
        assertEquals(before.scaled(.75f),c.normalize(owner,1,before.scaled(.75f)));
    }
    @Test void unrelatedLocalEditsOrDifferentRemainingAmountAreNotClobbered(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts before=food(-.2f,.02f,.04f,400f);c.observe(owner,1,before,.25f);
        FoodRecoveryAmounts edited=food(-.15f,.02f,.04f,999f);assertSame(edited,c.normalize(owner,1,edited));
        FoodRecoveryAmounts later=before.withHunger(-.1f);assertSame(later,c.normalize(owner,1,later));
    }
    @Test void ourTemporaryBoostNeverBecomesTheOriginalEffectOfTheRemainingFood(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts original=food(-.2f,.02f,.04f,400f);
        c.prepared(owner,1,original,4f,-4f);c.observe(owner,1,original.withEffects(4f,-4f),.25f);
        FoodRecoveryAmounts corrected=c.normalize(owner,1,original.withHunger(-.15f));
        assertEquals(.02f*.75f,corrected.endurance());assertEquals(.04f*.75f,corrected.fatigue());
    }
    @Test void cleanupCannotOverwriteAConcurrentFoodAmountEdit(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts before=food(-.2f,.02f,.04f,400f);c.observe(owner,1,before,.25f);
        FoodRecoveryAmounts edited=food(-.15f,.02f,.04f,999f);
        assertNull(c.cleanupProjection(owner,1,edited,before.scaled(.75f)));
    }
    @Test void cleanupAcceptsOnlyTheExpectedAuthoritativeRemainder(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts before=food(-.2f,.02f,.04f,400f);c.observe(owner,1,before,.25f);
        FoodRecoveryAmounts expected=before.scaled(.75f);assertEquals(expected,c.cleanupProjection(owner,1,before.withHunger(-.15f),expected));
        assertNull(c.cleanupProjection(owner,1,before.withHunger(-.15f),before.scaled(.5f)));
    }
    @Test void sixBoostedFieldsAreNotAllowedToRemainInTheFood(){
        Object owner=new Object();FoodRecoveryObservedAmounts cache=new FoodRecoveryObservedAmounts();
        FoodRecoveryAmounts original=food(-.2f,.02f,.04f,400f).withMood(-.1f,-.03f,12f,16f);
        FoodRecoveryAmounts boosted=original.withEffects(4f,-4f).withMood(-2.8f,-2.4f,-320f,-240f);
        cache.prepared(owner,1,original,4f,-4f);cache.observe(owner,1,boosted,.25f);
        FoodRecoveryAmounts cleaned=cache.normalize(owner,1,boosted.withHunger(-.15f));
        assertEquals(-.075f,cleaned.thirst(),1e-6f);assertEquals(-.0225f,cleaned.stress(),1e-6f);
        assertEquals(9f,cleaned.boredom(),1e-6f);assertEquals(12f,cleaned.unhappy(),1e-6f);
        assertEquals(.015f,cleaned.endurance(),1e-6f);assertEquals(.03f,cleaned.fatigue(),1e-6f);
        assertEquals(300f,cleaned.calories(),1e-6f);
    }
    @Test void aNewPreparationCannotReuseTheOldRemainderObservation(){
        Object owner=new Object();FoodRecoveryObservedAmounts cache=new FoodRecoveryObservedAmounts();
        FoodRecoveryAmounts original=food(-.2f,.02f,.04f,400f);
        cache.observe(owner,1,original,.25f);FoodRecoveryAmounts local=original.withHunger(-.15f);
        cache.prepared(owner,1,local,4f,-4f);
        assertSame(local,cache.normalize(owner,1,local));
    }
    @Test void observationsAreBoundedAndCannotCrossConnections(){
        Object owner=new Object();FoodRecoveryObservedAmounts c=new FoodRecoveryObservedAmounts();FoodRecoveryAmounts original=food(-.2f,.02f,.04f,400f);
        for(int i=0;i<200;i++)c.observe(owner,i,original,.25f);assertTrue(c.size()<=64);
        FoodRecoveryAmounts local=original.withHunger(-.15f);assertSame(local,c.normalize(new Object(),199,local));
    }
}
