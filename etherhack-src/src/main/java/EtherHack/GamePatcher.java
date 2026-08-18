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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GamePatcher {
    private final String[] patchFiles = new String[]{"GameWindow.class", "inventory/ItemContainer.class", "Lua/LuaEventManager.class", "Lua/LuaManager.class", "characters/IsoGameCharacter.class", "network/GameClient.class", "CombatManager.class"};
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
