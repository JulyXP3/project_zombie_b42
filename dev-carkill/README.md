# CarKill - 汽车秒杀独立工具 (红队 PoC)

独立工具。构建产物是单个 jar。只有游戏环境的电脑用启动参数加载，不需要单独安装 JDK。

## 只有游戏环境的电脑

构建后把这五个文件放在同一个目录（`car_kill.jar` 由 `build.ps1` 自动复制到脚本旁边）：

- `car_kill.jar`
- `install.bat`
- `install.ps1`
- `uninstall.bat`
- `uninstall.ps1`

运行 `install.bat`。jar 会被复制到 `%USERPROFILE%\Zomboid\car_kill.jar`。
脚本查找 Steam 里的 Project Zomboid，先备份这三个原始文件：

- `ProjectZomboid64.json`
- `ProjectZomboid64.bat`
- `ProjectZomboid64ShowConsole.bat`

然后在每个原有的 `-Xmx` 前插入：

```text
-javaagent:"%USERPROFILE%\Zomboid\car_kill.jar"
```

完全退出并重新启动游戏后，按 `\` 切换。效果只作用于本地玩家正在驾驶的车。

运行 `uninstall.bat` 会用 `.carkill-backup` 恢复这三个启动文件。jar 不会被删除。
游戏更新覆盖启动文件后，需要重新运行 `install.bat`。

## 已有 JDK 的调试机

游戏启动后也可以手动挂载：

```text
java -jar dist\car_kill.jar
java -jar dist\car_kill.jar <PID>
```

## 构建

```text
pwsh -File build.ps1
```

输出：`dist/car_kill.jar`

## 原理

- 启动时通过 `-javaagent` 加载；调试机也可在游戏启动后用 Attach API 挂载
- ASM 重定义 3 个类：
  - `IsoGameCharacter.calculateDamageFromVehicleImpact/RunOver`：仅僵尸返回 500
  - `BaseVehicle.calculateDamageWithCharacter`：僵尸撞击自伤返回 0
  - `BaseVehicle.applyImpulseFromHitPedestrian`：跳过僵尸撞击减速
  - `BaseVehicle.updateVelocityMultiplier`：主动上报无上限速度倍率
  - `GameKeyboard.update`：检测 LWJGL 反斜杠键 (43)
- 默认关闭，未开启时全部放行原版

## 限制

- 不修改 `projectzomboid.jar`
- 游戏更新导致方法签名变化时需要重新适配
