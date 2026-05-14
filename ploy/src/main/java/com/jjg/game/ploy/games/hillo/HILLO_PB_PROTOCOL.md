# HILLO PB 协议说明

本文档用于前端对接 HILLO 小游戏协议。

## 消息类型

HILLO 使用两类消息：

- `MessageTypeDef.PLOY_COMMON`：通用小游戏协议，负责进入、下注、历史记录。
- `MessageTypeDef.HILLO`：HILLO 专属协议，负责猜牌、跳过、兑现、自动投注。

## 进入游戏

请求：`ReqPloyConfig`

- `messageType = PLOY_COMMON`
- `cmd = REQ_PLOY_CONFIG`

响应：`ResHilloEnterGame`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `currentCard` | int | 当前公牌 id，0 表示没有进行中的局 |
| `currentCoin` | long | 当前可兑现奖励，含本金 |
| `remainRoundNum` | int | 本局剩余可猜次数，最大 50 |
| `remainSkipTimes` | int | 本局剩余跳过次数，最大 20 |
| `currentBetMode` | int | 当前模式，`0=手动`，`1=自动` |
| `autoBetting` | boolean | 是否正在自动投注 |
| `autoInfiniteBet` | boolean | 自动投注是否无限局 |
| `autoBet` | long | 自动投注金额 |
| `autoGuessTimes` | int | 自动投注单局猜测次数 |
| `autoRemainBetTimes` | int | 自动投注剩余局数，`autoInfiniteBet=true` 时 0 表示无限 |
| `chooseInfos` | List\<HilloChooseInfo> | 当前公牌可选投注项 |
| `historyChoose` | List\<HilloHistoryInfo> | 本局已发生的猜测/跳过过程 |
| `stakeList` | List\<Integer> | 下注上下限配置，通常为 `[min,max]` |
| `defaultBet` | long | 默认下注额 |

## 手动开始一局

请求：`ReqPloyBet`

- `messageType = PLOY_COMMON`
- `cmd = REQ_PLOY_BET`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bet` | long | 下注金额 |
| `value` | int | 投注模式。HILLO 中 `0=手动`，`1=自动`；手动开局传 0 |

响应：`ResHilloBet`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `currentCard` | int | 起手公牌 id |
| `remainRoundNum` | int | 剩余可猜次数，初始 50 |
| `remainSkipTimes` | int | 剩余跳过次数，初始 20 |
| `chooseInfos` | List\<HilloChooseInfo> | 当前公牌可投注项 |

## 投注项 HilloChooseInfo

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `chooseId` | int | 投注项 id |
| `chooseName` | String | 投注项文本 |
| `odd` | String | 赔率，例如 `"1.03"` |
| `winRate` | String | 中奖概率，小数形式，例如 `"0.9230"` |

`chooseId` 含义：

| chooseId | 含义 | 适用牌 |
| --- | --- | --- |
| `0` | 大于等于当前牌，`>=` | 2-Q |
| `1` | 小于等于当前牌，`<=` | 2-Q |
| `2` | 大于当前牌，`>` | A |
| `3` | 等于当前牌，`=` | A/K |
| `4` | 小于当前牌，`<` | K |

投注项返回规则：

- 当前牌为 `2-Q`：返回 `>=` 和 `<=`。
- 当前牌为 `A`：返回 `>` 和 `=`。
- 当前牌为 `K`：返回 `=` 和 `<`。

## 手动猜牌

请求：`ReqHilloChoose`

- `messageType = HILLO`
- `cmd = REQ_HILLO_CHOOSE`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `chooseId` | int | 选择的投注项 id |

响应：`ResHilloChoose`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `nextCardId` | int | 本次发出的下一张牌 id |
| `currentCoin` | long | 猜中后当前可兑现奖励；猜错时为 0 |
| `exchangeNum` | long | 达到最大猜测次数并自动兑现时，实际到账金币 |
| `remainRoundNum` | int | 剩余可猜次数 |
| `remainSkipTimes` | int | 剩余跳过次数 |
| `chooseInfos` | List\<HilloChooseInfo> | 猜中后下一张公牌的新投注项；猜错或已兑现时为空 |

前端判断建议：

- `code != SUCCESS`：请求失败。
- `exchangeNum > 0`：服务端已自动兑现，本局结束。
- `currentCoin > 0 && chooseInfos 非空`：猜中，可继续猜或手动兑现。
- `currentCoin == 0 && chooseInfos 为空`：猜错，本局结束。

## 手动兑现

请求：`ReqHilloExchange`

- `messageType = HILLO`
- `cmd = REQ_HILLO_EXCHANGE`

请求字段：无。

响应：`ResHilloExchange`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `getGoldNum` | long | 实际到账金币，已扣税 |

## 手动跳过

请求：`ReqHilloSkip`

- `messageType = HILLO`
- `cmd = REQ_HILLO_SKIP`

请求字段：无。

响应：`ResHilloSkip`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `currentCard` | int | 跳过后新公牌 id |
| `remainSkipTimes` | int | 剩余跳过次数 |
| `chooseInfos` | List\<HilloChooseInfo> | 新公牌对应投注项 |

限制：

- 起手牌不能跳过。
- 每局最多跳过 20 次。

## 开始自动投注

请求：`ReqHilloAutoBet`

- `messageType = HILLO`
- `cmd = REQ_HILLO_AUTO_BET`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bet` | long | 每局下注金额 |
| `guessTimes` | int | 单局猜测次数，服务端限制为 `1-50` |
| `betTimes` | int | 自动投注局数，`0` 表示无限局 |

响应：`ResHilloAutoBet`

服务端会立即执行第一步，并启动后续自动推进。后续每一步服务端也会主动推送 `ResHilloAutoBet`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `autoBetting` | boolean | 是否仍在自动投注 |
| `autoInfiniteBet` | boolean | 是否无限局 |
| `action` | int | 本次自动动作 |
| `currentCard` | int | 当前公牌 id |
| `nextCardId` | int | 本次猜牌发出的下一张牌 id |
| `chooseId` | int | 本次自动选择的投注项 id |
| `currentCoin` | long | 当前可兑现奖励 |
| `exchangeNum` | long | 本次自动兑现到账金币 |
| `remainRoundNum` | int | 本局剩余可猜次数 |
| `remainSkipTimes` | int | 本局剩余跳过次数 |
| `remainBetTimes` | int | 剩余自动投注局数；无限局时配合 `autoInfiniteBet=true` |
| `chooseInfos` | List\<HilloChooseInfo> | 当前公牌可投注项 |
| `historyChoose` | List\<HilloHistoryInfo> | 当前局历史过程 |

`action` 含义：

| action | 名称 | 说明 |
| --- | --- | --- |
| `0` | `NONE` | 无动作 |
| `1` | `START_ROUND` | 自动开始新局，已下注并发起手牌 |
| `2` | `CHOOSE` | 自动猜牌且猜中，局还在继续 |
| `3` | `SKIP` | 自动跳过当前公牌 |
| `4` | `EXCHANGE` | 自动兑现，本局结束 |
| `5` | `ROUND_LOSE` | 自动猜牌失败，本局结束 |
| `6` | `STOP` | 自动投注停止 |

自动投注规则：

- 服务端比较当前两个投注区域的胜率。
- 任一区域胜率 `>=60%`，选择胜率最高的区域。
- 两个区域都 `<60%` 时自动跳过。
- 首次猜测不能跳过，因此首次即使都 `<60%` 也会选择最优区域；公牌为 7 时等价于默认选大于等于。
- 猜中次数达到 `guessTimes` 后自动兑现。
- 猜错后自动进入下一局。
- `betTimes` 用完后自动停止。
- 余额不足或下注失败会自动停止。

## 取消自动投注

请求：`ReqHilloCancelAuto`

- `messageType = HILLO`
- `cmd = REQ_HILLO_CANCEL_AUTO`

请求字段：无。

响应：`ResHilloAutoBet`

字段同自动投注响应。

取消逻辑：

- 如果当前局已有可兑现奖励，服务端会先自动兑现，再停止自动投注。
- 如果当前没有可兑现奖励，直接停止。
- 响应里 `autoBetting=false`。

## 历史记录

请求：`ReqPloyRecord`

- `messageType = PLOY_COMMON`
- `cmd = REQ_PLOY_RECORD`

响应：`ResHilloRecord`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 结果码 |
| `historyInfoList` | List\<HilloRecordInfo> | 历史记录列表，最新记录在前 |

`HilloRecordInfo`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `historyInfos` | List\<HilloHistoryInfo> | 本局过程 |
| `totalIncome` | long | 本局盈亏，正数为赢，负数为输 |
| `startTime` | long | 本局开始时间，毫秒时间戳 |
| `bet` | long | 本局下注金额 |
| `betMode` | int | 投注模式，`0=手动`，`1=自动` |
| `balanceAfter` | long | 本局结束后的玩家余额 |

`HilloHistoryInfo`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `cardId` | int | 当时作为公牌的牌 id |
| `chooseId` | int | 当时选择的投注项 id |
| `odd` | String | 当时赔率 |
| `resultCardId` | int | 本次猜牌结果牌 id |
| `skipped` | boolean | 是否为跳过记录 |

说明：

- 普通猜牌记录：`skipped=false`，有 `chooseId/odd/resultCardId`。
- 跳过记录：`skipped=true`，表示 `cardId` 这张公牌被跳过。

## 牌 ID 说明

牌 id 范围：`1-52`。

点数计算：

```text
rank = (cardId - 1) % 13 + 1
```

点数含义：

| rank | 牌面 |
| --- | --- |
| `1` | A |
| `2-10` | 数字牌 |
| `11` | J |
| `12` | Q |
| `13` | K |

HILLO 只比点数，不比花色；A 最小，K 最大。
