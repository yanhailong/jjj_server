package com.jjg.game.slots.game.panJinLian.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianAwardLineInfo;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianGameRunInfo;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianResultLib;
import com.jjg.game.slots.game.panJinLian.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 潘金莲消息发送管理器
 *
 * @author lihaocao
 * @date 2025/12/2 17:40
 */
@Component
public class PanJinLianSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private PanJinLianGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private PanJinLianGenerateManager generateManager;

    /**
     * 发送游戏配置
     */
    public void sendConfigMessage(PlayerController playerController, PanJinLianGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();
        ResPanJinLianEnterGame res = new ResPanJinLianEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.getDefaultBetValue(gameRunInfo, config);
            res.totalWinGold = gameRunInfo.getData() == null ? 0 : gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData() == null ? 0 : gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData() == null ? 0 : gameRunInfo.getData().getRemainFreeCount().get();

            // 奖池信息
            if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    PanJinLianPoolInfo poolInfo = new PanJinLianPoolInfo();
                    poolInfo.id = poolId;
                    poolInfo.initTimes = poolCfg.getFakePoolInitTimes();
                    poolInfo.maxTimes = poolCfg.getFakePoolMax();
                    poolInfo.perSomeSec = poolCfg.getGrowthRate().get(0);
                    poolInfo.updateProp = poolCfg.getGrowthRate().get(1);
                    res.poolList.add(poolInfo);
                }
            }
        } else {
            res.code = Code.NOT_FOUND;
            log.debug("未找到游戏配置。playerId={}, roomCfgId={}", playerController.playerId(), playerController.getPlayer().getRoomCfgId());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回配置结果", false);
    }

    /**
     * 发送开始游戏结果
     */
    public void sendStartGameMessage(PlayerController playerController, PanJinLianGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResPanJinLianStartGame res = new ResPanJinLianStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            // 玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            // 本局赢得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            // 图标数据
            res.iconList = IntStream.range(1, 16).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            // 剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            // 免费累计奖励
            res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
            // 下一局状态
            res.status = gameRunInfo.getStatus();
            // 大赢展示ID
            res.bigWinShow = gameRunInfo.getBigShowId();
            // 等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            PanJinLianResultLib lib = (PanJinLianResultLib) gameRunInfo.getResultLib();
            res.rewardIconInfo = addRewardIcons(lib.getIconArr(), lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏失败。playerId={}, code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }

    /**
     * 组装中奖图标信息
     */
    private List<PanJinLianIconInfo> addRewardIcons(int[] arr, List<PanJinLianAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        List<PanJinLianIconInfo> iconInfolist = new ArrayList<>();
        for (PanJinLianAwardLineInfo awardLineInfo : awardLineInfoList) {
            PanJinLianIconInfo iconInfo = new PanJinLianIconInfo();
            List<Integer> indexList = new ArrayList<>(awardLineInfo.getSameIconSet());
            iconInfo.iconIndexs = indexList;
            iconInfo.winIcons = awardLineInfo.getSameIcon();
            iconInfo.linId = awardLineInfo.getLineId();
            iconInfo.win = awardLineInfo.getBaseTimes() * oneBetScore;
            iconInfolist.add(iconInfo);
        }

        return iconInfolist;
    }

    /**
     * 返回奖池结果
     */
    public void sendPoolValue(PlayerController playerController, PanJinLianGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResPanJinLianPoolInfo res = new ResPanJinLianPoolInfo(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            res.mini = gameRunInfo.getMini();
            res.minor = gameRunInfo.getMinor();
            res.major = gameRunInfo.getMajor();
            res.grand = gameRunInfo.getGrand();
        } else {
            log.debug("奖池结果错误。playerId={}, code={}", playerController.playerId(), gameRunInfo.getCode());
        }
        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回奖池结果", false);
    }
}
