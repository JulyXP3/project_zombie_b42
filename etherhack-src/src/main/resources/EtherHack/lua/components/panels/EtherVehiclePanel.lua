require "ISUI/ISPanel"

--*********************************************************
--* EtherVehiclePanel: 载具操作页
--*
--* 通道全部是 vanilla 服务器车辆指令 (media/lua/server/Vehicles/
--* VehicleCommands.lua, 无权限校验, 已逐行核实):
--*   - startEngine (:14-30): 只查 isDriver + 客户端上传的 haveKey,
--*     "引擎可用"检查被 `if true or` 短路成恒真;
--*   - fixPart (:32-62): 直接设 part/item condition 并三连 transmit,
--*     全程无 checkPermissions —— 对照 setPartCondition (:66) 需要
--*     UseMechanicsCheat 的权限不对称即本页的利用点;
--*   - setContainerContentAmount (:80-93): 任意 part 内容量直设。
--* 引擎能否真启动仍由服务端 tryStartEngine 判定 (油/电/损坏), 与原版一致。
--* "无条件启动引擎(自动重试)": 坐驾驶座且引擎未转时每秒补发 startEngine,
--*   成功即自动取消勾选; 取消勾选立即停止重试。
--* "无条件启动引擎(单次)": 手动单发一次 startEngine。
--* "车辆无条件短接": ServerSyncBlocker 下行保护 (tryHotwire/tryStartEngine
--*   安装期 ASM 注入), 引擎启动成功即自动回弹 (30s 仅作起不来时的兜底)。
--* 全部操作需坐在载具内 (玩家当前载具), 非驾驶座也可 (startEngine 由
--* 服务端再查 isDriver, 无效只产生一条 noise 日志)。
--*********************************************************

EtherVehiclePanel = EtherFormPanel:derive("EtherVehiclePanel");

--*********************************************************
--* 模块内说明行 (hint 缩放文字, 与 EtherCharacterPanel 的 ModuleHint 同款):
--* 做成子控件随滚动统一定位, 不吞鼠标事件。折行在构造时算一次。
--*********************************************************
local ModuleHint = ISPanel:derive("EtherVehicleHint");

function ModuleHint:render()
    for i = 1, #self.lines do
        EtherTheme.drawHintText(self, self.lines[i], 0, (i - 1) * EtherTheme.fontHgtHint,
            EtherTheme.textDim, 0.9);
    end
end

ModuleHint.onMouseDown = UIRowBox.onMouseDown;
ModuleHint.onMouseUp = UIRowBox.onMouseUp;
ModuleHint.onMouseMove = UIRowBox.onMouseMove;

function ModuleHint:new(x, y, w, text)
    local lines = EtherTheme.wrapHint(text, w - 8);
    local o = ISPanel:new(x, y, w, #lines * EtherTheme.fontHgtHint + 2);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.lines = lines;
    return o;
end

--*********************************************************
--* 当前载具 (玩家所坐的载具, 任意座位)。
--*********************************************************
local function currentVehicle()
    local player = getPlayer();
    if not player then return nil, nil; end
    local vehicle = player:getVehicle();
    return player, vehicle;
end

--*********************************************************
--* 修理: 遍历全部件, 已安装部件(有 inventory item)逐个发 fixPart 到 100。
--* 油箱等"容量型"部件没有物品条目, 修不了 —— 加油走 setContainerContentAmount。
--* haveBeenRepaired 必须是数字: vanilla VehicleCommands.fixPart 直传
--* item:setHaveBeenRepaired(int), 传 boolean 会炸 Kahlua 参数校验。
--*********************************************************
local function repairVehicle()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        if part and part:getInventoryItem() then
            sendClientCommand(player, "vehicle", "fixPart", {
                vehicle = vehicle:getId(), part = part:getId(),
                condition = 100, haveBeenRepaired = 1 });
        end
    end
end

--*********************************************************
--* 加油: GasTank 部件容量拉到上限。
--*********************************************************
local function refuelVehicle()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    local tank = vehicle:getPartById("GasTank");
    if tank then
        sendClientCommand(player, "vehicle", "setContainerContentAmount", {
            vehicle = vehicle:getId(), part = "GasTank",
            amount = tank:getContainerCapacity() });
    end
end

--*********************************************************
--* 立即启动: 单发一次 startEngine(haveKey=true)。
--*********************************************************
local function startEngineNow()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    sendClientCommand(player, "vehicle", "startEngine", { haveKey = true });
end

--*********************************************************
--* 模块内一行复选框的高度预算 (与 EtherVisualsPanel.rowStep 同一套规则)。
--*********************************************************
local function rowStep(key, w)
    local title = tr(key);
    local availW = w - (18 + 10 + 8);
    if getTextManager():MeasureStringX(UIFont.Small, title) > availW then
        local n = #EtherTheme.wrapText(title, availW, UIFont.Small);
        return n * (EtherTheme.fontHgtSmall + 2) + EtherFormPanel.BOX_PAD_Y * 2 + 4;
    end
    return EtherFormPanel.ROW_STEP;
end

--*********************************************************
--* 模块内摆放一行复选框, 返回行距与控件实例 (保护开关需对外暴露给
--* ServerSyncBlocker.lua 调 :setCheked)。
--*********************************************************
local function placeCheckbox(panel, key, getState, onToggle, x, y, w, onlyInGame)
    local cb = UICheckbox:new(x, y, tr(key), getState() and true or false, onToggle);
    panel:addWidget(cb, { onlyInGame = onlyInGame == true });
    return rowStep(key, w), cb;
end

--*********************************************************
--* 模块内按钮组: 按组宽与内容宽自适应 —— 放得下同排, 放不下竖排。
--* 高度预算与摆放共用同一套判定 (buttonRowHeight)。
--*********************************************************
local function buttonRowHeight(innerW, titles)
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth(titles);
    local ctrlH = EtherTheme.ctrlH;
    if btnW * #titles + gap * (#titles - 1) <= innerW then
        return ctrlH;
    end
    return #titles * ctrlH + (#titles - 1) * 4;
end

local function placeButtonRow(panel, bx, by, innerW, titles, callbacks)
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth(titles);
    local ctrlH = EtherTheme.ctrlH;
    if btnW * #titles + gap * (#titles - 1) <= innerW then
        local x = bx;
        for i = 1, #titles do
            local btn = UIButton:new(x, by, btnW, ctrlH, titles[i], callbacks[i], btnW);
            panel:addWidget(btn, { onlyInGame = true });
            x = x + btnW + gap;
        end
        return ctrlH;
    end
    local w = math.min(btnW, innerW);
    local y = by;
    for i = 1, #titles do
        local btn = UIButton:new(bx, y, w, ctrlH, titles[i], callbacks[i], w);
        panel:addWidget(btn, { onlyInGame = true });
        y = y + ctrlH + 4;
    end
    return y - by - 4;
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调): 两个功能模块。
--*********************************************************
function EtherVehiclePanel:build()
    -- 复选框实例表 (key -> widget), 供 ServerSyncBlocker.lua 回调 :setCheked
    EtherVehiclePanel.checkboxByKey = {};
    local modules = {
        {
            title = "UI_VehiclePanel_Group_Engine",
            checkboxes = {
                -- 自动重试: 每秒补发 startEngine 直到引擎转起来;
                -- 成功后由 ServerSyncBlocker 自动取消勾选 (toggleVehicleInstantStart(false))
                { key = "UI_VehiclePanel_InstantStart",
                  get = function() return isVehicleInstantStart and isVehicleInstantStart() or false; end,
                  on = function(c) toggleVehicleInstantStart(c); end },
                -- 车辆无条件短接 (ServerSyncBlocker 下行保护, 引擎启动成功即自动回弹, 30s 兜底;
                -- 仅游戏内可勾, 成功/超时由 ServerSyncBlocker 回调 :setCheked(false))
                { key = "UI_Exploit_VehicleProtection", onlyInGame = true,
                  get = function()
                      return (ServerSyncBlocker and type(ServerSyncBlocker) == "table"
                          and ServerSyncBlocker.vehicleProtection) or false;
                  end,
                  on = function(c)
                      if ServerSyncBlocker and type(ServerSyncBlocker) == "table" then
                          if c then
                              if ServerSyncBlocker.enableVehicle then ServerSyncBlocker.enableVehicle(); end
                          else
                              if ServerSyncBlocker.disableVehicle then ServerSyncBlocker.disableVehicle(); end
                          end
                      else
                          print("[EtherHack] ERROR: ServerSyncBlocker not loaded!");
                      end
                  end },
            },
            buttons = {
                { key = "UI_VehiclePanel_StartNow", fn = startEngineNow },
            },
            hint = "UI_VehiclePanel_EngineHint",
        },
        {
            title = "UI_VehiclePanel_Group_Service",
            buttons = {
                { key = "UI_VehiclePanel_Repair", fn = repairVehicle },
                { key = "UI_VehiclePanel_Refuel", fn = refuelVehicle },
            },
            hint = "UI_VehiclePanel_Hint",
        },
    };

    for mi = 1, #modules do
        if mi > 1 then
            self:addSpacer(EtherFormPanel.SECTION_GAP);
        end
        local mod = modules[mi];
        local w = self:_rowContentW();
        local innerW = w - EtherFormPanel.BOX_PAD_X * 2;

        -- 高度预算 (与摆放共用同一套判定)
        local contentH = 0;
        if mod.checkboxes then
            for i = 1, #mod.checkboxes do
                contentH = contentH + rowStep(mod.checkboxes[i].key, innerW) + 2;
            end
            contentH = contentH + 4;
        end
        local titles = {};
        for i = 1, #mod.buttons do titles[i] = tr(mod.buttons[i].key); end
        contentH = contentH + buttonRowHeight(innerW, titles);
        if mod.hint ~= nil then
            contentH = contentH + 6
                + #EtherTheme.wrapHint(tr(mod.hint), innerW - 8) * EtherTheme.fontHgtHint + 2;
        end

        self:addModule(mod.title, contentH + 2, function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X;
            local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
            local cy = by;
            if mod.checkboxes then
                for i = 1, #mod.checkboxes do
                    local step, cb = placeCheckbox(self, mod.checkboxes[i].key, mod.checkboxes[i].get,
                        mod.checkboxes[i].on, ix, cy, iW, mod.checkboxes[i].onlyInGame);
                    -- 复选框实例对外暴露: ServerSyncBlocker.lua 成功/授权后会调 :setCheked(false)
                    EtherVehiclePanel.checkboxByKey[mod.checkboxes[i].key] = cb;
                    cy = cy + step + 2;
                end
                cy = cy + 4;
            end
            cy = cy + placeButtonRow(self, ix, cy, iW, titles, (function()
                local fns = {};
                for i = 1, #mod.buttons do fns[i] = mod.buttons[i].fn; end
                return fns;
            end)());
            if mod.hint ~= nil then
                local hint = ModuleHint:new(ix, cy + 6, iW, tr(mod.hint));
                hint:initialise();
                hint:instantiate();
                self:_anchor(hint);
                self:addChild(hint);
            end
        end);
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
