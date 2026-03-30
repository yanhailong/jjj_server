package com.jjg.game.slots.game.hotfootball.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;
import com.jjg.game.slots.game.hotfootball.data.HotFootballAddIconInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballAwardLineInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballGameRunInfo;
import com.jjg.game.slots.game.hotfootball.data.HotFootballResultLib;
import com.jjg.game.slots.game.hotfootball.pb.*;
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
public class HotFootballSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private HotFootballGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private HotFootballGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, HotFootballGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();

        ResHotFootballEnterGame res = new ResHotFootballEnterGame(Code.SUCCESS);
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
                    HotFootballAddTimesInfo info = new HotFootballAddTimesInfo();
                    info.status = (k == HotFootballConstant.SpecialMode.FREE ? 1 : 0);
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
    public void sendStartGameMessage(PlayerController playerController, HotFootballGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResHotFootballStartGame res = new ResHotFootballStartGame(gameRunInfo.getCode());
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
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            HotFootballResultLib lib = (HotFootballResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getIconArr(), lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
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
     *
     * @param awardLineInfoList
     * @param oneBetScore
     * @return
     */
    private HotFootballIconInfo addRewardIcons(int[] arr, List<HotFootballAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        HotFootballIconInfo iconInfo = new HotFootballIconInfo();

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
    private List<HotFootballCascade> addIconInfos(HotFootballResultLib lib, HotFootballGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<HotFootballCascade> list = new ArrayList<>();
        for (HotFootballAddIconInfo hotFootballAddIconInfo : lib.getAddIconInfos()) {
            HotFootballCascade hotFootballCascade = new HotFootballCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();

            hotFootballAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            hotFootballCascade.rewardIconInfo = addRewardIcons(lib.getIconArr(), hotFootballAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            hotFootballCascade.addIconInfos = addIconInfos;

            list.add(hotFootballCascade);
        }
        return list;
    }
}
