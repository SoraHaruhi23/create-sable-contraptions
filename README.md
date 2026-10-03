# Create: Sable Contraptions

**机械动力：物理动态结构 · 0.7.6-beta.1**

## 中文

将 Create 动态结构接入 Sable 物理系统，保留原有控制方式和工作部件行为。

- 支持电梯、三类轴承、绳索滑轮、动力活塞、龙门、矿车结构及稳定子结构。
- 支持碰撞停转、运行中编辑、真实库存与流体访问，以及便携式接口对接。
- 提供工程师护目镜状态提示和游戏内配置，界面支持简中、繁中、英语、日语。
- 普通 Create 列车不在物理化范围内。

**环境：** Minecraft 1.21.1、Java 21、NeoForge 21.1.228+、Create 6.0.10+、Sable 2.0.3+。较新依赖版本不保证兼容。

**安装：** 将普通 JAR 放入 `mods`，移除旧版；多人客户端与服务端使用相同版本。配置入口：**Mods → Create: Sable Contraptions → 配置**。

**升级：** 0.7.4～0.7.6-alpha 可直接升级。旧名称版本的数据须先通过 [0.7.3-alpha 转换](docs/MIGRATION-0.7.3.md)。升级前请备份存档。

当前为 Beta。复杂耦合、第三方组件和长期性能仍需测试。问题反馈请附版本、复现步骤及日志。

## English

Brings Create contraptions into Sable physics while retaining their controls and movement behaviors.

- Supports elevators, mechanical/windmill/clockwork bearings, rope pulleys, mechanical pistons, gantries, minecart contraptions, and stabilized children.
- Includes collision stopping, editing while moving, live inventory/fluid access, and portable interface docking.
- Offers goggle status hints and in-game configuration, with Simplified Chinese, Traditional Chinese, English, and Japanese UI.
- Regular Create trains are outside the conversion scope.

**Requirements:** Minecraft 1.21.1, Java 21, NeoForge 21.1.228+, Create 6.0.10+, and Sable 2.0.3+. Compatibility with newer dependency versions is not guaranteed.

**Installation:** Place the regular JAR in `mods` and remove the previous version. Use matching versions on clients and servers. Configuration: **Mods → Create: Sable Contraptions → Config**.

**Upgrading:** Upgrade directly from 0.7.4–0.7.6-alpha. Data from the former mod ID requires [migration through 0.7.3-alpha](docs/MIGRATION-0.7.3.md). Back up your world first.

Beta release. Complex coupling, third-party components, and long-running performance need further testing. Include versions, reproduction steps, and logs in bug reports.

## 开发与文档 / Development & docs

Build with Java 21 / 使用 Java 21 构建：

```powershell
./gradlew.bat build --console=plain
```

160 checks across four dependency combinations: 640 passes. These are non-game checks. / 160 项非游戏检查在四组依赖上共通过 640 次。

[发布说明 / Release notes](docs/RELEASE-0.7.6-beta.1.md) · [更新记录 / Changelog](CHANGELOG.md) · [测试与限制 / Testing & limitations](docs/VALIDATION.md) · [GitHub 教程 / GitHub guide](docs/GITHUB-QUICKSTART.md)

**License: [LGPL-3.0-or-later](LICENSE).** [Third-party notices / 第三方说明](THIRD_PARTY_NOTICES.md).

社区兼容扩展，非 Create 或 Sable 官方项目。 / A community addon, not an official Create or Sable project.

