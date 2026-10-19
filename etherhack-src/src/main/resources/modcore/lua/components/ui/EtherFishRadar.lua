--*********************************************************
--* 「趣味-钓鱼」C: 探鱼雷达 (1 号候选) —— 鱼点圈 + 丰度热力 + 状态行
--*
--* 数据全部来自客户端本地 FishSchoolManager (服务端零交互):
--* 鱼点位置/半径/鱼数、丰度、打窝点、惊鱼状态, 只读不写。
--* 呈现 (用户拍板 B+A+D): 红圈=有鱼未惊 (r=鱼点半径+2, 官方
--* "@" 圈同款), 橙圈=枯竭或惊; 棕圈=打窝点 (1.6 格), 黄圈=惊窝;
--* 热力格描边按丰度三档 (青越亮鱼越多, 只用前 400 格);
--* 状态行=最近可用鱼点距离 + 咬钩速度档。
--*
--* 性能设计: 取数走 Java 两阶段跨帧 (fishRadarScanBegin 一帧预扫暂存,
--* fishRadarScanFinish 下一帧重放丰度+建表), 绘制走 Java 批量原语
--* (etherDrawIsoCells/etherDrawIsoCircles 各一次跨界调用)。扫描节拍:
--* 移动 ≥5 格且距上次 ≥4s, 或空闲 30s 兜底 —— 咬钩判定取扫描时点
--* 丰度近似 (v1 拍板), 卡车料理理同帧。宽限期 seed=-1 (MP 进服未
--* 收到 fishingData) 时返回码 0, 本帧不消费节拍, 自动重试。
--*
--* 边界: 未开/玩家死亡不动作; pcall 兜底, 内部异常自动关闭;
--* 取数桥缺失 (旧 jar 混装) 时 setEnabled 拒绝开启。
--* 详见 analysis/钓鱼作弊功能研判(1-2-4已实施-已实测-5已撤下留档).md (附录含实施全档)。
--*********************************************************
EtherFishRadar = EtherFishRadar or {}

EtherFishRadar.enabled = false
EtherFishRadar.SCAN_RADIUS = 60
EtherFishRadar.MOVE_MIN = 5
EtherFishRadar.MIN_INTERVAL_MS = 4000
EtherFishRadar.IDLE_RESCAN_MS = 30000

EtherFishRadar.pendingFinish = false
EtherFishRadar.forceScan = false
EtherFishRadar.lastScanMs = 0
EtherFishRadar.lastCx = 0
EtherFishRadar.lastCy = 0
EtherFishRadar.status = nil
EtherFishRadar.circleData = nil
EtherFishRadar.circleCount = 0
EtherFishRadar.cellData = nil
EtherFishRadar.cellCount = 0

--*********************************************************
--* 平铺数组写入 (circles 7 数/圈, cells 6 数/格; Java 侧按序读)
--*********************************************************
local function push7(t, n, a, b, c, d, e, f, g)
    t[n + 1] = a; t[n + 2] = b; t[n + 3] = c; t[n + 4] = d
    t[n + 5] = e; t[n + 6] = f; t[n + 7] = g
    return n + 7
end

local function push6(t, n, a, b, c, d, e, f)
    t[n + 1] = a; t[n + 2] = b; t[n + 3] = c
    t[n + 4] = d; t[n + 5] = e; t[n + 6] = f
    return n + 6
end

--*********************************************************
--* Finish 结果 -> 绘制数组 + 状态串 (阈值只在 Lua: 慢<10, 标准<26, 快≥26)
--*********************************************************
local function applyResult(res, px, py)
    local circles = {}
    local n = 0
    local chums = res.chums
    for i = 1, #chums do
        local c = chums[i]
        if c.scared then
            n = push7(circles, n, c.x, c.y, 1.6, 1.00, 1.00, 0.00, 0.60)
        else
            n = push7(circles, n, c.x, c.y, 1.6, 0.40, 0.25, 0.15, 1.00)
        end
    end

    local points = res.points
    local best, bestD2 = nil, nil
    for i = 1, #points do
        local pt = points[i]
        if pt.n > 0 and not pt.scared then
            n = push7(circles, n, pt.x, pt.y, pt.r + 2.0, 1.00, 0.00, 0.00, 1.00)
        else
            n = push7(circles, n, pt.x, pt.y, pt.r + 2.0, 1.00, 0.60, 0.00, 0.60)
        end
        if pt.n > 0 and not pt.scared and pt.ab > 0 then
            local dx, dy = pt.x - px, pt.y - py
            local d2 = dx * dx + dy * dy
            if bestD2 == nil or d2 < bestD2 then
                best = pt
                bestD2 = d2
            end
        end
    end

    local cells = {}
    local m = 0
    local heat = res.heat
    local i = 1
    while i + 2 <= #heat do
        local ab = heat[i + 2]
        if ab >= 26 then
            m = push6(cells, m, heat[i], heat[i + 1], 0.40, 1.00, 1.00, 0.90)
        elseif ab >= 10 then
            m = push6(cells, m, heat[i], heat[i + 1], 0.25, 0.90, 0.90, 0.55)
        else
            m = push6(cells, m, heat[i], heat[i + 1], 0.20, 0.75, 0.75, 0.30)
        end
        i = i + 3
    end

    EtherFishRadar.circleData = circles
    EtherFishRadar.circleCount = n
    EtherFishRadar.cellData = cells
    EtherFishRadar.cellCount = m

    if best ~= nil then
        local dist = math.floor(math.sqrt(bestD2))
        local tier
        if best.ab >= 26 then
            tier = "UI_Fun_FishTierFast"
        elseif best.ab >= 10 then
            tier = "UI_Fun_FishTierStd"
        else
            tier = "UI_Fun_FishTierSlow"
        end
        EtherFishRadar.status = tr("UI_Fun_FishRadarActive", { dist = dist, tier = getTranslate(tier) })
    else
        EtherFishRadar.status = tr("UI_Fun_FishRadarNone")
    end
end

--*********************************************************
--* 扫描节拍 (OnPlayerUpdate): Finish 优先, 然后判是否 Begin
--*********************************************************
local function tick(p)
    local now = getTimestampMs()
    if EtherFishRadar.pendingFinish then
        EtherFishRadar.pendingFinish = false
        local res = fishRadarScanFinish()
        if res ~= nil then
            applyResult(res, p:getX(), p:getY())
            EtherFishRadar.lastScanMs = now
        end
        return
    end

    local px, py = p:getX(), p:getY()
    local need = false
    if EtherFishRadar.forceScan then
        need = true
    else
        local dx, dy = px - EtherFishRadar.lastCx, py - EtherFishRadar.lastCy
        local moved = dx * dx + dy * dy >= EtherFishRadar.MOVE_MIN * EtherFishRadar.MOVE_MIN
        local elapsed = now - EtherFishRadar.lastScanMs
        if (moved and elapsed >= EtherFishRadar.MIN_INTERVAL_MS) or elapsed >= EtherFishRadar.IDLE_RESCAN_MS then
            need = true
        end
    end
    if not need then return end

    local rc = fishRadarScanBegin(px, py, EtherFishRadar.SCAN_RADIUS)
    if rc == 1 then
        EtherFishRadar.pendingFinish = true
        EtherFishRadar.forceScan = false
        EtherFishRadar.lastCx = px
        EtherFishRadar.lastCy = py
    elseif rc ~= 0 then
        print("[FishRadar] 取数桥不可用, 自动关闭")
        EtherFishRadar.setEnabled(false)
    end
end

local function onPlayerUpdate(p)
    if not EtherFishRadar.enabled then return end
    if p == nil or p:isDead() then return end
    local ok, err = pcall(tick, p)
    if not ok then
        print("[FishRadar] internal error, disabled: " .. tostring(err))
        EtherFishRadar.setEnabled(false)
    end
end

--*********************************************************
--* 绘制 (OnPostUIDraw): 先热力格后圆 (圈盖住格, 鱼点更醒目)
--*********************************************************
local function onPostUIDraw()
    if not EtherFishRadar.enabled then return end
    local cells, cc = EtherFishRadar.cellData, EtherFishRadar.cellCount
    if cells ~= nil and cc > 0 then
        etherDrawIsoCells(cells, cc)
    end
    local circles, rc = EtherFishRadar.circleData, EtherFishRadar.circleCount
    if circles ~= nil and rc > 0 then
        etherDrawIsoCircles(circles, rc)
    end
end

if Events ~= nil then
    Events.OnPlayerUpdate.Add(onPlayerUpdate)
    Events.OnPostUIDraw.Add(onPostUIDraw)
end

function EtherFishRadar.setEnabled(v)
    v = v and true or false
    if v then
        if type(fishRadarScanBegin) ~= "function" or type(fishRadarScanFinish) ~= "function"
            or type(etherDrawIsoCircles) ~= "function" or type(etherDrawIsoCells) ~= "function" then
            print("[FishRadar] 取数/绘制原语不可用, 拒绝开启")
            EtherFishRadar.enabled = false
            return false
        end
    end
    EtherFishRadar.enabled = v
    EtherFishRadar.pendingFinish = false
    EtherFishRadar.forceScan = v
    EtherFishRadar.lastScanMs = 0
    EtherFishRadar.lastCx = 0
    EtherFishRadar.lastCy = 0
    if not v then
        EtherFishRadar.status = getTranslate("UI_Fun_FishRadarOff")
    end
    print("[FishRadar] " .. (v and "ON" or "OFF"))
    return v
end
