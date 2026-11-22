package modcore.core;

import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.Item;

/** ASM-free, one-click adapter. Inventory traversal occurs only inside active selection steps. */
public final class FoodRecoveryRuntime {
    private static volatile boolean hooksReady;
    private static final FoodRecoveryObservedAmounts OBSERVED=new FoodRecoveryObservedAmounts();
    private static final GamePort PORT=new GamePort();
    private static final FoodRecoveryController FLOW=new FoodRecoveryController(PORT);
    private FoodRecoveryRuntime() { }
    public static boolean arm(){return FLOW.arm();}
    public static void cancel(){FLOW.cancel();}
    public static boolean prepare(InventoryItem item,float fraction){return FLOW.prepare(item,fraction);}
    public static void started(){FLOW.actionStarted();}
    public static void completed(){FLOW.actionCompleted();}
    public static void stopped(){FLOW.actionStopped();}
    public static void failed(){FLOW.hookFailed();}
    public static void contextLost(){FLOW.contextLost();OBSERVED.clear();}
    public static void tick(){FLOW.tick();}
    public static String phase(){return FLOW.phase().name();}public static String result(){return FLOW.result().name();}
    public static String cleanup(){return FLOW.cleanup().name();}
    public static String availability(){return FLOW.availability().name();}public static int remainingSeconds(){return FLOW.remainingSeconds();}
    public static InventoryItem candidate(){Object v=FLOW.candidate();return v instanceof InventoryItem i?i:null;}
    public static float request(){return FLOW.requestedPercentage();}public static int checked(){return FLOW.checked();}
    public static String foodName(){return PORT.lastFoodName;}
    public static void setHooksReady(boolean value){
        hooksReady=value;
        if(!value){FLOW.cancel();FLOW.contextLost();OBSERVED.clear();}
    }
    public static void observeServerStats(IsoPlayer player){
        if(player==null)return;
        try{FoodRecoveryController.Context c=PORT.context();
            if(!c.supported()||c.player()!=player)return;
            if(c.locks()){FLOW.tick();return;}
            RecoveryDemand reading=demand(player);
            FLOW.observeServerStats(c,player.getStats().get(CharacterStat.ENDURANCE),player.getStats().get(CharacterStat.FATIGUE),reading);
        }catch(Throwable failure){FLOW.hookFailed();}
    }
    /** Snapshot only. The packet's Food remains unchanged for the vanilla OnEat callback. */
    public static void observeServerFood(IsoPlayer player,Food food,float fraction,UdpConnection connection){
        if(player==null||food==null||connection==null)return;
        try{FoodRecoveryController.Context c=PORT.context();
            if(!c.supported()||c.player()!=player||c.connection()!=connection)return;
            FoodRecoveryAmounts amounts=capture(food);
            if(!FLOW.hasPreparedFood())OBSERVED.observe(connection,food.getID(),amounts,fraction);
            FLOW.observeConsumption(c,new FoodRecoveryController.Consumption(food.getID(),fraction,amounts));
        }catch(Throwable failure){FLOW.hookFailed();}
    }
    public static void observeServerRemainder(InventoryItem item,UdpConnection connection){
        if(item==null||connection==null||!(item instanceof Food food))return;
        try{FoodRecoveryController.Context c=PORT.context();
            if(!c.supported()||c.connection()!=connection)return;
            FLOW.observeRemainder(c,new FoodRecoveryController.Remainder(food.getID(),food.getHungChange(),food.getActualWeightUnmodded(),food.getCurrentUses()));
        }catch(Throwable failure){FLOW.hookFailed();}
    }
    /** Ordinary food with no nutrition is still edible; a closed can is not. */
    private static boolean edible(Food f){
        if(f==null)return false;
        Item scriptItem=f.getScriptItem();
        return scriptItem==null||!Boolean.TRUE.equals(scriptItem.isCantEat());
    }
    private static boolean safe(Food f){
        return edible(f)&&!f.isRotten()&&!f.isBurnt()&&!f.isFrozen()&&!f.isTainted()&&!f.isAlcoholic()&&f.getPoisonPower()==0
                &&(!f.isbDangerousUncooked()||f.isCooked())&&(f.getOnEat()==null||f.getOnEat().isEmpty())
                &&(f.getRequireInHandOrInventory()==null||f.getRequireInHandOrInventory().isEmpty())
                &&Float.isFinite(f.getAge())&&f.getHungChange()<=0f&&capture(f).safeAutomatic();
    }
    private static float multiplier(Food f){if(f.getAge()>=f.getOffAge()&&f.getAge()<f.getOffAgeMax())return .5f;return f.isCooked()?2f:1f;}
    private static FoodRecoveryModifiers modifiers(Food food){
        float thirst=food.isBurnt()?.2f:food.isCooked()?.5f:1f;
        float stress;
        if(food.isFertilized())stress=1f;
        else if(food.isBurnt())stress=.25f;
        else if(food.getAge()>=food.getOffAge()&&food.getAge()<food.getOffAgeMax())stress=1f/1.3f;
        else if(food.getAge()>=food.getOffAgeMax())stress=.5f;
        else stress=food.isCooked()?1.3f:1f;
        return new FoodRecoveryModifiers(thirst,stress,food.getBoredomChange()-food.getBoredomChangeUnmodified(),
                food.getUnhappyChange()-food.getUnhappyChangeUnmodified());
    }
    private static FoodRecoveryEffects.Fields fields(Food food){
        return new FoodRecoveryEffects.Fields(){
            public float thirst(){return food.getThirstChangeUnmodified();}public void thirst(float x){food.setThirstChange(x);}
            public float endurance(){return food.getEndChange();}public void endurance(float x){food.setEndChange(x);}
            public float fatigue(){return food.getFatigueChange();}public void fatigue(float x){food.setFatigueChange(x);}
            public float stress(){return food.getStressChangeUnmodified();}public void stress(float x){food.setStressChange(x);}
            public float boredom(){return food.getBoredomChangeUnmodified();}public void boredom(float x){food.setBoredomChange(x);}
            public float unhappy(){return food.getUnhappyChangeUnmodified();}public void unhappy(float x){food.setUnhappyChange(x);}
        };
    }
    /** Current server-synced readings for every state this recovery restores. */
    private static RecoveryDemand demand(IsoPlayer player){
        if(player==null)return null;
        try{var stats=player.getStats();
            return new RecoveryDemand(stats.get(CharacterStat.THIRST),stats.get(CharacterStat.ENDURANCE),
                    stats.get(CharacterStat.FATIGUE),stats.get(CharacterStat.STRESS),
                    stats.get(CharacterStat.BOREDOM),stats.get(CharacterStat.UNHAPPINESS));
        }catch(Throwable failure){return null;}
    }
    private static FoodRecoveryAmounts capture(Food f){return new FoodRecoveryAmounts(f.getHungChange(),f.getThirstChangeUnmodified(),f.getEnduranceChangeUnmodified(),f.getFatigueChange(),
            f.getStressChangeUnmodified(),f.getBoredomChangeUnmodified(),f.getUnhappyChangeUnmodified(),f.getCalories(),f.getCarbohydrates(),f.getProteins(),f.getLipids(),f.getPainReduction(),f.getFluReduction(),f.getFoodSicknessChange(),f.getPoisonPower());}
    private static void apply(Food f,FoodRecoveryAmounts a){f.setHungChange(a.hunger());f.setThirstChange(a.thirst());f.setEndChange(a.endurance());f.setFatigueChange(a.fatigue());f.setStressChange(a.stress());
        f.setBoredomChange(a.boredom());f.setUnhappyChange(a.unhappy());f.setCalories(a.calories());f.setCarbohydrates(a.carbs());f.setProteins(a.proteins());f.setLipids(a.lipids());f.setPainReduction(a.pain());f.setFluReduction(a.flu());f.setFoodSicknessChange(a.sickness());f.setPoisonPower(a.poison());}
    /** True when the food sits anywhere inside the player's own body inventory, including worn bags. */
    private static boolean owned(Food f,IsoPlayer p){
        if(p==null||f==null)return false;
        ItemContainer outer=f.getOutermostContainer();
        return outer!=null&&outer==p.getInventory();
    }
    /** The item must still be reachable from the body inventory, but not necessarily in its root. */
    private static boolean body(Food f,IsoPlayer p){
        if(!owned(f,p))return false;
        ItemContainer container=f.getContainer();
        return container!=null&&container.getItemWithID(f.getID())==f;
    }
    private static String describe(InventoryItem item){
        if(!(item instanceof Food f))return String.valueOf(item);
        return f.getType()+"#"+f.getID()+" h="+f.getHungChange()+" e="+f.getEndChange()+" f="+f.getFatigueChange()+" uses="+f.getCurrentUses();
    }
    private static String gates(){
        try{
            IsoPlayer p=IsoPlayer.getInstance();UdpConnection c=GameClient.connection;
            CoreMain m=CoreMain.getInstance();CoreAPI api=m==null?null:m.CoreAPI;
            return "hooksReady="+hooksReady+" client="+GameClient.client+" conn="+(c!=null)
                    +" ready="+(c!=null&&c.isReady())+" player="+(p!=null)
                    +" local="+(p!=null&&p.isLocalPlayer())+" alive="+(p!=null&&p.isAlive())
                    +" square="+(p!=null&&p.getCurrentSquare()!=null)+" inv="+(p!=null&&p.getInventory()!=null)
                    +" api="+(api!=null);
        }catch(Throwable failure){return "gates threw "+failure;}
    }
    private static final class GamePort implements FoodRecoveryController.Port {
        private FoodRecoverySelection<InventoryItem,ItemContainer> selection;private String lastFoodName="";private String preparedKey;
        public long now(){return System.nanoTime()/1_000_000L;}
            public String describe(Object item){return item instanceof InventoryItem i?FoodRecoveryRuntime.describe(i):String.valueOf(item);}
        public FoodRecoveryController.Context context(){try{IsoPlayer p=IsoPlayer.getInstance();UdpConnection c=GameClient.connection;CoreMain m=CoreMain.getInstance();CoreAPI api=m==null?null:m.CoreAPI;
            // 客户端连接的就绪标志是 isReady()（IsoPlayer.setJustMoved 置位）；isFullyConnected() 只在
            // 服务端 setFullyConnected() 置位，客户端恒为 false，用它当门槛会永远 UNSUPPORTED。
            return new FoodRecoveryController.Context(c,p,hooksReady&&GameClient.client&&c!=null&&c.isReady()&&p!=null&&p.isLocalPlayer()&&p.isAlive()&&p.getCurrentSquare()!=null&&p.getInventory()!=null&&api!=null,
                    api!=null&&(api.isUnlimitedEndurance||api.isUnlimitedCondition||api.isDisableFatigue
                            ||api.isDisableThirst||api.isDisableStress||api.isDisableStressFromCigarettes
                            ||api.isDisableBoredomLevel||api.isDisableUnhappynessLevel));
        }catch(Throwable ignored){return new FoodRecoveryController.Context(null,null,false,true);}}
        /**
         * Only states that really prevent starting a new meal. "Performing any action" is deliberately not
         * included: holding an item or finishing an unrelated action must not silently disable the button.
         */
        public boolean busy(){IsoPlayer p=IsoPlayer.getInstance();boolean value=p==null||p.isAttacking()||p.isClimbing()||p.isAsleep();
                        return value;}
        public void beginSelection(){IsoPlayer p=IsoPlayer.getInstance();ItemContainer inventory=p==null?null:p.getInventory();lastFoodName="";preparedKey=null;
            int items=inventory==null?-1:inventory.getItems().size();
            selection=new FoodRecoverySelection<>(new FoodRecoverySelection.Source<InventoryItem,ItemContainer>(){
                public InventoryItem primary(){return p==null?null:p.getPrimaryHandItem();}public InventoryItem secondary(){return p==null?null:p.getSecondaryHandItem();}
                public ItemContainer root(){return inventory;}public int size(ItemContainer c){return c.getItems().size();}public InventoryItem item(ItemContainer c,int i){return c.getItems().get(i);}
                public ItemContainer child(InventoryItem item){return item instanceof InventoryContainer bag&&bag.getOutermostContainer()==inventory?bag.getInventory():null;}
                // Must match inspect()/prepare(): the ItemStats packet can only address items in the
                // player's own inventory container, so nested-bag food is not offered as a candidate.
                // Scan everything worn/held on the body, including contents of bags: most food cannot be
                // equipped in a hand, and ContainerID addresses a bag through its containing item id.
                public boolean eligible(InventoryItem item){
                    if(!(item instanceof Food f))return false;
                    boolean isBody=body(f,p),isSafe=safe(f);
                    FoodRecoveryPortions.Dose d=FoodRecoveryPortions.dose(f.getBaseHunger(),f.getHungChange(),f.getThirstChange(),multiplier(f),FoodRecoveryPortions.smallRequest(f.getBaseHunger(),f.getHungChange()));
                    boolean ok=isBody&&isSafe&&d!=null;
                                                        return ok;}
            });selection.start();
        }
        public FoodRecoveryController.Selection select(int budget){if(selection==null)return new FoodRecoveryController.Selection(null,0,false);int before=selection.checked();selection.step(budget);
            InventoryItem item=selection.selected();if(item!=null)lastFoodName=item.getDisplayName();return new FoodRecoveryController.Selection(item,selection.checked()-before,selection.state()==FoodRecoverySelection.State.SEARCHING);}
        public void cancelSelection(){if(selection!=null)selection.cancel();selection=null;}
        private FoodRecoveryEffects.Fields boosted;
        public boolean requiresConsumptionReceipts(){return true;}
        public boolean requiresFullStats(){return true;}
        public boolean stoppedActionCanConsume(FoodRecoveryController.Food original){
            if(!(original.item() instanceof Food food))return true;
            float hunger=food.getHungerChange();
            return !Float.isFinite(hunger)||Math.abs(hunger*100f)>1f;
        }
        public void consumptionAccepted(FoodRecoveryController.Consumption value){
            OBSERVED.observe(GameClient.connection,value.id(),value.amounts(),value.fraction());
        }
        public void releaseEffects(){FoodRecoveryEffects.Fields f=boosted;boosted=null;if(f!=null)FoodRecoveryEffects.end(f);}
        public FoodRecoveryController.Food inspect(Object value){if(!(value instanceof Food f))return null;IsoPlayer p=IsoPlayer.getInstance();
            if(FLOW.phase()==FoodRecoveryController.Phase.ARMED&&FLOW.candidate()==f&&body(f,p)){FoodRecoveryAmounts local=capture(f),known=OBSERVED.normalize(GameClient.connection,f.getID(),local);if(known!=local)apply(f,known);}
            // Every value this recovery may overwrite has to be snapshotted so a cancelled meal can put the
            // item back exactly as it was.
            return new FoodRecoveryController.Food(f,f.getID(),f.getBaseHunger(),f.getHungChange(),
                    f.getThirstChangeUnmodified(),f.getEndChange(),f.getFatigueChange(),
                    f.getStressChangeUnmodified(),f.getBoredomChangeUnmodified(),f.getUnhappyChangeUnmodified(),
                    f.getCurrentUses(),body(f,p),safe(f));}
        public RecoveryDemand demand(){return FLOW.sampledDemand();}
        public FoodRecoveryPortions.Dose dose(FoodRecoveryController.Food key,float request){
            RecoveryDemand reading=demand();
            return reading!=null&&key.item() instanceof Food f?FoodRecoveryPortions.dose(key.baseHunger(),key.hunger(),
                    f.getThirstChange(),multiplier(f),request,reading,modifiers(f)):null;
        }
        public float request(Object item){return item instanceof Food f?FoodRecoveryPortions.smallRequest(f.getBaseHunger(),f.getHungChange()):Float.NaN;}
        public boolean exists(FoodRecoveryController.Food key){if(!(key.item() instanceof Food f))return true;ItemContainer c=f.getContainer();return (c!=null&&c.getItemWithID(key.id())==f)||f.getWorldItem()!=null;}
        private String key(Food f){return f.getFullType()+"|"+f.getBaseHunger()+"|"+f.isCooked()+"|"+f.isBurnt()+"|"+f.isFrozen()+"|"+f.isRotten()+"|"+multiplier(f)+"|"+f.getOnEat();}
        public boolean submit(FoodRecoveryController.Food original,float[] v){
            if(v==null||v.length!=6){return false;}
            FoodRecoveryController.Context c=context();
            if(!c.supported()){return false;}
            if(!(c.connection() instanceof UdpConnection con)){return false;}
            if(!(c.player() instanceof IsoPlayer p)){return false;}
            if(!(original.item() instanceof Food food)){return false;}
            if(!body(food,p)){return false;}
            if(food.getID()!=original.id()){return false;}
            // Snapshot once per transaction: rollback re-submission must not cache the boosted item as original.
            if(FLOW.phase()==FoodRecoveryController.Phase.PREPARED&&preparedKey==null){
                preparedKey=key(food);OBSERVED.prepared(con,food.getID(),capture(food),v[1],v[2]);
            }
            FoodRecoveryEffects.Fields fields=fields(food);
            boolean sent=FoodRecoveryEffects.send(fields,v,()->sendFood(con,p,food));
            // Keep the local transaction values until the server receipt establishes a safe remainder.
            // ItemStats transfers the raw fields; the server's normal eat settles them.
            boosted=sent?fields:null;
            return sent;}
        public FoodRecoveryController.Cleanup restoreRemaining(FoodRecoveryController.Food original,FoodRecoveryController.Consumption consumption,FoodRecoveryController.Remainder remainder){
            FoodRecoveryController.Context c=context();if(!c.supported()||!(c.connection() instanceof UdpConnection con)||!(c.player() instanceof IsoPlayer p)||!(original.item() instanceof Food food)
                    ||!body(food,p)||food.getID()!=original.id()||!safe(food)||preparedKey==null||!preparedKey.equals(key(food))||food.getCurrentUses()!=remainder.uses()
                    ||Math.abs(food.getHungChange()-remainder.hunger())>1e-5f||Math.abs(food.getActualWeightUnmodded()-remainder.actualWeight())>1e-5f){return FoodRecoveryController.Cleanup.UNCERTAIN;}
            // Project the remainder from the item's own six values, not from the boosted numbers that were
            // written for this serving; otherwise a partly eaten item keeps the boosted profile forever.
            FoodRecoveryAmounts expected=consumption.amounts()
                    .withEffects(original.endurance(),original.fatigue())
                    .withMood(original.thirst(),original.stress(),original.boredom(),original.unhappy())
                    .scaled(1f-consumption.fraction());
            FoodRecoveryAmounts amounts=OBSERVED.cleanupProjection(con,food.getID(),capture(food),expected);
            if(amounts==null){return FoodRecoveryController.Cleanup.UNCERTAIN;}
            // Keep the pre-cleanup values: if the packet fails the local item must not silently end up
            // rewritten while the server still holds the old numbers.
            FoodRecoveryAmounts before=capture(food);
            apply(food,amounts);
            try{sendFood(con,p,food);}
            catch(Exception e){apply(food,before);return FoodRecoveryController.Cleanup.UNCERTAIN;}
            // The server already settled this meal. Release now at the cleaned remaining values, so the
            // eventual terminal transition cannot overwrite them with old whole-food effects.
            if(boosted!=null){
                FoodRecoveryEffects.end(boosted,new float[]{amounts.thirst(),amounts.endurance(),amounts.fatigue(),
                        amounts.stress(),amounts.boredom(),amounts.unhappy()});boosted=null;
            }
            return FoodRecoveryController.Cleanup.REQUESTED;
        }
        private void sendFood(UdpConnection con,IsoPlayer p,Food food)throws Exception{
            if(GameClient.connection!=con||IsoPlayer.getInstance()!=p||!con.isReady()||!body(food,p))throw new IllegalStateException("food recovery ownership changed");
            // Address the container that actually holds the item: for bagged food ContainerID resolves the
            // bag through its containing item id, whereas the root inventory would fail to find the id.
            ItemContainer holder=food.getContainer();
            if(holder==null)throw new IllegalStateException("food recovery container lost");
            INetworkPacket packet=con.getPacket(PacketTypes.PacketType.ItemStats);
            modifiers(food).capture(fields(food),()->packet.setData(holder,food));
            ByteBufferWriter w=con.startPacket();
            try{PacketTypes.PacketType.ItemStats.doPacket(w);FoodRecoveryPacketWriter.write(packet,w);PacketTypes.PacketType.ItemStats.send(con);}catch(Exception e){con.cancelPacket();throw e;}
        }
    }
}