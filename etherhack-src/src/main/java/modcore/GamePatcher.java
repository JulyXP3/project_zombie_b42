/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.tree.AbstractInsnNode
 *  org.objectweb.asm.tree.FieldInsnNode
 *  org.objectweb.asm.tree.FieldNode
 *  org.objectweb.asm.tree.InsnList
 *  org.objectweb.asm.tree.InsnNode
 *  org.objectweb.asm.tree.JumpInsnNode
 *  org.objectweb.asm.tree.LabelNode
 *  org.objectweb.asm.tree.LdcInsnNode
 *  org.objectweb.asm.tree.MethodInsnNode
 *  org.objectweb.asm.tree.TryCatchBlockNode
 *  org.objectweb.asm.tree.VarInsnNode
 */
package modcore;

import modcore.Main;
import modcore.utils.Info;
import modcore.utils.Logger;
import modcore.utils.Patch;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Comparator;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GamePatcher {
    private final String[] patchFiles = new String[]{"GameWindow.class", "inventory/ItemContainer.class", "Lua/LuaEventManager.class", "Lua/LuaManager.class", "characters/IsoGameCharacter.class", "network/GameClient.class", "CombatManager.class", "characters/Role.class", "vehicles/BaseVehicle.class", "characters/IsoZombie.class", "network/packets/character/CreatePlayerPacket.class", "iso/IsoGridSquare.class", "iso/IsoChunk.class", "core/opengl/RenderSettings$PlayerRenderSettings.class", "iso/LightingJNI$JNILighting.class", "network/ServerLOS$ServerLighting.class", "core/physics/CarController.class"};
    private final String gameClassFolder = "zombie";
    private final String whiteListPathEtherFiles = "modcore";

    public void extractmodcore() {
        try {
            String jarFileUri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
            Path currentDirectory = Paths.get(System.getProperty("user.dir"), new String[0]);
            // 旧版/独立测试版时代残留: drive 域已并回 modcore (2026-09-06), 安装时清理旧目录
            this.removeLegacyDriveDir(currentDirectory);
            // L4b: 不再解包任何明文资源到游戏目录 — 安装产物收敛为:
            //   1. %USERPROFILE%\Zomboid\modcore.bin  (安装器自身 jar 的 AES-256-GCM
            //      密文, 随机密钥前置, 见 coreboot 模板 decrypt)
            //   2. zombie\coreboot.class (自举存根, 从本 jar 资源提取)
            // 运行时 GameWindow.init 注入点 -> coreboot.boot() 解密解包 modcore\。
            this.installLoadout(jarFileUri);
            Logger.print("Loadout installed (encrypted payload + bootstrap stub)");
        }
        catch (IOException | URISyntaxException e) {
            Logger.print("CRITICAL: Failed to install loadout: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to install loadout", e);
        }
    }

    /**
     * L4b 安装: 加密安装器自身 -> Zomboid\modcore.bin; 提取 coreboot 存根 ->
     * zombie\coreboot.class。游戏目录不再有明文 modcore 目录/jar。
     */
    private void installLoadout(String jarFileUri) throws IOException {
        // file URI -> Path 规范转换 (getLocation().toURI().getPath() 在 Windows
        // 产生 "/D:/..." 前导斜杠, Paths.get(String) 不容忍; 传完整 URI 字符串
        // 经 Paths.get(URI) 才是正解)
        Path jarPath;
        try {
            jarPath = Paths.get(java.net.URI.create(jarFileUri));
        } catch (IllegalArgumentException e) {
            throw new IOException("bad installer jar uri: " + jarFileUri, e);
        }
        try {
            // 1. 加密载荷: 从安装器自身提取内嵌 slim 载荷 (modcore/payload.jar,
            //    仅 modcore/* 类+资源) -> AES-256-GCM; 密钥现场随机, 前置 bin 头。
            //    (修正: 旧版加密整个安装器 fat jar (含 runtimeClasspath 解包的
            //    游戏依赖 26337 条目) → bin 63MB 完整游戏副本, 体积即特征)
            byte[] jarBytes;
            try (JarFile self = new JarFile(jarPath.toFile())) {
                ZipEntry payloadEntry = self.getEntry("modcore/payload.jar");
                if (payloadEntry == null) {
                    throw new IOException("embedded payload.jar not found in installer jar (rebuild required)");
                }
                try (InputStream in = self.getInputStream(payloadEntry)) {
                    jarBytes = in.readAllBytes();
                }
            }
            byte[] key = new byte[32];
            new java.security.SecureRandom().nextBytes(key);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key, "AES"));
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(jarBytes);

            Path binPath = Paths.get(System.getProperty("user.home"), "Zomboid", "modcore.bin");
            Files.createDirectories(binPath.getParent());
            java.io.ByteArrayOutputStream binOut = new java.io.ByteArrayOutputStream();
            binOut.write(key);
            binOut.write(iv);
            binOut.write(encrypted);
            Files.write(binPath, binOut.toByteArray());
        }
        catch (java.security.GeneralSecurityException e) {
            throw new IOException("payload encryption failed", e);
        }

        // 2. 提取存根 (jar 内资源 modcore/coreboot.class -> zombie\coreboot.class)
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            ZipEntry stubEntry = jarFile.getEntry("modcore/coreboot.class");
            if (stubEntry == null) {
                throw new IOException("coreboot.class not found in installer jar");
            }
            Path stubPath = Paths.get("zombie", "coreboot.class");
            Files.createDirectories(stubPath.getParent());
            try (InputStream in = jarFile.getInputStream(stubEntry)) {
                Files.copy(in, stubPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // 删除旧命名空间的 EtherDrive 目录 (独立测试版/3.2.4 早期构建残留; 本目录只会由
    // 本 mod 创建, 清理安全)
    private void removeLegacyDriveDir(Path currentDirectory) {
        Path legacyDrive = currentDirectory.resolve("EtherDrive");
        if (Files.exists(legacyDrive, new LinkOption[0])) {
            try {
                Files.walk(legacyDrive, new FileVisitOption[0]).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                Logger.print("Removed legacy EtherDrive directory (merged into modcore)");
            }
            catch (IOException except) {
                Logger.print("Failed to remove legacy EtherDrive directory: " + except.getMessage());
            }
        }
    }

    public void uninstallmodcoreFiles() {
        Logger.print("Deleting all modcore files...");
        try {
            Path currentDirectory = Paths.get(System.getProperty("user.dir"), new String[0]);
            Path targetPath = currentDirectory.resolve("modcore");
            if (Files.exists(targetPath, new LinkOption[0])) {
                Files.walk(targetPath, new FileVisitOption[0]).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            this.removeLegacyDriveDir(currentDirectory);
            // L4b 产物: 自举存根与加密载荷一并清理
            Files.deleteIfExists(Paths.get("zombie", "coreboot.class"));
            Files.deleteIfExists(Paths.get(System.getProperty("user.home"), "Zomboid", "modcore.bin"));
            Logger.print("Deletion modcore files completed successfully");
        }
        catch (IOException except) {
            Logger.print("Failed to delete modcore files: " + except.getMessage());
            except.printStackTrace();
        }
    }

    public void backupGameFiles() {
        Path currentPath = Paths.get("", new String[0]).toAbsolutePath();
        for (int i = 0; i < this.patchFiles.length; ++i) {
            String iteration = "[" + (i + 1) + "/" + this.patchFiles.length + "]";
            Logger.print("Creating a backup file '" + this.patchFiles[i] + "' " + iteration);
            Path originalFilePath = Paths.get(currentPath.toString(), "zombie", this.patchFiles[i]);
            if (Files.exists(originalFilePath, new LinkOption[0])) {
                try {
                    Path backupFilePath = Paths.get(String.valueOf(originalFilePath) + ".bkup", new String[0]);
                    if (Files.exists(backupFilePath, new LinkOption[0])) {
                        Logger.print("Backup of the file already exists. Skipping backup.");
                        continue;
                    }
                    Files.copy(originalFilePath, backupFilePath, new CopyOption[0]);
                }
                catch (IOException e) {
                    Logger.print("Error while creating backup file: " + e.getMessage());
                }
                continue;
            }
            Logger.print(this.patchFiles[i] + " file not found.");
        }
        Logger.print("Backups of game files have been completed!");
    }

    public void patchGameWindow() {
        Patch.injectIntoClass("zombie/GameWindow", "InitDisplay", true, method -> {
            AbstractInsnNode[] nodes;
            String oldTitle = "Project Zomboid";
            String newTitle = "Project Zomboid" + Info.APP_WINDOW_TITLE_SUFFIX;
            for (AbstractInsnNode insn : nodes = method.instructions.toArray()) {
                if (!(insn instanceof LdcInsnNode)) continue;
                LdcInsnNode ldcInsnNode = (LdcInsnNode)insn;
                if (!ldcInsnNode.cst.equals(oldTitle)) continue;
                ldcInsnNode.cst = newTitle;
            }
        });
        Patch.injectIntoClass("zombie/GameWindow", "init", true, method -> {
            AbstractInsnNode insertionPoint = null;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                MethodInsnNode methodInsn;
                if (!(insn instanceof MethodInsnNode) || (methodInsn = (MethodInsnNode)insn).getOpcode() != 184 || !methodInsn.owner.equals("zombie/Lua/LuaManager") || !methodInsn.name.equals("init")) continue;
                insertionPoint = insn;
                break;
            }
            if (insertionPoint == null) {
                throw new IllegalStateException("Cannot find LuaManager.init() invocation in the method when patching the Game window");
            }
            // L4b 两阶段注入 (实测教训 2026-09-10):
            //   boot() 解包 — 必须在 LuaManager.init **之前**: LuaManager.init 的
            //   exposer 链会触发 ClimateManager 类初始化 -> 已补丁的
            //   LuaEventManager.triggerEvent 引用 modcore 类, 解包前解析即
            //   NoClassDefFoundError (实机启动崩溃);
            //   start() 初始化链 — 在 LuaManager.init **之后**: CoreAPI 的
            //   SafeExposer 依赖 LuaManager.env/converterManager。
            // boot: 解包 (插在 LuaManager.init 调用之前)
            InsnList bootCall = new InsnList();
            LabelNode bootTryStart = new LabelNode();
            LabelNode bootTryEnd = new LabelNode();
            LabelNode bootCatchStart = new LabelNode();
            LabelNode bootAfter = new LabelNode();
            bootCall.add((AbstractInsnNode)bootTryStart);
            bootCall.add((AbstractInsnNode)new MethodInsnNode(184, "zombie/coreboot", "boot", "()V", false));
            bootCall.add((AbstractInsnNode)bootTryEnd);
            bootCall.add((AbstractInsnNode)new JumpInsnNode(167, bootAfter));
            bootCall.add((AbstractInsnNode)bootCatchStart);
            bootCall.add((AbstractInsnNode)new InsnNode(87));
            bootCall.add((AbstractInsnNode)bootAfter);
            method.instructions.insertBefore(insertionPoint, bootCall);
            method.tryCatchBlocks.add(new TryCatchBlockNode(bootTryStart, bootTryEnd, bootCatchStart, "java/lang/Throwable"));

            // start: 反射初始化链 (插在 LuaManager.init 调用之后, 原注入段位置)
            InsnList startCall = new InsnList();
            LabelNode startTryStart = new LabelNode();
            LabelNode startTryEnd = new LabelNode();
            LabelNode startCatchStart = new LabelNode();
            LabelNode startAfter = new LabelNode();
            startCall.add((AbstractInsnNode)startTryStart);
            startCall.add((AbstractInsnNode)new MethodInsnNode(184, "zombie/coreboot", "start", "()V", false));
            startCall.add((AbstractInsnNode)startTryEnd);
            startCall.add((AbstractInsnNode)new JumpInsnNode(167, startAfter));
            startCall.add((AbstractInsnNode)startCatchStart);
            startCall.add((AbstractInsnNode)new InsnNode(87));
            startCall.add((AbstractInsnNode)startAfter);
            method.instructions.insert(insertionPoint, startCall);
            method.tryCatchBlocks.add(new TryCatchBlockNode(startTryStart, startTryEnd, startCatchStart, "java/lang/Throwable"));
        });
    }

    public void patchItemContainer() {
        // 座位占用查询空守卫 (原版 B42 空指针): isOccupiedVehicleSeat 链
        // `vehiclePart.getVehicle().getCharacter(...)` 无空判 — 车被开远流式虚拟化
        // 后 owner 退化为 VirtualVehicle, getVehicle() 的 tryCastTo(BaseVehicle)
        // 返回 null → NPE 每帧刷屏 (物品栏正看着该车座位容器时, 实测)。
        // vehiclePart 为空或已虚拟化 → 返回 false (与 isVehicleSeat 短路语义一致)。
        // 注: 代码注入在 ItemContainer 自身类内, 访问自有字段不受跨包可见性限制。
        final boolean[] injected = {false};
        Patch.injectIntoClass("zombie/inventory/ItemContainer", "isOccupiedVehicleSeat", false, method -> {
            if (!method.desc.equals("()Z")) {
                return;
            }
            injected[0] = true;
            InsnList guard = new InsnList();
            LabelNode continueLabel = new LabelNode();
            LabelNode falseLabel = new LabelNode();
            guard.add(new VarInsnNode(25, 0)); // aload_0
            guard.add(new FieldInsnNode(180, "zombie/inventory/ItemContainer", "vehiclePart",
                    "Lzombie/vehicles/VehiclePart;"));
            guard.add(new JumpInsnNode(198, falseLabel)); // IFNULL (vehiclePart 空) → false
            guard.add(new VarInsnNode(25, 0));
            guard.add(new FieldInsnNode(180, "zombie/inventory/ItemContainer", "vehiclePart",
                    "Lzombie/vehicles/VehiclePart;"));
            guard.add(new MethodInsnNode(182, "zombie/vehicles/VehiclePart", "getVehicle",
                    "()Lzombie/vehicles/BaseVehicle;", false));
            guard.add(new JumpInsnNode(198, falseLabel)); // IFNULL (已虚拟化) → false
            guard.add(continueLabel);
            method.instructions.insert(guard);
            method.instructions.add(falseLabel);
            method.instructions.add(new InsnNode(3));  // ICONST_0
            method.instructions.add(new InsnNode(172)); // IRETURN
            Logger.print("  [OK] ItemContainer.isOccupiedVehicleSeat null-guard");
        });
        if (!injected[0]) {
            // 守卫是稳定性改善, 描述符不匹配只告警不中断安装 (game update 容错)
            Logger.print("Warning: isOccupiedVehicleSeat null-guard skipped (descriptor mismatch)");
        }
        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getCapacityWeight", false, method -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreAPI", "isUnlimitedCarry", "Z"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(153, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new InsnNode(11));
            newInstructions.add((AbstractInsnNode)new InsnNode(174));
            newInstructions.add((AbstractInsnNode)carryOnLabel);
            method.instructions.insert(newInstructions);
        });
        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getContentsWeight", false, method -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreAPI", "isUnlimitedCarry", "Z"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(153, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new InsnNode(11));
            newInstructions.add((AbstractInsnNode)new InsnNode(174));
            newInstructions.add((AbstractInsnNode)carryOnLabel);
            method.instructions.insert(newInstructions);
        });
    }

    public void patchCombatSpeed() {
        Patch.injectIntoClass("zombie/characters/IsoGameCharacter", "calculateCombatSpeed", false, method -> {
            AbstractInsnNode returnInsn = method.instructions.getLast();
            while (returnInsn != null && returnInsn.getOpcode() != 174) {
                returnInsn = returnInsn.getPrevious();
            }
            if (returnInsn == null) {
                throw new IllegalStateException("FRETURN not found in calculateCombatSpeed");
            }
            InsnList toInject = new InsnList();
            toInject.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
            toInject.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
            toInject.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreAPI", "combatSpeedMultiplier", "F"));
            toInject.add((AbstractInsnNode)new InsnNode(106));
            method.instructions.insertBefore(returnInsn, toInject);
            Logger.print("  [OK] Injected combat speed multiplier into IsoGameCharacter.calculateCombatSpeed()");
        });
    }

    //*********************************************************
    //* 近战攻击距离加成 (CoreAPI.attackRangeBonus, 单位=格, 0=原版):
    //* 1) HandWeapon.getMaxRange 两个重载: 枪械走 isRanged 提前 return (带参版
    //*    的首个 freturn), 注入在最后一个 freturn 前 = 只命中近战返回路径;
    //* 2) CombatManager 两方法里 3 处 "DistToSquared > 9.0f" 3 格硬上限替换为
    //*    max(getMaxRange(owner), 3.0)^2 —— 加成 0 时精确还原原版 3 格,
    //*    加成 X 时最远恰为 原版maxRange+X, 恒在服务器复核线 (原版maxRange+5)
    //*    之内留 1 格冗余, 与武器类型/钝斧 7 级 rangeMod 1.2 均无关。
    //*********************************************************
    public void patchAttackRange() {
        Patch.injectIntoClass("zombie/inventory/types/HandWeapon", "getMaxRange", false, method -> {
            AbstractInsnNode returnInsn = method.instructions.getLast();
            while (returnInsn != null && returnInsn.getOpcode() != 174) {
                returnInsn = returnInsn.getPrevious();
            }
            if (returnInsn == null) {
                throw new IllegalStateException("FRETURN not found in HandWeapon.getMaxRange");
            }
            InsnList toInject = new InsnList();
            LabelNode skipBonus = new LabelNode();
            toInject.add((AbstractInsnNode)new VarInsnNode(25, 0));
            toInject.add((AbstractInsnNode)new MethodInsnNode(182, "zombie/inventory/types/HandWeapon", "isRanged", "()Z", false));
            toInject.add((AbstractInsnNode)new JumpInsnNode(154, skipBonus));
            this.addAttackRangeBonusFetch(toInject, skipBonus);
            toInject.add((AbstractInsnNode)new InsnNode(98));
            toInject.add((AbstractInsnNode)skipBonus);
            method.instructions.insertBefore(returnInsn, toInject);
            Logger.print("  [OK] Injected melee range bonus into HandWeapon.getMaxRange" + method.desc);
        });
        this.patchMeleeRangeCap("calcValidTarget", 1, false);
        this.patchMeleeRangeCap("calculateHitListWeapon", 2, true);
    }

    // 把 CombatManager 方法里的 LDC 9.0f 常量替换为 max(maxRange, 3.0f)^2。
    // 取武器方式二选一: calcValidTarget 的 weapon 就是 2 号局部变量;
    // calculateHitListWeapon 的 2 号是 AttackVars, 需经 getWeapon(owner) 现取。
    private void patchMeleeRangeCap(String methodName, int expectedCaps, boolean weaponViaAttackVars) {
        Patch.injectIntoClass("zombie/CombatManager", methodName, false, method -> {
            int replaced = 0;
            AbstractInsnNode insn = method.instructions.getFirst();
            while (insn != null) {
                AbstractInsnNode next = insn.getNext();
                if (insn instanceof LdcInsnNode && ((LdcInsnNode)insn).cst instanceof Float
                        && ((Float)((LdcInsnNode)insn).cst).floatValue() == 9.0f) {
                    InsnList toInject = new InsnList();
                    toInject.add((AbstractInsnNode)new VarInsnNode(25, 2));
                    if (weaponViaAttackVars) {
                        toInject.add((AbstractInsnNode)new VarInsnNode(25, 1));
                        // 原源码此处是 getWeapon((IsoLivingCharacter)owner) 显式转型:
                        // owner 形参是父类 IsoGameCharacter, 字节码必须补 CHECKCAST,
                        // 否则类校验报 Bad type on operand stack (3.2.2 首版实测)
                        toInject.add((AbstractInsnNode)new TypeInsnNode(192, "zombie/characters/IsoLivingCharacter"));
                        toInject.add((AbstractInsnNode)new MethodInsnNode(182, "zombie/network/fields/hit/AttackVars", "getWeapon", "(Lzombie/characters/IsoLivingCharacter;)Lzombie/inventory/types/HandWeapon;", false));
                    }
                    toInject.add((AbstractInsnNode)new VarInsnNode(25, 1));
                    toInject.add((AbstractInsnNode)new MethodInsnNode(182, "zombie/inventory/types/HandWeapon", "getMaxRange", "(Lzombie/characters/IsoGameCharacter;)F", false));
                    toInject.add((AbstractInsnNode)new LdcInsnNode(Float.valueOf(3.0f)));
                    toInject.add((AbstractInsnNode)new MethodInsnNode(184, "java/lang/Math", "max", "(FF)F", false));
                    toInject.add((AbstractInsnNode)new InsnNode(89));
                    toInject.add((AbstractInsnNode)new InsnNode(106));
                    method.instructions.insert(insn, toInject);
                    method.instructions.remove(insn);
                    ++replaced;
                }
                insn = next;
            }
            if (replaced != expectedCaps) {
                throw new IllegalStateException("Expected " + expectedCaps + " range cap(s) in CombatManager." + methodName + ", replaced " + replaced);
            }
            Logger.print("  [OK] Patched " + replaced + " melee range cap(s) in CombatManager." + methodName + "()");
        });
    }

    // CoreMain.getInstance() → CoreAPI → attackRangeBonus 取值链, 带三级空守卫
    // (任一为空则跳过加成, 原值返回), 防 UI/脚本在主逻辑初始化前调用 getMaxRange。
    private void addAttackRangeBonusFetch(InsnList list, LabelNode skipLabel) {
        list.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add((AbstractInsnNode)new JumpInsnNode(198, skipLabel));
        list.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
        list.add((AbstractInsnNode)new JumpInsnNode(198, skipLabel));
        list.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
        list.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreAPI", "attackRangeBonus", "F"));
    }

    public void patchLuaEventManager() {
        Patch.injectIntoClass("zombie/Lua/LuaEventManager", "triggerEvent", true, method -> {
            InsnList toInject = new InsnList();
            toInject.add((AbstractInsnNode)new VarInsnNode(25, 0));
            toInject.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/utils/EventSubscriber", "invokeSubscriber", "(Ljava/lang/String;)V", false));
            method.instructions.insertBefore(method.instructions.get(0), toInject);
        });
    }

    public void patchLuaManager() {
        Patch.injectIntoClass("zombie/Lua/LuaManager", "RunLua", true, method -> {
            if (!method.desc.equals("(Ljava/lang/String;Z)Ljava/lang/Object;")) {
                return;
            }
            InsnList newInstructions = new InsnList();
            LabelNode endOfMethodLabel = new LabelNode();
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/LuaCompiler", "getInstance", "()Lmodcore/core/LuaCompiler;", false));
            newInstructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "modcore/core/LuaCompiler", "isShouldLuaCompile", "(Ljava/lang/String;)Z", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(154, endOfMethodLabel));
            newInstructions.add((AbstractInsnNode)new InsnNode(1));
            newInstructions.add((AbstractInsnNode)new InsnNode(176));
            newInstructions.add((AbstractInsnNode)endOfMethodLabel);
            method.instructions.insert(newInstructions);
        });
    }

    public void exposePrivateFields() {
        Logger.print("=======================================================");
        Logger.print("[ModCore] Build 42 Version Loaded");
        Logger.print("[ModCore] WARNING: Most features are experimental!");
        Logger.print("[ModCore] CONFIRMED WORKING: Item Spawner, Radar/ESP");
        Logger.print("=======================================================");
        Logger.print("[ModCore] Exposing private fields for direct access...");
        Patch.modifyClass("zombie/characters/IsoPlayer", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[ModCore] IsoPlayer: " + exposedCount + " fields exposed");
        });
        Patch.modifyClass("zombie/network/GameClient", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[ModCore] GameClient: " + exposedCount + " fields exposed");
        });
        Patch.modifyClass("zombie/characters/PlayerCheats", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[ModCore] PlayerCheats: " + exposedCount + " fields exposed");
        });
        Logger.print("[ModCore] Field exposure completed - direct access enabled");
    }

    public void patchAntiCheatSystem() {
        Logger.print("Patching anti-cheat system with bypass hooks...");
        try {
            this.patchAbstractAntiCheatValidation();
            this.patchSuspiciousActivityReporting();
            this.patchKickBanMethods();
            Logger.print("Anti-cheat system patching completed successfully");
        }
        catch (Exception e) {
            Logger.print("Warning: Anti-cheat patching encountered issues: " + e.getMessage());
            Logger.logException(e);
        }
    }

    private void patchHeadshotOnly() {
        Logger.print("Patching CombatManager.processHit with headshot-only hook...");
        try {
            Patch.injectIntoClass("zombie/CombatManager", "processHit", false, method -> {
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isHeadshotOnly", "Z"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new VarInsnNode(25, 2));
                hookInstructions.add(new VarInsnNode(25, 3));
                hookInstructions.add(new FieldInsnNode(178, "zombie/core/physics/RagdollBodyPart", "BODYPART_HEAD", "Lzombie/core/physics/RagdollBodyPart;"));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new VarInsnNode(25, 2));
                hookInstructions.add(new VarInsnNode(25, 3));
                hookInstructions.add(new MethodInsnNode(182, "zombie/CombatManager", "calculateShotDirection", "(Lzombie/characters/IsoGameCharacter;Lzombie/characters/IsoGameCharacter;)Lzombie/combat/ShotDirection;", false));
                hookInstructions.add(new MethodInsnNode(182, "zombie/CombatManager", "processTargetedHit", "(Lzombie/inventory/types/HandWeapon;Lzombie/characters/IsoGameCharacter;Lzombie/characters/IsoGameCharacter;Lzombie/core/physics/RagdollBodyPart;Lzombie/combat/ShotDirection;)V", false));
                hookInstructions.add(new FieldInsnNode(178, "zombie/core/physics/RagdollBodyPart", "BODYPART_HEAD", "Lzombie/core/physics/RagdollBodyPart;"));
                hookInstructions.add(new MethodInsnNode(182, "zombie/core/physics/RagdollBodyPart", "ordinal", "()I", false));
                hookInstructions.add(new InsnNode(172));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected headshot-only hook into CombatManager.processHit()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: Headshot-only injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    //*********************************************************
    //* 枪械百发百中 (CoreAPI.isAlwaysHit): 劫持 CombatManager.calculateHitChanceData
    //* 的收尾 PUTFIELD (唯一可能产生 <100 的 clamp 路径; 前两个早退 PUTFIELD 本就写
    //* MAXIMUM=100) —— 栈上 POP 丢弃钳制结果, LDC 100.0f 覆写。
    //* 消费方: calculateHitInfoList 的 hitInfo.chance (=100 → Rand.Next(100)<=100 恒中)
    //* + updateReticle 的 isoReticle.setChance (准星同步显示 100%)。
    //* 服务端包内无命中率字段, 与只爆头同一信任模型, MP 同样生效。
    //*********************************************************
    private void patchAlwaysHit() {
        Logger.print("Patching CombatManager.calculateHitChanceData with always-hit hook...");
        try {
            Patch.injectIntoClass("zombie/CombatManager", "calculateHitChanceData", false, method -> {
                AbstractInsnNode putfield = method.instructions.getLast();
                while (putfield != null && !(putfield instanceof FieldInsnNode
                        && ((FieldInsnNode)putfield).owner.equals("zombie/CombatManager$HitChanceData")
                        && ((FieldInsnNode)putfield).name.equals("hitChance"))) {
                    putfield = putfield.getPrevious();
                }
                if (putfield == null) {
                    throw new IllegalStateException("hitChance PUTFIELD not found in calculateHitChanceData");
                }
                // 开关守卫 (与 patchHeadshotOnly 同款三级判空): isAlwaysHit=false 时跳过覆写,
                // PUTFIELD 落回钳制原值 = 原版行为。守卫指令全部栈中性 (IFNULL/IFEQ 自弹测试值),
                // 不影响 [hitChanceData_ref, clamped_F] 操作数栈。
                InsnList toInject = new InsnList();
                LabelNode skipOverwrite = new LabelNode();
                toInject.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add((AbstractInsnNode)new JumpInsnNode(198, skipOverwrite));
                toInject.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                toInject.add((AbstractInsnNode)new JumpInsnNode(198, skipOverwrite));
                toInject.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                toInject.add((AbstractInsnNode)new FieldInsnNode(180, "modcore/core/CoreAPI", "isAlwaysHit", "Z"));
                toInject.add((AbstractInsnNode)new JumpInsnNode(153, skipOverwrite));
                toInject.add((AbstractInsnNode)new InsnNode(87));
                toInject.add((AbstractInsnNode)new LdcInsnNode(Float.valueOf(100.0f)));
                toInject.add((AbstractInsnNode)skipOverwrite);
                method.instructions.insertBefore(putfield, toInject);
                Logger.print("  [OK] Injected always-hit (chance=100) into CombatManager.calculateHitChanceData()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: Always-hit injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    //*********************************************************
    //* 超级群攻 (CoreAPI.isSuperMultiHit, 研判 超级群攻-研判(未实现).md §三A/§五/§六):
    //* ① maxHit 覆写: calculateHitInfoList 里 maxHit(局部槽 5) 全部赋值完成后、
    //*   `if (maxHit <= 0)` 门之前, 门控开时覆写为 superMultiHitCount — 近战裁剪
    //*   while(size > maxHit) 与枪械 hitCount >= maxHit 上限读同一局部槽, 一处全覆盖;
    //* ② 全向扇面: calcValidTargets / getNearestMeleeTargetPosAndDot 两处
    //*   getMinAngle/getMaxAngle 局部覆写为 -2/+2 (dot 值域 [-1,1], 恒通过;
    //*   倒地 minAngle /= 1.5 后仍 ≤ -1) — 单门控恒全向, 无独立扇面开关 (§六 用户拍板);
    //* ③ 属主僵尸命中包抑制: GameClient.sendPlayerHit 头部 — 开关开 + target 是
    //*   IsoZombie + 本地模拟 (NetworkZombieSimulator.isZombieSimulated, 属主判定现成 API)
    //*   → 直接 return 不发包; 伤害走客户端本地结算 + 血量经僵尸模拟包盲采上传,
    //*   非属主僵尸照常发包维持原版行为 (超额命中天然只作用于身边属主僵尸)。
    //*********************************************************
    public void patchSuperMultiHit() {
        Logger.print("Patching CombatManager for super multi-hit...");
        try {
            Patch.injectIntoClass("zombie/CombatManager", "calculateHitInfoList", false, method -> {
                // 锚点: 门控 `if (maxHit <= 0)` 的 ILOAD 5 (其后紧跟 IFGT)。
                // 注意: 不能锚 ISTORE 5 —— 最后一处 ISTORE 5 (bareHands+targetOnGround 分支体,
                // 字节码 istore@151) 之后、ILOAD 5 之前, 有 if_acmpne/ifnull 跳转直指汇聚点
                // (iload@191), 插在 istore 后 = 插进分支体内, 普通武器挥击被跳越, 门控永不执行
                // (实测 gateHits=0 / maxHit 恒为脚本值, 2026-09-09 重研判根因)。
                // ILOAD 5 + IFGT 全方法唯一 (trim/连射上限均为 if_icmpXX), 锚它并 insertBefore,
                // 使全部路径 (分支体 fall-through + 各路跳转目标) 都先经过注入区。
                AbstractInsnNode anchor = null;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof VarInsnNode && ((VarInsnNode)insn).getOpcode() == 21 && ((VarInsnNode)insn).var == 5) {
                        AbstractInsnNode jump = skipIgnorable(insn.getNext());
                        if (jump instanceof JumpInsnNode && ((JumpInsnNode)jump).getOpcode() == 157) { // IFGT: if (maxHit <= 0) 门
                            anchor = insn;
                        }
                    }
                }
                if (anchor == null) {
                    throw new IllegalStateException("maxHit gate (ILOAD 5 + IFGT) not found in calculateHitInfoList");
                }
                InsnList toInject = new InsnList();
                LabelNode skip = new LabelNode();
                addSuperMultiHitGate(toInject, skip);
                toInject.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                toInject.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "superMultiHitCount", "I"));
                toInject.add(new VarInsnNode(54, 5)); // ISTORE maxHit
                toInject.add(skip);
                method.instructions.insertBefore(anchor, toInject);
                Logger.print("  [OK] Injected super multi-hit maxHit overwrite before maxHit gate in CombatManager.calculateHitInfoList()");
            });
            this.patchWideMeleeArc("calcValidTargets");
            this.patchWideMeleeArc("getNearestMeleeTargetPosAndDot");
        }
        catch (Exception e) {
            Logger.print("Warning: super multi-hit injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    // 全向扇面: 方法内唯一的 getMinAngle→FSTORE / getMaxAngle→FSTORE 对, 其后覆写两局部为 -2/+2
    private void patchWideMeleeArc(String methodName) {
        Patch.injectIntoClass("zombie/CombatManager", methodName, false, method -> {
            AbstractInsnNode minStore = null;
            AbstractInsnNode maxStore = null;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode && ((MethodInsnNode)insn).getOpcode() == 182
                        && ((MethodInsnNode)insn).owner.equals("zombie/inventory/types/HandWeapon")
                        && ((MethodInsnNode)insn).name.equals("getMinAngle")) {
                    AbstractInsnNode store = skipIgnorable(insn.getNext());
                    if (store instanceof VarInsnNode && ((VarInsnNode)store).getOpcode() == 56) {
                        minStore = store;
                    }
                }
                if (insn instanceof MethodInsnNode && ((MethodInsnNode)insn).getOpcode() == 182
                        && ((MethodInsnNode)insn).owner.equals("zombie/inventory/types/HandWeapon")
                        && ((MethodInsnNode)insn).name.equals("getMaxAngle")) {
                    AbstractInsnNode store = skipIgnorable(insn.getNext());
                    if (store instanceof VarInsnNode && ((VarInsnNode)store).getOpcode() == 56) {
                        maxStore = store;
                    }
                }
            }
            if (minStore == null || maxStore == null) {
                throw new IllegalStateException("getMinAngle/getMaxAngle FSTORE pair not found in " + methodName);
            }
            int minVar = ((VarInsnNode)minStore).var;
            int maxVar = ((VarInsnNode)maxStore).var;
            InsnList toInject = new InsnList();
            LabelNode skip = new LabelNode();
            addSuperMultiHitGate(toInject, skip);
            toInject.add(new LdcInsnNode(Float.valueOf(-2.0f)));
            toInject.add(new VarInsnNode(56, minVar)); // FSTORE minAngle
            toInject.add(new LdcInsnNode(Float.valueOf(2.0f)));
            toInject.add(new VarInsnNode(56, maxVar)); // FSTORE maxAngle
            toInject.add(skip);
            method.instructions.insert(maxStore, toInject);
            Logger.print("  [OK] Injected omnidirectional melee arc into CombatManager." + methodName
                    + "() (angle slots " + minVar + "/" + maxVar + ")");
        });
    }

    // 锚点扫描辅助: 跳过 Label/Frame/LineNumber 等非指令节点 (ClassReader 会把
    // 行号表读进指令树, 不跳过会隔断相邻指令匹配)
    private static AbstractInsnNode skipIgnorable(AbstractInsnNode insn) {
        while (insn instanceof LabelNode || insn instanceof FrameNode || insn instanceof LineNumberNode) {
            insn = insn.getNext();
        }
        return insn;
    }

    // 三级空守卫 (patchAlwaysHit 同款): CoreMain.getInstance → CoreAPI → isSuperMultiHit,
    // 任一为空/关则跳到 skip, 全部指令栈中性
    private void addSuperMultiHitGate(InsnList list, LabelNode skip) {
        list.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add(new JumpInsnNode(198, skip));
        list.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
        list.add(new JumpInsnNode(198, skip));
        list.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
        list.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
        list.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isSuperMultiHit", "Z"));
        list.add(new JumpInsnNode(153, skip));
    }

    // 属主僵尸命中包抑制: GameClient.sendPlayerHit 头部 (静态方法, target = 参数槽 1)
    public void patchSuperMultiHitSuppression() {
        Logger.print("Patching GameClient.sendPlayerHit with owner-zombie suppression...");
        try {
            Patch.injectIntoClass("zombie/network/GameClient", "sendPlayerHit", true, method -> {
                InsnList toInject = new InsnList();
                LabelNode skip = new LabelNode();
                addSuperMultiHitGate(toInject, skip);
                // target instanceof IsoZombie
                toInject.add(new VarInsnNode(25, 1));
                toInject.add(new TypeInsnNode(193, "zombie/characters/IsoZombie"));
                toInject.add(new JumpInsnNode(153, skip));
                // NetworkZombieSimulator.getInstance().isZombieSimulated(Short.valueOf(((IsoZombie)target).getOnlineID()))
                toInject.add(new VarInsnNode(25, 1));
                toInject.add(new TypeInsnNode(192, "zombie/characters/IsoZombie"));
                toInject.add(new MethodInsnNode(182, "zombie/characters/IsoZombie", "getOnlineID", "()S", false));
                toInject.add(new MethodInsnNode(184, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
                toInject.add(new MethodInsnNode(184, "zombie/popman/NetworkZombieSimulator", "getInstance", "()Lzombie/popman/NetworkZombieSimulator;", false));
                toInject.add(new InsnNode(95)); // SWAP: [Short, sim] → [sim, Short]
                toInject.add(new MethodInsnNode(182, "zombie/popman/NetworkZombieSimulator", "isZombieSimulated", "(Ljava/lang/Short;)Z", false));
                toInject.add(new JumpInsnNode(153, skip));
                toInject.add(new InsnNode(177)); // RETURN: 属主模拟僵尸不发包 (血量走模拟包盲采)
                toInject.add(skip);
                method.instructions.insert(toInject);
                Logger.print("  [OK] Injected owner-zombie hit-packet suppression into GameClient.sendPlayerHit()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: sendPlayerHit suppression injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    //*********************************************************
    //* 注入⑨ 群攻全额伤害 (研判 §九): attackCollisionCheck 里 melee 伤害按
    //* `damageSplit = damage/(split++*0.5f)` 逐目标递减 (首目标 2×, 第 N 目标 2/N×),
    //* 20 目标时第 5 只往后伤害趋零 — 命中列表装了 20 只, 观感只见前 3 只倒。
    //* 门控开时跳过分摊除法, damageSplit = damage 原值 (全员 1×); split 局部
    //* 无其他读点 (javap 核实), 跳过 IINC 无副作用; 门控关闭指令栈完全还原原版。
    //*********************************************************
    public void patchSuperMultiHitFullDamage() {
        Logger.print("Patching CombatManager.attackCollisionCheck with full-damage hook...");
        try {
            Patch.injectIntoClass("zombie/CombatManager", "attackCollisionCheck", false, method -> {
                // 锚点: FLOAD damage → ILOAD split → IINC split,1 → I2F → LDC 0.5f → FMUL → FDIV → FSTORE damageSplit
                // (javap 实测槽位 damage=33/split=12/damageSplit=34, iload split 全方法唯一;
                //  扫描按语义序列匹配, 槽位从指令流现取, 不硬编码)
                AbstractInsnNode anchorFload = null;
                AbstractInsnNode anchorFstore = null;
                int damageVar = -1;
                int damageSplitVar = -1;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof VarInsnNode) || ((VarInsnNode)insn).getOpcode() != 23) continue; // FLOAD damage
                    AbstractInsnNode iload = skipIgnorable(insn.getNext());
                    if (!(iload instanceof VarInsnNode) || ((VarInsnNode)iload).getOpcode() != 21) continue; // ILOAD split
                    int splitVar = ((VarInsnNode)iload).var;
                    AbstractInsnNode iinc = skipIgnorable(iload.getNext());
                    if (!(iinc instanceof IincInsnNode) || ((IincInsnNode)iinc).var != splitVar || ((IincInsnNode)iinc).incr != 1) continue;
                    AbstractInsnNode i2f = skipIgnorable(iinc.getNext());
                    if (!(i2f instanceof InsnNode) || i2f.getOpcode() != 134) continue; // I2F
                    AbstractInsnNode ldc = skipIgnorable(i2f.getNext());
                    if (!(ldc instanceof LdcInsnNode) || !(((LdcInsnNode)ldc).cst instanceof Float)
                            || ((Float)((LdcInsnNode)ldc).cst).floatValue() != 0.5f) continue;
                    AbstractInsnNode fmul = skipIgnorable(ldc.getNext());
                    if (!(fmul instanceof InsnNode) || fmul.getOpcode() != 106) continue; // FMUL
                    AbstractInsnNode fdiv = skipIgnorable(fmul.getNext());
                    if (!(fdiv instanceof InsnNode) || fdiv.getOpcode() != 110) continue; // FDIV
                    AbstractInsnNode fstore = skipIgnorable(fdiv.getNext());
                    if (!(fstore instanceof VarInsnNode) || ((VarInsnNode)fstore).getOpcode() != 56) continue; // FSTORE damageSplit
                    anchorFload = insn;
                    anchorFstore = fstore;
                    damageVar = ((VarInsnNode)insn).var;
                    damageSplitVar = ((VarInsnNode)fstore).var;
                }
                if (anchorFload == null) {
                    throw new IllegalStateException("damage split sequence (FLOAD/ILOAD/IINC/I2F/LDC 0.5f/FMUL/FDIV/FSTORE) not found in attackCollisionCheck");
                }
                InsnList toInject = new InsnList();
                LabelNode skip = new LabelNode(); // 门关: 落回原版分摊 (damage 已在栈顶, 与原 FLOAD 后栈形一致)
                LabelNode end = new LabelNode();  // 门开: 直跳原版 FSTORE 之后
                addSuperMultiHitGate(toInject, skip);
                // 门开: damageSplit = damage (damage 已由锚点 FLOAD 压栈)
                toInject.add(new VarInsnNode(56, damageSplitVar));
                toInject.add(new JumpInsnNode(167, end)); // GOTO end
                toInject.add(skip);
                method.instructions.insert(anchorFload, toInject);
                method.instructions.insert(anchorFstore, end);
                Logger.print("  [OK] Injected full-damage (split skip, slots " + damageVar + "/" + damageSplitVar
                        + ") into CombatManager.attackCollisionCheck()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: full-damage injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    private void patchGameClientSyncBlocker() {
        Logger.print("Patching GameClient.update with server sync filter...");
        try {
            Patch.injectIntoClass("zombie/network/GameClient", "update", false, method -> {
                InsnList hookInstructions = new InsnList();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/ServerSyncBlocker", "filterIncomingSyncPackets", "()V", false));
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected server sync filter into GameClient.update()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: GameClient sync filter injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    private void patchRoleCapabilityForSP() {
        Logger.print("Patching Role.hasCapability for single-player unlock...");
        try {
            Patch.injectIntoClass("zombie/characters/Role", "hasCapability", true, method -> {
                if (!method.desc.equals("(Lzombie/characters/IsoMovingObject;Lzombie/characters/Capability;)Z")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new FieldInsnNode(178, "zombie/network/GameClient", "client", "Z"));
                hookInstructions.add(new JumpInsnNode(154, continueLabel));
                hookInstructions.add(new FieldInsnNode(178, "zombie/network/GameServer", "server", "Z"));
                hookInstructions.add(new JumpInsnNode(154, continueLabel));
                hookInstructions.add(new InsnNode(4));
                hookInstructions.add(new InsnNode(172));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected single-player capability unlock into Role.hasCapability()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: Role capability injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    private void patchVehicleNoKey() {
        Logger.print("Patching BaseVehicle for unconditional hotwire & keyless start...");
        try {
            Patch.injectIntoClass("zombie/vehicles/BaseVehicle", "tryHotwire", false, method -> {
                if (!method.desc.equals("(I)V")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                hookInstructions.add(new LdcInsnNode(200));
                hookInstructions.add(new VarInsnNode(54, 1));
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected electricity level 200 into BaseVehicle.tryHotwire()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: tryHotwire injection failed: " + e.getMessage());
            Logger.logException(e);
        }
        try {
            Patch.injectIntoClass("zombie/vehicles/BaseVehicle", "tryStartEngine", false, method -> {
                if (!method.desc.equals("(Z)V")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                hookInstructions.add(new InsnNode(4));
                hookInstructions.add(new VarInsnNode(54, 1));
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected haveKey=true into BaseVehicle.tryStartEngine()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: tryStartEngine injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * 僵尸不理会本机玩家 (多人可用的隐身):
     * 客户端模拟的僵尸目标经 ZombieSimulationPacket 上传 (target null => -1),
     * 服务端 parseZombie 零校验采纳 (-1 => target=null), 无目标的僵尸不追不咬
     * (getShouldAttack 在 target==null 时返回 false)。setTarget 是唯一的目标
     * setter, 视野/声音/被车撞/受击五条路径全走它 —— 开头拦截本地玩家即全覆盖。
     */
    private void patchZombieSetTarget() {
        Logger.print("Patching IsoZombie.setTarget with zombie-ignore hook...");
        try {
            Patch.injectIntoClass("zombie/characters/IsoZombie", "setTarget", false, method -> {
                if (!method.desc.equals("(Lzombie/iso/IsoMovingObject;)V")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isZombieDontAttack", "Z"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new TypeInsnNode(193, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new TypeInsnNode(192, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new MethodInsnNode(182, "zombie/characters/IsoPlayer", "isLocalPlayer", "()Z", false));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new InsnNode(177));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected zombie-ignore hook into IsoZombie.setTarget()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: IsoZombie.setTarget injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * 僵尸不感知本机玩家 (修 setTarget 拦截引发的 spottedNew NPE):
     * IsoPlayer.updateLOS 每帧对附近僵尸调 IsoZombie.spotted(player,...), 其 spottedNew/
     * spottedOld 内部 setTarget(other) 后立即解引用 this.target.getZ() (IsoZombie 反编译
     * :1909 等) —— 而 patchZombieSetTarget 把 setTarget(本机玩家) 拦成空操作, target 恒 null,
     * vanilla 到这行必 NPE (堆栈 IsoZombie.spottedNew -> spotted -> IsoPlayer.TestZombieSpotPlayer)。
     * spottedNew/spottedOld 只经 spotted() 进入, 故在 spotted 开头拦截: 开关开且 other 是本机
     * 玩家则直接 return —— 僵尸整套视野感知逻辑对本机玩家不再运行 (既不设 target、不解引用、也不
     * 追击), 与"不攻击/不理会"意图一致; 其余目标路径 (声音/被车撞/受击/网络) 仍由 setTarget 拦截保持
     * target=null (多人上传 target=-1), getShouldAttack 注入作为攻击门兜底。
     */
    private void patchZombieSpotted() {
        Logger.print("Patching IsoZombie.spotted with zombie-ignore hook...");
        try {
            Patch.injectIntoClass("zombie/characters/IsoZombie", "spotted", false, method -> {
                if (!method.desc.equals("(Lzombie/iso/IsoMovingObject;Z)V")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isZombieDontAttack", "Z"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new TypeInsnNode(193, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new TypeInsnNode(192, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new MethodInsnNode(182, "zombie/characters/IsoPlayer", "isLocalPlayer", "()Z", false));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new InsnNode(177));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected zombie-ignore hook into IsoZombie.spotted()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: IsoZombie.spotted injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * 僵尸不攻击本机玩家 (SP+MP 通用, 无需调试权限):
     * getShouldAttack() 是僵尸攻击的唯一裁决门 (作为动画变量 "battack" 驱动攻击动作),
     * vanilla 自身的"僵尸不攻击"标志(target.isZombiesDontAttack():862)与 ghostMode(:890)
     * 判定都在这里。而 IsoGameCharacter.setZombiesDontAttack 被 Role.hasCapability 门禁
     * (单人需 Core.debug 才放行, 否则强制置 false) —— 这正是单人下该功能失效、必须先开
     * "解锁调试权限"的根因。改为在 getShouldAttack 开头拦截: 开关开且当前 target 是本机玩家
     * 则直接 return false。此处位于所有目标设置路径(setTarget / 直写 target 字段 / 网络)的
     * 下游, 单人本地模拟僵尸与多人本机模拟僵尸均生效; 完全不碰能力系统/Core.debug, 无踢出、
     * 无关调试导致的游戏重启风险。
     */
    private void patchZombieShouldAttack() {
        Logger.print("Patching IsoZombie.getShouldAttack with zombie-ignore hook...");
        try {
            Patch.injectIntoClass("zombie/characters/IsoZombie", "getShouldAttack", false, method -> {
                if (!method.desc.equals("()Z")) {
                    return;
                }
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isZombieDontAttack", "Z"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new FieldInsnNode(180, "zombie/characters/IsoZombie", "target", "Lzombie/iso/IsoMovingObject;"));
                hookInstructions.add(new TypeInsnNode(193, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new FieldInsnNode(180, "zombie/characters/IsoZombie", "target", "Lzombie/iso/IsoMovingObject;"));
                hookInstructions.add(new TypeInsnNode(192, "zombie/characters/IsoPlayer"));
                hookInstructions.add(new MethodInsnNode(182, "zombie/characters/IsoPlayer", "isLocalPlayer", "()Z", false));
                hookInstructions.add(new JumpInsnNode(153, continueLabel));
                hookInstructions.add(new InsnNode(3));
                hookInstructions.add(new InsnNode(172));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected zombie-ignore hook into IsoZombie.getShouldAttack()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: IsoZombie.getShouldAttack injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * 建号增强: 在 CreatePlayerPacket.set(byte) (客户端发包前的最终组装点) 末尾
     * 调 CharacterCreationBoost.apply 改写 descriptor/traits/wornItems —— 服务端
     * 对建号包的特性点数/技能/服装零校验, 照单全收。
     */
    private void patchCharacterCreationBoost() {
        Logger.print("Patching CreatePlayerPacket.set with creation boost hook...");
        try {
            Patch.injectIntoClass("zombie/network/packets/character/CreatePlayerPacket", "set", false, method -> {
                if (!method.desc.equals("(B)V")) {
                    return;
                }
                AbstractInsnNode returnInsn = method.instructions.getLast();
                while (returnInsn != null && returnInsn.getOpcode() != 177) {
                    returnInsn = returnInsn.getPrevious();
                }
                if (returnInsn == null) {
                    throw new IllegalStateException("RETURN not found in CreatePlayerPacket.set");
                }
                InsnList toInject = new InsnList();
                toInject.add(new VarInsnNode(25, 0));
                toInject.add(new MethodInsnNode(184, "modcore/core/CharacterCreationBoost", "apply", "(Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(returnInsn, toInject);
                Logger.print("  [OK] Injected creation boost hook into CreatePlayerPacket.set()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: creation boost injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * SP 建号增强: 单人不走 CreatePlayerPacket (IsoWorld.init 直接 new IsoPlayer +
     * applyTraits(luaTraits)), 包注入在 SP 永远不会触发。改为在
     * IsoGameCharacter.applyTraits(List) 头部调 CharacterCreationBoost.applySP:
     * 就地改写 luaTraits 与 descriptor.xpBoostMap。MP 客户端不调 applyTraits
     * (服务端才调且无本 mod), 该钩子天然只影响 SP; CoreMain 为空时直通。
     */
    private void patchApplyTraitsSP() {
        Logger.print("Patching IsoGameCharacter.applyTraits with SP creation boost hook...");
        try {
            Patch.injectIntoClass("zombie/characters/IsoGameCharacter", "applyTraits", false, method -> {
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new JumpInsnNode(198, continueLabel));
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                hookInstructions.add(new JumpInsnNode(198, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/CharacterCreationBoost", "applySP", "(Lzombie/characters/IsoGameCharacter;Ljava/util/List;)V", false));
                hookInstructions.add(continueLabel);
                method.instructions.insert(hookInstructions);
                Logger.print("  [OK] Injected SP creation boost hook into IsoGameCharacter.applyTraits()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: applyTraits injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /*
     * Fullbright 真全亮 (功能 9, 纯客户端渲染, 零上行包)。B42 光照计算在 native
     * (Lighting64.dll), 但渲染取值全部经 Java 单点回读:
     *  ① IsoGridSquare.getVertLight(I,I) —— 全树 23 处调用全在渲染方法内 (墙/地板/
     *     水/雪/水洼/FBORenderCell), 且 interpolateLight 经它喂角色/载具模型 ambient,
     *     开头拦截返回 -1 (0xFFFFFFFF 全白) 即同时覆盖世界几何+模型;
     *  ② IsoGridSquare.cacheLightInfo() —— 每帧把 native lightInfo 缓存进
     *     lightInfo[playerIndex], 被 FBORenderCell/IsoObject 用作物件与精灵着色;
     *     原逻辑跑完后把缓存副本 r/g/b/a 拉满 (只改本方块缓存, 不碰共享对象);
     *  ③ RenderSettings$PlayerRenderSettings.updateRenderSettings 尾部 —— 全局
     *     夜色 tint/去饱和清零 (夜视镜正是靠 ambient=1.0 点亮全图的同一条链),
     *     同时经 stateEndFrame 把 ambient=1 传进 native 作冗余保险。
     * 视野锥出锥黑幕是独立 overlay (viewConeOpacity, 官方选项), 在 CoreAPI
     * 切换边沿置 0/还原; chunk FBO 缓存 (fboRenderChunk 默认开) 由同处的
     * LightingJNI.buildingsChanged() 强制全量重画。佐证: 游戏自带调试开关
     * DebugDraw.SkipWorldShading 干的就是同一件事, 官方已验证思路可行。
     */
    private void patchFullbright() {
        Logger.print("Patching IsoGridSquare/RenderSettings with fullbright hooks...");
        try {
            Patch.injectIntoClass("zombie/iso/IsoGridSquare", "getVertLight", false, method -> {
                if (!method.desc.equals("(II)I")) {
                    return;
                }
                /*
                 * 分支-free 注入 (2026-08-25 重写): 旧版内联 if(isFullbright) return -1
                 * 需要 JumpInsnNode+LabelNode 新分支目标, SafeClassWriter(2)=COMPUTE_FRAMES
                 * 重算整类帧时 Frame.merge 对无关类型合并 (org/joml/Vector3f <>
                 * zombie/vehicles/BaseVehicle) 抛 "Index -1 out of bounds" →
                 * IsoGridSquare.class 从未成功落盘, 组①②从未生效 (MP 纯客户端
                 * 室内黑的根因之一)。改为: 头部无条件压入覆盖值 (-1 全亮/0 不干预),
                 * 原 IRETURN 前插 IOR 位合并 — -1|x=-1, 0|x=x, 零新分支零新帧。
                 * 条件逻辑在 FullbrightHook.vertLightOverride() 内。
                 */
                InsnList hookInstructions = new InsnList();
                hookInstructions.add(new MethodInsnNode(184, "modcore/core/FullbrightHook", "vertLightOverride", "()I", false));
                method.instructions.insert(hookInstructions);
                AbstractInsnNode returnInsn = method.instructions.getLast();
                while (returnInsn != null && returnInsn.getOpcode() != 172) {
                    returnInsn = returnInsn.getPrevious();
                }
                if (returnInsn == null) {
                    throw new IllegalStateException("IRETURN not found in IsoGridSquare.getVertLight");
                }
                method.instructions.insertBefore(returnInsn, new InsnNode(128));   // IOR: 覆盖值|原值
                Logger.print("  [OK] Injected fullbright white into IsoGridSquare.getVertLight()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: getVertLight injection failed: " + e.getMessage());
            Logger.logException(e);
        }
        try {
            Patch.injectIntoClass("zombie/iso/IsoGridSquare", "cacheLightInfo", false, method -> {
                if (!method.desc.equals("()V")) {
                    return;
                }
                AbstractInsnNode returnInsn = method.instructions.getLast();
                while (returnInsn != null && returnInsn.getOpcode() != 177) {
                    returnInsn = returnInsn.getPrevious();
                }
                if (returnInsn == null) {
                    throw new IllegalStateException("RETURN not found in IsoGridSquare.cacheLightInfo");
                }
                InsnList toInject = new InsnList();
                /*
                 * 分支-free 注入 (2026-08-25 重写, 理由同组①): 旧版内联门禁
                 * (JumpInsnNode+LabelNode) + astore_2 局部槽写法在 COMPUTE_FRAMES 下
                 * 帧重算越界, IsoGridSquare.class 从未成功落盘。改为尾部无条件调用
                 * FullbrightHook.cacheLightInfoHook(this) — 条件在助手方法内部,
                 * 注入体零分支零标签零新局部。
                 */
                toInject.add(new VarInsnNode(25, 0));
                toInject.add(new MethodInsnNode(184, "modcore/core/FullbrightHook", "cacheLightInfoHook", "(Lzombie/iso/IsoGridSquare;)V", false));
                method.instructions.insertBefore(returnInsn, toInject);
                Logger.print("  [OK] Injected fullbright white into IsoGridSquare.cacheLightInfo()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: cacheLightInfo injection failed: " + e.getMessage());
            Logger.logException(e);
        }
        try {
            Patch.injectIntoClass("zombie/core/opengl/RenderSettings$PlayerRenderSettings", "updateRenderSettings", false, method -> {
                if (!method.desc.equals("(ILzombie/characters/IsoPlayer;)V")) {
                    return;
                }
                AbstractInsnNode returnInsn = method.instructions.getLast();
                while (returnInsn != null && returnInsn.getOpcode() != 177) {
                    returnInsn = returnInsn.getPrevious();
                }
                if (returnInsn == null) {
                    throw new IllegalStateException("RETURN not found in PlayerRenderSettings.updateRenderSettings");
                }
                InsnList toInject = new InsnList();
                LabelNode continueLabel = new LabelNode();
                toInject.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add(new JumpInsnNode(198, continueLabel));
                toInject.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                toInject.add(new JumpInsnNode(198, continueLabel));
                toInject.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                toInject.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                toInject.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isFullbright", "Z"));
                toInject.add(new JumpInsnNode(153, continueLabel));
                // 夜色全局参数清零: ambient=1 / night=0 / darkness=0 / rgb mod=1 /
                // blendIntensity=0 / desaturation=0 (字段私有但注入发生在同类内, 可直写)
                for (Object[] fieldAndValue : new Object[][]{{"ambient", Float.valueOf(1.0f)}, {"night", Float.valueOf(0.0f)}, {"darkness", Float.valueOf(0.0f)}, {"rmod", Float.valueOf(1.0f)}, {"gmod", Float.valueOf(1.0f)}, {"bmod", Float.valueOf(1.0f)}, {"blendIntensity", Float.valueOf(0.0f)}, {"desaturation", Float.valueOf(0.0f)}}) {
                    toInject.add(new VarInsnNode(25, 0));
                    toInject.add(new LdcInsnNode(fieldAndValue[1]));
                    toInject.add(new FieldInsnNode(181, "zombie/core/opengl/RenderSettings$PlayerRenderSettings", (String)fieldAndValue[0], "F"));
                }
                toInject.add(continueLabel);
                method.instructions.insertBefore(returnInsn, toInject);
                Logger.print("  [OK] Injected fullbright globals into RenderSettings$PlayerRenderSettings.updateRenderSettings()");
            });
        }
        catch (Exception e) {
            Logger.print("Warning: updateRenderSettings injection failed: " + e.getMessage());
            Logger.logException(e);
        }
        try {
            /*
             * ④ 可见性位与黑暗系数 (室内全亮的关键): 颜色三注入只解决"画出来的东西是白的",
             * 但无灯房间 native 把玩家对这些格子的 canSee/couldSee 判为 false, 渲染侧据此
             * *直接不画*: FBORenderCutaways(:729/:736/:1460/:1515/:1529) 对 !isCouldSee 的
             * 方块跳过裁剪绘制, renderFloorInternal(:7010) 对 darkMulti<0.5 的非本房间格子
             * 把地板 alpha 归零, :7560 对面墙 alpha=darkMulti*2 —— 这就是"户外全亮、室内
             * 无灯依旧漆黑一片"的根因 (户外夜晚有天光 => canSee 全真 => 照常绘制成白色)。
             * 强制 bCanSee/bCouldSee=true + darkMulti/targetDarkMulti=1, 几何体一律照常绘制。
             * 只动渲染侧: bSeen 不碰 (保留地图探索/Meta 统计), 服务端 LOS 走独立 ServerLOS,
             * 僵尸 AI 用自身感知 (spotted/vision cone) 不读方块 canSee, 多人零上行影响。
             * 2026-08-25 补: **listen 服房主** (GameServer.server=true 同进程) 的方块
             * lighting[0] 是 ServerLOS.ServerLighting 而非 JNILighting (IsoGridSquare:3992),
             * 其 darkMulti() 硬编码返 0 (ServerLOS.java:381)、bCouldSee 来自服务端 LOS 线程
             * 真实计算 (暗房间=false) → 房主渲染"户外亮、室内依旧漆黑" (纯客户端连
             * dedicated 服不受影响, 走 JNILighting)。对 ServerLighting 同四 getter 做同款
             * 注入; bSeen 不碰, 服务端 LOS 线程走 setter 写入端不受影响, 且反编译核实
             * isCanSee/isCouldSee 无服务端逻辑调用方 (ServerLOS.isCouldSee(player,sq) 读
             * 自己的 PlayerData.visible 数组), 房主服务端玩法行为不变。
             */
            String[][] visMethods = new String[][]{{"bCanSee", "()Z"}, {"bCouldSee", "()Z"}, {"darkMulti", "()F"}, {"targetDarkMulti", "()F"}};
            String[] visClasses = new String[]{"zombie/iso/LightingJNI$JNILighting", "zombie/network/ServerLOS$ServerLighting"};
            for (final String visClass : visClasses) {
                for (final String[] spec : visMethods) {
                    final boolean isFloat = spec[1].equals("()F");
                    Patch.injectIntoClass(visClass, spec[0], false, method -> {
                        if (!method.desc.equals(spec[1])) {
                            return;
                        }
                        InsnList hookInstructions = new InsnList();
                        LabelNode continueLabel = new LabelNode();
                        hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                        hookInstructions.add(new JumpInsnNode(198, continueLabel));
                        hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                        hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                        hookInstructions.add(new JumpInsnNode(198, continueLabel));
                        hookInstructions.add(new MethodInsnNode(184, "modcore/core/CoreMain", "getInstance", "()Lmodcore/core/CoreMain;", false));
                        hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreMain", "CoreAPI", "Lmodcore/core/CoreAPI;"));
                        hookInstructions.add(new FieldInsnNode(180, "modcore/core/CoreAPI", "isFullbright", "Z"));
                        hookInstructions.add(new JumpInsnNode(153, continueLabel));
                        hookInstructions.add(new InsnNode(isFloat ? 13 : 4));
                        hookInstructions.add(new InsnNode(isFloat ? 174 : 172));
                        hookInstructions.add(continueLabel);
                        method.instructions.insert(hookInstructions);
                    });
                }
                Logger.print("  [OK] Injected fullbright visibility into " + visClass + " (bCanSee/bCouldSee/darkMulti/targetDarkMulti)");
            }
        }
        catch (Exception e) {
            Logger.print("Warning: JNILighting visibility injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    private void patchAbstractAntiCheatValidation() {
        Patch.injectIntoClass("zombie/network/anticheats/AbstractAntiCheat", "validate", false, method -> {
            InsnList hookInstructions = new InsnList();
            LabelNode continueLabel = new LabelNode();
            hookInstructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
            hookInstructions.add((AbstractInsnNode)new InsnNode(1));
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/ServerAntiCheatBypass", "hookValidation", "(Ljava/lang/Object;Lzombie/characters/IsoPlayer;)Z", false));
            hookInstructions.add((AbstractInsnNode)new JumpInsnNode(153, continueLabel));
            hookInstructions.add((AbstractInsnNode)new InsnNode(1));
            hookInstructions.add((AbstractInsnNode)new InsnNode(176));
            hookInstructions.add((AbstractInsnNode)continueLabel);
            method.instructions.insert(hookInstructions);
            Logger.print("  [OK] Injected validation hook into AbstractAntiCheat.validate()");
        });
    }

    private void patchSuspiciousActivityReporting() {
        Patch.injectIntoClass("zombie/network/anticheats/SuspiciousActivity", "report", false, method -> {
            InsnList hookInstructions = new InsnList();
            LabelNode continueLabel = new LabelNode();
            hookInstructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
            hookInstructions.add((AbstractInsnNode)new InsnNode(1));
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/ServerAntiCheatBypass", "hookSuspiciousActivity", "(Ljava/lang/Object;Lzombie/characters/IsoPlayer;)Z", false));
            hookInstructions.add((AbstractInsnNode)new JumpInsnNode(153, continueLabel));
            hookInstructions.add((AbstractInsnNode)new InsnNode(3));
            hookInstructions.add((AbstractInsnNode)new InsnNode(172));
            hookInstructions.add((AbstractInsnNode)continueLabel);
            method.instructions.insert(hookInstructions);
            Logger.print("  [OK] Injected activity hook into SuspiciousActivity.report()");
        });
    }

    private void patchKickBanMethods() {
        Patch.injectIntoClass("zombie/network/GameServer", "kickPlayer", false, method -> {
            InsnList hookInstructions = new InsnList();
            LabelNode continueLabel = new LabelNode();
            hookInstructions.add((AbstractInsnNode)new VarInsnNode(25, 1));
            hookInstructions.add((AbstractInsnNode)new LdcInsnNode((Object)"Anti-cheat"));
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "modcore/core/ServerAntiCheatBypass", "hookKickAction", "(Ljava/lang/String;Ljava/lang/String;)Z", false));
            hookInstructions.add((AbstractInsnNode)new JumpInsnNode(153, continueLabel));
            hookInstructions.add((AbstractInsnNode)new InsnNode(177));
            hookInstructions.add((AbstractInsnNode)continueLabel);
            method.instructions.insert(hookInstructions);
            Logger.print("  [OK] Injected kick hook into GameServer.kickPlayer()");
        });
    }

    public boolean checkInjectedAnnotations() {
        boolean foundInFolder;
        Path gameFolderPath = Paths.get("zombie", new String[0]);
        if (Files.exists(gameFolderPath, new LinkOption[0]) && Files.isDirectory(gameFolderPath, new LinkOption[0]) && (foundInFolder = Arrays.stream(this.patchFiles).anyMatch(filePath -> {
            Path fullPath = gameFolderPath.resolve((String)filePath);
            if (Files.exists(fullPath, new LinkOption[0])) {
                return Patch.isInjectedAnnotationPresent(filePath, "zombie");
            }
            return false;
        }))) {
            return true;
        }
        Path jarPath = Paths.get("ProjectZomboid.jar", new String[0]);
        if (Files.exists(jarPath, new LinkOption[0])) {
            Patch.setProjectZomboidJarPath(jarPath.toAbsolutePath().toString());
            return Arrays.stream(this.patchFiles).anyMatch(filePath -> Patch.isInjectedAnnotationPresent(filePath, "zombie"));
        }
        return false;
    }

    public boolean isGameFolder() {
        Path jarPath = Paths.get("ProjectZomboid.jar", new String[0]);
        if (Files.exists(jarPath, new LinkOption[0])) {
            Logger.printLog("Found ProjectZomboid.jar");
            return true;
        }
        Path gameFolderPath = Paths.get("zombie", new String[0]);
        if (Files.exists(gameFolderPath, new LinkOption[0]) && Files.isDirectory(gameFolderPath, new LinkOption[0])) {
            return Arrays.stream(this.patchFiles).allMatch(fileName -> Files.exists(gameFolderPath.resolve((String)fileName), new LinkOption[0]));
        }
        return false;
    }

    public void patchGame() {
        Logger.printCredits();
        Logger.print("Preparing to install the modcore...");
        if (!this.isGameFolder()) {
            Logger.print("No game files were found in this directory. Place the cheat in the root folder of the game");
            return;
        }
        Path jarPath = Paths.get("ProjectZomboid.jar", new String[0]);
        if (Files.exists(jarPath, new LinkOption[0])) {
            Patch.setProjectZomboidJarPath(jarPath.toAbsolutePath().toString());
            Logger.print("Using ProjectZomboid.jar for class loading");
        } else {
            Logger.print("ProjectZomboid.jar not found, falling back to zombie folder");
        }
        Logger.print("Checking for injections in game files");
        if (this.checkInjectedAnnotations()) {
            // 旧版残留: 自动恢复原版类 + 清 modcore 目录, 然后全新安装。
            // (拒绝式重装会留下"新注入体 + 旧钩子类"版本错配 — 新 BaseVehicle 调
            // 新钩子方法, 而旧 BulletNoClipHook.class 没有它 → 启动即
            // NoSuchMethodError 卡死, 实测。安装器必须永远 fresh。)
            Logger.print("Previous injection detected — refreshing installation");
            this.restoreFiles();
        }
        Logger.print("No signs of injections were found. Preparing for backup...");
        this.backupGameFiles();
        // modcore 类解包提前到注入前 (原在末尾): 安装中途失败时, 磁盘上的钩子类
        // 与已注入体永远同版本, 不会错配 (NoSuchMethodError 实测教训)
        Logger.print("Extracting modcore files to the current directory...");
        this.extractmodcore();
        Logger.print("Preparation for injection into game file...");
        this.exposePrivateFields();
        this.patchGameWindow();
        this.patchItemContainer();
        this.patchCombatSpeed();
        this.patchAttackRange();
        this.patchLuaEventManager();
        this.patchLuaManager();
        this.patchAntiCheatSystem();
        this.patchHeadshotOnly();
        this.patchAlwaysHit();
        // 超级群攻: maxHit 扩容 + 全向扇面 + 属主命中包抑制 + 群攻全额伤害 (研判 §三A/§五/§六/§九)
        this.patchSuperMultiHit();
        this.patchSuperMultiHitSuppression();
        this.patchSuperMultiHitFullDamage();
        this.patchGameClientSyncBlocker();
        this.patchRoleCapabilityForSP();
        this.patchVehicleNoKey();
        this.patchZombieSetTarget();
        this.patchZombieSpotted();
        this.patchZombieShouldAttack();
        this.patchCharacterCreationBoost();
         this.patchApplyTraitsSP();
        this.patchFullbright();
        // 自动驾驶: 唯一补丁点 CarController.updateControls 门控 (drive/CarControllerPatch)
        modcore.drive.CarControllerPatch.install();
        // 伪·自动驾驶: 世界碰撞豁免 IsoChunk.calcPhysics 过滤 (drive/BulletNoClipPatch)
        modcore.drive.BulletNoClipPatch.install();
        Patch.saveModifiedClasses();
        // L4 收口: 删除 .bkup 磁盘残留 (还原已不依赖备份, 见 cleanupBackups)
        this.cleanupBackups();
        Logger.print("The injections were completed!");
        Logger.print("The cheat installation is complete, you can enter the game!");
    }

    public void restoreFiles() {
        Logger.printCredits();
        Logger.print("Restoring files...");
        Path currentPath = Paths.get("", new String[0]).toAbsolutePath();
        for (int i = 0; i < this.patchFiles.length; ++i) {
            String fileName = this.patchFiles[i];
            String iteration = "[" + (i + 1) + "/" + this.patchFiles.length + "]";
            Logger.print("Restoring the file '" + fileName + "' " + iteration);
            Path originalFilePath = Paths.get(currentPath.toString(), "zombie", this.patchFiles[i]);
            Path backupFilePath = Paths.get(originalFilePath.toString() + ".bkup", new String[0]);
            if (Files.exists(backupFilePath, new LinkOption[0])) {
                try {
                    if (Files.exists(originalFilePath, new LinkOption[0])) {
                        Files.delete(originalFilePath);
                    }
                    Files.move(backupFilePath, originalFilePath, new CopyOption[0]);
                }
                catch (IOException e) {
                    Logger.print("Error when restoring the game file '" + fileName + "': " + e.getMessage());
                }
                continue;
            }
            // 无备份 (新版安装不再保留 .bkup): 补丁文件直接从磁盘删除 —
            // 原版类永远可从 projectzomboid.jar 载入, 删除即回到干净状态
            if (Files.exists(originalFilePath, new LinkOption[0])) {
                try {
                    Files.delete(originalFilePath);
                    Logger.print("No backup for '" + fileName + "'; removed patched file (pristine source = game jar)");
                }
                catch (IOException e) {
                    Logger.print("Error removing patched file '" + fileName + "': " + e.getMessage());
                }
            } else {
                Logger.print("Nothing to restore for '" + fileName + "'");
            }
        }
        Logger.print("Files restoration completed!");
        this.uninstallmodcoreFiles();
    }

    /**
     * L4 磁盘痕迹收口 (2026-09-10 审计): 安装成功后删除全部 .bkup。
     * .bkup 仅服务"卸载还原", 而原版类始终可从 projectzomboid.jar 载入 —
     * 残留的 .bkup (17 个原版类副本) 只是额外的磁盘指纹。还原路径已改为
     * "无备份则删补丁文件" (见 restoreFiles), 故备份不再需要。
     */
    private void cleanupBackups() {
        Path zombieDir = Paths.get("zombie");
        if (!Files.exists(zombieDir, new LinkOption[0])) {
            return;
        }
        int removed = 0;
        try (java.util.stream.Stream<Path> walk = Files.walk(zombieDir)) {
            java.util.List<Path> bkups = walk
                .filter(p -> p.toString().endsWith(".bkup"))
                .collect(java.util.stream.Collectors.toList());
            for (Path p : bkups) {
                Files.deleteIfExists(p);
                ++removed;
            }
        }
        catch (IOException e) {
            Logger.print("Backup cleanup warning: " + e.getMessage());
            return;
        }
        if (removed > 0) {
            Logger.print("Removed " + removed + " .bkup file(s) (superseded: restore uses game jar)");
        }
    }
}
