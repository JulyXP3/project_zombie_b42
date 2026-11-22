package modcore.core;

/** One active transaction; UI getters never scan inventory or mutate the game. */
public final class FoodRecoveryController {
    public enum Phase { OFF, SELECTING, ARMED, PREPARED, EATING, VERIFYING, CLEANING }
    public enum Result { NONE, SUCCESS, CANCELLED, TIMED_OUT, UNSUPPORTED, CONFLICT,
        NO_SERVER_STATS, NO_NEED, NO_FOOD, UNSAFE_FOOD, SEND_FAILED, CONTEXT_LOST, HOOK_ERROR, BUSY }
    public enum Cleanup { NONE, NOT_NEEDED, WAITING, REQUESTED, UNCERTAIN }
    public record Context(Object connection,Object player,boolean supported,boolean locks) { }
    /**
     * Snapshot of the item taken before anything is written. Every value this recovery can overwrite must be
     * here, otherwise a cancelled meal could only be "restored" to zero and would silently destroy the food's
     * own thirst/stress/boredom/unhappiness profile.
     */
    public record Food(Object item,int id,float baseHunger,float hunger,float thirst,float endurance,float fatigue,
                       float stress,float boredom,float unhappy,int uses,boolean rootOwned,boolean safe) {
        /** Shape kept for callers that only model the two endurance/fatigue values. */
        public Food(Object item,int id,float baseHunger,float hunger,float endurance,float fatigue,int uses,boolean rootOwned,boolean safe){
            this(item,id,baseHunger,hunger,0f,endurance,fatigue,0f,0f,0f,uses,rootOwned,safe);
        }
    }
    public record Selection(Object item,int checked,boolean pending) { }
    public record Consumption(int id,float fraction,FoodRecoveryAmounts amounts) { }
    public record Remainder(int id,float hunger,float actualWeight,int uses) { }
    public interface Port {
        long now();Context context();Food inspect(Object item);boolean exists(Food key);
        /** Writes every restored food value at once; the order matches FoodRecoveryEffects.Fields. */
        boolean submit(Food key,float[] values);
        default boolean submit(Food key,float endurance,float fatigue){return submit(key,new float[]{0f,endurance,fatigue,0f,0f,0f});}
        default void beginSelection() { }
        /** null keeps the legacy manual-food entry; runtime ports implement bounded automatic selection. */
        default Selection select(int budget){return null;}
        default void cancelSelection() { }
        default boolean busy(){return false;}
        default boolean requiresConsumptionReceipts(){return false;}
        default boolean requiresFullStats(){return false;}
        default boolean stoppedActionCanConsume(Food food){return Math.abs(food.hunger()*100f)>1f;}
        /** Cache an accepted receipt before any cleanup consumes that evidence. */
        default void consumptionAccepted(Consumption value) { }
        default FoodRecoveryPortions.Dose dose(Food food,float request){return FoodRecoveryPortions.dose(food.baseHunger(),food.hunger(),0f,1f,request);}
        /** Current readings of every restored state; null keeps the legacy two-stat behaviour. */
        default RecoveryDemand demand(){return null;}
        default float request(Object item){Food f=inspect(item);return f==null?Float.NaN:FoodRecoveryPortions.smallRequest(f.baseHunger(),f.hunger());}
        default Cleanup restoreRemaining(Food original,Consumption consumption,Remainder remainder){return Cleanup.UNCERTAIN;}
        /** Drops any boost that must not outlive the transaction (called once on every terminal transition). */
        default void releaseEffects() { }
        /** Optional diagnostics sink; the game port leaves it silent. */
        default void debug(String message) { }
        /** Item identity for diagnostics. */
        default String describe(Object item){return String.valueOf(item);}
    }
    private final Port port;
    private Phase phase=Phase.OFF;private Result result=Result.NONE;private Cleanup cleanup=Cleanup.NONE;
    private Context owner,sampleOwner;private Food original;private Object selected;
    private FoodRecoveryPortions.Dose dose;private Consumption consumption;private Remainder remainder;
    private long deadline,globalDeadline,sampledAt=Long.MIN_VALUE,verifiedAt;private int confirmations,checked;
    private float sampledEndurance,sampledFatigue;private boolean started,actionPerformed,cleanupAttempted,foodConfirmed;
    private RecoveryDemand sampled;
    /** Last reported values, so repeated conditions are reported once. */
    private int selectionSteps,samples,ticks;
    public FoodRecoveryController(Port port){this.port=java.util.Objects.requireNonNull(port);}
    public synchronized Phase phase(){return phase;}public synchronized Result result(){return result;}public synchronized Cleanup cleanup(){return cleanup;}
    public synchronized Object candidate(){return selected;}public synchronized int checked(){return checked;}
    public synchronized boolean hasPreparedFood(){return original!=null;}
    public synchronized RecoveryDemand sampledDemand(){return sampled;}
    public synchronized float requestedPercentage(){return selected==null?Float.NaN:port.request(selected);}
    public synchronized int remainingSeconds(){return phase==Phase.OFF?0:(int)Math.max(0,(Math.min(deadline,globalDeadline)-port.now()+999)/1000);}
    public synchronized Result availability(){return decideAvailability();}
    private Result decideAvailability(){
        if(phase!=Phase.OFF)return Result.BUSY;Context c=port.context();
        if(c==null||!c.supported()||c.connection()==null||c.player()==null)return Result.UNSUPPORTED;
        if(c.locks())return Result.CONFLICT;if(port.busy())return Result.BUSY;
        if(!same(c,sampleOwner)||port.now()-sampledAt>2500)return Result.NO_SERVER_STATS;
        return recovered()?Result.NO_NEED:Result.NONE;
    }
    public synchronized boolean arm(){
        tick();if(phase!=Phase.OFF){return false;}
        Result reason=availability();result=reason;cleanup=Cleanup.NONE;
        if(reason!=Result.NONE){return false;}
        owner=port.context();original=null;selected=null;dose=null;consumption=null;remainder=null;confirmations=0;checked=0;started=false;actionPerformed=false;cleanupAttempted=false;foodConfirmed=false;
        selectionSteps=0;
        deadline=port.now()+10000;globalDeadline=port.now()+180000;phase=Phase.SELECTING;port.beginSelection();advanceSelection();return true;
    }
    private void advanceSelection(){
        Selection next=port.select(128);
        selectionSteps++;
        // null means this port does not perform automatic selection (legacy manual entry); it is not a
        // missing candidate, so the transaction simply waits for the caller to prepare an item.
        if(next==null){phase=Phase.ARMED;deadline=port.now()+30000;return;}
        checked+=Math.max(0,next.checked());
        if(next.item()!=null){selected=next.item();phase=Phase.ARMED;deadline=port.now()+30000;return;}
        if(!next.pending()){complete(Result.NO_FOOD,Cleanup.NOT_NEEDED);return;}
            }
    public synchronized boolean prepare(Object item,float percentage){
        tick();
        if(phase!=Phase.ARMED){return false;}
        if(!same(owner,sampleOwner)||port.now()-sampledAt>2500){finish(Result.NO_SERVER_STATS,true);return false;}
        Food food;try{food=port.inspect(item);}catch(RuntimeException e){finish(Result.HOOK_ERROR,true);return false;}
        // Queueing transfers the food into the body container and the game then hands the action a fresh
        // client instance of the same server item, so identity is the wrong test: compare the item id.
        if(!selectedIs(food)){return false;}
        if(!eligible(food)){finish(Result.UNSAFE_FOOD,true);return false;}
        FoodRecoveryPortions.Dose d=port.dose(food,percentage);
        if(d==null||(!port.requiresConsumptionReceipts()&&(d.fraction()!=1f||port.stoppedActionCanConsume(food)))){finish(Result.UNSAFE_FOOD,true);return false;}
        original=food;dose=d;phase=Phase.PREPARED;deadline=port.now()+15000;
        boolean sent;try{sent=port.submit(food,values(d));}catch(RuntimeException e){sent=false;}
        if(!sent){finish(Result.SEND_FAILED,true);return false;}
        return true;
    }
    public synchronized void actionStarted(){tick();if(phase!=Phase.PREPARED){return;}
        started=true;phase=Phase.EATING;deadline=port.now()+120000;}
    public synchronized void actionCompleted(){
        tick();if(original==null)return;actionPerformed=true;
        if(phase==Phase.CLEANING){deadline=Math.min(port.now()+10000,globalDeadline);return;}
        if(phase!=Phase.EATING){return;}
        phase=Phase.VERIFYING;deadline=port.now()+10000;confirmations=0;verifiedAt=0;
    }
    public synchronized void actionStopped(){
        tick();if(phase!=Phase.OFF)finish(phase==Phase.CLEANING&&result!=Result.NONE?result:Result.CANCELLED,true,true);
    }
    public synchronized void cancel(){if(phase==Phase.CLEANING){return;}
        if(phase!=Phase.OFF){finish(Result.CANCELLED,true);}}
    public synchronized void hookFailed(){if(phase!=Phase.OFF){finish(Result.HOOK_ERROR,true);}}
    public synchronized void contextLost(){if(phase!=Phase.OFF){finish(Result.CONTEXT_LOST,false);}
        sampleOwner=null;sampledAt=Long.MIN_VALUE;sampled=null;}
    public synchronized void tick(){
        if(phase==Phase.OFF)return;Context c=port.context();
        if(c==null||!c.supported()||!same(owner,c)){finish(Result.CONTEXT_LOST,false);return;}
        if(c.locks()&&phase!=Phase.CLEANING){finish(Result.CONFLICT,true);return;}
        if(port.now()>=Math.min(deadline,globalDeadline)){
            if(phase==Phase.CLEANING)complete(result,Cleanup.UNCERTAIN);else{finish(Result.TIMED_OUT,true);}return;
        }
        if(phase==Phase.SELECTING)advanceSelection();
            }
    /** Legacy entry without mood readings. */
    public synchronized void observeServerStats(Context c,float endurance,float fatigue){
        observeServerStats(c,endurance,fatigue,null);
    }
    public synchronized void observeServerStats(Context c,float endurance,float fatigue,RecoveryDemand demand){
        tick();
        if(c==null||!c.supported()||!same(c,port.context())){return;}
        if(c.locks()){sampleOwner=null;sampledAt=Long.MIN_VALUE;return;}
        if(!valid(endurance)||!valid(fatigue)||(port.requiresFullStats()&&demand==null)
                ||(demand!=null&&(!near(endurance,demand.endurance())||!near(fatigue,demand.fatigue())))){
            sampleOwner=null;sampledAt=Long.MIN_VALUE;sampled=null;confirmations=0;return;}
        sampleOwner=c;sampledAt=port.now();sampledEndurance=endurance;sampledFatigue=fatigue;sampled=demand;
        samples++;
                if((phase==Phase.SELECTING||phase==Phase.ARMED)&&recovered()){finish(Result.NO_NEED,true);return;}
        if(phase!=Phase.VERIFYING||!same(owner,c))return;
        if(!recovered()){confirmations=0;return;}
        if(confirmations==0){confirmations=1;verifiedAt=port.now();return;}
        if(port.now()-verifiedAt<500)return;confirmations++;checkSuccess();
    }
    public synchronized void observeConsumption(Context c,Consumption value){
        tick();
        if(original==null){return;}
        if(dose==null||value==null||!same(owner,c)||!same(c,port.context())){return;}
        if(value.id()!=original.id()){return;}
        FoodRecoveryAmounts a=value.amounts();float fraction=value.fraction();
        if(a==null||!a.valid()||!Float.isFinite(fraction)||fraction<0f||fraction>1f){return;}
        // All six fields are written, including zeros and negative reductions. Every stored value must
        // come back before this receipt may prove consumption or enter the remaining-food cache.
        boolean applied=near(a.hunger(),original.hunger())&&near(a.endurance(),dose.endurance())&&near(a.fatigue(),dose.fatigue())
                &&near(a.thirst(),dose.thirst())&&near(a.stress(),dose.stress())
                &&near(a.boredom(),dose.boredom())&&near(a.unhappy(),dose.unhappy());
        if(!applied){return;}
        port.consumptionAccepted(value);
        consumption=value;foodConfirmed=fraction>0f;
        if(phase==Phase.CLEANING)deadline=Math.min(port.now()+10000,globalDeadline);
        tryCleanup();checkSuccess();
    }
    public synchronized void observeRemainder(Context c,Remainder value){
        tick();
        if(original==null){return;}
        if(value==null||!same(owner,c)||!same(c,port.context())||value.id()!=original.id()
                ||!Float.isFinite(value.hunger())||!Float.isFinite(value.actualWeight())||value.actualWeight()<0f){return;}
        remainder=value;tryCleanup();checkSuccess();
    }
    private void checkSuccess(){if(phase==Phase.VERIFYING&&confirmations>=2&&(!port.requiresConsumptionReceipts()||foodConfirmed))finish(Result.SUCCESS,true);}
    private void tryCleanup(){
        if(original==null||cleanupAttempted||consumption==null)return;
        // uses is Food.getCurrentUses(), which is |hungChange*100| for ordinary food: it changes as soon as
        // the food is partly eaten, so it cannot identify the same portion. Compare the remaining hunger.
        if(consumption.fraction()==1f){cleanup=Cleanup.NOT_NEEDED;cleanupAttempted=true;}
        else if(remainder!=null
                &&near(remainder.hunger(),consumption.amounts().hunger()*(1f-consumption.fraction()))){
            cleanupAttempted=true;try{cleanup=port.restoreRemaining(original,consumption,remainder);}catch(RuntimeException e){cleanup=Cleanup.UNCERTAIN;}
            if(cleanup!=Cleanup.REQUESTED&&cleanup!=Cleanup.NOT_NEEDED)cleanup=Cleanup.UNCERTAIN;
        }
        if(cleanupAttempted&&phase==Phase.CLEANING)complete(result,cleanup);
    }
    private void finish(Result reason,boolean allowRollback){finish(reason,allowRollback,false);}
    private void finish(Result reason,boolean allowRollback,boolean stopped){
        if(original==null){complete(reason,Cleanup.NOT_NEEDED);return;}
        Context c=port.context();if(!allowRollback||c==null||!c.supported()||!same(owner,c)){complete(reason,Cleanup.UNCERTAIN);return;}
        if(cleanupAttempted){complete(reason,cleanup);return;}
        if(!port.requiresConsumptionReceipts()||!started
                ||(stopped&&!actionPerformed&&!port.stoppedActionCanConsume(original)&&consumption==null)){
            Cleanup status=Cleanup.UNCERTAIN;
            try{
                if(!port.requiresConsumptionReceipts()&&!port.exists(original))status=Cleanup.NOT_NEEDED;
                else{Food live=port.inspect(original.item());
                    if(unchanged(live)&&port.submit(live,originalValues()))status=Cleanup.REQUESTED;}
            }catch(RuntimeException ignored){status=Cleanup.UNCERTAIN;}
            complete(reason,status);return;
        }
        long cleanupDeadline=started&&!stopped&&!actionPerformed?Math.max(deadline,port.now()+10000):port.now()+10000;
        result=reason;cleanup=Cleanup.WAITING;phase=Phase.CLEANING;deadline=Math.min(cleanupDeadline,globalDeadline);tryCleanup();
    }
    private boolean selectedIs(Food queued){
        if(selected==null)return true;
        if(queued==null)return false;
        if(selected==queued.item())return true;
        Food armed=port.inspect(selected);
        return armed!=null&&armed.id()==queued.id();
    }
    private boolean unchanged(Food live){return live!=null&&live.item()==original.item()&&live.id()==original.id()&&live.rootOwned()&&live.safe()
            &&live.uses()==original.uses()&&near(live.baseHunger(),original.baseHunger())&&near(live.hunger(),original.hunger())
            &&(near(live.endurance(),original.endurance())||near(live.endurance(),dose.endurance()))
            &&(near(live.fatigue(),original.fatigue())||near(live.fatigue(),dose.fatigue()))
            // The four added values must still read as either the original or the boosted number.
            &&(near(live.thirst(),original.thirst())||near(live.thirst(),dose.thirst()))
            &&(near(live.stress(),original.stress())||near(live.stress(),dose.stress()))
            &&(near(live.boredom(),original.boredom())||near(live.boredom(),dose.boredom()))
            &&(near(live.unhappy(),original.unhappy())||near(live.unhappy(),dose.unhappy()));}
    private void complete(Result reason,Cleanup status){phase=Phase.OFF;result=reason;cleanup=status;original=null;selected=null;owner=null;dose=null;consumption=null;remainder=null;confirmations=0;deadline=0;globalDeadline=0;
        try{port.cancelSelection();}finally{port.releaseEffects();}}
    /** Body-worn/held containers (and bags inside them) are addressable through the player's inventory. */
    public static boolean addressesBodyContainer(Food f){return f!=null&&f.item()!=null&&f.rootOwned();}
    private static boolean eligible(Food f){return f!=null&&f.item()!=null&&f.rootOwned()&&f.safe()&&Float.isFinite(f.baseHunger())
            &&Float.isFinite(f.hunger())&&Float.isFinite(f.endurance())&&Float.isFinite(f.fatigue())
            &&Float.isFinite(f.thirst())&&Float.isFinite(f.stress())&&Float.isFinite(f.boredom())&&Float.isFinite(f.unhappy())
            &&f.baseHunger()<=0f&&f.hunger()<=0f;}
    /** The six food values written by this serving, in FoodRecoveryEffects order. */
    private static float[] values(FoodRecoveryPortions.Dose d){
        return new float[]{d.thirst(),d.endurance(),d.fatigue(),d.stress(),d.boredom(),d.unhappy()};
    }
    /** Values that undo an accepted serving: the item's own readings, taken before the boost was written. */
    private float[] originalValues(){
        return new float[]{original.thirst(),original.endurance(),original.fatigue(),
                original.stress(),original.boredom(),original.unhappy()};
    }
    private static boolean near(float a,float b){return Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=1e-5f*Math.max(1f,Math.abs(b));}
    private static boolean valid(float x){return Float.isFinite(x)&&x>=0f&&x<=1f;}
    /** Every restored state is close enough to its good end. */
    private boolean recovered(){
        RecoveryDemand d=sampled;
        if(d!=null)return withinDrift(d.thirst(),original==null?0f:original.thirst(),1f)
                &&d.endurance()>=GOOD_HIGH&&d.fatigue()<=GOOD_LOW
                &&withinDrift(d.stress(),original==null?0f:original.stress(),1f)
                &&withinDrift(d.boredom(),original==null?0f:original.boredom(),100f)
                &&withinDrift(d.unhappy(),original==null?0f:original.unhappy(),100f);
        return sampledEndurance>=GOOD_HIGH&&sampledFatigue<=GOOD_LOW;
    }
    /**
     * These four states drift on their own while a meal is verified: a level already at its good end was
     * observed rising a few thousandths during verification, and an absolute threshold reported that as
     * "not recovered" (TIMED_OUT). Accept values that did not get meaningfully worse than the reading the
     * serving started from, measured against each state's own range.
     */
    private static boolean withinDrift(float value,float start,float range){
        return value<=Math.max(EXTRA_LOW,start)+DRIFT*range;
    }
    /** Tolerances: states scale 0..1, the two mood states scale 0..100. */
    private static final float GOOD_HIGH=.9f,GOOD_LOW=.1f;
    /** Allow only small passive drift in the four added states, not 10% untreated values. */
    private static final float EXTRA_LOW=.001f;
    /** Natural per-sample drift allowed on the four added states, as a fraction of their own range. */
    private static final float DRIFT=.02f;
    private static boolean same(Context a,Context b){return a!=null&&b!=null&&a.connection()!=null&&a.player()!=null&&a.connection()==b.connection()&&a.player()==b.player();}
}