require "ISUI/ISPanel"

--*********************************************************
--* 琚氳阿鑺枩閭阿瑜滆柂瑜樻 瑜嶈瑜岄偑钖姱鑳佹郴鎳?UI
--*********************************************************
EtherPlayerEditor = ISPanel:derive("EtherPlayerEditor"); -- 琚ч偑瑜嬭阿姊板啓鑺儊閭柂鎳堟 鑺 ISPanel

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?prerender
--*********************************************************
function EtherPlayerEditor:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    local x, y, w, h = self.avatarPanel.x, self.avatarPanel.y, self.avatarPanel.width, self.avatarPanel.height
    self:drawRectBorder(x - 2, y - 2, w + 4, h + 4, 1, 0.3, 0.3, 0.3);
	self:drawTextureScaled(self.avatarBackgroundTexture, x, y, w, h, 1, 1, 1, 1);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?瑜嬭姱鏂滆瑜屾噲娉?娉昏姱璋㈡瑜嬫噲娉婚偑 灞戣瑜曟噲
--*********************************************************
function EtherPlayerEditor:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?render
--*********************************************************
function EtherPlayerEditor:render()
    ISPanel.render(self);

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end;

    self:clearStencilRect();
end

--*********************************************************
--* 琚涜姱鏂滈偑鑳佽阿姊拌柂鎳堟 瑜屾娉昏瑜岃姱鑳佽姱娉?瑜嬭瑜夎姱娉绘噲
--*********************************************************
function EtherPlayerEditor:addLabel(text, x, y, font, color)
    if font == nil then
        font = UIFont.Small;
    end

    if color == nil then
        color = {r = 1, g = 1, b = 1, a = 1}
    end

    local height = getTextManager():getFontHeight(font)

    local label = ISLabel:new(x, y, height, text, color.r, color.g, color.b, color.a, font, true)
    label:initialise()
    label:instantiate()
    self:addChild(label)
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鍐欒姱瑜旀瑜夎柂鎳堣 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?
--*********************************************************
function EtherPlayerEditor:createChildren()
    ISPanel.createChildren(self);
    
    -- Store initial player state
    self.childrenCreated = false;
    
    -- Try to create children if player exists
    self:tryCreatePlayerUI();
end

--*********************************************************
--* Try to create player UI elements
--*********************************************************
function EtherPlayerEditor:tryCreatePlayerUI()
    -- If already created, skip
    if self.childrenCreated then return; end
    
    -- Check if player exists and has a valid descriptor
    if self.localPlayer == nil then 
        self.localPlayer = getPlayer();
        if self.localPlayer == nil then return; end
    end
    
    local descriptor = self.localPlayer:getDescriptor();
    if descriptor == nil then return end;

    -- Create 3D avatar panel with error handling
    local avatarOk, avatarErr = pcall(function()
        self.avatarPanel = ISUI3DModel:new(20, 20, 128, 270)
        self.avatarPanel:setVisible(true)
        -- 必须设置角色, 否则 modelInstance 为 null, UI3DModel 渲染时每帧 NPE
        -- (原生 CharacterCreationAvatar 同款用法; 不要用 setOutfitName, 无 baseVisual 时也会 NPE)
        self.avatarPanel:setCharacter(self.localPlayer)
        self.avatarPanel:setState("idle")
        self.avatarPanel:setDirection(IsoDirections.S)
        self.avatarPanel:setIsometric(false)
        self:addChild(self.avatarPanel)
    end)
    if not avatarOk then
        print("[EtherHack] Avatar panel creation failed: " .. tostring(avatarErr));
        -- Create a placeholder panel instead
        self.avatarPanel = ISPanel:new(20, 20, 128, 270);
        self.avatarPanel:initialise();
        self.avatarPanel:instantiate();
        self.avatarPanel.backgroundColor = {r=0.2, g=0.2, b=0.2, a=0.8};
        self:addChild(self.avatarPanel);
    end

    self:addLabel(getText("IGUI_PlayerStats_Username") .. " ".. tostring(self.localPlayer:getUsername() or ""), 180, 20);
    self:addLabel(getText("IGUI_PlayerStats_DisplayName").. " ".. tostring(self.localPlayer:getDisplayName() or ""), 180, 60);
    self:addLabel(getText("UI_characreation_forename").. ": " .. tostring(descriptor:getForename() or ""), 180, 100);
    self:addLabel(getText("UI_characreation_surname").. ": " .. tostring(descriptor:getSurname() or ""), 180, 140);
    
    -- Safely get profession name (Build 42 uses CharacterProfessionDefinition)
    local professionName = "Unknown";
    local profOk, profErr = pcall(function()
        -- Build 42: Use getCharacterProfession() which returns CharacterProfession object
        local professionObj = nil;
        if descriptor.getCharacterProfession then
            professionObj = descriptor:getCharacterProfession();
        end
        
        if professionObj ~= nil then
            -- Build 42: Use CharacterProfessionDefinition to get the label
            if CharacterProfessionDefinition and CharacterProfessionDefinition.getCharacterProfessionDefinition then
                local profDef = CharacterProfessionDefinition.getCharacterProfessionDefinition(professionObj);
                if profDef and profDef.getLabel then
                    professionName = tostring(profDef:getLabel() or "Unknown");
                end
            end
            -- Fallback: try getName on the profession object itself
            if professionName == "Unknown" and professionObj.getName then
                professionName = tostring(professionObj:getName() or "Unknown");
            end
            -- Last fallback: tostring
            if professionName == "Unknown" then
                professionName = tostring(professionObj);
            end
        end
    end)
    if not profOk then
        print("[EtherHack] Failed to get profession: " .. tostring(profErr));
    end
    self:addLabel(getText("IGUI_PlayerStats_Profession").. " ".. professionName, 180, 180);
    
    -- Time survived
    local timeSurvived = "N/A"
    if self.localPlayer.getTimeSurvived then
        timeSurvived = tostring(self.localPlayer:getTimeSurvived() or "N/A")
    end
    self:addLabel(getText("IGUI_char_Survived_For").. ": " .. timeSurvived, 180, 220);
    
    local editTimeBtn = ISButton:new(500, 220, 120, 36, getTranslate("UI_PlayerEditor_EditStats") or "Edit", self, self.onEditTimeButton)
    editTimeBtn:initialise()
    editTimeBtn:instantiate()
    self:addChild(editTimeBtn)
    
    -- Zombie kills
    local zombieKills = "0"
    if self.localPlayer.getZombieKills then
        zombieKills = tostring(self.localPlayer:getZombieKills() or 0)
    end
    self:addLabel(getText("IGUI_char_Zombies_Killed").. ": " .. zombieKills, 180, 260);
    
    local editKillsBtn = ISButton:new(500, 260, 120, 36, getTranslate("UI_PlayerEditor_EditStats") or "Edit", self, self.onEditKillsButton)
    editKillsBtn:initialise()
    editKillsBtn:instantiate()
    self:addChild(editKillsBtn)

    local chatMuted = getText("Sandbox_ThumpNoChasing_option1") or "Yes";
    if self.localPlayer.isAllChatMuted and not self.localPlayer:isAllChatMuted() then
        chatMuted = getText("Sandbox_ThumpNoChasing_option2") or "No"
    end

    self:addLabel(getText("IGUI_PlayerStats_AccessLevel") .. " ".. tostring(self.localPlayer:getAccessLevel() or ""), self.width - 140, 20);
    self:addLabel(getText("IGUI_PlayerStats_ChatMuted").. " ".. chatMuted, self.width - 140, 60);
    
    -- Nutrition info
    local weight = "N/A"
    local calories = "N/A"
    local nutrition = self.localPlayer:getNutrition()
    if nutrition then
        if nutrition.getWeight then weight = tostring(math.floor(nutrition:getWeight() or 0)) end
        if nutrition.getCalories then calories = tostring(math.floor(nutrition:getCalories() or 0)) end
    end
    self:addLabel(getText("IGUI_char_Weight").. ": ".. weight, self.width - 140, 100);
    self:addLabel((getTranslate("UI_PlayerEditor_PlayerInfo_Calories") or "Calories").. ": ".. calories, self.width - 140, 140);

    self:addLabel(getTranslate("UI_PlayerEditor_PlayerTraits_Title") or "Traits", 20, self.avatarPanel.x + self.avatarPanel.height + 10, UIFont.Medium )

    -- Safely create traits panel
    local traitsOk, traitsErr = pcall(function()
        self.traitsPanel = UITraitsTable:new(20, 390, self.width - 20 * 2, 360);
        self.traitsPanel:initialise();
        self.traitsPanel.parent = self;
        self:addChild(self.traitsPanel);
    end)
    if not traitsOk then
        print("[EtherHack] Failed to create traits panel: " .. tostring(traitsErr))
    end

    self:addLabel(getTranslate("UI_PlayerEditor_PlayerSkills_Title") or "Skills", 20, 310 + 360 + 20, UIFont.Medium )

    -- Safely create skills panel
    local skillsOk, skillsErr = pcall(function()
        self.skillPanel = UISkillTable:new(20, 310 + 360 + 60, self.width - 20 * 2, 360);
        self.skillPanel:initialise();
        self.skillPanel.parent = self;
        self:addChild(self.skillPanel);
    end)
    if not skillsOk then
        print("[EtherHack] Failed to create skills panel: " .. tostring(skillsErr))
    end
    
    -- Mark children as successfully created
    self.childrenCreated = true;
    print("[EtherHack] Player Editor UI created successfully");
end

function EtherPlayerEditor:updateLabels()
    -- Remove all existing children
    if self.avatarPanel then
        self:removeChild(self.avatarPanel)
    end
    for _,child in pairs(self:getChildren()) do
        self:removeChild(child)
    end
    -- Reset creation flag and recreate
    self.childrenCreated = false;
    self:tryCreatePlayerUI()
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
--* 琚ㄦ枩钖姱鑳佽阿姊拌柂鎳堟 閿岄偑钖璋㈡噲
--*********************************************************
function EtherPlayerEditor:update()
    ISPanel.update(self);
    
    -- Try to create UI if player wasn't available before
    if not self.childrenCreated then
        self.localPlayer = getPlayer();
        if self.localPlayer then
            self:tryCreatePlayerUI();
        end
    end

    if self.localPlayer == nil then return end
    
    if self.avatarPanel then
        self.avatarPanel:setCharacter(self.localPlayer)
    end
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 钖姱鑳佽姱璋愯姱 瑜濇郴锜规灞戦攲璋㈣瑜夐偑 灞戞钖
--*********************************************************
function EtherPlayerEditor:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.avatarBackgroundTexture = getTexture("media/ui/avatarBackground.png")
    menuTableData.workInGameText = getTranslate("UI_PlayerEditor_PanelWorkOnlyInGame") or "This panel only works in-game";
    menuTableData.localPlayer = getPlayer();
    EtherPlayerEditor.instance = self;
    self.__index = self;

    return menuTableData;
end