require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIMovableMiniMap = ISPanel:derive("UIMovableMiniMap"); -- Наследование от ISPanel
UIMovableMiniMap.instance = nil;

-- 快捷按钮开/关两态文字色: render 每帧给 5 个按钮重涂, 提为常量避免每帧新建表
-- (ISButton 对 textColor 只读不写, 共享同一表实例安全, 值与原字面量完全一致)
local TOGGLE_COLOR_ON = { r = 1, g = 1, b = 1, a = 1 };
local TOGGLE_COLOR_OFF = { r = 0.35, g = 0.35, b = 0.35, a = 1 };

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UIMovableMiniMap:createChildren()
    ISPanel.createChildren(self);

    -- 快捷开关按钮行 (我/玩家/载具/僵尸/物品), 白=开, 灰=关
    -- 注: 世界画面物品标记的开关在 ESP 页「物品信息」与雷达页「在地图上显示」
    -- (同一总开关), 小地图不放第 6 枚 —— 与小地图标记绑死同一状态, 单独开关无意义
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
            if button.toggleKey == "drawItems" then
                -- 物品按钮只管小地图标记; ESP 画线在 ESP 页/雷达页独立开关
                EtherItemSearch.setMinimapEnabled(not UIMap.drawItems);
                return
            end
            UIMap[button.toggleKey] = not UIMap[button.toggleKey];
            local toggles = {
                drawLocalPlayer = toggleMapDrawLocalPlayer,
                drawAllPlayers = toggleMapDrawAllPlayers,
                drawVehicles = toggleMapDrawVehicles,
                drawZombies = toggleMapDrawZombies,
            };
            local mirror = toggles[button.toggleKey];
            if mirror ~= nil then mirror(UIMap[button.toggleKey]) end
            button.textColor = UIMap[button.toggleKey] and TOGGLE_COLOR_ON or TOGGLE_COLOR_OFF;
        end);
        b:initialise();
        b.toggleKey = d[2];
        b.textColor = UIMap[d[2]] and TOGGLE_COLOR_ON or TOGGLE_COLOR_OFF;
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
	self.closeButton:setImage(EtherTheme.getCloseTexture());
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

    if self.background then
        EtherTheme.drawGlass(self);
    end

	EtherTheme.drawTitleBar(self, self.title, UIFont.Small)
end

--************************************************************************--
--** Render карты
--************************************************************************--
function UIMovableMiniMap:render()
    ISPanel.render(self)

    if self.toggleButtons ~= nil then
        for _, b in ipairs(self.toggleButtons) do
            b.textColor = UIMap[b.toggleKey] and TOGGLE_COLOR_ON or TOGGLE_COLOR_OFF
        end
    end

    self:drawTexture(self.resizeimage, self.width-10, self.height - 10, 1, 1, 1, 1);
end

--*********************************************************
--* 位置记忆 (二百零八/二百零九):开着期间每**现实 30 分钟**保存一次 —
--* EveryTenMinutes 节拍只当唤醒 (约 10 现实秒一次), 存不存由 getTimestampMs
--* 真实时钟与上次保存的间隔决定 (用户拍板"现实时间30分钟"; 原版无 30 分钟
--* 事件, 游戏分钟节拍会随倍速漂移不能用)。关闭时终存不受门限限制 (直接退
--* 游戏/崩溃最多丢一个节拍); 关闭路径必须先存再拆实例。
--*********************************************************
local MINIMAP_POS_SAVE_INTERVAL_MS = 30 * 60 * 1000;
UIMovableMiniMap.lastPosSaveMs = 0;

function UIMovableMiniMap.doSavePos()
    if UIMovableMiniMap.instance ~= nil then
        setMinimapPos(UIMovableMiniMap.instance.x, UIMovableMiniMap.instance.y);
    end
end

function UIMovableMiniMap.savePos()
    local now = getTimestampMs();
    if now - UIMovableMiniMap.lastPosSaveMs >= MINIMAP_POS_SAVE_INTERVAL_MS then
        UIMovableMiniMap.lastPosSaveMs = now;
        UIMovableMiniMap.doSavePos();
    end
end

local function stopMiniMapPosWatch()
    Events.EveryTenMinutes.Remove(UIMovableMiniMap.savePos);
    UIMovableMiniMap.doSavePos();
end

--*********************************************************
--* Закрытие миникарты
--*********************************************************
function UIMovableMiniMap:close()
    stopMiniMapPosWatch();
    UIMovableMiniMap.instance:setVisible(false);
    UIMovableMiniMap.instance:removeFromUIManager();
    UIMovableMiniMap.instance = nil;
    setMinimapOpen(false);
end

--*********************************************************
--* Логика открытия миникарты
--*********************************************************
function UIMovableMiniMap.openPanel()
    -- Если панель уже существует, закрываем окно
    if UIMovableMiniMap.instance ~= nil then
        stopMiniMapPosWatch();
        UIMovableMiniMap.instance:setVisible(false);
        UIMovableMiniMap.instance:removeFromUIManager();
        UIMovableMiniMap.instance = nil;
        setMinimapOpen(false);
        return
    end

    -- Создаем новую панель
    UIMovableMiniMap.instance = UIMovableMiniMap:new();
    UIMovableMiniMap.instance:initialise();
    UIMovableMiniMap.instance:instantiate();
    UIMovableMiniMap.instance:addToUIManager();
    UIMovableMiniMap.instance:setVisible(true);
    UIMovableMiniMap.instance:setAlwaysOnTop(false);
    Events.EveryTenMinutes.Remove(UIMovableMiniMap.savePos);
    UIMovableMiniMap.lastPosSaveMs = getTimestampMs();
    Events.EveryTenMinutes.Add(UIMovableMiniMap.savePos);
    setMinimapOpen(true);
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIMovableMiniMap:new()
    local menuTableData = {};

    local width = 300;
    local height = 300;

    local positionX = getCore():getScreenWidth() - width - 15;
    local positionY = getCore():getScreenHeight() - height - 15;

    -- 位置记忆 (二百零八): 上次保存的坐标优先 (负值 = 无记忆), 并钳回屏幕内;
    -- 尺寸仍固定 300x300 (会话内可拉伸, 不落盘)
    local savedX = getMinimapPosX();
    local savedY = getMinimapPosY();
    if savedX ~= nil and savedY ~= nil and savedX >= 0 and savedY >= 0 then
        positionX = math.floor(savedX);
        positionY = math.floor(savedY);
    end
    positionX = math.max(0, math.min(positionX, getCore():getScreenWidth() - width));
    positionY = math.max(0, math.min(positionY, getCore():getScreenHeight() - height));

    menuTableData = ISPanel:new(positionX, positionY, width, height);
    setmetatable(menuTableData, self);
	menuTableData.borderColor = {r=0.10, g=0.52, b=0.22, a=0.6};
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.02, g=0.02, b=0.02, a=0.85};
    menuTableData.title = getTranslate("UI_Map_MiniMapTitle");
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    menuTableData.closeTexture = getTexture("media/ui/Dialog_Titlebar_CloseIcon.png");
	menuTableData.resizeimage = getTexture("media/ui/Panel_StatusBar_Resize.png");
    self.__index = self;

    return menuTableData;
end