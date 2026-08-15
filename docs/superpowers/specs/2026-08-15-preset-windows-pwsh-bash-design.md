# Preset 外壳平台拆分：Git Bash（POSIX）/ pwsh（Windows）

日期：2026-08-15
范围：用户代理 preset `anchored-standard`，位于 `C:\Users\July\.dsh\.agent-presets\anchored-standard`

## 目标

让该 preset 的外壳工具在每个平台都能正常工作：

- macOS / Linux：保留持久化 PTY `bash` 工具，继续使用已配置的 Git Bash（`E:\Git\bin\bash.exe`）。
- Windows（win32）：不再挂载持久化 PTY bash 工具，改用原生 `pwsh` 工具。

## 背景与原因

持久化 bash 后端在 Windows 上还没等到启动 Git Bash 就已经失败：

- `@deepseek-ai/dsh-subprocess-local` 抛出
  `subprocess-local: terminal inspection is unsupported on platform win32`，
  因为 `createProcessInspector()` 只实现了 Linux 与 macOS 的检查器。
- 该失败与 `terminal-bash.shellPath` 无关；把它指向
  `E:\Git\bin\bash.exe` 本身是正确的，但无法让 PTY 路径在 win32 上工作。

另外，Git Bash（MSYS2）无法在 Windows ACL 受限令牌
（`workspace-write` / `read-only` 沙箱模式使用）下初始化
（报错 `NtSetInformationToken (TokenDefaultDacl), 0xC0000022`）。
因此确定的方向是：在 Windows 上不提供 bash 工具，直接使用 pwsh，
与官方 `standard` preset 的做法一致。

## 改动内容

除 `preset.yml` 中的描述外，其余改动都在 preset 组合文件
`agent.cordis.yml` 中。

1. `persistent-shell` 组（id 为 `persistent-shell`）：
   - 增加 `disabled: !!js process.platform === 'win32'`。
   - 效果：Windows 上不再挂载该组（PTY 注册表、terminal-bash 后端、
     持久化 bash 工具），坏掉的 `bash` 工具不会再出现或报错。
2. `terminal-bash.shellPath`：
   - 保持不变：`E:\Git\bin\bash.exe`（Git Bash），供 POSIX 平台使用。
3. `tool-pwsh`（id 为 `tool-pwsh`）：
   - 保持不变：当前已配置为仅在 win32 启用；在 win32 上它是唯一的外壳工具。
4. `tool-bootstrap` 配置：
   - 将 `bootstrapTools` 从固定列表 `[bash, str_replace_editor]`
     改为按平台切换：
     - win32：`[pwsh, str_replace_editor]`
     - 其他平台：`[bash, str_replace_editor]`
   - 效果：Windows 上首请求 bootstrap 保持可用的双工具锚点，
     而不是因为缺少 `bash` 退化为完整工具目录。
5. `preset.yml` 描述：
   - 同步更新，注明 Windows 使用 pwsh、POSIX 使用持久化 Git Bash。

## 不改动的内容

- 不修改 host composition（`profiles/web/cordis.patch.yml` 保持不变）。
- 不新增 preset 插件文件。
- 不修改沙箱或审批策略。
- 不改变 pwsh 的执行方式或沙箱方式。

## 错误处理

- 在 win32 上，持久化 bash 工具直接不存在，而不是“存在但不可用”。
  模型原本会调用 `bash` 的场景应改用 `pwsh`；确实需要一次性运行 Bash 时，
  pwsh 仍可显式调用 `& 'E:\Git\bin\bash.exe' -c '...'`。
- 在 POSIX 上，行为保持不变。
- 如果 bootstrap 工具因任何原因不可用，`tool-bootstrap` 仍会
  发出一次性警告并退化为完整工具目录。

## 验证

- 使用加载器同款解析器（`js-yaml`）解析修改后的 YAML，确认语法正确。
- 确认 `E:\Git\bin\bash.exe` 存在（已验证：存在）。
- 确认加载器支持 `cordis:group` 条目上的 `disabled`，并会连带禁用其子行
  （已在 `@deepseek-ai/cordis-plugin-loader` 源码中确认）。
- 人工推演：修改后 win32 的启用工具集为 bootstrap 阶段的
  `pwsh` + `str_replace_editor`，提升后为完整 preset 目录；
  由于 `tool-bash` 行与 `persistent-shell` 组在 win32 均被禁用，
  win32 上不会残留重复的 `bash` 注册。
- 当前正在运行的会话不受影响；改动对宿主进程重载/重启后新建的会话生效。

## 不在范围内

- 让持久化 PTY 后端支持 Windows。
- 让 Git Bash 能在 Windows ACL 受限令牌沙箱模式下运行。
- 改变该 preset 的实验性 bootstrap/轨迹评估目标。
