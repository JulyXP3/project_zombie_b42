package modcore.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites only the launch command of the original game batch files.
 *
 * <p>The executable is deliberately left alone: it starts the unmodified game, so a player can
 * choose between "no mod at all" and "mod loaded" simply by picking the entry point.
 */
public final class LaunchConfiguration {
    private LaunchConfiguration() {}

    public static byte[] editBatch(byte[] input) throws IOException {
        if (input.length > 1024 * 1024) throw new IOException("Launcher batch file is too large");
        String source = new String(input, StandardCharsets.ISO_8859_1);
        // 已经是我们写过的形态：原样返回，不重复注入。
        if (source.contains("modcore.attach.LaunchCoordinator")) return input;
        String prefix = "%USERPROFILE%\\Zomboid\\modcore-agent";
        String bundled = "\"" + prefix + "\\attach-java\\bin\\java.exe\"";
        String selection = "set \"MC_JAVA=\"\r\n"
                + "if defined JAVA_HOME (\r\n  set \"MC_JAVA=%JAVA_HOME%\\bin\\java.exe\"\r\n) else (\r\n"
                + "  for %%J in (java.exe) do set \"MC_JAVA=%%~$PATH:J\"\r\n)\r\n"
                + "if not defined MC_JAVA goto MODCORE_NO_JDK\r\n"
                + "if not exist \"%MC_JAVA%\" goto MODCORE_NO_JDK\r\n"
                + "\"%MC_JAVA%\" -cp \"" + prefix + "\\coordinator.jar\" modcore.attach.JdkRequirement\r\n"
                + "if errorlevel 1 goto MODCORE_NO_JDK\r\n\"%MC_JAVA%\"";
        String result = new String(editBundledBatch(input), StandardCharsets.ISO_8859_1).replace(bundled, selection);
        result = result.replace("exit /b %ERRORLEVEL%", "exit /b %ERRORLEVEL%\r\n:MODCORE_NO_JDK\r\n"
                + "echo [ERROR] Install a Windows x64 full JDK 25. Set JAVA_HOME or add its bin folder to PATH.\r\nexit /b 1");
        return result.getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] editBundledBatch(byte[] input) throws IOException {
        if (input.length > 1024 * 1024) throw new IOException("Launcher batch file is too large");
        String source = new String(input, StandardCharsets.ISO_8859_1);
        if (source.contains("modcore.attach.LaunchCoordinator")) return input;
        // 原版的“启动失败自动重试”外壳：只认这一行，不再要求括号后紧跟换行或固定空行。
        source = Pattern.compile("(?im)^[ \\t]*IF %ERRORLEVEL% NEQ 0 \\([ \\t]*\\r?\\n").matcher(source).replaceAll("");
        Pattern launch = Pattern.compile("(?im)^\"\\.\\\\jre64\\\\bin\\\\java\\.exe\"([^\\r\\n]*?) -cp \"?%PZ_CLASSPATH%\"? zombie\\.gameStates\\.MainScreenState([^\\r\\n]*)(?=\\r?$)");
        Matcher match = launch.matcher(source);
        StringBuilder result = new StringBuilder();
        int count = 0;
        while (match.find()) {
            count++;
            String replacement = "";
            if (count == 1) {
                String tail = match.group(2);
                if (tail.trim().equals("%1 %2")) tail = " %*";
                String prefix = "%USERPROFILE%\\Zomboid\\modcore-agent";
                replacement = "\"" + prefix + "\\attach-java\\bin\\java.exe\" -cp \"" + prefix
                        + "\\coordinator.jar\" modcore.attach.LaunchCoordinator --game-dir \"%~dp0.\" --user-home \"%USERPROFILE%\" --"
                        + match.group(1) + " -cp \"%PZ_CLASSPATH%\" zombie.gameStates.MainScreenState" + tail
                        + "\r\nexit /b %ERRORLEVEL%";
            }
            // 其余同名启动行是原版重试分支的第二次启动：整行去掉，只留一条受控启动。
            match.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        match.appendTail(result);
        if (count == 0) throw new IOException("No game launch line was found in the bundled batch file");
        // 重试分支留下的落单右括号：连同独占一行的 ")" 一起去掉，避免块结构不完整。
        String text = Pattern.compile("(?im)^[ \\t]*\\)[ \\t]*(?=\\r?$)").matcher(result).replaceAll("");
        String normalized = Pattern.compile("(?im)^@?setlocal(?:[ \\t]+enableextensions)?(?:[ \\t]+(?:enable|disable)DelayedExpansion)?[ \\t]*(?=\\r?$)")
                .matcher(text).replaceFirst("@setlocal enableextensions disableDelayedExpansion");
        return normalized.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static byte[] editLegacyBatch(byte[] input) throws IOException {
        if (input.length > 1024 * 1024) throw new IOException("Launcher batch file is too large");
        // The substitutions are ASCII, so this round trip preserves OEM/ANSI comments byte for byte.
        String source = new String(input, StandardCharsets.ISO_8859_1);
        String lower = source.toLowerCase(Locale.ROOT);
        // 已是我们写过的形态（重复安装、旧安装残留、手工拷回的已装 bat）：幂等返回，
        // 不再二次注入，也不因为看到自己的 -javaagent 而拒绝。
        if (lower.contains("modcore.agent.launcher") && lower.contains("-dmodcore.gamejar=")) {
            return input;
        }
        // 放宽：别的 agent / 类加载器参数不再拒绝，原来的 java 参数整段保留。
        // 放宽：允许带引号、行首空白、SET 大小写不同的 PZ_CLASSPATH 写法。
        Pattern assignment = Pattern.compile("(?im)^[ \\t]*SET[ \\t]+\"?PZ_CLASSPATH=\\./;projectzomboid\\.jar\"?[ \\t]*(?=\\r?$)");
        // 放宽：-cp 后的 %PZ_CLASSPATH% 允许带引号。
        Pattern launch = Pattern.compile("(?im)^(\"\\.\\\\jre64\\\\bin\\\\java\\.exe\"[^\\r\\n]*?) -cp \"?%PZ_CLASSPATH%\"? zombie\\.gameStates\\.MainScreenState([^\\r\\n]*)(?=\\r?$)");
        var launches = launch.matcher(source);
        StringBuilder output = new StringBuilder();
        int count = 0;
        while (launches.find()) {
            count++;
            String tail = launches.group(2);
            if (tail.trim().equals("%1 %2")) tail = " %*";
            String replacement = launches.group(1)
                    + " \"-javaagent:%USERPROFILE%\\Zomboid\\modcore-agent\\agent.jar\""
                    + " \"-Dmodcore.gameJar=%~dp0projectzomboid.jar\""
                    + " \"-Dmodcore.payload=%USERPROFILE%\\Zomboid\\modcore.bin\""
                    + " -cp \"%PZ_CLASSPATH%\" modcore.agent.Launcher" + tail;
            launches.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        launches.appendTail(output);
        if (count == 0) throw new IOException("No game launch line was found in the legacy batch file");
        String result = assignment.matcher(output).replaceFirst(Matcher.quoteReplacement(
                "SET \"PZ_CLASSPATH=%USERPROFILE%\\Zomboid\\modcore-agent\\agent.jar\""));
        // 放宽：setlocal 允许带 enable/disableDelayedExpansion，或只有 setlocal；统一压成 disable。
        Pattern local = Pattern.compile("(?im)^@?setlocal(?:[ \\t]+enableextensions)?(?:[ \\t]+(?:enable|disable)DelayedExpansion)?[ \\t]*(?=\\r?$)");
        result = local.matcher(result).replaceFirst("@setlocal enableextensions disableDelayedExpansion");
        return result.getBytes(StandardCharsets.ISO_8859_1);
    }
}
