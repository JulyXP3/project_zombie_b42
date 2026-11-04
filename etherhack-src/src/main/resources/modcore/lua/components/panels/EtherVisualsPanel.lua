require "ISUI/ISPanel"

--*********************************************************
--* EtherVisualsPanel: ESP 开关面板
--*
--* 2026-08 重构 (用户需求): 打掉"总闸-子类-子开关"三层嵌套 ——
--*   - 四个功能模块 (addModule 带标题背景框, 与战利品页同款形态):
--*       玩家信息 / 独立功能 / 载具信息 / 僵尸信息;
--*   - 模块内开关一律平级: "显示指向载具的线条"不再要求先开
--*     "启用载具信息渲染" —— 只勾线条就只画线条 (Java 侧门控已同步扁平化);
--*   - 玩家模块取消总开关"启用玩家信息渲染" (它本身不画任何东西,
--*     昵称/信息/线条各自独立), 对应 Lua 全局与字段已删除;
--*   - "启用作弊信息渲染"(左下角署名 EtherCredits) 功能整体移除;
--*   - 总闸"启用视觉特效渲染"移入"独立功能"模块, 默认开启
--*     (只是布尔短路, 默认开不产生逐帧开销, 详见 Java 侧)。
--*
--* 2026-08 第二轮: "为本机玩家绘制视觉特效"整体移除 (其唯一作用是给自己
--*   头顶也画昵称/信息, 现本机玩家恒不绘制); 僵尸信息从"僵尸/生命: xx"
--*   两行文字改为头顶血条 (黑底 + 按生命绿->红填充); 新增"僵尸雷达"
--*   (150 格内僵尸 -> 本机玩家线条 + 距离数字, 与玩家/载具雷达同款);
--*   载具名称改用原版本地化名 (IGUI_VehicleName<scriptName>, 缺翻译回退
--*   脚本名); 六个开关标签改名去学术化。
--*
--* 2026-08 第三轮 (实机反馈): 模块顺序调整为 独立功能/玩家信息/载具信息/
--*   僵尸信息; 总闸更名"ESP总开关"; 载具信息改显示 马力/极速 (Java 侧
--*   getEnginePower); 僵尸血条改圆角长条 (矩形拼装, 见 Rendering.drawHpBar);
--*   雷达线条全部换 0.5px 细线; 多行文字按字体行高步进 (修高分辨率下
--*   固定 +10px 步距导致的两行重叠); 世界->屏幕换算去掉 B41 残留的
--*   "除以 zoom" (修缩放视角时 ESP 整体漂移/跟不上实体)。
--*
--* 契约: 沿用既有 toggleX/isX 全局, 名称与签名不变。
--*
--* 三百零一 动物种类多选: 原「动物种类」单选下拉改为 状态按钮 + 勾选弹窗
--*   (全部 / 已选 N 种 / 未选择); 弹窗挂 UIManager, 切页或收起主菜单时自动关闭;
--*   勾选在本地暂存, 点「确定」一次性写回 (setAnimalSpeciesAll / setAnimalSpeciesFilter)。
--*
--* 现况 (2026-09-21 审计核对): 僵尸信息实际只画**一行文字** `HP: <0-100>`
--*   (`core/CoreAPI.java` updateZombiesVisuals), 上面提到的血条/drawHpBar 已不存在
--*   (`Rendering` 无任何血条函数, 全库零命中) —— 标签/README 一律按"显示僵尸 HP"表述。
--*********************************************************

EtherVisualsPanel = EtherFormPanel:derive("EtherVisualsPanel");

--*********************************************************
--* 模块内一行复选框的高度预算 (与 addCheckbox 同一套折行/行距规则):
--* 长标签按模块内容宽折行; 单行紧凑 ROW_STEP, 多行按实际行高 + 盒内边距。
--*********************************************************
local function rowStep(item, w)
    if item.species then
        return EtherTheme.entryH + 10;      -- 多选行: 控件高 + 与下行的间隙
    end
    local title = tr(item.key);
    local availW = w - (18 + 10 + 8);
    if getTextManager():MeasureStringX(UIFont.Small, title) > availW then
        local n = #EtherTheme.wrapText(title, availW, UIFont.Small);
        return n * (EtherTheme.fontHgtSmall + 2) + EtherFormPanel.BOX_PAD_Y * 2 + 4;
    end
    return EtherFormPanel.ROW_STEP;
end

--*********************************************************
--* 在模块盒内 (x, y, w) 摆放一个复选框, 返回该行占用的行距。
--*********************************************************
local function moduleRow(panel, item, x, y, w)
    local title = tr(item.key);
    local tm = getTextManager();
    local availW = w - (18 + 10 + 8);
    local lines = { title };
    if tm:MeasureStringX(UIFont.Small, title) > availW then
        lines = EtherTheme.wrapText(title, availW, UIFont.Small);
    end

    local checked = false;
    if type(item.get) == "function" then
        checked = item.get() and true or false;
    elseif item.get then
        checked = true;
    end

    local cb = UICheckbox:new(x, y, title, checked, item.on);
    if #lines > 1 then
        cb.titleLines = lines;
        cb.height = math.max(18, #lines * (EtherTheme.fontHgtSmall + 2));
        cb.width = w;                         -- 多行时命中区域 = 整行
    end
    panel:addWidget(cb, item);
    return rowStep(item, w);
end

--*********************************************************
--* 动物模块 (二百二十六) 的物种多选行 (三百零一):
--* 右侧按钮显示当前状态 (全部 / 已选 N 种 / 未选择), 点击弹出勾选窗;
--* 选项与显示名都来自 Java (物种表炸开自游戏定义, 词条翻译在 Java 侧);
--* 未登记物种显示原始 id。
--*********************************************************
local function speciesSummaryText()
    if isAnimalSpeciesAll() then
        return tr("UI_VisualsPanel_AnimalSpecies_All");
    end
    local ids = getAnimalSpeciesIds();
    local n = 0;
    for i = 0, ids:size() - 1 do
        if isAnimalSpeciesSelected(ids:get(i)) then n = n + 1; end
    end
    if n == 0 then
        return tr("UI_VisualsPanel_AnimalSpecies_None");
    end
    return tr("UI_VisualsPanel_AnimalSpecies_Selected", { n = n });
end

--*********************************************************
--* 物种多选弹窗:
--*   - 独立面板挂 UIManager (不进滚动面板, 不会被裁剪/随内容滚走);
--*   - 勾选态只存在本地, 点「确定」才一次性写回配置 (只落盘一次);
--*   - 全部勾选 = 回到「全部」模式; 一种都不勾 + 确定 = 自定义空选择
--*     (不显示任何动物 — 与「全部」是两回事, Java 侧用独立开关区分)。
--*********************************************************
EtherAnimalSpeciesPopup = ISPanel:derive("EtherAnimalSpeciesPopup");
EtherAnimalSpeciesPopup.instance = nil;

local POPUP_PAD      = 10;
local POPUP_MIN_W    = 240;
local POPUP_MAX_ROWS = 8;          -- 超过 8 行启用滚动
local LIST_TOP_PAD   = 6;          -- 列表首行上内边距 (实测: 贴边太紧, "鸡"顶到框线)

local function speciesRowH()
    return EtherTheme.fontHgtSmall + 6;
end

-- 勾选列表容器: 只负责裁剪 + 滚动 (行数超过 POPUP_MAX_ROWS 时才可能滚动)
local SpeciesListPanel = ISPanel:derive("EtherSpeciesListPanel");

function SpeciesListPanel:prerender()
    self:setStencilRect(0, 0, self.width, self.height);
    ISPanel.prerender(self);
end

function SpeciesListPanel:render()
    self:clearStencilRect();
end

function EtherAnimalSpeciesPopup:new(ids, names, onApply)
    local rowH = speciesRowH();
    local titleH = EtherTheme.fontHgtSmall + 6;
    local contentH = ids:size() * rowH + LIST_TOP_PAD * 2;
    local listH = contentH;
    local maxListH = POPUP_MAX_ROWS * rowH + LIST_TOP_PAD * 2;
    if listH > maxListH then listH = maxListH; end
    if listH < rowH then listH = rowH; end

    -- 宽度: 标题 / 最长物种名 / 两个等宽按钮都要放得下
    local tm = getTextManager();
    local w = POPUP_MIN_W;
    for i = 0, names:size() - 1 do
        local need = 2 + 18 + 10 + tm:MeasureStringX(UIFont.Small, names:get(i)) + 20 + POPUP_PAD * 2;
        if need > w then w = need; end
    end
    local btnTitles = {
        tr("UI_VisualsPanel_AnimalSpecies_SelectAll"),
        tr("UI_VisualsPanel_AnimalSpecies_Clear"),
        tr("UI_VisualsPanel_AnimalSpecies_Apply"),
        tr("UI_VisualsPanel_AnimalSpecies_Cancel"),
    };
    local needBtn = UIButton.measureGroupWidth(btnTitles) * 2 + POPUP_PAD * 2 + EtherTheme.ctrlGap;
    if needBtn > w then w = needBtn; end

    local listY = POPUP_PAD + titleH + 4;
    local h = listY + listH + 8 + EtherTheme.ctrlH + 6 + EtherTheme.ctrlH + POPUP_PAD;

    local o = ISPanel:new(0, 0, w, h);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.ids = ids;
    o.names = names;
    o.checks = {};
    o.listY = listY;
    o.listH = listH;
    o.contentH = contentH;
    o.onApply = onApply;
    return o;
end

function EtherAnimalSpeciesPopup:createChildren()
    ISPanel.createChildren(self);

    local rowH = speciesRowH();
    local listW = self.width - POPUP_PAD * 2;
    local list = SpeciesListPanel:new(POPUP_PAD, self.listY, listW, self.listH);
    list:initialise();
    list:instantiate();
    self.listPanel = list;
    self:addChild(list);
    if self.contentH > self.listH then
        list:setScrollChildren(true);
        list:setScrollHeight(self.contentH);
        list:addScrollBars();
    end

    for i = 0, self.ids:size() - 1 do
        local cb = UICheckbox:new(2, LIST_TOP_PAD + i * rowH, self.names:get(i), isAnimalSpeciesSelected(self.ids:get(i)));
        cb.width = listW - 20;                   -- 命中区域 = 整行 (右侧留出滚动条)
        cb:initialise();
        cb:instantiate();
        list:addChild(cb);
        self.checks[i + 1] = cb;
    end

    local btnW = (self.width - POPUP_PAD * 2 - EtherTheme.ctrlGap) / 2;
    local by1 = self.listY + self.listH + 8;
    local by2 = by1 + EtherTheme.ctrlH + 6;
    local allBtn = UIButton:new(POPUP_PAD, by1, btnW, EtherTheme.ctrlH,
        tr("UI_VisualsPanel_AnimalSpecies_SelectAll"), function() self:setAllChecked(true); end, btnW);
    local clearBtn = UIButton:new(POPUP_PAD + btnW + EtherTheme.ctrlGap, by1, btnW, EtherTheme.ctrlH,
        tr("UI_VisualsPanel_AnimalSpecies_Clear"), function() self:setAllChecked(false); end, btnW);
    local cancelBtn = UIButton:new(POPUP_PAD, by2, btnW, EtherTheme.ctrlH,
        tr("UI_VisualsPanel_AnimalSpecies_Cancel"), function() self:close(); end, btnW);
    local applyBtn = UIButton:new(POPUP_PAD + btnW + EtherTheme.ctrlGap, by2, btnW, EtherTheme.ctrlH,
        tr("UI_VisualsPanel_AnimalSpecies_Apply"), function() self:apply(); end, btnW);
    self:addChild(allBtn);
    self:addChild(clearBtn);
    self:addChild(cancelBtn);
    self:addChild(applyBtn);
end

-- 背景/描边/标题必须画在 prerender: PZ 的 UIElement.render 顺序是
-- 本控件 prerender -> 子控件 render -> 本控件 render, 画在 render 会盖住按钮。
function EtherAnimalSpeciesPopup:prerender()
    local bg = EtherTheme.glassBG;
    self:drawRect(0, 0, self.width, self.height, 0.97, bg.r, bg.g, bg.b);
    EtherTheme.drawTileBox(self, 0, 0, self.width, self.height, true, 8);
    local tc = EtherTheme.text;
    self:drawText(tr("UI_VisualsPanel_AnimalSpecies"), POPUP_PAD, POPUP_PAD - 1,
        tc.r, tc.g, tc.b, 1.0, UIFont.Small);
end

function EtherAnimalSpeciesPopup:setAllChecked(checked)
    for i = 1, #self.checks do
        self.checks[i]:setCheked(checked);
    end
end

function EtherAnimalSpeciesPopup:apply()
    local parts = {};
    local allChecked = true;
    for i = 0, self.ids:size() - 1 do
        -- 读字段不要调 :isChecked(): UICheckbox:new 把同名字段 isChecked 设在实例上,
        -- 字段会遮住类方法, cb:isChecked() 实际是"调用布尔值" -> Kahlua 报
        -- "Object true did not have __call metatable set" (三百零四 实测报错)。
        if self.checks[i + 1].isChecked then
            table.insert(parts, self.ids:get(i));
        else
            allChecked = false;
        end
    end
    if allChecked and self.ids:size() > 0 then
        setAnimalSpeciesAll(true);
    else
        setAnimalSpeciesFilter(table.concat(parts, ","));
    end
    if self.onApply ~= nil then self.onApply(); end
    self:close();
end

function EtherAnimalSpeciesPopup:onMouseWheel(del)
    local list = self.listPanel;
    if list ~= nil and self.contentH > list.height then
        list:setYScroll(list:getYScroll() - del * 40);
        return true;
    end
    return false;
end

-- 主菜单整体隐藏 (菜单键收起/关闭) 时, 挂 UIManager 的弹窗不会随父面板隐藏 -> 自查收敛
function EtherAnimalSpeciesPopup:update()
    local main = EtherMain.instance;
    if main == nil or not main:getIsVisible() then
        self:close();
    end
end

function EtherAnimalSpeciesPopup:close()
    if EtherAnimalSpeciesPopup.instance == self then
        EtherAnimalSpeciesPopup.instance = nil;
    end
    self:setVisible(false);
    self:removeFromUIManager();
end

local function moduleSpeciesRow(panel, item, x, y, w)
    local title = tr(item.key);
    local tm = getTextManager();
    local entryH = EtherTheme.entryH;

    local label = EtherTheme.makeLabel(x, y, entryH, title);
    panel:addChild(label);
    panel:_track(label, item);

    -- 按钮宽按三种状态文案里最宽的算, 状态切换后不跳宽
    local btnW = UIButton.measureWidth(tr("UI_VisualsPanel_AnimalSpecies_All"));
    local noneW = UIButton.measureWidth(tr("UI_VisualsPanel_AnimalSpecies_None"));
    local selW = UIButton.measureWidth(tr("UI_VisualsPanel_AnimalSpecies_Selected", { n = 99 }));
    if noneW > btnW then btnW = noneW; end
    if selW > btnW then btnW = selW; end
    if btnW < 120 then btnW = 120; end
    local labelW = tm:MeasureStringX(UIFont.Small, title);
    local maxW = w - labelW - 12 - EtherFormPanel.BOX_PAD_X;
    if maxW < 60 then maxW = 60; end
    if btnW > maxW then btnW = maxW; end
    local bx = x + w - btnW - EtherFormPanel.BOX_PAD_X;
    if bx < x + labelW + 12 then bx = x + labelW + 12; end

    local btn;
    btn = UIButton:new(bx, y, btnW, entryH, speciesSummaryText(), function()
        panel:openSpeciesPopup();
    end, btnW);
    panel:addWidget(btn, item);
    panel.speciesBtn = btn;
end

--*********************************************************
--* 构建表单内容 (由基类 createChildren 在 instantiate 时回调)。
--* 描述表在运行时构建, 确保引用的全局已暴露。
--*********************************************************
function EtherVisualsPanel:build()
    local modules = {
        {
            title = "UI_VisualsPanel_Group_Standalone",
            items = {
                { key = "UI_VisualsPanel_IsVisualsEnable", on = toggleVisualsEnable,         get = isVisualsEnable },
                { key = "UI_VisualsPanel_360Vision",       on = toggleVisualEnable360Vision, get = isVisualEnable360Vision },
            },
        },
        {
            title = "UI_VisualsPanel_Group_Players",
            items = {
                { key = "UI_VisualsPanel_DrawPlayerName",   on = toggleVisualDrawPlayerNickname, get = isVisualDrawPlayerNickname },
                { key = "UI_VisualsPanel_DrawPlayerInfo",   on = toggleVisualDrawPlayerInfo,     get = isVisualDrawPlayerInfo },
                { key = "UI_VisualsPanel_DrawLineToPlayers", on = toggleVisualDrawLineToPlayers, get = isVisualDrawLineToPlayers },
            },
        },
        {
            title = "UI_VisualsPanel_Group_Vehicles",
            items = {
                { key = "UI_VisualsPanel_IsVisualsVehiclesEnable", on = toggleVisualsVehiclesEnable,   get = isVisualsVehiclesEnable },
                { key = "UI_VisualsPanel_DrawLineToVehicles",      on = toggleVisualDrawLineToVehicle, get = isVisualDrawLineToVehicle },
            },
        },
        -- E2 鼠标拾取模块 (八十八): **2026-09-14 九十一 用户裁定暂时隐藏** —— 目前没有消费方
        -- (原定给 F 类对象编辑/D2; D2 成品不需要它)。接口 (MousePickAPI/EtherPick/SelfProbe)
        -- 全部保留, 将来接 F 类时把下面这块取消注释即可恢复 UI。
        -- {
        --     title = "UI_Pick_Group",
        --     items = {
        --         { key = "UI_Pick_HoverOutline", on = setPickHover, get = isPickHover },
        --         { key = "UI_Pick_ShowInfo",     on = setPickInfo,  get = isPickInfo },
        --     },
        -- },
        {
            title = "UI_VisualsPanel_Group_Zombies",
            items = {
                { key = "UI_VisualsPanel_IsVisualsZombiesEnable", on = toggleVisualsZombiesEnable,     get = isVisualsZombiesEnable },
                { key = "UI_VisualsPanel_DrawLineToZombies",      on = toggleVisualDrawLineToZombies,  get = isVisualDrawLineToZombies },
            },
        },
        {
            title = "UI_VisualsPanel_Group_Animals",
            items = {
                { key = "UI_VisualsPanel_AnimalSpecies", species = true },
                { key = "UI_VisualsPanel_IsVisualsAnimalsEnable", on = toggleVisualsAnimalsEnable,    get = isVisualsAnimalsEnable },
                { key = "UI_VisualsPanel_DrawLineToAnimals",      on = toggleVisualDrawLineToAnimals, get = isVisualDrawLineToAnimals },
                { key = "UI_VisualsPanel_IsVisualsTrailersEnable", on = toggleVisualsTrailersEnable, get = isVisualsTrailersEnable },
            },
        },
    };

    for mi = 1, #modules do
        if mi > 1 then
            self:addSpacer(EtherFormPanel.SECTION_GAP);
        end
        local mod = modules[mi];
        -- 先按折行预算量出内容高, 再立模块盒; builder 在标题行之下逐行摆放
        local w = self:_rowContentW();
        local contentH = 0;
        for i = 1, #mod.items do
            contentH = contentH + rowStep(mod.items[i], w);
        end
        self:addModule(mod.title, contentH + 2, function(bx, by, bw)
            local cy = by;
            for i = 1, #mod.items do
                local item = mod.items[i];
                if item.species then
                    moduleSpeciesRow(self, item, bx, cy, bw);
                    cy = cy + rowStep(item, bw);
                else
                    cy = cy + moduleRow(self, item, bx, cy, bw);
                end
            end
        end);
    end
end

--*********************************************************
--* 打开/收起物种多选弹窗 (再次点同一按钮 = 收起)。
--* 位置取点击时的鼠标坐标 (刚点在按钮上): 弹窗左上落在光标处, 放不下就向左/上翻;
--* 不用控件绝对坐标 —— 面板开了滚动子控件, 滚动后的绝对坐标不可靠。
--*********************************************************
function EtherVisualsPanel:openSpeciesPopup()
    if EtherAnimalSpeciesPopup.instance ~= nil then
        EtherAnimalSpeciesPopup.instance:close();
        return;
    end
    local popup = EtherAnimalSpeciesPopup:new(getAnimalSpeciesIds(), getAnimalSpeciesNames(), function()
        if self.speciesBtn ~= nil then
            self.speciesBtn.title = speciesSummaryText();
        end
    end);
    popup:initialise();
    popup:instantiate();

    local sw, sh = getCore():getScreenWidth(), getCore():getScreenHeight();
    local px, py = getMouseX(), getMouseY() + 2;
    if px + popup.width > sw - 8 then px = sw - popup.width - 8; end
    if px < 8 then px = 8; end
    if py + popup.height > sh - 8 then py = py - popup.height - 18; end
    if py < 8 then py = 8; end
    popup:setX(px);
    popup:setY(py);

    popup:addToUIManager();
    popup:setVisible(true);
    EtherAnimalSpeciesPopup.instance = popup;
end

--*********************************************************
--* 切页时面板被隐藏 (UIButtonsPanel:openPanel), 弹窗挂 UIManager 不会跟着隐藏
--* -> 面板一隐藏就主动收起, 避免残留在别的页面上。
--*********************************************************
function EtherVisualsPanel:setVisible(visible)
    ISPanel.setVisible(self, visible);
    if not visible and EtherAnimalSpeciesPopup.instance ~= nil then
        EtherAnimalSpeciesPopup.instance:close();
    end
end

--*********************************************************
--* 新实例构造由 EtherFormPanel:new 继承 (透明底 + 可拖动 + 实例级游标/控件表)。
--* 无需在此重写 :new / :createChildren / :prerender / :render / :onMouseWheel。
--*********************************************************
