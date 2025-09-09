package com.jjg.game.poker.game.common;

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
    public static PokerPlayerInfo buildPlayerInfo(GamePlayer gamePlayer, SeatInfo seatInfo, BasePokerGameDataVo gameDataVo) {
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
        pokerPlayerInfo.icon = gamePlayer.getHeadImgId();
        pokerPlayerInfo.chipsId = gamePlayer.getChipsId();
        pokerPlayerInfo.cardBackgroundId = gamePlayer.getCardBackgroundId();
        pokerPlayerInfo.headFrameId = gamePlayer.getHeadFrameId();
        pokerPlayerInfo.nationalId = gamePlayer.getNationalId();
        pokerPlayerInfo.titleId = gamePlayer.getTitleId();
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
        return pokerPlayerInfo;
    }


    /**
     * 构建阶段变化消息
     */
    public static NotifyPokerPhaseChange buildNotifyPhaseChange(EGamePhase gamePhase, long endTime) {
        NotifyPokerPhaseChange notifyPokerPhaseChange = new NotifyPokerPhaseChange();
        notifyPokerPhaseChange.phase = gamePhase;
        notifyPokerPhaseChange.endTime = endTime;
        return notifyPokerPhaseChange;
    }

    /**
     * 返回基本的玩家信息(不包含操作类型)
     */
    public static PokerPlayerInfo getPokerPlayerInfo(SeatInfo seatInfo, BasePokerGameDataVo gameDataVo) {
        PokerPlayerInfo pokerPlayerInfo = new PokerPlayerInfo();
        pokerPlayerInfo.playerId = seatInfo.getPlayerId();
        pokerPlayerInfo.playerStatus = seatInfo.isJoinGame();
        pokerPlayerInfo.status = seatInfo.isSeatDown();
        pokerPlayerInfo.seatIndex = seatInfo.getSeatId();
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
        if (Objects.nonNull(gamePlayer)) {
            pokerPlayerInfo.name = gamePlayer.getNickName();
            pokerPlayerInfo.icon = gamePlayer.getHeadImgId();
            pokerPlayerInfo.chipsId = gamePlayer.getChipsId();
            pokerPlayerInfo.cardBackgroundId = gamePlayer.getCardBackgroundId();
            pokerPlayerInfo.headFrameId = gamePlayer.getHeadFrameId();
            pokerPlayerInfo.nationalId = gamePlayer.getNationalId();
            pokerPlayerInfo.titleId = gamePlayer.getTitleId();
            if (gameDataVo instanceof TexasGameDataVo texasGameDataVo) {
                pokerPlayerInfo.accountNumber = texasGameDataVo.getTempGold().getOrDefault(gamePlayer.getId(), 0L);
            } else {
                pokerPlayerInfo.accountNumber = gamePlayer.getGold();
            }
        }
        return pokerPlayerInfo;
    }
}
