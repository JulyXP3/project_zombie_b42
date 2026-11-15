package modcore;

import org.junit.jupiter.api.Test;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GeneratorMaintenancePanelTest {
    @Test
    void survivalPagePlacesGeneratorAfterItemsWithTwoButtonsAndTheRangeBelow() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                EtherFormPanel = ISPanel:derive("EtherFormPanel")
                EtherFormPanel.BOX_PAD_X = 10
                EtherFormPanel.SECTION_GAP = 12
                EtherFormPanel.planGrid = function(items, w) return {}, 0, w end
                EtherFormPanel.placeGrid = function() end
                function EtherFormPanel:_rowContentW() return 560 end
                function EtherFormPanel:_anchor() end
                function EtherFormPanel:addSpacer() end
                modules = {}
                function EtherFormPanel:addModule(key, contentH, builder)
                    table.insert(modules, { key = key, h = contentH })
                    builder(0, #modules * 100, 560)
                end
                function EtherFormPanel:addWidget(widget, opts)
                    widget:initialise()
                    widget:instantiate()
                    self:addChild(widget)
                end
                ModuleHint = ISPanel:derive("ModuleHint")
                function ModuleHint:new(x, y, w, text)
                    local o = ISPanel.new(self, x, y, w, #EtherTheme.wrapHint(text, w - 8) * 12 + 2)
                    setmetatable(o, self)
                    self.__index = self
                    o.lines = EtherTheme.wrapHint(text, w - 8)
                    return o
                end
                function ModuleHint:setHeight(value) self.height = value end
                function generatorMaintenanceReady() return true end
                """);
        var translations = (se.krka.kahlua.vm.KahluaTable) lua.environment.rawget("translations");
        translations.rawset("UI_Generator_Title", "发电机");
        translations.rawset("UI_Generator_Fuel", "加满燃油");
        translations.rawset("UI_Generator_Repair", "修复至满");
        translations.rawset("UI_Generator_Range", "作用范围为周围八格");
        lua.resource("modcore/lua/components/ui/GeneratorMaintenance.lua");
        lua.resource("modcore/lua/components/panels/EtherCharacterPanel.lua");
        Object[] result = lua.execute("""
                local panel = setmetatable({ children = {} }, { __index = EtherCharacterPanel })
                panel:build()
                local position = 0
                for i, m in ipairs(modules) do
                    if m.key == "UI_Generator_Title" then position = i end
                end
                local fuel = buttons[1]
                local repair = buttons[2]
                local hint = nil
                for _, child in ipairs(panel.children) do
                    if child.lines and child.lines[1] == translations.UI_Generator_Range then hint = child end
                end
                return position > 0, position > 0 and modules[position - 1].key or "missing",
                    #buttons, fuel and fuel.title or "missing", repair and repair.title or "missing",
                    hint ~= nil and hint.y > fuel.y + fuel.height,
                    panel.generatorStatus ~= nil and panel.generatorStatus.height >= 26
                """);
        assertArrayEquals(new Object[]{true, "UI_CharacterPanel_Group_Items", 2.0,
                "加满燃油", "修复至满", true, true}, result);
    }

    @Test
    void allTranslatedStatusesStayInsideTheModuleAtNarrowAndWideSizes() throws Exception {
        for (String language : new String[]{"CN", "EN", "RU"}) {
            for (int width : new int[]{578, 706}) {
                for (int fontHeight : new int[]{16, 32}) {
                    LuaTestRuntime lua = layoutRuntime(language, width, fontHeight);
                    for (String key : new String[]{"UI_Generator_Submitted", "UI_Generator_Partial",
                            "UI_Generator_Empty", "UI_Generator_Full", "UI_Generator_Busy",
                            "UI_Generator_NotReady"}) {
                        lua.environment.rawset("statusKey", key);
                        Object[] result = lua.execute("""
                                local text = tr(statusKey, { n = 289, skipped = 289 })
                                local lines = EtherTheme.wrapHint(text, panel.generatorStatus.width - 8)
                                local status = panel.generatorStatus
                                return #lines * EtherTheme.fontHgtHint + 2 <= status.height,
                                    status.y + status.height <= panel.moduleContentY + panel.moduleContentH,
                                    buttons[2].x + buttons[2].width <= panel.width - 16
                                """);
                        assertArrayEquals(new Object[]{true, true, true}, result,
                                language + " width=" + width + " font=" + fontHeight + " " + key);
                    }
                }
            }
        }
    }

    @Test
    void realFormGatingDisablesBothGeneratorButtonsOutsideTheGame() throws Exception {
        LuaTestRuntime lua = layoutRuntime("EN", 578, 16);
        Object[] result = lua.execute("""
                player = nil
                panel:refreshGating()
                return #panel.widgets, buttons[1].isOnlyInGame, buttons[2].isOnlyInGame,
                    buttons[1].enable, buttons[2].enable
                """);
        assertArrayEquals(new Object[]{2.0, true, true, false, false}, result);
    }

    private static LuaTestRuntime layoutRuntime(String language, int width, int fontHeight) throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        KahluaTable textManager = se.krka.kahlua.j2se.J2SEPlatform.getInstance().newTable();
        textManager.rawset("getFontHeight", (JavaFunction) (frame, count) -> frame.push((double) fontHeight));
        textManager.rawset("MeasureStringX", (JavaFunction) (frame, count) -> {
            String text = frame.get(2).toString();
            double units = text.codePoints().mapToDouble(c -> c > 0x2ff ? 1.0 : 0.6).sum();
            return frame.push(units * fontHeight);
        });
        lua.environment.rawset("getTextManager", (JavaFunction) (frame, count) -> frame.push(textManager));
        KahluaTable translations = (KahluaTable) lua.environment.rawget("translations");
        try (var stream = GeneratorMaintenancePanelTest.class.getClassLoader()
                .getResourceAsStream("modcore/translations/" + language + ".txt")) {
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = Pattern.compile("(?m)^\\s*(UI_Generator_\\w+)\\s*=\\s*\"([^\"]*)\"").matcher(text);
            while (matcher.find()) translations.rawset(matcher.group(1), matcher.group(2));
        }
        lua.resource("modcore/lua/components/ui/EtherTheme.lua");
        lua.resource("modcore/lua/components/ui/EtherI18n.lua");
        lua.resource("modcore/lua/components/ui/EtherFormPanel.lua");
        lua.execute("""
                function ISPanel:setAnchorLeft() end
                function ISPanel:setAnchorRight() end
                function ISPanel:setAnchorTop() end
                function ISPanel:setAnchorBottom() end
                function ISPanel:setScrollHeight(h) self.scrollHeight = h end
                function generatorMaintenanceReady() return true end
                local addModule = EtherFormPanel.addModule
                function EtherFormPanel:addModule(key, h, builder)
                    return addModule(self, key, h, function(x, y, w)
                        self.moduleContentY = y
                        self.moduleContentH = h
                        builder(x, y, w)
                    end)
                end
                """);
        lua.resource("modcore/lua/components/ui/GeneratorMaintenance.lua");
        lua.execute("panel = EtherFormPanel:new(0, 0, " + width + ", 530); GeneratorMaintenance.addModule(panel)");
        return lua;
    }
}
