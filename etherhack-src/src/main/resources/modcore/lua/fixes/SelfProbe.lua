--*********************************************************
--* 客户端自证探针 (红队): 复刻蓝队检测原语, 本地自测三层对抗
--*
--* 只写本地 console/日志, 不发任何网络包, 真服无痕。
--* 加载即跑一次 (OnMainMenuEnter 时点) + OnGameStart 再跑一次
--* (进服后 KWRR client lua 已加载, 环境才是真实对抗态)。
--*
--* 检测项 (对应 KWRR 检测原语):
--*   1. _G 扫描 KWRR 点名的 12 个符号 → 应全 nil (L2)
--*   2. _G 扫描 Ether 前缀全局 → 应零命中 (L2 改名; 不扫裸 UI 前缀 —
--*      UIFont/UIElement/UIManager 等是游戏原生类, 扫了全是误报)
--*   3. getLoadedLuaCount()+getLoadedLua(i) 枚举 → 应无 modcore 条目
--*      (L3 fileless; 签名与 KWRR 同款: count + 按索引取)
--**********************************************************

-- KWRR checkmodcore 硬编码符号表 (OverrideClient.lua:118-215)
local KWRR_SIGNATURES = {
    "EtherMain", "EtherAdminMenu", "EtherDebugMenu", "EtherEditWorldObjects",
    "EtherCharacterPane", "EtherExploitPanel", "EtherInfoPanel",
    "EtherItemCreator", "EtherMapPanel", "EtherPlayerEditor",
    "EtherSettingsPanel", "EtherVisualsPanel"
};

local function probe()
    -- 1. KWRR 点名符号: 应全 nil (L2 符号轮换后全局表里不存在)
    local hits = {};
    for i = 1, #KWRR_SIGNATURES do
        local name = KWRR_SIGNATURES[i];
        if rawget(_G, name) ~= nil then
            table.insert(hits, name);
        end
    end
    if #hits > 0 then
        print("[SelfProbe] FAIL-L2: KWRR signature globals still present: "
            .. table.concat(hits, ", "));
    else
        print("[SelfProbe] OK-L2: 12/12 KWRR signature globals nil");
    end

    -- 2. Ether 前缀全局: 应零命中 (L2 改名后我方全局全是随机前缀)
    local etherHits = {};
    for k in pairs(_G) do
        if type(k) == "string" and string.sub(k, 1, 5) == "Ether" then
            table.insert(etherHits, k);
        end
    end
    if #etherHits > 0 then
        print("[SelfProbe] FAIL-L2: Ether-prefixed globals present: "
            .. table.concat(etherHits, ", "));
    else
        print("[SelfProbe] OK-L2: no Ether-prefixed globals");
    end

    -- 3. 虚拟 FS 文件枚举: 我方 Lua 不应出现 (L3 fileless)
    --    签名 = getLoadedLuaCount() + getLoadedLua(i) (LuaManager.java:3770-3777)
    if type(getLoadedLuaCount) == "function" and type(getLoadedLua) == "function" then
        local ok, count = pcall(getLoadedLuaCount);
        if ok and type(count) == "number" then
            local fileHits = {};
            for i = 0, count - 1 do
                local okGet, f = pcall(getLoadedLua, i);
                if okGet and type(f) == "string"
                    and string.find(f, "modcore", 1, true) ~= nil then
                    table.insert(fileHits, f);
                end
            end
            if #fileHits > 0 then
                print("[SelfProbe] FAIL-L3: modcore lua visible in getLoadedLua: "
                    .. table.concat(fileHits, ", "));
            else
                print("[SelfProbe] OK-L3: no modcore files in getLoadedLua ("
                    .. tostring(count) .. " files scanned)");
            end
        end
    end

    -- 4. L1 私有清单可用性: isPrivateGlobal 应命中我方方法名, 不命中原版名;
    --    refreshPrivateGlobals 应可用 (上报前运行时补捕)
    if type(isPrivateGlobal) == "function" then
        local ok1, r1 = pcall(isPrivateGlobal, "getFlagState8");
        local ok2, r2 = pcall(isPrivateGlobal, "print");
        local hasRefresh = type(refreshPrivateGlobals) == "function";
        if ok1 and r1 == true and ok2 and r2 == false and hasRefresh then
            print("[SelfProbe] OK-L1: private-globals list active (refresh available)");
        else
            print("[SelfProbe] WARN-L1: private-globals list abnormal (ours="
                .. tostring(r1) .. ", vanilla=" .. tostring(r2)
                .. ", refresh=" .. tostring(hasRefresh) .. ")");
        end
    else
        print("[SelfProbe] WARN-L1: isPrivateGlobal not exposed");
    end
end

probe();

-- 进服后二次探针: KWRR mod 的 client lua 此时已加载, 环境才是真实对抗态
-- (主菜单时点 KWRR 未加载, _G/文件枚举还不是完整战场)
if type(Events) == "table" and type(Events.OnGameStart) == "table" then
    Events.OnGameStart.Add(function()
        probe();
    end);
end
