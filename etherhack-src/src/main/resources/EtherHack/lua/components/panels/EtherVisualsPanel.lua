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
--*********************************************************

EtherVisualsPanel = EtherFormPanel:derive("EtherVisualsPanel");

--*********************************************************
--* 模块内一行复选框的高度预算 (与 addCheckbox 同一套折行/行距规则):
--* 长标签按模块内容宽折行; 单行紧凑 ROW_STEP, 多行按实际行高 + 盒内边距。
--*********************************************************
local function rowStep(item, w)
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
        {
            title = "UI_VisualsPanel_Group_Zombies",
            items = {
                { key = "UI_VisualsPanel_IsVisualsZombiesEnable", on = toggleVisualsZombiesEnable,     get = isVisualsZombiesEnable },
                { key = "UI_VisualsPanel_DrawLineToZombies",      on = toggleVisualDrawLineToZombies,  get = isVisualDrawLineToZombies },
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
                cy = cy + moduleRow(self, mod.items[i], bx, cy, bw);
            end
        end);
    end
end

--*********************************************************
--* 新实例构造由 EtherFormPanel:new 继承 (透明底 + 可拖动 + 实例级游标/控件表)。
--* 无需在此重写 :new / :createChildren / :prerender / :render / :onMouseWheel。
--*********************************************************
