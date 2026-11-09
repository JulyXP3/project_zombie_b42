package modcore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PanelCleanupTest {
    @Test
    void lootPanelOffersSupportedSpawnModesWithWorkingCountVisibility() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.resource("modcore/lua/components/panels/EtherLootRollPanel.lua");
        Object[] result = lua.execute("""
                local panel = EtherLootRollPanel:new(0, 0, 800, 650)
                panel:createChildren()
                local fishHidden = panel.countBox.visible == false
                panel.modeCombo.data = "corpse"
                panel.modeCombo.callback(panel, panel.modeCombo)
                return #panel.modeCombo.options, panel.modeCombo.options[1].data,
                    panel.modeCombo.options[2].data, panel.modeCombo.options[3].data,
                    panel.modeCombo.options[4].data,
                    fishHidden, panel.countBox.visible, panel.countBox.editable,
                    panel.datas.y + panel.datas.height < panel.spawnBtn.y
                """);
        // 2026-11-07: 新增「穿戴生成」(worn) —— 四种生成方式。
        assertArrayEquals(new Object[]{4.0, "fish", "corpse", "poison", "worn",
            true, true, true, true}, result);
    }

    @Test
    void trapControlsStayOnOneRowAtNarrowWidthWithLargerLabels() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                function getTextManager()
                    return { getFontHeight = function() return 19 end,
                        MeasureStringX = function(self, font, text)
                            if text == "Search food" then return 90 end
                            if text == "ID" then return 20 end
                            if text == "Count" then return 40 end
                            return #text * 6
                        end }
                end
                EtherTrapPOC = {}
                """);
        lua.resource("modcore/lua/components/panels/EtherTrapSpawn.lua");
        Object[] result = lua.execute("""
                local panel = EtherTrapSpawn:new(0, 0, 440, 650)
                panel:createChildren()
                return panel.searchBox.y == panel.countEntry.y,
                    panel.searchBox.width >= 60, panel.searchId.width >= 60,
                    panel.countEntry.x + panel.countEntry.width <= panel.width - 26
                """);
        assertArrayEquals(new Object[]{true, true, true, true}, result);
    }

    @ParameterizedTest
    @ValueSource(ints = {578, 650, 800})
    void trapControlsShareOneSearchRowWithoutOverlapping(int width) throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("EtherTrapPOC = {}");
        lua.resource("modcore/lua/components/panels/EtherTrapSpawn.lua");
        Object[] result = lua.execute("""
                local panel = EtherTrapSpawn:new(0, 0, %d, 650)
                panel:createChildren()
                local n, id, count = panel.searchBox, panel.searchId, panel.countEntry
                return n.y == id.y and id.y == count.y,
                    n.x + n.width < id.x, id.x + id.width < count.x,
                    count.x + count.width <= panel.width - 26,
                    panel.datas.y + panel.datas.height < panel.spawnBtn.y,
                    #panel.groups
                """.formatted(width));
        assertArrayEquals(new Object[]{true, true, true, true, true, 2.0}, result);
    }
}
