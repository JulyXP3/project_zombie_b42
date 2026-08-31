# Project Zomboid B42 - EtherHack 社区构建版

面向 Project Zomboid Build 42 的 [EtherHack 3.1.0 (B42)](https://github.com/dei0/EtherHack) 社区维护构建。

在原版基础上新增了 **耕种 / 地图传送(自动寻路快速移动) / 点亮全图 / 真-夜视 / 载具 / 战利品重掷 / ESP / 物品搜索 + 小地图标记** 等大量功能, 并针对 B42 客户端 Lua 环境 (Kahlua) 做了多处修复与加固。完整功能见下方「功能一览」。

> **重要声明:** 本项目**禁止任何形式的商业用途**(包括售卖、付费下载等), 二次修改与转载**必须标注原作者**。授权条款详见文末「许可证」。

## 功能一览

界面: 赛博朋克风图标+文字磁贴导航, 中/英/俄三语即时切换, 菜单关闭后恢复上次页面与滚动位置。以下按导航页分组:

### 角色

- **战斗**: 秒杀模式 / 暴击Max / 枪械只爆头(必爆头, 3 倍伤害) / 提高枪械射速 / 对僵尸群攻 / 僵尸不会攻击玩家(多人可用) / 无限弹药(耗尽自动补满, 可设弹药数) / 无卡壳 / 手中物品无限耐久 / 自动修理背包中的物品
- **生存**: 无限负重 / 无限耐力 / 高速回血(不是无敌) / 禁用肌肉拉伤 / 状态与需求全系禁用(疲劳/饥饿/口渴/醉酒/愤怒/恐惧/疼痛/恐慌/士气低落/压力/疾病/吸烟压力/理智/无聊/不快乐/潮湿/感染/虚假感染) / 维持最佳卡路里与体重
- **特殊模式**: 创造模式(高风险) / 夜视 / **真-夜视**(渲染级全亮, 夜色与视野锥黑幕移除, 无灯室内不再漆黑, 纯客户端) / 上帝模式 / 穿墙模式 / 隐身模式(后三项仅单人, 需先开启「解锁调试权限(单人)」)

### 物品

- **物品创建**: 名称/类别/ID 过滤, 一键给予 x1/x2/x5/x10
- **物品搜索 + 小地图标记**: 以玩家为中心 56 格、±1 层扫描已加载区块(家具/容器、地面与包、尸体、车辆容器), 命中以小地图灰色方块显示(同格多件计数); 标记随移动自动刷新; 小地图快捷开关栏(我/玩家/载具/僵尸/物品)与地图页勾选框双向同步

### 陷阱

- 搜索 + 生成食物(需站在已放置的陷阱旁, 多人)

### 玩家

- 技能等级 ± / 添加经验 / 所有技能升满; 特性添加/移除; 卡路里编辑; 存活时间/击杀数编辑(多人需先开启「服务器同步保护」)

### ESP

- 总开关 + 四模块: 玩家信息(附近玩家用户名、主/副手物品)、载具信息(马力/极速)、僵尸信息(头顶血条、僵尸雷达)、独立功能(玩家雷达 150 格、载具雷达、视野 360 度)

### 地图

- **点亮全图**: 一键揭示全部未知区域(多人下服务端同步记录)
- **自动寻路快速移动**: 在地图上右键任意地点, 沿可行走路径滑行(18 格/s), 不触发移动反作弊, 距离不限, WASD/空格随时终止; 单人保持瞬时传送
- 小地图: 可移动窗口 + 快捷开关栏, 显示本机玩家/其他玩家/僵尸/载具/物品

### 战利品

- **重置战利品(F9)**: 半径可调(默认 10), 重置后打开容器重新生成战利品(可反复刷枪柜/弹药箱, 仅多人)
- **刷弹药**: 按弹匣/枪械类型生成弹药

### 载具

- **无条件启动引擎**(单次/自动重试, 成功即自动取消) / 修理车辆 / 加满油(引擎启动仍需油/电, 需坐在载具内)

### 耕种

- **作物管理**: 范围(格) N×N 可调(默认 3×3), 生长进入下一阶段 / 直接成熟(可收获) / 浇水到满 / 清空水分 / 全部治病 / 感染病害+25 / 收获 / 铲除 / 移除残骸; 状态行实时计数并区分残骸/空地; 生长类操作最晚在下一个游戏 10 分钟刻生效
- **播种**: 全种子列表名称/ID 搜索, 免农具翻土, 免种子播种, 播后自动浇水

### 创建角色

- **自定义编辑**: 建号时自由添加/删除特性(可搜索列表, 点击加入/移出)、自定义技能等级(0..10); 名单持久化
- **建号增强**: 建号全特性 / 技能满级 / 解锁全部服装(建号界面显示游戏原生全服装选择器, 建号时自由穿搭) / 角色特性点数(滑块调节)
- 所有效果在点击"创建"瞬间生效; 创建前取消勾选即回退

### 其他改动 / 修复

完整更新历史见 [更新日志.md](更新日志.md)。

## 安装

环境要求: **JDK 25** 与 **Gradle 9.1.0**。构建通过内置的 Gradle wrapper(`gradlew.bat`)执行, 首次运行会自动下载 Gradle; 也可使用本机已安装的 Gradle。

1. 准备编译依赖: 将游戏根目录下的 `projectzomboid.jar` 复制到 `etherhack-src/lib/` 目录下, 并改名为 `zombie.jar`(该文件仅用于编译, 不会被修改)。
2. 打开 `etherhack-src/build.bat`, 填写好 `JAVA_HOME`, 保存之后运行 `build.bat`。
3. 从 `build` 目录中拿到 `EtherHack-3.2.1-B42.jar`。
4. 将 jar 和 `etherhack-src/install.bat` 一起复制到游戏根目录。
5. 运行 `install.bat` 完成安装(需要系统装有 JDK)。

游戏中按 **Insert** 打开 EtherHack 面板。

## 从源码构建

环境要求: JDK 25, 已包含 Gradle wrapper。

```bat
cd etherhack-src
gradlew.bat jar
```

产物在 `etherhack-src/build/EtherHack-3.2.1-B42.jar`。Lua 源码嵌入在 `src/main/resources/EtherHack/lua/` 中, 构建时自动打包。

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
| `etherhack-src/build/EtherHack-3.2.1-B42.jar` | 可直接使用的构建产物(当前版本) |
| `etherhack-src/` | 完整源码 (Gradle 工程, 含 `build.bat` / `install.bat`) |
| `tests/` | Lua 冒烟测试 + Kahlua 兼容性检查脚本 |
| `analysis/` | 反编译取证用的类文件片段 |

## 已知限制

- **过滤是纯子串匹配**(名称/ID, 不区分大小写): 未选中任何物品就勾选「在地图上显示」或「ESP 画线追踪」, 会追踪过滤列表里的**全部**物品, 例如搜索 `Wrench` 会连 `Ratchet Wrench` 一起追踪。只想追踪单个物品, 请先在列表中选中它。
- 物品雷达只能扫描玩家周围 56 格内**已加载**的区块(客户端限制; 服务端的 `processItems` 注册表在客户端恒为空), 且刷新有节流: 背包变动防抖 1 秒后重扫, 移动时走出 5 格且距上次扫描 ≥4 秒才重扫, 命中位置不是逐帧实时。
- 刻意不扫描玩家背包/身上装备; 其他玩家的物品有 1~2 秒同步延迟。
- 服务器开启战利品加密后, 客户端完全看不到容器内容。

## 致谢

- 原版模组 [EtherHack](https://github.com/Yeet-Masta/Project-Zomboid-EtherHack) by Quzile & Yeet-Masta
- B42 移植: [EtherHack B42](https://github.com/dei0/EtherHack) by dei0 (原仓库已无法访问, 保留链接以标注作者)
- 本仓库维护与功能开发: JulyXP3

## 许可证

- EtherHack 基座 © 2023 Quzile, 遵循 [MIT](etherhack-src/LICENSE.txt) 许可;
- 本仓库在其基础上的修改与新增部分遵循 [PolyForm Noncommercial 1.0.0](LICENSE) 许可: 非商业用途可自由使用、修改与分发; **任何商业用途(售卖、付费下载、广告变现等)须事先获得作者书面授权**;
- 二次修改与转载必须标注原作者(Quzile、Yeet-Masta、dei0、JulyXP3)并保留本许可声明。
