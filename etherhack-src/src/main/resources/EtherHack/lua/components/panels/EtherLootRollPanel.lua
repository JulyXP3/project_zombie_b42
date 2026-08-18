require "ISUI/ISPanel"

--*********************************************************
--* 战利品重掷 + 刷弹药 面板 (clearContainerExplore POC, multiplayer)
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

    -- 重置战利品 标题
    self.sectionResetLabel = ISLabel:new(x, y, 20, getTranslate("UI_LootRoll_SectionReset"), EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b, 1, UIFont.Small, true);
    self.sectionResetLabel:initialise();
    self.sectionResetLabel:instantiate();
    self:addChild(self.sectionResetLabel);

    self.radiusLabel = ISLabel:new(x, y + 24, 20, getTranslate("UI_LootRoll_RadiusLabel"), 1, 1, 1, 1, UIFont.Medium, true);
    self.radiusLabel:initialise();
    self.radiusLabel:instantiate();
    self:addChild(self.radiusLabel);

    self.radiusBox = ISTextEntryBox:new(tostring(EtherContainerPOC.radius), x + 130, y + 22, 60, 20);
    self.radiusBox.font = UIFont.Small;
    self.radiusBox:initialise();
    self.radiusBox:instantiate();
    self.radiusBox:setClearButton(false)
    self:addChild(self.radiusBox);

    self.resetBtn = UIButton:new(x, y + 50, 150, 24, getTranslate("UI_LootRoll_Button"),
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

    self.hintLabel = ISLabel:new(x, y + 76, 15, getTranslate("UI_LootRoll_Hint"), 0.8, 0.8, 0.8, 1, UIFont.Small, true)
    self.hintLabel:initialise()
    self.hintLabel:instantiate()
    self:addChild(self.hintLabel)

    -- 刷弹药 标题
    self.sectionFarmLabel = ISLabel:new(x, y + 108, 20, getTranslate("UI_LootRoll_SectionFarm"), EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b, 1, UIFont.Small, true);
    self.sectionFarmLabel:initialise();
    self.sectionFarmLabel:instantiate();
    self:addChild(self.sectionFarmLabel);

    -- === 刷弹药 (ammo farming) — 紧贴"重置容器"下方, 单行: 自动刷(开) 自动刷(关) 设置弹药数 [N] ===
    local farmY = y + 132;
    local farmX = 15;   -- 与"重置战利品(F9)"左对齐
    local farmGap = 12;

    self.farmAutoBtn = UIButton:new(farmX, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmAuto"),
    function()
        local n = tonumber(self.ammoFarmBox:getInternalText());
        if n and n > 0 then setAmmoFarmCount(n) end
        farmSetAmmo();
        EtherAmmoFarm.enabled = true;
    end);
    self.farmAutoBtn:initialise();
    self.farmAutoBtn:instantiate();
    self:addChild(self.farmAutoBtn);
    farmX = farmX + self.farmAutoBtn.width + farmGap;

    self.farmStopBtn = UIButton:new(farmX, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmStop"),
    function()
        EtherAmmoFarm.enabled = false;
    end);
    self.farmStopBtn:initialise();
    self.farmStopBtn:instantiate();
    self:addChild(self.farmStopBtn);
    farmX = farmX + self.farmStopBtn.width + farmGap;

    self.farmSetBtn = UIButton:new(farmX, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmSet"),
    function()
        local n = tonumber(self.ammoFarmBox:getInternalText());
        if n and n > 0 then setAmmoFarmCount(n) end
        farmSetAmmo();
    end);
    self.farmSetBtn:initialise();
    self.farmSetBtn:instantiate();
    self:addChild(self.farmSetBtn);
    farmX = farmX + self.farmSetBtn.width + farmGap;

    self.ammoFarmBox = ISTextEntryBox:new(tostring(getAmmoFarmCount()), farmX, farmY + 2, 80, 20);
    self.ammoFarmBox.font = UIFont.Small;
    self.ammoFarmBox:initialise();
    self.ammoFarmBox:instantiate();
    self.ammoFarmBox:setClearButton(false)
    self:addChild(self.ammoFarmBox);
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