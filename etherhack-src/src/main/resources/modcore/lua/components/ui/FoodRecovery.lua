require "TimedActions/ISEatFoodAction"
require "TimedActions/ISTimedActionQueue"
require "ISUI/ISInventoryPaneContextMenu"

if FoodRecovery and type(FoodRecovery.uninstall) == "function" then FoodRecovery.uninstall(); end
FoodRecovery = {};
local generation = { alive = true };
local bindings, events = {}, {};
local pending = nil;
local required = { "armFoodRecovery", "cancelFoodRecovery", "prepareFoodRecovery", "foodRecoveryStarted",
    "foodRecoveryCompleted", "foodRecoveryStopped", "foodRecoveryFailed", "foodRecoveryContextLost", "setFoodRecoveryHooksReady",
    "updateFoodRecovery", "getFoodRecoveryState", "getFoodRecoveryResult", "getFoodRecoveryCleanup",
    "getFoodRecoveryAvailability", "getFoodRecoveryRemainingSeconds", "getFoodRecoveryCandidateItem",
    "getFoodRecoveryRequestedPercentage", "getFoodRecoveryCheckedItems", "getFoodRecoveryFoodName" };
local phases = { OFF="UI_FoodRecovery_Off", SELECTING="UI_FoodRecovery_Selecting", ARMED="UI_FoodRecovery_Armed", PREPARED="UI_FoodRecovery_Prepared",
    EATING="UI_FoodRecovery_Eating", VERIFYING="UI_FoodRecovery_Verifying", CLEANING="UI_FoodRecovery_Cleaning" };
local results = { SUCCESS="UI_FoodRecovery_Success", CANCELLED="UI_FoodRecovery_Cancelled",
    TIMED_OUT="UI_FoodRecovery_TimedOut", UNSUPPORTED="UI_FoodRecovery_Unsupported", CONFLICT="UI_FoodRecovery_Conflict",
    NO_SERVER_STATS="UI_FoodRecovery_NoStats", NO_NEED="UI_FoodRecovery_NoNeed", NO_FOOD="UI_FoodRecovery_NoFood", UNSAFE_FOOD="UI_FoodRecovery_Unsafe",
    SEND_FAILED="UI_FoodRecovery_SendFailed", CONTEXT_LOST="UI_FoodRecovery_ContextLost",
    HOOK_ERROR="UI_FoodRecovery_HookError", BUSY="UI_FoodRecovery_Busy" };
local function tr(key)
    if type(getTranslate)=="function" then return getTranslate(key); end
    return key;
end
local function pack(...) return { n=select("#", ...), ... }; end
local function call(name, ...)
    if type(_G[name])=="function" then return pcall(_G[name], ...); end
    return false;
end
local function isInGame()
    return type(getPlayer)=="function" and getPlayer()~=nil;
end
local function state()
    if type(getFoodRecoveryState)=="function" then return getFoodRecoveryState(); end
    return "OFF";
end
local queued = false;
local queuedAction = nil;
local function queueSelected()
    if queued or state()~="ARMED" or not FoodRecovery.installed then
        return;
    end
    local item=getFoodRecoveryCandidateItem();if not item then return; end
    local percentage=getFoodRecoveryRequestedPercentage();
    if type(percentage)~="number" or percentage~=percentage or percentage<=0 or percentage>1 then call("foodRecoveryFailed");return; end
    queued=true;
    local ok=pcall(function()
        local player=getPlayer();
        ISInventoryPaneContextMenu.transferIfNeeded(player,item,true);
        queuedAction=ISEatFoodAction:new(player,item,percentage);
        local added=ISTimedActionQueue.add(queuedAction);
        if not added then call("foodRecoveryFailed"); end
    end);
    if not ok then call("foodRecoveryFailed"); end
end
local function cancelQueued()
    if not queuedAction or queuedAction==pending or queuedAction.action then return; end
    if ISTimedActionQueue and type(ISTimedActionQueue.getTimedActionQueue)=="function" then
        local ok, queue=pcall(ISTimedActionQueue.getTimedActionQueue,queuedAction.character);
        if ok and queue and type(queue.removeFromQueue)=="function" then pcall(queue.removeFromQueue,queue,queuedAction); end
    end
end
local function invoke(original, action, ...)
    local values=pack(pcall(original, action, ...));
    if not values[1] then
        if pending==action then pending=nil;call("foodRecoveryFailed"); end
        error(values[2],0);
    end
    return unpack(values,2,values.n);
end
local function bind(owner,key,wrapper)
    local original=owner[key];
    bindings[#bindings+1]={owner=owner,key=key,raw=rawget(owner,key),wrapper=wrapper};
    owner[key]=wrapper;
end
local function event(name,handler)
    local e=Events and Events[name];
    if e and type(e.Add)=="function" then e.Add(handler);events[#events+1]={event=e,handler=handler}; end
end
function FoodRecovery.uninstall()
    generation.alive=false;cancelQueued();pending=nil;queued=false;queuedAction=nil;
    call("cancelFoodRecovery");call("setFoodRecoveryHooksReady",false);
    for _, b in ipairs(bindings) do if rawget(b.owner,b.key)==b.wrapper then b.owner[b.key]=b.raw; end end
    for _, e in ipairs(events) do if type(e.event.Remove)=="function" then e.event.Remove(e.handler); end end
    bindings={};events={};FoodRecovery.installed=false;
end
function FoodRecovery.install()
    if FoodRecovery.installed then return true; end
    for _, name in ipairs(required) do
        if type(_G[name])~="function" then return false; end
    end
    if type(ISEatFoodAction)~="table" or type(ISEatFoodAction.new)~="function"
        or not ISTimedActionQueue or type(ISTimedActionQueue.add)~="function"
        or not ISInventoryPaneContextMenu or type(ISInventoryPaneContextMenu.transferIfNeeded)~="function" then return false; end
    for _, name in ipairs({"begin","start","perform","stop"}) do
        if type(ISEatFoodAction[name])~="function" then return false; end
    end
    local oldBegin,oldStart,oldPerform,oldStop=ISEatFoodAction.begin,ISEatFoodAction.start,ISEatFoodAction.perform,ISEatFoodAction.stop;
    bind(ISEatFoodAction,"begin",function(action,...)
        if not generation.alive then return oldBegin(action,...); end
        if state()=="ARMED" and action.Type=="ISEatFoodAction" and type(getPlayer)=="function" and action.character==getPlayer() then
            local ok, prepared=call("prepareFoodRecovery",action.item,action.percentage);
            if ok and prepared==true then pending=action;
            elseif not ok then call("foodRecoveryFailed"); end
        end
        return invoke(oldBegin,action,...);
    end);
    bind(ISEatFoodAction,"start",function(action,...)
        if not generation.alive then return oldStart(action,...); end
        if pending==action and state()=="PREPARED" then call("foodRecoveryStarted"); end
        if state()=="OFF" then pending=nil; end
        return invoke(oldStart,action,...);
    end);
    bind(ISEatFoodAction,"perform",function(action,...)
        if not generation.alive then return oldPerform(action,...); end
        local tracked=pending==action;
        if tracked then pending=nil; end
        local values=pack(pcall(oldPerform,action,...));
        if not values[1] then if tracked then call("foodRecoveryFailed"); end;error(values[2],0); end
        if tracked then call("foodRecoveryCompleted"); end
        return unpack(values,2,values.n);
    end);
    bind(ISEatFoodAction,"stop",function(action,...)
        if not generation.alive then return oldStop(action,...); end
        local tracked=pending==action;
        local values=pack(pcall(oldStop,action,...));
        if tracked then pending=nil; end
        if not values[1] then if tracked then call("foodRecoveryFailed"); end;error(values[2],0); end
        if tracked then call("foodRecoveryStopped"); end
        return unpack(values,2,values.n);
    end);
    event("OnTick",function()
        if not generation.alive then return; end
        call("updateFoodRecovery");queueSelected();if state()=="OFF" then cancelQueued();pending=nil;queued=false;queuedAction=nil; end
    end);
    local lost=function() cancelQueued();pending=nil;queued=false;queuedAction=nil;call("foodRecoveryContextLost"); end;
    event("OnMainMenuEnter",lost);event("OnDisconnect",lost);
    event("OnPlayerDeath",function(player) if type(getPlayer)=="function" and player==getPlayer() then lost(); end end);
    FoodRecovery.installed=true;call("setFoodRecoveryHooksReady",true);
    return true;
end
function FoodRecovery.arm()
    local ok, accepted=call("armFoodRecovery");
    if ok and accepted==true then pending=nil;queued=false;queuedAction=nil;queueSelected(); end
    return ok, accepted;
end
function FoodRecovery.cancel()
    cancelQueued();
    if state()~="EATING" and state()~="CLEANING" then pending=nil; end
    return call("cancelFoodRecovery");
end
function FoodRecovery.statusText()
    if type(getFoodRecoveryResult)~="function" then return tr("UI_FoodRecovery_NoApi"); end
    local phase=state();local text;
    if phase~="OFF" then
        local seconds=type(getFoodRecoveryRemainingSeconds)=="function" and getFoodRecoveryRemainingSeconds() or 0;
        text=tr(phases[phase] or phases.OFF).." ("..tostring(math.floor(seconds)).."s)";
        if phase=="SELECTING" then text=text.." ["..tostring(getFoodRecoveryCheckedItems()).."]"; end
    else
        local result=getFoodRecoveryResult();
        if result=="NONE" and type(getFoodRecoveryAvailability)=="function" then result=getFoodRecoveryAvailability(); end
        text=tr(results[result] or phases.OFF);
    end
    local cleanup=type(getFoodRecoveryCleanup)=="function" and getFoodRecoveryCleanup() or "NONE";
    if cleanup=="WAITING" then text=text.."\n"..tr("UI_FoodRecovery_CleanupWaiting");
    elseif cleanup=="UNCERTAIN" then text=text.."\n"..tr("UI_FoodRecovery_CleanupUncertain");
    elseif cleanup=="REQUESTED" then text=text.."\n"..tr("UI_FoodRecovery_CleanupRequested"); end
    return text;
end
local function controls(width)
    local gap=EtherTheme.ctrlGap;local available=math.max(1,width);
    local bw=math.min(available,UIButton.measureGroupWidth({tr("UI_FoodRecovery_Arm"),tr("UI_FoodRecovery_Cancel")}));
    local columns=math.max(1,math.min(2,math.floor((available+gap)/(bw+gap))));
    return columns,bw,math.ceil(2/columns)*EtherTheme.ctrlH+(math.ceil(2/columns)-1)*gap;
end
function FoodRecovery.refreshUi(panel)
    local ui=panel.foodRecoveryUi;if not ui then return; end
    local active=state()~="OFF";
    -- Keep the button clickable in every in-game state: preferring held food, this is also the place
    -- that reports "no suitable food" / "already recovered" instead of silently greying out.
    ui.arm:setEnable(FoodRecovery.installed==true and not active and isInGame());
    ui.cancel:setEnable(FoodRecovery.installed==true and active and state()~="CLEANING");
    local name=type(getFoodRecoveryFoodName)=="function" and getFoodRecoveryFoodName() or "";
    if ui.food then ui.food.lines={tr("UI_FoodRecovery_Selected")..(name~="" and name or "-")}; end
    local text=FoodRecovery.statusText();
    if text~=ui.lastText then ui.lastText=text;ui.status.lines=EtherTheme.wrapHint(text,ui.width-8); end
end
function FoodRecovery.addModule(panel, hintClass)
    if not hintClass then return; end
    local width=panel:_rowContentW()-16;local _,_,buttonH=controls(width);
    local hint=tr("UI_FoodRecovery_Hint");
    local hintH=#EtherTheme.wrapHint(hint,width-8)*(EtherTheme.fontHgtHint+2)+4;
    local maxLines=1;
    for _,key in pairs(phases) do maxLines=math.max(maxLines,#EtherTheme.wrapHint(tr(key).." (180s) [9999999]",width-8)); end
    for _,key in pairs(results) do
        for _,suffix in ipairs({"", "\n"..tr("UI_FoodRecovery_CleanupUncertain"), "\n"..tr("UI_FoodRecovery_CleanupRequested"), "\n"..tr("UI_FoodRecovery_CleanupWaiting")}) do
            maxLines=math.max(maxLines,#EtherTheme.wrapHint(tr(key)..suffix,width-8));
        end
    end
    maxLines=math.max(maxLines,#EtherTheme.wrapHint(tr("UI_FoodRecovery_NoApi"),width-8));
    local statusH=maxLines*(EtherTheme.fontHgtHint+2)+4;
    local foodH=EtherTheme.fontHgtHint+2;
    panel:addModule(tr("UI_FoodRecovery_Group"),hintH+6+foodH+6+buttonH+6+statusH,function(x,y,w)
        local info=hintClass:new(x,y,w,hint);info:initialise();info:instantiate();panel:_anchor(info);panel:addChild(info);
        local foodLine=hintClass:new(x,y+hintH+6,w,"");foodLine.height=foodH;foodLine:initialise();foodLine:instantiate();panel:_anchor(foodLine);panel:addChild(foodLine);
        local cy=y+hintH+6+foodH+6;local columns,bw=controls(w);local buttons={};
        for i,spec in ipairs({{key="UI_FoodRecovery_Arm",run=FoodRecovery.arm},{key="UI_FoodRecovery_Cancel",run=FoodRecovery.cancel}}) do
            local run=spec.run;
            local b=UIButton:new(x+((i-1)%columns)*(bw+EtherTheme.ctrlGap),cy+math.floor((i-1)/columns)*(EtherTheme.ctrlH+EtherTheme.ctrlGap),
                bw,EtherTheme.ctrlH,tr(spec.key),function() run();FoodRecovery.refreshUi(panel);end,bw);
            panel:addWidget(b);buttons[i]=b;
        end
        local status=hintClass:new(x,cy+buttonH+6,w,"");status.height=statusH;status:initialise();status:instantiate();panel:_anchor(status);panel:addChild(status);
        panel.foodRecoveryUi={arm=buttons[1],cancel=buttons[2],status=status,food=foodLine,width=w};FoodRecovery.refreshUi(panel);
    end);
end
if not FoodRecovery.install() then
    -- Missing globals would otherwise leave the panel looking functional but inert.
    print("[FoodRecovery] install failed: a required API or action is missing");
    if type(getFoodRecoveryState)~="function" then print("[FoodRecovery] Java API not exposed") end
end