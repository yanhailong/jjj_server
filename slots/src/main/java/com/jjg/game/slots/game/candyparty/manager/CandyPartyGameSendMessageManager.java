package com.jjg.game.slots.game.candyparty.manager;

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
import com.jjg.game.slots.game.candyparty.data.*;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyCascade;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyPoolInfo;
import com.jjg.game.slots.game.candyparty.pb.bean.CandyPartyWinIconInfo;
import com.jjg.game.slots.game.candyparty.pb.res.ResCandyPartyEnterGame;
import com.jjg.game.slots.game.candyparty.pb.res.ResCandyPartyPoolValue;
import com.jjg.game.slots.game.candyparty.pb.res.ResCandyPartyStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class CandyPartyGameSendMessageManager extends BaseSendMessageManager {

    private final CandyPartyGameManager gameManager;
    private final SlotsLogger slotsLogger;

    public CandyPartyGameSendMessageManager(CandyPartyGameManager gameManager, SlotsLogger slotsLogger) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqCandyPartyEnterGame(PlayerController playerController, CandyPartyGameRunInfo gameRunInfo) {

        SendInfo sendInfo = new SendInfo();
        ResCandyPartyEnterGame res = new ResCandyPartyEnterGame(Code.SUCCESS);
        if (!gameRunInfo.success()) {
            res.code = gameRunInfo.getCode();
            sendInfo.addPlayerMsg(playerController.playerId(), res);
            sendInfo.getLogMessage().add(res);
            sendRun(playerController, sendInfo, "返回配置结果", false);
            return;
        }
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            CandyPartyPlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();
            res.curCollectNum = playerGameData.getCollectedIconNum();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();
            res.curLayer = playerGameData.getLayerNumber();
            CandyPartyResultLib freeLib = playerGameData.getFreeLib();
            if (freeLib != null) {
                res.freeMultiple = freeLib.getFreeGameMultiple();
            }
            res.freeAmount = playerGameData.getFreeAllWin();
            res.poolList = new ArrayList<>();
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                CandyPartyPoolInfo poolInfo = new CandyPartyPoolInfo();
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
    public void reqCandyPartyStartGame(PlayerController playerController, CandyPartyGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResCandyPartyStartGame res = new ResCandyPartyStartGame(gameRunInfo.getCode());
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
            res.freeGameMultiple = gameRunInfo.getFreeGameMultiple();
            res.collectedIconNum = gameRunInfo.getCollectedIconNum();
            res.remainIconNum = gameRunInfo.getRemainIconNum();
            res.layerNumber = gameRunInfo.getLayerNumber();
            res.nextLayerNumber = gameRunInfo.getNextLayerNumber();

            CandyPartyResultLib lib = (CandyPartyResultLib) gameRunInfo.getResultLib();

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
    private CandyPartyWinIconInfo addRewardIcons(List<CandyPartyAwardLineInfo> awardLineInfoList, CandyPartyPlayerGameData gameData) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }

        CandyPartyWinIconInfo iconInfo = new CandyPartyWinIconInfo();

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
    private List<CandyPartyCascade> addIconInfos(CandyPartyResultLib lib, CandyPartyGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<CandyPartyCascade> list = new ArrayList<>();
        for (CandyPartyAddIconInfo candyPartyAddIconInfo : lib.getAddIconInfos()) {
            CandyPartyCascade candyPartyCascade = new CandyPartyCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            candyPartyAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            candyPartyCascade.rewardIconInfo = addRewardIcons(candyPartyAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData());
            candyPartyCascade.addIconInfos = addIconInfos;

            list.add(candyPartyCascade);
        }
        return list;
    }

    public void sendCandyPartyPoolMessage(PlayerController playerController, CandyPartyGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResCandyPartyPoolValue res = new ResCandyPartyPoolValue(gameRunInfo.getCode());
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
