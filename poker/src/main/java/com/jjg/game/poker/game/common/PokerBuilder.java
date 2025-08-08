package com.jjg.game.poker.game.common;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.Objects;

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
    public static PokerPlayerInfo buildPlayerInfo(GamePlayer gamePlayer, SeatInfo seatInfo, BasePokerGameDataVo gameDataVo, boolean detail) {
        if (Objects.isNull(seatInfo)) {
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                if (info.getPlayerId() == gamePlayer.getId()) {
                    seatInfo = info;
                }
            }
        }
        PokerPlayerInfo pokerPlayerInfo = new PokerPlayerInfo();
        pokerPlayerInfo.playerId = gamePlayer.getId();
        pokerPlayerInfo.name = gamePlayer.getNickName();
        pokerPlayerInfo.icon = gamePlayer.getNickName();
        if (seatInfo != null) {
            pokerPlayerInfo.seatIndex = seatInfo.getSeatId();
            pokerPlayerInfo.status = seatInfo.isSeatDown();
            pokerPlayerInfo.playerStatus = seatInfo.isJoinGame();
        }
        if (gameDataVo instanceof TexasGameDataVo texasGameDataVo) {
            pokerPlayerInfo.accountNumber = texasGameDataVo.getTempGold().getOrDefault(gamePlayer.getId(), 0L);
        } else {
            pokerPlayerInfo.accountNumber = gamePlayer.getGold();
        }
        if (detail) {
            for (PlayerSeatInfo info : gameDataVo.getPlayerSeatInfoList()) {
                if (info.getPlayerId() == gamePlayer.getId()) {
                    pokerPlayerInfo.operationType = info.getOperationType();
                    break;
                }
            }
        }
        return pokerPlayerInfo;
    }

    /**
     * 构建玩家基本信息
     */
    public static PokerPlayerInfo buildPlayerInfo(GamePlayer gamePlayer, BasePokerGameDataVo gameDataVo, boolean detail) {
        return buildPlayerInfo(gamePlayer, null, gameDataVo, detail);
    }


    /**
     * 构建阶段变化消息
     *
     */
    public static NotifyPokerPhaseChange buildNotifyPhaseChange(EGamePhase gamePhase, long endTime) {
        NotifyPokerPhaseChange notifyPokerPhaseChange = new NotifyPokerPhaseChange();
        notifyPokerPhaseChange.phase = gamePhase;
        notifyPokerPhaseChange.endTime = endTime;
        return notifyPokerPhaseChange;
    }

}
