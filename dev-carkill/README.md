# CarKill - 汽车秒杀工具

独立工具。构建产物是单个 jar。

**游戏-only 机器（只有游戏、没有 JDK）**：直接跑 `install.bat`，不需要构建。
**开发/构建机器**：有 JDK 25+，用来跑 `build.bat` 生成 jar。

## 游戏-only 机器（推荐：直接安装）

构建后把这 4 个文件放在同一个目录（`car_kill.jar` 由构建脚本自动复制到脚本旁边）：

- `car_kill.jar`
- `install.bat`
- `install.ps1`
- `uninstall.bat`
- `uninstall.ps1`

**注意**：`.bat` 只是启动器，实际逻辑在对应的 `.ps1` 文件里。四个文件必须在同一目录，缺一不可。

运行 `install.bat`。jar 会被复制到 `%USERPROFILE%\Zomboid\car_kill.jar`，脚本查找 Steam 里的 Project Zomboid，先备份三个原始文件：

- `ProjectZomboid64.json`
- `ProjectZomboid64.bat`
- `ProjectZomboid64ShowConsole.bat`

然后在每个原有的 `-Xmx` 前插入：

```text
-javaagent:"%USERPROFILE%\Zomboid\car_kill.jar"
```

完全退出并重新启动游戏后，按 `\` 开关车秒杀；按 `=` 开关 50% 不扣弹。**两个开关默认关闭**；车秒杀效果只作用于本地玩家正在驾驶的车。

**车秒杀参数**: 伤害 **99** (原版僵尸血量 1.8–3.8, 一撞必死; 低于 vanilla `AntiCheatHitDamage` 的 `>100` 阈值); 车辆速度上限 **65 格/秒** (默认 `SpeedLimit=70` 时留 5 格/秒余量)。2026-10-21 实测通过。

**额外功能**：枪械发射子弹有 50% 概率不消耗弹药，**默认关闭**，按 `=` 开/关（`\` 仍是车秒杀开关）；切换时控制台打印 `[CarKill] AmmoSave ENABLED/DISABLED`。
命中（不扣弹）时会随原版 `SyncItemFields` 上行同步弹药数，服务端认可后不会触发
`AntiCheatHitWeapon: not enough ammo` 踢人（同本仓「无限弹药」的同步思路）。
只对本机玩家的瞄准型枪械生效，作用于单机、房主和多人客户端。

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

离线校验额外需要一份游戏本体：把游戏目录的 `projectzomboid.jar` 拷到 `../etherhack-src/lib/`（保持原文件名）即可。

构建产物一律不进 git，发给别人时从磁盘拿。
