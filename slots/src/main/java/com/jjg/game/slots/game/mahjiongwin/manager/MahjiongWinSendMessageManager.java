package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAddIconInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAwardLineInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.game.mahjiongwin.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 11
 * @date 2025/8/1 17:40
 */
@Component
public class MahjiongWinSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private MahjiongWinGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        SendInfo sendInfo = new SendInfo();

        ResMahjiongwinEnterGame res = new ResMahjiongwinEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
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
    public void sendStartGameMessage(PlayerController playerController, MahjiongWinGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResMahjiongwinStartGame res = new ResMahjiongwinStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getData().getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            MahjiongWinResultLib lib;
            if(res.status == MahjiongWinConstant.Status.FREE){
                lib = (MahjiongWinResultLib) gameRunInfo.getCurrentFreeLib();
            }else {
                lib = (MahjiongWinResultLib) gameRunInfo.getResultLib();
            }

            res.rewardIconInfo = addRewardIcons(lib.getIconArr(),lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
        slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo,res);
    }

    /**
     * 添加中奖图标信息
     * @param awardLineInfoList
     * @param oneBetScore
     * @return
     */
    private MahjiongWinIconInfo addRewardIcons(int[] arr,List<MahjiongWinAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        MahjiongWinIconInfo iconInfo = new MahjiongWinIconInfo();

        Set<Integer> indexSet = new HashSet<>();
        awardLineInfoList.forEach(info -> {
            indexSet.addAll(info.getSameIconSet());
            iconInfo.win += info.getBaseTimes() * oneBetScore;
        });

        List<Integer> replaceIconIndex = new ArrayList<>();
        indexSet.forEach(index -> {
            int icon = arr[index];
            if(icon >= MahjiongWinConstant.BaseElement.GOLD_MIN && icon <= MahjiongWinConstant.BaseElement.GOLD_MAX){
                replaceIconIndex.add(index);
            }
        });

        iconInfo.iconIndexs = new ArrayList<>(indexSet);
        iconInfo.replaceWildIndexs = replaceIconIndex;
        return iconInfo;
    }

    /**
     * 添加消除图标后，补齐的图标信息
     * @param lib
     * @param gameRunInfo
     * @return
     */
    private List<MahjiongCascade> addIconInfos(MahjiongWinResultLib lib, MahjiongWinGameRunInfo gameRunInfo) {
        if (lib == null || lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty()) {
            return null;
        }

        List<MahjiongCascade> list = new ArrayList<>();
        for(MahjiongWinAddIconInfo mahjiongWinAddIconInfo : lib.getAddIconInfos()){
            MahjiongCascade mahjiongCascade = new MahjiongCascade();
            List<KVInfo> addIconInfos = new ArrayList<>();

            mahjiongWinAddIconInfo.getAddIconMap().forEach((k,v) -> {
                KVInfo kv = new KVInfo();
                kv.key = k;
                kv.value = v;
                addIconInfos.add(kv);
            });
            mahjiongCascade.rewardIconInfo = addRewardIcons(lib.getIconArr(),mahjiongWinAddIconInfo.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            mahjiongCascade.addIconInfos = addIconInfos;

            list.add(mahjiongCascade);
        }
        return list;
    }
}
