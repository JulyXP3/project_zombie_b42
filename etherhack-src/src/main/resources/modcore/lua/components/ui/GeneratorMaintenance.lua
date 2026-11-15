--*********************************************************
--* 发电机: 同层八格范围的单次保养, 不改变开关与连接状态。
--* 公共 setter 已自行同步, 不追加 sync、不把本地满值当服务端回执。
--*********************************************************
GeneratorMaintenance = {}

local RADIUS = 8
local CLICK_INTERVAL_MS = 500
local lastRuns = {}
local lastPlayer = nil

local function finite(value)
    return type(value) == "number" and value == value and math.abs(value) < math.huge
end

local function validGenerator(obj, square, px, py, pz)
    if not instanceof(obj, "IsoGenerator") or obj:getSquare() ~= square then return false end
    local index = obj:getObjectIndex()
    local objects = square:getObjects()
    if index < 0 or index > 127 or index >= objects:size() or objects:get(index) ~= obj then
        return false
    end
    local x, y, z = obj:getX(), obj:getY(), obj:getZ()
    if not finite(x) or not finite(y) or not finite(z) or math.floor(z) ~= pz then return false end
    if (x - px) * (x - px) + (y - py) * (y - py) > RADIUS * RADIUS then return false end
    local fuel, maxFuel, condition = obj:getFuel(), obj:getMaxFuel(), obj:getCondition()
    return finite(fuel) and finite(maxFuel) and maxFuel > 0 and fuel >= 0 and fuel <= maxFuel
        and finite(condition) and condition >= 0 and condition <= 100
end

function GeneratorMaintenance.run(mode)
    local result = { code = "empty", found = 0, submitted = 0, skipped = 0, failed = 0 }
    if mode ~= "fuel" and mode ~= "condition" then
        result.code = "bad-mode"
        return result
    end
    local player = getPlayer()
    if player == nil or player:isDead() then
        result.code = "no-player"
        return result
    end
    if type(generatorMaintenanceReady) ~= "function" or not generatorMaintenanceReady() then
        result.code = "not-ready"
        return result
    end
    local cell = getCell()
    local px, py, pz = player:getX(), player:getY(), player:getZ()
    if cell == nil or not finite(px) or not finite(py) or not finite(pz) then
        result.code = "not-ready"
        return result
    end
    pz = math.floor(pz)
    if lastPlayer ~= player then
        lastPlayer = player
        lastRuns = {}
    end
    local now = getTimestampMs()
    if lastRuns[mode] ~= nil and now - lastRuns[mode] < CLICK_INTERVAL_MS then
        result.code = "busy"
        return result
    end
    lastRuns[mode] = now

    local seen = {}
    for x = math.floor(px) - RADIUS, math.floor(px) + RADIUS do
        for y = math.floor(py) - RADIUS, math.floor(py) + RADIUS do
            local square = cell:getGridSquare(x, y, pz)
            if square ~= nil then
                local objects = square:getObjects()
                for i = 0, objects:size() - 1 do
                    local obj = objects:get(i)
                    if obj ~= nil and not seen[obj] then
                        seen[obj] = true
                        local ok, valid = pcall(validGenerator, obj, square, px, py, pz)
                        if ok and valid then
                            result.found = result.found + 1
                            local full = mode == "fuel" and obj:getFuel() >= obj:getMaxFuel()
                                or mode == "condition" and obj:getCondition() >= 100
                            if full then
                                result.skipped = result.skipped + 1
                            else
                                local sent = pcall(function()
                                    if mode == "fuel" then
                                        obj:setFuel(obj:getMaxFuel())
                                    else
                                        obj:setCondition(100)
                                    end
                                end)
                                if not sent then
                                    -- 可能已开始同步: 停止本次批量, 不回滚或重发未知状态。
                                    result.failed = result.failed + 1
                                    result.code = "partial"
                                    return result
                                end
                                result.submitted = result.submitted + 1
                            end
                        end
                    end
                end
            end
        end
    end
    if result.submitted > 0 then result.code = "submitted"
    elseif result.found > 0 then result.code = "full" end
    return result
end

local messageKeys = {
    submitted = "UI_Generator_Submitted",
    partial = "UI_Generator_Partial",
    empty = "UI_Generator_Empty",
    full = "UI_Generator_Full",
    busy = "UI_Generator_Busy",
    ["no-player"] = "UI_Generator_NotReady",
    ["not-ready"] = "UI_Generator_NotReady",
    ["bad-mode"] = "UI_Generator_NotReady",
}

function GeneratorMaintenance.addModule(panel)
    local gap, ctrlH = EtherTheme.ctrlGap, EtherTheme.ctrlH
    local innerW = panel:_rowContentW() - EtherFormPanel.BOX_PAD_X * 2
    local hintText = tr("UI_Generator_Range")
    local hintH = #EtherTheme.wrapHint(hintText, innerW - 8) * EtherTheme.fontHgtHint + 2
    local statusH = EtherTheme.fontHgtHint * 2 + 2
    for _, key in pairs(messageKeys) do
        local text = tr(key, { n = 99999, skipped = 99999 })
        local height = #EtherTheme.wrapHint(text, innerW - 8) * EtherTheme.fontHgtHint + 2
        statusH = math.max(statusH, height)
    end
    panel:addModule("UI_Generator_Title", ctrlH + gap + hintH + gap + statusH,
        function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X
            local iw = bw - EtherFormPanel.BOX_PAD_X * 2
            local buttonW = math.floor((iw - gap) / 2)
            local function run(mode)
                local result = GeneratorMaintenance.run(mode)
                local text = tr(messageKeys[result.code], { n = result.submitted, skipped = result.skipped })
                panel.generatorStatus.lines = EtherTheme.wrapHint(text, iw - 8)
            end
            local fuelBtn = UIButton:new(ix, by, buttonW, ctrlH, tr("UI_Generator_Fuel"),
                function() run("fuel") end, buttonW)
            local repairBtn = UIButton:new(ix + buttonW + gap, by, buttonW, ctrlH,
                tr("UI_Generator_Repair"), function() run("condition") end, buttonW)
            panel:addWidget(fuelBtn, { onlyInGame = true })
            panel:addWidget(repairBtn, { onlyInGame = true })
            local hint = ModuleHint:new(ix, by + ctrlH + gap, iw, hintText)
            hint:initialise()
            hint:instantiate()
            panel:_anchor(hint)
            panel:addChild(hint)
            panel.generatorStatus = ModuleHint:new(ix, by + ctrlH + gap + hintH + gap, iw, "")
            panel.generatorStatus.height = statusH
            panel.generatorStatus:initialise()
            panel.generatorStatus:instantiate()
            panel:_anchor(panel.generatorStatus)
            panel:addChild(panel.generatorStatus)
        end)
end
