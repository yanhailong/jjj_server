package com.jjg.game.slots.game.findgoldcity.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.findgoldcity.constant.FindGoldCityConstant;
import com.jjg.game.slots.game.findgoldcity.dao.FindGoldCityResultLibDao;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityGameRunInfo;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityPlayerGameData;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
public abstract class AbstractFindGoldCityGameManager extends AbstractSlotsGameManager<FindGoldCityPlayerGameData, FindGoldCityResultLib, FindGoldCityGameRunInfo> {
    private final FindGoldCityGameGenerateManager gameGenerateManager;
    private final FindGoldCityResultLibDao FindGoldCityResultLibDao;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;

    public AbstractFindGoldCityGameManager(FindGoldCityGameGenerateManager gameGenerateManager, FindGoldCityResultLibDao FindGoldCityResultLibDao) {
        super(FindGoldCityPlayerGameData.class, FindGoldCityResultLib.class, FindGoldCityGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.FindGoldCityResultLibDao = FindGoldCityResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动寻找黄金城游戏管理器...");
        super.init();
    }

    @Override
    public FindGoldCityGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        FindGoldCityPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 getPlayerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new FindGoldCityGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        resetFreeStateIfInvalid(playerGameData, FindGoldCityConstant.Status.FREE, FindGoldCityConstant.Status.NORMAL, "寻找黄金城");
        FindGoldCityGameRunInfo gameRunInfo = new FindGoldCityGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     */
    protected void free(FindGoldCityGameRunInfo gameRunInfo, FindGoldCityPlayerGameData playerGameData) {
        CommonResult<FindGoldCityResultLib> libResult = freeGetLib(playerGameData, FindGoldCityConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        FindGoldCityResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        if (afterCount == 0) {
            playerGameData.setStatus(FindGoldCityConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 getPlayerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
    }

    /**
     * 开始游戏
     *
     */
    @Override
    public FindGoldCityGameRunInfo startGame(PlayerController playerController, FindGoldCityPlayerGameData playerGameData, long betValue, boolean auto) {
        FindGoldCityGameRunInfo gameRunInfo = new FindGoldCityGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == FindGoldCityConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == FindGoldCityConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 getPlayerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.getPlayerId());
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
        } catch (
                Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    /**
     * 普通正常流程
     *
     */
    @Override
    protected FindGoldCityGameRunInfo normal(FindGoldCityGameRunInfo gameRunInfo, FindGoldCityPlayerGameData playerGameData, long betValue, FindGoldCityResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(FindGoldCityConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(FindGoldCityConstant.Status.FREE);
            playerGameData.setFreeLib(resultLib);
            playerGameData.getRemainFreeCount().set(resultLib.getAddFreeCount());
            log.debug("触发免费模式  getPlayerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get(), resultLib.getTimes());
        }
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());
        log.debug("id = {}", resultLib.getId());
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(playerGameData.getStatus());
        return gameRunInfo;

    }

    /**
     * 获取奖池信息
     *
     * @param playerController
     * @param stake
     * @param
     * @return
     */
    @Override
    public FindGoldCityGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        FindGoldCityGameRunInfo gameRunInfo = new FindGoldCityGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMajor(getPoolValueByRoomCfgId(playerController.getPlayer().getRoomCfgId()));
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.FIND_GOLD_CITY;
    }

    @Override
    public void generate(Map<Integer, Integer> libTypeCountMap, boolean saveToDB) {
        Integer jackpotCount = libTypeCountMap.getOrDefault(FindGoldCityConstant.SpecialMode.JACKPOT, 0);
        libTypeCountMap.put(FindGoldCityConstant.SpecialMode.JACKPOT, jackpotCount / 10);
        super.generate(libTypeCountMap, saveToDB);
    }

    @Override
    protected FindGoldCityResultLibDao getResultLibDao() {
        return this.FindGoldCityResultLibDao;
    }

    @Override
    protected FindGoldCityGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭寻找黄金城游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
