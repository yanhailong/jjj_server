package com.jjg.game.common.constant;

/**
 * 功能类型
 *
 * @author 2CL
 */
public enum EFunctionType {

    // 小游戏
    MINI_GAME(1),
    // 摇钱树
    MONEY_TREE(2),
    // 官方派奖
    OFFICIAL_REWARD(3),
    // 积分大奖
    POINT_REWARD(4),
    // 分享推广
    SHARE_PROMOTION(5),
    // VIP功能
    VIP_FUNCTION(6),
    // 公告
    ANNOUNCEMENT(7),
    // 私人房间
    FRIEND_ROOM(8),
    // 商城界面
    SHOP_INTERFACE(9),
    //幸运夺宝
    LUCK_TREASURE(10),
    // 我的赌场
    MY_CASINO(101),
    // 每日奖金
    DAILY_BONUS(102),
    // 刮刮乐
    SCRATCH_CARD(103),
    // 储钱罐
    MONEY_BOX(104),
    // 等级礼包
    LEVEL_GIFT(105),
    // 首充礼包
    FIRST_RECHARGE_GIFT(106),
    // 成长基金
    GROWTH_FUND(107),
    // 签到奖励
    SIGN_IN_REWARD(108),
    // 邮箱
    MAIL(109),
    // 安全箱
    SAFE_BOX(110),
    // 客服
    CUSTOMER_SERVICE(111),
    // 设置
    SETTINGS(112),
    // 收集拼图
    PUZZLE_COLLECTION(113),
    // 每日充值
    DAILY_RECHARGE(114),
    // 排行
    RANKING(115),
    // 任务
    TASK(116),
    // 夺宝
    TREASURE_HUNT(117),
    // 钻石基金
    DIAMOND_FUND(118),
    // 限时礼包
    LIMITED_TIME_GIFT(119),
    // 财富轮盘
    WEALTH_ROULETTE(120);

    // 功能ID
    private final int functionId;

    EFunctionType(int functionId) {
        this.functionId = functionId;
    }

    public int getFunctionId() {
        return functionId;
    }

    public static EFunctionType getById(int id) {
        for (EFunctionType type : values()) {
            if (type.functionId == id) {
                return type;
            }
        }
        return null;
    }
}
