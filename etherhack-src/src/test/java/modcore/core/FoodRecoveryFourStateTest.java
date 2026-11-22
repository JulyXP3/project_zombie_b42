package modcore.core;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryFourStateTest {
    private static final RecoveryDemand BAD=new RecoveryDemand(.7f,.2f,.8f,.6f,80f,60f);
    private static final RecoveryDemand GOOD=new RecoveryDemand(0f,1f,0f,0f,0f,0f);
    @ParameterizedTest @ValueSource(ints={0,3,4,5})
    void everyNegativeFoodFieldMustBeProvedByTheConsumptionReceipt(int index){
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();
        float[] receipt={-2.8f,4f,-4f,-2.4f,-320f,-240f};receipt[index]+=1f;
        f.flow.observeConsumption(f.port.context(),new FoodRecoveryController.Consumption(123,.25f,amounts(receipt)));
        f.goodStats();
        assertEquals(FoodRecoveryController.Result.NONE,f.flow.result());
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.flow.phase());
    }
    @Test void anAcceptedReceiptIsCachedBeforeCleanupUsesIt(){
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();
        f.remain();f.consume();f.goodStats();
        assertEquals(FoodRecoveryController.Result.SUCCESS,f.flow.result());
        assertEquals(FoodRecoveryController.Cleanup.REQUESTED,f.flow.cleanup());
        assertNotNull(f.port.cleaned);
        assertEquals(-.075f,f.port.cleaned.thirst(),1e-6f);assertEquals(-.03f,f.port.cleaned.stress(),1e-6f);
        assertEquals(9f,f.port.cleaned.boredom(),1e-6f);assertEquals(12f,f.port.cleaned.unhappy(),1e-6f);
        assertEquals(300f,f.port.cleaned.calories(),1e-6f);
    }
    @Test void cancellationBeforeEatingReturnsAllOriginalValuesInsteadOfZeros(){
        Fixture f=new Fixture();assertTrue(f.flow.arm());assertTrue(f.flow.prepare(f.port.item,.25f));f.flow.cancel();
        assertArrayEquals(new float[]{-.1f,.03f,.02f,-.04f,12f,16f},f.port.sent.get(1));
        assertEquals(FoodRecoveryController.Result.CANCELLED,f.flow.result());
    }
    @Test void aCancelledZeroHungerDrinkDoesNotWaitForANonexistentConsumptionReceipt(){
        Fixture f=new Fixture();f.port.base=0f;f.port.uses=1;f.port.live=f.port.live.withHunger(0f);
        assertTrue(f.flow.arm());assertTrue(f.flow.prepare(f.port.item,1f));f.flow.actionStarted();f.flow.actionStopped();
        assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());
        assertEquals(FoodRecoveryController.Result.CANCELLED,f.flow.result());
        assertArrayEquals(new float[]{-.1f,.03f,.02f,-.04f,12f,16f},f.port.sent.get(1));
    }
    @Test void aCookedSmallFoodNeedsTheEffectiveServerStopPredicate(){
        Fixture f=new Fixture();f.port.base=-.01f;f.port.uses=1;f.port.stopOverride=true;
        f.port.live=f.port.live.withHunger(-.01f).withMood(0f,-.04f,12f,16f);
        assertTrue(f.flow.arm());assertTrue(f.flow.prepare(f.port.item,1f));f.flow.actionStarted();f.flow.actionStopped();
        assertEquals(FoodRecoveryController.Phase.CLEANING,f.flow.phase());assertEquals(1,f.port.sent.size());
    }
    @Test void cancellingPreparationDoesNotPretendThatTheOrdinaryMealStopped(){
        Fixture f=new Fixture();f.port.base=0f;f.port.uses=1;f.port.live=f.port.live.withHunger(0f).withMood(0f,0f,0f,0f);
        assertTrue(f.flow.arm());assertTrue(f.flow.prepare(f.port.item,1f));f.flow.actionStarted();f.flow.cancel();
        assertEquals(FoodRecoveryController.Phase.CLEANING,f.flow.phase());assertEquals(1,f.port.sent.size());
    }
    @Test void aCompletedMealCannotUseTheNoConsumptionStopShortcut(){
        Fixture f=new Fixture();f.port.base=0f;f.port.uses=1;f.port.live=f.port.live.withHunger(0f).withMood(0f,0f,0f,0f);
        assertTrue(f.flow.arm());assertTrue(f.flow.prepare(f.port.item,1f));f.flow.actionStarted();f.flow.actionCompleted();f.flow.actionStopped();
        assertEquals(FoodRecoveryController.Phase.CLEANING,f.flow.phase());assertEquals(1,f.port.sent.size());
    }
    @Test void naturallyRisingStressDuringVerificationIsNotAFailure(){
        // Observed in game: a meal started with stress 0 and the next samples drifted up to 0.0026 while
        // verifying, which the absolute 0.001 threshold treated as "not recovered" and reported TIMED_OUT.
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();f.consume();f.remain();
        f.port.time++;f.flow.observeServerStats(f.port.context(),1f,0f,
                new RecoveryDemand(0f,1f,0f,0.0026f,0f,0f));
        f.port.time+=500;f.flow.observeServerStats(f.port.context(),1f,0f,
                new RecoveryDemand(0f,1f,0f,0.0041f,0f,0f));
        assertEquals(FoodRecoveryController.Result.SUCCESS,f.flow.result());
    }
    @Test void aStateThatStaysClearlyBadStillCannotCountAsRecovered(){
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();f.consume();f.remain();
        f.port.time++;f.flow.observeServerStats(f.port.context(),1f,0f,
                new RecoveryDemand(0f,1f,0f,0.2f,0f,0f));
        f.port.time+=500;f.flow.observeServerStats(f.port.context(),1f,0f,
                new RecoveryDemand(0f,1f,0f,0.2f,0f,0f));
        assertEquals(FoodRecoveryController.Result.NONE,f.flow.result());
    }
    @Test void smallButRealThirstStillNeedsRecovery(){
        Fixture f=new Fixture();f.flow.contextLost();
        f.flow.observeServerStats(f.port.context(),1f,0f,new RecoveryDemand(.05f,1f,0f,0f,0f,0f));
        assertEquals(FoodRecoveryController.Result.NONE,f.flow.availability());
    }
    @Test void fiveMoodPointsMustNotBeMistakenForARecoveredState(){
        Fixture f=new Fixture();f.flow.contextLost();
        f.flow.observeServerStats(f.port.context(),1f,0f,new RecoveryDemand(0f,1f,0f,0f,5f,0f));
        assertEquals(FoodRecoveryController.Result.NONE,f.flow.availability());
    }
    @Test void missingFourStateReadingsCannotFallBackToTwoStatSuccess(){
        Fixture f=new Fixture();f.flow.contextLost();f.flow.observeServerStats(f.port.context(),1f,0f,null);
        assertEquals(FoodRecoveryController.Result.NO_SERVER_STATS,f.flow.availability());
        assertFalse(f.flow.arm());
    }
    private static FoodRecoveryAmounts amounts(float[] v){
        return new FoodRecoveryAmounts(-.2f,v[0],v[1],v[2],v[3],v[4],v[5],400f,40f,8f,12f,0f,0,0,0);
    }
    static final class Fixture {
        final Port port=new Port();final FoodRecoveryController flow=new FoodRecoveryController(port);
        Fixture(){flow.observeServerStats(port.context(),.2f,.8f,BAD);}
        void start(){assertTrue(flow.arm());assertTrue(flow.prepare(port.item,.25f));flow.actionStarted();}
        void consume(){flow.observeConsumption(port.context(),new FoodRecoveryController.Consumption(123,.25f,
                amounts(new float[]{-2.8f,4f,-4f,-2.4f,-320f,-240f})));}
        void remain(){port.live=port.live.withHunger(-.15f);flow.observeRemainder(port.context(),
                new FoodRecoveryController.Remainder(123,-.15f,.5f,15));}
        void goodStats(){port.time++;flow.observeServerStats(port.context(),1f,0f,GOOD);
            port.time+=500;flow.observeServerStats(port.context(),1f,0f,GOOD);}
    }
    static final class Port implements FoodRecoveryController.Port {
        Object connection=new Object(),player=new Object(),item=new Object();long time=100;
        float base=-.2f;int uses=20;Boolean stopOverride;
        float baseHunger=-.2f;
        final List<float[]> sent=new ArrayList<>();final FoodRecoveryObservedAmounts cache=new FoodRecoveryObservedAmounts();
        FoodRecoveryAmounts live=amounts(new float[]{-.1f,.03f,.02f,-.04f,12f,16f}),cleaned;
        public long now(){return time;}
        public FoodRecoveryController.Context context(){return new FoodRecoveryController.Context(connection,player,true,false);}
        public FoodRecoveryController.Food inspect(Object value){return value!=item?null:new FoodRecoveryController.Food(item,123,baseHunger,
                live.hunger(),live.thirst(),live.endurance(),live.fatigue(),live.stress(),live.boredom(),live.unhappy(),uses,true,true);}
        public boolean exists(FoodRecoveryController.Food f){return true;}
        public boolean requiresConsumptionReceipts(){return true;}
        public boolean requiresFullStats(){return true;}
        public boolean stoppedActionCanConsume(FoodRecoveryController.Food f){return stopOverride!=null?stopOverride:Math.abs(f.hunger()*100f)>1f;}
        public FoodRecoveryController.Selection select(int budget){return new FoodRecoveryController.Selection(item,1,false);}
        public FoodRecoveryPortions.Dose dose(FoodRecoveryController.Food f,float request){
            return FoodRecoveryPortions.dose(f.baseHunger(),f.hunger(),f.thirst(),1f,request,BAD);}
        public boolean submit(FoodRecoveryController.Food f,float[] v){
            if(sent.isEmpty())cache.prepared(connection,123,live,v[1],v[2]);
            sent.add(v.clone());live=live.withEffects(v[1],v[2]).withMood(v[0],v[3],v[4],v[5]);return true;}
        public void consumptionAccepted(FoodRecoveryController.Consumption c){cache.observe(connection,c.id(),c.amounts(),c.fraction());}
        public FoodRecoveryController.Cleanup restoreRemaining(FoodRecoveryController.Food f,FoodRecoveryController.Consumption c,
                FoodRecoveryController.Remainder r){
            FoodRecoveryAmounts expected=c.amounts().withEffects(f.endurance(),f.fatigue())
                    .withMood(f.thirst(),f.stress(),f.boredom(),f.unhappy()).scaled(1f-c.fraction());
            cleaned=cache.cleanupProjection(connection,123,live,expected);
            return cleaned==null?FoodRecoveryController.Cleanup.UNCERTAIN:FoodRecoveryController.Cleanup.REQUESTED;
        }
    }
}
