--[[
    ServerSyncBlocker.lua - Lua wrapper for server sync protection
    
    This module provides easy access to the Java ServerSyncBlocker class
    which prevents the server from overwriting local player modifications.
    
    Usage:
        require "EtherHack/lua/fixes/ServerSyncBlocker"
        
        -- Enable full protection (stats, skills, inventory, traits)
        enableFullProtection()
        
        -- Or enable specific protection
        enableStatsProtection()
        enableSkillsProtection()
        
        -- Protect specific stat values
        protectStat("ENDURANCE", 1.0)
        protectStat("HUNGER", 0.0)
        
        -- Protect skill levels
        protectSkill("Fitness", 10, 0)
]]--

-- Create global ServerSyncBlocker table FIRST (before anything else)
if not ServerSyncBlocker then
    ServerSyncBlocker = {}
    ServerSyncBlocker.statsProtected = {}
    ServerSyncBlocker.skillsProtected = {}
    ServerSyncBlocker.isEnabled = false
    ServerSyncBlocker._initialized = false
end

-- Java methods are resolved at call time (EtherAPI.loadAPI exposes them as globals).
-- Resolving lazily keeps this robust even if this file happens to load before exposure.

-- Initialize protection state (only if not already initialized)
function ServerSyncBlocker.init()
    if ServerSyncBlocker._initialized then return end
    ServerSyncBlocker._initialized = true
    ServerSyncBlocker.isEnabled = false
    ServerSyncBlocker.statsProtected = {}
    ServerSyncBlocker.skillsProtected = {}
end

-- Enable full protection mode
function ServerSyncBlocker.enableFull()
    if enableFullProtection then
        enableFullProtection()
    end
    ServerSyncBlocker.isEnabled = true
    print("[EtherHack] Full server sync protection enabled")
    return true
end

-- Disable all protection
function ServerSyncBlocker.disableFull()
    if disableFullProtection then
        disableFullProtection()
    end
    ServerSyncBlocker.isEnabled = false
    ServerSyncBlocker.statsProtected = {}
    ServerSyncBlocker.skillsProtected = {}
    print("[EtherHack] Server sync protection disabled")
    return true
end

-- Enable stats-only protection
function ServerSyncBlocker.enableStats()
    if enableStatsProtection then
        enableStatsProtection()
        return true
    end
    return false
end

-- Disable stats protection
function ServerSyncBlocker.disableStats()
    if disableStatsProtection then
        disableStatsProtection()
        return true
    end
    return false
end

-- Enable skills protection
function ServerSyncBlocker.enableSkills()
    if enableSkillsProtection then
        enableSkillsProtection()
        return true
    end
    return false
end

-- Disable skills protection
function ServerSyncBlocker.disableSkills()
    if disableSkillsProtection then
        disableSkillsProtection()
        return true
    end
    return false
end

-- Protect a specific stat with a value
-- statName: ENDURANCE, HUNGER, THIRST, FATIGUE, etc.
-- value: the value to maintain (usually 0.0 or 1.0)
function ServerSyncBlocker.protectStatValue(statName, value)
    if protectStat then
        protectStat(statName, value)
        ServerSyncBlocker.statsProtected[statName] = value
        return true
    end
    return false
end

-- Remove protection from a stat
function ServerSyncBlocker.unprotectStatValue(statName)
    if unprotectStat then
        unprotectStat(statName)
        ServerSyncBlocker.statsProtected[statName] = nil
        return true
    end
    return false
end

-- Protect a skill at a specific level and XP
function ServerSyncBlocker.protectSkillLevel(perkName, level, xp)
    xp = xp or 0
    if protectSkill then
        protectSkill(perkName, level, xp)
        ServerSyncBlocker.skillsProtected[perkName] = {level = level, xp = xp}
        return true
    end
    return false
end

-- Check if protection is currently active (use Lua state first)
function ServerSyncBlocker.isActive()
    return ServerSyncBlocker.isEnabled
end

-- Manually trigger reapply of protected values
function ServerSyncBlocker.reapply()
    if reapplyProtectedValues then
        reapplyProtectedValues()
    end
end

-- Manually trigger packet filtering
function ServerSyncBlocker.filterPackets()
    if filterIncomingSyncPackets then
        filterIncomingSyncPackets()
    end
end

-- Helper: Protect common godmode stats
function ServerSyncBlocker.enableGodModeProtection()
    ServerSyncBlocker.enableStats()
    
    -- Protect all common godmode stats
    ServerSyncBlocker.protectStatValue("ENDURANCE", 1.0)
    ServerSyncBlocker.protectStatValue("HUNGER", 0.0)
    ServerSyncBlocker.protectStatValue("THIRST", 0.0)
    ServerSyncBlocker.protectStatValue("FATIGUE", 0.0)
    ServerSyncBlocker.protectStatValue("STRESS", 0.0)
    ServerSyncBlocker.protectStatValue("PANIC", 0.0)
    ServerSyncBlocker.protectStatValue("PAIN", 0.0)
    ServerSyncBlocker.protectStatValue("SICKNESS", 0.0)
    ServerSyncBlocker.protectStatValue("BOREDOM", 0.0)
    ServerSyncBlocker.protectStatValue("UNHAPPINESS", 0.0)
    
    print("[EtherHack] GodMode stats protection enabled")
end

--============================================================================
-- Vehicle protection: unconditional hotwire + keyless engine start.
-- Blocks VehicleUpdate downlink so the server cannot revert local hotwire/
-- engine state, keeps the engine running locally, repeatedly replays the
-- hotwire TimedAction to the server until it authorizes hotwired=true, and
-- sends the vanilla startEngine command (server trusts client's haveKey) to
-- authorize the engine. After 30s the protection auto-disables and the
-- server-authorized state is read; if the server authorized both hotwire and
-- engine, the vehicle stays usable cleanly, otherwise it reverts.
--============================================================================

function ServerSyncBlocker.enableVehicle()
    if enableVehicleProtection then enableVehicleProtection() end
    ServerSyncBlocker.vehicleProtection = true
    ServerSyncBlocker.vehicleConfirming = false
    ServerSyncBlocker.vehicleConfirmDue = getTimestampMs() + 30000
    ServerSyncBlocker.vehicleRetryDue = 0
    print("[EtherHack] Vehicle protection enabled - hotwire & engine will be kept locally, server authorization in progress")
    return true
end

function ServerSyncBlocker.disableVehicle()
    if disableVehicleProtection then disableVehicleProtection() end
    ServerSyncBlocker.vehicleProtection = false
    ServerSyncBlocker.vehicleConfirming = false
    print("[EtherHack] Vehicle protection disabled")
    return true
end

function ServerSyncBlocker.isVehicleActive()
    return ServerSyncBlocker.vehicleProtection
end

local function vehicleAndDriver()
    local player = getPlayer()
    if not player then return nil end
    local vehicle = player:getVehicle()
    if not vehicle or not vehicle:isDriver(player) then return nil end
    return vehicle
end

function ServerSyncBlocker.applyVehicleProtection()
    local vehicle = vehicleAndDriver()
    if not vehicle then return end
    vehicle:setHotwired(true)
    if not vehicle:isEngineRunning() then
        vehicle:tryStartEngine(true)
    end
    local now = getTimestampMs()
    if now >= ServerSyncBlocker.vehicleRetryDue then
        ServerSyncBlocker.vehicleRetryDue = now + 5000
        ISTimedActionQueue.add(ISHotwireVehicle:new(getPlayer()))
        sendClientCommand(getPlayer(), "vehicle", "startEngine", {haveKey = true})
    end
end

function ServerSyncBlocker.confirmVehicleProtection()
    ServerSyncBlocker.vehicleConfirmTicks = ServerSyncBlocker.vehicleConfirmTicks + 1
    if ServerSyncBlocker.vehicleConfirmTicks < 2 then return end
    local vehicle = vehicleAndDriver()
    local ok = vehicle ~= nil and vehicle:isHotwired() and vehicle:isEngineRunning()
    ServerSyncBlocker.vehicleProtection = false
    ServerSyncBlocker.vehicleConfirming = false
    if EtherCharacterPanel and EtherCharacterPanel.vehicleCheckbox then
        EtherCharacterPanel.vehicleCheckbox:setSelected(false)
    end
    if ok then
        print("[EtherHack] Vehicle hotwire & engine authorized by server, protection auto-disabled")
    else
        print("[EtherHack] Vehicle protection auto-disabled after 30s (server did not authorize)")
    end
end

-- Register for OnTick to periodically reapply values
local function onTick()
    if ServerSyncBlocker.isActive() then
        ServerSyncBlocker.filterPackets()
        ServerSyncBlocker.reapply()
    end
    if ServerSyncBlocker.vehicleProtection then
        if ServerSyncBlocker.vehicleConfirming then
            ServerSyncBlocker.confirmVehicleProtection()
        else
            ServerSyncBlocker.applyVehicleProtection()
            if getTimestampMs() >= ServerSyncBlocker.vehicleConfirmDue then
                if disableVehicleProtection then disableVehicleProtection() end
                ServerSyncBlocker.vehicleConfirming = true
                ServerSyncBlocker.vehicleConfirmTicks = 0
            end
        end
    end
end

-- Hook into game events
if Events and Events.OnTick then
    Events.OnTick.Add(onTick)
end

print("[EtherHack] ServerSyncBlocker Lua wrapper loaded")

return ServerSyncBlocker
