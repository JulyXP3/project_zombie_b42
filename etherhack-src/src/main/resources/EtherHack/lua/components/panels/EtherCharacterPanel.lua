require "ISUI/ISPanel"

--*********************************************************
--* 琚氳阿鑺枩閭阿瑜滆柂瑜樻 瑜嶈瑜岄偑钖姱鑳佹郴鎳?UI
--*********************************************************
EtherCharacterPanel = ISPanel:derive("EtherCharacterPanel"); -- 琚ч偑瑜嬭阿姊板啓鑺儊閭柂鎳堟 鑺 ISPanel

--*********************************************************
--* 琚涜姱鏂滈偑鑳佽阿姊拌柂鎳堟 瑜旀娉绘枩鑺郴瑜嬭姱鑳?
--*********************************************************
function EtherCharacterPanel:addCheckBox(title, method, isSelected, isOnlyInGame)
    local checkBoxAmount = #self.checkBoxList;
    local checkboxX = 30;
    local checkboxY = 20 + checkBoxAmount * 40;

    local checkbox = UICheckbox:new(checkboxX, checkboxY, title, isSelected, method);
    checkbox:initialise();
    checkbox:instantiate();
    checkbox:setAnchorLeft(true);
    checkbox:setAnchorRight(false);
    checkbox:setAnchorTop(false);
    checkbox:setAnchorBottom(true);
    checkbox.isOnlyInGame = isOnlyInGame;
    self:addChild(checkbox);

    self:setScrollHeight(self:getScrollHeight() + checkbox.height + 40);

    table.insert(self.checkBoxList, checkbox);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?瑜嬭姱鏂滆瑜屾噲娉?娉昏姱璋㈡瑜嬫噲娉婚偑 灞戣瑜曟噲
--*********************************************************
function EtherCharacterPanel:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* 琚ㄦ枩钖姱鑳佽阿姊拌柂鎳堟 閿岄偑钖璋㈡噲
--*********************************************************
function EtherCharacterPanel:updatePanel()
    for i=1, #self.checkBoxList do
        local item = self.checkBoxList[i];
        if item.isOnlyInGame and self.localPlayer == nil then
        item:setEnable(false);
        end
    end
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鍐欒姱瑜旀瑜夎柂鎳堣 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?
--*********************************************************
function EtherCharacterPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true)
    self:setScrollHeight(0)
    self:addScrollBars();

    self:addCheckBox(getTranslate("UI_CharacterPanel_MultiHitZombies"), function(isChecked)
        toggleMultiHitZombies(isChecked);
    end, isMultiHitZombies(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_ZombieDontAttack"), function(isChecked)
        toggleZombieDontAttack(isChecked);
    end, isZombieDontAttack(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_BuildCheat"), function(isChecked)
        ISBuildMenu.cheat = isChecked;
    end, ISBuildMenu.cheat, false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_FarmingCheat"), function(isChecked)
        ISFarmingMenu.cheat = isChecked;
    end, ISFarmingMenu.cheat, false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_GodMode"), function(isChecked)
        toggleGodMode(isChecked);
    end, isEnableGodMode(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_TimedActionCheat"), function(isChecked)
        toggleTimedActionCheat(isChecked);
    end, isTimedActionCheat(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_NoClip"), function(isChecked)
        toggleNoclip(isChecked);
    end, isEnableNoclip(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_Invisible"), function(isChecked)
        toggleInvisible(isChecked);
    end, isEnableInvisible(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_NightVision"), function(isChecked)
        toggleNightVision(isChecked);
    end, isEnableNightVision(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_InstantKill"), function(isChecked)
        toggleExtraDamage(isChecked);
        if(not isChecked) then
            resetWeaponsStats()
        end
    end, isExtraDamage(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_UnlimitedCarry"), function(isChecked)
        toggleEnableUnlimitedCarry(isChecked);
    end, isEnableUnlimitedCarry(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_UnlimitedEndurance"), function(isChecked)
        toggleUnlimitedEndurance(isChecked);
    end, isUnlimitedEndurance(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_UnlimitedAmmo"), function(isChecked)
        toggleUnlimitedAmmo(isChecked);
    end, isUnlimitedAmmo(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_UnlimitedCondition"), function(isChecked)
        toggleUnlimitedCondition(isChecked);
    end, isUnlimitedCondition(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_AutoRepairsItems"), function(isChecked)
        toggleAutoRepairItems(isChecked);
    end, isAutoRepairItems(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableRecoil"), function(isChecked)
        toggleNoRecoil(isChecked)
    end, isNoRecoil(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableFatigue"), function(isChecked)
        toggleDisableFatigue(isChecked);
    end, isDisableFatigue(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableHunger"), function(isChecked)
        toggleDisableHunger(isChecked);
    end, isDisableHunger(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableThirst"), function(isChecked)
        toggleDisableThirst(isChecked);
    end, isDisableThirst(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableDrunkenness"), function(isChecked)
        toggleDisableDrunkenness(isChecked);
    end, isDisableDrunkenness(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableAnger"), function(isChecked)
        toggleDisableAnger(isChecked);
    end, isDisableAnger(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableFear"), function(isChecked)
        toggleDisableFear(isChecked);
    end, isDisableFear(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisablePain"), function(isChecked)
        toggleDisablePain(isChecked);
    end, isDisablePain(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisablePanic"), function(isChecked)
        toggleDisablePanic(isChecked);
    end, isDisablePanic(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableMorale"), function(isChecked)
        toggleDisableMorale(isChecked);
    end, isDisableMorale(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableStress"), function(isChecked)
        toggleDisableStress(isChecked);
    end, isDisableStress(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableSickness"), function(isChecked)
        toggleDisableSickness(isChecked);
    end, isDisableSickness(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableStressFromCigarettes"), function(isChecked)
        toggleDisableStressFromCigarettes(isChecked);
    end, isDisableStressFromCigarettes(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableSanity"), function(isChecked)
        toggleDisableSanity(isChecked);
    end, isDisableSanity(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableBoredomLevel"), function(isChecked)
        toggleDisableBoredomLevel(isChecked);
    end, isDisableBoredomLevel(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableUnhappynessLevel"), function(isChecked)
        toggleDisableUnhappynessLevel(isChecked);
    end, isDisableUnhappynessLevel(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableWetness"), function(isChecked)
        toggleDisableWetness(isChecked);
    end, isDisableWetness(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableInfectionLevel"), function(isChecked)
        toggleDisableInfectionLevel(isChecked);
    end, isDisableInfectionLevel(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_DisableFakeInfectionLevel"), function(isChecked)
        toggleDisableFakeInfectionLevel(isChecked);
    end, isDisableFakeInfectionLevel(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_OptimalCalories"), function(isChecked)
        toggleOptimalCalories(isChecked);
    end, isOptimalCalories(), false);

    self:addCheckBox(getTranslate("UI_CharacterPanel_OptimalWeight"), function(isChecked)
        toggleOptimalWeight(isChecked);
    end, isOptimalWeight(), false);

    -- 刷弹药 (ammo farming) — 紧贴"维持最佳体重"下方, 单行: 自动刷(开) 自动刷(关) 设置弹药数 [N]
    local farmY = 20 + #self.checkBoxList * 40;

    self.ammoFarmBox = ISTextEntryBox:new(tostring(getAmmoFarmCount()), 450, farmY + 2, 80, 20);
    self.ammoFarmBox:initialise();
    self.ammoFarmBox:instantiate();
    self:addChild(self.ammoFarmBox);

    self.farmAutoBtn = UIButton:new(30, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmAuto"),
    function()
        local n = tonumber(self.ammoFarmBox:getInternalText());
        if n and n > 0 then setAmmoFarmCount(n) end
        farmSetAmmo();
        EtherAmmoFarm.enabled = true;
    end);
    self.farmAutoBtn:initialise();
    self.farmAutoBtn:instantiate();
    self:addChild(self.farmAutoBtn);

    self.farmStopBtn = UIButton:new(170, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmStop"),
    function()
        EtherAmmoFarm.enabled = false;
    end);
    self.farmStopBtn:initialise();
    self.farmStopBtn:instantiate();
    self:addChild(self.farmStopBtn);

    self.farmSetBtn = UIButton:new(310, farmY, 130, 24, getTranslate("UI_CharacterPanel_AmmoFarmSet"),
    function()
        local n = tonumber(self.ammoFarmBox:getInternalText());
        if n and n > 0 then setAmmoFarmCount(n) end
        farmSetAmmo();
    end);
    self.farmSetBtn:initialise();
    self.farmSetBtn:instantiate();
    self:addChild(self.farmSetBtn);

    self:setScrollHeight(farmY + 40);

    self:updatePanel();
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?prerender
--*********************************************************
function EtherCharacterPanel:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?render
--*********************************************************
function EtherCharacterPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 钖姱鑳佽姱璋愯姱 瑜濇郴锜规灞戦攲璋㈣瑜夐偑 灞戞钖
--*********************************************************
function EtherCharacterPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    self.checkBoxList = {}; -- 灏忛攲鎳堣鑺郴 鑳佽姊拌 瑜旀娉绘枩鑺郴瑜嬭姱鑳?

    return menuTableData;
end