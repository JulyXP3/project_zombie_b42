require "ISUI/ISPanel"

--*********************************************************
--* 琚氳阿鑺枩閭阿瑜滆柂瑜樻 瑜嶈瑜岄偑钖姱鑳佹郴鎳?UI
--*********************************************************
EtherMapPanel = ISPanel:derive("EtherMapPanel"); -- 琚ч偑瑜嬭阿姊板啓鑺儊閭柂鎳堟 鑺 ISPanel

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?prerender
--*********************************************************
function EtherMapPanel:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?render
--*********************************************************
function EtherMapPanel:render()
    if self.mapCheckboxes ~= nil then
        for _, cb in ipairs(self.mapCheckboxes) do
            cb:setCheked(UIMap[cb.mapFlag] == true)
        end
    end
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then 
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end;
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?瑜嬭姱鏂滆瑜屾噲娉?娉昏姱璋㈡瑜嬫噲娉婚偑 灞戣瑜曟噲
--*********************************************************
function EtherMapPanel:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));

    if self:getMouseX() > 10 and self:getMouseY() > 10 and self:getMouseX() < self.map.width + 10 and self:getMouseY() < self.map.height + 10 then
        self.map:onMouseWheel(del);
    end 
	return true;
end

--*********************************************************
--* 琚涜姱鏂滈偑鑳佽阿姊拌柂鎳堟 瑜旀娉绘枩鑺郴瑜嬭姱鑳?
--*********************************************************
function EtherMapPanel:addCheckBox(title, method, isSelected, flagName)
    local rows = self.rows;
    local checkboxX = 10;
    local checkboxY = self.map.y + self.map.height + 20 + rows * 40;

    local checkbox = UICheckbox:new(checkboxX, checkboxY, title, isSelected, method);
    checkbox:initialise();
    checkbox:instantiate();
    checkbox:setAnchorLeft(true);
    checkbox:setAnchorRight(false);
    checkbox:setAnchorTop(false);
    checkbox:setAnchorBottom(true);
    if flagName ~= nil then
        checkbox.mapFlag = flagName;
        table.insert(self.mapCheckboxes, checkbox);
    end
    self:addChild(checkbox);

    self:setScrollHeight(self:getScrollHeight() + checkbox.height + 40);

    self.rows = self.rows + 1;

    table.insert(self.uiElements, checkbox);
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 灞戞瑜屾郴鎳?
--*********************************************************
function EtherMapPanel:addLabel(posX, posY, title)
    local label = ISLabel:new(posX, posY + 3, getTextManager():getFontHeight(UIFont.Small), title, 1, 1, 1, 1, UIFont.Small, true)
	self:addChild(label)
    return label
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 娉昏柂鑺攲娉绘噲
--*********************************************************
function EtherMapPanel:addButton(posX, posY, buttonTitle, onClick)
    local buttonWidth, buttonHeight = 260, 32;
    local button = UIButton:new(posX, posY, buttonWidth, buttonHeight, buttonTitle, onClick)
    button:initialise();
    button:instantiate();
    button:setAnchorLeft(true);
    button:setAnchorRight(false);
    button:setAnchorTop(false);
    button:setAnchorBottom(true);
    self:addChild(button);
    table.insert(self.uiElements, button);
    return button
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 娉昏柂鑺攲娉绘噲 瑜?锜归偑璋愯姱璋㈣姱鑳佹郴鑺睉
--*********************************************************
function EtherMapPanel:addButtonWithLabel(title, buttonTitle, func)
    local rows = self.rows;
    local buttonY = self.map.y + self.map.height + 20 + rows * 50;
    
    self:addLabel(10, buttonY - 3, title)
    local button = self:addButton(self:getWidth() - 260 - 40, buttonY, buttonTitle, func)

    self.rows = self.rows + 1;

    return button
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鍐欒姱瑜旀瑜夎柂鎳堣 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?
--*********************************************************
function EtherMapPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true)
    self:setScrollHeight(0)
    self:addScrollBars();

    if self.localPlayer == nil then return end;

    UIMap.ensureDrawFlags();

    self.map = UIMap:new(20, 20, self.width - 40, self.height - 400)
    self.map:initialise()
    self.map:instantiate()
    self.map:initDataAndStyle()
    self.map.mapAPI:resetView()
    self.map:restoreSettings()
    self:addChild(self.map)

    self:addButtonWithLabel(getTranslate("UI_Map_MiniMapOpenLabel"), getTranslate("UI_Map_MiniMapOpenButton"), function ()
        UIMovableMiniMap.openPanel()
    end)

    self:addCheckBox(getTranslate("UI_Map_DrawLocalPlayer"), function (isChecked)
        toggleMapDrawLocalPlayer(isChecked)
        UIMap.drawLocalPlayer = isChecked
    end, isMapDrawLocalPlayer(), "drawLocalPlayer")

    self:addCheckBox(getTranslate("UI_Map_DrawOtherPlayers"), function (isChecked)
        toggleMapDrawAllPlayers(isChecked)
        UIMap.drawAllPlayers = isChecked
    end, isMapDrawAllPlayers(), "drawAllPlayers")

    self:addCheckBox(getTranslate("UI_Map_DrawVehicles"), function (isChecked)
        toggleMapDrawVehicles(isChecked)
        UIMap.drawVehicles = isChecked
    end, isMapDrawVehicles(), "drawVehicles")

    self:addCheckBox(getTranslate("UI_Map_DrawZombies"), function (isChecked)
        toggleMapDrawZombies(isChecked)
        UIMap.drawZombies = isChecked
    end, isMapDrawZombies(), "drawZombies")

    self:addCheckBox(getTranslate("UI_Map_DrawItems"), function (isChecked)
        UIMap.drawItems = isChecked;
        EtherItemSearch.setEnabled(isChecked);
    end, UIMap.drawItems, "drawItems")

end
--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 钖姱鑳佽姱璋愯姱 瑜濇郴锜规灞戦攲璋㈣瑜夐偑 灞戞钖
--*********************************************************
function EtherMapPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_Map_PanelWorkOnlyInGame");
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    self.uiElements = {};
    self.rows = 0;
    self.mapCheckboxes = {};

    return menuTableData;
end