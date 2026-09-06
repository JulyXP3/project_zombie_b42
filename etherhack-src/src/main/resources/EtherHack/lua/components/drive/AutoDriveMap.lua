--*********************************************************
--* AutoDriveMap: 伪·自动驾驶地图锚定 (研判 §五「地图联动」)
--*
--* 三处挂钩 (EtherDriveModule 同款防御式, 类不存在时自动跳过):
--*   1. 自有 UIMap (主构建 minimap): 重定义 onRightMouseUp — 原体 (传送选项)
--*      照抄 + 追加「伪 · 自动驾驶至此」并列选项 (锚定不限区块有效性: 终点可以是
--*      未探索区, 与滚动规划衔接);
--*   2. 原版 M 大地图 ISWorldMap: 包装 onRightMouseUp — 普通玩家原版直接
--*      return false 无任何菜单 (实测 M 地图没选项的根因), 我们补出自有菜单;
--*      admin/调试菜单已弹出时把锚定追加进同一菜单;
--*   3. 原版 ISMap (背包地图物品阅读界面): 原版未定义 onRightMouseUp, 直接定义。
--* 终点标记: 两图 render 后追加绘制 (worldToUI 换算, 视口内才画)。
--*
--* 锚定语义 (§五): EtherAutoDriveAPI.autoDriveTarget(x, y) — z=0 车辆贴地,
--* 到达半径 4 格; 再锚定 = 换终点并重规划; 行驶中锚定 = 直接改道, 无需先停。
--* 开图与驾驶并存: M 地图是 UI 覆盖层不暂停 MP, 可边开边改终点。
--*********************************************************

require "ISUI/ISPanel"

--*********************************************************
--* 锚定动作: 调 API, 成功/失败都用玩家 Say 即时反馈 (状态行见驾驶页)
--*********************************************************
local function anchorDriveTarget(worldX, worldY)
    if type(autoDriveTarget) ~= "function" then return; end
    local player = getPlayer();
    if player == nil then return; end
    if autoDriveTarget(worldX, worldY) then
        player:Say(tr("UI_Drive_MapAnchored"));
    else
        local msgKey = autoDriveGetMessage();
        player:Say(msgKey ~= nil and msgKey ~= "" and tr(msgKey) or tr("UI_DrivePanel_MsgNoVehicle"));
    end
end

--*********************************************************
--* 终点标记绘制 (render 包装共用): 脉动十字标 + 文案, 视口内才画
--*********************************************************
local function drawDriveMarker(self)
    if type(autoDriveIsActive) ~= "function" or not autoDriveIsActive() then return; end
    if self.mapAPI == nil then return; end
    local tx, ty = autoDriveGetTargetX(), autoDriveGetTargetY();
    local ux = self.mapAPI:worldToUIX(tx, ty);
    local uy = self.mapAPI:worldToUIY(tx, ty);
    -- 视口剔除 (不裁剪的话会画出地图容器外)
    if ux < 0 or uy < 0 or ux > self:getWidth() or uy > self:getHeight() then return; end
    local s = 5;
    local r, g, b, a = 1.0, 0.35, 0.25, 1.0;
    -- 十字标
    self:drawRect(ux - s * 2, uy - 1, s * 4, 2, a, r, g, b);
    self:drawRect(ux - 1, uy - s * 2, 2, s * 4, a, r, g, b);
    self:drawRectBorder(ux - s, uy - s, s * 2, s * 2, 1, r, g, b);
    self:drawText(tr("UI_Drive_MapTarget"), ux + 8, uy - 4, r, g, b, a, UIFont.Small);
end

--*********************************************************
--* 挂钩 1: 自有 UIMap。防御式: UIMap 不存在时整体跳过。
--*********************************************************
if UIMap ~= nil and UIMap.onRightMouseUp ~= nil then
    -- 重定义 onRightMouseUp: 原体保留 (传送选项), 追加锚定选项。
    -- (不 wrap 复调: 原实现内部自建 ISContextMenu, 二次 get 会叠出第二个菜单)
    function UIMap:onRightMouseUp(x, y)
        local context = ISContextMenu.get(0, x + self:getAbsoluteX(), y + self:getAbsoluteY());
        local worldX = self.mapAPI:uiToWorldX(x, y);
        local worldY = self.mapAPI:uiToWorldY(x, y);
        if getWorld():getMetaGrid():isValidChunk(worldX / 10, worldY / 10) then
            context:addOption(getTranslate("UI_Map_TeleportContext"), self, self.onTeleport, worldX, worldY);
        end
        -- 自动驾驶锚定: 不做 isValidChunk 门禁 — 终点允许落在未探索区 (滚动规划衔接)
        context:addOption(tr("UI_Drive_MapAnchor"), self, self.onAutoDriveAnchor, worldX, worldY);
    end

    function UIMap:onAutoDriveAnchor(worldX, worldY)
        anchorDriveTarget(worldX, worldY);
    end

    -- 终点标记: 原 render 后追加
    local _origUIMapRender = UIMap.render;
    function UIMap:render()
        _origUIMapRender(self);
        drawDriveMarker(self);
    end
end

--*********************************************************
--* 挂钩 2: 原版 M 大地图 ISWorldMap (按 M 打开的世界地图; 注意不是 ISMap —
--* 那是背包地图物品的阅读界面, 见挂钩 3)。原版 onRightMouseUp 三种走向:
--*   symbolsUI 消费 → true; admin/调试 → 自建菜单 (网格/传送等) → true;
--*   普通玩家 → 直接 return false (原版永远无菜单, 这是 M 地图没选项的根因)。
--* 包装: 原版已弹菜单 (admin) 时把锚定追加进已开菜单 — ISContextMenu.get 是
--* "清空+复用"单例, 不能二次 get (会清掉原版选项); 未弹时自建仅有锚定的菜单。
--*********************************************************
if ISWorldMap ~= nil then
    local _origWorldMapRightUp = ISWorldMap.onRightMouseUp;
    function ISWorldMap:onRightMouseUp(x, y)
        local consumed = false;
        if _origWorldMapRightUp ~= nil then
            consumed = (_origWorldMapRightUp(self, x, y) and true) or false;
        end
        if self.mapAPI == nil or type(autoDriveTarget) ~= "function" or getPlayer() == nil then
            return consumed;
        end
        local worldX = self.mapAPI:uiToWorldX(x, y);
        local worldY = self.mapAPI:uiToWorldY(x, y);
        if consumed then
            -- symbolsUI 编辑态不弹上下文菜单; 仅 admin 菜单可见时追加
            local ctx = getPlayerContextMenu(0);
            if ctx ~= nil and ctx:isVisible() and ctx.addOption ~= nil then
                ctx:addOption(tr("UI_Drive_MapAnchor"), self, self.onAutoDriveAnchor, worldX, worldY);
            end
            return true;
        end
        local context = ISContextMenu.get(0, x + self:getAbsoluteX(), y + self:getAbsoluteY());
        context:addOption(tr("UI_Drive_MapAnchor"), self, self.onAutoDriveAnchor, worldX, worldY);
        return true;
    end

    function ISWorldMap:onAutoDriveAnchor(worldX, worldY)
        anchorDriveTarget(worldX, worldY);
    end

    -- 终点标记: 原 render 后追加 (mapAPI v3 同样有 uiToWorldX/worldToUIX)
    if ISWorldMap.render ~= nil then
        local _origWorldMapRender = ISWorldMap.render;
        function ISWorldMap:render()
            _origWorldMapRender(self);
            drawDriveMarker(self);
        end
    end
end

--*********************************************************
--* 挂钩 3: ISMap (背包地图物品阅读界面)。B42 ISMap 无 onRightMouseUp 定义
--* (基类返回 false), 直接定义即可; 若未来原版加了实现则退化为包装。
--*********************************************************
if ISMap ~= nil then
    local _origISMapRightUp = ISMap.onRightMouseUp;
    function ISMap:onRightMouseUp(x, y)
        if _origISMapRightUp ~= nil then
            _origISMapRightUp(self, x, y);
        end
        if self.mapAPI ~= nil and type(autoDriveTarget) == "function" then
            local context = ISContextMenu.get(0, x + self:getAbsoluteX(), y + self:getAbsoluteY());
            local worldX = self.mapAPI:uiToWorldX(x, y);
            local worldY = self.mapAPI:uiToWorldY(x, y);
            context:addOption(tr("UI_Drive_MapAnchor"), self, self.onAutoDriveAnchor, worldX, worldY);
        end
    end

    function ISMap:onAutoDriveAnchor(worldX, worldY)
        anchorDriveTarget(worldX, worldY);
    end

    -- 终点标记: 原 render 后追加 (ISMap.render 有原版实现)
    if ISMap.render ~= nil then
        local _origISMapRender = ISMap.render;
        function ISMap:render()
            _origISMapRender(self);
            drawDriveMarker(self);
        end
    end
end
