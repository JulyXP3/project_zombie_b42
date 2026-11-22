package modcore.core;

/** Food getter corrections applied when ItemStats snapshots values and again when the server eats. */
public record FoodRecoveryModifiers(float thirstScale,float stressScale,float boredomOffset,float unhappyOffset) {
    public static final FoodRecoveryModifiers IDENTITY=new FoodRecoveryModifiers(1f,1f,0f,0f);
    public FoodRecoveryModifiers {
        if(!Float.isFinite(thirstScale)||thirstScale<=0f||!Float.isFinite(stressScale)||stressScale<=0f
                ||!Float.isFinite(boredomOffset)||!Float.isFinite(unhappyOffset))
            throw new IllegalArgumentException("invalid food getter modifiers");
    }
    /** Stored field whose server getter yields this requested character-stat delta. */
    public float storedForEffect(Restored stat,float delta){
        if(stat==null||!Float.isFinite(delta))return Float.NaN;
        return switch(stat){
            case THIRST -> delta/thirstScale;
            case STRESS -> delta/stressScale;
            case BOREDOM -> delta-boredomOffset;
            case UNHAPPY -> delta-unhappyOffset;
            case ENDURANCE,FATIGUE -> delta;
        };
    }
    /**
     * ItemStats reads corrected getters, not the raw fields. Temporarily invert those corrections while
     * it snapshots them, then restore the exact local raw values before serialisation or action callbacks.
     */
    public void capture(FoodRecoveryEffects.Fields fields,FoodRecoveryEffects.Transport snapshot)throws Exception{
        if(snapshot==null)throw new IllegalArgumentException("missing packet snapshot");
        float[] raw=FoodRecoveryEffects.captureValues(fields),transport=raw.clone();
        transport[0]=raw[0]/thirstScale;transport[3]=raw[3]/stressScale;
        transport[4]=raw[4]-boredomOffset;transport[5]=raw[5]-unhappyOffset;
        try{FoodRecoveryEffects.applyValues(fields,transport);snapshot.send();}
        finally{FoodRecoveryEffects.applyValues(fields,raw);}
    }
}
