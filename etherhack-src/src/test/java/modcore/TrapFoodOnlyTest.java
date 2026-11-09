package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TrapFoodOnlyTest {
    @Test
    void obsoleteModeStateCannotSwitchTheListBackToWeapons() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function require() end
                UIFont = { Small = 1 }
                function getTextManager()
                    return { getFontHeight = function() return 16 end }
                end
                ISPanel = { derive = function(self) return {} end }
                ItemType = { WEAPON = 1 }
                string.sort = function(a, b) return a > b end
                function scriptItem(id, hunger, weapon, hidden, obsolete)
                    return {
                        getName = function() return id end,
                        getFullName = function() return "Base." .. id end,
                        getDisplayName = function() return id end,
                        getHungerChange = function() return hunger end,
                        isItemType = function() return weapon end,
                        isHidden = function() return hidden end,
                        getObsolete = function() return obsolete end,
                    }
                end
                items = {
                    scriptItem("Ham", -20, false, false, false),
                    scriptItem("Axe", 0, true, false, false),
                    scriptItem("HiddenFood", -10, false, true, false),
                    scriptItem("OldFood", -10, false, false, true),
                }
                function getAllItems()
                    return {
                        size = function() return #items end,
                        get = function(self, i) return items[i + 1] end,
                    }
                end
                """);
        lua.resource("modcore/lua/components/panels/EtherTrapSpawn.lua");
        Object[] result = lua.execute("""
                local data = {
                    items = {},
                    clear = function(self) self.items = {} end,
                    addItem = function(self, text, item)
                        table.insert(self.items, { text = text, item = item })
                    end,
                }
                local entry = { getInternalText = function() return "" end }
                local panel = setmetatable({
                    mode = "weapon", datas = data, searchBox = entry, searchId = entry,
                }, { __index = EtherTrapSpawn })
                panel:initList()
                return #data.items, data.items[1].item:getFullName()
                """);
        assertArrayEquals(new Object[]{1.0, "Base.Ham"}, result);
    }

    @Test
    void foodCollectionStillCompletesRequestedCyclesAndStops() throws Exception {
        LuaTestRuntime lua = foodRuntime();
        Object[] result = lua.execute("""
                EtherTrapPOC.setTarget("Base.Ham")
                EtherTrapPOC.count = 2
                EtherTrapPOC.trigger()
                tick()
                now = 1000; tick()
                now = 4001; tick()
                tick()
                now = 5001; tick()
                now = 8002; tick()
                return #sent, #collected, sent[1].animal.item, sent[1].animal.minSize,
                    EtherTrapPOC.cyclesDone, EtherTrapPOC.armed, EtherTrapPOC.phase
                """);
        assertArrayEquals(new Object[]{2.0, 2.0, "Base.Ham", 20.0, 2.0, false, "idle"}, result);
    }

    @Test
    void cancellingStopsFurtherFoodCommandsAndActions() throws Exception {
        LuaTestRuntime lua = foodRuntime();
        Object[] result = lua.execute("""
                EtherTrapPOC.trigger()
                tick()
                EtherTrapPOC.cancel()
                now = 10000; tick()
                return #sent, #collected, EtherTrapPOC.armed, EtherTrapPOC.phase
                """);
        assertArrayEquals(new Object[]{1.0, 0.0, false, "idle"}, result);
    }

    @Test
    void panelHasOneSpawnActionAndPassesTheFoodSelectionAndCount() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function require() end
                UIFont = { Small = 1, NewSmall = 2 }
                function getTextManager()
                    return {
                        getFontHeight = function() return 16 end,
                        MeasureStringX = function(self, font, text) return #text * 6 end,
                    }
                end
                function getTranslate(key) return key end
                function getPlayer() return {} end
                local control = {
                    initialise = function() end, instantiate = function() end,
                    setClearButton = function() end,
                    getInternalText = function(self) return self.text end,
                    addChild = function(self, child) table.insert(self.children, child) end,
                    clear = function(self) self.items = {} end,
                    addItem = function(self, text, item)
                        table.insert(self.items, { text = text, item = item })
                    end,
                }
                local function make(x, y, w, h)
                    return setmetatable({ x=x, y=y, width=w, height=h, children={}, items={} },
                        { __index = control })
                end
                ISPanel = setmetatable({
                    new = function(self, x, y, w, h) return make(x, y, w, h) end,
                    derive = function(self) return setmetatable({}, { __index = self }) end,
                    createChildren = function() end,
                }, { __index = control })
                ISTextEntryBox = { new = function(self, text, x, y, w, h)
                    local entry = make(x, y, w, h); entry.text = text; return entry
                end }
                buttons = {}
                UIButton = {
                    measureWidth = function() return 90 end,
                    new = function(self, x, y, w, h, title, callback)
                        local button = make(x, y, w, h)
                        button.title = title; button.callback = callback
                        table.insert(buttons, button)
                        return button
                    end,
                }
                EtherFastList = { new = function(self, x, y, w, h) return make(x, y, w, h) end }
                EtherTheme = {
                    ctrlGap = 6, ctrlH = 24, fontHgtSmall = 16, entryH = 24,
                    entryLabelDY = 4, fontHgtHint = 12, listItemH = 24,
                    text = {}, textDim = {},
                    styleEntry = function() end, styleList = function() end,
                    wrapHint = function(text) return { text } end,
                }
                function getAllItems()
                    return { size = function() return 0 end }
                end
                calls = 0
                EtherTrapPOC = {
                    setTarget = function(target) EtherTrapPOC.target = target end,
                    trigger = function() calls = calls + 1 end,
                }
                """);
        lua.resource("modcore/lua/components/panels/EtherTrapSpawn.lua");
        Object[] result = lua.execute("""
                local panel = EtherTrapSpawn:new(0, 0, 800, 650)
                panel:createChildren()
                panel.datas.selected = 1
                panel.datas.items = { { item = { getFullName = function() return "Base.Ham" end } } }
                panel.countEntry.text = "2"
                panel.spawnBtn.callback()
                return #buttons, calls, EtherTrapPOC.target, EtherTrapPOC.count,
                    panel.datas.y + panel.datas.height <= panel.spawnBtn.y,
                    panel.countEntry.y + panel.countEntry.height < panel.datas.y,
                    panel.countEntry.y == panel.searchBox.y,
                    panel.countEntry.x > panel.searchId.x
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, "Base.Ham", 2.0, true, true, true, true}, result);
    }

    private static LuaTestRuntime foodRuntime() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                now = 0
                sent = {}; collected = {}
                function print() end
                function isMultiplayer() return true end
                function getTimestampMs() return now end
                local player = {
                    getX = function() return 10 end,
                    getY = function() return 20 end,
                    getZ = function() return 0 end,
                }
                function getPlayer() return player end
                function instanceItem(target) return { id = target } end
                function itemBaseHunger(item) return -0.2 end
                local trap = { x = 10, y = 20, z = 0 }
                CTrapSystem = { instance = {
                    getLuaObjectAt = function(self, x, y, z)
                        if x == 10 and y == 20 and z == 0 then return trap end
                    end,
                    sendCommand = function(self, owner, command, args)
                        assert(owner == player and command == "addAnimalDebug")
                        assert(args.x == 10 and args.y == 20 and args.z == 0)
                        table.insert(sent, args)
                    end,
                } }
                ISCheckTrapAction = { new = function(self, owner, target)
                    assert(owner == player and target == trap)
                    return { owner = owner, target = target }
                end }
                ISTimedActionQueue = { add = function(action) table.insert(collected, action) end }
                Events = { OnTick = { Add = function(callback) tick = callback end } }
                """);
        lua.resource("modcore/lua/components/ui/EtherTrapPOC.lua");
        return lua;
    }
}
