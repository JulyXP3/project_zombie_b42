--*********************************************************
--* 红队 POC: 僵尸吸引 + 万象天引 (趣味恶搞, SP/MP 皆可)
--*
--* A 吸引僵尸 (勾选, 持续信标):
--*   vanilla 全局 addSound(nil, x, y, z, 200, 100) -> 6 参重载内部
--*   flags=4 (stressZombies=true) -> MP 走 WorldSoundPacket 上行
--*   (LoginOnServer 即可发, 服务端 processServer 对坐标/半径/音量
--*   零校验, 见 WorldSoundPacket.java:123-152), SP 直接本地注册。
--*   半径 200 >= 50 -> 服务端 ZPOP n_worldSound 连未加载尸群一起拽。
--*   僵尸 RespondToSound 0~16 tick 随机延迟后 pathToSound 寻路过来
--*   (走过来非瞬移, 落点 ±dist/2.5 格随机)。
--*   注意: sourceIsZombie 必须 false (僵尸忽略同类声源, WorldSoundManager
--*   getSoundZomb ignoreBySameType); stressHumans 默认 false 不惊动他人。
--*
--* B 万象天引 (按钮, 单发聚集):
--*   zombieGather() Java 原语: 把 isLocal (模拟权在本端, SP=全部) 且
--*   40 格内的僵尸环形落位到脚下 + target 指向自己; MP 下位姿随常规
--*   200ms 僵尸模拟上行流带给服务端 (NetworkZombiePacker 只查所有权
--*   零校验), 其他客户端看到 >3 格瞬移对齐。无新增网络包。
--*   0 只可支配 = 僵尸尚未锁定你 -> 先开 A 引怪 (所有权按 target-first
--*   分配, 追你的僵尸模拟权自动转到你手上)。
--*
--* Kahlua 陷阱备忘: 无 string.trim; getTimeInMillis() 可用 (耕种页先例)
--**********************************************************
ZombieLure = ZombieLure or {}

-- 参数口径 (analysis/僵尸吸引与尸群聚集(已实施).md §二)
ZombieLure.RADIUS = 200      -- 声音半径 (格): >=50 触发 ZPOP
ZombieLure.VOLUME = 100      -- 声音响度 (加权权重)
ZombieLure.INTERVAL = 4000   -- 重发间隔 (ms)
ZombieLure.GATHER_RADIUS = 60 -- B 聚集搜索半径 (格)

ZombieLure.enabled = false
ZombieLure.nextFireMs = 0
ZombieLure.fired = 0

--*********************************************************
--* A: 声音信标 (勾选回调 -> OnTick 循环)
--*********************************************************
function ZombieLure.setEnabled(v)
    ZombieLure.enabled = v and true or false
    if ZombieLure.enabled then
        ZombieLure.nextFireMs = 0   -- 立即发第一声
        print("[ZombieLure] sound beacon ON (radius=" .. ZombieLure.RADIUS .. " every " .. ZombieLure.INTERVAL .. "ms)")
    else
        print("[ZombieLure] sound beacon OFF")
    end
    return ZombieLure.enabled
end

function ZombieLure.fireOnce()
    local p = getPlayer();
    if p == nil then return false; end
    -- vanilla 全局 (LuaManager.java:9241 @LuaMethod global): MP 内部走
    -- sendWorldSound 上行; SP 走本地注册。source=null 不影响引尸语义。
    addSound(nil,
        math.floor(p:getX()), math.floor(p:getY()), math.floor(p:getZ()),
        ZombieLure.RADIUS, ZombieLure.VOLUME);
    ZombieLure.fired = ZombieLure.fired + 1;
    return true;
end

function ZombieLure.onTick()
    if not ZombieLure.enabled then return; end
    local p = getPlayer();
    if p == nil then return; end
    if p:isDead() then return; end
    local now = getTimeInMillis();
    if now < ZombieLure.nextFireMs then return; end
    ZombieLure.nextFireMs = now + ZombieLure.INTERVAL;
    -- pcall 兜底: 单发失败不打断循环 (下一轮重试)
    pcall(ZombieLure.fireOnce);
end

--*********************************************************
--* B: 万象天引 (面板按钮调用)
--*********************************************************
function ZombieLure.gather()
    local p = getPlayer();
    if p == nil then
        return 0, getTranslate("UI_Fun_GatherNoPlayer");
    end
    local ok, ret = pcall(zombieGather, ZombieLure.GATHER_RADIUS);
    if not ok then
        print("[ZombieLure] gather failed: " .. tostring(ret));
        return 0, tr("UI_Fun_GatherFailed", { reason = tostring(ret) });
    end
    local n = tonumber(ret) or 0;
    if n <= 0 then
        return 0, getTranslate("UI_Fun_GatherNone");
    end
    return n, tr("UI_Fun_GatherDone", { count = tostring(n) });
end

Events.OnTick.Add(ZombieLure.onTick);
