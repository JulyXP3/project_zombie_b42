# RE2 重制风 + 模拟毛玻璃 界面美化 (3.1.6)

日期: 2026-08-13
状态: 已批准

## 目标
在不改动任何功能/逻辑(点击、按键、配置、数据流全部保持)的前提下, 将 EtherHack 全部面板美化为生化危机 2 重制风格, 毛玻璃质感采用模拟实现(PZ Lua UI 无真实背景模糊)。

## 设计决策(已与用户确认)
- 基调: RE2R —— 近黑底 + 血红标题/边框 + 米白文字 + 噪点颗粒。
- 毛玻璃: 半透明深底 + noise 纹理 + 顶部高光 1px 线(原版主菜单同款做法)。
- 全局交互色 accentColor(用户可配置)保持为按钮填充/选中/滑块/激活图标染色, 不复写。
- 只动 render()/颜色字段/位置偏移, 零逻辑改动。

## 调色板 (EtherTheme.lua)
- `glassBG`      {0.02, 0.02, 0.02, 0.72}  窗口玻璃底
- `railBG`       {0.04, 0.03, 0.03, 0.85}  导航条底
- `blood`        {0.55, 0.08, 0.08, 1.0}   血红(标题条/边框/竖条)
- `bloodDim`     {0.55, 0.08, 0.08, 0.45}  淡化血红色(表格边框等)
- `edge`         {0.90, 0.88, 0.84, 0.12}  顶部高光
- `text`         {0.85, 0.83, 0.78, 1.0}   米白正文
- `textDim`      {0.85, 0.83, 0.78, 0.45}  次要文字
- `titleH`       = 20 (标题条高)

## 新资源(脚本生成 PNG)
- `media/ui/noise.png` — 128x128 噪点颗粒(低对比, 高 alpha 柔和), 全窗拉伸绘制。
- `media/ui/close_re.png` — 16x16 血红 X 关闭图标, 替换浮动窗与主窗原版关闭钮。

## 改动文件
| 文件 | 改动 |
|---|---|
| EtherTheme.lua (新) | 调色板 + drawNoise/drawTitleBar/drawGlass 辅助 |
| EtherHackMenu.lua | requireTheme; 玻璃底+血红边框; render 增加标题条+噪点; 关闭钮换图标; 内容区 y 下移 20 |
| UIButtonsPanel.lua | 玻璃底; 激活 = 血红 5px 竖条 + accent 图标, 未激活米白 |
| UIButton.lua | 玻璃钮+血红描边+左 3px accent 竖条; 按下 accent 底; 禁用暗化 |
| UICheckbox.lua | 文字米白, marginTexture 加大对齐 |
| UISlider.lua | 轨道透明化 + 血红描边, 滑块 accent + 血红勾边 |
| UISkillTable.lua | 修复列宽(等级/经验值/加成固定列宽, 表头对齐一致), 表头暗化 |
| EtherInfoPanel.lua | 修复文字重叠(行距/字号适配), 标题血红, 正文米白 |
| EtherVisualsPanel / EtherCharacterPanel | checkbox 图标与文字垂直对齐, margin 统一 |
| UIItemTables / UITraitsTable / EtherTrapSpawn | 表头/隔行/选中/边框换主题色, 列宽对齐一致 |
| UIMechanics / UIHealth / UIMovableMiniMap | 玻璃底+噪点+血红标题条+新关闭图标 |
| UIModalAddXP / UIModalAddTrait / EtherPlayerEditor / EtherSettingsPanel | 玻璃底+血红边框/头像框配色对齐 |

## 明确不做
- 不改原版控件(ISScrollingListBox 滚动条、ISComboBox、ISWorldMap、ISUI3DModel、ISTabPanel)外观 — 避免全局补丁污染游戏本体 UI。
- 不加任何按钮/开关/功能。

## 验证
- 对全部改动 Lua 跑 `tests/check_kahlua_compat.lua`(无 `next(`/`}[`/`:getVisible()` 等)。
- 构建 `cd etherhack-src; gradlew.bat clean jar -x test`, 产物 `EtherHack-3.1.6-B42.jar`。
- 版本号 3.1.5 → 3.1.6, CHANGELOG/README 同步(惯例)。