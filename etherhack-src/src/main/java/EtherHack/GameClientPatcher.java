/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.tree.AbstractInsnNode
 *  org.objectweb.asm.tree.AnnotationNode
 *  org.objectweb.asm.tree.FieldInsnNode
 *  org.objectweb.asm.tree.InsnList
 *  org.objectweb.asm.tree.InsnNode
 *  org.objectweb.asm.tree.MethodNode
 *  org.objectweb.asm.tree.VarInsnNode
 */
package EtherHack;

import EtherHack.utils.Logger;
import EtherHack.utils.Patch;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GameClientPatcher {
    public static void patch() {
        String[][] methodsToPatch;
        Logger.printLog("Patching GameClient methods...");
        for (String[] methodInfo : methodsToPatch = new String[][]{{"gameLoadingDealWithNetData", "false"}, {"mainLoopDealWithNetData", "false"}, {"mainLoopHandlePacketInternal", "false"}, {"delayPacket", "false"}, {"receivePlayerConnect", "true"}, {"checkAddedRemovedItems", "false"}, {"writeItemStats", "false"}, {"loadResetID", "false"}, {"saveResetID", "false"}, {"getPlayerFromUsername", "false"}, {"initStartupConfig", "false"}, {"disconnect", "false"}, {"loadConfig", "false"}, {"receivePacketCounts", "true"}}) {
            GameClientPatcher.patchMethod(methodInfo[0], Boolean.parseBoolean(methodInfo[1]));
        }
    }

    private static void patchMethod(String methodName, boolean isStatic) {
        try {
            Consumer<MethodNode> injector = method -> {
                Logger.printLog("Modifying access for method: " + methodName);
                int newAccess = method.access;
                newAccess &= 0xFFFFFFFD;
                newAccess &= 0xFFFFFFFB;
                newAccess |= 1;
                if ((method.access & 8) != 0) {
                    newAccess |= 8;
                }
                method.access = newAccess;
            };
            Patch.injectIntoClass("zombie/network/GameClient", methodName, isStatic, injector);
            Logger.printLog("Successfully patched method: " + methodName);
        }
        catch (Exception e) {
            Logger.error("Failed to patch method " + methodName, e);
        }
    }

    public static void applyPatches() {
        try {
            Logger.printLog("Patching GameClient methods...");
            Patch.injectIntoClass("zombie/network/GameClient", "incomingNetData", false, methodNode -> {
                methodNode.access &= 0xFFFFFFFD;
                methodNode.access &= 0xFFFFFFFB;
                methodNode.access |= 1;
                if (methodNode.visibleAnnotations == null) {
                    methodNode.visibleAnnotations = new ArrayList();
                }
                methodNode.visibleAnnotations.add(new AnnotationNode("LEtherHack/annotations/Injected;"));
            });
            GameClientPatcher.injectGetterMethod("zombie/network/GameClient", "getIncomingNetData", "()Ljava/util/concurrent/ConcurrentLinkedQueue;", methodNode -> {
                InsnList instructions = new InsnList();
                instructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
                instructions.add((AbstractInsnNode)new FieldInsnNode(180, "zombie/network/GameClient", "incomingNetData", "Ljava/util/concurrent/ConcurrentLinkedQueue;"));
                instructions.add((AbstractInsnNode)new InsnNode(176));
                methodNode.instructions = instructions;
            });
            GameClientPatcher.patchMethod("incomingNetData", false);
        }
        catch (Exception e) {
            Logger.printLog("Failed to patch GameClient: " + e.getMessage());
        }
    }

    private static void injectGetterMethod(String className, String methodName, String descriptor, Consumer<MethodNode> injector) {
        try {
            Patch.injectIntoClass(className, methodName, false, methodNode -> {
                methodNode.access = 1;
                methodNode.desc = descriptor;
                injector.accept((MethodNode)methodNode);
            });
        }
        catch (Exception e) {
            Logger.printLog("Failed to inject getter method: " + e.getMessage());
        }
    }
}
