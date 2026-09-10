require "ISUI/ISPanel"

--*********************************************************
--* Server Sync Protection (prevents server from overwriting local changes)
--* 注: ISChatFix / ServerSyncBlocker 不再 require, 由 EtherLuaManager 用 RunLua 直接加载
--* (PZ vanilla require loader 找不到 modcore/lua 下的文件)
--*********************************************************

--*********************************************************
--* Подключение модулей
--*********************************************************
local etherModules = {
    "modcore/lua/components/ui/EtherTheme.lua",
    "modcore/lua/components/ui/EtherI18n.lua",
    "modcore/lua/components/ui/EtherKeyBinds.lua",
    "modcore/lua/components/ui/EtherFormPanel.lua",
    "modcore/lua/components/override/EtherAdminMenu.lua",
    "modcore/lua/components/override/EtherDebugMenu.lua",
    "modcore/lua/components/override/EtherEditInventoryItem.lua",
    "modcore/lua/components/override/EtherEditWorldObjects.lua",
    "modcore/lua/components/ui/UIButtonsPanel.lua",
    "modcore/lua/components/ui/UICheckbox.lua",
    "modcore/lua/components/ui/UIButton.lua",
    "modcore/lua/components/ui/UISlider.lua",
    "modcore/lua/components/ui/UIMechanics.lua",
    "modcore/lua/components/ui/UIModalAddXP.lua",
    "modcore/lua/components/ui/UIMovableMiniMap.lua",
    "modcore/lua/components/ui/UIModalAddTrait.lua",
    "modcore/lua/components/ui/UIHealth.lua",
    "modcore/lua/components/ui/UIItemTables.lua",
    "modcore/lua/components/ui/EtherItemSearch.lua",
    "modcore/lua/components/ui/EtherTrapPOC.lua",
    "modcore/lua/components/ui/EtherFishSpawn.lua",
    "modcore/lua/components/ui/EtherRadioXp.lua",
    "modcore/lua/components/ui/EtherExchange.lua",
    "modcore/lua/components/ui/EtherAmmoFarm.lua",
    "modcore/lua/components/ui/UIMap.lua",
    -- 自动驾驶地图锚定 (drive 域): 在 UIMap 之后加载
    "modcore/lua/components/drive/AutoDriveMap.lua",
    "modcore/lua/components/ui/UISkillTable.lua",
    "modcore/lua/components/ui/UITraitsTable.lua",
    "modcore/lua/components/panels/EtherInfoPanel.lua",
    "modcore/lua/components/panels/EtherCharacterPanel.lua",
    "modcore/lua/components/ui/EtherTempWeapon.lua",
    "modcore/lua/components/panels/EtherCombatPanel.lua",
    "modcore/lua/components/panels/EtherItemCreator.lua",
    "modcore/lua/components/panels/EtherRadarPanel.lua",
    "modcore/lua/components/panels/EtherTrapSpawn.lua",
    "modcore/lua/components/panels/EtherExchangePanel.lua",
    "modcore/lua/components/panels/EtherFunPanel.lua",
    "modcore/lua/components/panels/EtherPlayerEditor.lua",
    "modcore/lua/components/panels/EtherVisualsPanel.lua",
    "modcore/lua/components/panels/EtherMapPanel.lua",
    "modcore/lua/components/panels/EtherExploitPanel.lua",
    "modcore/lua/components/panels/EtherLootRollPanel.lua",
    "modcore/lua/components/panels/EtherVehiclePanel.lua",
    -- 自动驾驶模块 (drive 域, 载具页 EtherVehiclePanel 构建时追加)
    "modcore/lua/components/drive/EtherDriveModule.lua",
    "modcore/lua/components/override/EtherCharacterCreation.lua",
    "modcore/lua/components/panels/EtherCharacterBoostPanel.lua",
    "modcore/lua/components/panels/EtherFarmingPanel.lua",
    "modcore/lua/components/panels/EtherSettingsPanel.lua",
    "modcore/lua/components/panels/EtherKeyBindsPanel.lua"
}

for _, module in ipairs(etherModules) do
    loadModuleScript(module);
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherMain                   = ISPanel:derive("EtherMain"); -- Наследование от ISPanel
EtherMain.instance          = nil; --Экземпляр окна
-- 呼出菜单键已迁入 EtherKeyBinds 绑定框架 (featureId="menu", 默认 Insert/210),
-- 可在「设置」页改绑; 此处不再持有硬编码键位
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
--* 主菜单收起时联动收起子面板 (按键绑定等): 否则菜单键切换主面板后,
--* 子面板悬空在游戏画面上, 主次结构断裂 (实测反馈)
--*********************************************************
function EtherMain:close()
	EtherMain.instance:setVisible(false);
    EtherMain.instance:removeFromUIManager();
    if EtherKeyBindsPanel ~= nil and EtherKeyBindsPanel.instance ~= nil then
        EtherKeyBindsPanel.close();
    end
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

    self.buttonsPanel:addButton("modcore/media/ui/info.png", "UI_Nav_Info", EtherInfoPanel);
    -- 「生存」(原「角色」): 战斗功能拆出后仅剩生存向作弊, 名称随之更贴切
    self.buttonsPanel:addButton("modcore/media/ui/character.png", "UI_Nav_Character", EtherCharacterPanel);
    -- 「战斗」: 战斗强化(自角色页迁入) + 超级群攻 + 临时武器
    self.buttonsPanel:addButton("modcore/media/ui/combat.png", "UI_Nav_Combat", EtherCombatPanel);
    self.buttonsPanel:addButton("modcore/media/ui/itemCreator.png", "UI_Nav_Items", EtherItemCreator);
    self.buttonsPanel:addButton("modcore/media/ui/radar.png", "UI_Nav_Radar", EtherRadarPanel);
    self.buttonsPanel:addButton("modcore/media/ui/trap.png", "UI_Nav_Traps", EtherTrapSpawn);
    -- 「等价交换」: 任意物品生成 (红队链), 位于「陷阱」之后 (同为物品生成类)
    self.buttonsPanel:addButton("modcore/media/ui/exchange.png", "UI_Nav_Exchange", EtherExchangePanel);
    self.buttonsPanel:addButton("modcore/media/ui/playerEditor.png", "UI_Nav_Player", EtherPlayerEditor);
    self.buttonsPanel:addButton("modcore/media/ui/visuals.png", "UI_Nav_Visuals", EtherVisualsPanel);
    self.buttonsPanel:addButton("modcore/media/ui/teleport.png", "UI_Nav_Teleport", EtherMapPanel);
    self.buttonsPanel:addButton("modcore/media/ui/loot.png", "UI_Nav_Loot", EtherLootRollPanel);
    self.buttonsPanel:addButton("modcore/media/ui/vehicle.png", "UI_Nav_Vehicle", EtherVehiclePanel);
    -- 「耕种」: 原角色页「作弊耕种模式」开关的面板化替代, 位于「载具」之后
    self.buttonsPanel:addButton("modcore/media/ui/farming.png", "UI_Nav_Farming", EtherFarmingPanel);
    -- 「趣味」: 整活功能集中页 (红队 PoC: 冒名发消息/僵尸皮肤), 位于「耕种」之后
    self.buttonsPanel:addButton("modcore/media/ui/fun.png", "UI_Nav_Fun", EtherFunPanel);
    -- 「创建角色」: 建号相关功能集中页 (自定义编辑/建号增强), 位于「耕种」与「其他」之间。
    self.buttonsPanel:addButton("modcore/media/ui/characterBoost.png", "UI_Nav_CharacterBoost", EtherCharacterBoostPanel);
    -- 「其他」(原「漏洞」): 位置固定在「耕种」与「设置」之间。
    self.buttonsPanel:addButton("modcore/media/ui/exploit.png", "UI_Nav_Exploit", EtherExploitPanel);
    self.buttonsPanel:addButton("modcore/media/ui/settings.png", "UI_Nav_Settings", EtherSettingsPanel);

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
--* Логика открытия и закрытия меню (绑定框架分发: 见文件尾 register)
--*********************************************************
function EtherMain.toggleMenu()
    -- Если панель уже существует, переключаем видимость (состояние вкладок/прокрутки сохраняется)
    if EtherMain.instance ~= nil then
        if EtherMain.instance:getIsVisible() then
            EtherMain.instance:setVisible(false);
            EtherMain.instance:removeFromUIManager();
            -- 菜单键收起主面板时联动收起子面板 (同 EtherMain:close)
            if EtherKeyBindsPanel ~= nil and EtherKeyBindsPanel.instance ~= nil then
                EtherKeyBindsPanel.close();
            end
        else
            EtherMain.instance:addToUIManager();
            EtherMain.instance:setVisible(true);
        end
        return
    end

    -- Создаем новую панель
    -- 菜单重建 => 面板实例缓存全失效: 缓存面板的标题/说明文字是构建时按当时语言
    -- 与配置烘焙的, 语言切换/重置设置重建菜单后复用旧实例会永远停在旧文案
    -- (修: 切换语言不生效)。单纯开关菜单走上方可见性分支, 缓存保留 (保输入状态)。
    UIButtonsPanel.panelCache = nil;
    EtherMain.instance  = EtherMain:new();
    EtherMain.instance:initialise();
    EtherMain.instance:instantiate();
    EtherMain.instance:addToUIManager();
    EtherMain.instance:setVisible(true);
    EtherMain.instance:setAlwaysOnTop(false);
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
print("[modcore] Build 42 Version Loaded")
print("[modcore] WARNING: Most features are experimental!")
print("[modcore] CONFIRMED WORKING: Item Spawner, Radar/ESP")
print("[modcore] EXPERIMENTAL: God Mode, Invisible, Cheats")
print("=======================================================")

--*********************************************************
--* 自动恢复上次状态: 读取 startup 配置(小地图开关/图层/视觉效果等),
--* 若上次关闭游戏时小地图处于打开状态则重新打开
--*********************************************************
local function onGameStart()
    UIMovableMiniMap.instance = nil; -- 上一场游戏的旧实例已失效, 避免 openPanel 误判为已打开
    EtherMain.instance = nil; -- 同上: 旧菜单实例已失效, 下次按键重新构建
    loadConfig("startup");
    if type(EtherKeyBinds.refresh) == "function" then
        EtherKeyBinds.refresh(); -- 配置加载后重读按键绑定
    end
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
-- 呼出菜单改走绑定框架 (默认 Insert=210, 可在设置页改绑/还原)
if type(EtherKeyBinds.register) == "function" then
    EtherKeyBinds.register("menu", "UI_KeyBind_Menu", function()
        EtherMain.toggleMenu();
    end, 210);
end