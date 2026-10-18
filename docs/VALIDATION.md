# Testing & limitations / 测试与限制

## English

### Current limitations

- Only unscaled Sable parents are supported. Assemble outer mechanisms first and disassemble inner mechanisms first; minecarts with independently assembled inner mechanisms cannot be packed.
- Collision prediction uses other bodies' current poses. High-speed relative motion needs further validation; detection budgets may stop very large structures.
- Third-party inventories, optional integrations and special advancements need separate verification. Recovery is not atomic across files or process crashes.

### Regression checklist

- [ ] Assemble structures with pressure plates and attached blocks; check supports, inventories, rollback after failed assembly and coupled-cart retries.
- [ ] With Steam 'n' Rails installed, change sliding-door modes after assembly, including while stopped at a floor; check manual/automatic operation and save/reload.
- [ ] Assemble, run, reverse, stop/resume, return and disassemble each supported mechanism.
- [ ] Check minecart curves, slopes, coupled carts, riding, assembler access, pickup and placement; drills must protect assembly sources.
- [ ] Run nested mechanisms on stationary, moving and rotating parents; check ropes, contacts, collision stopping and disassembly order.
- [ ] Check drills, deployers, redstone contacts, automatic doors and portable-interface docking/animations.
- [ ] Preserve items and fluids through bucket/interface transfers, assembly, pickup, save/reload and server restart.
- [ ] Check multiple clients, reconnects, normal/Flywheel rendering, goggles, configuration and UI languages; verify ordinary Create trains.

Report exact mod/dependency versions, reproduction steps, expected and actual results, and relevant logs. For stops, include `/csc cart_status` or `/csc bearing_status` output when available. Test upgrades in a world copy.

## 中文

### 当前限制

- 仅支持未缩放的 Sable 父结构。装配先外层，拆卸先内层；存在独立装配内层机构的矿车不能打包。
- 碰撞预测使用其他物理体的当前姿态，高速相对运动仍需验证；大型结构可能因检测预算不足停转。
- 第三方库存、可选集成及特殊进度需单独验证；恢复不保证跨文件或进程崩溃时的原子性。

### 回归清单

- [ ] 装配带压力板及附着方块的结构，检查支撑、库存、失败后的回滚及耦合矿车重试。
- [ ] 安装汽鸣铁道后，在装配状态下切换门模式（包括停在楼层时），检查手动／自动开门及保存重载。
- [ ] 各类机构装配、运行、反转、停止恢复、归位及拆卸。
- [ ] 矿车弯道、坡道、耦合、乘坐、装配站进出、收起放置；钻头保护装配源头。
- [ ] 在静止、移动及旋转父结构上运行嵌套机构，检查绳索、接触器、碰撞停转和拆卸顺序。
- [ ] 钻头、机械手、红石接触器、自动门及便携式接口停靠与动画。
- [ ] 桶和接口传输、装拆、矿车收起、保存重载及服务器重启后，物品和流体守恒。
- [ ] 多人观察与重连、普通及 Flywheel 渲染、护目镜、配置和界面语言；普通 Create 列车回归。

反馈请附准确版本、复现步骤、预期与实际结果及相关日志。停转问题可附 `/csc cart_status` 或 `/csc bearing_status` 输出。升级测试请使用存档副本。
