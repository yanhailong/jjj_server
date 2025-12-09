package com.jjg.game.slots.game.thor.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.dollarexpress.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.game.thor.dao.ThorGameDataDao;
import com.jjg.game.slots.game.thor.dao.ThorResultLibDao;
import com.jjg.game.slots.game.thor.data.ThorGameRunInfo;
import com.jjg.game.slots.game.thor.data.ThorPlayerGameData;
import com.jjg.game.slots.game.thor.data.ThorResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
@Component
public class ThorGameManager extends AbstractSlotsGameManager<ThorPlayerGameData, ThorResultLib> {

    @Autowired
    private ThorResultLibDao libDao;
    @Autowired
    private ThorGenerateManager generateManager;
    @Autowired
    private ThorGameDataDao gameDataDao;

    public ThorGameManager() {
        super(ThorPlayerGameData.class, ThorResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }


    /**
     * 玩家开始游戏
     *
     * @param playerController
     * @param stake
     * @return
     */
    public ThorGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 免费模式二选一
     *
     * @param playerController
     * @return
     */
    public ThorGameRunInfo freeChooseOne(PlayerController playerController, int chooseType) {
        //获取玩家游戏数据
        ThorPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，二选一失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new ThorGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        if (playerGameData.getStatus() != ThorConstant.Status.CHOOSE_ONE) {
            log.debug("玩家当前不处于二选一状态，二选一失败 playerId = {},gameType = {},roomCfgId = {},status = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), playerGameData.getStatus());
            return new ThorGameRunInfo(Code.FORBID, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());

        if (chooseType == 0) {
            playerGameData.setStatus(ThorConstant.Status.FIRE);
        } else {
            playerGameData.setStatus(ThorConstant.Status.ICE);
        }
        return new ThorGameRunInfo(Code.SUCCESS, playerController.playerId());
    }

    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param stake
     * @return
     */
    private ThorGameRunInfo startGame(PlayerController playerController, ThorPlayerGameData playerGameData, long stake, boolean auto) {
        ThorGameRunInfo gameRunInfo = new ThorGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == ThorConstant.Status.NORMAL) {  //正常
                gameRunInfo = normal(gameRunInfo, playerGameData, stake);
            } else if (status == ThorConstant.Status.CHOOSE_ONE) {  //二选一
                gameRunInfo.setCode(Code.FORBID);
                log.debug("当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            } else if (status == ThorConstant.Status.FIRE) {  //火焰免费
                gameRunInfo = free(gameRunInfo, playerGameData, ThorConstant.SpecialMode.FIRE);
            } else if (status == ThorConstant.Status.ICE) {  //冰冻免费
                gameRunInfo = free(gameRunInfo, playerGameData, ThorConstant.SpecialMode.ICE);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
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
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), stake);

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getGold());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, stake);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 获取奖池
     *
     * @param playerController
     */
    public ThorGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        ThorGameRunInfo gameRunInfo = new ThorGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(ThorConstant.Common.MINI_POOL_ID, stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(ThorConstant.Common.MINOR_POOL_ID, stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(ThorConstant.Common.MAJOR_POOL_ID, stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(ThorConstant.Common.GRAND_POOL_ID, stake));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
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
    private ThorGameRunInfo normal(ThorGameRunInfo gameRunInfo, ThorPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<ThorResultLib, Long>> libResult = normalGetLib(playerGameData, betValue, DollarExpressConstant.SpecialMode.TYPE_NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ThorResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setTax(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(ThorConstant.SpecialMode.FREE)) {  //是否会触发二选一
            playerGameData.setStatus(ThorConstant.Status.CHOOSE_ONE);
            log.debug("触发二选一  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        gameRunInfo.setIconArr(resultLib.getIconArr());

        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        //检查是否中大奖
        jackpool(gameRunInfo, playerGameData, resultLib);

        gameRunInfo.setStatus(playerGameData.getStatus());
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    /**
     * 免费模式
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    private ThorGameRunInfo free(ThorGameRunInfo gameRunInfo, ThorPlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<ThorResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        ThorResultLib freeGame = libResult.data;

        gameRunInfo.setStatus(playerGameData.getStatus());

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        if (afterCount < 1) {
            playerGameData.setStatus(DollarExpressConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setResultLib(freeGame);
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
    private ThorGameRunInfo jackpool(ThorGameRunInfo gameRunInfo, ThorPlayerGameData playerGameData, ThorResultLib resultLib) {
        if (!resultLib.getLibTypeSet().contains(ThorConstant.SpecialMode.JACKPOOL)) {
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
    protected ThorResultLibDao getResultLibDao() {
        return libDao;
    }

    @Override
    protected ThorGameDataDao getGameDataDao() {
        return gameDataDao;
    }

    @Override
    protected ThorGenerateManager getGenerateManager() {
        return generateManager;
    }

    @Override
    protected void offlineSaveGameDataDto(ThorPlayerGameData gameData) {

    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.THOR;
    }
}
