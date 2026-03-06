package com.jjg.game.table.russianlette.message;

import com.jjg.game.core.constant.Code;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.resp.*;

import java.util.List;
import java.util.Map;

/**
 * 俄罗斯转盘消息构建工具类
 *
 * @author lhc
 */
public class RussianLetteMessageBuilder {

    // =====================================================================
    // 阶段变化通知
    // =====================================================================

    /**
     * 构建俄罗斯转盘专属阶段变化通知
     *
     * @param gamePhase      当前游戏阶段
     * @param endTime        本阶段结束时间戳（ms）
     * @param prob           近 12 局概率信息（BET/DRAW_ON 阶段有值；REST/SETTLEMENT 为 null）
     * @param settlementInfo 开奖结果（DRAW_ON/SETTLEMENT 阶段有值；REST/BET 为 null）
     * @return 已填充字段的通知对象
     */
    public static NotifyRussianLettePhaseChangInfo buildPhaseChangInfo(
            EGamePhase gamePhase, long endTime,
            RussianLetteProb prob, RussianLetteSettlementInfo settlementInfo) {
        NotifyRussianLettePhaseChangInfo info = new NotifyRussianLettePhaseChangInfo();
        info.prob = prob;
        RussianLetteStageInfo stageInfo = new RussianLetteStageInfo();
        stageInfo.gamePhase = gamePhase;
        stageInfo.endTime = endTime;
        // 开奖/结算阶段：将开奖结果写入 stageInfo（settlementInfo.diceData 已做 37→0 映射）
        if (settlementInfo != null) {
            stageInfo.diceData = settlementInfo.diceData;
        }
        info.stageInfo = stageInfo;
        return info;
    }

    // =====================================================================
    // 概率统计
    // =====================================================================

    /**
     * 根据近 12 局历史记录计算红/黑/奇/偶概率
     * <p>数字 0（绿色）不计入红/黑/奇/偶分类，因此各概率之和可能小于 1。</p>
     *
     * @param historyBeans 全量历史记录（取最后 12 条计算）
     * @return 概率对象；历史为空时返回 null
     */
    public static RussianLetteProb buildProb(List<RussianLetteHistoryBean> historyBeans) {
        if (historyBeans == null || historyBeans.isEmpty()) {
            return null;
        }
        // 取最近 12 条
        int size = historyBeans.size();
        int start = Math.max(0, size - 12);
        List<RussianLetteHistoryBean> recent = historyBeans.subList(start, size);
        int total = recent.size();
        long redCount = 0, blackCount = 0, oddCount = 0, evenCount = 0;
        for (RussianLetteHistoryBean bean : recent) {
            RussianLetteMessageConstant.Number number =
                    RussianLetteMessageConstant.Numbers.getNumber(bean.diceData);
            if (number != null) {
                if (number.isRed) redCount++;
                if (number.isBlack) blackCount++;
                if (number.isOdd) oddCount++;
                if (number.isEvent) evenCount++;
            }
        }
        RussianLetteProb prob = new RussianLetteProb();
        prob.red = (double) redCount / total;
        prob.black = (double) blackCount / total;
        prob.odd = (double) oddCount / total;
        prob.event = (double) evenCount / total;
        return prob;
    }

    // =====================================================================
    // 结算信息构建
    // =====================================================================

    /**
     * 从历史记录构建结算信息（仅含开奖号码和中奖区域，不含玩家金币变化）
     * <p>用于 DRAW_ON 阶段广播和 SETTLEMENT 阶段相变通知。</p>
     *
     * @param historyBean 本局历史记录
     * @return 部分填充的结算信息（diceSettlementInfo 为 null）
     */
    public static RussianLetteSettlementInfo buildSettlementInfoFromHistory(
            RussianLetteHistoryBean historyBean) {
        RussianLetteSettlementInfo info = new RussianLetteSettlementInfo();
        info.rewardAreaIdx = historyBean.betIdxId;
        if (historyBean.diceData == 37) {
            info.diceData = 0;
        } else {
            info.diceData = historyBean.diceData;
        }// diceSettlementInfo 留 null，由结算阶段 settlementDice 填充
        return info;
    }

    /**
     * 构建结算信息体（内部调用 buildSettlementInfoFromHistory）
     */
    private static RussianLetteSettlementInfo buildSettlementInfo(
            RussianLetteHistoryBean russianLetteHistoryBean) {
        return buildSettlementInfoFromHistory(russianLetteHistoryBean);
    }

    // =====================================================================
    // 广播消息构建
    // =====================================================================

    /**
     * 构建结算广播消息（SETTLEMENT 阶段推送给全房间玩家）
     *
     * @param russianLetteHistoryBean 本局历史记录（含 diceData 和 betIdxId）
     * @return 填充了开奖结果的结算通知（playerChangedGolds 由 settlementDice 填充）
     */
    public static NotifyRussianLetteSettlement notifyAnimalsSettlement(
            RussianLetteHistoryBean russianLetteHistoryBean) {
        NotifyRussianLetteSettlement settlement = new NotifyRussianLetteSettlement();
        settlement.settlementInfo = buildSettlementInfo(russianLetteHistoryBean);
        return settlement;
    }

    /**
     * 构建桌面信息通知（进入房间 / 断线重连）
     *
     * @param playerId       目标玩家 ID
     * @param gameController 游戏控制器
     * @param isInitial      是否为初始化（首次进入）
     * @return 桌面信息通知
     */
    public static NotifyRussianLetteTableInfo notifyAnimalsTableInfo(
            long playerId, BaseTableGameController<RussianLetteGameDataVo> gameController, boolean isInitial) {
        NotifyRussianLetteTableInfo tableInfo = new NotifyRussianLetteTableInfo();
        RussianLetteGameDataVo gameDataVo = gameController.getGameDataVo();
        tableInfo.baseDiceTableInfo =
                BaseDiceMessageBuilder.buildDiceTableInfo(playerId, gameController, gameDataVo, isInitial);
        tableInfo.settlementHistory = gameDataVo.getWinAreaCfgIdHistory();
        tableInfo.settlementInfo = gameDataVo.getSettlementInfo();
        return tableInfo;
    }

    // =====================================================================
    // 摘要信息构建
    // =====================================================================

    /**
     * 构建单个房间详情摘要（{@link RussianLetteSingleRes}）
     * <p>供 {@code REQ_RUSSIAN_LETTE_SUMMARY} 单房间查询使用，包含 roundId、needClearRoad 等详细字段。</p>
     *
     * @param gameController 目标房间的游戏控制器
     * @return 单房间详情摘要
     */
    public static RussianLetteSingleRes buildRussianLetteSummary(
            BaseTableGameController<RussianLetteGameDataVo> gameController) {
        RussianLetteGameDataVo dataVo = gameController.getGameDataVo();
        RussianLetteSingleRes summary = new RussianLetteSingleRes();

        // 基础阶段信息（roomId 存放在 baseInfo 内）
        summary.baseInfo = buildBaseInfo(gameController);

        // 历史转盘结果（取 diceData 字段列表）
        summary.cardStateList = dataVo.getWinAreaCfgIdHistory().stream()
                .map(b -> {
                    if (b.diceData == 37) {
                        return 0;
                    }
                    return b.diceData;
                })
                .toList();

        // 近 12 局概率信息
        summary.prob = buildProb(dataVo.getWinAreaCfgIdHistory());
        return summary;
    }

    /**
     * 构建房间列表摘要（{@link RussianLetteSummary}）
     * <p>供 {@code REQ_RUSSIAN_LETTE_SUMMARY_LIST} / {@code REQ_RUSSIAN_LETTE_OTHER_SUMMARY_LIST}
     * 列表接口使用，包含顶层 roomId 字段，方便客户端识别房间。</p>
     *
     * @param gameController 目标房间的游戏控制器
     * @return 房间列表摘要
     */
    public static RussianLetteSummary buildRussianLetteSummaryInfo(
            BaseTableGameController<RussianLetteGameDataVo> gameController) {
        RussianLetteGameDataVo dataVo = gameController.getGameDataVo();
        RussianLetteSummary summary = new RussianLetteSummary();
        // 顶层 roomId，供列表中快速定位房间
        summary.roomId = dataVo.getRoomId();

        // 基础阶段信息
        summary.baseInfo = buildBaseInfo(gameController);

        // 历史转盘结果（取 diceData 字段列表）
        summary.cardStateList = dataVo.getWinAreaCfgIdHistory().stream()
                .map(b -> {
                    if (b.diceData == 37) {
                        return 0;
                    }
                    return b.diceData;
                })
                .toList();

        // 近 12 局概率信息
        summary.prob = buildProb(dataVo.getWinAreaCfgIdHistory());
        return summary;
    }

    // =====================================================================
    // 玩家首次进入 / 断线重连响应
    // =====================================================================

    /**
     * 构建玩家首次进入或断线重连时的完整桌面信息响应（{@link RespRussianLetteInfo}）
     * <p>
     * 包含所有 10 个字段：gamePhase、cardStateList、russianletteTableInfo、playerChangedGolds、
     * russianletteSettlementInfo、betInfoList、playerTotalNum、prob、playNum、allBet。
     * </p>
     *
     * @param playerId       请求玩家 ID
     * @param gameController 游戏控制器
     * @return 已填充全部字段的响应对象
     */
    public static RespRussianLetteInfo buildRespRussianLetteInfo(
            long playerId, BaseTableGameController<RussianLetteGameDataVo> gameController) {
        RussianLetteGameDataVo dataVo = gameController.getGameDataVo();
        RespRussianLetteInfo resp = new RespRussianLetteInfo(Code.SUCCESS);

        // ── 1. 当前游戏阶段 + 开奖结果 ─────────────────────────────────────────
        EGamePhase currentPhase = gameController.getCurrentGamePhase();
        RussianLetteStageInfo stageInfo = new RussianLetteStageInfo();
        stageInfo.gamePhase = currentPhase;
        stageInfo.endTime = dataVo.getPhaseEndTime();
        // 开奖/结算阶段：将当前开奖结果写入 stageInfo（37→0 映射）
        RussianLetteHistoryBean drawBean = dataVo.getDrawPhaseHistoryBean();
        if (drawBean != null) {
            stageInfo.diceData = drawBean.diceData == 37 ? 0 : drawBean.diceData;
        }
        resp.stageInfo = stageInfo;

        // ── 2. 历史转盘结果（37→0 映射，最多 recordsNum 条）───────────────────
        resp.cardStateList = dataVo.getWinAreaCfgIdHistory().stream()
                .map(b -> b.diceData == 37 ? 0 : b.diceData)
                .toList();

        // ── 3. 桌面数据（玩家列表、倒计时、区域下注信息、概率）─────────────────
        RussianLetteInfo tableInfo = new RussianLetteInfo();
        tableInfo.tablePlayerInfoList = TableMessageBuilder.buildTablePlayerInfo(
                gameController, playerId, dataVo, TableConstant.ON_TABLE_PLAYER_NUM);
        tableInfo.tableCountDownTime = dataVo.getPhaseEndTime();
//        tableInfo.totalTime = calcPhaseTotalTime(currentPhase, dataVo.getRoomCfg().getStageTime());
        List<BetTableInfo> areaInfos = TableMessageBuilder.buildBetTableInfos(dataVo, true);
        tableInfo.tableAreaInfos = TableMessageBuilder.buildPlayerBetInfo(areaInfos, dataVo, playerId);
        resp.russianletteTableInfo = tableInfo;

        // ── 4. 结算阶段的玩家金币变化（非结算阶段为 null）─────────────────────
        RussianLetteSettlementInfo settlementInfo = dataVo.getSettlementInfo();
        if (settlementInfo != null && settlementInfo.diceSettlementInfo != null) {
            resp.playerChangedGolds = settlementInfo.diceSettlementInfo.playerChangedGolds;
        }

        // ── 5. 结算信息（非结算阶段为 null）──────────────────────────────────
        resp.russianletteSettlementInfo = settlementInfo;

        // ── 6. 压分列表 ──────────────────────────────────────────────────────
        resp.betInfoList = dataVo.getRoomCfg().getBetList();

        // ── 7. 玩家总人数 ────────────────────────────────────────────────────
        resp.playerTotalNum = dataVo.getPlayerNum();

        // ── 8. 概率信息 ──────────────────────────────────────────────────────
        resp.prob = buildProb(dataVo.getWinAreaCfgIdHistory());

        // ── 9. 玩家牌桌玩的次数 ──────────────────────────────────────────────
        GamePlayer gamePlayer = dataVo.getGamePlayer(playerId);
        if (gamePlayer != null) {
            resp.playNum = gamePlayer.getTableGameData().getBetInfoList().size();
        }

        // ── 10. 累计本局下注总金额（断线重连同步）────────────────────────────
        Map<Integer, List<Integer>> playerBetInfo = dataVo.getPlayerBetInfo(playerId);
        if (playerBetInfo != null) {
            resp.allBet = playerBetInfo.values().stream()
                    .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
                    .sum();
        }

        return resp;
    }

    // =====================================================================
    // 内部工具
    // =====================================================================

    /**
     * 构建完整的 {@link RussianLetteBaseInfo}，填充所有字段。
     * <p>两处摘要方法共用，避免遗漏字段。</p>
     */
    private static RussianLetteBaseInfo buildBaseInfo(
            BaseTableGameController<RussianLetteGameDataVo> gameController) {
        RussianLetteGameDataVo dataVo = gameController.getGameDataVo();
        RussianLetteBaseInfo baseInfo = new RussianLetteBaseInfo();
        baseInfo.roomId            = dataVo.getRoomId();
        baseInfo.wareId            = gameController.getRoom().getRoomCfgId();
        baseInfo.phaseEndTimestamp = dataVo.getPhaseEndTime();
        baseInfo.serverCurrentTime = System.currentTimeMillis();
        // 当前游戏阶段
        EGamePhase currentPhase    = gameController.getCurrentGamePhase();
        baseInfo.eGamePhase        = currentPhase;
        // 当前阶段总时长（秒）
        baseInfo.phaseTotalTime    = calcPhaseTotalTime(currentPhase, dataVo.getRoomCfg().getStageTime());
        return baseInfo;
    }

    /**
     * 根据当前阶段枚举查找对应的阶段总时长（单位：秒）。
     * <p>stageTime 索引约定：[0]=REST  [1]=BET  [2]=DRAW_ON  [3]=SETTLEMENT</p>
     */
    private static int calcPhaseTotalTime(EGamePhase phase, List<Integer> stageTime) {
        if (phase == null || stageTime == null) {
            return 0;
        }
        return switch (phase) {
            case REST                       -> stageTime.size() > 0 ? stageTime.get(0) : 0;
            case BET                        -> stageTime.size() > 1 ? stageTime.get(1) : 0;
            case DRAW_ON                    -> stageTime.size() > 2 ? stageTime.get(2) : 0;
            case GAME_ROUND_OVER_SETTLEMENT -> stageTime.size() > 3 ? stageTime.get(3) : 0;
            default                         -> 0;
        };
    }
}
