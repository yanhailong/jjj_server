package com.jjg.game.table.baccarat.message;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.GlobalConfigCfg;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.BaccaratTempRoom;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.resp.*;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public class BaccaratMessageBuilder {

    private static final Logger log = LoggerFactory.getLogger(BaccaratMessageBuilder.class);

    /**
     * 构建百家乐的摘要数据
     */
    public static BaccaratTableSummary buildBaccaratSummaryInfo(BaccaratGameController gameController) {
        BaccaratGameDataVo gameDataVo = gameController.getGameDataVo();
        BaccaratTableSummary baccaratTableSummary = new BaccaratTableSummary();
        baccaratTableSummary.baccaratBaseInfo = buildBaccaratBaseInfo(gameController);
        baccaratTableSummary.cardStateList = new ArrayList<>(gameDataVo.getBetRecord());
        baccaratTableSummary.roomId = gameDataVo.getRoomId();
        return baccaratTableSummary;
    }


    /**
     * 通知所有的观察者
     */
    public static void notifyObserversOnPhaseChange(BaccaratGameController gameController) {
        BaccaratTempRoom baccaratTempRoom = CommonUtil.getContext().getBean(BaccaratTempRoom.class);
        NotifyBaccaratTableSummary notifyBaccaratTableSummary =
            BaccaratMessageBuilder.buildBaccaratSingleSummaryInfo(gameController);
        int roomCfgId = gameController.getGameDataVo().getRoomCfg().getId();
        Collection<PlayerController> playerControllers =
            baccaratTempRoom.getBaccaratObserverPlayers(roomCfgId).values();
        playerControllers.forEach(playerController -> {
            log.debug("给临时房间中的玩家：{} 发送：{} ", playerController.playerId(), JSON.toJSON(notifyBaccaratTableSummary));
            playerController.send(notifyBaccaratTableSummary);
        });
    }


    /**
     * 获取单局摘要数据
     */
    public static NotifyBaccaratTableSummary buildBaccaratSingleSummaryInfo(BaccaratGameController gameController) {
        NotifyBaccaratTableSummary notifyBaccaratTableSummary = new NotifyBaccaratTableSummary();
        BaccaratGameDataVo gameDataVo = gameController.getGameDataVo();
        notifyBaccaratTableSummary.tableSummary = new BaccaratTableSingleRes();
        notifyBaccaratTableSummary.tableSummary.baccaratBaseInfo = buildBaccaratBaseInfo(gameController);
        int roundId = gameDataVo.getBetRecord().size() - 1;
        if (!gameDataVo.getBetRecord().isEmpty()) {
            notifyBaccaratTableSummary.tableSummary.baccaratCardState = gameDataVo.getBetRecord().get(roundId);
        }
        notifyBaccaratTableSummary.tableSummary.roundId = roundId;
        if (gameController.getCurrentGamePhase() == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyBaccaratTableSummary.tableSummary.needClearRoad = gameDataVo.getCardList().size() < 6;
        }
        return notifyBaccaratTableSummary;
    }

    /**
     * 构建百家乐外部展示的基础信息
     */
    private static BaccaratBaseInfo buildBaccaratBaseInfo(BaccaratGameController gameController) {
        BaccaratGameDataVo gameDataVo = gameController.getGameDataVo();
        BaccaratBaseInfo baccaratBaseInfo = new BaccaratBaseInfo();
        baccaratBaseInfo.eGamePhase = gameController.getCurrentGamePhase();
        baccaratBaseInfo.roomId = gameDataVo.getRoomId();
        baccaratBaseInfo.phaseEndTimestamp = gameDataVo.getPhaseEndTime();
        baccaratBaseInfo.serverCurrentTime = System.currentTimeMillis();
        baccaratBaseInfo.phaseTotalTime = Math.toIntExact(gameDataVo.getPhaseRunTime());
        baccaratBaseInfo.totalCardNum = gameDataVo.getInitCardNum();
        baccaratBaseInfo.remainingCardNum = gameDataVo.getCardList().size();
        baccaratBaseInfo.wareId =
            gameController.getRoom().getRoomCfgId() - EGameType.BACCARAT.getGameTypeId() * 10;
        return baccaratBaseInfo;
    }

    /**
     * 断线重连进入时
     */
    public static NotifyBaccaratTableInfo buildNotifyBaccaratTableInfo(BaccaratGameDataVo gameDataVo,
                                                                       EGamePhase eGamePhase,
                                                                       NotifyBaccaratSettlementInfo settlementInfo) {
        NotifyBaccaratTableInfo notifyBaccaratTableInfo = new NotifyBaccaratTableInfo();
        if (eGamePhase == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            notifyBaccaratTableInfo.baccaratSettlementInfo = settlementInfo.baccaratSettlementInfo;
            notifyBaccaratTableInfo.playerChangedGolds = settlementInfo.playerChangedGolds;
        }
        notifyBaccaratTableInfo.cardStateList = gameDataVo.getBetRecord();
        notifyBaccaratTableInfo.gamePhase = eGamePhase;
        notifyBaccaratTableInfo.baccaratTableInfo = buildTableInfo(gameDataVo, true, true);
        return notifyBaccaratTableInfo;
    }

    /**
     * 玩家中途加入，结算和每局开始时发送此数据
     * <p>
     * 构建百家乐面板数据
     */
    public static RespBaccaratTableInfo buildRespBaccaratTableInfo(
        BaccaratGameDataVo gameDataVo, EGamePhase eGamePhase, NotifyBaccaratSettlementInfo settlementInfo) {
        RespBaccaratTableInfo respBaccaratTableInfo = new RespBaccaratTableInfo(Code.SUCCESS);
        if (settlementInfo != null) {
            respBaccaratTableInfo.baccaratSettlementInfo = settlementInfo.baccaratSettlementInfo;
            respBaccaratTableInfo.playerChangedGolds = settlementInfo.playerChangedGolds;
        }
        respBaccaratTableInfo.cardStateList = gameDataVo.getBetRecord();
        respBaccaratTableInfo.gamePhase = eGamePhase;
        respBaccaratTableInfo.baccaratTableInfo = buildTableInfo(gameDataVo, true, true);
        respBaccaratTableInfo.betInfoList = gameDataVo.getRoomCfg().getBetList();
        respBaccaratTableInfo.playerTotalNum = gameDataVo.getPlayerNum();
        GlobalConfigCfg globalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE);
        respBaccaratTableInfo.maxChipOnTable = globalConfigCfg.getIntValue();
        return respBaccaratTableInfo;
    }


    /**
     * 构建 消息通知
     */
    public static NotifyBaccaratBetStart buildNotifyBaccaratBetStart(BaccaratGameDataVo gameDataVo) {
        NotifyBaccaratBetStart notifyInfo = new NotifyBaccaratBetStart();
        notifyInfo.baccaratTableInfo = buildTableInfo(gameDataVo, false, true);
        return notifyInfo;
    }

    /**
     * 构建结算消息通知
     */
    public static NotifyBaccaratSettlementInfo buildNotifySettlementMessage(BaccaratGameDataVo gameDataVo,
                                                                            List<PlayerChangedGold> changedGolds,
                                                                            BaccaratSettlementInfo settlementInfo) {
        NotifyBaccaratSettlementInfo notifyInfo = new NotifyBaccaratSettlementInfo();
        notifyInfo.baccaratSettlementInfo = settlementInfo;
        notifyInfo.baccaratTableInfo = buildTableInfo(gameDataVo, false, false);
        notifyInfo.playerChangedGolds = changedGolds;
        notifyInfo.needClearRoad = gameDataVo.getCardList().size() < 6;
        log.debug("{} 场上庄家牌：{} 庄家补牌：{} 庄家点数：{} 闲家牌：{} 闲家补牌：{} 闲家点数：{} 输赢结果：{} 牌型结果：{}",
            gameDataVo.roomLogInfo(),
            PokerCardUtils.toHumanString(settlementInfo.bankerCardIds),
            PokerCardUtils.toHumanString(settlementInfo.extraBankerCardId),
            settlementInfo.bankerPointId,
            PokerCardUtils.toHumanString(settlementInfo.playerCardIds),
            PokerCardUtils.toHumanString(settlementInfo.extraPlayerCardId),
            settlementInfo.playerPointId,
            settlementInfo.cardState.winState,
            settlementInfo.cardState.cardTypeWinState
        );
        return notifyInfo;
    }

    /**
     * 构建百家乐场上基础信息
     */
    public static BaccaratTableInfo buildTableInfo(BaccaratGameDataVo gameDataVo, boolean needPlayerBetGold,
                                                   boolean needTablePlayer) {
        BaccaratTableInfo tableInfo = new BaccaratTableInfo();
        tableInfo.tableAreaInfos = TableMessageBuilder.buildBetTableInfos(gameDataVo, needPlayerBetGold);
        tableInfo.tableCountDownTime = gameDataVo.getPhaseEndTime();
        tableInfo.totalTime = (int) (gameDataVo.getPhaseRunTime());
        if (needTablePlayer) {
            // 刷新场上的玩家数据
            List<GamePlayer> gamePlayers =
                gameDataVo.getGamePlayerMap().values().stream().filter(g -> g.getTableGameData().getSitNum() > 0).toList();
            tableInfo.tablePlayerInfoList =
                gamePlayers.stream().limit(TableConstant.ON_TABLE_PLAYER_NUM).map(TableMessageBuilder::buildTablePlayerInfo).toList();
        }
        return tableInfo;
    }

    /**
     * 添加玩家下注区域的数据
     */
    public static List<BetTableInfo> buildPlayerBetInfo(
        BaccaratTableInfo baccaratTableInfo, BaccaratGameDataVo gameDataVo, long playerId) {
        return TableMessageBuilder.buildPlayerBetInfo(baccaratTableInfo.tableAreaInfos, gameDataVo, playerId);
    }
}
