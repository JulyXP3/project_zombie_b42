package carkill;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

public final class CarKillAgent {
    private CarKillAgent() {
    }

    public static void premain(String args, Instrumentation inst) {
        install(inst, "Loaded at startup");
    }

    public static void agentmain(String args, Instrumentation inst) {
        install(inst, "Attached to process");
    }

    private static void install(Instrumentation inst, String how) {
        CarKillInjector.install(inst);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            System.err.println("Usage: java -jar car_kill.jar [pid]");
            System.exit(2);
        }
        String pid = args.length == 1 ? args[0] : findPZ();
        if (pid == null || pid.isBlank()) {
            System.err.println("[CarKill] ProjectZomboid process not found. Pass its PID.");
            System.exit(1);
        }
        Path agent = Path.of(CarKillAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        System.out.println("[CarKill] Target PID: " + pid);
        System.out.println("[CarKill] Agent: " + agent.toAbsolutePath());
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            vm.loadAgent(agent.toAbsolutePath().toString());
        } finally {
            vm.detach();
        }
        System.out.println("[CarKill] Agent loaded. Press \\ in game to toggle.");
    }

    private static String findPZ() {
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String name = descriptor.displayName();
            if (name != null && name.toLowerCase().contains("projectzomboid")) {
                return descriptor.id();
            }
        }
        return null;
    }
}
