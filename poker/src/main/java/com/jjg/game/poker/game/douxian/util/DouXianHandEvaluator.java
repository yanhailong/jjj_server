package com.jjg.game.poker.game.douxian.util;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.data.DouXianDataHelper;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.sampledata.bean.ImmortalHandCfg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 三个区域的牌型判定 + 灵力值计算。
 * <p>
 * 点数约定沿用 texas.util.PokerHandEvaluator：{@link Card#getRank()} 中 A 为 14（高位），
 * 仅在识别 A 开头的低位顺子（A2 / A23 / A2345）时，将 A 按 1 点重新计算主体点数，
 * 对应 DESIGN.md 4.1 的特殊规则。
 */
public final class DouXianHandEvaluator {

    private DouXianHandEvaluator() {
    }

    // ------------------------------------------------------------------
    // 区域判定：给定恰好装满该区域的牌，判定牌型
    // ------------------------------------------------------------------

    public static DouXianHandResult evaluateZone(DouXianZone zone, List<Card> cards, int round) {
        return evaluateZone(null, zone, cards, round);
    }

    public static DouXianHandResult evaluateZone(DouXianGameDataVo gameDataVo, DouXianZone zone, List<Card> cards, int round) {
        return switch (zone) {
            case MORTAL -> evaluateZone2(gameDataVo, cards, round);
            case SPIRIT -> evaluateZone3(gameDataVo, cards, round);
            case IMMORTAL -> evaluateZone5(gameDataVo, cards, round);
        };
    }

    public static DouXianHandResult evaluateZone2(List<Card> cards, int round) {
        return evaluateZone2(null, cards, round);
    }

    public static DouXianHandResult evaluateZone2(DouXianGameDataVo gameDataVo, List<Card> cards, int round) {
        Card a = cards.get(0);
        Card b = cards.get(1);
        boolean sameSuit = a.getSuit() == b.getSuit();
        boolean pair = a.getRank() == b.getRank();
        Integer straightDominant = pair ? null : straightDominantRank(distinctRanksDesc(cards), 2);

        DouXianHandType2 type;
        int dominant;
        if (pair) {
            type = DouXianHandType2.LIANG_YI;
            dominant = a.getRank();
        } else if (straightDominant != null) {
            type = sameSuit ? DouXianHandType2.QING_LONG : DouXianHandType2.FEI_JIAN;
            dominant = straightDominant;
        } else if (sameSuit) {
            type = DouXianHandType2.TONG_HUA;
            dominant = Math.max(a.getRank(), b.getRank());
        } else {
            type = DouXianHandType2.SAN_SHOU;
            dominant = Math.max(a.getRank(), b.getRank());
        }
        return buildResult(gameDataVo, DouXianZone.MORTAL, type, dominant, cards, round);
    }

    public static DouXianHandResult evaluateZone3(List<Card> cards, int round) {
        return evaluateZone3(null, cards, round);
    }

    public static DouXianHandResult evaluateZone3(DouXianGameDataVo gameDataVo, List<Card> cards, int round) {
        List<Integer> ranks = cards.stream().map(Card::getRank).collect(Collectors.toList());
        Map<Integer, Long> rankCounts = ranks.stream().collect(Collectors.groupingBy(r -> r, Collectors.counting()));
        boolean sameSuit = cards.stream().map(Card::getSuit).distinct().count() == 1;
        Integer straightDominant = rankCounts.size() == 3 ? straightDominantRank(distinctRanksDesc(cards), 3) : null;

        DouXianHandType3 type;
        int dominant;
        if (rankCounts.containsValue(3L)) {
            type = DouXianHandType3.SAN_QING_JUE;
            dominant = ranks.getFirst();
        } else if (straightDominant != null && sameSuit) {
            type = DouXianHandType3.ZHI_ZUN_LONG;
            dominant = straightDominant;
        } else if (straightDominant != null) {
            type = DouXianHandType3.LIAN_HUAN_JIAN;
            dominant = straightDominant;
        } else if (sameSuit) {
            type = DouXianHandType3.WANG_YOU_HUA;
            dominant = ranks.stream().max(Integer::compareTo).orElseThrow();
        } else if (rankCounts.containsValue(2L)) {
            type = DouXianHandType3.LIANG_YI;
            dominant = rankCounts.entrySet().stream()
                    .filter(e -> e.getValue() == 2L)
                    .map(Map.Entry::getKey)
                    .findFirst().orElseThrow();
        } else {
            type = DouXianHandType3.SAN_SHOU;
            dominant = ranks.stream().max(Integer::compareTo).orElseThrow();
        }
        return buildResult(gameDataVo, DouXianZone.SPIRIT, type, dominant, cards, round);
    }

    public static DouXianHandResult evaluateZone5(List<Card> cards, int round) {
        return evaluateZone5(null, cards, round);
    }

    public static DouXianHandResult evaluateZone5(DouXianGameDataVo gameDataVo, List<Card> cards, int round) {
        List<Integer> ranksDesc = cards.stream().map(Card::getRank)
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        boolean sameSuit = cards.stream().map(Card::getSuit).distinct().count() == 1;
        Integer straightDominant = ranksDesc.stream().distinct().count() == 5
                ? straightDominantRank(ranksDesc.stream().distinct().collect(Collectors.toList()), 5)
                : null;

        Map<Integer, Long> rankCounts = ranksDesc.stream().collect(Collectors.groupingBy(r -> r, Collectors.counting()));
        List<Integer> byCountThenRank = rankCounts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Comparator.comparingInt((Map.Entry<Integer, Long> e) -> e.getKey()).reversed()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DouXianHandType5 type;
        int dominant;
        if (straightDominant != null && sameSuit) {
            type = DouXianHandType5.WU_ZHUA_JIN_LONG;
            dominant = straightDominant;
        } else if (rankCounts.containsValue(4L)) {
            type = DouXianHandType5.SI_XIANG_SHEN_GONG;
            dominant = byCountThenRank.getFirst();
        } else if (rankCounts.containsValue(3L) && rankCounts.containsValue(2L)) {
            type = DouXianHandType5.SAN_QING_LIANG_YI;
            dominant = byCountThenRank.getFirst();
        } else if (straightDominant != null) {
            type = DouXianHandType5.JIAN_GUAN_CHANG_KONG;
            dominant = straightDominant;
        } else if (sameSuit) {
            type = DouXianHandType5.TIAN_HUA_LUAN_ZHUI;
            dominant = ranksDesc.getFirst();
        } else if (rankCounts.containsValue(3L)) {
            type = DouXianHandType5.SAN_QING_JUE;
            dominant = byCountThenRank.getFirst();
        } else if (rankCounts.values().stream().filter(v -> v == 2L).count() == 2) {
            type = DouXianHandType5.QIAN_KUN_DUI;
            dominant = byCountThenRank.getFirst();
        } else if (rankCounts.containsValue(2L)) {
            type = DouXianHandType5.LIANG_YI;
            dominant = byCountThenRank.getFirst();
        } else {
            type = DouXianHandType5.SAN_SHOU;
            dominant = ranksDesc.getFirst();
        }
        return buildResult(gameDataVo, DouXianZone.IMMORTAL, type, dominant, cards, round);
    }

    // ------------------------------------------------------------------
    // 灵力值计算：（主体牌型点数大小 × 牌型倍率 + 牌型值）× 回合倍率
    // ------------------------------------------------------------------

    public static long calcAether(DouXianGameDataVo gameDataVo, DouXianZone zone, IDouXianHandType handType, int dominantRank, int round) {
        ImmortalHandCfg handCfg = gameDataVo == null ? null : DouXianDataHelper.getImmortalHandCfg(handType, zone);
        int roundMultiplier = gameDataVo == null ? DouXianConstant.getRoundMultiplier(round) : DouXianDataHelper.getRoundMultiplier(gameDataVo, round);
        int handMultiplier = handCfg == null ? handType.getMultiplier() : handCfg.getHandMultiplier();
        int handValue = handCfg == null ? handType.getValue() : handCfg.getHandValue();
        return (long) (dominantRank * handMultiplier + handValue) * roundMultiplier;
    }

    private static DouXianHandResult buildResult(DouXianGameDataVo gameDataVo, DouXianZone zone, IDouXianHandType type, int dominant,
                                                   List<Card> cards, int round) {
        return new DouXianHandResult(zone, type, dominant, cards, calcAether(gameDataVo, zone, type, dominant, round));
    }

    // ------------------------------------------------------------------
    // 托管/提示用：从候选牌（手牌+区域固定容量）中枚举组合，找灵力值最高的一组
    // 对应 DESIGN.md 5. 托管功能 的组排规则
    // ------------------------------------------------------------------

    public static DouXianHandResult findBestZone(DouXianZone zone, List<Card> candidates, int round) {
        return findBestZone(null, zone, candidates, round);
    }

    public static DouXianHandResult findBestZone(DouXianGameDataVo gameDataVo, DouXianZone zone, List<Card> candidates, int round) {
        return findBestZone(gameDataVo, zone, List.of(), candidates, round);
    }

    /**
     * 区域里已经有飞升锁定的牌(carried)时，只能从候选牌(通常是手牌)里挑选补满剩余名额，
     * 锁定牌本身不参与挑选、必须原样保留。
     */
    public static DouXianHandResult findBestZone(DouXianZone zone, List<Card> carried, List<Card> candidates, int round) {
        return findBestZone(null, zone, carried, candidates, round);
    }

    public static DouXianHandResult findBestZone(DouXianGameDataVo gameDataVo, DouXianZone zone, List<Card> carried, List<Card> candidates, int round) {
        int need = zone.getCapacity() - carried.size();
        if (need <= 0) {
            return evaluateZone(gameDataVo, zone, carried, round);
        }
        if (need == candidates.size()) {
            List<Card> full = new ArrayList<>(carried);
            full.addAll(candidates);
            return evaluateZone(gameDataVo, zone, full, round);
        }
        DouXianHandResult best = null;
        for (List<Card> combo : combinations(candidates, need)) {
            List<Card> full = new ArrayList<>(carried);
            full.addAll(combo);
            DouXianHandResult res = evaluateZone(gameDataVo, zone, full, round);
            if (best == null || res.getAetherValue() > best.getAetherValue()) {
                best = res;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // 比大小：结算/全胜判定都只依赖灵力值这一个数字，不需要额外的花色/踢脚比较。
    // 约定：灵力值相等按平局处理(不结算)——策划确认过牌型数值表不会让不同玩家在
    // 同一区域凑出完全相同的灵力值，真出现相等属于极端情况，不额外报错，交给
    // 结算引擎按"赢取积分=(赢家-输家)×底分"公式自然算出0来处理，不需要在这里特殊分支。
    // ------------------------------------------------------------------

    public static int compareAether(DouXianHandResult a, DouXianHandResult b) {
        return Long.compare(a.getAetherValue(), b.getAetherValue());
    }

    /**
     * 全胜判定，DESIGN.md 四.3：attacker 在凡/灵/仙三区域灵力值都不低于 defender，
     * 且至少一个区域严格大于，则视为 attacker 对 defender 全胜。
     *
     * @param attacker 攻方三个区域的成牌结果，key 必须覆盖 DouXianZone 全部三个枚举值
     * @param defender 守方三个区域的成牌结果，同上
     */
    public static boolean isGrandWin(Map<DouXianZone, DouXianHandResult> attacker,
                                      Map<DouXianZone, DouXianHandResult> defender) {
        boolean strictlyGreaterAtLeastOnce = false;
        for (DouXianZone zone : DouXianZone.values()) {
            int cmp = compareAether(attacker.get(zone), defender.get(zone));
            if (cmp < 0) {
                return false;
            }
            if (cmp > 0) {
                strictlyGreaterAtLeastOnce = true;
            }
        }
        return strictlyGreaterAtLeastOnce;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private static List<Integer> distinctRanksDesc(List<Card> cards) {
        return cards.stream().map(Card::getRank).distinct()
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    /**
     * 判断给定的互不相同、降序排列的点数是否构成长度为 length 的顺子。
     * 若命中 A 开头的低位顺子（如 A2、A23、A2345），A 按 1 点重新参与排序。
     *
     * @return 构成顺子时返回主体点数（顺子中最大那张牌的有效点数），否则返回 null
     */
    private static Integer straightDominantRank(List<Integer> ranksDesc, int length) {
        if (ranksDesc.size() != length) {
            return null;
        }
        if (isConsecutive(ranksDesc)) {
            return ranksDesc.getFirst();
        }
        if (ranksDesc.getFirst() == DouXianConstant.Common.ACE_HIGH_RANK) {
            List<Integer> aceLow = new ArrayList<>(ranksDesc);
            aceLow.replaceAll(r -> r == DouXianConstant.Common.ACE_HIGH_RANK ? DouXianConstant.Common.ACE_LOW_RANK : r);
            aceLow.sort(Comparator.reverseOrder());
            if (isConsecutive(aceLow)) {
                return aceLow.getFirst();
            }
        }
        return null;
    }

    private static boolean isConsecutive(List<Integer> ranksDesc) {
        for (int i = 0; i < ranksDesc.size() - 1; i++) {
            if (ranksDesc.get(i) - ranksDesc.get(i + 1) != 1) {
                return false;
            }
        }
        return true;
    }

    private static List<List<Card>> combinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        combineHelper(cards, 0, k, new ArrayList<>(), result);
        return result;
    }

    private static void combineHelper(List<Card> cards, int start, int k, List<Card> temp, List<List<Card>> result) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i <= cards.size() - (k - temp.size()); i++) {
            temp.add(cards.get(i));
            combineHelper(cards, i + 1, k, temp, result);
            temp.removeLast();
        }
    }
}
