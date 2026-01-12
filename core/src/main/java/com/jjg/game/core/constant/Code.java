package com.jjg.game.core.constant;

/**
 * @author 11
 * @date 2025/5/26 11:34
 */
public interface Code {
    // 成功
    int SUCCESS = 200;
    // 配置表错误
    int SAMPLE_ERROR = 201;
    // 房间已销毁
    int ROOM_DESTROYED = 202;
    // 房间不存在
    int ROOM_NOT_FOUND = 203;
    // 房间处于关闭流程中
    int ROOM_STOPPING = 204;
    // 数据查询异常
    int QUERY_EXCEPTION = 205;
    // 失败
    int FAIL = 400;
    // 错误的请求
    int ERROR_REQ = 401;
    // 参数错误
    int PARAM_ERROR = 402;
    // 已存在
    int EXIST = 403;
    // 未找到
    int NOT_FOUND = 404;
    // 禁止
    int FORBID = 405;
    // 重复操作
    int REPEAT_OP = 406;
    // 过期
    int EXPIRE = 407;
    // 余额不足
    int NOT_ENOUGH = 408;
    // Vip等级不足
    int VIP_NOT_ENOUGH = 409;
    // 玩家金币超过配置
    int GOLD_TOO_MUCH = 410;
    // 下注已达上限
    int BET_TO_LIMIT = 411;
    // 未知错误
    int UNKNOWN_ERROR = 412;
    // 加入房间失败
    int JOIN_ROOM_FAILED = 413;
    // 区域下注已达上限
    int AREA_BET_TO_LIMIT = 414;
    // 重复加入房间
    int REPEAT_JOIN_ROOM = 415;
    // 非法名字
    int ILLEGAL_NAME = 49010;
    // 房间创建数量已达上限
    int CREATE_ROOM_TO_LIMIT = 49054;
    // 非法的好友房邀请码
    int ILLEGAL_FRIEND_ROOM_INVITATION_CODE = 49002;
    // 未关注好友
    int NOT_FOLLOWED = 419;
    // 道具不足
    int NOT_ENOUGH_ITEM = 420;
    // 等级不足
    int LEVEL_NOT_ENOUGH = 421;
    //  已达等级上限
    int BUILDING_LEVEL_IS_MAX = 422;
    // 邀请码重置次数不足
    int INVITATION_CODE_RESET_TIMES_NOT_ENOUGH = 423;
    // 邀请码重置次数不足
    int MAX_FOLLOWED_FRIENDS = 424;
    // 添加玩家黑名单数量达到限制值
    int ADD_BLACK_LIST_PLAYER_TO_LIMIT = 425;
    // 被封号，禁止登录
    int BAN_ACCOUNT = 426;
    // 好友未关注
    int FRIEND_NOT_FOLLOWED = 427;
    // 房主不能上庄
    int ROOM_CREATOR_CANT_BE_BANKER = 428;
    // 庄家金币不足以赔付
    int BANKER_GOLD_NOT_ENOUGH_TO_PAY = 49025;
    // 房间不能加入
    int ROOM_CANT_JOIN = 430;
    // 房间已满
    int ROOM_FULL = 431;
    // 黑名单
    int BAN_CAUSE_BLACK_LIST = 49041;
    // 德州坐下余额不足
    int TEXAS_NOT_ENOUGH = 433;
    // 使用未拥有的皮肤
    int NOT_UNLOCKED = 434;
    // 庄家不能押注
    int BANKER_CANT_BET = 49136;
    // 庄家不能押注
    int HOMEOWNER_CANT_BET = 49137;
    // 庄家不能退出游戏
    int HOMEOWNER_CANT_EXIT = 49138;
    //请输入您要增加的上庄准备金	开房间功能
    int AMOUNT_OF_RESERVES_IS_INCORRECT = 49139;
    // 上庄的准备金不足，和配置相比
    int AMOUNT_OF_RESERVES_IS_INCORRECT_CONFIG = 49140;
    //准备金不足(slots)
    int AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT = 49142;
    // 請輸入正確的code
    int CODE_ERROR = 436;
    // 不能绑定自己
    int BOUND_SELF = 437;
    // 該玩家已绑定
    int ALREADY_BOUND = 438;
    // 官方派奖奖池为0了
    int OFFICIAL_AWARDS_POOL_NULL = 439;
    // 重复穿戴头像
    int ALREADY_WORN = 439;
    // 待关闭服务器禁止修改房间信息
    int WAIT_CLOSE_NOT_MODIFICATION = 441;
    //积分大奖积分不足
    int POINT_AWARD_POINT_NOT_ENOUGH = 66019;
    //积分大奖次数不足
    int POINT_AWARD_TIMES_NOT_ENOUGH = 66020;
    // 非庄家不能修改准备金
    int CANT_EDIT_BANKER_GOLD = 49074;
    // 财富转盘积分不足
    int WEALTH_ROULETTE_NOT_POINT = 80011;
    // 财富转盘购买上限
    int WEALTH_ROULETTE_BUY_LIMIT = 80029;
    // 已领取所有带附件的邮件
    int MAIL_ITEM_ALL_GET = 11010;
    // 服务器错误
    int EXCEPTION = 500;
}
