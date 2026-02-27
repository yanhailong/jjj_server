package com.jjg.game.slots.game.acedj.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.acedj.data.AceDjAddIconInfo;
import com.jjg.game.slots.game.acedj.data.AceDjAwardLineInfo;
import com.jjg.game.slots.game.acedj.data.AceDjGameRunInfo;
import com.jjg.game.slots.game.acedj.data.AceDjResultLib;
import com.jjg.game.slots.game.acedj.pb.*;
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
 * @author lihaocao
 * @date 2025/12/2 17:40
 */
@Component
public class AceDjSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private AceDjGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private AceDjGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, AceDjGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResAceDjEnterGame res = new ResAceDjEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameRunInfo.getData() != null && gameRunInfo.getData().getAllBetScore() > 0 ? gameRunInfo.getData().getAllBetScore() : gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();

            //连续中奖倍数信息
//            if (generateManager.getAddTimesMap() != null && !generateManager.getAddTimesMap().isEmpty()) {
//                res.timesInfoList = new ArrayList<>(generateManager.getAddTimesMap().size());
//                generateManager.getAddTimesMap().forEach((k, v) -> {
//                    AceDjAddTimesInfo info = new AceDjAddTimesInfo();
//                    info.status = (k == AceDjConstant.SpecialMode.FREE ? 1 : 0);
//                    info.times = new ArrayList<>(v.size());
//
//                    v.forEach((k1, v1) -> {
//                        KVInfo kv = new KVInfo();
//                        kv.key = k1;
//                        kv.value = v1;
//                        info.times.add(kv);
//                    });
//
//                    res.timesInfoList.add(info);
//                });
//
//                //奖池信息
//                if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
//                    res.poolList = new ArrayList<>();
//                    for (int poolId : prizePoolIdList) {
//                        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
//                        if (poolCfg == null) {
//                            continue;
//                        }
//                        AceDjPoolInfo poolInfo = new AceDjPoolInfo();
//                        poolInfo.id = poolId;
//                        poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
//                        poolInfo.maxTimes = poolCfg.getFakePoolMax();
//                        poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
//                        poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
//                        res.poolList.add(poolInfo);
//                    }
//                }
//            }
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
    public void sendStartGameMessage(PlayerController playerController, AceDjGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResAceDjStartGame res = new ResAceDjStartGame(gameRunInfo.getCode());
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
            res.iconList = IntStream.range(1, 31).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            AceDjResultLib lib = (AceDjResultLib) gameRunInfo.getResultLib();

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
    private AceDjIconInfo addRewardIcons(int[] arr, List<AceDjAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        AceDjIconInfo iconInfo = new AceDjIconInfo();

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
    private List<AceDjCascade> addIconInfos(AceDjResultLib lib, AceDjGameRunInfo gameRunInfo) {
        if (gameRunInfo.getStatus() == 1) {
            log.info("免费转 {}", gameRunInfo.getIconArr());
        }
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }
        List<AceDjCascade> list = new ArrayList<>();
        for (AceDjAddIconInfo aceDjAddIconInfo : lib.getAddIconInfos()) {
            AceDjCascade aceDjCascade = new AceDjCascade();

            List<KVInfo> addIconInfos = new ArrayList<>();
            aceDjAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });

            List<KVInfo> winTimes = new ArrayList<>();
            aceDjAddIconInfo.getWinTimes().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                winTimes.add(kv);
            });

            List<KVInfo> wildTimes = new ArrayList<>();
            for (int i = 0; i < aceDjAddIconInfo.getWildTimes().size(); i++) {
                KVInfo kv = new KVInfo();
                kv.key = i;
                kv.value = aceDjAddIconInfo.getWildTimes().get(i);
                wildTimes.add(kv);
            }

            aceDjCascade.rewardIconInfo = addRewardIcons(lib.getIconArr(), aceDjAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            aceDjCascade.addIconInfos = addIconInfos;
            aceDjCascade.winTimes = winTimes;
            aceDjCascade.times = wildTimes;

            list.add(aceDjCascade);
        }
        return list;
    }

    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, AceDjGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResAceDjPoolInfo res = new ResAceDjPoolInfo(gameRunInfo.getCode());
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
