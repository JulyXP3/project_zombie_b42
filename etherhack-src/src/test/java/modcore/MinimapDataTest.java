package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MinimapDataTest {
    @Test
    void twoMapsShareOneUiFrameCollectionButProjectIndependently() throws Exception {
        Object[] result = runtime().execute("""
                local small,large=visibleMaps()
                large.mapAPI:centerOn(200,200)
                zombies={actor(100,100),actor(200,200)}
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                small:render()
                large:render()
                return collections,#small.rects,#large.rects,
                    small.rects[1].x,large.rects[1].x
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, 1.0, 95.0, 95.0}, result);
    }

    @Test
    void eachActualUiFrameUpdatesCoordinatesWithoutATimerGate() throws Exception {
        Object[] result = runtime().execute("""
                local small=visibleMaps()
                local z=actor(100,100)
                zombies={z}
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                small:render()
                local first=small.rects[1].x
                z.x=101
                now=now+1
                Events.OnPreUIDraw.fire()
                small:render()
                return collections,first,small.rects[2].x
                """);
        assertArrayEquals(new Object[]{2.0, 95.0, 96.0}, result);
    }

    @Test
    void hiddenMapsAndDisabledLayersDoNotCollectDynamicObjects() throws Exception {
        Object[] result = runtime().execute("""
                local small,large=visibleMaps()
                small.visible=false
                large.visible=false
                UIMap.drawZombies=true
                Events.OnPreUIDraw.fire()
                local hidden=collections
                small.visible=true
                UIMap.drawZombies=false
                Events.OnPreUIDraw.fire()
                return hidden,collections
                """);
        assertArrayEquals(new Object[]{0.0, 0.0}, result);
    }

    @Test
    void newVehiclesAppearNextFrameAndDeletedLabelsLeaveNoObjectCache() throws Exception {
        Object[] result = runtime().execute("""
                local small=visibleMaps()
                UIMap.drawVehicles=true
                small.mapAPI.scale=6
                Events.OnPreUIDraw.fire()
                small:render()
                local vehicle=actor(100,100,"car")
                vehicle.getScriptName=function() return "Base.TestCar" end
                vehicles={vehicle}
                now=now+1
                Events.OnPreUIDraw.fire()
                small:render()
                local withCar=#small.rects
                vehicles={}
                Events.OnPreUIDraw.fire()
                small:render()
                return collections,withCar,#small.rects,#small.labels,
                    UIMap.vehLabelCache == nil
                """);
        assertArrayEquals(new Object[]{3.0, 1.0, 1.0, 2.0, true}, result);
    }

    @Test
    void invisibleFreezeTimeoutDoesNotResurrectAStationaryPlayer() throws Exception {
        Object[] result = runtime().execute("""
                local small=visibleMaps()
                UIMap.drawAllPlayers=true
                local other=actor(105,100,"other")
                other.id=2
                other.invisible=true
                players={other}
                Events.OnPreUIDraw.fire()
                small:render()
                local first=#small.rects
                now=now+5001
                Events.OnPreUIDraw.fire()
                small:render()
                local expired=#small.rects
                now=now+1
                Events.OnPreUIDraw.fire()
                small:render()
                return first,expired,#small.rects
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, 1.0}, result);
    }

    @Test
    void languageChangeRebuildsInvisiblePlayerLabelsWithoutWaitingFiveSeconds() throws Exception {
        Object[] result = runtime().execute("""
                local small=visibleMaps()
                small.mapAPI.scale=2
                UIMap.drawAllPlayers=true
                local other=actor(105,100,"other")
                other.id=2
                other.invisible=true
                players={other}
                translations.UI_Map_InvisibleTag="INV"
                Events.OnPreUIDraw.fire()
                small:render()
                local first=small.labels[2].text
                translations.UI_Map_InvisibleTag="HIDDEN"
                now=now+1
                Events.OnPreUIDraw.fire()
                small:render()
                return first,small.labels[4].text
                """);
        assertArrayEquals(new Object[]{"other INV", "other HIDDEN"}, result);
    }

    @Test
    void newPlayerSessionDiscardsStickyInvisiblePositions() throws Exception {
        Object[] result = runtime().execute("""
                local small=visibleMaps()
                UIMap.drawAllPlayers=true
                local other=actor(105,100,"old-player")
                other.id=2
                other.invisible=true
                players={other}
                Events.OnPreUIDraw.fire()
                small:render()
                local first=#small.rects
                players={}
                player=actor(100,100,"new-session")
                Events.OnPreUIDraw.fire()
                small:render()
                return first,#small.rects,small.localPlayer == player
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, true}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        lua.execute("""
                collections=0
                UIMap.ensureDrawFlags()
                function visibleMaps()
                    local small=UIMap:new(0,0,200,100)
                    local large=UIMap:new(0,0,200,100)
                    UIMovableMiniMap={ instance={ map=small } }
                    EtherMain={ instance={ buttonsPanel={ currentPanel={ map=large } } } }
                    return small,large
                end
                function mapCollectFrame(buffer,flags)
                    collections=collections+1
                    buffer.zombies=buffer.zombies or {}
                    buffer.vehicles=buffer.vehicles or {}
                    buffer.players=buffer.players or {}
                    for _, data in ipairs({buffer.zombies,buffer.vehicles,buffer.players}) do
                        for k in pairs(data) do data[k]=nil end
                    end
                    local function enabled(bit) return math.floor(flags/bit)%2 == 1 end
                    local function add(data,x,y,name,id,inv)
                        data[#data+1]=x
                        data[#data+1]=y
                        if name ~= nil then data[#data+1]=name end
                        if id ~= nil then data[#data+1]=id; data[#data+1]=inv end
                    end
                    if enabled(1) then
                        for _, z in ipairs(zombies) do add(buffer.zombies,z.x,z.y) end
                    end
                    if enabled(2) then
                        for _, v in ipairs(vehicles) do add(buffer.vehicles,v.x,v.y,v:getScriptName()) end
                    end
                    if enabled(4) then
                        for _, p in ipairs(players) do
                            add(buffer.players,p.x,p.y,p.name,tostring(p.id or 1),p.invisible == true)
                        end
                    end
                    buffer.localX=player.x; buffer.localY=player.y; buffer.localName=player.name
                    return true
                end
                """);
        return lua;
    }
}
