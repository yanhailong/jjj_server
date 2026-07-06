package com.jjg.game.slots.game.superGolf.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;
import com.jjg.game.slots.game.superGolf.dao.SuperGolfResultLibDao;
import com.jjg.game.slots.game.superGolf.data.SuperGolfGameRunInfo;
import com.jjg.game.slots.game.superGolf.data.SuperGolfPlayerGameData;
import com.jjg.game.slots.game.superGolf.data.SuperGolfResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 超级高尔夫游戏管理器抽象基类。
 * <p>
 * 跟 Dracula 主要差异：
 * <ul>
 *   <li>{@code normal()} 触发免费时调用 {@link SuperGolfPlayerGameData#setFreeMultiplierAccum(int)} 重置累计倍率</li>
 *   <li>{@code free()} 每局加上本局 lib.multiplier 到累计倍率，免费结束时清零</li>
 * </ul>
 */
public abstract class AbstractSuperGolfGameManager extends AbstractSlotsGameManager<SuperGolfPlayerGameData, SuperGolfResultLib, SuperGolfGameRunInfo> {
    @Autowired
    protected SuperGolfResultLibDao libDao;
    @Autowired
    protected SuperGolfGenerateManager generateManager;

    public AbstractSuperGolfGameManager() {
        super(SuperGolfPlayerGameData.class, SuperGolfResultLib.class, SuperGolfGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动超级高尔夫游戏管理器...");
        super.init();
    }

    @Override
    public void changeSampleCallbackCollector() {
        log.warn("超级高尔夫 无法重载配置表");
    }

    @Override
    public SuperGolfGameRunInfo enterGame(PlayerController playerController) {
        SuperGolfPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败 playerId={},gameType={},roomCfgId={}",
                    playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new SuperGolfGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        resetFreeStateIfInvalid(playerGameData, SuperGolfConstant.Status.FREE, SuperGolfConstant.Status.NORMAL, "超级高尔夫");

        SuperGolfGameRunInfo gameRunInfo = new SuperGolfGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    @Override
    public SuperGolfGameRunInfo startGame(PlayerController playerController, SuperGolfPlayerGameData playerGameData, long betValue, boolean auto) {
        SuperGolfGameRunInfo gameRunInfo = new SuperGolfGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            int status = playerGameData.getStatus();
            if (status == SuperGolfConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == SuperGolfConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId={},gameType={}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            int times = calWinTimes(gameRunInfo, playerGameData);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

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
    protected SuperGolfGameRunInfo normal(SuperGolfGameRunInfo gameRunInfo, SuperGolfPlayerGameData playerGameData, long betValue, SuperGolfResultLib resultLib) {
        Set<Integer> libTypeSet = resultLib.getLibTypeSet();
        List<SpecialAuxiliaryInfo> auxList = resultLib.getSpecialAuxiliaryInfoList();
        boolean triggerFree = libTypeSet != null
                && libTypeSet.contains(SuperGolfConstant.SpecialMode.FREE)
                && auxList != null
                && !auxList.isEmpty();
        if (triggerFree) {
            playerGameData.setStatus(SuperGolfConstant.Status.FREE);
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
            int addCount = allCount - againFreeCount;
            playerGameData.setRemainFreeCount(new AtomicInteger(addCount));

            //进入免费模式：累计倍率清零，准备从 0 开始累加
            playerGameData.setFreeMultiplierAccum(0);

            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());
            times += generateManager.calAfterAddIcons(resultLib.getAddIconInfos());

            playerGameData.setFreeLib(resultLib);
            gameRunInfo.addBigPoolTimes(times);
            log.debug("触发免费模式 playerId={},libId={},addFreeCount={},times={}",
                    playerGameData.getPlayerId(), resultLib.getId(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(SuperGolfConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏：消费一局，并把本局贡献的乘倍累加到 playerGameData.freeMultiplierAccum。
     * 免费结束时把累计倍率清零。
     */
    protected SuperGolfGameRunInfo free(SuperGolfGameRunInfo gameRunInfo, SuperGolfPlayerGameData playerGameData) {
        CommonResult<SuperGolfResultLib> libResult = freeGetLib(playerGameData, SuperGolfConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        SuperGolfResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("免费再触发 addFreeCount={},afterCount={}", freeGame.getAddFreeCount(), afterCount);
        }

        //免费模式：本局贡献的"新增"乘倍累加到玩家持久态
        //文档 [42]："从神秘符号收集而来的奖金倍数都将保留直到免费旋转模式结束"
        int prevAccum = playerGameData.getFreeMultiplierAccum();
        int newAccum = prevAccum + Math.max(freeGame.getMultiplier() - 1, 0);
        playerGameData.setFreeMultiplierAccum(newAccum);

        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            //免费结束：状态/缓存/累计倍率全部清零
            playerGameData.setStatus(SuperGolfConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            playerGameData.setFreeMultiplierAccum(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费结束，回到普通态 playerId={},roomCfgId={}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(SuperGolfConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.SUPER_GOLF;
    }

    @Override
    protected SuperGolfResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected SuperGolfGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭超级高尔夫游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
