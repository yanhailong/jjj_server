package com.jjg.game.hall.friendroom.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 好友房消息常量
 *
 * @author 2CL
 */
public interface FriendRoomMessageConstant {

    int BASE_MSG_MASK = MessageConst.MessageTypeDef.FRIEND_ROOM_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgCons {
        // 请求创建好友房
        int REQ_CREAT_FRIENDS_ROOM = BASE_MSG_MASK | 0x01;
        // 房间面板信息
        int REQ_FRIENDS_ROOM_PANEL_DATA = BASE_MSG_MASK | 0x02;
        // 通过邀请码添加关注
        int REQ_JOIN_ROOM_BY_INVITATION_CODE = BASE_MSG_MASK | 0x03;
        // 请求操作房间好友列表
        int REQ_OPERATE_ROOM_FRIENDS_LIST = BASE_MSG_MASK | 0x04;
        // 请求关注玩家的房间列表
        int REQ_FRIEND_ROOM_LIST = BASE_MSG_MASK | 0x05;
        // 请求刷新好友房间列表
        int REQ_REFRESH_ROOM_FRIEND_LIST = BASE_MSG_MASK | 0x07;
        // 请求操作屏蔽玩家
        int REQ_OPERATE_SHIELD_PLAYER = BASE_MSG_MASK | 0x08;
        // 请求获取屏蔽玩家列表
        int REQ_SHIELD_PLAYER_LIST = BASE_MSG_MASK | 0x09;
        // 请求修改房间名
        int REQ_CHANGE_FRIEND_ROOM_NAME = BASE_MSG_MASK | 0x0A;
        // 请求好友房账单历史数据
        int REQ_FRIEND_ROOM_BILL_HISTORY = BASE_MSG_MASK | 0x0B;
        // 请求好友房详细账单历史数据
        int REQ_FRIEND_ROOM_DETAIL_BILL_HISTORY = BASE_MSG_MASK | 0x0C;
        // 请求操作好友房
        int REQ_OPERATE_FRIEND_ROOM = BASE_MSG_MASK | 0x0D;
        // 请求重置邀请码
        int REQ_RESET_INVITATION_CODE = BASE_MSG_MASK | 0x0E;
        // 请求好友账单数据中单局玩家数据
        int REQ_FRIEND_ROOM_BILL_PLAYER_INFO = BASE_MSG_MASK | 0x1F;
        // 请求一键领取账单收益奖励
        int REQ_TAKE_FRIEND_ROOM_BILL_INCOME = BASE_MSG_MASK | 0x20;
        // 请求进入好友房
        int REQ_JOIN_FRIEND_ROOM = BASE_MSG_MASK | 0x21;
    }

    interface ResMsgCons {
        // 返回创建好友房
        int RES_CREAT_FRIENDS_ROOM = BASE_MSG_MASK | 0x81;
        // 房间面板信息
        int RES_FRIENDS_ROOM_PANEL_DATA = BASE_MSG_MASK | 0x82;
        // 通过邀请码加入房间
        int RES_JOIN_ROOM_BY_INVITATION_CODE = BASE_MSG_MASK | 0x83;
        // 返回操作房间好友列表
        int RES_OPERATE_ROOM_FRIENDS_LIST = BASE_MSG_MASK | 0x84;
        // 返回关注玩家的房间列表
        int RES_FRIEND_ROOM_LIST = BASE_MSG_MASK | 0x85;
        // 返回刷新好友房间列表
        int RES_REFRESH_ROOM_FRIEND_LIST = BASE_MSG_MASK | 0x87;
        // 返回操作屏蔽玩家
        int RES_OPERATE_SHIELD_PLAYER = BASE_MSG_MASK | 0x88;
        // 返回获取屏蔽玩家列表
        int RES_SHIELD_PLAYER_LIST = BASE_MSG_MASK | 0x89;
        // 返回修改房间名
        int RES_CHANGE_FRIEND_ROOM_NAME = BASE_MSG_MASK | 0x8A;
        // 返回好友房账单历史数据
        int RES_FRIEND_ROOM_BILL_HISTORY = BASE_MSG_MASK | 0x8B;
        // 返回好友房详细账单历史数据
        int RES_FRIEND_ROOM_DETAIL_BILL_HISTORY = BASE_MSG_MASK | 0x8C;
        // 返回操作好友房
        int RES_OPERATE_FRIEND_ROOM = BASE_MSG_MASK | 0x8D;
        // 重置邀请码返回
        int RES_RESET_INVITATION_CODE = BASE_MSG_MASK | 0x8E;
        // 请求好友账单数据中单局玩家数据
        int RES_FRIEND_ROOM_BILL_PLAYER_INFO = BASE_MSG_MASK | 0x8F;
        // 请求一键领取账单收益奖励
        int RES_TAKE_FRIEND_ROOM_BILL_INCOME = BASE_MSG_MASK | 0x90;
        // 返回进入好友房
        int RES_JOIN_FRIEND_ROOM = BASE_MSG_MASK | 0x91;
    }
}
