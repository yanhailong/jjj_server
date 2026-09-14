package com.jjg.game.poker.game.douxian.cardlib;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.poker.game.douxian.util.DouXianHandEvaluator;
import com.jjg.game.poker.game.douxian.util.DouXianHandResult;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** 无房间依赖的斗仙牌四回合模拟器。 */
public final class DouXianCardLibGenerator {

    static final int SEAT_COUNT = 4;
    static final int INITIAL_HAND_SIZE = 8;
    static final int SIMULATOR_VERSION = 3;

    private DouXianCardLibGenerator() {
    }

    /**
     * 随机生成一组首轮32张牌，对同一组牌重复模拟，并分别为1/2/3真人场景生成一条结果。
     */
    public static List<DouXianCardLib> simulateOneInitialDeal(int poolId, ImmortalCardCfg cfg,
                                                              Room_ChessCfg roomCfg, int rolloutCount) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(poolId);
        if (cardMap == null || cardMap.size() != 52 || roomCfg == null) {
            return List.of();
        }
        DouXianGameDataVo simulationData = new DouXianGameDataVo(roomCfg);
        List<Integer> allCards = new ArrayList<>(cardMap.keySet());
        Collections.shuffle(allCards);
        List<List<Integer>> initialHands = new ArrayList<>(SEAT_COUNT);
        for (int seat = 0; seat < SEAT_COUNT; seat++) {
            initialHands.add(new ArrayList<>(allCards.subList(
                    seat * INITIAL_HAND_SIZE, (seat + 1) * INITIAL_HAND_SIZE)));
        }

        List<List<Integer>> realSeatSets = new ArrayList<>(3);
        for (int realCount = 1; realCount <= 3; realCount++) {
            List<Integer> seats = new ArrayList<>(List.of(0, 1, 2, 3));
            Collections.shuffle(seats);
            realSeatSets.add(new ArrayList<>(seats.subList(0, realCount)));
        }

        int repeats = Math.max(1, rolloutCount);
        long[][] samples = new long[3][repeats];
        long[][] seatSamples = new long[SEAT_COUNT][repeats];
        int[] wins = new int[3];
        for (int rollout = 0; rollout < repeats; rollout++) {
            long[] seatProfit = simulateFourRounds(initialHands, allCards, cardMap, cfg, simulationData);
            for (int seat = 0; seat < SEAT_COUNT; seat++) {
                seatSamples[seat][rollout] = seatProfit[seat];
            }
            for (int scene = 0; scene < realSeatSets.size(); scene++) {
                long total = 0;
                for (int seat : realSeatSets.get(scene)) {
                    total += seatProfit[seat];
                }
                samples[scene][rollout] = total;
                if (total > 0) {
                    wins[scene]++;
                }
            }
        }

        long betBase = Math.max(1, cfg.getBetList());
        List<Long> seatExpectedMultipliers = new ArrayList<>(SEAT_COUNT);
        for (int seat = 0; seat < SEAT_COUNT; seat++) {
            seatExpectedMultipliers.add(Math.round(mean(seatSamples[seat]) / betBase));
        }
        List<DouXianCardLib> result = new ArrayList<>(3);
        for (int scene = 0; scene < 3; scene++) {
            double mean = mean(samples[scene]);
            long multiplier = Math.round(mean / betBase);
            long stdDev = Math.round(standardDeviation(samples[scene], mean) / betBase);
            result.add(new DouXianCardLib(multiplier, scene + 1,
                    new ArrayList<>(realSeatSets.get(scene)), deepCopy(initialHands),
                    new ArrayList<>(seatExpectedMultipliers), stdDev, repeats,
                    (int) Math.round(wins[scene] * 10000.0 / repeats), SIMULATOR_VERSION));
        }
        return result;
    }

    private static long[] simulateFourRounds(List<List<Integer>> initialHands, List<Integer> fullDeck,
                                              Map<Integer, PokerCard> cardMap, ImmortalCardCfg cfg,
                                              DouXianGameDataVo simulationData) {
        List<List<Integer>> hands = deepCopy(initialHands);
        List<Integer> deck = new ArrayList<>(fullDeck);
        for (List<Integer> hand : hands) {
            deck.removeAll(hand);
        }
        Collections.shuffle(deck);

        List<Map<DouXianZone, List<Integer>>> zones = new ArrayList<>(SEAT_COUNT);
        for (int seat = 0; seat < SEAT_COUNT; seat++) {
            Map<DouXianZone, List<Integer>> seatZones = new EnumMap<>(DouXianZone.class);
            for (DouXianZone zone : DouXianZone.values()) {
                seatZones.put(zone, new ArrayList<>());
            }
            zones.add(seatZones);
        }
        boolean[] recallAllZones = new boolean[SEAT_COUNT];
        long[] profit = new long[SEAT_COUNT];

        for (int round = 1; round <= DouXianConstant.Common.TOTAL_ROUND; round++) {
            for (int seat = 0; seat < SEAT_COUNT; seat++) {
                drawToEight(hands.get(seat), deck);
                if (recallAllZones[seat]) {
                    for (DouXianZone zone : DouXianZone.values()) {
                        hands.get(seat).addAll(zones.get(seat).get(zone));
                        zones.get(seat).get(zone).clear();
                    }
                    recallAllZones[seat] = false;
                }
                autoPlace(hands.get(seat), zones.get(seat), cardMap, simulationData, round);
            }

            int[] grandWins = new int[SEAT_COUNT];
            int[] grandLosses = new int[SEAT_COUNT];
            settleRound(zones, cardMap, cfg, simulationData, round, profit, grandWins, grandLosses);
            if (round == DouXianConstant.Common.TOTAL_ROUND) {
                break;
            }
            for (int seat = 0; seat < SEAT_COUNT; seat++) {
                if (grandWins[seat] >= DouXianConstant.Common.SPECIAL_RULE_TRIGGER_PLAYER_COUNT
                        || grandLosses[seat] >= DouXianConstant.Common.SPECIAL_RULE_TRIGGER_PLAYER_COUNT) {
                    recallAllZones[seat] = true;
                }
            }
            advanceTiers(zones, deck);
            for (List<Integer> hand : hands) {
                discardLikeCurrentRobot(hand, deck);
            }
        }
        return profit;
    }

    private static void drawToEight(List<Integer> hand, List<Integer> deck) {
        int need = INITIAL_HAND_SIZE - hand.size();
        for (int i = 0; i < need && !deck.isEmpty(); i++) {
            hand.add(deck.removeFirst());
        }
    }

    private static void autoPlace(List<Integer> hand, Map<DouXianZone, List<Integer>> zones,
                                  Map<Integer, PokerCard> cardMap, DouXianGameDataVo simulationData,
                                  int round) {
        for (DouXianZone zone : List.of(DouXianZone.IMMORTAL, DouXianZone.SPIRIT, DouXianZone.MORTAL)) {
            if (!zone.isOpenAt(round)) {
                continue;
            }
            List<Integer> carried = zones.get(zone);
            int need = zone.getCapacity() - carried.size();
            if (need <= 0) {
                continue;
            }
            List<Card> carriedCards = carried.stream().map(cardMap::get).map(c -> (Card) c).toList();
            List<Card> candidates = hand.stream().map(cardMap::get).map(c -> (Card) c).toList();
            DouXianHandResult best = DouXianHandEvaluator.findBestZone(
                    simulationData, zone, carriedCards, candidates, round);
            if (best == null) {
                throw new IllegalStateException("斗仙牌模拟无法补满区域 " + zone);
            }
            List<Card> selected = best.getCards().subList(carried.size(), best.getCards().size());
            for (Card card : selected) {
                int id = ((PokerCard) card).getPokerPoolId();
                if (!hand.remove(Integer.valueOf(id))) {
                    throw new IllegalStateException("斗仙牌模拟选中了不在手牌中的牌 " + id);
                }
                carried.add(id);
            }
        }
    }

    private static void settleRound(List<Map<DouXianZone, List<Integer>>> zones,
                                    Map<Integer, PokerCard> cardMap, ImmortalCardCfg cfg,
                                    DouXianGameDataVo simulationData, int round,
                                    long[] profit, int[] grandWins, int[] grandLosses) {
        List<DouXianZone> openZones = new ArrayList<>();
        for (DouXianZone zone : DouXianZone.values()) {
            if (zone.isOpenAt(round)) {
                openZones.add(zone);
            }
        }
        List<Map<DouXianZone, DouXianHandResult>> results = new ArrayList<>(SEAT_COUNT);
        for (int seat = 0; seat < SEAT_COUNT; seat++) {
            Map<DouXianZone, DouXianHandResult> seatResult = new EnumMap<>(DouXianZone.class);
            for (DouXianZone zone : openZones) {
                List<Card> cards = zones.get(seat).get(zone).stream()
                        .map(cardMap::get).map(c -> (Card) c).toList();
                seatResult.put(zone, DouXianHandEvaluator.evaluateZone(
                        simulationData, zone, cards, round));
            }
            results.add(seatResult);
        }

        long betBase = Math.max(1, cfg.getBetList());
        long maxCap = cfg.getMaxCap() > 0 ? cfg.getMaxCap() : Long.MAX_VALUE;
        for (int a = 0; a < SEAT_COUNT; a++) {
            for (int b = a + 1; b < SEAT_COUNT; b++) {
                for (DouXianZone zone : openZones) {
                    applyZoneDebt(a, results.get(a).get(zone), b, results.get(b).get(zone),
                            betBase, maxCap, profit);
                }
                if (openZones.size() == 3) {
                    if (DouXianHandEvaluator.isGrandWin(results.get(a), results.get(b))) {
                        grandWins[a]++;
                        grandLosses[b]++;
                        applyGrandWinDebts(a, b, results, betBase, maxCap, profit);
                    } else if (DouXianHandEvaluator.isGrandWin(results.get(b), results.get(a))) {
                        grandWins[b]++;
                        grandLosses[a]++;
                        applyGrandWinDebts(b, a, results, betBase, maxCap, profit);
                    }
                }
            }
        }
    }

    private static void applyZoneDebt(int a, DouXianHandResult resultA, int b, DouXianHandResult resultB,
                                      long betBase, long maxCap, long[] profit) {
        int cmp = DouXianHandEvaluator.compareAether(resultA, resultB);
        if (cmp == 0) {
            return;
        }
        int winner = cmp > 0 ? a : b;
        int loser = cmp > 0 ? b : a;
        long difference = Math.abs(resultA.getAetherValue() - resultB.getAetherValue());
        applyDebt(winner, loser, difference, betBase, maxCap, profit);
    }

    private static void applyGrandWinDebts(int winner, int loser,
                                           List<Map<DouXianZone, DouXianHandResult>> results,
                                           long betBase, long maxCap, long[] profit) {
        for (DouXianZone zone : DouXianZone.values()) {
            long difference = results.get(winner).get(zone).getAetherValue()
                    - results.get(loser).get(zone).getAetherValue();
            applyDebt(winner, loser, difference, betBase, maxCap, profit);
        }
    }

    private static void applyDebt(int winner, int loser, long aetherDifference,
                                  long betBase, long maxCap, long[] profit) {
        long amount;
        try {
            amount = Math.multiplyExact(Math.max(0, aetherDifference), betBase);
        } catch (ArithmeticException ignored) {
            amount = Long.MAX_VALUE;
        }
        amount = Math.min(amount, maxCap);
        profit[winner] += amount;
        profit[loser] -= amount;
    }

    private static void advanceTiers(List<Map<DouXianZone, List<Integer>>> zones, List<Integer> deck) {
        for (Map<DouXianZone, List<Integer>> seatZones : zones) {
            deck.addAll(seatZones.get(DouXianZone.IMMORTAL));
            List<Integer> spirit = new ArrayList<>(seatZones.get(DouXianZone.SPIRIT));
            List<Integer> mortal = new ArrayList<>(seatZones.get(DouXianZone.MORTAL));
            seatZones.get(DouXianZone.IMMORTAL).clear();
            seatZones.get(DouXianZone.IMMORTAL).addAll(spirit);
            seatZones.get(DouXianZone.SPIRIT).clear();
            seatZones.get(DouXianZone.SPIRIT).addAll(mortal);
            seatZones.get(DouXianZone.MORTAL).clear();
        }
        Collections.shuffle(deck);
    }

    private static void discardLikeCurrentRobot(List<Integer> hand, List<Integer> deck) {
        if (hand.isEmpty()) {
            return;
        }
        int probability = ThreadLocalRandom.current().nextInt(100);
        int discardCount;
        if (probability < 20) {
            return;
        } else if (probability < 40) {
            discardCount = hand.size();
        } else {
            discardCount = Math.min(ThreadLocalRandom.current().nextInt(1, 3), hand.size());
        }
        List<Integer> shuffled = new ArrayList<>(hand);
        Collections.shuffle(shuffled);
        List<Integer> discarded = new ArrayList<>(shuffled.subList(0, discardCount));
        hand.removeAll(discarded);
        deck.addAll(discarded);
        Collections.shuffle(deck);
    }

    static boolean isValidInitialDeal(DouXianCardLib lib, Map<Integer, PokerCard> cardMap, int realCount) {
        if (lib == null || cardMap == null || lib.getRealPlayerCount() != realCount
                || lib.getSimulatorVersion() != SIMULATOR_VERSION
                || lib.getRealSeatIndexes() == null || lib.getRealSeatIndexes().size() != realCount
                || lib.getSeatCards() == null || lib.getSeatCards().size() != SEAT_COUNT
                || lib.getSeatExpectedMultipliers() == null
                || lib.getSeatExpectedMultipliers().size() != SEAT_COUNT
                || lib.getSeatExpectedMultipliers().contains(null)) {
            return false;
        }
        Map<Integer, Boolean> seen = new HashMap<>();
        for (List<Integer> hand : lib.getSeatCards()) {
            if (hand == null || hand.size() != INITIAL_HAND_SIZE) {
                return false;
            }
            for (Integer cardId : hand) {
                if (!cardMap.containsKey(cardId) || seen.put(cardId, Boolean.TRUE) != null) {
                    return false;
                }
            }
        }
        return lib.getRealSeatIndexes().stream().distinct().count() == realCount
                && lib.getRealSeatIndexes().stream().allMatch(i -> i >= 0 && i < SEAT_COUNT);
    }

    private static List<List<Integer>> deepCopy(List<List<Integer>> source) {
        List<List<Integer>> result = new ArrayList<>(source.size());
        for (List<Integer> values : source) {
            result.add(new ArrayList<>(values));
        }
        return result;
    }

    private static double mean(long[] values) {
        double total = 0;
        for (long value : values) {
            total += value;
        }
        return total / values.length;
    }

    private static double standardDeviation(long[] values, double mean) {
        double sumSquares = 0;
        for (long value : values) {
            double difference = value - mean;
            sumSquares += difference * difference;
        }
        return Math.sqrt(sumSquares / values.length);
    }
}
