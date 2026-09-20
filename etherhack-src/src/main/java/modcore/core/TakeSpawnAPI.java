/*
 * 红队 POC: 计时动作真实生成 (ISTakeBricks) — 一百三十一 实施。
 *
 * 来源 = PienZ 真实源码 (temp\PienZ Source):
 *   · item_spawner.cpp / ItemSpawnTask.java —— 他们的"刷物品"**真路线**;
 *   · timed_action_accelerator.cpp —— 加速件 (Hand_L additionalPain 置 NaN + 上行 PlayerDamage)。
 *
 * 为什么换掉"直投"(InvMngGetItem): 那条链只让**目标客户端本地** addItem, 服务端全程不建物 →
 * 幽灵物品 (重登即失/他人不可见)。PienZ 自己也只把它当 Crash 载体 (UI 功能名 "Crash"),
 * 并不是物品生成路线 —— 用户实测"直投是幽灵物品"即此。
 *
 * 真路线原理 (原版动作链, 服务端校验并落地):
 *   ① 用**原版 Lua 计时动作** ISTakeBricks 携带任意物品类型与数量:
 *      `ISTakeBricks.new(character, pallet, square, sprite, item, amount)`
 *      (原版 Lua: media/lua/shared/TimedActions/ISTakeBricks.lua);
 *   ② 动作以 NetTimedAction 上行 → 服务端 `NetTimedActionPacket.processServer` 校验后 Accept;
 *   ③ 动作完成时原版 `ISTakeBricks:complete()` 执行
 *      `getInventory():AddItems(item, amount)` + **`sendAddItemsToContainer(...)`** ——
 *      这就是"真物品"的来源: 物品随正常的容器上行同步报给服务端 (可持久化、他人可见)。
 *      (对比: 直投的 InvMngGetItem 接收端只做本地 addItem, 没有这一步。)
 *
 * 加速 (可选, 移植 timed_action_accelerator):
 *   原版 `getDuration()` = `10 * amount`, 数量一多就是几分钟。PienZ 的做法是把 Hand_L 的
 *   `additionalPain` 置 NaN 并 `sendPlayerDamage` 上行 → **服务端副本**的该字段也成 NaN →
 *   服务端侧动作时长计算塌成 NaN → 动作在服务端眼里瞬间完成。本类为 **Arm/Maintain/Restore**
 *   三段式照搬 + **硬超时 (3s) + 收尾必还原** (工程纪律 4: 改前值记录, 只写回自己改过的键)。
 *   客户端侧"快"由 Lua 模块的 `ISTakeBricks.getDuration` 覆盖完成 (零作弊位, 见 EtherTakeSpawn.lua),
 *   两者合起来 = PienZ 的 accelerated 档 (他们是 InstantActionManager + 投毒, 我们自己那份等价物
 *   是 Lua 覆盖)。
 *
 * 仅限用户自己的服务器 / 自建测试环境。
 */
package modcore.core;

import modcore.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.network.GameClient;
import zombie.network.packets.NetTimedActionPacket;

public final class TakeSpawnAPI {

    /** 单次数量上限 (原版时长 10*amount, 上限由 UI 与这里双重把关)。 */
    private static final int MAX_COUNT = 100;
    /** 加速件: 投毒续期间隔与硬超时 (超时必还原, 防残留)。 */
    private static final long POISON_REFRESH_MS = 250L;
    private static final long ACCEL_MAX_MS = 3000L;

    private static IsoPlayer armedPlayer;
    private static BodyPart armedHand;
    private static float originalPain;
    private static long armedUntilMs;
    private static long lastPoisonMs;
    private static boolean armed;

    private TakeSpawnAPI() {
    }

    /**
     * 生成 count 份 itemType: 走原版 ISTakeBricks 计时动作 (服务端校验 → 完成时
     * AddItems + sendAddItemsToContainer = 真物品)。accelerate=true 时附加加速件。
     * 失败抛 RuntimeException (真实原因), 由 Lua 侧 pcall 展示。
     */
    @LuaMethod(name = "takeSpawnStart", global = true)
    public static boolean takeSpawnStart(String itemType, int count, boolean accelerate) {
        IsoPlayer p = IsoPlayer.getInstance();
        if (p == null) {
            throw new RuntimeException("no player");
        }
        if (itemType == null || itemType.isEmpty()) {
            throw new RuntimeException("item type is empty");
        }
        if (count < 1 || count > MAX_COUNT) {
            throw new RuntimeException("count out of range (1-" + MAX_COUNT + "): " + count);
        }
        if (p.getCurrentSquare() == null) {
            throw new RuntimeException("no current square");
        }
        // 只接受注册表里的合法定义 (物品类型不存在时原版 complete() 会静默少给)
        if (zombie.inventory.InventoryItemFactory.CreateItem(itemType) == null) {
            throw new RuntimeException("unknown item type: " + itemType);
        }
        if (accelerate && !arm(p)) {
            throw new RuntimeException("accelerator arm failed");
        }
        try {
            // 与 PienZ ItemSpawnTask 同形: values = (character, pallet, square, sprite, item, amount)
            // (pallet 传玩家自己 —— 原版 isValid 只要求 isExistInTheWorld)
            NetTimedActionPacket.createNewAndSend("ISTakeBricks", p,
                    p, p, p.getCurrentSquare(), null, itemType, (double) count);
        } catch (Throwable t) {
            if (accelerate) {
                restore();
            }
            throw new RuntimeException("action create/send failed: " + t);
        }
        Logger.printLog("[TakeSpawn] ISTakeBricks sent: " + count + "x " + itemType
                + (accelerate ? " (accelerated)" : ""));
        return true;
    }

    /** Lua 侧可提前收尾 (面板关闭/取消)。 */
    @LuaMethod(name = "takeSpawnCancel", global = true)
    public static void takeSpawnCancel() {
        restore();
    }

    /** 加速件状态 (面板显示用)。 */
    @LuaMethod(name = "takeSpawnIsArmed", global = true)
    public static boolean takeSpawnIsArmed() {
        return armed;
    }

    // ===== 加速件: Arm / Maintain / Restore (timed_action_accelerator 三段式) =====

    private static boolean arm(IsoPlayer p) {
        try {
            BodyDamage bd = p.getBodyDamage();
            if (bd == null) {
                return false;
            }
            BodyPart hand = bd.getBodyPart(BodyPartType.Hand_L);
            if (hand == null) {
                return false;
            }
            armedPlayer = p;
            armedHand = hand;
            float current = hand.getAdditionalPain();
            originalPain = Float.isFinite(current) ? current : 0.0f;
            armed = true;
            armedUntilMs = System.currentTimeMillis() + ACCEL_MAX_MS;
            lastPoisonMs = 0L;
            poison();
            Logger.printLog("[TakeSpawn] accelerator armed (Hand_L pain NaN + PlayerDamage)");
            return true;
        } catch (Throwable t) {
            Logger.printLog("[TakeSpawn] accelerator arm error: " + t);
            restore();
            return false;
        }
    }

    private static void poison() {
        try {
            armedHand.setAdditionalPain(Float.NaN);
            GameClient.sendPlayerDamage(armedPlayer);
        } catch (Throwable t) {
            Logger.printLog("[TakeSpawn] accelerator poison error: " + t);
        }
    }

    /**
     * 每帧调用 (CoreAPI 的 OnRenderTick 块): 加速件续期与收尾。未武装时立即返回 (零开销)。
     */
    public static void tick() {
        if (!armed) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= armedUntilMs || armedPlayer == null || !armedPlayer.isExistInTheWorld()) {
            restore();
            return;
        }
        if (now - lastPoisonMs >= POISON_REFRESH_MS) {
            lastPoisonMs = now;
            poison();
        }
    }

    /** 还原: 只写回自己改过的值 (NaN 才写), 并上行一次让服务端副本同步。 */
    private static void restore() {
        if (!armed) {
            return;
        }
        try {
            if (armedHand != null && Float.isNaN(armedHand.getAdditionalPain())) {
                armedHand.setAdditionalPain(originalPain);
            }
            if (armedPlayer != null) {
                GameClient.sendPlayerDamage(armedPlayer);
            }
            Logger.printLog("[TakeSpawn] accelerator restored (pain=" + originalPain + ")");
        } catch (Throwable t) {
            Logger.printLog("[TakeSpawn] accelerator restore error: " + t);
        } finally {
            armed = false;
            armedPlayer = null;
            armedHand = null;
            originalPain = 0.0f;
            armedUntilMs = 0L;
            lastPoisonMs = 0L;
        }
    }
}
