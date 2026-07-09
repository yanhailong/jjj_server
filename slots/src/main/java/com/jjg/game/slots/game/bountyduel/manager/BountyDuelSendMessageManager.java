package com.jjg.game.slots.game.bountyduel.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelAddIconInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelAwardLineInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelGameRunInfo;
import com.jjg.game.slots.game.bountyduel.data.BountyDuelResultLib;
import com.jjg.game.slots.game.bountyduel.pb.BountyDuelAddTimesInfo;
import com.jjg.game.slots.game.bountyduel.pb.BountyDuelCascade;
import com.jjg.game.slots.game.bountyduel.pb.BountyDuelIconInfo;
import com.jjg.game.slots.game.bountyduel.pb.ResBountyDuelEnterGame;
import com.jjg.game.slots.game.bountyduel.pb.ResBountyDuelPoolValue;
import com.jjg.game.slots.game.bountyduel.pb.ResBountyDuelStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BountyDuelSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private BountyDuelGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private BountyDuelGenerateManager generateManager;

    public void sendConfigMessage(PlayerController playerController, BountyDuelGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();
        ResBountyDuelEnterGame res = new ResBountyDuelEnterGame(Code.SUCCESS);
        if (config != null) {
            res.stakeList = gameManager.stakeList(gameRunInfo.getData());

            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();

            if (generateManager.getAddTimesMap() != null && !generateManager.getAddTimesMap().isEmpty()) {
                res.timesInfoList = new ArrayList<>(generateManager.getAddTimesMap().size());
                generateManager.getAddTimesMap().forEach((k, v) -> {
                    BountyDuelAddTimesInfo info = new BountyDuelAddTimesInfo();
                    info.status = (k == BountyDuelConstant.SpecialMode.FREE ? 1 : 0);
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
            log.debug("未找到游戏配置 playerId={}, roomCfgId={}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回赏金大对决配置结果", false);
    }

    public void sendStartGameMessage(PlayerController playerController, BountyDuelGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResBountyDuelStartGame res = new ResBountyDuelStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.allGold = gameRunInfo.getAfterGold();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
            res.status = gameRunInfo.getStatus();
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            res.bigWinShow = gameRunInfo.getBigShowId();
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            BountyDuelResultLib lib = (BountyDuelResultLib) gameRunInfo.getResultLib();

            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误 playerId={}, code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回赏金大对决押注结果", false);
    }

    /**
     * 返回奖池金额
     */
    public void sendPoolValue(PlayerController playerController, BountyDuelGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResBountyDuelPoolValue res = new ResBountyDuelPoolValue(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("赏金大对决奖池查询错误 playerId={}, code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendRun(playerController, sendInfo, "返回赏金大对决奖池金额", false);
    }

    private BountyDuelIconInfo addRewardIcons(List<BountyDuelAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        BountyDuelIconInfo iconInfo = new BountyDuelIconInfo();

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

    private List<BountyDuelCascade> addIconInfos(BountyDuelResultLib lib, BountyDuelGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<BountyDuelCascade> list = new ArrayList<>();
        for (BountyDuelAddIconInfo bountyDuelAddIconInfo : lib.getAddIconInfos()) {
            BountyDuelCascade bountyDuelCascade = new BountyDuelCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();

            bountyDuelAddIconInfo.getAddIconMap().forEach((k, v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            bountyDuelCascade.rewardIconInfo = addRewardIcons(bountyDuelAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            bountyDuelCascade.addIconInfos = addIconInfos;

            list.add(bountyDuelCascade);
        }
        return list;
    }
}
