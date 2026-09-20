--*********************************************************
--* E2 鼠标拾取 (2026-09-14 八十八, 见 analysis/DLL分析/E-信息层-设计方案(E1-E3已实施-E4不做).md §E2)
--*
--* 机制 (取证结论, 全部落在原版代码上):
--*   ① 原版每帧已经算好"鼠标下的物件" —— zombie/ui/UIManager.java:567-571 调
--*      IsoObjectPicker.Instance.ContextPick 并把结果写进 UIManager.setLastPicked
--*      (1225-1232); getLastPicked() 是 public static 且 UIManager 已暴露给 Lua
--*      → Lua 直接可读, 零成本。**不需要任何渲染补丁** (PienZ 那 5 处字段改写是
--*      原生侧拿不到 Java 内部实例所致, 我们用现成 API)。
--*   ② 但该值只在"鼠标不在任何 UI 元素上 + 拾取非空 + tooltip 非空"时才写入
--*      (UIManager.java:1219/1232 否则写 null) —— 我方面板打开时鼠标必在面板上,
--*      所以面板内取物件走 Java 兜底 pickObjectAt (把 ClickObject 解包成 IsoObject;
--*      ClickObject 不在 Lua 暴露白名单, 实例字段读不到)。
--*   ③ 坐标契约: ContextPick 内部会**再乘一次 zoom** (IsoObjectPicker.java:147-148;
--*      FBORenderObjectPicker.java:58-60) → 必须传 raw 坐标 (getMouseX/getMouseY),
--*      不要传 getMouseXScaled。
--*   ④ 性能: 一次 ContextPick = z0..31 × 14 格 + 3×3 尸体邻域 + 排序
--*      (FBORenderObjectPicker.java:234-261) → 只按 HOVER_MS 节流调用, 绝不在 render() 每帧调。
--*   ⑤ 留痕: 纯本地 (读渲染结果 + 写本地高亮位) → 零网络、零服务端可见状态。
--*********************************************************

EtherPick = {};

EtherPick.HOVER_MS = 250;    -- 拾取节流 (原版结果是免费的, 只有它为空时才走 Java 兜底)
EtherPick.obj      = nil;    -- 当前鼠标下物件 (缓存)
EtherPick.stamp    = nil;    -- 上次拾取时刻
EtherPick.tickMs   = nil;    -- OnTick 节流
EtherPick.info     = "";     -- 最近一次拾取信息 (中性串)
EtherPick.lastLog  = nil;    -- 上次打印过的信息 (只在变化时打印, 避免刷屏)
EtherPick.marked   = nil;    -- 被我们点亮、且点亮前**未亮**的对象 (回滚只碰自己改的键)
EtherPick.col      = nil;    -- 原高亮色 (若有)

togglePickHover = false;     -- 「高亮鼠标下的物体」
togglePickInfo  = false;     -- 「显示拾取信息」(控制台按变化打印)

function isPickHover() return togglePickHover == true; end
function isPickInfo()  return togglePickInfo  == true; end

function setPickHover(on)
    togglePickHover = on and true or false;
    if not togglePickHover then
        EtherPick.clearHighlight();
    end
    return togglePickHover;
end

function setPickInfo(on)
    togglePickInfo = on and true or false;
    if not togglePickInfo then
        EtherPick.lastLog = nil;
    end
    return togglePickInfo;
end

--*********************************************************
--* 鼠标下物件: 先用原版每帧结果 (免费), 为空再按 HOVER_MS 节流走 Java 兜底
--*********************************************************
function EtherPick.hover()
    local obj = UIManager.getLastPicked();
    if obj ~= nil then
        return obj;
    end
    local nowMs = getTimestampMs();
    if EtherPick.stamp ~= nil and nowMs - EtherPick.stamp < EtherPick.HOVER_MS then
        return EtherPick.obj;
    end
    EtherPick.stamp = nowMs;
    EtherPick.obj = pickObjectAt(getMouseX(), getMouseY());
    return EtherPick.obj;
end

--*********************************************************
--* 鼠标下格子 (与 EtherEditWorldObjects.pickSquare 同款换算: XToIso(x*zoom, y*zoom, z))
--*********************************************************
function EtherPick.hoverSquare()
    local player = getSpecificPlayer(0);
    if player == nil then return nil; end
    local sq = player:getSquare();
    local z = sq and sq:getZ() or 0;
    local zoom = getCore():getZoom(0);
    local wx = IsoUtils.XToIso(getMouseX() * zoom, getMouseY() * zoom, z);
    local wy = IsoUtils.YToIso(getMouseX() * zoom, getMouseY() * zoom, z);
    return getCell():getGridSquare(wx, wy, z), wx, wy, z;
end

--*********************************************************
--* 高亮 (H 工程纪律 §4: 先存后改, 回滚只写回自己改过的键)
--* 原版既有用法参考: ISInventoryPage.lua:472-474 / ISBuildWindow.lua:358-360 ——
--* 目标若本来就被原版点亮, 我们绝不动它。
--*********************************************************
function EtherPick.clearHighlight()
    local obj = EtherPick.marked;
    if obj ~= nil and EtherPick.col == nil then
        pcall(function() obj:setOutlineHighlight(false) end);
    end
    EtherPick.marked = nil;
    EtherPick.col = nil;
end

function EtherPick.applyHighlight(obj)
    if EtherPick.marked == obj then
        return;   -- 同一个目标, 什么都不用做
    end
    EtherPick.clearHighlight();
    if obj == nil then
        return;
    end
    local already = false;
    local ok = pcall(function() already = obj:isOutlineHighlight() end);
    if not ok then
        return;
    end
    if already then
        EtherPick.col = true;      -- 本来亮着 → 只记录, 不接管 (回滚时不动)
        EtherPick.marked = obj;
        return;
    end
    pcall(function() obj:setOutlineHighlight(true) end);
    EtherPick.marked = obj;
    EtherPick.col = nil;
end

--*********************************************************
--* 拾取信息 (中性串: name|type|sprite|x|y|z|score; 无命中为空串)
--*********************************************************
function EtherPick.describeCurrent()
    return pickObjectInfoAt(getMouseX(), getMouseY()) or "";
end

function EtherPick.infoToText(info)
    if info == nil or info == "" then
        return getTranslate("UI_Pick_None");
    end
    local name, kind, sprite, x, y, z = string.match(info, "^([^|]*)|([^|]*)|([^|]*)|([^|]*)|([^|]*)|([^|]*)");
    if name == nil then
        return info;
    end
    return tr("UI_Pick_Format", { name = name, kind = kind, x = x, y = y, z = z });
end

--*********************************************************
--* 节流驱动 (两个开关都关时几乎零开销)
--*********************************************************
local function pickTick()
    if not (togglePickHover or togglePickInfo) then
        if EtherPick.marked ~= nil then
            EtherPick.clearHighlight();
        end
        return;
    end
    local nowMs = getTimestampMs();
    if EtherPick.tickMs ~= nil and nowMs - EtherPick.tickMs < EtherPick.HOVER_MS then
        return;
    end
    EtherPick.tickMs = nowMs;

    if togglePickHover then
        EtherPick.applyHighlight(EtherPick.hover());
    end
    if togglePickInfo then
        local info = EtherPick.describeCurrent();
        EtherPick.info = info;
        if info ~= EtherPick.lastLog then
            EtherPick.lastLog = info;
            print("[Pick] " .. EtherPick.infoToText(info));
        end
    end
end

Events.OnTick.Add(pickTick);
