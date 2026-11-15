package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MinimapActionsTest {
    @Test
    void minimapAndPanelKeepBothFootTeleportsAndAllNavigationOptions() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        Object[] result = lua.execute("""
                routeCount=2
                local mini=UIMap:new(10,40,280,250)
                local panel=UIMap:new(16,20,560,300)
                mini:onRightMouseUp(140,125)
                local first=#menu.options
                choose("UI_Map_TeleportContext")
                mini:onRightMouseUp(160,125)
                choose("UI_Map_TeleportLinear")
                choose("UI_Drive_MapAnchor")
                choose("UI_Drive_MapResume")
                choose("UI_Drive_MapClearRoute")
                panel:onRightMouseUp(280,150)
                return first,#menu.options,player.x,player.y,anchoredX,anchoredY,resumed,cleared
                """);
        assertArrayEquals(new Object[]{5.0, 5.0, 120.0, 100.0, 120.0, 100.0, true, true}, result);
    }

    @Test
    void vehicleOptionReplacesFootModesAndPassengersRemainRejected() throws Exception {
        Object[] result = MinimapTestRuntime.create().execute("""
                player.vehicle={ isDriver=function() return false end }
                local mini=UIMap:new(0,0,280,250)
                mini:onRightMouseUp(140,125)
                local first=menu.options[1].name
                choose("UI_Map_VehTeleportContext")
                return first,#menu.options,player.x,player.y,player.halo
                """);
        assertArrayEquals(new Object[]{"UI_Map_VehTeleportContext", 2.0, 100.0, 100.0,
                "UI_Map_VehTeleportDriverOnly"}, result);
    }

    @Test
    void worldAndInventoryMapsAppendWithoutClearingTheOriginalMenu() throws Exception {
        Object[] result = MinimapTestRuntime.create().execute("""
                originalMenu=true
                routeCount=2
                local world=ISWorldMap:new(10,20,400,200)
                world:onRightMouseUp(200,100)
                local count=#menu.options
                local preserved=menu.options[1].name
                local gets=menuGets
                local item=ISMap:new(30,40,300,200)
                item:onRightMouseUp(150,100)
                return count,preserved,gets,#menu.options,menu.options[1].name,menuGets
                """);
        assertArrayEquals(new Object[]{6.0, "original", 1.0, 6.0, "original", 2.0}, result);
    }

    @Test
    void symbolEditingDoesNotOpenAMenuAndInvalidChunksKeepNavigation() throws Exception {
        Object[] result = MinimapTestRuntime.create().execute("""
                symbolsConsumed=true
                local world=ISWorldMap:new(0,0,400,200)
                local consumed=world:onRightMouseUp(200,100)
                local untouched=menuGets
                validChunk=false
                local mini=UIMap:new(0,0,280,250)
                mini:onRightMouseUp(140,125)
                return consumed,untouched,#menu.options,menu.options[1].name
                """);
        assertArrayEquals(new Object[]{true, 0.0, 1.0, "UI_Drive_MapAnchor"}, result);
    }

    @Test
    void multiplayerFootMoveKeepsItsPerTickCapAndInputCancellation() throws Exception {
        Object[] result = MinimapTestRuntime.create().execute("""
                multiplayer=true
                function isClient() return true end
                function teleportClimbState() return true end
                function teleportClimbReady() return true end
                function teleportClimbPending() return false end
                local view=UIMap:new(0,0,280,250)
                view:onTeleportLinear(110,100)
                Events.OnTick.fire()
                now=now+100
                Events.OnTick.fire()
                local after=player.x
                keys[Keyboard.KEY_W]=true
                now=now+100
                Events.OnTick.fire()
                return after,player.x,player.y,UIMap.lastTeleportCancel
                """);
        assertArrayEquals(new Object[]{101.0, 101.0, 100.0, "input"}, result);
    }

    @Test
    void reloadingNavigationHooksDoesNotDuplicateMenus() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        lua.resource("modcore/lua/components/drive/AutoDriveMap.lua");
        Object[] result = lua.execute("""
                originalMenu=true
                routeCount=2
                local world=ISWorldMap:new(0,0,400,200)
                world:onRightMouseUp(200,100)
                return #menu.options,menuGets
                """);
        assertArrayEquals(new Object[]{6.0, 1.0}, result);
    }

    @Test
    void actionReloadLeavesOnlyOneTickHandlerPerStateMachine() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        MinimapTestRuntime.loadOptional(lua, "MapActions");
        lua.resource("modcore/lua/components/ui/UIMap.lua");
        Object[] result = lua.execute("return #Events.OnTick.handlers");
        assertArrayEquals(new Object[]{2.0}, result);
    }
}
