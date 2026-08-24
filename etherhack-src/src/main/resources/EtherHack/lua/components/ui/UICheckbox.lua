require "ISUI/ISPanel"

UICheckbox = ISPanel:derive("UICheckbox");

--************************************************************************--
--** Инициализация чекбокса
--************************************************************************--
function UICheckbox:initialise()
	ISPanel.initialise(self);
end

--************************************************************************--
--** Установка состояния чекбокса
--************************************************************************--
function UICheckbox:setCheked(isChecked)
	self.isChecked = isChecked;
end

--************************************************************************--
--** Получение состояние чекбокса
--************************************************************************--
function UICheckbox:isChecked()
	return self.isChecked;
end

--************************************************************************--
--** Отрисовка чекбокса
--************************************************************************--
function UICheckbox:render()
	-- 方形复选框, 纹理已烘焙青色 -> 以 (1,1,1) 原样绘制 (不再被 accent 相乘, 始终青)
	if not self.isChecked then
		self:drawTextureScaled(self.uncheckedTexture, 0, 0, self.textureWidth, self.textureHeight, 1.0, 1.0, 1.0, 1.0);
	else
		self:drawTextureScaled(self.checkedTexture, 0, 0, self.textureWidth, self.textureHeight, 1.0, 1.0, 1.0, 1.0);
	end

	-- 多行标题 (长翻译如俄语由调用方折好传入): 逐行绘制, 方块与首行对齐
	if self.titleLines ~= nil and #self.titleLines > 1 then
		local lh = self.fontHeight + 2;
		local ty = (self.height - lh * #self.titleLines) / 2;
		for i = 1, #self.titleLines do
			self:drawText(self.titleLines[i], self.textureWidth + self.marginTexture,
				ty + (i - 1) * lh, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1.0, self.font);
		end
	else
		self:drawText(self.title, self.textureWidth + self.marginTexture, (self.textureHeight - self.fontHeight) / 2, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1.0, self.font);
	end
end

--************************************************************************--
--** Включение или отключение чекбокса
--************************************************************************--
function UICheckbox:setEnable(isEnable)
    self.enable = isEnable;
end

--************************************************************************--
--** Обработка клика мыши по чекбоксу
--************************************************************************--
function UICheckbox:onMouseUp(x, y)
    if self.enable and x >= 0 and y >= 0 and x <= self.width and y <= self.height then
        getSoundManager():playUISound("UIToggleTickBox");
        self.isChecked = not self.isChecked;

        if self.onCheckedMethod ~= nil then
            self.onCheckedMethod(self.isChecked);
        end
        return true;
    end

    return false;
end

--************************************************************************--
--** Создание нового чекбокса
--************************************************************************--
function UICheckbox:new (x, y, title, isChecked, onChecked)
	local uiTableData = {}
	setmetatable(uiTableData, self)
	self.__index = self
	uiTableData.x = x;
	uiTableData.y = y;
	uiTableData.checkedTexture = getExtraTexture("EtherHack/media/ui/checkbox_checked.png");
	uiTableData.uncheckedTexture = getExtraTexture("EtherHack/media/ui/checkbox_unchecked.png");
	uiTableData.textureWidth = 18;
	uiTableData.textureHeight = 18;
	uiTableData.marginTexture = 10;
	uiTableData.borderColor = {r=0, g=0, b=0, a=0.0};
	uiTableData.backgroundColor = {r=0, g=0, b=0, a=0.0};
	uiTableData.anchorLeft = true;
	uiTableData.anchorRight = false;
	uiTableData.anchorTop = true;
	uiTableData.anchorBottom = false;
	uiTableData.title = title;
	uiTableData.isChecked = isChecked;
	uiTableData.font = UIFont.Small;
    uiTableData.fontHeight = getTextManager():getFontHeight(uiTableData.font);
    uiTableData.textWidth = getTextManager():MeasureStringX(uiTableData.font, uiTableData.title);
	uiTableData.onCheckedMethod = onChecked;
	uiTableData.enable = true;

	uiTableData.width = uiTableData.textureHeight + uiTableData.marginTexture + uiTableData.textWidth + 20;
	uiTableData.height = uiTableData.textureHeight;
	uiTableData.titleLines = nil;   -- 多行时由调用方赋值 (build 时折行, render 零测量)
	return uiTableData;
end

