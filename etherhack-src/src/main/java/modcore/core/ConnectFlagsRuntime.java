/*
 * ConnectFlagsPatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 语义 (一百九十五, ⑩ 回退拆分恢复组合单开关 — 同时授予上帝+隐身; 通道取证见
 * analysis/连接期作弊旗标-多人上帝与隐身(已实施).md):
 * 「连接期授权」开启时把 ConnectPacket.write 上行的 extraInfoFlags 的
 * bit0 (GodMod) / bit2 (Invisible) 置 1 —— vanilla 位定义见 IsoPlayer.setExtraInfoFlags
 * (IsoPlayer.java:1303-1311)。服务端 receivePlayerConnect 以 setExtraInfoFlags(flags,
 * isForced=true) 绕过 Role 能力位门无条件接受并广播 —— 与管理员开挂不可区分。
 * 不碰 GhostMode(bit1)/NoClip(bit3)/AdminTag(bit4)/CanHearAll(bit5)。
 * 使用约束: ConnectPacket 是握手一次性上行包 —— 进服前开启 (配置落盘, 下次连接生效)。
 *
 * 一百九十八 关键补刀: ConnectPacket.set (ConnectPacket.java:43) 的 flags 直接取
 * **本地玩家实时作弊位** getExtraInfoFlags() —— 本地 god=true (如 SP 上帝开关被
 * CoreAPI stomp 顶真) 会以 bit0 泄漏进连接包, 服务端 forced 应用 → 关了连接期授权
 * 服务端照样上帝 (用户实测: 隐身清了/受伤秒痊愈的不对称残留)。故关闭时**主动清
 * 两位** (in & ~0x05) 而非原样透传: 这两位语义归本功能所有, 合法玩家本地根本
 * 置不了 true (单参 setter 被 Role 门禁), 清除无副作用; 重连即还服务端干净状态。
 */
package modcore.core;

public final class ConnectFlagsRuntime {

    /** extraInfoFlags 内本功能管的两位: GodMod | Invisible (mask = 0x05)。 */
    private static final int FLAG_MASK = 0x05;

    private ConnectFlagsRuntime() {
    }

    /** 返回改写后的 extraInfoFlags 字节; 开启 = 强制置两位, 关闭 = 强制清两位, 异常时原样返回。 */
    public static byte applyFlags(byte in) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isConnectFlags) {
                // 关闭 = 主动清两位: 杜绝本地实时位 (SP 开关/stomp 残留) 泄漏进连接包
                return (byte) (in & ~FLAG_MASK);
            }
            return (byte) ((in & ~FLAG_MASK) | FLAG_MASK);
        } catch (Throwable t) {
            return in;
        }
    }
}
