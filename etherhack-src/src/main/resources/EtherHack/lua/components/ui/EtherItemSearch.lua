--*********************************************************
--* 物品搜索模块: 扫描世界物品注册表, 缓存匹配物品的位置
--* 数据源: getCell():getProcessItems() (容器内物品)
--*         getCell():getProcessWorldItems() (地面散落物)
--* 坐标: 容器通过 getSquare() 解析最终所在方格; 地面物品用自身坐标
--*********************************************************
EtherItemSearch = EtherItemSearch or {};

EtherItemSearch.results = nil; -- { {x=.., y=.., count=..}, ... } 命中位置列表

--*********************************************************
--* 扫描: targetTypes = { ["Base.Axe"]=true, ... }
--* 返回命中位置数; 结果存到 EtherItemSearch.results
--*********************************************************
function EtherItemSearch.scan(targetTypes)
    local cell = getCell();
    if cell == nil or targetTypes == nil then
        EtherItemSearch.results = nil;
        return 0;
    end

    local player = getPlayer();
    local out, key, n = {}, {}, 0;

    local function addAt(x, y)
        local k = math.floor(x) * 100000 + math.floor(y);
        if key[k] ~= nil then
            out[key[k]].count = out[key[k]].count + 1;
        else
            n = n + 1;
            key[k] = n;
            out[n] = { x = math.floor(x), y = math.floor(y), count = 1 };
        end
    end

    -- 容器内物品 (先比对类型, 命中才解析坐标, 保证性能)
    local items = cell:getProcessItems();
    if items ~= nil then
        for i = 1, items:size() do
            local item = items:get(i - 1);
            if item ~= nil and targetTypes[item:getFullType()] then
                local c = item:getContainer();
                if c ~= nil and (player == nil or not c:isInCharacterInventory(player)) then
                    local sq = c:getSquare();
                    if sq ~= nil then
                        addAt(sq:getX(), sq:getY());
                    end
                end
            end
        end
    end

    -- 地面散落物品
    local witems = cell:getProcessWorldItems();
    if witems ~= nil then
        for i = 1, witems:size() do
            local w = witems:get(i - 1);
            if w ~= nil then
                local item = w:getItem();
                if item ~= nil and targetTypes[item:getFullType()] then
                    addAt(w:getX(), w:getY());
                end
            end
        end
    end

    EtherItemSearch.results = out;
    return n;
end

--*********************************************************
--* 清除搜索结果 (标记不再显示)
--*********************************************************
function EtherItemSearch.clear()
    EtherItemSearch.results = nil;
end
