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

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateLuaSymbols)
    from(generateLuaPrefix) {
        into("modcore")
    }
    from(file("build/generated/modcore/symbols.txt")) {
        into("modcore")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation(files("lib/fmod.jar"))
    implementation(files("lib/zombie.jar"))
    implementation(files("lib/Kahlua.jar"))
    implementation(files("lib/org.jar"))
}

tasks.named<Jar>("jar") {
    destinationDirectory.set(project.file("build"))
    archiveFileName.set("modcore-${version}.jar")

    manifest {
        attributes["Main-Class"] = "modcore.Main"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // L2 随机前缀资源 (generateLuaPrefix 产物)
    from(file("build/generated"))

    from(configurations.runtimeClasspath.get().map { file ->
        if (file.isDirectory) {
            file
        } else {
            zipTree(file)
        }
    })
}
// ============================================================
// L4b: 存根编译 (先于 jar: coreboot.class 进 jar 资源, install 提取)
// ============================================================
val compileCorebootStub by tasks.registering {
    inputs.file(file("stub/coreboot.template.java"))
    outputs.file(file("build/stub-out/zombie/coreboot.class"))
    doLast {
        val template = file("stub/coreboot.template.java").readText(Charsets.UTF_8)
        val srcDir = file("build/stub-src").apply { mkdirs() }
        File(srcDir, "coreboot.java").writeText(template, Charsets.UTF_8)
        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
            ?: throw GradleException("no system java compiler available")
        compiler.run(null, null, null,
            "-encoding", "UTF-8",
            "-d", file("build/stub-out").absolutePath,
            file("build/stub-src/coreboot.java").absolutePath
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    from(compileCorebootStub) {
        into("modcore")
    }
}

// ============================================================
// L4b: 加密载荷 (packageLoadout, jar 完成后)
//   1. 读取 build/modcore-<ver>.jar -> AES-256-GCM 加密 (随机密钥)
//   2. 生成 build/dist/modcore.bin  (32B key + 12B IV + 密文, 见 coreboot 模板)
// 安装形态: install 时加密安装器自身写 bin + 提取 jar 内 modcore/coreboot.class
// 游戏目录常驻 = zombie\coreboot.class + %USERPROFILE%\Zomboid\modcore.bin
// ============================================================
// ============================================================
// L4b 载荷瘦身修正 (2026-09-10): 旧实现把整个安装器 fat jar 当载荷加密,
// 而 fat jar 含 runtimeClasspath 全量解包 (游戏 jar 依赖, 62MB/26337 条目) →
// modcore.bin 63MB = 完整游戏副本密文, 体积即特征且解包会污染类路径。
// 修正: 载荷 = slim jar (仅 modcore/* 类+资源), 内嵌安装器 modcore/payload.jar;
// install 现场从自身提取 slim 载荷再加密 (密钥仍为安装现场随机)。
// 运行时类零 ASM 引用 (5 个补丁器类只在安装期加载, 不进运行链)。
// ============================================================
val slimJar by tasks.registering(Jar::class) {
    destinationDirectory.set(project.file("build/slim"))
    archiveFileName.set("modcore-payload.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
}

tasks.named<Jar>("jar") {
    dependsOn(slimJar)
    from(slimJar.map { it.archiveFile }) {
        into("modcore")
        rename { "payload.jar" }
    }
}

val packageLoadout by tasks.registering {
    dependsOn(tasks.named("jar"))
    doLast {
        val jarFile = file("build/slim/modcore-payload.jar")
        if (!jarFile.exists()) throw GradleException("payload jar not found: ${jarFile}")

        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val encrypted = cipher.doFinal(jarFile.readBytes())
        val outDir = file("build/dist").apply { mkdirs() }
        File(outDir, "modcore.bin").writeBytes(key + iv + encrypted)
        println("L4b loadout: build/dist/modcore.bin (payload ${encrypted.size} bytes)")
    }
}
tasks.named("assemble") { dependsOn(packageLoadout) }
// build.bat 跑 "clean jar": jar 完成后自动产出 L4b 载荷 (密文 bin + 存根)
tasks.named("jar") { finalizedBy(packageLoadout) }
