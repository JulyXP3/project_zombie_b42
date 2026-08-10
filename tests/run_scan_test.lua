-- EtherItemSearch.scan 逻辑冒烟测试 (桩模拟游戏 API)
-- 运行: lua5.1.exe run_scan_test.lua
local function fakeList(...)
    local arr = { ... }
    return {
        size = function() return #arr end,
        get = function(_, i) return arr[i + 1] end,
    }
end

local playerInv = { tag = "playerInv" }
local player = { getInventory = function() return playerInv end }

local function fakeItem(fullType, container)
    return { getFullType = function() return fullType end, getContainer = function() return container end }
end

local function fakeSquare(x, y)
    return { getX = function() return x end, getY = function() return y end }
end

local function fakeContainer(tag, sq, inPlayerInv)
    return {
        tag = tag,
        isInCharacterInventory = function(_, _p) return inPlayerInv end,
        getSquare = function() return sq end,
    }
end

local function fakeWorldItem(fullType, x, y)
    return {
        getItem = function() return fakeItem(fullType, nil) end,
        getX = function() return x end,
        getY = function() return y end,
    }
end

local worldSq10_20   = fakeSquare(10, 20)
local worldSq30_40   = fakeSquare(30, 40)
local playerSq       = fakeSquare(999, 999)

local playerInv = fakeContainer("playerInv", playerSq, true)
local worldContainer = fakeContainer("world", worldSq10_20, false)

local containerItems = fakeList(
    fakeItem("Base.Axe", worldContainer),                 -- 命中 (10,20)
    fakeItem("Base.Axe", worldContainer),                 -- 同格堆叠 (10,20)
    fakeItem("Base.Axe", fakeContainer("c2", worldSq30_40, false)), -- 命中 (30,40)
    fakeItem("Base.Axe", fakeContainer("c3", playerSq, true)),      -- 玩家背包 -> 排除
    fakeItem("Base.Axe", nil),                            -- 无容器 -> 跳过
    fakeItem("Base.Saw", worldContainer)                  -- 类型不匹配 -> 忽略
)

local worldItems = fakeList(
    fakeWorldItem("Base.Axe", 50.4, 60.7),                -- 地面命中 -> 取整 (50,60)
    fakeWorldItem("Base.Pen", 70, 80)                     -- 不匹配 -> 忽略
)

-- 桩全局环境 (在加载被测模块前定义)
getCell = function()
    return {
        getProcessItems = function() return containerItems end,
        getProcessWorldItems = function() return worldItems end,
    }
end
getPlayer = function() return player end

dofile(arg[1])  -- EtherItemSearch.lua

local target = { ["Base.Axe"] = true }
local n = EtherItemSearch.scan(target)

assert(n == 3, "expected 3 locations, got " .. n)
assert(#EtherItemSearch.results == 3, "expected 3 results entries")

local byKey = {}
for _, p in ipairs(EtherItemSearch.results) do
    byKey[p.x .. "," .. p.y] = p.count
end
assert(byKey["10,20"] == 2, "10,20 should have count 2")
assert(byKey["30,40"] == 1, "30,40 should have count 1")
assert(byKey["50,60"] == 1, "50,60 should have count 1 (floor rounded)")

EtherItemSearch.clear()
assert(EtherItemSearch.results == nil, "clear() should reset results")

print("PASS: scan logic smoke test")