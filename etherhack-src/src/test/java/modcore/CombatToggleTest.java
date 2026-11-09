package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 复选框回调约定回归 (2026-11-06): 无限弹药"关闭不生效"首先怀疑 UI 回调没有把
 * false 传到 Java。本用例用真实 Kahlua 跑 modcore 的 UICheckbox, 断言点击只翻转
 * 自身状态且回调收到的就是布尔值 —— 排除"回调参数被吞/传了控件对象"这类问题。
 */
class CombatToggleTest {
    @Test
    void checkboxPassesTheNewBooleanStateToItsCallback() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function require() end
                UIFont = { Small = 1 }
                function resolveTexture() return 0 end
                function getTextManager()
                    return {
                        getFontHeight = function() return 16 end,
                        MeasureStringX = function(self, font, text) return #text * 6 end,
                    }
                end
                function getSoundManager()
                    return { playUISound = function() end }
                end
                ISPanel = { derive = function(self) return {} end }
                EtherTheme = { text = { r = 1, g = 1, b = 1 } }
                """);
        lua.resource("modcore/lua/components/ui/UICheckbox.lua");
        Object[] result = lua.execute("""
                local seen = {}
                local box = UICheckbox:new(0, 0, "unlimited", true, function(v)
                    table.insert(seen, v)
                end)
                local first = box:onMouseUp(1, 1)
                local afterOff = box.isChecked
                local second = box:onMouseUp(1, 1)
                return first, afterOff, second, box.isChecked,
                    tostring(seen[1]), tostring(seen[2]), #seen
                """);
        assertArrayEquals(new Object[]{
                true, false, true, true, "false", "true", 2.0}, result);
    }
}
