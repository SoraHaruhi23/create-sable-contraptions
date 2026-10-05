# Publishing guide / 发布指南

## English

Source repository: [create-sable-contraptions](https://github.com/SoraHaruhi23/create-sable-contraptions). Current version: **0.7.6-beta**.

1. Build with Java 21, verify the version and test results, and update the [release notes](../RELEASE-0.7.6-beta.md).
2. Commit source and documentation, then tag the matching commit as `v0.7.6-beta`.
3. Create a GitHub Release with bilingual notes. Mark Beta releases as **This is a pre-release**.
4. Upload the installable JAR, sources JAR, full source ZIP, release notes, and `SHA256SUMS.txt` from `build/releases/0.7.6-beta/`.

Only the regular JAR is installable. Keep build artifacts, dependency caches, reference repositories, and game files out of source commits. See the [GitHub guide](GITHUB-QUICKSTART.md) for repository setup.

## 中文

源码仓库：[create-sable-contraptions](https://github.com/SoraHaruhi23/create-sable-contraptions)。当前版本：**0.7.6-beta**。

1. 使用 Java 21 构建，检查版本号和测试结果，更新 [发布说明](../RELEASE-0.7.6-beta.md)。
2. 将源码和文档提交到仓库，为对应提交创建 `v0.7.6-beta` 标签。
3. 创建 GitHub Release，Beta 勾选 **This is a pre-release**，附中英双语说明。
4. 上传安装 JAR、sources JAR、完整源码 ZIP、发布说明和 `SHA256SUMS.txt`。附件位于 `build/releases/0.7.6-beta/`。

仅普通 JAR 可安装。构建产物、依赖缓存、参考仓库和游戏文件不提交到源码仓库。新建仓库步骤见 [GitHub 教程](GITHUB-QUICKSTART.md)。
