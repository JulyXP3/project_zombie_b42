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
  - 车辆部件容器(后备箱、座位等)
- 命中位置在小地图上以**灰色方块**显示, 与玩家/僵尸标记同尺寸; 同格多件显示数量。
- 标记跟随角色: 走出 5 格(冷却 2 秒)、背包物品数变化(防抖 1 秒)、库存窗口容器变化时自动重扫。
- **小地图快捷开关栏**(可移动小地图窗口顶部): `我 / 玩家 / 载具 / 僵尸 / 物品` —— 白字=开, 灰字=关。关闭「物品」后立即清除标记并停止全部逐帧开销; 重新开启会自动按上次搜索目标静默重扫。
- 关闭小地图: 点击窗口右上角 **X** 按钮。

### 其他改动 / 修复

- **事件驱动刷新**取代定时器: 标记隐藏期间零开销。
- **Kahlua 兼容性加固** — 替换了 B42 Lua 虚拟机 (Kahlua) 中不存在的 API: `next()`、`ISUIElement.getVisible()`、`VehicleParts.getParts()`、`ItemContainer.size()`。并新增静态检查脚本 (`tests/check_kahlua_compat.lua`) 在构建前拦截这些禁用写法。
- 小地图默认尺寸改为 300×300。
- 更新了致谢文案(见 `Info.java`)。

## 安装

1. 从仓库根目录下载 `EtherHack-3.1.0-B42.jar`。
2. 复制到 `ProjectZomboid/mods/EtherHack/` 目录(没有就新建)。
3. 主菜单 → 模组 → 启用该模组。
4. 游戏中按 **Insert** 打开 EtherHack 面板。

## 从源码构建

环境要求: JDK 17+, 已包含 Gradle wrapper。

```bat
cd etherhack-src
gradlew.bat jar
```

产物在 `etherhack-src/build/EtherHack-3.1.0-B42.jar`。Lua 源码嵌入在 `src/main/resources/EtherHack/lua/` 中, 构建时自动打包。

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
| `EtherHack-3.1.0-B42.jar` | 可直接使用的构建产物(当前版本) |
| `etherhack-src/` | 完整源码 (Gradle 工程) |
| `tests/` | Lua 冒烟测试 + Kahlua 兼容性检查脚本 |
| `分析报告.md` | 分析报告: 可行性研究、反编译取证、扫描方案设计与限制 |
| `analysis/` | 反编译取证用的类文件片段 |

## 已知限制

- 只能扫描玩家周围**已加载**的区块(客户端限制; 服务端的 `processItems` 注册表在客户端恒为空)。
- 刻意不扫描玩家背包/身上装备; 其他玩家的物品有 1~2 秒同步延迟。
- 服务器开启战利品加密后, 客户端完全看不到容器内容。

## 致谢

- 原版模组: [EtherHack](https://github.com/dei0/EtherHack) by Quzile
- B42 移植: dei0

## 声明

- 本项目开发**仅使用了 Deepseek V4F 模型**, 如果采用 GPT 或 Claude, 效果将会更好。
- 本项目只是临时维护, 有需要的朋友可以自行拉取仓库做后续更新, 感谢!
