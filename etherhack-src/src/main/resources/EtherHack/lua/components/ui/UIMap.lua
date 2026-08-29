UIMap = ISWorldMap:derive("UIMap")

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
    UIMap.drawWorld = UIMap.drawItems; -- 世界标记与物品标记同一总开关 (雷达页勾选/ESP 模块/小地图按钮三处同步)
end

function UIMap:render() 
	
	self:suspendStencil()
    self:clampStencilRectToParent(0, 0, self:getWidth(), self:getHeight() )

	UIMap.ensureDrawFlags();

	-- Отрисовка зомби
	if UIMap.drawZombies then
		local zombies = getCell():getZombieList()
		for i=1,zombies:size() do
			local zombie = zombies:get(i-1)

			local x = self.mapAPI:worldToUIX(zombie:getX(), zombie:getY());
			local y = self.mapAPI:worldToUIY(zombie:getX(), zombie:getY());

			local size = 125 / self.mapAPI:getWorldScale()
			size = clamp(size, 2, 5)

			self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.zombieColor.a, self.zombieColor.r, self.zombieColor.g, self.zombieColor.b);
			self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
		end
	end

	-- Отрисовка машин
	if UIMap.drawVehicles then
		local vehicles = getCell():getVehicles():toArray()
		for i=1,#vehicles do
			local vehicle = vehicles[i]

			local x = self.mapAPI:worldToUIX(vehicle:getX(), vehicle:getY());
			local y = self.mapAPI:worldToUIY(vehicle:getX(), vehicle:getY());

			local size = 125 / self.mapAPI:getWorldScale()
			size = clamp(size, 2, 5)

			self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.vehicleColor.a, self.vehicleColor.r, self.vehicleColor.g, self.vehicleColor.b);
			self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
			if self.mapAPI:getWorldScale() > 5 then
			self:drawTextCentre(vehicle:getScriptName(), x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
			self:drawTextCentre(vehicle:getScriptName(), x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
		end
		end
	end

	-- Отрисовка других игроков
	if UIMap.drawAllPlayers then
		local players = getOnlinePlayers()

		if players ~= nil then
			for i=1,players:size() do
				local player = players:get(i-1)
				if player ~= self.localPlayer then
					local x = self.mapAPI:worldToUIX(player:getX(), player:getY());
					local y = self.mapAPI:worldToUIY(player:getX(), player:getY());

					local size = 125 / self.mapAPI:getWorldScale()
					size = clamp(size, 2, 5)

					self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.playerColor.a, self.playerColor.r, self.playerColor.g, self.playerColor.b);
					self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
					if self.mapAPI:getWorldScale() > 1 then
						self:drawTextCentre(player:getUsername(), x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
						self:drawTextCentre(player:getUsername(), x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
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
	
		local size = 125 / self.mapAPI:getWorldScale()
		size = clamp(size, 2, 5)
	
		self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, self.localPlayerColor.a, self.localPlayerColor.r, self.localPlayerColor.g, self.localPlayerColor.b);
		self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
		if self.mapAPI:getWorldScale() > 1 then
			self:drawTextCentre(player:getUsername(), x + 1, y + 6, 0.0, 0.0, 0.0, 1.0, UIFont.Small);
			self:drawTextCentre(player:getUsername(), x, y + 5, 1.0, 1.0, 1.0, 1.0, UIFont.Small);
		end
	end

	-- Отрисовка найденных предметов (поиск по миру); 刷新由事件驱动 (EtherItemSearch.refresh)
	if UIMap.drawItems and EtherItemSearch.results ~= nil then
		for _, p in pairs(EtherItemSearch.results) do
			local x = self.mapAPI:worldToUIX(p.x, p.y);
			local y = self.mapAPI:worldToUIY(p.x, p.y);

			-- 与玩家/僵尸标记同尺寸
			local size = 125 / self.mapAPI:getWorldScale()
			size = clamp(size, 2, 5)

			self:drawRect(x - size, y - size, size * 2 - 1, size * 2 - 1, 1.0, 0.75, 0.75, 0.75);
			self:drawRectBorder(x - size, y - size, size * 2, size * 2, 1, 0, 0, 0);
			if p.count > 1 then
				self:drawTextCentre(tostring(p.count), x, y + size + 2, 0.75, 0.75, 0.75, 1.0, UIFont.Small);
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
		local option = context:addOption(getTranslate("UI_Map_TeleportContext"), self, self.onTeleport, worldX, worldY)
	end
end

--*********************************************************
--* Безопасная телепортация
--* MP 修复 (2026-08-25): 旧实现单帧内逐格 setX+sendPlayer 瞬移,
--* 服务端 AntiCheatSpeed 以 ~1s 窗口采样位移均值 (SpeedChecker,
--* NetworkCharacterAI.java:330-382, speed=位移*1000/delta),
--* >20格/s 判违规, 默认策略 antiCheatSpeed=2=踢出 → 必被踢。
--* 改为 OnTick 时间限速步进: 15格/s 匀速推进 (上限20的安全余量),
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
--* Java 侧 safePlayerTeleport 保留未动 (SafeAPI/ProtectionManagerX
--* 的防篡改名单引用其名), 但 Lua 不再调用。
--*********************************************************
local TELEPORT_SPEED = 18.0;   -- 格/秒 (SpeedChecker 窗口均值余量: 上限20, 用户要求激进档18)
local teleportTask = nil;      -- 进行中的传送 { path=, node=, lastMs=, tx=, ty= }

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
	-- 玩家主动按键 (移动/跳跃) 立即终止快速移动, 交还操控权
	if isKeyDown(Keyboard.KEY_W) or isKeyDown(Keyboard.KEY_A) or isKeyDown(Keyboard.KEY_S)
		or isKeyDown(Keyboard.KEY_D) or isKeyDown(Keyboard.KEY_SPACE) then
		teleportTask = nil;
		return;
	end
	local now = getTimestampMs();
	if task.lastMs == nil then
		task.lastMs = now;
		return;
	end
	local dt = now - task.lastMs;
	task.lastMs = now;
	if dt <= 0 then return; end;
	if dt > 250 then dt = 250; end;
	-- 单 tick 位移钳 ≤1.0 格: floor 后要么同格 (NoClip 跳过) 要么相邻格
	-- (len=1.0 走 checkPathClamp 分支, BFS 已保证连通+门窗), 永不触发
	-- "Long blocked" (>2.5) 与对角分支 (1.0<len<2.0)
	local move = math.min(TELEPORT_SPEED * dt / 1000.0, 1.0);
	local px, py = player:getX(), player:getY();
	while move > 0.0001 and task.node <= #task.path do
		local wp = task.path[task.node];
		local tx, ty = wp[1] + 0.5, wp[2] + 0.5;   -- 格中心 (floor 后恒为目标格)
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