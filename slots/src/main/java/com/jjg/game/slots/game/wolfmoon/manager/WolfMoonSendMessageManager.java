package com.jjg.game.slots.game.wolfmoon.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonConfigInfo;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonFreeChooseOne;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonPoolValue;
import com.jjg.game.slots.game.wolfmoon.pb.res.ResWolfMoonStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Component
public class WolfMoonSendMessageManager extends BaseSendMessageManager {

    private final WolfMoonGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    public WolfMoonSendMessageManager(WolfMoonGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * 发送游戏配置
     */
    public void sendConfigMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonConfigInfo res = new ResWolfMoonConfigInfo(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            // 恢复免费游戏状态
            res.status = gameRunInfo.getStatus();
//            res.freeGameType = gameRunInfo.getFreeGameType();
//            res.remainingFreeGames = gameRunInfo.getRemainingFreeGames();
//            res.currentMultiplier = gameRunInfo.getCurrentMultiplier();
//            res.freeGameTriggered = gameRunInfo.isFreeGameTriggered();
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
     */
    public void sendStartGameMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonStartGame res = new ResWolfMoonStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.allGold = gameRunInfo.getBeforeGold();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
//            res.spinInfo = gameRunInfo.getSpinInfo();
//            res.jackpotValue = gameRunInfo.getJackpotValue();
//            res.poolValue = gameRunInfo.getPoolValue();
//            // 状态信息
//            res.status = gameRunInfo.getStatus();
//            res.freeGameType = gameRunInfo.getFreeGameType();
//            res.remainingFreeGames = gameRunInfo.getRemainingFreeGames();
//            res.currentMultiplier = gameRunInfo.getCurrentMultiplier();
//            res.freeGameTriggered = gameRunInfo.isFreeGameTriggered();
//            res.totalFreeWin = gameRunInfo.getTotalFreeWin();
//            res.freeEnd = gameRunInfo.isFreeEnd();

            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    /**
     * 返回奖池结果
     */
    public void sendPoolValue(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonPoolValue res = new ResWolfMoonPoolValue(Code.SUCCESS);
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("奖池结果错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
//        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", true);
    }

    /**
     * 发送免费游戏选择结果
     */
    public void sendFreeChooseOneMessage(PlayerController playerController, WolfMoonGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWolfMoonFreeChooseOne res = new ResWolfMoonFreeChooseOne(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
//            res.freeGameType = gameRunInfo.getFreeGameType();
//            res.remainingFreeGames = gameRunInfo.getRemainingFreeGames();
//            res.currentMultiplier = gameRunInfo.getCurrentMultiplier();
        } else {
            log.debug("选择免费游戏类型错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回免费游戏选择结果", false);
    }
}
