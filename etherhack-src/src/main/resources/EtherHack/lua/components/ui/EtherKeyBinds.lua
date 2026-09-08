--*********************************************************
--* EtherKeyBinds: 按键绑定框架 (2026-09-09)
--*
--* 统一分发: PZ 的鼠标按下同样走 OnKeyPressed 通道 (UIManager 合成键码
--* 10000+按钮号: 左键=10000 右键=10001 中键/侧键=10002+), 故键盘/鼠标
--* 热键天然同通道, 一个分发器全收。
--*
--* 用法:
--*   EtherKeyBinds.register("featureId", "UI_翻译键", handler, 默认键码);
--*   -- 任意位置(设置页/主菜单)注册即可; 分发在 OnKeyPressed 统一进行。
--*   EtherKeyBinds.beginCapture("featureId");  -- 捕获下一键盘/鼠标键 (ESC 取消)
--*   EtherKeyBinds.reset("featureId");         -- 还原默认键
--*   EtherKeyBinds.keyName(code);              -- 键码 -> 显示名 (含鼠标键翻译)
--*
--* typing 保护: getCore():isDoingTextEntry() 为真(聊天/输入框聚焦)时
--* 不派发热键, 避免打字误触。
--*********************************************************
EtherKeyBinds = {};

EtherKeyBinds.MOUSE_BASE = 10000;
EtherKeyBinds.ESC = 1;

EtherKeyBinds.registry = {};   -- featureId -> {label, handler, defaultKey}
EtherKeyBinds.bindings = {};   -- featureId -> 当前键码 (nil=未解析)
EtherKeyBinds.order = {};      -- 注册顺序 (分发/UI 依此, 确定性)
EtherKeyBinds.captureId = nil; -- 捕获中的 featureId

-- 注册一个可绑定的功能。冲突键在注册与捕获时都会提示。
function EtherKeyBinds.register(featureId, labelKey, handler, defaultKey)
    EtherKeyBinds.registry[featureId] = {
        label = labelKey,
        handler = handler,
        defaultKey = defaultKey,
    };
    EtherKeyBinds.bindings[featureId] = nil; -- 延迟解析: 配置可能尚未加载
    table.insert(EtherKeyBinds.order, featureId);
end

-- 解析当前键码: Java 侧配置优先, 缺失(未设置/0)回退默认键
function EtherKeyBinds.resolve(featureId)
    local b = EtherKeyBinds.bindings[featureId];
    if b ~= nil then return b; end
    local key = 0;
    if type(getKeyBind) == "function" then
        key = getKeyBind(featureId);
    end
    local r = EtherKeyBinds.registry[featureId];
    if key == nil or key == 0 then
        key = r and r.defaultKey or 0;
    end
    EtherKeyBinds.bindings[featureId] = key;
    return key;
end

-- 配置加载后重读 (onGameStart 里 loadConfig 之后调用)
function EtherKeyBinds.refresh()
    for _, id in ipairs(EtherKeyBinds.order) do
        EtherKeyBinds.bindings[id] = nil;
    end
end

function EtherKeyBinds.isCapturing()
    return EtherKeyBinds.captureId ~= nil;
end

function EtherKeyBinds.beginCapture(featureId)
    EtherKeyBinds.captureId = featureId;
end

function EtherKeyBinds.cancelCapture()
    EtherKeyBinds.captureId = nil;
end

-- 还原默认键并持久化
function EtherKeyBinds.reset(featureId)
    local r = EtherKeyBinds.registry[featureId];
    if r == nil then return; end
    EtherKeyBinds.bindings[featureId] = r.defaultKey;
    if type(setKeyBind) == "function" then
        setKeyBind(featureId, r.defaultKey);
    end
end

-- 冲突检测: 返回已占用该键的其他 featureId (无则 nil)
function EtherKeyBinds.conflictOf(featureId, key)
    for _, id in ipairs(EtherKeyBinds.order) do
        if id ~= featureId and EtherKeyBinds.resolve(id) == key then
            return id;
        end
    end
    return nil;
end

-- 键码 -> 显示名: 键盘反查 Keyboard 常量, 鼠标合成键转翻译
function EtherKeyBinds.keyName(code)
    if code == nil or code == 0 then return "-"; end
    if code >= EtherKeyBinds.MOUSE_BASE then
        local btn = code - EtherKeyBinds.MOUSE_BASE;
        if btn == 0 then return getTranslate("UI_KeyBind_MouseLeft"); end
        if btn == 1 then return getTranslate("UI_KeyBind_MouseRight"); end
        if btn == 2 then return getTranslate("UI_KeyBind_MouseMiddle"); end
        if btn == 3 then return getTranslate("UI_KeyBind_MouseBack"); end
        if btn == 4 then return getTranslate("UI_KeyBind_MouseForward"); end
        return getTranslate("UI_KeyBind_MouseBtn") .. " " .. btn;
    end
    for k, v in pairs(Keyboard) do
        if v == code then
            local n = tostring(k);
            if n:sub(1, 4) == "KEY_" then n = n:sub(5); end
            return n;
        end
    end
    return tostring(code);
end

-- 统一分发 (键盘 + 鼠标合成键): 捕获优先, 然后 typing 保护, 再按注册序派发
function EtherKeyBinds.onKeyPressed(key)
    if EtherKeyBinds.captureId ~= nil then
        EtherKeyBinds.capture(key);
        return;
    end
    if getCore():isDoingTextEntry() then return; end
    for _, id in ipairs(EtherKeyBinds.order) do
        if EtherKeyBinds.resolve(id) == key then
            local r = EtherKeyBinds.registry[id];
            if r ~= nil and r.handler ~= nil then
                r.handler();
            end
            return;
        end
    end
end

-- 捕获态: 赋值 + 持久化 + 冲突提示 + 通知 UI
function EtherKeyBinds.capture(key)
    local id = EtherKeyBinds.captureId;
    EtherKeyBinds.captureId = nil;
    if key == EtherKeyBinds.ESC then
        if EtherKeyBinds.onCaptureDone ~= nil then
            EtherKeyBinds.onCaptureDone(id, nil);
        end
        return;
    end
    local other = EtherKeyBinds.conflictOf(id, key);
    EtherKeyBinds.bindings[id] = key;
    if type(setKeyBind) == "function" then
        setKeyBind(id, key);
    end
    if other ~= nil then
        print("[EtherKeyBinds] key conflict: " .. id .. " overrides " .. other
            .. " (" .. EtherKeyBinds.keyName(key) .. ")");
    end
    if EtherKeyBinds.onCaptureDone ~= nil then
        EtherKeyBinds.onCaptureDone(id, key);
    end
end

Events.OnKeyPressed.Add(EtherKeyBinds.onKeyPressed);
