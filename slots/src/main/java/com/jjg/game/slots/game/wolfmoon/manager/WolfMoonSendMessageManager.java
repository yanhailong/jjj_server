package com.jjg.game.slots.game.wolfmoon.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAddIconInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAwardLineInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.game.wolfmoon.pb.*;
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
public class WolfMoonSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private WolfMoonGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    public void sendConfigMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();
        ResWolfMoonEnterGame res = new ResWolfMoonEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();
            res.freeMultiple = gameRunInfo.getData().getFreeMultiplyValue();
        } else {
            res.code = Code.NOT_FOUND;
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    public void sendStartGameMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonStartGame res = new ResWolfMoonStartGame(gameRunInfo.getCode());

        if (gameRunInfo.success()) {
            res.allGold = gameRunInfo.getAfterGold();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
            res.status = gameRunInfo.getStatus();
            res.iconList = IntStream.range(1, gameRunInfo.getIconArr().length)
                    .map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            res.bigWinShow = gameRunInfo.getBigShowId();
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.freeMultiple = gameRunInfo.getFreeMultiple();

            WolfMoonResultLib lib = (WolfMoonResultLib) gameRunInfo.getResultLib();
            res.rewardIconInfo = addRewardIcons(lib.getIconArr(), lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    public void sendFreeChooseOneMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonFreeChooseOne res = new ResWolfMoonFreeChooseOne(gameRunInfo.getCode());
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回免费模式三选一结果", false);
    }

    private WolfMoonIconInfo addRewardIcons(int[] arr, List<WolfMoonAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        WolfMoonIconInfo iconInfo = new WolfMoonIconInfo();
        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();

        for (WolfMoonAwardLineInfo info : awardLineInfoList) {
            if (info.getSameIconSet() != null) {
                indexSet.addAll(info.getSameIconSet());
            }
            winIconSet.add(info.getSameIcon());
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        }

        iconInfo.iconIndexs = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        return iconInfo;
    }

    private List<WolfMoonCascade> addIconInfos(WolfMoonResultLib lib, WolfMoonGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<WolfMoonCascade> list = new ArrayList<>();
        for (WolfMoonAddIconInfo addIconInfo : lib.getAddIconInfos()) {
            WolfMoonCascade cascade = new WolfMoonCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();
            addIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            cascade.rewardIconInfo = addRewardIcons(lib.getIconArr(), addIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            cascade.addIconInfos = addIconInfos;
            list.add(cascade);
        }
        return list;
    }
}
