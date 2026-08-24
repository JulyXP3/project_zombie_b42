# -*- coding: utf-8 -*-
"""布局溢出审计: 用真实 Lua 源码跑 createChildren, 记录所有控件/分组盒矩形,
断言没有任何元素越出面板或其所属分组盒。

针对的 bug 类型 (已经咬过两次):
  先按最小高度定下各行 Y, 之后在分支里把盒子加高/上移, 但各行不动 -> 内容溢出。
覆盖: EtherTrapSpawn (搜索盒 + 列表 + 底部块), UIItemTables (列表 + 底部控制块)。
在多组字体度量 / 多种面板尺寸 / 多种标签长度下反复验证。
"""
import sys

import os

import lupa

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

LUA = (r'D:\JavaProject\project_zombies_analysis\etherhack-src\src\main'
       r'\resources\EtherHack\lua')
OLD = os.environ.get('OLD_PANELS')


def psrc(rel):
    """面板源码; 设了 OLD_PANELS 就从那里读同名文件(用于验证审计对旧 bug 敏感)。"""
    if OLD:
        cand = os.path.join(OLD, os.path.basename(rel))
        if os.path.exists(cand):
            return open(cand, encoding='utf-8-sig').read()
    return open(LUA + rel, encoding='utf-8').read()

fails = []


def check(name, cond, detail=''):
    print('%-62s %s %s' % (name, 'OK' if cond else 'FAIL', detail))
    if not cond:
        fails.append(name)


PRELUDE = r'''
UIFont = { Small=1, Medium=2, Large=3, NewSmall=4, Title=5 }
FH, MF, CW, MSY = %d, %d, %d, %d
LABEL_MUL = %f

function getTextManager()
  return {
    getFontHeight  = function(self,f) return FH end,
    MeasureFont    = function(self,f) return MF end,
    MeasureStringX = function(self,f,s)
        return math.floor(#tostring(s) * CW * LABEL_MUL) end,
    MeasureStringY = function(self,f,s) return MSY end,
    -- 缩放绘制说明文字 (EtherTheme.drawHintText); 审计只跑 createChildren,
    -- 这里只是兜底防 render 路径意外被调用时崩掉
    DrawString     = function() end,
  }
end
function getExtraTexture(p) return { p = p,
    getWidth=function() return 12 end, getHeightOrig=function() return 12 end,
    getWidthOrig=function() return 12 end, getHeight=function() return 12 end } end
function getLanguage() return "EN" end
function require(x) end
function getTranslate(k) return k end
function getTranslateText(k) return k end
function tr(k, p) return k end
function getTimestampMs() return 0 end
function getSpecificPlayer(i) return PLAYER end
function getPlayer() return PLAYER end
function checkStringPattern(s) return true end
function spawnItem(a,b) end
function print(...) end
function instanceof(o,c) return false end
table.insert = table.insert
string.sort = function(a,b) return a < b end

RECT = {}   -- 所有被创建的控件矩形
GROUP = {}  -- 所有分组盒矩形

local function rec(kind, o)
  table.insert(RECT, { kind=kind, x=o.x, y=o.y, w=o.width, h=o.height })
end

ISUIElement = {}
function ISUIElement:new(x,y,w,h)
  local o = { x=x or 0, y=y or 0, width=w or 0, height=h or 0, children={},
              javaObject=nil, anchorLeft=true }
  setmetatable(o, self); self.__index = self; return o
end
function ISUIElement:derive(n)
  local o={}; setmetatable(o,self); self.__index=self; o.Type=n; return o
end
function ISUIElement:initialise() end
function ISUIElement:instantiate() end
function ISUIElement:addChild(c) table.insert(self.children, c) end
function ISUIElement:setWidth(w) self.width=w end
function ISUIElement:setHeight(h) self.height=h end
function ISUIElement:setX(x) self.x=x end
function ISUIElement:setY(y) self.y=y end
function ISUIElement:getWidth() return self.width end
function ISUIElement:getHeight() return self.height end
function ISUIElement:getYScroll() return 0 end
function ISUIElement:getXScroll() return 0 end
function ISUIElement:getAbsoluteX() return self.x end
function ISUIElement:getAbsoluteY() return self.y end
function ISUIElement:getScrollHeight() return 0 end
function ISUIElement:setScrollChildren(b) end
function ISUIElement:setAnchorLeft(b) end
function ISUIElement:setAnchorRight(b) end
function ISUIElement:setAnchorTop(b) end
function ISUIElement:setAnchorBottom(b) end
function ISUIElement:setVisible(b) self.visible=b end
function ISUIElement:setClearButton(b) end
function ISUIElement:setStencilRect() end
function ISUIElement:clearStencilRect() end
function ISUIElement:drawRect() end
function ISUIElement:drawRectBorder() end
function ISUIElement:drawText() end
function ISUIElement:drawTextCentre() end
function ISUIElement:drawTextRight() end
function ISUIElement:drawTexture() end
function ISUIElement:drawTextureScaled() end
function ISUIElement:drawTextureScaledColor() end
function ISUIElement:drawTextureScaledAspect2() end
function ISUIElement:isMouseOver() return false end
function ISUIElement:getMouseX() return 0 end
function ISUIElement:getMouseY() return 0 end
function ISUIElement:getInternalText() return "" end

ISPanel = ISUIElement:derive("ISPanel")
function ISPanel:new(x,y,w,h) return ISUIElement.new(self,x,y,w,h) end
function ISPanel:createChildren() end
function ISPanel:prerender() end
function ISPanel:render() end

ISLabel = ISUIElement:derive("ISLabel")
function ISLabel:new(x,y,h,name,r,g,b,a,font,bLeft)
  local o = ISUIElement.new(self,x,y,0,h)
  o.name=name; o.font=font; o.height=h
  o.width = getTextManager():MeasureStringX(font, name)
  if bLeft ~= true then o.x = o.x - o.width end
  if o.height <= 0 then o.height = getTextManager():getFontHeight(font) end
  return o
end
-- 与 PZ ISLabel:prerender (ISLabel.lua:70-92) 逐字一致: 居中量取
-- math.max(MeasureFont(font), MeasureStringY(font, txt)), 不是只用 MeasureFont。
function ISLabel:textDrawY()
  local h  = getTextManager():MeasureFont(self.font)
  local h2 = getTextManager():MeasureStringY(self.font, self.name)
  if h2 > h then h = h2 end
  return (self.height / 2) - (h / 2)
end

ISTextEntryBox = ISUIElement:derive("ISTextEntryBox")
function ISTextEntryBox:new(txt,x,y,w,h)
  local o = ISUIElement.new(self,x,y,w,h); o.txt=txt
  rec("entry", o); return o
end

ISScrollingListBox = ISUIElement:derive("ISScrollingListBox")
function ISScrollingListBox:new(x,y,w,h)
  local o = ISUIElement.new(self,x,y,w,h)
  o.items={}; o.columns={}; o.itemheight=1; o.selected=0
  rec("list", o); return o
end
function ISScrollingListBox:addColumn(n,s) table.insert(self.columns,{name=n,size=s}) end
function ISScrollingListBox:clear() self.items={} end
function ISScrollingListBox:addItem(a,b) table.insert(self.items,{text=a,item=b}); return self.items[#self.items] end
function ISScrollingListBox:size() return #self.items end

ISButton = ISUIElement:derive("ISButton")
function ISButton:new(x,y,w,h) return ISUIElement.new(self,x,y,w,h) end

-- 被面板引用的作弊模块 (只需存在, 不需要行为)
EtherTrapPOC   = { armed=false, target=nil, count=0, phase="idle",
                   setTarget=function() end, trigger=function() end }
EtherItemSearch = { scan=function() return 0 end }
EtherContainerPOC = { radius=10, reset=function() end }
EtherAmmoFarm  = {}
EtherMain = { accentColor={r=0,g=1,b=1} }
PLAYER = { getX=function() return 0 end, getY=function() return 0 end,
           getZ=function() return 0 end }
'''


def build(fh=15, mf=13, cw=7, label_mul=1.0, msy=19):
    rt = lupa.LuaRuntime(unpack_returned_tuples=True)
    rt.execute(PRELUDE % (fh, mf, cw, msy, label_mul))
    for rel in (r'\components\ui\EtherTheme.lua',
                r'\components\ui\UIButton.lua'):
        rt.execute(open(LUA + rel, encoding='utf-8').read())
    return rt


def rects(rt):
    out = []
    R = rt.globals().RECT
    i = 1
    while True:
        v = R[i]
        if v is None:
            break
        out.append((v['kind'], float(v['x']), float(v['y']),
                    float(v['w']), float(v['h'])))
        i += 1
    return out


def audit(tag, W, H, elems, groups, pad):
    """elems/groups: [(name,x,y,w,h)]; 断言都在面板内, 且元素在所属盒内。"""
    ok = True
    for n, x, y, w, h in elems:
        if x < 0 or y < 0 or x + w > W or y + h > H:
            check('  %s: %s 越出面板 (0,0,%d,%d)' % (tag, n, W, H), False,
                  'rect=(%.0f,%.0f,%.0f,%.0f) right=%.0f bottom=%.0f'
                  % (x, y, w, h, x + w, y + h))
            ok = False
    for gn, gx, gy, gw, gh in groups:
        if gx < 0 or gy < 0 or gx + gw > W or gy + gh > H:
            check('  %s: 盒 %s 越出面板' % (tag, gn), False,
                  'rect=(%.0f,%.0f,%.0f,%.0f)' % (gx, gy, gw, gh))
            ok = False
        for n, x, y, w, h in elems:
            cx, cy = x + w / 2.0, y + h / 2.0
            inside = gx <= cx <= gx + gw and gy <= cy <= gy + gh
            if inside and (x < gx or y < gy or x + w > gx + gw
                           or y + h > gy + gh):
                check('  %s: %s 越出盒 %s' % (tag, n, gn), False,
                      'el=(%.0f,%.0f,%.0f,%.0f) box=(%.0f,%.0f,%.0f,%.0f)'
                      % (x, y, w, h, gx, gy, gw, gh))
                ok = False
    return ok


# ---------------- EtherTrapSpawn ----------------
print('=== EtherTrapSpawn: 搜索盒 / 列表 / 底部块 ===')
for fh, mf, cw, mul, W, H in [
        (15, 19, 7, 1.0, 702, 700), (15, 19, 7, 1.0, 470, 480),
        (19, 24, 9, 1.0, 702, 700), (24, 31, 11, 1.0, 702, 700),
        (15, 19, 7, 3.2, 702, 700),   # 超长翻译(RU/CN)
        (15, 19, 7, 5.0, 470, 480),   # 超长翻译 + 窄面板
]:
    rt = build(fh, mf, cw, mul)
    rt.execute(psrc(r'\components\panels\EtherTrapSpawn.lua'))
    rt.execute('''
P = EtherTrapSpawn:new(0, 0, %d, %d)
P.localPlayer = PLAYER
P.buttonList = {}
P.fullList = {}
P.initList = function(self) end
P:createChildren()
''' % (W, H))
    P = rt.globals().P
    els = [('%s#%d' % (k, i + 1), x, y, w, h)
           for i, (k, x, y, w, h) in enumerate(rects(rt))]
    # 按钮也要审计
    ch = P['children']
    i = 1
    while True:
        c = ch[i]
        if c is None:
            break
        if c['title'] is not None:
            els.append(('btn:%s' % c['title'], float(c['x']), float(c['y']),
                        float(c['width']), float(c['height'])))
        i += 1
    G = P['groups']
    grps = []
    i = 1
    while True:
        g = G[i]
        if g is None:
            break
        grps.append(('g%d' % i, float(g['x']), float(g['y']),
                     float(g['w']), float(g['h'])))
        i += 1
    tag = 'fh%d W%d mul%.1f' % (fh, W, mul)
    if audit(tag, float(P['width']), float(P['height']), els, grps, 16):
        check('  %s: 全部元素在面板与盒内 (%d 元素 / %d 盒)'
              % (tag, len(els), len(grps)), True)

# ---------------- UIItemTables ----------------
print('')
print('=== UIItemTables: 列表 / 底部控制块 (计数+按钮+过滤行) ===')
for fh, mf, cw, mul, W, H in [
        (15, 19, 7, 1.0, 690, 660), (15, 19, 7, 1.0, 460, 470),
        (19, 24, 9, 1.0, 690, 660), (24, 31, 11, 1.0, 690, 660),
        (15, 19, 7, 2.6, 690, 660),   # 长翻译 -> 地图按钮换行
        (15, 19, 7, 4.0, 460, 470),   # 更长 -> 过滤行也拆两行
]:
    rt = build(fh, mf, cw, mul)
    rt.execute(psrc(r'\components\ui\UIItemTables.lua'))
    rt.execute('''
-- UIItemTables:new 用的是 setmetatable(o, self) 且不设 self.__index = self。
-- 标准 Lua 下实例查不到方法, Kahlua 的 __index 查找会沿元表链继续所以游戏里能跑。
-- 测试台按标准 Lua 运行, 故显式补上。
UIItemTables.__index = UIItemTables
P = UIItemTables:new(0, 0, %d, %d)
P.updatePanel = function(self) end
P:createChildren()
''' % (W, H))
    P = rt.globals().P
    els = [('%s#%d' % (k, i + 1), x, y, w, h)
           for i, (k, x, y, w, h) in enumerate(rects(rt))]
    ch = P['children']
    i = 1
    while True:
        c = ch[i]
        if c is None:
            break
        if c['title'] is not None:
            els.append(('btn:%s' % c['title'], float(c['x']), float(c['y']),
                        float(c['width']), float(c['height'])))
        i += 1
    cb = P['ctrlBox']
    grps = [('ctrlBox', float(cb['x']), float(cb['y']),
             float(cb['w']), float(cb['h']))]
    tag = 'fh%d W%d mul%.1f' % (fh, W, mul)
    okk = audit(tag, float(P['width']), float(P['height']), els, grps, 12)
    # 过滤输入框必须在 ctrlBox 内, 且底部留出内边距
    fw = P['filterWidgets']
    worst = 0
    j = 1
    while True:
        f = fw[j]
        if f is None:
            break
        worst = max(worst, float(f['y']) + float(f['height']))
        j += 1
    limit = float(cb['y']) + float(cb['h'])
    if not (worst <= limit):
        check('  %s: 过滤行底 %.0f <= 控制盒底 %.0f' % (tag, worst, limit), False)
        okk = False
    if okk:
        check('  %s: 全部元素在面板与控制盒内 (%d 元素)' % (tag, len(els)), True)

print('')
print('失败数: %d' % len(fails))
for f in fails:
    print('  - ' + f)

sys.exit(1 if fails else 0)
