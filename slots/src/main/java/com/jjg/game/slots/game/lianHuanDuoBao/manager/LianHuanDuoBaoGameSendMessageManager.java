package com.jjg.game.slots.game.lianHuanDuoBao.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.lianHuanDuoBao.data.*;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoCascade;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoChestRewardPb;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoPoolInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.bean.LianHuanDuoBaoWinIconInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.res.ResLianHuanDuoBaoBonusResult;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.res.ResLianHuanDuoBaoEnterGame;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.res.ResLianHuanDuoBaoPoolValue;
import com.jjg.game.slots.game.lianHuanDuoBao.pb.res.ResLianHuanDuoBaoStartGame;
import com.jjg.game.slots.logger.SlotsLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
//@Component
public class LianHuanDuoBaoGameSendMessageManager extends BaseSendMessageManager {

    private final LianHuanDuoBaoGameManager gameManager;
    private final SlotsLogger slotsLogger;

    public LianHuanDuoBaoGameSendMessageManager(LianHuanDuoBaoGameManager gameManager, SlotsLogger slotsLogger) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
    }

    /**
     * 玩家进入游戏 - 返回基础配置 + 玩家进度状态
     */
    public void reqEnterGame(PlayerController playerController, LianHuanDuoBaoGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResLianHuanDuoBaoEnterGame res = new ResLianHuanDuoBaoEnterGame(Code.SUCCESS);
        if (!gameRunInfo.success()) {
            res.code = gameRunInfo.getCode();
            sendInfo.addPlayerMsg(playerController.playerId(), res);
            sendInfo.getLogMessage().add(res);
            sendRun(playerController, sendInfo, "返回配置结果", false);
            return;
        }
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        if (config != null) {
            res.stakeList = gameManager.stakeList(gameRunInfo.getData());
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            LianHuanDuoBaoPlayerGameData data = gameRunInfo.getData();
            res.status = data.getStatus();
            res.curLayer = data.getLayerNumber();
            res.collectedKeyNum = data.getCollectedKeyNum();
            res.dragonBallCount = data.getDragonBallCount();
            res.treasureBowlAmount = data.getTreasureBowlAmount();
            res.poolList = new ArrayList<>();
            if (baseInitCfg != null && baseInitCfg.getPrizePoolIdList() != null) {
                for (int poolId : baseInitCfg.getPrizePoolIdList()) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    LianHuanDuoBaoPoolInfo poolInfo = new LianHuanDuoBaoPoolInfo();
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
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    /**
     * 旋转结果返回
     */
    public void reqStartGame(PlayerController playerController, LianHuanDuoBaoGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResLianHuanDuoBaoStartGame res = new ResLianHuanDuoBaoStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.allGold = gameRunInfo.getAfterGold();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.status = gameRunInfo.getStatus();
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            res.bigWinShow = gameRunInfo.getBigShowId();
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.collectedKeyNum = gameRunInfo.getCollectedKeyNum();
            res.totalKeyNum = gameRunInfo.getTotalKeyNum();
            res.layerNumber = gameRunInfo.getLayerNumber();
            res.nextLayerNumber = gameRunInfo.getNextLayerNumber();
            res.dragonBallCount = gameRunInfo.getDragonBallCount();
            res.treasureBowlDelta = gameRunInfo.getTreasureBowlDelta();
            res.treasureBowlAmount = gameRunInfo.getTreasureBowlAmount();

            LianHuanDuoBaoResultLib lib = (LianHuanDuoBaoResultLib) gameRunInfo.getResultLib();
            if (lib != null) {
                res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData());
                res.addIconInfoList = addIconInfos(lib, gameRunInfo);
                res.chestRewards = mapChestRewards(gameRunInfo.getChestRewards());
            }
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    /**
     * bonus 小游戏结算返回
     */
    public void reqBonusResult(PlayerController playerController, LianHuanDuoBaoGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResLianHuanDuoBaoBonusResult res = new ResLianHuanDuoBaoBonusResult(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.ballWins = gameRunInfo.getBonusBallWins();
            res.totalWin = gameRunInfo.getBonusTotalWin();
            res.allGold = gameRunInfo.getAfterGold();
            res.layerNumber = gameRunInfo.getLayerNumber();
            res.treasureBowlAmount = gameRunInfo.getTreasureBowlAmount();
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回 bonus 结果", false);
    }

    public void sendPoolMessage(PlayerController playerController, LianHuanDuoBaoGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResLianHuanDuoBaoPoolValue res = new ResLianHuanDuoBaoPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }

    private LianHuanDuoBaoWinIconInfo addRewardIcons(List<LianHuanDuoBaoAwardLineInfo> awardLineInfoList, LianHuanDuoBaoPlayerGameData data) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }
        LianHuanDuoBaoWinIconInfo iconInfo = new LianHuanDuoBaoWinIconInfo();
        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
        long oneBetScore = data.getOneBetScore();
        awardLineInfoList.forEach(info -> {
            if (info.getSameIconSet() != null) {
                indexSet.addAll(info.getSameIconSet());
            }
            winIconSet.add(info.getSameIcon());
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        });
        iconInfo.iconIndexes = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        return iconInfo;
    }

    private List<LianHuanDuoBaoCascade> addIconInfos(LianHuanDuoBaoResultLib lib, LianHuanDuoBaoGameRunInfo gameRunInfo) {
        if (lib == null || CollectionUtil.isEmpty(lib.getAddIconInfos())) {
            return null;
        }
        List<LianHuanDuoBaoCascade> list = new ArrayList<>();
        for (LianHuanDuoBaoAddIconInfo addIconInfo : lib.getAddIconInfos()) {
            LianHuanDuoBaoCascade cascade = new LianHuanDuoBaoCascade();
            List<KVInfo> kvList = new ArrayList<>();
            if (addIconInfo.getAddIconMap() != null) {
                addIconInfo.getAddIconMap().forEach((k, v) -> {
                    KVInfo kv = new KVInfo();
                    kv.key = k;
                    kv.value = v;
                    kvList.add(kv);
                });
            }
            cascade.rewardIconInfo = addRewardIcons(addIconInfo.getAwardLineInfoList(), gameRunInfo.getData());
            cascade.addIconInfos = kvList;
            list.add(cascade);
        }
        return list;
    }

    private List<LianHuanDuoBaoChestRewardPb> mapChestRewards(List<LianHuanDuoBaoChestReward> rewards) {
        if (CollectionUtil.isEmpty(rewards)) {
            return null;
        }
        List<LianHuanDuoBaoChestRewardPb> result = new ArrayList<>(rewards.size());
        for (LianHuanDuoBaoChestReward r : rewards) {
            LianHuanDuoBaoChestRewardPb pb = new LianHuanDuoBaoChestRewardPb();
            pb.chestIndex = r.getChestIndex();
            pb.rewardTimes = r.getRewardTimes();
            pb.dropDragonBall = r.isDropDragonBall();
            result.add(pb);
        }
        return result;
    }
}
