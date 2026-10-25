require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIMechanics = ISPanel:derive("UIMechanics"); -- Наследование от ISPanel
UIMechanics.instance = nil;

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UIMechanics:createChildren()
    ISPanel.createChildren(self);

    self.closeButton = ISButton:new(3, 0, 20, 20, "", self, function(self, button) self:close() end);
	self.closeButton:initialise();
	self.closeButton.borderColor.a = 0.0;
	self.closeButton.backgroundColor.a = 0;
	self.closeButton.backgroundColorMouseOver.a = 0;
	self.closeButton:setImage(EtherTheme.getCloseTexture());
	self:addChild(self.closeButton);

    self.datas = EtherFastList:new(10, 130, self.width - 20, self.height - 200);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.drawBorder = true;
    EtherTheme.styleList(self.datas);
    self.datas:addColumn(getTranslate("UI_Mechanics_PartsTableName"), 0);
    self.datas:addColumn(getTranslate("UI_Mechanics_PartsTableCondition"), 150)
    self.datas:addColumn(getTranslate("UI_Mechanics_PartsTableAmount"), 250)
    self.datas:addColumn(getTranslate("UI_Mechanics_PartsTableCategory"), 350)
    self:addChild(self.datas);

    self.getKeyButton = UIButton:new(10, self.datas.y + self.datas.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonGetKey"), 
    function() 
        sendClientCommand(self.localPlayer, "vehicle", "getKey", { vehicle = self.localPlayer:getNearVehicle():getId() })
    end)
    self.getKeyButton:initialise();
    self.getKeyButton:instantiate();
    self.getKeyButton:setAnchorLeft(true);
    self.getKeyButton:setAnchorRight(false);
    self.getKeyButton:setAnchorTop(false);
    self.getKeyButton:setAnchorBottom(true);
    self:addChild(self.getKeyButton);

    self.repairPartButton = UIButton:new(self.getKeyButton.x + self.getKeyButton.width + 10, self.datas.y + self.datas.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonRepairPart"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        sendClientCommand(self.localPlayer, "vehicle", "repairPart", { vehicle = item:getVehicle():getId(), part = item:getId() })
        self:loadParts()
    end)
    self.repairPartButton:initialise();
    self.repairPartButton:instantiate();
    self.repairPartButton:setAnchorLeft(true);
    self.repairPartButton:setAnchorRight(false);
    self.repairPartButton:setAnchorTop(false);
    self.repairPartButton:setAnchorBottom(true);
    self:addChild(self.repairPartButton);

    self.brokePart = UIButton:new(self.repairPartButton.x + self.repairPartButton.width + 10, self.datas.y + self.datas.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonBrokePart"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        sendClientCommand(self.localPlayer, "vehicle", "setPartCondition", { vehicle = item:getVehicle():getId(), part = item:getId(), condition = 0 })
        self:loadParts()
    end)
    self.brokePart:initialise();
    self.brokePart:instantiate();
    self.brokePart:setAnchorLeft(true);
    self.brokePart:setAnchorRight(false);
    self.brokePart:setAnchorTop(false);
    self.brokePart:setAnchorBottom(true);
    self:addChild(self.brokePart);

    self.repairVehicleButton = UIButton:new(self.brokePart.x + self.brokePart.width + 10, self.datas.y + self.datas.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonRepairVehicle"), 
    function() 
        local vehicle = self.localPlayer:getNearVehicle();
        sendClientCommand(self.localPlayer, "vehicle", "repair", { vehicle = vehicle:getId() })
        self.totalCondition = 100;
    end)
    self.repairVehicleButton:initialise();
    self.repairVehicleButton:instantiate();
    self.repairVehicleButton:setAnchorLeft(true);
    self.repairVehicleButton:setAnchorRight(false);
    self.repairVehicleButton:setAnchorTop(false);
    self.repairVehicleButton:setAnchorBottom(true);
    self:addChild(self.repairVehicleButton);

    self.emptyButton = UIButton:new(10, self.repairVehicleButton.y + self.repairVehicleButton.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonEmptyPart"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        sendClientCommand(self.localPlayer, "vehicle", "setContainerContentAmount", { vehicle = item:getVehicle():getId(), part = item:getId(), amount = 0 })
        self:loadParts()
    end)
    self.emptyButton:initialise();
    self.emptyButton:instantiate();
    self.emptyButton:setAnchorLeft(true);
    self.emptyButton:setAnchorRight(false);
    self.emptyButton:setAnchorTop(false);
    self.emptyButton:setAnchorBottom(true);
    self:addChild(self.emptyButton);

    self.fillButton = UIButton:new(self.emptyButton.x + self.emptyButton.width + 10, self.repairVehicleButton.y + self.repairVehicleButton.height + 10, 100, 20, getTranslate("UI_Mechanics_ButtonFillPart"), 
    function() 
        local item = self.datas.items[self.datas.selected].item;
        sendClientCommand(self.localPlayer, "vehicle", "setContainerContentAmount", { vehicle = item:getVehicle():getId(), part = item:getId(), amount = 100 })
        self:loadParts()
    end)
    self.fillButton:initialise();
    self.fillButton:instantiate();
    self.fillButton:setAnchorLeft(true);
    self.fillButton:setAnchorRight(false);
    self.fillButton:setAnchorTop(false);
    self.fillButton:setAnchorBottom(true);
    self:addChild(self.fillButton);

    
end


--*********************************************************
--* Отрисовка данных
--*********************************************************
function UIMechanics:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)

    -- 二百七十五 (L2): 行数据 500ms 节拍缓存 — 部件名直接用条目预存 text (loadParts
    -- 已翻译好), 状态数值/类别译文/颜色缓存到条目, 每帧只剩 drawText (原为逐行逐帧
    -- 拼键查翻译 + 多次重复桥调用 + 每行颜色表分配)
    local now = getTimestampMs();
    local c = item.mcCache;
    if c == nil or now - c.t > 500 then
        local condition = item.item:getCondition();
        local condRatio = condition / 100;
        local id = item.item:getId();
        local amount;
        if id == "Battery" then
            amount = tostring(math.floor(item.item:getInventoryItem():getUsedDelta() * 100)) .. "%";
        elseif id == "GasTank" then
            amount = tostring(math.floor(item.item:getContainerContentAmount() / item.item:getContainerCapacity() * 100)) .. "%";
        else
            amount = "-";
        end
        local category = item.item:getCategory() or "Other";
        c = {
            t = now,
            r = 1 - condRatio, g = condRatio,
            condText = tostring(condition) .. "%",
            amount = amount,
            catText = getText("IGUI_VehiclePartCat" .. category),
        };
        item.mcCache = c;
    end

    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    -- Устанавливаем маску для первого столбца
    self:suspendStencil()
    self:setStencilRect(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    self:drawText(item.text, 5, y + 4, c.r, c.g, 0, 1, UIFont.Small);
    -- Удаляем маску
    self:clearStencilRect()
    self:resumeStencil()


    self:drawText(c.condText, self.columns[2].size + 10, y + 4, c.r, c.g, 0, 1, UIFont.Small);
    self:drawText(c.amount, self.columns[3].size + 10, y + 4, c.r, c.g, 0, 1, UIFont.Small);
    self:drawText(c.catText, self.columns[4].size + 10, y + 4, c.r, c.g, 0, 1, UIFont.Small);

    return y + self.itemheight;
end
--************************************************************************--
--** Prerender механики
--************************************************************************--
function UIMechanics:prerender()
    ISPanel.prerender(self)

    if self.background then
        EtherTheme.drawGlass(self);
    end

	EtherTheme.drawTitleBar(self, self.title, UIFont.Small)
end

--************************************************************************--
--** Render механики
--************************************************************************--
function UIMechanics:render()
    ISPanel.render(self)
	
    self:drawTexture(self.resizeimage, self.width-10, self.height - 10, 1, 1, 1, 1);

    if (self.localPlayer:getNearVehicle() == nil) then
        self.datas:setVisible(false);
        self:drawTextCentre(tr("UI_Mechanics_NoVehicle"), self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Small);

    else
        local vehicle = self.localPlayer:getNearVehicle();
        self.datas:setVisible(true);

        -- 二百七十五 (L2): 车辆显示名按脚本名缓存 (换车/烧毁变体自动失效重建;
        -- 原为每帧 translator 查表 + match + gsub)
        local scriptName = vehicle:getScript():getName();
        if self.mcVehName == nil or self.mcVehScript ~= scriptName then
            local name = getText("IGUI_VehicleName" .. scriptName);
            if string.match(scriptName, "Burnt") then
                local unburnt = string.gsub(scriptName, "Burnt", "");
                if getTextOrNull("IGUI_VehicleName" .. unburnt) then
                    name = getText("IGUI_VehicleName" .. unburnt)
                end
                name = getText("IGUI_VehicleNameBurntCar", name);
            end
            self.mcVehName = name;
            self.mcVehScript = scriptName;
        end

        self:drawTextCentre( self.mcVehName, self.width / 2, 20, 1.0, 1.0, 1.0, 1.0, UIFont.Medium);
        -- 分隔符与单位交给 UI_Fmt_* 翻译模板, 不在 Lua 里拼 ": " / "%" / " hp";
        -- label 仍取 vanilla IGUI_ 官方词条, 避免重复翻译已有文案。
        self:drawTextCentre(tr("UI_Fmt_LabelPercent", {
            label = getText("IGUI_OverallCondition"),
            value = tostring(self.totalCondition),
        }), self.width / 2, 40, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
        self:drawTextCentre(tr("UI_Fmt_LabelValue", {
            label = getText("IGUI_char_Weight"),
            value = tostring(vehicle:getMass()),
        }), self.width / 2, 60, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
        if vehicle:getPartById("Engine") then
		    self:drawTextCentre(tr("UI_Fmt_EnginePower", {
                label = getText("IGUI_EnginePower"),
                value = tostring(vehicle:getEnginePower() / 10),
            }), self.width / 2, 80, 1, 1, 1, 1, UIFont.Small);
	    end

        if self.datas.items[self.datas.selected] ~= nil then
            if self.datas.items[self.datas.selected].item:getId() == "Battery" or self.datas.items[self.datas.selected].item:getId() == "GasTank" then
                self.emptyButton.isEnable = true;
                self.fillButton.isEnable = true;
            else
                self.emptyButton.isEnable = false;
                self.fillButton.isEnable = false;
            end
        end
    end
end

--************************************************************************--
--** Загрузка деталей
--************************************************************************--
function UIMechanics:loadParts()
    self.lastSelectedIndex = self.datas.selected or 0;
    local vehicle = self.localPlayer:getNearVehicle();
    self.datas:clear();

    self.totalCondition = 0;
    for i = 1, vehicle:getPartCount() do
		local part = vehicle:getPartByIndex(i-1)
        if part:getCategory() ~= "nodisplay" then
            self.datas:addItem(getText("IGUI_VehiclePart" .. part:getId()), part);
        end
        self.totalCondition = self.totalCondition  + part:getCondition();
	end
    self.totalCondition = round(self.totalCondition / vehicle:getPartCount())
    self.datas.selected = self.lastSelectedIndex;
end

--************************************************************************--
--** Обновление меню
--************************************************************************--
function UIMechanics:update()
    if (self.localPlayer:getNearVehicle() == nil) then 
        self.isPartsLoaded = false;
        self.repairPartButton:setVisible(false);
        self.repairVehicleButton:setVisible(false);
        self.getKeyButton:setVisible(false);
        self.brokePart:setVisible(false);
        self.emptyButton:setVisible(false);
        self.fillButton:setVisible(false);
        return
    end
    
    if not self.isPartsLoaded then  
        self:loadParts();
        self.isPartsLoaded = true;
        self.repairPartButton:setVisible(true);
        self.repairVehicleButton:setVisible(true);
        self.getKeyButton:setVisible(true);
        self.brokePart:setVisible(true);
        self.emptyButton:setVisible(true);
        self.fillButton:setVisible(true);
    end
  
end

--*********************************************************
--* Закрытие меню механики
--*********************************************************
function UIMechanics:close()
    UIMechanics.instance:setVisible(false);
    UIMechanics.instance:removeFromUIManager();
    UIMechanics.instance = nil;
end

--*********************************************************
--* Логика открытия меню механики
--*********************************************************
function UIMechanics.openPanel()
    -- Если панель уже существует, закрываем окно
    if UIMechanics.instance ~= nil then
        UIMechanics.instance:setVisible(false);
        UIMechanics.instance:removeFromUIManager();
        UIMechanics.instance = nil;
        return
    end

    -- Создаем новую панель
    UIMechanics.instance = UIMechanics:new();
    UIMechanics.instance:initialise();
    UIMechanics.instance:instantiate();
    UIMechanics.instance:addToUIManager();
    UIMechanics.instance:setVisible(true);
    UIMechanics.instance:setAlwaysOnTop(true);
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIMechanics:new()
    local menuTableData = {};

    local width = 530;
    local height = 400;

    local positionX = getCore():getScreenWidth() / 2 - width / 2;
    local positionY = getCore():getScreenHeight() / 2 - height / 2;

    menuTableData = ISPanel:new(positionX, positionY, width, height);
    setmetatable(menuTableData, self);
	menuTableData.borderColor = {r=0.10, g=0.52, b=0.22, a=0.6};
	menuTableData.backgroundColor = {r=0.02, g=0.02, b=0.02, a=0.85};
    menuTableData.title = getTranslate("UI_Mechanics_Title");
    menuTableData.moveWithMouse = true;
    menuTableData.isPartsLoaded = false;
    menuTableData.localPlayer = getPlayer();
    menuTableData.closeTexture = getTexture("media/ui/Dialog_Titlebar_CloseIcon.png");
    self.__index = self;
    return menuTableData;
end