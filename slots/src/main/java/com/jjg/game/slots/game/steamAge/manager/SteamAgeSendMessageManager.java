package com.jjg.game.slots.game.steamAge.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.steamAge.data.SteamAgeAwardLineInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgeGameRunInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import com.jjg.game.slots.game.steamAge.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author lihaocao
 * @date 2025/12/2 17:40
 */
@Component
public class SteamAgeSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private SteamAgeGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private SteamAgeGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResSteamAgeEnterGame res = new ResSteamAgeEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
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
                    SteamAgePoolInfo poolInfo = new SteamAgePoolInfo();
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
    public void sendStartGameMessage(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResSteamAgeStartGame res = new ResSteamAgeStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //免费游戏中累计获得金币
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            SteamAgeResultLib lib = (SteamAgeResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getIconArr(), lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    /**
     * 添加中奖图标信息
     *
     * @param awardLineInfoList
     * @param oneBetScore
     * @return
     */
    private SteamAgeIconInfo addRewardIcons(int[] arr, List<SteamAgeAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        SteamAgeIconInfo iconInfo = new SteamAgeIconInfo();

        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
//        Set<Integer> replaceWildIndexs = new HashSet<>();

        awardLineInfoList.forEach(info -> {
            indexSet.addAll(info.getSameIconSet());
            winIconSet.add(info.getSameIcon());
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        });

        iconInfo.iconIndexs = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
//        iconInfo.replaceWildIndexs = new ArrayList<>(replaceWildIndexs);
        return iconInfo;
    }


    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResSteamAgePoolInfo res = new ResSteamAgePoolInfo(gameRunInfo.getCode());
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
}
