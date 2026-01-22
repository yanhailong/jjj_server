package com.jjg.game.slots.game.wealthgod.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.TestLibData;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;
import com.jjg.game.slots.game.wealthgod.dao.WealthGodGameDataDao;
import com.jjg.game.slots.game.wealthgod.dao.WealthGodResultLibDao;
import com.jjg.game.slots.game.wealthgod.data.*;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodIconChangeInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodResultLineInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractWealthGodGameManager extends AbstractSlotsGameManager<WealthGodPlayerGameData, WealthGodResultLib, WealthGodGameRunInfo> {
    @Autowired
    protected WealthGodGenerateManager generateManager;
    @Autowired
    protected WealthGodResultLibDao wealthGodResultLibDao;
    @Autowired
    protected WealthGodGameDataDao gameDataDao;

    public AbstractWealthGodGameManager() {
        super(WealthGodPlayerGameData.class, WealthGodResultLib.class);
    }

    @Override
    public void init() {
        log.info("启动财神游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.WEALTH_GOD;
    }

    @Override
    protected WealthGodResultLibDao getResultLibDao() {
        return this.wealthGodResultLibDao;
    }

    @Override
    protected WealthGodGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected WealthGodGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<WealthGodPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return WealthGodPlayerGameDataDTO.class;
    }

    /**
     * 开始游戏
     */
    public WealthGodGameRunInfo playerStartGame(PlayerController playerController, long betValue) {
        //获取玩家游戏数据
        WealthGodPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new WealthGodGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, betValue);
    }

    /**
     * 开始游戏
     */
    public WealthGodGameRunInfo startGame(PlayerController playerController, WealthGodPlayerGameData playerGameData, long betValue) {
        WealthGodGameRunInfo gameRunInfo = new WealthGodGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            CommonResult<Pair<WealthGodResultLib, BetDivideInfo>> commonResult = normalGetLib(playerGameData, betValue, WealthGodConstant.SpecialMode.TYPE_NORMAL);
            if (!commonResult.success()) {
                gameRunInfo.setCode(commonResult.code);
                return gameRunInfo;
            }

            WealthGodResultLib resultLib = commonResult.data.getFirst();
            gameRunInfo.setBetDivideInfo(commonResult.data.getSecond());

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            gameRunInfo.setStake(betValue);
            //所有的spin数据
            List<WealthGodSpinInfo> infoList = new ArrayList<>();
            respinAnalysis(resultLib, playerGameData.getOneBetScore(), infoList, betValue, gameRunInfo);
            gameRunInfo.setSpinInfo(infoList);
            //记录奖池id
            gameRunInfo.setJackpotId(resultLib.firstJackpotId());

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            //奖池中奖
            rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getGold());

            gameRunInfo.setResultLib(resultLib);
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 重转数据解析
     */
    public void respinAnalysis(WealthGodResultLib resultLib, long oneBetScore, List<WealthGodSpinInfo> resultList, long betValue, WealthGodGameRunInfo gameRunInfo) {
        WealthGodSpinInfo spinInfo = new WealthGodSpinInfo();
        //记录中奖信息
        List<WealthGodAwardLineInfo> awardLineInfoList = resultLib.getAwardLineInfoList();
        if (awardLineInfoList != null && !awardLineInfoList.isEmpty()) {
            List<WealthGodResultLineInfo> resultLineInfos = new ArrayList<>();
            long totalTimes = 0;
            for (WealthGodAwardLineInfo lineInfo : awardLineInfoList) {
                WealthGodResultLineInfo resultLineInfo = new WealthGodResultLineInfo();
                resultLineInfo.id = lineInfo.getLineId();
                BaseLineCfg baseLineCfg = getBaseLineCfg(lineInfo.getLineId(), false);
                int direction = baseLineCfg.getDirection().getFirst();
                List<Integer> indexList = baseLineCfg.getPosLocation();
                if (direction == SlotsConst.BaseLine.DIRECTION_LEFT) {
                    resultLineInfo.iconIndex = indexList.subList(0, lineInfo.getSameCount());
                }
                //反向
                else if (direction == SlotsConst.BaseLine.DIRECTION_RIGHT) {
                    resultLineInfo.iconIndex = indexList.subList(indexList.size() - lineInfo.getSameCount(), indexList.size());
                }
                resultLineInfo.direction = direction;
                resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
                resultLineInfo.times = lineInfo.getBaseTimes();
                resultLineInfos.add(resultLineInfo);
                totalTimes += lineInfo.getBaseTimes();
            }
            spinInfo.setResultLineInfoList(resultLineInfos);
            spinInfo.setTimes(totalTimes);
        }
        List<Integer> iconList = Arrays.stream(resultLib.getSource())
                .filter(v -> v != 0)
                .boxed()
                .toList();
        //记录图标信息
        spinInfo.setIconList(iconList);
        //图标变化信息
        Map<Integer, Integer> iconChangeMap = resultLib.getIconChangeMap();
        List<WealthGodIconChangeInfo> iconChangeInfoList = new ArrayList<>();
        if (iconChangeMap != null && !iconChangeMap.isEmpty()) {
            iconChangeMap.forEach((index, icon) -> {
                WealthGodIconChangeInfo changeInfo = new WealthGodIconChangeInfo();
                changeInfo.setIndex(index);
                changeInfo.setIcon(icon);
                iconChangeInfoList.add(changeInfo);
            });
            //记录图标变化
            spinInfo.setIconChangeInfoList(iconChangeInfoList);
        }
        //使用本次计算的 不在从结果集中直接获取
        long resultLibTimes = spinInfo.getTimes();
        long allWinGold = oneBetScore * resultLibTimes;
        //添加大奖展示id
        int times = (int) (allWinGold / betValue);
        log.debug("计算出获奖倍数 times = {}", times);
        spinInfo.setBigWinShow(getBigShowIdByTimes(times));
        if (resultLibTimes > 0) {
            gameRunInfo.addBigPoolTimes(resultLibTimes);
        }
        resultList.add(spinInfo);
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfos = resultLib.getSpecialAuxiliaryInfoList();
        if (specialAuxiliaryInfos != null && !specialAuxiliaryInfos.isEmpty()) {
            SpecialAuxiliaryInfo specialAuxiliaryInfo = specialAuxiliaryInfos.getFirst();
            List<JSONObject> freeGames = specialAuxiliaryInfo.getFreeGames();
            if (freeGames != null && !freeGames.isEmpty()) {
                JSONObject gamesFirst = freeGames.getFirst();
                WealthGodResultLib temp = JSONObject.parseObject(gamesFirst.toJSONString(), WealthGodResultLib.class);
                if (temp != null) {
                    respinAnalysis(temp, oneBetScore, resultList, betValue, gameRunInfo);
                }
            }
        }
    }

    /**
     * 计算奖池的金额
     */
    public long calculatePool(int roomConfigId, int poolId, PlayerController playerController) {
        //奖池配置
        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
        if (poolCfg == null) {
            return 0L;
        }
        Number pool = slotsPoolDao.getBigPoolByRoomCfgId(getGameType(), roomConfigId);
        if (pool != null) {
            log.warn("财神计算奖池奖励金额为:[{}]!", pool.longValue());
        }
        Player player = playerController.getPlayer();
        long playerId = playerController.playerId();
        CommonResult<Long> slotsRewardPool = slotsPoolDao.rewardByRatioFromSmallPool(playerId, this.gameType, player.getRoomCfgId(), poolCfg.getTruePool(), AddType.SLOTS_JACKPOT_REWARD);
        if (slotsRewardPool != null) {
            return slotsRewardPool.data;
        }
        return 0L;
    }

    public long getPoolValue(PlayerController playerController) {
        //玩家当前金币
        Player player = playerController.getPlayer();
        //房间配置id
        int roomCfgId = player.getRoomCfgId();
        return getPoolValueByRoomCfgId(roomCfgId);
    }

    /**
     * 添加测试icons
     *
     * @param playerController
     */
    public boolean addTestIconDataIcons(PlayerController playerController, String icons) {
        WealthGodPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return false;
        }

        try {
            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
            int[] initArr = new int[baseInitCfg.getRows() * baseInitCfg.getCols() + 1];

            String[] splitArr = icons.split(";");
            String[] arr2 = splitArr[0].split(",");
            for (int i = 1; i < initArr.length; i++) {
                initArr[i] = Integer.parseInt(arr2[i - 1]);
            }

            Constructor<WealthGodResultLib> constructor = this.libClass.getConstructor();
            WealthGodResultLib lib = constructor.newInstance();
            lib.addLibType(1);
            lib.setId(RandomUtils.getUUid());
            lib.setSource(initArr);
            //替换财神图标
            int[] replaceArr = generateManager.replaceWealthGod(lib, initArr);
            TestLibData testLibData = new TestLibData();
            SlotsResultLib resultLib = getGenerateManager().checkAward(replaceArr, lib);
            testLibData.setData(resultLib);
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试icons成功 playerId = {},icons = {}", playerController.playerId(), icons);
            return true;
        } catch (Exception e) {
            log.error("", e);
        }
        return false;
    }
}
