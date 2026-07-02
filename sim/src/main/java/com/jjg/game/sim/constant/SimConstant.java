package com.jjg.game.sim.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/5/15
 */
public interface SimConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SIM_GAME << MessageConst.MessageCommon.RIGHT_MOVE;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //退出游戏
        int REQ_EXIT_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_EXIT_GAME = BASE_MSG_PREFIX | 0x4;

        //完成新手引导
        int REQ_FINISH_GUIDE = BASE_MSG_PREFIX | 0x5;
//        int RES_FINISH_GUIDE = BASE_MSG_PREFIX | 0x6;

        //通知客户端生成游客
        int NOTIFY_GENERATE_GUEST = BASE_MSG_PREFIX | 0x7;

        //请求解锁建筑
        int REQ_UNLOCK_BUILDING = BASE_MSG_PREFIX | 0x8;
        int RES_UNLOCK_BUILDING = BASE_MSG_PREFIX | 0x9;

        //获取技能
        int REQ_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xA;
        int RES_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xB;

        //升级技能
        int REQ_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xC;
        int RES_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xD;

        //请求升级建筑 (启动 CD)
        int REQ_UPGRADE_BUILDING = BASE_MSG_PREFIX | 0xE;
        int RES_UPGRADE_BUILDING = BASE_MSG_PREFIX | 0xF;

        //请求完成建筑升级 (CD 到时后客户端发起)
        int REQ_COMPLETE_BUILDING_UPGRADE = BASE_MSG_PREFIX | 0x10;
        int RES_COMPLETE_BUILDING_UPGRADE = BASE_MSG_PREFIX | 0x11;

        //请求清除建筑升级CD (钻石/道具)
        int REQ_CLEAR_BUILDING_CD = BASE_MSG_PREFIX | 0x12;
        int RES_CLEAR_BUILDING_CD = BASE_MSG_PREFIX | 0x13;

        //招募/解锁雇员
        int REQ_RECRUIT_EMPLOYEE = BASE_MSG_PREFIX | 0x14;
        int RES_RECRUIT_EMPLOYEE = BASE_MSG_PREFIX | 0x15;

        //升级雇员
        int REQ_UPGRADE_EMPLOYEE = BASE_MSG_PREFIX | 0x16;
        int RES_UPGRADE_EMPLOYEE = BASE_MSG_PREFIX | 0x17;

        //升星雇员
        int REQ_STAR_UP_EMPLOYEE = BASE_MSG_PREFIX | 0x18;
        int RES_STAR_UP_EMPLOYEE = BASE_MSG_PREFIX | 0x19;

        //任命主管 (按建筑类型)
        int REQ_ASSIGN_SUPERVISOR = BASE_MSG_PREFIX | 0x1A;
        int RES_ASSIGN_SUPERVISOR = BASE_MSG_PREFIX | 0x1B;

        //领取离线收益 (1倍/看广告2倍)
        int REQ_CLAIM_OFFLINE_REWARD = BASE_MSG_PREFIX | 0x1C;
        int RES_CLAIM_OFFLINE_REWARD = BASE_MSG_PREFIX | 0x1D;

        //获取建筑信息
        int REQ_BUILDING_INFO = BASE_MSG_PREFIX | 0x1E;
        int RES_BUILDING_INFO = BASE_MSG_PREFIX | 0x1F;

        //开辟新场景
        int REQ_UNLOCK_CASINO = BASE_MSG_PREFIX | 0x20;
        int RES_UNLOCK_CASINO = BASE_MSG_PREFIX | 0x21;

        //切换场景
        int REQ_SWITCH_CASINO = BASE_MSG_PREFIX | 0x22;
        int RES_SWITCH_CASINO = BASE_MSG_PREFIX | 0x23;

        //获取场景信息
        int REQ_CASINO_INFO = BASE_MSG_PREFIX | 0x24;
        int RES_CASINO_INFO = BASE_MSG_PREFIX | 0x25;

        //生成购买游客 (点击购买后立即生成, 只预生成目的地)
        int REQ_GEN_PURCHASED_GUEST = BASE_MSG_PREFIX | 0x26;
        int RES_GEN_PURCHASED_GUEST = BASE_MSG_PREFIX | 0x27;

        //领取购买游客奖励 (凭 uid 结算奖励)
        int REQ_PURCHASED_GUEST_REWARD = BASE_MSG_PREFIX | 0x28;
        int RES_PURCHASED_GUEST_REWARD = BASE_MSG_PREFIX | 0x29;

        //经营信息-运营数据
        int REQ_OPERATION_DATA = BASE_MSG_PREFIX | 0x2A;
        int RES_OPERATION_DATA = BASE_MSG_PREFIX | 0x2B;

        //经营信息-SPINE游戏数据 (>0指定游戏, 0所有游戏汇总)
        int REQ_SLOT_STAT = BASE_MSG_PREFIX | 0x2C;
        int RES_SLOT_STAT = BASE_MSG_PREFIX | 0x2D;

        //获取所有游客
        int REQ_ALL_GUEST = BASE_MSG_PREFIX | 0x2E;
        int RES_ALL_GUEST = BASE_MSG_PREFIX | 0x2F;

        //获取所有雇员
        int REQ_ALL_EMPLOYEE = BASE_MSG_PREFIX | 0x30;
        int RES_ALL_EMPLOYEE = BASE_MSG_PREFIX | 0x31;

        //招募游客
        int REQ_RECRUIT_GUEST = BASE_MSG_PREFIX | 0x32;
        int RES_RECRUIT_GUEST = BASE_MSG_PREFIX | 0x33;

        //升星游客
        int REQ_STAR_UP_GUEST = BASE_MSG_PREFIX | 0x34;
        int RES_STAR_UP_GUEST = BASE_MSG_PREFIX | 0x35;

        //获取已解锁羁绊
        int REQ_UNLOCK_BONDS = BASE_MSG_PREFIX | 0x36;
        int RES_UNLOCK_BONDS = BASE_MSG_PREFIX | 0x37;

        //获取游客卡池
        int REQ_GUEST_POOL = BASE_MSG_PREFIX | 0x38;
        int RES_GUEST_POOL = BASE_MSG_PREFIX | 0x39;

        //获取雇员卡池
        int REQ_EMPLOYEE_POOL = BASE_MSG_PREFIX | 0x3A;
        int RES_EMPLOYEE_POOL = BASE_MSG_PREFIX | 0x3B;

        //获取玩家信息
        int REQ_SIM_PLAYER_INFO = BASE_MSG_PREFIX | 0x3C;
        int RES_SIM_PLAYER_INFO = BASE_MSG_PREFIX | 0x3D;

        //任务列表 (主线+成就)
        int REQ_SIM_TASK_LIST = BASE_MSG_PREFIX | 0x3E;
        int RES_SIM_TASK_LIST = BASE_MSG_PREFIX | 0x3F;

        //领取任务奖励
        int REQ_SIM_TASK_REWARD = BASE_MSG_PREFIX | 0x40;
        int RES_SIM_TASK_REWARD = BASE_MSG_PREFIX | 0x41;

        //通知任务更新 (进度/状态变更)
        int NOTIFY_SIM_TASK_UPDATE = BASE_MSG_PREFIX | 0x42;

        //设置经营信息展示的成就勋章
        int REQ_SET_DISPLAYED_MEDALS = BASE_MSG_PREFIX | 0x43;
        int RES_SET_DISPLAYED_MEDALS = BASE_MSG_PREFIX | 0x44;

        //拜访赌场快照/随机切换
        int REQ_VISIT_CASINO = BASE_MSG_PREFIX | 0x45;
        int RES_VISIT_CASINO = BASE_MSG_PREFIX | 0x46;
        int REQ_RANDOM_VISIT = BASE_MSG_PREFIX | 0x47;

        //点赞/留言/送礼共用操作返回
        int REQ_VISIT_LIKE = BASE_MSG_PREFIX | 0x48;
        int RES_VISIT_ACTION = BASE_MSG_PREFIX | 0x49;
        int REQ_VISIT_COMMENT = BASE_MSG_PREFIX | 0x4A;
        int REQ_VISIT_GIFT = BASE_MSG_PREFIX | 0x4B;

        //记录/留言板
        int REQ_VISIT_RECORDS = BASE_MSG_PREFIX | 0x4C;
        int RES_VISIT_RECORDS = BASE_MSG_PREFIX | 0x4D;
        int REQ_VISIT_COMMENTS = BASE_MSG_PREFIX | 0x4E;
        int RES_VISIT_COMMENTS = BASE_MSG_PREFIX | 0x4F;
        int REQ_DELETE_VISIT_COMMENT = BASE_MSG_PREFIX | 0x50;
        int RES_DELETE_VISIT_COMMENT = BASE_MSG_PREFIX | 0x51;

        //当日汇总/人气榜
        int REQ_VISIT_SUMMARY = BASE_MSG_PREFIX | 0x52;
        int RES_VISIT_SUMMARY = BASE_MSG_PREFIX | 0x53;
        int REQ_VISIT_RANK = BASE_MSG_PREFIX | 0x54;
        int RES_VISIT_RANK = BASE_MSG_PREFIX | 0x55;

        //客座赌局
        int REQ_START_VISIT_TRIAL = BASE_MSG_PREFIX | 0x56;
        int RES_VISIT_TRIAL = BASE_MSG_PREFIX | 0x57;
        int REQ_EXIT_VISIT_TRIAL = BASE_MSG_PREFIX | 0x58;
    }

    interface Common {
        //默认解锁的场景id
        int DEFAULT_CASINO_ID = 1;
        //默认场景STATS id (CasinoStatsSheetCfg level=0)
        int DEFAULT_CASINO_STATS_ID = 1001;
        //"场景满员"判定窗口 (ms) — 10 分钟
        long CAPACITY_WINDOW_MS = 10 * 60 * 1000L;

        //服务能力加成系数基数 (VisitorStarCfg.Additioncoefficient, 100 表示 +0%)
        int SERVICE_CAPACITY_COEFFICIENT_BASE = 100;

        //雇员加成固定值换算基数 (固定值 / 1000 = 加成百分比, 雇员文档第243行)
        int EMPLOYEE_BONUS_DIVISOR = 1000;

        //广告收益倍数全局配置id (global.xlsx 135)
        int GLOBAL_AD_MULTIPLIER_ID = 135;
        int SOCIAL_SEND_GIFT_ID = 241;
        int ALLIANCE_DAILY_DONATE_ID = 229;
        int ALLIANCE_DONATE_ITEMS_ID = 230;
        int ALLIANCE_DONATE_REWARD_ID = 231;
        int ALLIANCE_DONATE_REPUTATION_ID = 242;
        int ALLIANCE_DAILY_FRESH_TASK = 243;
        int ALLIANCE_CREATE_ALLIANCE_CFG_ID = 222;

        //拜访系统全局配置
        int VISIT_LIKE_POPULARITY_ID = 232;
        int VISIT_COMMENT_POPULARITY_ID = 233;
        int VISIT_TRIAL_POPULARITY_ID = 234;
        int VISIT_DAILY_POPULARITY_LIMIT_ID = 235;
        int VISIT_DAILY_TRIAL_LIMIT_ID = 236;
        int VISIT_COMMISSION_RATE_ID = 237;
        int VISIT_DAILY_LIKE_LIMIT_ID = 243;
        int VISIT_DAILY_COMMENT_LIMIT_ID = 244;
        int VISIT_COMMENT_RECHARGE_ID = 245;
        int VISIT_COMMENT_MAX_LENGTH_ID = 246;
        int VISIT_RECORD_LIMIT_ID = 247;
        int VISIT_GIFT_LIST_ID = 248;
        int VISIT_DAILY_COMMISSION_LIMIT_ID = 249;
        int VISIT_TRIAL_SESSION_SECONDS_ID = 250;
        int VISIT_RANK_REWARD_ID = 251;

        //slots 每次旋转消耗的能量
        int SPIN_COST_POWER = 1;
        //slots 每次旋转增加的场景经验 (与消耗的能量保持一致)
        int SPIN_ADD_EXP = 1;
    }

    interface Global {
        //每次观看广告清除的时间(分钟)
        int ID_WATCH_AD_CLEAR_TIME = 136;
        //观看广告次数限制
        int ID_WATCH_AD_LIMIT = 137;
    }

    /**
     * 产出资源物品id
     */
    interface Item {
        //能量
        int ID_POWER = 1024001;
        //知名度
        int ID_AWARENESS = 1024002;
        //曝光度
        int ID_EXPOD = 1024003;
        //研究点
        int ID_RESEARCH_POINT = 1024005;
        //稀有研究点
        int ID_RARE_RESEARCH_POINT = 1024006;
        //加速卡
        int ID_CLEAR_CD = 1024007;

        //联盟-声誉值
        int ID_ALLIANCE_REPUTATION = 1024011;
        //联盟-贡献值
        int ID_ALLIANCE_CONTRIBUTION = 1024012;

        //指定游客id生成游客的道具
        int ID_BATCH_GENERATE_SPECIFY_ID_GUEST = 1024015;
        //指定游客品质生成游客的道具
        int ID_BATCH_GENERATE_SPECIFY_QUALITY_GUEST = 1024016;
        //白色品质游客
        int ID_GUEST_QULITY_WHITE = 1024019;
        //绿色品质游客
        int ID_GUEST_QULITY_GREEN = 1024020;
        //蓝色品质游客
        int ID_GUEST_QULITY_BLUE = 1024021;
        //紫色品质游客
        int ID_GUEST_QULITY_PUEPLE = 1024022;
        //金色品质游客
        int ID_GUEST_QULITY_GOLD = 1024023;
    }

    /**
     * 装备分类 (EquipmentTable.type)
     */
    interface EquipmentType {
        int DEVICE = 1;     //可交互设备
        //其它值: 装饰
    }

    interface Building {
        //运营部id
        int ID_OPERATIONS_DEPART = 1303;
    }

    interface ResearchPoint {
        //研究点类型
        //普通
        int NORMAL_TPYE = 1;
        //稀有
        int RARE_TPYE = 2;
    }

    /**
     * 大奖展示等级 (镜像 slots SlotsConst.BigWinShow, 用于 SPINE游戏统计的大奖次数分类)
     */
    interface BigWinShow {
        int SWEET = 1;
        int BIG = 2;
        int MEGA = 3;
        int EPIC = 4;
        int LEGENDARY = 5;
    }

    interface PoolList {
        int TYPE_GUEST = 1;
        int TYPE_EMPLOYEE = 2;
    }
}
