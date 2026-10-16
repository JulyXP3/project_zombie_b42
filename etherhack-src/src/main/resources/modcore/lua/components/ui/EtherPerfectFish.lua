--*********************************************************
--* 「趣味-钓鱼」A: 完美搏鱼 (2 号候选, 纯客户端零上行)
--*
--* 钓鱼张力模拟全在客户端 (FishingRod:updateLine 开头 isServer 早退),
--* 失败只有三条路, 全在 updateLine 117-160 段: 张力过高随机 damageLine,
--* 线损光 brokeLine, 高压/低压计时超限 missFish —— 而 missFish/brokeLine
--* 在 MP 下才发 fishing/missFish | brokeRodLine 客户端命令通知服务端,
--* 不被触发 = 服务端全程无感知, 唯一行为面只是收线节奏变化。
--*
--* 覆盖策略 (只跳过失败模拟, 保留其余逻辑):
--*  - updateLine: 仍调用 updateLineMoveCoeff (收线系数推进 + 咬钩后
--*    置 bobber.catchFishStarted, MP 下该标记会经 OnFishingActionMPUpdate
--*    同步给服务端 bobber, 必须保留), 然后直接 return true ——
--*    highTension/lowTension 计时器不再推进, 不断线不脱钩;
--*  - reel: 去掉原版 tension < 1 封顶, 收线系数 0.04 -> 0.20 (5 倍),
--*    lineLen 下限 0.1 (保张力恒正); lineLen 越短 getTension 越高,
--*    Bobber:update 的 tensionForce 0.025 倍率把浮标加速拉向玩家,
--*    isPickupBobber (dist < 0.6) 到达即进 PickupFish 收鱼。
--*
--* 实测注: 关闭开关瞬间即回原版 (两条 vanilla 函数原样回放)。
--* 详见 analysis/钓鱼作弊功能研判(2-4已实施-已实测-5已撤下留档).md。
--*********************************************************
EtherPerfectFish = EtherPerfectFish or {}

EtherPerfectFish.enabled = false
EtherPerfectFish.installed = false
EtherPerfectFish.REEL_SPEED = 0.20      -- 收线系数 (原版 0.04)
EtherPerfectFish.MIN_LINELEN = 0.1      -- 收线下限 (保张力恒正)

local vanillaUpdateLine = nil
local vanillaReel = nil

local function installOverrides()
    if EtherPerfectFish.installed then return true end
    if Fishing == nil or Fishing.FishingRod == nil then return false end

    vanillaUpdateLine = Fishing.FishingRod.updateLine
    vanillaReel = Fishing.FishingRod.reel

    function Fishing.FishingRod:updateLine()
        if not EtherPerfectFish.enabled then
            return vanillaUpdateLine(self)
        end
        if isServer() then return true end
        -- 保留收线系数/咬钩标记推进, 跳过张力失败模拟 (扣线/断线/脱钩)
        self:updateLineMoveCoeff()
        return true
    end

    function Fishing.FishingRod:reel()
        if not EtherPerfectFish.enabled then
            return vanillaReel(self)
        end
        -- 无张力封顶 + 5 倍速收线
        if self.lineLen ~= nil and self.lineLen > EtherPerfectFish.MIN_LINELEN then
            self.lineLen = self.lineLen - EtherPerfectFish.REEL_SPEED * getGameTime():getMultiplier() * (self.lineMoveCoeff * self.lineMoveCoeff)
            if self.lineLen < EtherPerfectFish.MIN_LINELEN then
                self.lineLen = EtherPerfectFish.MIN_LINELEN
            end
        end
    end

    EtherPerfectFish.installed = true
    print("[PerfectFish] hooked into Fishing.FishingRod")
    return true
end

-- 原版文件若尚未加载则先拉进来; 仍失败就等进游戏时再装 (双保险)
pcall(require, "Fishing/FishingRod")
if not installOverrides() then
    if Events ~= nil then
        Events.OnGameStart.Add(function()
            pcall(require, "Fishing/FishingRod")
            installOverrides()
        end)
    end
end

function EtherPerfectFish.setEnabled(v)
    EtherPerfectFish.enabled = v and true or false
    print("[PerfectFish] " .. (EtherPerfectFish.enabled and "ON" or "OFF"))
    return EtherPerfectFish.enabled
end
