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
--*   2. 载荷特征扫描: 字符串值含 "Ether" / "modcore/lua" /
--*      "ModCore" 或 键名以 Ether/UI/ServerSync/requireExtra 开头
--*      → 命中即整包拦截 (含未来新命令名)。
--*   - verifyChecksum: 再剔除 args.checksumTable 里键以
--*     "modcore/lua" 开头的条目 (文件面 L3 完成后理论上为空);
--*   - verifyLuaGlobals / updateLuaGlobals: 再剔除 args.luaGlobals 里
--*     以我方前缀开头的全局键;
--*   - modcore 命令: 直接丢弃 (封禁链路根断);
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
local FEATURE_SUBSTRINGS = { "Ether", "modcore/lua", "ModCore" };
-- 静态前缀表 (legacy 兜底): 2026-09-10 起主判定走 isPrivateGlobal 私有清单
-- (PrivateGlobals 捕获窗差集, 覆盖轮换符号/方法名/ENV_MARKER);
-- 本表仅保留旧名特征作双保险。requireExtra 为已改名前的历史前缀。
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

-- 我方全局名判定 (2026-09-10 封禁审计): 静态前缀 + Java 私有清单双源。
-- L2 轮换后 hYYu* 与 ~250 个 @LuaMethod 方法名无静态特征, 必须走
-- isPrivateGlobal (PrivateGlobals 三捕获窗差集, 见 core/PrivateGlobals.java)。
local function isPrivate(k)
    if type(k) ~= "string" then return false; end
    if isOurKey(k) or isOurString(k) then return true; end
    if type(isPrivateGlobal) == "function" then
        local ok, r = pcall(isPrivateGlobal, k);
        if ok and r == true then return true; end
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
        -- 封禁链路根断: modcore 上报永不抵达服务端
        if command == "modcore" then return false; end
        -- 上报类命令: 消息含我方特征即拦截 (L1 兜底 — 即使 L2 符号泄露,
        -- "has [EtherHack] installed" 这类报告也到不了服务器)
        if command == "reportCheat" or command == "custom" then
            if payloadTainted(args) then return false; end
            return true;
        end
        if type(args) == "table" then
            -- 文件校验表: 剔除我方路径条目, 服务端重算即全绿
            if command == "verifyChecksum" and type(args.checksumTable) == "table" then
                for path in pairs(args.checksumTable) do
                    if type(path) == "string"
                        and string.sub(path, 1, 14) == "modcore/lua/" then
                        args.checksumTable[path] = nil;
                    end
                end
            end
            -- 全局白名单表: 剔除我方全部全局名 (私有清单 + 静态前缀)
            if (command == "verifyLuaGlobals" or command == "updateLuaGlobals")
                and type(args.luaGlobals) == "table" then
                -- 运行时补捕: 与 _G 被枚举上交的时刻对齐, 捕获加载窗口外
                -- (lazy init 等) 创建的全局名
                if type(refreshPrivateGlobals) == "function" then
                    pcall(refreshPrivateGlobals);
                end
                for name in pairs(args.luaGlobals) do
                    if isPrivate(name) then
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
--*
--* 转发纪律 (2026-09-10 实测教训): 原版 sendClientCommand 是 Java
--* MultiLuaJavaInvoker 重载分派 (3参/4参两个实现), 分派依赖"到达时的
--* 参数个数"。包装闭包若固定以 4 参形态转发 (player, module, command,
--* args), 3 参调用方 (如 Zones 类 mod) 的参数计数即被破坏 — 原版 3 参
--* 重载因个数不符 fail, 4 参重载因首参 nil 转型槽位错位 fail, 两实现
--* 全灭 → "No implementation found for function" (实机 console.txt 报错,
--* 移除注入后消失, 因果实证)。故必须按实际传入参数个数原样透传。
--*********************************************************
if type(orig_sendClientCommand) == "function" then
    sendClientCommand = function(...)
        -- vararg 透传: 实参个数与调用方严格一致 (Kahlua callJava 按栈上
        -- nArguments 分派 Java 重载, 多压/少压都会让重载匹配全灭)。
        -- 形态 A (3参): module, command, args
        -- 形态 B (4参): player, module, command, args
        local n = select("#", ...);
        if n >= 3 and type((select(2, ...))) == "string" and type((select(3, ...))) == "string" then
            -- 4参形态: 槽2=module 槽3=command
            if not purify((select(2, ...)), (select(3, ...)), (select(4, ...))) then return; end
        elseif n >= 2 and type((select(1, ...))) == "string" and type((select(2, ...))) == "string" then
            -- 3参形态: 槽1=module 槽2=command (player 缺省, Java 侧按 null 处理)
            if not purify((select(1, ...)), (select(2, ...)), (select(3, ...))) then return; end
        end
        return orig_sendClientCommand(...);
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
