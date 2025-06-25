package com.jjg.game.logic.constant;

/**
 * 网格探索常量
 *
 * @author Alan
 */
public interface GridConstant {
  /** 网格探索配置时间格式 */
  String QLIPHOTH_CONFIG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  /** 功能模块概率极限值 */
  int WORLD_PROBABILITY_MAX = 10000;
  // 地图点类型
  /** 不存在点 */
  int WORLD_POINT_VOID = 0;
  /** 出生点 */
  int WORLD_POINT_BORN = 1;
  /** 阻挡点 */
  int WORLD_POINT_BLOCK = 2;
  /** 空白点 */
  int WORLD_POINT_BLANK = 0;

  // 事件参数
  /** 事件参数：目标位置 */
  int EVENT_PARAM_TARGET_POINT = 1;
  /** 事件参数：事件id */
  int EVENT_PARAM_CONFIG_ID = 2;
  /** 事件参数：事件间隔 */
  int EVENT_PARAM_SPACING = 3;
  /** 事件参数：关卡id */
  int EVENT_PARAM_DUNGEON = 4;
  /** 事件参数：怪物信息{id,num} */
  int EVENT_PARAM_ENEMY_INFO = 5;
  /** 事件参数：道具信息{id,num} */
  int EVENT_PARAM_ITEM_INFO = 6;
  /** 事件参数：地块id */
  int EVENT_PARAM_POINT_ID = 7;
  /** 事件参数：质点世界初始化 */
  int EVENT_PARAM_INIT_WORLD = 8;
  /** 事件参数：战斗胜利 */
  int EVENT_PARAM_BATTLE_WIN = 9;
  /** 事件参数：战斗伤害 */
  int EVENT_PARAM_BATTLE_DAMAGE = 10;
  /** 事件参数：过程事件目标点 */
  int EVENT_PARAM_PROGRESS_TARGET = 11;
  /** 事件参数：过程事件处理类型 */
  int EVENT_PARAM_PROGRESS_TYPE = 12;

  // 配置任务类型
  /** 击杀敌人总数 */
  int TASK_TYPE_COMMON_ENEMY = 1001;
  /** 获取道具 */
  int TASK_TYPE_PICK_ITEM = 1002;
  /** 击杀特定的敌人 */
  int TASK_TYPE_PARTICULAR_ENEMY = 1003;
  /** 区域探索 */
  int TASK_TYPE_EXPLORE_POINT = 1004;
  /** 通过关卡 */
  int TASK_TYPE_COMPLETE_DUNGEON = 1005;
  /** 触发事件 */
  int TASK_TYPE_TRIGGER_EVENT = 1006;
  /** 过程事件 */
  int TASK_TYPE_PROGRESS_EVENT = 1007;

  // 服务器本地事件类型
  /** 服务器本地事件：探索 */
  int TASK_LOCAL_EXPLORE = -1;
  /** 服务器本地事件：移动 */
  int TASK_LOCAL_MOVE = -2;
  /** 服务器本地事件：关卡 */
  int TASK_LOCAL_DUNGEON = -3;

  // 配置事件类型
  // 1001-BOSS击杀 =======>1.进入关卡
  // 1002-收集道具-常规 =======>2.直接获取道具
  // 1012-收集道具-剧情 =======>2.直接获取道具
  // 1022-收集道具-战斗 =======>1.进入关卡
  // 1003-击杀目标-数量 =======>1.进入关卡
  // 1013-击杀目标-具体怪物及数量=======>1.进入关卡
  // 1004-探索区域 =======>3.探索区域
  // 1005-占领据点 =======>1.进入关卡
  // 1006-剧情事件-常规 =======>4.无逻辑
  // 1016-剧情事件-战斗 =======>1.进入关卡
  // 2001-可见战斗 =======>1.进入关卡
  // 2002-宝箱事件-道具 =======>2.直接获取道具
  // 2012-宝箱事件-buff/debuff=======>5.获取buff/debuff
  // 2003-传送事件-随机传送 =======>6.传送事件-随机传送
  // 2013-传送事件-定点传送 =======>7.传送事件-定点传送
  // 2004-商店事件 =======>8.商店事件
  /** BOSS击杀 */
  int EVENT_TYPE_BOSS_FIGHT = 1001;
  /** 收集道具-常规 */
  int EVENT_TYPE_ITEM_NORMAL = 1002;
  /** 收集道具-剧情 */
  int EVENT_TYPE_ITEM_SCRIPT = 1012;
  /** 剧情事件-战斗 */
  int EVENT_TYPE_ITEM_FIGHT = 1022;
  /** 击杀目标-数量 */
  int EVENT_TYPE_FIGHT_NUM = 1003;
  /** 击杀目标-具体怪物及数量 */
  int EVENT_TYPE_FIGHT_ENEMY_NUM = 1013;
  /** 探索区域 */
  int EVENT_TYPE_EXPLORE_POINT = 1004;
  /** 占领据点 */
  int EVENT_TYPE_HOLD_POINT = 1005;
  /** 剧情事件-常规 */
  int EVENT_TYPE_SCRIPT_NORMAL = 1006;
  /** 剧情事件-战斗 */
  int EVENT_TYPE_SCRIPT_FIGHT = 1016;
  /** 可见战斗 */
  int EVENT_TYPE_VISIBLE_FIGHT = 2001;
  /** 宝箱事件-道具 */
  int EVENT_TYPE_BOX_ITEM = 2002;
  /** 宝箱事件-buff/debuff */
  int EVENT_TYPE_BOX_EFFECT = 2012;
  /** 传送事件-随机传送 */
  int EVENT_TYPE_PORTAL_RANDOM = 2003;
  /** 传送事件-定点传送 */
  int EVENT_TYPE_PORTAL_FIXED = 2013;
  /** 商店事件 */
  int EVENT_TYPE_STORE = 2004;
  /** 隐藏boss事件 */
  int EVENT_TYPE_HIDDEN_BOSS = 1101;
  /** 魔方事件 */
  int EVENT_TYPE_MAGIC_CUBE = 3001;
  /** 博弈筹码事件 */
  int EVENT_TYPE_GAMBLING = 3002;
  /** 遗忘残迹事件 */
  int EVENT_TYPE_ANCIENT = 3003;
  /** 卡巴拉之骰事件 */
  int EVENT_TYPE_DICE = 3004;
  /** 占卜事件 */
  int EVENT_TYPE_DIVINATION = 3005;
  /** 大凉山春节游戏 */
  int EVENT_TYPE_OFFICE_SPRING = 3006;

  // 道具
  /** 感染值道具：上阵英雄 */
  int HERO_INFECTION_TYPE_FORMATION = 1;
  /** 感染值道具：单个英雄 */
  int HERO_INFECTION_TYPE_SINGLE = 2;

  /** 道具类型：任务 */
  int ITEM_TYPE_TASK = 4;
  /** 道具类型：buff */
  int ITEM_TYPE_BUFF = 5;
  /** 道具类型：buff道具 */
  int ITEM_TYPE_BUFF_ITEM = 6;

  // ///////////////////////////////////////////
  // ///////////// buff类型 ////////////////
  // //////////////////////////////////////////
  /** buff类型：活动时效 */
  int BUFF_TYPE_ACTIVITY_TIME = 1;
  /** buff类型：战斗次数 */
  int BUFF_TYPE_FIGHT_COUNT = 2;
  // ///////////////////////////////////////////
  // ///////////// game类型 ////////////////
  // //////////////////////////////////////////
  /** 魔方游戏 */
  int GAME_TYPE_MAGIC_CUBE = 1001;
  /** 博弈筹码游戏 */
  int GAME_TYPE_GAMBLING = 1002;
  /** 遗忘残迹游戏 */
  int GAME_TYPE_ANCIENT = 1003;
  /** 卡巴拉之骰游戏 */
  int GAME_TYPE_DICE = 1004;
  /** 占卜游戏 */
  int GAME_TYPE_DIVINATION = 1005;
  /** 大凉山春节游戏 */
  int GAME_TYPE_OFFICE_SPRING = 1006;

  /** 大凉山春节游戏选项类型：无视 */
  int GAME_OFFICE_SPRING_OPTION_NOTHING = 1;
  /** 大凉山春节游戏选项类型：提示 */
  int GAME_OFFICE_SPRING_OPTION_TIPS = 2;
  /** 大凉山春节游戏选项类型：关卡 */
  int GAME_OFFICE_SPRING_OPTION_DUNGEON = 3;
  /** 大凉山春节游戏选项类型：获取道具 */
  int GAME_OFFICE_SPRING_OPTION_ITEM = 4;
  /** 大凉山春节游戏选项类型：对象 */
  int GAME_OFFICE_SPRING_OPTION_DIALOG = 5;

  /** 大凉山春节游戏结果类型：无视 */
  int GAME_OFFICE_SPRING_RESULT_NOTHING = 1;
  /** 大凉山春节游戏结果类型：不可用 */
  int GAME_OFFICE_SPRING_RESULT_DISABLE = 2;
  /** 大凉山春节游戏结果类型：移除 */
  int GAME_OFFICE_SPRING_RESULT_REMOVE = 3;
  /** 游戏消耗字段 */
  String GAME_COST_KEY = "answer";
  /** 游戏奖励字段 */
  String GAME_REWARD_KEY = "reward";
  /** 游戏占卜buff列表 */
  String GAME_DIVINATION_BUFF = "buff";
  /** 游戏骰子参数列表 */
  String GAME_DICE_ORDER = "buttonOrder";
  /** 游戏骰子参数数量 */
  String GAME_DICE_NUM = "buttonNum";

  ////////// 中间事件完结的点位处理类型
  /** 中间事件完结的点位处理类型：无视 */
  int PROGRESS_EVENT_POINT_NOTHING = 0;
  /** 中间事件完结的点位处理类型：禁用 */
  int PROGRESS_EVENT_POINT_DISABLE = 1;
  /** 中间事件完结的点位处理类型：移除 */
  int PROGRESS_EVENT_POINT_REMOVE = 2;
}
