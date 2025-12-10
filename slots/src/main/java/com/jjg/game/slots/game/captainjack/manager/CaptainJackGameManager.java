package com.jjg.game.slots.game.captainjack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.game.captainjack.pb.req.ReqCaptainJackTreasureChest;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class CaptainJackGameManager extends AbstractSlotsGameManager<CaptainJackPlayerGameData, CaptainJackResultLib> {
    private final CaptainJackGameGenerateManager gameGenerateManager;
    private final CaptainJackGameDataDao gameDataDao;
    private final CaptainJackResultLibDao captainJackResultLibDao;
    private final ClusterSystem clusterSystem;

    public CaptainJackGameManager(CaptainJackGameGenerateManager gameGenerateManager,
                                  CaptainJackGameDataDao gameDataDao, CaptainJackResultLibDao captainJackResultLibDao, ClusterSystem clusterSystem) {
        super(CaptainJackPlayerGameData.class, CaptainJackResultLib.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.captainJackResultLibDao = captainJackResultLibDao;
        this.clusterSystem = clusterSystem;
    }


    @Override
    public void init() {
        log.info("启动杰克船长游戏管理器...");
        super.init();

    }

    @Override
    public CaptainJackGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        CaptainJackPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new CaptainJackGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        CaptainJackGameRunInfo gameRunInfo = new CaptainJackGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     */
    public CaptainJackGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        CaptainJackPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new CaptainJackGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     */
    public CaptainJackGameRunInfo startGame(PlayerController playerController, CaptainJackPlayerGameData playerGameData, long betValue, boolean auto) {
        CaptainJackGameRunInfo gameRunInfo = new CaptainJackGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(player.getGold());
            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == CaptainJackConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == CaptainJackConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
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
                //如果是免费模式，要累计记录中奖金额
                if (status == CaptainJackConstant.Status.FREE) {
                    if (gameRunInfo.getRemainFreeCount() == 0) {
                        playerGameData.setFreeAllWin(addGold);
                    } else {
                        addGold = 0;
                    }
                }
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
    private void jackPool(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData, CaptainJackResultLib resultLib) {
        if (!resultLib.getLibTypeSet().contains(CaptainJackConstant.SpecialMode.JACK_POOL)) {
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
    private void normal(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<CaptainJackResultLib, Long>> libResult = normalGetLib(playerGameData, betValue, CaptainJackConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        CaptainJackResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setTax(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(CaptainJackConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(CaptainJackConstant.Status.FREE);
            int addCount;
            if (CollectionUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                SpecialAuxiliaryInfo first = resultLib.getSpecialAuxiliaryInfoList().getFirst();
                if (CollectionUtil.isNotEmpty(first.getFreeGames())) {
                    JSONObject object = first.getFreeGames().getFirst();
                    addCount = object.getInteger("addFreeCount");
                    playerGameData.getRemainFreeCount().addAndGet(addCount);
                }
            }
            long times = gameGenerateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += gameGenerateManager.calAfterAddIcons(resultLib.getAddIconInfos());
            playerGameData.setFreeLib(resultLib);

            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get(), times);
        } else if (resultLib.getLibTypeSet().contains(CaptainJackConstant.SpecialMode.MINI_GAME)) {
            playerGameData.setStatus(CaptainJackConstant.Status.TREASURE_CHEST);
            playerGameData.addAlreadyDigCount(resultLib.getDigTimes());
            playerGameData.setResultLib(resultLib);
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }
        //检查是否中大奖
        jackPool(gameRunInfo, playerGameData, resultLib);
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(CaptainJackConstant.Status.NORMAL);
    }

    /**
     * 免费游戏
     *
     */
    private void free(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData) {
        CommonResult<CaptainJackResultLib> libResult = freeGetLib(playerGameData, CaptainJackConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        CaptainJackResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        if (afterCount == 0) {
            if (playerGameData.getFreeLib() instanceof CaptainJackResultLib lib) {
                gameRunInfo.addBigPoolTimes(lib.getTimes());
            }
            playerGameData.setStatus(CaptainJackConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        } else {
            gameRunInfo.addBigPoolTimes(gameGenerateManager.getAddTimes());
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(CaptainJackConstant.Status.FREE);
    }

    public CaptainJackGameRunInfo treasureChest(PlayerController playerController, ReqCaptainJackTreasureChest req) {
        CaptainJackGameRunInfo gameRunInfo = new CaptainJackGameRunInfo(Code.SUCCESS, playerController.playerId());
        //获取玩家数据
        CaptainJackPlayerGameData playerGameData = getPlayerGameData(playerController);
        //判断玩家是否正在寻宝当中
        if (playerGameData.getStatus() != CaptainJackConstant.Status.TREASURE_CHEST || playerGameData.getResultLib() == null) {
            gameRunInfo.setCode(Code.ERROR_REQ);
            return gameRunInfo;
        }
        //获取结果库
        CaptainJackResultLib resultLib = playerGameData.getResultLib();
        if (resultLib.getDigTimes() <= playerGameData.getAlreadyDigCount()) {
            gameRunInfo.setCode(Code.ERROR_REQ);
            return gameRunInfo;
        }
        int alreadyDigCount = playerGameData.addAlreadyDigCount(1);
        gameRunInfo.setAlreadyDigCount(alreadyDigCount);
        gameRunInfo.addDigTimesMultiplier(resultLib.getDigTimesMultiplier().get(alreadyDigCount));
        //如果是最后一次进行结算
        if (alreadyDigCount == resultLib.getDigTimes()) {
            long addGold = playerGameData.getOneBetScore() * gameRunInfo.getDigTimesMultiplier();
            if (addGold > 0) {
                CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, AddType.SLOTS_INVEST_REWARD);
                if (!result.success()) {
                    log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                    gameRunInfo.setCode(result.code);
                    return gameRunInfo;
                }
                playerGameData.setStatus(CaptainJackConstant.Status.NORMAL);
                playerGameData.setResultLib(null);
                playerGameData.setAlreadyDigCount(null);
                gameRunInfo.setAllWinGold(addGold);
            }
            gameRunInfo.setStatus(CaptainJackConstant.Status.TREASURE_CHEST);
        }
        return gameRunInfo;
    }


    @Override
    public int getGameType() {
        return CoreConst.GameType.CAPTAIN_JACK;
    }

    @Override
    protected void offlineSaveGameDataDto(CaptainJackPlayerGameData gameData) {
        try {
            SlotsPlayerGameDataDTO dto = gameData.converToDto(SlotsPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected CaptainJackResultLibDao getResultLibDao() {
        return this.captainJackResultLibDao;
    }

    @Override
    protected CaptainJackGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected CaptainJackGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭Captain Jack游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
