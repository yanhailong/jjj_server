package com.jjg.game.slots.game.luckymouse.manager;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.game.christmasBashNight.ChristmasBashNightConstant;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;
import com.jjg.game.slots.game.luckymouse.dao.LuckyMouseGameDataDao;
import com.jjg.game.slots.game.luckymouse.dao.LuckyMouseResultLibDao;
import com.jjg.game.slots.game.luckymouse.data.*;
import com.jjg.game.slots.game.luckymouse.pb.LuckyMouseWinIconInfo;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLuckyMouseGameManager extends AbstractSlotsGameManager<LuckyMousePlayerGameData, LuckyMouseResultLib, LuckyMouseGameRunInfo> {
    @Autowired
    private LuckyMouseResultLibDao libDao;
    @Autowired
    private LuckyMouseGenerateManager gameGenerateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private LuckyMouseGameDataDao gameDataDao;

    private int fake_fu_shu_prop = 0;

    public AbstractLuckyMouseGameManager() {
        super(LuckyMousePlayerGameData.class, LuckyMouseResultLib.class, LuckyMouseGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动鼠鼠福福游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }

    @Override
    public LuckyMouseGameRunInfo enterGame(PlayerController playerController) {
        LuckyMousePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new LuckyMouseGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        resetFreeStateIfInvalid(playerGameData, LuckyMouseConstant.Status.REAL_FU_SHU, LuckyMouseConstant.Status.NORMAL, "幸运小鼠");

        LuckyMouseGameRunInfo gameRunInfo = new LuckyMouseGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }


    @Override
    protected LuckyMouseGameRunInfo startGame(PlayerController playerController, LuckyMousePlayerGameData playerGameData, long stake, boolean auto) {
        LuckyMouseGameRunInfo gameRunInfo = new LuckyMouseGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));
            int status = playerGameData.getStatus();
            if (status == LuckyMouseConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, stake);
            } else if (status == LuckyMouseConstant.Status.REAL_FU_SHU) {
                free(gameRunInfo, playerGameData, LuckyMouseConstant.SpecialMode.FREE);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), stake, warehouseCfg.getTransactionItemId());
            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, stake);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
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

    @Override
    public LuckyMouseGameRunInfo normal(LuckyMouseGameRunInfo gameRunInfo, LuckyMousePlayerGameData playerGameData, long betValue, LuckyMouseResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        if (resultLib.getLibTypeSet().contains(LuckyMouseConstant.SpecialMode.FREE)) {  //是否会触发二选一
            if (CollUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames().size()));
                playerGameData.setStatus(LuckyMouseConstant.Status.REAL_FU_SHU);
                gameRunInfo.setStatus(LuckyMouseConstant.Status.REAL_FU_SHU);
                playerGameData.setFreeLib(resultLib);
            } else {
                log.warn("福鼠的免费模式没有免费次数 gameType = {}, libId = {}，检查配置！", this.gameType, resultLib.getId());
                gameRunInfo.setStatus(LuckyMouseConstant.Status.FAKE_FU_SHU);
            }
            log.debug("触发真福鼠  playerId = {},libId = {},status = {}, freeGamesList = {}"
                    , playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus(), resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames());
        } else {
            // 随机触发假福鼠
            if (SlotsUtil.calProp(this.fake_fu_shu_prop)) {
                gameRunInfo.setStatus(LuckyMouseConstant.Status.FAKE_FU_SHU);
                log.debug("触发假福鼠  playerId = {},libId = {},status = {}", playerGameData.playerId(), resultLib.getId(), playerGameData.getStatus());
            } else {
                gameRunInfo.setStatus(playerGameData.getStatus());
            }
        }
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        }

        // 检查是否中大奖
        if (resultLib.getJackpotId() > 0) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(resultLib.getJackpotId());
            //检查是否中大奖
            CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(),
                    poolCfg.getTruePool(), AddType.SLOTS_JACKPOT_REWARD);
            if (result.success()) {
                gameRunInfo.addSmallPoolGold(result.data);
            }
        }
        gameRunInfo.setRemainFreeCount(playerGameData.getRemainFreeCount().get());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    /**
     * 免费模式
     *
     * @param gameRunInfo
     * @param playerGameData
     */
    protected void free(LuckyMouseGameRunInfo gameRunInfo, LuckyMousePlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<LuckyMouseResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        LuckyMouseResultLib freeGame = libResult.data;
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(LuckyMouseConstant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("福鼠游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.playerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(LuckyMouseConstant.Status.REAL_FU_SHU);
    }

    protected List<LuckyMouseWinIconInfo> transAwardLinePbInfo(List<LuckyMouseAwardLineInfo> infoList, long oneBetScore) {
        if (CollUtil.isEmpty(infoList)) {
            return null;
        }
        List<LuckyMouseWinIconInfo> list = new ArrayList<>(infoList.size());
        for (LuckyMouseAwardLineInfo lineInfo : infoList) {
            LuckyMouseWinIconInfo resultLineInfo = new LuckyMouseWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    @Override
    protected void specialPlayConfig() {
        //随机触发假免费
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(LuckyMouseConstant.SpecialPlay.FU_SHU_TRIGGER_ID);
        if (specialPlayCfg == null || StringUtils.isBlank(specialPlayCfg.getValue())) {
            return;
        }

        this.fake_fu_shu_prop = Integer.parseInt(specialPlayCfg.getValue().split(",")[1]);
    }

    /**
     * 获取奖池信息
     *
     * @param playerController
     * @param stake
     * @param
     * @return
     */
    public LuckyMouseGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        LuckyMouseGameRunInfo gameRunInfo = new LuckyMouseGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMajor(getPoolValueByRoomCfgId(playerController.getPlayer().getRoomCfgId()));
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(LuckyMousePlayerGameData gameData, int eventId) {
//        if (gameData.getStatus() == LuckyMouseConstant.Status.REAL_FU_SHU) {
//            freeStateAction(gameData, (playerGameData) ->
//                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
//        }
    }

    @Override
    protected LuckyMouseResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected LuckyMouseGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected LuckyMouseGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected Class<LuckyMousePlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return LuckyMousePlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.LUCKY_MOUSE;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭鼠鼠福福游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
