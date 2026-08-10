require "ISUI/ISPanel"

--*********************************************************
--* 琚氳阿鑺枩閭阿瑜滆柂瑜樻 瑜嶈瑜岄偑钖姱鑳佹郴鎳?UI
--*********************************************************
EtherVisualsPanel = ISPanel:derive("EtherVisualsPanel"); -- 琚ч偑瑜嬭阿姊板啓鑺儊閭柂鎳堟 鑺 ISPanel

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?prerender
--*********************************************************
function EtherVisualsPanel:prerender()
    self:setStencilRect(0,10,self:getWidth(),self:getHeight() - 20);
    ISPanel.prerender(self);
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?render
--*********************************************************
function EtherVisualsPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();
end

--*********************************************************
--* 琚ㄦ枩瑜夐偑鏂滆姱瑜屾郴閭?瑜嬭姱鏂滆瑜屾噲娉?娉昏姱璋㈡瑜嬫噲娉婚偑 灞戣瑜曟噲
--*********************************************************
function EtherVisualsPanel:onMouseWheel(del)
	self:setYScroll(self:getYScroll() - (del * 40));
	return true;
end

--*********************************************************
--* 琚涜姱鏂滈偑鑳佽阿姊拌柂鎳堟 瑜旀娉绘枩鑺郴瑜嬭姱鑳?
--*********************************************************
function EtherVisualsPanel:addCheckBox(title, method, isSelected)
    local yOffset = 5;
    if #self.uiElements == 0 then
        yOffset = 0;
    end

    local checkbox = UICheckbox:new(20, self.yRowPosition + yOffset, title, isSelected, method);
    checkbox:initialise();
    checkbox:instantiate();
    checkbox:setAnchorLeft(true);
    checkbox:setAnchorRight(false);
    checkbox:setAnchorTop(false);
    checkbox:setAnchorBottom(true);
    self:addChild(checkbox);

    self:setScrollHeight(self:getScrollHeight() + checkbox.height + 40);

    self.yRowPosition = self.yRowPosition + checkbox.height + yOffset;

    table.insert(self.uiElements, checkbox);
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 灞戞瑜屾郴鎳?
--*********************************************************
function EtherVisualsPanel:addLabel(posX, posY, title)
    local label = ISLabel:new(posX, posY + 3, getTextManager():getFontHeight(UIFont.Small), title, 1, 1, 1, 1, UIFont.Small, true)
	self:addChild(label)
    return label
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 瑜嬭阿閭硠鍐欐瑜夐偑
--*********************************************************
function EtherVisualsPanel:addSlider(posX, posY, width, height, value, minValue, maxValue, method)
    local slider = UISlider:new(posX, posY, width, height, value, minValue, maxValue, method)
    slider:initialise();
    slider:instantiate();
    self:addChild(slider);
    return slider
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 瑜嬭阿閭硠鍐欐瑜夐偑
--*********************************************************
function EtherVisualsPanel:addSliderWithLabel(title, value, minValue, maxValue, method)
    local yOffset = 20;
    if #self.uiElements == 0 then
        yOffset = 20;
    end

    local sliderHeight= 20;
    local sliderWidth = 200;

    self:addLabel(10, self.yRowPosition + yOffset, title);
    local slider = self:addSlider(self.width - sliderWidth - 100, self.yRowPosition + yOffset + 16, sliderWidth, sliderHeight, value, minValue, maxValue, method)
    
    self:setScrollHeight(self:getScrollHeight() + sliderHeight * 2 + 10 + yOffset);
    
    self.yRowPosition = self.yRowPosition + sliderHeight * 2 + 10 + yOffset;

    table.insert(self.uiElements, slider);

    return slider;
end

--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 鍐欒姱瑜旀瑜夎柂鎳堣 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?
--*********************************************************
function EtherVisualsPanel:createChildren()
    ISPanel.createChildren(self);

    self:setScrollChildren(true);
    self:setScrollHeight(0);
    self:addScrollBars();

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawCheatCredits"), function(isChecked)
        toggleVisualDrawCredits(isChecked);
    end, isVisualDrawCredits());

    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsEnable"), function(isChecked)
        toggleVisualsEnable(isChecked);
    end, isVisualsEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_360Vision"), function(isChecked)
        toggleVisualEnable360Vision(isChecked);
    end, isVisualEnable360Vision());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsVehiclesEnable"), function(isChecked)
        toggleVisualsVehiclesEnable(isChecked);
    end, isVisualsVehiclesEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawLineToVehicles"), function(isChecked)
        toggleVisualDrawLineToVehicle(isChecked);
    end, isVisualDrawLineToVehicle());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsZombiesEnable"), function(isChecked)
        toggleVisualsZombiesEnable(isChecked);
    end, isVisualsZombiesEnable());



    self:addCheckBox(getTranslate("UI_VisualsPanel_IsVisualsPlayersEnable"), function(isChecked)
        toggleVisualsPlayersEnable(isChecked);
    end, isVisualsPlayersEnable());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawToLocalPlayer"), function(isChecked)
        toggleVisualDrawToLocalPlayer(isChecked);
    end, isVisualDrawToLocalPlayer());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawLineToPlayers"), function(isChecked)
         toggleVisualDrawLineToPlayers(isChecked);
    end, isVisualDrawLineToPlayers());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawPlayerName"), function(isChecked)
        toggleVisualDrawPlayerNickname(isChecked) ;
    end, isVisualDrawPlayerNickname());

    self:addCheckBox(getTranslate("UI_VisualsPanel_DrawPlayerInfo"), function(isChecked)
        toggleVisualDrawPlayerInfo(isChecked) ;
    end, isVisualDrawPlayerInfo());
end
--*********************************************************
--* 灏忚姱锜瑰啓閭柂鎳堟 钖姱鑳佽姱璋愯姱 瑜濇郴锜规灞戦攲璋㈣瑜夐偑 灞戞钖
--*********************************************************
function EtherVisualsPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.yRowPosition = 10;
    self.__index = self;

    self.uiElements = {}; -- 灏忛攲鎳堣鑺郴 鑳佽姊拌 瑜濊阿姊板睉姊拌柂瑜岃姱鑳?

    return menuTableData;
end