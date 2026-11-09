package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class VehiclePanelTest {
    @Test
    void remainingControlsBuildAndSingleStartKeepsTheCurrentVehicleTarget() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherVehiclePanel })
                panel:build()
                buttons[1].callback()
                return #modules, #buttons, #checkboxes, buttons[1].title,
                    commands[1].module, commands[1].command,
                    EtherVehiclePanel.checkboxByKey["UI_VehiclePanel_RepairAuto"] ~= nil,
                    EtherVehiclePanel.checkboxByKey["UI_VehiclePanel_InstantStart"] ~= nil,
                    EtherVehiclePanel.checkboxByKey["UI_Exploit_VehicleProtection"] == nil
                """);
        assertArrayEquals(new Object[]{2.0, 1.0, 2.0, "UI_VehiclePanel_StartNow",
                "vehicle", "startEngine", true, true, true}, result,
                "the duplicate any-key auto entry is removed; single + auto-retry remain");
    }

    @Test
    void repairCheckboxStartsAndStopsTheJob() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherVehiclePanel })
                panel:build()
                local cb = EtherVehiclePanel.checkboxByKey["UI_VehiclePanel_RepairAuto"]
                cb.callback(true)
                local started = EtherRepairJob ~= nil
                cb.callback(false)
                return started, EtherRepairJob == nil, cb.checked
                """);
        assertArrayEquals(new Object[]{true, true, false}, result,
                "checking starts the job and unchecking stops it and clears the checkbox");
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                Events = { OnTick = { Add = function(callback) repairTick = callback end } }
                modules = {}; commands = {}; checkboxes = {}
                local vehicle = {}
                vehicle.isDriver = function() return true end
                vehicle.isStopped = function() return true end
                player.getVehicle = function() return vehicle end
                function getTimestampMs() return 0 end
                function sendClientCommand(owner, module, command, args)
                    assert(owner == player)
                    table.insert(commands, { module = module, command = command, args = args })
                end
                function isVehicleInstantStart() return false end
                UICheckbox = { new = function(self, x, y, title, checked, callback)
                    local control = { x=x, y=y, title=title, callback=callback, checked=checked }
                    control.setCheked = function(self, checked) self.checked = checked end
                    table.insert(checkboxes, control)
                    return control
                end }
                EtherFormPanel = setmetatable({
                    BOX_PAD_X = 10, BOX_PAD_Y = 3, ROW_STEP = 26, SECTION_GAP = 12,
                    derive = function(self) return setmetatable({}, { __index = self }) end,
                    addWidget = function(self, control) table.insert(self.children, control) end,
                    addSpacer = function() end,
                    _rowContentW = function() return 700 end,
                    _anchor = function() end,
                    addModule = function(self, key, h, builder)
                        table.insert(modules, { key = key, height = h })
                        builder(0, 0, 700)
                    end,
                }, { __index = ISPanel })
                ModuleHint = { new = function(self, x, y, w, text)
                    return { x=x, y=y, width=w, text=text,
                        initialise=function() end, instantiate=function() end }
                end }
                """);
        lua.resource("modcore/lua/components/panels/EtherVehiclePanel.lua");
        return lua;
    }
}
