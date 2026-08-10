```
介绍:
基于 Yeet-Masta 的 EtherHack 二次开发, 适配 Build 42 的工具 (Lua 渲染)。
B42 将游戏类都打包进 projectzomboid.jar, 本工具会自动从 jar 提取目标类并做字节码注入。

使用方法:
0. 需要 JDK 17 或以上版本 (建议 JDK 17/25; 运行 patch 用系统 java 即可)
1. 执行 build.bat 用 gradle 构建 (会生成 build\EtherHack-x.x.x.jar)
2. 将打包好的 jar 放入僵毁根目录 (X:\Steam\steamapps\common\ProjectZomboid)
3. 将 "运行.bat" 放入僵毁根目录
4. 管理员执行 "运行.bat" 即可 (自动执行 --uninstall + --install, 可重复执行)
5. 也可手动执行: java -jar EtherHack-x.x.x.jar --install / --uninstall
6. insert键呼出/关闭菜单
```