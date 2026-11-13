--*********************************************************
--* EtherSpawnQueue: 物品生成待办清单 + 串行执行器 (三百零三)
--*
--* 两种生成方式的多选统一在这里排队:
--*   尸体生成 — 整个清单合成一次调用, 一具尸体装多件 (同一条 AddCorpseToMapPacket);
--*   钓竿生成 — 逐件串行, 等 EtherFishSpawn 状态机结束 (busy / epoch / lastResult)。
--*
--* 规则:
--*   · 失败即停, 保留清单与失败原因; 再次点「开始生成」只重试未完成的项;
--*   · 完成判定一律优先用"背包里该物品数量真的多了", 动作结束只是辅助信号;
--*   · 清空/停止在清单运行中也可用, 但不会硬中断进行中的钓竿 (让它自然结束)。
--*   · 开盒生成 (poison) 不支持多选, 不进本队列。
--*
--* 仅限用户自己的服务器 / 自建测试环境。
--*********************************************************
require "ISUI/ISPanel"

EtherSpawnQueue = EtherSpawnQueue or {}

EtherSpawnQueue.rows = {}
EtherSpawnQueue.mode = nil
EtherSpawnQueue.running = false
EtherSpawnQueue.activeIndex = 0
EtherSpawnQueue.message = ""
EtherSpawnQueue.stepStartedAt = 0
EtherSpawnQueue.maxTotal = 300      -- 与 Java 侧 MAX_TOTAL_ITEMS 一致

local ITEM_INTERVAL_MS = 1000       -- 相邻两件之间的最小间隔 (降低连续发包的日志频次)
local FISH_TIMEOUT_MS = 60000

local function nowMs()
    return getTimestampMs()
end

local function statusKey(status)
    if status == "running" then return "UI_SpawnQueue_StatusRunning" end
    if status == "done" then return "UI_SpawnQueue_StatusDone" end
    if status == "failed" then return "UI_SpawnQueue_StatusFailed" end
    return "UI_SpawnQueue_StatusPending"
end
EtherSpawnQueue.statusKey = statusKey

--*********************************************************
--* 清单维护
--*********************************************************
function EtherSpawnQueue.count()
    return #EtherSpawnQueue.rows
end

function EtherSpawnQueue.doneCount()
    local n = 0
    for _, r in ipairs(EtherSpawnQueue.rows) do
        if r.status == "done" then n = n + 1 end
    end
    return n
end

function EtherSpawnQueue.add(fullName, name, count)
    if fullName == nil or fullName == "" then return false end
    count = math.floor(tonumber(count) or 1)
    if count < 1 then count = 1 end
    if count > 100 then count = 100 end
    for _, r in ipairs(EtherSpawnQueue.rows) do
        if r.fullName == fullName then
            r.count = math.min(100, r.count + count)
            r.status = "pending"
            r.reason = ""
            return true
        end
    end
    table.insert(EtherSpawnQueue.rows, {
        fullName = fullName,
        name = name or fullName,
        count = count,
        status = "pending",
        reason = "",
    })
    return true
end

function EtherSpawnQueue.removeAt(i)
    if EtherSpawnQueue.running and EtherSpawnQueue.activeIndex == i then
        return false
    end
    table.remove(EtherSpawnQueue.rows, i)
    if EtherSpawnQueue.activeIndex > i then
        EtherSpawnQueue.activeIndex = EtherSpawnQueue.activeIndex - 1
    end
    return true
end

function EtherSpawnQueue.setCount(i, n)
    local r = EtherSpawnQueue.rows[i]
    if r == nil then return end
    if EtherSpawnQueue.running and EtherSpawnQueue.activeIndex == i then return end
    n = math.floor(tonumber(n) or r.count)
    if n < 1 then n = 1 end
    if n > 100 then n = 100 end
    r.count = n
    if r.status == "done" then r.status = "pending" end
end

function EtherSpawnQueue.clear()
    EtherSpawnQueue.stop()
    EtherSpawnQueue.rows = {}
    EtherSpawnQueue.message = ""
end

--*********************************************************
--* 背包数量 (完成判定的唯一硬证据)
--*********************************************************
function EtherSpawnQueue.itemCount(fullName)
    local p = getPlayer()
    if p == nil then return 0 end
    local ok, n = pcall(function() return p:getInventory():getItemCount(fullName) end)
    if ok and n ~= nil then return n end
    return 0
end

--*********************************************************
--* 启动 / 停止
--*********************************************************
function EtherSpawnQueue.start(mode)
    if EtherSpawnQueue.running then return false end
    if mode ~= "fish" and mode ~= "corpse" then return false end
    if #EtherSpawnQueue.rows == 0 then return false end
    if not isMultiplayer() then
        EtherSpawnQueue.message = getTranslate("UI_FishSpawn_MultiplayerOnly")
        return false
    end

    EtherSpawnQueue.mode = mode
    for _, r in ipairs(EtherSpawnQueue.rows) do
        if r.status ~= "done" then
            r.status = "pending"
            r.reason = ""
        end
    end
    EtherSpawnQueue.running = true
    EtherSpawnQueue.activeIndex = 0
    EtherSpawnQueue.nextAt = 0
    EtherSpawnQueue.message = ""

    if mode == "corpse" then
        EtherSpawnQueue.runCorpseBatch()
    else
        EtherSpawnQueue.dispatchNext()
    end
    return true
end

function EtherSpawnQueue.stop()
    if not EtherSpawnQueue.running then return end
    local r = EtherSpawnQueue.rows[EtherSpawnQueue.activeIndex]
    if r ~= nil and r.status == "running" then
        r.status = "pending"
    end
    EtherSpawnQueue.activeIndex = 0
    EtherSpawnQueue.running = false
    EtherSpawnQueue.message = getTranslate("UI_SpawnQueue_Stopped")
end

--*********************************************************
--* 尸体生成: 整个清单一次打包 (一具尸体装多件)
--*********************************************************
function EtherSpawnQueue.runCorpseBatch()
    local parts = {}
    local pending = {}
    local total = 0
    for _, r in ipairs(EtherSpawnQueue.rows) do
        if r.status ~= "done" then
            total = total + r.count
            table.insert(parts, r.fullName .. "=" .. tostring(r.count))
            table.insert(pending, r)
        end
    end
    if #parts == 0 then
        EtherSpawnQueue.running = false
        EtherSpawnQueue.message = getTranslate("UI_SpawnQueue_AllDone")
        EtherSpawnQueue.rows = {}
        return
    end
    if total > EtherSpawnQueue.maxTotal then
        local msg = tr("UI_SpawnQueue_TooMany", { max = tostring(EtherSpawnQueue.maxTotal) })
        for _, r in ipairs(pending) do
            r.status = "failed"
            r.reason = msg
        end
        EtherSpawnQueue.running = false
        EtherSpawnQueue.message = msg
        return
    end

    local ok = false
    local ret = nil
    if type(corpseSpawnPrepareBatch) == "function" then
        ok, ret = pcall(corpseSpawnPrepareBatch, table.concat(parts, ","))
    else
        ret = "corpseSpawnPrepareBatch unavailable"
    end
    local success = ok and ret == true
    local reason = success and "" or (ok and "unknown error" or tostring(ret))
    for _, r in ipairs(pending) do
        r.status = success and "done" or "failed"
        r.reason = reason
    end
    EtherSpawnQueue.running = false
    EtherSpawnQueue.message = success and getTranslate("UI_SpawnQueue_AllDone") or reason
    -- 三百一十一: 全部成功就清空清单 (失败保留现场, 供查看/重试)
    if success then
        EtherSpawnQueue.rows = {}
    end
end

--*********************************************************
--* 逐件派发 (钓竿)
--*********************************************************
function EtherSpawnQueue.dispatchNext()
    local idx = nil
    for i, r in ipairs(EtherSpawnQueue.rows) do
        if r.status ~= "done" then
            idx = i
            break
        end
    end
    if idx == nil then
        EtherSpawnQueue.running = false
        EtherSpawnQueue.activeIndex = 0
        EtherSpawnQueue.message = getTranslate("UI_SpawnQueue_AllDone")
        -- 三百一十一: 全部完成即清空 —— 否则"清单里还有 N 件"会被误解成再点生成
        -- 会把它们重新生成一遍 (实际只跑未完成项, 已完成项会被跳过)
        EtherSpawnQueue.rows = {}
        return
    end

    local r = EtherSpawnQueue.rows[idx]
    EtherSpawnQueue.activeIndex = idx
    r.status = "running"
    r.reason = ""
    r.startedAt = nowMs()
    r.before = EtherSpawnQueue.itemCount(r.fullName)

    if EtherSpawnQueue.mode == "fish" then
        r.timeoutMs = FISH_TIMEOUT_MS
        r.epoch = (EtherFishSpawn ~= nil and EtherFishSpawn.epoch) or 0
        local ok, started = pcall(EtherFishSpawn.trigger, r.fullName)
        if not ok or not started then
            local why = ok and (EtherFishSpawn.message or "start failed") or tostring(started)
            EtherSpawnQueue.finishRow(idx, false, why)
        end
    else
        EtherSpawnQueue.finishRow(idx, false, "unsupported mode")
    end
end

function EtherSpawnQueue.finishRow(idx, ok, reason)
    local r = EtherSpawnQueue.rows[idx]
    if r == nil then return end
    r.status = ok and "done" or "failed"
    r.reason = reason or ""
    EtherSpawnQueue.activeIndex = 0
    if not ok then
        EtherSpawnQueue.running = false
        EtherSpawnQueue.message = reason or "failed"
        return
    end
    -- 成功: 交给 tick 在最小间隔后派发下一件 (不在这里直接连发)
    EtherSpawnQueue.nextAt = nowMs() + ITEM_INTERVAL_MS
end

--*********************************************************
--* 每帧推进 (空闲时立即返回)
--*********************************************************
function EtherSpawnQueue.tick()
    if not EtherSpawnQueue.running then return end
    if EtherSpawnQueue.mode == "corpse" then return end

    local idx = EtherSpawnQueue.activeIndex
    if idx == 0 then
        if nowMs() >= (EtherSpawnQueue.nextAt or 0) then
            EtherSpawnQueue.dispatchNext()
        end
        return
    end
    local r = EtherSpawnQueue.rows[idx]
    if r == nil or r.status ~= "running" then
        EtherSpawnQueue.dispatchNext()
        return
    end

    local now = nowMs()
    local elapsed = now - (r.startedAt or now)
    local gained = EtherSpawnQueue.itemCount(r.fullName) - (r.before or 0)
    if gained > 0 then
        EtherSpawnQueue.finishRow(idx, true)
        return
    end
    if elapsed > (r.timeoutMs or FISH_TIMEOUT_MS) then
        EtherSpawnQueue.finishRow(idx, false, getTranslate("UI_SpawnQueue_Timeout"))
        return
    end

    if EtherSpawnQueue.mode == "fish" then
        if EtherFishSpawn == nil or not EtherFishSpawn.busy then
            local epoch = (EtherFishSpawn ~= nil and EtherFishSpawn.epoch) or 0
            if epoch ~= (r.epoch or 0) then
                EtherSpawnQueue.finishRow(idx, false, EtherFishSpawn.lastResult or "failed")
            else
                EtherSpawnQueue.finishRow(idx, false, "state machine not running")
            end
        end
    end
end

function EtherSpawnQueue.progressText()
    if EtherSpawnQueue.running then
        return tr("UI_SpawnQueue_Progress", {
            i = tostring(EtherSpawnQueue.activeIndex),
            n = tostring(#EtherSpawnQueue.rows),
        })
    end
    return EtherSpawnQueue.message or ""
end

if Events ~= nil then
    Events.OnTick.Add(EtherSpawnQueue.tick)
end

--*********************************************************
--* 清单弹窗: 行内改数量/删除 + 开始/停止/清空/关闭。
--* 行是手绘的 (无子控件): 行数少、每帧刷新状态, 省掉一堆控件同步。
--* 与动物多选弹窗同一套生命周期: 挂 UIManager, 切页/收菜单自动关闭。
--*********************************************************
EtherSpawnQueuePopup = ISPanel:derive("EtherSpawnQueuePopup")
EtherSpawnQueuePopup.instance = nil

local POPUP_PAD    = 10
local POPUP_W      = 430
-- 行高随字体档位: 两行文字 (名称 + 状态) + 内边距。写死 40 在大字体下会把
-- "待生成/Pending" 裁掉半行 (实机反馈)。EtherTheme 由模块清单先行加载;
-- 兜底值只防加载顺序被改动时算出 nil。
local ROW_H = ((EtherTheme and EtherTheme.fontHgtSmall or 19)
    + (EtherTheme and EtherTheme.fontHgtHint or 13)) + 12
local MAX_LIST_PX  = 420
local MAX_ROWS     = 8
local CTRL_W       = 18
local QTY_W        = 34

local function maxVisibleRows()
    local n = math.floor(MAX_LIST_PX / ROW_H)
    if n < 3 then n = 3 end
    if n > MAX_ROWS then n = MAX_ROWS end
    return n
end

local function queueRowVisible()
    local n = EtherSpawnQueue.count()
    local cap = maxVisibleRows()
    if n > cap then return cap end
    if n < 1 then return 1 end
    return n
end

function EtherSpawnQueue.openPopup(mode)
    if EtherSpawnQueuePopup.instance ~= nil then
        EtherSpawnQueuePopup.instance:close()
        return
    end
    if EtherSpawnQueue.running then
        mode = EtherSpawnQueue.mode
    end
    local popup = EtherSpawnQueuePopup:new(mode)
    popup:initialise()
    popup:instantiate()

    local sw, sh = getCore():getScreenWidth(), getCore():getScreenHeight()
    local px = math.floor(getMouseX() + 2)
    local py = math.floor(getMouseY() + 2)
    if px + popup.width > sw - 8 then px = sw - popup.width - 8 end
    if px < 8 then px = 8 end
    if py + popup.height > sh - 8 then py = py - popup.height - 18 end
    if py < 8 then py = 8 end
    popup:setX(px)
    popup:setY(py)

    popup:addToUIManager()
    popup:setVisible(true)
    EtherSpawnQueuePopup.instance = popup
end

function EtherSpawnQueuePopup:new(mode)
    local fh = EtherTheme.fontHgtSmall
    local titleH = fh + 6
    local listH = queueRowVisible() * ROW_H
    local statusH = EtherTheme.fontHgtHint + 4
    local btnH = EtherTheme.ctrlH
    -- 三百零八: 弹窗里不再有「开始」按钮 —— 生成统一由主面板按钮执行, 这里只留
    -- 停止/清空/关闭, 单排三个等宽按钮。
    local h = POPUP_PAD + titleH + 4 + listH + 6 + statusH + 4 + btnH + POPUP_PAD

    local o = ISPanel:new(0, 0, POPUP_W, h)
    setmetatable(o, self)
    self.__index = self
    o.background = false
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 }
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 }
    o.moveWithMouse = false
    o.mode = mode
    o.titleH = titleH
    o.listY = POPUP_PAD + titleH + 4
    o.listH = listH
    o.statusY = o.listY + listH + 6
    o.btnY1 = o.statusY + statusH + 4
    o.scroll = 0
    return o
end

function EtherSpawnQueuePopup:createChildren()
    ISPanel.createChildren(self)
    local gap = EtherTheme.ctrlGap
    local bw = math.floor((self.width - POPUP_PAD * 2 - gap * 2) / 3)
    local H = EtherTheme.ctrlH
    self.stopBtn = UIButton:new(POPUP_PAD, self.btnY1, bw, H,
        getTranslate("UI_SpawnQueue_Stop"), function() EtherSpawnQueue.stop() end, bw)
    self.clearBtn = UIButton:new(POPUP_PAD + bw + gap, self.btnY1, bw, H,
        getTranslate("UI_SpawnQueue_Clear"), function() EtherSpawnQueue.clear() end, bw)
    self.closeBtn = UIButton:new(POPUP_PAD + (bw + gap) * 2, self.btnY1, bw, H,
        getTranslate("UI_SpawnQueue_Close"), function() self:close() end, bw)
    self:addChild(self.stopBtn)
    self:addChild(self.clearBtn)
    self:addChild(self.closeBtn)
end

-- 背景/标题必须在 prerender (render 在子控件之后: 画在 render 会盖住按钮)
function EtherSpawnQueuePopup:prerender()
    -- 背景对齐主面板 (纯黑底 + 切角青框), 不再用玻璃青底/激活磁贴填充 (用户 2026-11-13)
    local bg = EtherTheme.panelBG
    self:drawRect(0, 0, self.width, self.height, 1.0, bg.r, bg.g, bg.b)
    EtherTheme.drawFrame(self, 0, 0, self.width, self.height)
    local tc = EtherTheme.text
    local title = getTranslate("UI_SpawnQueue_Title")
    local modeKey = self.mode == "corpse" and "UI_SpawnMode_Corpse"
        or self.mode == "fish" and "UI_SpawnMode_Fish"
        or nil
    if modeKey ~= nil then
        title = title .. " · " .. getTranslate(modeKey)
    end
    self:drawText(title, POPUP_PAD, POPUP_PAD - 1, tc.r, tc.g, tc.b, 1.0, UIFont.Small)
end

local function reasonText(reason)
    if reason == nil or reason == "" then return "" end
    return tostring(reason)
end

function EtherSpawnQueuePopup:render()
    local fh = EtherTheme.fontHgtSmall
    local th = EtherTheme
    local rows = EtherSpawnQueue.rows

    -- 行区
    local stencilY = self.listY
    self:setStencilRect(POPUP_PAD - 2, stencilY, self.width - POPUP_PAD * 2 + 4, self.listH)
    for i = 1, #rows do
        local y = self.listY + (i - 1) * ROW_H - self.scroll
        if y + ROW_H > self.listY and y < self.listY + self.listH then
            self:drawQueueRow(rows[i], i, y, fh, th)
        end
    end
    self:clearStencilRect()

    -- 状态行
    local msg = EtherSpawnQueue.progressText()
    if msg ~= "" then
        local col = EtherSpawnQueue.running and th.statusBlue or th.textDim
        EtherTheme.drawHintText(self, msg, POPUP_PAD, self.statusY, col, 0.95)
    end

    -- 空清单提示
    if #rows == 0 then
        self:drawText(getTranslate("UI_SpawnQueue_Empty"), POPUP_PAD, self.listY + 6,
            th.textDim.r, th.textDim.g, th.textDim.b, 1.0, UIFont.Small)
    end
end

function EtherSpawnQueuePopup:drawQueueRow(r, index, y, fh, th)
    local w = self.width
    local removeX = w - POPUP_PAD - CTRL_W
    local plusX = removeX - 6 - CTRL_W
    local qtyX = plusX - QTY_W
    local minusX = qtyX - CTRL_W
    local textW = minusX - POPUP_PAD - 6

    -- 状态色条
    local sc = r.status == "done" and th.statusGreen
        or r.status == "failed" and th.statusRed
        or r.status == "running" and th.statusBlue
        or th.textDim
    self:drawRect(POPUP_PAD - 4, y + 2, 2, ROW_H - 4, 1.0, sc.r, sc.g, sc.b)

    -- 名称 + 状态
    self:setStencilRect(POPUP_PAD, y, math.max(10, textW), ROW_H)
    self:drawText(tostring(r.name or r.fullName), POPUP_PAD, y + 3,
        th.text.r, th.text.g, th.text.b, 1.0, UIFont.Small)
    local sub = getTranslate(statusKey(r.status))
    local reason = reasonText(r.reason)
    if reason ~= "" then
        sub = sub .. " · " .. reason
    end
    EtherTheme.drawHintText(self, sub, POPUP_PAD, y + 4 + fh, sc, 0.85)
    self:clearStencilRect()

    -- 数量控件 (钓竿模式固定 1 件, 不显示 +/-, 只显示 x1)
    local qtyText = "x" .. tostring(r.count)
    if self.mode == "fish" then
        qtyText = "x1"
        self:drawText(qtyText, qtyX, y + (ROW_H - fh) / 2, th.textDim.r, th.textDim.g, th.textDim.b, 1.0, UIFont.Small)
    else
        self:drawQueueCtrl(minusX, y, "-", r.status ~= "running")
        self:drawText(qtyText, qtyX, y + (ROW_H - fh) / 2, th.text.r, th.text.g, th.text.b, 1.0, UIFont.Small)
        self:drawQueueCtrl(plusX, y, "+", r.status ~= "running")
    end
    self:drawQueueCtrl(removeX, y, "x", r.status ~= "running")
end

function EtherSpawnQueuePopup:drawQueueCtrl(x, y, label, enable)
    local h = 18
    local yy = y + (ROW_H - h) / 2
    local th = EtherTheme
    local a = enable and 0.9 or 0.3
    self:drawRect(x, yy, CTRL_W, h, 0.18, th.blood.r, th.blood.g, th.blood.b)
    self:drawRectBorder(x, yy, CTRL_W, h, a * 0.6, th.blood.r, th.blood.g, th.blood.b)
    local tw = getTextManager():MeasureStringX(UIFont.Small, label)
    self:drawText(label, x + (CTRL_W - tw) / 2, yy + (h - th.fontHgtSmall) / 2,
        th.text.r, th.text.g, th.text.b, a, UIFont.Small)
end

function EtherSpawnQueuePopup:onMouseDown(x, y)
    if y < self.listY or y >= self.listY + self.listH then return false end
    local idx = math.floor((y - self.listY + self.scroll) / ROW_H) + 1
    local r = EtherSpawnQueue.rows[idx]
    if r == nil then return false end
    if r.status == "running" then return true end        -- 进行中的行不许改

    local w = self.width
    local removeX = w - POPUP_PAD - CTRL_W
    local plusX = removeX - 6 - CTRL_W
    local qtyX = plusX - QTY_W
    local minusX = qtyX - CTRL_W
    if x >= removeX and x <= removeX + CTRL_W then
        EtherSpawnQueue.removeAt(idx)
        return true
    end
    if self.mode ~= "fish" then
        if x >= minusX and x <= minusX + CTRL_W then
            EtherSpawnQueue.setCount(idx, r.count - 1)
            return true
        end
        if x >= plusX and x <= plusX + CTRL_W then
            EtherSpawnQueue.setCount(idx, r.count + 1)
            return true
        end
    end
    return true
end

function EtherSpawnQueuePopup:onMouseWheel(del)
    local total = EtherSpawnQueue.count() * ROW_H
    local maxScroll = math.max(0, total - self.listH)
    self.scroll = math.max(0, math.min(maxScroll, self.scroll - del * 40))
    return true
end

function EtherSpawnQueuePopup:update()
    if EtherSpawnQueuePopup.instance ~= self then return end
    -- 清单变短时把滚动收回到有效范围
    local total = EtherSpawnQueue.count() * ROW_H
    local maxScroll = math.max(0, total - self.listH)
    if self.scroll > maxScroll then self.scroll = maxScroll end
    if self.stopBtn ~= nil then
        self.stopBtn:setEnable(EtherSpawnQueue.running)
        self.clearBtn:setEnable(not EtherSpawnQueue.running and EtherSpawnQueue.count() > 0)
    end
    local main = EtherMain.instance
    if main == nil or not main:getIsVisible() then
        self:close()
    end
end

function EtherSpawnQueuePopup:close()
    if EtherSpawnQueuePopup.instance == self then
        EtherSpawnQueuePopup.instance = nil
    end
    self:setVisible(false)
    self:removeFromUIManager()
end
