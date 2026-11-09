package modcore;

import java.io.IOException;

/** Lightweight UI boundaries for executing production Lua panel builders in Kahlua. */
final class LuaPanelRuntime {
    private LuaPanelRuntime() {}

    static LuaTestRuntime create() throws IOException {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function require() end
                function print() end
                UIFont = { Small = 1, NewSmall = 2, Medium = 3, Large = 4 }
                function getTextManager()
                    return {
                        getFontHeight = function() return 16 end,
                        MeasureStringX = function(self, font, text) return #text * 6 end,
                    }
                end
                translations = {
                    UI_TrapSpawn_SearchLabel = "Search food",
                    UI_Exchange_SearchId = "ID",
                    UI_TrapSpawn_Count = "Count",
                }
                function getTranslate(key) return translations[key] or key end
                function tr(key, params) return getTranslate(key) end
                player = {}
                function getPlayer() return player end
                function isMultiplayer() return true end
                local control = {
                    initialise = function() end, instantiate = function() end,
                    setClearButton = function() end, setVisible = function(self, v) self.visible = v end,
                    setEditable = function(self, v) self.editable = v end,
                    setEnable = function(self, v) self.enable = v end,
                    getInternalText = function(self) return self.text end,
                    addChild = function(self, child) table.insert(self.children, child) end,
                    clear = function(self) self.items = {} end,
                    addItem = function(self, text, item)
                        table.insert(self.items, { text = text, item = item })
                    end,
                    addColumn = function() end,
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
                    measureWidth = function(text) return #text * 6 + 16 end,
                    measureGroupWidth = function(titles)
                        local w = 0
                        for _, title in ipairs(titles) do w = math.max(w, #title * 6 + 16) end
                        return w
                    end,
                    new = function(self, x, y, w, h, title, callback)
                        local button = make(x, y, w, h)
                        button.title = title; button.callback = callback
                        table.insert(buttons, button)
                        return button
                    end,
                }
                EtherFastList = { new = function(self, x, y, w, h) return make(x, y, w, h) end }
                ISComboBox = { new = function(self, x, y, w, h, target, callback)
                    local combo = make(x, y, w, h)
                    combo.target = target; combo.callback = callback; combo.options = {}
                    combo.addOptionWithData = function(self, text, data)
                        table.insert(self.options, { text = text, data = data })
                    end
                    combo.getSelectedData = function(self) return self.data end
                    return combo
                end }
                EtherTheme = {
                    ctrlGap = 6, ctrlH = 24, fontHgtSmall = 16, fontHgtMedium = 20,
                    entryH = 24, entryLabelDY = 4, entryBtnDY = 0,
                    fontHgtHint = 12, listItemH = 24, listHeaderH = 24,
                    text = {}, textDim = {}, statusRed = {},
                    styleEntry = function() end, styleList = function() end, styleCombo = function() end,
                    wrapHint = function(text) return { text } end,
                    wrapText = function(text) return { text } end,
                }
                function getAllItems() return { size = function() return 0 end } end
                string.sort = function(a, b) return a > b end
                function getAmmoFarmCount() return 30 end
                EtherAmmoFarm = { enabled = false }
                EtherSpawnQueue = { running = false, count = function() return 0 end }
                EtherFishSpawn = {}; EtherCorpseSpawn = {}; EtherBoxPoison = {}
                """);
        return lua;
    }
}
