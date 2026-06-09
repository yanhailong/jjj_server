package com.jjg.game.slots.game.dracula.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.dracula.DraculaConstant;
import com.jjg.game.slots.game.dracula.data.DraculaAddIconInfo;
import com.jjg.game.slots.game.dracula.data.DraculaAwardLineInfo;
import com.jjg.game.slots.game.dracula.data.DraculaGameRunInfo;
import com.jjg.game.slots.game.dracula.data.DraculaResultLib;
import com.jjg.game.slots.game.dracula.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 11
 * @date 2025/8/1 17:40
 */
@Component
public class DraculaSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private DraculaGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private DraculaGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, DraculaGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();

        ResDraculaEnterGame res = new ResDraculaEnterGame(Code.SUCCESS);
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

            //连续中奖倍数信息
            if (generateManager.getAddTimesMap() != null && !generateManager.getAddTimesMap().isEmpty()) {
                res.timesInfoList = new ArrayList<>(generateManager.getAddTimesMap().size());
                generateManager.getAddTimesMap().forEach((k, v) -> {
                    DraculaAddTimesInfo info = new DraculaAddTimesInfo();
                    info.status = (k == DraculaConstant.SpecialMode.FREE ? 1 : 0);
                    info.times = new ArrayList<>(v.size());

                    v.forEach((k1, v1) -> {
                        KVInfo kv = new KVInfo();
                        kv.key = k1;
                        kv.value = v1;
                        info.times.add(kv);
                    });

                    res.timesInfoList.add(info);
                });
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
    public void sendStartGameMessage(PlayerController playerController, DraculaGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResDraculaStartGame res = new ResDraculaStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //免费游戏中累计获得金币
            res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 37).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            DraculaResultLib lib = (DraculaResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getIconArr(), lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);

            //免费模式能量值/守门员机制（文档 [37-43]）—— 字段在 NORMAL 局上默认为 0
            res.multiplier = lib.getMultiplier();
            res.energyAfter = lib.getEnergyAfter();
            res.maxEnergyAfter = lib.getMaxEnergyAfter();
            //本局触发的额外免费次数（+1 符号），用于客户端 +N 动画
            res.addFreeCount = lib.getAddFreeCount();

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
    private DraculaIconInfo addRewardIcons(int[] arr, List<DraculaAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        DraculaIconInfo iconInfo = new DraculaIconInfo();

        Set<Integer> indexSet = new HashSet<>();
        Set<Integer> winIconSet = new HashSet<>();
        Set<Integer> replaceWildIndexs = new HashSet<>();

        awardLineInfoList.forEach(info -> {
            indexSet.addAll(info.getSameIconSet());
            winIconSet.add(info.getSameIcon());
            if (info.getReplaceWildIndexs() != null && !info.getReplaceWildIndexs().isEmpty()) {
                replaceWildIndexs.addAll(info.getReplaceWildIndexs());
            }
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        });

        iconInfo.iconIndexs = new ArrayList<>(indexSet);
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        iconInfo.replaceWildIndexs = new ArrayList<>(replaceWildIndexs);
        return iconInfo;
    }

    /**
     * 添加消除图标后，补齐的图标信息
     *
     * @param lib
     * @param gameRunInfo
     * @return
     */
    private List<DraculaCascade> addIconInfos(DraculaResultLib lib, DraculaGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<DraculaCascade> list = new ArrayList<>();
        for (DraculaAddIconInfo DraculaAddIconInfo : lib.getAddIconInfos()) {
            DraculaCascade DraculaCascade = new DraculaCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();

            DraculaAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            DraculaCascade.rewardIconInfo = addRewardIcons(lib.getIconArr(), DraculaAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            DraculaCascade.addIconInfos = addIconInfos;

            list.add(DraculaCascade);
        }
        return list;
    }
    /**
     * Return pool value.
     *
     * @param playerController player
     * @param gameRunInfo game run info
     */
    public void sendPoolValue(PlayerController playerController, DraculaGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResDraculaPoolValue res = new ResDraculaPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("dracula pool value error playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendRun(playerController, sendInfo, "return dracula pool value", false);
    }
}
