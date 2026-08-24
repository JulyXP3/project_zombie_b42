require "ISUI/ISPanel"

--*********************************************************
--* 战利品重掷 + 刷弹药 面板 (clearContainerExplore POC, multiplayer)
--* 服务端 object.clearContainerExplore 无权限/距离校验:
--* 任意客户端可清空容器「已探索」标记 + 房间程序化生成记录,
--* 之后打开容器由服务端按容器类型表重新 roll 战利品
--* (枪柜/军用包等表含武器弹药)。
--* 复用 EtherContainerPOC (UIItemTables.lua 定义, F10 同入口)。
--*********************************************************
EtherLootRollPanel = ISPanel:derive("EtherLootRollPanel");

--*********************************************************
--* 布局登记 (createChildren 期间算好, prerender 统一绘制)
--*   _group 分组盒矩形 (三个功能分组各自成框, 建立边界感)
--*   _text  静态文案 (标题/标签/提示; 集中登记便于按宽度折行, 防各语言溢出)
--* 本面板不滚动 (无 setScrollChildren), 故可直接按内容坐标绘制。
--*********************************************************
function EtherLootRollPanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherLootRollPanel:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text,
        font = font or UIFont.Small,
        header = false,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制。
        -- 不能在 createChildren 里即时画: 此时面板还没挂到父节点, getAbsoluteX
        -- 拿到的是错误坐标, 而且 createChildren 只执行一次, 画完即丢 (实机
        -- g1 提示消失即此因)。
        hint = hint or false,
    });
end

-- 分区标题: Small 薄荷居中 (标题在功能分组盒内, 与全局标题语言一致)
function EtherLootRollPanel:_header(tx, ty, text, w)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        header = true,
        w = w,
    });
end

--*********************************************************
--* Обработка prerender
--* 分组盒与静态文案画在子控件之下 (PZ 渲染序: prerender -> 子控件 -> render)
--*********************************************************
function EtherLootRollPanel:prerender()
    self:setStencilRect(0, 10, self:getWidth(), self:getHeight() - 20);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    if self.groups == nil then return end

    for i = 1, #self.groups do
        local g = self.groups[i];
        EtherTheme.drawTileBox(self, g.x, g.y, g.w, g.h, false, 8);
    end
    for i = 1, #self.texts do
        local t = self.texts[i];
        if t.header then
            EtherTheme.drawSectionTitle(self, t.x, t.y, t.w, t.text, nil, "lines");
        elseif t.hint then
            EtherTheme.drawHintText(self, t.text, t.x, t.y, t.col);
        else
            self:drawText(t.text, t.x, t.y, t.col.r, t.col.g, t.col.b, t.col.a or 1, t.font);
        end
    end
end

--*********************************************************
--* Обработка render (仅"仅游戏中可用"提示)
--*********************************************************
function EtherLootRollPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end
end

--*********************************************************
--* Создание дочерних элементов
--* 排版: 两个功能分组 (重置战利品 / 刷弹药) 各自一个切角盒,
--*   盒内统一 标题 -> 控件行 -> 提示 的节奏; 控件高度/内边距统一走
--*   EtherTheme.ctrlH / ctrlPadX / ctrlGap; 静态文案按内宽折行防各语言溢出。
--*********************************************************
function EtherLootRollPanel:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end

    local tm = getTextManager();
    local W = self.width;
    local PAD  = 16;                     -- 面板外边距
    local IP   = 12;                     -- 分组盒内边距
    local GAP  = EtherTheme.ctrlGap;     -- 控件/行间距
    local GGAP = 12;                     -- 分组之间间距
    local ctrlH = EtherTheme.ctrlH;
    local fhS  = EtherTheme.fontHgtSmall;
    local fhM  = EtherTheme.fontHgtMedium;
    local boxW = W - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;
    local labelDY = math.floor((ctrlH - fhS) / 2);   -- 标签相对控件行的垂直居中偏移

    self.groups = {};
    self.texts = {};

    -- ================= 分组1: 重置战利品 =================
    local g1y = 12;
    local cy = g1y + IP;
    self:_header(innerX, cy, getTranslate("UI_LootRoll_SectionReset"), innerW);
    cy = cy + fhS + GAP;

    -- 半径: 标签 + 输入框 + 重置按钮 (同排, 输入行高 entryH; 按钮在其中竖直居中)
    local radiusText = getTranslate("UI_LootRoll_RadiusLabel");
    self:_text(innerX, cy + EtherTheme.entryLabelDY, radiusText, EtherTheme.text, UIFont.Small);
    local rbX = innerX + tm:MeasureStringX(UIFont.Small, radiusText) + GAP;
    local rbW = 70;
    self.radiusBox = ISTextEntryBox:new(tostring(EtherContainerPOC.radius), rbX, cy, rbW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.radiusBox);
    self.radiusBox:initialise();
    self.radiusBox:instantiate();
    self.radiusBox:setClearButton(false);
    self:addChild(self.radiusBox);

    local resetTitle = getTranslate("UI_LootRoll_Button");
    local resetW = UIButton.measureWidth(resetTitle);
    local maxResetW = (innerX + innerW) - (rbX + rbW + GAP);
    if resetW > maxResetW then resetW = maxResetW; end
    self.resetBtn = UIButton:new(rbX + rbW + GAP, cy + EtherTheme.entryBtnDY, resetW, ctrlH, resetTitle,
    function()
        if not isMultiplayer() then
            print("[ContainerPOC] multiplayer only (use your own dedicated server)")
            return
        end
        local r = tonumber(self.radiusBox:getInternalText());
        if r ~= nil and r >= 1 and r <= 100 then
            EtherContainerPOC.radius = math.floor(r);
        end
        EtherContainerPOC.reset();
    end, resetW);
    self.resetBtn:initialise();
    self.resetBtn:instantiate();
    self.resetBtn.isOnlyInGame = true;
    self:addChild(self.resetBtn);
    cy = cy + EtherTheme.entryH + GAP;

    -- 提示 (按内宽折行, 防俄语等长文案越过右缘; 说明文字按 hintScale 缩小绘制)
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_LootRoll_Hint"), innerW);
    for i = 1, #hintLines do
        self:_text(innerX, cy, hintLines[i], EtherTheme.textDim, nil, true);
        cy = cy + EtherTheme.fontHgtHint;
    end
    self:_group(PAD, g1y, boxW, (cy - g1y) + IP);

    -- ================= 分组2: 刷弹药 =================
    local g2y = cy + IP + GGAP;
    cy = g2y + IP;
    self:_header(innerX, cy, getTranslate("UI_LootRoll_SectionFarm"), innerW);
    cy = cy + fhS + GAP;

    -- 三行竖排 (用户实测反馈: "自动开"文案带退弹提示较长, 与"自动关"并排时
    -- 被钳到半行宽, 文字溢出按钮框 —— 图1):
    --   行1: [自动刷(开)] 独占整行
    --   行2: [自动刷(关)] 独占整行
    --   行3: [设置弹药数] [弹药数输入框 占余宽]
    -- 各按钮按自身文案取宽、钳到整行内宽; 仍放不下的极端语言/字体档
    -- 由 UIButton:render 的缩字兜底保证不越出按钮框。
    -- 必须传 maxWidth 防 UIButton:new 的自动加宽还原钳制。
    local farmTitles = {
        getTranslate("UI_CharacterPanel_AmmoFarmAuto"),
        getTranslate("UI_CharacterPanel_AmmoFarmStop"),
        getTranslate("UI_CharacterPanel_AmmoFarmSet"),
    };
    local farmActions = {
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
            EtherAmmoFarm.enabled = true;
        end,
        function()
            EtherAmmoFarm.enabled = false;
        end,
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
        end,
    };

    local fw = {};
    for i = 1, 3 do
        fw[i] = math.min(UIButton.measureWidth(farmTitles[i]), innerW);
    end

    for i = 1, 2 do
        local btn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[i], ctrlH, farmTitles[i], farmActions[i], fw[i]);
        btn:initialise();
        btn:instantiate();
        self:addChild(btn);
        cy = cy + EtherTheme.entryH + GAP;
    end

    local b3 = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[3], ctrlH,
        farmTitles[3], farmActions[3], fw[3]);
    b3:initialise();
    b3:instantiate();
    self:addChild(b3);

    local ax = innerX + fw[3] + GAP;
    self.ammoFarmBox = ISTextEntryBox:new(tostring(getAmmoFarmCount()),
        ax, cy, (innerX + innerW) - ax, EtherTheme.entryH);
    EtherTheme.styleEntry(self.ammoFarmBox);
    self.ammoFarmBox:initialise();
    self.ammoFarmBox:instantiate();
    self.ammoFarmBox:setClearButton(false);
    self:addChild(self.ammoFarmBox);
    cy = cy + EtherTheme.entryH;
    self:_group(PAD, g2y, boxW, (cy - g2y) + IP);
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherLootRollPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_LootRoll_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    self.__index = self;

    return menuTableData;
end