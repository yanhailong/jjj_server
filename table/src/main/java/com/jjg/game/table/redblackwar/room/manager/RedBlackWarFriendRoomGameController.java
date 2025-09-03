package com.jjg.game.table.redblackwar.room.manager;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseFriendRoomTableGameController;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarBetPhase;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarSettlementPhase;
import com.jjg.game.table.redblackwar.gamephase.RedBlackWarTableWaitReadyPhase;
import com.jjg.game.table.redblackwar.message.RedBlackMessageBuilder;
import com.jjg.game.table.redblackwar.message.resp.NotifyRedBlackWarInfo;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;

import java.util.LinkedHashSet;

/**
 * 红黑大战游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.RED_BLACK_WAR, roomType = RoomType.BET_TEAM_UP_ROOM)
public class RedBlackWarFriendRoomGameController extends BaseFriendRoomTableGameController<RedBlackWarGameDataVo> {

    public RedBlackWarFriendRoomGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RED_BLACK_WAR;
    }

    /**
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        gamePhases.add(new RedBlackWarTableWaitReadyPhase(this));
        gamePhases.add(new RedBlackWarBetPhase(this));
        gamePhases.add(new RedBlackWarSettlementPhase(this));
        return gamePhases;
    }


    @Override
    protected RedBlackWarGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new RedBlackWarGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        //发送房间信息
        RedBlackWarGameDataVo dataVo = getGameDataVo();
        NotifyRedBlackWarInfo notifyRedBlackWarInfo =
            RedBlackMessageBuilder.buildInitInfo(playerController.playerId(), dataVo, getCurrentGamePhase());
        //发送给玩家
        broadcastToPlayers(
            RoomMessageBuilder.newBuilder().addPlayerId(playerController.playerId()).setData(notifyRedBlackWarInfo));
    }

}
