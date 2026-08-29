--*********************************************************
--* EtherTheme: 薄荷青霓虹 主题(仅外观, 零逻辑) — 配色对齐参考图实测值
--*********************************************************

EtherTheme = {}

-- 调色板 (参考图实测: 近黑青底 #0E1213 + 薄荷青 #48D8A8 主强调 + 天蓝 #63C7F8 数值 + 银轨 #949C9D)
EtherTheme.glassBG       = { r = 0.055, g = 0.070, b = 0.075, a = 0.94 }  -- 窗口底(近黑青, 参考图实测)
EtherTheme.railBG        = { r = 0.075, g = 0.115, b = 0.115, a = 0.94 }  -- 导航条底(暗石板)
EtherTheme.listHeaderBG  = { r = 0.08,  g = 0.15,  b = 0.15,  a = 1 }     -- 列表表头底(暗青; 取代 ISScrollingListBox 默认暗红)
EtherTheme.blood         = { r = 0.28,  g = 0.85,  b = 0.66,  a = 1 }     -- 薄荷青(参考图拇指实测 #48D8A8)
EtherTheme.bloodDim      = { r = 0.28,  g = 0.85,  b = 0.66,  a = 0.38 }  -- 淡薄荷(网格/边框)
EtherTheme.edge          = { r = 0.75,  g = 0.95,  b = 0.92,  a = 0.22 }  -- 顶部高光(冷白微青)
EtherTheme.text          = { r = 0.90,  g = 0.98,  b = 0.97,  a = 1 }     -- 正文(冷白)
EtherTheme.textDim       = { r = 0.55,  g = 0.72,  b = 0.73,  a = 1 }     -- 次要文字(中性青灰)
EtherTheme.sky           = { r = 0.39,  g = 0.78,  b = 0.97,  a = 1 }     -- 数值/强调文字(参考图 #63C7F8)
EtherTheme.trackSilver   = { r = 0.58,  g = 0.61,  b = 0.62,  a = 1 }     -- 拉条轨道银(参考图 #949C9D)
-- 状态色板 (仅选中/警示用, 方案 §2): 安全绿 / 信息蓝 / 受限琥珀 / 危险红
EtherTheme.statusGreen   = { r = 0.30, g = 0.90, b = 0.45, a = 1 }
EtherTheme.statusBlue    = { r = 0.30, g = 0.70, b = 1.00, a = 1 }
EtherTheme.statusAmber   = { r = 1.00, g = 0.75, b = 0.25, a = 1 }
EtherTheme.statusRed     = { r = 1.00, g = 0.35, b = 0.35, a = 1 }
EtherTheme.titleH        = 20                                           -- 标题条高

-- 纹理缓存 (全部懒加载; 失败时缓存 false 见 lazyTex 的负缓存约定)
EtherTheme.singleTex     = {}                                            -- 单张纹理的负缓存槽位
EtherTheme.fontHgtSmall  = getTextManager():getFontHeight(UIFont.Small)
EtherTheme.fontHgtMedium = getTextManager():getFontHeight(UIFont.Medium)
-- 说明文字/提示段落专用缩放档位:
-- B42 的字体枚举里**没有**比 Small 更小的中文字体 —— fonts.txt 里 CN 的 NewSmall
-- 与 Small 是同一个 .fnt 文件(视觉零变化), 更小的 zomboidNewSmall(13px)/
-- Code(16px)/zomboidNormal1x(14-16px) 全是纯拉丁字形, 拿它们画中文会被
-- AngelCodeFont 直接跳过缺字形(drawString 对 chars[c]==null 是 continue),
-- 中文整行消失。所以说明文字不改枚举, 改用 vanilla 自带的"整串缩放绘制":
--   TextManager:DrawString(font, x, y, scale, str, ...) 内部把 s_scale 同时乘在
--   字形四边形与步进上 (AngelCodeFont$CharDef.draw), 中/英/俄文字形通用。
-- 缩放取 0.7 (约 3 个字号档), 但缩后行高不足 13px 时抬高到 13px —— 13px 是
-- vanilla 自己最小文字档 (zomboidNewSmall) 的行高, 再小汉字就糊了。
EtherTheme.hintFontEnum = UIFont.Small
EtherTheme.hintScale    = math.max(0.70, 13 / EtherTheme.fontHgtSmall)
EtherTheme.fontHgtHint  = math.floor(EtherTheme.fontHgtSmall * EtherTheme.hintScale + 0.5)
-- 页眉标题字体: 收到 Small, 使整条页眉尽量矮
EtherTheme.fontTitle     = UIFont.Small
EtherTheme.fontHgtTitle  = getTextManager():getFontHeight(UIFont.Small)
-- 页眉现在只画一个居中标题 (署名/开源声明已移到"信息"选项卡),
-- 所以高度只需容纳标题一行 + 极紧内边距; HEADER_MAX 是硬上限,
-- 保证任何字体度量下页眉都不会被撑高。
EtherTheme.HEADER_MAX    = 24
EtherTheme.headerH       = math.min(EtherTheme.HEADER_MAX,
                                    EtherTheme.fontHgtTitle + 4)

EtherTheme.framePad      = 6                                            -- 切角外框内缩量(子元素让出边距给外框)

--*********************************************************
--* UTF-8 逐字符切分 (中文无空格, 折行只能按字符; 按字节切会把汉字劈成乱码)
--*********************************************************
function EtherTheme.utf8Chars(s)
    local out = {};
    local i = 1;
    local n = #s;
    while i <= n do
        local c = string.byte(s, i);
        local len = 1;
        if c >= 240 then len = 4;
        elseif c >= 224 then len = 3;
        elseif c >= 192 then len = 2; end
        table.insert(out, string.sub(s, i, i + len - 1));
        i = i + len;
    end
    return out;
end

--*********************************************************
--* 按空格切词 (Kahlua 的 string 库没有 gmatch/gfind, 只能用 find 手写)
--*********************************************************
function EtherTheme.splitWords(s)
    local out = {};
    local start = 1;
    while true do
        local i = string.find(s, " ", start, true);
        if i == nil then
            if start <= #s then table.insert(out, string.sub(s, start)); end
            break;
        end
        if i > start then table.insert(out, string.sub(s, start, i - 1)); end
        start = i + 1;
    end
    return out;
end

--*********************************************************
--* 文本折行 (公共): 先按空格断词(俄/英), 词本身超宽再按字符断(中文)。
--*   text  原文; maxW 可用像素宽; font 字体
--* 返回折好的行数组 (至少一行)。
--* 注: 结果应由调用方缓存, 不要每帧调用 (内部有多次 MeasureStringX)。
--*********************************************************
function EtherTheme.wrapText(text, maxW, font)
    local tm = getTextManager();
    if maxW <= 0 or tm:MeasureStringX(font, text) <= maxW then
        return { text };
    end

    local function wrapByChar(str)
        local lines = {};
        local cur = "";
        local chars = EtherTheme.utf8Chars(str);
        for i = 1, #chars do
            local nextStr = cur .. chars[i];
            if cur ~= "" and tm:MeasureStringX(font, nextStr) > maxW then
                table.insert(lines, cur);
                cur = chars[i];
            else
                cur = nextStr;
            end
        end
        if cur ~= "" then table.insert(lines, cur); end
        return lines;
    end

    if string.find(text, " ", 1, true) == nil then
        return wrapByChar(text);
    end

    local lines = {};
    local cur = "";
    local words = EtherTheme.splitWords(text);
    for w = 1, #words do
        local word = words[w];
        local nextStr = (cur == "") and word or (cur .. " " .. word);
        if cur ~= "" and tm:MeasureStringX(font, nextStr) > maxW then
            table.insert(lines, cur);
            cur = word;
        else
            cur = nextStr;
        end
        if tm:MeasureStringX(font, cur) > maxW then
            local sub = wrapByChar(cur);
            for k = 1, #sub - 1 do table.insert(lines, sub[k]); end
            cur = sub[#sub];
        end
    end
    if cur ~= "" then table.insert(lines, cur); end
    return lines;
end

--*********************************************************
--* 缩放绘制一行说明文字 (drawHintText) 与其宽度测量 (hintWidth)。
--*
--* vanilla 的 UIElement:drawText 没有 scale 参数; 带 scale 的只有
--* TextManager:DrawString(font, x, y, scale, str, r,g,b,a) —— 它画在
--* **屏幕绝对坐标**上, 这条调用链与 UIElement.DrawTextUntrimmed 内部完全
--* 相同 (x + getAbsoluteX() + xScroll), 所以这里手动换算绝对坐标。
--* vanilla Lua 先例: media/lua/client/erosion/debug/DebugDemoTime.lua
--* 直接 tm:DrawString(UIFont.Small, px, py, str, r,g,b,a)。
--*
--* 注意: 折行/居中不能用未缩放的 MeasureStringX, 必须用 hintWidth
--* (乘过 hintScale), 行距用 fontHgtHint, 三者与 drawHintText 同一套缩放。
--*********************************************************
function EtherTheme.drawHintText(parent, text, x, y, col, a)
    getTextManager():DrawString(EtherTheme.hintFontEnum,
        x + parent:getAbsoluteX() + parent:getXScroll(),
        y + parent:getAbsoluteY() + parent:getYScroll(),
        EtherTheme.hintScale, text, col.r, col.g, col.b, a or col.a or 1);
end

-- hintWidth 记忆化: 每帧逐行调用 (标签侧为静态文本), 同串重复测量纯浪费。
-- hintFontEnum/hintScale 仅装载期赋值, 文件重执行时缓存随局部变量一并重建, 无跨档位脏读;
-- 缓存封顶 256 条防动态数值串 (等级/经验等) 无限增长, 清空只影响一次回源测量, 不影响正确性。
local hintWidthCache = {};
local hintWidthCount = 0;
function EtherTheme.hintWidth(text)
    local w = hintWidthCache[text];
    if w ~= nil then return w; end
    w = math.floor(getTextManager():MeasureStringX(EtherTheme.hintFontEnum, text)
                   * EtherTheme.hintScale + 0.5);
    if hintWidthCount >= 256 then
        hintWidthCache = {};
        hintWidthCount = 0;
    end
    hintWidthCache[text] = w;
    hintWidthCount = hintWidthCount + 1;
    return w;
end

-- 说明文字折行: 先把可用宽换算回未缩放像素再折 (wrapText 用未缩宽度测量)。
-- 返回行数组, 行距由调用方用 EtherTheme.fontHgtHint 推进。
function EtherTheme.wrapHint(text, maxW)
    return EtherTheme.wrapText(text, maxW / EtherTheme.hintScale, EtherTheme.hintFontEnum);
end

--*********************************************************
--* 纹理懒加载 (Kahlua 无元表方法, 用显式函数)
--*
--* 负缓存约定: 加载失败时写入 false 而不是留 nil。
--*   否则 "== nil 才加载" 的判断每帧都成立 -> 缺文件时会每帧、每个盒子、
--*   每个按钮重复尝试加载并重复拼接路径 (切角绘制是最热的每帧路径)。
--*   取值时统一把 false 还原成 nil 返回给调用方。
--*********************************************************

--- 懒加载并缓存一个纹理; 失败时缓存 false 以避免每帧重试。
-- @param cache 缓存表
-- @param k     缓存键
-- @param path  纹理路径
-- @return userdata|nil
local function lazyTex(cache, k, path)
    local v = cache[k];
    if v == nil then
        v = getExtraTexture(path);
        if v == nil then v = false; end
        cache[k] = v;
    end
    if v == false then return nil; end
    return v;
end

EtherTheme.singleTex = {};   -- 单张纹理的负缓存槽位

function EtherTheme.getNoise()
    return lazyTex(EtherTheme.singleTex, "noise", "EtherHack/media/ui/noise.png");
end

function EtherTheme.getCloseTexture()
    return lazyTex(EtherTheme.singleTex, "close", "EtherHack/media/ui/close_re.png");
end

--*********************************************************
--* 切角图集 (chamfer_atlas.png): 全部皮肤小纹理(角线/楔形/
--* 外框角/扫描线)合成一张, 经 vanilla ISUIElement:drawSubTexture
--* 画子区域 — 整个皮肤共享一次纹理绑定, 消除原来每个切角盒
--* 8 次不同小纹理的切换开销。
--* 布局与 tools/gen_cyber_textures.py 的 compose_chamfer_atlas
--* 一一对应, 改任一侧必须同步另一侧:
--*   y=0   tile_corner tl/tr/bl/br  (x=0/16/32/48,  内容 12px)
--*   y=16  tile_wedge  tl/tr/bl/br  (x=0/16/32/48,  内容 12px)
--*   y=32  frame_corner tl/tr/bl/br (x=0/36/72/108, 内容 32px)
--*   y=68  scanlines 128x128        (x=0)
--*********************************************************
EtherTheme.ATLAS_TILE  = 12;   -- tile 角线/楔形单元内容尺寸
EtherTheme.ATLAS_FRAME = 32;   -- 外框角单元内容尺寸
EtherTheme.ATLAS_SCAN_Y = 68;  -- 扫描线单元 y (128x128, x=0)

EtherTheme.atlasCorner = { tl = 0,  tr = 16, bl = 32, br = 48 };   -- tile 角线 x (y=0)
EtherTheme.atlasWedge  = { tl = 0,  tr = 16, bl = 32, br = 48 };   -- 楔形 x (y=16)
EtherTheme.atlasFrame  = { tl = 0,  tr = 36, bl = 72, br = 108 };  -- 外框角 x (y=32)

function EtherTheme.getAtlas()
    return lazyTex(EtherTheme.singleTex, "atlas", "EtherHack/media/ui/chamfer_atlas.png");
end

--*********************************************************
--* 玻璃底 + 噪点 + 顶部高光 + 血红描边 (整窗背景)
--*********************************************************
function EtherTheme.drawGlass(parent)
    local g = EtherTheme.glassBG;
    parent:drawRect(0, 0, parent.width, parent.height, g.a, g.r, g.g, g.b);
    parent:drawTextureScaled(EtherTheme.getNoise(), 0, 0, parent.width, parent.height, 0.15, 1, 1, 1);
    parent:drawRect(0, 0, parent.width, 1, EtherTheme.edge.a, EtherTheme.edge.r, EtherTheme.edge.g, EtherTheme.edge.b);
    parent:drawRectBorder(0, 0, parent.width, parent.height, 0.8, EtherTheme.blood.r, EtherTheme.blood.g, EtherTheme.blood.b);
end

--*********************************************************
--* 切角(八边形)青边框 (方案 §3 drawFrame, 9宫格式): 四角纹理 + 四边直线。
--* 角纹理已烘焙白核+青辉光, 以 (1,1,1) 绘制不叠色保留霓虹; 直边用 blood 青。
--* 供外壳整窗调用 (磁贴/行盒请用 drawTileBox, 那里是无辉光细线切角)。
--*********************************************************
function EtherTheme.drawFrame(parent, x, y, w, h)
    local b = EtherTheme.blood;
    local cs = 18;                    -- 屏上角尺寸
    local inset = 4 * cs / 32;        -- 角纹理内线相对边缘的内收 (纹理内 4px)
    local atlas = EtherTheme.getAtlas();
    if atlas ~= nil then
        local n = EtherTheme.ATLAS_FRAME;
        parent:drawSubTexture(atlas, EtherTheme.atlasFrame.tl, 32, n, n, x,          y,          cs, cs, 1, 1, 1, 1);
        parent:drawSubTexture(atlas, EtherTheme.atlasFrame.tr, 32, n, n, x + w - cs, y,          cs, cs, 1, 1, 1, 1);
        parent:drawSubTexture(atlas, EtherTheme.atlasFrame.bl, 32, n, n, x,          y + h - cs, cs, cs, 1, 1, 1, 1);
        parent:drawSubTexture(atlas, EtherTheme.atlasFrame.br, 32, n, n, x + w - cs, y + h - cs, cs, cs, 1, 1, 1, 1);
    end
    parent:drawRect(x + cs,            y + inset,          w - 2 * cs, 1, b.a, b.r, b.g, b.b);  -- 上边
    parent:drawRect(x + cs,            y + h - inset - 1,  w - 2 * cs, 1, b.a, b.r, b.g, b.b);  -- 下边
    parent:drawRect(x + inset,         y + cs,             1, h - 2 * cs, b.a, b.r, b.g, b.b);  -- 左边
    parent:drawRect(x + w - inset - 1, y + cs,             1, h - 2 * cs, b.a, b.r, b.g, b.b);  -- 右边
end

--*********************************************************
--* 磁贴盒 (严格对齐概念图 cyber-A-refined 的导航磁贴):
--*   细净等宽青描边的八边形(明显切角) + 轻底; 激活项额外加左侧粗亮青竖条,
--*   并叠一层内描边模拟概念图激活磁贴的"较粗/发光"观感。
--* 与整窗 drawFrame 的区别: 这里用 tile_corner_*(纯 45° 细线, 无辉光), 不用
--* frame_corner_*(32px 白核+青辉光) —— 后者缩到磁贴尺寸会糊成光斑, 正是
--* 旧实现与概念图不像的原因。
--*
--*   active  真=激活态(亮描边+内描边+轻底+左粗竖条), 假=普通态(暗描边+极淡底)
--*   cs      切角尺寸, 省略时用 EtherTheme.tileCS (导航磁贴 12; 功能行盒传 8)
--* 导航磁贴与功能行盒共用本函数, 保证"边界感"风格完全一致。
--*********************************************************
EtherTheme.tileCS = 12;   -- 磁贴切角尺寸 (= tile_corner_*.png 原生 12px, 1:1 最锐利)

function EtherTheme.drawTileBox(parent, x, y, w, h, active, cs)
    local b = EtherTheme.blood;
    cs = cs or EtherTheme.tileCS;
    -- 盒子过小时退化为普通细边框, 避免切角互相穿插
    if w < cs * 2 + 2 or h < cs * 2 + 2 then
        parent:drawRectBorder(x, y, w, h, active and 0.95 or 0.40, b.r, b.g, b.b);
        return;
    end

    if active then
        -- 激活: 较实的青底 (概念图激活磁贴是明显的深青填充, 不是几乎透明的底)
        EtherTheme.fillChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.22, cs);
        EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 1.0, cs);
        -- 选中指示: 紧贴左边框内侧的粗亮青条, 上下与直边段对齐
        -- (旧实现是浮在中间、上下超出切角的细条, 观感突兀)
        parent:drawRect(x + 1, y + cs, 4, h - 2 * cs, 1.0, b.r, b.g, b.b);
    else
        EtherTheme.fillChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.05, cs);
        EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.40, cs);
    end
end

--*********************************************************
--* 统一控件尺寸标记 (全项目按钮/输入框共用, 保证各面板观感一致)
--*********************************************************
EtherTheme.ctrlH      = 24    -- 按钮统一高度
EtherTheme.ctrlPadX   = 14    -- 按钮文字左右内边距 (每侧)
EtherTheme.ctrlCS     = 6     -- 按钮/输入框切角尺寸 (比磁贴小, 适配 24px 高)
EtherTheme.ctrlGap    = 8     -- 同组控件间距

-- ISTextEntryBox 的真实最小渲染高度 (反编译 zombie.ui.UITextBox2 确认):
-- update() 每帧检查 lineHeight + 2*EdgeSize(5) > height, 不足就把框撑高,
-- 且框会离开代码给的 y/height (实机实测 ±10px 漂移, 顶穿分组盒/压到
-- 相邻行 —— 即反复出现的"输入框超出背景框")。所以输入框一律按 entryH
-- 创建、输入行一律按 entryH 留位, 让自动撑高永不触发, 渲染即所见。
-- 与输入框同排的按钮仍是 ctrlH, 用 entryBtnDY 在 entryH 行内竖直居中。
EtherTheme.entryH       = EtherTheme.fontHgtSmall + 10   -- = lineHeight + 2*inset(5)
EtherTheme.entryBtnDY   = math.floor((EtherTheme.entryH - EtherTheme.ctrlH) / 2)
EtherTheme.entryLabelDY = math.floor((EtherTheme.entryH - EtherTheme.fontHgtSmall) / 2)

-- 列表行高 / 表头预留 (单一来源, 必须成对使用)
--
-- 为什么要绑在一起 (踩过的坑):
--   ISScrollingListBox:prerender 把列头画在 drawRect(0, 0 - self.itemheight, ...),
--   即"列表 y 之上、自身边界之外", 高度恰好等于 itemheight。
--   所以把带列头的列表放进行盒/面板时, 必须在列表上方预留 **itemheight** 那么多空间。
--   过去这里写死 22 而各表格用 fontHgtSmall + 8 推导 itemheight, 一旦游戏字体偏大
--   (itemheight > 22), 表头就会向上溢出, 盖住上一行的功能模块标题(用户反馈的
--   "Character Traits / Config list / 搜索行被遮挡")。
EtherTheme.listItemPadY = 4;
EtherTheme.listItemH    = EtherTheme.fontHgtSmall + EtherTheme.listItemPadY * 2;
EtherTheme.listHeaderH  = EtherTheme.listItemH;   -- 表头高 == 行高 (PZ 行为)

--*********************************************************
--* 按钮统一皮肤 (全项目 UIButton 共用): 切角青边 + 状态化底色。
--* 与导航磁贴/功能行盒同一套切角语言, 取代原先的直角边框 + 左侧 3px 色块。
--*   state: "normal" | "hover" | "pressed" | "disabled"
--* 返回文字颜色 (r,g,b,a), 由调用方绘制标题, 保证状态与文字色一致。
--*********************************************************
function EtherTheme.drawButton(parent, x, y, w, h, state)
    -- 切角皮肤 (用户定稿回归): 切角薄荷描边 + 状态化底色, 与磁贴/行盒同一语言。
    local b = EtherTheme.blood;
    local cs = EtherTheme.ctrlCS;

    if state == "pressed" then
        EtherTheme.fillChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.85, cs);
        EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 1.0, cs);
        return 0.02, 0.10, 0.10, 1.0;
    elseif state == "disabled" then
        EtherTheme.fillChamfer(parent, x, y, w, h, 0.10, 0.14, 0.15, 0.60, cs);
        EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.22, cs);
        return EtherTheme.textDim.r, EtherTheme.textDim.g, EtherTheme.textDim.b, 0.55;
    elseif state == "hover" then
        EtherTheme.fillChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.26, cs);
        EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 1.0, cs);
        return 1.0, 1.0, 1.0, 1.0;
    end
    EtherTheme.fillChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.12, cs);
    EtherTheme.strokeChamfer(parent, x, y, w, h, b.r, b.g, b.b, 0.70, cs);
    return EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 1.0;
end

--*********************************************************
--* 切角填充: 中段满宽 + 上下两段内缩 + 四角楔形三角贴图补齐。
--* 只用矩形近似 (上下内缩 / 中段满宽) 会在 y=cs 处出现台阶, 底色呈十字外凸,
--* 与八边形描边不吻合; 故四角改用烘焙的楔形纹理精确补齐, 开销恒定 (3 矩形 + 4 贴图)。
--*********************************************************
function EtherTheme.fillChamfer(parent, x, y, w, h, r, g, b, a, cs)
    if w <= 0 or h <= 0 then return; end
    cs = cs or EtherTheme.tileCS;
    -- 图集缺失(资源未打包)时退回纯矩形: 切角盒退化为方角盒, 不白付贴图调用
    local atlas = EtherTheme.getAtlas();
    if w < cs * 2 + 2 or h < cs * 2 + 2 or atlas == nil then
        parent:drawRect(x, y, w, h, a, r, g, b);
        return;
    end
    parent:drawRect(x + cs, y,          w - cs * 2, cs,         a, r, g, b);  -- 顶段(避开上切角)
    parent:drawRect(x,      y + cs,     w,          h - cs * 2, a, r, g, b);  -- 中段(满宽)
    parent:drawRect(x + cs, y + h - cs, w - cs * 2, cs,         a, r, g, b);  -- 底段(避开下切角)
    -- 四角内侧三角 (同一图集, 与描边/外框零纹理切换)
    local n = EtherTheme.ATLAS_TILE;
    parent:drawSubTexture(atlas, EtherTheme.atlasWedge.tl, 16, n, n, x,         y,         cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasWedge.tr, 16, n, n, x + w - cs, y,         cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasWedge.bl, 16, n, n, x,         y + h - cs, cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasWedge.br, 16, n, n, x + w - cs, y + h - cs, cs, cs, a, r, g, b);
end

--*********************************************************
--* 切角描边 (任意颜色/透明度; 复用 tile_corner_* 纹理)
--*********************************************************
function EtherTheme.strokeChamfer(parent, x, y, w, h, r, g, b, a, cs)
    cs = cs or EtherTheme.tileCS;
    -- 图集缺失时退回普通矩形描边 (见 fillChamfer 同样的兜底理由)
    local atlas = EtherTheme.getAtlas();
    if w < cs * 2 + 2 or h < cs * 2 + 2 or atlas == nil then
        parent:drawRectBorder(x, y, w, h, a, r, g, b);
        return;
    end
    local n = EtherTheme.ATLAS_TILE;
    parent:drawSubTexture(atlas, EtherTheme.atlasCorner.tl, 0, n, n, x,         y,         cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasCorner.tr, 0, n, n, x + w - cs, y,         cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasCorner.bl, 0, n, n, x,         y + h - cs, cs, cs, a, r, g, b);
    parent:drawSubTexture(atlas, EtherTheme.atlasCorner.br, 0, n, n, x + w - cs, y + h - cs, cs, cs, a, r, g, b);
    parent:drawRect(x + cs,    y,         w - 2 * cs, 1, a, r, g, b);
    parent:drawRect(x + cs,    y + h - 1, w - 2 * cs, 1, a, r, g, b);
    parent:drawRect(x,         y + cs,    1, h - 2 * cs, a, r, g, b);
    parent:drawRect(x + w - 1, y + cs,    1, h - 2 * cs, a, r, g, b);
end

--*********************************************************
--* 让内嵌列表在自身滚到边界时把滚轮交还父面板。
--*
--* 背景 (踩过的坑): ISScrollingListBox:onMouseWheel 无条件 return true, 事件被吃掉。
--*   于是把表格嵌进可滚动表单页(如"玩家"页的特质/技能表)后, 只要鼠标停在表格上,
--*   外层页面就完全滚不动 —— 而表格自身往往已经到底, 表现为"滚轮没反应"。
--*
--* 修法: 包装 onMouseWheel, 仅当列表自身确实还能滚时才消费事件;
--*   已到顶/到底(或内容不足以滚动)就 return false, 让 PZ 把事件冒泡给父级。
--*********************************************************
function EtherTheme.bubbleWheelAtEdge(list)
    if list == nil then return list; end
    local base = list.onMouseWheel;
    list.onMouseWheel = function(self2, del)
        local canScroll = self2:getScrollHeight() > self2.height;
        if canScroll then
            local y = self2:getYScroll();          -- <= 0, 越小越靠下
            local maxScroll = self2:getScrollHeight() - self2.height;
            -- del < 0 向上滚: 未到顶才消费; del > 0 向下滚: 未到底才消费
            if (del < 0 and y < 0) or (del > 0 and -y < maxScroll) then
                return base(self2, del);
            end
        end
        return false;   -- 交还父面板
    end
    return list;
end

--*********************************************************
--* 全工程唯一的"行内文字垂直基准": 文字块顶 = (rowH - getFontHeight)/2。
--*
--* 为什么是 getFontHeight 而不是 MeasureFont (实机验证过, 别再翻回去):
--*   ISUIElement:drawText 与 drawTextCentre 都直接转发到同一个 Java
--*   DrawText/DrawTextCentre, y 语义相同 = 文字块顶。而 Java 实际使用的
--*   文字块高度是 getFontHeight。改用 MeasureFont 会让所有按钮文字整体上移
--*   (曾经这么改过, 进游戏后每个按钮的文字都错位)。
--*
--* UIButton:render 就是按这个基准画的, 所以本函数是"标签要对齐到的目标"。
--*********************************************************
function EtherTheme.textTopInRow(rowH, font)
    return (rowH - getTextManager():getFontHeight(font or UIFont.Small)) / 2;
end

--*********************************************************
--* 统一标签构造: 让标签文字与同一行的按钮文字**共线**。
--*
--* 坑在这里 (两套居中基准不一致):
--*   ISLabel:prerender (ISLabel.lua:70-92) 的居中量是
--*     local h  = MeasureFont(font)
--*     local h2 = MeasureStringY(font, txt)     -- 处理多行
--*     h = math.max(h, h2)
--*     drawText(txt, 0, (self.height/2) - (h/2), ...)
--*   注意是 **max**, 而且 MeasureStringY 依赖文本内容。
--*   而按钮 (以及 Java DrawText 的实际文字块高) 用的是 getFontHeight。
--*   两者不等, 同一行里标签就会与按钮文字错开。
--*
--* 修法: 不改 ISLabel(它是 vanilla), 而是把它的 y 偏移这个差值, 让它内部的
--*   居中结果正好落在 EtherTheme.textTopInRow 给出的基准上:
--*     y_label + rowH/2 - h/2 == y + (rowH - getFontHeight)/2
--*   => y_label = y + (h - getFontHeight)/2
--*   差值符号无关, 谁大都成立。
--*
--* 必须用与 ISLabel 完全相同的 h (含 MeasureStringY 与 max), 否则补偿量算错:
--*   只按 MeasureFont 补偿时, 长标签(MeasureStringY 更大)会整体上移数像素
--*   —— 正是"设置页颜色行 / Exploit 页各行的说明文字贴在行盒顶部"那次缺陷。
--*
--* 用法: y 传行顶, rowH 传该行控件高度(如 EtherTheme.ctrlH), 不要再手工偏移。
--*   text   显示文本 (已翻译)
--*   rowH   该行控件高度; 文字在这个高度内与同行按钮共线
--*   col    可选 { r, g, b } 颜色, 默认白
--*   font   可选字体, 默认 Small
--*********************************************************
function EtherTheme.makeLabel(x, y, rowH, text, col, font)
    font = font or UIFont.Small;
    local r, g, b = 1, 1, 1;
    if col ~= nil then r, g, b = col.r, col.g, col.b; end
    local tm = getTextManager();
    -- 复刻 ISLabel:prerender 的居中量 h (必须包含 MeasureStringY 与 max)
    local h = tm:MeasureFont(font);
    local h2 = tm:MeasureStringY(font, text);
    if h2 > h then h = h2; end
    local baselineFix = (h - tm:getFontHeight(font)) / 2;
    local lb = ISLabel:new(x, y + baselineFix, rowH, text, r, g, b, 1, font, true);
    lb:initialise();
    return lb;
end

--*********************************************************
--* 输入框统一皮肤: 与拉条同族的"深槽"观感 (参考图输入框 = 深底窄槽 + 青边)。
--* ISTextEntryBox 自绘底与边框, 这里只改它的配色字段, 零行为改动。
--*********************************************************
function EtherTheme.styleEntry(entry)
    if entry == nil then return entry; end
    local b = EtherTheme.blood;
    entry.backgroundColor = { r = 0.03, g = 0.06, b = 0.065, a = 0.95 };  -- 深槽底(比窗口底更深)
    entry.borderColor     = { r = b.r,  g = b.g,  b = b.b,  a = 0.55 };  -- 薄荷细边
    entry.font            = UIFont.Small;
    return entry;
end

--*********************************************************
--* 分区标题 (定稿): Small 薄荷标题 + 居中 + 两侧线条, 如 ---- 标题 ----。
--*   mode "lines": 居中标题 + 两侧线 (标题身处模块盒/分组盒内时用)
--*   mode "box":   切角盒包裹 + 盒内居中标题 + 两侧线 (标题独占一块时用,
--*                 如信息页)
--* 返回标题行高。textW 构建时测好传入可省一次测量 (nil 则现场量)。
--*********************************************************
function EtherTheme.drawSectionTitle(parent, x, y, w, text, textW, mode)
    local b = EtherTheme.blood;
    local dim = EtherTheme.bloodDim;
    local fh = EtherTheme.fontHgtSmall;
    if textW == nil then
        textW = getTextManager():MeasureStringX(UIFont.Small, text);
    end
    local h = fh;
    local textY = y;
    if mode == "box" then
        h = fh + 8;
        EtherTheme.drawTileBox(parent, x, y, w, h, false, 8);
        textY = y + 4;
    end
    local tx = x + (w - textW) / 2;
    local midY = textY + fh / 2;
    parent:drawTextCentre(text, x + w / 2, textY, b.r, b.g, b.b, 1, UIFont.Small);
    local pad = 10;
    if mode == "box" then pad = 14; end
    if tx - x - pad > 2 then
        parent:drawRect(x + (pad - 10), midY, tx - x - pad, 1, 0.45, dim.r, dim.g, dim.b);
    end
    if x + w - (tx + textW + pad) > 2 then
        parent:drawRect(tx + textW + pad, midY, x + w - pad + 10 - (tx + textW + pad), 1, 0.45, dim.r, dim.g, dim.b);
    end
    return h;
end

--*********************************************************
--* 静态扫描线叠层: 平铺纹理低透明度覆盖整窗。
--* 单元在图集内 (与其他皮肤纹理共享同一次绑定), 经 drawSubTexture
--* 拉伸到整窗 — 与原 scanlines.png 独立纹理版视觉完全一致。
--*********************************************************
function EtherTheme.drawScanlines(parent)
    local atlas = EtherTheme.getAtlas();
    if atlas == nil then return; end
    parent:drawSubTexture(atlas, 0, EtherTheme.ATLAS_SCAN_Y, 128, 128,
        0, 0, parent.width, parent.height, 0.5, 1, 1, 1);
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
    list.listHeaderColor = EtherTheme.listHeaderBG;   -- 暗青表头 (原为暗红 0.12,0.05,0.05)
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

--*********************************************************
--* 布局常量 (所有面板共用, 保证行高一致)
--*********************************************************
EtherTheme.rowH       = 50   -- 列表/表单项统一行高
EtherTheme.btnH       = 24   -- 按钮高度
EtherTheme.labelPadY  = 3    -- label 相对行顶的 Y 偏移