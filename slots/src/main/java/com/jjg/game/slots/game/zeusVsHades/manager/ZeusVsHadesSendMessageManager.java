package com.jjg.game.slots.game.zeusVsHades.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;

import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesAwardLineInfo;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesGameRunInfo;
import com.jjg.game.slots.game.zeusVsHades.data.ZeusVsHadesResultLib;
import com.jjg.game.slots.game.zeusVsHades.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author lihaocao
 * @date 2025/12/2 17:40
 */
@Component
public class ZeusVsHadesSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private ZeusVsHadesGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private ZeusVsHadesGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, ZeusVsHadesGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResZeusVsHadesEnterGame res = new ResZeusVsHadesEnterGame(Code.SUCCESS);
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
                    ZeusVsHadesPoolInfo poolInfo = new ZeusVsHadesPoolInfo();
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
    public void sendStartGameMessage(PlayerController playerController, ZeusVsHadesGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResZeusVsHadesStartGame res = new ResZeusVsHadesStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //图标信息
            res.iconList = IntStream.range(1, gameRunInfo.getIconArr().length).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //免费游戏中累计获得金币
            if (gameRunInfo.getStatus() == ZeusVsHadesConstant.Status.ZEUS) {
                res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            } else {
                res.totalWinGold = 0;
            }
            //下一次状态
            res.status = gameRunInfo.getData().getStatus();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            ZeusVsHadesResultLib lib = (ZeusVsHadesResultLib) gameRunInfo.getResultLib();

            Map<Integer, Integer> vsTimes = lib.getVsTimes();
            List<KVInfo> wildColumnTimes = new ArrayList<>();
            vsTimes.forEach((key, value) -> {
                KVInfo kvInfo = new KVInfo();
                kvInfo.key = key + 1;
                kvInfo.value = value;
                wildColumnTimes.add(kvInfo);
            });
            res.wildColumnTimes = wildColumnTimes;

            Map<Integer, Set<Integer>> replaceWildIndexs = lib.getReplaceWildIndexs();
            Set<Integer> set = replaceWildIndexs.get(1);
            if(set != null && !set.isEmpty()){
                res.hadesWildSet = set;
            }else {
                res.hadesWildSet = new HashSet<>();
            }

            res.wildStatus = lib.getWildStatus();

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
    private List<ZeusVsHadesIconInfo> addRewardIcons(int[] arr, List<ZeusVsHadesAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        List<ZeusVsHadesIconInfo> iconInfolist = new ArrayList<>();
        for (ZeusVsHadesAwardLineInfo awardLineInfo : awardLineInfoList) {
            ZeusVsHadesIconInfo iconInfo = new ZeusVsHadesIconInfo();
            List<Integer> indexList = new ArrayList<>();
            indexList.addAll(awardLineInfo.getSameIconSet());
            iconInfo.iconIndexs = indexList;
            iconInfo.winIcons = awardLineInfo.getSameIcon();
            iconInfo.linId = awardLineInfo.getLineId();
            iconInfo.win = awardLineInfo.getBaseTimes() * oneBetScore;
            iconInfolist.add(iconInfo);
        }

        return iconInfolist;
    }

    /**
     * 发送二选一结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendFreeChooseOneMessage(PlayerController playerController, ZeusVsHadesGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResZeusVsHadesFreeChooseOne res = new ResZeusVsHadesFreeChooseOne(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
        } else {
            log.debug("二选一错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回二选一结果", false);
    }

    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, ZeusVsHadesGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResZeusVsHadesPoolInfo res = new ResZeusVsHadesPoolInfo(gameRunInfo.getCode());
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
