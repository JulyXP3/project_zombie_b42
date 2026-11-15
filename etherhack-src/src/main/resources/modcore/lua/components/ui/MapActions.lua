--*********************************************************
--* MapActions: 地图传送状态机。原动作参数、速度与收尾逻辑保持不变。
--* UIMap 保留同名入口, 地图绘制不再持有动作与常驻 tick。
--*********************************************************
if MapActions ~= nil then
    if MapActions.reset ~= nil then MapActions.reset(); end
    Events.OnTick.Remove(MapActions.onTeleportTick);
    Events.OnTick.Remove(MapActions.onVehTeleportTick);
    Events.OnMainMenuEnter.Remove(MapActions.reset);
end
MapActions = {};

--*********************************************************
--* Безопасная телепортация
--* MP 修复 (2026-08-25): 旧实现单帧内逐格 setX+sendPlayer 瞬移,
--* 服务端 AntiCheatSpeed 以 ~1s 窗口采样位移均值 (SpeedChecker,
--* NetworkCharacterAI.java:330-382, speed=位移*1000/delta),
--* >20格/s 判违规, 默认策略 antiCheatSpeed=2=踢出 → 必被踢。
--* 改为 OnTick 时间限速步进: 匀速推进 (上限20的安全余量, 2026-09-13 起为 13格/s),
--* 任何采样窗口测得的均值都低于上限; 客户端位置经常规
--* PlayerPacket 流自动同步, 不再手动 sendPlayer。
--* 单人无服务端校验, 保持原瞬时传送不变。
--*
--* MP 二次修复 (2026-08-25, 蓝队内部服实测被踢): 服务端开启
--* antiCheatNoClip (默认关, 内部服常开) 时 AntiCheatNoClip 逐包
--* 检查 (releventPos 每 PlayerPacket 更新, PlayerPacket.java:142):
--*   包间位移 >2.5格 → "Long blocked" (:90);
--*   相邻格须 pathMatrix 连通 (:106 checkPathClamp) 且过门/窗
--*   检查 (:108 checkReachablePath) → "Unreachable/Reachable blocked"。
--* 直线滑行穿墙的瞬间即违规。改为客户端 BFS 寻路 + 沿路径逐格
--* 中心走: 邻接判定与 NoClip 检查同源 (getPathMatrix 连通 +
--* 门/窗 IsOpen, 保守不放行可翻越型), 只走正交步, 单 tick 位移钳 ≤1.0 格;
--* 网络包间仍可跨多步, 不能由 tick 步长推导包间位移。目标被围死时吸附最近可达格。
--* Java 侧 movePlayerToPos 保留未动 (SafeAPI/ProtectionManagerX
--* 的防篡改名单引用其名), 但 Lua 不再调用。
--*********************************************************
local TELEPORT_SPEED = 17.0;   -- 格/秒 (SpeedChecker 上限 20; 2026-09-14 用户拍板 13→17)
local TELEPORT_STEP_TIMEOUT = 5000;    -- A6: 单步无进展超时 (ms)
local TELEPORT_TOTAL_TIMEOUT = 120000; -- A6: 整程超时 (ms)
local TELEPORT_CANCEL_GRACE = 200;     -- A6: 启动后取消输入宽限 (ms), 防误吃启动那次点击
local teleportTask = nil;      -- 进行中的传送 { path=, node=, lastMs=, startedMs=, lastNodeMs=, nodeSeen=, tx=, ty= }
-- 穿墙会话: 状态门只抑制任务所属玩家, 移动与 Enter/Exit 由 Java 同可靠有序流提交。
-- 不用定时重发或多等一帧建立顺序; 失败不继续写坐标, 结束无条件关闭存储的会话。
local teleportClimbActive = false;
local teleportClimbPlayer = nil;


--*********************************************************
--* 邻接判定 (与 AntiCheatNoClip.checkPathClamp/checkReachablePath 同源):
--* pathMatrix 连通 + 中间门/窗必须敞开 (保守: 可翻越型也绕路)。
--* 返回目标格对象 (可走) 或 nil (不可走/未加载)。
--*********************************************************
local function canStepSq(sq, nx, ny)
	local dx, dy = nx - sq:getX(), ny - sq:getY();
	if math.abs(dx) + math.abs(dy) ~= 1 then return nil; end   -- 只走正交步
	local nsq = getCell():getGridSquare(nx, ny, sq:getZ());
	if nsq == nil then return nil; end
	if sq:getPathMatrix(dx, dy, 0) then return nil; end        -- true=阻挡
	local obj;
	if dy == -1 then obj = sq:getDoorOrWindow(true);           -- N 查 source
	elseif dx == -1 then obj = sq:getDoorOrWindow(false);      -- W 查 source
	elseif dy == 1 then obj = nsq:getDoorOrWindow(true);       -- S 查 target
	else obj = nsq:getDoorOrWindow(false); end                 -- E 查 target
	if obj ~= nil and not obj:IsOpen() then return nil; end
	return nsq;
end

--*********************************************************
--* BFS 寻路 (4 向正交)。返回格坐标数组 {{x,y},...} (不含起点);
--* 目标不可达时返回离目标欧氏距离最近的可达格路径; 起点无格返回 nil。
--*********************************************************
local function findTeleportPath(sx, sy, tx, ty, z)
	local startSq = getCell():getGridSquare(sx, sy, z);
	if startSq == nil then return nil; end
	if sx == tx and sy == ty then return {}; end
	local dirs = { { 0, -1 }, { -1, 0 }, { 0, 1 }, { 1, 0 } };
	local key = function(x, y) return x * 100000 + y; end;
	local visited = { [key(sx, sy)] = true };
	local prev = {};
	local queue = { { sx, sy, startSq } };
	local head = 1;
	local best = nil;
	local bestD = math.huge;
	while head <= #queue do
		local cur = queue[head];
		head = head + 1;
		local cx, cy = cur[1], cur[2];
		local cd = math.sqrt((cx - tx) * (cx - tx) + (cy - ty) * (cy - ty));
		if cd < bestD then
			bestD = cd;
			best = { cx, cy };
		end
		if cx == tx and cy == ty then break; end
		for i = 1, 4 do
			local nx, ny = cx + dirs[i][1], cy + dirs[i][2];
			local k = key(nx, ny);
			if not visited[k] then
				local nsq = canStepSq(cur[3], nx, ny);
				if nsq ~= nil then
					visited[k] = true;
					prev[k] = { cx, cy };
					table.insert(queue, { nx, ny, nsq });
				end
			end
		end
	end
	local path = {};
	local cur = best;
	while not (cur[1] == sx and cur[2] == sy) do
		table.insert(path, 1, cur);
		cur = prev[key(cur[1], cur[2])];
		if cur == nil then return nil; end
	end
	return path;
end

local function endTeleportClimbSync()
	local player = teleportClimbPlayer;
	-- Java 自存所属玩家/连接, Lua nil 也会关闭; 关闭期等待两次原版服务器周期回声。
	local requested = type(teleportClimbState) == "function" and teleportClimbState(player, false);
	local pending = type(teleportClimbPending) == "function" and teleportClimbPending(player);
	local failed = type(teleportClimbCloseFailed) == "function" and teleportClimbCloseFailed(player);
	teleportClimbActive = pending;
	if not pending then teleportClimbPlayer = nil; end
	if failed and player ~= nil then
		UIMap.lastTeleportCancel = "sync-failed";
		if HaloTextHelper ~= nil then HaloTextHelper.addBadText(player, getTranslate("UI_Map_TeleportSyncFailed")); end
	end
	return requested and not failed;
end

local function stopTeleport(reason)
	UIMap.lastTeleportCancel = reason;
	teleportTask = nil;
	local player = teleportClimbPlayer;
	local wasActive = teleportClimbActive;
	local closed = endTeleportClimbSync();
	if wasActive and not closed then UIMap.lastTeleportCancel = "sync-failed"; end
	if UIMap.lastTeleportCancel == "sync-failed" and HaloTextHelper ~= nil and player ~= nil
		and not (type(teleportClimbCloseFailed) == "function" and teleportClimbCloseFailed(player)) then
		HaloTextHelper.addBadText(player, getTranslate("UI_Map_TeleportSyncFailed"));
	end
end

local function onTeleportTick()
	local task = teleportTask;
	if task == nil then
		if teleportClimbActive then endTeleportClimbSync(); end
		return;
	end
	local player = getPlayer();
	if player == nil or task.player ~= nil and task.player ~= player then
		stopTeleport("state");
		return;
	end
	if task.linear and isClient() and (type(teleportClimbReady) ~= "function" or not teleportClimbReady(player)) then
		stopTeleport("sync-failed");
		return;
	end
	local now = getTimestampMs();
	if task.lastMs == nil then
		task.lastMs = now;
		task.startedMs = now;
		task.lastNodeMs = now;
		task.nodeSeen = task.node;
		return;
	end
	-- A6 (2026-09-13): 取消入口 —— 宽限期过后, 移动/跳跃键与 ESC/鼠标左键 立即交还操控权
	local cancelGraceOver = (now - task.startedMs) > TELEPORT_CANCEL_GRACE;
	if isKeyDown(Keyboard.KEY_W) or isKeyDown(Keyboard.KEY_A) or isKeyDown(Keyboard.KEY_S)
		or isKeyDown(Keyboard.KEY_D) or isKeyDown(Keyboard.KEY_SPACE)
		or (cancelGraceOver and (isKeyDown(Keyboard.KEY_ESCAPE) or isMouseButtonDown(0))) then
		stopTeleport("input");
		return;
	end
	-- A6: 状态异常 (死亡/进入载具) 时路径已失效, 立即终止避免位置跳变
	if player:isDead() or player:getVehicle() ~= nil then
		stopTeleport("state");
		return;
	end
	local dt = now - task.lastMs;
	task.lastMs = now;
	if dt <= 0 then return; end;
	if dt > 250 then dt = 250; end;
	-- A6: 单步无进展 5s / 整程 120s 超时 (传送卡死时不再无限推进)
	if task.node ~= task.nodeSeen then
		task.nodeSeen = task.node;
		task.lastNodeMs = now;
	elseif (now - task.lastNodeMs) > TELEPORT_STEP_TIMEOUT then
		stopTeleport("step-timeout");
		return;
	end
	if (now - task.startedMs) > TELEPORT_TOTAL_TIMEOUT then
		stopTeleport("total-timeout");
		return;
	end
	-- 单 tick 位移钳 ≤1.0 格; BFS 档逐格走连通路径, 不代表网络包间位移也 ≤1.0。
	-- 穿墙档依赖所属玩家的状态同步, 不能仅凭路径点间距保证服务端放行。
	local move = math.min(TELEPORT_SPEED * dt / 1000.0, 1.0);
	local px, py = player:getX(), player:getY();
	while move > 0.0001 and task.node <= #task.path do
		local wp = task.path[task.node];
		-- BFS 档存格坐标 (走格中心); 穿墙档存绝对小数坐标
		local tx, ty;
		if task.linear then tx, ty = wp[1], wp[2];
		else tx, ty = wp[1] + 0.5, wp[2] + 0.5; end
		local dx, dy = tx - px, ty - py;
		local dist = math.sqrt(dx * dx + dy * dy);
		if dist <= move then
			player:setX(tx);
			player:setY(ty);
			px, py = tx, ty;
			move = move - dist;
			task.node = task.node + 1;
		else
			player:setX(px + dx / dist * move);
			player:setY(py + dy / dist * move);
			move = 0;
		end
	end
	if task.node > #task.path then
		-- 终点微调到精确点击位置 (与最后路径格同格, floor 后同格不触发 NoClip)
		player:setX(task.tx);
		player:setY(task.ty);
		stopTeleport("done");
	end
end

MapActions.onTeleportTick = onTeleportTick;
Events.OnTick.Add(MapActions.onTeleportTick);

function MapActions:onTeleport(x, y)
	if teleportTask ~= nil or teleportClimbActive
		or type(teleportClimbPending) == "function" and teleportClimbPending(nil) then
		return
	end
	local player = self.localPlayer or getPlayer();
	if player == nil then return; end

	-- C3 (九十四): 在驾驶位时本次点击改走"连人带车"档 (一份实现两用, 见文件下方车辆档)
	if self:startVehicleTeleport(x, y) then return; end

	if not isMultiplayer() then
		-- 单人: 无服务端速度校验, 瞬时传送 (与旧行为一致)
		player:setX(x);
		player:setY(y);
		return;
	end

	-- MP: 先寻路再沿路走 (直线穿墙, 开 antiCheatNoClip 的服必踢)
	local sx, sy = math.floor(player:getX()), math.floor(player:getY());
	local path = findTeleportPath(sx, sy, math.floor(x), math.floor(y),
		math.floor(player:getZ() + 0.001));
	if path == nil then return; end
	-- 目标不可达时 BFS 收敛在最近可达格: 终点吸附改用该格中心,
	-- 否则收尾 snap 会把玩家吸进不可达的墙格 (沙盒实测抓到)
	if #path > 0 then
		local lastW = path[#path];
		if lastW[1] ~= math.floor(x) or lastW[2] ~= math.floor(y) then
			x, y = lastW[1] + 0.5, lastW[2] + 0.5;
		end
	end
	teleportTask = { path = path, node = 1, lastMs = nil, tx = x, ty = y };
end

--*********************************************************
--* 穿墙档: 直线路径不查连通, 与安全档共用限速/取消/超时。
--* MP 须先提交所属玩家状态, 最终移动经过两阶段服务器周期回声后再结束状态;
--* 同步失败不继续写坐标, 正常流程与异常边界仍需自建服实测。
--*********************************************************
function MapActions:onTeleportLinear(x, y)
	if teleportTask ~= nil then
		return
	end
	local player = self.localPlayer or getPlayer();
	if player == nil then return; end

	-- C3 (九十四): 在驾驶位时本次点击改走"连人带车"档
	if self:startVehicleTeleport(x, y) then return; end

	if not isMultiplayer() then
		player:setX(x);
		player:setY(y);
		return;
	end

	local sx, sy = player:getX(), player:getY();
	local dist = math.sqrt((x - sx) * (x - sx) + (y - sy) * (y - sy));
	if dist < 1.0 then return; end
	-- 中途点间距 0.75 格, 单 tick 仍钳 ≤1.0; 网络采样间隔可跨多步。
	local n = math.max(1, math.ceil(dist / 0.75));
	local path = {};
	for i = 1, n do
		path[i] = { sx + (x - sx) * i / n, sy + (y - sy) * i / n };
	end
	if isClient() then
		if teleportClimbActive or type(teleportClimbPending) == "function" and teleportClimbPending(nil) then return; end
		if type(teleportClimbState) ~= "function" or type(teleportClimbReady) ~= "function"
			or type(teleportClimbPending) ~= "function" or not teleportClimbState(player, true) then
			stopTeleport("sync-failed");
			if HaloTextHelper ~= nil then HaloTextHelper.addBadText(player, getTranslate("UI_Map_TeleportSyncFailed")); end
			return;
		end
		teleportClimbActive = true;
		teleportClimbPlayer = player;
	end
	teleportTask = { path = path, linear = true, player = player, node = 1, lastMs = nil, tx = x, ty = y };
end
--*********************************************************
--* C3 连人带车传送 (2026-09-14 九十四, 见 analysis/DLL分析/C-传送与载具-设计方案(C1-C3已实施).md §C3)
--*
--* 入口与「地图传送」共用 (用户裁定: 一份实现两用): 玩家坐在**驾驶位**时, 地图右键传送
--* 自动改为"连人带车" —— 载具不能走 BFS 人行路径, 车辆档一律走直线分跳。乘客不启动
--* (载具物理属主=司机客户端, 乘客侧推不动车), 点传送时给红字提示。
--*
--* 单跳 = 一次原生物理体落位, 四道保护 (九十四 v3, 全部来自 v2 实测教训):
--*   ① **目的地就绪才动手**: vehicleNativeDestReady 判"区块在区块图内 + 格存在"。
--*      v2 实测: 目的地没加载时硬跳 → 原生体下面没有地面几何 → 整车自由落体
--*      (z 一路 -6 → -551, 约 20 秒), 只有手动「车辆重置」才拉回来。
--*   ② **降档推进**: 单跳上限 = min(45, 位移预算), 区块图以玩家为中心、随缩放只有
--*      ±(chunkGridWidth/2)*8 格 (最小缩放仅 ±16 格), 所以大档没就绪时按阶梯逐级降档,
--*      用"小步把窗口带过去"代替死等 (全档位都不就绪才等, 等到 VEH_LOAD_WAIT_MS 上限中止)。
--*   ③ **抬高落下 + 逐帧落位判定** (一百零五 方案A, 取代 一百 的"固定 z 平移"): 每跳把物理体
--*      抬高 VEH_RAISE 再平移, 车从目标上空落下, 落位高度由物理引擎解算 —— 消除"旧 z 嵌进
--*      更高地形"的穿地下坠; 落点是水面时车沉到河床停稳 (水面对物理是空的, IsoChunk.calcPhysics
--*      不为水格生成形状), 同样保留。接地/三源一致且跨时间窗高度收敛后才算完成；
--*      从抬高点掉超 VEH_ABORT_DROP 仍未停 = 真空区, 当帧回滚; 超时未落地 = 失败回滚。
--*      (一百 的"下沉超过 15/5 就回滚"两条线已删 —— 它们会把合法的落谷/落水误杀成 sinking。)
--*   ④ **反作弊位移预算** (一百零一 重做, 取代 一百 的判据): 被查的量不是"载具速度", 而是
--*      **乘客的 SpeedChecker**, 喂进去的是**物理包里的载具坐标**
--*      (VehiclePhysicsPacket.resetMovable → speedChecker.set(x, y, true, nil)), 按 **1000ms 门**
--*      采样算 位移*1000/间隔 = 格/秒 (NetworkCharacterAI.SpeedChecker), 阈值 =
--*      ServerOptions.SpeedLimit (默认 70; AntiCheatSpeed.java:33)。
--*      **一百 的漏洞**: 采样间隔 dt ∈ (1000, 1000+p], p = 到下一个物理包的等待, 而物理包周期
--*      = 150ms (BaseVehicle.limitPhysicSend) → dt 实际可达 ~1150ms。所以"间隔 ≥1000ms 就最多
--*      含 1 跳"不成立 —— 实测就是这么被踢的: 跳间隔 1006ms, 采样窗跨过两跳
--*      = 90 格 / 1089ms = **82.64 > 70** (服务端日志 speed=82.642967); 而且**一次越线就够**:
--*      speed 是缓存值, 同一秒内每个后续包都会重新上报它 → 4 格计数器在 5 帧内烧满 → 踢出。
--*      现在的判据 = **不变式**: 任意 ≤W 的时间窗内载具位移 ≤ B
--*      ⇒ 任何采样窗 (dt ≥ 1000ms) 读数 ≤ B*1000/dt ≤ B = 0.6*限速, 恒不触发。
--*      B/W 由**服务器真实限速**推导 (客户端握手即收到该选项: ConnectionDetails →
--*      ConnectToServerState.receiveServerOptions; 走既有 autoDriveGetSpeedLimit, 缺函数退 70)。
--*   ⑤ **停稳门**: 反作弊读数会把司机自己驾驶的位移一起算进去 (100km/h ≈ 28 格/秒, 叠上单跳
--*      42 格就顶到 70), 所以起跳前要求"车已静止 ≥ VEH_STILL_HOLD_MS" —— 该时长 > 采样窗
--*      上限, 保证起跳那次采样窗里只剩我们自己的单跳位移。
--*   ⑥ **层号 (一百零九)**: 目的地判定与抬高基准一律用**载具逻辑层** (Java 侧 vehicle.getZ(),
--*      vanilla 自己维护的"车在哪一层"), **不用原生物理 z** —— 车落在水面以下时物理 z 为负, 拿来
--*      当楼层用会 fastfloor 成 -1, 而陆地/水面区块 minLevel = 0 ⇒ 任何目的地在任何档位都
--*      no-square ⇒ 车传送不出水域 (用户实测: "汽车在水上动不了, 传送不出去")。抬高基准改为
--*      max(物理 z, 逻辑层 x 2.44949), 保证"始终从目的地地面 +1.5 楼层落下" (否则从水里起跳会
--*      落在陆地地面之下, 又变回"嵌进地形")。陆地/建筑上层两种写法等价 ⇒ 零行为变化。
--*   ⑦ **3×3 区块环 + 冻结判定 (三百三十)**: 目的地必须 3×3 区块全部就绪 —— 否则
--*      BaseVehicle.checkSurroundingChunks 会把物理强制停用, 车停在抬高点上悬空;
--*      旧实现把"z 不再变化"当落地, 区块到位、物理恢复后原生体又把车写回起跳点。
--*      现在持续复核授权/区块环/模拟开关，不再凭高度动过一次绕过冻结；到达后仍有 30 秒看门狗。
--* 取消: ESC / 鼠标左键 (宽限期后), 或不再在驾驶位 / 死亡 / 整程超时。
--*********************************************************
local VEH_HOP_CAP = 45;                          -- 单跳硬上限 (区块窗口半径约 48~56 格, 取 45 留边)
local VEH_STEP_LADDER = { 45, 36, 28, 20, 14, 9, 5, 3, 1 };  -- 取"就绪且不超预算"的最大档
local VEH_BUDGET_RATIO = 0.6;                    -- 位移预算 B = 限速 x 它 (留 40% 余量)
local VEH_WINDOW_MS = 1500;                      -- 位移台账滑窗 W: > 采样窗上限 (1000+150) 留 350ms
local VEH_HOP_MIN_MS = 250;                      -- 跳间绝对下限 (只防小步连跳刷爆包频; 正常由台账管)
local VEH_STILL_EPS = 0.15;                      -- 停稳门: 100ms 内位移超过它 = 车还在动
local VEH_STILL_HOLD_MS = 1300;                  -- 停稳门: 要求的静止时长 (> 采样窗上限 1150ms)
local VEH_STILL_WAIT_MS = 10000;                 -- 等停稳的上限, 超过以 "moving" 中止
local VEH_SETTLE_MAX_MS = 3000;                  -- Java 共用落地观察的硬上限；Lua 同时保留兜底时限
local VEH_RAISE = 3.7;                           -- 方案A 抬高落下: 起跳抬升量 = 1.5 楼层 (物理单位,
                                                 -- 1 楼层 ≈ 2.44949; 用户实测: 初值 15.0 ≈ 6 楼层过高,
                                                 -- 一百零六 按实测调为 1.5 楼层)
local VEH_VERIFY_MAX_SINK = 100.0;               -- 停稳后允许的总下沉 (落水沉河床合法); 兜底防极端
local VEH_ABORT_DROP = 100.0;                    -- 逐帧熔断线: 从抬高点掉超它仍未停 = 真空区, 立即回滚
local VEH_LOAD_WAIT_MS = 8000;                   -- 全档位都不就绪时的等待上限
local VEH_TOTAL_TIMEOUT = 300000;                -- 整程超时
local VEH_CANCEL_GRACE = 800;                    -- 启动后取消输入宽限 (防误吃启动那次点击)
local VEH_LEDGER_WAIT_MS = 4500;                 -- 台账一直不清空的兜底 (3 倍滑窗; 正常不该发生)
local VEH_WATCHDOG_MS = 30000;                   -- 到达后看门狗时长 (确认车没有被同步回原位)
local VEH_WATCHDOG_TILES = 3.0;                  -- 看门狗位移阈值 (超过即提示)

--* 反作弊预算推导 (起跳时算一次): L = 服务器真实限速, B = 单窗口位移预算, D = 单跳上限。
--* 缺 autoDriveGetSpeedLimit (安装态旧) 时退回 70 —— 那正是 SpeedLimit 的默认值。
local function vehBudget()
    local limit = 70;
    if type(autoDriveGetSpeedLimit) == "function" then
        local ok, v = pcall(autoDriveGetSpeedLimit);
        if ok and type(v) == "number" and v >= 10 then limit = v; end
    end
    local budget = math.max(6, math.floor(limit * VEH_BUDGET_RATIO));
    return limit, budget, math.min(VEH_HOP_CAP, budget);
end

local vehTeleportTask = nil;   -- { vehicle=, tx=, ty=, startedMs=, nextHopMs=, expectX=, expectY=, hops=, notReadyMs=,
                                --   observing=, observeDeadline=, hopZ0=, hopZMin=, lastDrop=, uprighted=,
                                --   limit=, budget=, windowMs=, hopMax=, ledger={}, gateOpen=, gateHinted=, ledgerWaitMs= }

--* 位移台账 (一百零一, 反作弊位移预算的执行器):
--*   跳后记一条 (时刻, 实际落点); 起跳前对候选落点算"W 窗内所有记录 + 当前车身位置"的**最大**距离,
--*   超过预算 B 就退档, 退不动就等最老一条滚出滑窗。这条不变式 = "任意 ≤W 窗内位移 ≤ B"。
--*   台账用**实际位置**记账 (不是命令的跳距), 所以车自己滑行/带油门漂出来的位移一样会被扣掉。
local function vehLedgerPush(task, now, x, y)
    task.ledger[#task.ledger + 1] = { t = now, x = x, y = y };
    while #task.ledger > 0 and (now - task.ledger[1].t) > task.windowMs do
        table.remove(task.ledger, 1);
    end
end

local function vehLedgerUsed(task, now, cx, cy, px, py)
    local best = 0;                                  -- 用平方比较, 最后一次开方 (省每帧开销)
    for i = 1, #task.ledger do
        local e = task.ledger[i];
        if (now - e.t) <= task.windowMs then
            local dx, dy = px - e.x, py - e.y;
            local d = dx * dx + dy * dy;
            if d > best then best = d; end
        end
    end
    -- 当前车身位置也算一条虚拟记录 (候选落点相对"现在"的位移同样要进预算)
    local dx, dy = px - cx, py - cy;
    local d = dx * dx + dy * dy;
    if d > best then best = d; end
    return math.sqrt(best);
end

--* 停稳跟踪 (常驻, 100ms 节流, 只在没有传送任务时跑):
--*   用户的实际操作顺序是"先停车 → 再开地图 → 右键传送", 所以静止时长必须从**点之前**就开始
--*   累计, 否则停稳门每次都要白等一个静止窗。车一动 (100ms 内位移 > VEH_STILL_EPS) 就重新计时。
local vehStill = { t = 0, x = 0, y = 0, since = nil };

local function vehTrackStill(now, vehicle)
    if vehicle == nil then
        vehStill.since = nil;                        -- 人不在车上 → 历史作废
        return;
    end
    if (now - vehStill.t) < 100 then return; end
    vehStill.t = now;
    local x, y = vehicle:getX(), vehicle:getY();
    if vehStill.since == nil then
        vehStill.x, vehStill.y, vehStill.since = x, y, now;
        return;
    end
    local dx, dy = x - vehStill.x, y - vehStill.y;
    if (dx * dx + dy * dy) > (VEH_STILL_EPS * VEH_STILL_EPS) then
        vehStill.since = now;
    end
    vehStill.x, vehStill.y = x, y;
end

--* 头顶浮字 (与修车/电台 XP 同款): 汽车传送是秒级~几十秒的长流程, 必须给开始/到位/中止三个反馈,
--* 否则用户看不出"点了没反应"还是"正在跳"。
local function vehHalo(player, key, params, isError)
    if player == nil or HaloTextHelper == nil then return; end
    local color = isError and HaloTextHelper.getColorRed() or HaloTextHelper.getColorGreen();
    HaloTextHelper.addText(player, tr(key, params), "[col=175,175,175], [/]", color);
end

--* 中止原因 → 人话 (其余状态码本身就是自解释的英文枚举, 直接原样显示)
local VEH_REASON_KEYS = {
    ["dest-unloaded"] = "UI_Map_VehTeleportReasonUnloaded",
    ["verify"] = "UI_Map_VehTeleportReasonVerify",
    ["sinking"] = "UI_Map_VehTeleportReasonVerify",
    ["moving"] = "UI_Map_VehTeleportReasonMoving",
    ["frozen"] = "UI_Map_VehTeleportReasonFrozen",
    ["hop"] = "UI_Map_VehTeleportReasonHop",
};

local VEH_DETAIL_KEYS = {
    ["pending-restore"] = "UI_Map_VehTeleportReasonPendingRestore",
    ["restore-timeout"] = "UI_Map_VehTeleportReasonRestoreTimeout",
    ["not-local"] = "UI_Map_VehTeleportReasonNotLocal",
    ["engine-off"] = "UI_Map_VehTeleportEngineOff",
    ["towing-linked"] = "UI_Map_VehTeleportTowing",
    ["waiting-towing"] = "UI_Map_VehTeleportTowing",
    ["physics-off"] = "UI_Map_VehTeleportReasonFrozen",
    ["no-driver"] = "UI_Map_VehTeleportDriverOnly",
};

local function vehAbortReasonText(reason, detail)
    local code = tostring(detail or "");
    if string.sub(code, 1, 8) == "blocked-" then code = string.sub(code, 9); end
    local key = VEH_DETAIL_KEYS[code];
    if string.sub(code, 1, 16) == "physics-rejected" then
        key = "UI_Map_VehTeleportReasonRejected";
    end
    key = key or VEH_REASON_KEYS[reason];
    if key ~= nil then return tr(key); end
    return tostring(reason);
end

local function vehTeleportAbort(reason, detail)
    if vehTeleportTask == nil then return; end
    local task = vehTeleportTask;
    local vehicle = task.vehicle;
    local restoreText = nil;
    if task.observing and vehicle ~= nil then
        if type(vehicleNativeProbeRead) == "function" then
            local probed, snapshot = pcall(vehicleNativeProbeRead, vehicle);
            if probed then print("[VehTeleport] failed landing: " .. tostring(snapshot)); end
        end
        local ok, rolled = pcall(vehicleNativeRollback, vehicle);
        print("[VehTeleport] rollback: " .. tostring(ok and rolled or "error"));
        restoreText = tr(ok and rolled == "ok" and "UI_Map_VehTeleportRestored"
            or "UI_Map_VehTeleportRestorePending");
    end
    if task.observing and vehicle ~= nil and type(vehicleNativeLandingEnd) == "function" then
        pcall(vehicleNativeLandingEnd, vehicle);
    end
    vehTeleportTask = nil;
    UIMap.lastTeleportCancel = reason;
    local reasonText = vehAbortReasonText(reason, detail);
    if restoreText ~= nil then reasonText = reasonText .. "; " .. restoreText; end
    vehHalo(getPlayer(), "UI_Map_VehTeleportAborted", { reason = reasonText }, true);
    print("[VehTeleport] aborted: " .. tostring(reason) .. (detail and (" - " .. detail) or ""));
    --* 一百零九 排障: 中止时补一行三源探针 (逻辑层 lvl= / 物理 z / 由物理 z 反推的层 physLvl= /
    --* 区块窗口)。用户实测那次失败只留下 "aborted: ... no-square", 无法判断是"层不合法"还是
    --* "格没加载" —— 这一行把两者并排打出来。pcall 兜住车辆已消失的情况, 排障不许再引爆。
    if vehicle ~= nil and type(vehicleNativeProbeRead) == "function" then
        local ok, s = pcall(vehicleNativeProbeRead, vehicle);
        if ok then print("[VehTeleport] probe: " .. tostring(s)); end
    end
end

-- 二百七十七 (L12): 空闲期停稳跟踪的 100ms 门限提到桥调用之前 — 原每 tick 白做
-- getPlayer/getVehicle 两次跨界 (16 个常驻 OnTick 中唯一空闲期有实质开销的)
--* 到达后看门狗 (三百三十): 30 秒内若车从落点移动超过 3 格, 打印一次并给红字提示。
--* 用途是确认"过一段时间回到原位"是否已被修好; 只诊断, 不自动再传送。
local vehWatch = nil;
local function vehArmWatch(vehicle, now)
    if vehicle == nil then vehWatch = nil; return; end
    vehWatch = {
        x = vehicle:getX(), y = vehicle:getY(),
        untilMs = now + VEH_WATCHDOG_MS, nextMs = now + 1000, warned = false,
    };
end

local function vehWatchTick(now)
    if vehWatch == nil then return; end
    if now > vehWatch.untilMs then vehWatch = nil; return; end
    if now < vehWatch.nextMs then return; end
    vehWatch.nextMs = now + 1000;
    local player = getPlayer();
    local vehicle = player ~= nil and player:getVehicle() or nil;
    if vehicle == nil then vehWatch = nil; return; end
    local dx, dy = vehicle:getX() - vehWatch.x, vehicle:getY() - vehWatch.y;
    local d2 = dx * dx + dy * dy;
    if not vehWatch.warned and d2 > (VEH_WATCHDOG_TILES * VEH_WATCHDOG_TILES) then
        vehWatch.warned = true;
        local d = math.sqrt(d2);
        vehHalo(player, "UI_Map_VehTeleportWatchdog", { dist = math.floor(d + 0.5) }, true);
        print(string.format("[VehTeleport] watchdog: vehicle moved %.1f tiles after arrival (%.1f,%.1f -> %.1f,%.1f)",
            d, vehWatch.x, vehWatch.y, vehicle:getX(), vehicle:getY()));
    end
end

local vehStillGateMs = 0;
local function onVehTeleportTick()
    local now = getTimestampMs();
    vehWatchTick(now);
    local task = vehTeleportTask;
    if task == nil or not task.gateOpen then
        -- 停稳跟踪 (见 vehTrackStill): 空闲时常驻, 起跳门未开时也要继续跟 —— 否则"刚上车就点
        -- 传送"时静止计时从未开始, 门会一直空等到 VEH_STILL_WAIT_MS 才误判成 moving 中止。
        if now - vehStillGateMs >= 100 then
            vehStillGateMs = now;
            local watched = getPlayer();
            if watched ~= nil then vehTrackStill(now, watched:getVehicle()); end
        end
    end
    if task == nil then return; end
    local player = getPlayer();
    if task.startedMs == nil then
        -- 首帧只记时 (与玩家传送同款: 让宽限期从真实起点算)
        task.startedMs = now;
        task.nextHopMs = now;
        return;
    end
    if player == nil or player:isDead() then
        return vehTeleportAbort("state", "player gone");
    end
    local vehicle = task.vehicle;
    if vehicle == nil or player:getVehicle() ~= vehicle then
        return vehTeleportAbort("state", "left the vehicle");
    end
    if not vehicle:isDriver(player) then
        return vehTeleportAbort("driver", "not in the driver seat anymore");
    end
    if (now - task.startedMs) > VEH_CANCEL_GRACE
        and (isKeyDown(Keyboard.KEY_ESCAPE) or isMouseButtonDown(0)) then
        return vehTeleportAbort("input");
    end
    if (now - task.startedMs) > VEH_TOTAL_TIMEOUT then
        return vehTeleportAbort("total-timeout");
    end

    -- ⑤ 停稳门 (一百零一): 反作弊读数 = 采样窗内载具位移, 会把**司机自己驾驶的位移**一起算进去
    --    (100km/h ≈ 28 格/秒, 叠上单跳 42 格就顶到限速 70)。所以起跳前要求"车已静止
    --    ≥ VEH_STILL_HOLD_MS"; 该时长 > 采样窗上限 (1000+150ms) ⇒ 起跳那次采样窗里只剩单跳位移。
    if not task.gateOpen then
        if vehStill.since ~= nil and (now - vehStill.since) >= VEH_STILL_HOLD_MS then
            task.gateOpen = true;
            task.ledger = {};                        -- 从静止点重新起账 (之前的位移是司机自己的)
            vehLedgerPush(task, now, vehicle:getX(), vehicle:getY());
            task.nextHopMs = now;
            print(string.format(
                "[VehTeleport] vehicle still, hops start (limit=%.0f/s budget=%d tiles/%dms hop<=%d rate=%.1f/s)",
                task.limit, task.budget, task.windowMs, task.hopMax, task.budget * 1000 / task.windowMs));
        else
            if (now - task.startedMs) > VEH_STILL_WAIT_MS then
                return vehTeleportAbort("moving", string.format("still moving after %ds", VEH_STILL_WAIT_MS / 1000));
            end
            if task.gateHinted == nil then
                task.gateHinted = true;
                vehHalo(player, "UI_Map_VehTeleportWaitStop", nil, false);
                print("[VehTeleport] waiting for the vehicle to stop (anti-cheat window)");
            end
            return;
        end
    end

    -- ③ 传送与重置共用落地观察；时间窗未收敛、无支撑或显示分裂都不能报到达。
    if task.observing then
        local z = vehicleNativePhysicsZ(vehicle);
        local live = type(z) == "number" and z == z and math.abs(z) < math.huge;
        if live and task.hopZ0 ~= nil then
            if z < task.hopZMin then task.hopZMin = z; end
            if (task.hopZ0 - z) > VEH_ABORT_DROP then
                return vehTeleportAbort("sinking", string.format("dz=%.1f", z - task.hopZ0));
            end
        end
        local polled, verdict = pcall(vehicleNativeLandingPoll, vehicle);
        if not polled then verdict = "error"; end
        if verdict == "flipped" then
            if task.uprighted then return vehTeleportAbort("verify", "flipped-again"); end
            local ok, up = pcall(vehicleNativeUpright, vehicle);
            if not ok or up ~= "fixed" then
                return vehTeleportAbort("verify", tostring(up));
            end
            task.uprighted = true;
            if not vehicleNativeLandingBegin(vehicle) then
                return vehTeleportAbort("verify", "no-observation");
            end
            task.observeDeadline = now + VEH_SETTLE_MAX_MS;
            print("[VehTeleport] landing was flipped -> uprighted, checking the new landing");
            return;
        end
        if verdict ~= "ok" then
            if verdict == "not-local" or verdict == "no-driver" or verdict == "waiting-towing"
                or verdict == "towing-linked" or verdict == "no vehicle" or verdict == "no-observation"
                or verdict == "chunk-ring-not-loaded" or verdict == "physics-off" then
                return vehTeleportAbort(verdict == "physics-off" and "frozen" or "verify", tostring(verdict));
            end
            if now < task.observeDeadline and string.sub(tostring(verdict), 1, 8) ~= "timeout-" then
                return;
            end
            return vehTeleportAbort("verify", "landing " .. tostring(verdict));
        end
        verdict = vehicleNativeVerify(vehicle, task.expectX, task.expectY, VEH_VERIFY_MAX_SINK);
        if verdict ~= "ok" then
            return vehTeleportAbort("verify", tostring(verdict));
        end
        task.lastDrop = (task.hopZ0 ~= nil and task.hopZMin ~= nil)
            and (task.hopZ0 - task.hopZMin) or 0;
        if not vehicleNativeCommit(vehicle) then
            return vehTeleportAbort("verify", "commit-unconfirmed");
        end
        task.observing = false;
        task.hops = task.hops + 1;
        local ex, ey = task.tx - vehicle:getX(), task.ty - vehicle:getY();
        if math.sqrt(ex * ex + ey * ey) <= 1.5 then
            vehTeleportTask = nil;
            UIMap.lastTeleportCancel = "done";
            vehArmWatch(vehicle, now);
            vehHalo(player, "UI_Map_VehTeleportArrived", { hops = task.hops }, false);
            print(string.format("[VehTeleport] arrived: %d hop(s), vehicle (%.1f,%.1f)",
                task.hops, vehicle:getX(), vehicle:getY()));
            return;
        end
    end

    if now < task.nextHopMs then return; end

    -- 本次跳的位移: 朝目标直线, 末段收缩到目标点
    local vx, vy = vehicle:getX(), vehicle:getY();
    local dx, dy = task.tx - vx, task.ty - vy;
    local dist = math.sqrt(dx * dx + dy * dy);
    if dist <= 1.0 then
        vehTeleportTask = nil;
        UIMap.lastTeleportCancel = "done";
        vehArmWatch(vehicle, now);
        vehHalo(player, "UI_Map_VehTeleportArrived", { hops = task.hops }, false);
        print(string.format("[VehTeleport] arrived: %d hop(s)", task.hops));
        return;
    end
    local reach = math.min(task.hopMax, dist);

    -- ② 降档: 取"就绪 **且** 不超反作弊预算"的最大档位 (两道门: 目的地就绪 / 位移台账)
    local step = nil;
    local lastReady = "?";
    local overBudget, sawNotReady = false, false;
    for i = 1, #VEH_STEP_LADDER do
        local s = math.min(VEH_STEP_LADDER[i], reach);
        if s >= 1 then
            local nx, ny = vx + dx / dist * s, vy + dy / dist * s;
            if vehLedgerUsed(task, now, vx, vy, nx, ny) > task.budget then
                overBudget = true;                   -- 这一档会超预算 → 试更小档 (窗口滚动后自动恢复)
            else
                local ready = vehicleNativeDestReady(vehicle, dx / dist * s, dy / dist * s);
                if ready == "not-local" or ready == "engine-off" or ready == "no-driver"
                    or ready == "waiting-towing" or ready == "towing-linked" then
                    return vehTeleportAbort("verify", tostring(ready));
                end
                lastReady = ready;
                if ready == "ok" then
                    step = s;
                    break;
                end
                sawNotReady = true;
            end
        end
    end
    if step == nil then
        --* 一百零九: "全档位不就绪"原先**完全静默** (用户日志里只有 start 与 aborted, 看不出卡在哪),
        --* 现每次任务最多打一行现场 (last= 具体不就绪原因 + 三源探针), 失败一次就能定位。
        if task.stallLogged == nil then
            task.stallLogged = true;
            local probe = "";
            if type(vehicleNativeProbeRead) == "function" then
                local ok, s = pcall(vehicleNativeProbeRead, vehicle);
                if ok then probe = " | " .. tostring(s); end
            end
            print(string.format("[VehTeleport] no ready step: last=%s overBudget=%s%s",
                tostring(lastReady), tostring(overBudget), probe));
        end
        if sawNotReady then
            if task.notReadyMs == nil then task.notReadyMs = now; end
            if (now - task.notReadyMs) > VEH_LOAD_WAIT_MS then
                return vehTeleportAbort("dest-unloaded", tostring(lastReady));
            end
        else
            task.notReadyMs = nil;
        end
        if overBudget then
            -- 纯预算等待: 最老一条滚出滑窗即可放行 (上限 = 3 倍滑窗, 防"永远不清空"的死循环)
            if task.ledgerWaitMs == nil then task.ledgerWaitMs = now; end
            if (now - task.ledgerWaitMs) > VEH_LEDGER_WAIT_MS then
                return vehTeleportAbort("budget-stall", "ledger window never cleared");
            end
        else
            task.ledgerWaitMs = nil;
        end
        return;   -- 等区块流式加载 / 等预算恢复, 下一 tick 再试
    end
    task.notReadyMs, task.ledgerWaitMs = nil, nil;

    local result = vehicleNativeHop(vehicle, dx / dist * step, dy / dist * step, VEH_RAISE);
    if result ~= "ok" then
        return vehTeleportAbort("hop", tostring(result));
    end
    task.expectX, task.expectY = vx + dx / dist * step, vy + dy / dist * step;
    -- 位移台账: 记"这一跳之后车在哪"; 后面几跳的预算就是从这里算的 (用实际落点, 不用命令跳距)
    vehLedgerPush(task, now, task.expectX, task.expectY);
    -- 自动拼接的可见性: 每跳一行 (档位/剩余距离/预算余量), 控制台能看出是"在连续跳"还是"卡住了"
    print(string.format("[VehTeleport] hop %d: +%d tiles -> %s, %.1f tiles to go, budget %.0f/%d%s",
        task.hops + 1, step, tostring(result), math.max(0, dist - step),
        vehLedgerUsed(task, now, task.expectX, task.expectY, task.expectX, task.expectY), task.budget,
        task.lastDrop and string.format(" (prev landing dz=%.2f)", task.lastDrop) or ""));
    local z0 = vehicleNativePhysicsZ(vehicle);
    if type(z0) == "number" and z0 == z0 and math.abs(z0) < math.huge then
        task.hopZ0, task.hopZMin = z0, z0;
    else
        task.hopZ0, task.hopZMin = nil, nil;
    end
    task.observing = true;
    task.uprighted = false;
    if not vehicleNativeLandingBegin(vehicle) then
        return vehTeleportAbort("verify", "no-observation");
    end
    task.observeDeadline = now + VEH_SETTLE_MAX_MS;
    task.nextHopMs = now + math.max(VEH_HOP_MIN_MS, step * task.windowMs / task.budget);
end
MapActions.onVehTeleportTick = onVehTeleportTick;
Events.OnTick.Add(MapActions.onVehTeleportTick);

--*********************************************************
--* 车辆档分流入口: 调用方 = UIMap:onTeleport / UIMap:onTeleportLinear。
--* 返回 true = 本次点击已由车辆档消费 (无论是否真的启动); false = 不在车里, 交回玩家档。
--*********************************************************
function MapActions:startVehicleTeleport(x, y)
    local player = self.localPlayer or getPlayer();
    if player == nil then return false; end
    local vehicle = player:getVehicle();
    if vehicle == nil then return false; end
    if not vehicle:isDriver(player) then
        -- 乘客: 载具物理属主是司机客户端, 我们推不动车 → 明确拒绝 (不静默失败)
        if HaloTextHelper ~= nil then
            HaloTextHelper.addText(player, getTranslate("UI_Map_VehTeleportDriverOnly"),
                "[col=175,175,175], [/]", HaloTextHelper.getColorRed());
        end
        print("[VehTeleport] refused: only the driver can teleport the vehicle");
        return true;
    end
    if type(vehicleNativeHop) ~= "function" or type(vehicleNativeDestReady) ~= "function"
        or type(vehicleNativePhysicsZ) ~= "function" or type(vehicleNativeVerify) ~= "function"
        or type(vehicleNativeRollback) ~= "function" or type(vehicleNativeUpright) ~= "function"
        or type(vehicleNativeLandingBegin) ~= "function" or type(vehicleNativeLandingPoll) ~= "function"
        or type(vehicleNativeLandingEnd) ~= "function" or type(vehicleNativeCommit) ~= "function" then
        -- 安装态比 Lua 旧 (Java 原语缺失): 明确拒绝, 不进入每帧报错的空转
        print("[VehTeleport] refused: vehicle teleport API missing (reinstall the mod jar)");
        return true;
    end
    if vehTeleportTask ~= nil then
        print("[VehTeleport] already running");
        return true;
    end
    --* 拖挂与未启动在入口就拒绝，避免先报“xx 格 / xx 跳”再在落位检查里失败。
    local entryState = vehicleNativeDestReady(vehicle, 0, 0);
    if entryState == "towing-linked" or entryState == "waiting-towing" then
        vehHalo(player, "UI_Map_VehTeleportTowing", nil, true);
        print("[VehTeleport] refused: " .. tostring(entryState));
        return true;
    end
    if entryState == "engine-off" then
        vehHalo(player, "UI_Map_VehTeleportEngineOff", nil, true);
        print("[VehTeleport] refused: start the engine before teleporting");
        return true;
    end
    vehWatch = nil;                                  -- 新任务开始 → 上一次到达的看门狗作废
    if teleportTask ~= nil then
        UIMap.lastTeleportCancel = "state";
        teleportTask = nil;
    end
    local vx, vy = vehicle:getX(), vehicle:getY();
    local dx, dy = x - vx, y - vy;
    local dist = math.sqrt(dx * dx + dy * dy);
    if dist <= 1.5 then return true; end
    local limit, budget, hopMax = vehBudget();
    vehTeleportTask = {
        vehicle = vehicle, tx = x, ty = y,
        startedMs = nil, nextHopMs = 0, hops = 0,
        ledger = {}, gateOpen = false,
        limit = limit, budget = budget, windowMs = VEH_WINDOW_MS, hopMax = hopMax,
    };
    vehHalo(player, "UI_Map_VehTeleportStart",
        { dist = math.floor(dist + 0.5), hops = math.ceil(dist / hopMax) }, false);
    print(string.format(
        "[VehTeleport] start: target (%.1f,%.1f), %.1f tiles from vehicle (limit=%.0f/s budget=%d tiles/%dms hop<=%d)",
        x, y, dist, limit, budget, VEH_WINDOW_MS, hopMax));
    return true;
end


function MapActions.addTeleportOptions(context, worldX, worldY)
    if type(UIMap) ~= "table" or type(UIMap.startVehicleTeleport) ~= "function" then return; end
    if not getWorld():getMetaGrid():isValidChunk(worldX / 10, worldY / 10) then return; end
    local player = getPlayer();
    if player ~= nil and player:getVehicle() ~= nil then
        context:addOption(getTranslate("UI_Map_VehTeleportContext"), UIMap, UIMap.startVehicleTeleport, worldX, worldY);
    else
        context:addOption(getTranslate("UI_Map_TeleportContext"), UIMap, UIMap.onTeleport, worldX, worldY);
        context:addOption(getTranslate("UI_Map_TeleportLinear"), UIMap, UIMap.onTeleportLinear, worldX, worldY);
    end
end

function MapActions.reset()
    teleportTask = nil;
    endTeleportClimbSync();
    if vehTeleportTask ~= nil then vehTeleportAbort("state", "session reset"); end
    vehWatch = nil;
    vehStill = { t = 0, x = 0, y = 0, since = nil };
    vehStillGateMs = 0;
end
Events.OnMainMenuEnter.Remove(MapActions.reset);
Events.OnMainMenuEnter.Add(MapActions.reset);
