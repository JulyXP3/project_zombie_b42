package modcore.core;

/**
 * Food effect boost that stays armed until the meal actually settles.
 *
 * <p>ItemStatsPacket carries these food values to the server, which writes them onto its own copy of
 * the item; the normal eat then settles them against the character's statistics. begin() arms the
 * boost, end() restores the saved originals once the transaction is over.
 */
public final class FoodRecoveryEffects {
    /** The food values this recovery writes. Unmodified getters/setters only: the game rescales the
     *  cooked/spoiled variants at read time, so storing a scaled value would be rescaled twice. */
    public interface Fields {
        float thirst(); void thirst(float value);
        float endurance(); void endurance(float value);
        float fatigue(); void fatigue(float value);
        float stress(); void stress(float value);
        float boredom(); void boredom(float value);
        float unhappy(); void unhappy(float value);
    }
    @FunctionalInterface public interface Transport { void send() throws Exception; }
    private FoodRecoveryEffects() { }
    static float[] captureValues(Fields fields){
        if(fields==null)throw new IllegalArgumentException("missing food fields");
        return new float[]{fields.thirst(),fields.endurance(),fields.fatigue(),fields.stress(),fields.boredom(),fields.unhappy()};
    }
    static void applyValues(Fields fields,float[] values){
        if(fields==null||values==null||values.length!=6)throw new IllegalArgumentException("six food fields required");
        for(float value:values)if(!Float.isFinite(value))throw new IllegalArgumentException("non-finite food value");
        fields.thirst(values[0]);fields.endurance(values[1]);fields.fatigue(values[2]);
        fields.stress(values[3]);fields.boredom(values[4]);fields.unhappy(values[5]);
    }

    private static Fields armed;
    private static float originalThirst,originalEndurance,originalFatigue,originalStress,originalBoredom,originalUnhappy;

    /** Arms the boost and leaves it applied until end() runs. */
    public static boolean begin(Fields fields,float[] values) {
        if(fields==null||values==null||values.length!=6)return false;
        for(float value:values)if(!Float.isFinite(value))return false;
        float currentThirst=fields.thirst(),currentEndurance=fields.endurance(),currentFatigue=fields.fatigue();
        float currentStress=fields.stress(),currentBoredom=fields.boredom(),currentUnhappy=fields.unhappy();
        if(!Float.isFinite(currentThirst)||!Float.isFinite(currentEndurance)||!Float.isFinite(currentFatigue)
                ||!Float.isFinite(currentStress)||!Float.isFinite(currentBoredom)||!Float.isFinite(currentUnhappy))return false;
        // Save the originals only while nothing is armed: a follow-up begin() (restore re-submit) must not
        // capture the already-boosted values as the value to restore later.
        if(armed==null){armed=fields;originalThirst=currentThirst;originalEndurance=currentEndurance;originalFatigue=currentFatigue;
            originalStress=currentStress;originalBoredom=currentBoredom;originalUnhappy=currentUnhappy;}
        applyValues(fields,values);
        return true;
    }

    /** Restores the saved originals on whichever handle is holding the boost. */
    public static void end(Fields fields) {
        end(fields,new float[]{originalThirst,originalEndurance,originalFatigue,originalStress,originalBoredom,originalUnhappy});
    }
    /** Ends an accepted partial meal at its cleaned remaining values, not at old whole-food values. */
    public static void end(Fields fields,float[] restoration){
        if(fields==null||armed==null)return;
        try{applyValues(fields,restoration);}finally{armed=null;}
    }

    /**
     * Writes the boost, transports it, and keeps it armed.
     * The boost is only reverted by end(), after consumption or cancellation.
     */
    public static boolean send(Fields fields,float[] values,Transport transport) {
        if(transport==null||!begin(fields,values))return false;
        try { transport.send(); return true; }
        catch(Exception failure){end(fields);return false;}
    }
}
