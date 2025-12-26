package com.jjg.game.slots.game.steamAge.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.OffLineEventData;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;
import com.jjg.game.slots.game.steamAge.dao.SteamAgeGameDataDao;
import com.jjg.game.slots.game.steamAge.dao.SteamAgeResultLibDao;
import com.jjg.game.slots.game.steamAge.data.SteamAgeGameRunInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameData;
import com.jjg.game.slots.game.steamAge.data.SteamAgePlayerGameDataDTO;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public class AbstractSteamAgeGameManager extends AbstractSlotsGameManager<SteamAgePlayerGameData, SteamAgeResultLib> {
    @Autowired
    private SteamAgeResultLibDao libDao;
    @Autowired
    private SteamAgeGenerateManager generateManager;
    @Autowired
    private SteamAgeGameDataDao gameDataDao;

    public AbstractSteamAgeGameManager() {
        super(SteamAgePlayerGameData.class, SteamAgeResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("启动蒸汽时代游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    @Override
    public SteamAgeGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        SteamAgePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new SteamAgeGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        SteamAgeGameRunInfo gameRunInfo = new SteamAgeGameRunInfo(Code.SUCCESS, playerGameData.playerId());
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
    public SteamAgeGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        SteamAgePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new SteamAgeGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     * @param playerGameData
     * @param auto
     * @return
     */
    public SteamAgeGameRunInfo startGame(SteamAgePlayerGameData playerGameData, long betValue, boolean auto) {
        SteamAgeGameRunInfo gameRunInfo = new SteamAgeGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(player.getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == SteamAgeConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == SteamAgeConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", player.getIp(), player.getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(player, gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());

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
     * @param gameRunInfo
     * @param playerGameData
     * @param betValue
     * @return
     */
    public SteamAgeGameRunInfo normal(SteamAgeGameRunInfo gameRunInfo, SteamAgePlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<SteamAgeResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, SteamAgeConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        SteamAgeResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(SteamAgeConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(SteamAgeConstant.Status.FREE);
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
            //免费次数 -1 时候赋值 0
            playerGameData.getRemainFreeCount().updateAndGet(value -> value < 0 ? 0 : value);
            ;
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotId(), false);

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(SteamAgeConstant.Status.NORMAL);

        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    public SteamAgeGameRunInfo free(SteamAgeGameRunInfo gameRunInfo, SteamAgePlayerGameData playerGameData) {
        CommonResult<SteamAgeResultLib> libResult = freeGetLib(playerGameData, SteamAgeConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        log.debug("免费剩余次数 抽奖 afterCount = {}", afterCount);

        SteamAgeResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(SteamAgeConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(SteamAgeConstant.Status.FREE);

        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.STEAM_AGE;
    }

    @Override
    protected void offlineSaveGameDataDto(SteamAgePlayerGameData gameData) {
        try {
            SteamAgePlayerGameDataDTO dto = gameData.converToDto(SteamAgePlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected SteamAgeResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected SteamAgeGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected SteamAgeGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭蒸汽时代游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(SteamAgePlayerGameData playerGameData, int eventId) {
        //检查当前是否处于特殊模式
        if (playerGameData.getStatus() == SteamAgeConstant.Status.FREE) {
            int forCount = playerGameData.getRemainFreeCount().get();
            while (forCount > 0) {
                autoStartGame(playerGameData, playerGameData.getAllBetScore());
                forCount = playerGameData.getRemainFreeCount().get();
            }
        }
    }

    /**
     * 自动玩游戏
     *
     * @param betValue
     * @return
     */
    public SteamAgeGameRunInfo autoStartGame(SteamAgePlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());
        return startGame(playerGameData, betValue, true);
    }
}
