# GitHub 仓库与发布准备

当前版本：0.7.6-beta.1。此文是操作建议，不表示仓库或 Release 已创建。

完整操作步骤见 [新建仓库教程](GITHUB-QUICKSTART.md)，可直接粘贴的正文见 [Beta 发布说明](RELEASE-0.7.6-beta.1.md)。发布附件集中在 `build/releases/0.7.6-beta.1/`。

## 建议顺序

1. 创建名为 `create-sable-contraptions` 的 GitHub repository，先决定公开还是私有。若尚未决定源码公开方式，可先设为 Private。
2. 使用现有源码、README 和 .gitignore 建立本地 Git 历史并推送。GitHub 建仓时不再生成 README、.gitignore 或 LICENSE，以免与本地文件冲突；使用本地已选定的 LGPL-3.0-or-later 文件。
3. 用仓库保存源码与文档，用 Issues 收集问题。没有必要等到 beta 才建立仓库。
4. 准备提供下载时，为准确的源码提交创建 `v0.7.6-beta.1` 标签及 GitHub Release，勾选 **This is a pre-release**，附上模组 JAR 和本版说明。也可先保存为 Draft。
5. 作者已同意进入首个 beta。以后发布到 Modrinth/CurseForge 时，可将该仓库作为源码和问题反馈地址。

公开仓库、发布下载和进入 beta 是不同决定。本次没有执行 Git 初始化、推送、建仓或发布。

## 源码与附件

源码提交范围：`src/`、`gradle/`（包括 wrapper JAR）、`gradlew`、`gradlew.bat`、Gradle 配置、`scripts/` 和项目文档。已有 .gitignore 排除了 `build/`、`.gradle/`、`.gradle-user-home/`、`.reference/`、`.tools/`、`run/` 等本地目录。

不要把整个工作目录拖进网页上传：网页操作不能代替本地 Git 对忽略规则和暂存文件的核对。推送前检查实际暂存文件；上游参考仓库、依赖缓存、游戏存档、日志和本地工具不应作为项目源码提交。

可供玩家下载的附件：`build/libs/create-sable-contraptions-0.7.6-beta.1.jar`。不要将其提交到源码目录；放入 Release 附件。`-sources.jar` 是源码包，GitHub 自动生成的 Source code ZIP 也不是可安装模组。

## 当前发布说明要点

- Minecraft 1.21.1 / Java 21 / NeoForge 21.1.228 构建基线。
- Create 6.0.10+、Sable 2.0.3+ 为加载声明，实际矩阵见 README；不宣称未来版本已验证。
- 当前为首个 beta；160 项非游戏检查在四组依赖上通过，不代表完整游戏验证。
- 首个 beta 延续 0.7.4 起移除旧名称读取的规则；仅供已转换的数据或新存档使用。旧存档须先通过 0.7.3 迁移。
- 普通列车不在转换范围内；建议用存档副本测试。

## 许可证状态

项目原创内容已采用 **LGPL-3.0-or-later**。提交源码时包括根目录 LICENSE、COPYING、COPYING.LESSER 和 THIRD_PARTY_NOTICES.md；构建会将这四份文件加入普通 JAR 和 sources JAR。

发布二进制时，应同时向接收者提供与该二进制对应的完整源码和构建脚本，并遵守适用条款。建议 Release 对应准确的源码标签并提供源码下载；不要只提供一个无法定位对应版本的首页链接。源码获得方式不等于要求每个私下修改者上传公共 GitHub。

Create、Sable、Gradle Wrapper 等第三方内容仍遵循各自许可。选择 LGPL 不代替发布前的第三方来源与条款核对。

## 官方操作参考

- [创建仓库](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository)：仓库名、可见性、导入已有项目时的初始化选项。
- [管理 Release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)：标签、二进制附件、Draft 和 pre-release。
