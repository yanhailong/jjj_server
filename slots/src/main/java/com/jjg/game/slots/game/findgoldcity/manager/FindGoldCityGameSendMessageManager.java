package com.jjg.game.slots.game.findgoldcity.manager;

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
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;
import com.jjg.game.slots.game.findgoldcity.data.*;
import com.jjg.game.slots.game.findgoldcity.pb.bean.FindGoldCityCascade;
import com.jjg.game.slots.game.findgoldcity.pb.bean.FindGoldCityPoolInfo;
import com.jjg.game.slots.game.findgoldcity.pb.bean.FindGoldCityWinIconInfo;
import com.jjg.game.slots.game.findgoldcity.pb.res.ResFindGoldCityEnterGame;
import com.jjg.game.slots.game.findgoldcity.pb.res.ResFindGoldCityPoolValue;
import com.jjg.game.slots.game.findgoldcity.pb.res.ResFindGoldCityStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class FindGoldCityGameSendMessageManager extends BaseSendMessageManager {

    private final AbstractFindGoldCityGameManager gameManager;
    private final SlotsLogger slotsLogger;

    public FindGoldCityGameSendMessageManager(FindGoldCityGameManager gameManager, SlotsLogger slotsLogger) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqFindGoldCityEnterGame(PlayerController playerController, FindGoldCityGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        SendInfo sendInfo = new SendInfo();
        ResFindGoldCityEnterGame res = new ResFindGoldCityEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            FindGoldCityPlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.poolList = new ArrayList<>();
            res.remainFreeTimes = playerGameData.getRemainFreeCount().get();
            if (playerGameData.getStatus() == FindGoldCityConstant.Status.FREE) {
                //获取倍数其他信息
                SlotsResultLib<?> freeLib = playerGameData.getFreeLib();
                if (freeLib instanceof FindGoldCityResultLib lib) {
                    if (CollectionUtil.isNotEmpty(lib.getSpecialAuxiliaryInfoList())) {
                        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                            if (CollectionUtil.isNotEmpty(specialAuxiliaryInfo.getFreeGames())) {
                                JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(playerGameData.getFreeIndex().get());
                                FindGoldCityResultLib resultLib = jsonObject.toJavaObject(FindGoldCityResultLib.class);
                                res.currentMultiple = resultLib.getCurrentMultiple();
                            }
                        }
                    }

                }
            }
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                FindGoldCityPoolInfo poolInfo = new FindGoldCityPoolInfo();
                poolInfo.id = poolId;
                poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                poolInfo.maxTimes = poolCfg.getFakePoolMax();
                poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                res.poolList.add(poolInfo);
            }
        } else {
            res.code = Code.NOT_FOUND;
            log.debug("未找到游戏配置  getPlayerId={},roomCfgId={}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    /**
     * 添加中奖图标信息
     */
    private FindGoldCityWinIconInfo addRewardIcons(List<FindGoldCityAwardLineInfo> awardLineInfoList, FindGoldCityPlayerGameData gameData) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }

        FindGoldCityWinIconInfo iconInfo = new FindGoldCityWinIconInfo();

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
     * 发送游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void reqFindGoldCityStartGame(PlayerController playerController, FindGoldCityGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResFindGoldCityStartGame res = new ResFindGoldCityStartGame(gameRunInfo.getCode());
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
            if (gameRunInfo.getResultLib() instanceof FindGoldCityResultLib lib) {
                res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData());
                res.addIconInfoList = addIconInfos(lib, gameRunInfo);
            }
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  getPlayerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }


    /**
     * 添加消除图标后，补齐的图标信息
     */
    private List<FindGoldCityCascade> addIconInfos(FindGoldCityResultLib lib, FindGoldCityGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<FindGoldCityCascade> list = new ArrayList<>();
        for (FindGoldCityAddIconInfo findGoldCityAddIconInfo : lib.getAddIconInfos()) {
            FindGoldCityCascade findGoldCityCascade = new FindGoldCityCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            findGoldCityAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            Map<Integer, Integer> elementRemainTimesMap = new LinkedHashMap<>();
            if (CollectionUtil.isNotEmpty(findGoldCityAddIconInfo.getAwardLineInfoList())) {
                findGoldCityAddIconInfo.getAwardLineInfoList().forEach(awardLineInfo -> {
                    Map<Integer, Integer> elementRemainTimes = awardLineInfo.getElementRemainTimes();
                    if (CollectionUtil.isNotEmpty(elementRemainTimes)) {
                        elementRemainTimes.forEach(elementRemainTimesMap::putIfAbsent);
                    }
                });
            }
            List<KVInfo> elementRemainTimesList = new ArrayList<>(elementRemainTimesMap.size());
            elementRemainTimesMap.forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                elementRemainTimesList.add(kv);
            });
            findGoldCityCascade.rewardIconInfo = addRewardIcons(findGoldCityAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData());
            findGoldCityCascade.addIconInfos = addIconInfos;
            findGoldCityCascade.remainIconCount = elementRemainTimesList;
            list.add(findGoldCityCascade);
        }
        return list;
    }

    public void reqFindGoldCityPoolValue(PlayerController playerController, FindGoldCityGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResFindGoldCityPoolValue res = new ResFindGoldCityPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.major = gameRunInfo.getMajor();
        } else {
            log.debug("奖池结果错误  getPlayerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }
}
