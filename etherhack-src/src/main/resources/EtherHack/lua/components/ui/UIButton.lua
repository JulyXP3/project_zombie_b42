require "ISUI/ISPanel"

UIButton = ISPanel:derive("UIButton");

--************************************************************************--
--** Обработка поднятия клавиши нажатия
--************************************************************************--
function UIButton:onMouseUp(x, y)
    if not self:getIsVisible() or not self.isEnable and x >= 0 and y >= 0 and x <= self.width and y <= self.height then
		return;
	end

    local process = false;

    if self.onPressed == true then
        process = true;
    end

    self.onPressed = false;
    
	if self.onClickMethod == nil then
        return;
    end

    if process or self.allowMouseUpProcessing then
        getSoundManager():playUISound(self.activateSound)
        self.onClickMethod();
    end
end

--************************************************************************--
--** Обработка выхода мыши за границы кнопки
--************************************************************************--
function UIButton:onMouseUpOutside(x, y)
    self.onPressed = false;
end

--************************************************************************--
--** Обработка нажатия по кнопке
--************************************************************************--
function UIButton:onMouseDown(x, y)
	if not self:getIsVisible() or not self.isEnable and x >= 0 and y >= 0 and x <= self.width and y <= self.height then
		return;
	end

    self.onPressed = true;
end

--************************************************************************--
--** Обработка двойного клика
--************************************************************************--
function UIButton:onMouseDoubleClick(x, y)
	return self:onMouseDown(x, y)
end


--************************************************************************--
--** Отрисовка кнопки
--************************************************************************--
function UIButton:render()
	local fontH = getTextManager():getFontHeight(self.font);
	if self.isEnable then
		if not self.onPressed then
			self:drawRect(0, 0, self.width, self.height, 0.16, EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b)
			self:drawRectBorder(0, 0, self.width, self.height, 0.85, EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b)
			self:drawRect(0, 0, 3, self.height, 0.9, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b)
			self:drawTextCentre(self.title, self.width / 2 + 1, (self.height - fontH) / 2, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1.0, self.font);
		else
			self:drawRect(0, 0, self.width, self.height, 0.9, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b)
			self:drawRectBorder(0, 0, self.width, self.height, 1.0, 0.0, 0.0, 0.0)
			self:drawTextCentre(self.title, self.width / 2 + 1, (self.height - fontH) / 2, 0.05, 0.05, 0.05, 1.0, self.font);
		end
	else
		self:drawRect(0, 0, self.width, self.height, 0.08, 0.12, 0.11, 0.11)
		self:drawRectBorder(0, 0, self.width, self.height, 0.3, EtherTheme.bloodDim.r, EtherTheme.bloodDim.g, EtherTheme.bloodDim.b)
		self:drawTextCentre(self.title, self.width / 2 + 1, (self.height - fontH) / 2, 1.0, 1.0, 1.0, 0.5, self.font);
	end
end

--************************************************************************--
--** Включение или отключение кнопки
--************************************************************************--
function UIButton:setEnable(isEnable)
	self.isEnable = isEnable;
end

--************************************************************************--
--** Создание новой кнопки
--************************************************************************--
function UIButton:new (x, y, width, height, title, onClickMethod)

	-- 自动按文本实际宽度扩展按钮, 防止切换语言后文字溢出
	-- (必须在 ISPanel:new 之前算好宽度, 否则 Java 元素不会跟着变宽)
	local textWidth = getTextManager():MeasureStringX(UIFont.Small, title or "");
	if width < textWidth + 20 then
        width = textWidth + 20;
    end

	local uiTableData = {}
	uiTableData = ISPanel:new(x, y, width, height);
	setmetatable(uiTableData, self)
    self.__index = self
	uiTableData.x = x;
	uiTableData.y = y;
	uiTableData.font = UIFont.Small;
	uiTableData.textureWidth = width;
	uiTableData.textureHeight = height;
	uiTableData.borderColor = {r=0, g=0, b=0, a=0};
	uiTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    uiTableData.textColor = {r=1.0, g=1.0, b=1.0, a=1.0};
    uiTableData.width = width;
    uiTableData.height = height;
	uiTableData.anchorLeft = true;
	uiTableData.anchorRight = false;
	uiTableData.anchorTop = true;
	uiTableData.anchorBottom = false;
	uiTableData.mouseOver = false;
	uiTableData.title = title;
	uiTableData.onClickMethod = onClickMethod;
	uiTableData.isEnable = true;
	uiTableData.onPressed = false;
    uiTableData.activateSound = "UIActivateButton"
   return uiTableData
end
