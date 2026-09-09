--*********************************************************
--* 按键绑定子面板 (独立窗口, 自设置页呼出)
--* 为什么独立: 绑定功能会随功能域增长, 内嵌设置页会无限拉长 (用户决策)
--* 实现注记: 刻意不继承 EtherFormPanel (它为主面板内嵌页设计, 行盒绘制/
--*   滚动/门控与独立窗组合有隐性约定, 实测踩坑) —— 直接 ISPanel:derive
--*   手动摆控件; 滚动用 ISPanel 原生 setScrollChildren/addScrollBars;
--*   窗口语言(深底/页眉/外框/关闭钮)复用 EtherTheme, 与主窗同族但小尺寸
--*********************************************************
require "ISUI/ISPanel"

EtherKeyBindsPanel = ISPanel:derive("EtherKeyBindsPanel");
local Panel = EtherKeyBindsPanel;

local BASE_W, H = 520, 480;

local function mediumButtonWidth(t)
    return getTextManager():MeasureStringX(UIFont.Medium, t) + EtherTheme.ctrlPadX * 2;
end

-- 按当前语言 + 当前绑定实时算宽: 俄语鼠标键名会撑爆固定宽度 (实机反馈)
function Panel.calcWidth()
    local w = BASE_W;
    if type(EtherKeyBinds) ~= "table" or type(EtherKeyBinds.order) ~= "table" then
        return w;
    end
    local changeT = getTranslate("UI_KeyBind_Change");
    local resetW = mediumButtonWidth(getTranslate("UI_KeyBind_Default"));
    for _, bid in ipairs(EtherKeyBinds.order) do
        local r = EtherKeyBinds.registry[bid];
        if r ~= nil then
            local labelW = getTextManager():MeasureStringX(UIFont.Small, getTranslate(r.label)) + 8;
            local initTitle = EtherKeyBinds.keyName(EtherKeyBinds.resolve(bid)) .. "  " .. changeT;
            local changeW = math.max(mediumButtonWidth(initTitle), 130);
            local need = EtherTheme.framePad * 2 + EtherTheme.ctrlPadX * 2
                + labelW + changeW + resetW + EtherTheme.ctrlGap;
            if need > w then w = need; end
        end
    end
    return w + 16;
end

--*********************************************************
--* 打开/关闭 (单实例; 设置页按钮调用)
--*********************************************************
function Panel.open()
    if Panel.instance ~= nil then
        Panel.instance:addToUIManager();
        Panel.instance:setVisible(true);
        Panel.instance:setAlwaysOnTop(true);   -- 盖在主菜单上 (主次结构)
        return;
    end
    local screenW = getCore():getScreenWidth();
    local screenH = getCore():getScreenHeight();
    local w = math.min(Panel.calcWidth(), screenW - 40);
    local h = math.min(H, screenH - 40);
    Panel.instance = Panel:new(math.floor(screenW / 2 - w / 2), math.floor(screenH / 2 - h / 2), w, h);
    -- 深底由 ISPanel:prerender 画 (backgroundColor), 边框关掉 (render 画切角青框)
    Panel.instance.backgroundColor = { r = 0.03, g = 0.05, b = 0.06, a = 1 };
    Panel.instance.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    Panel.instance:initialise();
    Panel.instance:instantiate();
    Panel.instance:addToUIManager();
    Panel.instance:setVisible(true);
    Panel.instance:setAlwaysOnTop(true);
end

function Panel.close()
    if Panel.instance ~= nil then
        Panel.instance:removeFromUIManager();
        Panel.instance:setVisible(false);
    end
end

-- 销毁缓存实例 (主菜单整建/语言切换后, 文案按旧语言烘焙, 需重建)
function Panel.rebuild()
    Panel.close();
    Panel.instance = nil;
end

--*********************************************************
--* 构建: 手动布局 (页眉下起点, 行高累计), 滚动交给 ISPanel 原生机制
--*********************************************************
function Panel:createChildren()
    ISPanel.createChildren(self);

    -- 滚动三件套 (ISPanel 原生): 子控件随滚动条整体位移
    self:setScrollChildren(true);
    self:setScrollHeight(0);
    self:addScrollBars();

    local ctrlH = EtherTheme.ctrlH;
    local gap = EtherTheme.ctrlGap;
    local padX = EtherTheme.framePad;
    -- 内容内缩: 行盒与文字/按钮之间留气口 (实机反馈: 功能名贴左缘)
    local inner = EtherTheme.ctrlPadX;
    local top = EtherTheme.headerH + 12;   -- 页眉下起点
    local cx = padX + inner;               -- 内容左缘
    local cw = self.width - (padX + inner) * 2;   -- 内容可用宽
    local y = top;

    -- Medium 字体量宽 (本面板按钮统一 Medium)
    local function mw(t)
        return getTextManager():MeasureStringX(UIFont.Medium, t) + EtherTheme.ctrlPadX * 2;
    end

    self.rowRects = {};   -- 行盒位置 (prerender 里画切角盒)

    -- 返回行 (标题在页眉, 不占内容区)
    local backT = getTranslate("UI_KeyBind_Back");
    local backW = mw(backT);
    local backBtn = UIButton:new(cx + cw - backW, y, backW, ctrlH, backT, function()
        Panel.close();
    end, backW);
    backBtn.font = UIFont.Medium;
    self:addChild(backBtn);
    table.insert(self.rowRects, { x = padX, y = y, w = self.width - padX * 2, h = ctrlH });
    y = y + ctrlH + gap;

    -- 绑定行 (label + 改绑 + 默认), 顺序 = 注册顺序
    if type(EtherKeyBinds) == "table" and type(EtherKeyBinds.order) == "table" then
        for _, bid in ipairs(EtherKeyBinds.order) do
            local r = EtherKeyBinds.registry[bid];
            if r ~= nil then
                local label = getTranslate(r.label);
                local changeT = getTranslate("UI_KeyBind_Change");
                local resetT = getTranslate("UI_KeyBind_Default");
                local labelW = getTextManager():MeasureStringX(UIFont.Small, label) + 8;
                local resetW = mw(resetT);
                local initTitle = EtherKeyBinds.keyName(EtherKeyBinds.resolve(bid)) .. "  " .. changeT;
                local changeW = math.max(mw(initTitle), 130);
                local btnX = cx + cw - changeW - resetW - gap;
                if btnX < cx + labelW then btnX = cx + labelW; end
                self:addChild(EtherTheme.makeLabel(cx, y + EtherTheme.entryLabelDY,
                    EtherTheme.fontHgtHint, label, EtherTheme.text, UIFont.Small));
                -- Lua 5.1: 闭包看不到"声明中"的 local, 先声明后赋值
                local changeBtn;
                changeBtn = UIButton:new(btnX, y, changeW, ctrlH,
                    initTitle, function()
                        EtherKeyBinds.beginCapture(bid);
                        EtherKeyBinds.onCaptureDone = function(cid, key)
                            if cid == bid and changeBtn ~= nil then
                                if key == nil then
                                    changeBtn.title = EtherKeyBinds.keyName(EtherKeyBinds.resolve(bid)) .. "  " .. changeT;
                                else
                                    changeBtn.title = EtherKeyBinds.keyName(key) .. "  " .. changeT;
                                end
                            end
                        end
                        changeBtn.title = getTranslate("UI_KeyBind_CaptureShort");
                    end, changeW);
                changeBtn.font = UIFont.Medium;
                self:addChild(changeBtn);
                local resetBtn = UIButton:new(btnX + changeW + gap, y, resetW, ctrlH, resetT, function()
                    EtherKeyBinds.reset(bid);
                    changeBtn.title = EtherKeyBinds.keyName(EtherKeyBinds.resolve(bid)) .. "  " .. changeT;
                end, resetW);
                resetBtn.font = UIFont.Medium;
                self:addChild(resetBtn);
                table.insert(self.rowRects, { x = padX, y = y, w = self.width - padX * 2, h = ctrlH });
                y = y + ctrlH + gap;
            end
        end
    end

    self:setScrollHeight(y + EtherFormPanel.BOTTOM_PAD);
end

--*********************************************************
--* 绘制: 页眉 + 扫描线 + 外框 + 标题 + 行盒
--* 关键 (UIElement.java:1279-1292 绘制顺序 = prerender -> children -> render):
--*   窗口底/页眉/行盒等装饰必须在 prerender 画 —— render 里画会盖住子控件
--*   (实测: render 画不透明全窗底 -> 面板内容整体"空白")。深底本身由
--*   ISPanel.prerender 按 backgroundColor 绘制 (open 里设)。
--*********************************************************
function Panel:prerender()
    ISPanel.prerender(self);   -- 深底 (backgroundColor, 在子控件之下)
    local hh = EtherTheme.headerH;
    local hb = EtherTheme.railBG;
    local c  = EtherTheme.blood;

    -- 页眉底 + 扫描线 + 霓虹下缘线
    self:drawRect(0, 0, self.width, hh, 0.96, hb.r, hb.g, hb.b);
    EtherTheme.drawScanlines(self);
    self:drawRect(0, hh - 1, self.width, 1, 1, c.r, c.g, c.b);
    -- 切角霓虹外框
    EtherTheme.drawFrame(self, 0, 0, self.width, self.height);

    -- 行盒 (无辉光细线切角, 与功能行盒同款; 叠加滚动偏移跟随子控件)
    if self.rowRects ~= nil then
        local sy = self:getYScroll();
        for _, rr in ipairs(self.rowRects) do
            EtherTheme.drawTileBox(self, rr.x, rr.y - sy, rr.w, rr.h, false, 8);
        end
    end

    -- 页眉标题居中 (语言切换会 rebuild, 宽度缓存按语言失效)
    local title = getTranslate("UI_KeyBind_Title");
    if Panel.titleW == nil or Panel.titleLang ~= getLanguage() then
        Panel.titleW = getTextManager():MeasureStringX(EtherTheme.fontTitle, title);
        Panel.titleLang = getLanguage();
    end
    self:drawText(title, math.floor((self.width - Panel.titleW) / 2),
        math.floor((hh - EtherTheme.fontHgtTitle) / 2), c.r, c.g, c.b, 1, EtherTheme.fontTitle);
end
