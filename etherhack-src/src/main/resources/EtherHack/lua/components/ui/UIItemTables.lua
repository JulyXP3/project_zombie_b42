require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIItemTables = ISPanel:derive("UIItemTables");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Обработка render
--*********************************************************
function UIItemTables:render()
    ISPanel.render(self);
    
    local y = self.datas.y + self.datas.height + 5
    self:drawText(getText("IGUI_DbViewer_TotalResult") .. self.totalResult, 0, y, 1,1,1,1,UIFont.Small)

    -- 按钮显隐: 搜索有结果, 或直接选中了某项物品时显示 (每帧计算, 覆盖列表点击)
    self:refreshShowOnMap();
end

--*********************************************************
--* "在地图上显示"按钮显隐: 搜索有结果, 或直接选中了某项物品时显示 (每帧计算, 覆盖列表点击)
--*********************************************************
function UIItemTables:refreshShowOnMap()
    local hasFilter = false;
    for j = 1, #self.filterWidgets do
        if self.filterWidgets[j]:getInternalText() ~= "" then
            hasFilter = true;
            break;
        end
    end
    local show = false;
    if hasFilter then
        show = self.totalResult > 0;
    else
        show = self.datas.selected > 0 and self.datas.items ~= nil and self.datas.selected <= #self.datas.items;
    end
    if show ~= self.showOnMap:getIsVisible() then
        self.showOnMap:setVisible(show);
        if not show then
            EtherItemSearch.clear();
        end
    end
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
EtherContainerPOC = EtherContainerPOC or { radius = 10 }
-- F10 快捷键与「战利品重掷」选项卡共用此入口

-- 重置附近容器战利品: 服务端 clearContainerExplore 无校验地
-- 清空 explored 标记 + 房间程序化生成记录 -> 再次搜索重新 roll 战利品
-- 半径可调: 选项卡输入框 / ` 键 Lua 控制台(需 -debug 启动) EtherContainerPOC.radius = 2
function EtherContainerPOC.reset()
    local player = getPlayer();
    if player == nil then return end
    local R = EtherContainerPOC.radius or 10;
    local ok, err = pcall(function()
        local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ());
        local count = 0;
        for dy = -R, R do
            for dx = -R, R do
                local sq = getCell():getGridSquare(px + dx, py + dy, pz);
                if sq ~= nil then
                    local objects = sq:getObjects();
                    for i = 0, objects:size() - 1 do
                        local obj = objects:get(i);
                        if obj ~= nil then
                            local nCont = obj:getContainerCount();
                            for ci = 0, nCont - 1 do
                                local c = obj:getContainerByIndex(ci);
                                if c ~= nil and c:getSourceGrid() ~= nil and c:getSourceGrid():getRoom() ~= nil then
                                    c:setExplored(false); -- 客户端本地标记也要清, 否则打开时跳过向服务端请求, 只显示旧缓存
                                    sendClientCommand(player, "object", "clearContainerExplore", {
                                        x = sq:getX(), y = sq:getY(), z = sq:getZ(),
                                        index = i, containerIndex = ci,
                                    });
                                    count = count + 1;
                                end
                            end
                        end
                    end
                end
            end
        end
        print("[ContainerPOC] reset " .. tostring(count) .. " containers nearby, search them for fresh loot");
    end)
    if not ok then
        print("[ContainerPOC] failed: " .. tostring(err));
    end
end

function UIItemTables:createChildren()
    ISPanel.createChildren(self);

    self.datas = ISScrollingListBox:new(0, 25, self.width, self.height - 150);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = fontHeightSmall + 4 * 2
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.drawBorder = true;
    self.datas:addColumn(getTranslate("UI_ItemCreator_Title_ItemName"), 0);
    self.datas:addColumn(getTranslate("UI_ItemCreator_Title_ItemCategory"), 250)
    self:addChild(self.datas);

    self.filterByNameTitle = ISLabel:new(0, self.height - 40, 20, getTranslate("UI_ItemCreator_Title_FilterByName"), 1, 1, 1, 1, UIFont.Medium, true)
    self.filterByNameTitle:initialise()
    self.filterByNameTitle:instantiate()
    self:addChild(self.filterByNameTitle)

    self.filterByName = ISTextEntryBox:new("", 0, self.height - 20, self.width / 2 - 10, 20);
    self.filterByName.font = UIFont.Small;
    self.filterByName:initialise();
    self.filterByName:instantiate();
    self.filterByName.target = self;
    self.filterByName.itemsListFilter = self.filterName;
    self.filterByName.onTextChange = UIItemTables.onFilterChange;
    self.filterByName:setClearButton(true)
    self:addChild(self.filterByName);
    table.insert(self.filterWidgets, self.filterByName);

    self.filterByIdTitle = ISLabel:new(self.width / 2, self.height - 40, 20, getTranslate("UI_ItemCreator_Title_FilterById"), 1, 1, 1, 1, UIFont.Medium, true)
    self.filterByIdTitle:initialise()
    self.filterByIdTitle:instantiate()
    self:addChild(self.filterByIdTitle)

    self.filterById = ISTextEntryBox:new("", self.width / 2, self.height - 20, self.width / 2, 20);
    self.filterById.font = UIFont.Small;
    self.filterById:initialise();
    self.filterById:instantiate();
    self.filterById:setClearButton(true)
    self.filterById.target = self;
    self.filterById.itemsListFilter = self.filterType;
    self.filterById.onTextChange = UIItemTables.onFilterChange;
    self:addChild(self.filterById);
    table.insert(self.filterWidgets, self.filterById);

    self.addItemX1 = UIButton:new(0, self.height - 80, 100, 24, getTranslate("UI_ItemCreator_Button_AddItemX1"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        spawnItem(item:getFullName(), 1);
    end)
    self.addItemX1:initialise();
    self.addItemX1:instantiate();
    self.addItemX1:setAnchorLeft(true);
    self.addItemX1:setAnchorRight(false);
    self.addItemX1:setAnchorTop(false);
    self.addItemX1:setAnchorBottom(true);
    self.addItemX1.isOnlyInGame = true;
    self:addChild(self.addItemX1);
    table.insert(self.buttonList, self.addItemX1);

    self.addItemX2 = UIButton:new(self.addItemX1:getX() + self.addItemX1.width + 10, self.height - 80, 100, 24, getTranslate("UI_ItemCreator_Button_AddItemX2"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        spawnItem(item:getFullName(), 2);
    end)
    self.addItemX2:initialise();
    self.addItemX2:instantiate();
    self.addItemX2:setAnchorLeft(true);
    self.addItemX2:setAnchorRight(false);
    self.addItemX2:setAnchorTop(false);
    self.addItemX2:setAnchorBottom(true);
    self.addItemX2.isOnlyInGame = true;
    self:addChild(self.addItemX2);
    table.insert(self.buttonList, self.addItemX2);

    self.addItemX5 = UIButton:new(self.addItemX2:getX() + self.addItemX2.width + 10, self.height - 80, 100, 24, getTranslate("UI_ItemCreator_Button_AddItemX5"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        spawnItem(item:getFullName(), 5);
    end)
    self.addItemX5:initialise();
    self.addItemX5:instantiate();
    self.addItemX5:setAnchorLeft(true);
    self.addItemX5:setAnchorRight(false);
    self.addItemX5:setAnchorTop(false);
    self.addItemX5:setAnchorBottom(true);
    self.addItemX5.isOnlyInGame = true;
    self:addChild(self.addItemX5);
    table.insert(self.buttonList, self.addItemX5);

    self.addItemX10 = UIButton:new(self.addItemX5:getX() + self.addItemX5.width + 10, self.height - 80, 100, 24, getTranslate("UI_ItemCreator_Button_AddItemX10"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        spawnItem(item:getFullName(), 10);
    end)
    self.addItemX10:initialise();
    self.addItemX10:instantiate();
    self.addItemX10:setAnchorLeft(true);
    self.addItemX10:setAnchorRight(false);
    self.addItemX10:setAnchorTop(false);
    self.addItemX10:setAnchorBottom(true);
    self.addItemX10.isOnlyInGame = true;
    self:addChild(self.addItemX10);
    table.insert(self.buttonList, self.addItemX10);

    self.showOnMap = UIButton:new(self.addItemX10:getX() + self.addItemX10.width + 10, self.height - 80, 150, 24, getTranslate("UI_ItemSearch_ShowOnMap"), 
    function() 
        -- 搜索词非空: 扫描当前过滤列表里的全部物品; 否则: 扫描选中的单个物品
        local hasFilter = false;
        for j = 1, #self.filterWidgets do
            if self.filterWidgets[j]:getInternalText() ~= "" then
                hasFilter = true;
                break;
            end
        end

        local targetTypes = {};
        local nTargets = 0;
        if hasFilter then
            if self.datas.items == nil or #self.datas.items == 0 then return end
            for i = 1, #self.datas.items do
                local scriptItem = self.datas.items[i].item;
                if scriptItem ~= nil then
                    targetTypes[scriptItem:getFullName()] = true;
                    nTargets = nTargets + 1;
                end
            end
        else
            local sel = self.datas.selected;
            if self.datas.items == nil or sel < 1 or sel > #self.datas.items then return end
            local scriptItem = self.datas.items[sel].item;
            if scriptItem ~= nil then
                targetTypes[scriptItem:getFullName()] = true;
                nTargets = nTargets + 1;
            end
        end
        if nTargets == 0 then return end

        self.showOnMap.title = getTranslate("UI_ItemSearch_Scanning");
        EtherItemSearch.scan(targetTypes);
        self.showOnMap.title = getTranslate("UI_ItemSearch_ShowOnMap");
    end)
    self.showOnMap:initialise();
    self.showOnMap:instantiate();
    self.showOnMap:setVisible(false);
    self.showOnMap.isOnlyInGame = true;
    self:addChild(self.showOnMap);
    table.insert(self.buttonList, self.showOnMap);

    self:updatePanel();
end

--*********************************************************
--* Обновление панели
--*********************************************************
function UIItemTables:updatePanel()
    for i=1, #self.buttonList do
        local item = self.buttonList[i];
        if item.isOnlyInGame and getPlayer() == nil or getPlayer():isDead() then
            item:setEnable(false);
        end
    end
end


--*********************************************************
--* Инициализация списков
--*********************************************************
function UIItemTables:initList(module)
    self.totalResult = 0;
    local displayCategoryNames = {}
    local displayCategoryMap = {}
    for _, v in ipairs(module) do
        self.datas:addItem(v:getDisplayName(), v);
        if not displayCategoryMap[v:getDisplayCategory()] then
            displayCategoryMap[v:getDisplayCategory()] = true
            table.insert(displayCategoryNames, v:getDisplayCategory())
        end
        self.totalResult = self.totalResult + 1;
    end
    table.sort(self.datas.items, function(a,b) return not string.sort(a.item:getDisplayName(), b.item:getDisplayName()); end);
end

--*********************************************************
--* Обновление таблицы
--*********************************************************
function UIItemTables:update()
    self.datas.doDrawItem = self.drawDatas;
end

--*********************************************************
--* Фильтр по названию
--*********************************************************
function UIItemTables:filterName(widget, scriptItem)
    local txtToCheck = string.lower(scriptItem:getDisplayName())
    local filterTxt = string.lower(widget:getInternalText())
    return checkStringPattern(filterTxt) and string.match(txtToCheck, filterTxt)
end

--*********************************************************
--* Фильтр по ID
--*********************************************************
function UIItemTables:filterType(widget, scriptItem)
    local txtToCheck = string.lower(scriptItem:getName())
    local filterTxt = string.lower(widget:getInternalText())
    return checkStringPattern(filterTxt) and string.match(txtToCheck, filterTxt)
end

--*********************************************************
--* Применение фильтра при написании текста
--*********************************************************
function UIItemTables.onFilterChange(widget)
    local datas = widget.parent.datas;
    if not datas.fullList then datas.fullList = datas.items; end
    widget.parent.totalResult = 0;
    datas:clear();
    for i,v in ipairs(datas.fullList) do -- check every items
        local add = true;
        for j,widget in ipairs(widget.parent.filterWidgets) do -- check every filters
            if not widget.itemsListFilter(self, widget, v.item) then
                add = false
                break
            end
        end
        if add then
            datas:addItem(i, v.item);
            widget.parent.totalResult = widget.parent.totalResult + 1;
        end
    end

    -- 按钮显隐统一交给 render() 里的 refreshShowOnMap 计算; 清空搜索词时清除标记
    local hasFilter = false;
    for j = 1, #widget.parent.filterWidgets do
        if widget.parent.filterWidgets[j]:getInternalText() ~= "" then
            hasFilter = true;
            break;
        end
    end
    if not hasFilter then
        EtherItemSearch.clear();
    end
end

--*********************************************************
--* Отрисовка данных
--*********************************************************
function UIItemTables:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end
    
    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)

    local iconX = 4
    local iconSize = fontHeightSmall;

    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)
    
    self:setStencilRect(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    self:drawText(item.item:getDisplayName(), 25, y + 4, 1, 1, 1, a, self.font);
    self:clearStencilRect()

    if item.item:getDisplayCategory() ~= nil then
        self:drawText(getText("IGUI_ItemCat_" .. item.item:getDisplayCategory()), self.columns[2].size + 10, y + 4, 1, 1, 1, a, self.font);
    else
        self:drawText("<NONE>", self.columns[2].size + 10, y + 4, 1, 1, 1, a, self.font);
    end
    
    self:repaintStencilRect(0, clipY, self.width - 20, clipY2 - clipY)

    local icon = item.item:getIcon()
    if item.item:getIconsForTexture() and not item.item:getIconsForTexture():isEmpty() then
        icon = item.item:getIconsForTexture():get(0)
    end
    if icon then
        local texture = getTexture("Item_" .. icon)
        if texture then
            self:drawTextureScaledAspect2(texture, self.columns[1].size + iconX, y + (self.itemheight - iconSize) / 2, iconSize, iconSize,  1, 1, 1, 1);
        end
    end
    
    return y + self.itemheight;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIItemTables:new (x, y, width, height)
    local menuTableData = ISPanel:new(x, y, width, height);
    setmetatable(menuTableData, self);
    menuTableData.listHeaderColor = {r=0.12, g=0.05, b=0.05, a=1.0};
    menuTableData.borderColor = EtherTheme.bloodDim;
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.buttonBorderColor = {r=0.7, g=0.7, b=0.7, a=0.0};
    menuTableData.totalResult = 0;
    menuTableData.filterWidgets = {};
    menuTableData.buttonList = {};
    UIItemTables.instance = menuTableData;
    return menuTableData;
end