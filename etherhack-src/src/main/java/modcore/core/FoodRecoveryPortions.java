package modcore.core;

/**
 * Mirrors the game's serving-ratio and tiny-remainder rules before deriving the recovery doses.
 *
 * <p>Units differ between stats: Endurance, Fatigue, Thirst and Stress are 0..1, while Boredom and
 * Unhappiness are 0..100. Each dose therefore uses its own scale; one shared multiplier would be off by
 * a factor of one hundred.
 */
public final class FoodRecoveryPortions {
    public record Dose(float fraction,float thirst,float endurance,float fatigue,float stress,
                       float boredom,float unhappy) {
        /** Shape kept for callers that only ever needed the two endurance/fatigue values. */
        public Dose(float fraction,float endurance,float fatigue){this(fraction,0f,endurance,fatigue,0f,0f,0f);}
    }
    private FoodRecoveryPortions() { }
    public static float smallRequest(float baseHunger,float hunger){
        if(!Float.isFinite(baseHunger)||!Float.isFinite(hunger))return Float.NaN;
        if(hunger==0f)return 1f;
        if(hunger>0f)return Float.NaN;
        if(baseHunger==0f)return .25f;
        if(baseHunger>0f)return Float.NaN;
        float requested=.25f*hunger/baseHunger;
        return Float.isFinite(requested)&&requested>0f?Math.min(1f,requested):Float.NaN;
    }
    public static float effectiveFraction(float baseHunger,float hunger,float thirst,float requested){
        if(!Float.isFinite(baseHunger)||!Float.isFinite(hunger)||!Float.isFinite(thirst)||!Float.isFinite(requested))return Float.NaN;
        float fraction=Math.max(0f,Math.min(1f,requested));
        if(baseHunger!=0f&&hunger!=0f)fraction=Math.max(0f,Math.min(1f,baseHunger*fraction/hunger));
        if(hunger<0f&&hunger*(1f-fraction)>-.01f)fraction=1f;
        if(hunger==0f&&thirst<0f&&thirst*(1f-fraction)>-.01f)fraction=1f;
        return fraction;
    }
    /** Legacy two-stat dose, kept for ports that only model endurance and fatigue. */
    public static Dose dose(float baseHunger,float hunger,float thirst,float enduranceMultiplier,float requested){
        return dose(baseHunger,hunger,thirst,enduranceMultiplier,requested,null);
    }
    /**
     * Derives every recovery dose for one serving. {@code demand} may be null, in which case only the
     * endurance and fatigue values are produced and the other food values stay untouched.
     */
    public static Dose dose(float baseHunger,float hunger,float thirst,float enduranceMultiplier,float requested,
                            RecoveryDemand demand){
        return dose(baseHunger,hunger,thirst,enduranceMultiplier,requested,demand,FoodRecoveryModifiers.IDENTITY);
    }
    public static Dose dose(float baseHunger,float hunger,float thirst,float enduranceMultiplier,float requested,
                            RecoveryDemand demand,FoodRecoveryModifiers modifiers){
        if(modifiers==null)return null;
        if(!Float.isFinite(enduranceMultiplier)||enduranceMultiplier<=0f||!Float.isFinite(requested)||requested<=0f)return null;
        float fraction=effectiveFraction(baseHunger,hunger,thirst,requested);
        if(!Float.isFinite(fraction)||fraction<=0f)return null;
        float endurance=1f/(fraction*enduranceMultiplier),fatigue=-1f/fraction;
        if(!Float.isFinite(endurance)||!Float.isFinite(fatigue))return null;
        if(demand==null)return new Dose(fraction,0f,endurance,fatigue,0f,0f,0f);
        float thirstDose=modifiers.storedForEffect(Restored.THIRST,demand.perUnit(Restored.THIRST,fraction));
        float stressDose=modifiers.storedForEffect(Restored.STRESS,demand.perUnit(Restored.STRESS,fraction));
        float boredomDose=modifiers.storedForEffect(Restored.BOREDOM,demand.perUnit(Restored.BOREDOM,fraction));
        float unhappyDose=modifiers.storedForEffect(Restored.UNHAPPY,demand.perUnit(Restored.UNHAPPY,fraction));
        if(!Float.isFinite(thirstDose)||!Float.isFinite(stressDose)||!Float.isFinite(boredomDose)||!Float.isFinite(unhappyDose))return null;
        return new Dose(fraction,thirstDose,endurance,fatigue,stressDose,boredomDose,unhappyDose);
    }
}
