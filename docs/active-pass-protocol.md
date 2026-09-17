# 活跃通行证服务端接口

客户端人员及客户端 Codex 的完整接入流程、缓存规则和验收清单见 [客户端对接指南](active-pass-client-integration.md)。

消息类型为 `SIM_GAME=0x54`，请求入口统一使用 `SimMessageHandler`。以下接口在大厅及游戏节点均可调用，统一由玩家所属 SIM 节点处理；到账也路由至同一节点。

## 请求与响应

| 请求 | cmd | 参数 |
| --- | --- | --- |
| `ReqActivePassInfo` | `0x54094` | 无 |
| `ReqActivePassTaskClaim` | `0x54095` | `passId`、`taskId`、`day`；每日任务回传列表的日期，周期任务可传 0 |
| `ReqActivePassRewardClaim` | `0x54096` | `passId`、`rewardId`、`track`；rewardId=0 为所有已达标等级，track=0 为所有已解锁轨道 |
| `ReqActivePassBuyPoints` | `0x54097` | `passId`、`count`（购买份数）、`expectedPurchasedPoints`（上次收到的已购积分） |

统一返回 `ResActivePass(0x54098)`：`requestCmd` 标识请求，`code` 为状态码，`info` 为最新状态，`items` 为本次领取的等级奖励。

`info` 包含当前 `passId`、`endTime`（毫秒时间戳）、`level`、`points`（本期累计积分）、`purchasedPoints`、`purchasedTracks`、`day`（yyyyMMdd）、任务列表和已领取奖励记录。没有开放期时 `passId=0`。

轨道位标记统一为 `1=免费、2=初级、4=高级`。初级与高级分别购买、分别解锁。奖励记录按 `PassReward.id` 返回 `claimedTracks`；未返回的记录视为尚未领取。

任务包含 `taskId/group/taskType/progress/target/status`。状态为 `0=进行中、1=待领取、2=已领取`。周期任务每组只返回当前节点，领取后用同组的新节点替换；同口径的递进目标保留已完成进度，待领奖时不提前累计下一阶段。

`NotifyActivePassUpdate(0x54099)` 推送期数、累计积分、等级、付费轨道和变更任务。`reset=true` 表示换期、关闭或日切，清理旧任务并重新获取完整状态；其他更新按 `(taskType, group)` 替换对应任务。服务器推送通过玩家当前会话投递，支持游戏内接收。

## 购买与显示

通用 `ReqGenerateOrder` 使用 `rechargeType=16`、`productId=期数ID_轨道`，例如 `1001_2`。初级价格来源为 `ActivePass.ShopRechargeListID`，高级为 `ShopRechargeList1ID`，均关联 `ShopRechargeList.id`。付款成功后仅解锁权益，不自动领取当期等级奖励。订单绑定原期，跨期到账在原期结算，不解锁新期。

购买积分：global 350 的 `intValue` 为本期购买积分总上限；global 351 的 `value=道具ID_每份数量_每份积分`。购买 count 份消耗 `count×每份数量`、增加 `count×每份积分`。超出购买积分上限或满级累计积分上限时拒绝整笔购买。余额不足、非法参数和重复旧请求均不增加积分。

等级由本期 RewardGroup 对应的 `PassReward.ActivePoints` 累计门槛决定。当前等级进度为 `points−当前等级累计门槛`，下一等级所需为 `下一等级累计门槛−当前等级累计门槛`；0 级基准门槛为 0，满级显示由客户端处理。

global 352 控制客户端在哪些游戏显示通行证图标；它不限制服务端接口或任务事件。图标优先显示每日任务，再显示周期任务；布局、红点和排序由已返回的状态及客户端配置决定。

## 生命周期与边界

- 开放区间为 `[time_start, time_end)`，同一时间只处理一期。时间沿用公共格式 `yyyy/MM/dd HH:mm:ss`。
- 每日任务：taskType=8，按 group 分组，在 `SimBaseData.allLevel > CasinoLevelMin` 的候选中随机取一条；当日保持不变，跨日重选，不额外采用 CasinoLevelMax。
- 周期任务：taskType=9，每组按任务 ID 升序，从第一条开始；领取积分后推进下一条，不随日切重置。
- 完成任务先进入待领取状态；手动领取后增加 integralNum。通行证积分独立于积分大奖系统。
- 期末未领取的已达标等级奖励按轨道通过现有幂等邮件入口补发。在线玩家由 tick 结算；离线玩家下次加载时按玩家索引补结算，不扫描全服玩家。邮件有效期沿用现有全局邮件配置。
- 期数据独立保存；历史记录保留积分、权益和领奖记录供迟到订单处理，结算后释放任务明细。数据库写入失败时支付流程不确认发货成功。

服务端不硬编码文档中的等级总数、价格、任务数量、抽取池或额外宝箱规则，也不接入文档注明不要的周期内活动。
