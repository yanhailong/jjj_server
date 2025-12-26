package com.jjg.game.slots.game.steamAge.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;
import com.jjg.game.slots.game.steamAge.data.*;
import com.jjg.game.slots.game.steamAge.pb.*;
import com.jjg.game.slots.logger.SlotsLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author lihaocao
 * @date 2025/12/2 17:40
 */
@Component
public class SteamAgeSendMessageManager extends BaseSendMessageManager {
    @Autowired
    private SteamAgeGameManager gameManager;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private SteamAgeGenerateManager generateManager;

    /**
     * 发送游戏配置
     *
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        BaseRoomCfg config = GameDataManager.getBaseRoomCfg(playerController.getPlayer().getRoomCfgId());

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
        List<Integer> prizePoolIdList = baseInitCfg.getPrizePoolIdList();

        SendInfo sendInfo = new SendInfo();

        ResSteamAgeEnterGame res = new ResSteamAgeEnterGame(Code.SUCCESS);
        if (config != null) {
            List<long[]> list = gameManager.getAllStakeMap().get(playerController.getPlayer().getRoomCfgId());
            res.stakeList = new ArrayList<>(list.size());
            for (long[] arr : list) {
                res.stakeList.add(arr[1]);
            }

            res.defaultBet = gameManager.oneLineToAllStake(config.getDefaultBet().get(0));
            res.totalWinGold = gameRunInfo.getData().getFreeAllWin();
            res.status = gameRunInfo.getData().getStatus();
            res.remainFreeCount = gameRunInfo.getData().getRemainFreeCount().get();
            res.remainFreeCount = res.remainFreeCount > 0 ? res.remainFreeCount : 0;
            if (res.remainFreeCount < 1) {
                res.status = SteamAgeConstant.Status.NORMAL;
            }
            if (res.status == SteamAgeConstant.Status.FREE) {
                SteamAgeResultLib lib = (SteamAgeResultLib) gameRunInfo.getData().getFreeLib();
                res.baseTimes = generateManager.getSteamAgeExpandRollerInfoMap().get(SteamAgeConstant.SpecialMode.FREE).get(lib.getExpandTimes()).getBaseTimes();
            } else {
                res.baseTimes = generateManager.getSteamAgeExpandRollerInfoMap().get(SteamAgeConstant.SpecialMode.NORMAL).get(0).getBaseTimes();
            }

            //奖池信息
            if (prizePoolIdList != null && !prizePoolIdList.isEmpty()) {
                res.poolList = new ArrayList<>();
                for (int poolId : prizePoolIdList) {
                    PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                    if (poolCfg == null) {
                        continue;
                    }
                    SteamAgePoolInfo poolInfo = new SteamAgePoolInfo();
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
    public void sendStartGameMessage(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResSteamAgeStartGame res = new ResSteamAgeStartGame(gameRunInfo.getCode());
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
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //剩余免费次数
            res.remainFreeCount = gameRunInfo.getRemainFreeCount() > 0 ? gameRunInfo.getRemainFreeCount() : 0;
            //大奖展示id
            res.bigWinShow = gameRunInfo.getBigShowId();
            //等级信息
            res.level = playerController.getPlayer().getLevel();
            res.exp = playerController.getPlayer().getExp();

            SteamAgeResultLib lib = (SteamAgeResultLib) gameRunInfo.getResultLib();
            res.rewardIconInfo = addRewardIcons(lib.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore(), 0, res.status, lib.getExpandTimes());
            //连线则触发，添加图标信息（右扩展图标）
            res.addIconInfoList = addIconInfos(lib, gameRunInfo);
            //高亮图标
            res.highlightList = highlight(res.iconList, res.addIconInfoList, lib);
            //是否触发 免费转
            res.triggerStatus = gameRunInfo.getRemainFreeCount() > 0 && res.status == SteamAgeConstant.Status.NORMAL ? 1 : 0;
            slotsLogger.gameResult(playerController.getPlayer(), gameRunInfo, res);
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);

    }

    private List<SteamAgeExpand> addIconInfos(SteamAgeResultLib lib, SteamAgeGameRunInfo gameRunInfo) {
        List<SteamAgeExpand> iconInfos = new ArrayList<>();
        if (lib.getAddIconInfos() != null && !lib.getAddIconInfos().isEmpty()) {
            for (int i = 0; i < lib.getAddIconInfos().size(); i++) {
                SteamAgeExpandIconInfo info = lib.getAddIconInfos().get(i);
                SteamAgeExpand iconInfo = new SteamAgeExpand();
                iconInfo.iconList = info.getAddIconList();
                iconInfo.rewardIconInfo = addRewardIcons(info.getAwardLineInfoList(), gameRunInfo.getData().getOneBetScore(), i + 1, lib.getGameType(), lib.getExpandTimes());
                iconInfos.add(iconInfo);
            }
        }
        return iconInfos;
    }

    /**
     * 高亮展示
     *
     * @param
     * @return
     */
    private List<Integer> highlight(List<Integer> iconList, List<SteamAgeExpand> addIconInfoList, SteamAgeResultLib lib) {
//        int[] iconArr = lib.getIconArr();
        List<Integer> highlightList = new ArrayList<>();
        if (addIconInfoList == null
                || addIconInfoList.isEmpty()
                || !iconList.contains(SteamAgeConstant.BaseElement.ID_ADD)
                || !lib.getLibTypeSet().contains(SteamAgeConstant.SpecialMode.FREE)
                || !lib.getLibTypeSet().contains(SteamAgeConstant.SpecialMode.JACKPOOL)
        ) {
            return highlightList;
        }
        for (int i = 0; i < iconList.size(); i++) {
            if (iconList.get(i) == SteamAgeConstant.BaseElement.ID_WILD
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_SCATTER
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_ADD
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_MINOR
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_MAJOR
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_GRAND
                    || iconList.get(i) == SteamAgeConstant.BaseElement.ID_MINI) {
                highlightList.add(i + 1);
            }
        }
        //扩列添加
        for (int i = 0; i < addIconInfoList.size(); i++) {
            SteamAgeExpand steamAgeExpand = addIconInfoList.get(i);
            if (steamAgeExpand != null) {
                List<Integer> expandIconList = steamAgeExpand.iconList;
                for (int i2 = 0; i2 < expandIconList.size(); i2++) {
                    if (expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_ADD) {
                        highlightList.add((4 * i) + 20 + i2 + 1);
                    }
                    if (expandIconList != null && !expandIconList.isEmpty()) {
                        if (expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_WILD
                                || expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_SCATTER
                                || expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_MINOR
                                || expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_MAJOR
                                || expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_GRAND
                                || expandIconList.get(i2) == SteamAgeConstant.BaseElement.ID_MINI) {
                            highlightList.add((4 * i) + 20 + i2 + 1);
                        }
                    }
                }
            }
        }
        log.info("highlightList:{}", JSONObject.toJSONString(highlightList));
        return highlightList;
    }

    /**
     * 添加中奖图标信息
     *
     * @param awardLineInfoList
     * @param oneBetScore
     * @return
     */
    private SteamAgeIconInfo addRewardIcons(List<SteamAgeAwardLineInfo> awardLineInfoList, long oneBetScore, int num, int status, int expandNum) {
        if (awardLineInfoList == null || awardLineInfoList.isEmpty()) {
            return null;
        }

        SteamAgeIconInfo iconInfo = new SteamAgeIconInfo();

        Set<Integer> indexSet = new HashSet<>();

        Set<Integer> winIconSet = new HashSet<>();

        for (int i = 0; i < awardLineInfoList.size(); i++) {
            SteamAgeAwardLineInfo info = awardLineInfoList.get(i);
            indexSet.addAll(info.getSameIconSet());
            winIconSet.add(info.getSameIcon());
            iconInfo.win += info.getLineTimes() * oneBetScore;
            iconInfo.baseTimes = info.getBaseTimes();
//            //没有中奖 倍数是默认1 过来的 重新赋值
            if (info.getBaseTimes() == 1
                    && (info.getSameIconSet() == null || info.getSameIconSet().isEmpty())) {
                SteamAgeExpandRollerInfo steamAgeExpandRollerInfo = generateManager.getSteamAgeExpandRollerInfoMap().get(status == 1 ? SteamAgeConstant.SpecialMode.FREE : SteamAgeConstant.SpecialMode.NORMAL).get(expandNum + num);
                iconInfo.baseTimes = steamAgeExpandRollerInfo.getBaseTimes();
            }
            log.info("iconInfo.baseTimes:{}", iconInfo.baseTimes);
        }

        //转化下坐标数组
        if (num > 0) {
            int[] arr = getIndexArr(num);
            Set<Integer> indexSet2 = new HashSet<>();
            for (Integer i : indexSet) {
                indexSet2.add(arr[i]);
            }
            iconInfo.iconIndexs = new ArrayList<>(indexSet2);
//            log.info("================>arr={}", JSONObject.toJSONString(arr));
//            log.info("================>indexSet={}", JSONObject.toJSONString(indexSet));
//            log.info("================>indexSet2={}", JSONObject.toJSONString(indexSet2));
        } else {
            iconInfo.iconIndexs = new ArrayList<>(indexSet);
        }
        iconInfo.winIcons = new ArrayList<>(winIconSet);
        return iconInfo;
    }


    /**
     * 根据n次获取数组 坐标
     * 4列 5行
     *
     * @return
     */
    public int[] getIndexArr(int num) {
        List<Integer> arr = new ArrayList<>();
        arr.add(0);
        if (num > 0) {
            for (int i = num; i > 0; i--) {
                for (int i1 = 0; i1 < 4; i1++) {
                    arr.add(20 + ((i - 1) * 4) + i1 + 1);
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            arr.add(i + 1);
        }

        return arr.stream().mapToInt(Integer::intValue).toArray();
    }


    /**
     * 返回奖池结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendPoolValue(PlayerController playerController, SteamAgeGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResSteamAgePoolInfo res = new ResSteamAgePoolInfo(gameRunInfo.getCode());
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
