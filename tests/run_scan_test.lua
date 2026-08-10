-- EtherItemSearch.scan / 事件刷新逻辑冒烟测试 (桩模拟游戏 API)
-- 运行: lua5.1.exe run_scan_test.lua <路径>/EtherItemSearch.lua
local fakeClock = { now = 100000 }
getTimestampMs = function() return fakeClock.now end

-- 捕获事件注册, 便于手动触发 (注: dot 调用无隐式 self, Add 只收 1 个参数)
local handlers = {}
Events = {
    OnPlayerUpdate = { Add = function(f) handlers.onPlayerUpdate = f end },
    OnRefreshInventoryWindowContainers = { Add = function(f) handlers.onInventoryWindowChanged = f end },
}

local invSize = 5
local px, py = 100.4, 200.6
local player = { getX = function() return px end, getY = function() return py end, getZ = function() return 0 end }
player.getInventory = function() return { getItems = function() return { size = function() return invSize end } end } end

local function fakeItem(fullType, typeName, inventory)
    local o = {
        getFullType = function() return fullType end,
        getType = function() return typeName or fullType end,
        IsInventoryContainer = function() return inventory ~= nil end,
    }
    if inventory ~= nil then
        o.getInventory = function() return inventory end
    end
    return o
end

local function fakeList(...)
    local arr = { ... }
    return {
        size = function() return #arr end,
        get = function(_, i) return arr[i + 1] end,
    }
end

local function fakeContainer(...)
    local arr = { ... }
    return {
        size = function() return #arr end,
        get = function(_, i) return arr[i + 1] end,
        getItems = function() return fakeList(unpack(arr)) end,
    }
end

local function fakeObj(x, y, o)
    o.getX = o.getX or function() return x end
    o.getY = o.getY or function() return y end
    return o
end

-- 家具: 1 个容器 (全名命中 Axe + 短名命中 Alternative/Axe + 忽略 Saw)
local shortNameItem = fakeItem("Base.Alternative", "Axe")
local worldObj = fakeObj(110, 210, {
    getContainerCount = function() return 1 end,
    getContainerByIndex = function(_, i)
        if i == 0 then
            return fakeContainer(fakeItem("Base.Axe", "Axe"), fakeItem("Base.Saw"), shortNameItem)
        end
        return nil
    end,
})

-- 地面背包: getItem 命中 + getInventory 容器内命中
local groundBag = fakeObj(120, 220, {
    getItem = function() return fakeItem("Base.Axe", "Axe", fakeContainer(fakeItem("Base.Axe", "Axe"), fakeItem("Base.Pen", "Pen"))) end,
})

-- 地面散落物: 命中 (非容器 -> 不取内部物品)
local groundItem = fakeObj(130, 230, {
    getItem = function() return fakeItem("Base.Axe", "Axe") end,
})

-- 普通物体: 无容器 -> 忽略
local wall = fakeObj(140, 240, {
    getContainerCount = function() return 0 end,
})

local squares = {
    [100 * 100000 + 200] = {
        getObjects = function() return fakeList(worldObj, wall) end,
        getWorldObjects = function() return fakeList(groundBag, groundItem) end,
    },
}

-- 车辆: 后备箱容器内命中; getVehicles 返回 Set, 用 :toArray() 转数组 (官方同款)
local vehicle = {
    getX = function() return 148 end, getY = function() return 248 end,
    getPartCount = function() return 2 end,
    getPartByIndex = function(_, i)
        if i ~= 0 then
            return { getItemContainer = function() return nil end, getX = function() return 149 end, getY = function() return 249 end }
        end
        local part = {
            isContainer = function() return true end,
            getItemContainer = function() return fakeContainer(fakeItem("Base.Axe", "Axe")) end,
            getX = function() return 149 end, getY = function() return 249 end,
        }
        return part
    end,
}

-- 统计 getGridSquare 调用次数, 验证 tick 触发了重扫
local squareCalls = 0
getCell = function()
    return {
        getVehicles = function() return { toArray = function() return { vehicle } end } end,
        getGridSquare = function(_, x, y, z)
            squareCalls = squareCalls + 1
            if z ~= 0 then return nil end
            return squares[x * 100000 + y]
        end,
    }
end
getPlayer = function() return player end

dofile(arg[1])  -- EtherItemSearch.lua

-- 用例 1: 全名匹配
local target = { ["Base.Axe"] = true }
local n = EtherItemSearch.scan(target)

assert(n == 4, "expected 4 locations, got " .. n)
assert(#EtherItemSearch.results == 4, "expected 4 results entries")

local byKey = {}
for _, p in ipairs(EtherItemSearch.results) do
    byKey[p.x .. "," .. p.y] = p.count
end
assert(byKey["110,210"] == 1, "110,210 (furniture, full name only for Axe) should have count 1")
assert(byKey["120,220"] == 2, "120,220 (ground bag) should have count 2")
assert(byKey["130,230"] == 1, "130,230 (floor item) should have count 1")
assert(byKey["149,249"] == 1, "149,249 (vehicle trunk) should have count 1")

-- 用例 2: 短名匹配 (按钮侧 getFullName 返回短名时的回退)
EtherItemSearch.clear()
local n3 = EtherItemSearch.scan({ ["Axe"] = true })
assert(n3 == 4, "expected 4 locations with short name, got " .. n3)
local byKey3 = {}
for _, p in ipairs(EtherItemSearch.results) do
    byKey3[p.x .. "," .. p.y] = p.count
end
assert(byKey3["110,210"] == 2, "110,210 (full+short) should have count 2 with short-name target")

-- 用例 3: 防抖刷新 —— 背包物品数变化后不立即扫; 安静满 1 秒才重扫一次
local callsBefore = squareCalls
invSize = invSize + 1
handlers.onPlayerUpdate(player)          -- 变化事件: 只置脏
assert(squareCalls == callsBefore, "change must not rescan immediately (debounce)")

fakeClock.now = fakeClock.now + 500
handlers.onPlayerUpdate(player)          -- 仅安静 0.5s, 不扫
assert(squareCalls == callsBefore, "must not rescan before 1s quiet")

fakeClock.now = fakeClock.now + 600
handlers.onPlayerUpdate(player)          -- 安静满 1s -> 重扫一次
assert(squareCalls > callsBefore, "must rescan once after 1s quiet")
assert(EtherItemSearch.results ~= nil and #EtherItemSearch.results == 4, "silent rescan keeps results")

local callsAfter = squareCalls
handlers.onPlayerUpdate(player)          -- pending 已清, 不再扫
assert(squareCalls == callsAfter, "no rescan when nothing changed")

-- 用例 3b: 库存窗口容器变化同样走防抖
callsBefore = squareCalls
handlers.onInventoryWindowChanged()
assert(squareCalls == callsBefore, "container window change must not rescan immediately")
fakeClock.now = fakeClock.now + 1000
handlers.onPlayerUpdate(player)
assert(squareCalls > callsBefore, "container window change rescans after 1s quiet")

-- 用例 3c: 移动刷新 —— 走够 5 格且距上次扫描 >= 2 秒才重扫
callsBefore = squareCalls
px = px + 3  -- 只走 3 格, 不触发
handlers.onPlayerUpdate(player)
assert(squareCalls == callsBefore, "movement < 5 tiles must not rescan")

px = px + 3  -- 累计 6 格, 但距上次扫描不足 2 秒
handlers.onPlayerUpdate(player)
assert(squareCalls == callsBefore, "movement must respect 2s cooldown")

fakeClock.now = fakeClock.now + 2100
handlers.onPlayerUpdate(player)  -- 走够 5 格且冷却已过 -> 重扫
assert(squareCalls > callsBefore, "movement >= 5 tiles and cooldown passed must rescan")
assert(EtherItemSearch.results ~= nil, "movement rescan keeps results")

-- 用例 4: 无匹配 -> n==0 且 results 清空 (手动扫描才打印诊断)
local n2 = EtherItemSearch.scan({ ["Base.Nope"] = true })
assert(n2 == 0, "expected 0 locations for unmatched target")

EtherItemSearch.clear()
assert(EtherItemSearch.results == nil, "clear() should reset results")
assert(EtherItemSearch.lastTargets == nil, "clear() should reset lastTargets")

print("PASS: scan logic smoke test")