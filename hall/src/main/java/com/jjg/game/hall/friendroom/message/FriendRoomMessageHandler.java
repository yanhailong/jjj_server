package com.jjg.game.hall.friendroom.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.req.*;
import com.jjg.game.hall.friendroom.services.FriendRoomServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 好友房消息handler
 *
 * @author 2CL
 */
@Component
@MessageType(MessageConst.MessageTypeDef.FRIEND_ROOM_TYPE)
public class FriendRoomMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomMessageHandler.class);
    @Autowired
    private FriendRoomServices friendRoomServices;

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CREAT_FRIENDS_ROOM)
    public void reqCreateFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        try {
            friendRoomServices.createFriendRoom(playerController, reqCreateFriendsRoom);
        } catch (Exception e) {
            log.error("创建好友房异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIENDS_ROOM_PANEL_DATA)
    public void reqFriendRoomPanelData(PlayerController playerController) {
        try {
            friendRoomServices.reqFriendPanelData(playerController);
        } catch (Exception e) {
            log.error("请求好友房面板数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_JOIN_ROOM_BY_INVITATION_CODE)
    public void reqFollowByInvitationCode(PlayerController playerController, ReqFollowByInvitationCode req) {
        try {
            friendRoomServices.reqFollowedByInvitationCode(playerController, req.invitationCode);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_ROOM_FRIENDS_LIST)
    public void reqOperateFollowedFriendsList(PlayerController playerController, ReqOperateFollowedFriendsList req) {
        try {
            friendRoomServices.reqOperateFollowedFriendsList(playerController, req);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_LIST)
    public void reqFollowedFriendRoomList(PlayerController playerController, ReqFollowedFriendRoomList req) {
        try {
            friendRoomServices.reqFollowedFriendRoomList(playerController, req);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_REFRESH_ROOM_FRIEND_LIST)
    public void reqRefreshFollowedFriendList(PlayerController playerController, ReqRefreshFollowedFriendList req) {
        try {
            friendRoomServices.reqRefreshFollowedFriendList(playerController, req);
        } catch (Exception e) {
            log.error("请求刷新关注好友列表异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_SHIELD_PLAYER)
    public void reqOperateShieldPlayer(PlayerController playerController, ReqOperateShieldPlayer req) {
        try {
            friendRoomServices.reqOperateShieldPlayer(playerController, req);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }


    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_SHIELD_PLAYER_LIST)
    public void reqShieldPlayerInFriendRoom(PlayerController playerController, ReqShieldPlayerInFriendRoom req) {
        try {
            friendRoomServices.reqPlayerBlackList(playerController);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CHANGE_FRIEND_ROOM_NAME)
    public void reqUpdateFriendRoomName(PlayerController playerController, ReqUpdateFriendRoomName req) {
        try {
            friendRoomServices.reqUpdateFriendRoomName(playerController, req);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CHANGE_FRIEND_ROOM_NAME)
    public void reqFriendRoomBillHistory(PlayerController playerController, ReqFriendRoomBillHistory req) {
        try {
            friendRoomServices.reqFriendRoomBillHistory(playerController, req);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_DETAIL_BILL_HISTORY)
    public void reqFriendRoomDetailBillHistory(PlayerController playerController, ReqFriendRoomDetailBillHistory req) {
        try {
            friendRoomServices.reqFriendRoomDetailBillHistory(playerController, req);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_RESET_INVITATION_CODE)
    public void reqResetInvitationCode(PlayerController playerController) {
        try {
            friendRoomServices.reqResetInvitationCode(playerController);
        } catch (Exception e) {
            log.error("请求刷新关注好友列表异常，{}", e.getMessage(), e);
        }
    }
}
