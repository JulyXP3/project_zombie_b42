require "ISUI/ISPanel"

--*********************************************************
-- Server-Side Bypass System for Skill Changes
--*********************************************************

-- Full bypass sync - combines client and server bypass
local function bypassSync(player, perkName, targetLevel)
    if not player then return; end
    
    -- Single player - just sync normally
    if isSinglePlayer and isSinglePlayer() then
        if SyncXp then SyncXp(player); end
        return;
    end
    
    -- Multiplayer - use full server bypass
    if safeSkillBoost and perkName and targetLevel then
        -- Use the Java server bypass method
        safeSkillBoost(player, perkName, targetLevel);
    else
        -- Fallback to basic bypass
        if disableAntiCheatLocally then
            disableAntiCheatLocally("all");
        end
        if bypassServerValidation then
            bypassServerValidation();
        end
        if spoofSyncTimestamp then
            spoofSyncTimestamp(player);
        end
        if SyncXp then SyncXp(player); end
    end
end

-- Simple safe sync for basic operations
local function safeSync(player)
    if not player then return; end
    
    -- Single player - always safe
    if isSinglePlayer and isSinglePlayer() then
        if SyncXp then SyncXp(player); end
        return;
    end
    
    -- Multiplayer - full bypass
    if disableAntiCheatLocally then
        disableAntiCheatLocally("all");
    end
    if bypassServerValidation then
        bypassServerValidation();
    end
    if spoofSyncTimestamp then
        spoofSyncTimestamp(player);
    end
    if SyncXp then SyncXp(player); end
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UISkillTable = ISPanel:derive("UISkillTable");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* 按钮右侧说明文字 (与 EtherVehiclePanel/EtherCharacterPanel 的 ModuleHint 同款):
--* 做成子控件以随表格一同定位/滚动, 不吞鼠标事件 (点击要能穿到按钮)。
--* 折行在构造时算一次; 鼠标handler就地定义, 不引用 EtherFormPanel.UIRowBox,
--* 以免依赖两个文件的加载先后顺序。
--*********************************************************
local ButtonHint = ISPanel:derive("EtherSkillButtonHint");

function ButtonHint:render()
    for i = 1, #self.lines do
        EtherTheme.drawHintText(self, self.lines[i], 0, (i - 1) * EtherTheme.fontHgtHint,
            EtherTheme.textDim, 0.9);
    end
end

function ButtonHint:onMouseDown(x, y) return false; end
function ButtonHint:onMouseUp(x, y) return false; end
function ButtonHint:onMouseMove(dx, dy) return false; end

function ButtonHint:new(x, y, w, text)
    local lines = EtherTheme.wrapHint(text, w);
    local o = ISPanel:new(x, y, w, #lines * EtherTheme.fontHgtHint + 2);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.lines = lines;
    return o;
end

--*********************************************************
--* 在指定按钮右侧摆一条说明文字, 纵向对齐到按钮行中线。
--* 余宽不足 (窄窗口/按钮换行占满整排) 时直接不放, 避免逐字折行糊成一团。
--*********************************************************
local MIN_HINT_W = 60;

local function placeButtonHint(panel, btnX, btnY, btnW, availRight, key)
    local hx = btnX + btnW + EtherTheme.ctrlGap;
    local hw = availRight - hx;
    if hw < MIN_HINT_W then return nil; end
    local hint = ButtonHint:new(hx, btnY, hw, getTranslate(key));
    hint:setY(btnY + math.floor((EtherTheme.ctrlH - hint.height) / 2));
    hint:initialise();
    hint:instantiate();
    hint:setAnchorLeft(true);
    hint:setAnchorRight(false);
    hint:setAnchorTop(false);
    hint:setAnchorBottom(true);
    panel:addChild(hint);
    return hint;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UISkillTable:createChildren()
    ISPanel.createChildren(self);

    -- 按钮行贴表格底部并留内边距 (原实现 y=height-80 且 x=0: 按钮下方空 56px
    -- 显得悬在中间, x=0 还压住表格左边框)。四个按钮等宽, 排不下自动换行。
    local PAD = 8;
    local ctrlH = EtherTheme.ctrlH;
    local GAP = EtherTheme.ctrlGap;
    local tm = getTextManager();
    local titles = {
        getTranslate("UI_PlayerEditor_PlayerSkills_AddXP"),
        getTranslate("UI_PlayerEditor_PlayerSkills_AddLevel"),
        getTranslate("UI_PlayerEditor_PlayerSkills_TakeLevel"),
        getTranslate("UI_PlayerEditor_PlayerSkills_MaxAllSkills"),
    };
    local btnW = UIButton.measureGroupWidth(titles);
    -- 单个按钮不得超过整行可用宽度, 否则会越出表格右缘
    local maxBtnW = self.width - PAD * 2;
    if btnW > maxBtnW then btnW = maxBtnW; end

    local perRow = math.floor((self.width - PAD * 2 + GAP) / (btnW + GAP));
    if perRow < 1 then perRow = 1; end
    local btnRows = math.ceil(#titles / perRow);
    local btnBlockH = btnRows * ctrlH + (btnRows - 1) * GAP;
    local btnY = self.height - btnBlockH - PAD;
    local listH = btnY - GAP;
    if listH < 60 then listH = 60; end

    local function btnPos(i)
        local r = math.floor((i - 1) / perRow);
        local c = (i - 1) - r * perRow;
        return PAD + c * (btnW + GAP), btnY + r * (ctrlH + GAP);
    end

    self.datas = ISScrollingListBox:new(0, 0, self.width, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.backgroundColor = {r=0, g=0, b=0, a=0.0};
    EtherTheme.styleList(self.datas);
    self.datas.drawBorder = false;      -- 外层行盒已提供边框
    -- 列宽按表宽比例分配, 避免固定像素在窄面板下挤在一起
    self.datas:addColumn(getText("IGUI_PlayerStats_Perk"), 0);
    self.datas:addColumn(getText("IGUI_PlayerStats_Level"), math.floor(self.width * 0.34));
    self.datas:addColumn(getText("IGUI_PlayerStats_XP"), math.floor(self.width * 0.52));
    self.datas:addColumn(getText("IGUI_PlayerStats_Boost"), math.floor(self.width * 0.74))
    -- 本表嵌在可滚动的"玩家"页里: 自身滚到边界时把滚轮交还外层, 否则外层滚不动
    EtherTheme.bubbleWheelAtEdge(self.datas);
    self:addChild(self.datas);

    local bx, by = btnPos(1);
    self.addXP = UIButton:new(bx, by, btnW, ctrlH, titles[1],
    function() 
        if UIModalAddXP.instance then
            UIModalAddXP.instance:close()
        end
        local modal = UIModalAddXP:new()
        modal:initialise();
        modal:addToUIManager();
        modal:setAlwaysOnTop(true);
    end, btnW)
    self.addXP:initialise();
    self.addXP:instantiate();
    self.addXP:setAnchorLeft(true);
    self.addXP:setAnchorRight(false);
    self.addXP:setAnchorTop(false);
    self.addXP:setAnchorBottom(true);
    self:addChild(self.addXP);

    bx, by = btnPos(2);
    self.addLevel = UIButton:new(bx, by, btnW, ctrlH, titles[2],
    function() 
        local selectedItem = self.datas.items[self.datas.selected].item
        self.localPlayer:LevelPerk(selectedItem.perk);
        self.localPlayer:getXp():setXPToLevel(selectedItem.perk, self.localPlayer:getPerkLevel(selectedItem.perk));
        safeSync(self.localPlayer)
        self:updateSkills();
        if selectedItem.perk == Perks.Strength or selectedItem.perk == Perks.Fitness then
            self.parent.traitsPanel:updateTraits();
        end
    end, btnW)
    self.addLevel:initialise();
    self.addLevel:instantiate();
    self.addLevel:setAnchorLeft(true);
    self.addLevel:setAnchorRight(false);
    self.addLevel:setAnchorTop(false);
    self.addLevel:setAnchorBottom(true);
    self.addLevel.isOnlyInGame = true;
    self.addLevel.isRequireSelected = true;
    self:addChild(self.addLevel);
    table.insert(self.buttonList, self.addLevel);

    bx, by = btnPos(3);
    self.takeLevel = UIButton:new(bx, by, btnW, ctrlH, titles[3], 
    function() 
        local selectedItem = self.datas.items[self.datas.selected].item
        self.localPlayer:LoseLevel(selectedItem.perk);
        self.localPlayer:getXp():setXPToLevel(selectedItem.perk, self.localPlayer:getPerkLevel(selectedItem.perk));
        safeSync(self.localPlayer)
        self:updateSkills();
        if selectedItem.perk == Perks.Strength or selectedItem.perk == Perks.Fitness then
            self.parent.traitsPanel:updateTraits();
        end
    end, btnW)
    self.takeLevel:initialise();
    self.takeLevel:instantiate();
    self.takeLevel:setAnchorLeft(true);
    self.takeLevel:setAnchorRight(false);
    self.takeLevel:setAnchorTop(false);
    self.takeLevel:setAnchorBottom(true);
    self.takeLevel.isOnlyInGame = true;
    self.takeLevel.isRequireSelected = true;
    self:addChild(self.takeLevel);
    table.insert(self.buttonList, self.takeLevel);
    
    bx, by = btnPos(4);
    self.maxSkill = UIButton:new(bx, by, btnW, ctrlH, titles[4], 
    function() 
         for i=0, Perks.getMaxIndex() - 1 do
            local perk = PerkFactory.getPerk(Perks.fromIndex(i));
            if perk and perk:getParent() ~= Perks.None then
                for i=1, 10 do
                    self.localPlayer:LevelPerk(perk, false);
                    self.localPlayer:getXp():setXPToLevel(perk, self.localPlayer:getPerkLevel(perk));
                end
            end
        end
        -- Sync once at the end instead of every level
        safeSync(self.localPlayer);
        self.parent.traitsPanel:updateTraits();
        self:updateSkills();
    end, btnW)
    self.maxSkill:initialise();
    self.maxSkill:instantiate();
    self.maxSkill:setAnchorLeft(true);
    self.maxSkill:setAnchorRight(false);
    self.maxSkill:setAnchorTop(false);
    self.maxSkill:setAnchorBottom(true);
    self:addChild(self.maxSkill);

    -- 「所有技能升满」右侧提示: 多人下升技能会被服务端回滚, 需先开「其他-服务器同步保护」。
    -- 取实际 getX/getWidth 而非 btnW: UIButton 会按文案自动加宽 (见 UIButton:new);
    -- 按钮若换行到下一排, by 也随之取自 btnPos(4), 提示跟着走。
    self.syncHint = placeButtonHint(self, self.maxSkill:getX(), by,
        self.maxSkill:getWidth(), self.width - PAD, "UI_PlayerEditor_SyncProtectionHint");

    self:updateSkills();
end

--*********************************************************
--* Инициализация черт характера
--*********************************************************
function UISkillTable:updateSkills()
    self.lastSelectedIndex = self.datas.selected or 0;
    self.datas:clear();

    for i=0, Perks.getMaxIndex() - 1 do
        local perk = PerkFactory.getPerk(Perks.fromIndex(i));
            if perk ~= nil then
                if perk and perk:getParent() ~= Perks.None then
                local newPerk = {};
                newPerk.perk = Perks.fromIndex(i);
                newPerk.name = perk:getName() .. " (" .. PerkFactory.getPerkName(perk:getParent()) .. ")";
                newPerk.level = self.localPlayer:getPerkLevel(Perks.fromIndex(i));
                newPerk.xpToLevel = perk:getXpForLevel(newPerk.level + 1);
                newPerk.xp = self.localPlayer:getXp():getXP(newPerk.perk) - ISSkillProgressBar.getPreviousXpLvl(perk, newPerk.level);
                newPerk.xp = round(newPerk.xp,2)
                local xpBoost = self.localPlayer:getXp():getPerkBoost(newPerk.perk);
                if xpBoost == 1 then
                    newPerk.boost = "75%";
                elseif xpBoost == 2 then
                    newPerk.boost = "100%";
                elseif xpBoost == 3 then
                    newPerk.boost = "125%";
                else
                    newPerk.boost = "50%";
                end
                newPerk.multiplier = self.localPlayer:getXp():getMultiplier(newPerk.perk);
                self.datas:addItem(newPerk.name, newPerk);
            end
        end
    end
    self.datas.selected = self.lastSelectedIndex;
end

--*********************************************************
--* Обновление таблицы
--*********************************************************
function UISkillTable:update()
    self.datas.doDrawItem = self.drawDatas;
    for i=1, #self.buttonList do
        local item = self.buttonList[i];
        if item.isOnlyInGame and self.localPlayer == nil or self.localPlayer:isDead() then
            item:setEnable(false);
        end
        if (not self.datas.items[self.datas.selected] or #self.datas.items < 1) and item.isRequireSelected then
            item:setEnable(false);
        else
            item:setEnable(true);
        end
    end
end

--*********************************************************
--* Отрисовка данных
--*********************************************************
function UISkillTable:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)

    local yoff = 2;
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    self:suspendStencil()
    self:clampStencilRectToParent(clipX, clipY, clipX2 - clipX, clipY2 - clipY)

    self:drawText(item.item.name, 25, y + yoff, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1, UIFont.Small);

    self:clearStencilRect()
    self:resumeStencil()

    self:drawTextCentre(tostring(item.item.level), (self.columns[2].size + self.columns[3].size) / 2, y + yoff, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1, UIFont.Small)

    if item.item.xpToLevel == -1 then
        self:drawTextCentre(getTranslate("UI_Skill_MaxLevel"), (self.columns[3].size + self.columns[4].size) / 2, y + yoff, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1, UIFont.Small);
    else
        self:drawTextCentre(tostring(item.item.xp) .. "/" .. tostring(item.item.xpToLevel), (self.columns[3].size + self.columns[4].size) / 2, y + yoff, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1, UIFont.Small);
    end

    self:drawTextCentre(tostring(item.item.boost), (self.columns[4].size + self:getWidth()) / 2, y + yoff, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b, 1, UIFont.Small);

    return y + self.itemheight;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UISkillTable:new (x, y, width, height)
    local menuTableData = ISPanel:new(x, y, width, height);
    setmetatable(menuTableData, self);
    menuTableData.borderColor = EtherTheme.bloodDim;
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.localPlayer = getPlayer();
    menuTableData.lastSelectedIndex = 0;
    menuTableData.buttonList = {};
    UISkillTable.instance = menuTableData;
    return menuTableData;
end