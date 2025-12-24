package com.jjg.game.slots.game.moneyrabbit.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitGameRunInfo;
import com.jjg.game.slots.game.moneyrabbit.pb.MoneyRabbitPoolInfo;
import com.jjg.game.slots.game.moneyrabbit.pb.ResMoneyRabbitEnterGame;
import com.jjg.game.slots.game.moneyrabbit.pb.ResMoneyRabbitPool;
import com.jjg.game.slots.game.moneyrabbit.pb.ResMoneyRabbitStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MoneyRabbitSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private MoneyRabbitGameManager gameManager;
    @Autowired
    private MoneyRabbitGenerateManager generateManager;
    @Autowired
    private SlotsLogger slotsLogger;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, MoneyRabbitGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResMoneyRabbitEnterGame res = new ResMoneyRabbitEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.poolValue = gameManager.getPoolValueByRoomCfgId(config.getId());

            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();

            //奖池信息
            if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    MoneyRabbitPoolInfo poolInfo = new MoneyRabbitPoolInfo();
                    poolInfo.id = poolId;
                    poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                    poolInfo.maxTimes = poolCfg.getFakePoolMax();
                    poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                    poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                    res.poolList.add(poolInfo);
                }
            }
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
    public void sendStartGameMessage(PlayerController playerController, MoneyRabbitGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResMoneyRabbitStartGame res = new ResMoneyRabbitStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //图标信息
            res.iconList = IntStream.range(1, gameRunInfo.getIconArr().length).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            //中奖线信息
            res.winIconInfoList = gameRunInfo.getAwardLineInfos();
            res.status = gameRunInfo.getStatus();
            res.rewardPoolValue = gameRunInfo.getSmallPoolGold();
            res.freeModeTotalReward = gameRunInfo.getFreeModeTotalReward();
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //金钱信息
            res.coinInfoList = gameRunInfo.getCoinInfoList();

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, MoneyRabbitGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResMoneyRabbitPool res = new ResMoneyRabbitPool(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
//        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }
}
