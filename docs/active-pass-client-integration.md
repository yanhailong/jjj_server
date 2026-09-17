# 活跃通行证客户端对接指南

面向客户端开发人员及客户端 Codex。本文按 2026-09-17 当前服务端实现编写；与配套生成协议、同版本配置一起使用。代码示例仅描述字段和逻辑，不是 JSON 网络接口，也不定义 protobuf 字段序号。

## 1. 接入范围与约定

活跃通行证专用请求、响应和推送全部归属 `SIM_GAME = 0x54`，服务器入口为现有 `SimMessageHandler`。大厅和游戏内使用相同协议，经当前已登录连接发送，服务端负责路由到玩家所属节点。客户端不需要切回大厅、进入模拟经营场景或调用服务端内部 RPC。

付费解锁复用现有充值下单及支付流程，其消息仍属于通用 CORE 协议，不改成 SIM 协议。

接入前准备：

1. 更新并生成本指南所列消息及嵌套结构，复用项目现有消息注册、序列化和错误码机制。
2. 同步客户端 `ActivePass`、`PassReward`、`task`、`ShopRechargeList`、`global` 和相关道具/文本配置。
3. 保留已有背包、支付、邮件、服务器时间服务；通行证模块只维护通行证状态与交互。

所有 ID 示例均为演示值，不能直接作为真实配置。`passId` 是 `ActivePass.id`，`rewardId` 是 `PassReward.id`，`taskId` 是 `task.id`；三者不能混用。奖励 ID 也不能用等级值代替。

## 2. 协议清单

以下 cmd 均为完整消息号，客户端不能再次拼接前缀。

| 消息 | 方向 | 完整 cmd | 作用 |
| --- | --- | --- | --- |
| `ReqActivePassInfo` | 客户端 → 服务器 | `0x54094` | 获取全量状态，无业务参数 |
| `ReqActivePassTaskClaim` | 客户端 → 服务器 | `0x54095` | 领取单个任务积分 |
| `ReqActivePassRewardClaim` | 客户端 → 服务器 | `0x54096` | 领取等级奖励，支持批量 |
| `ReqActivePassBuyPoints` | 客户端 → 服务器 | `0x54097` | 消耗道具购买积分 |
| `ResActivePass` | 服务器 → 客户端 | `0x54098` | 上述四种请求的统一响应 |
| `NotifyActivePassUpdate` | 服务器 → 客户端 | `0x54099` | 状态变化推送 |

### 2.1 请求参数

| 请求 | 字段 | 类型 | 含义 |
| --- | --- | --- | --- |
| `ReqActivePassTaskClaim` | `passId` | int32 | 当前快照的期数 |
| 同上 | `taskId` | int32 | 当前任务 ID |
| 同上 | `day` | int32 | 每日任务原样回传快照的 day；周期任务可传 0 |
| `ReqActivePassRewardClaim` | `passId` | int32 | 当前快照的期数 |
| 同上 | `rewardId` | int32 | 指定 PassReward.id；0 表示本期所有达标奖励行 |
| 同上 | `track` | int32 | 1 免费、2 初级、4 高级；0 表示所有已解锁轨道 |
| `ReqActivePassBuyPoints` | `passId` | int32 | 当前快照的期数 |
| 同上 | `count` | int32 | 正整数购买份数，不是等级数，也不是积分数 |
| 同上 | `expectedPurchasedPoints` | int32 | 原样回传最近全量快照中的 purchasedPoints，用于校验旧请求 |

`track` 请求参数仅接受 `0/1/2/4`，不接受组合值 `3/6/7`。组合位标记只用于状态字段。

### 2.2 统一响应 ResActivePass

| 字段 | 类型 | 处理方式 |
| --- | --- | --- |
| `code` | int32 | 继承公共响应；200 成功，其他按公共错误机制处理 |
| `requestCmd` | int32 | 对应请求的完整 cmd，例如 `0x54097`；不是请求序列号 |
| `info` | ActivePassInfo | 非空时为全量快照；失败响应也可能携带最新快照 |
| `items` | ItemInfo 列表 | 本次领取等级奖励实际获得的道具；其他操作通常为空 |

`ItemInfo` 使用公共结构：`itemId:int32`、`count:int64`。奖励展示使用实际返回的 items；背包变更复用现有公共同步机制，避免收到响应后又自行给本地背包重复加道具。

### 2.3 全量快照 ActivePassInfo

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `passId` | int32 | 当前期数；0 表示没有开放的通行证 |
| `endTime` | int64 | 本期结束时间，Unix 毫秒时间戳 |
| `level` | int32 | 服务端按累计积分计算的当前等级 |
| `points` | int64 | 本期累计积分，包括已领取任务积分和购买积分 |
| `purchasedPoints` | int32 | 本期已购买的积分总量，不是购买次数 |
| `purchasedTracks` | int32 | 付费权益位标记：0 未购买，2 初级，4 高级，6 两者均已购买 |
| `day` | int32 | 服务端业务日期，形如 20260917；每日任务领取时原样回传 |
| `tasks` | ActivePassTaskInfo 列表 | 当前每日任务及各组当前周期任务 |
| `rewards` | ActivePassRewardInfo 列表 | 奖励行的已领取位标记；不是奖励配置列表 |

集合缺失/null 时按空集合处理。`info == null` 不等于活动关闭；路由或内部异常可能没有 info。只有有效快照中的 `passId == 0` 才表示当前未开放。

### 2.4 任务与领奖记录

`ActivePassTaskInfo`：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `taskId` | int32 | 用于查任务名称、描述和 integralNum |
| `group` | int32 | task.group |
| `taskType` | int32 | 8 每日任务，9 周期任务 |
| `progress` | int64 | 当前进度 |
| `target` | int64 | 完成目标 |
| `status` | int32 | 0 进行中，1 待领取积分，2 已领取 |

`ActivePassRewardInfo`：`rewardId:int32`、`claimedTracks:int32`。位标记为 1 免费、2 初级、4 高级；例如 5 表示该奖励行已领免费和高级。未出现的奖励行按 `claimedTracks=0` 处理。

客户端必须保留 int64 精度，尤其是积分、任务进度、道具数量和时间戳；使用生成协议对应的长整数类型。

## 3. 客户端状态同步

建议在账号会话层维护一份状态，界面只订阅它。大厅和游戏共用缓存；切换账号、登出时清空。推送监听的生命周期不要绑定通行证面板。

### 3.1 何时获取全量

- 登录/重连完成后，以及打开通行证界面时，发送 `ReqActivePassInfo`。
- 收到 `reset=true` 的推送，或推送期数与本地不同，失效旧状态并重新获取。
- 支付流程返回后、购买积分结果不确定时，重新获取以确认服务端状态。
- 本地倒计时到期时暂停旧期操作并刷新；不能自行推算下一期状态。

合并重复的全量刷新请求，无需逐帧或高频轮询。面板关闭期间仍处理推送；重新打开时用全量快照校准。

收到 `ResActivePass`：先按 requestCmd 结束对应请求的等待状态；只要 info 非空，就用其整体替换旧快照，包括完整替换 tasks、rewards 和 purchasedPoints。随后根据 code 展示操作结果。info 为空时保留已有展示但将相关操作标为待同步，按现有网络策略重试查询。

### 3.2 增量推送 NotifyActivePassUpdate

字段为：`code:int32`、`passId:int32`、`level:int32`、`points:int64`、`purchasedTracks:int32`、`reset:bool`、`tasks:ActivePassTaskInfo[]`。

推送**不包含** `day/endTime/purchasedPoints/rewards/items`。这些字段不能根据缺失值清零，也不能靠推送恢复完整状态。

处理规则：

```text
收到成功推送：
  若没有有效快照，或 reset=true，或 passId 与快照不同：
    标记旧缓存失效，停止使用旧任务参数和旧期操作
    合并触发一次全量查询，等快照后恢复操作
  否则：
    直接替换 level、points、purchasedTracks
    对 tasks 按 (taskType, group) 逐条替换
    保留其余字段；tasks 为空表示没有任务变化，不是清空列表
```

当前协议没有状态版本号，也没有独立业务请求 ID。客户端应将通行证主动请求串行发送，防止同一 cmd 的并发请求无法区分；网络超时后先查询状态再决定下一步。推送可能先于对应响应到达，不能把推送当作请求完成信号。切换连接后按现有会话隔离机制丢弃旧连接消息，再查询全量。

## 4. 任务交互

每日任务由服务器按每个 group 抽取一条，筛选条件为 `SimBaseData.allLevel > task.CasinoLevelMin`。客户端只显示下发的任务，不能本地抽取、补齐组或在升级时重抽。每日刷新由服务器日期决定，不使用手机日期生成 day。

周期任务每组按任务 ID 升序推进，每次只下发当前一条。领取积分后，响应可能直接把当前任务替换成下一条；用 `(taskType, group)` 定位卡片，用新 taskId 替换内容。最后一条领取后保持 status=2。同口径的递进任务可能保留进度，不保证新任务从零开始，也不提前累计待领奖期间的下一阶段进度。

领取流程：

1. status=1 时启用领取按钮。
2. 发送当前 passId、taskId；每日任务携带快照 day，周期任务传 0。
3. 等统一响应后替换快照，按返回 points/level 展示升级变化。

完成任务只进入待领取状态；领取后才增加 `task.integralNum`。没有“全部领取任务”的独立协议；若界面有此交互，需逐条串行领取，每次依据新快照更新候选，避免重复领取或错误推进周期任务。

## 5. 等级、奖励与红点

先由 `ActivePass[passId].RewardGroup` 筛选同组 PassReward，再按 level 排列奖励。`PassReward.ActivePoints` 是单个 int 累计门槛，不是数组，也不是升一级所需的增量。

未满级时可展示：

```text
本级进度 = points - 当前等级的累计门槛
升级所需 = 下一配置等级的累计门槛 - 当前等级的累计门槛
0 级尚未达到首个门槛时，当前门槛取 0
```

采用配置中的相邻等级，不假定配置行 ID 等于等级或等级总数固定。满级时显示满级状态；任务积分可能继续累计，不要因此显示负数或超长进度条。

轨道对应：

| 轨道 | 值 | 奖励字段 | 解锁条件 |
| --- | --- | --- | --- |
| 免费 | 1 | `PassReward.FreeRewards` | 默认解锁 |
| 初级 | 2 | `PassReward.BasicPaidRewards` | `(purchasedTracks & 2) != 0` |
| 高级 | 4 | `PassReward.PremiumPaidRewards` | `(purchasedTracks & 4) != 0` |

初级、高级分别购买、分别解锁。不能把高级视为包含初级。某行某轨道的可领取条件同时满足：奖励内容非空、累计积分达到该行门槛、轨道已解锁、`(claimedTracks & track)==0`。

领取参数组合：

| 操作 | rewardId | track |
| --- | --- | --- |
| 单行单轨道 | 该行 PassReward.id | 1、2 或 4 |
| 单行全部已解锁轨道 | 该行 PassReward.id | 0 |
| 某轨道全部可领行 | 0 | 1、2 或 4 |
| 一键领取全部等级奖励 | 0 | 0 |

未达标、未解锁、已领取或不属于本期的奖励可能得到 `code=200` 且 items 为空，这是当前服务端的无发奖结果，不代表产生了道具。仅在 items 非空时播放实际获得奖励表现。领取记录以返回快照为准；领奖操作不另发专门的领奖记录推送。

红点由客户端依据任务 status=1、等级奖励可领条件计算。入口优先展示每日任务，再展示周期任务；具体排序、布局、动画依需求 UI 实现，服务器不下发专用显示字段。global 352 的 value 决定哪些游戏显示入口，不限制协议调用或任务计数。

## 6. 消耗道具购买积分

读取 global：350 的 intValue 为每期累计购买积分上限；351 的 value 为 `道具ID_每份消耗数量_每份获得积分`。

设每份积分为 P、每份价格为 C，购买 count 份：增加 `count×P` 积分，消耗 `count×C` 道具。界面应区分“份数”和“目标等级”。如果提供购买至目标等级，先按积分差换算份数，再校验限制，不直接把等级差作为 count。

按当前状态可购买份数上限为：

```text
max(0, min(
  floor((global350.intValue - purchasedPoints) / P),
  floor((满级累计门槛 - points) / P),
  floor(当前道具余额 / C)
))
```

必须整份购买。超过任一积分上限时，服务器拒绝整笔请求，不自动裁剪份数。例如还差 10 积分满级而每份 20 积分，当前不能购买这 1 份。配置缺失或价格无效时禁用购买并按项目方式报告配置问题，不硬编码默认价格。

请求示例（字段示意）：`passId=1001, count=2, expectedPurchasedPoints=100`。expectedPurchasedPoints 必须来自最近快照，不是购买后预测值。

购买期间禁用重复点击，成功后从响应读取新 purchasedPoints。超时后先查询全量；不要用刷新后的 expectedPurchasedPoints 自动重发上次购买，这会变成另一笔有效购买。推送只有总 points，不能用它反推已购积分。

## 7. 现金购买付费权益

复用项目已有 `ReqGenerateOrder`：

| 字段 | 本功能填写 |
| --- | --- |
| `payType` | 沿用现有支付渠道；协议注释为 1 Google、2 iOS |
| `rechargeType` | `16`（ACTIVE_PASS） |
| `productId` | `ActivePass.id_轨道`，初级示例 `1001_2`，高级示例 `1001_4` |
| `desc` | 沿用现有支付流程的 JSON 格式备注约定；本功能无额外必填业务字段 |

`ResGenerateOrder` 返回公共 code 和 orderId。下单成功只表示产生订单；继续走现有平台支付和验单流程。不要新增“客户端直接解锁通行证”请求，也不要仅凭 SDK 支付成功修改 purchasedTracks。

初级商品配置关联 `ActivePass.ShopRechargeListID → ShopRechargeList.id`；高级关联 `ActivePass.ShopRechargeList1ID → ShopRechargeList.id`。通行证下单 productId 不是 ShopRechargeList.id，也不是平台 SKU；平台商品选择沿用已有支付模块及配置。服务端按对应 ShopRechargeList.price 校验价格。

当前期到账后，服务器推送新的 purchasedTracks。客户端支付返回时再拉取快照；若服务器尚未到账，保留“确认中”并使用现有支付补单/查询机制。初级、高级已购买的按钮分别禁用。

解锁后，之前已达标但未领取的对应轨道奖励变为可领，仍需手动领取。订单绑定下单时的期数；跨期到账在原期处理，通过邮件补发原期符合条件的奖励，不解锁新期。

## 8. 错误、日切与期末

| code | 含义与典型场景 | 客户端处理 |
| --- | --- | --- |
| 200 | 成功，也可能没有新发奖 | 应用快照；按实际 items 展示 |
| 400 | 条件未满足；例如任务未完成、付费预下单不满足条件 | 展示公共提示，更新状态 |
| 402 | 参数错误；例如非法轨道、购买超限、expectedPurchasedPoints 过期 | 应用快照，纠正参数；不自动再次扣费 |
| 404 | 期数、任务或每日日期失效等 | 应用快照；必要时重新查询 |
| 500 | 内部/路由异常，可能没有 info | 结束等待，按公共错误策略提示并查询确认 |
| 其他 | 背包扣除/发放等公共业务错误 | 使用公共错误码映射，不自行归类为成功 |

不能把失败响应一律丢弃，其 info 可能正是日切或换期后的新快照。

日切只重置每日任务，不清累计积分、已购积分、付费权益和周期任务。旧每日请求携带旧 day 会被拒绝；不要把旧 taskId 与新 day 拼成新的自动重试。

期末未手动领取的已达标等级奖励按已解锁轨道通过现有邮件系统补发。未领取的任务积分不会自动计入积分，也不会作为任务奖励补发。客户端无需发起结算请求。旧期记录不通过本查询接口浏览；新期重新获取自己的积分、任务、权益和领奖记录。

## 9. 给客户端 Codex 的执行说明

可将以下文字与本文、生成协议一起提供给客户端 Codex：

> 请先阅读本对接指南及同版本生成协议，检查客户端既有 SIM_GAME 消息注册、长连接、配置读取、背包同步、支付、邮件、服务器时间和红点实现，再接入活跃通行证。复用现有模块，保持网络状态与界面显示分工，不新增独立消息类型，不修改服务端或擅改协议字段。实现全量快照替换、增量任务按 (taskType, group) 合并、reset/跨期重取、请求串行及超时状态确认。必须使用 2/4 两个独立付费轨道、累计积分门槛、原样回传 day 和 expectedPurchasedPoints。按本文验收清单验证；遇到协议与代码不一致，列出具体字段和证据，停止猜测该项契约。最终说明修改文件、验证结果及仍需服务端联调的部分。

建议按“协议和状态模型 → 只读界面 → 任务/等级领奖 → 购买积分 → 付费接入 → 异常恢复”的顺序完成。不要仅完成静态界面就视为功能接入完成。

## 10. 联调验收清单

| 场景 | 预期 |
| --- | --- |
| 大厅、游戏内分别执行四种请求 | 均可交互，结果一致，无需回大厅 |
| 无开放期 | info.passId=0，关闭入口/旧面板，不遗留旧期可点击按钮 |
| 每日任务完成与领取 | 完成只变待领取；领取后积分和等级更新，重复领取不重复加分 |
| 周期任务推进 | 同组卡片替换成新 taskId，其他组保留；末条领取后已领取 |
| 每日跨日请求 | 旧 day 不领取新任务；重取任务，积分/权益/周期任务保留 |
| 免费、初级、高级与组合权益 | 2 只解锁初级，4 只解锁高级，6 表示两个均已解锁 |
| 单领、一键领、空奖励结果 | 仅发放当前符合条件且未领的奖励；空 items 不播放获得道具 |
| 购买积分成功/余额不足/两种积分上限/余量不足一份 | 正确预估与提示；拒绝时不展示成功扣费或加分 |
| 重复点击、断线、响应超时 | 不自动产生第二笔购买；重连全量恢复 |
| 增量推送 tasks 为空 | 保留任务列表、已购积分和领奖记录 |
| reset 或期数变化 | 旧缓存失效，重新获取 day、endTime 等完整字段 |
| 预下单成功、支付取消、支付延迟到账 | 仅按服务端权益解锁；不以下单成功代替支付到账 |
| 跨期支付与期末未领奖 | 新期不被旧订单解锁，符合条件的旧期奖励走现有邮件 |
| 入口隐藏的游戏 | 图标遵循 global352；任务仍可计数，接口不受显示配置限制 |

以上是客户端待执行的验收项，不代表已经完成客户端或真实支付联调。

## 11. 契约来源与交接文件

以下路径相对服务端仓库根目录，用于开发人员或 Codex 定位实现。客户端仓库无这些源码时，交接本文、同版本生成协议和配置即可；不要求复制服务端业务实现。

- 消息号：`sim/src/main/java/com/jjg/game/sim/constant/SimConstant.java` 的 MsgBean。
- 消息结构：`sim/src/main/java/com/jjg/game/activepass/pb/`。
- 请求入口：`sim/src/main/java/com/jjg/game/sim/handler/SimMessageHandler.java`。
- 行为与错误码：`sim/src/main/java/com/jjg/game/activepass/service/ActivePassService.java`、`core/src/main/java/com/jjg/game/core/constant/Code.java`。
- 通行证订单：`sim/src/main/java/com/jjg/game/activepass/service/ActivePassOrderService.java`。
- 通用支付协议：`core/src/main/java/com/jjg/game/core/pb/ReqGenerateOrder.java`、`ResGenerateOrder.java`。
- 简版接口索引：[active-pass-protocol.md](active-pass-protocol.md)。
