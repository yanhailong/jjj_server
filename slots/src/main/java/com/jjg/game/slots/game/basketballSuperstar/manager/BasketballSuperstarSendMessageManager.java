package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarAwardLineInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarGameRunInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.game.basketballSuperstar.pb.*;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;
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
public class BasketballSuperstarSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private BasketballSuperstarGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private BasketballSuperstarGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, BasketballSuperstarGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResBasketballSuperstarEnterGame res = new ResBasketballSuperstarEnterGame(Code.SUCCESS);
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
            res.remainFreeCount = res.remainFreeCount > 0 ? res.remainFreeCount : 0;
            if (res.remainFreeCount < 1) {
                res.status = SteamAgeConstant.Status.NORMAL;
            }
            if (res.status == BasketballSuperstarConstant.Status.FREE) {
                BasketballSuperstarResultLib freeLib = (BasketballSuperstarResultLib) gameRunInfo.getData().getFreeLib();
                List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = freeLib.getSpecialAuxiliaryInfoList();
                if (specialAuxiliaryInfoList != null && !specialAuxiliaryInfoList.isEmpty()) {
                    List<JSONObject> freeGames = specialAuxiliaryInfoList.get(0).getFreeGames();
                    if (freeGames != null && !freeGames.isEmpty()) {
                        int freeIndex = gameRunInfo.getData().getFreeIndex().get();
                        JSONObject jsonObject = freeGames.get(freeIndex >= 1 ? freeIndex - 1 : 0);
                        BasketballSuperstarResultLib basketballSuperstarResultLib = JSON.parseObject(jsonObject.toJSONString(), BasketballSuperstarResultLib.class);
                        res.stickyIcon = basketballSuperstarResultLib.getStickyIcon();
                        if (freeIndex >= 1) {
                            res.changeStickyIconSet = basketballSuperstarResultLib.getChangeStickyIconSet();
                            res.addStickyIconSet = basketballSuperstarResultLib.getAddStickyIconSet();
                            res.freeCount = basketballSuperstarResultLib.getFreeCount();
                        } else {
                            res.freeCount = 0;
                            res.changeStickyIconSet = new HashSet<>();
                            res.addStickyIconSet = new HashSet<>();
                        }
                    }
                }
            }

            //奖池信息
            if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    BasketballSuperstarPoolInfo poolInfo = new BasketballSuperstarPoolInfo();
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
    public void sendStartGameMessage(PlayerController playerController, BasketballSuperstarGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResBasketballSuperstarStartGame res = new ResBasketballSuperstarStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //玩家当前金币
            res.allGold = gameRunInfo.getAfterGold();
            //本局获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //免费游戏中累计获得金币
            if (gameRunInfo.getStatus() == FrozenThroneConstant.Status.FREE) {
                res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
                if (gameRunInfo.getRemainFreeCount() <= 0) {
                    res.totalWinGold = gameRunInfo.getFreeModeTotalReward();
                }
            } else {
                res.totalWinGold = 0;
            }
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 25).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount();
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();
            //是否触发 免费转
            res.triggerStatus = gameRunInfo.getRemainFreeCount() > 0 && res.status == SteamAgeConstant.Status.NORMAL ? 1 : 0;
            BasketballSuperstarResultLib lib = (BasketballSuperstarResultLib) gameRunInfo.getResultLib();
            res.changeStickyIconSet = lib.getChangeStickyIconSet();
            res.addStickyIconSet = lib.getAddStickyIconSet();
            res.freeCount = lib.getFreeCount();
            int[] iconArr = lib.getIconArr();
            //如果是免费转 可能需要修改
            if (res.status == BasketballSuperstarConstant.Status.FREE
                    && gameRunInfo.getChangeStickyIconSet() != null
                    && !gameRunInfo.getChangeStickyIconSet().isEmpty()) {
                Set<Integer> changeStickyIconSet = gameRunInfo.getChangeStickyIconSet();
                for (Integer i : changeStickyIconSet) {
                    iconArr[i] = gameRunInfo.getStickyIcon();
                }
            }
            res.rewardIconInfo = addRewardIcons(iconArr, lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore());
            //根据权重选取 变成wild 图标 免费转结束，才取消
            res.stickyIcon = gameRunInfo.getStickyIcon();
            if (res.triggerStatus == 1) {
                List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = lib.getSpecialAuxiliaryInfoList();
                if (specialAuxiliaryInfoList != null && !specialAuxiliaryInfoList.isEmpty()) {
                    List<JSONObject> freeGames = specialAuxiliaryInfoList.get(0).getFreeGames();
                    if (freeGames != null && !freeGames.isEmpty()) {
                        JSONObject jsonObject = freeGames.get(0);
                        BasketballSuperstarResultLib basketballSuperstarResultLib = JSON.parseObject(jsonObject.toJSONString(), BasketballSuperstarResultLib.class);
                        res.stickyIcon = basketballSuperstarResultLib.getStickyIcon();
                    }
                }
            }
            //免费转  图标变成wild  变化的图案， key -> 图标id
            res.changeStickyIconSet = gameRunInfo.getChangeStickyIconSet();

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
    private List<BasketballSuperstarIconInfo> addRewardIcons(int[] arr, List<BasketballSuperstarAwardLineInfo> awardLineInfoList, long oneBetScore) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }
        List<BasketballSuperstarIconInfo> iconInfos = new ArrayList<>();

        awardLineInfoList.forEach(info -> {
            BasketballSuperstarIconInfo iconInfo = new BasketballSuperstarIconInfo();
            List<Integer> iconIndexs = new ArrayList<>();
            iconIndexs.addAll(info.getSameIconSet());
            iconInfo.iconIndexs = iconIndexs;
            iconInfo.winIcons = info.getSameIcon();
            iconInfo.win = info.getBaseTimes() * oneBetScore;
            iconInfos.add(iconInfo);
        });
        return iconInfos;
    }


    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, BasketballSuperstarGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResBasketballSuperstarPoolInfo res = new ResBasketballSuperstarPoolInfo(gameRunInfo.getCode());
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
