package com.jjg.game.slots.game.angrybirds.manager;

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
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsGameRunInfo;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsPlayerGameData;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import com.jjg.game.slots.game.angrybirds.pb.bean.AngryBirdsPoolInfo;
import com.jjg.game.slots.game.angrybirds.pb.res.ResAngryBirdsEnterGame;
import com.jjg.game.slots.game.angrybirds.pb.res.ResAngryBirdsPoolValue;
import com.jjg.game.slots.game.angrybirds.pb.res.ResAngryBirdsStartGame;
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
public class AngryBirdsGameSendMessageManager extends BaseSendMessageManager {

    private final AngryBirdsGameManager gameManager;
    private final SlotsLogger slotsLogger;
    private final AngryBirdsGenerateManager generateManager;

    public AngryBirdsGameSendMessageManager(AngryBirdsGameManager gameManager, SlotsLogger slotsLogger, AngryBirdsGenerateManager generateManager) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
        this.generateManager = generateManager;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqAngryBirdsEnterGame(PlayerController playerController, AngryBirdsGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        SendInfo sendInfo = new SendInfo();
        ResAngryBirdsEnterGame res = new ResAngryBirdsEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().getFirst());
            AngryBirdsPlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();
            //计算当前免费倍率
            if (playerGameData.getStatus() == AngryBirdsConstant.Status.FREE) {
                if (playerGameData.getFreeLib() instanceof AngryBirdsResultLib lib) {
                    int freeIndex = playerGameData.getFreeIndex().get();
                    if (CollectionUtil.isNotEmpty(lib.getSpecialAuxiliaryInfoList())) {
                        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                            List<JSONObject> freeGames = specialAuxiliaryInfo.getFreeGames();
                            if (CollectionUtil.isNotEmpty(freeGames)) {
                                if (freeIndex >= 0 && freeIndex < freeGames.size()) {
                                    JSONObject jsonObject = freeGames.get(freeIndex);
                                    AngryBirdsResultLib resultLib = jsonObject.toJavaObject(AngryBirdsResultLib.class);
                                    res.freeMultiplier = resultLib.getFreeMultiplier();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            res.poolList = new ArrayList<>();
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                AngryBirdsPoolInfo poolInfo = new AngryBirdsPoolInfo();
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
    public void reqAngryBirdsStartGame(PlayerController playerController, AngryBirdsGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResAngryBirdsStartGame res = new ResAngryBirdsStartGame(gameRunInfo.getCode());
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
            res.replaceInfo = gameRunInfo.getReplaceInfo();
            AngryBirdsResultLib lib = (AngryBirdsResultLib) gameRunInfo.getResultLib();
            res.freeMultiplier = lib.getFreeMultiplier();
            res.rewardIconInfo = gameRunInfo.getAwardLineInfos();
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }


    public void reqAngryBirdsPoolValue(PlayerController playerController, AngryBirdsGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResAngryBirdsPoolValue res = new ResAngryBirdsPoolValue(gameRunInfo.getCode());
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
