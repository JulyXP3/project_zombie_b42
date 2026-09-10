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
    self.mouseOver = false;
end

--************************************************************************--
--** 悬停状态自行维护 (不用 UIElement:isMouseOver, 它返回 Java Boolean 对象,
--** Kahlua 下 Boolean(false) 也是真值, 会导致按钮永远处于悬停态)
--************************************************************************--
function UIButton:onMouseMove(dx, dy)
    self.mouseOver = true;
end

function UIButton:onMouseMoveOutside(dx, dy)
    self.mouseOver = false;
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
--** 统一皮肤走 EtherTheme.drawButton (切角青边), 与导航磁贴/功能行盒同一
--** 套视觉语言; 取代原先的直角描边 + 左侧 3px accent 色块(观感突兀)。
--************************************************************************--
function UIButton:render()
	-- 垂直居中基准: getFontHeight。这是 Java DrawText/DrawTextCentre 实际使用的
	-- 文字块高度, 实机验证过 —— 换成 MeasureFont 会让所有按钮文字整体上移。
	-- 全工程的行内文字都以此为准, 标签侧由 EtherTheme.makeLabel 对齐到这里。
	local fontH = getTextManager():getFontHeight(self.font);
	local state = "normal";
	if not self.isEnable then
		state = "disabled";
	elseif self.onPressed then
		state = "pressed";
	elseif self.mouseOver then
		state = "hover";
	end

	local tr, tg, tb, ta = EtherTheme.drawButton(self, 0, 0, self.width, self.height, state);

	-- 长文案兜底: 按钮被调用点钳窄(多语言长标题/大字体档)时按可用宽度缩字
	-- 居中绘制, 保证标题不越出按钮框 (战利品-刷弹药 图1 实机反馈)。
	-- TextManager:DrawString 吃屏幕绝对坐标, 与 Java DrawTextCentre 的
	-- getAbsoluteX()+x 算式对齐; 缩放机制与说明文字(EtherTheme.drawHintText)同源。
	-- 不设缩放下限: 极端组合(俄语长句×窄面板×大字体)下宁可字小(≈说明文字
	-- 字号)也不允许溢出 —— 溢出正是本次要修的缺陷。
	local title = self.title or "";
	-- MeasureStringX 对 (font, title) 是确定值, 标题/字体静态时每帧重测纯浪费;
	-- 换标题(如模式切换钮)或换字体自动失效重测
	local tw;
	if self._mtTitle == title and self._mtFont == self.font then
		tw = self._mtW;
	else
		tw = getTextManager():MeasureStringX(self.font, title);
		self._mtTitle = title;
		self._mtFont = self.font;
		self._mtW = tw;
	end
	local availW = self.width - EtherTheme.ctrlPadX * 2;
	if tw > availW and tw > 0 then
		local scale = availW / tw;
		getTextManager():DrawString(self.font,
			self:getAbsoluteX() + (self.width - tw * scale) / 2,
			self:getAbsoluteY() + (self.height - fontH * scale) / 2,
			scale, title, tr, tg, tb, ta);
		return;
	end
	self:drawTextCentre(self.title, self.width / 2, (self.height - fontH) / 2, tr, tg, tb, ta, self.font);
end

--************************************************************************--
--** Включение или отключение кнопки
--************************************************************************--
function UIButton:setEnable(isEnable)
	self.isEnable = isEnable;
end

--************************************************************************--
--** 按文字内容测量按钮所需宽度 (单一来源)。
--** 调用点需要"等宽按钮组"或"限宽"时都用它预算, 不要各自复制 padX 公式。
--************************************************************************--
function UIButton.measureWidth(title)
	return getTextManager():MeasureStringX(UIFont.Small, title or "") + EtherTheme.ctrlPadX * 2;
end

--************************************************************************--
--** 一组按钮的统一宽度 (取组内最宽文案), 用于等宽排布。
--************************************************************************--
function UIButton.measureGroupWidth(titles)
	local w = 0;
	for i = 1, #titles do
		local t = UIButton.measureWidth(titles[i]);
		if t > w then w = t; end
	end
	return w;
end

--************************************************************************--
--** Создание новой кнопки
--**
--** maxWidth (可选): 宽度硬上限。不传时按文字自动加宽(防切换语言后文字溢出);
--**   传了就绝不超过它 —— 否则调用点写的 "if bw > rowW then bw = rowW" 会被
--**   这里的自动加宽悄悄还原, 按钮越出行盒/面板右缘(压到滚动条外)。
--************************************************************************--
function UIButton:new (x, y, width, height, title, onClickMethod, maxWidth)

	-- 自动按文本实际宽度扩展按钮, 防止切换语言后文字溢出
	-- (必须在 ISPanel:new 之前算好宽度, 否则 Java 元素不会跟着变宽)
	-- 内边距走 EtherTheme.ctrlPadX, 与全项目按钮统一
	local minW = UIButton.measureWidth(title);
	if width < minW then
        width = minW;
    end
	if maxWidth ~= nil and width > maxWidth then
		width = maxWidth;
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
