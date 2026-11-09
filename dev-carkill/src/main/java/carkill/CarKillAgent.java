package carkill;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Locale;

public final class CarKillAgent {
    /** --wait 上限 (秒): 防止脚本永久挂起。 */
    private static final long MAX_WAIT_SECONDS = 600L;

    private CarKillAgent() {
    }

    /** 旧启动方式 (-javaagent) 的入口: 保留兼容, 但会在版本号后留下 agents=1。 */
    public static void premain(String args, Instrumentation inst) {
        install(inst, "Loaded at startup");
    }

    /** 动态挂载入口 (推荐): 手动脚本/attach 用, 命令行不带 -javaagent。 */
    public static void agentmain(String args, Instrumentation inst) {
        install(inst, "Attached to process");
    }

    private static void install(Instrumentation inst, String how) {
        CarKillInjector.install(inst);
    }

    /**
     * 手动挂载入口。
     *
     * <p>用法:
     * <pre>
     *   CarKillAgent [--wait 秒数] [PID]
     * </pre>
     * 不给 PID 时用 VirtualMachine.list() 识别游戏 JVM; 给 --wait 时最多等待指定秒数,
     * 因此可以先跑脚本、再启动游戏。挂载走 VirtualMachine.loadAgent (agentmain),
     * 不向目标 JVM 添加 -javaagent, 游戏完整性报告不会出现 agents 项。
     */
    public static void main(String[] args) throws Exception {
        long waitSeconds = 0L;
        String pid = null;
        boolean diagnose = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--wait")) {
                if (++i >= args.length) usage();
                try {
                    waitSeconds = Long.parseLong(args[i]);
                } catch (NumberFormatException error) {
                    usage();
                }
                if (waitSeconds < 0L || waitSeconds > MAX_WAIT_SECONDS) usage();
            } else if (arg.equals("--help") || arg.equals("-h")) {
                usage();
            } else if (arg.equals("--diagnose") || arg.equals("--list")) {
                diagnose = true;
            } else if (pid == null && arg.matches("\\d+")) {
                pid = arg;
            } else {
                usage();
            }
        }
        if (diagnose) {
            printReport();
            return;
        }
        if (pid == null) {
            pid = findPZ();
            if (pid == null && waitSeconds > 0L) {
                System.out.println("[CarKill] Waiting up to " + waitSeconds + "s for Project Zomboid...");
                long deadline = System.nanoTime() + waitSeconds * 1_000_000_000L;
                while (pid == null && System.nanoTime() < deadline) {
                    Thread.sleep(500L);
                    pid = findPZ();
                }
            }
        }
        if (pid == null || pid.isBlank()) {
            System.err.println("[CarKill] Project Zomboid process not found. Start the game, then run this script again (or pass its PID).");
            printReport();
            System.exit(1);
        }
        Path agent = Path.of(CarKillAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println("[CarKill] Target PID: " + pid);
        System.out.println("[CarKill] Agent: " + agent.toAbsolutePath());
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                boolean alreadyAttached;
                try {
                    alreadyAttached = vm.getSystemProperties()
                            .getProperty(CarKillProtocol.INSTALLED_PROPERTY) != null;
                } catch (Throwable ignored) {
                    alreadyAttached = false;
                }
                if (alreadyAttached) {
                    System.out.println("[CarKill] already attached; nothing to do.");
                    return;
                }
                vm.loadAgent(agent.toAbsolutePath().toString());
            } finally {
                vm.detach();
            }
        } catch (Throwable error) {
            System.err.println("[CarKill] Attach failed: " + error);
            System.exit(1);
        }
        System.out.println("[CarKill] Agent loaded. Press \\ for car kill, = for ammo save.");
    }

    private static void usage() {
        System.err.println("Usage: java -cp car_kill.jar carkill.CarKillAgent [--wait seconds] [--diagnose] [pid]");
        System.exit(2);
    }

    /** 诊断: 列出 attach 可见的 JVM 与 Java 进程, 帮助判断是列表看不到还是显示名不匹配。 */
    private static void printReport() {
        System.out.println("[CarKill] Attachable JVMs (VirtualMachine.list):");
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String name = descriptor.displayName();
            System.out.println("  pid=" + descriptor.id() + "  "
                    + (name == null || name.isEmpty() ? "(empty displayName; likely embedded/JNI launch)" : name));
        }
        System.out.println("[CarKill] Java processes (ProcessHandle, command line may be empty on Windows):");
        for (ProcessHandle process : ProcessHandle.allProcesses().toList()) {
            String command = process.info().command().orElse("");
            String line = process.info().commandLine().orElse("");
            String text = (command + " " + line).toLowerCase(Locale.ROOT);
            if (!text.contains("java")) continue;
            System.out.println("  pid=" + process.pid() + "  " + (line.isEmpty() ? command : line));
        }
        String match = findPZ();
        System.out.println("[CarKill] auto-detect result: " + (match == null ? "not found" : match));
    }

    /**
     * 找 Project Zomboid 的游戏 JVM。
     *
     * <p>Windows 上 Java 的 ProcessHandle.Info.commandLine() 读不到其他 JVM 的命令行
     * (实测为空)，不能用来筛选。改用 VirtualMachine.list() 的 displayName: HotSpot 的
     * displayName = 主类 + 启动参数，既能识别原版 MainScreenState，也能识别本体的
     * LaunchGate，还能把协调器/脚本自身排除掉。
     */
    private static String findPZ() {
        String fallback = null;
        String emptyPid = null;
        boolean multipleEmpty = false;
        long self = ProcessHandle.current().pid();
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            long pid;
            try {
                pid = Long.parseLong(descriptor.id());
            } catch (NumberFormatException error) {
                continue;
            }
            if (pid == self) continue;
            String name = descriptor.displayName();
            String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (lower.contains("coordinator.jar") || lower.contains("launchcoordinator")) continue; // 本体协调器, 不是游戏
            if (lower.contains("car_kill.jar")) continue;                                          // attach 脚本自身
            if (lower.contains("modcore.launch")) return descriptor.id();                          // 本体启动门 = 游戏 JVM
            if (lower.contains("zombie.gamestates.mainscreenstate")
                    || lower.contains("zombie/gamestates/mainscreenstate")) {
                return descriptor.id();                                                            // 原版主类
            }
            if (lower.contains("projectzomboid")) {
                fallback = descriptor.id();
                continue;
            }
            if (lower.isEmpty()) {
                // 原版 EXE 以 JNI 内嵌 JVM 启动时 perf 里没有 sun.java.command, displayName
                // 为空; 本体/协调器通常有名字。只有一个空名候选时视为游戏, 多个则不瞎猜
                // (交给 WMI 或用户传 PID)。
                if (emptyPid == null) emptyPid = descriptor.id();
                else multipleEmpty = true;
            }
        }
        if (fallback != null) return fallback;
        if (emptyPid != null && !multipleEmpty) return emptyPid;
        return null;
    }
}
