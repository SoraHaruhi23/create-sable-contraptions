# Changelog / 更新记录

## English

### 0.8.1-beta

- Defer assembly neighbour callbacks until supports, ownership and validation are complete; restore rollback blocks before notifying neighbours.
- Report minecart transfer failures through the assembler error display, and remove failed-attempt contraption couplings.

### 0.8.0-beta

- Synchronize live sliding-door settings with movement behaviours after wrench mode changes.
- Support assembly on unscaled Sable parent structures, including parent motion, save/reload and disassembly back into the parent.
- Adapt collisions, minecart alignment and portable interfaces; fix nested elevator ropes, contacts, carried collisions and Flywheel rendering.
- Protect active inner controllers and nested minecart pickup; validate parent links and fluid tank indices.

### 0.7.7-beta

- Fix alignment and riding support for the second cart in shared minecart structures.
- Exclude supporting carts from collisions with their own physical structure.

### 0.7.6-beta

- First public Beta: elevators, bearings, pulleys, pistons, gantries, minecart structures and stabilized children.
- Include collision stopping, inventory/fluid access, interface docking, goggle hints and configuration with four UI languages.
- Include fluid tank updates and portable-interface animation fixes.

## 中文

### 0.8.1-beta

- 装配邻居通知延后到支撑、归属及校验完成；回滚先恢复全部方块和数据再通知。
- 捕获矿车搬运阶段的装配异常，显示装配站错误并清理本次失败新增的结构耦合。

### 0.8.0-beta

- 同步滑动门扳手修改后的实时设置，避免动态结构行为沿用旧模式。
- 支持在未缩放的 Sable 父结构上装配，跟随父结构运动，保存重载后保留关联，并拆卸回父结构。
- 适配碰撞、矿车对齐及便携式接口；修复嵌套电梯绳索、接触器、随动碰撞和 Flywheel 渲染。
- 保护已装配内层控制器和嵌套矿车打包，校验父结构关联及储罐索引。

### 0.7.7-beta

- 修复共享结构中第二辆矿车的对齐和乘坐支持。
- 排除承载矿车与自身物理结构的碰撞。

### 0.7.6-beta

- 首个公开 Beta，支持电梯、轴承、滑轮、活塞、龙门、矿车及稳定子结构。
- 包含碰撞停转、库存与流体访问、接口停靠、护目镜提示及四语言配置。
- 包含储罐更新和便携式接口动画修复。
