# Project Zomboid B42 - EtherHack 社区构建版

面向 Project Zomboid Build 42 的 [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) 社区维护构建。

在原版基础上主要新增了 **物品搜索 + 小地图标记** 功能, 并针对 B42 客户端 Lua 环境 (Kahlua) 做了多处修复与加固。

> **重要声明:** 本项目只是**临时维护**, 有需要的朋友可以自行拉取本仓库做后续更新。感谢!

## 功能一览

### 物品搜索 & 小地图标记 (新增)

- 在**生成物品**页面(主面板)按名称/ID 过滤物品, 选中物品后点击 **「在地图上显示」** 按钮。
- 以玩家为中心扫描半径 48 格、玩家所在楼层 ±1 的**已加载区块**:
  - 家具/容器内容 (`IsoObject` 容器)
  - 地面散落物品、地上的包(含包内物品)
  - 尸体身上的物品 (尸体容器, `getDeadBodys()` → `getContainer()`)
  - 车辆部件容器(后备箱、座位等)
- 命中位置在小地图上以**灰色方块**显示, 与玩家/僵尸标记同尺寸; 同格多件显示数量。
- 标记跟随角色: 走出 5 格(冷却 2 秒)、背包物品数变化(防抖 1 秒)、库存窗口容器变化时自动重扫。
- **小地图快捷开关栏**(可移动小地图窗口顶部): `我 / 玩家 / 载具 / 僵尸 / 物品` —— 白字=开, 灰字=关。关闭「物品」后立即清除标记并停止全部逐帧开销; 重新开启会自动按上次搜索目标静默重扫。
- 主面板「地图」页的 **显示本机玩家 / 显示其他玩家 / 显示载具 / 显示僵尸 / 显示物品** 勾选框与小地图快捷开关栏**双向同步**(任意一侧切换, 另一侧即时跟随, 并持久化到配置)。
- 关闭小地图: 点击窗口右上角 **X** 按钮。

### 其他改动 / 修复

完整更新历史见 [CHANGELOG_zh.md](CHANGELOG_zh.md)。

## 安装

环境要求: **JDK 25** 与 **Gradle 9.1.0**。构建通过内置的 Gradle wrapper(`gradlew.bat`)执行, 首次运行会自动下载 Gradle; 也可使用本机已安装的 Gradle。

1. 准备编译依赖: 将游戏根目录下的 `projectzomboid.jar` 复制到 `etherhack-src/lib/` 目录下, 并改名为 `zombie.jar`(该文件仅用于编译, 不会被修改)。
2. 打开 `etherhack-src/build.bat`, 填写好 `JAVA_HOME`, 保存之后运行 `build.bat`。
3. 从 `build` 目录中拿到 `EtherHack-3.1.7-B42.jar`。
4. 将 jar 和 `etherhack-src/install.bat` 一起复制到游戏根目录。
5. 运行 `install.bat` 完成安装(需要系统装有 JDK)。

游戏中按 **Insert** 打开 EtherHack 面板。

## 从源码构建

环境要求: JDK 25, 已包含 Gradle wrapper。

```bat
cd etherhack-src
gradlew.bat jar
```

产物在 `etherhack-src/build/EtherHack-3.1.7-B42.jar`。Lua 源码嵌入在 `src/main/resources/EtherHack/lua/` 中, 构建时自动打包。

## 测试

```bat
rem Lua 冒烟测试 (扫描 + 防抖 + 移动刷新 + 开关逻辑)
temp\tools\lua51\lua5.1.exe tests\run_scan_test.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\EtherItemSearch.lua

rem Kahlua 兼容性静态检查 (禁用 API 黑名单)
temp\tools\lua51\lua5.1.exe tests\check_kahlua_compat.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\EtherItemSearch.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIItemTables.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIMap.lua etherhack-src\src\main\resources\EtherHack\lua\components\ui\UIMovableMiniMap.lua
```

注意: `temp/` 是本机临时目录, 不属于仓库内容。

## 仓库结构

| 路径 | 说明 |
|---|---|
| `etherhack-src/build/EtherHack-3.1.7-B42.jar` | 可直接使用的构建产物(当前版本) |
| `etherhack-src/` | 完整源码 (Gradle 工程, 含 `build.bat` / `install.bat`) |
| `tests/` | Lua 冒烟测试 + Kahlua 兼容性检查脚本 |
| `analysis/` | 反编译取证用的类文件片段 |

## 已知限制

- **物品雷达的同名匹配 bug**: 搜索词非空时点击「在地图上显示」, 会追踪过滤列表里的**全部**物品(名称过滤是子串匹配)。例如搜索 `Wrench`, 也会同时追踪 `Ratchet Wrench`。如果只想追踪单个物品, 请先清空搜索词, 再直接点选列表中的那一项。
- 只能扫描玩家周围**已加载**的区块(客户端限制; 服务端的 `processItems` 注册表在客户端恒为空)。
- 刻意不扫描玩家背包/身上装备; 其他玩家的物品有 1~2 秒同步延迟。
- 服务器开启战利品加密后, 客户端完全看不到容器内容。

## 致谢

- 原版模组: [EtherHack](https://github.com/dei0/EtherHack) by Quzile
- B42 移植: dei0

## 声明

- 本项目开发**仅使用了 Deepseek V4F 模型**, 如果采用 GPT 或 Claude, 效果将会更好。
- 本项目只是临时维护, 有需要的朋友可以自行拉取仓库做后续更新, 感谢!
