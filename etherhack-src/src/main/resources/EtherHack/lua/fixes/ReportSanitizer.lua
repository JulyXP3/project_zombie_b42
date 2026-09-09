--*********************************************************
--* 上报面净化 (红队 L1): KWRR_Security 等客户端自证上报拦截
--*
--* 原理 (方案 §3-L1 + §7.2-2/3 + §8.3-1/2):
--*   PZ 的 Lua 全局是运行时查找 (LuaManager.env) — 本文件在
--*   OnMainMenuEnter 阶段最先执行, 先把原始函数存私有 upvalue,
--*   再覆盖 _G.sendClientCommand / sendClientCommandV / ModData.transmit;
--*   之后任何 mod (KWRR / 换皮反作弊 / 服务器热下发代码) 调这三个
--*   上报通道, 都先过这里的净化规则 — 换插件零改动。
--*
--* 净化规则 (内容特征, 不绑定单一插件名):
--*   1. 模块名 == KWRR_Security (防升版本目录改名前的兜底);
--*   2. 载荷特征扫描: 字符串值含 "Ether" / "EtherHack/lua" /
--*      "DeiClient" 或 键名以 Ether/UI/ServerSync/requireExtra 开头
--*      → 命中即整包拦截 (含未来新命令名)。
--*   - verifyChecksum: 再剔除 args.checksumTable 里键以
--*     "EtherHack/lua" 开头的条目 (文件面 L3 完成后理论上为空);
--*   - verifyLuaGlobals / updateLuaGlobals: 再剔除 args.luaGlobals 里
--*     以我方前缀开头的全局键;
--*   - EtherHack 命令: 直接丢弃 (封禁链路根断);
--*   - 其余命令: 原样转发 (无痕, 不破坏游戏功能)。
--*
--* 不拦 ModData.request (下行, 无自证载荷); transmit 只放行非我方
--* 特征 tag (我们自己的 ModData 走 SendObjects 派生表, 不走 transmit)。
--*
--* 帧安全: 净化扫描只在键/字符串上做前缀与子串判断, 不递归展开
--* 深层表 (游戏网络包只序列化一层键值表, 深层扫描无收益)。
--*********************************************************

-- 私有捕获: 必须在覆盖前取值; Kahlua 闭包按 upvalue 持引用,
-- 蓝队即使之后检测 _G.sendClientCommand ~= 原始函数也无法还原 (L2 改名后连检测点都找不到)
local orig_sendClientCommand = sendClientCommand;
local orig_sendClientCommandV = sendClientCommandV;
local orig_ModData_transmit = ModData and ModData.transmit;

--*********************************************************
--* 特征判定
--*********************************************************
local FEATURE_SUBSTRINGS = { "Ether", "EtherHack/lua", "DeiClient" };
local KEY_PREFIXES = { "Ether", "UI", "ServerSync", "requireExtra" };

local function isOurString(v)
    if type(v) ~= "string" then return false; end
    for i = 1, #FEATURE_SUBSTRINGS do
        if string.find(v, FEATURE_SUBSTRINGS[i], 1, true) ~= nil then return true; end
    end
    return false;
end

local function isOurKey(k)
    if type(k) ~= "string" then return false; end
    for i = 1, #KEY_PREFIXES do
        if string.sub(k, 1, string.len(KEY_PREFIXES[i])) == KEY_PREFIXES[i] then return true; end
    end
    return false;
end

-- 载荷扫描: 命中任一特征 → 整包拦截 (宁拦勿漏, 上报面是我方唯一泄露通道)
local function payloadTainted(args)
    if type(args) ~= "table" then return false; end
    for k, v in pairs(args) do
        if isOurKey(k) or isOurString(k) or isOurString(v) then return true; end
    end
    return false;
end

--*********************************************************
--* 命令级净化 (返回 false = 拦截, true = 放行; args 就地修改)
--*********************************************************
local function purify(module, command, args)
    -- 已知蓝队模块: 按命令名精准处理; 未知模块走内容特征兜底
    if module == "KWRR_Security" then
        -- 封禁链路根断: EtherHack 上报永不抵达服务端
        if command == "EtherHack" then return false; end
        if type(args) == "table" then
            -- 文件校验表: 剔除我方路径条目, 服务端重算即全绿
            if command == "verifyChecksum" and type(args.checksumTable) == "table" then
                for path in pairs(args.checksumTable) do
                    if type(path) == "string"
                        and string.sub(path, 1, 14) == "EtherHack/lua/" then
                        args.checksumTable[path] = nil;
                    end
                end
            end
            -- 全局白名单表: 剔除我方前缀键
            if (command == "verifyLuaGlobals" or command == "updateLuaGlobals")
                and type(args.luaGlobals) == "table" then
                for name in pairs(args.luaGlobals) do
                    if isOurKey(name) or isOurString(name) then
                        args.luaGlobals[name] = nil;
                    end
                end
            end
        end
        return true;
    end
    -- 换插件兜底: 内容特征命中即整包拦截
    if payloadTainted(args) then return false; end
    return true;
end

--*********************************************************
--* 通道覆盖 (三入口, 方案 §7.2-2)
--*********************************************************
if type(orig_sendClientCommand) == "function" then
    sendClientCommand = function(player, module, command, args)
        if not purify(module, command, args) then return; end
        return orig_sendClientCommand(player, module, command, args);
    end
end

if type(orig_sendClientCommandV) == "function" then
    sendClientCommandV = function(player, module, command, ...)
        if module == "KWRR_Security" then return; end
        local n = select("#", ...);
        for i = 1, n do
            if isOurString((select(i, ...))) then return; end
        end
        return orig_sendClientCommandV(player, module, command, ...);
    end
end

-- ModData.transmit: Lua 层第二上报通道 (方案 §8.3-2), tag 特征命中即拦
if type(orig_ModData_transmit) == "function" and ModData ~= nil then
    ModData.transmit = function(tag)
        if isOurString(tag) or isOurKey(tag) then return; end
        return orig_ModData_transmit(tag);
    end
end
