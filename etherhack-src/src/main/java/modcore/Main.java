package modcore;

/** Offline preparation and explicit, reversible deployment entry. */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("--install-agent") || args[0].equals("--uninstall-agent"))) {
            try {
                modcore.build.AgentInstaller.run(args);
            } catch (Exception e) {
                System.err.println("Agent deployment failed: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }
        if (args.length == 4 && (args[0].equals("--prepare-agent") || args[0].equals("--prepare-runtime"))) {
            try {
                modcore.attach.JdkRequirement.requireCurrent();
                if (args[0].equals("--prepare-runtime")) {
                    modcore.build.AgentLoadout.prepareRuntime(java.nio.file.Path.of(args[1]),
                            java.nio.file.Path.of(args[2]), java.nio.file.Path.of(args[3]));
                } else {
                    modcore.build.AgentLoadout.prepare(java.nio.file.Path.of(args[1]),
                            java.nio.file.Path.of(args[2]), java.nio.file.Path.of(args[3]));
                }
            } catch (Exception e) {
                System.err.println("Agent preparation failed: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }
        System.err.println("Legacy install/uninstall commands are disabled in this preview build.");
        System.err.println("Use the three-file installer or --prepare-runtime <game.jar> <payload.jar> <runtime.jar>.");
        System.err.println("Review old deployment files manually; this tool will not remove them.");
        System.exit(1);
    }
}
