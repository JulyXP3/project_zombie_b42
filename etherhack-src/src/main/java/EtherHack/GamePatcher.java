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
package EtherHack;

import EtherHack.Main;
import EtherHack.utils.Info;
import EtherHack.utils.Logger;
import EtherHack.utils.Patch;
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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GamePatcher {
    private final String[] patchFiles = new String[]{"GameWindow.class", "inventory/ItemContainer.class", "Lua/LuaEventManager.class", "Lua/LuaManager.class", "characters/IsoGameCharacter.class", "network/GameClient.class", "CombatManager.class", "characters/Role.class", "vehicles/BaseVehicle.class", "characters/IsoZombie.class", "network/packets/character/CreatePlayerPacket.class", "iso/IsoGridSquare.class", "core/opengl/RenderSettings$PlayerRenderSettings.class", "iso/LightingJNI$JNILighting.class", "network/ServerLOS$ServerLighting.class"};
    private final String gameClassFolder = "zombie";
    private final String whiteListPathEtherFiles = "EtherHack";

    public void extractEtherHack() {
        try {
            String jarFilePath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            Path currentDirectory = Paths.get(System.getProperty("user.dir"), new String[0]);
            try (JarFile jarFile = new JarFile(jarFilePath);){
                jarFile.stream().filter(entry -> entry.getName().startsWith("EtherHack")).forEach(entry -> {
                    block9: {
                        try {
                            Path extractPath = currentDirectory.resolve(entry.getName());
                            if (entry.isDirectory()) {
                                Files.createDirectories(extractPath, new FileAttribute[0]);
                                break block9;
                            }
                            Files.createDirectories(extractPath.getParent(), new FileAttribute[0]);
                            try (InputStream inputStream = jarFile.getInputStream((ZipEntry)entry);){
                                Files.copy(inputStream, extractPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        catch (IOException e) {
                            Logger.print("Error extracting EtherHack files: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                Logger.print("Extraction completed successfully");
            }
        }
        catch (IOException | URISyntaxException e) {
            Logger.print("CRITICAL: Failed to extract EtherHack: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to extract EtherHack", e);
        }
    }

    public void uninstallEtherHackFiles() {
        Logger.print("Deleting all EtherHack files...");
        try {
            Path currentDirectory = Paths.get(System.getProperty("user.dir"), new String[0]);
            Path targetPath = currentDirectory.resolve("EtherHack");
            if (Files.exists(targetPath, new LinkOption[0])) {
                Files.walk(targetPath, new FileVisitOption[0]).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
            Logger.print("Deletion EtherHack files completed successfully");
        }
        catch (IOException except) {
            Logger.print("Failed to delete EtherHack files: " + except.getMessage());
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
            String newTitle = "Project Zomboid" + Info.CHEAT_WINDOW_TITLE_SUFFIX;
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
            InsnList allInstructions = new InsnList();
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode catchStart = new LabelNode();
            LabelNode afterCatch = new LabelNode();
            allInstructions.add((AbstractInsnNode)tryStart);
            allInstructions.add((AbstractInsnNode)new LdcInsnNode((Object)"Starting EtherHack initialization..."));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/utils/Logger", "printLog", "(Ljava/lang/String;)V", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherLuaCompiler", "getInstance", "()LEtherHack/Ether/EtherLuaCompiler;", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "EtherHack/Ether/EtherLuaCompiler", "init", "()V", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherLogo", "getInstance", "()LEtherHack/Ether/EtherLogo;", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "EtherHack/Ether/EtherLogo", "init", "()V", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "EtherHack/Ether/EtherMain", "init", "()V", false));
            allInstructions.add((AbstractInsnNode)tryEnd);
            allInstructions.add((AbstractInsnNode)new JumpInsnNode(167, afterCatch));
            allInstructions.add((AbstractInsnNode)catchStart);
            allInstructions.add((AbstractInsnNode)new InsnNode(89));
            allInstructions.add((AbstractInsnNode)new LdcInsnNode((Object)"CRITICAL: Exception during EtherHack initialization in GameWindow.init()"));
            allInstructions.add((AbstractInsnNode)new InsnNode(95));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/utils/Logger", "crash", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false));
            allInstructions.add((AbstractInsnNode)new FieldInsnNode(178, "java/lang/System", "err", "Ljava/io/PrintStream;"));
            allInstructions.add((AbstractInsnNode)new LdcInsnNode((Object)"CRITICAL ERROR in EtherHack initialization:"));
            allInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
            allInstructions.add((AbstractInsnNode)new InsnNode(87));
            allInstructions.add((AbstractInsnNode)afterCatch);
            method.instructions.insert(insertionPoint, allInstructions);
            method.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchStart, "java/lang/Throwable"));
        });
    }

    public void patchItemContainer() {
        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getCapacityWeight", false, method -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(153, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new InsnNode(11));
            newInstructions.add((AbstractInsnNode)new InsnNode(174));
            newInstructions.add((AbstractInsnNode)carryOnLabel);
            method.instructions.insert(newInstructions);
        });
        Patch.injectIntoClass("zombie/inventory/ItemContainer", "getContentsWeight", false, method -> {
            InsnList newInstructions = new InsnList();
            LabelNode carryOnLabel = new LabelNode();
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(198, carryOnLabel));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            newInstructions.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isUnlimitedCarry", "Z"));
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
            toInject.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
            toInject.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
            toInject.add((AbstractInsnNode)new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "combatSpeedMultiplier", "F"));
            toInject.add((AbstractInsnNode)new InsnNode(106));
            method.instructions.insertBefore(returnInsn, toInject);
            Logger.print("  [OK] Injected combat speed multiplier into IsoGameCharacter.calculateCombatSpeed()");
        });
    }

    public void patchLuaEventManager() {
        Patch.injectIntoClass("zombie/Lua/LuaEventManager", "triggerEvent", true, method -> {
            InsnList toInject = new InsnList();
            toInject.add((AbstractInsnNode)new VarInsnNode(25, 0));
            toInject.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/utils/EventSubscriber", "invokeSubscriber", "(Ljava/lang/String;)V", false));
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
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/EtherLuaCompiler", "getInstance", "()LEtherHack/Ether/EtherLuaCompiler;", false));
            newInstructions.add((AbstractInsnNode)new VarInsnNode(25, 0));
            newInstructions.add((AbstractInsnNode)new MethodInsnNode(182, "EtherHack/Ether/EtherLuaCompiler", "isShouldLuaCompile", "(Ljava/lang/String;)Z", false));
            newInstructions.add((AbstractInsnNode)new JumpInsnNode(154, endOfMethodLabel));
            newInstructions.add((AbstractInsnNode)new InsnNode(1));
            newInstructions.add((AbstractInsnNode)new InsnNode(176));
            newInstructions.add((AbstractInsnNode)endOfMethodLabel);
            method.instructions.insert(newInstructions);
        });
    }

    public void exposePrivateFields() {
        Logger.print("=======================================================");
        Logger.print("[DeiClient] Build 42 Version Loaded");
        Logger.print("[DeiClient] WARNING: Most features are experimental!");
        Logger.print("[DeiClient] CONFIRMED WORKING: Item Spawner, Radar/ESP");
        Logger.print("=======================================================");
        Logger.print("[DeiClient] Exposing private fields for direct access...");
        Patch.modifyClass("zombie/characters/IsoPlayer", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[DeiClient] IsoPlayer: " + exposedCount + " fields exposed");
        });
        Patch.modifyClass("zombie/network/GameClient", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[DeiClient] GameClient: " + exposedCount + " fields exposed");
        });
        Patch.modifyClass("zombie/characters/PlayerCheats", classNode -> {
            int exposedCount = 0;
            for (FieldNode field : classNode.fields) {
                if ((field.access & 2) == 0) continue;
                field.access = field.access & 0xFFFFFFFD | 1;
                ++exposedCount;
            }
            Logger.print("[DeiClient] PlayerCheats: " + exposedCount + " fields exposed");
        });
        Logger.print("[DeiClient] Field exposure completed - direct access enabled");
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
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isHeadshotOnly", "Z"));
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

    private void patchGameClientSyncBlocker() {
        Logger.print("Patching GameClient.update with server sync filter...");
        try {
            Patch.injectIntoClass("zombie/network/GameClient", "update", false, method -> {
                InsnList hookInstructions = new InsnList();
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/ServerSyncBlocker", "filterIncomingSyncPackets", "()V", false));
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
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isZombieDontAttack", "Z"));
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
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isZombieDontAttack", "Z"));
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
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isZombieDontAttack", "Z"));
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
                toInject.add(new MethodInsnNode(184, "EtherHack/Ether/CharacterCreationBoost", "apply", "(Ljava/lang/Object;)V", false));
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
     * (服务端才调且无本 mod), 该钩子天然只影响 SP; EtherMain 为空时直通。
     */
    private void patchApplyTraitsSP() {
        Logger.print("Patching IsoGameCharacter.applyTraits with SP creation boost hook...");
        try {
            Patch.injectIntoClass("zombie/characters/IsoGameCharacter", "applyTraits", false, method -> {
                InsnList hookInstructions = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new JumpInsnNode(198, continueLabel));
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                hookInstructions.add(new JumpInsnNode(198, continueLabel));
                hookInstructions.add(new VarInsnNode(25, 0));
                hookInstructions.add(new VarInsnNode(25, 1));
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/CharacterCreationBoost", "applySP", "(Lzombie/characters/IsoGameCharacter;Ljava/util/List;)V", false));
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
     * 视野锥出锥黑幕是独立 overlay (viewConeOpacity, 官方选项), 在 EtherAPI
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
                hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/FullbrightHook", "vertLightOverride", "()I", false));
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
                toInject.add(new MethodInsnNode(184, "EtherHack/Ether/FullbrightHook", "cacheLightInfoHook", "(Lzombie/iso/IsoGridSquare;)V", false));
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
                toInject.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                toInject.add(new JumpInsnNode(198, continueLabel));
                toInject.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                toInject.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                toInject.add(new JumpInsnNode(198, continueLabel));
                toInject.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                toInject.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                toInject.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isFullbright", "Z"));
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
                        hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                        hookInstructions.add(new JumpInsnNode(198, continueLabel));
                        hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                        hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                        hookInstructions.add(new JumpInsnNode(198, continueLabel));
                        hookInstructions.add(new MethodInsnNode(184, "EtherHack/Ether/EtherMain", "getInstance", "()LEtherHack/Ether/EtherMain;", false));
                        hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherMain", "etherAPI", "LEtherHack/Ether/EtherAPI;"));
                        hookInstructions.add(new FieldInsnNode(180, "EtherHack/Ether/EtherAPI", "isFullbright", "Z"));
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
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/ServerAntiCheatBypass", "hookValidation", "(Ljava/lang/Object;Lzombie/characters/IsoPlayer;)Z", false));
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
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/ServerAntiCheatBypass", "hookSuspiciousActivity", "(Ljava/lang/Object;Lzombie/characters/IsoPlayer;)Z", false));
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
            hookInstructions.add((AbstractInsnNode)new MethodInsnNode(184, "EtherHack/Ether/ServerAntiCheatBypass", "hookKickAction", "(Ljava/lang/String;Ljava/lang/String;)Z", false));
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
        Logger.print("Preparing to install the EtherHack...");
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
            Logger.print("Signs of interference were found in the game files. If you have installed this cheat before, run it with the '--uninstall' flag. Otherwise, check the integrity of the game files via Steam");
            return;
        }
        Logger.print("No signs of injections were found. Preparing for backup...");
        this.backupGameFiles();
        Logger.print("Preparation for injection into game file...");
        this.exposePrivateFields();
        this.patchGameWindow();
        this.patchItemContainer();
        this.patchCombatSpeed();
        this.patchLuaEventManager();
        this.patchLuaManager();
        this.patchAntiCheatSystem();
        this.patchHeadshotOnly();
        this.patchGameClientSyncBlocker();
        this.patchRoleCapabilityForSP();
        this.patchVehicleNoKey();
        this.patchZombieSetTarget();
        this.patchZombieSpotted();
        this.patchZombieShouldAttack();
        this.patchCharacterCreationBoost();
         this.patchApplyTraitsSP();
        this.patchFullbright();
        Patch.saveModifiedClasses();
        Logger.print("The injections were completed!");
        Logger.print("Extracting EtherHack files to the current directory...");
        this.extractEtherHack();
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
            Logger.print("Backup file '" + fileName + ".bkup' not found. Skipping restore");
        }
        Logger.print("Files restoration completed!");
        this.uninstallEtherHackFiles();
    }
}
