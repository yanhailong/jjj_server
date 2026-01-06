package com.jjg.game.slots.game.cleopatra.manager;

import com.alibaba.fastjson.JSON;
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
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.TestLibData;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraGameDataDao;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraResultLibDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameData;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameDataDTO;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameDataDTO;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCleopatraGameManager extends AbstractSlotsGameManager<CleopatraPlayerGameData, CleopatraResultLib> {
    @Autowired
    protected CleopatraResultLibDao libDao;
    @Autowired
    protected CleopatraGenerateManager generateManager;
    @Autowired
    protected CleopatraGameDataDao gameDataDao;

    public AbstractCleopatraGameManager() {
        super(CleopatraPlayerGameData.class, CleopatraResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动埃及艳后游戏管理器...");
        super.init();
        addUpdatePoolEvent();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        addGenerateLibEvent(map);
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public CleopatraGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        CleopatraPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new CleopatraGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 获取奖池
     *
     * @param playerController
     */
    public CleopatraGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        CleopatraGameRunInfo gameRunInfo = new CleopatraGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByRoomCfgId(playerController.getPlayer().getRoomCfgId()));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param auto
     * @return
     */
    public CleopatraGameRunInfo startGame(PlayerController playerController, CleopatraPlayerGameData playerGameData, long betValue, boolean auto) {
        CleopatraGameRunInfo gameRunInfo = new CleopatraGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            gameRunInfo = normal(gameRunInfo, playerGameData, betValue);

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    /**
     * 普通正常流程
     *
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected CleopatraGameRunInfo normal(CleopatraGameRunInfo gameRunInfo, CleopatraPlayerGameData playerGameData, long betValue) {
        CleopatraResultLib resultLib = null;
        PoolCfg poolCfg = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
//            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN);
//            if (!result.success()) {
//                continue;
//            }
            //获取结果库
            CommonResult<Pair<CleopatraResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, CleopatraConstant.SpecialMode.NORMAL);
            if (!libResult.success()) {
                gameRunInfo.setCode(libResult.code);
                return gameRunInfo;
            }

            CleopatraResultLib tmpLib = libResult.data.getFirst();
            gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

            //检查是否有奖池奖励
            if (tmpLib.getJackpotIds() != null && !tmpLib.getJackpotIds().isEmpty()) {
                //判断中奖概率
                int poolId = tmpLib.getJackpotIds().get(0);
//                poolCfg = randWinPool(playerGameData, poolId);
//                if (poolCfg == null) { //为空表示不能奖池中奖，重新获取
//                    log.debug("获取的结果库中奖池，但是配置不允许中奖，所以重试 playerId = {},libId = {}", playerGameData.playerId(), tmpLib.getId());
//                    continue;
//                }
                poolCfg = GameDataManager.getPoolCfg(poolId);
            } else {
                poolCfg = null;
            }
            resultLib = tmpLib;
            break;
        }

        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.playerId(), this.gameType, betValue);
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
            for (int poolId : resultLib.getJackpotIds()) {
                if (poolCfg.getId() == poolId && poolCfg.getTruePool() > 0) {
                    CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), poolCfg.getTruePool(), AddType.SLOTS_JACKPOT_REWARD);
                    if (result.success()) {
                        gameRunInfo.addSmallPoolGold(result.data);

                        gameRunInfo.setCurrentPoolValue(getPoolValueByRoomCfgId(playerGameData.getRoomCfgId()));
                    }
                }
            }
        }

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        return gameRunInfo;
    }

    @Override
    public boolean addTestIconDataIcons(PlayerController playerController, String icons) {
        CleopatraPlayerGameData playerGameData = getPlayerGameData(playerController);
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

            CleopatraResultLib lib = new CleopatraResultLib();
            lib.addLibType(1);
            lib.setId(RandomUtils.getUUid());

            List<int[]> addIconList = new ArrayList<>();
            if (splitArr.length > 1) {
                for (int i = 1; i < splitArr.length; i++) {
                    String[] arr3 = splitArr[i].split(",");

                    int[] tmpArr = new int[baseInitCfg.getRows()];
                    for (int j = 0; j < tmpArr.length; j++) {
                        tmpArr[j] = Integer.parseInt(arr3[j]);
                    }
                    addIconList.add(tmpArr);
                }
            }

            TestLibData testLibData = new TestLibData();
            lib = generateManager.checkAward(initArr, lib, addIconList);
            testLibData.setData(lib);
            playerGameData.addTestIconsData(testLibData);
            log.info("添加测试icons成功 playerId = {},icons = {}", playerController.playerId(), icons);
            return true;
        } catch (Exception e) {
            log.error("", e);
        }
        return false;
    }

    @Override
    protected CleopatraResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected CleopatraGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected CleopatraGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected Class<CleopatraPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return CleopatraPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.CLEOPATRA;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭埃及艳后游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
