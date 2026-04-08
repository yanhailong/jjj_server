package com.jjg.game.slots.game.garaGemstone1.manager;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
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
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.garaGemstone1.GaraGemstone1Constant;
import com.jjg.game.slots.game.garaGemstone1.dao.GaraGemstone1GameDataDao;
import com.jjg.game.slots.game.garaGemstone1.dao.GaraGemstone1ResultLibDao;
import com.jjg.game.slots.game.garaGemstone1.data.*;
import com.jjg.game.slots.game.garaGemstone1.pb.GaraGemstone1WinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractGaraGemstone1GameManager extends AbstractSlotsGameManager<GaraGemstone1PlayerGameData, GaraGemstone1ResultLib, GaraGemstone1GameRunInfo> {
    @Autowired
    private GaraGemstone1ResultLibDao libDao;
    @Autowired
    private GaraGemstone1GenerateManager gameGenerateManager;
    @Autowired
    private SlotsPoolDao slotsPoolDao;
    @Autowired
    private GaraGemstone1GameDataDao gameDataDao;

    private int fake_fu_shu_prop = 0;

    public AbstractGaraGemstone1GameManager() {
        super(GaraGemstone1PlayerGameData.class, GaraGemstone1ResultLib.class, GaraGemstone1GameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动鼠鼠福福游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }


    @Override
    protected GaraGemstone1GameRunInfo startGame(PlayerController playerController, GaraGemstone1PlayerGameData playerGameData, long stake, boolean auto) {
        GaraGemstone1GameRunInfo gameRunInfo = new GaraGemstone1GameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));
            int status = playerGameData.getStatus();
            if (status == GaraGemstone1Constant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, stake);
            } else if (status == GaraGemstone1Constant.Status.REAL_FU_SHU) {
                free(gameRunInfo, playerGameData, GaraGemstone1Constant.SpecialMode.FREE);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
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
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    public GaraGemstone1GameRunInfo normal(GaraGemstone1GameRunInfo gameRunInfo, GaraGemstone1PlayerGameData playerGameData, long betValue, GaraGemstone1ResultLib resultLib) {
        //根据结果库类型不同，从不同地方获取icon
        long addTimes = resultLib.getTimes();
        if (resultLib.getLibTypeSet().contains(GaraGemstone1Constant.SpecialMode.FREE)) {  //是否会触发二选一
            if (CollUtil.isNotEmpty(resultLib.getSpecialAuxiliaryInfoList())) {
                playerGameData.setRemainFreeCount(new AtomicInteger(resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames().size()));
                playerGameData.setStatus(GaraGemstone1Constant.Status.REAL_FU_SHU);
                gameRunInfo.setStatus(GaraGemstone1Constant.Status.REAL_FU_SHU);
                playerGameData.setFreeLib(resultLib);
            } else {
                log.warn("福鼠的免费模式没有免费次数 gameType = {}, libId = {}，检查配置！", this.gameType, resultLib.getId());
                gameRunInfo.setStatus(GaraGemstone1Constant.Status.FAKE_FU_SHU);
            }
            //触发局不能将所有的钱加到玩家身上
            addTimes = 0;
            log.debug("触发真福鼠  playerId = {},libId = {},status = {}, freeGamesList = {}"
                    , playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus(), resultLib.getSpecialAuxiliaryInfoList().getFirst().getFreeGames());
        } else {
            // 随机触发假福鼠
            if (SlotsUtil.calProp(this.fake_fu_shu_prop)) {
                gameRunInfo.setStatus(GaraGemstone1Constant.Status.FAKE_FU_SHU);
                log.debug("触发假福鼠  playerId = {},libId = {},status = {}", playerGameData.getPlayerId(), resultLib.getId(), playerGameData.getStatus());
            } else {
                gameRunInfo.setStatus(playerGameData.getStatus());
            }
        }
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(addTimes);
        }

        // 检查是否中大奖（散花触发的jackpot）
        if (resultLib.getJackpotId() > 0) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(resultLib.getJackpotId());
            //检查是否中大奖
            CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(),
                    poolCfg.getTruePool(), poolCfg.getId(), AddType.SLOTS_JACKPOT_REWARD);
            if (result.success()) {
                gameRunInfo.addSmallPoolGold(result.data);
            }
        }
        // 检查第四轴（倍数轴）奖金符号触发的奖池
        if (resultLib.getAxisJackpotId() > 0) {
            PoolCfg axisPoolCfg = GameDataManager.getPoolCfg(resultLib.getAxisJackpotId());
            if (axisPoolCfg != null) {
                CommonResult<Long> axisResult = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(),
                        axisPoolCfg.getTruePool(), axisPoolCfg.getId(), AddType.SLOTS_JACKPOT_REWARD);
                if (axisResult.success()) {
                    gameRunInfo.addSmallPoolGold(axisResult.data);
                    log.debug("触发倍数轴奖金符号奖池 playerId={} poolId={} reward={}", playerGameData.getPlayerId(), resultLib.getAxisJackpotId(), axisResult.data);
                }
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
    protected void free(GaraGemstone1GameRunInfo gameRunInfo, GaraGemstone1PlayerGameData playerGameData, int specialModeFreeLibType) {
        CommonResult<GaraGemstone1ResultLib> libResult = freeGetLib(playerGameData, specialModeFreeLibType);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        GaraGemstone1ResultLib freeGame = libResult.data;
        int afterCount = playerGameData.getRemainFreeCount().addAndGet(-1);
        //累计免费模式的中奖金额
        playerGameData.addFreeAllWin(playerGameData.getOneBetScore() * freeGame.getTimes());

        if (afterCount < 1) {
            playerGameData.setStatus(GaraGemstone1Constant.Status.NORMAL);
            playerGameData.setFreeLib(null);
            playerGameData.getFreeIndex().set(0);
            gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
            playerGameData.setFreeAllWin(0);
            log.debug("福鼠游戏次数结束，回归正常状态 playerId = {},roomCfgId = {}", playerGameData.getPlayerId(), playerGameData.getRoomCfgId());
        }
        gameRunInfo.setFreeModeTotalReward(playerGameData.getFreeAllWin());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(freeGame.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setIconArr(freeGame.getIconArr());
        gameRunInfo.setResultLib(freeGame);
        gameRunInfo.setBigPoolTimes(freeGame.getTimes());
        gameRunInfo.setRemainFreeCount(afterCount);
        gameRunInfo.setStatus(GaraGemstone1Constant.Status.REAL_FU_SHU);
        // 检查免费局中第四轴奖金符号触发的奖池
        if (freeGame.getAxisJackpotId() > 0) {
            PoolCfg axisPoolCfg = GameDataManager.getPoolCfg(freeGame.getAxisJackpotId());
            if (axisPoolCfg != null) {
                CommonResult<Long> axisResult = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(),
                        axisPoolCfg.getTruePool(), axisPoolCfg.getId(), AddType.SLOTS_JACKPOT_REWARD);
                if (axisResult.success()) {
                    gameRunInfo.addSmallPoolGold(axisResult.data);
                    log.debug("免费局触发倍数轴奖金符号奖池 playerId={} poolId={} reward={}", playerGameData.getPlayerId(), freeGame.getAxisJackpotId(), axisResult.data);
                }
            }
        }
    }

    protected List<GaraGemstone1WinIconInfo> transAwardLinePbInfo(List<GaraGemstone1AwardLineInfo> infoList, long oneBetScore) {
        if (CollUtil.isEmpty(infoList)) {
            return null;
        }
        List<GaraGemstone1WinIconInfo> list = new ArrayList<>(infoList.size());
        for (GaraGemstone1AwardLineInfo lineInfo : infoList) {
            GaraGemstone1WinIconInfo resultLineInfo = new GaraGemstone1WinIconInfo();
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
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(GaraGemstone1Constant.SpecialPlay.FU_SHU_TRIGGER_ID);
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
    public GaraGemstone1GameRunInfo getPoolValue(PlayerController playerController, long stake) {
        GaraGemstone1GameRunInfo gameRunInfo = new GaraGemstone1GameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMajor(getPoolValueByRoomCfgId(playerController.getPlayer().getRoomCfgId()));
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected void onAutoExitAction(GaraGemstone1PlayerGameData gameData, int eventId) {
//        if (gameData.getStatus() == GaraGemstone1Constant.Status.REAL_FU_SHU) {
//            freeStateAction(gameData, (playerGameData) ->
//                    startGame(new PlayerController(null, null), playerGameData, playerGameData.getAllBetScore(), true));
//        }
    }

    @Override
    protected GaraGemstone1ResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected GaraGemstone1GameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected GaraGemstone1GenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return GaraGemstone1PlayerGameDataDTO.class;
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
