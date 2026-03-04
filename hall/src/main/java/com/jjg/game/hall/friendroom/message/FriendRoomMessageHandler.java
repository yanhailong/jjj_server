package com.jjg.game.hall.friendroom.message;

import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.hall.friendroom.constant.FriendRoomMessageConstant;
import com.jjg.game.hall.friendroom.message.req.*;
import com.jjg.game.hall.friendroom.message.res.ResManageFriendRoom;
import com.jjg.game.hall.friendroom.message.res.ResTakeFriendRoomBillIncome;
import com.jjg.game.hall.friendroom.message.res.RespCreateFriendsRoom;
import com.jjg.game.hall.friendroom.services.FriendRoomServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final FriendRoomServices friendRoomServices;
    private final GameFunctionService gameFunctionService;

    public FriendRoomMessageHandler(FriendRoomServices friendRoomServices, GameFunctionService gameFunctionService) {
        this.friendRoomServices = friendRoomServices;
        this.gameFunctionService = gameFunctionService;
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CREAT_FRIENDS_ROOM)
    public void reqCreateFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            int errCode = friendRoomServices.createFriendRoom(playerController, reqCreateFriendsRoom);
            if (errCode != Code.SUCCESS) {
                RespCreateFriendsRoom res = new RespCreateFriendsRoom(errCode);
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("创建好友房异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIENDS_ROOM_PANEL_DATA)
    public void reqFriendRoomPanelData(PlayerController playerController) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFriendPanelData(playerController);
        } catch (Exception e) {
            log.error("请求好友房面板数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_JOIN_ROOM_BY_INVITATION_CODE)
    public void reqFollowByInvitationCode(PlayerController playerController, ReqFollowByInvitationCode req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFollowedByInvitationCode(playerController, req.invitationCode);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_ROOM_FRIENDS_LIST)
    public void reqOperateFollowedFriendsList(PlayerController playerController, ReqOperateFollowedFriendsList req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqOperateFollowedFriendsList(playerController, req);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_LIST)
    public void reqFollowedFriendRoomList(PlayerController playerController, ReqFollowedFriendRoomList req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFollowedFriendRoomList(playerController, req);
        } catch (Exception e) {
            log.error("请求通过邀请码添加好友数据异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_REFRESH_ROOM_FRIEND_LIST)
    public void reqRefreshFollowedFriendList(PlayerController playerController, ReqRefreshFollowedFriendList req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqRefreshFollowedFriendList(playerController, req);
        } catch (Exception e) {
            log.error("请求刷新关注好友列表异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_SHIELD_PLAYER)
    public void reqOperateShieldPlayer(PlayerController playerController, ReqOperateShieldPlayer req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqOperateShieldPlayer(playerController, req);
        } catch (Exception e) {
            log.error("请求操作屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }


    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_SHIELD_PLAYER_LIST)
    public void reqShieldPlayerInFriendRoom(PlayerController playerController, ReqShieldPlayerInFriendRoom req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqPlayerBlackList(playerController);
        } catch (Exception e) {
            log.error("请求屏蔽玩家异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_CHANGE_FRIEND_ROOM_NAME)
    public void reqUpdateFriendRoomName(PlayerController playerController, ReqUpdateFriendRoom req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            int updateCode = friendRoomServices.reqUpdateFriendRoomData(playerController, req);
            if (updateCode != Code.SUCCESS) {
                ResManageFriendRoom res = new ResManageFriendRoom(updateCode);
                playerController.send(res);
            }
        } catch (Exception e) {
            log.error("请求更新房间名异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_BILL_HISTORY)
    public void reqFriendRoomBillHistory(PlayerController playerController, ReqFriendRoomBillHistory req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFriendRoomBillHistory(playerController, req);
        } catch (Exception e) {
            log.error("请求好友房账单历史，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_DETAIL_BILL_HISTORY)
    public void reqFriendRoomDetailBillHistory(PlayerController playerController, ReqFriendRoomDetailBillHistory req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFriendRoomDetailBillHistory(playerController, req);
        } catch (Exception e) {
            log.error("请求好友房详细账单历史，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_FRIEND_ROOM_BILL_PLAYER_INFO)
    public void reqFriendRoomBillPlayerInfo(PlayerController playerController, ReqFriendRoomBillPlayerInfo req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqFriendRoomBillPlayerInfo(playerController, req);
        } catch (Exception e) {
            log.error("请求好友房账单中玩家信息异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_TAKE_FRIEND_ROOM_BILL_INCOME)
    public void reqTakeFriendRoomIncomeReward(PlayerController playerController) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            int resCode = friendRoomServices.reqTakeFriendRoomIncomeReward(playerController.playerId());
            ResTakeFriendRoomBillIncome res = new ResTakeFriendRoomBillIncome(resCode);
            playerController.send(res);
        } catch (Exception e) {
            log.error("请求一键领取好友房收益奖励异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_OPERATE_FRIEND_ROOM)
    public void reqOperateFriendRoom(PlayerController playerController, ReqOperateFriendRoom req) {
        if (req.operateCode != 3 && !gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqOperateFriendRoom(playerController, req);
        } catch (Exception e) {
            log.error("请求操作好友房异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_RESET_INVITATION_CODE)
    public void reqResetInvitationCode(PlayerController playerController) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqResetInvitationCode(playerController);
        } catch (Exception e) {
            log.error("请求刷新关注好友列表异常，{}", e.getMessage(), e);
        }
    }

    @Command(FriendRoomMessageConstant.ReqMsgCons.REQ_JOIN_FRIEND_ROOM)
    public void reqJoinFriendRoom(PlayerController playerController, ReqJoinFriendRoom req) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.FRIEND_ROOM)) {
            return;
        }
        try {
            friendRoomServices.reqJoinFriendRoom(playerController, req);
        } catch (Exception e) {
            log.error("请求刷新关注好友列表异常，{}", e.getMessage(), e);
        }
    }
}
