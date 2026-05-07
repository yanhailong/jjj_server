package com.jjg.game.slots.game.garaGemstone2.manager;

import cn.hutool.core.collection.CollUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2GameRunInfo;
import com.jjg.game.slots.game.garaGemstone2.pb.GaraGemstone2PoolInfo;
import com.jjg.game.slots.game.garaGemstone2.pb.ResGaraGemstone2EnterGame;
import com.jjg.game.slots.game.garaGemstone2.pb.ResGaraGemstone2PoolValue;
import com.jjg.game.slots.game.garaGemstone2.pb.ResGaraGemstone2StartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GaraGemstone2SendMessageManager extends BaseSendMessageManager {
    @Autowired
    private GaraGemstone2GameManager gameManager;
    @Autowired
    private GaraGemstone2GenerateManager generateManager;
    @Autowired
    private SlotsLogger logger;

    /**
     * 发送配置信息
     * @param playerController
     * @param gameRunInfo
     */
    public void sendConfigMessage(PlayerController playerController, GaraGemstone2GameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResGaraGemstone2EnterGame res = new ResGaraGemstone2EnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for(long[] arr : list){
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            res.poolValue = gameManager.getPoolValueByRoomCfgId(config.getId());
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();
            // 奖池信息
            if (CollUtil.isNotEmpty(prizePoolIdList)) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    GaraGemstone2PoolInfo poolInfo = new GaraGemstone2PoolInfo();
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
    public void sendStartGameMessage(PlayerController playerController, GaraGemstone2GameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResGaraGemstone2StartGame res = new ResGaraGemstone2StartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.status = gameRunInfo.getStatus();
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //图标信息
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            res.rewardPoolValue = gameRunInfo.getSmallPoolGold();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.winIconInfoList = gameRunInfo.getAwardLineInfos();
            res.multiplyAxisTimes = gameRunInfo.getMultiplyAxisTimes();
            res.wheelTimes = gameRunInfo.getWheelTimes();
            logger.gameResult(playerController.getPlayer(), gameRunInfo,res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    public void sendPoolValueMessage(PlayerController playerController, GaraGemstone2GameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResGaraGemstone2PoolValue res = new ResGaraGemstone2PoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.major = gameRunInfo.getMajor();
        } else {
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }
}
