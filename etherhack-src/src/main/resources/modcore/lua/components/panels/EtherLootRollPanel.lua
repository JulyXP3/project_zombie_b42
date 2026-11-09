require "ISUI/ISPanel"

--*********************************************************
--* 战利品页: 刷弹药 + 物品生成 (FishingSpawn / CorpseSpawn / BoxPoison / WornSpawn,
--*   仅自建服务器): 全物品列表 + 名称/ID 搜索 + 单次生成 (风格对齐物品生成)
--*   (穿戴生成的换装与掉落必须等服务端确认；代码路径存在不等于联机验收通过)
--*   (重置战利品已下架: b42.22 服务端需要 Capability.AddItem, 普通角色会被
--*    服务端拒绝并可能被服务器反作弊踢出; 计时生成同样下架, 因为 b42.22 的
--*    ISTakeBricks 只产固定 pallet 产物, 无法生成任意物品)
--*********************************************************
EtherLootRollPanel = ISPanel:derive("EtherLootRollPanel");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* 布局登记 (createChildren 期间算好, prerender 统一绘制)
--*   _group 分组盒矩形 (三个功能分组各自成框, 建立边界感)
--*   _text  静态文案 (标题/标签/提示; 集中登记便于按宽度折行, 防各语言溢出)
--* 本面板不滚动 (无 setScrollChildren), 故可直接按内容坐标绘制。
--*********************************************************
function EtherLootRollPanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherLootRollPanel:_text(tx, ty, text, col, font, hint)
    local t = {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text,
        font = font or UIFont.Small,
        header = false,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制。
        -- 不能在 createChildren 里即时画: 此时面板还没挂到父节点, getAbsoluteX
        -- 拿到的是错误坐标, 而且 createChildren 只执行一次, 画完即丢 (实机:
        -- g1 提示消失、render 里逐帧画的钓鱼提示却正常, 即此因)。
        hint = hint or false,
    };
    table.insert(self.texts, t);
    return t;   -- 返回条目: 模式切换时可按 hidden 动态隐藏 (如钓竿模式的数量标签)
end

-- 分区标题: Small 薄荷居中 (标题在功能分组盒内, 与全局标题语言一致)
function EtherLootRollPanel:_header(tx, ty, text, w)
    -- 二百七十七 (L7): 标题宽登记时测一次 — 原 prerender 每帧对 nil 补测 (×3 标题/帧)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        header = true,
        w = w,
        textW = getTextManager():MeasureStringX(UIFont.Small, text),
    });
end

--*********************************************************
--* Обработка prerender
--* 分组盒与静态文案画在子控件之下 (PZ 渲染序: prerender -> 子控件 -> render)
--*********************************************************
function EtherLootRollPanel:prerender()
    self:setStencilRect(0, 10, self:getWidth(), self:getHeight() - 20);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    if self.groups == nil then return end

    -- 清单数量变化时刷新按钮文案 (勾选加入/弹窗删除都会改)
    if self.queueBtn ~= nil then
        local qn = EtherSpawnQueue ~= nil and EtherSpawnQueue.count() or 0;
        if self._queueCountShown ~= qn then
            self._queueCountShown = qn;
            self:updateQueueBtnText();
            -- 清单数量变化会影响开盒模式下「清单」按钮的可用态 (非空才可点去清空),
            -- 以及生成按钮的文案 (生成 / 生成清单 (N))
            self:updateSpawnModeUI();
            self:updateSpawnBtn();
            -- 清单清空后撤掉「请先清空清单」提示, 否则会一直挂在开盒模式的状态行上
            if qn == 0 and EtherBoxPoison ~= nil
                and EtherBoxPoison.message == getTranslate("UI_SpawnQueue_ClearFirst") then
                EtherBoxPoison.message = "";
            end
        end
    end

    for i = 1, #self.groups do
        local g = self.groups[i];
        EtherTheme.drawTileBox(self, g.x, g.y, g.w, g.h, false, 8);
    end
    for i = 1, #self.texts do
        local t = self.texts[i];
        if not t.hidden then
            if t.header then
                EtherTheme.drawSectionTitle(self, t.x, t.y, t.w, t.text, t.textW, "lines");
            elseif t.hint then
                EtherTheme.drawHintText(self, t.text, t.x, t.y, t.col);
            else
                self:drawText(t.text, t.x, t.y, t.col.r, t.col.g, t.col.b, t.col.a or 1, t.font);
            end
        end
    end
    -- 底部说明 (提示/警告) 按当前生成模式二选一绘制
    if self.modeTexts ~= nil then
        for i = 1, #self.modeTexts do
            local t = self.modeTexts[i];
            if t.mode == self.spawnMode then
                EtherTheme.drawHintText(self, t.text, t.x, t.y, t.col);
            end
        end
    end
end

--*********************************************************
--* Обработка render (仅动态状态文字)
--*********************************************************
function EtherLootRollPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    -- 生成状态 (生成中/已生成/失败; 空闲时无消息) —— 按模式取对应状态源,
    -- 长消息按可用宽度折行, 且结果缓存 (render 每帧调用, 不能每帧测量)
    local src = EtherFishSpawn;
    if self.spawnMode == "corpse" then src = EtherCorpseSpawn;
    elseif self.spawnMode == "poison" then src = EtherBoxPoison;
    elseif self.spawnMode == "worn" then src = EtherWornSpawn; end
    local fishStatus = tostring((src and src.message) or "")
    if fishStatus ~= "" and self.statusX ~= nil then
        if self.statusCacheText ~= fishStatus or self.statusCacheW ~= self.statusW then
            self.statusLines = EtherTheme.wrapHint(fishStatus, self.statusW);
            self.statusCacheText = fishStatus;
            self.statusCacheW = self.statusW;
        end
        local td = EtherTheme.textDim;
        -- 多行时整体上移, 使文字块相对按钮竖直居中 (说明文字按 hintScale 缩小绘制)
        local fhH = EtherTheme.fontHgtHint;
        local n = #self.statusLines;
        local y0 = self.statusY - math.floor((n - 1) * fhH / 2);
        for i = 1, n do
            EtherTheme.drawHintText(self, self.statusLines[i], self.statusX,
                y0 + (i - 1) * fhH, td, 0.9);
        end
    end
end

--*********************************************************
--* 生成模式切换 UI 状态: 当前模式按钮禁用 (视觉区分 + 防重复点击)
--*********************************************************
function EtherLootRollPanel:updateSpawnModeUI()
    -- 数量输入框: 只有钓竿生成不用数量 (EtherFishSpawn.trigger 无数量参数, 一次抛竿
    -- 只出一件) —— 该模式下隐藏标签与输入框, 避免用户以为能一次生成多件。
    local needCount = self.spawnMode ~= "fish" and self.spawnMode ~= "worn";
    if self.countBox ~= nil then
        self.countBox:setVisible(needCount);
        self.countBox:setEditable(needCount);
    end
    if self.countLabel ~= nil then
        self.countLabel.hidden = not needCount;
    end

    -- 开盒生成不支持多选 (用户拍板): 「加入清单」置灰; 「清单」在清单为空时也置灰,
    -- 但非空时保留可点 —— 否则用户在开盒模式里无法清空清单, 「请先清空清单」的
    -- 提示就没法就地执行。
    if self.addBtn ~= nil then
        self.addBtn:setEnable(self.spawnMode ~= "poison" and self.spawnMode ~= "worn")
    end
    if self.queueBtn ~= nil then
        local hasItems = EtherSpawnQueue ~= nil and EtherSpawnQueue.count() > 0;
        self.queueBtn:setEnable((self.spawnMode ~= "poison" and self.spawnMode ~= "worn") or hasItems)
    end
end

--*********************************************************
--* 多选清单 (三百零三): 把勾选行按当前数量加进待生成清单
--*********************************************************
function EtherLootRollPanel:updateQueueBtnText()
    if self.queueBtn == nil then return end
    self.queueBtn.title = tr("UI_SpawnQueue_Open", { n = tostring(EtherSpawnQueue.count()) });
end

-- 生成按钮的可用态 (清单运行中禁用; 文案恒为「生成」, 三百一十一)
function EtherLootRollPanel:updateSpawnBtn()
    if self.spawnBtn == nil then return end
    local running = EtherSpawnQueue ~= nil and EtherSpawnQueue.running;
    self.spawnBtn:setEnable(not running);
end

function EtherLootRollPanel:addCheckedToQueue()
    if self.datas == nil or self.datas.items == nil then return end
    if self.spawnMode == "poison" or self.spawnMode == "worn" then
        -- 开盒/穿戴不做多选: 只提示, 不入清单
        EtherSpawnQueue.message = getTranslate("UI_SpawnQueue_ModeUnsupported");
        return
    end
    local checked = self.datas.checked or {};
    -- 钓竿生成一次只出一件: 数量固定 1 (输入框在该模式下已隐藏)
    local count = (self.spawnMode == "fish") and 1
        or (tonumber(self.countBox:getInternalText()) or 1);
    -- 勾选是按物品全名跨过滤保存的: 这里遍历整个勾选集合, 而不是当前可见列表 ——
    -- 否则"搜鱼勾两件 → 搜 m16 勾一件 → 加入清单"只会加到最后搜索里勾的 (三百一十 修)
    local entries = {};
    for full, name in pairs(checked) do
        table.insert(entries, { full = full, name = name });
    end
    for _, e in ipairs(entries) do
        EtherSpawnQueue.add(e.full, e.name, count);
    end
    self.datas.checked = {};          -- 全部加入后清空勾选 (含当前过滤之外的)
    local added = #entries;
    self:updateQueueBtnText();
    if added == 0 then
        EtherSpawnQueue.message = getTranslate("UI_SpawnQueue_NothingChecked");
        print("[SpawnQueue] nothing checked");
    else
        EtherSpawnQueue.message = "";
        print("[SpawnQueue] added " .. added .. " item(s), count " .. count);
    end
end

--*********************************************************
--* 物品列表过滤 (名称 + ID 双条件)
--*********************************************************
function EtherLootRollPanel:applyFishFilter()
    -- 纯子串匹配 (与 UIItemTables 修复同款): 物品名含 ( ) : . 等时
    -- Lua 模式匹配会把括号当捕获组, 完整名称失配; 名称/ID 用 initFishList
    -- 预取的小写字段, 逐键过滤零 Java 跨界 (原每键全库 2 次跨界/条)
    local nameTxt = string.lower(self.filterName:getInternalText() or "");
    local idTxt = string.lower(self.filterId:getInternalText() or "");
    self.datas:clear();
    self.totalResult = 0;
    for i, v in ipairs(self.fullList) do
        local okName = nameTxt == "" or string.find(v.lname, nameTxt, 1, true);
        local okId = idTxt == "" or string.find(v.lfid, idTxt, 1, true);
        if okName and okId then
            -- 行文本传名称 (修复前传数字 i, 行 text 会变成序号 — UIItemTables 同款遗留)
            self.datas:addItem(v.name, v.item);
            self.totalResult = self.totalResult + 1;
        end
    end
end

--*********************************************************
--* 列表行绘制 (勾选框 + 名称 + 物品ID, 风格对齐 UIItemTables)
--* 三百零三: 行首勾选框用于「加入清单」; 勾选态按物品全名存在列表对象上,
--*   过滤重建列表不丢勾选 (key 不是行号)。
--*********************************************************
local CHECK_TEX = {};
local CHECK_W = 16;
local CHECK_HIT_W = 22;
local NAME_X = 44;

local function checkTexture(on)
    local key = on and "on" or "off";
    if CHECK_TEX[key] == nil then
        CHECK_TEX[key] = resolveTexture(on
            and "modcore/media/ui/checkbox_checked.png"
            or "modcore/media/ui/checkbox_unchecked.png") or false;
    end
    return CHECK_TEX[key];
end

function EtherLootRollPanel:drawFishItem(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)

    local iconSize = fontHeightSmall;
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    -- 勾选框 (行首): 点击由此行右侧的 onMouseDown 处理 (只认 x <= CHECK_HIT_W)
    local fullName = item.item and item.item:getFullName() or nil
    local tex = checkTexture(fullName ~= nil and self.checked ~= nil and self.checked[fullName] ~= nil)
    if tex then
        self:drawTextureScaled(tex, 3, y + (self.itemheight - CHECK_W) / 2, CHECK_W, CHECK_W, 1, 1, 1, 1)
    end

    self:setStencilRect(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    self:drawText(item.text, NAME_X, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);
    self:clearStencilRect()

    local itemId = item.item:getFullName()
    if itemId == nil then itemId = item.item:getName() or "?" end
    self:drawText(itemId, self.columns[2].size + 10, y + 4, th.textDim.r, th.textDim.g, th.textDim.b, a, self.font);

    -- 图标 (原 drawItemIcon 固定画在 x=4, 勾选框占位后右移)
    local texture = EtherTheme.itemTexture(item.item)
    if texture then
        self:drawTextureScaledAspect2(texture, 23, y + (self.itemheight - iconSize) / 2, iconSize, iconSize, 1, 1, 1, 1)
    end

    return y + self.itemheight;
end

--*********************************************************
--* 列表鼠标: 行首勾选区域切换勾选, 其余区域保持原版整行选中。
--*********************************************************
function EtherLootRollPanel.onListMouseDown(list, x, y)
    if x <= CHECK_HIT_W and list.checked ~= nil then
        local row = list:rowAt(x, y);
        if row >= 1 and row <= #list.items then
            local it = list.items[row];
            local full = it.item and it.item:getFullName() or nil;
            if full ~= nil then
                -- 存显示名而不是 true: 过滤走掉后仍能按名字加入清单 (三百一十 修)
                if list.checked[full] ~= nil then
                    list.checked[full] = nil;
                else
                    list.checked[full] = it.text or full;
                    -- 勾选与行选中联动: 勾上时把"当前选中"切到这一行, 取消勾选时不动它。
                    -- 这样不会出现"选中 A、却勾着 B"的两套状态并存, 点生成时不会误生成 A。
                    list.selected = row;
                end
                return true;
            end
        end
    end
    ISScrollingListBox.onMouseDown(list, x, y);
    return true;
end

--*********************************************************
--* 初始化全物品列表
--*********************************************************
function EtherLootRollPanel:initFishList()
    local items = getAllItems();
    local all = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if item ~= nil and not item:getObsolete() and not item:isHidden() then
            -- 名称/物品ID 预取小写 (与陷阱页/物品页同款修法): 排序/过滤全程纯 Lua。
            -- 原比较器每次比较调两次 getDisplayName(), 全库 N log N 次 Java 跨界,
            -- 是点开页面卡顿主因 (开发日志 2026-08-30 物品页同款定罪)
            local name = item:getDisplayName();
            table.insert(all, {
                item = item, name = name,
                lname = string.lower(name),
                lfid = string.lower(item:getFullName() or ""),
            });
        end
    end
    table.sort(all, function(a, b) return not string.sort(a.lname, b.lname); end);

    self.fullList = all;
    self.totalResult = 0;
    self.datas:clear();
    for i, v in ipairs(all) do
        self.datas:addItem(v.name, v.item);
        self.totalResult = self.totalResult + 1;
    end
end

--*********************************************************
--* Создание дочерних элементов
--* 排版: 三个功能分组 (重置战利品 / 刷弹药 / 钓竿生成) 各自一个切角盒,
--*   盒内统一 标题 -> 控件行 -> 提示 的节奏; 控件高度/内边距统一走
--*   EtherTheme.ctrlH / ctrlPadX / ctrlGap; 静态文案按内宽折行防各语言溢出。
--*********************************************************
function EtherLootRollPanel:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end

    local tm = getTextManager();
    local W, H = self.width, self.height;
    local PAD  = 16;                     -- 面板外边距
    local IP   = 12;                     -- 分组盒内边距
    local GAP  = EtherTheme.ctrlGap;     -- 控件/行间距
    local GGAP = 12;                     -- 分组之间间距
    local ctrlH = EtherTheme.ctrlH;
    local fhS  = EtherTheme.fontHgtSmall;
    local fhM  = EtherTheme.fontHgtMedium;
    local boxW = W - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;
    local labelDY = math.floor((ctrlH - fhS) / 2);   -- 标签相对控件行的垂直居中偏移

    self.groups = {};
    self.texts = {};

    -- ================= 分组1: 刷弹药 =================
    local g1y = 12;
    local cy = g1y + IP;
    self:_header(innerX, cy, getTranslate("UI_LootRoll_SectionFarm"), innerW);
    cy = cy + fhS + GAP;

    -- 三行竖排 (用户实测反馈: "自动开"文案带退弹提示较长, 与"自动关"并排时
    -- 被钳到半行宽, 文字溢出按钮框 —— 图1):
    --   行1: [自动刷(开)] 独占整行
    --   行2: [自动刷(关)] 独占整行
    --   行3: [设置弹药数] [弹药数输入框 占余宽]
    -- 各按钮按自身文案取宽、钳到整行内宽; 仍放不下的极端语言/字体档
    -- 由 UIButton:render 的缩字兜底保证不越出按钮框。
    -- 必须传 maxWidth 防 UIButton:new 的自动加宽还原钳制。
    local farmTitles = {
        getTranslate("UI_CharacterPanel_AmmoFarmAuto"),
        getTranslate("UI_CharacterPanel_AmmoFarmStop"),
        getTranslate("UI_CharacterPanel_AmmoFarmSet"),
    };
    local farmActions = {
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
            EtherAmmoFarm.enabled = true;
        end,
        function()
            EtherAmmoFarm.enabled = false;
        end,
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
        end,
    };

    local fw = {};
    for i = 1, 3 do
        fw[i] = math.min(UIButton.measureWidth(farmTitles[i]), innerW);
    end

    for i = 1, 2 do
        local btn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[i], ctrlH, farmTitles[i], farmActions[i], fw[i]);
        btn:initialise();
        btn:instantiate();
        self:addChild(btn);
        cy = cy + EtherTheme.entryH + GAP;
    end

    local b3 = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[3], ctrlH,
        farmTitles[3], farmActions[3], fw[3]);
    b3:initialise();
    b3:instantiate();
    self:addChild(b3);

    local ax = innerX + fw[3] + GAP;
    self.ammoFarmBox = ISTextEntryBox:new(tostring(getAmmoFarmCount()),
        ax, cy, (innerX + innerW) - ax, EtherTheme.entryH);
    EtherTheme.styleEntry(self.ammoFarmBox);
    self.ammoFarmBox:initialise();
    self.ammoFarmBox:instantiate();
    self.ammoFarmBox:setClearButton(false);
    self:addChild(self.ammoFarmBox);
    cy = cy + EtherTheme.entryH;
    self:_group(PAD, g1y, boxW, (cy - g1y) + IP);

    -- ================= 分组2: 生成模式 (占据剩余高度) =================
    -- 三模式共用物品列表与搜索: 钓竿 (FishingSpawn) / 尸体 (CorpseSpawn) /
    -- 开箱投毒 (BoxPoison)
    local g3y = cy + IP + GGAP;
    -- 矮屏(窗口被钳制)时必须压缩本组而不是兜底撑高: 强制最小高度会把
    -- 盒底推出面板, 组内列表与搜索行全部越界 (实机多轮 "search food 重叠" 根因)。
    local g3h = H - g3y - PAD;
    if g3h < 150 then g3h = 150; end
    self:_group(PAD, g3y, boxW, g3h);

    cy = g3y + IP;
    self:_header(innerX, cy, getTranslate("UI_FishSpawn_Title"), innerW);
    cy = cy + EtherTheme.fontHgtSmall + GAP;

    -- 模式切换行: [生成方式 下拉框........] [数量 输入框]
    -- 三模式的下拉框与数量区: 下拉按最长文案 + 箭头取宽, 数量区保留右侧。
    local modeDefs = {
        { mode = "fish",   key = "UI_SpawnMode_Fish" },
        { mode = "corpse", key = "UI_SpawnMode_Corpse" },
        { mode = "poison", key = "UI_SpawnMode_Poison" },
        { mode = "worn",   key = "UI_SpawnMode_Worn" },
    };
    local countLabelT = getTranslate("UI_TrapSpawn_Count");
    local cntW = 56;
    local cntX = innerX + innerW - cntW;
    -- 数量标签宽度各语言差一个数量级 (CN "数量" / EN "Count" / RU "Количество"),
    -- 下拉框吃满后放不下就不画标签 (输入框默认值 1 已自明, 沿用旧兜底)。
    local labelW = tm:MeasureStringX(UIFont.Small, countLabelT);
    local comboMaxW = cntX - GAP - innerX;                  -- 下拉框可用上限 (数量区之前)
    local comboPad = 26;                                    -- 右侧箭头 + 内边距
    local comboW = comboPad;
    for i = 1, #modeDefs do
        local w = UIButton.measureWidth(getTranslate(modeDefs[i].key)) + comboPad;
        if w > comboW then comboW = w; end
    end
    local showCountLabel = true;
    if comboW + labelW + GAP > comboMaxW then
        showCountLabel = false;
    end
    local cap = showCountLabel and (comboMaxW - labelW - GAP) or comboMaxW;
    if comboW > cap then comboW = cap; end
    if comboW < 80 then comboW = 80; end
    self.spawnMode = "fish";
    self.modeCombo = ISComboBox:new(innerX, cy, comboW, EtherTheme.entryH, self,
    function(t, combo)
        local data = combo:getSelectedData();
        if data ~= nil then
            t.spawnMode = data;
            t:updateSpawnModeUI();
        end
    end);
    self.modeCombo:initialise();
    self.modeCombo:instantiate();
    EtherTheme.styleCombo(self.modeCombo);
    for i = 1, #modeDefs do
        self.modeCombo:addOptionWithData(getTranslate(modeDefs[i].key), modeDefs[i].mode);
    end
    self:addChild(self.modeCombo);

    if showCountLabel then
        self.countLabel = self:_text(cntX - labelW - math.floor(GAP / 2),
            cy + EtherTheme.entryLabelDY, countLabelT, EtherTheme.text, UIFont.Small);
    end
    self.countBox = ISTextEntryBox:new("1", cntX, cy, cntW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.countBox);
    self.countBox:initialise();
    self.countBox:instantiate();
    self.countBox:setClearButton(false);
    self:addChild(self.countBox);
    self:updateSpawnModeUI();
    cy = cy + EtherTheme.entryH + GAP;

    -- 过滤行: 名称 + ID (宽度不足时自动拆成两行, 避免标签压住输入框)
    local nameT = getTranslate("UI_ItemCreator_Title_FilterByName");
    local idT   = getTranslate("UI_ItemCreator_Title_FilterById");
    local nlW = tm:MeasureStringX(UIFont.Small, nameT);
    local ilW = tm:MeasureStringX(UIFont.Small, idT);
    -- 标签宽一律用**实测值**: 文案是按完整字符串绘制的, 把预算截短 (旧的 40% 限宽)
    -- 只会让长标签 (RU "ID предмета" ≈ 19 字符) 画到输入框上面 —— 放不下就拆两行,
    -- 每行的输入框按该行标签重算宽 (2026-09-21 布局审计, 578 宽实测)。
    local entW = math.floor((innerW - nlW - ilW - GAP * 3) / 2);
    local twoRows = entW < 90;
    if twoRows then
        entW = innerW - nlW - GAP;
        if entW < 60 then entW = 60; end
    end

    self:_text(innerX, cy + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self.filterName = ISTextEntryBox:new("", innerX + nlW + GAP, cy, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterName);
    self.filterName:initialise();
    self.filterName:instantiate();
    self.filterName:setClearButton(true);
    self.filterName.onTextChange = function() EtherLootRollPanel.applyFishFilter(self) end
    self:addChild(self.filterName);

    local idX, idY;
    if twoRows then
        cy = cy + EtherTheme.entryH + GAP;
        idX, idY = innerX, cy;
        entW = math.max(60, innerW - ilW - GAP);
    else
        idX = innerX + nlW + GAP + entW + GAP;
        idY = cy;
    end
    self:_text(idX, idY + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self.filterId = ISTextEntryBox:new("", idX + ilW + GAP, idY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterId);
    self.filterId:initialise();
    self.filterId:instantiate();
    self.filterId:setClearButton(true);
    self.filterId.onTextChange = function() EtherLootRollPanel.applyFishFilter(self) end
    self:addChild(self.filterId);
    cy = cy + EtherTheme.entryH + GAP;

    -- 底部区: 生成按钮行 + 使用提示 + 留痕警告 (自下而上: 警告 -> 提示 -> 按钮),
    -- 说明两段静态注册于按钮行之下, 列表高度随之自动收缩;
    -- 双模式取两者提示行数的较大值 (说明文字按 spawnMode 二选一绘制)
    local hintLinesF = EtherTheme.wrapHint(getTranslate("UI_FishSpawn_Hint"), innerW);
    local warnLinesF = EtherTheme.wrapHint(getTranslate("UI_FishSpawn_TraceWarn"), innerW);
    local hintLinesC = EtherTheme.wrapHint(getTranslate("UI_CorpseSpawn_Hint"), innerW);
    -- 尸体生成无红字警告 (2026-09-05 用户决策): corpse +1 行与正常拾取/搬运尸体物品
    -- 同形同频, 单行/频次均不可区分, 配对分析属理论取证手段, 不值得 UI 级警示
    -- 尸体生成也加红字留痕警告 (一百四十五 用户: 与钓竿生成同规格)
    local warnLinesC = EtherTheme.wrapHint(getTranslate("UI_CorpseSpawn_TraceWarn"), innerW);
    local hintLinesP = EtherTheme.wrapHint(getTranslate("UI_BoxPoison_Hint"), innerW);
    local hintLinesW = EtherTheme.wrapHint(getTranslate("UI_WornSpawn_Hint"), innerW);
    local warnLinesW = EtherTheme.wrapHint(getTranslate("UI_WornSpawn_TraceWarn"), innerW);
    local notesMax = math.max(#hintLinesF + #warnLinesF,
        math.max(math.max(#hintLinesC + #warnLinesC, #hintLinesP), #hintLinesW + #warnLinesW));
    -- 说明/警告是附注: 空间不够时**先砍它们的行** (红字警告优先于灰字提示),
    -- 保证列表至少 30px 且不压到生成按钮行 —— 窄窗口/长翻译实测 (620x620 fh19:
    -- 固定行数会把列表挤到 30px 下限, 直接盖在按钮上)。
    local listMinH = 30;
    local notesAvail = (g3y + g3h - IP - ctrlH - GAP * 2 - 16 - listMinH)
        - (cy + EtherTheme.listHeaderH);
    local notesLines = math.min(notesMax,
        math.max(0, math.floor(notesAvail / EtherTheme.fontHgtHint)));
    local notesH = notesLines * EtherTheme.fontHgtHint + GAP * 2;
    local bottomY = g3y + g3h - IP - ctrlH - notesH;
    -- 三百一十一: 按钮一律显示「生成」, 清单/单件的区别只体现在点击后的逻辑上,
    -- 宽度只按这一个文案取 (旧的双文案预留会白白占宽)
    local spawnTitle = getTranslate("UI_FishSpawn_Button");
    local spawnW = UIButton.measureWidth(spawnTitle);
    if spawnW > innerW then spawnW = innerW; end

    -- 列表: 填满过滤行与底部行之间。
    -- 注意: ISScrollingListBox 的列头画在列表 y 之上(自身边界外, 见 UIItemTables 建在 y=25),
    -- 故这里为列头预留一段高度, 否则 "Name/Item ID" 会压到上方过滤行。
    local hdrH = EtherTheme.listHeaderH;   -- 与列表 itemheight 同源, 防表头顶穿上方搜索行
    local listY = cy + hdrH;
    -- 列表压缩适配 + 16px 分离带; 空间不足时收缩列表, 绝不顶穿搜索行
    local listH = bottomY - 16 - listY;
    -- 不设"最小 30px": 空间不足时让列表自己变矮 —— 强行撑高会盖住下方生成按钮行,
    -- 越出分组盒 (800x600 级页面实测)。正常尺寸下 notesLines 的预算已保证 listH ≥ 30,
    -- 这里只兜住页面整体装不下的退化情形 (内容仍然齐全, 只是列表变矮)。
    if listH < 1 then listH = 1; end

    self.datas = EtherFastList:new(innerX, listY, innerW, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawFishItem;
    EtherTheme.styleList(self.datas);
    self.datas.drawBorder = false;      -- 分组盒已提供边框, 避免直角边框与切角框重叠
    self.datas:addColumn(getTranslate("UI_ItemCreator_Title_ItemName"), 0);
    self.datas:addColumn(idT, math.floor(innerW * 0.6));
    self:addChild(self.datas);
    -- 三百零三 多选: 勾选态按物品全名保存 (过滤重建列表不丢), 行首点击切换
    self.datas.checked = {};
    self.datas.onMouseDown = function(list, x, y)
        return EtherLootRollPanel.onListMouseDown(list, x, y);
    end

    self:initFishList();

    self.spawnBtn = UIButton:new(innerX, bottomY, spawnW, ctrlH, spawnTitle,
    function()
        -- 三百零八: 生成统一走这一个按钮 —— 清单非空时按清单串行生成, 为空时生成当前选中行
        local qn = EtherSpawnQueue ~= nil and EtherSpawnQueue.count() or 0;
        if qn > 0 then
            if self.spawnMode == "poison" or self.spawnMode == "worn" then
                -- 开盒/穿戴不支持多选: 只提示先清空清单 (不执行生成)
                if self.spawnMode == "worn" then
                    EtherWornSpawn.message = getTranslate("UI_SpawnQueue_ClearFirst");
                else
                    EtherBoxPoison.message = getTranslate("UI_SpawnQueue_ClearFirst");
                end
                print("[SpawnQueue] clear the list before using this mode");
                return;
            end
            EtherSpawnQueue.start(self.spawnMode);
            return;
        end
        local sel = self.datas.selected;
        if self.datas.items == nil or sel < 1 or sel > #self.datas.items then
            print("[Spawn] select an item first")
            return
        end
        local item = self.datas.items[sel].item;
        if item == nil then return end
        if self.spawnMode == "corpse" then
            -- 尸体生成: 预填最近僵尸背包, 击杀后搜尸取物
            local count = tonumber(self.countBox:getInternalText()) or 1;
            EtherCorpseSpawn.trigger(item:getFullName(), count);
        elseif self.spawnMode == "poison" then
            -- 开盒生成: 给背包载体 modData 写投毒键并上行, 再正常跑对应合成即出物
            local count = tonumber(self.countBox:getInternalText()) or 1;
            EtherBoxPoison.trigger(item:getFullName(), count);
        elseif self.spawnMode == "worn" then
            -- 穿戴生成: 造穿戴幽灵 -> 食物临时压重 -> 服务端换装掉落 -> 捡起
            EtherWornSpawn.trigger(item:getFullName());
        else
            if not isMultiplayer() then
                print("[FishSpawn] multiplayer only (use your own dedicated server)")
                return
            end
            EtherFishSpawn.trigger(item:getFullName());
        end
    end, spawnW)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    -- 多选入口 (三百零三): [加入清单] 把勾选行按当前数量加进待生成清单,
    -- [清单 (N)] 打开清单弹窗 (改数量/删除/开始/停止)。支持: 钓竿/尸体两种;
    -- 开盒生成不支持, 弹窗里点开始会提示。
    local addTitle = getTranslate("UI_SpawnQueue_Add");
    local queueTitleProbe = tr("UI_SpawnQueue_Open", { n = "00" });
    local addW = math.min(UIButton.measureWidth(addTitle), math.floor(innerW * 0.34));
    local queueW = math.min(UIButton.measureWidth(queueTitleProbe), math.floor(innerW * 0.34));
    -- 页脚右侧留状态区: 三按钮先让位, 至少 130px 给动态消息。俄语长按钮下
    -- 原布局会把状态文字挤到面板外 (三百一十 实测), 这里宁可缩按钮 (UIButton
    -- 自带缩字兜底) 也不让状态文字出界。
    local statusMinW = 130;
    local usedW = spawnW + addW + queueW + GAP * 3;
    if innerW - usedW < statusMinW then
        local deficit = statusMinW - (innerW - usedW);
        local nq = math.max(70, queueW - deficit);
        deficit = deficit - (queueW - nq);
        queueW = nq;
        if deficit > 0 then
            addW = math.max(70, addW - deficit);
        end
    end
    self.addBtn = UIButton:new(innerX + spawnW + GAP, bottomY, addW, ctrlH, addTitle,
    function()
        self:addCheckedToQueue();
    end, addW);
    self.addBtn:initialise();
    self.addBtn:instantiate();
    self.addBtn.isOnlyInGame = true;
    self:addChild(self.addBtn);

    self.queueBtn = UIButton:new(innerX + spawnW + GAP * 2 + addW, bottomY, queueW, ctrlH,
        tr("UI_SpawnQueue_Open", { n = tostring(EtherSpawnQueue.count()) }),
    function()
        EtherSpawnQueue.openPopup(self.spawnMode);
    end, queueW);
    self.queueBtn:initialise();
    self.queueBtn:instantiate();
    self.queueBtn.isOnlyInGame = true;
    self:addChild(self.queueBtn);
    self._queueCountShown = EtherSpawnQueue.count();


    -- 多选/数量控件的初始可用态 (按钮此时才创建完毕, 需再刷一次)
    self:updateSpawnModeUI();
    self:updateSpawnBtn();

    -- 使用提示 + 留痕警告 (按钮行之下, 按内宽折行静态注册; 双模式各注册一份,
    -- 绘制时按 spawnMode 二选一 —— modeTexts 条目带 mode 字段, prerender 过滤)
    self.modeTexts = {};
    -- 每个模式一段: 行预算 notesLines 内**先保红字警告再保灰字提示**, 自段顶往下排
    local function regModeBlock(hintLines, warnLines, mode)
        local nWarn = math.min(#warnLines, notesLines);
        local nHint = math.min(#hintLines, notesLines - nWarn);
        local y0 = bottomY + ctrlH + GAP;
        for i = 1, nHint do
            table.insert(self.modeTexts, { x = innerX, y = y0, text = hintLines[i],
                col = EtherTheme.textDim, mode = mode });
            y0 = y0 + EtherTheme.fontHgtHint;
        end
        for i = 1, nWarn do
            table.insert(self.modeTexts, { x = innerX, y = y0, text = warnLines[i],
                col = EtherTheme.statusRed, mode = mode });
            y0 = y0 + EtherTheme.fontHgtHint;
        end
    end
    regModeBlock(hintLinesF, warnLinesF, "fish");
    regModeBlock(hintLinesC, warnLinesC, "corpse");
    regModeBlock(hintLinesP, {}, "poison");
    regModeBlock(hintLinesW, warnLinesW, "worn");

    -- 状态文字区域 (按钮行右侧): 只显示动态状态消息 (生成中/已生成/失败),
    -- 空闲时留空 —— 使用提示已改为底部静态注册, 不再在此兜底。
    self.statusX = innerX + spawnW + GAP * 3 + addW + queueW;
    self.statusY = bottomY + labelDY;
    self.statusW = (innerX + innerW) - self.statusX;
    if self.statusW < 60 then self.statusW = 60; end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherLootRollPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_LootRoll_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.fullList = {};
    menuTableData.totalResult = 0;
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    self.__index = self;

    return menuTableData;
end