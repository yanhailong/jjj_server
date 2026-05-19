package com.jjg.game.slots.game.wolfmoon.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.wolfmoon.data.*;
import com.jjg.game.slots.game.wolfmoon.pb.bean.WolfMoonCascade;
import com.jjg.game.slots.game.wolfmoon.pb.bean.WolfMoonPoolInfo;
import com.jjg.game.slots.game.wolfmoon.pb.bean.WolfMoonWinIconInfo;
import com.jjg.game.slots.game.wolfmoon.pb.req.ReqWolfMoonFreeChooseOne;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonConfigInfo;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonFreeChooseOne;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonPoolValue;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Component
public class WolfMoonSendMessageManager extends BaseSendMessageManager {

    private final WolfMoonGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    public WolfMoonSendMessageManager(WolfMoonGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * 发送游戏配置
     */
    public void reqWolfMoonConfigInfo(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {


        ResWolfMoonConfigInfo res = new ResWolfMoonConfigInfo(Code.SUCCESS);
        SendInfo sendInfo = new SendInfo();
        if (!gameRunInfo.success()) {
            res.code = Code.NOT_FOUND;
            sendInfo.addPlayerMsg(playerController.playerId(), res);
            sendInfo.getLogMessage().add(res);
            sendRun(playerController, sendInfo, "返回配置结果", false);
            return;
        }
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        if (config != null) {
            res.stakeList = gameManager.stakeList(gameRunInfo.getData());
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            WolfMoonPlayerGameData playerGameData = gameRunInfo.getData();
            if (playerGameData != null) {
                res.status = playerGameData.getStatus();
                res.freeAmount = playerGameData.getFreeAllWin();
                res.currentMultiplier = getCurrentMultiplier(playerGameData);
                res.remainingFreeGames = playerGameData.getRemainFreeCount().get();
                res.freeGameType = playerGameData.getFreeGameType();
            }
            res.poolList = new ArrayList<>();
            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
            if (baseInitCfg != null && CollectionUtil.isNotEmpty(baseInitCfg.getPrizePoolIdList())) {
                List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    WolfMoonPoolInfo poolInfo = new WolfMoonPoolInfo();
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

    private int getCurrentMultiplier(WolfMoonPlayerGameData playerGameData) {
        SlotsResultLib<?> freeLib = playerGameData.getFreeLib();
        if (freeLib != null && CollectionUtil.isNotEmpty(freeLib.getSpecialAuxiliaryInfoList())) {
            for (SpecialAuxiliaryInfo auxiliaryInfo : freeLib.getSpecialAuxiliaryInfoList()) {
                if (CollectionUtil.isEmpty(auxiliaryInfo.getFreeGames())) {
                    continue;
                }
                JSONObject jsonObject = auxiliaryInfo.getFreeGames().get(playerGameData.getFreeIndex().get());
                WolfMoonResultLib resultLib = jsonObject.toJavaObject(WolfMoonResultLib.class);
                return resultLib.getBaseMultiple();
            }
        }
        return 0;
    }

    /**
     * 发送游戏结果
     */
    public void sendWolfMoonStartGame(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonStartGame res = new ResWolfMoonStartGame(gameRunInfo.getCode());
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
            res.currentMultiplier = gameRunInfo.getCurrentMultiplier();
            WolfMoonResultLib lib = (WolfMoonResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);
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
    private WolfMoonWinIconInfo addRewardIcons(List<WolfMoonAwardLineInfo> awardLineInfoList, WolfMoonPlayerGameData gameData) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }

        WolfMoonWinIconInfo iconInfo = new WolfMoonWinIconInfo();

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

    /**
     * 添加消除图标后，补齐的图标信息
     */
    private List<WolfMoonCascade> addIconInfos(WolfMoonResultLib lib, WolfMoonGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<WolfMoonCascade> list = new ArrayList<>();
        for (WolfMoonAddIconInfo wolfMoonAddIconInfo : lib.getAddIconInfos()) {
            WolfMoonCascade wolfMoonCascade = new WolfMoonCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            wolfMoonAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            wolfMoonCascade.rewardIconInfo = addRewardIcons(wolfMoonAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData());
            wolfMoonCascade.addIconInfos = addIconInfos;

            list.add(wolfMoonCascade);
        }
        return list;
    }

    /**
     * 返回奖池结果
     */
    public void sendWolfMoonPoolValue(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonPoolValue res = new ResWolfMoonPoolValue(Code.SUCCESS);
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            res.code = gameRunInfo.getCode();
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendRun(playerController, sendInfo, "返回奖池结果", true);
    }

    /**
     * 发送免费游戏选择结果
     */
    public void sendFreeChooseOneMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo, ReqWolfMoonFreeChooseOne req) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonFreeChooseOne res = new ResWolfMoonFreeChooseOne(gameRunInfo.getCode());
        WolfMoonPlayerGameData playerGameData = gameRunInfo.getData();
        if (gameRunInfo.success() && playerGameData != null) {
            res.freeGameType = playerGameData.getFreeGameType();
            res.remainingFreeGames = playerGameData.getRemainFreeCount().get();
            res.currentMultiplier = getCurrentMultiplier(playerGameData);
        } else {
            log.debug("选择免费游戏类型错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回免费游戏选择结果", false);
    }
}
