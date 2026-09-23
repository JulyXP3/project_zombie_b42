# CarKill - 汽车秒杀工具

独立工具。构建产物是单个 jar。游戏-only 机器用 install.bat 安装，不需要 JDK。

## 只拷 3 个文件到游戏机器

构建后把这三个文件放在同一个目录（`car_kill.jar` 由构建脚本自动复制到脚本旁边）：

- `car_kill.jar`
- `install.bat`
- `uninstall.bat`

运行 `install.bat`。jar 会被复制到 `%USERPROFILE%\Zomboid\car_kill.jar`，脚本查找 Steam 里的 Project Zomboid，先备份三个原始文件：

- `ProjectZomboid64.json`
- `ProjectZomboid64.bat`
- `ProjectZomboid64ShowConsole.bat`

然后在每个原有的 `-Xmx` 前插入：

```text
-javaagent:"%USERPROFILE%\Zomboid\car_kill.jar"
```

完全退出并重新启动游戏后，按 `\` 开关。效果只作用于本地玩家正在驾驶的车。

运行 `uninstall.bat` 会恢复三个启动文件，并删除 `%USERPROFILE%\Zomboid\car_kill.jar`（游戏开着时删不掉：先退游戏再跑一次）。
游戏更新覆盖启动文件后，需要重新运行 `install.bat`。

## 有 JDK 的调试机

游戏启动后也可以手动挂载：

```text
java -jar dist\car_kill.jar
java -jar dist\car_kill.jar <PID>
```

## 新环境从零构建（刚拉下仓库时看这节）

前置：JDK 25+、pwsh、能访问 Maven Central 的网络。

```text
pwsh -File build.ps1
```

构建脚本自己搞定一切，不需要手动准备依赖：首次构建自动下载所需文件到 `lib/`（忽略目录，删了重下就行）；输出 `dist/car_kill.jar`，并自动复制一份到脚本旁边的 `car_kill.jar`（安装器认这个位置）。

离线校验额外需要一份游戏本体：把游戏目录的 `projectzomboid.jar` 拷到 `../etherhack-src/lib/` 改名 `zombie.jar` 即可。

构建产物一律不进 git，发给别人时从磁盘拿。
