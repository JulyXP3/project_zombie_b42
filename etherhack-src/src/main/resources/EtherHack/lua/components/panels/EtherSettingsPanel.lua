require "ISUI/ISPanel"

--*********************************************************
--* 设置面板 (已迁移到 EtherFormPanel: 游标布局 + 统一 add* API)
--*
--* 迁移前的问题 (UI重构方案.md P1/P2/P3/P7):
--*   - 一个 self.rows 被多个 helper 共用, 基数在 20 / 400 之间跳,
--*     布局靠调用顺序撑住, 增删一行就要重算一片偏移;
--*   - 自实现 addLabel/addButton/addSlider/addCheckBox, 与其他面板签名不一;
--*   - checkBoxList / buttonList / rows 挂在类表上 (跨实例共享);
--*   - 4 个取色器块 + 3 个配置按钮块几乎逐行重复。
--* 迁移后: 位置由基类游标自动排布, 取色器/配置动作降为数据表 + 循环,
--*   控件样式统一走 EtherTheme, 状态改为实例级。
--*
--* 红线 (零功能影响): saveConfig/loadConfig/deleteConfig/getConfigList、
--*   get/setAccentUIColor、get/setPlayersUIColor、get/setVehicleUIColor、
--*   get/setZombieUIColor、get/setLanguage、getCore():ResetLua
--*   等全局契约的名称与参数一律未改, 只改"控件怎么摆、文案怎么取"。
--* 例外 (用户明确要求的功能变更): 面板尺寸固定 888x888, 故移除尺寸调整行,
--*   不再调用 setPanelSize (Java 侧该方法仍保留)。
--*********************************************************
EtherSettingsPanel = EtherFormPanel:derive("EtherSettingsPanel");

--*********************************************************
--* 四个颜色项: 数据表 + 循环 (取代原先 4 份近乎相同的 ~12 行块)
--* 每项: 翻译键 + 取色函数 + 应用函数 (全局契约原样引用)
--*********************************************************
local function colorPickers()
    return {
        {
            key = "UI_Settings_AccentColor",
            get = getAccentUIColor,
            set = function(r, g, b)
                setAccentUIColor(r, g, b);
                -- accent 变更需同步到主窗缓存, 供各处绘制取用
                EtherMain.accentColor = {
                    r = getAccentUIColor():getR(),
                    g = getAccentUIColor():getG(),
                    b = getAccentUIColor():getB(),
                    a = 1.0,
                };
            end,
        },
        { key = "UI_Settings_PlayersColor", get = getPlayersUIColor, set = setPlayersUIColor },
        { key = "UI_Settings_VehicleColor", get = getVehicleUIColor, set = setVehicleUIColor },
        { key = "UI_Settings_ZombiesColor", get = getZombieUIColor, set = setZombieUIColor },
    };
end

--*********************************************************
--* 面板内容构建 (基类在 createChildren 里回调 build)
--*********************************************************
function EtherSettingsPanel:build()
    local tm = getTextManager();
    local ctrlH = EtherTheme.ctrlH;
    local gap = EtherTheme.ctrlGap;

    -- ================= 配置管理 =================
    -- 配置列表模块: 标题在模块盒内顶部 (---- 配置列表 ----), 列表在标题之下
    -- 注意: ISScrollingListBox 的列头画在列表 y 之上(自身边界外), 故为它预留
    -- LIST_HEADER_H, 否则"名称"表头会顶出行盒上缘 (实测缺陷)。
    local listH = 170;
    local hdrH = EtherFormPanel.LIST_HEADER_H;
    self:addModule("UI_Settings_ConfigTitle", listH + hdrH, function(bx, by, bw)
        self.configs = ISScrollingListBox:new(bx, by + hdrH, bw, listH);
        self.configs:initialise();
        self.configs:instantiate();
        self.configs.itemheight = EtherTheme.listItemH;
        self.configs.selected = 0;
        self.configs.joypadParent = self;
        self.configs.font = UIFont.NewSmall;
        self.configs.doDrawItem = self.drawConfigs;
        EtherTheme.styleList(self.configs);
        self.configs.drawBorder = false;      -- 行盒已提供边框, 避免直角边框与切角框重叠
        self.configs:addColumn(getTranslate("UI_Settings_ConfigName"), 0);
        self:_anchor(self.configs);
        self:addChild(self.configs);
    end);

    -- 名称输入 + 保存/加载/删除 (两行: 输入框上, 按钮居中下)
    self:addCustomRow(ctrlH * 2 + gap, function(bx, by, bw)
        local actions = {
            {
                key = "UI_Settings_ConfigSave",
                run = function()
                    local name = self.entry:getText();
                    if name ~= "" then
                        saveConfig(name);
                        self:updateConfigsList();
                    end
                end,
                -- 名称为空时禁用
                canRun = function() return self.entry:getText() ~= ""; end,
            },
            {
                key = "UI_Settings_ConfigLoad",
                run = function()
                    local name = self:selectedConfig();
                    if name ~= nil then
                        loadConfig(name);
                        EtherMain.accentColor = {
                            r = getAccentUIColor():getR(),
                            g = getAccentUIColor():getG(),
                            b = getAccentUIColor():getB(),
                            a = 1.0,
                        };
                    end
                end,
                canRun = function() return self:selectedConfig() ~= nil; end,
            },
            {
                key = "UI_Settings_ConfigDelete",
                run = function()
                    local name = self:selectedConfig();
                    if name ~= nil then
                        deleteConfig(name);
                        self:updateConfigsList();
                    end
                end,
                canRun = function() return self:selectedConfig() ~= nil; end,
            },
            {
                -- 重置默认设置: 空配置走 loadConfig 缺省分支 (颜色/全部开关/编译选项/建号名单),
                -- 写回 startup.properties 持久化; 语言与配置档案文件不动
                key = "UI_Settings_ResetDefaults",
                run = function()
                    resetConfig();
                    saveConfig("startup");
                    -- Lua 侧镜像 flag 置空: 菜单重建后 ensureDrawFlags 从 Java 缺省重初始化
                    UIMap.drawZombies = nil; UIMap.drawVehicles = nil; UIMap.drawAllPlayers = nil;
                    UIMap.drawLocalPlayer = nil; UIMap.drawItems = nil; UIMap.drawItemEsp = nil;
                    EtherMain.accentColor = {
                        r = getAccentUIColor():getR(),
                        g = getAccentUIColor():getG(),
                        b = getAccentUIColor():getB(),
                        a = 1.0,
                    };
                    -- 整菜单重建: 所有面板勾选框按默认值重画
                    EtherMain.instance:removeFromUIManager();
                    EtherMain.instance = nil;
                    EtherMain.OnOpenPanel(EtherMain.menuKeyID);
                end,
                canRun = function() return true; end,
            },
        };

        -- 实机定稿: 输入框在左吃剩余宽, 三个等宽按钮靠右;
        -- 盒子是两倍控件高, 输入框(entryH)与按钮(ctrlH)各自按真实高度在盒内上下居中
        local actionTitles = {};
        for i = 1, #actions do
            table.insert(actionTitles, getTranslate(actions[i].key));
        end
        local btnW = UIButton.measureGroupWidth(actionTitles);
        local maxBtnW = math.floor((bw - 110 - gap * #actions) / #actions);
        if maxBtnW < 40 then maxBtnW = 40; end
        if btnW > maxBtnW then btnW = maxBtnW; end

        local btnsW = (btnW + gap) * #actions - gap;
        local entryW = bw - btnsW - gap;
        if entryW < 90 then entryW = 90; end
        local boxH = ctrlH * 2 + gap;
        local midY = by + math.floor((boxH - EtherTheme.entryH) / 2);   -- 输入框
        local midBtnY = by + math.floor((boxH - ctrlH) / 2);            -- 按钮
        self.entry = ISTextEntryBox:new(
            "EtherConfig-" .. tostring(getConfigList():size() + 1), bx, midY, entryW, EtherTheme.entryH);
        EtherTheme.styleEntry(self.entry);
        self.entry:initialise();
        self.entry:instantiate();
        self:_anchor(self.entry);
        self:addChild(self.entry);

        -- 按钮靠右, 上下居中 (各自按真实高度)
        local cx = bx + bw - btnsW;
        for i = 1, #actions do
            local act = actions[i];
            local btn = UIButton:new(cx, midBtnY, btnW, ctrlH, getTranslate(act.key), act.run, btnW);
            -- 按钮可用性随输入/选中态实时变化 (与迁移前一致)
            btn.update = function()
                btn.isEnable = act.canRun() and true or false;
            end
            self:addWidget(btn);
            cx = cx + btnW + gap;
        end
    end);

    -- ================= 界面颜色 =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    local pickers = colorPickers();
    for i = 1, #pickers do
        local p = pickers[i];
        self:addColorPicker(p.key, p.get, p.set);
    end

    -- ================= 语言 / 重载 =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);

    -- 语言按 CN -> RU -> EN -> CN 轮转 (与迁移前一致)
    local current = getLanguage();
    local nextLang = "CN";
    if current == "CN" then nextLang = "RU";
    elseif current == "RU" then nextLang = "EN"; end

    self:addLabeledButton("UI_Settings_Language", nextLang, function()
        setLanguage(nextLang);
        EtherMain.instance:removeFromUIManager();
        EtherMain.instance = nil;
        EtherMain.OnOpenPanel(EtherMain.menuKeyID);
    end);

    self:addLabeledButton("UI_Settings_ResetLuaLabel",
        getTranslate("UI_Settings_ResetLuaButton"), function()
            getCore():ResetLua("default", "Force")
        end, { onlyNotInGame = true });

    -- 注: 面板尺寸已固定 888x888 (EtherHackMenu 常量), 故不再提供尺寸调整行。

    self:updateConfigsList();
end

--*********************************************************
--* 当前选中的配置名 (无选中返回 nil; 集中一处避免各按钮各写一遍越界判断)
--*********************************************************
function EtherSettingsPanel:selectedConfig()
    if self.configs == nil or self.configs.items == nil then return nil; end
    local sel = self.configs.selected;
    if sel == nil or sel < 1 or sel > #self.configs.items then return nil; end
    local row = self.configs.items[sel];
    if row == nil then return nil; end
    return row.item;
end

--*********************************************************
--* 刷新配置列表 (保留当前选中项)
--*********************************************************
function EtherSettingsPanel:updateConfigsList()
    if self.configs == nil then return; end
    self.lastSelectedIndex = self.configs.selected or 0;
    self.configs:clear();

    local configList = getConfigList();
    for i = 0, configList:size() - 1 do
        self.configs:addItem("Config", configList:get(i));
    end
    self.configs.selected = self.lastSelectedIndex;
end

--*********************************************************
--* 配置列表行绘制 (选中高亮 + 交替底色)
--* 注: 由 ISScrollingListBox 以列表自身为 self 调用。
--*********************************************************
function EtherSettingsPanel:drawConfigs(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);

    local th = EtherTheme;
    self:drawText(tostring(item.item), 8, y + 4, th.text.r, th.text.g, th.text.b, 1, UIFont.Small);

    return y + self.itemheight;
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
