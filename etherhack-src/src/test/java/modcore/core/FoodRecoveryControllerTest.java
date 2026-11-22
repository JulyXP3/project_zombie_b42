package modcore.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryControllerTest {
    @Test void preparationIsNotPersistedAcrossControllerInstances() {
        Fixture f=new Fixture(); assertTrue(f.controller.arm());
        assertEquals(FoodRecoveryController.Phase.ARMED,f.controller.phase());
        assertEquals(FoodRecoveryController.Phase.OFF,new FoodRecoveryController(f.port).phase());
    }
    @Test void multiplayerContextAndDisabledLocalLocksAreRequired() {
        Fixture f=new Fixture();f.port.supported=false;
        assertFalse(f.controller.arm());assertEquals(FoodRecoveryController.Result.UNSUPPORTED,f.controller.result());
        f=new Fixture();f.port.locks=true;
        assertFalse(f.controller.arm());assertEquals(FoodRecoveryController.Result.CONFLICT,f.controller.result());
    }
    @Test void localValuesWithoutFreshServerDataCannotArmTheRecovery() {
        Port p=new Port();FoodRecoveryController c=new FoodRecoveryController(p);
        assertFalse(c.arm());assertEquals(FoodRecoveryController.Result.NO_SERVER_STATS,c.result());
        Fixture f=new Fixture();f.port.time+=2501;
        assertFalse(f.controller.arm());assertEquals(FoodRecoveryController.Result.NO_SERVER_STATS,f.controller.result());
    }
    @Test void anAlreadyRecoveredServerSnapshotIsNotClaimedAsANewSuccess() {
        Fixture f=new Fixture();f.controller.observeServerStats(f.port.context(),1f,0f);
        assertFalse(f.controller.arm());assertEquals(FoodRecoveryController.Result.NO_NEED,f.controller.result());
    }
    @Test void nextFullSafeFoodReceivesEveryRestoredDoseOnce() {
        Fixture f=new Fixture();assertTrue(f.controller.arm());
        assertFalse(f.controller.arm());
        assertTrue(f.controller.prepare(f.port.item,1f));
        assertEquals(FoodRecoveryController.Phase.PREPARED,f.controller.phase());
        assertEquals(1,f.port.sent.size());
        float[] values=f.port.sent.get(0);
        // Order: thirst, endurance, fatigue, stress, boredom, unhappy. The stub reports no mood readings,
        // so only the two endurance/fatigue doses are produced and the other food values stay untouched.
        assertEquals(6,values.length);assertEquals(1f,values[1]);assertEquals(-1f,values[2]);
        assertEquals(0f,values[0]);assertEquals(0f,values[3]);assertEquals(0f,values[4]);assertEquals(0f,values[5]);
        assertFalse(f.controller.prepare(f.port.item,1f));assertEquals(1,f.port.sent.size());
    }
    @Test void anotherInventoryItemCannotConsumeThePreparation() {
        Fixture f=new Fixture();f.port.rootOwned=false;f.controller.arm();
        assertFalse(f.controller.prepare(f.port.item,1f));assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        assertEquals(FoodRecoveryController.Result.UNSAFE_FOOD,f.controller.result());assertTrue(f.port.sent.isEmpty());
    }
    @Test void largerFoodsThatTheServerPartlyConsumesOnCancelAreRejected() {
        Fixture f=new Fixture();f.port.hunger=-.05f;f.port.baseHunger=-.05f;f.controller.arm();
        assertFalse(f.controller.prepare(f.port.item,1f));assertTrue(f.port.sent.isEmpty());
    }
    @Test void partlyEatenFoodIsAcceptedButUnsafeAndNonFiniteFoodIsNot() {
        Fixture f=new Fixture();f.controller.arm();assertTrue(f.controller.prepare(f.port.item,.25f)); // Vanilla finishes this tiny remaining item.
        // A partly eaten item reports a smaller |hungChange*100| as its use count; that residue must not
        // disqualify it, because the owner asked for partly eaten food to be usable.
        f=new Fixture();f.port.uses=2;f.controller.arm();assertTrue(f.controller.prepare(f.port.item,1f));
        f=new Fixture();f.port.safe=false;f.controller.arm();assertFalse(f.controller.prepare(f.port.item,1f));
        f=new Fixture();f.port.fatigue=Float.NaN;f.controller.arm();assertFalse(f.controller.prepare(f.port.item,1f));
        f=new Fixture();f.controller.arm();assertFalse(f.controller.prepare(f.port.item,Float.NaN));
    }
    @Test void theQueuedActionMayHoldANewInstanceOfTheSameSelectedItem() {
        // Queueing the meal replaces the client-side object while the server-side id stays the same, so an
        // identity check rejected every real meal: the log showed the same id on both sides of the test.
        Fixture f=new Fixture();f.port.selectionOn=true;assertTrue(f.controller.arm());
        Object queued=new Object();f.port.sameId.add(queued);
        assertTrue(f.controller.prepare(queued,.25f));
        assertEquals(FoodRecoveryController.Phase.PREPARED,f.controller.phase());
    }
    @Test void aDifferentItemIdStillCannotConsumeThePreparation() {
        Fixture f=new Fixture();f.port.selectionOn=true;assertTrue(f.controller.arm());
        Object otherFood=new Object();
        assertFalse(f.controller.prepare(otherFood,.25f));
        assertEquals(FoodRecoveryController.Phase.ARMED,f.controller.phase());
        assertTrue(f.port.sent.isEmpty());
    }
    @Test void ordinaryActionCompletionOnlyStartsServerVerification() {
        Fixture f=new Fixture();f.start();f.controller.actionCompleted();
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
        assertEquals(FoodRecoveryController.Result.NONE,f.controller.result());
    }
    @Test void twoSpacedFreshServerSamplesAutomaticallyFinishTheOneShot() {
        Fixture f=new Fixture();f.start();f.controller.actionCompleted();f.port.exists=false;
        f.port.time+=1;f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
        f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),.99f,.01f);
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        assertEquals(FoodRecoveryController.Result.SUCCESS,f.controller.result());
        assertEquals(FoodRecoveryController.Cleanup.NOT_NEEDED,f.controller.cleanup());
        assertEquals(1,f.port.sent.size());
    }
    @Test void aNonRecoveredSampleResetsTheConsecutiveConfirmation() {
        Fixture f=new Fixture();f.start();f.controller.actionCompleted();
        f.port.time+=1;f.controller.observeServerStats(f.port.context(),1f,0f);
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),.4f,.7f);
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Result.SUCCESS,f.controller.result());
    }
    @Test void samplesReceivedBeforeCompletionAreNotCompletionProof() {
        Fixture f=new Fixture();f.start();
        f.port.time+=1;f.controller.observeServerStats(f.port.context(),1f,0f);
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),1f,0f);
        f.controller.actionCompleted();assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
    }
    @Test void cancellationRestoresOnlyOriginalEffectsOnTheSameUnconsumedFood() {
        Fixture f=new Fixture();f.start();f.controller.cancel();
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        assertEquals(FoodRecoveryController.Result.CANCELLED,f.controller.result());
        assertEquals(FoodRecoveryController.Cleanup.REQUESTED,f.controller.cleanup());
        assertEquals(2,f.port.sent.size());
        float[] restore=f.port.sent.get(1);
        // The rollback restores the snapshot, not zeros: a cancelled meal must leave the food's own
        // thirst/stress/boredom/unhappiness profile untouched.
        assertEquals(6,restore.length);
        assertEquals(f.port.snapshotThirst,restore[0]);assertEquals(f.port.endurance,restore[1]);
        assertEquals(f.port.fatigue,restore[2]);assertEquals(f.port.snapshotStress,restore[3]);
        f.controller.actionCompleted();assertEquals(FoodRecoveryController.Result.CANCELLED,f.controller.result());
    }
    @Test void noRollbackIsSentToAnItemWhoseConsumptionStateChanged() {
        Fixture f=new Fixture();f.start();f.port.hunger=-.004f;f.controller.cancel();
        assertEquals(FoodRecoveryController.Cleanup.UNCERTAIN,f.controller.cleanup());assertEquals(1,f.port.sent.size());
    }
    @Test void noRollbackCrossesAConnectionOrPlayerReplacement() {
        Fixture f=new Fixture();f.start();f.port.connection=new Object();f.controller.tick();
        assertEquals(FoodRecoveryController.Result.CONTEXT_LOST,f.controller.result());
        assertEquals(FoodRecoveryController.Cleanup.UNCERTAIN,f.controller.cleanup());assertEquals(1,f.port.sent.size());
        f=new Fixture();f.start();f.port.player=new Object();f.controller.tick();assertEquals(1,f.port.sent.size());
    }
    @Test void aNewLocalLockEndsVerificationInsteadOfProducingFalseSuccess() {
        Fixture f=new Fixture();f.start();f.controller.actionCompleted();f.port.locks=true;
        f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Result.CONFLICT,f.controller.result());
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
    }
    @Test void failureDuringSubmissionEndsThePreparationWithoutRetryLoops() {
        Fixture f=new Fixture();f.controller.arm();f.port.sendOk=false;
        assertFalse(f.controller.prepare(f.port.item,1f));
        assertEquals(FoodRecoveryController.Result.SEND_FAILED,f.controller.result());
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        assertEquals(FoodRecoveryController.Cleanup.UNCERTAIN,f.controller.cleanup());
        int attempts=f.port.sent.size();f.port.time+=10000;f.controller.tick();assertEquals(attempts,f.port.sent.size());
    }
    @Test void eachPhaseHasABoundedTimeoutAndAutomaticallyTurnsOff() {
        Fixture f=new Fixture();f.controller.arm();f.port.time+=90000;f.controller.tick();
        assertEquals(FoodRecoveryController.Result.TIMED_OUT,f.controller.result());assertTrue(f.port.sent.isEmpty());
        f=new Fixture();f.controller.arm();f.controller.prepare(f.port.item,1f);f.port.time+=15000;f.controller.tick();
        assertEquals(FoodRecoveryController.Result.TIMED_OUT,f.controller.result());
        f=new Fixture();f.start();f.port.time+=120000;f.controller.tick();assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        f=new Fixture();f.start();f.controller.actionCompleted();f.port.time+=10000;f.controller.tick();
        assertEquals(FoodRecoveryController.Result.TIMED_OUT,f.controller.result());
    }
    @Test void aGlobalDeadlineCannotBeExtendedByMovingBetweenPhases() {
        Fixture f=new Fixture();f.controller.arm();f.port.time+=89000;
        f.controller.observeServerStats(f.port.context(),.2f,.8f);f.controller.prepare(f.port.item,1f);
        f.port.time+=14000;f.controller.actionStarted();f.port.time=180100;f.controller.tick();
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());assertEquals(FoodRecoveryController.Result.TIMED_OUT,f.controller.result());
    }
    @Test void invalidServerNumbersCannotCountAsConfirmation() {
        Fixture f=new Fixture();f.start();f.controller.actionCompleted();
        f.controller.observeServerStats(f.port.context(),Float.NaN,0f);
        f.port.time+=1000;f.controller.observeServerStats(f.port.context(),1f,Float.NEGATIVE_INFINITY);
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.controller.phase());
    }
    @Test void boostMustSurviveUntilTheServerActuallyConsumesTheFood() {
        // EatFoodPacket.write() serialises the client Food object; reverting the effects before the eat
        // action reaches the server makes the eat apply the original 0/0 values (observed as "no recovery").
        Effects fields=new Effects();
        assertTrue(FoodRecoveryEffects.begin(fields,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f}));
        assertEquals(1f,fields.e);assertEquals(-1f,fields.f);assertEquals(-.1f,fields.t);
        assertEquals(-10f,fields.b);assertEquals(-12f,fields.u);
        assertEquals(1f,fields.e,"boost must persist after arming, not be reverted immediately");
    }
    @Test void finishingTheBoostRestoresTheOriginalLocalEffects() {
        Effects fields=new Effects();
        assertTrue(FoodRecoveryEffects.begin(fields,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f}));
        FoodRecoveryEffects.end(fields);
        assertEquals(.03f,fields.e);assertEquals(.02f,fields.f);assertEquals(-.1f,fields.t);
        assertEquals(10f,fields.b);assertEquals(12f,fields.u);
    }
    @Test void finishingWithoutArmingCannotInventAnOriginalValue() {
        Effects fields=new Effects();fields.e=1f;fields.f=-1f;
        FoodRecoveryEffects.end(fields);
        assertEquals(1f,fields.e,"end() without begin() must not guess an original value");
    }
    @Test void armingRejectsNonFiniteOrAlreadyBrokenFields() {
        Effects fields=new Effects();
        assertFalse(FoodRecoveryEffects.begin(fields,new float[]{-.1f,Float.NaN,-1f,-.05f,-10f,-12f}));
        assertFalse(FoodRecoveryEffects.begin(fields,new float[]{-.1f,1f,Float.POSITIVE_INFINITY,-.05f,-10f,-12f}));
        assertEquals(.03f,fields.e);assertEquals(.02f,fields.f);
    }
    @Test void aSecondSubmitMustNotTreatTheBoostedValueAsTheOriginal() {
        // Cancel-after-prepare re-submits to restore; a fresh Fields instance used to overwrite the saved
        // originals with the boosted 1/-1, so the food stayed enhanced and end() could not undo it.
        Effects first=new Effects();Effects second=new Effects();
        assertTrue(FoodRecoveryEffects.begin(first,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f}));
        first.e=1f;first.f=-1f;second.e=1f;second.f=-1f;
        assertTrue(FoodRecoveryEffects.begin(second,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f}));
        FoodRecoveryEffects.end(second);
        assertEquals(.03f,second.e,"originals must stay the pre-boost values");
        assertEquals(.02f,second.f);
    }
    @Test void endingWithADifferentHandleStillRestoresTheRealOriginals() {
        Effects first=new Effects();Effects second=new Effects();
        assertTrue(FoodRecoveryEffects.begin(first,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f}));
        FoodRecoveryEffects.end(second);
        assertEquals(.03f,second.e);assertEquals(.02f,second.f);
    }
    @Test void everyEndingReleasesTheBoostExactlyOnce() {
        for(String ending:new String[]{"cancel","timeout","contextLost","hookFailed","success"}) {
            Spy s=new Spy();FoodRecoveryController c=new FoodRecoveryController(s);
            c.observeServerStats(s.context(),.2f,.8f);assertTrue(c.arm(),"arm failed: "+ending);assertTrue(c.prepare(s.item,1f),"prepare failed: "+ending+" phase="+c.phase()+" result="+c.result());c.actionStarted();
            switch(ending){
                case "cancel" -> c.cancel();
                case "timeout" -> {s.time+=120001;c.tick();}
                case "contextLost" -> {s.supported=false;c.tick();}
                case "hookFailed" -> c.hookFailed();
                case "success" -> {c.actionCompleted();s.time++;c.observeServerStats(s.context(),1f,0f);s.time+=500;c.observeServerStats(s.context(),1f,0f);}
                default -> throw new IllegalStateException();
            }
            assertEquals(-1,s.released,"ending "+ending+" must release the boost exactly once");
        }
    }
    static final class Spy implements FoodRecoveryController.Port {
        long time=100;Object item=new Object(),player=new Object(),connection=new Object();boolean supported=true;int released;
        public long now(){return time;}
        public FoodRecoveryController.Context context(){return new FoodRecoveryController.Context(connection,player,supported,false);}
        public FoodRecoveryController.Food inspect(Object v){return v==item?new FoodRecoveryController.Food(item,7,-.01f,-.01f,0f,.02f,.01f,0f,0f,0f,1,true,true):null;}
        public boolean exists(FoodRecoveryController.Food k){return true;}
        public boolean submit(FoodRecoveryController.Food k,float[] v){return true;}
        public FoodRecoveryController.Selection select(int budget){return new FoodRecoveryController.Selection(item,1,false);}
        public void releaseEffects(){released--;}
    }
    @Test void theBoostStaysVisibleToTheSerialisedFoodAndIsReleasedOnlyOnEnd() {
        // EatFoodPacket.write() saves the live Food object, so the boost has to be present when the eat
        // action serialises it; releasing right after the stats packet was the "no recovery" defect.
        Effects fields=new Effects();float[] seen=new float[2];
        assertTrue(FoodRecoveryEffects.send(fields,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f},()->{seen[0]=fields.e;seen[1]=fields.f;}));
        assertArrayEquals(new float[]{1f,-1f},seen);
        assertEquals(1f,fields.e);assertEquals(-1f,fields.f);
        FoodRecoveryEffects.end(fields);
        assertEquals(.03f,fields.e);assertEquals(.02f,fields.f);
    }
    @Test void transportFailureStillRestoresBothOriginalLocalEffects() {
        Effects fields=new Effects();
        assertFalse(FoodRecoveryEffects.send(fields,new float[]{-.1f,1f,-1f,-.05f,-10f,-12f},()->{throw new java.io.IOException("send failed");}));
        assertEquals(.03f,fields.e);assertEquals(.02f,fields.f);
    }
    @Test void nonFiniteEffectsNeverReachTheTransport() {
        Effects fields=new Effects();int[] sends={0};
        assertFalse(FoodRecoveryEffects.send(fields,new float[]{-.1f,Float.NaN,-1f,-.05f,-10f,-12f},()->sends[0]++));
        assertEquals(0,sends[0]);assertEquals(.03f,fields.e);assertEquals(.02f,fields.f);
    }
    static final class Effects implements FoodRecoveryEffects.Fields {
        float e=.03f,f=.02f,t=-.1f,s=-.05f,b=10f,u=12f;
        public float thirst(){return t;}public void thirst(float value){t=value;}
        public float endurance(){return e;}public void endurance(float value){e=value;}
        public float fatigue(){return f;}public void fatigue(float value){f=value;}
        public float stress(){return s;}public void stress(float value){s=value;}
        public float boredom(){return b;}public void boredom(float value){b=value;}
        public float unhappy(){return u;}public void unhappy(float value){u=value;}
    }
    @Test void naturallyRecoveredServerStateEndsPreparationWithoutEditingFood() {
        Fixture f=new Fixture();assertTrue(f.controller.arm());
        f.port.time+=500;f.controller.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.OFF,f.controller.phase());
        assertEquals(FoodRecoveryController.Result.NO_NEED,f.controller.result());
        assertFalse(f.controller.prepare(f.port.item,1f));assertTrue(f.port.sent.isEmpty());
    }
    static final class Fixture {
        final Port port=new Port();final FoodRecoveryController controller=new FoodRecoveryController(port);
        Fixture(){controller.observeServerStats(port.context(),.2f,.8f);}
        void start(){assertTrue(controller.arm());assertTrue(controller.prepare(port.item,1f));controller.actionStarted();}
    }
    static final class Port implements FoodRecoveryController.Port {
        long time=100;Object connection=new Object(),player=new Object(),item=new Object();
        boolean supported=true,locks=false,rootOwned=true,safe=true,exists=true,sendOk=true,selectionOn=false;
        float baseHungerValue=-.01f;
        final java.util.Set<Object> sameId=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int uses=1;float hunger=-.01f,baseHunger=-.01f,endurance=.03f,fatigue=.02f;
        float snapshotThirst=0f,snapshotStress=0f,snapshotBoredom=0f,snapshotUnhappy=0f;
        final List<float[]> sent=new ArrayList<>();
        public long now(){return time;}
        public FoodRecoveryController.Context context(){return new FoodRecoveryController.Context(connection,player,supported,locks);}
        public FoodRecoveryController.Selection select(int budget){return selectionOn?new FoodRecoveryController.Selection(item,1,false):null;}
        public FoodRecoveryController.Food inspect(Object value){
            if(value==null)return null;
            int id=value==item||sameId.contains(value)?123:999;
            return new FoodRecoveryController.Food(value,id,baseHungerValue,hunger,0f,endurance,fatigue,0f,0f,0f,uses,rootOwned,safe);
        }
        public boolean exists(FoodRecoveryController.Food key){return exists;}
        public boolean submit(FoodRecoveryController.Food key,float[] v){sent.add(v.clone());return sendOk;}
    }
}
