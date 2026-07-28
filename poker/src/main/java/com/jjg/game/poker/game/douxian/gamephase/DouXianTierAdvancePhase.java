package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianBuilder;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.data.DouXianZoneCards;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPlayerInfo;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianTierAdvance;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 飞升阶段，DESIGN.md 3.3.3/8.10：
 * 仙界当前的牌(如果有)先舍弃回公共牌库，然后灵界的牌整体移入仙界成为锁定牌，
 * 凡界的牌整体移入灵界成为锁定牌，凡界清空。三步必须按 仙->灵->凡 的顺序执行——
 * 先清空仙界腾出位置，再把灵界的牌"倒"进去，最后灵界腾出的位置再接住凡界的牌，
 * 顺序反了会把还没转移的牌覆盖掉。
 */
public class DouXianTierAdvancePhase extends BasePokerPhase<DouXianGameDataVo> {

    public DouXianTierAdvancePhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.TIER_ADVANCE;
    }

    @Override
    public int getPhaseRunTime() {
        return DouXianConstant.Time.TIER_ADVANCE_EFFECT_TIME;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        if (!(gameController instanceof DouXianGameController controller)) {
            return;
        }
        int round = gameDataVo.getRound();
        List<Integer> allDiscardedToPool = new ArrayList<>();
        List<DouXianPlayerInfo> playerInfos = new ArrayList<>();

        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            long playerId = seatInfo.getPlayerId();
            Map<DouXianZone, DouXianZoneCards> zones = gameDataVo.getPlayerZoneCards(playerId);
            DouXianZoneCards immortal = zones.get(DouXianZone.IMMORTAL);
            DouXianZoneCards spirit = zones.get(DouXianZone.SPIRIT);
            DouXianZoneCards mortal = zones.get(DouXianZone.MORTAL);

            List<Integer> immortalDiscard = immortal.getAllCards();
            allDiscardedToPool.addAll(immortalDiscard);
            immortal.clear();

            List<Integer> spiritCarry = spirit.getAllCards();
            spirit.clear();
            immortal.getCarriedCards().addAll(spiritCarry);

            List<Integer> mortalCarry = mortal.getAllCards();
            mortal.clear();
            spirit.getCarriedCards().addAll(mortalCarry);

            log.info("斗仙牌飞升 playerId:{} 仙界舍弃回池:{} 灵界->仙界:{} 凡界->灵界:{}",
                    playerId, DouXianDataHelper.cfgIdsToString(gameDataVo, immortalDiscard),
                    DouXianDataHelper.cfgIdsToString(gameDataVo, spiritCarry),
                    DouXianDataHelper.cfgIdsToString(gameDataVo, mortalCarry));

            // 此时已过本回合结算亮牌，飞升后区域里只剩上一回合已公开的牌，selfView=true对谁都安全
            playerInfos.add(DouXianBuilder.buildPlayerInfo(playerId, controller, true));
        }
        DouXianDataHelper.returnCardsToPool(gameDataVo, allDiscardedToPool);

        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            long viewerId = seatInfo.getPlayerId();
            NotifyDouXianTierAdvance notify = new NotifyDouXianTierAdvance();
            notify.playerInfos = playerInfos;
            notify.discardedToPoolCount = allDiscardedToPool.size();
            notify.firstRound = round == 1;
            notify.selfHandCardIds = DouXianDataHelper.getClientCardIds(gameDataVo,
                    gameDataVo.getHandCards().getOrDefault(viewerId, List.of()));
            notify.hasSelfSnapshot = true;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(viewerId, notify));
        }
        log.info("斗仙牌飞升完成 roomCfgId:{} round:{} 舍弃回牌库:{}张",
                gameDataVo.getRoomCfg().getId(), round, allDiscardedToPool.size());
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof DouXianGameController controller) {
            controller.addPokerPhaseTimer(new DouXianDiscardPhase(controller));
        }
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
