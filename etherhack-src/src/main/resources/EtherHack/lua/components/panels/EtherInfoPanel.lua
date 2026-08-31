require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherInfoPanel = ISPanel:derive("Dei0InfoPanel"); -- Наследование от ISPanel

local SIDE_PAD = 16;   -- 左右安全边距 (文本不得越过)

-- 作者名单 (原先写在 EtherHackMenu 页眉里, 随署名一起移到本面板)
local AUTHORS = "Quzile & Yeet-Masta & dei0 & JulyXP3";

--*********************************************************
--* 折行统一走 EtherTheme (公共实现, 信息页/各面板共用),
--* 避免同一套 UTF-8 折行逻辑在多处各写一遍。
--* 正文段落属于"说明文字": 走 wrapHint, 按 hintScale 缩小绘制;
--* 分区标题(drawSectionTitle)不是说明文字, 仍用 Small 原尺寸。
--*********************************************************

--*********************************************************
--* 缩放居中绘制一行正文 (说明文字): 宽度按 hintScale 折算, 保证真居中
--*********************************************************
function EtherInfoPanel:drawHintCentered(text, y, col)
    local x = (self.width - EtherTheme.hintWidth(text)) / 2;
    EtherTheme.drawHintText(self, text, x, y, col);
end

--*********************************************************
--* 构建显示行列表 (折行结果缓存, 不在 render 里每帧测量)。
--* 缓存失效条件: 面板宽度变化 / 语言切换 / 反作弊状态变化。
--*********************************************************
function EtherInfoPanel:buildLines()
    local th = EtherTheme;
    local maxW = self.width - SIDE_PAD * 2 - 32;   -- 盒内文字再留边
    local blocks = {};
    local fhS = th.fontHgtSmall;          -- 分区标题行高 (Small, 原尺寸)
    local lhH = th.fontHgtHint + 4;       -- 正文行距 (hintScale 缩小后的说明文字)

    -- 块 = 切角盒包裹 "---- 标题 ----" + 全部内容行 (实机定稿: 标题+内容一起被包)
    local function addBlock(titleKey, entries)
        if (titleKey == nil) and (#entries == 0) then return; end
        local t = nil;
        local tw = 0;
        if titleKey ~= nil then
            t = getTranslate(titleKey);
            tw = getTextManager():MeasureStringX(UIFont.Small, t);
        end
        local h = 8 + (t ~= nil and (fhS + 6) or 0) + #entries * lhH + 8;
        table.insert(blocks, { title = t, titleW = tw, lines = entries, h = h });
    end

    local function keysToLines(keys, col)
        local out = {};
        for i = 1, #keys do
            local wrapped = EtherTheme.wrapHint(getTranslate(keys[i]), maxW);
            for j = 1, #wrapped do
                table.insert(out, { text = wrapped[j], col = col });
            end
        end
        return out;
    end

    local function textToLines(text, col)
        local out = {};
        local wrapped = EtherTheme.wrapHint(text, maxW);
        for j = 1, #wrapped do
            table.insert(out, { text = wrapped[j], col = col });
        end
        return out;
    end

    -- 声明 (作者署名 + 开源声明)
    local decl = textToLines(tr("UI_Title_Credit", { names = AUTHORS }), th.text);
    local decl2 = textToLines(tr("UI_Title_Notice"), th.text);
    for i = 1, #decl2 do table.insert(decl, decl2[i]); end
    addBlock("UI_InformationPanel_Declaration_Title", decl);

    addBlock("UI_InformationPanel_General_Title", keysToLines({
        "UI_InformationPanel_General_Text1",
        "UI_InformationPanel_General_Text2",
        "UI_InformationPanel_General_Text3",
        "UI_InformationPanel_General_Text4",
        "UI_InformationPanel_General_Text5",
    }, th.text));

    addBlock("UI_InformationPanel_Disclaimer_Title", keysToLines({
        "UI_InformationPanel_Disclaimer_Text1",
        "UI_InformationPanel_Disclaimer_Text2",
        "UI_InformationPanel_Disclaimer_Text3",
        "UI_InformationPanel_Disclaimer_Text4",
        "UI_InformationPanel_Disclaimer_Text5",
    }, th.textDim));

    -- 反作弊状态 (逐项带 [启用/禁用], 状态色区分)
    local statusLines = {};
    local statusKeys = {
        "UI_InformationPanel_AntiCheatStatus_Text1",
        "UI_InformationPanel_AntiCheatStatus_Text2",
        "UI_InformationPanel_AntiCheatStatus_BikiniTools",
        "UI_InformationPanel_AntiCheatStatus_CustomLogger",
    };
    local flags = self:statusFlags();
    for i = 1, #statusKeys do
        local on = flags[i];
        local statusText = on and getTranslate("UI_InformationPanel_AntiCheatStatus_Enable")
                              or getTranslate("UI_InformationPanel_AntiCheatStatus_Disable");
        local col = on and th.text or th.statusRed;
        local lineText = tr("UI_InformationPanel_AntiCheatStatus_Format", {
            label = getTranslate(statusKeys[i]),
            value = statusText,
        });
        local wrapped = EtherTheme.wrapHint(lineText, maxW);
        for j = 1, #wrapped do
            table.insert(statusLines, { text = wrapped[j], col = col });
        end
    end
    addBlock("UI_InformationPanel_AntiCheatStatus_Title", statusLines);

    -- 联系方式: 平台名是专有名词(不翻译), 但"无"必须可翻译; 链接纯文本显示
    local none = getTranslate("UI_Common_None");
    local channels = {
        { label = "GitHub",  value = "https://github.com/JulyXP3/project_zombie_b42" },
        { label = "Discord", value = "https://discord.gg/bkyNqSYyh" },
        { label = "Email" },
        { label = getTranslate("UI_InformationPanel_Contacts_Donation") },
    };
    local contactLines = {};
    for i = 1, #channels do
        local ch = channels[i];
        local wrapped = EtherTheme.wrapHint(tr("UI_Fmt_LabelValue",
            { label = ch.label, value = ch.value or none }), maxW);
        for j = 1, #wrapped do
            table.insert(contactLines, { text = wrapped[j], col = th.textDim });
        end
    end
    addBlock("UI_InformationPanel_Contacts_Title", contactLines);

    self.blocks = blocks;
    self.cacheW = self.width;
    self.cacheLang = getLanguage();
    self.cacheFlags = self:statusFlags();

    -- 内容总高 (供滚动范围用)
    local total = 12;
    for i = 1, #blocks do
        total = total + blocks[i].h + 14;
    end
    self.contentH = total;
end

--*********************************************************
--* 反作弊/日志检测状态 (布尔数组) 与变更检测 (用于缓存失效判断)
--*********************************************************
function EtherInfoPanel:statusFlags()
    local customLogger = PARP ~= nil or LogExtenderClient ~= nil or LogExtenderServer ~= nil or AVCS ~= nil;
    local bikinitools = BTSE ~= nil or PARP ~= nil or Bikinitools ~= nil;
    local inGame = self.localPlayer ~= nil;
    return {
        inGame and getAntiCheat12Status() or false,
        inGame and getAntiCheat8Status() or false,
        inGame and bikinitools or false,
        inGame and customLogger or false,
    };
end

--- 变更检测去字符串化: render 每帧调用, 直接逐位比较特征布尔
--- (替代旧的拼 "1"/"0" 签名串; 两个反作弊状态均返回 boolean, 元素比对与签名比对等价)
function EtherInfoPanel:statusFlagsChanged()
    local c = self.cacheFlags;
    if c == nil then return true; end
    local f = self:statusFlags();
    for i = 1, #f do
        if c[i] ~= f[i] then return true; end
    end
    return false;
end

--*********************************************************
--* Отрисовка текста (折行 + 竖向滚动, 保证长文本不越界)
--*********************************************************
function EtherInfoPanel:render()
    local th = EtherTheme;

    -- 缓存失效则重建折行 (宽度/语言/状态变化)
    if self.lines == nil or self.cacheW ~= self.width
        or self.cacheLang ~= getLanguage() or self:statusFlagsChanged() then
        self:buildLines();
    end

    -- 竖向滚动范围钳制 (内容比面板高时才可滚)
    local minScroll = math.min(0, self.height - self.contentH - 12);
    if self.scrollY == nil then self.scrollY = 0; end
    if self.scrollY < minScroll then self.scrollY = minScroll; end
    if self.scrollY > 0 then self.scrollY = 0; end

    self:setStencilRect(0, 10, self.width, self.height - 20);

    -- 块渲染: 切角盒包裹 "---- 标题 ----" + 内容行 (标题与内容同盒, 实机定稿;
    -- 标题仍是 Small, 正文按 hintScale 缩小)
    local BX = 24;
    local BW = self.width - BX * 2;
    local fhS = th.fontHgtSmall;
    local lhH = th.fontHgtHint + 4;
    local y = 10 + self.scrollY;
    for i = 1, #self.blocks do
        local blk = self.blocks[i];
        if y + blk.h > 0 and y < self.height then
            EtherTheme.drawTileBox(self, BX, y, BW, blk.h, false, 8);
            local iy = y + 8;
            if blk.title ~= nil then
                EtherTheme.drawSectionTitle(self, BX + 10, iy, BW - 20,
                    blk.title, blk.titleW, "lines");
                iy = iy + fhS + 6;
            end
            for j = 1, #blk.lines do
                local ln = blk.lines[j];
                if iy + lhH > 0 and iy < self.height then
                    self:drawHintCentered(ln.text, iy, ln.col);
                end
                iy = iy + lhH;
            end
        end
        y = y + blk.h + 14;
    end

    self:clearStencilRect();
end

--*********************************************************
--* 滚轮滚动 (内容超高时可看完整声明)
--*********************************************************
function EtherInfoPanel:onMouseWheel(del)
    self.scrollY = (self.scrollY or 0) - (del * 40);
    return true;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherInfoPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    menuTableData.scrollY = 0;
    self.__index = self;

    return menuTableData;
end