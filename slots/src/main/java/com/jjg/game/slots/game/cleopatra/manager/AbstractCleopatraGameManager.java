package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.TestLibData;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraGameDataDao;
import com.jjg.game.slots.game.cleopatra.dao.CleopatraResultLibDao;
import com.jjg.game.slots.game.cleopatra.data.CleopatraGameRunInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameData;
import com.jjg.game.slots.game.cleopatra.data.CleopatraPlayerGameDataDTO;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCleopatraGameManager extends AbstractSlotsGameManager<CleopatraPlayerGameData, CleopatraResultLib, CleopatraGameRunInfo> {
    @Autowired
    protected CleopatraResultLibDao libDao;
    @Autowired
    protected CleopatraGenerateManager generateManager;
    @Autowired
    protected CleopatraGameDataDao gameDataDao;

    public AbstractCleopatraGameManager() {
        super(CleopatraPlayerGameData.class, CleopatraResultLib.class, CleopatraGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动埃及艳后游戏管理器...");
        super.init();
        addUpdatePoolEvent();
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
    @Override
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
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData);
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
    @Override
    protected CleopatraGameRunInfo normal(CleopatraGameRunInfo gameRunInfo, CleopatraPlayerGameData playerGameData, long betValue) {
        //获取结果库
        CommonResult<Pair<CleopatraResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, CleopatraConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        CleopatraResultLib resultLib = libResult.data.getFirst();
        if (resultLib == null) {
            log.debug("获取的结果为空 playerId = {},gameType = {},betValue = {}", playerGameData.playerId(), this.gameType, betValue);
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }

        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        PoolCfg poolCfg = null;
        //检查是否有奖池奖励
        if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
            //判断中奖概率
            int poolId = resultLib.firstJackpotId();
            poolCfg = GameDataManager.getPoolCfg(poolId);
        } else {
            poolCfg = null;
        }

        log.debug("id = {}", resultLib.getId());

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
