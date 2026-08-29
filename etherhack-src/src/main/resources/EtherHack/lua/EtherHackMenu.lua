require "ISUI/ISPanel"

--*********************************************************
--* Server Sync Protection (prevents server from overwriting local changes)
--* 注: ISChatFix / ServerSyncBlocker 不再 require, 由 EtherLuaManager 用 RunLua 直接加载
--* (PZ vanilla require loader 找不到 EtherHack/lua 下的文件)
--*********************************************************

--*********************************************************
--* Подключение модулей
--*********************************************************
local etherModules = {
    "EtherHack/lua/components/ui/EtherTheme.lua",
    "EtherHack/lua/components/ui/EtherI18n.lua",
    "EtherHack/lua/components/ui/EtherFormPanel.lua",
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
    "EtherHack/lua/components/ui/EtherAmmoFarm.lua",
    "EtherHack/lua/components/ui/UIMap.lua",
    "EtherHack/lua/components/ui/UISkillTable.lua",
    "EtherHack/lua/components/ui/UITraitsTable.lua",
    "EtherHack/lua/components/panels/EtherInfoPanel.lua",
    "EtherHack/lua/components/panels/EtherCharacterPanel.lua",
    "EtherHack/lua/components/panels/EtherItemCreator.lua",
    "EtherHack/lua/components/panels/EtherRadarPanel.lua",
    "EtherHack/lua/components/panels/EtherTrapSpawn.lua",
    "EtherHack/lua/components/panels/EtherPlayerEditor.lua",
    "EtherHack/lua/components/panels/EtherVisualsPanel.lua",
    "EtherHack/lua/components/panels/EtherMapPanel.lua",
    "EtherHack/lua/components/panels/EtherExploitPanel.lua",
    "EtherHack/lua/components/panels/EtherLootRollPanel.lua",
    "EtherHack/lua/components/panels/EtherVehiclePanel.lua",
    "EtherHack/lua/components/override/EtherCharacterCreation.lua",
    "EtherHack/lua/components/panels/EtherCharacterBoostPanel.lua",
    "EtherHack/lua/components/panels/EtherFarmingPanel.lua",
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
-- 窗口设计尺寸 888x888 (用户不可调): 所有面板按此定宽排版, 不再读取/写入配置里的尺寸。
-- 注: Java 侧 getPanelWidth/getPanelHeight/setPanelSize 保持不动(冻结契约), 只是 UI 不再使用。
-- 实际尺寸在 EtherMain:new 里按屏幕钳制, 小分辨率下会小于设计值:
--   宽度变窄时 EtherFormPanel:planColumns 会自动从 2 列降为 1 列;
--   高度变矮时各面板走自身的竖向滚动。minWidth/minHeight 是下限, 低于此
--   导航栏(170)+内容区就挤不出可用空间了。
EtherMain.defaultWidth      = 888;
EtherMain.defaultHeight     = 888;
EtherMain.minWidth          = 640;
EtherMain.minHeight         = 480;
EtherMain.currentTabID      = 1; -- Последняя открытая вкладка
EtherMain.accentColor       = {r = getAccentUIColor():getR(), g = getAccentUIColor():getG(), b = getAccentUIColor():getB(), a = 1.0}; -- Акцентный цвет

--*********************************************************
--* Закрытие окна по нажатию кнопки UI
--*********************************************************
function EtherMain:close()
	EtherMain.instance:setVisible(false);
    EtherMain.instance:removeFromUIManager();
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function EtherMain:createChildren()
    ISPanel.createChildren(self);

    self.buttonsPanel = UIButtonsPanel:new(EtherTheme.framePad, EtherTheme.headerH, 170, self.height - EtherTheme.headerH - EtherTheme.framePad, self, EtherMain.accentColor);
    self.buttonsPanel:initialise();
    self.buttonsPanel:instantiate();
    self.buttonsPanel:setVisible(true);
    self:addChild(self.buttonsPanel);

    self.buttonsPanel:addButton("EtherHack/media/ui/info.png", "UI_Nav_Info", EtherInfoPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/character.png", "UI_Nav_Character", EtherCharacterPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/itemCreator.png", "UI_Nav_Items", EtherItemCreator);
    self.buttonsPanel:addButton("EtherHack/media/ui/radar.png", "UI_Nav_Radar", EtherRadarPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/trap.png", "UI_Nav_Traps", EtherTrapSpawn);
    self.buttonsPanel:addButton("EtherHack/media/ui/playerEditor.png", "UI_Nav_Player", EtherPlayerEditor);
    self.buttonsPanel:addButton("EtherHack/media/ui/visuals.png", "UI_Nav_Visuals", EtherVisualsPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/teleport.png", "UI_Nav_Teleport", EtherMapPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/loot.png", "UI_Nav_Loot", EtherLootRollPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/vehicle.png", "UI_Nav_Vehicle", EtherVehiclePanel);
    -- 「耕种」: 原角色页「作弊耕种模式」开关的面板化替代, 位于「载具」之后
    self.buttonsPanel:addButton("EtherHack/media/ui/farming.png", "UI_Nav_Farming", EtherFarmingPanel);
    -- 「创建角色」: 建号相关功能集中页 (自定义编辑/建号增强), 位于「耕种」与「其他」之间。
    self.buttonsPanel:addButton("EtherHack/media/ui/characterBoost.png", "UI_Nav_CharacterBoost", EtherCharacterBoostPanel);
    -- 「其他」(原「漏洞」): 位置固定在「耕种」与「设置」之间。
    self.buttonsPanel:addButton("EtherHack/media/ui/exploit.png", "UI_Nav_Exploit", EtherExploitPanel);
    self.buttonsPanel:addButton("EtherHack/media/ui/settings.png", "UI_Nav_Settings", EtherSettingsPanel);

    self.buttonsPanel:openPanel(EtherMain.currentTabID);

    -- 关闭键: 尺寸随页眉收紧, 并在页眉内竖直居中
    local closeS = 18;
    self.closeButton = ISButton:new(self.width - closeS - 6,
        math.floor((EtherTheme.headerH - closeS) / 2), closeS, closeS, "", self,
        function(self2, button) EtherMain:close() end);
    self.closeButton:initialise();
    self.closeButton.borderColor.a = 0.0;
    self.closeButton.backgroundColor.a = 0;
    self.closeButton.backgroundColorMouseOver.a = 0;
    self.closeButton:setImage(EtherTheme.getCloseTexture());
    self.closeButton:setAnchorRight(true);   -- 跟随右边缘, 缩放/重建后仍在右上角
    self.closeButton:setAnchorLeft(false);
    self.closeButton:setAnchorTop(true);
    self:addChild(self.closeButton);
end

--*********************************************************
--* 玻璃底 + 红标题条 (纯外观)
--*********************************************************
function EtherMain:render()
    ISPanel.render(self);
    local tm = getTextManager();
    local hh = EtherTheme.headerH;
    local ftt = EtherTheme.fontHgtTitle;       -- 标题行高
    local hb = EtherTheme.railBG;     -- 深青页眉底
    local c  = EtherTheme.blood;      -- 青霓虹 (大标题/下缘线)

    -- 深色页眉底 + 极淡扫描线 + 霓虹下缘线
    self:drawRect(0, 0, self.width, hh, 0.96, hb.r, hb.g, hb.b);
    EtherTheme.drawScanlines(self);
    self:drawRect(0, hh - 1, self.width, 1, 1, c.r, c.g, c.b);

    -- 标题: 青霓虹, 水平 + 竖直居中 (取整避免半像素导致字体发虚)。
    -- 署名与开源声明已移到"信息"选项卡 (EtherInfoPanel), 页眉只保留标题,
    -- 因此不再需要右侧文本块 / 折行缓存 / 省略号截断那套逻辑。
    -- 标题是常量, 宽度只测一次 (MeasureStringX 不便宜, 不放每帧路径)。
    local title = "ETHER HACK // B42";
    if EtherMain.titleW == nil then
        EtherMain.titleW = tm:MeasureStringX(EtherTheme.fontTitle, title);
    end
    local tw = EtherMain.titleW;
    self:drawText(title, math.floor((self.width - tw) / 2),
        math.floor((hh - ftt) / 2), c.r, c.g, c.b, 1, EtherTheme.fontTitle);

    -- 切角霓虹外框 (最后画, 压在页眉之上; 导航/内容已内缩 framePad 让出边距)
    EtherTheme.drawFrame(self, 0, 0, self.width, self.height);
end

--*********************************************************
--* Логика открытия и закрытия меню по нажатию клавиши
--*********************************************************
function EtherMain.OnOpenPanel(key)
    if key == EtherMain.menuKeyID then
        -- Если панель уже существует, переключаем видимость (состояние вкладок/прокрутки сохраняется)
        if EtherMain.instance ~= nil then
            if EtherMain.instance:getIsVisible() then
                EtherMain.instance:setVisible(false);
                EtherMain.instance:removeFromUIManager();
            else
                EtherMain.instance:addToUIManager();
                EtherMain.instance:setVisible(true);
            end
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

    -- 尺寸按屏幕钳制: 888 是设计尺寸, 但 1366x768 / 1280x720 这类常见分辨率
    -- 高度不足 888, 不钳制会让面板顶部越出屏幕(positionY 变负), 且各面板按
    -- self.height - N 定位的底部控件会落到可见区之外点不到。
    -- 面板尺寸设置项已移除, 用户无法手动补救, 所以必须在这里兜住。
    local margin = 40;
    local screenW = getCore():getScreenWidth();
    local screenH = getCore():getScreenHeight();
    local w = math.min(EtherMain.defaultWidth, screenW - margin);
    local h = math.min(EtherMain.defaultHeight, screenH - margin);
    if w < EtherMain.minWidth then w = EtherMain.minWidth; end
    if h < EtherMain.minHeight then h = EtherMain.minHeight; end

    local positionX = math.floor(screenW / 2 - w / 2);
    local positionY = math.floor(screenH / 2 - h / 2);
    if positionX < 0 then positionX = 0; end
    if positionY < 0 then positionY = 0; end

    menuTableData = ISPanel:new(positionX, positionY, w, h);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.03, g=0.05, b=0.06, a=1};
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
    EtherMain.instance = nil; -- 同上: 旧菜单实例已失效, 下次按键重新构建
    loadConfig("startup");
    if clearCharacterBoostCustom ~= nil then
        clearCharacterBoostCustom(); -- 建号名单一次性使用: 进入游戏即清空 (须在 loadConfig 之后, 否则被重新加载)
    end
    if isMinimapOpen() and getPlayer() ~= nil then
        UIMovableMiniMap.openPanel();
    end
end

Events.OnGameStart.Add(onGameStart);

--*********************************************************
--* F9: 重置附近容器战利品 (与「战利品重掷」选项卡同入口)
--*********************************************************
function EtherMain.OnKeyPressed(key)
    if key == Keyboard.KEY_F9 and getPlayer() ~= nil and isMultiplayer() then
        EtherContainerPOC.reset();
    end
end

Events.OnKeyPressed.Add(EtherMain.OnKeyPressed);
Events.OnKeyPressed.Add(EtherMain.OnOpenPanel);