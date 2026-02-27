package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
@Component
public class DollarExpressSendMessageManager extends BaseSendMessageManager {

    @Autowired
    private DollarExpressGameManager gameManager;
    @Autowired
    private DollarExpressGenerateManager generateManager;
    @Autowired
    private SlotsLogger logger;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController,DollarExpressGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResConfigInfo res = new ResConfigInfo(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for(long[] arr : list){
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);

            //奖池信息
            if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    PoolInfo poolInfo = new PoolInfo();
                    poolInfo.id = poolId;
                    poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                    poolInfo.maxTimes = poolCfg.getFakePoolMax();
                    poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                    poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                    res.poolList.add(poolInfo);
                }
            }

            res.dollarTargetCount = gameManager.getDollarExpressCollectDollarConfig().getMax();
            res.collectMinStake = gameManager.getDollarExpressCollectDollarConfig().getStakeAllBetScoreMin();
            res.dollarCollectedCount = gameRunInfo.getTotalDollars();
            SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = generateManager.getSpecialAuxiliaryPropConfigMap().get(DollarExpressConstant.SpecialAuxiliary.FREE_COUNT_CONFIG_ID);
            if (specialAuxiliaryPropConfig != null) {
                res.freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
            }
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
        } else {
            res.code = Code.NOT_FOUND;
            log.debug("未找到游戏配置  playerId={},roomCfgId={}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    /**
     * 发送游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendStartGameMessage(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResStartGame res = new ResStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //中奖线信息
            res.resultLineInfoList = gameRunInfo.getAwardLineInfos();
            //火车
            res.trainInfoList = gameRunInfo.getTrainList();
            //美元信息
            res.dollarsInfo = gameRunInfo.getDollarsInfo();
            res.totalDollars = gameRunInfo.getTotalDollars();
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //投资小游戏
            res.choosableAreas = gameRunInfo.getChoosableAreas();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //高亮展示
            res.highlightList = highlight(res,gameRunInfo);

            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.freeModeTotalReward = gameRunInfo.getFreeModeTotalReward();

            logger.gameResult(playerController.getPlayer(), gameRunInfo,res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }


    /**
     * 发送二选一
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendChooseOneMessage(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResChooseFreeModel res = new ResChooseFreeModel(gameRunInfo.getCode());
        res.freeCount = gameRunInfo.getRemainFreeCount();
        res.status = gameRunInfo.getStatus();

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回二选一结果", false);
    }

    /**
     * 返回投资游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendInvers(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResInvestArea res = new ResInvestArea(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.goldList = gameRunInfo.getInvestRewardGoldList();

            if (gameRunInfo.getInvestRewardGoldTrainCount() > 0) {
                res.allWinTrainInfo = goldTrain(gameRunInfo.getInvestRewardGoldTrainCount(), gameRunInfo.getInvestRewardGold());
            }
            res.allAreaUnLock = gameRunInfo.isAllAreaUnLock();
            res.allGold = playerController.getPlayer().getGold();
        } else {
            log.debug("投资游戏结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回投资游戏结果", false);
    }

    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResPoolValue res = new ResPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }

    /**
     * 金火车信息
     *
     * @param count
     * @param gold
     * @return
     */
    private TrainInfo goldTrain(int count, long gold) {
        TrainInfo trainInfo = new TrainInfo();
        trainInfo.type = DollarExpressConstant.BaseElement.ID_GOLD_TRAIN;
        trainInfo.goldList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            trainInfo.goldList.add(gold);
        }
        return trainInfo;
    }

    /**
     * 高亮展示
     *
     * @param res
     * @return
     */
    private List<Integer> highlight(ResStartGame res, DollarExpressGameRunInfo gameRunInfo) {
        if (res.status == DollarExpressConstant.Status.NORMAL) {  //普通
            //普通火车
            boolean normalTrain = res.trainInfoList != null && res.trainInfoList.stream().anyMatch(trainInfo -> generateManager.trainId(trainInfo.type));
            //黄金列车
            boolean goldTrain = res.trainInfoList != null && res.trainInfoList.stream().anyMatch(trainInfo -> trainInfo.type == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN);
            //现金奖励
            boolean safeBox = res.dollarsInfo != null && res.dollarsInfo.coinIndexId > 0;
            //找出所有的all board图标
            return getIconIndex(gameRunInfo,normalTrain,goldTrain,safeBox,false);
        } else if (res.status == DollarExpressConstant.Status.NOTMAL_ALL_BOARD || res.status == DollarExpressConstant.Status.GOLD_ALL_BOARD) {  //二选一
            //找出所有的all board图标
            return getIconIndex(gameRunInfo,false,false,false,true);
        }
        return null;
    }

    /**
     * 获取坐标
     * @param gameRunInfo
     * @param normalTrain
     * @param goldTrain
     * @param safeBox
     * @return
     */
    private List<Integer> getIconIndex(DollarExpressGameRunInfo gameRunInfo,boolean normalTrain,
                                       boolean goldTrain,boolean safeBox,boolean allBoard){
        List<Integer> highlightList = new ArrayList<>();
        for (int i = 1; i < gameRunInfo.getIconArr().length; i++) {
            int icon = gameRunInfo.getIconArr()[i];
            //添加普通火车对应图标坐标
            if(normalTrain){
                if (icon == DollarExpressConstant.BaseElement.ID_GREEN_TRAIN || icon == DollarExpressConstant.BaseElement.ID_RED_TRAIN ||
                        icon == DollarExpressConstant.BaseElement.ID_BLUE_TRAIN || icon == DollarExpressConstant.BaseElement.ID_PURPLE_TRAIN ||
                        icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                    highlightList.add(i);
                }
            }

            //添加黄金火车对应图标坐标
            if(goldTrain){
                if (icon == DollarExpressConstant.BaseElement.ID_DOLLAR || icon == DollarExpressConstant.BaseElement.ID_DOLLAR_2 || icon == DollarExpressConstant.BaseElement.ID_GOLD_TRAIN) {
                    highlightList.add(i);
                }
            }

            //添加现金奖励对应图标坐标
            if(safeBox){
                if (icon == DollarExpressConstant.BaseElement.ID_DOLLAR || icon == DollarExpressConstant.BaseElement.ID_DOLLAR_2 || icon == DollarExpressConstant.BaseElement.ID_SAFE_BOX) {
                    highlightList.add(i);
                }
            }

            //添加二选一对应图标坐标
            if(allBoard){
                if (icon == DollarExpressConstant.BaseElement.ID_ALL_ABOARD) {
                    highlightList.add(i);
                }
            }
        }
        return highlightList;
    }
}
