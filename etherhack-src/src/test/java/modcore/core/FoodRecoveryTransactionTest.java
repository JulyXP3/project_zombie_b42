package modcore.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryTransactionTest {
    @Test void inventorySelectionOnlyAdvancesInTheActiveSelectionPhase() {
        Fixture f=new Fixture();f.port.pendingSteps=2;assertTrue(f.flow.arm());
        assertEquals(FoodRecoveryController.Phase.SELECTING,f.flow.phase());int scans=f.port.selectionSteps;
        for(int i=0;i<50;i++){f.flow.phase();f.flow.availability();f.flow.remainingSeconds();f.flow.candidate();}
        assertEquals(scans,f.port.selectionSteps);f.flow.tick();f.flow.tick();
        assertEquals(f.port.item,f.flow.candidate());assertEquals(FoodRecoveryController.Phase.ARMED,f.flow.phase());
    }
    @Test void noSafeFoodEndsWithoutSendingAnyItemModification() {
        Fixture f=new Fixture();f.port.noFood=true;f.flow.arm();
        assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());assertEquals(FoodRecoveryController.Result.NO_FOOD,f.flow.result());assertTrue(f.port.sent.isEmpty());
    }
    @Test void anotherFoodCannotHijackTheAutomaticallySelectedAction() {
        Fixture f=new Fixture();f.flow.arm();assertFalse(f.flow.prepare(new Object(),.25f));
        assertEquals(FoodRecoveryController.Phase.ARMED,f.flow.phase());assertTrue(f.port.sent.isEmpty());
    }
    @Test void ordinaryQuarterFoodGetsDoseForItsActualPortion() {
        Fixture f=new Fixture();f.start();
        float[] dose=f.port.sent.get(0);
        // Order: thirst, endurance, fatigue, stress, boredom, unhappy. This stub reports no mood readings.
        assertEquals(6,dose.length);assertEquals(4f,dose[1]);assertEquals(-4f,dose[2]);
    }
    @Test void partialCleanupProjectionKeepsTheItemsOwnMoodValues() {
        // The serving boosted thirst/stress/boredom/unhappy; the leftover must be projected from the item's
        // own readings, otherwise a partly eaten item keeps the boosted profile forever.
        Fixture f=new Fixture();f.port.hunger=-.4f;f.port.base=-.4f;f.start();
        f.flow.cancel();
        f.remain(-.3f);f.consume(.25f);
        // The stub writes no mood dose (all zero), so the projection must keep the item's own zeros.
        assertEquals(0f,f.port.restored.thirst());assertEquals(0f,f.port.restored.stress());
        assertEquals(0f,f.port.restored.boredom());assertEquals(0f,f.port.restored.unhappy());
    }
    @Test void cancellationWaitsForServerConsumptionAndRemainderBeforeWritingBack() {
        Fixture f=new Fixture();f.start();f.flow.cancel();assertEquals(FoodRecoveryController.Phase.CLEANING,f.flow.phase());
        assertEquals(1,f.port.sent.size());f.consume(.25f);assertEquals(1,f.port.sent.size());
        f.remain(-.15f);assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());
        assertEquals(FoodRecoveryController.Result.CANCELLED,f.flow.result());assertEquals(FoodRecoveryController.Cleanup.REQUESTED,f.flow.cleanup());
        assertEquals(-.15f,f.port.restored.hunger());assertEquals(300f,f.port.restored.calories());
        assertEquals(.03f*.75f,f.port.restored.endurance());assertEquals(.02f*.75f,f.port.restored.fatigue());
    }
    @Test void remainderMayArriveBeforeTheFullConsumptionReceipt() {
        Fixture f=new Fixture();f.start();f.flow.cancel();f.remain(-.15f);assertNull(f.port.restored);
        f.consume(.25f);assertNotNull(f.port.restored);assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());
    }
    @Test void mismatchedRemainderNeverUploadsAnOldWholeFoodSnapshot() {
        Fixture f=new Fixture();f.start();f.flow.cancel();f.consume(.25f);f.remain(-.1f);
        assertNull(f.port.restored);assertEquals(1,f.port.sent.size());f.port.time+=10000;f.flow.tick();
        assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());assertEquals(FoodRecoveryController.Cleanup.UNCERTAIN,f.flow.cleanup());
    }
    @Test void wrongItemOrUnmodifiedFoodReceiptIsNotOurConsumptionProof() {
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();
        f.flow.observeConsumption(f.port.context(),new FoodRecoveryController.Consumption(999,.25f,f.amounts(4f,-4f)));
        f.flow.observeConsumption(f.port.context(),new FoodRecoveryController.Consumption(123,.25f,f.amounts(.03f,.02f)));
        f.remain(-.15f);f.stats();assertEquals(FoodRecoveryController.Phase.VERIFYING,f.flow.phase());assertNull(f.port.restored);
    }
    @Test void twoServerStatsNeedActualFoodConsumptionProofAndBoundedCleanup() {
        Fixture f=new Fixture();f.start();f.flow.actionCompleted();f.stats();assertEquals(FoodRecoveryController.Phase.VERIFYING,f.flow.phase());
        f.consume(.25f);f.remain(-.15f);assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());
        assertEquals(FoodRecoveryController.Result.SUCCESS,f.flow.result());assertEquals(FoodRecoveryController.Cleanup.REQUESTED,f.flow.cleanup());
    }
    @Test void fullConsumptionDoesNotRecreateAnItemOrWaitForAResidue() {
        Fixture f=new Fixture();f.port.hunger=-.01f;f.port.base=-.01f;f.start();f.flow.actionCompleted();
        f.consume(1f);f.stats();assertEquals(FoodRecoveryController.Cleanup.NOT_NEEDED,f.flow.cleanup());assertNull(f.port.restored);assertEquals(1,f.port.sent.size());
    }
    @Test void cleanupDoesNotCrossANewConnection() {
        Fixture f=new Fixture();f.start();f.flow.cancel();f.port.connection=new Object();f.consume(.25f);f.remain(-.15f);
        assertNull(f.port.restored);assertEquals(FoodRecoveryController.Cleanup.UNCERTAIN,f.flow.cleanup());
    }
    @Test void unsafeFieldsStillFailWithoutAnyPacketSubmission() {
        Fixture f=new Fixture();f.port.safe=false;f.flow.arm();assertFalse(f.flow.prepare(f.port.item,.25f));assertTrue(f.port.sent.isEmpty());
    }
    @Test void remainingAmountProjectionPreservesVanillaIntegerTruncationAndAllNutrition() {
        FoodRecoveryAmounts a=new FoodRecoveryAmounts(-.2f,-.1f,.03f,.02f,-.04f,12f,16f,400f,40f,8f,12f,10f,5,-7,3);
        FoodRecoveryAmounts r=a.scaled(.25f);assertEquals(-.05f,r.hunger());assertEquals(100f,r.calories());assertEquals(10f,r.carbs());
        assertEquals(2f,r.proteins());assertEquals(3f,r.lipids());assertEquals(1,r.flu());assertEquals(-1,r.sickness());assertEquals(0,r.poison());
    }
    @Test void automaticFoodProfilesRejectDirectSicknessPoisonAndHarmfulRemedyAmounts(){
        FoodRecoveryAmounts normal=new FoodRecoveryAmounts(-.2f,.03f,.02f,.01f,0f,0f,0f,200f,10f,5f,2f,0f,0,0,0);
        assertTrue(normal.safeAutomatic());
        assertFalse(new FoodRecoveryAmounts(-.2f,0f,0f,0f,0f,0f,0f,200f,10f,5f,2f,0f,0,5,0).safeAutomatic());
        assertFalse(new FoodRecoveryAmounts(-.2f,0f,0f,0f,0f,0f,0f,200f,10f,5f,2f,0f,0,0,1).safeAutomatic());
        assertFalse(new FoodRecoveryAmounts(-.2f,0f,0f,0f,0f,0f,0f,200f,10f,5f,2f,-1f,-1,0,0).safeAutomatic());
    }
    @Test void everyFoodInsideThePlayerBodyIsAddressableNotOnlyTheRootContainer() {
        // ContainerID.setInventoryContainer addresses a bag through its containing item id and the
        // server resolves it with getItemWithIDRecursiv, so a worn/held bag's food is valid to send.
        FoodRecoveryController.Food root=new FoodRecoveryController.Food(new Object(),1,-.2f,-.2f,0f,.02f,.04f,0f,0f,0f,1,true,true);
        assertTrue(root.rootOwned());
        assertTrue(FoodRecoveryController.addressesBodyContainer(root));
    }
    static final class Fixture {
        final Port port=new Port();final FoodRecoveryController flow=new FoodRecoveryController(port);
        Fixture(){flow.observeServerStats(port.context(),.2f,.8f);}
        void start(){assertTrue(flow.arm());assertTrue(flow.prepare(port.item,FoodRecoveryPortions.smallRequest(port.base,port.hunger)));flow.actionStarted();}
        /**
         * Receipt for an arbitrary endurance/fatigue pair. The stub writes no thirst/stress/boredom/unhappy
         * dose, so those stay at the item's own values and are not part of the proof.
         */
        FoodRecoveryAmounts amounts(float e,float f){
            return new FoodRecoveryAmounts(port.hunger,0f,e,f,0f,0f,0f,400f,40f,8f,12f,10f,5,-7,0);
        }
        /** Receipt carrying exactly the dose that was sent, so the check has real evidence to match. */
        FoodRecoveryAmounts sentAmounts(){
            float[] d=port.sent.get(0);
            return new FoodRecoveryAmounts(port.hunger,d[0],d[1],d[2],d[3],d[4],d[5],400f,40f,8f,12f,10f,5,-7,0);
        }
        void consume(float p){flow.observeConsumption(port.context(),new FoodRecoveryController.Consumption(123,p,sentAmounts()));}
        void remain(float h){flow.observeRemainder(port.context(),new FoodRecoveryController.Remainder(123,h,.5f,1));}
        void stats(){port.time++;flow.observeServerStats(port.context(),1f,0f);port.time+=500;flow.observeServerStats(port.context(),1f,0f);}
    }
    static final class Port implements FoodRecoveryController.Port {
        long time=100;Object connection=new Object(),player=new Object(),item=new Object();boolean safe=true,noFood;int pendingSteps,selectionSteps;
        float hunger=-.2f,base=-.2f;List<float[]> sent=new ArrayList<>();FoodRecoveryAmounts restored;
        public long now(){return time;}public FoodRecoveryController.Context context(){return new FoodRecoveryController.Context(connection,player,true,false);}
        public FoodRecoveryController.Food inspect(Object i){return i==item?new FoodRecoveryController.Food(item,123,base,hunger,0f,.03f,.02f,0f,0f,0f,1,true,safe):null;}
        public boolean exists(FoodRecoveryController.Food key){return true;}public boolean submit(FoodRecoveryController.Food key,float[] v){sent.add(v.clone());return true;}
        public boolean requiresConsumptionReceipts(){return true;}
        public FoodRecoveryController.Selection select(int budget){selectionSteps++;if(pendingSteps-->0)return new FoodRecoveryController.Selection(null,128,true);return new FoodRecoveryController.Selection(noFood?null:item,1,false);}
        public FoodRecoveryController.Cleanup restoreRemaining(FoodRecoveryController.Food original,FoodRecoveryController.Consumption c,FoodRecoveryController.Remainder r){
            restored=c.amounts().withEffects(original.endurance(),original.fatigue()).scaled(1f-c.fraction());sent.add(new float[]{restored.endurance(),restored.fatigue()});return FoodRecoveryController.Cleanup.REQUESTED;
        }
    }
}
