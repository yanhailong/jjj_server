package com.jjg.game.slots.game.superGolf.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.superGolf.data.SuperGolfAddIconInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfAwardLineInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfGameRunInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfResultLib;
import com.jjg.game.slots.game.superGolf.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class SuperGolfSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private SuperGolfGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    public void sendConfigMessage(PlayerController playerController, SuperGolfGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        SendInfo sendInfo = new SendInfo();
        ResSuperGolfEnterGame res = new ResSuperGolfEnterGame(Code.SUCCESS);
        if (config != null) {
            res.stakeList = gameManager.stakeList(gameRunInfo.getData());
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();
            res.freeMultiplierAccum = gameRunInfo.getData().getFreeMultiplierAccum();
        } else {
            res.code = Code.NOT_FOUND;
            log.debug("未找到游戏配置 playerId={},roomCfgId={}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    public void sendStartGameMessage(PlayerController playerController, SuperGolfGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResSuperGolfStartGame res = new ResSuperGolfStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.allGold = gameRunInfo.getAfterGold();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
            res.status = gameRunInfo.getStatus();
            //6x6 = 36 格，下标 1-36
            res.iconList = IntStream.range(1, 37).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            res.bigWinShow = gameRunInfo.getBigShowId();
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            SuperGolfResultLib lib = (SuperGolfResultLib) gameRunInfo.getResultLib();
            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);
            res.multiplier = lib.getMultiplier();
            res.freeMultiplierAccum = gameRunInfo.getData().getFreeMultiplierAccum();

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误 playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    private SuperGolfIconInfo addRewardIcons(List<SuperGolfAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }
        SuperGolfIconInfo iconInfo = new SuperGolfIconInfo();
        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
        Set<Integer> turnToMysteryIndexes = new HashSet<>();

        for (SuperGolfAwardLineInfo info : awardLineInfoList) {
            indexSet.addAll(info.getSameIconSet());
            winIconSet.add(info.getSameIcon());
            if (info.getBoxedToMysteryIndexes() != null) {
                turnToMysteryIndexes.addAll(info.getBoxedToMysteryIndexes());
            }
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        }
        iconInfo.iconIndexs = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        iconInfo.turnToMysteryIndexes = new ArrayList<>(turnToMysteryIndexes);
        return iconInfo;
    }

    private List<SuperGolfCascade> addIconInfos(SuperGolfResultLib lib, SuperGolfGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }
        List<SuperGolfCascade> list = new ArrayList<>();
        for (SuperGolfAddIconInfo addIconInfo : lib.getAddIconInfos()) {
            SuperGolfCascade cascade = new SuperGolfCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            if (addIconInfo.getAddIconMap() != null) {
                addIconInfo.getAddIconMap().forEach((k, v) -> {
                    KVInfo kv = new KVInfo();
                    kv.key = k;
                    kv.value = v;
                    addIconInfos.add(kv);
                });
            }
            cascade.rewardIconInfo = addRewardIcons(addIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            cascade.addIconInfos = addIconInfos;
            cascade.turnToMysteryIndexes = addIconInfo.getTurnToMysteryIndexes();
            cascade.mysteryConvertedToIcon = addIconInfo.getMysteryConvertedToIcon();
            cascade.multiplier = addIconInfo.getMultiplier();
            list.add(cascade);
        }
        return list;
    }

    public void sendPoolValue(PlayerController playerController, SuperGolfGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResSuperGolfPoolValue res = new ResSuperGolfPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("奖池请求错误 playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendRun(playerController, sendInfo, "返回奖池值", false);
    }
}
