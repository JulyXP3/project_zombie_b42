package modcore.agent;

/** Historical entry retained only to explain migration; it cannot launch the game. */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        System.err.println("The premain/custom-loader entry was retired. Reinstall and use the controlled launch gate.");
        System.exit(1);
    }
}
