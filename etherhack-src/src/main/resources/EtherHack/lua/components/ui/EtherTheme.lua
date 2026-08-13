--*********************************************************
--* EtherTheme: RE2 重制风 + 模拟毛玻璃 主题(仅外观, 零逻辑)
--*********************************************************

EtherTheme = {}

-- 调色板 (生化危机绿: 深绿铬色, 用户可配置的 accentColor 保持为高亮)
EtherTheme.glassBG       = { r = 0.02, g = 0.02, b = 0.02, a = 0.78 }  -- 窗口玻璃底
EtherTheme.railBG        = { r = 0.03, g = 0.05, b = 0.035, a = 0.88 } -- 导航条底(暗绿)
EtherTheme.blood         = { r = 0.10, g = 0.52, b = 0.22, a = 1 }     -- 生化危机绿(标题条/激活条)
EtherTheme.bloodDim      = { r = 0.10, g = 0.52, b = 0.22, a = 0.35 }  -- 淡化绿(网格/边框)
EtherTheme.edge          = { r = 0.75, g = 0.92, b = 0.82, a = 0.16 }  -- 顶部高光(微绿)
EtherTheme.text          = { r = 0.93, g = 0.92, b = 0.88, a = 1 }     -- 米白正文(提亮)
EtherTheme.textDim       = { r = 0.75, g = 0.74, b = 0.71, a = 1 }     -- 次要文字(提亮)
EtherTheme.titleH        = 20                                           -- 标题条高

EtherTheme.noise         = nil
EtherTheme.closeTex      = nil
EtherTheme.fontHgtSmall  = getTextManager():getFontHeight(UIFont.Small)
EtherTheme.fontHgtMedium = getTextManager():getFontHeight(UIFont.Medium)
EtherTheme.fontHgtLarge  = getTextManager():getFontHeight(UIFont.Large)

--*********************************************************
--* 纹理懒加载 (Kahlua 无元表方法, 用显式函数)
--*********************************************************
function EtherTheme.getNoise()
    if EtherTheme.noise == nil then
        EtherTheme.noise = getExtraTexture("EtherHack/media/ui/noise.png");
    end
    return EtherTheme.noise;
end

function EtherTheme.getCloseTexture()
    if EtherTheme.closeTex == nil then
        EtherTheme.closeTex = getExtraTexture("EtherHack/media/ui/close_re.png");
    end
    return EtherTheme.closeTex;
end

--*********************************************************
--* 玻璃底 + 噪点 + 顶部高光 + 血红描边 (整窗背景)
--*********************************************************
function EtherTheme.drawGlass(parent)
    local g = EtherTheme.glassBG;
    parent:drawRect(0, 0, parent.width, parent.height, g.a, g.r, g.g, g.b);
    parent:drawTextureScaled(EtherTheme.getNoise(), 0, 0, parent.width, parent.height, 1, 1, 1, 1);
    parent:drawRect(0, 0, parent.width, 1, EtherTheme.edge.a, EtherTheme.edge.r, EtherTheme.edge.g, EtherTheme.edge.b);
    parent:drawRectBorder(0, 0, parent.width, parent.height, 0.8, EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b);
end

--*********************************************************
--* 血红标题条 (浮动窗/弹窗用)
--*********************************************************
function EtherTheme.drawTitleBar(parent, title, font)
    local b = EtherTheme.blood;
    local f = font or UIFont.Small;
    local fh = f == UIFont.Medium and EtherTheme.fontHgtMedium or EtherTheme.fontHgtSmall;
    parent:drawRect(0, 0, parent.width, EtherTheme.titleH, 0.92, b.r, b.g, b.b);
    parent:drawRect(0, EtherTheme.titleH, parent.width, 1, 0.5, 0.03, 0.18, 0.08);
    parent:drawTextCentre(title, parent.width / 2, EtherTheme.titleH / 2 - fh / 2, 1, 1, 1, 1, f);
end

--*********************************************************
--* 列表样式统一 (ISScrollingListBox 实例)
--*********************************************************
function EtherTheme.styleList(list)
    list.backgroundColor = { r = 0.02, g = 0.02, b = 0.02, a = 0.35 };
    list.listHeaderColor = { r = 0.12, g = 0.05, b = 0.05, a = 1 };
    list.borderColor = EtherTheme.bloodDim;
    list.drawBorder = true;
end

--*********************************************************
--* 表格行底: 选中(accent 加深) / 隔行玻璃染色
--*********************************************************
function EtherTheme.drawRowUnderlay(self, y, selected, alt, height)
    local a = EtherMain.accentColor;
    if selected then
        self:drawRect(0, y, self:getWidth(), height, 0.35, a.r, a.g, a.b);
    elseif alt then
        self:drawRect(0, y, self:getWidth(), height, 0.08, 0.12, 0.11, 0.11);
    end
    self:drawRectBorder(0, y, self:getWidth(), height, 0.5, EtherTheme.bloodDim.r, EtherTheme.bloodDim.g, EtherTheme.bloodDim.b);
end

--*********************************************************
--* 表格列分隔竖线 (1px, 淡化血红)
--*********************************************************
function EtherTheme.drawColumnLines(self, y, height)
    for i = 2, #self.columns do
        self:drawRectBorder(self.columns[i].size, y, 1, height, 0.5, EtherTheme.bloodDim.r, EtherTheme.bloodDim.g, EtherTheme.bloodDim.b)
    end
end