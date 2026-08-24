require "ISUI/ISPanel"

--*********************************************************
--* 玩家编辑面板 (已迁移到 EtherFormPanel: 游标布局 + 统一 add* API)
--*
--* 迁移前的问题 (UI重构方案.md P1/P2/P4 + 用户实测缺陷):
--*   - 纯绝对坐标网格 (180,20) (180,60) ... 与魔法式 310+360+20;
--*   - 右侧信息列固定放在 self.width - 140, 面板收窄到 888 后文字被右缘截断
--*     ("管理等级: nor…"), 且与 x=500 的 Edit 按钮重叠;
--*   - 特质/技能表的列头画在表体之上(PZ 行为), 会压住上方的人物信息行;
--*   - 信息文案全靠 getText(k) .. ": " .. value 拼接。
--* 迁移后: 头像 + 信息 + 两张表各占一个自定义行, 位置由基类游标接管;
--*   信息列按实测宽度排布并右对齐按钮, 任何语言都不越界。
--* 信息文字走 Info 页同款"整串缩放绘制" (drawHintText, 约小3号);
--*   姓/名行已删 (与用户名重复)。Edit 按钮仍为子控件, 行位不变。
--*
--* 红线 (零功能影响): getHoursAlive/setHoursAlive/getZombieKills/setZombieKills、
--*   UITraitsTable/UISkillTable、ISUI3DModel 的用法与参数一律未改。
--*********************************************************
EtherPlayerEditor = EtherFormPanel:derive("EtherPlayerEditor");

--*********************************************************
--* 头像衬底 (纹理 + 描边): 做成子控件, 位置随滚动由 UI 系统处理,
--* 避免在 prerender 手绘时还要自己叠加 getYScroll()。
--*********************************************************
local AvatarBackdrop = ISPanel:derive("EtherAvatarBackdrop");

function AvatarBackdrop:render()
    if self.tex ~= nil then
        self:drawTextureScaled(self.tex, 0, 0, self.width, self.height, 1, 1, 1, 1);
    end
    local b = EtherTheme.blood;
    self:drawRectBorder(0, 0, self.width, self.height, 0.5, b.r, b.g, b.b);
end

function AvatarBackdrop:onMouseDown(x, y) return false; end
function AvatarBackdrop:onMouseUp(x, y) return false; end

function AvatarBackdrop:new(x, y, w, h, tex)
    local o = ISPanel:new(x, y, w, h);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.tex = tex;
    return o;
end

--*********************************************************
--* 安全取职业名 (B42: CharacterProfessionDefinition; 逐级回退)
--*********************************************************
local function professionName(descriptor)
    local name = "Unknown";
    local ok, err = pcall(function()
        local professionObj = nil;
        if descriptor.getCharacterProfession then
            professionObj = descriptor:getCharacterProfession();
        end
        if professionObj ~= nil then
            if CharacterProfessionDefinition and CharacterProfessionDefinition.getCharacterProfessionDefinition then
                local profDef = CharacterProfessionDefinition.getCharacterProfessionDefinition(professionObj);
                if profDef and profDef.getLabel then
                    name = tostring(profDef:getLabel() or "Unknown");
                end
            end
            if name == "Unknown" and professionObj.getName then
                name = tostring(professionObj:getName() or "Unknown");
            end
            if name == "Unknown" then
                name = tostring(professionObj);
            end
        end
    end)
    if not ok then
        print("[EtherHack] Failed to get profession: " .. tostring(err));
    end
    return name;
end

--*********************************************************
--* 人物信息行数据 (标签 + 值), 集中构建便于统一排布与折行
--*********************************************************
function EtherPlayerEditor:infoRows()
    local p = self.localPlayer;
    local descriptor = p:getDescriptor();

    local timeSurvived = "N/A";
    if p.getTimeSurvived then
        timeSurvived = tostring(p:getTimeSurvived() or "N/A");
    end
    local zombieKills = "0";
    if p.getZombieKills then
        zombieKills = tostring(p:getZombieKills() or 0);
    end

    local chatMuted = getText("Sandbox_ThumpNoChasing_option1") or "Yes";
    if p.isAllChatMuted and not p:isAllChatMuted() then
        chatMuted = getText("Sandbox_ThumpNoChasing_option2") or "No";
    end

    local weight, calories = "N/A", "N/A";
    local nutrition = p:getNutrition();
    if nutrition then
        if nutrition.getWeight then weight = tostring(math.floor(nutrition:getWeight() or 0)); end
        if nutrition.getCalories then calories = tostring(math.floor(nutrition:getCalories() or 0)); end
    end

    return {
        { label = getText("IGUI_PlayerStats_Username"), value = tostring(p:getUsername() or "") },
        { label = getText("IGUI_PlayerStats_DisplayName"), value = tostring(p:getDisplayName() or "") },
        { label = getText("IGUI_PlayerStats_Profession"), value = professionName(descriptor) },
        { label = getText("IGUI_PlayerStats_AccessLevel"), value = tostring(p:getAccessLevel() or "") },
        { label = getText("IGUI_PlayerStats_ChatMuted"), value = chatMuted },
        { label = getText("IGUI_char_Weight"), value = weight },
        { label = getTranslate("UI_PlayerEditor_PlayerInfo_Calories") or "Calories", value = calories },
        { label = getText("IGUI_char_Survived_For"), value = timeSurvived, edit = "time" },
        { label = getText("IGUI_char_Zombies_Killed"), value = zombieKills, edit = "kills" },
    };
end

--*********************************************************
--* 面板内容构建 (基类在 createChildren 里回调 build)
--*********************************************************
function EtherPlayerEditor:build()
    self.localPlayer = getPlayer();
    if self.localPlayer == nil then return end
    if self.localPlayer:getDescriptor() == nil then return end

    local tm = getTextManager();
    local ctrlH = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    -- 行距必须容纳 Edit 按钮 (ctrlH), 否则按钮会比行高还高、上下溢出相邻行
    local rowStep = math.max(fhS + 8, ctrlH + 2);
    local AV_W, AV_H = 128, 260;

    -- ================= 头像 + 信息 =================
    local rows = self:infoRows();
    local infoH = math.max(AV_H, #rows * rowStep);

    self:addCustomRow(infoH, function(bx, by, bw)
        -- 头像衬底 + 3D 模型 (衬底先加 -> 在模型下层)
        local backdrop = AvatarBackdrop:new(bx, by, AV_W, AV_H, self.avatarBackgroundTexture);
        backdrop:initialise();
        backdrop:instantiate();
        self:_anchor(backdrop);
        self:addChild(backdrop);

        local avatarOk, avatarErr = pcall(function()
            self.avatarPanel = ISUI3DModel:new(bx, by, AV_W, AV_H)
            self.avatarPanel:setVisible(true)
            -- 必须设置角色, 否则 modelInstance 为 null, UI3DModel 每帧 NPE
            self.avatarPanel:setCharacter(self.localPlayer)
            self.avatarPanel:setState("idle")
            self.avatarPanel:setDirection(IsoDirections.S)
            self.avatarPanel:setIsometric(false)
            self:addChild(self.avatarPanel)
        end)
        if not avatarOk then
            print("[EtherHack] Avatar panel creation failed: " .. tostring(avatarErr));
        end

        -- 信息列: 头像右侧单列纵向排布。
        -- 单列 + 值右对齐, 从根上避免"右侧固定列被面板右缘截断"与"和 Edit 按钮重叠"。
        local ix = bx + AV_W + 16;
        local iw = (bx + bw) - ix;
        local editTitle = getTranslate("UI_PlayerEditor_EditStats") or "Edit";
        local editW = tm:MeasureStringX(UIFont.Small, editTitle) + EtherTheme.ctrlPadX * 2;

        -- 信息文字改走 Info 页同款"整串缩放绘制": 标签/值不再建 ISLabel 子控件,
        -- 布局参数存 self.infoDraw, 由 render 逐帧 drawHintText (约小3号)。
        -- 行位/rowStep 不变, 与 Edit 按钮共用同一条中线。
        self.infoDraw = {
            rows = rows,
            x = ix, right = bx + bw, y0 = by,
            iw = iw, rowStep = rowStep, editW = editW,
        };

        for i = 1, #rows do
            local r = rows[i];
            local y = by + (i - 1) * rowStep;

            -- Edit 按钮: 与本行竖直居中, 右对齐到信息列右缘
            if r.edit ~= nil then
                local cb = (r.edit == "time")
                    and function() self:onEditTimeButton(); end
                    or function() self:onEditKillsButton(); end
                -- 按钮在本行内竖直居中 (行高 rowStep, 按钮高 ctrlH)
                local btn = UIButton:new((bx + bw) - editW,
                    y + math.floor((rowStep - ctrlH) / 2), editW, ctrlH, editTitle, cb);
                self:addWidget(btn);
            end
        end
    end);

    -- ================= 特质 =================
    -- 两张表各自占一个自定义行; 表的列头画在表体之上(PZ 行为), 故预留 LIST_HEADER_H
    local hdrH = EtherFormPanel.LIST_HEADER_H;
    local tableH = 220;

    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:addModule("UI_PlayerEditor_PlayerTraits_Title", tableH + hdrH, function(bx, by, bw)
        local ok, err = pcall(function()
            self.traitsPanel = UITraitsTable:new(bx, by + hdrH, bw, tableH);
            self.traitsPanel:initialise();
            self.traitsPanel.parent = self;
            self:_anchor(self.traitsPanel);
            self:addChild(self.traitsPanel);
        end)
        if not ok then
            print("[EtherHack] Failed to create traits panel: " .. tostring(err))
        end
    end);

    -- ================= 技能 =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:addModule("UI_PlayerEditor_PlayerSkills_Title", tableH + hdrH, function(bx, by, bw)
        local ok, err = pcall(function()
            self.skillPanel = UISkillTable:new(bx, by + hdrH, bw, tableH);
            self.skillPanel:initialise();
            self.skillPanel.parent = self;
            self:_anchor(self.skillPanel);
            self:addChild(self.skillPanel);
        end)
        if not ok then
            print("[EtherHack] Failed to create skills panel: " .. tostring(err))
        end
    end);

    self.childrenCreated = true;
end

--*********************************************************
--* 重建面板内容 (编辑存活时长/击杀数后刷新显示)
--*********************************************************
function EtherPlayerEditor:updateLabels()
    -- 先快照再移除: 直接在遍历 Java 子元素列表时移除元素并不安全
    local kids = {};
    for _, child in pairs(self:getChildren()) do
        table.insert(kids, child);
    end
    for i = 1, #kids do
        self:removeChild(kids[i]);
    end

    self.avatarPanel = nil;
    self.traitsPanel = nil;
    self.skillPanel = nil;
    self.childrenCreated = false;
    self:createChildren();
end

function EtherPlayerEditor:onEditTimeButton()
    local modal = ISTextBox:new(0, 0, 560, 360, getTranslate("UI_PlayerEditor_EditHoursTitle") or "Edit Hours",
        tostring(getHoursAlive and getHoursAlive() or 0),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value and setHoursAlive then
                    setHoursAlive(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

function EtherPlayerEditor:onEditKillsButton()
    local modal = ISTextBox:new(0, 0, 560, 360, getTranslate("UI_PlayerEditor_EditKillsTitle") or "Edit Kills",
        tostring(getZombieKills and getZombieKills() or 0),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value and setZombieKills then
                    setZombieKills(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

--*********************************************************
--* 手绘内容 (基类 renderContent 钩子): 在裁剪区内逐帧绘制信息行,
--* 滚出面板的像素由裁剪区剪掉 —— 此前画在 clearStencilRect 之后,
--* 滚动时文字会越出面板画到其它 UI 上 (实机缺陷, 已修)。
--* 标签白/值暗色, 值右对齐并让出 Edit 按钮位, 过长折行取第一行;
--* 测宽/折行/行距全部用 hint 缩放套件 (hintWidth/wrapHint/fontHgtHint),
--* 与 drawHintText 同一套缩放 (见 EtherTheme 注释)。
--*********************************************************
function EtherPlayerEditor:renderContent()
    local info = self.infoDraw;
    if info == nil then return end

    local dy = math.floor((info.rowStep - EtherTheme.fontHgtHint) / 2);
    for i = 1, #info.rows do
        local r = info.rows[i];
        local y = info.y0 + (i - 1) * info.rowStep + dy;
        EtherTheme.drawHintText(self, r.label, info.x, y, EtherTheme.text);

        -- 该行右侧预留: 有 Edit 按钮的行要让出按钮宽度
        local rightPad = 0;
        if r.edit ~= nil then rightPad = info.editW + EtherTheme.ctrlGap; end

        local availW = info.iw - EtherTheme.hintWidth(r.label) - EtherTheme.ctrlGap - rightPad;
        if availW < 40 then availW = 40; end
        local valueText = r.value;
        if EtherTheme.hintWidth(valueText) > availW then
            valueText = EtherTheme.wrapHint(valueText, availW)[1];
        end
        local vx = info.right - rightPad - EtherTheme.hintWidth(valueText);
        EtherTheme.drawHintText(self, valueText, vx, y, EtherTheme.textDim);
    end
end

function EtherPlayerEditor:render()
    EtherFormPanel.render(self);

    if getPlayer() == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2,
            1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end
end

--*********************************************************
--* update: 进入游戏后补建 UI; 并保持头像跟随当前角色
--*********************************************************
function EtherPlayerEditor:update()
    ISPanel.update(self);

    if not self.childrenCreated then
        local p = getPlayer();
        if p ~= nil then
            self.localPlayer = p;
            self:createChildren();
        end
        return;
    end

    if self.avatarPanel ~= nil and self.localPlayer ~= nil then
        self.avatarPanel:setCharacter(self.localPlayer)
    end
end

--*********************************************************
--* 构造 (其余继承 EtherFormPanel)
--*********************************************************
function EtherPlayerEditor:new(posX, posY, width, height)
    local o = EtherFormPanel.new(self, posX, posY, width, height);
    o.avatarBackgroundTexture = getTexture("media/ui/avatarBackground.png");
    o.workInGameText = getTranslate("UI_PlayerEditor_PanelWorkOnlyInGame") or "This panel only works in-game";
    o.localPlayer = getPlayer();
    o.childrenCreated = false;
    EtherPlayerEditor.instance = o;      -- 原实现误把类表赋给 instance
    return o;
end
