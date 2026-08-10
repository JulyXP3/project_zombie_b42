
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

-- Safe trait addition with server bypass
local function safeAddTraitLocal(player, traitType)
    if not player or not traitType then return; end
    
    -- Convert CharacterTrait object to string if needed
    local traitTypeStr = traitType;
    if type(traitType) ~= "string" then
        -- Build 42: traitType might be a CharacterTrait object, convert to string
        if traitType.getType then
            traitTypeStr = tostring(traitType:getType());
        else
            traitTypeStr = tostring(traitType);
        end
    end
    
    -- Prepare bypass
    if prepareBypass then prepareBypass(player); end
    
    -- Add trait directly via Lua (more reliable than Java reflection)
    local pTraits = player.getCharacterTraits and player:getCharacterTraits() or player:getTraits();
    if pTraits then 
        pTraits:add(traitType); -- Use original object for add()
    end
    
    -- Sync with bypass
    safeSync(player);
end

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIModalAddTrait = ISPanel:derive("UIModalAddTrait");

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UIModalAddTrait:createChildren()
    -- Build 42: Use CharacterTraitDefinition instead of TraitFactory
    local allTraits = nil;
    if CharacterTraitDefinition and CharacterTraitDefinition.getTraits then
        allTraits = CharacterTraitDefinition.getTraits();
    end
    
    -- Build 42: Use getCharacterTraits() instead of getTraits()
    local playerTraits = nil;
    if self.localPlayer then
        if self.localPlayer.getCharacterTraits then
            playerTraits = self.localPlayer:getCharacterTraits();
        elseif self.localPlayer.getTraits then
            playerTraits = self.localPlayer:getTraits();
        end
    end
    
    if allTraits then
        for i=0, allTraits:size()-1 do
            local traitDef = allTraits:get(i);
            if traitDef then
                local traitType = traitDef:getType();
                -- Check if player already has this trait using hasTrait method
                local hasTrait = false;
                if self.localPlayer and self.localPlayer.hasTrait then
                    hasTrait = self.localPlayer:hasTrait(traitType);
                elseif playerTraits and playerTraits.get then
                    hasTrait = playerTraits:get(traitType);
                end
                
                if not hasTrait then
                    if traitDef:getCost() >= 0 then
                        table.insert(self.goodTraits, traitDef)
                    else
                        table.insert(self.badTraits, traitDef)
                    end
                end
            end
        end
    end

    self.acceptButton = UIButton:new(10, self.height - 35, 100, 25, getTranslate("UI_PlayerEditor_PlayerTraits_ModalAccept"), 
    function() 
        UIModalAddTrait.instance:setVisible(false);
        UIModalAddTrait.instance:removeFromUIManager();
        UIModalAddTrait.instance = nil;

        local list = self.badTraits;
        if self.traitsSelector.isChecked then
            list = self.goodTraits;
        end
        local traitDef = list[self.combo.selected];
        
        if traitDef then
            -- Build 42: getType() returns CharacterTrait object, pass the whole traitDef
            local traitType = traitDef:getType();
            -- Use server bypass for adding trait
            safeAddTraitLocal(self.localPlayer, traitType);
            if UITraitsTable.instance then UITraitsTable.instance:updateTraits(); end
        end
    end)
    self.acceptButton:initialise();
    self.acceptButton:instantiate();
    self.acceptButton:setAnchorLeft(true);
    self.acceptButton:setAnchorRight(false);
    self.acceptButton:setAnchorTop(false);
    self.acceptButton:setAnchorBottom(true);
    self.acceptButton.isOnlyInGame = true;
    self:addChild(self.acceptButton);
    table.insert(self.buttonList, self.acceptButton);

    self.closeButton = UIButton:new(self.acceptButton.x + self.acceptButton.width + 10, self.height - 35, 100, 25, getTranslate("UI_PlayerEditor_PlayerTraits_ModalClose"), 
    function() 
        UIModalAddTrait.instance:setVisible(false);
        UIModalAddTrait.instance:removeFromUIManager();
        UIModalAddTrait.instance = nil;
    end)
    self.closeButton:initialise();
    self.closeButton:instantiate();
    self.closeButton:setAnchorLeft(true);
    self.closeButton:setAnchorRight(false);
    self.closeButton:setAnchorTop(false);
    self.closeButton:setAnchorBottom(true);
    self.closeButton.isOnlyInGame = true;
    self:addChild(self.closeButton);
    table.insert(self.buttonList, self.closeButton);

    self.combo = ISComboBox:new(10, 10, self.width - 20, 30, nil,nil);
    self.combo:initialise();
    self.goodTrait = {};
    self:addChild(self.combo);

    self.traitsSelector = UICheckbox:new(10, self.combo.y + self.combo.height + 10, getTranslate("UI_PlayerEditor_PlayerTraits_IsGoodTrait"), true, function ()
        self:updateTraitsList();
    end)
    self.traitsSelector:initialise();
    self.traitsSelector:instantiate();
    self:addChild(self.traitsSelector);

    self:updateTraitsList();
end

--*********************************************************
--* Обновление черт характера
--*********************************************************
function UIModalAddTrait:updateTraitsList()
    self.combo:clear();
    local list = self.badTraits;
    if self.traitsSelector.isChecked then
        list = self.goodTraits;
    end
    local tooltipMap = {};
    for _,v in ipairs(list) do
        self.combo:addOption(v:getLabel());
        tooltipMap[v:getLabel()] = v:getDescription();
    end
    self.combo:setToolTipMap(tooltipMap);

    if self.traitsSelector.isChecked then
        local hc = getCore():getGoodHighlitedColor()
        self.combo.textColor = {r=hc:getR(), g=hc:getG(), b=hc:getB(),a=0.9};
    else
        local hc = getCore():getBadHighlitedColor()
        self.combo.textColor = {r=hc:getR(), g=hc:getG(), b=hc:getB(),a=0.9};
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIModalAddTrait:new()
    local menuTableData = {};

    local width, height = 230, 110;
    local positionX = getCore():getScreenWidth() / 2 - width / 2;
    local positionY = getCore():getScreenHeight() / 2 - height/ 2;

    menuTableData = ISPanel:new(positionX, positionY, width, height);
    setmetatable(menuTableData, self);
    self.__index = self;
    menuTableData.variableColor={r=0.9, g=0.55, b=0.1, a=1};
    menuTableData.borderColor = {r=0.4, g=0.4, b=0.4, a=1};
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0.8};
    menuTableData.localPlayer = getPlayer();
    menuTableData.comboList = {};
    menuTableData.goodTraits = {};
    menuTableData.badTraits = {};
    menuTableData.buttonList = {};
    menuTableData.moveWithMouse = true;
    UIModalAddTrait.instance = menuTableData;
    return menuTableData;
end
