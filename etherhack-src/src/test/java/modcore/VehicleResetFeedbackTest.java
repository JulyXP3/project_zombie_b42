package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 三百三十: 「车辆重置」按钮必须把后端结果码翻译成可见提示。
 *
 * 旧实现丢弃 `autoDriveResetVehicle()` 的返回值, 正常姿态下车时后端是合法零动作,
 * 玩家看到的就是"点了没反应"。这里逐个结果码验证面板文案。
 */
class VehicleResetFeedbackTest {

    @Test
    void resetButtonReportsTheBackendResult() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute(setup());
        lua.resource("modcore/lua/components/drive/EtherDriveModule.lua");
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherFormPanel })
                EtherDriveModule_addTo(panel)
                local btn
                for i = 1, #buttons do
                    if buttons[i].title == "UI_DrivePanel_ResetVehicle" then btn = buttons[i] end
                end
                assert(btn ~= nil, "reset button not built")
                resetResult = "lifted"; btn.callback()
                resetResult = "blocked-chunks"; btn.callback()
                resetResult = "no-vehicle"; btn.callback()
                resetResult = "noop"; btn.callback()
                return halo[1], halo[2], halo[3], halo[4]
                """);
        assertArrayEquals(new Object[]{
                "UI_DrivePanel_ResetLifted",
                "UI_DrivePanel_ResetBlocked",
                "UI_DrivePanel_ResetNoVehicle",
                "UI_DrivePanel_ResetNoop"}, result,
                "every reset result must produce a visible message");
    }

    @Test
    void pendingResetShowsCompletionOnlyAfterTheBackendConfirmsLanding() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute(setup());
        lua.resource("modcore/lua/components/drive/EtherDriveModule.lua");
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherFormPanel })
                EtherDriveModule_addTo(panel)
                local btn
                for i = 1, #buttons do
                    if buttons[i].title == "UI_DrivePanel_ResetVehicle" then btn = buttons[i] end
                end
                resetResult = "checking"; btn.callback()
                clock = 100; resetTick()
                assert(#halo == 1, "pending must not produce a success message")
                resetResult = "dropped"; clock = 200; resetTick()
                clock = 300; resetTick()
                return halo[1], halo[2], #halo
                """);
        assertArrayEquals(new Object[]{"UI_DrivePanel_ResetChecking", "UI_DrivePanel_ResetDropped", 2.0}, result);
    }

    @Test
    void failedLandingIsNotRenderedAsANoop() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute(setup());
        lua.resource("modcore/lua/components/drive/EtherDriveModule.lua");
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherFormPanel })
                EtherDriveModule_addTo(panel)
                for i = 1, #buttons do
                    if buttons[i].title == "UI_DrivePanel_ResetVehicle" then
                        resetResult = "checking"; buttons[i].callback()
                    end
                end
                resetResult = "timeout-unsupported"; clock = 200; resetTick()
                return halo[2]
                """);
        assertArrayEquals(new Object[]{"UI_DrivePanel_ResetFailed"}, result);
    }

    private static String setup() {
        return """
                UIRowBox = { onMouseDown = function() end, onMouseUp = function() end, onMouseMove = function() end }
                UICheckbox = { new = function(self, x, y, title, checked, callback)
                    return { x=x, y=y, title=title, checked=checked, callback=callback,
                        initialise=function() end, instantiate=function() end,
                        setCheked=function(self, v) self.checked = v end }
                end }
                EtherTheme.makeLabel = function(x, y, h, text)
                    return { x=x, y=y, height=h, text=text,
                        initialise=function() end, instantiate=function() end }
                end
                EtherFormPanel = setmetatable({
                    BOX_PAD_X = 10, BOX_PAD_Y = 3, ROW_STEP = 26, SECTION_GAP = 12, ENTRY_W = 100,
                    derive = function(self) return setmetatable({}, { __index = self }) end,
                    addWidget = function(self, control) table.insert(self.children, control) end,
                    addChild = function(self, control) table.insert(self.children, control) end,
                    addSpacer = function() end,
                    _rowContentW = function() return 700 end,
                    _anchor = function() end,
                    addModule = function(self, key, h, builder)
                        table.insert(modules, { key = key, height = h })
                        builder(0, 0, 700)
                    end,
                }, { __index = ISPanel })
                modules = {}
                clock = 0
                function getTimestampMs() return clock end
                Events = { OnTick = { Add = function(callback) resetTick = callback end } }
                resetResult = "noop"
                halo = {}
                HaloTextHelper = {
                    addText = function(who, text) table.insert(halo, text) end,
                    getColorRed = function() return {} end,
                    getColorGreen = function() return {} end,
                }
                function autoDriveGetStateId() return 0 end
                function autoDriveIsActive() return false end
                function autoDriveGetTargetX() return 0 end
                function autoDriveGetTargetY() return 0 end
                function autoDriveGetMessage() return "" end
                function autoDriveGetSpeedLimit() return 70 end
                function autoDriveGetHardCap() return 60 end
                function autoDriveGetDiagnostics() return 0 end
                function autoDriveSetDiagnostics() end
                function autoDriveGetCruiseSpeed() return 0 end
                function autoDriveSetCruiseSpeed() end
                function autoDriveResetVehicle()
                    return resetResult ~= "no-vehicle" and resetResult ~= "blocked-chunks"
                end
                function autoDriveGetResetResult() return resetResult end
                """;
    }
}
