package com.jjg.game.slots.game.cleopatra.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraGameDataDao;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraResultLibDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameData;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.TestLibData;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 麻将胡了游戏逻辑处理器
 *
 * @author 11
 * @date 2025/8/1 17:25
 */
@Component
public class CleopatraGameManager extends AbstractSlotsGameManager<CleopatraPlayerGameData, CleopatraResultLib> {
    @Autowired
    private CleopatraResultLibDao libDao;
    @Autowired
    private CleopatraGenerateManager generateManager;
    @Autowired
    private CleopatraGameDataDao gameDataDao;
    @Autowired
    private SlotsPoolDao slotsPoolDao;

    public CleopatraGameManager() {
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
    public DollarExpressGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        DollarExpressGameRunInfo gameRunInfo = new DollarExpressGameRunInfo(Code.SUCCESS, playerController.playerId());
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
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setBeforeGold(player.getGold());
            if (playerController != null) {
                playerController.setPlayer(player);
            }

            gameRunInfo = normal(gameRunInfo, playerGameData, betValue);

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setAfterGold(player.getGold());
            if (playerController != null) {
                playerController.setPlayer(player);
            }

            //添加大奖展示id
            int times = (int) (gameRunInfo.getAllWinGold() / betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
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
    private CleopatraGameRunInfo normal(CleopatraGameRunInfo gameRunInfo, CleopatraPlayerGameData playerGameData, long betValue) {
        CleopatraResultLib resultLib = null;
        PoolCfg poolCfg = null;
        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取一个倍数区间
//            CommonResult<Integer> result = getResultLibSection(playerGameData.getLastModelId(), DollarExpressConstant.SpecialMode.TYPE_TRIGGER_NORMAL_TRAIN);
//            if (!result.success()) {
//                continue;
//            }
            //获取结果库
            CommonResult<CleopatraResultLib> libResult = normalGetLib(playerGameData, betValue);
            if (!libResult.success()) {
                continue;
            }

            CleopatraResultLib tmpLib = libResult.data;
            //检查是否有奖池奖励
            if (tmpLib.getJackpotIds() != null && !tmpLib.getJackpotIds().isEmpty()) {
                //判断中奖概率
                int poolId = tmpLib.getJackpotIds().get(0);
                poolCfg = randWinPool(playerGameData, poolId);
                if (poolCfg == null) { //为空表示不能奖池中奖，重新获取
                    continue;
                }
            } else {
                poolCfg = null;
            }
            resultLib = tmpLib;
            break;
        }

        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.playerId(), this.gameType, betValue);
            return gameRunInfo;
        }
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
            for (int poolId : resultLib.getJackpotIds()) {
                if (poolCfg.getId() == poolId && poolCfg.getTruePool() > 0) {
                    CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.playerId(), this.gameType, poolCfg.getTruePool(), poolCfg.getTruePool(), "SLOTS_REWARD_POOL");
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
            if(splitArr.length > 1){
                for(int i = 1; i < splitArr.length; i++){
                    String[] arr3 = splitArr[i].split(",");

                    int[] tmpArr = new int[baseInitCfg.getRows()];
                    for(int j = 0; j < tmpArr.length; j++){
                        tmpArr[j] = Integer.parseInt(arr3[j]);
                    }
                    addIconList.add(tmpArr);
                }
            }

            TestLibData testLibData = new TestLibData();
            lib = generateManager.checkAward(initArr,lib,addIconList);
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
    public int getGameType() {
        return CoreConst.GameType.CLEOPATRA;
    }

    @Override
    protected void offlineSaveGameDataDto(CleopatraPlayerGameData gameData) {

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
