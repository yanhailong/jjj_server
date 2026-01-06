package com.jjg.game.table.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.AbstractMsgDealRoomPhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.BaseRoomMessageDispatcher;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.bean.PlayerChip;
import com.jjg.game.table.common.message.req.ReqOnlinePlayerChipInfo;
import com.jjg.game.table.common.message.req.ReqPlayerInfo;
import com.jjg.game.table.common.message.req.ReqRoomBaseInfo;
import com.jjg.game.table.common.message.req.ReqTablePlayerInfo;
import com.jjg.game.table.common.message.res.ResOnlinePlayerChipInfo;
import com.jjg.game.table.common.message.res.ResPlayerInfo;
import com.jjg.game.table.common.message.res.RespTablePlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 下注对战类的消息分发器，目前主要处理实现了{@linkplain AbstractMsgDealRoomPhase}类的消息类
 *
 * @author 2CL
 */
@MessageType(value = MessageConst.MessageTypeDef.BET_GENERAL_TYPE, isGroupMessage = true)
@Component
public class TableGroupMessageDispatcher extends BaseRoomMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TableGroupMessageDispatcher.class);

    /**
     * 请求发送消息
     *
     * @param message message
     */
    @Command(value = MessageConst.MessageTypeDef.BET_GENERAL_TYPE, isGroupMsgDispatcher = true)
    @Override
    public void dispatchMsg(PlayerController playerController, PFMessage message) {
        super.dispatchMsg(playerController, message);
    }

    /**
     * 请求牌桌上玩家信息
     */
    @Command(value = TableRoomMessageConstant.ReqMsgBean.REQ_TABLE_PLAYER_INFO)
    public void reqTablePlayerInfo(PlayerController playerController, ReqTablePlayerInfo reqTablePlayerInfo) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        int code = checkPlayerInGame(gameController, playerController);
        if (code != Code.SUCCESS) {
            log.warn("请求玩家信息时失败 CODE：{}", code);
            playerController.send(new RespTablePlayerInfo(code));
            return;
        }
        TableGameDataVo tableGameDataVo = (TableGameDataVo) gameController.getGameDataVo();
        // 更新操作时间
        tableGameDataVo.updatePlayerOperateTime(playerController.playerId());
        RespTablePlayerInfo respTablePlayerInfo =
                TableMessageBuilder.buildTableAllPlayerInfo(gameController, tableGameDataVo);
        playerController.send(respTablePlayerInfo);
    }

    /**
     * 请求牌桌上玩家信息
     */
    @Command(value = TableRoomMessageConstant.ReqMsgBean.REQ_ONLINE_PLAYER_CHIP_INFO)
    public void reqOnlinePlayerChipInfo(PlayerController playerController, ReqOnlinePlayerChipInfo req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        ResOnlinePlayerChipInfo resOnlinePlayerChipInfo = new ResOnlinePlayerChipInfo(Code.PARAM_ERROR);
        if (gameController instanceof BaseTableGameController<? extends TableGameDataVo> controller) {
            resOnlinePlayerChipInfo.chips = new ArrayList<>();
            TableGameDataVo gameDataVo = controller.getGameDataVo();
            for (GamePlayer gamePlayer : gameDataVo.getGamePlayerMap().values()) {
                PlayerChip playerChip = new PlayerChip();
                playerChip.playerId = gamePlayer.getId();
                playerChip.chipId = gamePlayer.getChipsId();
                resOnlinePlayerChipInfo.chips.add(playerChip);
            }
            resOnlinePlayerChipInfo.code = Code.SUCCESS;
        }
        playerController.send(resOnlinePlayerChipInfo);
    }

    /**
     * 请求牌桌上玩家信息
     */
    @Command(value = TableRoomMessageConstant.ReqMsgBean.REQ_PLAYER_INFO)
    public void ReqPlayerInfo(PlayerController playerController, ReqPlayerInfo req) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        ResPlayerInfo msg = new ResPlayerInfo(Code.SUCCESS);
        if (gameController instanceof BaseTableGameController<? extends TableGameDataVo> controller) {
            GamePlayer gamePlayer = controller.getGamePlayer(playerController.playerId());
            if (gamePlayer != null) {
                msg.tablePlayerInfo = TableMessageBuilder.buildTablePlayerInfo(controller, gamePlayer);
            }
        }
        playerController.send(msg);
    }


    /**
     * 玩家发送房间初始信息 客户端在刚进入房间时，不能收到服务端的主动推送，所以需要等客户端初始化完成后，主动向服务端请求
     */
    @Command(value = TableRoomMessageConstant.ReqMsgBean.REQ_ROOM_BASE_INFO)
    public void reqRoomBaseInfo(PlayerController playerController, ReqRoomBaseInfo reqRoomBaseInfo) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        int code = checkPlayerInGame(gameController, playerController);
        if (code != Code.SUCCESS) {
            log.warn("请求房间初始化消息时失败 CODE：{}", code);
            return;
        }
        TableGameDataVo tableGameDataVo = (TableGameDataVo) gameController.getGameDataVo();
        // 更新操作时间
        tableGameDataVo.updatePlayerOperateTime(playerController.playerId());
        gameController.respRoomInitInfo(playerController);
    }

    /**
     * 检查当前玩家是否在游戏中
     */
    private int checkPlayerInGame(AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController,
                                  PlayerController playerController) {
        if (gameController == null) {
            log.error("玩家： {} 找不到对应的房间", playerController.playerId());
            return Code.ROOM_NOT_FOUND;
        }
        if (!(gameController.getGameDataVo() instanceof TableGameDataVo)) {
            log.error("玩家： {} 不在table类游戏中请求数据", playerController.playerId());
            return Code.PARAM_ERROR;
        }
        return Code.SUCCESS;
    }
}
