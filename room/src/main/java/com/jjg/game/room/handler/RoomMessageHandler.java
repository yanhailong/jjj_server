package com.jjg.game.room.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.ReqExitGame;
import com.jjg.game.core.pb.ResExitGame;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.message.RoomMessageConstant;
import com.jjg.game.room.message.req.ReqApplyBankerInFriendRoom;
import com.jjg.game.room.message.req.ReqEditBankerPredicateGold;
import com.jjg.game.room.message.resp.ResApplyBankerInFriendRoom;
import com.jjg.game.room.message.resp.ResBankerApplyListInFriendRoom;
import com.jjg.game.room.message.resp.ResCancelBeBankerInFriendRoom;
import com.jjg.game.room.message.resp.ResEditBankerPredicateGold;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author 11
 * @date 2025/7/15 15:23
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ROOM_TYPE)
public class RoomMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RoomManager roomManager;

    private final RoomEventListener playerEventListener;

    private final CoreLogger coreLogger;

    public RoomMessageHandler(RoomManager roomManager, RoomEventListener playerEventListener, CoreLogger coreLogger) {
        this.roomManager = roomManager;
        this.playerEventListener = playerEventListener;
        this.coreLogger = coreLogger;
    }


    @Command(MessageConst.RoomMessage.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqExitGame req) {
        try {
            long playerId = playerController.playerId();
            log.debug("退出游戏 playerId = {}", playerId);
            GamePlayer gamePlayer = null;
            if (playerController.getPlayer().getGameType() != EGameType.BACCARAT.getGameTypeId()) {
                AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                        roomManager.getGameControllerByRoomId(playerController.getPlayer().getRoomId());
                if (Objects.isNull(gameController)) {
                    playerController.send(new ResExitGame(Code.PARAM_ERROR));
                    return;
                }
                int core = gameController.canExitGame(playerId);
                if (core != Code.SUCCESS) {
                    playerController.send(new ResExitGame(core));
                    return;
                }
                gamePlayer = gameController.getGamePlayer(playerId);
            }
            int code = playerEventListener.exitGame(playerController);
            if (code == Code.SUCCESS && gamePlayer != null) {
                coreLogger.exitGame(gamePlayer, TimeHelper.nowInt() - gamePlayer.getEnterGameTime(), gamePlayer.getDeviceType());
            }
            playerController.send(new ResExitGame(code));
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }


    /**
     * 请求申请成为庄家
     */
    @Command(RoomMessageConstant.ReqMsgBean.REQ_APPLY_BANKER)
    public void reqApplyBanker(PlayerController playerController, ReqApplyBankerInFriendRoom req) {
        ResApplyBankerInFriendRoom res = new ResApplyBankerInFriendRoom(Code.PARAM_ERROR);
        long playerId = playerController.playerId();
        if (req.predictCostGold <= 0) {
            playerController.send(res);
            return;
        }
        AbstractFriendRoomController<?, ?> friendRoomController = getFriendRoomController(playerId);
        // 如果玩家不在房间中，或者不在好友房中
        if (friendRoomController == null) {
            res.code = Code.ROOM_NOT_FOUND;
            playerController.send(res);
            return;
        }
        // 申请成为庄家
        res.code = friendRoomController.supplyBeBanker(playerId, req.predictCostGold);
        log.debug("{} 请求上庄： {} {}", playerController.playerId(), res.code, req.predictCostGold);
        playerController.send(res);
    }

    /**
     * 取消成为庄家
     */
    @Command(RoomMessageConstant.ReqMsgBean.REQ_CANCEL_BE_BANKER)
    public void reqCancelBeBanker(PlayerController playerController) {
        ResCancelBeBankerInFriendRoom res = new ResCancelBeBankerInFriendRoom(Code.PARAM_ERROR);
        long playerId = playerController.playerId();
        AbstractFriendRoomController<?, ?> friendRoomController = getFriendRoomController(playerId);
        // 如果玩家不在房间中，或者不在好友房中
        if (friendRoomController == null) {
            res.code = Code.ROOM_NOT_FOUND;
            playerController.send(res);
            return;
        }
        // 调用房间的取消庄家,先标记，在下一轮开始前下庄
        res.code = friendRoomController.markBankerCancel(playerId);
        playerController.send(res);
    }

    /**
     * 请求庄家列表
     */
    @Command(RoomMessageConstant.ReqMsgBean.REQ_BANKER_APPLY_LIST)
    public void reqBankerList(PlayerController playerController) {
        AbstractFriendRoomController<?, ?> controller = getFriendRoomController(playerController.playerId());
        if (controller == null) {
            ResBankerApplyListInFriendRoom res = new ResBankerApplyListInFriendRoom(Code.ROOM_NOT_FOUND);
            playerController.send(res);
            return;
        }
        controller.reqBankerList(playerController);
    }

    /**
     * 请求修改庄家预付金币
     */
    @Command(RoomMessageConstant.ReqMsgBean.REQ_EDIT_BANKER_PREDICATE_GOLD)
    public void reqEditBankerPredicateGold(PlayerController playerController, ReqEditBankerPredicateGold req) {
        ResEditBankerPredicateGold res = new ResEditBankerPredicateGold(Code.ROOM_NOT_FOUND);
        AbstractFriendRoomController<?, ?> controller = getFriendRoomController(playerController.playerId());
        if (controller == null) {
            playerController.send(res);
            return;
        }
        res.code = controller.bankerEditPredicateGold(playerController, req.predicateGold);
        if (res.code != Code.SUCCESS) {
            playerController.send(res);
        }
    }

    /**
     * 通过玩家获取房间控制器
     */
    private AbstractFriendRoomController<?, ?> getFriendRoomController(long playerId) {
        AbstractRoomController<? extends RoomCfg, ? extends Room> roomController =
                roomManager.getRoomControllerByPlayer(playerId);
        // 如果玩家不在房间中，或者不在好友房中
        if (!(roomController instanceof AbstractFriendRoomController<?, ?> friendRoomController)) {
            return null;
        }
        return friendRoomController;
    }
}
