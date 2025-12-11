package com.jjg.game.slots.game.captainjack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAddIconInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackAwardLineInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.game.captainjack.pb.bean.CaptainJackCascade;
import com.jjg.game.slots.game.captainjack.pb.bean.CaptainJackWinIconInfo;
import com.jjg.game.slots.game.captainjack.pb.res.ResCaptainJackEnterGame;
import com.jjg.game.slots.game.captainjack.pb.res.ResCaptainJackPoolValue;
import com.jjg.game.slots.game.captainjack.pb.res.ResCaptainJackStartGame;
import com.jjg.game.slots.game.captainjack.pb.res.ResCaptainJackTreasureHunting;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class CaptainJackGameSendMessageManager extends BaseSendMessageManager {

    private final CaptainJackGameManager gameManager;
    private final SlotsLogger slotsLogger;
    private final CaptainJackGameGenerateManager generateManager;

    public CaptainJackGameSendMessageManager(CaptainJackGameManager gameManager, SlotsLogger slotsLogger, CaptainJackGameGenerateManager generateManager) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
        this.generateManager = generateManager;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqCaptainJackEnterGame(PlayerController playerController, CaptainJackGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();

        ResCaptainJackEnterGame res = new ResCaptainJackEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().getFirst());
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();
            //计算当前免费倍率
            if (gameRunInfo.getStatus() == CaptainJackConstant.Status.FREE) {
                AtomicInteger freeIndex = gameRunInfo.getData().getFreeIndex();
                if (gameRunInfo.getData().getFreeLib() instanceof CaptainJackResultLib lib) {
                    res.freeMultiplier = (int) generateManager.calFree(lib, freeIndex.get());
                }
            }
            if (gameRunInfo.getStatus() == CaptainJackConstant.Status.TREASURE_CHEST) {
                CaptainJackResultLib treasureResults = gameRunInfo.getData().getResultLib();
                if (treasureResults != null) {
                    res.accumulationRate = treasureResults.getDigTimesMultiplier().subList(0, gameRunInfo.getData().getAlreadyDigCount())
                            .stream()
                            .mapToInt(Integer::intValue)
                            .sum();
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
    public void reqCaptainJackStartGame(PlayerController playerController, CaptainJackGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResCaptainJackStartGame res = new ResCaptainJackStartGame(gameRunInfo.getCode());
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
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, 21).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            CaptainJackResultLib lib = (CaptainJackResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
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
    private CaptainJackWinIconInfo addRewardIcons(List<CaptainJackAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (CollectionUtil.isEmpty(awardLineInfoList)) {
            return null;
        }

        CaptainJackWinIconInfo iconInfo = new CaptainJackWinIconInfo();

        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
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
    private List<CaptainJackCascade> addIconInfos(CaptainJackResultLib lib, CaptainJackGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<CaptainJackCascade> list = new ArrayList<>();
        for (CaptainJackAddIconInfo captainJackAddIconInfo : lib.getAddIconInfos()) {
            CaptainJackCascade captainJackCascade = new CaptainJackCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            captainJackAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            captainJackCascade.rewardIconInfo = addRewardIcons(captainJackAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            captainJackCascade.addIconInfos = addIconInfos;

            list.add(captainJackCascade);
        }
        return list;
    }

    public void sendPoolMessage(PlayerController playerController, CaptainJackGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResCaptainJackPoolValue res = new ResCaptainJackPoolValue(gameRunInfo.getCode());
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

    public void sendTreasureHunting(PlayerController playerController, CaptainJackGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResCaptainJackTreasureHunting res = new ResCaptainJackTreasureHunting(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            if (gameRunInfo.getRemainDigCount() == 0) {
                res.allGold = gameRunInfo.getAfterGold();
                res.totalWinGold = gameRunInfo.getAllWinGold();
            }
            res.currentRate = gameRunInfo.getDigTimesMultiplier();
            res.remainDigCount = gameRunInfo.getRemainDigCount();
        } else {
            log.debug("挖宝结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回挖宝结果", false);
    }
}
