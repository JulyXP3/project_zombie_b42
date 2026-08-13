require "ISUI/ISPanel"

--*********************************************************
--* 陷阱刷物品面板 (trap-spawn items, multiplayer)
--* 列出所有陷阱链能生成的物品 (食物, hungerChange < 0),
--* 搜索 + 点击生成; 需站在已放置的陷阱旁。
--*********************************************************
EtherTrapSpawn = ISPanel:derive("EtherTrapSpawn");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Обработка prerender
--*********************************************************
function EtherTrapSpawn:prerender()
    self:setStencilRect(0, 10, self:getWidth(), self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* Обработка render
--*********************************************************
function EtherTrapSpawn:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    local status = "idle";
    if EtherTrapPOC.armed then
        status = tostring(EtherTrapPOC.target) .. " x" .. tostring(EtherTrapPOC.count) .. " [" .. tostring(EtherTrapPOC.phase) .. "]";
    end
    self:drawText("[TrapPOC] " .. status, 15, self.height - 22, EtherTheme.textDim.r, EtherTheme.textDim.g, EtherTheme.textDim.b, 1, UIFont.Small);
end

--*********************************************************
--* Фильтр по названию
--*********************************************************
function EtherTrapSpawn:applyFilter()
    local filterTxt = string.lower(self.searchBox:getInternalText());
    local full = self.fullList;
    self.datas:clear();
    self.totalResult = 0;
    for i, v in ipairs(full) do
        local name = string.lower(v.item:getDisplayName());
        if filterTxt == "" or (checkStringPattern(filterTxt) and string.match(name, filterTxt)) then
            self.datas:addItem(i, v.item);
            self.totalResult = self.totalResult + 1;
        end
    end
end

--*********************************************************
--* Отрисовка данных
--*********************************************************
function EtherTrapSpawn:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local iconSize = fontHeightSmall;
    self:drawText(item.item:getDisplayName(), 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);

    local icon = item.item:getIcon()
    if item.item:getIconsForTexture() and not item.item:getIconsForTexture():isEmpty() then
        icon = item.item:getIconsForTexture():get(0)
    end
    if icon then
        local texture = getTexture("Item_" .. icon)
        if texture then
            self:drawTextureScaledAspect2(texture, 4, y + (self.itemheight - iconSize) / 2, iconSize, iconSize, 1, 1, 1, 1);
        end
    end

    return y + self.itemheight;
end

--*********************************************************
--* Инициализация списка (только食物: hungerChange < 0)
--*********************************************************
function EtherTrapSpawn:initList()
    local items = getAllItems();
    local foodList = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() and item:getHungerChange() < 0 then
            table.insert(foodList, { item = item });
        end
    end
    table.sort(foodList, function(a, b) return not string.sort(a.item:getDisplayName(), b.item:getDisplayName()); end);

    self.fullList = foodList;
    self.totalResult = 0;
    self.datas:clear();
    for i, v in ipairs(foodList) do
        self.datas:addItem(v.item:getDisplayName(), v.item);
        self.totalResult = self.totalResult + 1;
    end
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherTrapSpawn:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end;

    self.searchLabel = ISLabel:new(15, 14, 20, getTranslate("UI_TrapSpawn_SearchLabel"), 1, 1, 1, 1, UIFont.Medium, true)
    self.searchLabel:initialise()
    self.searchLabel:instantiate()
    self:addChild(self.searchLabel)

    self.searchBox = ISTextEntryBox:new("", 100, 12, 220, 20);
    self.searchBox.font = UIFont.Small;
    self.searchBox:initialise();
    self.searchBox:instantiate();
    self.searchBox:setClearButton(true)
    self.searchBox.onTextChange = function()
        EtherTrapSpawn.applyFilter(self)
    end
    self:addChild(self.searchBox);

    self.datas = ISScrollingListBox:new(15, 44, self.width - 30, self.height - 150);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = fontHeightSmall + 4 * 2
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.drawBorder = true;
    EtherTheme.styleList(self.datas);
    self.datas:addColumn("", self.width - 30);
    self:addChild(self.datas);

    self:initList();

    self.spawnBtn = UIButton:new(15, self.height - 66, 140, 24, getTranslate("UI_TrapSpawn_Button"), 
    function() 
        local sel = self.datas.selected;
        if self.datas.items == nil or sel < 1 or sel > #self.datas.items then
            print("[TrapPOC] select an item first")
            return
        end
        local scriptItem = self.datas.items[sel].item;
        if scriptItem == nil then return end
        EtherTrapPOC.setTarget(scriptItem:getFullName());
        EtherTrapPOC.count = 1;
        EtherTrapPOC.trigger();
    end)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    self.hintLabel = ISLabel:new(165, self.height - 63, 15, getTranslate("UI_TrapSpawn_Hint"), 0.8, 0.8, 0.8, 1, UIFont.Small, true)
    self.hintLabel:initialise()
    self.hintLabel:instantiate()
    self:addChild(self.hintLabel)
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherTrapSpawn:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_TrapSpawn_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    return menuTableData;
end
