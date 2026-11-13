--*********************************************************
--* 多选追踪清单 (2026-11-12):
--*   只维护"要追踪哪些物品全名"的集合, 不统计背包数量。
--*   供物品雷达页「加入清单」写入 / 「清单」弹窗移除 / 「追踪清单」使用。
--*   编码格式: "Base.A;Base.B" (分号分隔, 顺序 = 加入顺序)。
--*   注意: 不做物品存在性校验 (面板拿到全物品库后再过滤)。
--*********************************************************
EtherWatchlist = EtherWatchlist or {}

EtherWatchlist.MAX_TYPES = 50
EtherWatchlist.list = {}   -- 顺序数组
EtherWatchlist.set = {}    -- fullType -> true

local function validName(fullType)
    if type(fullType) ~= "string" then return false end
    if fullType == "" or #fullType > 200 then return false end
    if string.find(fullType, ";", 1, true) ~= nil then return false end
    return true
end

local function add(fullType)
    if EtherWatchlist.set[fullType] then return true end
    if #EtherWatchlist.list >= EtherWatchlist.MAX_TYPES then return false end
    table.insert(EtherWatchlist.list, fullType)
    EtherWatchlist.set[fullType] = true
    return true
end

function EtherWatchlist.clear()
    EtherWatchlist.list = {}
    EtherWatchlist.set = {}
end

function EtherWatchlist.load(encoded)
    EtherWatchlist.clear()
    if type(encoded) ~= "string" then return end
    local start = 1
    while start <= #encoded do
        local sep = string.find(encoded, ";", start, true)
        local part
        if sep == nil then
            part = string.sub(encoded, start)
            start = #encoded + 1
        else
            part = string.sub(encoded, start, sep - 1)
            start = sep + 1
        end
        local name = string.gsub(part, "^%s+", "")
        name = string.gsub(name, "%s+$", "")
        if validName(name) then add(name) end
    end
end

function EtherWatchlist.encode()
    return table.concat(EtherWatchlist.list, ";")
end

function EtherWatchlist.contains(fullType)
    return EtherWatchlist.set[fullType] == true
end

function EtherWatchlist.count()
    return #EtherWatchlist.list
end

function EtherWatchlist.at(index)
    return EtherWatchlist.list[index]
end

function EtherWatchlist.removeAt(index)
    local name = EtherWatchlist.list[index]
    if name == nil then return false end
    table.remove(EtherWatchlist.list, index)
    EtherWatchlist.set[name] = nil
    return true
end

function EtherWatchlist.isFull()
    return #EtherWatchlist.list >= EtherWatchlist.MAX_TYPES
end

-- 返回 added / removed / full / bad
function EtherWatchlist.toggle(fullType)
    if not validName(fullType) then return "bad" end
    if EtherWatchlist.set[fullType] then
        EtherWatchlist.set[fullType] = nil
        for i = #EtherWatchlist.list, 1, -1 do
            if EtherWatchlist.list[i] == fullType then
                table.remove(EtherWatchlist.list, i)
            end
        end
        return "removed"
    end
    if not add(fullType) then return "full" end
    return "added"
end

-- 给 EtherItemSearch.startScan 的目标集合 { [fullType]=true }
function EtherWatchlist.targets()
    local t = {}
    for i = 1, #EtherWatchlist.list do
        t[EtherWatchlist.list[i]] = true
    end
    return t
end
