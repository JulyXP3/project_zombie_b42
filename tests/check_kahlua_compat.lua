-- Kahlua 兼容性静态检查: 扫描游戏 Lua 里已验证在 PZ 的 Kahlua 环境不可用的 API
-- 用法: lua5.1.exe check_kahlua_compat.lua <文件1> [<文件2> ...]
-- 每个黑名单模式都来自游戏内实测报错 (Object tried to call nil / non-table)
local BANNED = {
    ["next("] = "Kahlua 未实现全局 next (报错: Object tried to call nil), 用计数/遍历代替",
    [":getVisible()"] = "ISUIElement Lua 类没有 getVisible (只有 setVisible/getIsVisible), 用 getIsVisible()",
    [":getParts()"] = "getParts() 返回的 VehicleParts java 对象未暴露给 Lua (non-table), 用 vehicle:getPartCount()/getPartByIndex()",
    [":getInventory():size()"] = "ItemContainer 未直接暴露 size(), 用 getInventory():getItems():size()",
}

local n = 0
for _, path in ipairs(arg) do
    local f, err = io.open(path, "r")
    if not f then
        print("ERROR: cannot open " .. path .. ": " .. tostring(err))
        os.exit(1)
    end
    local content = f:read("*a")
    f:close()
    for line in content:gmatch("[^\r\n]*") do
        for pattern, why in pairs(BANNED) do
            if line:find(pattern, 1, true) then
                n = n + 1
                print(string.format("%s: banned %q -> %s", path, pattern, why))
            end
        end
    end
end

if n > 0 then
    print("FAIL: " .. n .. " banned pattern(s) found")
    os.exit(1)
end
print("OK: no banned patterns")
