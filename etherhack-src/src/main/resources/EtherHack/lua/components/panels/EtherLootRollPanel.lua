require "ISUI/ISPanel"

--*********************************************************
--* 战利品重掷面板 (clearContainerExplore POC, multiplayer)
--* 服务端 object.clearContainerExplore 无权限/距离校验:
--* 任意客户端可清空容器「已探索」标记 + 房间程序化生成记录,
--* 之后打开容器由服务端按容器类型表重新 roll 战利品
--* (枪柜/军用包等表含武器弹药)。
--* 复用 EtherContainerPOC (UIItemTables.lua 定义, F10 同入口)。
--*********************************************************
EtherLootRollPanel = ISPanel:derive("EtherLootRollPanel");

--*********************************************************
--* Обработка prerender
--*********************************************************
function EtherLootRollPanel:prerender()
    self:setStencilRect(0, 10, self:getWidth(), self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* Обработка render
--*********************************************************
function EtherLootRollPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    local status = "radius=" .. tostring(EtherContainerPOC.radius);
    self:drawText("[ContainerPOC] " .. status, 15, self.height - 22, EtherTheme.textDim.r, EtherTheme.textDim.g, EtherTheme.textDim.b, 1, UIFont.Small);
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherLootRollPanel:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end

    local x = 15;
    local y = 14;

    self.radiusLabel = ISLabel:new(x, y, 20, getTranslate("UI_LootRoll_RadiusLabel"), 1, 1, 1, 1, UIFont.Medium, true);
    self.radiusLabel:initialise();
    self.radiusLabel:instantiate();
    self:addChild(self.radiusLabel);

    self.radiusBox = ISTextEntryBox:new(tostring(EtherContainerPOC.radius), x + 130, y - 2, 60, 20);
    self.radiusBox.font = UIFont.Small;
    self.radiusBox:initialise();
    self.radiusBox:instantiate();
    self.radiusBox:setClearButton(false)
    self:addChild(self.radiusBox);

    self.resetBtn = UIButton:new(x, y + 40, 150, 24, getTranslate("UI_LootRoll_Button"),
    function()
        if not isMultiplayer() then
            print("[ContainerPOC] multiplayer only (use your own dedicated server)")
            return
        end
        local r = tonumber(self.radiusBox:getInternalText());
        if r ~= nil and r >= 1 and r <= 100 then
            EtherContainerPOC.radius = math.floor(r);
        end
        EtherContainerPOC.reset();
    end);
    self.resetBtn:initialise();
    self.resetBtn:instantiate();
    self.resetBtn.isOnlyInGame = true;
    self:addChild(self.resetBtn);

    self.hintLabel = ISLabel:new(x, y + 72, 15, getTranslate("UI_LootRoll_Hint"), 0.8, 0.8, 0.8, 1, UIFont.Small, true)
    self.hintLabel:initialise()
    self.hintLabel:instantiate()
    self:addChild(self.hintLabel)
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherLootRollPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_LootRoll_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    return menuTableData;
end