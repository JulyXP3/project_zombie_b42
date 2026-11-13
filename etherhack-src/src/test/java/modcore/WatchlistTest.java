package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 多选追踪清单: 只存物品全名, 不统计数量。 */
class WatchlistTest {
    @Test
    void togglingAddsAndRemovesAndReportsTheChange() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("")
                local a = EtherWatchlist.toggle("Base.WoodNails")
                local inList = EtherWatchlist.contains("Base.WoodNails")
                local b = EtherWatchlist.toggle("Base.WoodNails")
                return a, inList, b, EtherWatchlist.contains("Base.WoodNails"), EtherWatchlist.count()
                """);
        assertArrayEquals(new Object[]{"added", true, "removed", false, 0.0}, result,
                "toggling twice returns to an empty list");
    }

    @Test
    void duplicateEntriesAreCollapsedOnLoad() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("Base.WoodNails; Base.Plank ;Base.WoodNails;")
                return EtherWatchlist.count(), EtherWatchlist.encode()
                """);
        assertArrayEquals(new Object[]{2.0, "Base.WoodNails;Base.Plank"}, result,
                "load trims blanks and keeps insertion order without duplicates");
    }

    @Test
    void listIsCappedAtFiftyTypes() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("")
                local last = "added"
                for i = 1, 51 do
                    last = EtherWatchlist.toggle("Base.Item" .. i)
                end
                return EtherWatchlist.count(), last, EtherWatchlist.contains("Base.Item51")
                """);
        assertArrayEquals(new Object[]{50.0, "full", false}, result,
                "the 51st distinct type is rejected instead of growing without bound");
    }

    @Test
    void invalidNamesAreRejected() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("")
                return EtherWatchlist.toggle(""), EtherWatchlist.toggle(nil),
                    EtherWatchlist.toggle("Base.Item;Other"), EtherWatchlist.count()
                """);
        assertArrayEquals(new Object[]{"bad", "bad", "bad", 0.0}, result,
                "empty, nil and separator-containing names are rejected");
    }

    @Test
    void targetsExposeOnlySelectedTypes() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("Base.WoodNails;Base.Plank")
                local t = EtherWatchlist.targets()
                local n = 0
                for k, v in pairs(t) do if v then n = n + 1 end end
                return n, t["Base.WoodNails"], t["Base.Axe"] == nil
                """);
        assertArrayEquals(new Object[]{2.0, true, true}, result,
                "targets() is the set handed to the existing radar scan");
    }

    @Test
    void entriesCanBeRemovedByIndex() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("Base.A;Base.B;Base.C")
                local ok = EtherWatchlist.removeAt(2)
                return ok, EtherWatchlist.count(), EtherWatchlist.encode(), EtherWatchlist.contains("Base.B")
                """);
        assertArrayEquals(new Object[]{true, 2.0, "Base.A;Base.C", false}, result,
                "the popup removes exactly the selected list entry");
    }

    @Test
    void clearRemovesEverything() throws Exception {
        Object[] result = runtime().execute("""
                EtherWatchlist.load("Base.WoodNails;Base.Plank")
                EtherWatchlist.clear()
                return EtherWatchlist.count(), EtherWatchlist.encode()
                """);
        assertArrayEquals(new Object[]{0.0, ""}, result, "clear empties the list and its encoding");
    }

    @Test
    void popupOpensFromTheListButton() throws Exception {
        LuaTestRuntime lua = popupRuntime();
        Object[] result = lua.execute("""
                EtherWatchlist.load("Base.WoodNails;Base.Plank")
                EtherWatchlistPopup.open(function() popupChanged = true end)
                local popup = EtherWatchlistPopup.instance
                -- 测试替身 instantiate 不回调 createChildren (游戏内会), 此处手动建控件
                popup:createChildren()
                return popup ~= nil, popup.inUIManager, #buttons, #popup.children
                """);
        assertArrayEquals(new Object[]{true, true, 2.0, 2.0}, result,
                "the list button opens a popup with clear/close controls");
    }

    @Test
    void popupKeepsAFixedSizeRegardlessOfEntryCount() throws Exception {
        Object[] result = popupRuntime().execute("""
                EtherWatchlist.load("")
                EtherWatchlistPopup.open(nil)
                local emptyW, emptyH = EtherWatchlistPopup.instance.width, EtherWatchlistPopup.instance.height
                EtherWatchlistPopup.instance:close()
                EtherWatchlist.load("Base.A;Base.B;Base.C;Base.D;Base.E;Base.F;Base.G;Base.H")
                EtherWatchlistPopup.open(nil)
                local fullW, fullH = EtherWatchlistPopup.instance.width, EtherWatchlistPopup.instance.height
                return emptyW, emptyH, emptyW == fullW, emptyH == fullH
                """);
        assertArrayEquals(new Object[]{430.0, 252.0, true, true}, result,
                "popup size is fixed (430x252) and does not track entry count");
    }

    @Test
    void popupResolvesFullNamesToDisplayNames() throws Exception {
        Object[] result = popupRuntime().execute("""
                getItemNameFromFullType = function(full) return "Wooden Nails" end
                local shown = EtherWatchlistPopup.displayName("Base.WoodNails")
                getItemNameFromFullType = function(full) return nil end
                local fallback = EtherWatchlistPopup.displayName("Base.WoodNails")
                return shown, fallback
                """);
        assertArrayEquals(new Object[]{"Wooden Nails", "Base.WoodNails"}, result,
                "popup shows the translated item name and falls back to the full type");
    }

    private static LuaTestRuntime popupRuntime() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.resource("modcore/lua/components/ui/EtherWatchlistPopup.lua");
        return lua;
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.resource("modcore/lua/components/ui/EtherWatchlist.lua");
        return lua;
    }
}
