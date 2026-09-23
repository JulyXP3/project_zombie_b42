/*
 * Red-team helper: scan ENABLED mods for building classes that can ride
 * the free-build chain (放宽标准版, 一百七十六; 已启用限定, 一百七十七).
 *
 * What it does: effective enabled set (MP clients: server-sent serverMods,
 * because ActiveMods "loaded" is never populated for GameClient;
 * otherwise ActiveMods loaded) -> ChooseGameInfo mod dirs
 * (workshop auto-resolved by the game) -> walk each media/lua tree
 * (read-only), match ISBuildingObject derive declarations plus their
 * function new signatures, return Type/ctor/mod/file lines.
 * Disabled mods are never touched (their classes are not loaded anyway).
 *
 * What it does NOT do (by design): no sprite discovery (impossible
 * statically), no placeability verdict (needs empirical test). Results are
 * candidates: user fills sprites into BuildCatalogCustom.lua (format in that
 * file's header) and tests placement. Listed != guaranteed.
 */
package modcore.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.gameStates.ChooseGameInfo;
import zombie.modding.ActiveMods;
import zombie.network.GameClient;
import modcore.utils.Logger;

public final class ModScanAPI {

    private static final Pattern CLASS_RE =
        Pattern.compile("(\\w+)\\s*=\\s*(\\w+)\\s*:\\s*derive\\s*\\(\\s*\"(\\w+)\"\\s*\\)");
    private static final Pattern ACCEPTED_BASE = Pattern.compile("ISBuildingObject|ISBuildIsoEntity");
    private static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_FILES = 4000;

    // Cache: mod list hash -> scan result. Mod list is stable within a session.
    private static String cachedModHash = null;
    private static String cachedResult = null;

    private ModScanAPI() {
    }

    @LuaMethod(name = "clearModScanCache", global = true)
    public static void clearModScanCache() {
        cachedModHash = null;
        cachedResult = null;
    }

    /**
     * @return "Type|ctorParams|baseClass|modId/relativeFile" lines joined by \n,
     *         or "error: ..." / "no enabled mods" short strings.
     *         Only mods in the effective enabled set are scanned; disabled
     *         mods are skipped (their classes are not loaded anyway).
     *         MP clients never populate ActiveMods "loaded"
     *         (ZomboidFileSystem.loadMods returns early for GameClient),
     *         so on clients the server-sent serverMods list is used instead.
     */
    @LuaMethod(name = "scanModBuildings", global = true)
    public static String scanModBuildings() {
        try {
            List<String> modIds = null;
            if (GameClient.client && GameClient.connection != null
                    && GameClient.instance != null && GameClient.instance.serverMods != null
                    && !GameClient.instance.serverMods.isEmpty()) {
                modIds = new ArrayList<String>(GameClient.instance.serverMods);
            } else {
                try {
                    modIds = ActiveMods.getById("loaded").getMods();
                }
                catch (Throwable t) {
                    return "error: ActiveMods unreadable: " + String.valueOf(t.getMessage());
                }
            }
            if (modIds == null || modIds.isEmpty()) {
                return "no enabled mods";
            }
            // Cache hit: mod list unchanged -> return previous result instantly.
            List<String> sorted = new ArrayList<String>(modIds);
            Collections.sort(sorted);
            String modHash = String.join("|", sorted);
            if (modHash.equals(cachedModHash) && cachedResult != null) {
                return cachedResult;
            }
            final Map<String, String> ctors = new LinkedHashMap<String, String>();
            final Map<String, String> bases = new LinkedHashMap<String, String>();
            final Map<String, String> files = new LinkedHashMap<String, String>();
            int luaCount = 0;
            final StringBuilder debug = new StringBuilder("[ModScan] enabled=");
            for (String modId : modIds) {
                debug.append(modId).append(';');
            }
            debug.append(" dirs=");
            for (String modId : modIds) {
                String dir = null;
                try {
                    ChooseGameInfo.Mod info = ChooseGameInfo.getModDetails(modId);
                    if (info != null) {
                        dir = info.getDir();
                    }
                }
                catch (Throwable t) {
                    // ignore
                }
                if (dir == null) {
                    // Fallback 1: direct mods/<modId> folder (bypass ChooseGameInfo)
                    Path gameDir = Paths.get(System.getProperty("user.dir"));
                    Path direct = gameDir.resolve("mods").resolve(modId);
                    if (Files.isDirectory(direct)) {
                        dir = direct.toString();
                    } else {
                        // Fallback 2: search workshop/content/108600/*/mods/<modId>
                        Path steamApps = gameDir.getParent().getParent();
                        Path workshopRoot = steamApps.resolve("workshop").resolve("content").resolve("108600");
                        if (Files.isDirectory(workshopRoot)) {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workshopRoot)) {
                                for (Path wsDir : stream) {
                                    Path modsDir = wsDir.resolve("mods").resolve(modId);
                                    if (Files.isDirectory(modsDir)) {
                                        dir = modsDir.toString();
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                    if (dir == null) {
                        debug.append(modId).append("=>null;");
                        continue;
                    }
                }
                debug.append(modId).append("=>").append(dir).append(';');
                List<Path> roots = new ArrayList<Path>();
                Path luaRoot = Paths.get(dir, "media", "lua");
                if (Files.isDirectory(luaRoot)) {
                    roots.add(luaRoot);
                } else {
                    // PZ versioned mods: media/lua may live under 42.0/, 42.15/, common/
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
                        for (Path child : stream) {
                            if (Files.isDirectory(child)) {
                                Path candidate = child.resolve("media").resolve("lua");
                                if (Files.isDirectory(candidate)) {
                                    roots.add(candidate);
                                }
                            }
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
                debug.append("roots=").append(roots.size()).append(';');
                if (roots.isEmpty()) {
                    debug.append("(no_lua);");
                    continue;
                }
                final List<Path> luaFiles = new ArrayList<Path>();
                final String id = modId;
                for (Path root : roots) {
                    try {
                        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (luaFiles.size() >= MAX_FILES) {
                                    return FileVisitResult.TERMINATE;
                                }
                                if (file.toString().endsWith(".lua") && attrs.size() < MAX_FILE_BYTES) {
                                    luaFiles.add(file);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                    catch (IOException e) {
                        debug.append(id).append("=>").append(root.getFileName()).append(":err;");
                    }
                }
                debug.append(id).append("=>").append(luaFiles.size()).append("files;");
                luaCount += luaFiles.size();
                for (Path file : luaFiles) {
                    String text;
                    try {
                        text = new String(Files.readAllBytes(file), Charset.forName("UTF-8"));
                    }
                    catch (IOException e) {
                        continue;
                    }
                    Matcher m = CLASS_RE.matcher(text);
                    while (m.find()) {
                        String cls = m.group(1);
                        String base = m.group(2);
                        if (!ACCEPTED_BASE.matcher(base).find()) {
                            continue;
                        }
                        if (ctors.containsKey(cls)) {
                            continue;
                        }
                        ctors.put(cls, ctorOf(text, cls));
                        bases.put(cls, base);
                        files.put(cls, id + "/" + luaRoot.relativize(file).toString());
                    }
                }
            }
            if (ctors.isEmpty()) {
                Logger.printLog("[ModScan] debug: " + debug.toString());
                cachedResult = "none found in " + modIds.size() + " enabled mods (" + luaCount + " lua files)";
                cachedModHash = modHash;
                return cachedResult;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : ctors.entrySet()) {
                sb.append(e.getKey()).append('|')
                  .append(e.getValue()).append('|')
                  .append(bases.get(e.getKey())).append('|')
                  .append(files.get(e.getKey())).append('\n');
            }
            String out = sb.toString();
            Logger.printLog("[ModScan] debug: " + debug.toString());
            Logger.printLog("[ModScan] " + ctors.size() + " candidates:\n" + out);
            cachedResult = out;
            cachedModHash = modHash;
            return out;
        }
        catch (Throwable t) {
            Logger.printLog("[ModScan] error: " + t);
            return "error: " + String.valueOf(t.getMessage());
        }
    }

    /** First single-line `function Cls:new(...)` signature, "" if none/multiline. */
    private static String ctorOf(String text, String cls) {
        Pattern p = Pattern.compile(
            "function\\s+" + Pattern.quote(cls) + "\\s*:\\s*new\\s*\\(([^)\\n]*)\\)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }
}
