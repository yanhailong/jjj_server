package com.jjg.game.slots.game.dracula.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.dracula.DraculaConstant;
import com.jjg.game.slots.game.dracula.dao.DraculaResultLibDao;
import com.jjg.game.slots.game.dracula.data.DraculaGameRunInfo;
import com.jjg.game.slots.game.dracula.data.DraculaPlayerGameData;
import com.jjg.game.slots.game.dracula.data.DraculaResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractDraculaGameManager extends AbstractSlotsGameManager<DraculaPlayerGameData, DraculaResultLib, DraculaGameRunInfo> {
    @Autowired
    protected DraculaResultLibDao libDao;
    @Autowired
    protected DraculaGenerateManager generateManager;

    public AbstractDraculaGameManager() {
        super(DraculaPlayerGameData.class, DraculaResultLib.class, DraculaGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动德古拉黑暗财富游戏管理器...");
        super.init();
    }

    @Override
    public void changeSampleCallbackCollector() {
        log.warn("德古拉黑暗财富 无法重载配置表");
    }

    @Override
    public DraculaGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        DraculaPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new DraculaGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        resetFreeStateIfInvalid(playerGameData, DraculaConstant.Status.FREE, DraculaConstant.Status.NORMAL, "德古拉黑暗财富");

        DraculaGameRunInfo gameRunInfo = new DraculaGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
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
    public DraculaGameRunInfo startGame(PlayerController playerController, DraculaPlayerGameData playerGameData, long betValue, boolean auto) {
        DraculaGameRunInfo gameRunInfo = new DraculaGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == DraculaConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == DraculaConstant.Status.FREE) {
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
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

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
    protected DraculaGameRunInfo normal(DraculaGameRunInfo gameRunInfo, DraculaPlayerGameData playerGameData, long betValue, DraculaResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        //防御性判断：libTypeSet 含 FREE 但 specialAuxiliaryInfoList 为空时（如池子里抽到 FREE 子库当 NORMAL 用），按普通局处理避免 NPE
        Set<Integer> libTypeSet = resultLib.getLibTypeSet();
        List<SpecialAuxiliaryInfo> auxList = resultLib.getSpecialAuxiliaryInfoList();
        boolean triggerFree = libTypeSet != null
                && libTypeSet.contains(DraculaConstant.SpecialMode.FREE)
                && auxList != null
                && !auxList.isEmpty();
        if (triggerFree) {  //是否会触发免费
            playerGameData.setStatus(DraculaConstant.Status.FREE);
            int againFreeCount = 0;
            int allCount = 0;
            for (SpecialAuxiliaryInfo info : auxList) {
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
        gameRunInfo.setStatus(DraculaConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     *
     * @param gameRunInfo
     * @param playerGameData
     * @return
     */
    protected DraculaGameRunInfo free(DraculaGameRunInfo gameRunInfo, DraculaPlayerGameData playerGameData) {
        CommonResult<DraculaResultLib> libResult = freeGetLib(playerGameData, DraculaConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        //扣除免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        DraculaResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("添加免费次数 addFreeCount = {},afterCount = {}", freeGame.getAddFreeCount(), afterCount);
        }

        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(DraculaConstant.Status.NORMAL);
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
        gameRunInfo.setStatus(DraculaConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.DRACULA;
    }

    @Override
    protected DraculaResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected DraculaGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭德古拉黑暗财富游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
