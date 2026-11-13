--*********************************************************
--* 追踪清单弹窗 (2026-11-13):
--*   列出已加入清单的物品, 行末 × 移除; 清空/关闭。
--*   不含数量、不执行任何动作, 只维护 EtherWatchlist 集合。
--*   版式对齐战利品清单弹窗 (EtherSpawnQueuePopup): 固定宽高、黑底,
--*   标题/列表/提示/按钮四段, 行内控件水平+垂直居中。
--*********************************************************
require "ISUI/ISPanel"

local POPUP_W = 430
local POPUP_PAD = 10
local LIST_ROWS = 6
local CTRL_W = 18
local CTRL_H = 18
local ROW_H = EtherTheme.fontHgtSmall + 10
local TITLE_H = EtherTheme.fontHgtSmall + 6
local LIST_H = LIST_ROWS * ROW_H
local HINT_H = EtherTheme.fontHgtHint + 4
local POPUP_H = POPUP_PAD + TITLE_H + 4 + LIST_H + 6 + HINT_H + 4 + EtherTheme.ctrlH + POPUP_PAD

EtherWatchlistPopup = ISPanel:derive("EtherWatchlistPopup")
EtherWatchlistPopup.instance = nil

--*********************************************************
--* 打开/关闭: 同一时间只保留一个弹窗 (再点按钮 = 关闭)
--*********************************************************
function EtherWatchlistPopup.open(onChanged)
    if EtherWatchlistPopup.instance ~= nil then
        EtherWatchlistPopup.instance:close()
        return
    end
    local popup = EtherWatchlistPopup:new(onChanged)
    popup:initialise()
    popup:instantiate()

    local sw, sh = getCore():getScreenWidth(), getCore():getScreenHeight()
    local px = math.floor(getMouseX() + 2)
    local py = math.floor(getMouseY() + 2)
    if px + popup.width > sw - 8 then px = sw - popup.width - 8 end
    if px < 8 then px = 8 end
    if py + popup.height > sh - 8 then py = py - popup.height - 18 end
    if py < 8 then py = 8 end
    popup:setX(px)
    popup:setY(py)

    popup:addToUIManager()
    popup:setVisible(true)
    EtherWatchlistPopup.instance = popup
end

function EtherWatchlistPopup:new(onChanged)
    local o = ISPanel:new(0, 0, POPUP_W, POPUP_H)
    setmetatable(o, self)
    self.__index = self
    o.background = false
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 }
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 }
    o.moveWithMouse = false
    o.onChanged = onChanged
    o.titleH = TITLE_H
    o.listY = POPUP_PAD + TITLE_H + 4
    o.listH = LIST_H
    o.hintY = o.listY + LIST_H + 6
    o.btnY = o.hintY + HINT_H + 4
    o.scroll = 0
    return o
end

function EtherWatchlistPopup:createChildren()
    ISPanel.createChildren(self)
    local gap = EtherTheme.ctrlGap
    local bw = math.floor((self.width - POPUP_PAD * 2 - gap) / 2)
    self.clearBtn = UIButton:new(POPUP_PAD, self.btnY, bw, EtherTheme.ctrlH,
        getTranslate("UI_RadarPanel_ClearList"), function()
            EtherWatchlist.clear()
            if self.onChanged ~= nil then self.onChanged() end
        end, bw)
    self.closeBtn = UIButton:new(POPUP_PAD + bw + gap, self.btnY, bw, EtherTheme.ctrlH,
        getTranslate("UI_RadarPanel_CloseList"), function() self:close() end, bw)
    self:addChild(self.clearBtn)
    self:addChild(self.closeBtn)
end

--*********************************************************
--* 背景/标题走 prerender: render 在子控件之后执行, 画在那里会盖住按钮
--*********************************************************
function EtherWatchlistPopup:prerender()
    local bg = EtherTheme.panelBG
    self:drawRect(0, 0, self.width, self.height, 1.0, bg.r, bg.g, bg.b)
    EtherTheme.drawFrame(self, 0, 0, self.width, self.height)
    local th = EtherTheme
    self:drawText(getTranslate("UI_RadarPanel_ListTitle"), POPUP_PAD, POPUP_PAD - 1,
        th.sky.r, th.sky.g, th.sky.b, 1.0, UIFont.Small)
end

--*********************************************************
--* 行绘制: 名称 (超长走 stencil 裁切) + 行末 × 控件
--* 清单持久化成物品全名 (追踪扫描按全名比对), 弹窗只负责把全名翻成显示名;
--* 战利品清单在加入时就存了显示名, 这里不能照抄, 否则 targets() 会失配。
--*********************************************************
function EtherWatchlistPopup.displayName(fullName)
    local name = getItemNameFromFullType(fullName)
    if name == nil or name == "" then
        return tostring(fullName)
    end
    return name
end

function EtherWatchlistPopup:drawCtrl(x, y, label)
    local th = EtherTheme
    local yy = y + (ROW_H - CTRL_H) / 2
    self:drawRect(x, yy, CTRL_W, CTRL_H, 0.18, th.blood.r, th.blood.g, th.blood.b)
    self:drawRectBorder(x, yy, CTRL_W, CTRL_H, 0.6, th.blood.r, th.blood.g, th.blood.b)
    local tw = getTextManager():MeasureStringX(UIFont.Small, label)
    self:drawText(label, x + (CTRL_W - tw) / 2, yy + (CTRL_H - th.fontHgtSmall) / 2,
        th.text.r, th.text.g, th.text.b, 0.9, UIFont.Small)
end

function EtherWatchlistPopup:render()
    local th = EtherTheme
    local fh = EtherTheme.fontHgtSmall
    local count = EtherWatchlist.count()
    local removeX = self.width - POPUP_PAD - CTRL_W

    self:setStencilRect(POPUP_PAD - 2, self.listY, self.width - POPUP_PAD * 2 + 4, self.listH)
    if count == 0 then
        local lines = EtherTheme.wrapHint(getTranslate("UI_RadarPanel_ListEmpty"), self.width - POPUP_PAD * 2)
        for i = 1, #lines do
            EtherTheme.drawHintText(self, lines[i], POPUP_PAD,
                self.listY + 6 + (i - 1) * EtherTheme.fontHgtHint, th.textDim, 1.0)
        end
    else
        local textW = removeX - POPUP_PAD - 6
        for i = 1, count do
            local y = self.listY + (i - 1) * ROW_H - self.scroll
            if y + ROW_H > self.listY and y < self.listY + self.listH then
                local name = EtherWatchlist.at(i)
                if name ~= nil then
                    self:setStencilRect(POPUP_PAD, y, math.max(10, textW), ROW_H)
                    self:drawText(EtherWatchlistPopup.displayName(name), POPUP_PAD, y + (ROW_H - fh) / 2,
                        th.text.r, th.text.g, th.text.b, 1.0, UIFont.Small)
                    self:clearStencilRect()
                    self:drawCtrl(removeX, y, "x")
                end
            end
        end
    end
    self:clearStencilRect()

    EtherTheme.drawHintText(self, getTranslate("UI_RadarPanel_ListRemoveHint"), POPUP_PAD, self.hintY,
        th.textDim, 0.9)
end

--*********************************************************
--* 交互: 仅行末 × 命中移除, 其余点击吞掉不穿透到下层面板
--*********************************************************
function EtherWatchlistPopup:onMouseDown(x, y)
    if y < self.listY or y >= self.listY + self.listH then return false end
    local row = math.floor((y - self.listY + self.scroll) / ROW_H) + 1
    local name = EtherWatchlist.at(row)
    if name == nil then return false end
    local removeX = self.width - POPUP_PAD - CTRL_W
    if x >= removeX and x <= removeX + CTRL_W then
        EtherWatchlist.removeAt(row)
        if self.onChanged ~= nil then self.onChanged() end
    end
    return true
end

function EtherWatchlistPopup:onMouseWheel(del)
    local maxScroll = math.max(0, EtherWatchlist.count() * ROW_H - self.listH)
    self.scroll = math.max(0, math.min(maxScroll, self.scroll - del * 40))
    return true
end

-- 清单变短时把滚动收回到有效范围
function EtherWatchlistPopup:update()
    if EtherWatchlistPopup.instance ~= self then return end
    local maxScroll = math.max(0, EtherWatchlist.count() * ROW_H - self.listH)
    if self.scroll > maxScroll then self.scroll = maxScroll end
end

function EtherWatchlistPopup:close()
    if EtherWatchlistPopup.instance == self then
        EtherWatchlistPopup.instance = nil
    end
    self:setVisible(false)
    self:removeFromUIManager()
end
