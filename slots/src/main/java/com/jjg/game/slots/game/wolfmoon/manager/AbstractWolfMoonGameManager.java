package com.jjg.game.slots.game.wolfmoon.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonGameDataDao;
import com.jjg.game.slots.game.wolfmoon.dao.WolfMoonResultLibDao;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonGameRunInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameData;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameDataDTO;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractWolfMoonGameManager extends AbstractSlotsGameManager<WolfMoonPlayerGameData, WolfMoonResultLib, WolfMoonGameRunInfo> {
    @Autowired
    protected WolfMoonResultLibDao libDao;
    @Autowired
    protected WolfMoonGenerateManager generateManager;
    @Autowired
    protected WolfMoonGameDataDao gameDataDao;

    public AbstractWolfMoonGameManager() {
        super(WolfMoonPlayerGameData.class, WolfMoonResultLib.class, WolfMoonGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动狼月游戏管理器...");
        super.init();
    }

    @Override
    public WolfMoonGameRunInfo enterGame(PlayerController playerController) throws Exception {
        WolfMoonGameRunInfo gameRunInfo = super.enterGame(playerController);
        if (!gameRunInfo.success()) {
            return gameRunInfo;
        }

        WolfMoonPlayerGameData data = gameRunInfo.getData();
        resetFreeStateIfInvalid(data, WolfMoonConstant.Status.FREE_HIGH_PAY, WolfMoonConstant.Status.NORMAL, "狼月");
        resetFreeStateIfInvalid(data, WolfMoonConstant.Status.FREE_STACK_WILD, WolfMoonConstant.Status.NORMAL, "狼月");
        resetFreeStateIfInvalid(data, WolfMoonConstant.Status.FREE_MULTIPLIER, WolfMoonConstant.Status.NORMAL, "狼月");

        if (data.getStatus() == WolfMoonConstant.Status.CHOOSE_ONE && data.getFreeLib() != null) {
            // free trigger only keeps status, no free lib should be retained.
            data.setFreeLib(null);
        }
        return gameRunInfo;
    }

    @Override
    protected WolfMoonGameRunInfo startGame(PlayerController playerController, WolfMoonPlayerGameData playerGameData, long betValue, boolean auto) {
        WolfMoonGameRunInfo gameRunInfo = new WolfMoonGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            Player player = slotsPlayerService.get(playerGameData.playerId());
            if (playerController != null) {
                playerController.setPlayer(player);
            }

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerGameData.getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            int status = playerGameData.getStatus();
            if (status == WolfMoonConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == WolfMoonConstant.Status.CHOOSE_ONE) {
                gameRunInfo.setCode(Code.FORBID);
                return gameRunInfo;
            } else if (status == WolfMoonConstant.Status.FREE_HIGH_PAY) {
                gameRunInfo = free(gameRunInfo, playerGameData, WolfMoonConstant.SpecialMode.FREE_HIGH_PAY);
            } else if (status == WolfMoonConstant.Status.FREE_STACK_WILD) {
                gameRunInfo = free(gameRunInfo, playerGameData, WolfMoonConstant.SpecialMode.FREE_STACK_WILD);
            } else if (status == WolfMoonConstant.Status.FREE_MULTIPLIER) {
                gameRunInfo = free(gameRunInfo, playerGameData, WolfMoonConstant.SpecialMode.FREE_MULTIPLIER);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            triggerWinTask(player, gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            player = slotsPlayerService.get(playerGameData.playerId());
            if (playerController != null) {
                playerController.setPlayer(player);
            }
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            int times = calWinTimes(gameRunInfo, playerGameData);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    public WolfMoonGameRunInfo freeChooseOne(PlayerController playerController, int chooseType) {
        WolfMoonPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            return new WolfMoonGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }

        if (playerGameData.getStatus() != WolfMoonConstant.Status.CHOOSE_ONE) {
            return new WolfMoonGameRunInfo(Code.FORBID, playerController.playerId());
        }

        if (chooseType == WolfMoonConstant.FreeChoose.HIGH_PAY) {
            playerGameData.setStatus(WolfMoonConstant.Status.FREE_HIGH_PAY);
        } else if (chooseType == WolfMoonConstant.FreeChoose.STACK_WILD) {
            playerGameData.setStatus(WolfMoonConstant.Status.FREE_STACK_WILD);
        } else if (chooseType == WolfMoonConstant.FreeChoose.MULTIPLIER) {
            playerGameData.setStatus(WolfMoonConstant.Status.FREE_MULTIPLIER);
            playerGameData.setFreeMultiplyValue(generateManager.getFreeMultiplierStart());
        } else {
            return new WolfMoonGameRunInfo(Code.FAIL, playerController.playerId());
        }

        playerGameData.setFreeLib(null);
        playerGameData.setFreeIndex(new AtomicInteger(0));
        playerGameData.setRemainFreeCount(new AtomicInteger(0));

        return new WolfMoonGameRunInfo(Code.SUCCESS, playerController.playerId());
    }

    @Override
    protected WolfMoonGameRunInfo normal(WolfMoonGameRunInfo gameRunInfo, WolfMoonPlayerGameData playerGameData, long betValue, WolfMoonResultLib resultLib) {
        if (resultLib.getLibTypeSet().contains(WolfMoonConstant.SpecialMode.FREE_TRIGGER)) {
            playerGameData.setStatus(WolfMoonConstant.Status.CHOOSE_ONE);
        }

        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setStatus(playerGameData.getStatus());
        gameRunInfo.setFreeMultiple(playerGameData.getFreeMultiplyValue());
        return gameRunInfo;
    }

    protected WolfMoonGameRunInfo free(WolfMoonGameRunInfo gameRunInfo, WolfMoonPlayerGameData playerGameData, int specialModeType) {
        CommonResult<WolfMoonResultLib> libResult = freeGetLib(playerGameData, specialModeType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }

        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);

        WolfMoonResultLib freeGame = libResult.data;
        if (freeGame.getAddFreeCount() > 0) {
            afterCount = playerGameData.getRemainFreeCount().addAndGet(freeGame.getAddFreeCount());
        }

        long spinTimes = freeGame.getTimes();
        if (specialModeType == WolfMoonConstant.SpecialMode.FREE_MULTIPLIER) {
            int curMulti = playerGameData.getFreeMultiplyValue();
            if (curMulti < 1) {
                curMulti = generateManager.getFreeMultiplierStart();
            }
            gameRunInfo.setFreeMultiple(curMulti);
            spinTimes *= curMulti;
            playerGameData.setFreeMultiplyValue(Math.min(generateManager.getFreeMultiplierMax(), curMulti + generateManager.getFreeMultiplierStep()));
        }

        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * spinTimes);

        if (afterCount < 1) {
            playerGameData.setStatus(WolfMoonConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            playerGameData.setFreeMultiplyValue(0);

            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
        }

        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.addBigPoolTimes(spinTimes);
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(playerGameData.getStatus());
        if (specialModeType != WolfMoonConstant.SpecialMode.FREE_MULTIPLIER) {
            gameRunInfo.setFreeMultiple(playerGameData.getFreeMultiplyValue());
        }
        return gameRunInfo;
    }

    @Override
    protected CommonResult<WolfMoonResultLib> freeGetLib(WolfMoonPlayerGameData playerGameData, int specialModeFreeLibType, int specialAuxiliary) {
        CommonResult<WolfMoonResultLib> result = new CommonResult<>(Code.SUCCESS);

        WolfMoonResultLib freeLib = (WolfMoonResultLib) playerGameData.getFreeLib();
        if (freeLib == null) {
            CommonResult<WolfMoonResultLib> libResult = getLibFromDB(playerGameData, specialModeFreeLibType);
            if (!libResult.success()) {
                result.code = libResult.code;
                return result;
            }
            freeLib = libResult.data;
        }

        if (freeLib == null || freeLib.getSpecialAuxiliaryInfoList() == null || freeLib.getSpecialAuxiliaryInfoList().isEmpty()) {
            result.code = Code.NOT_FOUND;
            return result;
        }

        SpecialAuxiliaryInfo specialAuxiliaryInfo = null;
        for (Object obj : freeLib.getSpecialAuxiliaryInfoList()) {
            SpecialAuxiliaryInfo tmpInfo = (SpecialAuxiliaryInfo) obj;
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(tmpInfo.getCfgId());
            if (specialAuxiliary > 0 && specialAuxiliaryCfg.getType() != specialAuxiliary) {
                continue;
            }
            if (tmpInfo.getFreeGames() == null || tmpInfo.getFreeGames().isEmpty()) {
                continue;
            }
            specialAuxiliaryInfo = tmpInfo;
            break;
        }

        if (specialAuxiliaryInfo == null || specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
            result.code = Code.NOT_FOUND;
            return result;
        }

        int index = playerGameData.getFreeIndex().getAndAdd(1);
        if (index < 0 || index >= specialAuxiliaryInfo.getFreeGames().size()) {
            playerGameData.setFreeLib(null);
            result.code = Code.NOT_FOUND;
            return result;
        }

        JSONObject jsonObject = specialAuxiliaryInfo.getFreeGames().get(index);
        WolfMoonResultLib freeGame = JSON.parseObject(jsonObject.toJSONString(), this.libClass);
        if (freeGame == null) {
            playerGameData.setFreeLib(null);
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (index < 1) {
            int allCount = specialAuxiliaryInfo.getFreeGames().size();
            int addCount = 0;
            for (JSONObject json : specialAuxiliaryInfo.getFreeGames()) {
                Integer tmpAddCount = json.getInteger("addFreeCount");
                if (tmpAddCount != null && tmpAddCount > 0) {
                    addCount += tmpAddCount;
                }
            }
            playerGameData.setRemainFreeCount(new AtomicInteger(Math.max(0, allCount - addCount)));
        }

        playerGameData.setFreeLib(freeLib);
        result.data = freeGame;
        return result;
    }

    @Override
    protected void onAutoExitAction(WolfMoonPlayerGameData gameData, int eventId) {
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.WOLF_MOON;
    }

    @Override
    protected WolfMoonResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected WolfMoonGenerateManager getGenerateManager() {
        return this.generateManager;
    }

    @Override
    protected WolfMoonGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return WolfMoonPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭狼月游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
