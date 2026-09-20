/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.AnnotationVisitor
 *  org.objectweb.asm.ClassReader
 *  org.objectweb.asm.ClassVisitor
 *  org.objectweb.asm.ClassWriter
 *  org.objectweb.asm.MethodVisitor
 *  org.objectweb.asm.tree.AnnotationNode
 *  org.objectweb.asm.tree.ClassNode
 *  org.objectweb.asm.tree.MethodNode
 */
package modcore.utils;

import modcore.utils.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class Patch {
    private static final Map<String, ClassNode> classNodeMap = new HashMap<String, ClassNode>();

    /**
     * C (2026-09-14 八十二, 安装事故教训): **失败可见**。旧版"某个类没写进磁盘"只在日志里
     * 留一行, 安装器照样打印 "The cheat installation is complete" —— 实测 GameClient 整类
     * 丢失 (该类的全部补丁一起失效) 而用户看不到任何失败提示。
     * ① failedClasses = 结构校验/写盘失败 (硬): 该类保持原版, 相关功能整块缺失;
     * ② missingTargets  = 目标方法在本版游戏里不存在或形状不符 (软): 版本漂移, 相关补丁空转。
     */
    private static final java.util.Set<String> failedClasses = new java.util.LinkedHashSet<String>();
    private static final java.util.Set<String> missingTargets = new java.util.LinkedHashSet<String>();

    public static java.util.Set<String> getFailedClasses() {
        return failedClasses;
    }

    public static java.util.Set<String> getMissingTargets() {
        return missingTargets;
    }

    /** 清空上一轮安装留下的失败记录 (同一 JVM 内重复 patchGame / 自检程序用) */
    public static void resetPatchReport() {
        failedClasses.clear();
        missingTargets.clear();
    }

    /**
     * H1 (2026-09-13, analysis/DLL分析/H-工程纪律-设计方案(已并规约).md):
     * 注入前的**形状校验** —— 检查目标方法既有指令形状, 不匹配就抛错, 不盲注。
     */
    public interface ShapeGuard {
        void check(MethodNode method);
    }
    private static String projectZomboidJarPath = null;

    public static void setProjectZomboidJarPath(String jarPath) {
        projectZomboidJarPath = jarPath;
        Logger.printLog("Set ProjectZomboid.jar path: " + jarPath);
    }

    /**
     * A (2026-09-14 八十二, 安装事故根因修复): **整方法替换 (full-replacement) 的安全清体**。
     *
     * 类是用 ClassReader flags=8 (EXPAND_FRAMES) 读入的 —— 指令链**之外**的调试/结构表
     * (localVariables 局部变量表、局部变量注解) 持有**旧 LabelNode 引用**。只调
     * instructions.clear() 时这些表仍然存在, 而它们引用的 label 已不在指令链里, 写出时
     * 拿不到字节码偏移 → 落盘前的往返校验读回这段调试表时 label 索引越界:
     *   ArrayIndexOutOfBoundsException: Index 51 out of bounds for length 6
     *   (原方法体 label 编到 51, 替换体只有 3 条指令约 6 个 label —— 数字完全吻合)
     * 后果不是"少一个补丁", 而是**整个类被判为坏**而丢弃 (GameClient 实例: 三个补丁一起
     * 失效, 安装器却仍打印成功)。
     *
     * 约定: 任何"清空方法体 → 重新添加指令"的补丁必须先调本方法, 再添加新指令。
     * 副作用 (L4 磁盘痕迹角度是好事): 被替换方法的局部变量名/行号表不再写出。
     */
    public static void clearBody(MethodNode method) {
        method.instructions.clear();
        if (method.tryCatchBlocks != null) {
            method.tryCatchBlocks.clear();
        }
        method.localVariables = null;                   // LocalVariableTable (持有旧 label)
        method.visibleLocalVariableAnnotations = null;  // 局部变量注解 (持有旧 label)
        method.invisibleLocalVariableAnnotations = null;
    }

    public static void injectIntoClass(String className, String methodName, boolean isStatic, Consumer<MethodNode> injector) {
        injectIntoClass(className, methodName, isStatic, null, injector);
    }

    /**
     * H1: 带形状校验的注入。语义与旧重载一致, 另加三条纪律:
     * ① guard 非空时先校验既有指令形状, 不匹配抛 IllegalStateException(不盲注);
     * ② **幂等** —— 目标方法已带 {@code @Injected} 标记时跳过(记日志), 避免同一会话重复注入同一钩子;
     * ③ 注入后做结构校验(见 {@link #verifyClassNode})。
     * 目标方法不存在时**记错误日志并跳过**(不抛): 游戏版本变更时保证其余补丁照常生效。
     */
    public static void injectIntoClass(String className, String methodName, boolean isStatic,
                                       ShapeGuard guard, Consumer<MethodNode> injector) {
        injectIntoClass(className, methodName, isStatic, guard, injector, null, null);
    }

    /**
     * 一百三十三 加固 (借鉴 PienZ 的 ReadBack 校验 + 半装检测; 见
     * analysis/DLL分析/I-PienZ源码可借鉴项-清单与排期(部分已实施).md 二.4/二.5):
     *
     * ④ **ReadBack (注入后回读)**: 传 {@code hookOwner/hookName} 时, 注入完必须能在方法里
     *    找到那条 INVOKESTATIC, 找不到即抛错 —— 光靠"注入器跑过了"不足以证明钩子真的进了
     *    字节码 (旧的失败模式: 注入器内部早退/条件写岔, 日志照样打印成功)。
     * ⑤ **半装检测**: 方法已带 @Injected 标记但**钩子调用不在** = 上一轮只装了一半
     *    (旧实现在这种情况下静默 skip, 功能静默缺失)。此时抛 IllegalStateException,
     *    让安装器把该类计入硬失败, 而不是装作成功。
     *
     * {@code hookOwner/hookName} 为 null 时行为与旧版一致 (向后兼容既有 30+ 处调用)。
     */
    public static void injectIntoClass(String className, String methodName, boolean isStatic,
                                       ShapeGuard guard, Consumer<MethodNode> injector,
                                       String hookOwner, String hookName) {
        Logger.print("Injection into a game file '" + className + "' in method: '" + methodName + "'");
        ClassNode classNode = classNodeMap.computeIfAbsent(className, Patch::loadClassNode);
        if (classNode == null) {
            throw new RuntimeException("Failed to load class " + className);
        }
        boolean matched = false;
        for (MethodNode methodNode : classNode.methods) {
            if (!methodNode.name.equals(methodName) || Modifier.isStatic(methodNode.access) != isStatic) continue;
            matched = true;
            // 幂等 (H1): 同一会话对同一方法重复注入时跳过, 避免钩子叠加
            if (Patch.hasInjectedAnnotation(methodNode)) {
                // 半装检测 (一百三十三): 标记在 = 上一轮声称装过, 但钩子调用不在 →
                // 那类补丁是坏的, 必须报硬失败而不是静默跳过 (旧实现就是静默跳过)
                if (hookOwner != null && !Patch.hasHookCall(methodNode, hookOwner, hookName)) {
                    Patch.failedClasses.add(className);
                    throw new IllegalStateException("Half-installed patch detected: " + className + "#"
                            + methodName + " is marked @Injected but the hook call "
                            + hookOwner + "." + hookName + " is missing (stale/partial transformation)");
                }
                Logger.printLog("Skip injection (already marked @Injected): " + className + "#" + methodName);
                continue;
            }
            if (guard != null) {
                try {
                    guard.check(methodNode);
                }
                catch (RuntimeException e) {
                    Patch.missingTargets.add(className + "#" + methodName + " (shape guard)");
                    throw new IllegalStateException("Patch shape guard failed for " + className + "#" + methodName
                            + ": " + e.getMessage(), e);
                }
            }
            injector.accept(methodNode);
            // ReadBack (一百三十三): 注入器跑过 ≠ 钩子进了字节码, 回读确认
            if (hookOwner != null && !Patch.hasHookCall(methodNode, hookOwner, hookName)) {
                Patch.failedClasses.add(className);
                throw new IllegalStateException("Post-injection read-back failed: " + className + "#" + methodName
                        + " does not contain the expected hook call " + hookOwner + "." + hookName);
            }
            Patch.addInjectAnnotation(classNode, methodName);
        }
        if (!matched) {
            // B 修复 (2026-09-14 八十二): 旧代码把 null 当 Throwable 传给
            // Logger.error(String, Throwable) → 方法内部 throwable.getMessage() 先 NPE,
            // 真正的 "Patch target not found" 消息一个字都打不出来 (实测:
            // GameServer#kickPlayer 在 B42 已改名 kick, 这条诊断被 NPE 掩盖了整整一轮,
            // 表现成一句无意义的 "Cannot invoke Throwable.getMessage()")。
            // 改为单参调用, 并汇总进 missingTargets 供安装器逐条汇报。
            Logger.error("Patch target not found (game version drift?): " + className + "#" + methodName
                    + " (static=" + isStatic + ")");
            Patch.missingTargets.add(className + "#" + methodName);
        }
        try {
            Patch.verifyClassNode(className, classNode);
        }
        catch (RuntimeException e) {
            // A/C (2026-09-14 八十二): 结构校验失败 = 该类已被改坏。从待写盘表里剔除
            // (宁可该类保持原版, 也不把带病字节码交给游戏 JVM —— 那会是 VerifyError 崩溃),
            // 记入硬失败清单后继续上抛 (调用方补上下文日志)。剔除的另一重意义: 同一类的
            // 后续补丁会重新从 jar 载入干净副本, 坏改动不再污染同类其它补丁。
            classNodeMap.remove(className);
            Patch.failedClasses.add(className);
            throw e;
        }
        classNodeMap.put(className, classNode);
    }

    public static void modifyClass(String className, Consumer<ClassNode> modifier) {
        ClassNode classNode = classNodeMap.computeIfAbsent(className, Patch::loadClassNode);
        if (classNode == null) {
            throw new RuntimeException("Failed to load class " + className);
        }
        modifier.accept(classNode);
        try {
            Patch.verifyClassNode(className, classNode);
        }
        catch (RuntimeException e) {
            // 与 injectIntoClass 同规 (2026-09-14 八十二): 坏改动不进待写盘表
            classNodeMap.remove(className);
            Patch.failedClasses.add(className);
            throw e;
        }
        classNodeMap.put(className, classNode);
    }

    /**
     * H1 (2026-09-13): 补丁后的**结构校验** —— 序列化往返 + 基本不变量。
     * 失败抛 IllegalStateException: 宁可让启动期明确报错, 也不要把带病字节码交给游戏 JVM (那会是 VerifyError 崩溃)。
     * 说明: 本项目未引入 asm-util(无 CheckClassAdapter), 这里做的是结构级校验 + 往返可写可读;
     * 完整的数据流/帧校验由游戏 JVM 加载时的 verifier 兜底。
     */
    public static void verifyClassNode(String className, ClassNode classNode) {
        try {
            SafeClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            byte[] bytes = writer.toByteArray();
            new ClassReader(bytes).accept(new ClassNode(), 0);
        }
        catch (RuntimeException | Error e) {
            throw new IllegalStateException("Patch verify failed (round-trip) for " + className + ": " + e, e);
        }
        for (MethodNode method : classNode.methods) {
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            String where = className + "#" + method.name + method.desc;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof JumpInsnNode && ((JumpInsnNode) insn).label == null) {
                    throw new IllegalStateException("Patch verify failed: null jump label at " + where);
                }
                if (insn instanceof LabelNode && ((LabelNode) insn).getLabel() == null) {
                    throw new IllegalStateException("Patch verify failed: unbound label at " + where);
                }
            }
            if (method.tryCatchBlocks != null) {
                for (TryCatchBlockNode handler : method.tryCatchBlocks) {
                    if (handler.start == null || handler.end == null || handler.handler == null) {
                        throw new IllegalStateException("Patch verify failed: incomplete try/catch block at " + where);
                    }
                }
            }
        }
    }

    private static ClassNode loadClassNode(String key) {
        ClassNode node = new ClassNode();
        try {
            if (projectZomboidJarPath != null && Files.exists(Paths.get(projectZomboidJarPath, new String[0]), new LinkOption[0])) {
                try (JarFile jarFile = new JarFile(projectZomboidJarPath)) {
                    ZipEntry entry = jarFile.getEntry(key + ".class");
                    if (entry != null) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            ClassReader reader = new ClassReader(is);
                            reader.accept((ClassVisitor)node, 8);
                            Logger.printLog("Loaded class from ProjectZomboid.jar: " + key);
                            return node;
                        }
                    }
                }
            }
            ClassReader reader = new ClassReader(key);
            reader.accept((ClassVisitor)node, 8);
            Logger.printLog("Loaded class from file system: " + key);
            return node;
        }
        catch (IOException e) {
            Logger.error("Failed to read class: " + key, e);
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isInjectedAnnotationPresent(String file, String baseDir) {
        boolean result;
        Path filePath = Paths.get(baseDir, file);
        InputStream inputStream = null;
        ZipFile jarFile = null;
        try {
            ZipEntry entry;
            if (Files.exists(filePath, new LinkOption[0])) {
                inputStream = new FileInputStream(filePath.toString());
            } else if (projectZomboidJarPath != null && Files.exists(Paths.get(projectZomboidJarPath, new String[0]), new LinkOption[0]) && (entry = ((JarFile)(jarFile = new JarFile(projectZomboidJarPath))).getEntry(file)) != null) {
                inputStream = ((JarFile)jarFile).getInputStream(entry);
            }
            if (inputStream == null) {
                Logger.printLog("Could not find class file: " + file);
                return false;
            }
            ClassReader reader = new ClassReader(inputStream);
            final boolean[] found = new boolean[]{false};
            reader.accept(new ClassVisitor(589824){

                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(589824, mv){

                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            if (descriptor.equals("Lmodcore/annotations/Injected;")) {
                                found[0] = true;
                            }
                            return super.visitAnnotation(descriptor, visible);
                        }
                    };
                }
            }, 0);
            result = found[0];
        }
        catch (IOException e) {
            Logger.error("Error checking for injected annotations", e);
            return false;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException iOException) {}
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                }
                catch (IOException iOException) {}
            }
        }
        return result;
    }

    private static void addInjectAnnotation(ClassNode classNode, String methodName) {
        for (MethodNode method : classNode.methods) {
            boolean hasAnnotation;
            if (!method.name.equals(methodName)) continue;
            if (method.visibleAnnotations == null) {
                method.visibleAnnotations = new LinkedList();
            }
            if (!(hasAnnotation = method.visibleAnnotations.stream().anyMatch(anno -> anno.desc.equals("Lmodcore/annotations/Injected;")))) {
                method.visibleAnnotations.add(new AnnotationNode("Lmodcore/annotations/Injected;"));
            }
            return;
        }
    }

    private static boolean hasInjectedAnnotation(MethodNode method) {
        if (method.visibleAnnotations == null) {
            return false;
        }
        return method.visibleAnnotations.stream().anyMatch(anno -> anno.desc.equals("Lmodcore/annotations/Injected;"));
    }

    /**
     * ReadBack 辅助 (一百三十三): 方法字节码里是否存在 owner.name 的 INVOKESTATIC 调用。
     * 用于"注入后回读"与"半装检测"—— 光看注入器有没有跑过不算数。
     */
    private static boolean hasHookCall(MethodNode method, String owner, String name) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof org.objectweb.asm.tree.MethodInsnNode)) continue;
            org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) insn;
            if (call.getOpcode() == 184 && call.owner.equals(owner) && call.name.equals(name)) {
                return true;
            }
        }
        return false;
    }


    public static void saveModifiedClasses() {
        // C (2026-09-14 八十二): 遍历副本 —— 失败项会在循环里从 classNodeMap 剔除
        for (Map.Entry<String, ClassNode> entry : new java.util.ArrayList<Map.Entry<String, ClassNode>>(classNodeMap.entrySet())) {
            String className = entry.getKey();
            ClassNode classNode = entry.getValue();
            try {
                Patch.verifyClassNode(className, classNode);   // H1: 落盘前再校验一遍
                SafeClassWriter writer = new SafeClassWriter(2);
                classNode.accept((ClassVisitor)writer);
                byte[] bytes = writer.toByteArray();
                Path classFilePath = Paths.get(className + ".class", new String[0]);
                if (classFilePath.getParent() != null) {
                    Files.createDirectories(classFilePath.getParent(), new FileAttribute[0]);
                }
                try (FileOutputStream fos = new FileOutputStream(classFilePath.toFile());){
                    fos.write(bytes);
                }
                Logger.printLog("Successfully saved modified class: " + className);
            }
            catch (IOException e) {
                Logger.error("Error saving modified class '" + className + "'", e);
                Patch.failedClasses.add(className);
                classNodeMap.remove(className);
            }
            catch (Exception e) {
                Logger.error("Unexpected error processing class '" + className + "'", e);
                e.printStackTrace();
                Patch.failedClasses.add(className);
                classNodeMap.remove(className);
            }
        }
        if (!Patch.failedClasses.isEmpty()) {
            Logger.error("Patch failures: " + Patch.failedClasses.size()
                    + " class(es) were NOT written to disk: " + Patch.failedClasses);
        }
    }

    private static class SafeClassWriter
    extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return super.getCommonSuperClass(type1, type2);
            }
            catch (RuntimeException e) {
                Logger.printLog("Could not resolve common superclass for " + type1 + " and " + type2 + ", using Object");
                return "java/lang/Object";
            }
        }
    }
}
