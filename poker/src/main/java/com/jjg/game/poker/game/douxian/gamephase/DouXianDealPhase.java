package com.jjg.game.poker.game.douxian.gamephase;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BasePokerPhase;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.data.DouXianZoneCards;
import com.jjg.game.poker.game.douxian.message.resp.NotifyDouXianDealCards;
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
 * 开局/补牌阶段，对应 DESIGN.md 3.2/3.3.5：
 * - 第1回合：全新洗牌，每人发8张手牌；
 * - 第2~4回合：从公共牌库补牌至8张（弃牌阶段只负责减牌，补牌统一放到下一回合开始时做，和文档措辞一致）。
 * 固定播放 {@link DouXianConstant.Time#DEAL_TIME} 的发牌动画后自动进入出牌阶段。
 * <p>
 * 补牌之后还要处理上一回合结算触发的得证大道/隐忍渡劫(DESIGN.md 三/8.13)：把该玩家场上
 * 三个区域的牌(不管是不是飞升锁定的)全部收回手牌，本回合这些区域要重新摆满。
 * 协议上没有单独给"收牌"开一个通知类型，直接并进这次的 {@link NotifyDouXianDealCards} 里，
 * 客户端看到的是"这次一起进手牌的牌"变多了，没有单独区分来源。
 */
public class DouXianDealPhase extends BasePokerPhase<DouXianGameDataVo> {

    public DouXianDealPhase(AbstractPhaseGameController<Room_ChessCfg, DouXianGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.START_GAME;
    }

    @Override
    public int getPhaseRunTime() {
        return DouXianConstant.Time.DEAL_TIME;
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        int round = gameDataVo.getRound();
        List<Integer> openZoneIds = new ArrayList<>();
        for (DouXianZone zone : DouXianZone.values()) {
            if (zone.isOpenAt(round)) {
                openZoneIds.add(zone.getId());
            }
        }
        long overTime = System.currentTimeMillis() + DouXianConstant.Time.DEAL_TIME + DouXianConstant.Time.PLAY_CARD_TIME;
        log.info("========== 斗仙牌第{}回合开始 roomCfgId:{} roomId:{} 开放区域:{} ==========",
                round, gameDataVo.getRoomCfg().getId(), gameController.getRoom().getId(), openZoneIds);

        // 本回合开始前携带金币快照，DESIGN.md 6.2 "小额玩家保护"判定依据之一
        gameDataVo.getRoundStartBalance().clear();
        for (Long playerId : gameDataVo.getActivePlayerIds()) {
            long balance = gameController.getTransactionItemNum(playerId);
            gameDataVo.getRoundStartBalance().put(playerId, balance);
            log.info("斗仙牌回合开始金币快照 round:{} playerId:{} balance:{}", round, playerId, balance);
        }

        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            long playerId = seatInfo.getPlayerId();
            List<Integer> hand = gameDataVo.getHandCards().computeIfAbsent(playerId, k -> new ArrayList<>());
            int need = DouXianConstant.Common.HAND_CARD_NUM - hand.size();
            List<Integer> drawn = need > 0 ? DouXianDataHelper.drawCards(gameDataVo, need) : List.of();
            hand.addAll(drawn);

            List<Integer> arrivedThisBeat = new ArrayList<>(drawn);
            Integer specialRuleType = gameDataVo.getPendingSpecialRule().remove(playerId);
            if (specialRuleType != null) {
                List<Integer> recalled = recallAllZoneCards(playerId);
                hand.addAll(recalled);
                arrivedThisBeat.addAll(recalled);
                log.info("斗仙牌{}生效，收回场上牌 playerId:{} ruleType:{} 收回数量:{}",
                        specialRuleType == 1 ? "得证大道" : "隐忍渡劫", playerId, specialRuleType, recalled.size());
            }

            NotifyDouXianDealCards notify = new NotifyDouXianDealCards();
            notify.round = round;
            notify.newCardIds = DouXianDataHelper.getClientCardIds(gameDataVo, arrivedThisBeat);
            notify.handCardNum = hand.size();
            notify.openZoneIds = openZoneIds;
            notify.overTime = overTime;
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(playerId, notify));

            log.info("斗仙牌发牌 round:{} playerId:{} 本次新到:{} 手牌共{}张:{}",
                    round, playerId, DouXianDataHelper.cfgIdsToString(gameDataVo, arrivedThisBeat),
                    hand.size(), DouXianDataHelper.cfgIdsToString(gameDataVo, hand));
        }
        log.info("斗仙牌发牌完成 roomCfgId:{} round:{} openZones:{}",
                gameDataVo.getRoomCfg().getId(), round, openZoneIds);
    }

    /**
     * 把某玩家凡/灵/仙三个区域当前的全部牌(锁定的+新摆的)收回手牌并清空区域
     *
     * @return 收回的牌(pokerPool配置id)
     */
    private List<Integer> recallAllZoneCards(long playerId) {
        List<Integer> recalled = new ArrayList<>();
        Map<DouXianZone, DouXianZoneCards> zones = gameDataVo.getPlayerZoneCards(playerId);
        for (DouXianZone zone : DouXianZone.values()) {
            DouXianZoneCards zc = zones.get(zone);
            recalled.addAll(zc.getAllCards());
            zc.clear();
        }
        return recalled;
    }

    @Override
    public void phaseFinish() {
        if (gameController instanceof BasePokerGameController<DouXianGameDataVo> controller) {
            controller.addPokerPhaseTimer(new DouXianPlayCardPhase(controller));
        }
    }

    @Override
    protected void robotActionOnPhaseStart(GameRobotPlayer gamePlayer) {
    }

    @Override
    protected void hostingPlayerActionOnPhaseStart(GamePlayer gamePlayer) {
    }
}
