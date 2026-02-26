package com.jjg.game.slots.game.demonchild.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildGameRunInfo;
import com.jjg.game.slots.game.demonchild.data.DemonChildPlayerGameData;
import com.jjg.game.slots.game.demonchild.data.DemonChildResultLib;
import com.jjg.game.slots.game.demonchild.pb.bean.DemonChildPoolInfo;
import com.jjg.game.slots.game.demonchild.pb.res.ResDemonChildEnterGame;
import com.jjg.game.slots.game.demonchild.pb.res.ResDemonChildPoolValue;
import com.jjg.game.slots.game.demonchild.pb.res.ResDemonChildStartGame;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class DemonChildGameSendMessageManager extends BaseSendMessageManager {

    private final DemonChildGameManager gameManager;
    private final SlotsLogger slotsLogger;
    private final DemonChildGameGenerateManager generateManager;

    public DemonChildGameSendMessageManager(DemonChildGameManager gameManager, SlotsLogger slotsLogger, DemonChildGameGenerateManager generateManager) {
        this.gameManager = gameManager;
        this.slotsLogger = slotsLogger;
        this.generateManager = generateManager;
    }

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void reqDemonChildEnterGame(PlayerController playerController, DemonChildGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();
        SendInfo sendInfo = new SendInfo();
        ResDemonChildEnterGame res = new ResDemonChildEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }
            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().getFirst());
            DemonChildPlayerGameData playerGameData = gameRunInfo.getData();
            res.totalWinGold = playerGameData.getFreeAllWin();
            res.status = playerGameData.getStatus();
            res.remainFreeCount = playerGameData.getRemainFreeCount().get();
            res.totalFreeCount = gameRunInfo.getTotalFreeCount();
            res.poolList = new ArrayList<>();
            for (int poolId : prizePoolIdList) {
                PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                if (poolCfg == null) {
                    continue;
                }
                DemonChildPoolInfo poolInfo = new DemonChildPoolInfo();
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
    public void reqDemonChildStartGame(PlayerController playerController, DemonChildGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResDemonChildStartGame res = new ResDemonChildStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = Arrays.stream(gameRunInfo.getIconArr(), 1, gameRunInfo.getIconArr().length).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            res.totalFreeCount = gameRunInfo.getTotalFreeCount();
            DemonChildResultLib lib = (DemonChildResultLib) gameRunInfo.getResultLib();
            res.iconAmountList = buildIconAmount(lib, gameRunInfo.getData());
            res.rewardLineInfo = gameRunInfo.getAwardLineInfos();
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    private List<KVInfo> buildIconAmount(DemonChildResultLib lib, DemonChildPlayerGameData playerGameData) {
        List<KVInfo> kvInfos = new ArrayList<>();
        if (CollectionUtil.isEmpty(lib.getSpecialGirdInfoList())) {
            return kvInfos;
        }
        for (SpecialGirdInfo specialGirdInfo : lib.getSpecialGirdInfoList()) {
            if (CollectionUtil.isEmpty(specialGirdInfo.getValueMap())) {
                continue;
            }
            Map<Integer, Integer> valueMap = specialGirdInfo.getValueMap();
            valueMap.forEach((key, value) -> {
                KVInfo kvInfo = new KVInfo();
                kvInfo.key = key;
                kvInfo.value = value * (int) playerGameData.getOneBetScore();
                kvInfos.add(kvInfo);
            });
        }
        return kvInfos;
    }


    public void sendPoolMessage(PlayerController playerController, DemonChildGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResDemonChildPoolValue res = new ResDemonChildPoolValue(gameRunInfo.getCode());
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
