package com.jjg.game.sim.constant;

/**
 * 经营信息看板统计数据 KEY 定义。
 * <p>
 * 与客户端"统计数据配置表"约定: 客户端凭 KEY 决定展示哪些数据及其图标/名称,
 * 服务端按 KEY 下发对应的数值 ({@link com.jjg.game.sim.pb.struct.StatInfo}).
 * <p>
 * 运营数据 1~99; SPINE游戏数据 101~199.
 *
 * @author 11
 * @date 2026/6/15
 */
public interface SimStatKey {

    /**
     * 运营数据 (当前场景)
     */
    interface Operation {
        //容纳游客人数 (value=当前可容纳, max=升满级最大容纳)
        int CAPACITY = 1;
        //雇员人数 (value=已激活, max=雇员总数)
        int EMPLOYEE = 2;
        //接待游客人次 (累计)
        int RECEPTION = 3;
        //经营收益 (累计金币)
        int BUSINESS_INCOME = 4;
        //完成任务数 (累计)
        int FINISH_TASK = 5;
        //观看广告数 (累计)
        int WATCH_AD = 6;
        //能量房间 每分钟产量
        int ENERGY_ROOM = 7;
        //游戏区金币总收益 (SLOT+扑克+捕鱼每分钟产量)
        int GOLD_INCOME = 8;
        //兼容旧客户端命名
        int SLOT_ROOM = GOLD_INCOME;
        //扑克房间 每分钟产量
        int POKER_ROOM = 9;
        //捕鱼房间 每分钟产量
        int FISHING_ROOM = 10;
        //接待区 服务能力值
        int RECEPTION_AREA = 11;
        //营销部 曝光度值
        int MARKETING_DEPT = 12;
        //运营部 运营值
        int OPERATIONS_DEPT = 13;
        //研发部 (value=已研发游戏数, max=游戏总数)
        int RESEARCH_DEPT = 14;
        //玩家所有娱乐城已解锁的SLOT游戏数
        int UNLOCK_GAME = 15;
    }

    /**
     * SPINE游戏数据 (>0指定游戏, 0所有游戏汇总)
     */
    interface Slot {
        //解锁游戏数 (当前场景)
        int UNLOCK_GAME = 101;
        //投注总数 (累计)
        int TOTAL_BET = 102;
        //SPINE次数 (累计旋转次数)
        int SPIN_COUNT = 103;
        //总赢奖 (累计)
        int TOTAL_WIN = 104;
        //最高赢奖 (单次)
        int MAX_WIN = 105;
        //最高倍数 (单次)
        int MAX_MULTIPLE = 106;
        //SWEET WIN 次数
        int SWEET_WIN = 107;
        //BIG WIN 次数
        int BIG_WIN = 108;
        //MEGA WIN 次数
        int MEGA_WIN = 109;
        //EPIC WIN 次数
        int EPIC_WIN = 110;
        //LEGENDARY WIN 次数
        int LEGENDARY_WIN = 111;
        //MINI 奖池次数
        int MINI = 112;
        //MINOR 奖池次数
        int MINOR = 113;
        //MAJOR 奖池次数
        int MAJOR = 114;
        //GRAND 奖池次数
        int GRAND = 115;
        //免费游戏触发次数
        int FREE_GAME = 116;
    }
}
