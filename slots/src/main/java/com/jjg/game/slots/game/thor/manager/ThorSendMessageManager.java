package com.jjg.game.slots.game.thor.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.dollarexpress.pb.PoolInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResPoolValue;
import com.jjg.game.slots.game.thor.data.ThorGameRunInfo;
import com.jjg.game.slots.game.thor.pb.ResThorFreeChooseOne;
import com.jjg.game.slots.game.thor.pb.ResThorEnterGame;
import com.jjg.game.slots.game.thor.pb.ResThorPoolValue;
import com.jjg.game.slots.game.thor.pb.ResThorStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 11
 * @date 2025/12/1 18:00
 */
@Component
public class ThorSendMessageManager extends BaseSendMessageManager {

    @Autowired
    private ThorGameManager gameManager;
    @Autowired
    private ThorGenerateManager generateManager;
    @Autowired
    private SlotsLogger logger;

    /**
     * 发送配置信息
     * @param playerController
     * @param gameRunInfo
     */
    public void sendConfigMessage(PlayerController playerController, ThorGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResThorEnterGame res = new ResThorEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for(long[] arr : list){
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();

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
    public void sendStartGameMessage(PlayerController playerController, ThorGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResThorStartGame res = new ResThorStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, gameRunInfo.getIconArr().length).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //中奖线信息
            res.winIconInfoList = gameRunInfo.getAwardLineInfos();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.rewardPoolValue = gameRunInfo.getSmallPoolGold();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();

            logger.gameResult(playerController.getPlayer(), gameRunInfo,res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }


    /**
     * 发送二选一结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendFreeChooseOneMessage(PlayerController playerController, ThorGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResThorFreeChooseOne res = new ResThorFreeChooseOne(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
        } else {
            log.debug("二选一错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回二选一结果", false);
    }

    /**
     * 发送奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolMessage(PlayerController playerController, ThorGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResThorPoolValue res = new ResThorPoolValue(gameRunInfo.getCode());
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
