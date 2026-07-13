package com.jjg.game.poker.game.douxian.util;

import com.jjg.game.poker.game.douxian.constant.DouXianZone;
import com.jjg.game.poker.game.douxian.message.bean.DouXianZoneSettlementInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结算金额计算，对应 DESIGN.md 6.1/6.2（斗仙牌.docx V1.02 更新版）。
 * <p>
 * 分两步：先算出整回合所有"理论应赔付"金额(不做任何封顶)，再对这一整批结果统一应用
 * 三层封顶(见 {@link #applyRoundCaps})。之所以要先把整回合的债务都摊出来才能封顶，
 * 是因为封顶规则本身就是"回合级别/输家维度"的聚合约束，不是能按单笔结算顺序处理的：
 * - 小额玩家保护是按"赢家本回合总共赢了多少"聚合的；
 * - 输家不够赔多家时是按"输家本回合总共要赔多少"聚合、按理论金额比例分摊的。
 * 真正的加减金币操作在 DouXianSettlementPhase 里做，这个类只算数字。
 */
public final class DouXianSettlementCalculator {

    private DouXianSettlementCalculator() {
    }

    /**
     * 单区域理论结算结果（未封顶）。灵力值相等按平局处理(winnerId/loserId保持0，changeValue=0)。
     */
    public static DouXianZoneSettlementInfo computeZoneResult(long playerAId, DouXianHandResult resultA,
                                                                long playerBId, DouXianHandResult resultB,
                                                                long betBase) {
        DouXianZoneSettlementInfo info = new DouXianZoneSettlementInfo();
        info.zoneId = resultA.getZone().getId();
        int cmp = DouXianHandEvaluator.compareAether(resultA, resultB);
        info.winnerAether = cmp >= 0 ? resultA.getAetherValue() : resultB.getAetherValue();
        info.loserAether = cmp >= 0 ? resultB.getAetherValue() : resultA.getAetherValue();
        if (cmp == 0) {
            return info;
        }
        boolean aWins = cmp > 0;
        info.winnerId = aWins ? playerAId : playerBId;
        info.loserId = aWins ? playerBId : playerAId;
        info.changeValue = (info.winnerAether - info.loserAether) * betBase;
        return info;
    }

    /**
     * 全胜二次结算的理论结果（未封顶），赢家/输家方向已经由 {@link DouXianHandEvaluator#isGrandWin} 确定，
     * 对已开放的每个区域按同一方向再算一次(DESIGN.md 6.3)。
     */
    public static List<DouXianZoneSettlementInfo> computeGrandWinExtra(long winnerId, long loserId,
                                                                         Map<DouXianZone, DouXianHandResult> winnerResults,
                                                                         Map<DouXianZone, DouXianHandResult> loserResults,
                                                                         List<DouXianZone> openZones,
                                                                         long betBase) {
        List<DouXianZoneSettlementInfo> list = new ArrayList<>();
        for (DouXianZone zone : openZones) {
            DouXianHandResult winnerResult = winnerResults.get(zone);
            DouXianHandResult loserResult = loserResults.get(zone);
            DouXianZoneSettlementInfo info = new DouXianZoneSettlementInfo();
            info.zoneId = zone.getId();
            info.winnerId = winnerId;
            info.loserId = loserId;
            info.winnerAether = winnerResult.getAetherValue();
            info.loserAether = loserResult.getAetherValue();
            info.changeValue = (winnerResult.getAetherValue() - loserResult.getAetherValue()) * betBase;
            list.add(info);
        }
        return list;
    }

    /**
     * 对整回合的理论结算结果就地应用 DESIGN.md 6.2 的三层封顶，直接修改每条记录的 changeValue：
     * <ol>
     *   <li>A. 单笔结算不超过场次封顶值(maxCap)；</li>
     *   <li>B. 小额玩家保护：赢家"开局前携带金币"和"本回合开始前携带金币"都低于场次最小输赢(minWinLimit)时，
     *       该赢家本回合能赢到的总额封顶在 minWinLimit（按其本回合所有理论所得比例分摊）；</li>
     *   <li>C. 单个输家本回合总共要赔付的金额不能超过自己当前携带金币，超出时按各笔(A、B处理后)理论金额比例分摊，
     *       分摊后正好花光，不会出现负数。</li>
     * </ol>
     * 平局(winnerId==0)的记录不受影响。
     *
     * @param allDebts          本回合全部结算记录(含常规结算和全胜二次结算，同一批一起处理)
     * @param maxCap            场次封顶值
     * @param minWinLimit       场次最小输赢
     * @param gameStartBalance  玩家id -> 开局前携带金币
     * @param roundStartBalance 玩家id -> 本回合开始前携带金币
     * @param currentBalance    玩家id -> 结算前一刻的实时携带金币(本回合任何转账都还没发生时的快照)
     */
    public static void applyRoundCaps(List<DouXianZoneSettlementInfo> allDebts,
                                       long maxCap, long minWinLimit,
                                       Map<Long, Long> gameStartBalance,
                                       Map<Long, Long> roundStartBalance,
                                       Map<Long, Long> currentBalance) {
        List<DouXianZoneSettlementInfo> effectiveDebts = allDebts.stream()
                .filter(d -> d.winnerId != 0)
                .collect(Collectors.toList());

        // A. 单笔结算封顶
        for (DouXianZoneSettlementInfo debt : effectiveDebts) {
            debt.changeValue = Math.min(debt.changeValue, maxCap);
        }

        // B. 小额玩家保护：按赢家聚合
        Map<Long, List<DouXianZoneSettlementInfo>> byWinner = effectiveDebts.stream()
                .collect(Collectors.groupingBy(d -> d.winnerId));
        for (Map.Entry<Long, List<DouXianZoneSettlementInfo>> entry : byWinner.entrySet()) {
            long winnerId = entry.getKey();
            boolean smallStakes = gameStartBalance.getOrDefault(winnerId, 0L) < minWinLimit
                    && roundStartBalance.getOrDefault(winnerId, 0L) < minWinLimit;
            if (smallStakes) {
                scaleDownToTarget(entry.getValue(), minWinLimit);
            }
        }

        // C. 输家赔付总额不能超过自己当前余额，按理论金额比例分摊
        Map<Long, List<DouXianZoneSettlementInfo>> byLoser = effectiveDebts.stream()
                .collect(Collectors.groupingBy(d -> d.loserId));
        for (Map.Entry<Long, List<DouXianZoneSettlementInfo>> entry : byLoser.entrySet()) {
            long loserId = entry.getKey();
            long balance = Math.max(0, currentBalance.getOrDefault(loserId, 0L));
            scaleDownToTarget(entry.getValue(), balance);
        }
    }

    /**
     * 把一组结算记录的 changeValue 按各自占比缩放，使总和不超过 target。
     * 用 floorDiv 逐笔算份额、最后一笔吃掉尾差，保证缩放后总和恰好等于 target(不多不少)，
     * 不会因为多笔取整损耗导致总和明显小于 target。
     */
    private static void scaleDownToTarget(List<DouXianZoneSettlementInfo> debts, long target) {
        long sum = 0;
        for (DouXianZoneSettlementInfo debt : debts) {
            sum += debt.changeValue;
        }
        if (sum <= target) {
            return;
        }
        if (target <= 0) {
            for (DouXianZoneSettlementInfo debt : debts) {
                debt.changeValue = 0;
            }
            return;
        }
        long remaining = target;
        for (int i = 0; i < debts.size(); i++) {
            DouXianZoneSettlementInfo debt = debts.get(i);
            long scaled;
            if (i == debts.size() - 1) {
                scaled = remaining;
            } else {
                scaled = Math.floorDiv(debt.changeValue * target, sum);
                remaining -= scaled;
            }
            debt.changeValue = Math.max(0, scaled);
        }
    }
}
