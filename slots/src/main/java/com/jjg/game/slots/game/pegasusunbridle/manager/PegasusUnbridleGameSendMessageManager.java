package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleGameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridlePlayerGameData;
import com.jjg.game.slots.game.pegasusunbridle.pb.bean.PegasusUnbridlePoolInfo;
import com.jjg.game.slots.game.pegasusunbridle.pb.res.ResPegasusUnbridleEnterGame;
import com.jjg.game.slots.game.pegasusunbridle.pb.res.ResPegasusUnbridleStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class PegasusUnbridleGameSendMessageManager extends BaseSendMessageManager {

    private final AbstractPegasusUnbridleGameManager gameManager;
    private final SlotsLogger slotsLogger;

    public PegasusUnbridleGameSendMessageManager(PegasusUnbridleGameManager gameManager, SlotsLogger slotsLogger) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqPegasusUnbridleEnterGame(PlayerController playerController, PegasusUnbridleGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        SendInfo sendInfo = new SendInfo();
        ResPegasusUnbridleEnterGame res = new ResPegasusUnbridleEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().getFirst());
            PegasusUnbridlePlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();

            res.poolList = new ArrayList<>();
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                PegasusUnbridlePoolInfo poolInfo = new PegasusUnbridlePoolInfo();
                poolInfo.id = poolId;
                poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                poolInfo.maxTimes = poolCfg.getFakePoolMax();
                poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                res.poolList.add(poolInfo);
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
    public void reqPegasusUnbridleStartGame(PlayerController playerController, PegasusUnbridleGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResPegasusUnbridleStartGame res = new ResPegasusUnbridleStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            res.winIconInfoList = gameRunInfo.getAwardLineInfos();
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

}
