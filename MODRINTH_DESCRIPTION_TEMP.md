# Create: Sable Contraptions

> **测试版 / Beta**：功能仍在开发中，完整游戏内验证尚未完成。请备份世界后再安装。

让 Create 的动态结构接入 Sable 的物理子世界。保留 Create 原有的控制与运动逻辑，同时为结构增加真实的物理位置、碰撞和玩家乘坐体验。

## 功能

- 将 Create 电梯、机械轴承、风车轴承、时钟轴承、绳索滑轮、动力活塞、龙门滑块和部分矿车结构转换为 Sable 物理结构。
- 支持稳定子结构跟随、结构运行中编辑、真实库存与流体交互。
- 运动结构会检查与世界方块及其他物理结构的碰撞；受阻时暂停，障碍解除后可恢复。
- 钻头可根据自身运动轨迹处理前方碰撞目标；便携式存储接口在接近固定接口时减速对接。
- 继续使用 Create 的控制器、运动行为及相关工作流程，而不是将结构改造成自由动力学机器。

- 工程师护目镜提供停转原因与可关闭的障碍高亮；Mods 配置入口及常规界面支持四种语言。
- 保护本结构家族、所属控制器和矿车装配站，避免钻头破坏装配源头。

## 前置依赖

- Minecraft 1.21.1
- NeoForge 21.1.228 或兼容版本
- Create 6.0.10 或兼容版本
- Sable 2.0.3 或兼容版本

各依赖仍须满足它们各自的加载器及版本要求。建议使用项目说明中列出的兼容组合。

## 当前版本与状态

当前开发版本：**0.7.6-beta.1**。

160 项非游戏检查在四组依赖组合上通过，共 640 次。已有部分用户实机反馈，但尚未完成最新版系统性游戏内回归。轴承、滑轮、活塞、龙门、矿车、钻掘、多人同步及保存重载等行为可能存在问题；大型结构的性能和部分嵌套/第三方结构交互也未完全验证。请备份存档，并在测试环境中使用。

## 安装

将发布包中的 `create-sable-contraptions-*.jar` 放入 Minecraft 实例的 `mods` 文件夹，并安装上述前置依赖。不要安装 sources 包。

## 说明

这是 Create 与 Sable 的社区兼容扩展，不隶属于 Create 或 Sable 项目。Create、Sable 及其他依赖由其各自作者维护，并受各自许可证约束。

---

# Create: Sable Contraptions (English)

> **Beta:** This mod is still in development and has not completed in-game validation. Back up your worlds before installing.

Bring Create contraptions into Sable's physical sub-levels. The mod keeps Create's existing controls and movement behavior while adding physical positioning, collision checks, and rideable moving structures.

## Features

- Converts Create elevators, mechanical, windmill and clockwork bearings, rope pulleys, mechanical pistons, gantry carriages, and selected minecart contraptions into Sable physical structures.
- Supports stabilized child structures, editing while structures are moving, and interaction with real inventories and fluid handlers.
- Moving structures check collisions against world blocks and other physical structures. They can pause when blocked and resume after the obstruction is cleared.
- Drills can process collision targets along their own movement path. Portable storage interfaces slow down when approaching compatible stationary interfaces.
- Keeps Create's controllers, movement behaviors, and related work flows; structures are not converted into free-running physics machines.

- Engineer's goggles show stop reasons and optional obstacle outlines. A Mods configuration screen and four UI languages are available.
- Movement breakers protect their structure family, controllers and minecart assembly stations.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or a compatible version
- Create 6.0.10 or a compatible version
- Sable 2.0.3 or a compatible version

Each dependency must also meet its own loader and version requirements. See the project documentation for tested combinations.

## Version and testing status

Current development version: **0.7.6-beta.1**.

The same 160 non-game checks pass across four dependency combinations (640 executions). There is user feedback on individual fixes, but systematic in-game regression testing of the latest version is not complete. Bearings, pulleys, pistons, gantries, minecarts, drilling, multiplayer synchronization, and save/reload behavior may still have issues. Large-structure performance and some nested or third-party structure interactions are also not fully verified. Back up your worlds and test in a separate instance.

## Installation

Place the released `create-sable-contraptions-*.jar` in your Minecraft instance's `mods` folder and install the required dependencies above. Do not install the sources package.

## Notice

This is a community compatibility addon for Create and Sable. It is not affiliated with the Create or Sable projects. Create, Sable, and other dependencies remain the property of their respective authors and are subject to their own licenses.

## License / 许可证

Original project material is licensed under **LGPL-3.0-or-later**. Third-party components retain their own licenses. / 本项目原创内容采用 **LGPL-3.0-or-later**，第三方内容保留各自许可。
