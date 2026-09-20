--*********************************************************
--* 红队 POC: 计时动作真实生成 (ISTakeBricks) — 一百三十一 实施
--* 移植自 PienZ 真实源码: item_spawner.cpp / ItemSpawnTask.java +
--*   timed_action_accelerator.cpp (加速件; 见 TakeSpawnAPI 文件头)。
--*
--* 为什么不是"直投": InvMngGetItem 那条链只让目标客户端本地 addItem, 服务端全程不建物 →
--* 幽灵物品 (重登即失)。PienZ 自己也只把它当 Crash 载体, 不是给物路线。
--* 真物品链 = 原版 ISTakeBricks 计时动作 → 服务端校验 Accept → 完成时
--*   `AddItems(item, amount)` + **sendAddItemsToContainer** 上行 = 落地真实物品。
--*
--* 客户端提速 (零作弊位): 原版 `ISTakeBricks:getDuration()` 内部读
--* `character:isTimedActionInstant()` 作弊位 (KWRR 会查) —— 本模块改为**运行期覆盖
--* getDuration** 直接返回 1 (仅在自己的生成调用期间生效, 调用返回即还原; 纪律 4:
--* 记录改前值含 nil 缺失态, 只写回自己改过的键)。
--*********************************************************
EtherTakeSpawn = EtherTakeSpawn or {}

EtherTakeSpawn.busy = false
EtherTakeSpawn.message = ""
EtherTakeSpawn.accelerate = true      -- 面板「加速」开关 (默认开: 1 份也要 10s, 不开太慢)

local DURATION_ORIGINAL = ISTakeBricks and ISTakeBricks.getDuration or nil
local overrideActive = false

local function installDurationOverride()
    if overrideActive or ISTakeBricks == nil then return end
    overrideActive = true
    ISTakeBricks.getDuration = function(self)
        -- 极短时长 = 客户端动作 1 帧完成; 服务端侧由加速件 (Hand_L pain NaN) 保证同步瞬间完成
        return 1
    end
end

local function restoreDurationOverride()
    if not overrideActive then return end
    overrideActive = false
    if ISTakeBricks == nil then return end
    if DURATION_ORIGINAL ~= nil then
        ISTakeBricks.getDuration = DURATION_ORIGINAL
    else
        ISTakeBricks.getDuration = nil      -- 改前缺失态: 还原成 nil, 不留空壳
    end
end

--*********************************************************
--* 入口: 面板"生成"按钮 (target = 物品全名)
--*********************************************************
function EtherTakeSpawn.trigger(target, count, accelerate)
    if EtherTakeSpawn.busy then
        return
    end
    if target == nil or target == "" then
        return
    end
    count = tonumber(count) or 1
    if count < 1 then count = 1 end
    if count > 100 then count = 100 end
    if accelerate == nil then accelerate = EtherTakeSpawn.accelerate end

    EtherTakeSpawn.busy = true
    EtherTakeSpawn.message = getTranslate("UI_TakeSpawn_Preparing")
    print("[TakeSpawn] target " .. target .. " x" .. count
        .. (accelerate and " (accelerated)" or ""))

    if type(takeSpawnStart) ~= "function" then
        EtherTakeSpawn.message = getTranslate("UI_TakeSpawn_Unavailable")
        EtherTakeSpawn.busy = false
        return
    end

    -- 客户端提速只在动作构造期需要 (maxTime = getDuration() 在 ISTakeBricks.new 里算一次)
    if accelerate then
        installDurationOverride()
    end
    local ok, ret = pcall(takeSpawnStart, target, count, accelerate == true)
    restoreDurationOverride()

    if ok and ret then
        EtherTakeSpawn.message = tr("UI_TakeSpawn_Done", {
            item = tostring(target), count = tostring(count) })
        print("[TakeSpawn] action sent: " .. count .. "x " .. target)
    else
        local reason = ok and "unknown error" or tostring(ret)
        EtherTakeSpawn.message = tr("UI_TakeSpawn_Failed", { reason = reason })
        print("[TakeSpawn] failed: " .. tostring(ret))
        if type(takeSpawnCancel) == "function" then
            pcall(takeSpawnCancel);       -- 失败时确保加速件不留残
        end
    end
    EtherTakeSpawn.busy = false
end
