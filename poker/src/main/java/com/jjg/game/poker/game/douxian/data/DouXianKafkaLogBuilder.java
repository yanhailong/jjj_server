package com.jjg.game.poker.game.douxian.data;

import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.message.bean.DouXianPairSettlementInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianSpecialRuleInfo;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZoneSettlementInfo;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.poker.game.douxian.room.data.DouXianKafkaRoundLog;
import com.jjg.game.poker.game.douxian.util.DouXianHandResult;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds detailed DouXian monitoring data for the shared game_bet_settlement topic. */
public final class DouXianKafkaLogBuilder {

    private DouXianKafkaLogBuilder() {
    }

    public static DouXianKafkaRoundLog buildRound(
            DouXianGameController controller,
            Map<Long, Map<DouXianZone, DouXianHandResult>> playerZoneResults,
            List<DouXianPairSettlementInfo> pairResults,
            List<DouXianSpecialRuleInfo> specialRules,
            Map<Long, Long> balanceAfter) {
        DouXianGameDataVo data = controller.getGameDataVo();
        int round = data.getRound();
        long betBase = data.getRoomCfg().getBetBase();
        List<Long> playerIds = new ArrayList<>(playerZoneResults.keySet());

        Map<Long, Long> totalBet = calculateTotalBet(playerIds, pairResults);
        Map<Long, Map<String, Object>> playerStats = new LinkedHashMap<>();
        List<Map<String, Object>> players = new ArrayList<>();
        for (Long playerId : playerIds) {
            long before = data.getRoundStartBalance().getOrDefault(
                    playerId, balanceAfter.getOrDefault(playerId, 0L));
            long after = balanceAfter.getOrDefault(playerId, before);
            long income = after - before;
            long bet = totalBet.getOrDefault(playerId, 0L);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("TotalBet", bet);
            stats.put("TotalWin", bet + income);
            stats.put("Income", income);
            stats.put("EffectiveBet", bet);
            stats.put("BalanceBefore", before);
            stats.put("BalanceAfter", after);
            playerStats.put(playerId, stats);

            Map<String, Object> player = new LinkedHashMap<>();
            player.put("playerId", playerId);
            GamePlayer gamePlayer = data.getGamePlayer(playerId);
            player.put("robot", gamePlayer instanceof GameRobotPlayer);
            player.put("balanceBefore", before);
            player.put("balanceAfter", after);
            player.put("income", income);
            player.put("totalBet", bet);
            player.put("zones", buildZones(data, playerId, playerZoneResults.get(playerId)));
            players.add(player);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("logType", "ROUND_SETTLEMENT");
        detail.put("round", round);
        detail.put("roundMultiplier", DouXianDataHelper.getRoundMultiplier(data, round));
        detail.put("roomCfgId", data.getRoomCfg().getId());
        detail.put("betBase", betBase);
        detail.put("currencyId", controller.getGameTransactionItemId());
        detail.put("players", players);
        detail.put("pairResults", pairResults);
        detail.put("grandWinRelations", buildGrandWinRelations(pairResults));
        detail.put("specialEvents", specialRules);

        Map<String, Object> gameData = new LinkedHashMap<>();
        gameData.put("tax", 0L);
        gameData.put("gameName", "\u6597\u4ed9\u724c");
        gameData.put("currencyId", controller.getGameTransactionItemId());
        gameData.put("round", round);
        gameData.put("betBase", betBase);
        gameData.put("douXianInfo", detail);
        return new DouXianKafkaRoundLog(gameData, playerStats);
    }

    /** Emits a traceable record when a game ends before the current round settles. */
    public static DouXianKafkaRoundLog buildGrandOnly(DouXianGameController controller) {
        DouXianGameDataVo data = controller.getGameDataVo();
        Map<Long, Map<String, Object>> playerStats = new LinkedHashMap<>();
        List<Map<String, Object>> players = new ArrayList<>();
        for (Map.Entry<Long, GamePlayer> entry : data.getGamePlayerMap().entrySet()) {
            long playerId = entry.getKey();
            long before = data.getGameStartBalance().getOrDefault(
                    playerId, controller.getTransactionItemNum(playerId));
            long after = controller.getTransactionItemNum(playerId);
            long income = after - before;
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("TotalBet", 0L);
            stats.put("TotalWin", 0L);
            stats.put("Income", 0L);
            stats.put("EffectiveBet", 0L);
            stats.put("BalanceBefore", before);
            stats.put("BalanceAfter", after);
            playerStats.put(playerId, stats);

            Map<String, Object> player = new LinkedHashMap<>();
            player.put("playerId", playerId);
            player.put("robot", entry.getValue() instanceof GameRobotPlayer);
            player.put("balanceBefore", before);
            player.put("balanceAfter", after);
            player.put("income", income);
            player.put("conceded", data.getConcededPlayerIds().contains(playerId));
            players.add(player);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("logType", "GRAND_SETTLEMENT_ONLY");
        detail.put("round", data.getRound());
        detail.put("roomCfgId", data.getRoomCfg().getId());
        detail.put("betBase", data.getRoomCfg().getBetBase());
        detail.put("currencyId", controller.getGameTransactionItemId());
        detail.put("players", players);

        Map<String, Object> gameData = new LinkedHashMap<>();
        gameData.put("tax", 0L);
        gameData.put("gameName", "\u6597\u4ed9\u724c");
        gameData.put("currencyId", controller.getGameTransactionItemId());
        gameData.put("round", data.getRound());
        gameData.put("betBase", data.getRoomCfg().getBetBase());
        gameData.put("douXianInfo", detail);
        return new DouXianKafkaRoundLog(gameData, playerStats);
    }

    private static List<Map<String, Object>> buildZones(
            DouXianGameDataVo data, long playerId, Map<DouXianZone, DouXianHandResult> results) {
        List<Map<String, Object>> zones = new ArrayList<>();
        if (results == null) {
            return zones;
        }
        for (Map.Entry<DouXianZone, DouXianHandResult> entry : results.entrySet()) {
            DouXianZone zone = entry.getKey();
            DouXianHandResult result = entry.getValue();
            List<Integer> cfgCardIds = data.getPlayerZoneCards(playerId).get(zone).getAllCards();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("zoneId", zone.getId());
            item.put("zoneName", zone.name());
            item.put("handTypeCfgId", result.getHandType().getConfigId());
            item.put("handTypeName", result.getHandType().getDisplayName());
            item.put("handTypeNameId",
                    DouXianDataHelper.getHandTypeNameId(result.getHandType(), zone));
            item.put("dominantRank", result.getDominantRank());
            item.put("aether", result.getAetherValue());
            item.put("cardCfgIds", new ArrayList<>(cfgCardIds));
            item.put("clientCardIds", DouXianDataHelper.getClientCardIds(data, cfgCardIds));
            zones.add(item);
        }
        return zones;
    }

    private static Map<Long, Long> calculateTotalBet(
            List<Long> playerIds, List<DouXianPairSettlementInfo> pairResults) {
        Map<Long, Long> totalBet = new LinkedHashMap<>();
        playerIds.forEach(playerId -> totalBet.put(playerId, 0L));
        for (DouXianPairSettlementInfo pair : pairResults) {
            addDebts(totalBet, pair.zoneResults);
            addDebts(totalBet, pair.grandWinExtraResults);
        }
        return totalBet;
    }

    private static void addDebts(
            Map<Long, Long> totalBet, List<DouXianZoneSettlementInfo> debts) {
        if (debts == null) {
            return;
        }
        for (DouXianZoneSettlementInfo debt : debts) {
            if (debt.winnerId == 0 || debt.changeValue <= 0) {
                continue;
            }
            totalBet.merge(debt.winnerId, debt.changeValue, Long::sum);
            totalBet.merge(debt.loserId, debt.changeValue, Long::sum);
        }
    }

    private static List<Map<String, Object>> buildGrandWinRelations(
            List<DouXianPairSettlementInfo> pairs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DouXianPairSettlementInfo pair : pairs) {
            if (pair.grandWinPlayerId == 0) {
                continue;
            }
            Map<String, Object> relation = new LinkedHashMap<>();
            relation.put("winnerId", pair.grandWinPlayerId);
            relation.put("loserId",
                    pair.grandWinPlayerId == pair.playerAId ? pair.playerBId : pair.playerAId);
            relation.put("extraResults", pair.grandWinExtraResults);
            result.add(relation);
        }
        return result;
    }
}
