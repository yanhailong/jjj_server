package com.jjg.game.slots.game.pegasusunbridle.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleGameDataDao;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridlePlayerGameDataDTO;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleGameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridlePlayerGameData;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class PegasusUnbridleGameManager extends AbstractSlotsGameManager<PegasusUnbridlePlayerGameData, PegasusUnbridleResultLib> {
    private final PegasusUnbridleGameGenerateManager gameGenerateManager;
    private final PegasusUnbridleGameDataDao gameDataDao;
    private final PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao;

    public PegasusUnbridleGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager,
                                      PegasusUnbridleGameDataDao gameDataDao, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(PegasusUnbridlePlayerGameData.class, PegasusUnbridleResultLib.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.PegasusUnbridleResultLibDao = PegasusUnbridleResultLibDao;
    }


    @Override
    public void init() {
        this.log =  LoggerFactory.getLogger(getClass());
        log.info("启动神马飞扬游戏管理器...");
        super.init();

    }

    @Override
    public PegasusUnbridleGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        PegasusUnbridlePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new PegasusUnbridleGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     */
    public PegasusUnbridleGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        PegasusUnbridlePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new PegasusUnbridleGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     */
    public PegasusUnbridleGameRunInfo startGame(PlayerController playerController, PegasusUnbridlePlayerGameData playerGameData, long betValue, boolean auto) {
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(player.getGold());
            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == PegasusUnbridleConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
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
            }
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue);

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
     * 检查中大奖
     *
     */
    private void jackPool(PegasusUnbridleGameRunInfo gameRunInfo, PegasusUnbridlePlayerGameData playerGameData, PegasusUnbridleResultLib resultLib) {
        if (!resultLib.getLibTypeSet().contains(PegasusUnbridleConstant.SpecialMode.JACK_POOL)) {
            return;
        }
        try {
            PoolCfg poolCfg = randWinPool(playerGameData, resultLib.getJackpotId());
            if (poolCfg == null) {
                log.warn("杰克船长 未找到对应的奖池配置 poolId = {}", resultLib.getJackpotId());
                return;
            }

            long poolValue = calPoolValue(playerGameData.getOneBetScore(), poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax(), poolCfg.getDelayTime());
            //给玩家加钱
            CommonResult<Player> result = slotsPoolDao.rewardFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), poolValue, AddType.SLOTS_TRAIN, resultLib.getJackpotId() + "");
            if (!result.success()) {
                log.warn("杰克船长 从小池子扣除，并给玩家加钱失败 code = {}", result.code);
                return;
            }
            playerGameData.addSmallPoolReward(poolValue);
            gameRunInfo.addSmallPoolGold(poolValue);

            log.info("杰克船长 玩家奖池中奖 playerId = {},gameType = {},roomCfgId = {},poolId = {},poolValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), resultLib.getJackpotId(), poolValue);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 普通正常流程
     *
     */
    private void normal(PegasusUnbridleGameRunInfo gameRunInfo, PegasusUnbridlePlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<PegasusUnbridleResultLib, Long>> libResult = normalGetLib(playerGameData, betValue, PegasusUnbridleConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        PegasusUnbridleResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setTax(libResult.data.getSecond());
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        //检查是否中大奖
        jackPool(gameRunInfo, playerGameData, resultLib);
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(playerGameData.getStatus());
    }




    @Override
    public int getGameType() {
        return CoreConst.GameType.PEGASUS_UNBRIDLE;
    }

    @Override
    protected void offlineSaveGameDataDto(PegasusUnbridlePlayerGameData gameData) {
        try {
            PegasusUnbridlePlayerGameDataDTO dto = gameData.converToDto(PegasusUnbridlePlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected PegasusUnbridleResultLibDao getResultLibDao() {
        return this.PegasusUnbridleResultLibDao;
    }

    @Override
    protected PegasusUnbridleGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected PegasusUnbridleGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭PEGASUS_UNBRIDLE游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }


    public PegasusUnbridleGameRunInfo getPoolValue(PlayerController playerController, long stakeValue) {
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(PegasusUnbridleConstant.Common.MINI_POOL_ID, stakeValue));
            gameRunInfo.setMinor(getPoolValueByPoolId(PegasusUnbridleConstant.Common.MINOR_POOL_ID, stakeValue));
            gameRunInfo.setMajor(getPoolValueByPoolId(PegasusUnbridleConstant.Common.MAJOR_POOL_ID, stakeValue));
            gameRunInfo.setGrand(getPoolValueByPoolId(PegasusUnbridleConstant.Common.GRAND_POOL_ID, stakeValue));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }


    @Override
    protected void onAutoExitAction(PegasusUnbridlePlayerGameData gameData) {

    }
}
