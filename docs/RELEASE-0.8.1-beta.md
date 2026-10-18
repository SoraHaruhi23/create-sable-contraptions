# Create: Sable Contraptions 0.8.1-beta

## English

- Fix minecart assembly transfer errors escaping into the server tick. The assembler now displays the error and clears contraption couplings created by the failed attempt.
- Delay neighbour updates until assembly supports, ownership and validation are complete, avoiding premature updates to pressure plates and other attached blocks.
- Restore blocks and block-entity data before notifying neighbours when rolling back a failed assembly.

**Requirements:** Minecraft 1.21.1, Java 21, NeoForge 21.1.228+, Create 6.0.10+, and Sable 2.0.3+.

Replace the previous JAR on both clients and servers. Back up worlds before upgrading.

## 中文

- 修复矿车装配搬运异常可能导致服务器崩溃的问题。装配站现在会显示错误，并清理本次失败新增的结构耦合。
- 完成支撑、归属和校验后再执行邻居更新，避免压力板等附着方块在装配未完成时提前更新。
- 装配失败回滚时，先恢复全部方块及方块实体数据，再通知邻居。

**环境：** Minecraft 1.21.1、Java 21、NeoForge 21.1.228+、Create 6.0.10+、Sable 2.0.3+。

客户端和服务端同步替换旧版 JAR，升级前备份存档。

---

**LGPL-3.0-or-later · Pre-release / Beta**
