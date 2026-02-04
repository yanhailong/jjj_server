package com.jjg.game.slots.game.frozenThrone.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.game.frozenThrone.dao.FrozenThroneGameDataDao;
import com.jjg.game.slots.game.frozenThrone.dao.FrozenThroneResultLibDao;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneGameRunInfo;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameData;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThronePlayerGameDataDTO;
import com.jjg.game.slots.game.frozenThrone.data.FrozenThroneResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractFrozenThroneGameManager extends AbstractSlotsGameManager<FrozenThronePlayerGameData, FrozenThroneResultLib, FrozenThroneGameRunInfo> {
    @Autowired
    private FrozenThroneResultLibDao libDao;
    @Autowired
    private FrozenThroneGenerateManager generateManager;
    @Autowired
    private FrozenThroneGameDataDao gameDataDao;

    public AbstractFrozenThroneGameManager() {
        super(FrozenThronePlayerGameData.class, FrozenThroneResultLib.class, FrozenThroneGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动寒冰王座游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }

    @Override
    public FrozenThroneGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        FrozenThronePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new FrozenThroneGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        FrozenThroneGameRunInfo gameRunInfo = new FrozenThroneGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 开始游戏
     *
     * @param playerGameData
     * @param auto
     * @return
     */
    @Override
    public FrozenThroneGameRunInfo startGame(PlayerController playerController, FrozenThronePlayerGameData playerGameData, long betValue, boolean auto) {
        FrozenThroneGameRunInfo gameRunInfo = new FrozenThroneGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(player.getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == FrozenThroneConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == FrozenThroneConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", player.getId(), player.getGameType());
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

    @Override
    protected FrozenThroneGameRunInfo normal(FrozenThroneGameRunInfo gameRunInfo, FrozenThronePlayerGameData playerGameData, long betValue, FrozenThroneResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(FrozenThroneConstant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(FrozenThroneConstant.Status.FREE);
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
            //特殊 客户端开发要求推下次游戏状态 -》 赋值状态 免费转
//            gameRunInfo.setStatus(FrozenThroneConstant.Status.FREE);
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
//            //特殊 客户端开发要求推下次游戏状态 -》 赋值状态 免费转
//            gameRunInfo.setStatus(FrozenThroneConstant.Status.NORMAL);
        }

        log.debug("id = {}", resultLib.getId());

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        //特殊 客户端开发要求推下次游戏状态 -》 所以注释
        gameRunInfo.setStatus(FrozenThroneConstant.Status.NORMAL);

        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    public FrozenThroneGameRunInfo free(FrozenThroneGameRunInfo gameRunInfo, FrozenThronePlayerGameData playerGameData) {
        CommonResult<FrozenThroneResultLib> libResult = freeGetLib(playerGameData, FrozenThroneConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        FrozenThroneResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(FrozenThroneConstant.Status.NORMAL);
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
        gameRunInfo.setStatus(FrozenThroneConstant.Status.FREE);

        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.FROZEN_THRONE;
    }

    @Override
    protected FrozenThroneResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected FrozenThroneGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected FrozenThroneGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return FrozenThronePlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭寒冰王座游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(FrozenThronePlayerGameData playerGameData, int eventId) {
        //检查当前是否处于特殊模式
        if (playerGameData.getStatus() == BasketballSuperstarConstant.Status.FREE) {
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
    public FrozenThroneGameRunInfo autoStartGame(FrozenThronePlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());
        return startGame(null,playerGameData, betValue, true);
    }
}
