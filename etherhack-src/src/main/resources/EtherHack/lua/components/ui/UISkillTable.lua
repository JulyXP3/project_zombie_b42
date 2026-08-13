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
--* Создание дочерних элементов
--*********************************************************
function UISkillTable:createChildren()
    ISPanel.createChildren(self);

    self.datas = ISScrollingListBox:new(0, 0, self.width, self.height - 90);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = fontHeightSmall + 4 * 2
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    self.datas.drawBorder = true;
    self.datas.backgroundColor = {r=0, g=0, b=0, a=0.0};
    EtherTheme.styleList(self.datas);
    self.datas:addColumn(getText("IGUI_PlayerStats_Perk"), 0);
    self.datas:addColumn(getText("IGUI_PlayerStats_Level"), 140);
    self.datas:addColumn(getText("IGUI_PlayerStats_XP"), 230);
    self.datas:addColumn(getText("IGUI_PlayerStats_Boost"),370)
    self:addChild(self.datas);

    self.addXP = UIButton:new(0, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerSkills_AddXP"), 
    function() 
        if UIModalAddXP.instance then
            UIModalAddXP.instance:close()
        end
        local modal = UIModalAddXP:new()
        modal:initialise();
        modal:addToUIManager();
        modal:setAlwaysOnTop(true);
    end)
    self.addXP:initialise();
    self.addXP:instantiate();
    self.addXP:setAnchorLeft(true);
    self.addXP:setAnchorRight(false);
    self.addXP:setAnchorTop(false);
    self.addXP:setAnchorBottom(true);
    self:addChild(self.addXP);

    self.addLevel = UIButton:new(self.addXP.x + self.addXP.width + 10, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerSkills_AddLevel"), 
    function() 
        local selectedItem = self.datas.items[self.datas.selected].item
        self.localPlayer:LevelPerk(selectedItem.perk);
        self.localPlayer:getXp():setXPToLevel(selectedItem.perk, self.localPlayer:getPerkLevel(selectedItem.perk));
        safeSync(self.localPlayer)
        self:updateSkills();
        if selectedItem.perk == Perks.Strength or selectedItem.perk == Perks.Fitness then
            self.parent.traitsPanel:updateTraits();
        end
    end)
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

    self.takeLevel = UIButton:new(self.addLevel.x + self.addLevel.width + 10, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerSkills_TakeLevel"), 
    function() 
        local selectedItem = self.datas.items[self.datas.selected].item
        self.localPlayer:LoseLevel(selectedItem.perk);
        self.localPlayer:getXp():setXPToLevel(selectedItem.perk, self.localPlayer:getPerkLevel(selectedItem.perk));
        safeSync(self.localPlayer)
        self:updateSkills();
        if selectedItem.perk == Perks.Strength or selectedItem.perk == Perks.Fitness then
            self.parent.traitsPanel:updateTraits();
        end
    end)
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
    
    self.maxSkill = UIButton:new(self.takeLevel.x + self.takeLevel.width + 10, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerSkills_MaxAllSkills"), 
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
    end)
    self.maxSkill:initialise();
    self.maxSkill:instantiate();
    self.maxSkill:setAnchorLeft(true);
    self.maxSkill:setAnchorRight(false);
    self.maxSkill:setAnchorTop(false);
    self.maxSkill:setAnchorBottom(true);
    self:addChild(self.maxSkill);

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
        self:drawTextCentre("MAX", (self.columns[3].size + self.columns[4].size) / 2, y + yoff, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1, UIFont.Small);
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