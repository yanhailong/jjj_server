package com.jjg.game.table.russianlette.message;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.dicecommon.message.BaseDiceMessageBuilder;
import com.jjg.game.table.russianlette.RussianLetteTempRoom;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.resp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 俄罗斯转盘消息构建工具类
 *
 * @author lhc
 */
public class RussianLetteMessageBuilder {
    private static final Logger log = LoggerFactory.getLogger(RussianLetteMessageBuilder.class);

    private static ClusterSystem clusterSystem;

    private static ClusterSystem getClusterSystem() {
        if (clusterSystem == null) {
            clusterSystem = CommonUtil.getContext().getBean(ClusterSystem.class);
        }
        return clusterSystem;
    }

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
        // 开奖/结算阶段：将开奖结果写入 stageInfo（37 代表绿色 0）
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
            return new RussianLetteProb();
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
        BigDecimal bd = new BigDecimal(prob.red);
        bd = bd.setScale(2, RoundingMode.HALF_UP);  // 四舍五入保留两位小数
        prob.red = bd.doubleValue();

        prob.black = (double) blackCount / total;
        bd = new BigDecimal(prob.black);
        bd = bd.setScale(2, RoundingMode.HALF_UP);  // 四舍五入保留两位小数
        prob.black = bd.doubleValue();

        prob.odd = (double) oddCount / total;
        bd = new BigDecimal(prob.odd);
        bd = bd.setScale(2, RoundingMode.HALF_UP);  // 四舍五入保留两位小数
        prob.odd = bd.doubleValue();

        prob.event = (double) evenCount / total;
        bd = new BigDecimal(prob.event);
        bd = bd.setScale(2, RoundingMode.HALF_UP);  // 四舍五入保留两位小数
        prob.event = bd.doubleValue();
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
//        if (historyBean.diceData == 37) {
//            info.diceData = 0;
//        } else {
//            info.diceData = historyBean.diceData;
//        }// diceSettlementInfo 留 null，由结算阶段 settlementDice 填充
        info.diceData = historyBean.diceData;
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
     * <p>
     * 填充字段：{@code settlementInfo}、{@code stageInfo}（GAME_ROUND_OVER_SETTLEMENT）、{@code prob}。
     * <p>
     * 注意：{@code playNum} 和 {@code allBet} 为玩家维度数据，由结算阶段按玩家单独设置后广播。
     *
     * @param russianLetteHistoryBean 本局历史记录（含 diceData 和 betIdxId）
     * @param gameDataVo              游戏数据（用于读取阶段信息和历史记录）
     * @return 填充了开奖结果的结算通知（playerChangedGolds 由 settlementDice 填充）
     */
    public static NotifyRussianLetteSettlement notifyAnimalsSettlement(
            RussianLetteHistoryBean russianLetteHistoryBean, RussianLetteGameDataVo gameDataVo) {
        NotifyRussianLetteSettlement settlement = new NotifyRussianLetteSettlement();
        settlement.settlementInfo = buildSettlementInfo(russianLetteHistoryBean);

        // 当前阶段信息（结算阶段 + 开奖号码）
        RussianLetteStageInfo stageInfo = new RussianLetteStageInfo();
        stageInfo.gamePhase = EGamePhase.GAME_ROUND_OVER_SETTLEMENT;
        stageInfo.endTime = gameDataVo.getPhaseEndTime();
        stageInfo.diceData = settlement.settlementInfo.diceData; // 37 代表绿色 0
        settlement.stageInfo = stageInfo;

        // 概率信息
        settlement.prob = buildProb(gameDataVo.getWinAreaCfgIdHistory());
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

        // 概率信息
        tableInfo.prob = buildProb(gameDataVo.getWinAreaCfgIdHistory());

        // 玩家牌桌玩的次数
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(playerId);
        if (gamePlayer != null) {
            tableInfo.playNum = gamePlayer.getTableGameData().getPlayNum();
        }

        // 累计本局下注总金额
        Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
        if (playerBetInfo != null) {
            tableInfo.allBet = playerBetInfo.values().stream()
                    .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
                    .sum();
        }
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
//        summary.cardStateList = dataVo.getWinAreaCfgIdHistory().stream()
//                .map(b -> {
//                    if (b.diceData == 37) {
//                        return 0;
//                    }
//                    return b.diceData;
//                })
//                .toList();
        summary.cardStateList = buildReversedCardStateList(dataVo.getWinAreaCfgIdHistory());
        // 近 12 局概率信息
        summary.prob = buildProb(dataVo.getWinAreaCfgIdHistory());
        // 对局 ID（历史记录条数 - 1，与百家乐 roundId = betRecord.size() - 1 一致）
        summary.roundId = Math.max(0, dataVo.getWinAreaCfgIdHistory().size() - 1);

        // 俄罗斯转盘无洗牌概念，始终不需要清除路单
        summary.needClearRoad = false;
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

        // 当前阶段 + 开奖结果
        RussianLetteStageInfo stageInfo = new RussianLetteStageInfo();
        stageInfo.gamePhase = gameController.getCurrentGamePhase();
        stageInfo.endTime = dataVo.getPhaseEndTime();
        RussianLetteHistoryBean drawBean = dataVo.getDrawPhaseHistoryBean();
        if (drawBean != null) {
//            stageInfo.diceData = drawBean.diceData == 37 ? 0 : drawBean.diceData;
            stageInfo.diceData = drawBean.diceData;
        }

        summary.stageInfo = stageInfo;

        // 基础阶段信息
        summary.baseInfo = buildBaseInfo(gameController);

        // 历史转盘结果（取 diceData 字段列表）
//            summary.cardStateList     = dataVo.getWinAreaCfgIdHistory().stream()
//                .map(b -> {
//                    if (b.diceData == 37) {
//                        return 0;
//                    }
//                    return b.diceData;
//                })
//                .toList();
        summary.cardStateList = buildReversedCardStateList(dataVo.getWinAreaCfgIdHistory());
        // 近 12 局概率信息
        summary.prob = buildProb(dataVo.getWinAreaCfgIdHistory());
        summary.roomType = dataVo.getRoomCfg().getRoomID();
        return summary;
    }

    // =====================================================================
    // 观察者推送（房间列表页玩家）
    // =====================================================================

    /**
     * 构建单条房间摘要通知（{@link NotifyRussianLetteTableSummary}）
     * <p>用于阶段变化时向观察者推送最新房间状态。</p>
     * <p>
     * 特殊处理：DRAW_ON（开奖）阶段不同步最新开奖数字，等 SETTLEMENT（结算）阶段再同步。
     * 例如：已开奖 3,0,27，当前 DRAW_ON 开出 35 → 推送时 cardStateList 仍为 3,0,27，diceData 不发送；
     * 进入 SETTLEMENT 阶段 → 推送时 cardStateList 变为 3,0,27,35，diceData = 35。
     * </p>
     *
     * @param gameController 目标房间的游戏控制器
     * @return 包含 {@link RussianLetteSummary} 的通知对象
     */
    public static NotifyRussianLetteTableSummary buildRussianLetteSingleSummaryInfo(
            BaseTableGameController<RussianLetteGameDataVo> gameController) {
        NotifyRussianLetteTableSummary notify = new NotifyRussianLetteTableSummary();
        notify.tableSummary = buildRussianLetteSummaryInfo(gameController);

        // DRAW_ON 阶段：不同步最新开奖数字，等结算阶段再同步
        if (gameController.getCurrentGamePhase() == EGamePhase.DRAW_ON) {
            // 清除当前开奖数字（int 默认 0 表示无数据）
            notify.tableSummary.stageInfo.diceData = 0;
            // cardStateList 是倒序的（最新在前），移除第一个即本局最新开奖数字
            if (notify.tableSummary.cardStateList != null && !notify.tableSummary.cardStateList.isEmpty()) {
                notify.tableSummary.cardStateList = new ArrayList<>(notify.tableSummary.cardStateList);
                notify.tableSummary.cardStateList.remove(0);
            }
        }

        return notify;
    }

    /**
     * 通知所有观察者（正在浏览房间列表但未进入房间的玩家 + 请求了 OtherSummaryList 的玩家）
     * <p>在 BET、DRAW_ON 和 SETTLEMENT 阶段调用，向观察者推送房间摘要更新。</p>
     *
     * @param gameController 当前房间的游戏控制器
     */
    @SuppressWarnings("unchecked")
    public static void notifyObserversOnPhaseChange(
            BaseTableGameController<RussianLetteGameDataVo> gameController) {
        RussianLetteTempRoom tempRoom = CommonUtil.getContext().getBean(RussianLetteTempRoom.class);
        NotifyRussianLetteTableSummary notify = buildRussianLetteSingleSummaryInfo(gameController);
        int roomCfgId = gameController.getGameDataVo().getRoomCfg().getId();
        ClusterSystem system = getClusterSystem();

        // 通知选房界面的观察者
        Set<Long> playerIds = tempRoom.getObserverPlayers(roomCfgId);
        for (Long playerId : playerIds) {
            system.sendToPlayer(notify, playerId);
        }

        // 通知请求了 OtherSummaryList 的观察者（游戏中查看同场次其他房间的玩家）
        Set<Long> otherObserverIds = tempRoom.getOtherSummaryObservers(roomCfgId);
        for (Long playerId : otherObserverIds) {
            system.sendToPlayer(notify, playerId);
        }
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
        // 开奖/结算阶段：将当前开奖结果写入 stageInfo（37 代表绿色 0）
        RussianLetteHistoryBean drawBean = dataVo.getDrawPhaseHistoryBean();
        if (drawBean != null) {
//            stageInfo.diceData = drawBean.diceData == 37 ? 0 : drawBean.diceData;
            stageInfo.diceData = drawBean.diceData;
        }

        resp.stageInfo = stageInfo;

        // ── 2. 历史转盘结果（37 代表绿色 0，最多 recordsNum 条）───────────────────
//        resp.cardStateList = dataVo.getWinAreaCfgIdHistory().stream()
//                .map(b -> b.diceData == 37 ? 0 : b.diceData)
//                .toList();
        resp.cardStateList = buildReversedCardStateList(dataVo.getWinAreaCfgIdHistory());
        // ── 3. 桌面数据（玩家列表、倒计时、区域下注信息、概率）─────────────────
        RussianLetteInfo tableInfo = new RussianLetteInfo();
        tableInfo.tablePlayerInfoList = TableMessageBuilder.buildTablePlayerInfo(
                gameController, playerId, dataVo, TableConstant.ON_TABLE_PLAYER_NUM);
        tableInfo.tableCountDownTime = dataVo.getPhaseEndTime();
//        tableInfo.totalTime = calcPhaseTotalTime(currentPhase, dataVo.getRoomCfg().getStageTime());
        List<BetTableInfo> betTableInfos = TableMessageBuilder.buildBetTableInfos(playerId, dataVo, true);
        //将玩家最后一次下注值放入到betValue
        Map<Integer, List<Integer>> betInfo = gameController.getGameDataVo().getPlayerBetInfo(playerId);
        if (CollectionUtil.isNotEmpty(betInfo)) {
            for (BetTableInfo betTableInfo : betTableInfos) {
                List<Integer> betValueList = betInfo.get(betTableInfo.betIdx);
                if (betValueList != null) {
                    betTableInfo.betValue = betValueList.getLast();
                }
            }
        }
        tableInfo.tableAreaInfos = betTableInfos;
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
            resp.playNum = gamePlayer.getTableGameData().getPlayNum();
        }

        // ── 10. 累计本局下注总金额（断线重连同步）────────────────────────────
        Map<Integer, List<Integer>> playerBetInfo = dataVo.getPlayerBetInfo(playerId);
        if (playerBetInfo != null) {
            resp.allBet = playerBetInfo.values().stream()
                    .flatMapToInt(list -> list.stream().mapToInt(Integer::intValue))
                    .sum();
        }
        resp.roomId = dataVo.getRoomId();
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
        baseInfo.roomId = dataVo.getRoomId();
        baseInfo.wareId = gameController.getRoom().getRoomCfgId();
        baseInfo.phaseEndTimestamp = dataVo.getPhaseEndTime();
        baseInfo.serverCurrentTime = System.currentTimeMillis();
        // 当前游戏阶段
        EGamePhase currentPhase = gameController.getCurrentGamePhase();
        baseInfo.eGamePhase = currentPhase;

        // 当前阶段总时长（秒）
        int calcPhaseTotalTime = calcPhaseTotalTime(currentPhase, dataVo.getRoomCfg().getStageTime());
//        log.info("currentPhase:{},phaseTotalTime:{}",currentPhase.getPhaseName(), calcPhaseTotalTime);
        baseInfo.phaseTotalTime = calcPhaseTotalTime(currentPhase, dataVo.getRoomCfg().getStageTime());
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
//            case REST                       -> stageTime.size() > 0 ? stageTime.get(0) : 0;
            case BET -> stageTime.size() > 0 ? stageTime.get(0) : 0;
            case DRAW_ON -> stageTime.size() > 1 ? stageTime.get(1) : 0;
            case GAME_ROUND_OVER_SETTLEMENT -> stageTime.size() > 2 ? stageTime.get(2) : 0;
            default -> 0;
        };
    }

    private static List<Integer> buildReversedCardStateList(List<RussianLetteHistoryBean> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(history.size());
        for (RussianLetteHistoryBean bean : history) {
//            int value = (bean.diceData == 37) ? 0 : bean.diceData;
            int value = bean.diceData;
            result.add(value);
        }
        Collections.reverse(result);
        return result;
    }
}
