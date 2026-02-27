package com.jjg.game.slots.game.elephantgod.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodAwardLineInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodGameRunInfo;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameData;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import com.jjg.game.slots.game.elephantgod.pb.bean.ElephantGodPoolInfo;
import com.jjg.game.slots.game.elephantgod.pb.bean.ElephantGodWinIconInfo;
import com.jjg.game.slots.game.elephantgod.pb.res.ResElephantGodEnterGame;
import com.jjg.game.slots.game.elephantgod.pb.res.ResElephantGodPoolValue;
import com.jjg.game.slots.game.elephantgod.pb.res.ResElephantGodStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ElephantGodSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private ElephantGodGameManager gameManager;
    @Autowired
    private ElephantGodGenerateManager generateManager;
    @Autowired
    private SlotsLogger slotsLogger;

    /**
     * 发送配置信息
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendConfigMessage(PlayerController playerController, ElephantGodGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        SendInfo sendInfo = new SendInfo();
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        ResElephantGodEnterGame res = new ResElephantGodEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameRunInfo.getData() != null && gameRunInfo.getData().getAllBetScore() > 0 ? gameRunInfo.getData().getAllBetScore() : gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            ElephantGodPlayerGameData playerGameData = gameRunInfo.getData();
            res.freeTotalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();
            res.poolList = new ArrayList<>();
            Object freeLib = playerGameData.getFreeLib();
            buildFreeGameInfo(freeLib, playerGameData, res);
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                ElephantGodPoolInfo poolInfo = new ElephantGodPoolInfo();
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
     * 构建免费游戏信息
     *
     * @param freeLib        免费游戏结果库
     * @param playerGameData 玩家信息
     * @param res            响应
     */
    private void buildFreeGameInfo(Object freeLib, ElephantGodPlayerGameData playerGameData, ResElephantGodEnterGame res) {
        if (freeLib instanceof ElephantGodResultLib lib) {
            List<SpecialAuxiliaryInfo> specialAuxiliaryInfos = lib.getSpecialAuxiliaryInfoList();
            if (CollectionUtil.isNotEmpty(specialAuxiliaryInfos)) {
                for (SpecialAuxiliaryInfo info : specialAuxiliaryInfos) {
                    if (CollectionUtil.isNotEmpty(info.getFreeGames())) {
                        JSONObject jsonObject = info.getFreeGames().get(playerGameData.getFreeIndex().get());
                        ElephantGodResultLib resultLib = jsonObject.toJavaObject(ElephantGodResultLib.class);
                        res.currentMultiplier = resultLib.getBasicMultiplier();
                        res.wildCount = resultLib.getWildCount();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 发送游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendStartGameMessage(PlayerController playerController, ElephantGodGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResElephantGodStartGame res = new ResElephantGodStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            ElephantGodResultLib lib = (ElephantGodResultLib) gameRunInfo.getResultLib();
            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData());
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
     */
    private ElephantGodWinIconInfo addRewardIcons(List<ElephantGodAwardLineInfo> awardLineInfoList, ElephantGodPlayerGameData gameData) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }

        ElephantGodWinIconInfo iconInfo = new ElephantGodWinIconInfo();

        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
        long oneBetScore = gameData.getOneBetScore();
        awardLineInfoList.forEach(info -> {
            indexSet.addAll(info.getSameIconSet());
            winIconSet.add(info.getSameIcon());
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        });

        iconInfo.iconIndexes = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        return iconInfo;
    }

    public void sendPoolMessage(PlayerController playerController, ElephantGodGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResElephantGodPoolValue res = new ResElephantGodPoolValue(gameRunInfo.getCode());
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
