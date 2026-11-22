package modcore;

import modcore.core.FoodRecoveryController;
import org.junit.jupiter.api.Test;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FoodRecoveryLuaTest {
    @Test void preparationRunsBeforeTheOriginalBeginAndOrdinaryStartIsPreserved() throws Exception {
        Fixture f=new Fixture();assertTrue(f.flow.arm());
        Object[] values=f.lua.execute("action=makeAction(item,player);local a,b=action:begin();return phaseAtBegin,getFoodRecoveryState(),beginCount,startCount,a,b");
        assertArrayEquals(new Object[]{"PREPARED","EATING",1.0,1.0,"ordinary-begin",7.0},values);
        assertEquals(1,f.port.sent.size());
    }
    @Test void eatingWithoutPreparationKeepsTheOriginalActionAndSendsNothing() throws Exception {
        Fixture f=new Fixture();Object[] values=f.lua.execute("makeAction(item,player):begin();return getFoodRecoveryState(),beginCount,startCount");
        assertArrayEquals(new Object[]{"OFF",1.0,1.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void anotherCharactersFoodActionCannotConsumeOurPreparation() throws Exception {
        Fixture f=new Fixture();f.flow.arm();Object[] values=f.lua.execute("makeAction(item,other):begin();return getFoodRecoveryState(),beginCount");
        assertArrayEquals(new Object[]{"ARMED",1.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void performStartsVerificationAndTwoServerSamplesEndItAutomatically() throws Exception {
        Fixture f=new Fixture();f.flow.arm();f.lua.execute("action=makeAction(item,player);action:begin();action:perform()");
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.flow.phase());
        f.port.exists=false;f.port.time++;f.flow.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.VERIFYING,f.flow.phase());
        f.port.time+=500;f.flow.observeServerStats(f.port.context(),1f,0f);
        assertEquals(FoodRecoveryController.Phase.OFF,f.flow.phase());assertEquals(FoodRecoveryController.Result.SUCCESS,f.flow.result());
    }
    @Test void originalStopStillRunsAndCancellationDoesNotPretendToBeSuccess() throws Exception {
        Fixture f=new Fixture();f.flow.arm();Object[] values=f.lua.execute("action=makeAction(item,player);action:begin();action:stop();return getFoodRecoveryState(),getFoodRecoveryResult(),stopCount");
        assertArrayEquals(new Object[]{"OFF","CANCELLED",1.0},values);assertEquals(2,f.port.sent.size());
    }
    @Test void originalBeginErrorsAreRethrownAfterEndingThePreparedTransaction() throws Exception {
        Fixture f=new Fixture();f.flow.arm();Object[] values=f.lua.execute("action=makeAction(item,player);action.fail=true;local ok=pcall(function()action:begin()end);return ok,getFoodRecoveryState(),getFoodRecoveryResult()");
        assertArrayEquals(new Object[]{false,"OFF","HOOK_ERROR"},values);assertEquals(2,f.port.sent.size());
    }
    @Test void uninstallRestoresMissingOwnSlotsAndDoesNotOverwriteAnotherMod() throws Exception {
        Fixture f=new Fixture();f.flow.arm();Object[] values=f.lua.execute("action=makeAction(item,player);action:begin();custom=function()return'other-mod'end;ISEatFoodAction.stop=custom;FoodRecovery.uninstall();return rawget(ISEatFoodAction,'begin')==nil,ISEatFoodAction.stop==custom,Events.OnTick.handler==nil,getFoodRecoveryState()");
        assertArrayEquals(new Object[]{true,true,true,"OFF"},values);assertEquals(2,f.port.sent.size());
    }
    @Test void reloadAndTickTimeoutDoNotLeaveDuplicatedActionWrappersOrActiveFlags() throws Exception {
        Fixture f=new Fixture();f.lua.resource("modcore/lua/components/ui/FoodRecovery.lua");
        f.flow.observeServerStats(f.port.context(),.2f,.8f);f.flow.arm();f.port.time+=90000;
        Object[] values=f.lua.execute("Events.OnTick.handler();return getFoodRecoveryState(),getFoodRecoveryResult(),Events.OnTick.count");
        assertArrayEquals(new Object[]{"OFF","TIMED_OUT",1.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void pressingPreparationAgainCannotLoseTheTrackedEatingAction() throws Exception {
        Fixture f=new Fixture();f.flow.arm();
        Object[] values=f.lua.execute("action=makeAction(item,player);action:begin();FoodRecovery.arm();action:perform();return getFoodRecoveryState(),performCount");
        assertArrayEquals(new Object[]{"VERIFYING",1.0},values);assertEquals(1,f.port.sent.size());
    }
    @Test void aWrapperRetainedByAnotherModBecomesInactiveAfterUninstall() throws Exception {
        Fixture f=new Fixture();
        f.lua.execute("oldWrappedBegin=ISEatFoodAction.begin;FoodRecovery.uninstall()");
        f.lua.resource("modcore/lua/components/ui/FoodRecovery.lua");
        f.flow.observeServerStats(f.port.context(),.2f,.8f);assertTrue(f.flow.arm());
        Object[] values=f.lua.execute("oldWrappedBegin(makeAction(item,player));return getFoodRecoveryState(),beginCount");
        assertArrayEquals(new Object[]{"ARMED",1.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void oneClickQueuesOneNormalSmallServingWithoutRequiringSpecificFood() throws Exception {
        Fixture f=new Fixture();Object[] values=f.lua.execute("FoodRecovery.arm();return getFoodRecoveryState(),queueCount,transferCount,startCount,lastPercentage");
        assertArrayEquals(new Object[]{"EATING",1.0,1.0,1.0,.25},values);assertEquals(1,f.port.sent.size());
        f.lua.execute("for i=1,50 do FoodRecovery.statusText();Events.OnTick.handler() end");
        assertEquals(1.0,f.lua.execute("return queueCount")[0]);
    }
    @Test void inventorySelectionIsDeferredAndStatusRefreshNeverRescans() throws Exception {
        Fixture f=new Fixture();f.port.pendingSteps=2;f.lua.execute("FoodRecovery.arm()");int count=f.port.steps;
        f.lua.execute("for i=1,50 do FoodRecovery.statusText();getFoodRecoveryState();getFoodRecoveryAvailability() end");
        assertEquals(count,f.port.steps);assertEquals(0.0,f.lua.execute("return queueCount")[0]);
        f.lua.execute("Events.OnTick.handler();Events.OnTick.handler()");assertEquals(1.0,f.lua.execute("return queueCount")[0]);
    }
    @Test void noEligibleFoodReportsFailureWithoutQueueingOrChangingFood() throws Exception {
        Fixture f=new Fixture();f.port.noFood=true;Object[] values=f.lua.execute("FoodRecovery.arm();return getFoodRecoveryState(),getFoodRecoveryResult(),queueCount");
        assertArrayEquals(new Object[]{"OFF","NO_FOOD",0.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void cancellingBeforeEatingRemovesOnlyOurQueuedFoodAction() throws Exception {
        Fixture f=new Fixture();deferFoodQueue(f);f.lua.execute("FoodRecovery.arm();table.insert(deferredQueue,unrelated);FoodRecovery.cancel()");
        Object[] values=f.lua.execute("return #deferredQueue,deferredQueue[1]==unrelated,getFoodRecoveryState(),beginCount");
        assertArrayEquals(new Object[]{1.0,true,"OFF",0.0},values);assertTrue(f.port.sent.isEmpty());
    }
    @Test void naturalRecoveryBeforeQueuedFoodStartsDoesNotConsumeFoodUnnecessarily() throws Exception {
        Fixture f=new Fixture();deferFoodQueue(f);f.lua.execute("FoodRecovery.arm();table.insert(deferredQueue,unrelated)");
        f.flow.observeServerStats(f.port.context(),1f,0f);f.lua.execute("Events.OnTick.handler()");
        Object[] values=f.lua.execute("return #deferredQueue,deferredQueue[1]==unrelated,getFoodRecoveryResult(),beginCount");
        assertArrayEquals(new Object[]{1.0,true,"NO_NEED",0.0},values);assertTrue(f.port.sent.isEmpty());
    }
    private static void deferFoodQueue(Fixture f) throws Exception {
        f.lua.execute("""
            deferredQueue={};unrelated={}
            ISTimedActionQueue.add=function(action)queueCount=queueCount+1;table.insert(deferredQueue,action);return {} end
            ISTimedActionQueue.getTimedActionQueue=function()
                return {removeFromQueue=function(_,action)
                    for i,a in ipairs(deferredQueue) do if a==action then table.remove(deferredQueue,i);return end end
                end}
            end
            """);
    }
    @Test void recoveryModuleBuildsAgainstTheRealPanelApiWithoutContentWidthHelper() throws Exception {
        Fixture f=new Fixture();
        Object[] values=f.lua.execute("""
            local built=0
            EtherTheme={ctrlGap=4,ctrlH=24,fontHgtHint=12,wrapHint=function(t) return {t} end}
            UIButton={}
            function UIButton.measureGroupWidth(list) local w=0;for _,t in ipairs(list) do if #t*6>w then w=#t*6 end end;return w end
            function UIButton:new(x,y,w,h,title,cb) local o={x=x,y=y,width=w,height=h,title=title,onClick=cb};setmetatable(o,{__index=UIButton});return o end
            function UIButton:setEnable(v) self.enabled=v end
            local hintClass={}
            function hintClass:new(x,y,w,text) local o={x=x,y=y,width=w,lines={text}};setmetatable(o,{__index=hintClass});return o end
            function hintClass:initialise(self) end
            function hintClass:instantiate(self) end
            local panel={width=420}
            function panel:_rowContentW() return 404 end
            function panel:_anchor(w) self.anchored=(self.anchored or 0)+1 end
            function panel:addChild(c) self.children=(self.children or 0)+1 end
            function panel:addWidget(w) self.widgets=(self.widgets or 0)+1 end
            function panel:addModule(key,h,builder) built=built+1;builder(8,40,404);return 1 end
            FoodRecovery.addModule(panel,hintClass)
            return built,panel.widgets or 0,panel.children or 0,getFoodRecoveryState()
            """);
        assertArrayEquals(new Object[]{1.0,2.0,3.0,"OFF"},values);
    }
    @Test void fullyRestedCharacterCanStillSeeWhyRecoveryIsNotNeeded() throws Exception {
        Fixture f=new Fixture();
        Object[] values=f.lua.execute("""
            EtherTheme={ctrlGap=4,ctrlH=24,fontHgtHint=12,wrapHint=function(t) return {t} end}
            UIButton={}
            function UIButton.measureGroupWidth(list) local w=0;for _,t in ipairs(list) do if #t*6>w then w=#t*6 end end;return w end
            function UIButton:new(x,y,w,h,title,cb) local o={x=x,y=y,width=w,height=h,title=title,onClick=cb,isEnable=true};setmetatable(o,{__index=UIButton});return o end
            function UIButton:setEnable(v) self.isEnable=v end
            local hintClass={}
            function hintClass:new(x,y,w,text) local o={lines={text}};setmetatable(o,{__index=hintClass});return o end
            function hintClass:initialise(self) end
            function hintClass:instantiate(self) end
            local panel={width=420}
            function panel:_rowContentW() return 404 end
            function panel:_anchor(w) end
            function panel:addChild(c) end
            function panel:addWidget(w) end
            function panel:addModule(key,h,builder) builder(8,40,404);return 1 end
            FoodRecovery.addModule(panel,hintClass)
            return panel.foodRecoveryUi.arm.isEnable
            """);
        assertArrayEquals(new Object[]{true},values);
    }
    static final class Fixture {
        final LuaTestRuntime lua=new LuaTestRuntime();final Port port=new Port();final FoodRecoveryController flow=new FoodRecoveryController(port);
        Fixture() throws Exception {
            lua.execute("""
                require=function()end
                function event()
                    return {count=0,Add=function(self)end}
                end
                local function ev()
                    local e={count=0}
                    e.Add=function(fn)e.handler=fn;e.count=e.count+1 end
                    e.Remove=function(fn)if e.handler==fn then e.handler=nil;e.count=e.count-1 end end
                    return e
                end
                Events={OnTick=ev(),OnMainMenuEnter=ev(),OnDisconnect=ev(),OnPlayerDeath=ev()}
                player={};other={};item={};beginCount=0;startCount=0;performCount=0;stopCount=0
                queueCount=0;transferCount=0;lastPercentage=0
                ISTimedActionQueue={add=function(action)queueCount=queueCount+1;action:begin();return {} end}
                ISInventoryPaneContextMenu={transferIfNeeded=function()transferCount=transferCount+1 end}

                getPlayer=function()return player end
                ISBaseTimedAction={}
                function ISBaseTimedAction:begin()
                    beginCount=beginCount+1;phaseAtBegin=getFoodRecoveryState()
                    if self.fail then error('ordinary begin error') end
                    self:start();return 'ordinary-begin',7
                end
                ISEatFoodAction={Type='ISEatFoodAction'}
                function ISEatFoodAction:new(p,i,percentage)
                    local action=makeAction(i,p);action.percentage=percentage;lastPercentage=percentage;return action
                end

                setmetatable(ISEatFoodAction,{__index=ISBaseTimedAction})
                function ISEatFoodAction:start()startCount=startCount+1 end
                function ISEatFoodAction:perform()performCount=performCount+1 end
                function ISEatFoodAction:stop()stopCount=stopCount+1 end
                function makeAction(i,p)
                    local o={item=i,character=p,percentage=1};setmetatable(o,{__index=ISEatFoodAction});return o
                end
                """);
            port.player=lua.environment.rawget("player");port.item=lua.environment.rawget("item");
            bind("armFoodRecovery",(frame,n)->frame.push(flow.arm()));
            bind("prepareFoodRecovery",(frame,n)->frame.push(flow.prepare(frame.get(0),((Number)frame.get(1)).floatValue())));
            bind("foodRecoveryStarted",(frame,n)->{flow.actionStarted();return 0;});
            bind("foodRecoveryCompleted",(frame,n)->{flow.actionCompleted();return 0;});
            bind("foodRecoveryStopped",(frame,n)->{flow.actionStopped();return 0;});
            bind("foodRecoveryFailed",(frame,n)->{flow.hookFailed();return 0;});
            bind("cancelFoodRecovery",(frame,n)->{flow.cancel();return 0;});
            bind("foodRecoveryContextLost",(frame,n)->{flow.contextLost();return 0;});
            bind("setFoodRecoveryHooksReady",(frame,n)->{boolean on=Boolean.TRUE.equals(frame.get(0));if(!on)flow.cancel();port.supported=on;if(!on)flow.contextLost();return 0;});
            bind("updateFoodRecovery",(frame,n)->{flow.tick();return 0;});
            bind("getFoodRecoveryState",(frame,n)->frame.push(flow.phase().name()));
            bind("getFoodRecoveryResult",(frame,n)->frame.push(flow.result().name()));
            bind("getFoodRecoveryCleanup",(frame,n)->frame.push(flow.cleanup().name()));
            bind("getFoodRecoveryAvailability",(frame,n)->frame.push(flow.availability().name()));
            bind("getFoodRecoveryRemainingSeconds",(frame,n)->frame.push((double)flow.remainingSeconds()));
            bind("getFoodRecoveryCandidateItem",(frame,n)->frame.push(flow.candidate()));
            bind("getFoodRecoveryRequestedPercentage",(frame,n)->frame.push((double)flow.requestedPercentage()));
            bind("getFoodRecoveryCheckedItems",(frame,n)->frame.push((double)flow.checked()));
            bind("getFoodRecoveryFoodName",(frame,n)->frame.push("normal food"));
            lua.resource("modcore/lua/components/ui/FoodRecovery.lua");flow.observeServerStats(port.context(),.2f,.8f);
        }
        void bind(String name,JavaFunction function){lua.environment.rawset(name,function);}
    }
    static final class Port implements FoodRecoveryController.Port {
        long time=100;Object connection=new Object(),player,item;boolean supported,exists=true,noFood;int pendingSteps,steps;
        List<float[]> sent=new ArrayList<>();
        public long now(){return time;}
        public FoodRecoveryController.Context context(){return new FoodRecoveryController.Context(connection,player,supported,false);}
        public FoodRecoveryController.Food inspect(Object x){return x==item?new FoodRecoveryController.Food(item,123,-.01f,-.01f,0f,0f,0f,0f,0f,0f,1,true,true):null;}
        public FoodRecoveryController.Selection select(int budget){steps++;if(pendingSteps-->0)return new FoodRecoveryController.Selection(null,128,true);return new FoodRecoveryController.Selection(noFood?null:item,1,false);}
        public boolean exists(FoodRecoveryController.Food key){return exists;}
        public boolean submit(FoodRecoveryController.Food key,float[] v){sent.add(v.clone());return true;}
    }
}
