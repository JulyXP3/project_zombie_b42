require "ISUI/ISPanel"

--*********************************************************
--* Подключение фиксов для vanilla багов
--*********************************************************
require "EtherHack/lua/fixes/ISChatFix"

--*********************************************************
--* Server Sync Protection (prevents server from overwriting local changes)
--*********************************************************
require "EtherHack/lua/fixes/ServerSyncBlocker"

--*********************************************************
--* Подключение модулей
--*********************************************************
local etherModules = {
    "EtherHack/lua/components/ui/EtherTheme.lua",
    "EtherHack/lua/components/override/EtherAdminMenu.lua",
    "EtherHack/lua/components/override/EtherDebugMenu.lua",
    "EtherHack/lua/components/override/EtherEditInventoryItem.lua",
    "EtherHack/lua/components/override/EtherEditWorldObjects.lua",
    "EtherHack/lua/components/ui/UIButtonsPanel.lua",
    "EtherHack/lua/components/ui/UICheckbox.lua",
    "EtherHack/lua/components/ui/UIButton.lua",
    "EtherHack/lua/components/ui/UISlider.lua",
    "EtherHack/lua/components/ui/UIMechanics.lua",
    "EtherHack/lua/components/ui/UIModalAddXP.lua",
    "EtherHack/lua/components/ui/UIMovableMiniMap.lua",
    "EtherHack/lua/components/ui/UIModalAddTrait.lua",
    "EtherHack/lua/components/ui/UIHealth.lua",
    "EtherHack/lua/components/ui/UIItemTables.lua",
    "EtherHack/lua/components/ui/EtherItemSearch.lua",
    "EtherHack/lua/components/ui/EtherTrapPOC.lua",
    "EtherHack/lua/components/ui/UIMap.lua",
    "EtherHack/lua/components/ui/UISkillTable.lua",
    "EtherHack/lua/components/ui/UITraitsTable.lua",
    "EtherHack/lua/components/panels/EtherInfoPanel.lua",
    "EtherHack/lua/components/panels/EtherCharacterPanel.lua",
    "EtherHack/lua/components/panels/EtherItemCreator.lua",
    "EtherHack/lua/components/panels/EtherTrapSpawn.lua",
    "EtherHack/lua/components/panels/EtherPlayerEditor.lua",
    "EtherHack/lua/components/panels/EtherVisualsPanel.lua",
    "EtherHack/lua/components/panels/EtherMapPanel.lua",
    "EtherHack/lua/components/panels/EtherExploitPanel.lua",
    "EtherHack/lua/components/panels/EtherSettingsPanel.lua"
}

for _, module in ipairs(etherModules) do
    requireExtra(module);
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherMain                   = ISPanel:derive("EtherMain"); -- Наследование от ISPanel
EtherMain.instance          = nil; --Экземпляр окна
EtherMain.menuKeyID         = 210; -- Клавиша открытия окна - Insert (210)
EtherMain.defaultWidth      = getPanelWidth(); -- Стандартная ширина окна (из конфига)
EtherMain.defaultHeight     = getPanelHeight(); -- Стандартная высота окна (из конфига)
EtherMain.currentTabID      = 1; -- Последняя открытая вкладка
EtherMain.accentColor       = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0}; -- Акцентный цвет

--*********************************************************
--* Закрытие окна по нажатию кнопки UI
--*********************************************************
function EtherMain:close()
	EtherMain.instance:setVisible(false);
    EtherMain.instance:removeFromUIManager();
    EtherMain.instance = nil;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherMain:createChildren()
    ISPanel.createChildren(self);

    self.buttonsPanel = UIButtonsPanel:new(0, EtherTheme.titleH, 50, self.height - EtherTheme.titleH, self, EtherMain.accentColor);
    self.buttonsPanel:initialise();
    self.buttonsPanel:instantiate();
    self.buttonsPanel:setVisible(true);
    self:addChild(self.buttonsPanel);

    self.buttonsPanel:addButton("EtherHack/media/ui/info.png", EtherInfoPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/character.png", EtherCharacterPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/itemCreator.png", EtherItemCreator);
    self.buttonsPanel:addButton("EtherHack/media/ui/trap.png", EtherTrapSpawn);
    self.buttonsPanel:addButton("EtherHack/media/ui/playerEditor.png", EtherPlayerEditor);
    self.buttonsPanel:addButton("EtherHack/media/ui/visuals.png", EtherVisualsPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/teleport.png", EtherMapPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/exploit.png", EtherExploitPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/settings.png", EtherSettingsPanel);

    self.buttonsPanel:openPanel(EtherMain.currentTabID);

    self.closeButton = ISButton:new(self.width - 23, 2, 20, 20, "", self, function(self2, button) EtherMain:close() end);
    self.closeButton:initialise();
    self.closeButton.borderColor.a = 0.0;
    self.closeButton.backgroundColor.a = 0;
    self.closeButton.backgroundColorMouseOver.a = 0;
    self.closeButton:setImage(EtherTheme.getCloseTexture());
    self:addChild(self.closeButton);
end

--*********************************************************
--* 玻璃底 + 红标题条 (纯外观)
--*********************************************************
function EtherMain:render()
    ISPanel.render(self);
    if self.background then
        EtherTheme.drawGlass(self);
    end
    local b = EtherTheme.blood;
    self:drawRect(0, 0, self.width, EtherTheme.titleH, 0.92, b.r, b.g, b.b);
    self:drawRect(0, EtherTheme.titleH, self.width, 1, 0.5, 0.03, 0.18, 0.08);
    self:drawText("E T H E R   H A C K  //  B42", 60, EtherTheme.titleH / 2 - EtherTheme.fontHgtSmall / 2, 1, 1, 1, 1, UIFont.Small);
end

--*********************************************************
--* Логика открытия и закрытия меню по нажатию клавиши
--*********************************************************
function EtherMain.OnOpenPanel(key)
    if key == EtherMain.menuKeyID then
        -- Если панель уже существует, закрываем окно
        if EtherMain.instance ~= nil then
            EtherMain.instance:setVisible(false);
            EtherMain.instance:removeFromUIManager();
            EtherMain.instance = nil;
            return
        end

        -- Создаем новую панель
        EtherMain.instance  = EtherMain:new();
        EtherMain.instance:initialise();
        EtherMain.instance:instantiate();
        EtherMain.instance:addToUIManager();
        EtherMain.instance:setVisible(true);
        EtherMain.instance:setAlwaysOnTop(false);
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherMain:new()
    local menuTableData = {};

    local positionX = getCore():getScreenWidth() / 2 - EtherMain.defaultWidth / 2;
    local positionY = getCore():getScreenHeight() / 2 - EtherMain.defaultHeight / 2;

    menuTableData = ISPanel:new(positionX, positionY, EtherMain.defaultWidth, EtherMain.defaultHeight);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.05, g=0.05, b=0.05, a=1};
	menuTableData.borderColor = {r=0, g=0, b=0, a=0};
	menuTableData.moveWithMouse = true;
    self.__index = self;

    return menuTableData;
end

--*********************************************************
--* STATUS WARNING
--*********************************************************
print("=======================================================")
print("[EtherHack] Build 42 Version Loaded")
print("[EtherHack] WARNING: Most features are experimental!")
print("[EtherHack] CONFIRMED WORKING: Item Spawner, Radar/ESP")
print("[EtherHack] EXPERIMENTAL: God Mode, Invisible, Cheats")
print("=======================================================")

--*********************************************************
--* 自动恢复上次状态: 读取 startup 配置(小地图开关/图层/视觉效果等),
--* 若上次关闭游戏时小地图处于打开状态则重新打开
--*********************************************************
local function onGameStart()
    UIMovableMiniMap.instance = nil; -- 上一场游戏的旧实例已失效, 避免 openPanel 误判为已打开
    loadConfig("startup");
    if isMinimapOpen() and getPlayer() ~= nil then
        UIMovableMiniMap.openPanel();
    end
end

Events.OnGameStart.Add(onGameStart);

--*********************************************************
--* F10: 重置附近容器战利品 (与物品页「重置容器」按钮同入口)
--*********************************************************
function EtherMain.OnKeyPressed(key)
    if key == Keyboard.KEY_F10 and getPlayer() ~= nil and isMultiplayer() then
        EtherContainerPOC.reset();
    end
end

Events.OnKeyPressed.Add(EtherMain.OnKeyPressed);
Events.OnKeyPressed.Add(EtherMain.OnOpenPanel);