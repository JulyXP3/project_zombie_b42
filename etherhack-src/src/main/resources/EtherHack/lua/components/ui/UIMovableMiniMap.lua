require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIMovableMiniMap = ISPanel:derive("UIMovableMiniMap"); -- Наследование от ISPanel
UIMovableMiniMap.instance = nil;

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UIMovableMiniMap:createChildren()
    ISPanel.createChildren(self);

    -- 快捷开关按钮行 (我/玩家/载具/僵尸/物品), 白=开, 灰=关
    UIMap.ensureDrawFlags();
    self.toggleButtons = {};
    local defs = {
        { "UI_Map_Toggle_LocalPlayer", "drawLocalPlayer" },
        { "UI_Map_Toggle_OtherPlayers", "drawAllPlayers" },
        { "UI_Map_Toggle_Vehicles", "drawVehicles" },
        { "UI_Map_Toggle_Zombies", "drawZombies" },
        { "UI_Map_Toggle_Items", "drawItems" },
    }
    local bx = 6;
    for _, d in ipairs(defs) do
        local b = ISButton:new(bx, 20, 48, 18, getTranslate(d[1]), self, function(self, button)
            UIMap[button.toggleKey] = not UIMap[button.toggleKey];
            button.textColor = UIMap[button.toggleKey] and { r = 1, g = 1, b = 1, a = 1 } or { r = 0.35, g = 0.35, b = 0.35, a = 1 };
            if button.toggleKey == "drawItems" then
                EtherItemSearch.setEnabled(UIMap.drawItems);
            end
        end);
        b:initialise();
        b.toggleKey = d[2];
        b.textColor = UIMap[d[2]] and { r = 1, g = 1, b = 1, a = 1 } or { r = 0.35, g = 0.35, b = 0.35, a = 1 };
        self:addChild(b);
        table.insert(self.toggleButtons, b);
        bx = bx + 50;
    end

    self.map = UIMap:new(10, 40, self.width - 20, self.height - 50)
    self.map:initialise()
    self.map:instantiate()
    self.map:initDataAndStyle()
    self.map.mapAPI:resetView()
    self.map:restoreSettings()
    self.map.centerByPlayer = true
    self:addChild(self.map)

    self.closeButton = ISButton:new(3, 0, 20, 20, "", self, function(self, button) self:close() end);
	self.closeButton:initialise();
	self.closeButton.borderColor.a = 0.0;
	self.closeButton.backgroundColor.a = 0;
	self.closeButton.backgroundColorMouseOver.a = 0;
	self.closeButton:setImage(self.closeTexture);
	self:addChild(self.closeButton);

    self.resizeWidgetCorner = ISResizeWidget:new(self.width-10, self.height-10, 10, 10, self);
	self.resizeWidgetCorner:initialise();
	self.resizeWidgetCorner:setVisible(true)
	self:addChild(self.resizeWidgetCorner);

end

--************************************************************************--
--** Prerender карты
--************************************************************************--
function UIMovableMiniMap:prerender()
    ISPanel.prerender(self)

	self:drawRect( 0, 0, self.width, 20, 1.0, 0, 0, 0, 0.5)
	self:drawTextCentre(self.title, self:getWidth() / 2, 1, 1, 1, 1, 1, UIFont.Small);
	
end

--************************************************************************--
--** Render карты
--************************************************************************--
function UIMovableMiniMap:render()
    ISPanel.render(self)
	
    self:drawTexture(self.resizeimage, self.width-10, self.height - 10, 1, 1, 1, 1);
end

--*********************************************************
--* Закрытие миникарты
--*********************************************************
function UIMovableMiniMap:close()
    UIMovableMiniMap.instance:setVisible(false);
    UIMovableMiniMap.instance:removeFromUIManager();
    UIMovableMiniMap.instance = nil;
end

--*********************************************************
--* Логика открытия миникарты
--*********************************************************
function UIMovableMiniMap.openPanel()
    -- Если панель уже существует, закрываем окно
    if UIMovableMiniMap.instance ~= nil then
        UIMovableMiniMap.instance:setVisible(false);
        UIMovableMiniMap.instance:removeFromUIManager();
        UIMovableMiniMap.instance = nil;
        return
    end

    -- Создаем новую панель
    UIMovableMiniMap.instance = UIMovableMiniMap:new();
    UIMovableMiniMap.instance:initialise();
    UIMovableMiniMap.instance:instantiate();
    UIMovableMiniMap.instance:addToUIManager();
    UIMovableMiniMap.instance:setVisible(true);
    UIMovableMiniMap.instance:setAlwaysOnTop(false);
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIMovableMiniMap:new()
    local menuTableData = {};

    local width = 256;
    local height = 256;

    local positionX = getCore():getScreenWidth() - width - 15;
    local positionY = getCore():getScreenHeight() - height - 15;

    menuTableData = ISPanel:new(positionX, positionY, width, height);
    setmetatable(menuTableData, self);
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.title = getTranslate("UI_Map_MiniMapTitle");
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    menuTableData.closeTexture = getTexture("media/ui/Dialog_Titlebar_CloseIcon.png");
	menuTableData.resizeimage = getTexture("media/ui/Panel_StatusBar_Resize.png");
    self.__index = self;

    return menuTableData;
end