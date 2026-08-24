require "ISUI/ISPanel"

--*********************************************************
--* UIButtonsPanel: 左侧导航条 (带边框的 图标+文字 磁贴, 严格对齐 cyber-A-refined)
--*
--* 每个磁贴 = 一个青霓虹描边盒子, 内含 图标(左) + 文字(右)。
--*   激活项: 亮青描边 + 左青竖条 + 轻底 + 青染图标/文字;
--*   未激活: 暗青描边 + 白图标 + 次青文字。
--* 纯自绘 + 手动命中(不再用 ISButton 子元素), 杜绝图标重复; 静态无动效。
--*********************************************************

UIButtonsPanel = ISPanel:derive("UIButtonsPanel");

local TILE_H = 44;   -- 磁贴行高(含间距)
local BOX_M  = 4;    -- 盒子上下留白 -> 盒子高 = TILE_H - 2*BOX_M
local ICON   = 22;   -- 图标尺寸
local PAD_X  = 12;   -- 盒子内图标左边距
local GAP    = 10;   -- 图标与文字间距
local TOP    = 8;    -- 首个磁贴上边距

--*********************************************************
--* Prerender: 画每个磁贴(盒子 + 图标 + 文字)
--*********************************************************
function UIButtonsPanel:prerender()
    ISPanel.prerender(self);

    local b  = EtherTheme.blood;      -- 亮青(激活)
    local td = EtherTheme.textDim;    -- 次青(未激活文字)

    for id = 1, #self.tiles do
        local t = self.tiles[id];
        local ty = t.y;
        local boxY = ty + BOX_M;
        local boxH = TILE_H - BOX_M * 2;
        local active = (t.id == self.currentTabID);

        -- 盒子: 细净切角八边形描边 (EtherTheme.drawTileBox, 严格对齐概念图);
        -- 激活 = 亮描边 + 轻底 + 左亮青竖条, 未激活 = 暗描边 + 极淡底
        local bx, bw = BOX_M, self.width - BOX_M * 2;
        EtherTheme.drawTileBox(self, bx, boxY, bw, boxH, active);

        -- 图标(盒内左, 竖直居中): 激活青染, 否则白
        local iy = ty + (TILE_H - ICON) / 2;
        if active then
            self:drawTextureScaled(t.iconTexture, BOX_M + PAD_X, iy, ICON, ICON, 1.0, b.r, b.g, b.b);
        else
            self:drawTextureScaled(t.iconTexture, BOX_M + PAD_X, iy, ICON, ICON, 0.9, 1, 1, 1);
        end

        -- 文字: 激活青亮, 否则次青
        local tc = active and b or td;
        self:drawText(tr(t.labelKey), BOX_M + PAD_X + ICON + GAP,
            ty + (TILE_H - EtherTheme.fontHgtSmall) / 2, tc.r, tc.g, tc.b, 1, UIFont.Small);
    end
end

--*********************************************************
--* 命中测试: 鼠标松开落在哪个磁贴 -> 打开对应页
--*********************************************************
function UIButtonsPanel:onMouseUp(x, y)
    for id = 1, #self.tiles do
        local t = self.tiles[id];
        if y >= t.y and y < t.y + TILE_H then
            self:openPanel(t.id);
            return true;
        end
    end
    return true;
end

function UIButtonsPanel:onMouseDown(x, y)
    return true;   -- 捕获点击, 避免穿透/拖窗
end

--*********************************************************
--* 打开指定 ID 的选项卡 (内容面板由本面板右侧生成; 内缩 framePad 让出外框)
--*********************************************************
function UIButtonsPanel:openPanel(id)
    if #self.tiles <= 0 then return end

    local panelById = self.tiles[id or 1].panelTag;
    self.currentTabID = id;

    if self.currentPanel ~= nil then
        self.currentPanel:setVisible(false);
        EtherMain.instance:removeChild(self.currentPanel)
    end

    local panel = panelById:new(EtherTheme.framePad + self.width, EtherTheme.headerH, self.parent.width - self.width - EtherTheme.framePad * 2, self.parent.height - EtherTheme.headerH - EtherTheme.framePad);
    panel:initialise();
    panel:instantiate();
    panel:setVisible(true);
    self.parent:addChild(panel);

    self.currentPanel = panel;
    EtherMain.currentTabID = id;
end

--*********************************************************
--* 添加磁贴: 图标纹理 + 文字翻译键 + 对应内容面板类
--*********************************************************
function UIButtonsPanel:addButton(iconPath, labelKey, panelTag)
    local id = #self.tiles + 1;
    table.insert(self.tiles, {
        id = id,
        iconTexture = getExtraTexture(iconPath),
        labelKey = labelKey,
        panelTag = panelTag,
        y = TOP + (id - 1) * TILE_H,
    });
end

--*********************************************************
--* 创建新实例
--*********************************************************
function UIButtonsPanel:new(posX, posY, width, height, parent, accentColor)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
    menuTableData.backgroundColor = EtherTheme.railBG;
    menuTableData.borderColor = EtherTheme.bloodDim;
    menuTableData.moveWithMouse = false;   -- 导航条不拖窗(拖窗由页眉负责), 点击只切页
    self.__index = self;

    self.parent = parent;
    self.tiles = {};
    self.currentTabID = 1;
    self.currentPanel = nil;

    return menuTableData;
end
