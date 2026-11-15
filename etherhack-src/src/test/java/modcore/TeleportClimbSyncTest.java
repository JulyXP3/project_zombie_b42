package modcore;

import modcore.core.LuaMethods;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.characters.IsoPlayer;
import zombie.core.random.RandAbstract;
import zombie.core.random.RandStandard;
import zombie.network.GameClient;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 穿墙档 (地图直线传送) 反作弊同步的离线契约测试。覆盖三件决定成败的事实:
 * ① 服务端豁免分支匹配的状态名与 Enter 处理契约;
 * ② scratched 参数对缺失键返回 null (服务端 .booleanValue() 会 NPE) → 组包前必须预置;
 * ③ 同步在非客户端环境静默门控, 不触达玩家内部状态。
 * 完整发包链路 (setData→sendToServer) 与服务端实际放行属游戏内验收。
 */
public class TeleportClimbSyncTest {
    private Random previousRandom;
    private boolean previousClient;

    @BeforeEach
    void setUp() throws Exception {
        // GameClient 类初始化会走到 ServerOptions → Rand, 必须先给随机源 (HeadshotDamageTest 同款)
        previousRandom = (Random) randomHandle().get(RandStandard.INSTANCE);
        randomHandle().set(RandStandard.INSTANCE, new Random(1L));
        previousClient = GameClient.client;
        GameClient.client = false;
    }

    @AfterEach
    void tearDown() throws Exception {
        GameClient.client = previousClient;
        randomHandle().set(RandStandard.INSTANCE, previousRandom);
    }

    @Test
    void climbThroughWindowStateMatchesTheServerExemptionContract() {
        ClimbThroughWindowState state = ClimbThroughWindowState.instance();
        assertEquals("ClimbThroughWindowState", state.getName(),
                "AntiCheatNoClip.validate:67 以该状态名命中豁免分支 (State.getName=类简单名)");
        assertTrue(state.isProcessedOnEnter(),
                "服务端 Enter 分支会执行 processOnEnter (直读 scratched 参数)");
        assertFalse(state.isProcessedOnExit(),
                "Exit 只做队列清理, 无服务端副作用 (isProcessedOnExit 默认 false)");
    }

    @Test
    void scratchedParamIsAbsentFromFreshParamsAndMustBePreSet() {
        Map<Object, Object> delegate = new HashMap<>();
        assertNull(ClimbThroughWindowState.SCRATCHED.fromDelegate(delegate),
                "State.Param.fromDelegate 对缺失键返回 null —— 服务端直接 .booleanValue() 会 NPE"
                        + " (→ AntiCheatPacketException), 组包前必须 player.set(SCRATCHED, false) 预置");
        delegate.put("scratched", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ClimbThroughWindowState.SCRATCHED.fromDelegate(delegate),
                "预置 false 后服务端 processOnEnter 安全走无划伤分支");
    }

    @Test
    void syncGatesOnNullPlayerAndDisabledClient() throws Exception {
        assertFalse(LuaMethods.teleportClimbState(null, true),
                "空玩家直接门控, 不触碰 GameClient");
        IsoPlayer player = allocate(IsoPlayer.class);
        assertFalse(LuaMethods.teleportClimbState(player, true),
                "非客户端环境门控返回 false, 不触达玩家内部状态");
        assertFalse(LuaMethods.teleportClimbState(player, false),
                "Exit 同步同样门控");
    }

    private static VarHandle randomHandle() throws Exception {
        return MethodHandles.privateLookupIn(RandAbstract.class, MethodHandles.lookup())
                .findVarHandle(RandAbstract.class, "rand", Random.class);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Unsafe unsafe = (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
                .findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class).get();
        return type.cast(unsafe.allocateInstance(type));
    }
}
