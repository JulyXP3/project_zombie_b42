require "ISUI/ISScrollingListBox"

local FONT_HGT_SMALL = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* EtherFastList: 定高虚拟化列表 (ISScrollingListBox 子类)
--*
--* 原版 prerender 每帧遍历全部条目逐条调 doDrawItem 算行高 (为支持可变行高
--* 条目而设计), 全物品库 (~1 万条) 时单帧成本约 4ms —— 「战利品」页停留
--* 掉帧 (120→80fps) 的根因 (开发日志 二百三十七)。
--* 本 mod 全部列表行高固定 (draw 一律 return y + self.itemheight), 故重写为
--* 只遍历可见区间 (≈ 高度/itemheight + 2 行), 其余逐条保留原版语义:
--* 背景/边框/stencil/滚动条留白/平滑滚动/tooltip/alt 条纹奇偶/v.index/列头/
--* setScrollHeight (按条数直接算, 与原版每帧回写等价)。
--* 兜底: 若可见行实际返回高度 ≠ itemheight (未来引入变高行), 永久回退原版
--* 全量路径, 保证不出现半虚拟化的错位渲染。
--*********************************************************
EtherFastList = ISScrollingListBox:derive("EtherFastList");

function EtherFastList:prerender()
    if self.items == nil then
        return;
    end

    if self.fastListFallback then
        return ISScrollingListBox.prerender(self);
    end

    local stencilX = 0
    local stencilY = 0
    local stencilX2 = self.width
    local stencilY2 = self.height

    self:drawRect(0, -self:getYScroll(), self.width, self.height, self.backgroundColor.a, self.backgroundColor.r, self.backgroundColor.g, self.backgroundColor.b);
    if self.drawBorder then
        self:drawRectBorder(0, -self:getYScroll(), self.width, self.height, self.borderColor.a, self.borderColor.r, self.borderColor.g, self.borderColor.b)
        stencilX = 1
        stencilY = 1
        stencilX2 = self.width - 1
        stencilY2 = self.height - 1
    end

    if self:isVScrollBarVisible() then
        stencilX2 = self.vscroll.x + 3
    end

    -- This is to handle this listbox being inside a scrolling parent.
    if self:parentsHaveScrollChildren() then
        stencilX = self.javaObject:clampToParentX(self:getAbsoluteX() + stencilX) - self:getAbsoluteX()
        stencilX2 = self.javaObject:clampToParentX(self:getAbsoluteX() + stencilX2) - self:getAbsoluteX()
        stencilY = self.javaObject:clampToParentY(self:getAbsoluteY() + stencilY) - self:getAbsoluteY()
        stencilY2 = self.javaObject:clampToParentY(self:getAbsoluteY() + stencilY2) - self:getAbsoluteY()
    end
    self:setStencilRect(stencilX, stencilY, stencilX2 - stencilX, stencilY2 - stencilY)

    local n = #self.items
    local itemH = self.itemheight
    local totalH = n * itemH

    if self.selected ~= -1 and self.selected > n then
        self.selected = n
    end

    -- 可见区间: 行 k 的 content-y = (k-1)*itemH, 视口 content 范围 [-yScroll, -yScroll+height)
    -- (Java 绘制原语内部自动加 yScroll, 行坐标沿用原版 content 坐标系)
    local viewTop = 0 - self:getYScroll()
    local first = math.floor(viewTop / itemH) + 1
    if first < 1 then first = 1 end
    local last = math.ceil((viewTop + self.height) / itemH)
    if last > n then last = n end

    local altBg = self.altBgColor
    local alt = ((first % 2) == 0)   -- 原版: 第 1 行 alt=false, 逐行翻转
    local y = (first - 1) * itemH
    for k = first, last do
        local v = self.items[k]
        if not v.height then v.height = itemH end -- compatibililty (与原版同)
        if alt and altBg then
            self:drawRect(0, y, self:getWidth(), v.height - 1, altBg.r, altBg.g, altBg.b, altBg.a);
        end
        v.index = k
        local y2 = self:doDrawItem(y, v, alt)
        if y2 ~= y + itemH then
            -- 变高行: 虚拟化前提被破坏 -> 永久回退原版全量路径 (本帧重绘)
            self.fastListFallback = true
            return ISScrollingListBox.prerender(self);
        end
        if self.stopPrerender then
            self.stopPrerender = false;
            return;
        end
        v.height = y2 - y
        y = y2
        alt = not alt
    end

    self.listHeight = totalH
    self:setScrollHeight(totalH)
    self:clearStencilRect()
    if self.doRepaintStencil then
        self:repaintStencilRect(stencilX, stencilY, stencilX2 - stencilX, stencilY2 - stencilY)
    end

    local mouseY = self:getMouseY()
    self:updateSmoothScrolling()
    if mouseY ~= self:getMouseY() and self:isMouseOver() then
        self:onMouseMove(0, self:getMouseY() - mouseY)
    end
    self:updateTooltip()

    if #self.columns > 0 then
        self:drawRectBorderStatic(0, 0 - self.itemheight, self.width, self.itemheight, 1, self.borderColor.r, self.borderColor.g, self.borderColor.b);
        self:drawRectStatic(0, 0 - self.itemheight, self.width, self.itemheight, self.listHeaderColor.a, self.listHeaderColor.r, self.listHeaderColor.g, self.listHeaderColor.b);
        local dyText = (self.itemheight - FONT_HGT_SMALL) / 2
        for i, v in ipairs(self.columns) do
            self:drawRectStatic(v.size, 0 - self.itemheight, 1, self.itemheight + math.min(self.height, self.itemheight * #self.items - 1), 1, self.borderColor.r, self.borderColor.g, self.borderColor.b);
            if v.name then
                self:drawText(v.name, v.size + 10, 0 - self.itemheight - 1 + dyText - self:getYScroll(), 1, 1, 1, 1, UIFont.Small);
            end
        end
    end
    if self.useStencilForChildren then
        self:setStencilRect(0, 0, self.width, self.height)
    end
end
