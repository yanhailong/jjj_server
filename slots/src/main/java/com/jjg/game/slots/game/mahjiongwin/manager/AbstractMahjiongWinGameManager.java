package com.jjg.game.slots.game.mahjiongwin.manager;

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
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.christmasBashNight.ChristmasBashNightConstant;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinGameDataDao;
import com.jjg.game.slots.game.mahjiongwin.dao.MahjiongWinResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameDataDTO;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public class AbstractMahjiongWinGameManager extends AbstractSlotsGameManager<MahjiongWinPlayerGameData, MahjiongWinResultLib> {
    @Autowired
    protected MahjiongWinResultLibDao libDao;
    @Autowired
    protected MahjiongWinGenerateManager generateManager;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;
    @Autowired
    protected SlotsLogger logger;
    @Autowired
    protected MahjiongWinGameDataDao gameDataDao;

    public AbstractMahjiongWinGameManager() {
        super(MahjiongWinPlayerGameData.class, MahjiongWinResultLib.class);
    }

    @Override
    public void init() {
        log.info("启动麻将胡了游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    @Override
    public MahjiongWinGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        MahjiongWinPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new MahjiongWinGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        MahjiongWinGameRunInfo gameRunInfo = new MahjiongWinGameRunInfo(Code.SUCCESS, playerGameData.playerId());
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
    public MahjiongWinGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        MahjiongWinPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new MahjiongWinGameRunInfo(Code.NOT_FOUND, playerController.playerId());
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
    public MahjiongWinGameRunInfo startGame(PlayerController playerController, MahjiongWinPlayerGameData playerGameData, long betValue, boolean auto) {
        MahjiongWinGameRunInfo gameRunInfo = new MahjiongWinGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getGold());

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == MahjiongWinConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == MahjiongWinConstant.Status.FREE) {
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
                if(status == MahjiongWinConstant.Status.FREE) {
                    playerGameData.setFreeAllWin(playerGameData.getFreeAllWin() + addGold);
                }else {
                    playerGameData.setFreeAllWin(0);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(),gameRunInfo.getAllWinGold(),betValue);

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
    protected MahjiongWinGameRunInfo normal(MahjiongWinGameRunInfo gameRunInfo, MahjiongWinPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<MahjiongWinResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, MahjiongWinConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        MahjiongWinResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());

        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(MahjiongWinConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(MahjiongWinConstant.Status.FREE);
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

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(MahjiongWinConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected MahjiongWinGameRunInfo free(MahjiongWinGameRunInfo gameRunInfo, MahjiongWinPlayerGameData playerGameData) {
        CommonResult<MahjiongWinResultLib> libResult = freeGetLib(playerGameData, MahjiongWinConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        MahjiongWinResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        if (afterCount < 1) {
            playerGameData.setStatus(MahjiongWinConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(MahjiongWinConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(MahjiongWinPlayerGameData gameData) {
        if (gameData.getStatus() == ChristmasBashNightConstant.Status.FREE) {
            freeStateAction(gameData, (playerGameData) ->
                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.MAHJIONG_WIN;
    }

    @Override
    protected void offlineSaveGameDataDto(MahjiongWinPlayerGameData gameData) {
        try {
            MahjiongWinPlayerGameDataDTO dto = gameData.converToDto(MahjiongWinPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected MahjiongWinResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected MahjiongWinGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected MahjiongWinGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭麻将胡了游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
