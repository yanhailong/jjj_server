package com.jjg.game.poker.game.douxian.data;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.message.bean.DouXianGrandSettlementPlayerInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPlayerInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZonePlacementInfo;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.poker.game.douxian.util.DouXianHandEvaluator;
import com.jjg.game.poker.game.douxian.util.DouXianHandResult;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组装斗仙牌相关的通知消息内容
 */
public final class DouXianBuilder {

    private DouXianBuilder() {
    }

    /**
     * 构建某个玩家当前三区域的摆牌快照，区域满了才计算牌型/灵力值，没摆满只展示牌面。
     *
     * @param selfView 是否以"本人"视角构建：true=完整展示(自己看自己/结算亮牌后广播)；
     *                 false=以"他人"视角构建，本回合尚未结算的新摆牌只给数量不给牌面/牌型，防止结算前偷看对手牌，见DESIGN.md 8.8
     */
    public static DouXianPlayerInfo buildPlayerInfo(long playerId, DouXianGameController controller, boolean selfView) {
        DouXianGameDataVo gameDataVo = controller.getGameDataVo();
        DouXianPlayerInfo info = new DouXianPlayerInfo();
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        SeatInfo seatInfo = gameDataVo.getSeatInfo().values().stream()
                .filter(s -> s.getPlayerId() == playerId).findFirst().orElse(null);
        if (gamePlayer != null) {
            info.pokerPlayerInfo = PokerBuilder.buildPlayerInfo(gamePlayer, seatInfo, controller);
        }
        info.handCardNum = gameDataVo.getHandCards().getOrDefault(playerId, List.of()).size();
        info.confirmed = gameDataVo.getConfirmedPlayerIds().contains(playerId);
        info.hosting = gameDataVo.getHostingPlayerIds().contains(playerId);
        info.conceded = gameDataVo.getConcededPlayerIds().contains(playerId);
        info.ready = gameDataVo.getReadyPlayerIds().contains(playerId);
        info.zones = buildZonePlacements(playerId, gameDataVo, selfView);
        return info;
    }

    public static List<DouXianZonePlacementInfo> buildZonePlacements(long playerId, DouXianGameDataVo gameDataVo, boolean selfView) {
        List<DouXianZonePlacementInfo> zones = new ArrayList<>();
        Map<DouXianZone, com.jjg.game.poker.game.douxian.data.DouXianZoneCards> zoneMap =
                gameDataVo.getPlayerZoneCards(playerId);
        int round = gameDataVo.getRound();
        for (DouXianZone zone : DouXianZone.values()) {
            com.jjg.game.poker.game.douxian.data.DouXianZoneCards zc = zoneMap.get(zone);
            List<Integer> visibleCfgIds = selfView ? zc.getAllCards() : new ArrayList<>(zc.getCarriedCards());
            int hiddenCount = selfView ? 0 : zc.getNewCards().size();
            if (visibleCfgIds.isEmpty() && hiddenCount == 0) {
                continue;
            }
            DouXianZonePlacementInfo placement = new DouXianZonePlacementInfo();
            placement.zoneId = zone.getId();
            placement.cardIds = DouXianDataHelper.getClientCardIds(gameDataVo, visibleCfgIds);
            placement.hiddenCount = hiddenCount;
            placement.locked = !zc.getCarriedCards().isEmpty() && zc.getNewCards().isEmpty();
            if (selfView && zc.isFull()) {
                List<Card> cards = DouXianDataHelper.toCards(gameDataVo, zc.getAllCards());
                DouXianHandResult result = DouXianHandEvaluator.evaluateZone(zone, cards, round);
                placement.handTypeName = result.getHandType().getDisplayName();
                placement.aetherValue = result.getAetherValue();
            }
            zones.add(placement);
        }
        return zones;
    }

    /**
     * 组装大结算(DESIGN.md 8.12)的玩家结果列表：座位没被标记删除的人都会出现(含已认输的)，
     * 中途认输/离场玩家的 roundChangeList 会比其他人短——只记录了实际打过结算的那几个回合，
     * 不会补0凑够4个，前端按实际长度展示就行。头像/昵称等基本信息也带上，前端结算弹窗不用再单独查。
     */
    public static List<DouXianGrandSettlementPlayerInfo> buildGrandSettlementPlayerInfos(DouXianGameController controller) {
        DouXianGameDataVo gameDataVo = controller.getGameDataVo();
        List<DouXianGrandSettlementPlayerInfo> result = new ArrayList<>();
        for (PlayerSeatInfo seatInfo : gameDataVo.getPlayerSeatInfoList()) {
            if (seatInfo.isDelState()) {
                continue;
            }
            long playerId = seatInfo.getPlayerId();
            DouXianGrandSettlementPlayerInfo info = new DouXianGrandSettlementPlayerInfo();
            info.playerId = playerId;
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
            if (gamePlayer != null) {
                SeatInfo liveSeatInfo = gameDataVo.getSeatInfo().values().stream()
                        .filter(s -> s.getPlayerId() == playerId).findFirst().orElse(null);
                info.pokerPlayerInfo = PokerBuilder.buildPlayerInfo(gamePlayer, liveSeatInfo, controller);
            }
            List<Long> rounds = gameDataVo.getRoundChangeList().getOrDefault(playerId, List.of());
            info.roundChangeList = new ArrayList<>(rounds);
            long total = 0;
            for (Long change : rounds) {
                total += change;
            }
            info.totalChange = total;
            info.conceded = gameDataVo.getConcededPlayerIds().contains(playerId);
            result.add(info);
        }
        return result;
    }
}
