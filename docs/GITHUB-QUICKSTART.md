# 首次建立 GitHub 仓库与发布 Beta

适用于本项目 0.7.6-beta.1，2026-10-03 核对 GitHub 官方文档。本次仅提供教程，没有替你初始化 Git、创建仓库、推送或发布。

## 1. 在网页创建空仓库

登录 GitHub，右上角 **+ → New repository**，或打开 https://github.com/new 。

- Owner：你的账户。
- Repository name：`create-sable-contraptions`。
- Description：`Create contraptions powered by Sable physics for Minecraft 1.21.1 NeoForge.`
- Visibility：打算公开源码和收集反馈时选择 **Public**。
- **不要勾选 Add a README file；.gitignore 和 License 都选 None。** 本地已有这些文件，包括 LGPL-3.0-or-later 授权与完整条款。

点击 **Create repository**，复制页面给出的 HTTPS 地址，通常是 `https://github.com/你的用户名/create-sable-contraptions.git`。参见 [GitHub：创建仓库](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository)。

## 2. 在本地初始化并检查源码

先安装 Git，并在 PowerShell 确认 `git --version` 可用。以下命令在当前项目文件夹使用；若你已手动建立 Git 仓库，跳过 `git init`，先检查 `git status`。

```powershell
Set-Location 'E:\codex\sable physical contraptions'
git init -b main
git config user.name '你的提交署名'
git config user.email '你的提交邮箱或 GitHub 提供的 noreply 邮箱'
git add .
git status --short
git diff --cached --stat
git diff --cached --name-only
```

这些配置只作用于本仓库；将占位文字替换为自己的信息。提交前核对：包含 `src`、`gradle`、构建脚本、文档、LICENSE、COPYING、COPYING.LESSER、THIRD_PARTY_NOTICES.md；不应包含 `.reference`、`.tools`、`.gradle-user-home`、`build`、游戏存档或日志。现有 `.gitignore` 已排除这些本地目录。不要强制添加被忽略的缓存和构建产物。

确认内容正确后执行：

```powershell
git commit -m 'Release 0.7.6-beta.1'
git remote add origin https://github.com/YOUR_USERNAME/create-sable-contraptions.git
git remote -v
git push -u origin main
```

把 `YOUR_USERNAME` 替换为自己的 GitHub 用户名。按 Git 凭据管理器提示登录；不要把密码或令牌写入 remote URL。若提示 origin 已存在，先核对 `git remote -v`，不要重复添加；若推送被拒绝，先检查远程是否意外生成了 README/许可证，不要用强制推送掩盖冲突。参见 [GitHub：上传本地代码](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)。

## 3. 为首个 Beta 建立 Release

源码推送后，在项目根目录执行：

```powershell
git status --short
git tag -a v0.7.6-beta.1 -m 'First beta release'
git push origin v0.7.6-beta.1
```

先确认工作区没有遗漏的发布修改；标签应指向构建该 JAR 的源码提交。

在仓库网页打开 **Releases → Draft a new release**：

1. Tag 选择 `v0.7.6-beta.1`。
2. Title 填 `Create: Sable Contraptions 0.7.6-beta.1`。
3. 正文粘贴 [发布说明](RELEASE-0.7.6-beta.1.md)。
4. 上传 `build/releases/0.7.6-beta.1/` 中的安装 JAR、sources JAR、完整 source ZIP 和 SHA256SUMS.txt；也可附发布说明文件。
5. 勾选 **This is a pre-release**，核对后选择 **Publish release**，尚未准备公开可保存 Draft。

玩家下载普通 `.jar`。完整 `-source.zip` 包含构建脚本与测试，`-sources.jar` 便于开发工具查看源码。GitHub 也会为标签提供源码归档。JAR 作为 Release 附件上传，不提交到 Git 源码目录。参见 [GitHub：管理 Release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)。

## 4. 仓库建好以后

将真实仓库地址填入后续发布页面的源码和问题反馈地址。现在的文件没有虚构仓库 URL。后续版本按“修改源码 → 构建检查 → 提交 → 新标签 → 新 Release”发布，不覆盖已经发布的同名版本附件。
