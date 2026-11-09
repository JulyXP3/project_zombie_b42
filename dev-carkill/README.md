# CarKill - 汽车秒杀工具

独立工具。构建产物是单个 jar。

**目标机没有 Java（推荐）**：用打包好的 `car-kill/` 目录，双击 `car_kill_attach.bat`，内置 attach 运行时，不需要安装 Java。
**开发/构建机器**：有 JDK 25+，用来跑 `build.bat` 生成 jar 与打包。

## 目标机没有 Java：手动挂载（推荐）

构建产物在 `dist\car-kill\`（同时生成 `dist\car-kill.zip`）：

- `car_kill.jar`
- `attach-java\`（只含 `jdk.attach` + `java.instrument` 的最小运行时，约 31MB）
- `car_kill_attach.bat`
- `car_kill_diag.bat`（诊断：列出 attach 可见的 JVM）
- `find_pid.ps1`（WMI 按命令行找 PID 的兜底）
- `README.md`

用法（两种顺序都行）：

1. **脚本先跑（推荐）**：双击 `car_kill_attach.bat`，它最多等 120 秒；看到 `Waiting...` 后正常启动游戏（Steam 或 `ProjectZomboid64.bat` 都行）。
2. **游戏先开**：先启动游戏，再双击脚本，检测到进程后立即挂载。

看到 `[CarKill] Agent loaded.` 即成功；按 `\` 开关车秒杀，按 `=` 开关 50% 不扣弹。两个开关默认关闭。

**重复运行 attach 会怎样**：第二次会打印 `[CarKill] already attached; nothing to do.`，不会重复注册 transformer/重复注入。若这一局是在加防重入之前挂的旧 agent（JVM 里没有 `carkill.installed` 标记），守卫看不到，仍可能重复；退出游戏重开、只跑一次即可。agent 本身随游戏进程退出，不会留在后台。

**一直显示 `Waiting up to 120s...` 怎么办**：

- 先确认游戏已经启动到主菜单；
- 双击 `car_kill_diag.bat`，它会列出 `VirtualMachine.list()` 能看到的 JVM（`pid` + 显示名）和 Java 进程；把输出发回；
- 诊断里出现 `pid=... (empty displayName; likely embedded/JNI launch)` 就是游戏：`ProjectZomboid64.exe` 以 JNI 内嵌 JVM 启动，perf 里没有 `sun.java.command`，所以显示名为空。新包在只有一个空名候选时会自动选它；若有多个空名，仍请传 PID；
- 也可以直接传 PID：任务管理器 → 详细信息 → 找到路径为 `...\ProjectZomboid\jre64\bin\java.exe`（或 `ProjectZomboid64.exe`）的进程，然后 `car_kill_attach.bat <PID>`；
- 脚本会先用 `find_pid.ps1`（WMI 命令行）找 PID，找不到再让 Java 等待并扫描；两条路都找不到时，通常是游戏与脚本不是同一 Windows 用户/权限（例如游戏以管理员运行），请用同样权限运行脚本。
- 构建时若提示 `old package is in use (game running?)`，说明游戏已经加载了该 jar（agent jar 在游戏运行期间一直打开）；构建会写到 `dist\car-kill-<时间>\`，不会破坏旧包。退出游戏后重跑构建即可恢复 `dist\car-kill\` 名称。

这条路径**不修改游戏目录、不写 `-javaagent`**，因此游戏版本号后不会出现 `agents=1`。`attach-java` 只用于挂载，不是给游戏换运行时；游戏仍用自带 `jre64`（它没有 `jdk.attach`，所以挂载进程必须用完整 JDK 或本包内置的 attach 运行时）。

## 旧方式：安装到启动文件（会显示 agents=1）

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

**车秒杀参数**: 伤害 **99** (原版僵尸血量 1.8–3.8, 一撞必死; 当前版本已移除旧的伤害阈值反作弊, 99 不再需要贴着某个上限取值); 开启时把车辆的「撞碎障碍减速上限」替换为 **65 格/秒、倍率 1.0** —— 原版该处是 `34 - breakingSlowFactor`, 所以效果是高速撞穿障碍/僵尸不减速, 默认 `SpeedLimit=70` 时仍留 5 格/秒余量。**注意**: 若服务器把 `SpeedLimit` 调到 65 以下, 65 会超限并可能被速度反作弊判定 (本工具未做自适应, 需要时再改)。2026-10-21 实测通过。

**额外功能**：枪械发射子弹有 50% 概率不消耗弹药，**默认关闭**，按 `=` 开/关（`\` 仍是车秒杀开关）；切换时控制台打印 `[CarKill] AmmoSave ENABLED/DISABLED`。
命中（不扣弹）时会随原版 `SyncItemFields` 上行同步弹药数，服务端认可后不会触发
`AntiCheatHitWeapon: not enough ammo` 踢人（同本仓「无限弹药」的同步思路）。
只对本机玩家的瞄准型枪械生效，作用于单机、房主和多人客户端。

运行 `uninstall.bat` 会恢复三个启动文件，并删除 `%USERPROFILE%\Zomboid\car_kill.jar`（游戏开着时删不掉：先退游戏再跑一次）。
游戏更新覆盖启动文件后，需要重新运行 `install.bat`。

## 兼容性核查（2026-11-09，当前游戏版本）

- 用当前游戏 jar（revision `3d39148846`）重编译通过；离线 `TransformCheck` 全过：7 个注入点、每个变换后类的 ASM 帧校验、钩子默认值与守卫，输出 `[CarKill] transform check passed`。
- 注入点：`IsoGameCharacter.calculateDamageFromVehicleImpact/RunOver`（私有）、`BaseVehicle.calculateDamageWithCharacter/applyImpulseFromHitPedestrian/updateVelocityMultiplier`、`GameKeyboard.update`、`InventoryItem.setCurrentAmmoCount`，描述符均未变。
- 当前版本已移除 `AntiCheatHitDamage`（b42.21 的伤害 >100 检查）；`AntiCheatHitVehicle` 只校验驾驶员授权；弹药检查仍是 `ammo <= 0`，跳过扣减 + `syncItemFields` 的思路继续有效。
- 注入是 fail-open 的：游戏更新若改了方法签名，对应类会原样运行并在控制台打印 `[CarKill] transform skipped: <类名>`——看到这行说明需要重新适配。

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
