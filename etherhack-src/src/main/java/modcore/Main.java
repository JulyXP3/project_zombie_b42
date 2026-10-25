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
                // C (2026-09-14 八十二): 有类补丁失败时以非 0 退出码收尾, install.bat 据此
                // 报错并保留安装器 jar (旧版一律退 0 → 装坏了也显示 "Installation completed")
                if (!gamePatcher.patchGame()) {
                    Logger.error("Installation FAILED - see the messages above; the installer jar was kept for retry.");
                    System.exit(1);
                }
                break;
            }
            case "--uninstall": {
                gamePatcher.restoreFiles();
                // 二百七十: 卸载零留痕 — 用户目录 modcore 树 (日志/配置) 最后深清
                gamePatcher.purgeUserData();
                break;
            }
            default: {
                Logger.print("Unknown flag '" + args[0] + "'");
            }
        }
    }
}
