# Review Findings Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development task-by-task.

**Goal:** 修复协作任务结算可靠性、邮件幂等和勋章激活链路中的 6 个已确认问题。

**Architecture:** 在玩家协作任务文档中原子保存按 roomId 索引的结算回执，使 ACK 重试不再依赖可删除的任务项；在 Redis 房间记录中保存完整 FINISHED 载荷，并在 slots 启动时扫描恢复本节点未确认结算。邮件使用独立稀疏唯一业务键，勋章统一走 SimPackService，并在上下文装配时执行一次存量迁移。

**Tech Stack:** Java 21、Spring Data MongoDB、Spring Data Redis、JUnit 5、Mockito、Maven。

---

### Task 1: 持久化协作任务结算回执

1. 在 DAO 测试中先证明任务删除后仍可按 roomId 识别已结算。
2. 在 `SimCoopTaskData` 增加结算回执，并让 `SimCoopTaskDao` 在状态迁移时原子写入回执。
3. 修改 `SimCoopTaskService.onSettle` 使用回执判断重复结算并定期清理过期回执。
4. 运行聚焦测试。

### Task 2: 恢复 slots 未确认结算

1. 先增加 Redis 记录序列化和启动恢复测试。
2. 扩展 `CoopRoomRecord` 保存 success、finishTime、sharedProgress 和 helperIds。
3. 增加按 nodePath 扫描房间记录的方法；slots 启动时恢复 WAITING、将遗留 RUNNING 判负、重投 FINISHED。
4. 运行聚焦测试。

### Task 3: 原子邮件幂等

1. 先增加重复业务键测试。
2. 给 `Mail` 增加稀疏唯一 `bizKey`，发送时直接插入并把重复键视为已投递。
3. 运行聚焦测试。

### Task 4: 修复勋章激活与迁移

1. 先增加成就领奖激活、存量背包迁移和品质统计测试。
2. 成就奖励改走 `SimPackService`，失败时不推进任务状态。
3. 在 `SimBaseData` 增加迁移标记，在 `SimManager` 装配上下文时执行一次迁移。
4. 仅对已激活勋章累计品质数量，并按配置顺序返回激活列表。
5. 运行聚焦测试。

### Task 5: 完整验证

1. 运行相关单元测试。
2. 运行 `mvn compile -pl hall,sim,slots -am -DskipTests`。
3. 运行过滤后的 `git diff --check` 并复查未提交 diff。
