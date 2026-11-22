package modcore.core;

/** Raw consumable amounts, projected with the same float multiplication/int truncation as Food.multiplyFoodValues. */
public record FoodRecoveryAmounts(float hunger,float thirst,float endurance,float fatigue,float stress,
        float boredom,float unhappy,float calories,float carbs,float proteins,float lipids,float pain,
        int flu,int sickness,int poison) {
    public boolean valid(){return Float.isFinite(hunger)&&Float.isFinite(thirst)&&Float.isFinite(endurance)
            &&Float.isFinite(fatigue)&&Float.isFinite(stress)&&Float.isFinite(boredom)&&Float.isFinite(unhappy)
            &&Float.isFinite(calories)&&Float.isFinite(carbs)&&Float.isFinite(proteins)&&Float.isFinite(lipids)&&Float.isFinite(pain);}
    public boolean safeAutomatic(){return valid()&&poison==0&&sickness<=0&&pain>=0f&&flu>=0;}
    public FoodRecoveryAmounts withEffects(float e,float f){return new FoodRecoveryAmounts(hunger,thirst,e,f,stress,boredom,unhappy,calories,carbs,proteins,lipids,pain,flu,sickness,poison);}
    /** Restores the item's own thirst/stress/boredom/unhappiness values after a boosted serving. */
    public FoodRecoveryAmounts withMood(float t,float s,float b,float u){return new FoodRecoveryAmounts(hunger,t,endurance,fatigue,s,b,u,calories,carbs,proteins,lipids,pain,flu,sickness,poison);}
    public FoodRecoveryAmounts withHunger(float h){return new FoodRecoveryAmounts(h,thirst,endurance,fatigue,stress,boredom,unhappy,calories,carbs,proteins,lipids,pain,flu,sickness,poison);}
    public FoodRecoveryAmounts scaled(float ratio){
        if(!Float.isFinite(ratio)||ratio<0f||ratio>1f)throw new IllegalArgumentException("invalid food remainder ratio");
        return new FoodRecoveryAmounts(hunger*ratio,thirst*ratio,endurance*ratio,fatigue*ratio,stress*ratio,
                boredom*ratio,unhappy*ratio,calories*ratio,carbs*ratio,proteins*ratio,lipids*ratio,pain*ratio,
                (int)(flu*ratio),(int)(sickness*ratio),(int)(poison*ratio));
    }
}
