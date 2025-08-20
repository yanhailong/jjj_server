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
    int ILLEGAL_NAME = 416;
    // 房间创建数量已达上限
    int CREATE_ROOM_TO_LIMIT = 417;
    // 非法的好友房邀请码
    int ILLEGAL_FRIEND_ROOM_INVITATION_CODE = 418;
    // 未关注好友
    int NOT_FOLLOWED = 419;
    // 道具不足
    int NOT_ENOUGH_ITEM = 420;
    // 等级不足
    int Level_NOT_ENOUGH = 421;
    //  已达等级上限
    int BUILDING_LEVEL_IS_MAX = 422;
    // 服务器错误
    int EXCEPTION = 500;
}
