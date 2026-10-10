/*
 * ConnectFlagsPatch (一百九十五) — 「连接期授权」的 ConnectPacket 旗标注入。
 *
 * 机制: 多人下 vanilla 把管理员作弊旗标 (extraInfoFlags) 当作客户端握手自报字段接受 ——
 * 服务端 receivePlayerConnect (GameServer.java:2642-2644) 以 setExtraInfoFlags(flags,
 * isForced=true) 绕过 Role 能力位门 (IsoGameCharacter.java:11099 forced 路径直达
 * setGodModCheat)。本补丁在客户端 ConnectPacket.write 的 extraInfoFlags 读取点后插
 * ConnectFlagsRuntime.applyFlags, 把 bit0 (GodMod) / bit2 (Invisible) 置 1 ——
 * 通道取证与消费侧分析见 analysis/连接期作弊旗标-多人上帝与隐身(已实施).md。
 *
 * 注入形态 (中段插入, 非 MaxWeightPatch 的头插): write() 内恰有一处
 * `getfield extraInfoFlags:B` (javap 实锤: aload_1/aload_0/getfield/invokevirtual putByte ×3),
 * 在该节点**之后**插 INVOKESTATIC —— putByte 消费的即改写后的字节。
 *
 * ShapeGuard (一百九十三 纪律): 剔除 Label/LineNumber/Frame 元数据后断言
 * "恰 1 处 GETFIELD extraInfoFlags:B (owner ConnectPacket)" + 方法 desc; 不对原始列表断长度/位置。
 * 离线终审: temp/ConnectPacketGuardCheck (对真实 jar 跑 ASM, 仿 MaxWeightGuardCheck 模板)。
 * @Injected 幂等 + ReadBack 回读校验。
 *
 * Only for the user's own server / self-built test environment.
 */
package modcore.core;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class ConnectFlagsPatch {

    private static final String TARGET_CLASS = "zombie/network/packets/connection/ConnectPacket";
    private static final String TARGET_METHOD = "write";
    private static final String TARGET_DESC = "(Lzombie/core/network/ByteBufferWriter;)V";
    private static final String HOOK_OWNER = "modcore/core/ConnectFlagsRuntime";
    private static final String HOOK_NAME = "applyFlags";

    private ConnectFlagsPatch() {
    }

    public static void install() {
        Logger.print("Patching ConnectPacket.write with connect-flags override...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    // ShapeGuard: desc 匹配 + 元数据过滤后恰 1 处 extraInfoFlags GETFIELD
                    // (owner=ConnectPacket, desc=B)。多一处/零一处 = 版本漂移, 拒绝注入。
                    method -> {
                        if (!method.desc.equals(TARGET_DESC)) {
                            throw new IllegalStateException("unexpected ConnectPacket.write desc: " + method.desc);
                        }
                        List<AbstractInsnNode> real = new ArrayList<AbstractInsnNode>();
                        for (int i = 0; i < method.instructions.size(); i++) {
                            AbstractInsnNode n = method.instructions.get(i);
                            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) continue;
                            real.add(n);
                        }
                        int flagReads = 0;
                        AbstractInsnNode flagRead = null;
                        for (AbstractInsnNode n : real) {
                            if (n instanceof FieldInsnNode
                                    && n.getOpcode() == 180 // GETFIELD
                                    && "extraInfoFlags".equals(((FieldInsnNode) n).name)
                                    && "B".equals(((FieldInsnNode) n).desc)) {
                                ++flagReads;
                                flagRead = n;
                            }
                        }
                        if (flagReads != 1 || flagRead == null) {
                            throw new IllegalStateException("ConnectPacket.write shape changed "
                                    + "(expected exactly 1 extraInfoFlags read, got " + flagReads + ")");
                        }
                    },
                    method -> {
                        // 找到 extraInfoFlags GETFIELD 节点, 在其后插 INVOKESTATIC
                        for (int i = 0; i < method.instructions.size(); i++) {
                            AbstractInsnNode n = method.instructions.get(i);
                            if (n instanceof FieldInsnNode
                                    && n.getOpcode() == 180
                                    && "extraInfoFlags".equals(((FieldInsnNode) n).name)) {
                                InsnList hook = new InsnList();
                                hook.add(new MethodInsnNode(184, HOOK_OWNER, HOOK_NAME,
                                        "(B)B", false));
                                method.instructions.insert(n, hook);
                                Logger.print("  [OK] Injected connect-flags override after extraInfoFlags read");
                                return;
                            }
                        }
                        throw new IllegalStateException("extraInfoFlags read not found at injection time");
                    },
                    HOOK_OWNER, HOOK_NAME);
        } catch (Exception e) {
            Logger.print("Warning: connect-flags injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
