package com.jjg.game.core.constant;

/**
 * 全局表的常量ID
 *
 * @author 2CL
 */
public interface GlobalSampleConstantId {

    // 押注桌面展示的筹码数量上限
    int MAX_CHIP_ON_TABLE = 11;
    // 创建房间功能基础收益万分比
    int CREATE_ROOM_FUNC_INCOME_RATIO = 12;
    // 邀请码刷新间隔
    int INVITATION_REFRESH_INTERVAL = 13;
    // 好友房操作间隔时间
    int FRIEND_ROOM_OPERATE_INTERVAL = 15;
    // 连续坐庄上限
    int BE_BANKER_MAX_ROUND = 16;
    //我的赌场清除CD价值，每X秒需要Y钻石
    int CASINO_REDUCE_TIME_CONFIG = 17;
    //购买一键领取消耗 global表id
    int BUY_ALL_CLAIM_ALL_REWARDS = 19;
}
