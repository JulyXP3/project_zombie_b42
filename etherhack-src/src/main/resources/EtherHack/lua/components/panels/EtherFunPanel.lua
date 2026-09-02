require "ISUI/ISPanel"

--*********************************************************
--* 「趣味」面板: 冒名发消息 + 僵尸皮肤 (红队 PoC, 仅自有服务器验证)
--*
--* 冒名聊天: Java 侧 ChatAPI.sendChatAs (global) 转发 vanilla
--* ChatManager.sendMessageToChat(author, type, msg) —— B42 该方法
--* public (B41 Kairos 插件需要反射), author 任意串经
--* ChatMessageFromPlayer 包上送, 服务端零作者校验直接广播;
--* 广播跳过被冒名者 playerID (对方看不到), 目标不在线则整条丢弃。
--*
--* 僵尸皮肤: 纯 vanilla Lua 全局链 —— 玩家/僵尸共用身体纹理选择
--* (HumanVisual.getSkinTexture: skinTextureName 非空直接返回),
--* setSkinTextureName 塞 M/F_ZedBodyXX_levelY 后 resetModelNextFrame
--* 本地重建模, sendVisual 上行 HumanVisual 包 (服务端整包零校验
--* 全服广播), 即点即变免重登。皮肤随 HumanVisual.save 写入存档,
--* 重登保持; 首次伪装前记原值, 「恢复」按原值还原。
--*
--* 僵尸烂脸: 皮肤只换身体底图, 头/脸不变 (实测); 烂脸真身是
--* ZedDmg_* 覆盖件 (僵尸同款 bodyVisuals 通道)。vanilla public
--* addBodyVisualFromItemType/removeBodyVisualFromItemType 直调,
--* 随 HumanVisual 同包同步; 先清后加 = 每次点击换脸, 恢复一并清。
--*
--* Kahlua 陷阱备忘 (check_kahlua_compat.lua 禁则):
--*   - 无 string.trim: 手写 ^%s*(.-)%s*$ 模式
--*   - addItem/drawText 文本一律字符串; type() 可用
--*********************************************************
EtherFunPanel = EtherFormPanel:derive("EtherFunPanel");

-- 类级会话状态: 原皮肤纹理名 (首次伪装前捕获, 面板重建不丢)
EtherFunPanel.savedSkin = nil;

--*********************************************************
--* 工具: Kahlua 无 string.trim 的等价物
--*********************************************************
local function trim(s)
    if s == nil then return "" end
    return (string.match(s, "^%s*(.-)%s*$"));
end

--*********************************************************
--* 工具: 状态行设置 (renderContent 绘制)
--*********************************************************
local function setStatus(panel, key)
    panel.statusText = getTranslate(key);
end

--*********************************************************
--* 僵尸皮肤: 按性别随机挑一张 (每次点击换一张, level=腐烂等级)
--*********************************************************
local function pickZedTexture(level)
    local p = getPlayer();
    if p == nil then return nil; end
    local female = p:isFemale();
    local n = female and 3 or 4;
    local idx = ZombRand(n) + 1;
    local prefix = female and "F_ZedBody" or "M_ZedBody";
    -- string.format 在 Kahlua 可用 (交换页/雷达页同款先例)
    return string.format("%s%02d_level%d", prefix, idx, level);
end

local function applySkinTexture(panel, tex)
    local p = getPlayer();
    if p == nil then return false; end
    local hv = p:getHumanVisual();
    if hv == nil then return false; end
    -- 首次伪装前捕获原皮肤 (getSkinTexture: name 非空直接返回, 否则按 index 生成)
    if EtherFunPanel.savedSkin == nil then
        local orig = hv:getSkinTexture();
        if orig ~= nil and orig ~= "" then
            EtherFunPanel.savedSkin = orig;
        end
    end
    hv:setSkinTextureName(tex);
    p:resetModelNextFrame();
    -- vanilla 全局 (LuaManager @LuaMethod global): MP 下上行 HumanVisual 包,
    -- 服务端 parse 整包 load 零校验 -> processServer 全服广播; SP 下为空操作
    sendVisual(p);
    return true;
end

--*********************************************************
--* 烂脸: ZedDmg 面部覆盖件 (bodyVisuals 通道, 僵尸同款)。
--* 全部是 Base.ZedDmg_* 脚本项 (BodyLocation=zeddmg + hidden,
--* 不进物品栏不顶帽子), 随 HumanVisual 同包同步零校验。
--*********************************************************
local ZED_FACE_POOL = {
    "ZedDmg_FaceSkullLeft", "ZedDmg_FaceSkullRight",     -- 脸露骷髅
    "ZedDmg_BulletFace01", "ZedDmg_BulletFace02",         -- 枪击烂脸
    "ZedDmg_HeadSlashCentre01", "ZedDmg_HeadSlashCentre02", "ZedDmg_HeadSlashCentre03",
    "ZedDmg_HeadSlashLeft01", "ZedDmg_HeadSlashLeft02",
    "ZedDmg_HeadSlashRight01", "ZedDmg_HeadSlashRight02",
    "ZedDmg_ShotgunFaceFull", "ZedDmg_ShotgunFaceLeft",   -- 霰弹毁容
    "ZedDmg_BulletForehead01", "ZedDmg_BulletForehead02", -- 额洞
};

-- 移除已装的全部 ZedDmg_* bodyVisuals (先清后加 = 每次点击换脸)
local function removeZedFaceVisuals(hv)
    local removed = false;
    -- 快照遍历 (bodyVisuals 是 Java ArrayList, 边删边遍历会跳项)
    local snapshot = {};
    local visuals = hv:getBodyVisuals();
    for i = 0, visuals:size() - 1 do
        local t = visuals:get(i):getItemType();
        if t ~= nil then snapshot[#snapshot + 1] = t; end
    end
    for i = 1, #snapshot do
        local t = snapshot[i];
        -- plain find 防物品名含 Lua 模式字符
        if string.find(t, "ZedDmg_", 1, true) ~= nil then
            hv:removeBodyVisualFromItemType(t);
            removed = true;
        end
    end
    return removed;
end

local function applyZedFace(panel)
    local p = getPlayer();
    if p == nil then return false; end
    local hv = p:getHumanVisual();
    if hv == nil then return false; end
    removeZedFaceVisuals(hv);
    -- 随机 1-2 件面部 ZedDmg 覆盖件 (addBodyVisualFromItemType: vanilla public,
    -- 内部按 clothingItemName 去重; 件不存在/无 ClothingItem 字段返回 null, 静默)
    local count = 1 + ZombRand(2);
    for i = 1, count do
        local pick = ZED_FACE_POOL[ZombRand(#ZED_FACE_POOL) + 1];
        hv:addBodyVisualFromItemType("Base." .. pick);
    end
    p:resetModelNextFrame();
    sendVisual(p);
    return true;
end

--*********************************************************
--* 工具: 标签 + 输入框一行 (标签左, 输入框吃余宽; 陷阱页同款观感)
--*********************************************************
local function entryRow(panel, key, x, y, w)
    local labelT = getTranslate(key);
    local tm = getTextManager();
    local labelW = tm:MeasureStringX(UIFont.Small, labelT);
    -- 标签限宽 40%, 防长翻译压住输入框 (交换页同规则)
    local maxLabelW = math.floor(w * 0.4);
    if labelW > maxLabelW then labelW = maxLabelW; end
    local entW = w - labelW - EtherTheme.ctrlGap;
    if entW < 60 then entW = 60; end

    local label = EtherTheme.makeLabel(x, y, EtherTheme.entryH, labelT, EtherTheme.text);
    panel:addChild(label);
    panel:_track(label, { onlyInGame = true });

    local entry = ISTextEntryBox:new("", x + labelW + EtherTheme.ctrlGap, y, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();
    panel:addChild(entry);
    panel:_track(entry, { onlyInGame = true });
    return entry;
end

--*********************************************************
--* 工具: 等宽按钮组一行 (按最宽文字统一宽度, 居中排列)
--*********************************************************
local function buttonRow(panel, defs, x, y, w)
    local titles = {};
    for i = 1, #defs do titles[i] = getTranslate(defs[i][1]); end
    local btnW = UIButton.measureGroupWidth(titles);
    local gap = EtherTheme.ctrlGap;
    local totalW = btnW * #defs + gap * (#defs - 1);
    if totalW > w then
        btnW = math.floor((w - gap * (#defs - 1)) / #defs);
        totalW = btnW * #defs + gap * (#defs - 1);
    end
    local bx = x + math.floor((w - totalW) / 2);
    local btns = {};
    for i = 1, #defs do
        local btn = UIButton:new(bx, y, btnW, EtherTheme.ctrlH, titles[i], defs[i][2], btnW);
        btn:initialise();
        btn:instantiate();
        panel:addChild(btn);
        panel:_track(btn, { onlyInGame = true });
        btns[i] = btn;
        bx = bx + btnW + gap;
    end
    return btns;
end

--*********************************************************
--* 面板内容构建 (基类 createChildren 回调)
--*********************************************************
function EtherFunPanel:build()
    local gap = EtherTheme.ctrlGap;
    local entryH = EtherTheme.entryH;
    local ctrlH = EtherTheme.ctrlH;
    self.statusText = "";

    -- ================= 模块一: 冒名发消息 =================
    local chatRowsH = entryH * 2 + gap + ctrlH + gap + EtherTheme.fontHgtSmall;
    self:addModule("UI_Fun_ChatTitle", chatRowsH, function(bx, by, bw)
        local cy = by;

        self.authorEntry = entryRow(self, "UI_Fun_ChatAuthor", bx, cy, bw);
        cy = cy + entryH + gap;

        self.msgEntry = entryRow(self, "UI_Fun_ChatMessage", bx, cy, bw);
        cy = cy + entryH + gap;

        -- 频道切换 + 发送: 同行左右 (陷阱页 modeBtn 同款 title 热切换)
        self.chatChannel = "general";
        local function channelTitle()
            return getTranslate("UI_Fun_Channel") .. ": " ..
                getTranslate(self.chatChannel == "general" and "UI_Fun_ChGeneral" or "UI_Fun_ChSay");
        end
        local sendT = getTranslate("UI_Fun_Send");
        local chMaxW = math.floor((bw - gap) * 0.6);
        local chW = UIButton.measureWidth(channelTitle());
        if chW > chMaxW then chW = chMaxW; end
        local sendW = math.floor(bw - chW - gap);

        self.channelBtn = UIButton:new(bx, cy, chW, ctrlH, channelTitle(), function()
            self.chatChannel = (self.chatChannel == "general") and "say" or "general";
            self.channelBtn.title = channelTitle();
        end, chW);
        self.channelBtn:initialise();
        self.channelBtn:instantiate();
        self:addChild(self.channelBtn);
        self:_track(self.channelBtn, { onlyInGame = true });

        self.sendBtn = UIButton:new(bx + chW + gap, cy, sendW, ctrlH, sendT, function()
            local author = trim(self.authorEntry:getInternalText());
            local text = trim(self.msgEntry:getInternalText());
            if author == "" or text == "" then
                setStatus(self, "UI_Fun_ErrEmpty");
                return;
            end
            if sendChatAs(author, self.chatChannel, text) then
                -- 消息已走 vanilla 发送链 (本地聊天框也会显示一条), 清输入留玩家名便于连发
                if self.msgEntry.clear then self.msgEntry:clear(); end
                setStatus(self, "UI_Fun_Sent");
            else
                setStatus(self, "UI_Fun_SendFailed");
            end
        end, sendW);
        self.sendBtn:initialise();
        self.sendBtn:instantiate();
        self:addChild(self.sendBtn);
        self:_track(self.sendBtn, { onlyInGame = true });
        cy = cy + ctrlH + gap;

        -- 状态行绘制锚点 (renderContent 里画)
        self.chatStatusX = bx;
        self.chatStatusY = cy;
    end);

    self:addSpacer(EtherFormPanel.SECTION_GAP);

    -- ================= 模块二: 僵尸皮肤 =================
    local skinRowsH = ctrlH + gap + EtherTheme.fontHgtSmall;
    self:addModule("UI_Fun_SkinTitle", skinRowsH, function(bx, by, bw)
        local cy = by;
        buttonRow(self, {
            { "UI_Fun_SkinLight", function()
                if applySkinTexture(self, pickZedTexture(1)) then
                    setStatus(self, "UI_Fun_SkinDone");
                end
            end },
            { "UI_Fun_SkinHeavy", function()
                if applySkinTexture(self, pickZedTexture(3)) then
                    setStatus(self, "UI_Fun_SkinDone");
                end
            end },
            { "UI_Fun_SkinFace", function()
                if applyZedFace(self) then
                    setStatus(self, "UI_Fun_SkinFaceDone");
                end
            end },
            { "UI_Fun_SkinRestore", function()
                local did = false;
                local p = getPlayer();
                if p ~= nil then
                    local hv = p:getHumanVisual();
                    if hv ~= nil then
                        if removeZedFaceVisuals(hv) then did = true; end
                    end
                end
                if EtherFunPanel.savedSkin == nil then
                    if did then
                        p:resetModelNextFrame();
                        sendVisual(p);
                        setStatus(self, "UI_Fun_SkinRestored");
                    else
                        setStatus(self, "UI_Fun_SkinNoSaved");
                    end
                    return;
                end
                if applySkinTexture(self, EtherFunPanel.savedSkin) then
                    setStatus(self, "UI_Fun_SkinRestored");
                end
            end },
        }, bx, cy, bw);
        cy = cy + ctrlH + gap;
        self.skinStatusX = bx;
        self.skinStatusY = cy;
    end);

    -- ================= 底部提示 =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    local hintW = self:_rowContentW();
    self.hintLines = EtherTheme.wrapHint(getTranslate("UI_Fun_Hint"), hintW);
    self.hintH = #self.hintLines * EtherTheme.fontHgtHint;
    -- 行 y 由 addCustomRow 的 builder 回调给出 (游标推进在其内部发生, 外侧算不准)
    self:addCustomRow(self.hintH + 4, function(x, y, w)
        self.hintX = x;
        self.hintY = y + 2;
    end);
end

--*********************************************************
--* 状态行 + 提示绘制 (EtherFormPanel:render 回调, 需自带滚动偏移)
--*********************************************************
function EtherFunPanel:renderContent()
    local ys = self:getYScroll();
    if self.statusText ~= nil and self.statusText ~= "" then
        local td = EtherTheme.textDim;
        self:drawText(self.statusText, self.chatStatusX, self.chatStatusY + ys,
            td.r, td.g, td.b, 1, UIFont.Small);
    end
    if self.hintLines ~= nil and self.hintX ~= nil then
        for i = 1, #self.hintLines do
            EtherTheme.drawHintText(self, self.hintLines[i],
                self.hintX, self.hintY + (i - 1) * EtherTheme.fontHgtHint,
                EtherTheme.textDim);
        end
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel。
--*********************************************************
