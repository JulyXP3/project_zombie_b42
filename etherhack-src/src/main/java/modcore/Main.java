/*
 * Decompiled with CFR 0.152.
 */
package modcore;

import modcore.GamePatcher;
import modcore.utils.Logger;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            Logger.print("You must specify one of the '--install' or '--uninstall' flags");
            return;
        }
        GamePatcher gamePatcher = new GamePatcher();
        switch (args[0]) {
            case "--install": {
                gamePatcher.patchGame();
                break;
            }
            case "--uninstall": {
                gamePatcher.restoreFiles();
                break;
            }
            default: {
                Logger.print("Unknown flag '" + args[0] + "'");
            }
        }
    }
}
