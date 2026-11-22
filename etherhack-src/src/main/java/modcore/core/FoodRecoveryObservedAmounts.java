package modcore.core;

import java.util.LinkedHashMap;

/** Small event-driven cache; no inventory iteration and no per-frame maintenance. */
public final class FoodRecoveryObservedAmounts {
    private static final class Observation {FoodRecoveryAmounts prepared,before,after,receiptBefore,receiptAfter;float e,f;}
    private Object owner;private final LinkedHashMap<Integer,Observation> items=new LinkedHashMap<>(16,.75f,true);
    public synchronized int size(){return items.size();}
    public synchronized void clear(){items.clear();owner=null;}
    private void owner(Object value){if(owner!=value){items.clear();owner=value;}}
    private Observation item(int id){Observation o=items.get(id);if(o==null){o=new Observation();items.put(id,o);}while(items.size()>64)items.remove(items.keySet().iterator().next());return o;}
    public synchronized void prepared(Object connection,int id,FoodRecoveryAmounts original,float e,float f){
        owner(connection);Observation o=item(id);o.prepared=original;o.e=e;o.f=f;
        o.before=null;o.after=null;o.receiptBefore=null;o.receiptAfter=null;
    }
    public synchronized void observe(Object connection,int id,FoodRecoveryAmounts before,float fraction){
        owner(connection);if(before==null||!before.valid()||!Float.isFinite(fraction)||fraction<0f||fraction>1f)return;
        Observation o=item(id);FoodRecoveryAmounts normal=before;
        if(o.prepared!=null&&near(before.endurance(),o.e)&&near(before.fatigue(),o.f)){
            if(!near(before.hunger(),o.prepared.hunger()))return;
            normal=before.withEffects(o.prepared.endurance(),o.prepared.fatigue())
                    .withMood(o.prepared.thirst(),o.prepared.stress(),o.prepared.boredom(),o.prepared.unhappy());
        }
        o.before=normal;o.after=normal.scaled(1f-fraction);
        o.receiptBefore=before;o.receiptAfter=before.scaled(1f-fraction);
    }
    public synchronized FoodRecoveryAmounts normalize(Object connection,int id,FoodRecoveryAmounts local){
        owner(connection);Observation o=items.get(id);if(local==null||o==null||o.before==null||o.after==null||!near(local.hunger(),o.after.hunger()))return local;
        FoodRecoveryAmounts b=o.before,a=o.after;
        FoodRecoveryAmounts rb=o.receiptBefore,ra=o.receiptAfter;if(rb==null||ra==null)return local;
        if(known(local.thirst(),b.thirst(),a.thirst(),rb.thirst(),ra.thirst())
                &&known(local.endurance(),b.endurance(),a.endurance(),rb.endurance(),ra.endurance())
                &&known(local.fatigue(),b.fatigue(),a.fatigue(),rb.fatigue(),ra.fatigue())
                &&known(local.stress(),b.stress(),a.stress(),rb.stress(),ra.stress())
                &&known(local.boredom(),b.boredom(),a.boredom(),rb.boredom(),ra.boredom())
                &&known(local.unhappy(),b.unhappy(),a.unhappy(),rb.unhappy(),ra.unhappy())
                &&pair(local.calories(),b.calories(),a.calories())&&pair(local.carbs(),b.carbs(),a.carbs())&&pair(local.proteins(),b.proteins(),a.proteins())
                &&pair(local.lipids(),b.lipids(),a.lipids())&&pair(local.pain(),b.pain(),a.pain())&&pair(local.flu(),b.flu(),a.flu())
                &&pair(local.sickness(),b.sickness(),a.sickness())&&pair(local.poison(),b.poison(),a.poison()))return a;
        return local;
    }
    public synchronized FoodRecoveryAmounts cleanupProjection(Object connection,int id,FoodRecoveryAmounts local,FoodRecoveryAmounts expected){
        if(owner!=connection||expected==null||!expected.valid())return null;
        Observation o=items.get(id);if(o==null||o.after==null||!o.after.equals(expected))return null;
        FoodRecoveryAmounts known=normalize(connection,id,local);
        return expected.equals(known)?expected:null;
    }
    private static boolean known(float value,float b,float a,float rb,float ra){return pair(value,b,a)||pair(value,rb,ra);}
    private static boolean pair(float value,float b,float a){return near(value,b)||near(value,a);}
    private static boolean near(float a,float b){return Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=1e-5f*Math.max(1f,Math.abs(b));}
}
