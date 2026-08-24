require "ISUI/ISPanel"

UISlider = ISPanel:derive("UISlider");

--*********************************************************
--* Ограничение значений параметра
--*********************************************************
local function valueClamp(value, min, max)
    if (value < min) then
        return min;
    end
    if (value > max) then
        return max;
    end
    return value
end

--*********************************************************
--* Нажите клавиши мыши
--*********************************************************
function UISlider:onMouseDown(x, y)
    if not self.isEnable and x >= 0 and y >= 0 and x <= self.width and y <= self.height then
		return;
	end

    self.isDragging = true;

    self.currentValue = valueClamp(math.ceil((x / self.width) * (self.maxValue - self.minValue) + self.minValue), self.minValue, self.maxValue);

    if self.onChangeMethod ~= nil then
        self.onChangeMethod(self.currentValue);
    end
end

--*********************************************************
--* Движение мыши
--*********************************************************
function UISlider:onMouseMove(x, y)
    if not self.isDragging then return; end

    local absoluteX = self:getMouseX();
    self.currentValue = valueClamp(math.floor((absoluteX / self.width) * (self.maxValue - self.minValue) + self.minValue), self.minValue, self.maxValue);

    if self.onChangeMethod ~= nil then
        self.onChangeMethod(self.currentValue);
    end
end

--*********************************************************
--* Движение мыши вне слайдера
--*********************************************************
function UISlider:onMouseMoveOutside(x, y)
    self:onMouseMove(x, y);
end

--*********************************************************
--* Поднятие клавиши мыши вне слайдера
--*********************************************************
function UISlider:onMouseUpOutside(x, y)
    self.isDragging = false;
end

--*********************************************************
--* Поднятие клавиши мыши
--*********************************************************
function UISlider:onMouseUp(x, y)
    self.isDragging = false;
end

--*********************************************************
--* Отрисовка слайдера
--* 定稿: 全宽深凹槽(40px) + 与槽等高的竖条拇指(亮薄荷+白芯)。
--* 无填充段; 当前值(天蓝)画在框外左侧紧贴框缘。
--*********************************************************
function UISlider:render()
    ISPanel.render(self);

    local b = EtherTheme.blood;
    -- 拇指用用户可自定义的强调色 (默认即主题薄荷青)。
    -- 必须在 render 时实时读 EtherMain.accentColor: 设置面板改色时是整表重新
    -- 赋值, 在 :new 里快照的引用不会跟着更新。
    local ac = EtherMain.accentColor or b;
    local dim = EtherTheme.bloodDim;
    local span = self.maxValue - self.minValue;
    local ratio = 0;
    if span > 0 then ratio = (self.currentValue - self.minValue) / span; end
    local cy = math.floor(self.height / 2);
    local GH = 40;                          -- 凹槽高 (再扩大一倍; 拇指等高)
    local TW = 8;
    local thumbCX = math.floor(TW / 2 + ratio * (self.width - TW));

    -- 凹槽: 深底 + 细薄荷边 (全宽)
    self:drawRect(0, cy - GH / 2, self.width, GH, 0.92, 0.05, 0.09, 0.095);
    self:drawRectBorder(0, cy - GH / 2, self.width, GH, 0.35, dim.r, dim.g, dim.b);

    -- 拇指: 与槽等高的竖条, 亮薄荷 + 中央白芯 (禁用时转灰)
    local tr, tg, tb, ta = ac.r, ac.g, ac.b, 1.0;
    if not self.isEnable then tr, tg, tb, ta = 0.45, 0.45, 0.45, 0.8; end
    self:drawRect(thumbCX - TW / 2, cy - GH / 2, TW, GH, ta, tr, tg, tb);
    self:drawRect(thumbCX - 1, cy - GH / 2, 2, GH, ta * 0.85, 1, 1, 1);

    -- 当前值: 天蓝, 画在拉条框"外"的左侧、紧贴框缘 (定稿位置)
    local sky = EtherTheme.sky;
    self:drawTextRight(tostring(self.currentValue), -10,
        cy - EtherTheme.fontHgtSmall / 2, sky.r, sky.g, sky.b, ta, UIFont.Small);
end


--*********************************************************
--* Создание нового экземпляра слайдера
--*********************************************************
function UISlider:new (x, y, width, height, value, minValue, maxValue, onChangeMethod)
    local uiTableData = ISPanel:new(x, y, width, height);
    setmetatable(uiTableData, self)
    self.__index = self
    uiTableData.x = x;
    uiTableData.y = y;
    uiTableData.isEnable = true;
    uiTableData.background = false;
    -- 配色不再快照到字段: render 直接取 EtherTheme / EtherMain.accentColor,
    -- 这样设置面板改强调色后立即生效。
    uiTableData.width = width;
    uiTableData.height = height;
    uiTableData.anchorLeft = true;
    uiTableData.anchorRight = false;
    uiTableData.anchorTop = true;
    uiTableData.anchorBottom = false;

    uiTableData.minValue = minValue;
    uiTableData.maxValue = maxValue;
    uiTableData.currentValue = valueClamp(value, minValue, maxValue);
    uiTableData.onChangeMethod = onChangeMethod;
    return uiTableData
end

