package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.basketballSuperstar.dao.BasketballSuperstarGameDataDao;
import com.jjg.game.slots.game.basketballSuperstar.dao.BasketballSuperstarResultLibDao;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarGameRunInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameData;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarPlayerGameDataDTO;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBasketballSuperstarGameManager extends AbstractSlotsGameManager<BasketballSuperstarPlayerGameData, BasketballSuperstarResultLib, BasketballSuperstarGameRunInfo> {
    @Autowired
    private BasketballSuperstarResultLibDao libDao;
    @Autowired
    private BasketballSuperstarGenerateManager generateManager;
    @Autowired
    private BasketballSuperstarGameDataDao gameDataDao;

    public AbstractBasketballSuperstarGameManager() {
        super(BasketballSuperstarPlayerGameData.class, BasketballSuperstarResultLib.class, BasketballSuperstarGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动篮球巨星游戏管理器...");
        super.init();
    }

    /**
     * 开始游戏
     *
     * @param playerGameData
     * @param auto
     * @return
     */
    @Override
    public BasketballSuperstarGameRunInfo startGame(PlayerController playerController,BasketballSuperstarPlayerGameData playerGameData, long betValue, boolean auto) {
        BasketballSuperstarGameRunInfo gameRunInfo = new BasketballSuperstarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            Player player = slotsPlayerService.get(playerGameData.playerId());
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(player.getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == BasketballSuperstarConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == BasketballSuperstarConstant.Status.FREE) {
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
            triggerWinTask(player, gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());

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
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected BasketballSuperstarGameRunInfo normal(BasketballSuperstarGameRunInfo gameRunInfo, BasketballSuperstarPlayerGameData playerGameData, long betValue, BasketballSuperstarResultLib resultLib) {
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

        log.debug("id = {}", resultLib.getId());

        //检查是否中大奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

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

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(BasketballSuperstarConstant.Status.NORMAL);
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
    protected Class<BasketballSuperstarPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return BasketballSuperstarPlayerGameDataDTO.class;
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


    @Override
    protected void onAutoExitAction(BasketballSuperstarPlayerGameData playerGameData, int eventId) {
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
    public BasketballSuperstarGameRunInfo autoStartGame(BasketballSuperstarPlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动玩游戏 playerId = {}", playerGameData.playerId());
        return startGame(null,playerGameData, betValue, true);
    }
}
