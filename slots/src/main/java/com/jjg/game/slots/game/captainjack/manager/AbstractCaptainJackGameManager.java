package com.jjg.game.slots.game.captainjack.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackPlayerGameDataDTO;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.List;

public abstract class AbstractCaptainJackGameManager extends AbstractSlotsGameManager<CaptainJackPlayerGameData, CaptainJackResultLib, CaptainJackGameRunInfo> {
    protected final CaptainJackGameGenerateManager gameGenerateManager;
    protected final CaptainJackGameDataDao gameDataDao;
    protected final CaptainJackResultLibDao captainJackResultLibDao;

    public AbstractCaptainJackGameManager(CaptainJackGameGenerateManager gameGenerateManager,
                                          CaptainJackGameDataDao gameDataDao, CaptainJackResultLibDao captainJackResultLibDao) {
        super(CaptainJackPlayerGameData.class, CaptainJackResultLib.class, CaptainJackGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.captainJackResultLibDao = captainJackResultLibDao;
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
    @Override
    public CaptainJackGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        CaptainJackPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new CaptainJackGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        if (playerGameData.getStatus() == CaptainJackConstant.Status.TREASURE_CHEST) {
            return new CaptainJackGameRunInfo(Code.ERROR_REQ, playerController.playerId());
        }
        if (getRoomType() != null) {
            int code = slotsRoomManager.checkCanPlay(this, playerController);
            if (code != Code.SUCCESS) {
                log.debug("该游戏无法继续 playerId = {},gameType = {},roomCfgId = {},code = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), code);
                return new CaptainJackGameRunInfo(code, playerController.playerId());
            }
        }
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 开始游戏
     *
     */
    @Override
    public CaptainJackGameRunInfo startGame(PlayerController playerController, CaptainJackPlayerGameData playerGameData, long betValue, boolean auto) {
        CaptainJackGameRunInfo gameRunInfo = new CaptainJackGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == CaptainJackConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == CaptainJackConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
            } else if (status == CaptainJackConstant.Status.TREASURE_CHEST) {
                treasureChest(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

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
     * 挖宝
     */
    protected void treasureChest(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData) {
        CaptainJackResultLib treasureChestLib = playerGameData.getResultLib();
        if (treasureChestLib == null || playerGameData.getAlreadyDigCount() > treasureChestLib.getDigTimes()) {
            gameRunInfo.setCode(Code.FAIL);
            return;
        }
        //增加挖宝次数
        int afterCount = playerGameData.getAlreadyDigCount();
        gameRunInfo.setRemainDigCount(treasureChestLib.getDigTimes() - afterCount);
        if (afterCount == treasureChestLib.getDigTimes()) {
            //重新计算总赔率
            int sum = treasureChestLib.getDigTimesMultiplier().stream().mapToInt(Integer::intValue).sum();
            gameRunInfo.setBigPoolTimes(sum);
            if (playerGameData.getRemainFreeCount().get() > 0) {
                playerGameData.setStatus(CaptainJackConstant.Status.FREE);
            } else {
                playerGameData.setStatus(CaptainJackConstant.Status.NORMAL);
            }
            playerGameData.setResultLib(null);
            playerGameData.setAlreadyDigCount(null);
            log.debug("挖宝游戏次数结束，回归之前状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        } else {
            afterCount = playerGameData.addAlreadyDigCount(1);
        }
        gameRunInfo.setDigTimesMultiplier(treasureChestLib.getDigTimesMultiplier().get(afterCount - 1));
        gameRunInfo.setStatus(playerGameData.getStatus());
        gameRunInfo.setAllWinGold(playerGameData.getOneBetScore() * gameRunInfo.getDigTimesMultiplier());
    }

    @Override
    protected CaptainJackGameRunInfo normal(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData, long betValue, CaptainJackResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(CaptainJackConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(CaptainJackConstant.Status.FREE);
            long times = gameGenerateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += gameGenerateManager.calAfterAddIcons(resultLib.getAddIconInfos());
            playerGameData.setFreeLib(resultLib);
            playerGameData.getRemainFreeCount().set(resultLib.getAddFreeCount());
            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(),
                    playerGameData.getRemainFreeCount().get(), times);
        } else if (resultLib.getLibTypeSet().contains(CaptainJackConstant.SpecialMode.MINI_GAME)) {
            playerGameData.setStatus(CaptainJackConstant.Status.TREASURE_CHEST);
            playerGameData.setResultLib(resultLib);
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }
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
     * 免费游戏
     *
     */
    protected void free(CaptainJackGameRunInfo gameRunInfo, CaptainJackPlayerGameData playerGameData) {
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

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        if (afterCount == 0) {
            playerGameData.setStatus(CaptainJackConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }
        //免费触发挖宝
        if (freeGame.getDigTimes() > 0 && CollectionUtil.isNotEmpty(freeGame.getDigTimesMultiplier())) {
            playerGameData.setStatus(CaptainJackConstant.Status.TREASURE_CHEST);
            playerGameData.setResultLib(freeGame);
        }
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.CAPTAIN_JACK;
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
    protected Class<CaptainJackPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return CaptainJackPlayerGameDataDTO.class;
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

    public CaptainJackGameRunInfo treasureHunting(PlayerController playerController) {
        //获取玩家游戏数据
        CaptainJackPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null || playerGameData.getStatus() != CaptainJackConstant.Status.TREASURE_CHEST) {
            log.debug("获取玩家游戏数据失败，开始挖宝失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new CaptainJackGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        int code = slotsRoomManager.checkCanPlay(this, playerController);
        if (code != Code.SUCCESS) {
            log.debug("该playerId = {},gameType = {},roomCfgId = {},code = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId(), code);
            return new CaptainJackGameRunInfo(code, playerController.playerId());
        }
        return startGame(playerController, playerGameData, playerGameData.getAllBetScore(), false);
    }

    @Override
    protected void onAutoExitAction(CaptainJackPlayerGameData gameData, int eventId) {
        //发放免费模式和探宝奖励
        if (gameData.getStatus() == CaptainJackConstant.Status.FREE) {
            Object freeLib = gameData.getFreeLib();
            if (freeLib instanceof CaptainJackResultLib lib) {
                List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = lib.getSpecialAuxiliaryInfoList();
                int totalSize = 0;
                for (SpecialAuxiliaryInfo auxiliaryInfo : specialAuxiliaryInfoList) {
                    if (auxiliaryInfo.getFreeGames() != null) {
                        totalSize = auxiliaryInfo.getFreeGames().size();
                    }
                }
                int index = gameData.getFreeIndex().get();
                for (int i = index; i < totalSize; i++) {
                    startGame(new PlayerController(null, null), gameData, gameData.getAllBetScore(), true);
                    autoRunTreasureChest(gameData);
                }
            }
        }
        autoRunTreasureChest(gameData);
    }

    /**
     * 自动执行探宝
     *
     * @param gameData 游戏数据
     */
    private void autoRunTreasureChest(CaptainJackPlayerGameData gameData) {
        if (gameData.getStatus() == CaptainJackConstant.Status.TREASURE_CHEST) {
            CaptainJackResultLib resultLib = gameData.getResultLib();
            if (resultLib == null) {
                return;
            }
            int remainCount = resultLib.getDigTimes() - gameData.getAlreadyDigCount();
            for (int i = 0; i < remainCount; i++) {
                startGame(new PlayerController(null, null), gameData, gameData.getAllBetScore(), true);
            }
        }
    }
}
