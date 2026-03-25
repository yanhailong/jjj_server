package com.jjg.game.slots.game.mahjiongwin2.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.mahjiongwin2.MahjiongWin2Constant;
import com.jjg.game.slots.game.mahjiongwin2.dao.MahjiongWin2ResultLibDao;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2GameRunInfo;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2PlayerGameData;
import com.jjg.game.slots.game.mahjiongwin2.data.MahjiongWin2ResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractMahjiongWin2GameManager extends AbstractSlotsGameManager<MahjiongWin2PlayerGameData, MahjiongWin2ResultLib, MahjiongWin2GameRunInfo> {
    @Autowired
    protected MahjiongWin2ResultLibDao libDao;
    @Autowired
    protected MahjiongWin2GenerateManager generateManager;

    public AbstractMahjiongWin2GameManager() {
        super(MahjiongWin2PlayerGameData.class, MahjiongWin2ResultLib.class, MahjiongWin2GameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动麻将胡了2游戏管理器...");
        super.init();

//        Map<Integer, Integer> map = new HashMap<>();
//        map.put(1, 50000);
//        map.put(2, 50000);
//        addGenerateLibEvent(map);
    }


    /**
     * 开始游戏
     *
     * @param playerController
     * @param playerGameData
     * @param auto
     * @return
     */
    @Override
    public MahjiongWin2GameRunInfo startGame(PlayerController playerController, MahjiongWin2PlayerGameData playerGameData, long betValue, boolean auto) {
        MahjiongWin2GameRunInfo gameRunInfo = new MahjiongWin2GameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == MahjiongWin2Constant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == MahjiongWin2Constant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
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
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

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
    protected MahjiongWin2GameRunInfo normal(MahjiongWin2GameRunInfo gameRunInfo, MahjiongWin2PlayerGameData playerGameData, long betValue, MahjiongWin2ResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(MahjiongWin2Constant.SpecialMode.FREE)) {  //是否会触发免费
            playerGameData.setStatus(MahjiongWin2Constant.Status.FREE);
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
            log.debug("触发免费模式  playerId = {},libId = {},status = {},addFreeCount = {},times = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("id = {}", resultLib.getId());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(MahjiongWin2Constant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected MahjiongWin2GameRunInfo free(MahjiongWin2GameRunInfo gameRunInfo, MahjiongWin2PlayerGameData playerGameData) {
        CommonResult<MahjiongWin2ResultLib> libResult = freeGetLib(playerGameData, MahjiongWin2Constant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        MahjiongWin2ResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(MahjiongWin2Constant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(MahjiongWin2Constant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(MahjiongWin2PlayerGameData gameData, int eventId) {
//        if (gameData.getStatus() == MahjiongWin2Constant.Status.FREE) {
//            freeStateAction(gameData, (playerGameData) ->
//                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
//        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.MAHJIONG_WIN2;
    }

    @Override
    protected MahjiongWin2ResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected MahjiongWin2GenerateManager getGenerateManager() {
        return this.generateManager;
    }


    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭麻将胡了2游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
