require "ISUI/ISPanel"

--*********************************************************
--* EtherFormPanel: 表单式面板统一基类 (游标布局 + 统一 add* API)
--*
--* 目标 (对应 UI重构方案.md 的 P1/P2/P3/P7):
--*   - 消除各面板手算像素 (y = 基数 + 行号*步长, 且步长各处不一);
--*   - 消除各面板各自实现、签名不一的 add* helper;
--*   - 消除挂在类表上的共享 self.rows / self.uiElements (改为实例级);
--*   - 消除每个控件后重复的 initialise/instantiate/anchor 样板。
--*
--* 红线 (零功能影响): 本基类只负责"控件如何创建与摆放", 不承载任何
--*   作弊功能逻辑; 子类通过传入的回调/取值函数引用既有全局 (toggleX/
--*   isX/setX 等), 名称与签名保持冻结契约, 一律不改。
--*
--* 用法:
--*   MyPanel = EtherFormPanel:derive("MyPanel");
--*   function MyPanel:build()              -- 子类只实现 build, 不碰 createChildren
--*       self:addCheckbox("UI_Key", onToggle, getState, { onlyInGame = true });
--*       self:addSpacer(EtherFormPanel.SECTION_GAP);
--*       ...
--*   end
--*   -- :new / createChildren / prerender / render / onMouseWheel 全部继承本基类。
--*
--* 布局标记 (design tokens): 采用 8px 密集网格 (dense dashboard 档),
--*   与 EtherTheme 现有 rowH/btnH 对齐; 集中在此处, 不再散落各面板。
--*********************************************************

EtherFormPanel = ISPanel:derive("EtherFormPanel");

--*********************************************************
--* UIRowBox: 功能行的切角边框 (只负责画一个盒子, 不参与交互)。
--*
--* 为什么做成子控件而不是在 prerender 里手绘:
--*   面板开了 setScrollChildren, 滚动时由 UI 系统统一位移子控件;
--*   手绘需要自己叠加 getYScroll(), 一旦语义不符就会出现"盒子与控件错位、
--*   下方行没有盒子"(实测缺陷)。做成子控件后位置由滚动系统保证一致。
--* 加入顺序: 先加盒子再加控件 -> 盒子恒在控件下层。
--*********************************************************
UIRowBox = ISPanel:derive("UIRowBox");

function UIRowBox:render()
    EtherTheme.drawTileBox(self, 0, 0, self.width, self.height, false, self.chamfer);
end

-- 不吞鼠标事件: 让点击穿到复选框/按钮, 也不影响拖窗
function UIRowBox:onMouseDown(x, y) return false; end
function UIRowBox:onMouseUp(x, y) return false; end
function UIRowBox:onMouseMove(dx, dy) return false; end

function UIRowBox:new(x, y, width, height, chamfer)
    local o = ISPanel:new(x, y, width, height);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.chamfer = chamfer or 6;
    return o;
end

--*********************************************************
--* UISectionHeader: 分区标题 (Small 居中标题, 自带切角标题盒 —
--* 标题被背景框包裹, 紧贴模块顶部)。标题宽度构建时实测一次
--* (render 零测量); 做成子控件, 滚动时由 UI 系统统一定位。
--*********************************************************
UISectionHeader = ISPanel:derive("UISectionHeader");

function UISectionHeader:render()
    EtherTheme.drawSectionTitle(self, 0, 0, self.width, self.title, self.titleW, "lines");
end

UISectionHeader.onMouseDown = UIRowBox.onMouseDown;
UISectionHeader.onMouseUp = UIRowBox.onMouseUp;
UISectionHeader.onMouseMove = UIRowBox.onMouseMove;

function UISectionHeader:new(x, y, width, title, align)
    local o = ISPanel:new(x, y, width, EtherTheme.fontHgtSmall + 8);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.title = title;
    o.titleW = getTextManager():MeasureStringX(UIFont.Small, title);
    o.align = align;
    return o;
end

-- 8px 密集网格间距标记 (统一竖向节奏, 保证行距一致)
-- 注: 行距在"功能行盒"(§边界感, 见 _box/drawFeatureBoxes)接入后统一加高,
--     使每行盒之间留出可见缝隙, 否则盒边会首尾相接糊成一片。
EtherFormPanel.PAD_X       = 16;   -- 控件左内边距
EtherFormPanel.TOP_Y       = 10;   -- 首行顶部留白 (须保证 TOP_Y - BOX_PAD_Y >= stencil 顶, 否则首行盒被剪)
EtherFormPanel.STENCIL_TOP = 2;    -- 裁剪区顶部 (原为 10, 会把首行行盒上缘剪掉)
EtherFormPanel.ROW_STEP    = 34;   -- 复选框/标签等单行行距 (18 复选框 + 盒内上下各6 + 缝隙4)
EtherFormPanel.SECTION_GAP = 16;   -- 逻辑分组之间的留白 (whitespace 分组)
EtherFormPanel.SLIDER_STEP = 66;   -- 滑块行距 (44px 高拉条 + 盒边 + 缝隙)
EtherFormPanel.ENTRY_W     = 75;   -- 数值输入框宽度
EtherFormPanel.ENTRY_H     = EtherTheme.entryH;  -- 输入行高 (= UITextBox2 最小渲染高; 按此留位防自动撑高)
EtherFormPanel.BTN_H       = 24;   -- 普通按钮高度 (= EtherTheme.btnH)
EtherFormPanel.BOTTOM_PAD  = 16;   -- 滚动内容底部余量
EtherFormPanel.MIN_COL_W   = 300;  -- 单列最小宽度; 固定 888 窗宽下内容区可用 674 -> 2 列 (§15.2)
EtherFormPanel.MAX_COLS    = 3;    -- 最多列数

-- 功能行盒 (边界感): 与左侧导航磁贴同款切角描边, 由 EtherTheme.drawTileBox 绘制
EtherFormPanel.BOX_PAD_X   = 8;    -- 盒子相对控件左侧外扩
EtherFormPanel.BOX_PAD_Y   = 6;    -- 盒子相对控件上下外扩 (盒高 = 18+12 = 30)
EtherFormPanel.BOX_GAP     = 10;   -- 相邻列盒子之间的横向缝隙 (太窄时两侧切角会拼成 X 形杂乱)
EtherFormPanel.SCROLLBAR_W = 16;   -- 右侧滚动条让位, 防盒子压在滚动条下
EtherFormPanel.BOX_CS      = 6;    -- 行盒切角尺寸: 行高仅 30, 切角过大会让盒子显得尖角/像六边形
-- ISScrollingListBox 的列表头画在列表 y 之上(自身边界之外), 高度 == 列表 itemheight。
-- 单一来源在 EtherTheme.listHeaderH, 这里只做别名, 保证与各表格实际 itemheight 同步:
-- 写死常量会在游戏字体偏大时被表头顶穿, 盖住上一行的模块标题。
EtherFormPanel.LIST_HEADER_H = EtherTheme.listHeaderH;

--*********************************************************
--* 内部: 按面板宽度计算列数与列宽 (§15.2 自适应多列)。
--* cols = clamp(floor(可用宽 / MIN_COL_W), 1, MAX_COLS); 窄面板自动回落单列。
--*********************************************************
function EtherFormPanel:_computeCols()
    local usable = self.width - EtherFormPanel.PAD_X * 2;
    local n = math.floor(usable / EtherFormPanel.MIN_COL_W);
    if n < 1 then n = 1; end
    if n > EtherFormPanel.MAX_COLS then n = EtherFormPanel.MAX_COLS; end
    self.cols = n;
    self.colW = usable / n;
end

--*********************************************************
--* 内部: 整行推进 (全宽项用: 标签/按钮/滑块/输入/分区/留白)。
--* 若当前停在半个网格行 (col>0), 先落到下一行, 保证全宽项独占一行。
--* 修复 P1: 位置由游标自动排布, 面板代码不再出现魔法坐标。
--*********************************************************
function EtherFormPanel:_advance(step)
    if self.col and self.col > 0 then
        self.col = 0;
        self.cursorY = self.cursorY + EtherFormPanel.ROW_STEP;
    end
    local y = self.cursorY;
    self.cursorY = self.cursorY + step;
    self:setScrollHeight(self.cursorY + EtherFormPanel.BOTTOM_PAD);
    return y;
end

--*********************************************************
--* 内部: 网格单元 (短项用: 复选框)。返回该项左上角 (x, y)。
--* 逐列填充, 填满一行后换行; 列宽由 _computeCols 决定。
--*********************************************************
function EtherFormPanel:_grid(step)
    local x = EtherFormPanel.PAD_X + self.col * self.colW;
    local y = self.cursorY;
    self.col = self.col + 1;
    if self.col >= self.cols then
        self.col = 0;
        self.cursorY = self.cursorY + step;
        self:setScrollHeight(self.cursorY + EtherFormPanel.BOTTOM_PAD);
    end
    return x, y;
end

--*********************************************************
--* 内部: 统一锚定 (随窗口缩放; 与原各面板一致: 左对齐 + 底部锚定)。
--* 修复 P7: anchor 样板集中到一处。
--*********************************************************
function EtherFormPanel:_anchor(widget)
    widget:setAnchorLeft(true);
    widget:setAnchorRight(false);
    widget:setAnchorTop(false);
    widget:setAnchorBottom(true);
end

--*********************************************************
--* 内部: 收集控件到实例级列表 (修复 P3: 状态不再挂类表)。
--* opts.onlyInGame 为真时标记, 供 refreshGating 门控。
--*********************************************************
function EtherFormPanel:_track(widget, opts)
    if opts and opts.onlyInGame then
        widget.isOnlyInGame = true;
    end
    if opts and opts.onlyNotInGame then
        widget.isOnlyNotInGame = true;
    end
    table.insert(self.widgets, widget);
    return widget;
end

--*********************************************************
--* 内部: 为一行加"切角行盒" (立即作为子控件插入, 必须在该行的控件之前调用,
--* 以保证盒子在控件下层; 位置随滚动由 UI 系统统一处理)。
--*   contentX/contentY  控件左上角; contentW/contentH 控件占位尺寸
--*********************************************************
function EtherFormPanel:_box(contentX, contentY, contentW, contentH)
    local box = UIRowBox:new(
        contentX - EtherFormPanel.BOX_PAD_X,
        contentY - EtherFormPanel.BOX_PAD_Y,
        contentW + EtherFormPanel.BOX_PAD_X * 2,
        contentH + EtherFormPanel.BOX_PAD_Y * 2,
        EtherFormPanel.BOX_CS);
    box:initialise();
    box:instantiate();
    self:_anchor(box);
    self:addChild(box);
    return box;
end

--*********************************************************
--* 内部: 整行盒的内容宽度 (右侧给滚动条让位)。
--*********************************************************
function EtherFormPanel:_rowContentW()
    local w = self.width - EtherFormPanel.PAD_X - EtherFormPanel.SCROLLBAR_W
        - EtherFormPanel.BOX_PAD_X;
    if w < 40 then w = 40; end
    return w;
end

--*********************************************************
--* 内部: 单列格盒的内容宽度 (多列时按列宽扣掉列间缝隙)。
--*********************************************************
function EtherFormPanel:_cellContentW()
    if self.cols <= 1 then return self:_rowContentW(); end
    local w = self.colW - EtherFormPanel.BOX_GAP - EtherFormPanel.BOX_PAD_X * 2;
    if w < 40 then w = 40; end
    return w;
end

--*********************************************************
--* 复选框
--*   key      翻译键 (基类内部调 tr, 面板不再出现 getTranslate)
--*   onToggle function(isChecked) — 直接引用既有全局 toggleX (冻结契约)
--*   getState function()->bool 或 布尔值 — 初始勾选态, 构建时求值
--*   opts     可选 { onlyInGame = true }
--*********************************************************
function EtherFormPanel:addCheckbox(key, onToggle, getState, opts)
    local title = tr(key);
    local tm = getTextManager();
    -- 长标签(如俄语)按整行可用宽折行, 杜绝溢出面板 (实机缺陷)
    local rowW = self:_rowContentW();
    local availW = rowW - (18 + 10 + 20);
    local lines = { title };
    if tm:MeasureStringX(UIFont.Small, title) > availW then
        lines = EtherTheme.wrapText(title, availW, UIFont.Small);
    end
    local needW = 18 + 10 + tm:MeasureStringX(UIFont.Small, lines[1]) + 20;
    -- 行距两难 (俄语长标签折行 × 大字体档, 两轮实机反馈):
    --   一律 ROW_STEP: 大字体(2x/3x, 行高 26+)下多行行距不足 -> 行盒互相重叠;
    --   一律按字号推进: 单行也被撑到"字号+内边距" -> 全页间距巨大。
    -- 分支取值: 单行保持紧凑 ROW_STEP; 多行按实际盒高(行数×行高+内边距)+4px 缝隙。
    local lineH = EtherTheme.fontHgtSmall + 2;
    local step = EtherFormPanel.ROW_STEP;
    if #lines > 1 then
        step = #lines * lineH + EtherFormPanel.BOX_PAD_Y * 2 + 4;
    end

    local x, y, boxW;
    if self.cols > 1 and needW <= self.colW and #lines == 1 then
        x, y = self:_grid(step);             -- 短标签进多列
        boxW = self:_cellContentW();
    else
        y = self:_advance(step);             -- 长标签独占整行(或折行), 防串列杂乱
        x = EtherFormPanel.PAD_X;
        boxW = rowW;
    end

    local checked = false;
    if type(getState) == "function" then
        checked = getState() and true or false;
    elseif getState then
        checked = true;
    end

    local cb = UICheckbox:new(x, y, title, checked, onToggle);
    if #lines > 1 then
        cb.titleLines = lines;
        cb.height = math.max(18, #lines * (EtherTheme.fontHgtSmall + 2));
        cb.width = boxW;                     -- 命中区域=整行盒
    end
    -- 行盒必须先入队(在控件之前), 才能画在控件下层
    self:_box(x, y, boxW, cb.height);
    cb:initialise();
    cb:instantiate();
    self:_anchor(cb);
    self:addChild(cb);
    return self:_track(cb, opts);
end

--*********************************************************
--* 文本标签 (静态)
--*   key   翻译键; opts.raw = true 时按原文显示不翻译
--*********************************************************
function EtherFormPanel:addLabel(key, opts)
    local y = self:_advance((opts and opts.step) or EtherFormPanel.ROW_STEP);
    local text = (opts and opts.raw) and key or tr(key);
    local font = (opts and opts.font) or UIFont.Small;
    -- 行高取字体行高 + 上下留白, 交给 makeLabel 在其中居中 (不再手工加 labelPadY)
    local rowH = getTextManager():getFontHeight(font) + EtherTheme.labelPadY * 2;
    local label = EtherTheme.makeLabel(EtherFormPanel.PAD_X, y, rowH, text, nil, font);
    self:addChild(label);
    return self:_track(label, opts);
end

--*********************************************************
--* 分区标题 (参考图/mockup 样式): 留白 + 薄荷标题 + 右侧分割线, 建立信息层级。
--* 注意: key 必须是已存在的翻译键, 否则会显示原始 key。
--*********************************************************
--*********************************************************
--* 带标题的功能模块: 一个切角盒 = 顶部"---- 标题 ----"(居中+两侧线)
--* + 盒内内容。标题被模块背景框包裹 (实机定稿的模块形态)。
--*   key       标题翻译键
--*   contentH  内容高度 (不含标题行)
--*   builder   function(bx, by, bw) 在标题行之下摆放内容
--*********************************************************
function EtherFormPanel:addModule(key, contentH, builder)
    local titleH = EtherTheme.fontHgtSmall + 12;
    local y = self:_advance(titleH + contentH + EtherFormPanel.BOX_PAD_Y * 2 + 6);
    local w = self:_rowContentW();
    -- 盒先入队 (画在下层), 标题行与内容都在盒内
    self:_box(EtherFormPanel.PAD_X, y, w, titleH + contentH);
    local hdr = UISectionHeader:new(EtherFormPanel.PAD_X, y + 2, w, tr(key));
    hdr:initialise();
    hdr:instantiate();
    self:_anchor(hdr);
    self:addChild(hdr);
    if builder then
        builder(EtherFormPanel.PAD_X, y + titleH, w);
    end
    return y;
end

function EtherFormPanel:beginSection(key)
    -- 标题盒尽量上贴: 分区间距由行盒自身缝隙提供, 这里只留极小呼吸
    self:addSpacer(6);
    local y = self:_advance(EtherTheme.fontHgtSmall + 12);
    local hdr = UISectionHeader:new(EtherFormPanel.PAD_X, y, self:_rowContentW(), tr(key));
    hdr:initialise();
    hdr:instantiate();
    self:_anchor(hdr);
    self:addChild(hdr);
    return self:_track(hdr, nil);
end

--*********************************************************
--* 纯留白 (逻辑分组间隔)
--*********************************************************
function EtherFormPanel:addSpacer(px)
    self:_advance(px or EtherFormPanel.SECTION_GAP);
end

--*********************************************************
--* 普通按钮
--*   key      翻译键
--*   onClick  function()
--*   opts     可选 { width = n, onlyInGame = true }
--*********************************************************
function EtherFormPanel:addButton(key, onClick, opts)
    local y = self:_advance(EtherFormPanel.BTN_H + 8);
    local w = (opts and opts.width) or 120;
    -- 传 maxWidth 锁死上限: UIButton 会按文字撑宽, 长翻译(如 RU)否则会越出行盒。
    local maxW = self:_rowContentW();
    if w > maxW then w = maxW; end
    local btn = UIButton:new(EtherFormPanel.PAD_X, y, w, EtherFormPanel.BTN_H, tr(key), onClick, maxW);
    btn:initialise();
    btn:instantiate();
    self:addChild(btn);
    return self:_track(btn, opts);
end

--*********************************************************
--* 数值文本输入行 (标签 + 输入框, 可选 应用/重置 按钮)
--*   key      翻译键
--*   getVal   function()->number 或 值 — 初始值
--*   applyFn  function(number) — 直接引用既有全局 setX (冻结契约)
--*   opts     可选 { min, max, default, gap, onlyInGame }
--*            提供 default 时才生成 应用/重置 按钮 (与原 CharacterPanel 一致)
--*********************************************************
function EtherFormPanel:addTextEntry(key, getVal, applyFn, opts)
    opts = opts or {};
    local title = tr(key);
    local tm = getTextManager();
    local rowW = self:_rowContentW();
    local gap = EtherTheme.ctrlGap;
    local rowH = EtherTheme.entryH;    -- 行高 = 输入框实际渲染高; 按钮在行内竖直居中
    local btnH = EtherTheme.ctrlH;

    -- 控件块宽度: 输入框 (+ 应用/重置按钮, 二者统一取较宽文字)
    local applyT, resetT, btnW = nil, nil, 0;
    if opts.default ~= nil then
        applyT = tr("UI_CharacterPanel_ApplyButton");
        resetT = tr("UI_CharacterPanel_ResetButton");
        btnW = UIButton.measureGroupWidth({ applyT, resetT });
    end
    local ctrlW = EtherFormPanel.ENTRY_W + (btnW > 0 and (gap + btnW) * 2 or 0);

    -- 标签与控件块能否同排: 放不下就标签单独一行、控件另起一行右对齐,
    -- 避免长标签(如俄语"攻速(1.0-3.0; >2.0 风险)")把按钮挤出面板右缘。
    local labelW = tm:MeasureStringX(UIFont.Small, title);
    local twoLine = (labelW + gap * 2 + ctrlW) > rowW;
    local contentH = twoLine and (EtherTheme.fontHgtSmall + 4 + rowH) or rowH;
    local y = self:_advance(contentH + EtherFormPanel.BOX_PAD_Y * 2 + 6 + (opts.gap or 0));

    -- 行盒先入队 (在控件之前 -> 画在下层)
    self:_box(EtherFormPanel.PAD_X, y, rowW, contentH);

    -- 单行时标签行高取控件高度, 由 makeLabel 在其中居中 -> 与输入框/按钮完全对齐;
    -- 双行时标签独占一行, 行高取字体行高。
    local labelRowH = twoLine and EtherTheme.fontHgtSmall or rowH;
    local label = EtherTheme.makeLabel(EtherFormPanel.PAD_X, y, labelRowH, title);
    self:addChild(label);
    self:_track(label, opts);

    -- 控件块统一右对齐到行盒右缘"内侧"(内缩 BOX_PAD_X, 不压切角边)
    local ctrlY = twoLine and (y + EtherTheme.fontHgtSmall + 4) or y;
    local cx = EtherFormPanel.PAD_X + rowW - ctrlW - EtherFormPanel.BOX_PAD_X;
    if cx < EtherFormPanel.PAD_X then cx = EtherFormPanel.PAD_X; end

    local initial = (type(getVal) == "function") and getVal() or getVal;
    local entry = ISTextEntryBox:new(tostring(initial), cx, ctrlY,
        EtherFormPanel.ENTRY_W, rowH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();

    local function applyEntry()
        local num = tonumber(entry:getText());
        if num ~= nil then
            if opts.min ~= nil and num < opts.min then num = opts.min; end
            if opts.max ~= nil and num > opts.max then num = opts.max; end
            applyFn(num);
        end
    end
    entry.onTextChange = applyEntry;
    self:addChild(entry);
    self:_track(entry, opts);

    if opts.default ~= nil then
        local bx = cx + EtherFormPanel.ENTRY_W + gap;
        -- 传 maxWidth 锁死等宽, 防止 UIButton 按文字撑宽后两个按钮不等宽并越出行盒
        local applyBtn = UIButton:new(bx, ctrlY + EtherTheme.entryBtnDY, btnW, btnH, applyT, applyEntry, btnW);
        applyBtn:initialise();
        applyBtn:instantiate();
        self:addChild(applyBtn);
        self:_track(applyBtn, opts);

        local resetBtn = UIButton:new(bx + btnW + gap, ctrlY + EtherTheme.entryBtnDY, btnW, btnH, resetT,
            function()
                entry:setText(tostring(opts.default));
                applyEntry();
            end, btnW);
        resetBtn:initialise();
        resetBtn:instantiate();
        self:addChild(resetBtn);
        self:_track(resetBtn, opts);
    end

    return entry;
end

--*********************************************************
--* 按一批开关项的"最宽标签"计算并锁定列数, 供整页多组共用。
--* 用途: 一页里有多个开关分组时, 若各组各自算列数, 组与组之间宽度仍会不一致;
--* 先用全页所有项 planColumns 一次, 后续各组沿用同一列宽 -> 全页等宽。
--*   items 可以是扁平列表, 也可以是"列表的列表"
--*********************************************************
function EtherFormPanel:planColumns(items)
    local tm = getTextManager();
    local needW = 0;

    local function scan(list)
        for i = 1, #list do
            local it = list[i];
            if it.key ~= nil then
                local w = 18 + 10 + tm:MeasureStringX(UIFont.Small, tr(it.key)) + 20;
                if w > needW then needW = w; end
            elseif type(it) == "table" then
                scan(it);           -- 嵌套分组
            end
        end
    end
    scan(items);

    local usable = self.width - EtherFormPanel.PAD_X * 2;
    local minW = EtherFormPanel.MIN_COL_W;
    if needW > minW then minW = needW; end
    local cols = math.floor(usable / minW);
    if cols < 1 then cols = 1; end
    if cols > EtherFormPanel.MAX_COLS then cols = EtherFormPanel.MAX_COLS; end

    self.cols = cols;
    self.colW = usable / cols;
    self.colsLocked = true;
    return cols;
end

--*********************************************************
--* 复选框组 (推荐用法): 一次传入整组开关数据, 基类按"组内最宽标签"统一定列数,
--* 使该组内所有行盒宽度完全一致。
--*
--* 为什么需要它: 逐个 addCheckbox 时, 短标签进多列、长标签退整行, 结果同一页里
--* 混着半宽盒与整宽盒, 观感参差 (用户反馈的"功能框长短不一")。这里改为:
--* 先量出最宽项, 若两/三列容不下最宽项就整组回落到更少列 -> 全组等宽。
--* 若已调用 planColumns 锁定列数, 则沿用锁定值 (全页等宽)。
--*
--*   items = { { key=, on=, get=, onlyInGame= }, ... }
--*********************************************************
function EtherFormPanel:addCheckboxGroup(items)
    if items == nil or #items == 0 then return; end

    if not self.colsLocked then
        self:planColumns(items);
    end

    -- 换组前先结束上一组的半行, 保证组与组之间不串行
    if self.col > 0 then
        self.col = 0;
        self.cursorY = self.cursorY + EtherFormPanel.ROW_STEP;
    end

    local boxW = self:_cellContentW();
    local lineH = EtherTheme.fontHgtSmall + 2;
    -- 预折行: 长标签(俄语等)在格宽内折行, 行高按行内最大行数增长
    local wrapped = {};
    for i = 1, #items do
        local t = tr(items[i].key);
        local lines = { t };
        local availW = boxW - (18 + 10 + 20) - 4;
        if getTextManager():MeasureStringX(UIFont.Small, t) > availW then
            lines = EtherTheme.wrapText(t, availW, UIFont.Small);
        end
        wrapped[i] = lines;
    end

    local col = self.col;
    local rowStep = 0;
    for i = 1, #items do
        local it = items[i];
        local lines = wrapped[i];
        local x = EtherFormPanel.PAD_X + col * self.colW;
        local y = self.cursorY;

        local checked = false;
        if type(it.get) == "function" then
            checked = it.get() and true or false;
        elseif it.get then
            checked = true;
        end

        local cb = UICheckbox:new(x, y, tr(it.key), checked, it.on);
        if #lines > 1 then
            cb.titleLines = lines;
            cb.height = math.max(18, #lines * lineH);
            cb.width = boxW;
        end
        self:_box(x, y, boxW, cb.height);      -- 行盒先入队 -> 画在控件下层
        cb:initialise();
        cb:instantiate();
        self:_anchor(cb);
        self:addChild(cb);
        self:_track(cb, it);
        it.widget = cb;                         -- 回填, 便于面板侧引用 (如地图 flag 同步)

        -- 行距同 addCheckbox 的分支取值: 单行紧凑 ROW_STEP, 多行按实际盒高+4px
        local itemStep = EtherFormPanel.ROW_STEP;
        if #lines > 1 then
            itemStep = #lines * lineH + EtherFormPanel.BOX_PAD_Y * 2 + 4;
        end
        rowStep = math.max(rowStep, itemStep);
        col = col + 1;
        if col >= self.cols then
            col = 0;
            self.cursorY = self.cursorY + rowStep;
            self:setScrollHeight(self.cursorY + EtherFormPanel.BOTTOM_PAD);
            rowStep = 0;
        end
    end

    -- 组结束: 收尾半行, 使后续整行项不与本组串行。
    -- col 必须清零: 半行已在上面按实际行距收尾, 若留给 _advance 它会再按固定
    -- ROW_STEP 二次推进 —— 多行半行时凭空多出一大段空隙 (大字体下尤为明显)。
    local leftover = col;
    self.col = 0;
    if leftover > 0 then
        self.cursorY = self.cursorY + rowStep;
        self:setScrollHeight(self.cursorY + EtherFormPanel.BOTTOM_PAD);
    end
end

--*********************************************************
--* 通用控件挂载 (供子类放置基类未内建的控件)
--*********************************************************
function EtherFormPanel:addWidget(widget, opts)
    if widget.initialise then widget:initialise(); end
    if widget.instantiate then widget:instantiate(); end
    self:_anchor(widget);
    self:addChild(widget);
    return self:_track(widget, opts);
end

--*********************************************************
--* 自定义行: 登记一个高度 h 的切角行盒, 并回调 builder(x, y, w) 由调用方
--* 在盒内自行摆放控件 (用于列表 / 多控件组合等复合行)。
--*********************************************************
function EtherFormPanel:addCustomRow(h, builder)
    local y = self:_advance(h + EtherFormPanel.BOX_PAD_Y * 2 + 6);
    local w = self:_rowContentW();
    self:_box(EtherFormPanel.PAD_X, y, w, h);          -- 先入队 -> 画在控件下层
    if builder then builder(EtherFormPanel.PAD_X, y, w); end
    return y;
end

--*********************************************************
--* 标签 + 右对齐按钮 行 (设置页的 语言切换 / 重载 Lua 等)
--*   key       左侧标签翻译键
--*   btnTitle  按钮文字 (已翻译或原文, 如语言代码 "RU")
--*   opts      { onlyInGame / onlyNotInGame / width }
--*********************************************************
function EtherFormPanel:addLabeledButton(key, btnTitle, onClick, opts)
    opts = opts or {};
    local tm = getTextManager();
    local ctrlH = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    local rowW = self:_rowContentW();
    local y = self:_advance(ctrlH + EtherFormPanel.BOX_PAD_Y * 2 + 6);
    self:_box(EtherFormPanel.PAD_X, y, rowW, ctrlH);

    local label = EtherTheme.makeLabel(EtherFormPanel.PAD_X, y, ctrlH, tr(key));
    self:addChild(label);
    self:_track(label, opts);

    local bw = opts.width or UIButton.measureWidth(btnTitle);
    local maxBw = rowW - EtherFormPanel.BOX_PAD_X;
    if bw > maxBw then bw = maxBw; end
    -- 必须传 maxWidth=bw, 否则 UIButton:new 会按文字把 bw 又撑回去, 上面这行 clamp 白写
    local btn = UIButton:new(EtherFormPanel.PAD_X + rowW - bw - EtherFormPanel.BOX_PAD_X, y, bw, ctrlH, btnTitle, onClick, bw);
    return self:addWidget(btn, opts);
end

--*********************************************************
--* 标签 + 右对齐取色块 行 (设置页四个颜色项共用, 消灭 4 份重复块)
--*   getColor  function() -> PZ Color (需支持 :getR/:getG/:getB)
--*   setColor  function(r, g, b) — 直接引用既有全局 setXUIColor (冻结契约)
--* 注: 取色后先应用再刷新色块, 修正原实现"色块显示上一次颜色"的滞后。
--*********************************************************
function EtherFormPanel:addColorPicker(key, getColor, setColor, opts)
    opts = opts or {};
    local sw = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    local rowW = self:_rowContentW();
    local y = self:_advance(sw + EtherFormPanel.BOX_PAD_Y * 2 + 6);
    self:_box(EtherFormPanel.PAD_X, y, rowW, sw);

    local label = EtherTheme.makeLabel(EtherFormPanel.PAD_X, y, sw, tr(key));
    self:addChild(label);
    self:_track(label, opts);

    local btn;
    local function refresh()
        local c = getColor();
        btn.backgroundColor = { r = c:getR(), g = c:getG(), b = c:getB(), a = 1 };
        btn.backgroundColorMouseOver = btn.backgroundColor;
    end

    btn = ISButton:new(EtherFormPanel.PAD_X + rowW - sw - EtherFormPanel.BOX_PAD_X, y, sw, sw, "", self, function()
        local picker = ISColorPicker:new(getMouseX(), getMouseY());
        picker:initialise();
        picker.pickedTarget = self;
        picker.resetFocusTo = self;
        picker:setInitialColor(getColor());
        picker.pickedFunc = function(target, color, mouseUp)
            setColor(color.r, color.g, color.b);
            refresh();
        end;
        picker:addToUIManager();
    end);
    btn:initialise();
    btn:instantiate();
    refresh();
    btn.borderColor = { r = EtherTheme.blood.r, g = EtherTheme.blood.g, b = EtherTheme.blood.b, a = 0.8 };
    self:_anchor(btn);
    self:addChild(btn);
    return self:_track(btn, opts);
end

--*********************************************************
--* 滑块行 (参考图布局: 拉条在左 + 数值紧随 + 标签靠右)
--*   key      翻译键
--*   getVal   function()->number 或 值 — 初始值
--*   setVal   function(number) — 直接引用既有全局 setX (冻结契约)
--*   min/max  数值范围
--*   opts     可选 { width = n, onlyInGame = true } (width 覆盖拉条宽)
--* 数值由 UISlider 自绘在拉条右侧 (天蓝色), 这里只预留槽位 (~84px)。
--*********************************************************
function EtherFormPanel:addSlider(key, getVal, setVal, minValue, maxValue, opts)
    opts = opts or {};
    local tm = getTextManager();
    local rowW = self:_rowContentW();
    local label = tr(key);
    local labelW = tm:MeasureStringX(UIFont.Small, label);

    -- 拉条宽: 默认 55% 行宽; 布局 = 标签左 | 数值(框外左侧) | 拉条右
    local sliderW = opts.width or math.floor(rowW * 0.55);
    local maxSlider = rowW - labelW - 56;
    if sliderW > maxSlider then sliderW = maxSlider; end
    if sliderW < 80 then sliderW = 80; end

    local y = self:_advance(EtherFormPanel.SLIDER_STEP);
    -- 行盒先入队 (在控件之前 -> 画在下层); 内容高 46 = 拉条(44)居中
    self:_box(EtherFormPanel.PAD_X, y, rowW, 46);

    local val = (type(getVal) == "function") and getVal() or (getVal or minValue);
    -- 拉条在右 (行盒右缘内侧); 数值由 UISlider 自绘在拉条框外左侧
    local sx = EtherFormPanel.PAD_X + rowW - sliderW - EtherFormPanel.BOX_PAD_X;
    local slider = UISlider:new(sx, y + 1, sliderW, 44, val, minValue, maxValue, setVal);
    slider:initialise();
    slider:instantiate();
    self:_anchor(slider);
    self:addChild(slider);
    self:_track(slider, opts);

    -- 标签在左 (预留数值槽宽)
    local lx = EtherFormPanel.PAD_X;
    local labelWgt = EtherTheme.makeLabel(lx, y, 46, label);
    self:addChild(labelWgt);
    return self:_track(labelWgt, opts);
end

--*********************************************************
--* 脚手架: 子类不重写 createChildren, 改实现 :build()。
--* 本方法负责滚动/裁剪脚手架, 重置实例级游标与控件表, 再回调 build。
--*********************************************************
function EtherFormPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true);
    self:setScrollHeight(0);
    self:addScrollBars();

    self:_computeCols();                    -- 按面板宽度定列数/列宽 (§15.2)
    self.cursorY = EtherFormPanel.TOP_Y;   -- 实例级 (修复 P3)
    self.col = 0;                           -- 当前网格列 (实例级)
    self.widgets = {};                      -- 实例级 (修复 P3)

    if self.build then
        self:build();
    end

    -- 收尾: 若停在半个网格行, 补一行高度确保滚动范围正确
    if self.col > 0 then
        self.col = 0;
        self.cursorY = self.cursorY + EtherFormPanel.ROW_STEP;
        self:setScrollHeight(self.cursorY + EtherFormPanel.BOTTOM_PAD);
    end

    self:refreshGating();
end

--*********************************************************
--* 门控: 不在游戏内时禁用 onlyInGame 控件; 在游戏内时禁用 onlyNotInGame 控件。
--* 等价于原 CharacterPanel:updatePanel / SettingsPanel:updatePanel,
--* 但作用于实例级 widgets (修复 P3)。
--*********************************************************
function EtherFormPanel:refreshGating()
    local inGame = getPlayer() ~= nil;
    for i = 1, #self.widgets do
        local w = self.widgets[i];
        if w.setEnable then
            if w.isOnlyInGame and not inGame then
                w:setEnable(false);
            elseif w.isOnlyNotInGame and inGame then
                w:setEnable(false);
            end
        end
    end
end

--*********************************************************
--* 滚动裁剪 (与原各表单面板一致)。
--* 行盒已改为子控件 (UIRowBox), 由滚动系统统一定位, 这里不再手绘。
--*********************************************************
function EtherFormPanel:prerender()
    self:setStencilRect(0, EtherFormPanel.STENCIL_TOP, self:getWidth(),
        self:getHeight() - EtherFormPanel.STENCIL_TOP * 2);
    ISPanel.prerender(self);
end

function EtherFormPanel:render()
    ISPanel.render(self);
    -- 子类手绘内容钩子: 在 clearStencilRect 之前执行, 画到面板外的像素
    -- 会被裁剪区剪掉 (修玩家页滚动时信息文字越出面板的缺陷)。
    -- 手绘需自带滚动偏移 —— EtherTheme.drawHintText 系列已叠加 getYScroll。
    if self.renderContent then self:renderContent(); end
    self:clearStencilRect();
end

function EtherFormPanel:onMouseWheel(del)
    self:setYScroll(self:getYScroll() - (del * 40));
    return true;
end

--*********************************************************
--* 统一构造 (透明底 + 可拖动; 供各子类继承, 通常无需再写 :new)。
--* 以 EtherXxxPanel:new(...) 调用时 self = 子类, 元表链解析正确。
--*********************************************************
function EtherFormPanel:new(posX, posY, width, height)
    local o = ISPanel:new(posX, posY, width, height);
    setmetatable(o, self);
    self.__index = self;

    o.background = true;
    o.backgroundColor = { r = 0.0, g = 0.0, b = 0.0, a = 0.0 };
    o.borderColor = { r = 0.0, g = 0.0, b = 0.0, a = 0.0 };
    o.moveWithMouse = true;
    o.cursorY = EtherFormPanel.TOP_Y;
    o.col = 0;
    o.cols = 1;
    o.colW = width - EtherFormPanel.PAD_X * 2;
    o.widgets = {};

    return o;
end
