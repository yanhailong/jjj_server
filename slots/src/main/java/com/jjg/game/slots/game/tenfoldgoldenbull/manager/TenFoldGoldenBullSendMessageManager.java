package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullGameRunInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullPlayerGameData;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.bean.TenFoldGoldenBullPoolInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.res.ResTenFoldGoldenBullEnterGame;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.res.ResTenFoldGoldenBullPoolValue;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.res.ResTenFoldGoldenBullStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class TenFoldGoldenBullSendMessageManager extends BaseSendMessageManager {

    private final AbstractTenFoldGoldenBullGameManager gameManager;
    private final SlotsLogger slotsLogger;

    public TenFoldGoldenBullSendMessageManager(TenFoldGoldenBullGameManager gameManager, SlotsLogger slotsLogger) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqPegasusUnbridleEnterGame(PlayerController playerController, TenFoldGoldenBullGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        SendInfo sendInfo = new SendInfo();
        ResTenFoldGoldenBullEnterGame res = new ResTenFoldGoldenBullEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameRunInfo.getData() != null && gameRunInfo.getData().getAllBetScore() > 0 ? gameRunInfo.getData().getAllBetScore() : gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            TenFoldGoldenBullPlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            if (playerGameData.getStatus() == TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL) {
                if (playerGameData.getLuckyBull() != null) {
                    List<TenFoldGoldenBullResultLib> randomResult = playerGameData.getLuckyBull().getRandomResult();
                    if (CollectionUtil.isNotEmpty(randomResult)) {
                        TenFoldGoldenBullResultLib resultLib = randomResult.get(playerGameData.getCurrentRandomIndex());
                        res.iconList = Arrays.stream(resultLib.getIconArr(), 1, resultLib.getIconArr().length).boxed().collect(Collectors.toList());
                        res.scrollType = playerGameData.getLuckyBull().getRollerMode();
                    }
                }
            }
            res.poolList = new ArrayList<>();
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                TenFoldGoldenBullPoolInfo poolInfo = new TenFoldGoldenBullPoolInfo();
                poolInfo.id = poolId;
                poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                poolInfo.maxTimes = poolCfg.getFakePoolMax();
                poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                res.poolList.add(poolInfo);
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
    public void reqPegasusUnbridleStartGame(PlayerController playerController, TenFoldGoldenBullGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResTenFoldGoldenBullStartGame res = new ResTenFoldGoldenBullStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            res.winIconInfoList = gameRunInfo.getAwardLineInfos();
            res.scrollType = gameRunInfo.getScrollType();
            res.isLuckyBullEnd = gameRunInfo.isLuckyBullEnd();
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    public void sendPoolMessage(PlayerController playerController, TenFoldGoldenBullGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResTenFoldGoldenBullPoolValue res = new ResTenFoldGoldenBullPoolValue(gameRunInfo.getCode());
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
