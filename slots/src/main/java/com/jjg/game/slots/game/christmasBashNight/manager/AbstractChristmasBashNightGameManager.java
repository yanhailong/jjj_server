package com.jjg.game.slots.game.christmasBashNight.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.christmasBashNight.ChristmasBashNightConstant;
import com.jjg.game.slots.game.christmasBashNight.dao.ChristmasBashNightGameDataDao;
import com.jjg.game.slots.game.christmasBashNight.dao.ChristmasBashNightResultLibDao;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightGameRunInfo;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightPlayerGameData;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightPlayerGameDataDTO;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightResultLib;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractChristmasBashNightGameManager extends AbstractSlotsGameManager<ChristmasBashNightPlayerGameData, ChristmasBashNightResultLib> {
    @Autowired
    protected ChristmasBashNightResultLibDao libDao;
    @Autowired
    protected ChristmasBashNightGenerateManager generateManager;
    @Autowired
    protected SlotsLogger logger;
    @Autowired
    protected ChristmasBashNightGameDataDao gameDataDao;

    public AbstractChristmasBashNightGameManager() {
        super(ChristmasBashNightPlayerGameData.class, ChristmasBashNightResultLib.class);
    }

    @Override
    public void init() {
        log.info("启动圣诞狂欢夜游戏管理器...");
        super.init();
    }

    @Override
    public ChristmasBashNightGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        ChristmasBashNightPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ChristmasBashNightGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        ChristmasBashNightGameRunInfo gameRunInfo = new ChristmasBashNightGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public ChristmasBashNightGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        ChristmasBashNightPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ChristmasBashNightGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param auto
     * @return
     */
    public ChristmasBashNightGameRunInfo startGame(PlayerController playerController, ChristmasBashNightPlayerGameData playerGameData, long betValue, boolean auto) {
        ChristmasBashNightGameRunInfo gameRunInfo = new ChristmasBashNightGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == ChristmasBashNightConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == ChristmasBashNightConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }

                //如果是免费模式，要累计记录中奖金额
                if (status == ChristmasBashNightConstant.Status.FREE) {
                    playerGameData.setFreeAllWin(playerGameData.getFreeAllWin() + addGold);
                } else {
                    playerGameData.setFreeAllWin(0);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(player, gameRunInfo.getAllWinGold(), betValue);

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getGold());

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
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    protected ChristmasBashNightGameRunInfo normal(ChristmasBashNightGameRunInfo gameRunInfo, ChristmasBashNightPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<ChristmasBashNightResultLib, Long>> libResult = normalGetLib(playerGameData, betValue, ChristmasBashNightConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ChristmasBashNightResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setTax(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(ChristmasBashNightConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(ChristmasBashNightConstant.Status.FREE);
            int againFreeCount = 0;
            int allCount = 0;
            for (SpecialAuxiliaryInfo info : resultLib.getSpecialAuxiliaryInfoList()) {
                for (JSONObject json : info.getFreeGames()) {
                    Integer addFreeCount = json.getInteger("addFreeCount");
                    if (addFreeCount != null && addFreeCount > 0) {
                        againFreeCount += addFreeCount;
                    }
                }
                allCount += info.getFreeGames().size();
            }
            //设置添加的免费次数
            int addCount = allCount - againFreeCount;
            playerGameData.setRemainFreeCount(new AtomicInteger(addCount));

            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += generateManager.calAfterAddIcons(resultLib.getAddIconInfos());

            playerGameData.setFreeLib(resultLib);

            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        //检查是否中大奖
        jackpool(gameRunInfo, playerGameData, resultLib);

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(ChristmasBashNightConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected ChristmasBashNightGameRunInfo free(ChristmasBashNightGameRunInfo gameRunInfo, ChristmasBashNightPlayerGameData playerGameData) {
        CommonResult<ChristmasBashNightResultLib> libResult = freeGetLib(playerGameData, ChristmasBashNightConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        ChristmasBashNightResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        if (afterCount < 1) {
            playerGameData.setStatus(ChristmasBashNightConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(ChristmasBashNightConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.CHRISTMAS_PARTY;
    }

    @Override
    protected void offlineSaveGameDataDto(ChristmasBashNightPlayerGameData gameData) {
        try {
            ChristmasBashNightPlayerGameDataDTO dto = gameData.converToDto(ChristmasBashNightPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected ChristmasBashNightResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected ChristmasBashNightGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected ChristmasBashNightGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭圣诞狂欢夜游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 获取奖池 信息
     *
     * @param playerController 玩家控制类
     * @return
     */
    public ChristmasBashNightGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        ChristmasBashNightGameRunInfo gameRunInfo = new ChristmasBashNightGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(ChristmasBashNightConstant.Common.MINI_POOL_ID, stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(ChristmasBashNightConstant.Common.MINOR_POOL_ID, stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(ChristmasBashNightConstant.Common.MAJOR_POOL_ID, stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(ChristmasBashNightConstant.Common.GRAND_POOL_ID, stake));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }


    /**
     * 检查中大奖
     *
     * @param gameRunInfo
     * @param playerGameData
     * @param resultLib
     * @return
     */
    private ChristmasBashNightGameRunInfo jackpool(ChristmasBashNightGameRunInfo gameRunInfo, ChristmasBashNightPlayerGameData playerGameData, ChristmasBashNightResultLib resultLib) {
        if (!resultLib.getLibTypeSet().contains(ChristmasBashNightConstant.SpecialMode.JACKPOOL)) {
            return gameRunInfo;
        }

        try {
            PoolCfg poolCfg = randWinPool(playerGameData, resultLib.getJackpotId());
            if (poolCfg == null) {
                log.warn("未找到对应的奖池配置 poolId = {}", resultLib.getJackpotId());
                return gameRunInfo;
            }

            long poolValue = calPoolValue(playerGameData.getOneBetScore(), poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax(), poolCfg.getDelayTime());
            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), poolValue, AddType.SLOTS_TRAIN, resultLib.getJackpotId() + "");
            if (!result.success()) {
                log.warn("从小池子扣除，并给玩家加钱失败 code = {}", result.code);
                return gameRunInfo;
            }
            playerGameData.addSmallPoolReward(poolValue);
            gameRunInfo.addSmallPoolGold(poolValue);

            log.info("玩家奖池中奖 playerId = {},gameType = {},roomCfgId = {},poolId = {},poolValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), resultLib.getJackpotId(), poolValue);
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(ChristmasBashNightPlayerGameData gameData) {
        if (gameData.getStatus() == ChristmasBashNightConstant.Status.FREE) {
            freeStateAction(gameData, (playerGameData) ->
                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
        }
    }
}
