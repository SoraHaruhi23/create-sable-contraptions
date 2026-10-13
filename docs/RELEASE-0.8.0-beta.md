# Create: Sable Contraptions 0.8.0-beta

## English

- Synchronize live sliding-door settings with movement behaviours so wrench mode changes supplied by door addons are no longer left in the assembly snapshot.
- Assemble supported contraptions on unscaled Sable structures. Mechanisms follow their parent, retain the association across save/reload, and disassemble back into it.
- Adapt collisions, minecart alignment, actor rendering and portable-interface docking on the same parent. Fix nested pulley ropes, elevator contact checks and carried-structure collision detection.
- Remove duplicate block-entity visuals with Flywheel and correct parent-before-child physics updates and collision outlines.
- Protect active inner controllers and prevent packing minecarts with independently assembled inner mechanisms. Guard invalid fluid tank indices.

Assemble the outer mechanism first, then the inner mechanism; disassemble in reverse order. Scaled parents are not supported. Regular Create trains remain outside the conversion scope.

**Requirements:** Minecraft 1.21.1, Java 21, NeoForge 21.1.228+, Create 6.0.10+, and Sable 2.0.3+.

Install the regular JAR and remove the previous version. Use matching versions on clients and servers. Back up worlds before upgrading; test nested mechanisms in a world copy.

## 中文

- 同步物理结构上滑动门的实时设置与动态结构行为，避免附属提供的扳手模式修改后仍沿用装配时的旧数据。
- 支持在未缩放的 Sable 结构上装配已有类型的机构，跟随父结构运动，保存重载后保留关联，拆卸回原父结构。
- 适配碰撞、矿车对齐、工作部件渲染及同父结构便携式接口停靠；修复嵌套滑轮绳索、电梯接触器校验和随动结构的碰撞检测。
- 消除 Flywheel 重复方块实体画面，修正父子物理更新顺序和碰撞高亮。
- 保护已装配的内层控制器，阻止打包仍承载独立装配内层机构的矿车；处理非法储罐索引。

装配先外层、后内层；拆卸先内层、后外层。暂不支持缩放父结构，普通 Create 列车仍不参与物理化。

**环境：** Minecraft 1.21.1、Java 21、NeoForge 21.1.228+、Create 6.0.10+、Sable 2.0.3+。

安装普通 JAR，移除旧版；客户端与服务端保持版本一致。升级前备份存档，在存档副本中测试嵌套机构。

---

**LGPL-3.0-or-later · Pre-release / Beta**
