package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.cleopatra.data.AddColumnConfig;
import com.jjg.game.slots.game.cleopatra.data.CleopatraAddColumnInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.cleopatra.pb.*;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.PoolInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResPoolValue;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.game.mahjiongwin.pb.ResMahjiongwinEnterGame;
import com.jjg.game.slots.game.mahjiongwin.pb.ResMahjiongwinStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.checkerframework.checker.units.qual.A;
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
public class CleopatraSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private CleopatraGameManager gameManager;
    @Autowired
    private CleopatraGenerateManager generateManager;
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

        ResCleopatraEnterGame res = new ResCleopatraEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.poolValue = gameManager.getPoolValueByRoomCfgId(config.getId());
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
    public void sendStartGameMessage(PlayerController playerController, CleopatraGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResCleotapraStartGame res = new ResCleotapraStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //图标信息
            res.iconList = IntStream.range(1, 13).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            CleopatraResultLib lib = (CleopatraResultLib) gameRunInfo.getResultLib();
            res.winIconInfoList = new ArrayList<>();

            if(lib.getWinIcons() != null && !lib.getWinIcons().isEmpty()) {
                lib.getWinIcons().forEach((k,v) -> {
                    CleopatraWinIconInfo winIconInfo = new CleopatraWinIconInfo();
                    winIconInfo.iconId = k;
                    winIconInfo.indexList = new ArrayList<>(v);
                    res.winIconInfoList.add(winIconInfo);
                });
            }
            res.addColumInfoList = addColumInfoList(lib);

            res.rewardPoolValue = gameRunInfo.getSmallPoolGold();
            res.poolValue = gameRunInfo.getCurrentPoolValue();

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo,res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResCleopatraPool res = new ResCleopatraPool(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.poolValue = gameRunInfo.getMini();
        } else {
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
//        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", true);
    }

    private List<CleopatraAddColumInfo> addColumInfoList(CleopatraResultLib lib){
        if(lib == null || lib.getAwardLineInfoList() == null || lib.getAwardLineInfoList().isEmpty()){
            return null;
        }

        List<CleopatraAddColumInfo> list = new ArrayList<>();

        int winCount = 1;
        for(CleopatraAddColumnInfo info : lib.getAwardLineInfoList()){
            CleopatraAddColumInfo addColumInfo = new CleopatraAddColumInfo();
            addColumInfo.icons = Arrays.stream(info.getArr()).boxed().collect(Collectors.toList());

            if(info.getWinIconIndexMap() != null && !info.getWinIconIndexMap().isEmpty()){
                addColumInfo.winIconInfoList = new ArrayList<>();
                info.getWinIconIndexMap().forEach((k,v) -> {
                    CleopatraWinIconInfo winIconInfo = new CleopatraWinIconInfo();
                    winIconInfo.iconId = k;
                    winIconInfo.indexList = new ArrayList<>(v);
                    addColumInfo.winIconInfoList.add(winIconInfo);
                });
            }
            AddColumnConfig addColumnConfig = generateManager.getAddColumnInfoMap().get(winCount);
            addColumInfo.times = addColumnConfig.getTimes();
            winCount++;

            if(info.getPoolIconIndexSet() != null && !info.getPoolIconIndexSet().isEmpty()){
                addColumInfo.poolIconIndexs = new ArrayList<>(info.getPoolIconIndexSet());
            }
            list.add(addColumInfo);
        }
        return list;
    }
}
