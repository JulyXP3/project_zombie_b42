--[[
    ISChatFix.lua - Compatibility fix for vanilla ISChat null reference bug
    
    This patches the vanilla ISChat:makeFade function to prevent the error:
    "attempted index: setContentTransparency of non-table: null"
    
    This is a known Build 42 bug where self.chatText becomes nil.
]]--

require "ISUI/ISChat"

-- Store the original function
local original_ISChat_makeFade = ISChat.makeFade

-- Override with nil-safe version
function ISChat:makeFade()
    -- Safety check: skip if chatText is nil
    if self.chatText == nil then
        return
    end
    
    -- Call the original function
    original_ISChat_makeFade(self)
end

print("[EtherHack] ISChat compatibility fix loaded")
