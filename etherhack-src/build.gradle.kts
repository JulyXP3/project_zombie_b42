import org.gradle.jvm.tasks.Jar
import java.util.Properties

plugins {
    java
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

fun loadProperties(): Properties {
    return Properties().apply {
        project.file("src/main/resources/EtherHack/EtherHack.properties").inputStream().use {
            load(it)
        }
    }
}

group = "EtherHack"
version = loadProperties().getProperty("version").replace("'", "")

// L2 符号轮换: 每次构建生成随机 Lua 符号前缀 (方案 §8.3-3) — 签名库对
// 上一构建有效, 对当前构建过期。写进 jar 内资源, EtherLuaLoader 运行时读取。
val generateLuaPrefix by tasks.registering {
    doLast {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz"
        val prefix = buildString {
            repeat(4) { append(chars.random()) }
        }
        val outDir = project.file("build/generated/EtherHack")
        outDir.mkdirs()
        File(outDir, "lua-prefix.properties").writeText("lua.prefix=$prefix\n")
        println("Lua symbol prefix for this build: $prefix")
    }
}

tasks.named<ProcessResources>("processResources") {
    from(generateLuaPrefix) {
        into("EtherHack")
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
    archiveFileName.set("EtherHack-${version}.jar")

    manifest {
        attributes["Main-Class"] = "EtherHack.Main"
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