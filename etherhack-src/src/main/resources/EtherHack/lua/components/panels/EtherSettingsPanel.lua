require "ISUI/ISPanel"

--*********************************************************
--* 琚氳阿鑺枩閭阿瑜滆柂瑜樻 瑜嶈瑜岄偑钖姱鑳佹郴鎳?UI
--*********************************************************
EtherSettingsPanel = ISPanel:derive("EtherSettingsPanel"); -- 琚ч偑瑜嬭阿姊板啓鑺儊閭柂鎳堟 鑺 ISPanel

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 灞戞瑜屾郴鎳?
--*********************************************************
function EtherSettingsPanel:addLabel(posX, posY, title)
    local label = ISLabel:new(posX, posY + 3, getTextManager():getFontHeight(UIFont.Small), title, 1, 1, 1, 1, UIFont.Small, true)
	self:addChild(label)
    return label
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 娉昏柂鑺攲娉绘噲
--*********************************************************
function EtherSettingsPanel:addButton(posX, posY, buttonTitle, onClick, isOnlyNotInGame)
    local buttonWidth, buttonHeight = 260, 32;
    local button = UIButton:new(posX, posY, buttonWidth, buttonHeight, buttonTitle, onClick)
    button:initialise();
    button:instantiate();
    button:setAnchorLeft(true);
    button:setAnchorRight(false);
    button:setAnchorTop(false);
    button:setAnchorBottom(true);
    button.isOnlyNotInGame = isOnlyNotInGame;
    self:addChild(button);
    table.insert(self.buttonList, button);
    return button
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 瑜嬭阿閭硠鍐欐瑜夐偑
--*********************************************************
function EtherSettingsPanel:addSlider(posX, posY, width, height, value, minValue, maxValue, method)
    local slider = UISlider:new(posX, posY, width, height, value, minValue, maxValue, method)
    slider:initialise();
    slider:instantiate();
    self:addChild(slider);
    return slider
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 娉昏柂鑺攲娉绘噲 瑜?锜归偑璋愯姱璋㈣姱鑳佹郴鑺睉
--*********************************************************
function EtherSettingsPanel:addButtonWithLabel(title, buttonTitle, func, isOnlyNotInGame)
    local rows = self.rows;
    local buttonY = 400 + rows * 50;

    self:addLabel(10, buttonY - 3, title)
    self:addButton(self:getWidth() - 260 - 20, buttonY, buttonTitle, func, isOnlyNotInGame)

    self:setScrollHeight(self:getScrollHeight() + 82);
    self.rows = self.rows + 1;
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鑳佽鏂滆姱瑜夐偑 瑜戣儊姊拌閭?瑜?锜归偑璋愯姱璋㈣姱鑳佹郴鑺睉
--*********************************************************
function EtherSettingsPanel:addColorPickerWithLabel(title, func, startColor)
    local rows = self.rows;
    local buttonY = 400 + rows * 50;

    self:addLabel(10, buttonY - 3, title)

    local buttonWidth, buttonHeight = 32, 32;
    local button = ISButton:new(self:getWidth() - buttonWidth - 20, buttonY, buttonWidth, buttonHeight, "", self, func)
    button:initialise();
    button.backgroundColor = {r = startColor:getR(), g = startColor:getG(), b = startColor:getB(), a = 1};
	button.backgroundColorMouseOver = {r = startColor:getR(), g = startColor:getG(), b = startColor:getB(), a = 1};

    self:addChild(button);
    table.insert(self.buttonList, button);

    self:setScrollHeight(self:getScrollHeight() + 82);
    self.rows = self.rows + 1;
    return button
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 锜归偑璋愯姱璋㈣姱鑳佹郴閭?瑜?鍐欒儊瑜嶅睉瑜?娉昏柂鑺攲娉婚偑灞戞噲
--*********************************************************
function EtherSettingsPanel:addSliderWithLabel(title, sliderMethod, value, minValue, maxValue)
    local rows = self.rows;
    local buttonY = 20 + rows * 50;

    self:addLabel(15, buttonY - 3, title)
    self:addSlider(150, buttonY + 3, self:getWidth() - 160, 20, value, minValue, maxValue, sliderMethod)

    self:setScrollHeight(self:getScrollHeight() + 60);

    self.rows = self.rows + 1;
end


--*********************************************************
--* 琚涜姱鏂滈偑鑳佽阿姊拌柂鎳堟 瑜旀娉绘枩鑺郴瑜嬭姱鑳?
--*********************************************************
function EtherSettingsPanel:addCheckBox(title, method, isSelected, isOnlyInGame)
    local rows = self.rows;
    local checkboxX = 30;
    local checkboxY = 20 + rows * 40;

    local checkbox = UICheckbox:new(checkboxX, checkboxY, title, isSelected, method);
    checkbox:initialise();
    checkbox:instantiate();
    checkbox:setAnchorLeft(true);
    checkbox:setAnchorRight(false);
    checkbox:setAnchorTop(false);
    checkbox:setAnchorBottom(true);
    checkbox.isOnlyInGame = isOnlyInGame;
    self:addChild(checkbox);

    self:setScrollHeight(self:getScrollHeight() + checkbox.height + 40);

    self.rows = self.rows + 1;

    table.insert(self.checkBoxList, checkbox);
end


--*********************************************************
--* 琚ㄦ枩钖姱鑳佽阿姊拌柂鎳堟 閿岄偑钖璋㈡噲
--*********************************************************
function EtherSettingsPanel:updatePanel()
    for i=1, #self.checkBoxList do
        local item = self.checkBoxList[i];
        if item.isOnlyInGame and self.localPlayer == nil then
            item:setEnable(false);
        end
    end
    for i=1, #self.buttonList do
        local item = self.buttonList[i];
        if item.isOnlyNotInGame and self.localPlayer ~= nil then
            item:setEnable(false);
        end
    end
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鍐欒姱瑜旀瑜夎柂鎳堣 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?
--*********************************************************
function EtherSettingsPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true);
    self:setScrollHeight(0);
    self:addScrollBars();

    self:addLabel(10, 10, getTranslate("UI_Settings_ConfigTitle"))
    self.configs = ISScrollingListBox:new(20, 120, self.width - 40, 200);
    self.configs:initialise();
    self.configs:instantiate();
    self.configs.itemheight = 48
    self.configs.selected = 0;
    self.configs.joypadParent = self;
    self.configs.font = UIFont.NewSmall;
    self.configs.doDrawItem = self.drawConfigs;
    self.configs.drawBorder = true;
    self.configs.backgroundColor = {r=0, g=0, b=0, a=0.0};
    self.configs:addColumn(getTranslate("UI_Settings_ConfigName"), 0);
    self:addChild(self.configs);

    self.entry = ISTextEntryBox:new("EtherConfig-"..tostring(getConfigList():size()+1), 20, self.configs.y + self.configs.height + 20, self.width / 2 - 120, 48);
    self.entry.font = UIFont.Small;
    self.entry:initialise();
    self.entry:instantiate();
    self:addChild(self.entry);

    local saveButton = UIButton:new(self.entry.x + self.entry.width + 20, self.entry.y, 160, 48, getTranslate("UI_Settings_ConfigSave"), function ()
        local configName = self.entry:getText();
        if (configName ~= "") then
            saveConfig(configName);
            self:updateConfigsList();
        end
    end)
    saveButton:initialise();
    saveButton:instantiate();
    saveButton:setAnchorLeft(true);
    saveButton:setAnchorRight(false);
    saveButton:setAnchorTop(false);
    saveButton:setAnchorBottom(true);
    saveButton.update = function ()
        local text = self.entry:getText();
        if (text ~= "") then
            saveButton.isEnable = true;
        else
            saveButton.isEnable = false;
        end
    end
    self:addChild(saveButton)

    local loadButton = UIButton:new(saveButton.x + saveButton.width + 20, saveButton.y, 160, 48, getTranslate("UI_Settings_ConfigLoad"), function ()
        local configName = self.configs.items[self.configs.selected].item;
        if (configName ~= nil) then
            loadConfig(configName);
            EtherMain.accentColor = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0};
        end
    end)
    loadButton:initialise();
    loadButton:instantiate();
    loadButton:setAnchorLeft(true);
    loadButton:setAnchorRight(false);
    loadButton:setAnchorTop(false);
    loadButton:setAnchorBottom(true);
    loadButton.update = function ()
        local config = self.configs.items[self.configs.selected];
        if (config ~= nil) then
            loadButton.isEnable = true;
        else
            loadButton.isEnable = false;
        end
    end

    self:addChild(loadButton)

    local deleteButton = UIButton:new(loadButton.x + loadButton.width + 20, loadButton.y, 160, 48, getTranslate("UI_Settings_ConfigDelete"), function ()
        local configName = self.configs.items[self.configs.selected].item;
        if (configName ~= nil) then
            deleteConfig(configName);
            self:updateConfigsList();
        end
    end)
    deleteButton:initialise();
    deleteButton:instantiate();
    deleteButton:setAnchorLeft(true);
    deleteButton:setAnchorRight(false);
    deleteButton:setAnchorTop(false);
    deleteButton:setAnchorBottom(true);
    deleteButton.update = function ()
        local config = self.configs.items[self.configs.selected];
        if (config ~= nil) then
            deleteButton.isEnable = true;
        else
            deleteButton.isEnable = false;
        end
    end
    self:addChild(deleteButton)

    self.accentColor = self:addColorPickerWithLabel(getTranslate("UI_Settings_AccentColor"), function ()
        local picker = ISColorPicker:new(getMouseX(), getMouseY())
        picker:initialise()
        picker.pickedTarget = self
        picker.resetFocusTo = self
        picker:setInitialColor(getAccentUIColor());
        picker.pickedFunc = function (target, color, mouseUp)
            self.accentColor.backgroundColor = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0};
            setAccentUIColor(color.r, color.g, color.b);
            EtherMain.accentColor = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0};
        end;
        picker:addToUIManager();
    end, getAccentUIColor())

    self.playerColors = self:addColorPickerWithLabel(getTranslate("UI_Settings_PlayersColor"), function ()
        local picker = ISColorPicker:new(getMouseX(), getMouseY())
        picker:initialise()
        picker.pickedTarget = self
        picker.resetFocusTo = self
        picker:setInitialColor(getPlayersUIColor());
        picker.pickedFunc = function (target, color, mouseUp)
            self.playerColors.backgroundColor = {r = getPlayersUIColor():getR(), g = getPlayersUIColor():getG(), b = getPlayersUIColor():getB(), a = 1.0};
            setPlayersUIColor(color.r, color.g, color.b);
        end;
        picker:addToUIManager();
    end, getPlayersUIColor())

    self.vehicleColors = self:addColorPickerWithLabel(getTranslate("UI_Settings_VehicleColor"), function ()
        local picker = ISColorPicker:new(getMouseX(), getMouseY())
        picker:initialise()
        picker.pickedTarget = self
        picker.resetFocusTo = self
        picker:setInitialColor(getVehicleUIColor());
        picker.pickedFunc = function (target, color, mouseUp)
            self.vehicleColors.backgroundColor = {r = getVehicleUIColor():getR(), g = getVehicleUIColor():getG(), b = getVehicleUIColor():getB(), a = 1.0};
            setVehicleUIColor(color.r, color.g, color.b);
        end;
        picker:addToUIManager();
    end, getVehicleUIColor())

    self.zombieColors = self:addColorPickerWithLabel(getTranslate("UI_Settings_ZombiesColor"), function ()
        local picker = ISColorPicker:new(getMouseX(), getMouseY())
        picker:initialise()
        picker.pickedTarget = self
        picker.resetFocusTo = self
        picker:setInitialColor(getZombieUIColor());
        picker.pickedFunc = function (target, color, mouseUp)
            self.zombieColors.backgroundColor = {r = getZombieUIColor():getR(), g = getZombieUIColor():getG(), b = getZombieUIColor():getB(), a = 1.0};
            setZombieUIColor(color.r, color.g, color.b);
        end;
        picker:addToUIManager();
    end, getZombieUIColor())

    local currentLang = getLanguage();
    local nextLang, nextName;
    if currentLang == "CN" then
        nextLang, nextName = "RU", "RU";
    elseif currentLang == "RU" then
        nextLang, nextName = "EN", "EN";
    else
        nextLang, nextName = "CN", "CN";
    end

    self:addButtonWithLabel(getTranslate("UI_Settings_Language"), nextName, function ()
        setLanguage(nextLang);
        EtherMain:close();
        EtherMain.OnOpenPanel(EtherMain.menuKeyID);
    end, false);

    self:addButtonWithLabel(getTranslate("UI_Settings_ResetLuaLabel"), getTranslate("UI_Settings_ResetLuaButton"), function ()
        getCore():ResetLua("default", "Force")
    end, true);

    local sizeRow = 400 + self.rows * 50;
    self:addLabel(10, sizeRow - 3, getTranslate("UI_Settings_PanelSize"));

    self.widthEntry = ISTextEntryBox:new(tostring(EtherMain.defaultWidth), 150, sizeRow, 80, 32);
    self.widthEntry.font = UIFont.Small;
    self.widthEntry:initialise();
    self.widthEntry:instantiate();
    self:addChild(self.widthEntry);

    self.heightEntry = ISTextEntryBox:new(tostring(EtherMain.defaultHeight), 235, sizeRow, 80, 32);
    self.heightEntry.font = UIFont.Small;
    self.heightEntry:initialise();
    self.heightEntry:instantiate();
    self:addChild(self.heightEntry);

    local sizeSaveButton = UIButton:new(325, sizeRow, 125, 32, getTranslate("UI_Settings_PanelSizeSave"), function ()
        local newWidth = tonumber(self.widthEntry:getText()) or EtherMain.defaultWidth;
        local newHeight = tonumber(self.heightEntry:getText()) or EtherMain.defaultHeight;
        newWidth = math.max(350, newWidth);
        newHeight = math.max(400, newHeight);
        setPanelSize(newWidth, newHeight);
        EtherMain.defaultWidth = newWidth;
        EtherMain.defaultHeight = newHeight;
        EtherMain.instance:setWidth(newWidth);
        EtherMain.instance:setHeight(newHeight);
        EtherMain.instance.buttonsPanel:openPanel(EtherMain.currentTabID);
    end)
    sizeSaveButton:initialise();
    sizeSaveButton:instantiate();
    sizeSaveButton:setAnchorLeft(true);
    sizeSaveButton:setAnchorRight(false);
    sizeSaveButton:setAnchorTop(false);
    sizeSaveButton:setAnchorBottom(true);
    self:addChild(sizeSaveButton);

    self:setScrollHeight(self:getScrollHeight() + 82);
    self.rows = self.rows + 1;

    self:updateConfigsList();
    self:updatePanel();
end


--*********************************************************
--* 琚犺柂鎳堣鎳堥偑璋㈡噲锜归偑瑜戞噲瑜?瑜嬮攲鎳堣娉婚偑 娉昏姱钖鎳堣皭鑺儊
--*********************************************************
function EtherSettingsPanel:updateConfigsList()
    self.lastSelectedIndex = self.configs.selected or 0;
    self.configs:clear();

    local configList = getConfigList();
    for i=0, configList:size() - 1 do
        local config = configList:get(i)
        self.configs:addItem("Config", config);
    end
    self.configs.selected = self.lastSelectedIndex;
end

--*********************************************************
--* 琚ㄨ瑜夋噲瑜嬭姱鑳佹郴閭?娉昏姱钖鎳堣皭鑺儊
--*********************************************************
function EtherSettingsPanel:drawConfigs(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    if self.selected == item.index then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b);
    end

    if alt then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, 0.3, 0.3, 0.3);
    end


    self:drawText(tostring(item.item), 5 + self.columns[1].size, y + 5, 1, 1, 1, 1, UIFont.Small);

    return y + self.itemheight;
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?瑜嬭姱鏂滆瑜屾噲娉?娉昏姱璋㈡瑜嬫噲娉婚偑 灞戣瑜曟噲
--*********************************************************
function EtherSettingsPanel:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?prerender
--*********************************************************
function EtherSettingsPanel:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?render
--*********************************************************
function EtherSettingsPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 钖姱鑳佽姱璋愯姱 瑜濇郴锜规灞戦攲璋㈣瑜夐偑 灞戞钖
--*********************************************************
function EtherSettingsPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    self.checkBoxList = {}; -- 灏忛攲鎳堣鑺郴 鑳佽姊拌 瑜旀娉绘枩鑺郴瑜嬭姱鑳?
    self.buttonList = {}; -- 灏忛攲鎳堣鑺郴 鑳佽姊拌 娉昏柂鑺攲鑺郴
    self.rows = 0;

    return menuTableData;
end