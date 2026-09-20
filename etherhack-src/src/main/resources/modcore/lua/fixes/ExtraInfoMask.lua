--*********************************************************
--* ExtraInfoMask (一百一十六 "全家"): 屏蔽我方作弊位被原版 UI 捎带上报。
--*
--* 背景 (逆向 + 游戏源码双侧取证): 原版三处 UI — ISVehicleMechanics.onCheatToggle
--* (:624) / ISWidgetBuildControl (:197) / ISAdminPowerUI (:403) — 会调用 Lua 全局
--* sendPlayerExtraInfo, 该包把**全部**作弊位打包上报; 服务端
--* ExtraInfoPacket.processServer 逐位查能力位 (Role.hasCapability), 无能力位且位为真
--* → AntiCheat.Capability.act → 记录 userlog/控制台, 计数 1/1 满即按 antiCheatPermission
--* 踢/禁。我方 6 个本地作弊位 (僵尸不攻击/隐身/穿墙/无敌/秒动作/无限体力) 会随包暴露
--* (用户实测: 僵尸不攻击被自建服务器记录)。
--*
--* 做法: 包一层 Lua 全局 sendPlayerExtraInfo — 发送前调 Java cheatMaskSuspend 暂摘我方
--* 6 位 → 调原版 → cheatMaskRestore 按快照精确回写 (只写动过的位)。摘除窗口仅一次同步
--* 调用 (无帧跨越), 游戏内零影响; 原版抛错走 pcall 兜底, 无论如何都回写。
--* Java 侧两方法为 @LuaMethod(global=true) 走 exposer 循环 (PrivateGlobals 捕获窗内),
--* 旧版本 (无这两个全局) 时整模块自动跳过 — 防御式兼容。
--*********************************************************

local orig = sendPlayerExtraInfo;
if type(orig) == "function"
        and type(cheatMaskSuspend) == "function"
        and type(cheatMaskRestore) == "function" then
    sendPlayerExtraInfo = function(player)
        local saved = cheatMaskSuspend();
        local ok, err = pcall(orig, player);
        cheatMaskRestore(saved);
        if not ok then
            print("[ExtraInfoMask] original sendPlayerExtraInfo failed: " .. tostring(err));
        end
    end
end
