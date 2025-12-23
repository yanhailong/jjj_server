package com.jjg.game.slots.game.basketballSuperstar.manager;

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
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.basketballSuperstar.dao.BasketballSuperstarGameDataDao;
import com.jjg.game.slots.game.basketballSuperstar.dao.BasketballSuperstarResultLibDao;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarGameRunInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameData;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameDataDTO;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class AbstractBasketballSuperstarGameManager extends AbstractSlotsGameManager<BasketballSuperstarPlayerGameData, BasketballSuperstarResultLib> {
    @Autowired
    private BasketballSuperstarResultLibDao libDao;
    @Autowired
    private BasketballSuperstarGenerateManager generateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private SlotsLogger logger;
    @Autowired
    private BasketballSuperstarGameDataDao gameDataDao;

    public AbstractBasketballSuperstarGameManager() {
        super(BasketballSuperstarPlayerGameData.class, BasketballSuperstarResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动篮球巨星游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    @Override
    public BasketballSuperstarGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        BasketballSuperstarPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new BasketballSuperstarGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        BasketballSuperstarGameRunInfo gameRunInfo = new BasketballSuperstarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
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
    public BasketballSuperstarGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        BasketballSuperstarPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new BasketballSuperstarGameRunInfo(Code.NOT_FOUND, playerController.playerId());
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
    public BasketballSuperstarGameRunInfo startGame(PlayerController playerController, BasketballSuperstarPlayerGameData playerGameData, long betValue, boolean auto) {
        BasketballSuperstarGameRunInfo gameRunInfo = new BasketballSuperstarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == BasketballSuperstarConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == BasketballSuperstarConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if(!gameRunInfo.success()){
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
                if(status == BasketballSuperstarConstant.Status.FREE) {
                    playerGameData.setFreeAllWin(playerGameData.getFreeAllWin() + addGold);
                }else {
                    playerGameData.setFreeAllWin(0);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(player,gameRunInfo.getAllWinGold(),betValue);

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
    public BasketballSuperstarGameRunInfo normal(BasketballSuperstarGameRunInfo gameRunInfo, BasketballSuperstarPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<BasketballSuperstarResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, BasketballSuperstarConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        BasketballSuperstarResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(BasketballSuperstarConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(BasketballSuperstarConstant.Status.FREE);
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
        gameRunInfo.setStatus(BasketballSuperstarConstant.Status.NORMAL);

        gameRunInfo.setChangeStickyIconSet(new HashSet<>());
        gameRunInfo.setStickyIcon(0);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    public BasketballSuperstarGameRunInfo free(BasketballSuperstarGameRunInfo gameRunInfo, BasketballSuperstarPlayerGameData playerGameData) {
        CommonResult<BasketballSuperstarResultLib> libResult = freeGetLib(playerGameData, BasketballSuperstarConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        BasketballSuperstarResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        if (afterCount < 1) {
            playerGameData.setStatus(BasketballSuperstarConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(BasketballSuperstarConstant.Status.FREE);

        gameRunInfo.setChangeStickyIconSet(freeGame.getChangeStickyIconSet());
        gameRunInfo.setStickyIcon(freeGame.getStickyIcon());

        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.BASKETBALL_STAR;
    }

    @Override
    protected void offlineSaveGameDataDto(BasketballSuperstarPlayerGameData gameData) {
        try {
            BasketballSuperstarPlayerGameDataDTO dto = gameData.converToDto(BasketballSuperstarPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected BasketballSuperstarResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected BasketballSuperstarGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected BasketballSuperstarGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭篮球巨星游戏管理器");
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
    public BasketballSuperstarGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        BasketballSuperstarGameRunInfo gameRunInfo  = new BasketballSuperstarGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(BasketballSuperstarConstant.Common.MINI_POOL_ID, stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(BasketballSuperstarConstant.Common.MINOR_POOL_ID, stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(BasketballSuperstarConstant.Common.MAJOR_POOL_ID, stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(BasketballSuperstarConstant.Common.GRAND_POOL_ID, stake));
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
    private BasketballSuperstarGameRunInfo jackpool(BasketballSuperstarGameRunInfo gameRunInfo, BasketballSuperstarPlayerGameData playerGameData, BasketballSuperstarResultLib resultLib) {
        if (!resultLib.getLibTypeSet().contains(BasketballSuperstarConstant.SpecialMode.JACKPOOL)) {
            return gameRunInfo;
        }

        try {
            PoolCfg poolCfg = randWinPool(playerGameData, resultLib.getJackpotId());
            if (poolCfg == null) {
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
}
