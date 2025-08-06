package com.jjg.game.poker.game.common;

import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.message.bean.PlayerInfo;
import com.jjg.game.poker.game.common.message.reps.NotifyPhaseChange;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author lm
 * @date 2025/7/26 11:37
 */
public class PokerBuilder {
    private PokerBuilder() {
    }

    /**
     * 构建玩家基本信息
     */
    public static PlayerInfo buildPlayerInfo(GamePlayer gamePlayer, BasePokerGameDataVo gameDataVo, boolean detail) {
        SeatInfo seatInfo = null;
        for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
            if (info.getPlayerId() == gamePlayer.getId()) {
                seatInfo = info;
            }
        }
        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.playerId = gamePlayer.getId();
        playerInfo.name = gamePlayer.getNickName();
        playerInfo.icon = gamePlayer.getNickName();
        if (seatInfo != null) {
            playerInfo.seatIndex = seatInfo.getSeatId();
            playerInfo.status = seatInfo.isSeatDown();
            playerInfo.playerStatus = seatInfo.isJoinGame();
        }
        if (gameDataVo instanceof TexasGameDataVo texasGameDataVo) {
            playerInfo.accountNumber = texasGameDataVo.getTempGold().getOrDefault(gamePlayer.getId(), 0L);
        } else {
            playerInfo.accountNumber = gamePlayer.getGold();
        }
        if (detail) {
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                if (info.getPlayerId() == gamePlayer.getId()) {
                    playerInfo.operationType = info.getOperationType();
                    break;
                }
            }
        }
        return playerInfo;
    }


    /**
     * 构建阶段变化消息
     *
     * @param gamePhase
     * @param endTime
     * @return
     */
    public static NotifyPhaseChange buildNotifyPhaseChange(EGamePhase gamePhase, long endTime) {
        NotifyPhaseChange notifyPhaseChange = new NotifyPhaseChange();
        notifyPhaseChange.phase = gamePhase;
        notifyPhaseChange.endTime = endTime;
        return notifyPhaseChange;
    }

}
