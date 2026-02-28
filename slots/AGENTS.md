# AGENTS.md

## Scope

- 本规则集适用于 `slots` 模块下所有 slot 游戏实现。
- 具体游戏的特例（如 `gameId`、类名前缀、专属配置文件、特殊状态机）不应写死在本文件中。
- 若某个游戏存在专属规则，应在对应游戏目录下创建 `AGENTS.md` 或 `AGENTS.override.md` 进行补充或覆盖。

## 项目规则

- 目标模块：`slots`。
- 先看父类再改子类：
    - 流程父类：`AbstractSlotsGameManager`
    - 生成父类：`AbstractSlotsGenerateManager`
- 玩法配置强依赖 `sampledata` 表结构；新增逻辑优先配置化，不要硬编码概率、倍率、次数、阈值等核心参数。
- 服务端职责：结算、状态、协议、配置驱动、数据持久化、一致性控制。
- 客户端职责通常包括：UI、音效、动效、表现层逻辑；需求文档中的界面章节默认优先判断为客户端侧。
- 任何改动必须评估主要模式链路是否同时生效：
    - 普通模式
    - 免费模式（如存在）
    - 其它特殊模式（如存在）

## 编码规范

- 协议字段兼容优先，已有字段名（含历史拼写）不可随意改动。
- 复杂逻辑优先“配置 + 小方法拆分”，避免继续膨胀单个超长方法。
- 不要散落魔法值；常量应集中到对应游戏或模块的常量类中。
- 新增玩家持久化字段时，必须同步对应的：
    - `PlayerGameData`
    - `PlayerGameDataDTO`
    - `PlayerGameDataRoomDTO`
- 若目标游戏已有既定命名、分层、封装模式，优先复用，不随意新造体系。
- 修改逻辑前，优先定位：
    - 父类已有通用能力
    - 同模块相似玩法已有实现
    - 现有配置是否已经支持，仅缺少接线

## 配置约定（CSV列名/类型/必填）

- 通用约定：
    - `id:int` 必填且唯一。
    - `gameType:int` 或 `gameId:int` 必填，必须等于当前目标游戏ID。
    - 概率/权重字段通常为 `Map<int_int>`，不能为空，且总权重不能为 0。
    - `List` / `Map` / 嵌套结构字段的格式必须严格符合项目既有解析器要求，不可随意修改分隔符、层级、空值表示方式。
    - 所有新增配置字段时，必须评估：
        - 默认值
        - 空值兼容性
        - 旧配置兼容性
        - 是否影响线上已有逻辑

- `BaseElementCfg.csv`
    - 关键列：`elementId:int`(必填), `gameId:int`(必填), `type:int`(必填), `postChangeElementId:Map<int_int>`(可选)。
    - 用途：定义基础图标、图标类型、图标替换关系。

- `BaseElementRewardCfg.csv`
    - 关键列：`gameType:int`(必填), `elementId:List<int>`(必填), `lineType:int`(必填), `rewardNum:int`(必填), `bet:int`(
      必填), `featureTriggerId:List<int>`(可选)。
    - 用途：定义图标组合、线路类型、奖励倍数及特殊触发条件。

- `BaseInitCfg.csv`
    - 关键列：`rows:int`(必填), `cols:int`(必填), `maxLine:int`(必填), `betMultiple:List<int>`(必填),
      `lineMultiple:List<int>`(必填), `lineType:int`(必填)。
    - 用途：定义棋盘规模、线路数量、下注倍率、线路倍率等初始化参数。

- `BaseLineCfg.csv`
    - 关键列：`gameType:int`, `lineId:int`, `posLocation:List<int>`。
    - 用途：定义线路玩法的连线位置。
    - 说明：仅在线路玩法启用时必填；若为 ways、cluster、消除类玩法，可无此表或部分字段无效。

- `BaseRoomCfg.csv`
    - 关键列：`lineBetScore:List<int>`(必填), `defaultBet:List<int>`(必填), `marqueeTrigger:List<long>`(可选)。
    - 用途：定义房间下注档位、默认下注及跑马灯触发阈值。

- `SpecialModeCfg.csv`
    - 关键列：`type:int`(必填), `rollerMode:int`(必填), `specialGirdID:List<int>`(可选),
      `specialGroupGirdID:Map<int_int>`(可选)。
    - 用途：定义特殊模式、转轴模式或特殊格子分组规则。

- `SpecialAuxiliaryCfg.csv`
    - 关键列：`id:int`(必填), `type:int`(必填), `triggerCount:Map<int_int>`(可选), `rollerMode:int`(可选),
      `specialGirdID:List<int>`(可选)。
    - 用途：定义特殊玩法辅助规则，例如免费次数权重、模式附加条件等。

- `SpecialGirdCfg.csv`
    - 关键列：`id:int`(必填), `element:Map<int_int>`(目标图标权重), `affectGird:List<List<int>>`(可修改格子),
      `randCount:Map<int_int>`(修改次数权重), `notReplaceEle:List<int>`(可选)。
    - 用途：定义特殊格子的目标元素、影响范围、替换次数及排除元素。

- `SpecialPlayCfg.csv`
    - 关键列：`playType:int`(必填), `value:String`(必填，格式严格)。
    - 用途：定义特殊玩法参数。
    - 注意：具体 `playType` 含义必须以目标玩法现有实现、配置说明或需求文档为准，不可跨游戏强行复用。

## 相似需求实现步骤

1. 先阅读需求文档，并拆成：
    - 判奖规则
    - 模式规则
    - 加成规则
    - 协议返回
    - 配置变更
2. 在 `slots` 模块中查找最相似的现有玩法实现，优先复用已有结构和流程。
3. 优先补齐配置，再补生成与判奖逻辑，再补状态流转与协议字段。
4. 明确目标玩法涉及的模式链路：
    - 普通模式
    - 免费模式
    - 其它特殊模式
5. 在目标游戏对应的 `GenerateManager` 中增加或复用判奖、改图、特殊触发逻辑。
6. 在目标游戏对应的 `GameManager` 中对接状态流转、次数维护、结算承接。
7. 在目标游戏对应的 `SendMessageManager` 中补充客户端需要的返回字段。
8. 同步检查 DTO 与 RoomDTO 字段，避免房间模式、断线重连、缓存恢复时丢状态。
9. 输出改动摘要时必须说明：
    - 改了哪些配置
    - 改了哪些类
    - 涉及哪些模式链路
    - 有哪些潜在风险点

## Review / Bug Check（只读模式）

- 当任务目标是 review、查 bug、做映射分析时：
    - 不要修改任何文件
    - 不要生成补丁
    - 只输出证据、代码位置、配置位置、调用链路、风险说明
- 输出优先包含：
    - 文件路径
    - 类名 / 方法名
    - 关键行号或最小定位片段
    - 对应配置文件与字段
    - 影响链路与风险判断

## 验收建议

- 最小回归至少覆盖：
    - 普通一局
    - 触发特殊模式
    - 特殊模式中关键加成/加免/改图
    - 特殊模式结束回归普通
- 若需求涉及协议字段变更，还需验证：
    - 房间态
    - 断线重连
    - DTO/缓存恢复
    - 历史兼容性

## 验收命令（Maven）

- 编译（推荐）：
    - `mvn -pl slots -am -DskipTests compile`
- 打包：
    - `mvn -pl slots -am -DskipTests package`
- 若需强制执行测试（项目默认可能跳测）：
    - `mvn -pl slots -am test -DskipTests=false -e`