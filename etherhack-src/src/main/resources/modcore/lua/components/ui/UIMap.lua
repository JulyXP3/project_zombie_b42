UIMap = ISWorldMap:derive("UIMap")

--*********************************************************
--* P2/E1 性能与绘制策略 (2026-09-14 八十五)
--* P2: 本文件 render() 每帧跑 (可移动小地图内嵌了一个 UIMap 实例 → 小地图常驻时是持续负载):
--*   ① 车辆清单改为**每秒**重建一次 (旧实现每帧 getVehicles():toArray() 复制整张车辆表,
--*      且视口过滤在复制之后 —— 屏外几百辆车照样复制); 坐标仍每帧实时读;
--*   ② 玩家标签串按 (名字, 隐身位) 缓存, 不再每帧拼接; 翻译串按语言缓存;
--*   ③ 去掉每帧每玩家的两次 pcall (isInvisible/getOnlineID 均为原版方法, 恒存在)。
--* E1 (用户要求 2026-09-14): 隐身者坐标被服务端停更 → **冻结 5 秒后清掉记录与绘制**,
--*   不再把"最后已知位置"长期钉在地图上挡视线 (旧实现 TTL 10 分钟)。
--*********************************************************
local E1_FREEZE_CLEAR_MS = 5000;    -- 坐标停止变化多久后清掉隐身标记
local VEHICLE_LIST_TTL_MS = 1000;   -- 车辆清单重建间隔

--- P2: 车辆清单缓存 (车辆增删远慢于帧率; 位置每帧照读, 不影响平滑)
function UIMap.vehicleList()
    local nowMs = getTimestampMs();
    if UIMap.vehicleListCache == nil or nowMs - (UIMap.vehicleListTs or 0) > VEHICLE_LIST_TTL_MS then
        UIMap.vehicleListCache = getCell():getVehicles():toArray();
        UIMap.vehicleListTs = nowMs;
    end
    return UIMap.vehicleListCache;
end

--- P2: 地图标注用的翻译串 (按语言缓存, 避免每帧每标签查表)
function UIMap.mapTagStrings()
    local lang = getLanguage();
    if UIMap.tagCacheLang ~= lang then
        UIMap.tagCacheLang = lang;
        UIMap.tagInvisible = getTranslate("UI_Map_InvisibleTag");
        UIMap.tagLastKnown = getTranslate("UI_Map_LastKnown");
    end
    return UIMap.tagInvisible, UIMap.tagLastKnown;
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
function UIMap:createChildren() end

--*********************************************************
--* Восстановление настроек
--*********************************************************
function UIMap:restoreSettings()
	if not MainScreen.instance or not MainScreen.instance.inGame then return end
	local settings = WorldMapSettings.getInstance()
	if settings:getFileVersion() ~= 1 then return end
	local centerX = settings:getDouble("WorldMap.CenterX", 0.0)
	local centerY = settings:getDouble("WorldMap.CenterY", 0.0)
	local zoom = settings:getDouble("WorldMap.Zoom", 0.0)
	if zoom == 0.0 then return end -- ISMiniMap loaded settings for the first time
	local isometric = settings:getBoolean("WorldMap.Isometric")

	if self.localPlayer ~= nil then
		centerX = self.localPlayer:getX()
		centerY = self.localPlayer:getY()
		zoom = 18;
	end
	
	self.mapAPI:centerOn(centerX, centerY)
	self.mapAPI:setZoom(zoom)
	self.mapAPI:setBoolean("Isometric", isometric)
end

--*********************************************************
--* Отрисовка символов
--*********************************************************
function UIMap:onToggleSymbols() 

end
--*********************************************************
--* Ограничение значений
--*********************************************************
local function clamp(val, lower, upper)
    if lower > upper then lower, upper = upper, lower end
    return math.max(lower, math.min(upper, val))
end

function UIMap:prerender()
	if self.centerByPlayer then
		if self.dragging then return end
		local playerObj = self.localPlayer;
		if not playerObj then return end
		local vehicle = playerObj:getVehicle();
		if vehicle then
			self.mapAPI:centerOn(vehicle:getX(), vehicle:getY())
		else
			self.mapAPI:centerOn(playerObj:getX(), playerObj:getY())
		end
	end
end
--*********************************************************
--* Отрисовка
--*********************************************************
--*********************************************************
--* 图层显示开关 (小地图上方按钮控制, 首次初始化为游戏原设置)
--*********************************************************
function UIMap.ensureDrawFlags()
    if UIMap.drawZombies ~= nil then return end
    UIMap.drawZombies = isMapDrawZombies();
    UIMap.drawVehicles = isMapDrawVehicles();
    UIMap.drawAllPlayers = isMapDrawAllPlayers();
    UIMap.drawLocalPlayer = isMapDrawLocalPlayer();
    UIMap.drawItems = isMapDrawItems();
    UIMap.drawItemEsp = false; -- ESP 画线追踪 (世界画面雷达线): 会话级, 独立于小地图标记
end

function UIMap:render() 
	
	self:suspendStencil()
    self:clampStencilRectToParent(0, 0, self:getWidth(), self:getHeight() )

	UIMap.ensureDrawFlags();

	-- 每帧共享量: 世界缩放/标记尺寸/视口剔除半径 —— 桥调用一次提出循环外
	-- (原每符号各调一次, 700 符号 ≈ 3500+ 桥调用/帧, 是"注入后 hitch 膨胀"主源)
	local worldScale = self.mapAPI:getWorldScale()
	local baseSize = 125 / worldScale
	local size = clamp(baseSize, 2, 5)
	-- 视口剔除半径(世界格): 高缩放时视野仅几格, 视口外符号跳过全套桥换算;
	-- 低缩放(拉远)时半径自动覆盖全列表, 不误删。+3 格余量防边缘抖动
	local px, py = self.localPlayer:getX(), self.localPlayer:getY()
	local visR = (math.min(self:getWidth(), self:getHeight()) / 2) / worldScale + 3

	-- Отрисовка зомби
	if UIMap.drawZombies then
		local zombies = getCell():getZombieList()
		for i=1,zombies:size() do
			local zombie = zombies:get(i-1)

			local zx, zy = zombie:getX(), zombie:getY()
			if math.abs(zx - px) > visR or math.abs(zy - py) > visR then
				-- 视口外: 纯 Lua 判断跳过, 不走 worldToUIX/Y 桥
			else
				local x = self.mapAPI:worldToUIX(zx, zy);
				local y = self.mapAPI:worldToUIY(zx, zy);

				self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.zombieColor.a, self.zombieColor.r, self.zombieColor.g, self.zombieColor.b);
				self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
			end
		end
	end

	-- Отрисовка машин
	if UIMap.drawVehicles then
		local vehicles = UIMap.vehicleList()   -- P2: 清单每秒重建一次 (坐标仍每帧读)
		for i=1,#vehicles do
			local vehicle = vehicles[i]
			local vx, vy = vehicle:getX(), vehicle:getY()
			if math.abs(vx - px) > visR or math.abs(vy - py) > visR then
				-- 视口外跳过
			else
				local x = self.mapAPI:worldToUIX(vx, vy);
				local y = self.mapAPI:worldToUIY(vx, vy);

				self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.vehicleColor.a, self.vehicleColor.r, self.vehicleColor.g, self.vehicleColor.b);
				self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
				if worldScale > 5 then
				self:drawTextCentre(vehicle:getScriptName(), x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
				self:drawTextCentre(vehicle:getScriptName(), x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
			end
			end
		end
	end

	-- Отрисовка других игроков
	if UIMap.drawAllPlayers then
		if UIMap.invisibleSeen == nil then UIMap.invisibleSeen = {}; end   -- E1: onlineID -> 最后已知位置
		if UIMap.labelCache == nil then UIMap.labelCache = {}; end         -- P2: onlineID -> 缓存标签
		local currentIds = {};
		local nowMs = getTimestampMs();
		local tagInv, tagLast = UIMap.mapTagStrings();                     -- P2: 翻译串按语言缓存
		local players = getOnlinePlayers()

		if players ~= nil then
			for i=1,players:size() do
				local player = players:get(i-1)
				if player ~= self.localPlayer then
					local ox, oy = player:getX(), player:getY()
					if math.abs(ox - px) > visR or math.abs(oy - py) > visR then
						-- 视口外跳过
					else
						-- P2: 直调原版方法 (isInvisible/getOnlineID 恒存在), 不再每帧每玩家两次 pcall
						local inv = player:isInvisible();
						local name = player:getUsername();
						local key = tostring(player:getOnlineID());
						currentIds[key] = true;

						-- E1 (2026-09-14 八十五, 用户要求): 隐身者坐标被服务端停更 →
						-- 只在**坐标确实变过**时刷新记录; 冻结超过 E1_FREEZE_CLEAR_MS 即清掉
						-- 记录并停止绘制 (旧实现会一直把冻结坐标钉在地图上挡视线)
						local drawThis = true;
						if inv then
							local rec = UIMap.invisibleSeen[key];
							if rec == nil or rec.x ~= ox or rec.y ~= oy then
								UIMap.invisibleSeen[key] = { x = ox, y = oy, name = name, ts = nowMs };
							else
								rec.name = name;
								if nowMs - rec.ts > E1_FREEZE_CLEAR_MS then
									UIMap.invisibleSeen[key] = nil;   -- 冻结超时: 清掉
									drawThis = false;
								end
							end
						else
							UIMap.invisibleSeen[key] = nil;           -- 已恢复可见: 清记录
						end

						if drawThis then
							local x = self.mapAPI:worldToUIX(ox, oy);
							local y = self.mapAPI:worldToUIY(ox, oy);

							self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.playerColor.a, self.playerColor.r, self.playerColor.g, self.playerColor.b);
							self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
							if worldScale > 1 then
								-- P2: 标签串按 (名字, 隐身位) 缓存, 变化才重建
								local cached = UIMap.labelCache[key];
								if cached == nil or cached.name ~= name or cached.inv ~= inv then
									cached = { name = name, inv = inv,
										label = inv and (name .. " " .. tagInv) or name };
									UIMap.labelCache[key] = cached;
								end
								self:drawTextCentre(cached.label, x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
								self:drawTextCentre(cached.label, x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
							end
						end
					end
				end
			end
		end

		-- E1 修订 (2026-09-14 八十五, 用户要求): 隐身"最后已知位置"粘性标记 ——
		-- 服务端对隐身者停止坐标同步, 其从在线列表消失 (实测); 记录在坐标停止变化后
		-- E1_FREEZE_CLEAR_MS 内仍绘制 (名字带"·最后位置"以免误读为实时), 超时即清掉,
		-- 不再长时间钉在地图上 (旧实现 TTL 10 分钟)
		for key, rec in pairs(UIMap.invisibleSeen) do
			if not currentIds[key] then
				if nowMs - rec.ts > E1_FREEZE_CLEAR_MS then
					UIMap.invisibleSeen[key] = nil;
				elseif math.abs(rec.x - px) <= visR and math.abs(rec.y - py) <= visR then
					local x = self.mapAPI:worldToUIX(rec.x, rec.y);
					local y = self.mapAPI:worldToUIY(rec.x, rec.y);
					self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.playerColor.a, self.playerColor.r, self.playerColor.g, self.playerColor.b);
					self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
					if worldScale > 1 then
						local lbl = rec.name .. " " .. tagInv .. tagLast;
						self:drawTextCentre(lbl, x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
						self:drawTextCentre(lbl, x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
					end
				end
			end
		end
	end

	-- Отрисовка локального игрока
	if UIMap.drawLocalPlayer then
		local player = self.localPlayer;
		
		local x = self.mapAPI:worldToUIX(player:getX(), player:getY());
		local y = self.mapAPI:worldToUIY(player:getX(), player:getY());
	
		self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.localPlayerColor.a, self.localPlayerColor.r, self.localPlayerColor.g, self.localPlayerColor.b);
		self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
		if worldScale > 1 then
			self:drawTextCentre(player:getUsername(), x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
			self:drawTextCentre(player:getUsername(), x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
		end
	end

	-- Отрисовка найденных предметов (поиск по миру); 刷新由事件驱动 (EtherItemSearch.refresh)
	if UIMap.drawItems and EtherItemSearch.results ~= nil then
		for _, p in pairs(EtherItemSearch.results) do
			local ix, iy = p.x, p.y
			if math.abs(ix - px) > visR or math.abs(iy - py) > visR then
				-- 视口外跳过
			else
				local x = self.mapAPI:worldToUIX(ix, iy);
				local y = self.mapAPI:worldToUIY(ix, iy);

				-- 与玩家/僵尸标记同尺寸
				self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, 1.0, 0.75, 0.75, 0.75);
				self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
				if p.count > 1 then
					self:drawTextCentre(tostring(p.count), x, y + size + 2, 0.75, 0.75, 0.75, 1.0, UIFont.Small);
				end
			end
		end
	end

	self:clearStencilRect()
    self:resumeStencil()
end

--*********************************************************
--* Нажатие джойстиком
--*********************************************************
function UIMap:onJoypadDown()

end

--*********************************************************
--* ЛКМ - нажатие клавиши
--*********************************************************
function UIMap:onMouseDown(x, y)
	self.dragging = true
	self.dragMoved = false
	self.dragStartX = x
	self.dragStartY = y
	self.dragStartCX = self.mapAPI:getCenterWorldX()
	self.dragStartCY = self.mapAPI:getCenterWorldY()
	self.dragStartZoomF = self.mapAPI:getZoomF()
	self.dragStartWorldX = self.mapAPI:uiToWorldX(x, y)
	self.dragStartWorldY = self.mapAPI:uiToWorldY(x, y)
	return true
end

--*********************************************************
--* Движение мыши
--*********************************************************
function UIMap:onMouseMove(dx, dy)
	if self.dragging then
		local mouseX = self:getMouseX()
		local mouseY = self:getMouseY()
		if not self.dragMoved and math.abs(mouseX - self.dragStartX) <= 4 and math.abs(mouseY - self.dragStartY) <= 4 then
			return
		end
		self.dragMoved = true
		local worldX = self.mapAPI:uiToWorldX(mouseX, mouseY, self.dragStartZoomF, self.dragStartCX, self.dragStartCY)
		local worldY = self.mapAPI:uiToWorldY(mouseX, mouseY, self.dragStartZoomF, self.dragStartCX, self.dragStartCY)
		self.mapAPI:centerOn(self.dragStartCX + self.dragStartWorldX - worldX, self.dragStartCY + self.dragStartWorldY - worldY)
	end
	return true
end

--*********************************************************
--* Движение мыши вне карты
--*********************************************************
function UIMap:onMouseMoveOutside(dx, dy)
	return self:onMouseMove(dx, dy)
end

--*********************************************************
--* ЛКМ - поднятие клавиши
--*********************************************************
function UIMap:onMouseUp(x, y)
	self.dragging = false
	return true
end

--*********************************************************
--* ЛКМ - поднятие клавиши мыши вне карты
--*********************************************************
function UIMap:onMouseUpOutside(x, y)
	self.dragging = false
	return true
end

--*********************************************************
--* Движение колесика мыши
--*********************************************************
function UIMap:onMouseWheel(del)
	self.mapAPI:zoomAt(self:getMouseX(), self:getMouseY(), del)
	return true
end

--*********************************************************
--* ПКМ - нажатие клавиши
--*********************************************************
function UIMap:onRightMouseDown(x, y)
	return false
end

--*********************************************************
--* ПКМ - поднятие клавиши
--*********************************************************
function UIMap:onRightMouseUp(x, y) 
	local context = ISContextMenu.get(0, x + self:getAbsoluteX(), y + self:getAbsoluteY())

	local player = self.localPlayer;
	local worldX = self.mapAPI:uiToWorldX(x, y)
	local worldY = self.mapAPI:uiToWorldY(x, y)

	-- ±100 格限制已移除 (2026-08-25 用户要求): 限速步进与距离无关, 远距只是
	-- 路程时间更长; 保留区块有效性检查 (未生成区块无法寻路)
	if getWorld():getMetaGrid():isValidChunk(worldX / 10, worldY / 10) then
		-- C3 (2026-09-14 九十六 用户裁定): 坐在车里时菜单换**独立的「汽车传送」项** ——
		-- 步行两档在车内本就无效 (原版传送被载具状态拦下), 汽车档是一次点完的**自动拼接**:
		-- 按 25 格/跳连续推进直到目标 (距离多长都只点一次), 目的地未加载时自动降档/等待。
		-- 乘客也能看到该项, 但会被明确拒绝并红字说明原因 (载具物理属主 = 司机客户端)。
		local menuPlayer = self.localPlayer or getPlayer();
		if menuPlayer ~= nil and menuPlayer:getVehicle() ~= nil then
			context:addOption(getTranslate("UI_Map_VehTeleportContext"), self, self.startVehicleTeleport, worldX, worldY)
		else
			context:addOption(getTranslate("UI_Map_TeleportContext"), self, self.onTeleport, worldX, worldY)
			-- 穿墙档 (2026-09-14): 直线路径不查连通 —— 对应 PienZ TileTeleport 的"直线"兜底档;
			-- 服务端 antiCheatNoClip 关闭 (默认) 时可用, 开启会撞 checkPathClamp/checkReachablePath
			-- 被踢 (文案已标注)。单机两档同为瞬时, 选项保留以保持菜单一致 (2026-09-14 用户实测反馈
			-- MP 下选项缺失, 去掉 isMultiplayer 门控排除该变量)。
			context:addOption(getTranslate("UI_Map_TeleportLinear"), self, self.onTeleportLinear, worldX, worldY)
		end
	end
	return true;   -- 消费事件: 不再向下游传播 (阻断原版"导航前往此处"混入同一菜单)
end

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
--* 门/窗 IsOpen, 保守不放行可翻越型), 只走正交步 (len=1 不触发
--* 对角分支), 单 tick 位移钳 ≤1.0 格 (包间位移恒 <2.5 且 floor 后
--* 非同格即相邻)。目标被围死 → 传到 BFS 中离目标最近的可达格。
--* Java 侧 movePlayerToPos 保留未动 (SafeAPI/ProtectionManagerX
--* 的防篡改名单引用其名), 但 Lua 不再调用。
--*********************************************************
local TELEPORT_SPEED = 17.0;   -- 格/秒 (SpeedChecker 上限 20; 2026-09-14 用户拍板 13→17)
local TELEPORT_STEP_TIMEOUT = 5000;    -- A6: 单步无进展超时 (ms)
local TELEPORT_TOTAL_TIMEOUT = 120000; -- A6: 整程超时 (ms)
local TELEPORT_CANCEL_GRACE = 200;     -- A6: 启动后取消输入宽限 (ms), 防误吃启动那次点击
local teleportTask = nil;      -- 进行中的传送 { path=, node=, lastMs=, startedMs=, lastNodeMs=, nodeSeen=, tx=, ty= }
UIMap.lastTeleportCancel = nil; -- A6: 取消/结束原因 ("input"/"state"/"step-timeout"/"total-timeout"/"done")

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

local function onTeleportTick()
	local task = teleportTask;
	if task == nil then return; end
	local player = getPlayer();
	if player == nil then
		teleportTask = nil;
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
		UIMap.lastTeleportCancel = "input";
		teleportTask = nil;
		return;
	end
	-- A6: 状态异常 (死亡/进入载具) 时路径已失效, 立即终止避免位置跳变
	if player:isDead() or player:getVehicle() ~= nil then
		UIMap.lastTeleportCancel = "state";
		teleportTask = nil;
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
		UIMap.lastTeleportCancel = "step-timeout";
		teleportTask = nil;
		return;
	end
	if (now - task.startedMs) > TELEPORT_TOTAL_TIMEOUT then
		UIMap.lastTeleportCancel = "total-timeout";
		teleportTask = nil;
		return;
	end
	-- 单 tick 位移钳 ≤1.0 格: floor 后要么同格 (NoClip 跳过) 要么相邻格
	-- (len=1.0 走 checkPathClamp 分支, BFS 已保证连通+门窗), 永不触发
	-- "Long blocked" (>2.5) 与对角分支 (1.0<len<2.0)
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
		UIMap.lastTeleportCancel = "done";
		teleportTask = nil;
	end
end

Events.OnTick.Add(onTeleportTick);

function UIMap:onTeleport(x, y)
	if teleportTask ~= nil then
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
--* 穿墙档 (2026-09-14): 直线路径, 不查连通 —— 对应 PienZ TileTeleport 的
--* "直线"兜底档。服务端 antiCheatNoClip 关闭 (默认) 时可用; 开启的服会撞
--* checkPathClamp/checkReachablePath 被踢, 选项只在 MP 显示且文案已标注。
--* 限速/取消/超时与安全档共用同一套 (onTeleportTick)。
--*********************************************************
function UIMap:onTeleportLinear(x, y)
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
	-- 中途点间距 0.75 格: 单 tick 位移钳 ≤1.0 格照常生效, 包间位移恒 <2.5 (不撞 "Long blocked")
	local n = math.max(1, math.ceil(dist / 0.75));
	local path = {};
	for i = 1, n do
		path[i] = { sx + (x - sx) * i / n, sy + (y - sy) * i / n };
	end
	teleportTask = { path = path, linear = true, node = 1, lastMs = nil, tx = x, ty = y };
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
--*      不为水格生成形状), 同样算成功。判定只看"是否停稳": 仍在动就继续等; 停稳 = 校验漂移过关;
--*      从抬高点掉超 VEH_ABORT_DROP 仍未停 = 真空区, 当帧回滚; 超时未停稳 = 失败回滚。
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
local VEH_SETTLE_EPS = 0.05;                     -- 单帧高度变化小于它 = 这一帧没动
local VEH_SETTLE_HOLD_MS = 200;                  -- 连续没动这么久 = 落位完成
local VEH_SETTLE_MAX_MS = 3000;                  -- 落位观察硬上限 (抬高落体需要更久, 一百零五 方案A)
local VEH_RAISE = 3.7;                           -- 方案A 抬高落下: 起跳抬升量 = 1.5 楼层 (物理单位,
                                                 -- 1 楼层 ≈ 2.44949; 用户实测: 初值 15.0 ≈ 6 楼层过高,
                                                 -- 一百零六 按实测调为 1.5 楼层)
local VEH_VERIFY_MAX_SINK = 100.0;               -- 停稳后允许的总下沉 (落水沉河床合法); 兜底防极端
local VEH_ABORT_DROP = 100.0;                    -- 逐帧熔断线: 从抬高点掉超它仍未停 = 真空区, 立即回滚
local VEH_LOAD_WAIT_MS = 8000;                   -- 全档位都不就绪时的等待上限
local VEH_TOTAL_TIMEOUT = 300000;                -- 整程超时
local VEH_CANCEL_GRACE = 800;                    -- 启动后取消输入宽限 (防误吃启动那次点击)
local VEH_LEDGER_WAIT_MS = 4500;                 -- 台账一直不清空的兜底 (3 倍滑窗; 正常不该发生)

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
                                --   observing=, observeDeadline=, settleMs=, hopZ0=, hopZMin=, zPrev=, lastDrop=,
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
};

local function vehAbortReasonText(reason)
    local key = VEH_REASON_KEYS[reason];
    if key ~= nil then return tr(key); end
    return tostring(reason);
end

local function vehTeleportAbort(reason, detail)
    if vehTeleportTask == nil then return; end
    local vehicle = vehTeleportTask.vehicle;
    vehTeleportTask = nil;
    UIMap.lastTeleportCancel = reason;
    vehHalo(getPlayer(), "UI_Map_VehTeleportAborted", { reason = vehAbortReasonText(reason) }, true);
    print("[VehTeleport] aborted: " .. tostring(reason) .. (detail and (" - " .. detail) or ""));
    --* 一百零九 排障: 中止时补一行三源探针 (逻辑层 lvl= / 物理 z / 由物理 z 反推的层 physLvl= /
    --* 区块窗口)。用户实测那次失败只留下 "aborted: ... no-square", 无法判断是"层不合法"还是
    --* "格没加载" —— 这一行把两者并排打出来。pcall 兜住车辆已消失的情况, 排障不许再引爆。
    if vehicle ~= nil and type(vehicleNativeProbeRead) == "function" then
        local ok, s = pcall(vehicleNativeProbeRead, vehicle);
        if ok then print("[VehTeleport] probe: " .. tostring(s)); end
    end
end

local function onVehTeleportTick()
    local now = getTimestampMs();
    local task = vehTeleportTask;
    if task == nil or not task.gateOpen then
        -- 停稳跟踪 (见 vehTrackStill): 空闲时常驻, 起跳门未开时也要继续跟 —— 否则"刚上车就点
        -- 传送"时静止计时从未开始, 门会一直空等到 VEH_STILL_WAIT_MS 才误判成 moving 中止。
        local watched = getPlayer();
        if watched ~= nil then vehTrackStill(now, watched:getVehicle()); end
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

    -- ③ 落位判定 (一百零五 方案A: 抬高落下 —— 停稳即成功): 跳后车从抬高点落下 (落谷/落水到河床
    --    都合法), 判定只看"是否停稳" —— 仍在动 → 继续等 (最多 VEH_SETTLE_MAX_MS); 停稳 → 校验
    --    漂移过关; 从抬高点掉超 VEH_ABORT_DROP 仍未停 = 真空区 (无地面), 当帧回滚; 超时未停稳 = 失败。
    if task.observing then
        local z = vehicleNativePhysicsZ(vehicle);
        local live = (z == z);                       -- NaN = 读不到物理体
        if live and task.hopZ0 ~= nil then
            if task.zPrev == nil or math.abs(z - task.zPrev) > VEH_SETTLE_EPS then
                task.settleMs = now;                 -- 这一帧还在动 → 停稳计时从头开始
            end
            task.zPrev = z;
            if z < task.hopZMin then task.hopZMin = z; end
            if (task.hopZ0 - z) > VEH_ABORT_DROP then
                local rolled = vehicleNativeRollback(vehicle);
                print(string.format("[VehTeleport] runaway fall dz=%.1f (abort limit %.1f, rollback %s)",
                    z - task.hopZ0, VEH_ABORT_DROP, tostring(rolled)));
                return vehTeleportAbort("sinking", string.format("dz=%.1f", z - task.hopZ0));
            end
        end
        if not (live and task.hopZ0 ~= nil and (now - task.settleMs) >= VEH_SETTLE_HOLD_MS)
            and now < task.observeDeadline then
            return;                                  -- 还没停稳, 也没超时 → 继续观察
        end
        task.observing = false;
        -- lastDrop = 抬高点到最低点的总落差 (含落谷/落水的合法下沉, 只作日志取证, 不再回滚)
        task.lastDrop = (task.hopZ0 ~= nil and task.hopZMin ~= nil)
            and (task.hopZ0 - task.hopZMin) or 0;
        local verdict = vehicleNativeVerify(vehicle, task.expectX, task.expectY, VEH_VERIFY_MAX_SINK);
        if verdict ~= "ok" then
            local rolled = vehicleNativeRollback(vehicle);
            print("[VehTeleport] hop verify failed: " .. tostring(verdict)
                .. " (rollback " .. tostring(rolled) .. ")");
            return vehTeleportAbort("verify", tostring(verdict));
        end
        -- 一百零八 落位扶正: 落体触地可能翻覆 (用户实测 1.5 楼层落下会翻车), 未翻则零动作。
        -- 软检查 (老 jar 无此原语时静默跳过), 免得"Lua 新 jar 旧"的安装态直接拒绝传送。
        if type(vehicleNativeUpright) == "function" then
            local up = vehicleNativeUpright(vehicle);
            if up == "fixed" then
                print("[VehTeleport] landing was flipped -> uprighted (heading kept)");
            elseif up ~= "ok" then
                print("[VehTeleport] upright check: " .. tostring(up));
            end
        end
        task.hops = task.hops + 1;
        local ex, ey = task.tx - vehicle:getX(), task.ty - vehicle:getY();
        if math.sqrt(ex * ex + ey * ey) <= 1.5 then
            vehTeleportTask = nil;
            UIMap.lastTeleportCancel = "done";
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
    if z0 == z0 then
        task.hopZ0, task.hopZMin, task.zPrev = z0, z0, nil;
    else
        task.hopZ0, task.hopZMin, task.zPrev = nil, nil, nil;   -- 读不到物理体 → 退回超时判定
    end
    task.observing = true;
    task.settleMs = now;
    task.observeDeadline = now + VEH_SETTLE_MAX_MS;
    task.nextHopMs = now + math.max(VEH_HOP_MIN_MS, step * task.windowMs / task.budget);
end
Events.OnTick.Add(onVehTeleportTick);

--*********************************************************
--* 车辆档分流入口: 调用方 = UIMap:onTeleport / UIMap:onTeleportLinear。
--* 返回 true = 本次点击已由车辆档消费 (无论是否真的启动); false = 不在车里, 交回玩家档。
--*********************************************************
function UIMap:startVehicleTeleport(x, y)
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
        or type(vehicleNativePhysicsZ) ~= "function" then
        -- 安装态比 Lua 旧 (Java 原语缺失): 明确拒绝, 不进入每帧报错的空转
        print("[VehTeleport] refused: vehicle teleport API missing (reinstall the mod jar)");
        return true;
    end
    if vehTeleportTask ~= nil then
        print("[VehTeleport] already running");
        return true;
    end
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

--*********************************************************
--* Создание нового экземпляра
--*********************************************************
function UIMap:new(x, y, width, height)
	local uiTableData = {}

	uiTableData = ISWorldMap:new(x, y, width, height)
	setmetatable(uiTableData, self)
	self.__index = self

	uiTableData.localPlayer = getPlayer();
	uiTableData.localPlayerColor = {r = 0.5, g = 1.0, b = 0.5, a = 1.0}
	uiTableData.playerColor = {r = 1.0, g = 0.2, b = 0.2, a = 1.0}
	uiTableData.vehicleColor = {r = 0.2, g = 0.2, b = 1.0, a = 1.0}
	uiTableData.zombieColor = {r = 1.0, g = 0.5, b = 0.3, a = 1.0}
	uiTableData.centerByPlayer = false;


	return uiTableData
end