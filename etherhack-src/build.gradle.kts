import org.gradle.jvm.tasks.Jar
import java.security.SecureRandom
import java.util.Properties

plugins {
    java
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

fun loadProperties(): Properties {
    return Properties().apply {
        project.file("src/main/resources/modcore/modcore.properties").inputStream().use {
            load(it)
        }
    }
}

group = "modcore"
version = loadProperties().getProperty("version").replace("'", "")

// L2 符号轮换: 每次构建生成随机 Lua 符号前缀 (方案 §8.3-3) — 签名库对
// 上一构建有效, 对当前构建过期。写进 jar 内资源, EtherLuaLoader 运行时读取。
val generateLuaPrefix by tasks.registering {
    doLast {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz"
        val prefix = buildString {
            repeat(4) { append(chars.random()) }
        }
        val outDir = project.file("build/generated/modcore")
        outDir.mkdirs()
        File(outDir, "lua-prefix.properties").writeText("lua.prefix=$prefix\n")
        println("Lua symbol prefix for this build: $prefix")
    }
}

// L2 符号表机械化生成 (2026-09-10 封禁事件修正): 手写 SYMBOL_MAP 漏了
// EtherMain 等 3 个全局 → 裸奔被 KWRR 探测器抓到。改为构建期从 Lua 树
// 机械扫描全局赋值/函数定义目标 (^Ether\w+ / ^UI\w+), 写入
// build/generated/modcore/symbols.txt (每行一个), LuaLoader 静态块读取。
// 判定规则与运行时 rewriteSymbols 全词匹配一致, 漏网→编译产物必然缺失→
// SelfProbe 的 L2 检查仍兜底。
val generateLuaSymbols by tasks.registering {
    inputs.dir(file("src/main/resources/modcore/lua"))
    outputs.file(file("build/generated/modcore/symbols.txt"))
    doLast {
        val luaDir = file("src/main/resources/modcore/lua")
        val symbols = sortedSetOf<String>()
        val reAssign = Regex("""(?m)^\s*(Ether\w+|UI\w+)\s*[=.]""")
        val reFunc = Regex("""(?m)^\s*function\s+(Ether\w+|UI\w+)[\s(.:]""")
        luaDir.walkTopDown().filter { it.extension == "lua" }.forEach { f ->
            val text = f.readText(Charsets.UTF_8)
            reAssign.findAll(text).forEach { symbols.add(it.groupValues[1]) }
            reFunc.findAll(text).forEach { symbols.add(it.groupValues[1]) }
        }
        val out = file("build/generated/modcore/symbols.txt")
        out.parentFile.mkdirs()
        out.writeText(symbols.joinToString("\n") + "\n", Charsets.UTF_8)
        println("L2 symbols generated: ${symbols.size} globals")
    }
}

val generateResourceIndex by tasks.registering {
    inputs.dir(file("src/main/resources/modcore"))
    outputs.file(file("build/generated/modcore/resources.index"))
    doLast {
        val resourceRoot = file("src/main/resources")
        val names = file("src/main/resources/modcore").walkTopDown().filter { it.isFile }
            .map { it.relativeTo(resourceRoot).invariantSeparatorsPath }.toMutableList()
        names.addAll(listOf("modcore/symbols.txt", "modcore/lua-prefix.properties"))
        val output = file("build/generated/modcore/resources.index")
        output.parentFile.mkdirs()
        output.writeText(names.distinct().sorted().joinToString("\n", postfix = "\n"), Charsets.UTF_8)
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateLuaSymbols, generateLuaPrefix, generateResourceIndex)
    from(file("build/generated/modcore/resources.index")) { into("modcore") }
    from(file("build/generated/modcore/symbols.txt")) {
        into("modcore")
    }
    // (二百四十二) 必须用 file(...) 源: generateLuaPrefix 未声明 outputs, from(task)
    // 拷贝 0 个文件 — 曾致 lua-prefix.properties 从未进 L4b 载荷 (前缀退化 q0)。
    from(file("build/generated/modcore/lua-prefix.properties")) {
        into("modcore")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    // 二百六十八 (E1 瘦身): 游戏 jar 只作编译依赖 — 安装期原版类从游戏目录的
    // ProjectZomboid.jar 磁盘读取 (Patch.loadClassNode: setProjectZomboidJarPath,
    // 由 GamePatcher.patchGame 传入 cwd 的游戏 jar), 安装器自身 classpath 用不到它。
    // 旧 implementation 会让 fat jar 解包整份游戏 (66.8MB/26418 条目, 其中 sqlite
    // 原生库 192 条目), 即 2026-09-10 定案"体积即特征"只修了载荷漏掉的安装器本体。
    // 代价: 若 cwd 无游戏 jar 且无 zombie/ 类目录 (游戏无法运行的场景), 类读取兜底
    // 失败 → 安装器以非 0 退出码如实报错, 不会静默装坏。
    compileOnly(files("lib/projectzomboid.jar"))
    testImplementation(files("lib/projectzomboid.jar"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

val launcherSource = sourceSets.create("launcher")
val attachSource = sourceSets.create("attach") {
    compileClasspath += launcherSource.output
    runtimeClasspath += launcherSource.output
}
sourceSets.main { compileClasspath += attachSource.output }
sourceSets.test {
    compileClasspath += launcherSource.output + attachSource.output
    runtimeClasspath += launcherSource.output + attachSource.output
}

val launchGateJar by tasks.registering(Jar::class) {
    destinationDirectory.set(project.file("build/gate"))
    archiveFileName.set("launch-gate.jar")
    from(launcherSource.output)
}

val coordinatorJar by tasks.registering(Jar::class) {
    destinationDirectory.set(project.file("build/gate"))
    archiveFileName.set("coordinator.jar")
    from(attachSource.output)
    from(launcherSource.output) { exclude("module-info.class", "modcore/launch/LaunchGate*.class") }
}

tasks.named<Jar>("jar") {
    dependsOn(launchGateJar, coordinatorJar)
    from(launchGateJar.map { it.archiveFile }) { into("modcore"); rename { "launch-gate.jar" } }
    from(coordinatorJar.map { it.archiveFile }) { into("modcore"); rename { "coordinator.jar" } }
    from(attachSource.output) { include("modcore/attach/JdkRequirement*.class") }
}

tasks.test {
    useJUnitPlatform()
    // 三个部署/安装生命周期用例会真实启动进程、复制运行时目录并整套执行补丁准备,
    // 占测试时间约 85%。-PskipDeploymentTests 供日常迭代跳过它们; 该模式下产出的
    // jar 只是本机自测件, build.bat 会明确提示"未经完整验证", 不是交付包。
    if (project.hasProperty("skipDeploymentTests")) {
        exclude("**/ThreeFileDeploymentTest.class", "**/BatchEntryTest.class",
            "**/PreparedDeploymentTest.class")
    }
    val sandbox = rootDir.parentFile.resolve("temp/headshot-tests")
    workingDir(sandbox.resolve("work"))
    systemProperty("user.home", sandbox.resolve("home").absolutePath)
    systemProperty("java.io.tmpdir", sandbox.resolve("tmp").absolutePath)
    environment("LOCALAPPDATA", sandbox.resolve("appdata").absolutePath)
    systemProperty("headshot.gameJar", file("lib/projectzomboid.jar").absolutePath)
    systemProperty("agent.testRoot", rootDir.parentFile.resolve("temp/instrumentation-tests").absolutePath)
    systemProperty("agent.jarFile", file("build/dist/modcore-agent.jar").absolutePath)
    systemProperty("agent.payloadJar", file("build/slim/modcore-payload.jar").absolutePath)
    systemProperty("agent.launchScript", file("launch-agent.ps1").absolutePath)
    systemProperty("agent.installerJar", file("build/modcore-${version}.jar").absolutePath)
    systemProperty("gate.jarFile", file("build/gate/launch-gate.jar").absolutePath)
    dependsOn("slimJar", "jar", launchGateJar, coordinatorJar)
    doFirst {
        listOf("work", "home", "tmp").forEach { sandbox.resolve(it).mkdirs() }
    }
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(project.file("build"))
    archiveFileName.set("modcore-${version}.jar")

    manifest {
        attributes["Main-Class"] = "modcore.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("modcore/agent/Launcher*.class", "modcore/agent/PayloadClassLoader*.class")

    // L2 随机前缀资源 (generateLuaPrefix 产物)
    from(file("build/generated/modcore")) {
        into("modcore")
    }

    from(configurations.runtimeClasspath.get().map { file ->
        if (file.isDirectory) {
            file
        } else {
            zipTree(file)
        }
    })
}
// The legacy coreboot template is retained for reference, but is not compiled or packaged.
val slimJar by tasks.registering(Jar::class) {
    destinationDirectory.set(project.file("build/slim"))
    archiveFileName.set("modcore-payload.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output) {
        include("modcore/**")
        exclude("modcore/agent/**", "modcore/build/**", "modcore/Main.class", "modcore/GamePatcher*.class",
            "modcore/utils/Patch*.class", "modcore/core/*Patch*.class", "modcore/drive/*Patch*.class",
            "modcore/**/coreboot.class", "modcore/coreboot.class")
    }
}

val agentJar by tasks.registering(Jar::class) {
    destinationDirectory.set(project.file("build/dist"))
    archiveFileName.set("modcore-agent.jar")
    from(sourceSets.main.get().output) {
        include("modcore/agent/**")
        exclude("modcore/agent/Launcher*.class", "modcore/agent/PayloadClassLoader*.class")
    }
    manifest {
        attributes["Agent-Class"] = "modcore.agent.AgentMain"
    }
}

tasks.test { dependsOn(agentJar) }

tasks.named<Jar>("jar") {
    dependsOn(slimJar, agentJar)
    from(slimJar.map { it.archiveFile }) {
        into("modcore")
        rename { "payload.jar" }
    }
    from(agentJar.map { it.archiveFile }) {
        into("modcore")
        rename { "agent.jar" }
    }
}

val packageLoadout by tasks.registering(JavaExec::class) {
    dependsOn(tasks.named("jar"), slimJar, agentJar)
    classpath = files(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    mainClass.set("modcore.Main")
    val sandbox = rootDir.parentFile.resolve("temp/instrumentation-prepare")
    workingDir(sandbox.resolve("work"))
    systemProperty("user.home", sandbox.resolve("home").absolutePath)
    systemProperty("java.io.tmpdir", sandbox.resolve("tmp").absolutePath)
    environment("LOCALAPPDATA", sandbox.resolve("appdata").absolutePath)
    args("--prepare-runtime", file("lib/projectzomboid.jar").absolutePath,
        file("build/slim/modcore-payload.jar").absolutePath, file("build/dist/runtime.jar").absolutePath)
    doFirst {
        listOf("work", "home", "tmp").forEach { sandbox.resolve(it).mkdirs() }
    }
}

val agentDistribution by tasks.registering(Copy::class) {
    dependsOn(packageLoadout)
    from("launch-agent.ps1")
    into("build/dist")
}

tasks.named("assemble") { dependsOn(agentDistribution) }
