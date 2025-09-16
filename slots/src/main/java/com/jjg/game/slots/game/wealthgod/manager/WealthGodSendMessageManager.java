package com.jjg.game.slots.game.wealthgod.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.slots.game.wealthgod.data.WealthGodGameRunInfo;
import com.jjg.game.slots.game.wealthgod.pb.res.ResWealthGodConfigInfo;
import com.jjg.game.slots.game.wealthgod.pb.res.ResWealthGodPoolValue;
import com.jjg.game.slots.game.wealthgod.pb.res.ResWealthGodStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 财神发送消息管理器
 */
@Component
public class WealthGodSendMessageManager extends BaseSendMessageManager {

    private final WealthGodGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;

    public WealthGodSendMessageManager(WealthGodGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * 发送游戏配置
     */
    public void sendConfigMessage(PlayerController playerController, WealthGodGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        SendInfo sendInfo = new SendInfo();
        ResWealthGodConfigInfo res = new ResWealthGodConfigInfo(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().getFirst());
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
    public void sendStartGameMessage(PlayerController playerController, WealthGodGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResWealthGodStartGame res = new ResWealthGodStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            //本次spin数据
            res.spinInfo = gameRunInfo.getSpinInfo();
            //jackpot奖池奖励金额
            res.jackpotValue = gameRunInfo.getJackpotValue();
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
        slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo,res);
    }

    /**
     * 返回奖池结果
     */
    public void sendPoolValue(PlayerController playerController, long poolValue) {
        SendInfo sendInfo = new SendInfo();
        ResWealthGodPoolValue res = new ResWealthGodPoolValue(Code.SUCCESS);
        res.value = poolValue;
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }

}
