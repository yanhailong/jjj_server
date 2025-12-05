package com.jjg.game.core.constant;

/**
 * @author 11
 * @date 2025/10/29 20:12
 */
public enum AddType {
    //gm测试
    GM_TEST(1),
    //失败回滚
    FAIL_ROLLBACK(2),
    //gm操作
    GM_OPERATOR(3),


    //道具兑换
    ITEM_EXCHANGE(101),
    //使用道具
    USE_ITEM(102),
    //道具掉落
    DROP_ITEM(103),
    //道具掉落(主分类)
    DROP_TRUNK_ITEM(104),


    //升级建筑
    UP_BUILD(201),
    //一键升级购买
    ONE_CLICK_UPGRADE_PURCHASE(202),
    //请求雇佣职员
    EMPLOYEE_STAFF(203),
    //加速清理
    CLEANUP_PROCESS(204),
    //加速升级
    UPGRADE_PROCESS(205),
    //一键领取赌场收益
    ONE_CLICK_CLAIM_GAMB_EARNINGS(206),
    //一键领取机台收益
    ONE_CLICK_CLAIM_TABKE_EARNINGS(207),
    //赌场
    GAMBLING(208),


    //绑定奖励
    BIND_REWARD(301),

    //创建好友房
    CREATE_FRIEND_ROOM(401),
    //管理好友房
    MANAGE_FRIEND_ROOM(402),



    //获取邮件奖励
    GET_MAIL_ITEMS(501),
    //一键领取邮件
    GET_ALL_MAILS_ITEMS(502),
    //删除邮件
    REMOVE_MAIL(503),


    //摇钱树
    ACTIVITY_CASHCOW(601),
    //摇钱树奖励
    ACTIVITY_CASHCOW_REWARDS(602),
    //参加摇钱树
    ACTIVITY_CASHCOW_JOIN(603),
    //摇钱树免费奖励
    ACTIVITY_CASHCOW_FREE_REWARDS(604),

    //推广分享
    ACTIVITY_SHARE_PROMOTE(605),
    //推广分享奖励
    ACTIVITY_SHARE_PROMOTE_REWARDS(606),

    //官方派奖
    ACTIVITY_OFFICIAL_AWARDS(607),
    //每日登录
    ACTIVITY_DAILY_LOGIN(608),
    //成长基金
    ACTIVITY_GROWTH_FUND_BUY(609),
    //成长基金领取奖励
    ACTIVITY_GROWTH_FUND_CLAIM_REWARDS(610),

    //购买特权卡
    ACTIVITY_PRIVILEGE_CARD_BUY(611),
    //领取特权卡奖励
    ACTIVITY_PRIVILEGE_REWARDS(612),

    //参加刮刮卡
    ACTIVITY_SCRATCH_CARDS_JOIN(613),
    //购买刮刮乐礼包
    ACTIVITY_SCRATCH_CARDS_BUY_GIFT(614),

    //储钱罐奖励
    ACTIVITY_PIGGY_BANK_REWARDS(615),
    //首充
    ACTIVITY_FIRST_PAYMENT(616),
    //活动
    ACTIVITY(617),
    //推广领取绑定奖励
    ACTIVITY_SHARE_PROMOTE_BIND_REWARDS(618),
    //财富转盘奖励
    ACTIVITY_WEALTH_ROULETTE_REWARDS(619),
    //财富转盘商城购买
    ACTIVITY_WEALTH_ROULETTE_BUY_REWARDS(620),
    //每日充值进度奖励
    ACTIVITY_DAILY_RECHARGE_PROGRESS(621),
    //每日充值购买礼包奖励
    ACTIVITY_DAILY_RECHARGE_GIFT(622),
    //好友房申请庄家扣除准备金
    FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE(701),
    //好友房取消申请庄家，添加准备金
    FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD(702),
    //好友房销毁房间回退预付金币
    FRIEND_ROOM_DESTROY_ROOM_BANKER_ADD_GOLD(703),
    //好友房离开房间回退预付金币
    FRIEND_ROOM_LEAVE_ROOM_ADD_GOLD(704),
    //好友房连续坐庄，自动下庄回退预付金币
    FRIEND_ROOM_CONTINUES_BANKER_ADD_GOLD(705),
    //好友房预付金币不足，自动下庄回退预付金币
    FRIEND_ROOM_PREDICATE_GOLD_NOT_ENOUGH(706),
    //好友房自动续费时长扣金币
    FRIEND_ROOM_AUTO_RENEW_TIME(707),
    //好友房解散时返还准备金
    FRIEND_ROOM_DISBAND_REBACK_GOLD(708),
    //好友房添加房主返还
    FRIEND_ROOM_ADD_ROOM_CREATOR_RATIO(709),
    //游戏结算
    GAME_SETTLEMENT(710),
    //游戏结算,庄家赢钱
    GAME_SETTLEMENT_BANKER_ADD(711),
    //游戏押注
    GAME_BET(712),
    //货币变化
    ROOM_CURRENCY_CHANGE(713),
    //好友房道具收益
    FRIEND_ROOM_INCOME_TAKE_ALL(714),


    //任务奖励
    TASKAWARD(801),


    //领取等级奖励
    LEVEL_CLAIM(901),
    //升级
    LEVEL_UPGRADE(902),

    //商城充值
    SHOP_RECHARGE(1001),

    //夺宝奇兵
    LUCKY_TREASURE_BUY(1101),
    //购买夺宝奇兵道具失败回滚
    LUCKY_TREASURE_BUY_EXCEPTION_ROLL_BACK(1102),
    //夺宝奇兵,活动状态变更，退还道具
    LUCKY_TREASURE_BUY_STATUSCHANGED_ROLL_BACK(1103),
    //夺宝奇兵,剩余数量不足，退还道具
    LUCKY_TREASURE_BUY_NOT_ENOUGH_ROLLBACK(1104),
    //夺宝奇兵,购买失败，退还道具
    LUCKY_TREASURE_BUY_FAILED_ROLLBACK(1105),
    //夺宝奇兵奖励
    LUCKY_TREASURE_REWARDS(1106),


    //slots押注
    SLOTS_BET(1201),
    //slots押注奖励
    SLOTS_BET_REWARD(1202),
    //投资奖励
    SLOTS_INVEST_REWARD(1203),
    //奖池奖励
    SLOTS_JACKPOT_REWARD(1204),
    //火车奖励
    SLOTS_TRAIN(1205),


    //积分大奖阶梯奖励
    POINTS_AWARD_LADDER_REWARDS(1301),
    //积分大奖转盘奖励
    POINTS_AWARD_TURNTABLE_REWARDS(1302),
    //积分大奖签到奖励
    POINTS_AWARD_SIGN_REWARDS(1303),


    //存入保险箱
    DEPOSIT_IN_SAFE_BOX(1401),
    //从保险箱取出
    WITHDRAW_FROM_SAFE_BOX(1402),


    //VIP奖励
    VIP_REWARDS(1501),
    //解散房间时保证金退还
    BANKER_PREDICATE_BACK(1502),
    //购买头像
    BUY_AVATAR(1601),

    //注册金币领取
    PLAYER_REGISTER(1602),
    ;


    private int value;

    AddType(int value){
        this.value = value;
    }

    public int getValue() {
        return value;
    }


    public static AddType valueOf(int value) {
        for (AddType addType : values()) {
            if (addType.value == value) {
                return addType;
            }
        }
        return null;
    }
}
