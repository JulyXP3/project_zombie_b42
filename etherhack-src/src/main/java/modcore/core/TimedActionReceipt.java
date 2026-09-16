package modcore.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Queue;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.ActionManager;
import zombie.core.NetTimedAction;
import zombie.core.Transaction;

/**
 * A2 (2026-09-14 八十九): **原版计时动作的精确回执桥**。
 *
 * 取证结论 (推翻"需要 TimeSync 预言机"的旧设计): **原版引擎本来就在等精确回执** ——
 * `zombie/characters/CharacterTimedActions/LuaTimedActionNew.java:88-103` 在 MP 下自己轮询
 * `ActionManager.isDone/isRejected`, 服务端 `NetTimedActionPacket:63-89` + `ActionManager:88-103`
 * 会回 Accept/Reject/Done。我方 8 个修车/刷物动作 (ISRepairLightbar / ISTakeBricks / ...) 都定义了
 * `complete` → 全是服务端权威动作 → **全部自带这条回执**, 我们只是没把它读出来。
 *
 * 因此本类只做一件事: 给定 Lua 动作表, 返回它在服务端事务里的**状态**。
 * 匹配键用 `NetTimedAction.action` (**public** 字段, NetTimedAction.java:36) 做同一性比较;
 * 状态读 `Action.state` (**protected**, Action.java:24) → 走 VarHandle + privateLookupIn
 * (AGENTS.md 工程纪律 §3: 优先 VarHandle, 不用 setAccessible 硬开)。
 *
 * 返回码: 0=进行中(Request/Accept) 1=已完成(Done) 2=被拒(Reject) 3=未知(不在队列/桥不可用)。
 * **"回执到了" ≠ "服务端已受理"**: 反作弊否决路径 (`PacketTypes.java:685-692`) 直接 return,
 * 服务端不会回任何纠正包; 这里的 Reject 是原版事务层给的, 可信。
 */
public final class TimedActionReceipt {

    private static VarHandle ACTIONS;
    private static VarHandle STATE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(ActionManager.class, MethodHandles.lookup());
            ACTIONS = lookup.findVarHandle(ActionManager.class, "actions", Queue.class);
        }
        catch (Throwable t) {
            ACTIONS = null;   // 退化: 一律返回 3 (未知), 不影响其它功能
        }
        try {
            // zombie.core.Action 是**包私有抽象类** (Action.java:17) → 不能写 Action.class (跨包不可访问),
            // 只能按名取 Class 再查字段 (VarHandle 按字段名查, 不需要编译期可见性)。
            Class<?> actionClass = Class.forName("zombie.core.Action");
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(actionClass, MethodHandles.lookup());
            STATE = lookup.findVarHandle(actionClass, "state", Transaction.TransactionState.class);
        }
        catch (Throwable t) {
            STATE = null;   // 退化: 一律返回 3 (未知), 修车状态机会退回原有的容差判定
        }
    }

    private TimedActionReceipt() {
    }

    /** @return 0=进行中 1=已完成 2=被拒 3=未知 */
    @LuaMethod(name = "timedActionState", global = true)
    public static int state(KahluaTable actionTable) {
        try {
            if (ACTIONS == null || STATE == null || actionTable == null) {
                return 3;
            }
            Object queueObject = ACTIONS.get();
            if (!(queueObject instanceof Queue)) {
                return 3;
            }
            for (Object entry : (Queue<?>) queueObject) {
                if (!(entry instanceof NetTimedAction)) {
                    continue;
                }
                NetTimedAction netAction = (NetTimedAction) entry;
                if (netAction.action != actionTable) {
                    continue;
                }
                Object stateObject = STATE.get(netAction);
                if (stateObject == Transaction.TransactionState.Done) {
                    return 1;
                }
                if (stateObject == Transaction.TransactionState.Reject) {
                    return 2;
                }
                return 0;
            }
        }
        catch (Throwable t) {
            return 3;
        }
        return 3;
    }
}
