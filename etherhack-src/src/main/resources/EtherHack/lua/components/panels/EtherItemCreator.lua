require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherItemCreator = ISPanel:derive("EtherItemCreator"); -- Наследование от ISPanel

--*********************************************************
--* Обработка prerender
--*********************************************************
function EtherItemCreator:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* Обработка render
--*********************************************************
function EtherItemCreator:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then 
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherItemCreator:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end;

    self:initList();
end

--*********************************************************
--* Инициализация таблиц с предметами
--* 2026-08-30 重构: 移除按模块分页的 ISTabPanel —— 原实现为每个模块各建
--* 一套完整 UIItemTables (列表+按钮+过滤), "All" 视图再把全库建一遍,
--* 数千条 ×2 次填充+排序是物品页点开卡顿的另一半来源; 雷达相关功能
--* 已迁「雷达」选项卡。现在只有一个全库列表 (耕种-播种同款展示)。
--*********************************************************
function EtherItemCreator:initList()
    local items = getAllItems();
    local allItems = {}
    for i=0,items:size()-1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() then
            table.insert(allItems, item)
        end
    end

    local listBox = UIItemTables:new(0, 0, self.width, self.height);
    listBox:initialise();
    self:addChild(listBox);
    listBox:initList(allItems);
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherItemCreator:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_ItemCreator_PanelWorkOnlyInGame");
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    return menuTableData;
end