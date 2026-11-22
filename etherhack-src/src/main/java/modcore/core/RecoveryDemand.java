package modcore.core;

/**
 * Current readings for every restored state, taken straight from the local player's server-synced
 * statistics. Each reading uses the same unit as the matching food value, so one dose is simply
 * "how far this state is from its good end, divided by the served fraction".
 */
public record RecoveryDemand(float thirst,float endurance,float fatigue,float stress,float boredom,float unhappy) {
    public RecoveryDemand {
        if(!inRange(thirst,1f)||!inRange(endurance,1f)||!inRange(fatigue,1f)
                ||!inRange(stress,1f)||!inRange(boredom,100f)||!inRange(unhappy,100f))
            throw new IllegalArgumentException("recovery readings must match the stat ranges");
    }
    /** Value the food must carry so that one serving of {@code fraction} moves this state to its good end. */
    public float perUnit(Restored stat,float fraction){
        if(stat==null||!Float.isFinite(fraction)||fraction<=0f||fraction>1f)return Float.NaN;
        float remaining;
        switch(stat){
            case ENDURANCE: remaining=1f-endurance; break;
            case FATIGUE:   remaining=-fatigue;     break;
            case THIRST:    remaining=-thirst;      break;
            case STRESS:    remaining=-stress;      break;
            case BOREDOM:   remaining=-boredom;     break;
            case UNHAPPY:   remaining=-unhappy;     break;
            default: return Float.NaN;
        }
        // Reducing states need a negative delta. Only a zero difference means no change is needed.
        float value=remaining==0f?0f:remaining/fraction;
        return Float.isFinite(value)?value:Float.NaN;
    }
    private static boolean inRange(float value,float maximum){
        return Float.isFinite(value)&&value>=0f&&value<=maximum;
    }
    /** True when every state is already at its good end. */
    public boolean recovered(){
        return thirst<=0f&&endurance>=1f&&fatigue<=0f&&stress<=0f&&boredom<=0f&&unhappy<=0f;
    }
}
