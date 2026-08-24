--*********************************************************
--* EtherI18n: 统一翻译入口 tr(key[, params])
--*
--* 目的: 消除各面板 getText(..)/getTranslate(..) 混用与手工 ".." 拼接。
--*   手工拼接的问题: 语序、标点(中文全角"："vs 英文": ")、单位位置都被写死在
--*   代码里, 译者无法调整 -> 改为在翻译串里用 {name} 占位符。
--*
--* 占位符替换为什么在 Lua 侧做:
--*   Java 层 EtherTranslator 确实有 getTranslate(key, KahluaTable) 2 参重载,
--*   且和 1 参重载共用同一个 @LuaMethod(name="getTranslate", global=true)。
--*   同名双重载在 Kahlua 里靠 MultiLuaJavaInvoker 按参数分派, 但本 mod 此前
--*   从未有 Lua 侧调用走过 2 参路径, 无法离线验证分派是否可靠。
--*   因此这里只用"已被全部现有调用验证过"的 1 参重载取出原串,
--*   再用 string.gsub 自己替换占位符 —— 零运行期风险。
--*
--* 支持:
--*   tr("K")                          -> 原样翻译
--*   tr("K", { count = 3 })           -> 把翻译串里的 {count} 换成 3
--*   翻译串中的 <br> 一律转成真正的换行 (与 Java 侧行为保持一致)
--*********************************************************

--- 转义 gsub 替换串中的 % (Lua 中 % 在替换串里是魔法字符)。
local function escapeRepl(s)
    return (string.gsub(s, "%%", "%%%%"));
end

--- 翻译并可选参数化替换占位符。
-- @param key    翻译键 (如 "UI_CharacterPanel_GodMode")
-- @param params 可选; { name = value } 形式, 替换翻译串中的 {name}
-- @return string 已翻译(并替换占位符)的文本
function tr(key, params)
    local s = getTranslate(key);
    if s == nil then return key; end

    if params ~= nil then
        for k, v in pairs(params) do
            -- "{" "}" 在 Lua 模式里不是魔法字符, 可直接当字面量匹配
            s = string.gsub(s, "{" .. k .. "}", escapeRepl(tostring(v)));
        end
    end

    -- 与 Java 侧一致: <br> 视为换行
    s = string.gsub(s, "<br>", "\n");
    return s;
end
