require "ISUI/ISPanel"

--*********************************************************
--* EtherCharacterBoostPanel: 创建角色面板
--*
--* 建号相关功能集中页 (2026-08-26 用户需求, 自角色页/其他页迁出):
--*   - 自定义编辑 (上): 建号时自由添加/删除特性 (名单)、自定义技能等级 (0..10)。
--*     名单持久化到配置 (EtherAPI.charCreateCustomTraits / charCreateCustomSkillLevels),
--*     实际生效在建号包发送瞬间 (CharacterCreationBoost.apply 注入)。
--*   - 建号增强 (下): 建号全特性 / 建号技能满级 / 解锁全部服装
--*     (解锁全部服装 = Lua 覆盖建号界面 shouldShowAllOutfits, 见
--*      override/EtherCharacterCreation.lua; 原「建号自带服装」预设注入已删除)
--*     + 角色特性点数滑块 (自「其他」页迁入; 仅建号流程中显示,
--*       CharacterCreationProfession.instance 为 nil 时隐藏该行)。
--*
--* 生效时机: 所有注入都在 CreatePlayerPacket.set 瞬间读取开关与名单,
--* 建号界面停留期间勾选/取消只改状态, 点最终创建前取消即等效回退;
--* 角色创建完成后无法回退 (已随包定型), 开关只影响下一次建号。
--*
--* 零功能影响: toggleCharCreateX/isCharCreateX 等 Java 全局原样沿用。
--* 本页面在主菜单即可使用 (控件不标 onlyInGame)。
--*********************************************************

EtherCharacterBoostPanel = EtherFormPanel:derive("EtherCharacterBoostPanel");

local LIST_H = 210;   -- 自定义编辑两个列表的高度

--*********************************************************
--* 模块内一行复选框的高度预算 (ESP 页同款折行/行距规则)
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
        cb.width = w;
    end
    panel:addWidget(cb, item);
    return rowStep(item, w);
end

--*********************************************************
--* 数据源枚举
--*********************************************************
local function buildTraitEntries()
    local defs = CharacterTraitDefinition.getTraits();
    local out = {};
    for i = 0, defs:size() - 1 do
        local def = defs:get(i);
        table.insert(out, {
            type = def:getType():getName(),     -- 显式 getName() -> 真 Lua 字符串 (tostring 对 Java 对象不可靠)
            label = def:getLabel(),
            cost = def:getCost(),
        });
    end
    table.sort(out, function(a, b) return string.sort(a.label, b.label); end);
    return out;
end

local function getPerkDisplayName(perkName)
    local key = "IGUI_perks_" .. perkName;
    local t = getText(key);
    if t == nil or t == key or t == "" then return perkName; end
    return t;
end

local function buildPerkEntries()
    local out = {};
    -- vanilla 同款链 (ISPlayerStatsUI.lua:720): Perks 枚举常量没有 getName(),
    -- 必须经 PerkFactory.getPerk 转成 Perk 对象再调
    local maxIndex = Perks.getMaxIndex();
    for i = 0, maxIndex - 1 do
        local perk = PerkFactory.getPerk(Perks.fromIndex(i));
        if perk ~= nil and perk ~= Perks.None and perk ~= Perks.MAX
            and not (perk.parent == Perks.None
                and perk ~= Perks.Fitness and perk ~= Perks.Strength) then
            local name = perk:getName();        -- 显式 getName() -> 真 Lua 字符串
            table.insert(out, {
                name = name,
                label = getPerkDisplayName(name),
            });
        end
    end
    table.sort(out, function(a, b) return string.sort(a.label, b.label); end);
    return out;
end

local function getCustomTraitSet()
    local set = {};
    if getCharacterBoostCustomTraits == nil then return set; end
    local t = getCharacterBoostCustomTraits();
    for _, v in pairs(t) do
        set[v] = true;
    end
    return set;
end

local function getSkillLevel(panel, name)
    -- 读 Lua 镜像 (Lua number, 规避 KahluaTable 取值/Java Integer 比较的怪癖)
    local v = panel.skillLevels[name];
    return v == nil and 0 or v;
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调)
--*********************************************************
function EtherCharacterBoostPanel:build()
    self.customTraits = getCustomTraitSet();
    self.hintRows = {};                          -- 提示行登记 {text,x,y} (renderContent 绘制;
                                                 -- build 时面板未入 UI 管理器, 直接画坐标无效)
    self.skillLevels = {};                       -- Lua 侧镜像 {name=number}: 显示与比较的稳定来源
    if getCharacterBoostCustomSkillLevels ~= nil then
        local t = getCharacterBoostCustomSkillLevels();
        for k, v in pairs(t) do
            local n = tonumber(v);
            if n ~= nil and n > 0 then self.skillLevels[k] = n; end
        end
    end

    local traitDefs = buildTraitEntries();
    local perkDefs = buildPerkEntries();

    local w = self:_rowContentW();
    local innerW = w - EtherFormPanel.BOX_PAD_X * 2;

    -- ================= 模块一: 自定义编辑 =================
    local searchH = EtherTheme.entryH;
    local ctrlRowH = EtherTheme.ctrlH;
    local hintKey = "UI_CharacterBoostPanel_CustomHint";
    local hintLines = EtherTheme.wrapHint(tr(hintKey), innerW);
    local hintH = #hintLines * EtherTheme.fontHgtHint;

    -- 高度: 特性(搜索+列表+两按钮) + 技能(搜索+列表+两按钮); 提示行紧贴控制条
    local traitButtonsH = EtherTheme.ctrlH + 6;
    local skillButtonsH = EtherTheme.ctrlH + 6;
    local moduleH = searchH + 4 + LIST_H + 6 + traitButtonsH
        + 20 + searchH + 4 + LIST_H + 6 + skillButtonsH
        + 12 + hintH + 6;

    self:addModule("UI_CharacterBoostPanel_Group_Custom", moduleH, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iW = bw - EtherFormPanel.BOX_PAD_X * 2;

        local function makeSection(cy, labelKey, entries)
            local lb = EtherTheme.makeLabel(ix, cy, EtherTheme.entryH, tr(labelKey));
            self:addChild(lb);
            local entryX = ix + getTextManager():MeasureStringX(UIFont.Small, tr(labelKey)) + EtherTheme.ctrlGap;
            local entry = ISTextEntryBox:new("", entryX, cy,
                math.max(80, ix + iW - entryX), EtherTheme.entryH);
            EtherTheme.styleEntry(entry);
            self:addWidget(entry);

            local listY = cy + EtherTheme.entryH + 4;
            local list = ISScrollingListBox:new(ix, listY, iW, LIST_H);
            list:initialise();
            list:instantiate();
            -- 游戏 new 默认 font=UIFont.Large (行高≈40px); 行高压到 Small 后
            -- Large 字被挤裁, 显式降到 Small 与自定义 doDrawItem 的字号一致
            list.font = UIFont.Small;
            list.fontHgt = getTextManager():getFontFromEnum(UIFont.Small):getLineHeight();
            list.itemheight = EtherTheme.listItemH;
            list.selected = -1;
            list.drawBorder = false;
            EtherTheme.styleList(list);
            self:addWidget(list);

            local function refill()
                local query = string.lower(entry:getInternalText() or "");
                list:clear();
                for i = 1, #entries do
                    local e = entries[i];
                    if query == "" or string.find(string.lower(e.label), query, 1, true) ~= nil then
                        list:addItem(e.label, e);
                    end
                end
            end
            entry.onTextChange = refill;
            refill();
            return list, listY + LIST_H;
        end

        -- ---- 特性区 (仅通过下方按钮添加/移除，行点击不直接切换) ----
        local traitList, nextY = makeSection(by, "UI_CharacterBoostPanel_Traits", traitDefs);
        function traitList:doDrawItem(y, item, alt)
            if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
                return y + self.itemheight;
            end
            EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);
            local mark = self.panel.customTraits[item.item.type] and "[+] " or "[ ] ";
            self:drawText(mark .. item.item.label .. " (" .. tostring(item.item.cost) .. ")",
                6, y + 4, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, UIFont.Small);
            return y + self.itemheight;
        end
        traitList.panel = self;

        -- 特性 添加/移除 按钮
        local traitBtnY = nextY + 6;
        local traitBtnW = math.floor((iW - EtherTheme.ctrlGap) / 2);
        local addTraitBtn = UIButton:new(ix, traitBtnY, traitBtnW, EtherTheme.ctrlH,
            tr("UI_CharacterBoostPanel_Add"), function()
                local sel = traitList.selected;
                if sel == nil or sel < 1 or sel > #traitList.items then return; end
                local it = traitList.items[sel].item;
                if it == nil or it.type == nil then return; end
                if not self.customTraits[it.type] then
                    addCharacterBoostCustomTrait(it.type);
                    self.customTraits[it.type] = true;
                end
            end, traitBtnW);
        addTraitBtn:initialise(); addTraitBtn:instantiate(); self:addChild(addTraitBtn);
        local remTraitBtn = UIButton:new(ix + traitBtnW + EtherTheme.ctrlGap, traitBtnY, traitBtnW, EtherTheme.ctrlH,
            tr("UI_CharacterBoostPanel_Remove"), function()
                local sel = traitList.selected;
                if sel == nil or sel < 1 or sel > #traitList.items then return; end
                local it = traitList.items[sel].item;
                if it == nil or it.type == nil then return; end
                if self.customTraits[it.type] then
                    removeCharacterBoostCustomTrait(it.type);
                    self.customTraits[it.type] = nil;
                end
            end, traitBtnW);
        remTraitBtn:initialise(); remTraitBtn:instantiate(); self:addChild(remTraitBtn);

        -- ---- 技能区 ----
        local skillList, afterSkills = makeSection(nextY + 6 + traitButtonsH + 20,
            "UI_CharacterBoostPanel_Skills", perkDefs);
        skillList.target = self;

        function skillList:doDrawItem(y, item, alt)
            if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
                return y + self.itemheight;
            end
            EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);
            local lvl = getSkillLevel(self.panel, item.item.name);
            local mark = lvl > 0 and ("[" .. tostring(lvl) .. "] ") or "[ ] ";
            self:drawText(mark .. item.item.label,
                6, y + 4, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, UIFont.Small);
            return y + self.itemheight;
        end
        skillList.panel = self;

        -- ---- 技能等级控制: 「等级-」「等级+」两按钮 ----
        local ctrlY = afterSkills + 6;
        local skillBtnW = math.floor((iW - EtherTheme.ctrlGap) / 2);

        local function selectedSkillEntry()
            local sel = skillList.selected;
            if sel == nil or sel < 1 or sel > #skillList.items then return nil; end
            return skillList.items[sel].item;
        end
        local function adjust(delta)
            local it = selectedSkillEntry();
            if it == nil or it.name == nil then return; end
            local v = getSkillLevel(self, it.name) + delta;   -- 镜像恒为 Lua number
            if v < 0 then v = 0; end
            if v > 10 then v = 10; end
            self.skillLevels[it.name] = (v > 0) and v or nil;
            setCharacterBoostCustomSkillLevel(it.name, v);    -- 同步 Java 持久化
        end

        local decBtn = UIButton:new(ix, ctrlY, skillBtnW, EtherTheme.ctrlH,
            tr("UI_CharacterBoostPanel_LevelDown"), function() adjust(-1); end, skillBtnW);
        decBtn:initialise(); decBtn:instantiate(); self:addChild(decBtn);

        local incBtn = UIButton:new(ix + skillBtnW + EtherTheme.ctrlGap, ctrlY, skillBtnW, EtherTheme.ctrlH,
            tr("UI_CharacterBoostPanel_LevelUp"), function() adjust(1); end, skillBtnW);
        incBtn:initialise(); incBtn:instantiate(); self:addChild(incBtn);

        -- 提示行 (登记位置, renderContent 统一绘制)
        local hintY = ctrlY + ctrlRowH + 4;
        for hi = 1, #hintLines do
            table.insert(self.hintRows, { text = hintLines[hi], x = ix,
                y = hintY + (hi - 1) * EtherTheme.fontHgtHint });
        end
    end);

    -- ================= 模块二: 建号增强 =================
    local toggles = {
        { key = "UI_CharacterBoostPanel_AllTraits",   on = toggleCharCreateAllTraits,  get = isCharCreateAllTraits },
        { key = "UI_CharacterBoostPanel_MaxSkills",   on = toggleCharCreateMaxSkills,  get = isCharCreateMaxSkills },
        { key = "UI_CharacterBoostPanel_AllClothing", on = toggleCharCreateAllClothes, get = isCharCreateAllClothes },
    };
    local contentH = 0;
    for i = 1, #toggles do
        contentH = contentH + rowStep(toggles[i], innerW);
    end
    local prof = CharacterCreationProfession and CharacterCreationProfession.instance;
    local sliderH = (prof ~= nil) and 52 or 0;
    local boostHintKey = "UI_CharacterBoostPanel_Hint";
    local boostHints = EtherTheme.wrapHint(tr(boostHintKey), innerW);
    local boostHintH = #boostHints * EtherTheme.fontHgtHint;

    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:addModule("UI_CharacterBoostPanel_Group_Boost",
        contentH + 8 + sliderH + 8 + boostHintH + 6, function(bx, by, bw)
            local cy = by;
            for i = 1, #toggles do
                cy = cy + moduleRow(self, toggles[i], bx, cy, bw);
            end
            cy = cy + 8;

            -- 角色特性点数 (仅建号流程): UISlider 行, addSlider 同款手工构造
            if prof ~= nil then
                local tm = getTextManager();
                local rowW = self:_rowContentW();
                local label = tr("UI_CharacterBoostPanel_TraitPoints");
                local labelW = tm:MeasureStringX(UIFont.Small, label);
                local sliderW = math.floor(rowW * 0.55);
                local maxSlider = rowW - labelW - 56;
                if sliderW > maxSlider then sliderW = maxSlider; end
                if sliderW < 80 then sliderW = 80; end
                local sx = bx + rowW - sliderW - EtherFormPanel.BOX_PAD_X;
                local slider = UISlider:new(sx, cy + 1, sliderW, 44,
                    CharacterCreationProfession.instance:PointToSpend(),
                    -1000, 1000, function(value)
                        CharacterCreationProfession.instance.pointToSpend =
                            value - CharacterCreationProfession.instance.cost;
                    end);
                slider:initialise();
                slider:instantiate();
                self:_anchor(slider);
                self:addChild(slider);
                self:addChild(EtherTheme.makeLabel(bx, cy, 46, label));
                cy = cy + 46;
            end

            for hi = 1, #boostHints do
                table.insert(self.hintRows, { text = boostHints[hi], x = bx,
                    y = cy + 4 + (hi - 1) * EtherTheme.fontHgtHint });
            end
        end);
end

--*********************************************************
--* 提示行绘制 (renderContent 钩子: 裁剪区内, 坐标随滚动)
--*********************************************************
function EtherCharacterBoostPanel:renderContent()
    if self.hintRows == nil then return; end
    for i = 1, #self.hintRows do
        local h = self.hintRows[i];
        EtherTheme.drawHintText(self, h.text, h.x, h.y, EtherTheme.textDim, 0.9);
    end
end

