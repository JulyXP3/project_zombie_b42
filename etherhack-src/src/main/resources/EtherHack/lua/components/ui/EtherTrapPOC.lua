--*********************************************************
--* Red team POC: trap-spawn FOOD items + WEAPONS (multiplayer)
--*
--* 食物链 (mode="food"):
--*   client sends "addAnimalDebug" (no whitelist/auth)
--*   -> server setAnimal() trusts the client animal table
--*   -> ISCheckTrapAction (vanilla timed action) -> server
--*   removeAnimalItem() -> instanceItem(animal.item) -> AddItem
--*   Usage: place any trap -> stand next to it -> select item ->
--*   "Trap-spawn item" button. ONLY Food items spawn (server calls
--*   Food-only getBaseHunger; non-food errors out).
--*
--* 武器链 (mode="weapon", 链3 爆炸陷阱):
--*   client forges AddExplosiveTrapPacket with an arbitrary
--*   HandWeapon (TrapSpawnAPI.trapSpawnPlace, 伪造大引信防爆) ->
--*   server creates IsoTrap at the player's own square (zero
--*   validation) -> vanilla ISTakeTrap -> server-side complete()
--*   -> inventory:AddItem(trap:getItem()). No packets for the
--*   take step beyond the vanilla timed action sync.
--*   count=N loops automatically (a few seconds per item).
--*
--* Note (food): baseHunger<0 = clean stats (size=|baseHunger|*100);
--*   food baseHunger=0 = NaN weight.
--*********************************************************
EtherTrapPOC = EtherTrapPOC or {}

EtherTrapPOC.target = "Base.Ham"
EtherTrapPOC.mode = "food"
EtherTrapPOC.count = 1
EtherTrapPOC.armed = false
EtherTrapPOC.phase = "idle"
EtherTrapPOC.timer = 0
EtherTrapPOC.armTime = 0
EtherTrapPOC.remindTimer = 0
EtherTrapPOC.cyclesDone = 0
EtherTrapPOC.timeoutMs = 120000
EtherTrapPOC.trapX = nil
EtherTrapPOC.trapY = nil
EtherTrapPOC.trapZ = nil
EtherTrapPOC.trapObj = nil

function EtherTrapPOC.setTarget(t)
    if t ~= nil and t ~= "" then
        EtherTrapPOC.target = t
    end
end

function EtherTrapPOC.cancel()
    if not EtherTrapPOC.armed then return end
    EtherTrapPOC.armed = false
    EtherTrapPOC.phase = "idle"
    EtherTrapPOC.trapObj = nil
    print("[TrapPOC] cancelled by user")
end

function EtherTrapPOC.trigger()
    if not isMultiplayer() then
        print("[TrapPOC] multiplayer only (use your own dedicated server)")
        return
    end
    if EtherTrapPOC.armed then
        print("[TrapPOC] already armed, target: " .. EtherTrapPOC.target)
        return
    end
    EtherTrapPOC.mode = "food"
    EtherTrapPOC.armed = true
    EtherTrapPOC.phase = "find_trap"
    EtherTrapPOC.cyclesDone = 0
    EtherTrapPOC.trapX = nil
    EtherTrapPOC.trapY = nil
    EtherTrapPOC.trapZ = nil
    EtherTrapPOC.trapObj = nil
    EtherTrapPOC.armTime = getTimestampMs()
    EtherTrapPOC.remindTimer = getTimestampMs()
    print("[TrapPOC] target " .. EtherTrapPOC.target .. " x" .. EtherTrapPOC.count .. ": stand next to a placed trap")
end

function EtherTrapPOC.triggerWeapon(count)
    if not isMultiplayer() then
        print("[TrapPOC] multiplayer only (use your own dedicated server)")
        return
    end
    if EtherTrapPOC.armed then
        print("[TrapPOC] already armed, target: " .. EtherTrapPOC.target)
        return
    end
    count = tonumber(count) or 1
    if count < 1 then count = 1 end
    if count > 50 then count = 50 end
    EtherTrapPOC.mode = "weapon"
    EtherTrapPOC.count = math.floor(count)
    EtherTrapPOC.armed = true
    EtherTrapPOC.phase = "place"
    EtherTrapPOC.cyclesDone = 0
    EtherTrapPOC.trapObj = nil
    EtherTrapPOC.armTime = getTimestampMs()
    print("[TrapPOC] weapon target " .. EtherTrapPOC.target .. " x" .. EtherTrapPOC.count)
end

local function findNearbyTrap(player)
    local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ())
    for dy = -2, 2 do
        for dx = -2, 2 do
            local t = CTrapSystem.instance:getLuaObjectAt(px + dx, py + dy, pz)
            if t ~= nil and t.x ~= nil then
                return t, px + dx, py + dy, pz
            end
        end
    end
    return nil, nil, nil, nil
end

local function animalTableFor(target)
    local mn, mx = 1, 2
    local hg = 0
    local ok = pcall(function()
        local probe = instanceItem(target)
        if probe ~= nil then
            hg = itemBaseHunger(probe)
        end
    end)
    if not ok then
        print("[TrapPOC] probe " .. target .. ": instanceItem failed, using default size")
    else
        -- 服务端 removeAnimalItem 用 statsModifier = size / (baseHunger * -100)
        -- 缩放饥饿/卡路里/重量。为让 statsModifier == 1 (保持原版饥饿值),
        -- 注入 size = |baseHunger| * 100。itemBaseHunger 返回负值 baseHunger。
        if hg ~= nil and hg < 0 then
            local typical = math.floor(math.abs(hg) * 100)
            if typical > 0 then
                mn, mx = typical, typical
            end
        end
        if hg == nil or hg >= 0 then
            print("[TrapPOC] warning: " .. target .. " has no HungerChange, spawned weight will be NaN")
        end
        print("[TrapPOC] probe " .. target .. ": baseHunger=" .. tostring(hg) .. " size=[" .. tostring(mn) .. "," .. tostring(mx) .. "]")
    end
    return { type = "Rabbit", item = target, minSize = mn, maxSize = mx }
end

-- 扫描指定格上的 IsoTrap 世界对象 (链3: 服务端放置后经 CompleteItem 同步到客户端)
local function findTrapOnSquare(x, y, z)
    local sq = getCell():getGridSquare(x, y, z)
    if sq == nil then return nil end
    local objects = sq:getObjects()
    for i = 0, objects:size() - 1 do
        local o = objects:get(i)
        if instanceof(o, "IsoTrap") and o:getItem() ~= nil then
            return o
        end
    end
    return nil
end

-- 陷阱是否仍在该格 (按对象身份比对; 拾取后对象从格上移除即消失)
local function trapStillThere(x, y, z, trapObj)
    local sq = getCell():getGridSquare(x, y, z)
    if sq == nil then return false end
    local objects = sq:getObjects()
    for i = 0, objects:size() - 1 do
        if objects:get(i) == trapObj then
            return true
        end
    end
    return false
end

local function onTickInner()
    if not EtherTrapPOC.armed then return end
    local player = getPlayer()
    local now = getTimestampMs()
    if player == nil or now - EtherTrapPOC.armTime > EtherTrapPOC.timeoutMs then
        if player ~= nil then print("[TrapPOC] timeout, gave up: " .. EtherTrapPOC.target) end
        EtherTrapPOC.armed = false
        EtherTrapPOC.phase = "idle"
        EtherTrapPOC.trapObj = nil
        return
    end

    -- done 结算优先
    if EtherTrapPOC.phase == "done" and now - EtherTrapPOC.timer > 3000 then
        EtherTrapPOC.cyclesDone = EtherTrapPOC.cyclesDone + 1
        if EtherTrapPOC.cyclesDone < EtherTrapPOC.count then
            EtherTrapPOC.phase = (EtherTrapPOC.mode == "weapon") and "place" or "inject"
            print("[TrapPOC] item " .. EtherTrapPOC.cyclesDone .. "/" .. EtherTrapPOC.count .. " done, next one...")
        else
            print("[TrapPOC] done, check your inventory: " .. EtherTrapPOC.target .. " x" .. EtherTrapPOC.count)
            EtherTrapPOC.armed = false
            EtherTrapPOC.phase = "idle"
            EtherTrapPOC.trapObj = nil
        end
        return
    end

    -- ===== 武器链 (链3 爆炸陷阱) =====
    if EtherTrapPOC.mode == "weapon" then
        if EtherTrapPOC.phase == "place" then
            local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ())
            if trapSpawnPlace(EtherTrapPOC.target) then
                EtherTrapPOC.trapX = px
                EtherTrapPOC.trapY = py
                EtherTrapPOC.trapZ = pz
                EtherTrapPOC.phase = "wait_trap"
                EtherTrapPOC.timer = now
            else
                print("[TrapPOC] place failed, abort: " .. EtherTrapPOC.target)
                EtherTrapPOC.armed = false
                EtherTrapPOC.phase = "idle"
            end
            return
        end
        if EtherTrapPOC.phase == "wait_trap" then
            if now - EtherTrapPOC.timer > 5000 then
                print("[TrapPOC] trap never appeared, abort: " .. EtherTrapPOC.target)
                EtherTrapPOC.armed = false
                EtherTrapPOC.phase = "idle"
                return
            end
            if now - EtherTrapPOC.timer >= 700 then
                local t = findTrapOnSquare(EtherTrapPOC.trapX, EtherTrapPOC.trapY, EtherTrapPOC.trapZ)
                if t ~= nil then
                    EtherTrapPOC.trapObj = t
                    EtherTrapPOC.phase = "take"
                end
            end
            return
        end
        if EtherTrapPOC.phase == "take" then
            ISTimedActionQueue.add(ISTakeTrap:new(player, EtherTrapPOC.trapObj))
            print("[TrapPOC] taking trap, item in ~2s")
            EtherTrapPOC.phase = "wait_gone"
            EtherTrapPOC.timer = now
            return
        end
        if EtherTrapPOC.phase == "wait_gone" then
            if now - EtherTrapPOC.timer > 8000 then
                print("[TrapPOC] trap pickup stalled, abort: " .. EtherTrapPOC.target)
                EtherTrapPOC.armed = false
                EtherTrapPOC.phase = "idle"
                EtherTrapPOC.trapObj = nil
                return
            end
            if not trapStillThere(EtherTrapPOC.trapX, EtherTrapPOC.trapY, EtherTrapPOC.trapZ, EtherTrapPOC.trapObj) then
                EtherTrapPOC.phase = "done"
                EtherTrapPOC.timer = now
            end
            return
        end
        return
    end

    -- ===== 食物链 (addAnimalDebug) =====
    if EtherTrapPOC.phase == "find_trap" then
        local t, x, y, z = findNearbyTrap(player)
        if t == nil then
            if now - EtherTrapPOC.remindTimer > 30000 then
                EtherTrapPOC.remindTimer = now
                print("[TrapPOC] stand next to a placed trap")
            end
            return
        end
        EtherTrapPOC.trapX = x
        EtherTrapPOC.trapY = y
        EtherTrapPOC.trapZ = z
        EtherTrapPOC.phase = "inject"
    end

    if EtherTrapPOC.phase == "inject" then
        CTrapSystem.instance:sendCommand(player, "addAnimalDebug", {
            x = EtherTrapPOC.trapX,
            y = EtherTrapPOC.trapY,
            z = EtherTrapPOC.trapZ,
            animal = animalTableFor(EtherTrapPOC.target),
        })
        print("[TrapPOC] forged animal injected: " .. EtherTrapPOC.target)
        EtherTrapPOC.phase = "check"
        EtherTrapPOC.timer = now
    elseif EtherTrapPOC.phase == "check" then
        if now - EtherTrapPOC.timer < 1000 then return end
        local t = CTrapSystem.instance:getLuaObjectAt(EtherTrapPOC.trapX, EtherTrapPOC.trapY, EtherTrapPOC.trapZ)
        if t == nil then
            EtherTrapPOC.armed = false
            EtherTrapPOC.phase = "idle"
            print("[TrapPOC] aborted: trap not found")
            return
        end
        ISTimedActionQueue.add(ISCheckTrapAction:new(player, t))
        print("[TrapPOC] trap checked, item in ~2s")
        EtherTrapPOC.phase = "done"
        EtherTrapPOC.timer = now
    end
end

local function onTick()
    local ok, err = pcall(onTickInner)
    if not ok then
        print("[TrapPOC] internal error, disarmed: " .. tostring(err))
        EtherTrapPOC.armed = false
        EtherTrapPOC.phase = "idle"
    end
end

if Events ~= nil then
    Events.OnTick.Add(onTick)
end
