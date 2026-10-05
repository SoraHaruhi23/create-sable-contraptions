# Create: Sable Contraptions

**机械动力：物理动态结构 · 0.7.6-beta**

## English

Brings Create contraptions into Sable physics while retaining their controls and movement behaviors.

- Supports elevators, mechanical/windmill/clockwork bearings, rope pulleys, mechanical pistons, gantries, minecart contraptions, and stabilized children.
- Includes collision stopping, editing while moving, live inventory/fluid access, and portable interface docking.
- Offers goggle status hints and in-game configuration, with Simplified Chinese, Traditional Chinese, English, and Japanese UI.
- Regular Create trains are outside the conversion scope.

**Requirements:** Minecraft 1.21.1, Java 21, NeoForge 21.1.228+, Create 6.0.10+, and Sable 2.0.3+. Compatibility with newer dependency versions is not guaranteed.

**Installation:** Place the regular JAR in `mods` and remove the previous version. Use matching versions on clients and servers. Configuration: **Mods → Create: Sable Contraptions → Config**.

Beta release. Complex coupling, third-party components, and long-running performance need further testing. Include versions, reproduction steps, and logs in bug reports.

## 中文

将 Create 动态结构接入 Sable 物理系统，保留原有控制方式和工作部件行为。

- 支持电梯、三类轴承、绳索滑轮、动力活塞、龙门、矿车结构及稳定子结构。
- 支持碰撞停转、运行中编辑、真实库存与流体访问，以及便携式接口对接。
- 提供工程师护目镜状态提示和游戏内配置，界面支持简中、繁中、英语、日语。
- 普通 Create 列车不在物理化范围内。

**环境：** Minecraft 1.21.1、Java 21、NeoForge 21.1.228+、Create 6.0.10+、Sable 2.0.3+。较新依赖版本不保证兼容。

**安装：** 将普通 JAR 放入 `mods`，移除旧版；多人客户端与服务端使用相同版本。配置入口：**Mods → Create: Sable Contraptions → 配置**。

当前为 Beta。复杂耦合、第三方组件和长期性能仍需测试。问题反馈请附版本、复现步骤及日志。

## Development & docs / 开发与文档

This project was developed with assistance from GPT. / 本项目使用 GPT 辅助开发。

Build with Java 21 / 使用 Java 21 构建：

```powershell
./gradlew.bat build --console=plain
```

[Documentation / 文档目录](docs/README.md) · [Release notes / 发布说明](docs/RELEASE-0.7.6-beta.md) · [Changelog / 更新记录](CHANGELOG.md) · [Testing & limitations / 测试与限制](docs/VALIDATION.md) · [GitHub guide / GitHub 教程](docs/publishing/GITHUB-QUICKSTART.md)

**License: [LGPL-3.0-or-later](LICENSE).** [Copyright / 版权声明](NOTICE.md) · [Third-party notices / 第三方说明](THIRD_PARTY_NOTICES.md).

A community addon, not an official Create or Sable project. / 社区兼容扩展，非 Create 或 Sable 官方项目。
