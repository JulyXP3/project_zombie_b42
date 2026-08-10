require "ISUI/ISPanel"

--*********************************************************
-- Server-Side Bypass System for Trait Changes
--*********************************************************

-- Full bypass sync for trait operations
local function safeSync(player)
    if not player then return; end
    
    -- Single player - always safe
    if isSinglePlayer and isSinglePlayer() then
        if SyncXp then SyncXp(player); end
        return;
    end
    
    -- Multiplayer - full server bypass
    if prepareBypass then prepareBypass(player); end
    if SyncXp then SyncXp(player); end
end

-- Safe trait removal with server bypass (local function to avoid recursion)
local function safeRemoveTraitLocal(player, traitObj)
    if not player or not traitObj then return; end
    
    -- Prepare bypass
    if prepareBypass then prepareBypass(player); end
    
    -- Remove trait directly
    local playerTraits = player.getCharacterTraits and player:getCharacterTraits() or player:getTraits();
    if playerTraits then playerTraits:remove(traitObj); end
    
    -- Sync
    safeSync(player);
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UITraitsTable = ISPanel:derive("UITraitsTable");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UITraitsTable:createChildren()
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
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_NameTitle"), 0);
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_Description"), 150)
    self:addChild(self.datas);


    self.addTrait = UIButton:new(0, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerTraits_AddTrait"), 
    function() 
        if UIModalAddTrait.instance then
            UIModalAddTrait.instance:close()
        end
        local modal = UIModalAddTrait:new()
        modal:initialise();
        modal:addToUIManager();
        modal:setAlwaysOnTop(true);
    end)
    self.addTrait:initialise();
    self.addTrait:instantiate();
    self.addTrait:setAnchorLeft(true);
    self.addTrait:setAnchorRight(false);
    self.addTrait:setAnchorTop(false);
    self.addTrait:setAnchorBottom(true);
    self.addTrait.isOnlyInGame = true;
    self.addTrait.isRequireSelected = false;
    self:addChild(self.addTrait);
    table.insert(self.buttonList, self.addTrait);

    self.deleteTrait = UIButton:new(self.addTrait.x + self.addTrait.width + 10, self.height - 80, 100, 24, getTranslate("UI_PlayerEditor_PlayerTraits_DeleteTrait"), 
    function() 
        local selectedItem = self.datas.items[self.datas.selected];
        if selectedItem and selectedItem.item then
            -- New structure: item = {traitObj = CharacterTrait, traitDef = CharacterTraitDefinition}
            local traitObj = selectedItem.item.traitObj;
            if traitObj then
                -- Use safe removal with bypass
                safeRemoveTraitLocal(self.localPlayer, traitObj);
                self:updateTraits();
            end
        end
    end)
    self.deleteTrait:initialise();
    self.deleteTrait:instantiate();
    self.deleteTrait:setAnchorLeft(true);
    self.deleteTrait:setAnchorRight(false);
    self.deleteTrait:setAnchorTop(false);
    self.deleteTrait:setAnchorBottom(true);
    self.deleteTrait.isOnlyInGame = true;
    self.deleteTrait.isRequireSelected = true;
    self:addChild(self.deleteTrait);
    table.insert(self.buttonList, self.deleteTrait);

    self:updateTraits();
end

--*********************************************************
--* Инициализация черт характера
--*********************************************************
function UITraitsTable:updateTraits()
    self.lastSelectedIndex = self.datas.selected or 0;
    self.datas:clear();

    if not self.localPlayer then return; end
    
    -- Build 42: Use getCharacterTraits() instead of getTraits()
    local playerTraits = nil;
    if self.localPlayer.getCharacterTraits then
        playerTraits = self.localPlayer:getCharacterTraits();
    elseif self.localPlayer.getTraits then
        playerTraits = self.localPlayer:getTraits();
    end
    if not playerTraits then return; end
    
    -- Build 42: getKnownTraits() returns List<CharacterTrait> (enum objects)
    local traitsList = nil;
    local ok, err = pcall(function()
        if playerTraits.getKnownTraits then
            traitsList = playerTraits:getKnownTraits();
        end
    end)
    if not ok then
        print("[EtherHack] Error getting traits list: " .. tostring(err));
        return;
    end
    
    if traitsList and traitsList.size then
        local count = traitsList:size();
        for i=0, count - 1 do
            local traitObj = traitsList:get(i);  -- CharacterTrait enum object
            if traitObj then
                -- Build 42: Use CharacterTraitDefinition with CharacterTrait object
                local traitDef = nil;
                local defOk, defErr = pcall(function()
                    if CharacterTraitDefinition and CharacterTraitDefinition.getCharacterTraitDefinition then
                        traitDef = CharacterTraitDefinition.getCharacterTraitDefinition(traitObj);
                    end
                end)
                
                if traitDef ~= nil then
                    -- Store both the CharacterTrait object and the definition
                    local label = "Unknown";
                    if traitDef.getLabel then label = traitDef:getLabel() or "Unknown"; end
                    self.datas:addItem(label, {traitObj = traitObj, traitDef = traitDef});
                end
            end
        end
    end
    self.datas.selected = self.lastSelectedIndex;
end

--*********************************************************
--* Обновление таблицы
--*********************************************************
function UITraitsTable:update()
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
function UITraitsTable:drawDatas(y, item, alt)
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)
    
    local scrollBarOffset = 14
    if self:getScrollHeight() < 70 then
        scrollBarOffset = 0
    end
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    self:suspendStencil()
    self:clampStencilRectToParent(0, clipY, self:getWidth() - scrollBarOffset, clipY2 - clipY)
    
    if self.selected == item.index then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b);
    end

    if alt then
        self:drawRect(0, y, self:getWidth(), self.itemheight, 0.3, 0.3, 0.3, 0.3);
    end
    self:drawRectBorder(0, y, self:getWidth(), self.itemheight, 0.5, self.borderColor.r, self.borderColor.g, self.borderColor.b);
    self:drawRectBorder(self.columns[1].size, y, self.columns[2].size, self.itemheight, 0.5, self.borderColor.r, self.borderColor.g, self.borderColor.b);
    
    self:clearStencilRect()
    self:resumeStencil()
  
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    -- Get the trait definition from the new data structure
    local traitDef = item.item.traitDef or item.item;
    if not traitDef then return y + self.itemheight; end

    -- Устанавливаем маску для первого столбца
    self:suspendStencil()
    self:clampStencilRectToParent(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    local label = traitDef.getLabel and traitDef:getLabel() or "Unknown";
    self:drawText(label, 25, y + 4, 1, 1, 1, 1, UIFont.Small);
    -- Удаляем маску
    self:clearStencilRect()
    self:resumeStencil()

    local description = "";
    if traitDef.getDescription then
        description = (traitDef:getDescription() or ""):gsub("\n", "; ");
    end
    
    -- Устанавливаем маску для второго столбца
    self:suspendStencil()
    self:clampStencilRectToParent(self.columns[2].size, clipY, self.width - self.columns[2].size - scrollBarOffset, clipY2 - clipY)
    self:drawText(description, self.columns[2].size + 10, y + 4, 1, 1, 1, 1, UIFont.Small);
    -- Удаляем маску
    self:clearStencilRect()
    self:resumeStencil()

    self:repaintStencilRect(0, clipY, self.width - scrollBarOffset, clipY2 - clipY)

    local iconX = 4
    local iconSize = fontHeightSmall;

    local texture = traitDef.getTexture and traitDef:getTexture() or nil;
    if texture then
        self:suspendStencil()
        self:clampStencilRectToParent(self.columns[1].size + iconX, clipY, iconSize, clipY2 - clipY)
        self:drawTextureScaledAspect2(texture, self.columns[1].size + iconX, y + (self.itemheight - iconSize) / 2, iconSize, iconSize,  1, 1, 1, 1);
        self:clearStencilRect()
        self:resumeStencil()

    end
    return y + self.itemheight;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UITraitsTable:new (x, y, width, height)
    local menuTableData = ISPanel:new(x, y, width, height);
    setmetatable(menuTableData, self);
    menuTableData.borderColor = {r=0.4, g=0.4, b=0.4, a=0};
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.localPlayer = getPlayer();
    menuTableData.lastSelectedIndex = 0;
    menuTableData.buttonList = {};
    menuTableData.updateTraits = self.updateTraits;
    UITraitsTable.instance = menuTableData;
    return menuTableData;
end