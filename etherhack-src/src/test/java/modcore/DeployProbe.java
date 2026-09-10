package modcore;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * Smoke deploy-state probe (called by fakegame_smoke.py).
 *
 * Simulates the real game layout: classes unpacked into the game dir,
 * the deploy jar sits in user.dir but is NOT on the classpath, and no
 * Lua file exists on disk (L3 zero-footprint).
 *
 * Validates via reflection into LuaLoader private chain (avoids
 * touching game classes that need zombie.core.Core):
 *   1. openResource: classpath 404 then fallback scan of user.dir
 *      modcore-*.jar zip entries, read all probe Lua sources.
 *   2. rewriteSymbols: rewritten source must not contain original
 *      Ether/UI identifiers in global-assignment positions.
 * Full syntax check for all 55 files is covered by _test_rewriter.py.
 *
 * Any failure exits non-zero.
 */
public class DeployProbe {
    public static void main(String[] args) throws Exception {
        Class<?> loader = Class.forName("modcore.core.LuaLoader");
        Method open = loader.getDeclaredMethod("openResource", String.class);
        open.setAccessible(true);
        Method rewrite = loader.getDeclaredMethod("rewriteSymbols", String.class);
        rewrite.setAccessible(true);

        String[] probes = {
            "modcore/lua/fixes/ReportSanitizer.lua",
            "modcore/lua/fixes/SelfProbe.lua",
            "modcore/lua/modcoreMenu.lua",
            "modcore/lua/components/ui/EtherTheme.lua"
        };
        String[] sigs = {
            "CoreMain", "EtherTheme", "EtherKeyBinds", "UIButton", "EtherFormPanel"
        };

        for (String p : probes) {
            String src;
            try (InputStream in = (InputStream) open.invoke(null, p)) {
                if (in == null) {
                    System.out.println("DEPLOY-PROBE FAIL: openResource null " + p);
                    System.exit(2);
                }
                byte[] buf = in.readAllBytes();
                src = new String(buf, StandardCharsets.UTF_8);
            }
            if (src.isEmpty()) {
                System.out.println("DEPLOY-PROBE FAIL: empty source " + p);
                System.exit(3);
            }
            String out = (String) rewrite.invoke(null, src);
            for (String sig : sigs) {
                if (out.contains("\n" + sig + " ") || out.contains(" " + sig + " =")) {
                    System.out.println("DEPLOY-PROBE FAIL: symbol leak '" + sig + "' in " + p);
                    System.exit(4);
                }
            }
            System.out.println("DEPLOY-PROBE OK: " + p);
        }
        System.out.println("DEPLOY-PROBE PASS");
    }
}
