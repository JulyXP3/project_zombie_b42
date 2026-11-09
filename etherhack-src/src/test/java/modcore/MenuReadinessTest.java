package modcore;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MenuReadinessTest {
    @Test
    void requiredModuleFailureStopsBeforeMenuAndKeyRegistration() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                calls = 0
                function loadModuleScript(path)
                    calls = calls + 1
                    return false
                end
                """);
        var result = lua.execute("""
                local ok, failure = pcall(function() MENU_SOURCE() end)
                return ok, failure
                """.replace("MENU_SOURCE()", source()));
        assertEquals(Boolean.FALSE, result[0]);
        assertTrue(String.valueOf(result[1]).contains("Required module failed"), String.valueOf(result[1]));
        assertEquals(1.0, lua.environment.rawget("calls"));
        assertNull(lua.environment.rawget("EtherMain"));
    }

    @Test
    void menuRecordsOnlyActualVisibleOpenAndRegistersMenuKeyOnce() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                function loadModuleScript() return true end
                function getAccentUIColor() return { getR=function() return 1 end, getG=function() return 1 end, getB=function() return 1 end } end
                function getTimestampMs() return 0 end
                function setMainPanelPos() end
                local event = { Add=function() end, Remove=function() end }
                Events = { EveryTenMinutes=event, OnGameStart=event }
                Keyboard = { KEY_GRAVE=41 }
                registered = 0
                EtherKeyBinds = { registry={} }
                function EtherKeyBinds.register(id, label, handler, key)
                    registered = registered + 1
                    EtherKeyBinds.registry[id] = {handler=handler}
                end
                opened = 0
                function recordProjectUIOpened() opened = opened + 1 end
                function getProjectLaunchId() return "fixture-build/session" end
                cancelled = 0
                EtherWornSpawn = { cancel=function() cancelled = cancelled + 1 end }
                """);
        lua.execute(source());
        assertEquals(2.0, lua.environment.rawget("registered"));
        assertEquals(0.0, lua.environment.rawget("opened"));
        lua.execute("""
                EtherMain.instance = {
                    visible=false, x=0, y=0,
                    getIsVisible=function(self) return self.visible end,
                    setVisible=function(self, value) self.visible=value end,
                    addToUIManager=function() end, removeFromUIManager=function() end,
                }
                EtherMain.toggleMenu()
                """);
        assertEquals(1.0, lua.environment.rawget("opened"));
        assertEquals(0.0, lua.environment.rawget("cancelled"));
        lua.execute("EtherMain.toggleMenu()");
        assertEquals(1.0, lua.environment.rawget("opened"));
        assertEquals(1.0, lua.environment.rawget("cancelled"));
        lua.execute("EtherMain.toggleMenu(); EtherMain:close()");
        assertEquals(2.0, lua.environment.rawget("opened"));
        assertEquals(2.0, lua.environment.rawget("cancelled"));
    }

    @Test
    void registeringMenuAfterRetryDoesNotDuplicateItsDispatchSlot() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("Events = { OnKeyPressed = { Add=function() end } }");
        lua.resource("modcore/lua/components/ui/EtherKeyBinds.lua");
        lua.execute("""
                EtherKeyBinds.register("menu", "label", function() end, 210)
                EtherKeyBinds.register("menu", "label", function() end, 210)
                """);
        var result = lua.execute("return #EtherKeyBinds.order");
        assertEquals(1.0, result[0]);
    }

    private static String source() throws Exception {
        try (var in = MenuReadinessTest.class.getClassLoader().getResourceAsStream("modcore/lua/modcoreMenu.lua")) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
