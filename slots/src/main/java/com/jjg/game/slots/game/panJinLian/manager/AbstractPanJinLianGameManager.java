package com.jjg.game.slots.game.panJinLian.manager;

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
import com.jjg.game.slots.game.panJinLian.PanJinLianConstant;
import com.jjg.game.slots.game.panJinLian.dao.PanJinLianGameDataDao;
import com.jjg.game.slots.game.panJinLian.dao.PanJinLianResultLibDao;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianGameRunInfo;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianPlayerGameData;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianPlayerGameDataDTO;
import com.jjg.game.slots.game.panJinLian.data.PanJinLianResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractPanJinLianGameManager extends AbstractSlotsGameManager<PanJinLianPlayerGameData, PanJinLianResultLib, PanJinLianGameRunInfo> {
    @Autowired
    private PanJinLianResultLibDao libDao;
    @Autowired
    private PanJinLianGenerateManager generateManager;
    @Autowired
    private PanJinLianGameDataDao gameDataDao;

    public AbstractPanJinLianGameManager() {
        super(PanJinLianPlayerGameData.class, PanJinLianResultLib.class, PanJinLianGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动潘金莲游戏管理器...");
        super.init();
    }

    /**
     * 开始游戏
     */
    @Override
    public PanJinLianGameRunInfo startGame(PlayerController playerController, PanJinLianPlayerGameData playerGameData, long betValue, boolean auto) {
        PanJinLianGameRunInfo gameRunInfo = new PanJinLianGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            // 玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(player.getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            // 当前状态
            int status = playerGameData.getStatus();
            if (status == PanJinLianConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == PanJinLianConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误。playerId={}, gameType={}, status={}", player.getId(), player.getGameType(), status);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            // 从大奖池结算
            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            // 触发赢钱任务
            triggerWinTask(player, gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            // 计算大赢展示
            int times = calWinTimes(gameRunInfo, playerGameData);
            log.debug("计算中奖倍数 times={}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            // 自动模式不跑马灯
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
    protected PanJinLianGameRunInfo normal(PanJinLianGameRunInfo gameRunInfo, PanJinLianPlayerGameData playerGameData, long betValue, PanJinLianResultLib resultLib) {
        // 是否触发免费模式
        if (resultLib.getLibTypeSet().contains(PanJinLianConstant.SpecialMode.FREE)) {
            playerGameData.setStatus(PanJinLianConstant.Status.FREE);
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

            int addCount = allCount - againFreeCount;
            playerGameData.setRemainFreeCount(new AtomicInteger(addCount));

            long times = generateManager.calLineTimes(resultLib.getAwardLineInfoList());
            playerGameData.setFreeLib(resultLib);
            gameRunInfo.addBigPoolTimes(times);

            log.debug("触发免费模式。playerId={}, libId={}, status={}, addFreeCount={}, times={}",
                    playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), addCount, times);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        log.debug("本次结果库 id={}", resultLib.getId());

        // 检查小奖池中奖
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(PanJinLianConstant.Status.NORMAL);
        return gameRunInfo;
    }

    /**
     * 免费游戏
     */
    public PanJinLianGameRunInfo free(PanJinLianGameRunInfo gameRunInfo, PanJinLianPlayerGameData playerGameData) {
        CommonResult<PanJinLianResultLib> libResult = freeGetLib(playerGameData, PanJinLianConstant.SpecialMode.FREE);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        // 扣减免费次数
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        PanJinLianResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
            log.debug("增加免费次数。addFreeCount={}, afterCount={}", freeGame.getAddFreeCount(), afterCount);
        }

        // 累计免费模式中奖
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(PanJinLianConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("免费次数结束，回归普通状态。playerId={}, roomCfgId={}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(PanJinLianConstant.Status.FREE);
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.PAN_JIN_LIAN;
    }

    @Override
    protected PanJinLianResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected PanJinLianGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected PanJinLianGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return PanJinLianPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭潘金莲游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected void onAutoExitAction(PanJinLianPlayerGameData playerGameData, int eventId) {
//        if (playerGameData.getStatus() == PanJinLianConstant.Status.FREE) {
//            int forCount = playerGameData.getRemainFreeCount().get();
//            while (forCount > 0) {
//                autoStartGame(playerGameData, playerGameData.getAllBetScore());
//                forCount = playerGameData.getRemainFreeCount().get();
//            }
//        }
    }

    /**
     * 自动游戏
     */
    public PanJinLianGameRunInfo autoStartGame(PanJinLianPlayerGameData playerGameData, long betValue) {
        log.debug("系统开始自动游戏 playerId={}", playerGameData.playerId());
        return startGame(null, playerGameData, betValue, true);
    }
}
