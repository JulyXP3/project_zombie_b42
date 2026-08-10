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

-- Cache for Java methods (may be nil if not yet exposed)
local _enableFullProtection = enableFullProtection
local _disableFullProtection = disableFullProtection
local _enableStatsProtection = enableStatsProtection
local _disableStatsProtection = disableStatsProtection
local _enableSkillsProtection = enableSkillsProtection
local _disableSkillsProtection = disableSkillsProtection
local _protectStat = protectStat
local _unprotectStat = unprotectStat
local _protectSkill = protectSkill
local _isProtectionActive = isProtectionActive
local _reapplyProtectedValues = reapplyProtectedValues
local _filterIncomingSyncPackets = filterIncomingSyncPackets

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
    if _enableFullProtection then
        _enableFullProtection()
    end
    ServerSyncBlocker.isEnabled = true
    print("[EtherHack] Full server sync protection enabled")
    return true
end

-- Disable all protection
function ServerSyncBlocker.disableFull()
    if _disableFullProtection then
        _disableFullProtection()
    end
    ServerSyncBlocker.isEnabled = false
    ServerSyncBlocker.statsProtected = {}
    ServerSyncBlocker.skillsProtected = {}
    print("[EtherHack] Server sync protection disabled")
    return true
end

-- Enable stats-only protection
function ServerSyncBlocker.enableStats()
    if _enableStatsProtection then
        _enableStatsProtection()
        return true
    end
    return false
end

-- Disable stats protection
function ServerSyncBlocker.disableStats()
    if _disableStatsProtection then
        _disableStatsProtection()
        return true
    end
    return false
end

-- Enable skills protection
function ServerSyncBlocker.enableSkills()
    if _enableSkillsProtection then
        _enableSkillsProtection()
        return true
    end
    return false
end

-- Disable skills protection
function ServerSyncBlocker.disableSkills()
    if _disableSkillsProtection then
        _disableSkillsProtection()
        return true
    end
    return false
end

-- Protect a specific stat with a value
-- statName: ENDURANCE, HUNGER, THIRST, FATIGUE, etc.
-- value: the value to maintain (usually 0.0 or 1.0)
function ServerSyncBlocker.protectStatValue(statName, value)
    if _protectStat then
        _protectStat(statName, value)
        ServerSyncBlocker.statsProtected[statName] = value
        return true
    end
    return false
end

-- Remove protection from a stat
function ServerSyncBlocker.unprotectStatValue(statName)
    if _unprotectStat then
        _unprotectStat(statName)
        ServerSyncBlocker.statsProtected[statName] = nil
        return true
    end
    return false
end

-- Protect a skill at a specific level and XP
function ServerSyncBlocker.protectSkillLevel(perkName, level, xp)
    xp = xp or 0
    if _protectSkill then
        _protectSkill(perkName, level, xp)
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
    if _reapplyProtectedValues then
        _reapplyProtectedValues()
    end
end

-- Manually trigger packet filtering
function ServerSyncBlocker.filterPackets()
    if _filterIncomingSyncPackets then
        _filterIncomingSyncPackets()
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

-- Register for OnTick to periodically reapply values
local function onTick()
    if ServerSyncBlocker.isActive() then
        ServerSyncBlocker.filterPackets()
        ServerSyncBlocker.reapply()
    end
end

-- Hook into game events
if Events and Events.OnTick then
    Events.OnTick.Add(onTick)
end

print("[EtherHack] ServerSyncBlocker Lua wrapper loaded")

return ServerSyncBlocker
