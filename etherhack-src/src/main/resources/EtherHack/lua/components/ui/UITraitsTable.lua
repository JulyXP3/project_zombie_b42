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
--* 按钮右侧说明文字 (与 EtherVehiclePanel/EtherCharacterPanel 的 ModuleHint 同款):
--* 做成子控件以随表格一同定位/滚动, 不吞鼠标事件 (点击要能穿到按钮)。
--* 折行在构造时算一次; 鼠标handler就地定义, 不引用 EtherFormPanel.UIRowBox,
--* 以免依赖两个文件的加载先后顺序。
--*********************************************************
local ButtonHint = ISPanel:derive("EtherTraitsButtonHint");

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
--* 余宽不足 (窄窗口) 时直接不放, 避免逐字折行糊成一团。
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
function UITraitsTable:createChildren()
    ISPanel.createChildren(self);

    -- 按钮行贴表格底部, 并左右留出内边距。
    -- 原实现: 列表高 height-90、按钮 y=height-80 且 x=0 -> 按钮下方空出 56px,
    -- 视觉上"按钮悬在中间", 且 x=0 会压住表格左边框 (实测缺陷)。
    local PAD = 8;
    local ctrlH = EtherTheme.ctrlH;
    local GAP = EtherTheme.ctrlGap;
    local btnY = self.height - ctrlH - PAD;
    local listH = btnY - GAP;
    if listH < 60 then listH = 60; end

    self.datas = ISScrollingListBox:new(0, 0, self.width, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    EtherTheme.styleList(self.datas);   -- 与同页 UISkillTable 共用同一套列表配色
    self.datas.drawBorder = false;      -- 外层行盒已提供边框
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_NameTitle"), 0);
    self.datas:addColumn(getTranslate("UI_PlayerEditor_PlayerTraits_Description"), math.floor(self.width * 0.3))
    -- 本表嵌在可滚动的"玩家"页里: 自身滚到边界时把滚轮交还外层, 否则外层滚不动
    EtherTheme.bubbleWheelAtEdge(self.datas);
    self:addChild(self.datas);

    local addTitle = getTranslate("UI_PlayerEditor_PlayerTraits_AddTrait");
    local delTitle = getTranslate("UI_PlayerEditor_PlayerTraits_DeleteTrait");
    local tm = getTextManager();
    -- 两个按钮等宽 (取最宽文案), 各语言下观感一致; 并限制在可用宽度内
    local btnW = UIButton.measureGroupWidth({ addTitle, delTitle });
    local maxBtnW = math.floor((self.width - PAD * 2 - GAP) / 2);
    if btnW > maxBtnW then btnW = maxBtnW; end

    self.addTrait = UIButton:new(PAD, btnY, btnW, ctrlH, addTitle,
    function() 
        if UIModalAddTrait.instance then
            UIModalAddTrait.instance:close()
        end
        local modal = UIModalAddTrait:new()
        modal:initialise();
        modal:addToUIManager();
        modal:setAlwaysOnTop(true);
    end, btnW)
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

    self.deleteTrait = UIButton:new(PAD + btnW + GAP, btnY, btnW, ctrlH, delTitle,
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
    end, btnW)
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

    -- 「移除」右侧提示: 多人下改特质会被服务端回滚, 需先开「其他-服务器同步保护」。
    -- 取实际 getX/getWidth 而非 btnW: UIButton 会按文案自动加宽 (见 UIButton:new)。
    self.syncHint = placeButtonHint(self, self.deleteTrait:getX(), btnY,
        self.deleteTrait:getWidth(), self.width - PAD, "UI_PlayerEditor_SyncProtectionHint");

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
    
    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)
    
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
    menuTableData.borderColor = EtherTheme.bloodDim;
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.localPlayer = getPlayer();
    menuTableData.lastSelectedIndex = 0;
    menuTableData.buttonList = {};
    menuTableData.updateTraits = self.updateTraits;
    UITraitsTable.instance = menuTableData;
    return menuTableData;
end