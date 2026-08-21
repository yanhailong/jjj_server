package com.jjg.game.alliance.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 联盟模块常量。
 * <p>
 * 消息号段使用框架已预留的 {@link MessageConst.MessageTypeDef#ALLIANCE}(0x57)。
 * 标量型可调参数集中在 {@link Cfg}(死常量, 与需求文档一致);
 * 结构化配置表(等级表/任务表/商店表/捐献表/对决奖励表)集中在 {@code AllianceConfigService},
 * 后续策划 Excel 落表后只需改该服务的取数实现。
 *
 * @author 11
 * @date 2026/6/11
 */
public interface AllianceConst {

    interface RedDot {
        //每日首次免费捐献
        int FREE_DONATE = 1;
        //待处理入盟申请数量
        int APPLICATION = 2;
    }

    /**
     * 客户端 <-> 服务端 消息号 (messageType = ALLIANCE)
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ALLIANCE << MessageConst.MessageCommon.RIGHT_MOVE;

        //----------------- 信息查询 -----------------
        //联盟主界面信息 (联盟信息 + 我的贡献值; 无联盟时仅返回个人侧数据)
        int REQ_ALLIANCE_INFO = BASE_MSG_PREFIX | 0x01;
        int RES_ALLIANCE_INFO = BASE_MSG_PREFIX | 0x02;

        //可加入联盟列表 (按声誉值降序, 只含满足加入条件的)
        int REQ_ALLIANCE_LIST = BASE_MSG_PREFIX | 0x03;
        int RES_ALLIANCE_LIST = BASE_MSG_PREFIX | 0x04;

        //按 ID 搜索联盟
        int REQ_SEARCH_ALLIANCE = BASE_MSG_PREFIX | 0x05;
        int RES_SEARCH_ALLIANCE = BASE_MSG_PREFIX | 0x06;

        //----------------- 核心操作 -----------------
        //创建联盟
        int REQ_CREATE_ALLIANCE = BASE_MSG_PREFIX | 0x10;
        int RES_CREATE_ALLIANCE = BASE_MSG_PREFIX | 0x11;

        //加入联盟 (allianceId>0 申请指定联盟; allianceId=0 一键申请)
        int REQ_JOIN_ALLIANCE = BASE_MSG_PREFIX | 0x12;
        int RES_JOIN_ALLIANCE = BASE_MSG_PREFIX | 0x13;

        //退出联盟
        int REQ_QUIT_ALLIANCE = BASE_MSG_PREFIX | 0x14;
        int RES_QUIT_ALLIANCE = BASE_MSG_PREFIX | 0x15;

        //解散联盟 (盟主)
        int REQ_DISSOLVE_ALLIANCE = BASE_MSG_PREFIX | 0x16;
        int RES_DISSOLVE_ALLIANCE = BASE_MSG_PREFIX | 0x17;

        //编辑联盟 (盟主: 名称/图标/公告/入盟等级/审核开关)
        int REQ_EDIT_ALLIANCE = BASE_MSG_PREFIX | 0x18;
        int RES_EDIT_ALLIANCE = BASE_MSG_PREFIX | 0x19;

        //踢出成员 (盟主)
        int REQ_KICK_MEMBER = BASE_MSG_PREFIX | 0x1A;
        int RES_KICK_MEMBER = BASE_MSG_PREFIX | 0x1B;

        //转让盟主
        int REQ_TRANSFER_LEADER = BASE_MSG_PREFIX | 0x1C;
        int RES_TRANSFER_LEADER = BASE_MSG_PREFIX | 0x1D;

        //处理入盟申请 (同意/拒绝, 支持一键)
        int REQ_HANDLE_APPLICATION = BASE_MSG_PREFIX | 0x1E;
        int RES_HANDLE_APPLICATION = BASE_MSG_PREFIX | 0x1F;

        //入盟申请列表 (盟主)
        int REQ_APPLICATION_LIST = BASE_MSG_PREFIX | 0x20;
        int RES_APPLICATION_LIST = BASE_MSG_PREFIX | 0x21;

        //成员列表
        int REQ_MEMBER_LIST = BASE_MSG_PREFIX | 0x22;
        int RES_MEMBER_LIST = BASE_MSG_PREFIX | 0x23;

        //----------------- 联盟任务 -----------------
        //任务列表 (联盟任务池 + 我接取的任务)
        int REQ_TASK_LIST = BASE_MSG_PREFIX | 0x30;
        int RES_TASK_LIST = BASE_MSG_PREFIX | 0x31;

        //接取任务
        int REQ_ACCEPT_TASK = BASE_MSG_PREFIX | 0x32;
        int RES_ACCEPT_TASK = BASE_MSG_PREFIX | 0x33;

        //放弃任务
        int REQ_ABANDON_TASK = BASE_MSG_PREFIX | 0x34;
        int RES_ABANDON_TASK = BASE_MSG_PREFIX | 0x35;

        //任务完成/失败通知 (服务端 -> 客户端)
        int NOTIFY_TASK = BASE_MSG_PREFIX | 0x36;

        //刷新任务
        int REQ_REFRESH_TASK = BASE_MSG_PREFIX | 0x37;
        int RES_REFRESH_TASK = BASE_MSG_PREFIX | 0x41;

        //获取已完成任务列表
        int REQ_FINISHED_TASK = BASE_MSG_PREFIX | 0x42;
        int RES_FINISHED_TASK = BASE_MSG_PREFIX | 0x43;

        //----------------- 成员互助 -----------------
        //发起求助 (任务求助/建筑加速求助)
        int REQ_SEEK_HELP = BASE_MSG_PREFIX | 0x38;
        int RES_SEEK_HELP = BASE_MSG_PREFIX | 0x39;

        //帮助单个求助订单 (点击助力)
        int REQ_HELP = BASE_MSG_PREFIX | 0x3A;
        int RES_HELP = BASE_MSG_PREFIX | 0x3B;

        //一键帮助 (仅建筑加速类订单)
        int REQ_ONE_KEY_HELP = BASE_MSG_PREFIX | 0x3C;
        int RES_ONE_KEY_HELP = BASE_MSG_PREFIX | 0x3D;

        //求助订单列表
        int REQ_HELP_LIST = BASE_MSG_PREFIX | 0x3E;
        int RES_HELP_LIST = BASE_MSG_PREFIX | 0x3F;

        //被助力通知 (服务端 -> 求助者)
        int NOTIFY_HELPED = BASE_MSG_PREFIX | 0x40;

        //----------------- 联盟商店 -----------------
        //商店列表
        int REQ_SHOP_LIST = BASE_MSG_PREFIX | 0x44;
        int RES_SHOP_LIST = BASE_MSG_PREFIX | 0x45;

        //购买商品
        int REQ_SHOP_BUY = BASE_MSG_PREFIX | 0x46;
        int RES_SHOP_BUY = BASE_MSG_PREFIX | 0x47;

        //----------------- 联盟捐献 -----------------
        //捐献界面信息
        int REQ_DONATE_INFO = BASE_MSG_PREFIX | 0x48;
        int RES_DONATE_INFO = BASE_MSG_PREFIX | 0x49;

        //捐献
        int REQ_DONATE = BASE_MSG_PREFIX | 0x4A;
        int RES_DONATE = BASE_MSG_PREFIX | 0x4B;

        //----------------- 排行榜 -----------------
        //贡献度周榜 (盟内成员)
        int REQ_CONTRIB_RANK = BASE_MSG_PREFIX | 0x50;
        int RES_CONTRIB_RANK = BASE_MSG_PREFIX | 0x51;

        //联盟声誉排行 (全服)
        int REQ_ALLIANCE_RANK = BASE_MSG_PREFIX | 0x52;
        int RES_ALLIANCE_RANK = BASE_MSG_PREFIX | 0x53;

        //----------------- 联盟对决 -----------------
        //对决信息 (当前阶段/对手/比分/倒计时)
        int REQ_BATTLE_INFO = BASE_MSG_PREFIX | 0x58;
        int RES_BATTLE_INFO = BASE_MSG_PREFIX | 0x59;

        //对决报名 (盟主)
        int REQ_BATTLE_SIGNUP = BASE_MSG_PREFIX | 0x5A;
        int RES_BATTLE_SIGNUP = BASE_MSG_PREFIX | 0x5B;

        //对决贡献榜单 (盟内个人比赛值)
        int REQ_BATTLE_RANK = BASE_MSG_PREFIX | 0x5C;
        int RES_BATTLE_RANK = BASE_MSG_PREFIX | 0x5D;

        //领取对决阶段奖励 (个人)
        int REQ_BATTLE_STAGE_CLAIM = BASE_MSG_PREFIX | 0x5E;
        int RES_BATTLE_STAGE_CLAIM = BASE_MSG_PREFIX | 0x5F;

        //获取求助信息
        int REQ_GET_HELP_INFO = BASE_MSG_PREFIX | 0x60;
        int RES_GET_HELP_INFO = BASE_MSG_PREFIX | 0x61;

        //----------------- 通知 -----------------
        //联盟通用变更通知 (升级/被踢/解散/审批通过等, 以 type 区分)
        int NOTIFY_ALLIANCE = BASE_MSG_PREFIX | 0x70;


    }

    /**
     * 职位 (需求: 仅盟主/成员两种; "管理"职位文档中仅一句带过且权限表未列, 预留枚举值暂不开放)
     */
    interface Position {
        int LEADER = 1;
        int MANAGER = 2;
        int MEMBER = 3;
    }

    /**
     * 通用变更通知类型 (NOTIFY_ALLIANCE.type)
     */
    interface NotifyType {
        //被踢出联盟
        int KICKED = 1;
        //联盟解散
        int DISSOLVED = 2;
        //入盟申请被同意
        int APPLY_AGREED = 3;
        //入盟申请被拒绝
        int APPLY_REJECTED = 4;
        //联盟升级
        int LEVEL_UP = 5;
        //成为盟主 (被转让)
        int BECOME_LEADER = 6;
        //收到新的入盟申请 (推送给盟主)
        int NEW_APPLICATION = 7;
        //新求助订单 (推送给全盟, param=orderId, 前端在联盟频道渲染卡片)
        int NEW_HELP_ORDER = 8;
    }

    /**
     * 互助订单类型
     */
    interface HelpType {
        //任务求助 (帮助计入任务完成进度)
        int TASK = 1;
        //建筑加速求助 (每次点击助力减少建造CD)
        int BUILD_SPEEDUP = 2;
    }

    interface BattleState {
        //本期未创建/未到报名时间
        int NONE = 0;
        //报名中 (周三 9:00-21:00)
        int SIGNUP = 1;
        //报名截止待匹配 (周三 21:00 - 周四 10:00)
        int SIGNUP_CLOSED = 2;
        //匹配完成待开战 (周四 12:00 前)
        int MATCHED = 3;
        //对决进行中 (周四 12:00 - 周日 12:00)
        int FIGHTING = 4;
        //已结算
        int SETTLED = 5;
    }

    /**
     * 标量型配置 (死常量, 与需求文档一致; 需调整直接改这里)
     */
    interface Cfg {
        //--------- 创建/加入 ---------
        //创建/加入所需最低场景等级
        int CREATE_MIN_CASINO_LEVEL = 1;
        //联盟名称最大字符数 (前端限定 11 字符)
        int NAME_MAX_LEN = 11;
        //联盟描述/公告最大字符数
        int NOTICE_MAX_LEN = 30;
        //入盟最低场景等级设置范围
        int JOIN_LEVEL_MIN = 1;
        int JOIN_LEVEL_MAX = 99;
        //入盟申请列表上限 (防文档无界膨胀, 满则最早的申请被挤掉)
        int APPLICATION_LIMIT = 50;
        //申请有效期(ms): 超期惰性清理
        long APPLICATION_VALID_MILLS = 3 * 24 * 3600 * 1000L;

        //--------- 等级/人数 ---------
        //1级人数上限
        int BASE_MEMBER_LIMIT = 20;
        //每升1级增加人数
        int MEMBER_LIMIT_PER_LEVEL = 10;

        //--------- 任务 ---------
        //放弃任务冷却(秒)
        int ABANDON_TASK_CD_SEC = 5 * 60;
        //任务进度 Redis 计数 TTL(秒): 略大于最长任务持续时间即可
        int TASK_PROGRESS_TTL_SEC = 2 * 24 * 3600;
        //已完成任务保留条数 (最近 N 条滚动保留, 供"已完成任务"查询)
        int FINISHED_TASK_KEEP = 20;

        //--------- 互助
        //每人每日任务求助次数 (建筑加速求助走 global 表 249, 见 Global.SPEEDUP_DAILY_SEEK_LIMIT_ID)
        int DAILY_SEEK_HELP_LIMIT = 3;
        //单个任务求助订单可被帮助的次数上限 (需求: 同一任务只能由一名用户帮助)
        int TASK_ORDER_MAX_HELP = 1;
        //求助订单留存时间(ms): 超时惰性清理
        long HELP_ORDER_VALID_MILLS = 24 * 3600 * 1000L;
        //建筑加速抵扣 Redis key TTL(秒)
        int SPEEDUP_TTL_SEC = 7 * 24 * 3600;


        //--------- 排行榜 ---------
        //贡献度周榜显示条数
        int CONTRIB_RANK_SHOW = 50;
        //联盟声誉榜显示条数
        int ALLIANCE_RANK_SHOW = 10;

        //--------- 对决 ---------
        //报名所需最低联盟等级
        int BATTLE_MIN_LEVEL = 2;
        //报名人数模式: 1=总人数 2=近2天活跃人数 (后台配置项)
        int BATTLE_SIGNUP_MODE = 1;
        //报名最低(活跃)人数要求
        int BATTLE_MIN_MEMBERS = 1;
        //活跃人数统计窗口(天)
        int BATTLE_ACTIVE_DAYS = 2;
        //活跃时间写节流(ms): 距上次记录超过该值才回写共享联盟文档, 避免登录潮并发写同一文档;
        //远小于活跃统计窗口(天级), 不影响对决报名活跃人数判定
        long ACTIVE_TOUCH_THROTTLE_MILLS = 30 * 60 * 1000L;
        //对决期间消耗体力掉落比赛积分概率(万分比)
        int BATTLE_DROP_PROB = 5000;
        //单次掉落积分数
        int BATTLE_DROP_SCORE = 1;
        //对决贡献榜单批量拉取上限
        int BATTLE_RANK_SHOW = 100;
    }

    /**
     * Redis key。
     */
    interface RedisKey {
        //联盟 id 发号器 (INCR, 初始化为 100000, 保证 5 位数字)
        String ID_SEQ = "alliance:id:seq";
        //联盟数据变更失效广播频道 (payload=allianceId)
        String INVALIDATE_CHANNEL = "alliance:invalidate";
        //玩家->联盟映射失效广播频道 (payload=playerId, 逗号分隔支持批量)
        String PLAYER_INVALIDATE_CHANNEL = "alliance:invalidate:player";
        //玩家 -> 联盟 id 映射缓存 (string, 拼接 playerId)
        String PLAYER_ALLIANCE_PREFIX = "alliance:pid:";
        //声誉总榜 zset (member=allianceId)
        String RANK_REPUTATION = "alliance:rank:reputation";
        //赛季(自然月)声誉榜 zset 前缀 (拼接 yyyyMM)
        String RANK_SEASON_PREFIX = "alliance:rank:season:";
        //盟内贡献度周榜 zset 前缀 (拼接 allianceId:yyyyWW)
        String RANK_CONTRIB_PREFIX = "alliance:rank:contrib:";
        //玩家任务进度计数 (string INCR, 拼接 playerId:taskUid)
        String TASK_PROGRESS_PREFIX = "alliance:task:progress:";
        //建筑加速帮助累计抵扣秒数 (string INCR, 拼接 playerId:buildingId)
        String SPEEDUP_PREFIX = "alliance:help:speedup:";
        // 建筑加速待消费通知 (payload=playerId:buildingId)
        String SPEEDUP_PENDING_CHANNEL = "alliance:help:speedup:pending";
        //对决联盟总分 zset 前缀 (拼接 period)
        String BATTLE_SCORE_PREFIX = "alliance:battle:score:";
        //对决盟内个人分 zset 前缀 (拼接 period:allianceId)
        String BATTLE_PERSONAL_PREFIX = "alliance:battle:personal:";
        //对决阶段推进分布式锁
        String BATTLE_TICK_LOCK = "alliance:battle:tick:lock";
        //周榜/赛季榜结算分布式锁
        String RANK_SETTLE_LOCK = "alliance:rank:settle:lock";
    }

    interface ApplyFailReason {
        //已在联盟中
        int ALREADY_IN = 1;
        //人数已满
        int FULL = 2;
    }

    interface Global {
        int CREATE_MIN_CASINO_LEVEL_ID = 221;
        //任务池条数
        int TASK_POOL_SIZE_ID = 223;
        //每人每日可完成任务次数上限
        int DAILY_TASK_LIMIT_ID = 224;
        //用户建筑加速每日被帮助次数上限
        int SPEEDUP_DAILY_HELPED_LIMIT_ID = 225;
        //用户每日给盟友可提供的加速次数上限
        int SPEEDUP_DAILY_HELP_LIMIT_ID = 226;
        //用户的建筑被帮助加速一次可减少的时间(分钟)
        int SPEEDUP_MINUTES_PER_HELP_ID = 227;
        //用户帮助求助者一次可获得的贡献值数量
        int HELP_REWARD_CONTRIBUTION_ID = 228;
        //用户每日在建筑上进行分享(发起建筑加速求助)的次数上限
        int SPEEDUP_DAILY_SEEK_LIMIT_ID = 249;
    }
}
