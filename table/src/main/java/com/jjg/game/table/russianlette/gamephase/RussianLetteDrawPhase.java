package com.jjg.game.table.russianlette.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.RoomCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.dicecommon.DiceDataHolder;
import com.jjg.game.table.dicecommon.DiceUtils;
import com.jjg.game.table.dicecommon.phase.BaseDiceSettlementPhase;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.RussianLetteMessageConstant;
import com.jjg.game.table.russianlette.message.resp.RussianLetteHistoryBean;
import com.jjg.game.table.russianlette.message.resp.RussianLetteProb;
import com.jjg.game.table.russianlette.message.resp.RussianLetteSettlementInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 俄罗斯转盘开奖阶段（DRAW_ON）
 * <p>
 * 持续 {@code stageTime[2]} 秒（默认 9s，倒计时 8→0）。
 * <p>
 * 职责：
 * <ol>
 *   <li>触发回收机制判断，必要时通过 {@code generateRecyclingResults()} 生成干预骰子点数</li>
 *   <li>否则调用 {@code DiceUtils.randomDice(1, 0, 37)} 生成 [0,36] 随机点数</li>
 *   <li>通过 {@code DiceDataHolder.getWinPosWeightCfg()} 查找对应中奖区域配置</li>
 *   <li>创建历史记录 {@link RussianLetteHistoryBean} 并写入 {@code winAreaCfgIdHistory}</li>
 *   <li>将本局历史记录和中奖配置缓存到 {@link RussianLetteGameDataVo}，供结算阶段使用</li>
 *   <li>向全房间广播 {@code NotifyRussianLettePhaseChangInfo(DRAW_ON)} 携带开奖结果及概率</li>
 * </ol>
 * <p>
 * 注意：本阶段不进行金币结算，仅广播开奖号码，供客户端播放转盘动画。
 * 金币结算在后续的 {@link RussianLetteSettlementPhase} 中完成。
 *
 * @author lhc
 */
public class RussianLetteDrawPhase extends BaseDiceSettlementPhase<RussianLetteGameDataVo> {

    private final RoomManager roomManager;


    public RussianLetteDrawPhase(AbstractPhaseGameController<Room_BetCfg, RussianLetteGameDataVo> gameController) {
        super(gameController);
        roomManager  = CommonUtil.getContext().getBean(RoomManager.class);
    }

    @Override
    public EGamePhase getGamePhase() {
        return EGamePhase.DRAW_ON;
    }

    /**
     * 开奖阶段持续时间：stageTime[1]（默认 9s）
     */
    @Override
    public int getPhaseRunTime() {
        List<Integer> stageTime = gameDataVo.getRoomCfg().getStageTime();
        if (stageTime.size() >= 3) {
            return stageTime.get(1);
        }
        return 8000;
    }

    /**
     * 阶段开始：生成本局骰子点数，广播开奖结果
     */
    @Override
    public void phaseDoAction() {
        long startTime = System.currentTimeMillis();

        super.phaseDoAction();
        log.info("执行RussianLetteDrawPhase（开奖阶段）中phaseDoAction");
        // ── 1. 生成 1-37（37 代表绿色 0）随机骰子点数 ─────────────────────────
        //      优先级：GM 指定 > 回收干预 > 随机
        List<Integer> randomNumDice = null;

        // GM 测试：如果设置了指定开奖值，直接使用（仅生效一局）
        int testDice = gameDataVo.getTestDiceData();
        if (testDice >= 0) {
            randomNumDice = List.of(testDice);
            gameDataVo.setTestDiceData(-1);
            log.info("俄罗斯转盘 {} GM 指定开奖 diceData={}", gameDataVo.roomLogInfo(), testDice);
        }

        // 回收干预
        if (randomNumDice == null) {
            Pair<Long, Long> currentPool = canTriggerRecycling();
            if (currentPool != null) {
                List<Integer> result = generateRecyclingResults(1, 1, 37, EGameType.RUSSIAN_ROULETTE);
                if (result == null) {
                    log.error("俄罗斯转盘回收触发 生成结果失败 当前池:{} 标准池:{}",
                            currentPool.getFirst(), currentPool.getSecond());
                } else {
                    randomNumDice = result;
                    log.info("俄罗斯转盘回收触发 生成结果成功 当前池:{} 标准池:{}",
                            currentPool.getFirst(), currentPool.getSecond());
                }
            }
        }

        // 随机生成（数据库 1-37，其中 37 代表绿色 0）
        if (randomNumDice == null) {
            randomNumDice = DiceUtils.randomDice(1, 1, 37);
        }

        int diceData = randomNumDice.getFirst();
        RussianLetteMessageConstant.Number number = RussianLetteMessageConstant.Numbers.getNumber(diceData);
        log.info("俄罗斯转盘 {} 开奖：骰子={} isRed={} isBlack={} isOdd={} isEvent={}",
                gameDataVo.roomLogInfo(), diceData,
                number != null && number.isRed,
                number != null && number.isBlack,
                number != null && number.isOdd,
                number != null && number.isEvent);

        // ── 2. 查找中奖区域配置 ─────────────────────────────────────────────────
        List<WinPosWeightCfg> winPosWeightCfgs =
                DiceDataHolder.getWinPosWeightCfg(EGameType.RUSSIAN_ROULETTE, randomNumDice);
        if (winPosWeightCfgs == null || winPosWeightCfgs.isEmpty()) {
            log.error("俄罗斯转盘开奖异常，中奖区域为空，骰子：{}", randomNumDice);
            return;
        }

        // ── 3. 获取去重后的中奖 BetAreaCfg 列表 ────────────────────────────────
        List<BetAreaCfg> betAreaCfgs =
                winPosWeightCfgs.stream()
                        .map(WinPosWeightCfg::getBetArea)
                        .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
                        .stream()
                        .distinct()
                        .map(a -> GameDataManager.getBetAreaCfg((Integer) a))
                        .toList();
        log.debug("{} 开奖区域 ID: {} → BetArea IDs: {}",
                gameDataVo.roomLogInfo(),
                winPosWeightCfgs.stream().map(WinPosWeightCfg::getId).collect(Collectors.toList()),
                betAreaCfgs.stream().map(BetAreaCfg::getId).map(String::valueOf).collect(Collectors.joining(",")));

        // ── 4. 创建并保存历史记录（添加后概率统计包含本局）─────────────────────
        RussianLetteHistoryBean historyBean = buildAndSaveHistory(diceData, betAreaCfgs);

        // ── 5. 将开奖数据缓存到 DataVo，供结算阶段（SETTLEMENT）使用 ────────────
        gameDataVo.setDrawPhaseHistoryBean(historyBean);
        gameDataVo.setDrawPhaseWinCfgs(winPosWeightCfgs);
        gameDataVo.getGamePlayerMap().forEach((k,v)->{
            v.getTableGameData().addPlayNum();
        });
        // ── 6. 构建本阶段广播的 settlementInfo（不含金币结算，仅告知开奖号码）──
        RussianLetteSettlementInfo settlementInfo = RussianLetteMessageBuilder.buildSettlementInfoFromHistory(historyBean);

        // ── 7. 基于最新历史（含本局）计算概率 ──────────────────────────────────
        RussianLetteProb prob = RussianLetteMessageBuilder.buildProb(gameDataVo.getWinAreaCfgIdHistory());

        // ── 8. 广播开奖阶段变化通知 ─────────────────────────────────────────────
        broadcastMsgToRoom(RussianLetteMessageBuilder.buildPhaseChangInfo(
                EGamePhase.DRAW_ON,
                gameDataVo.getPhaseEndTime(),
                prob,
                settlementInfo));
        log.debug("俄罗斯转盘 {} DRAW_ON 广播完成：{}", gameDataVo.roomLogInfo(), JSON.toJSONString(settlementInfo));

        // 通知所有观察者（房间列表页玩家）
        RussianLetteMessageBuilder.notifyObserversOnPhaseChange(
                (BaseTableGameController<RussianLetteGameDataVo>) gameController);

        long endTime = System.currentTimeMillis();
        log.info("开奖阶段  执行时间：{}   距离下个阶段时间：{}",endTime-startTime,gameDataVo.getPhaseEndTime()-endTime);
    }

    /**
     * 构建历史记录并写入 DataVo 的历史队列
     *
     * @param diceData    本局转盘落点数字（0-36）
     * @param betAreaCfgs 中奖 BetArea 列表
     * @return 已构建并已写入队列的历史 Bean
     */
    private RussianLetteHistoryBean buildAndSaveHistory(int diceData, List<BetAreaCfg> betAreaCfgs) {
        RussianLetteHistoryBean historyBean = new RussianLetteHistoryBean();
        historyBean.diceData = diceData;
        historyBean.betIdxId = betAreaCfgs.stream().map(BetAreaCfg::getId).toList();
        // 写入滚动历史队列（超出 recordsNum 时自动移除最旧一条）
        gameDataVo.addWinAreaCfgIdHistory(historyBean);
        return historyBean;
    }

    @Override
    public void phaseFinish() {
        // 开奖阶段结束，无额外清理（数据由结算阶段使用后在 clearRoundData 中清除）
    }

    @Override
    public void onPlayerHalfwayJoinPhase(GamePlayer gamePlayer) {
        // 中途进入开奖阶段：向该玩家补推当前开奖阶段变化通知
        RussianLetteHistoryBean historyBean = gameDataVo.getDrawPhaseHistoryBean();
        if (historyBean == null) {
            return;
        }
        RussianLetteSettlementInfo settlementInfo =
                RussianLetteMessageBuilder.buildSettlementInfoFromHistory(historyBean);
        RussianLetteProb prob = RussianLetteMessageBuilder.buildProb(gameDataVo.getWinAreaCfgIdHistory());
        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(gamePlayer.getId(), RussianLetteMessageBuilder.buildPhaseChangInfo(
                EGamePhase.DRAW_ON,
                gameDataVo.getPhaseEndTime(),
                prob,
                settlementInfo)));
    }

    @Override
    public void onPlayerHalfwayExitPhase(GamePlayer gamePlayer) {
        // 中途退出，无需额外处理
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
