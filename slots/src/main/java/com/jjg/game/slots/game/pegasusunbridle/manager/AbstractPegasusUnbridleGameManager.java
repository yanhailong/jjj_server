package com.jjg.game.slots.game.pegasusunbridle.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
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
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleGameDataDao;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridlePlayerGameDataDTO;
import com.jjg.game.slots.game.pegasusunbridle.dao.PegasusUnbridleResultLibDao;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleAwardLineInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleGameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridlePlayerGameData;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.game.pegasusunbridle.pb.bean.PegasusUnbridleWinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class AbstractPegasusUnbridleGameManager extends AbstractSlotsGameManager<PegasusUnbridlePlayerGameData, PegasusUnbridleResultLib> {
    private final PegasusUnbridleGameGenerateManager gameGenerateManager;
    private final PegasusUnbridleGameDataDao gameDataDao;
    private final PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;

    public AbstractPegasusUnbridleGameManager(PegasusUnbridleGameGenerateManager gameGenerateManager,
                                              PegasusUnbridleGameDataDao gameDataDao, PegasusUnbridleResultLibDao PegasusUnbridleResultLibDao) {
        super(PegasusUnbridlePlayerGameData.class, PegasusUnbridleResultLib.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.PegasusUnbridleResultLibDao = PegasusUnbridleResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动神马飞扬游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }

    @Override
    public PegasusUnbridleGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        PegasusUnbridlePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new PegasusUnbridleGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     */
    public PegasusUnbridleGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        PegasusUnbridlePlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new PegasusUnbridleGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<PegasusUnbridleWinIconInfo> transAwardLinePbInfo(List<PegasusUnbridleAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }
        List<PegasusUnbridleWinIconInfo> list = new ArrayList<>(infoList.size());
        for (PegasusUnbridleAwardLineInfo lineInfo : infoList) {
            PegasusUnbridleWinIconInfo resultLineInfo = new PegasusUnbridleWinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexes = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 开始游戏
     *
     */
    public PegasusUnbridleGameRunInfo startGame(PlayerController playerController, PegasusUnbridlePlayerGameData playerGameData, long betValue, boolean auto) {
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == PegasusUnbridleConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == PegasusUnbridleConstant.Status.REAL_FU_MA) {
                fuMa(gameRunInfo, playerGameData, betValue);
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
        } catch (
                Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    private void fuMa(PegasusUnbridleGameRunInfo gameRunInfo, PegasusUnbridlePlayerGameData playerGameData, long betValue) {
        PegasusUnbridleResultLib fuMaResultLib = playerGameData.getFuMa();
        if (fuMaResultLib == null || CollectionUtil.isEmpty(fuMaResultLib.getRandomResult())) {
            playerGameData.setStatus(PegasusUnbridleConstant.Status.NORMAL);
            playerGameData.setFuMa(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        int currentRandomIndex = playerGameData.getCurrentRandomIndex();
        List<PegasusUnbridleResultLib> randomResult = fuMaResultLib.getRandomResult();
        if (currentRandomIndex >= randomResult.size()) {
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        gameRunInfo.setScrollType(fuMaResultLib.getRollerMode());
        if (currentRandomIndex == randomResult.size() - 1) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(fuMaResultLib.getJackpotId());
            if (poolCfg != null) {
                //检查是否中大奖
                CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(),
                        poolCfg.getTruePool(), AddType.SLOTS_JACKPOT_REWARD);
                if (result.success()) {
                    gameRunInfo.addSmallPoolGold(result.data);
                }
            }
            //最后一次
            playerGameData.setStatus(PegasusUnbridleConstant.Status.NORMAL);
            playerGameData.setFuMa(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setBigShowId(fuMaResultLib.getJackpotId());
            gameRunInfo.setFuMaEnd(true);
            gameRunInfo.setBigPoolTimes(fuMaResultLib.getTimes());
            gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(fuMaResultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
            gameRunInfo.setIconArr(fuMaResultLib.getIconArr());
            gameRunInfo.setStatus(PegasusUnbridleConstant.Status.REAL_FU_MA);
            return;
        }
        PegasusUnbridleResultLib resultLib = randomResult.get(currentRandomIndex);
        playerGameData.setCurrentRandomIndex(currentRandomIndex + 1);
        gameRunInfo.setStatus(PegasusUnbridleConstant.Status.REAL_FU_MA);
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
    }

    /**
     * 普通正常流程
     *
     */
    protected void normal(PegasusUnbridleGameRunInfo gameRunInfo, PegasusUnbridlePlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<PegasusUnbridleResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, PegasusUnbridleConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        PegasusUnbridleResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        Set<Integer> typeSet = resultLib.getLibTypeSet();
        //检查是否触发假福马
        if (gameGenerateManager.getModelRandom() != null) {
            if (typeSet != null && typeSet.size() == 1 && typeSet.contains(gameGenerateManager.getModelRandom().getFirst())) {
                if (RandomUtil.randomInt(10000) < gameGenerateManager.getModelRandom().getSecond()) {
                    gameRunInfo.setStatus(PegasusUnbridleConstant.Status.FAKE_FU_MA);
                }
            }
        }
        if (typeSet != null && !typeSet.contains(PegasusUnbridleConstant.SpecialMode.NORMAL)) {
            gameRunInfo.setStatus(PegasusUnbridleConstant.Status.REAL_FU_MA);
            playerGameData.setStatus(PegasusUnbridleConstant.Status.REAL_FU_MA);
            playerGameData.setFuMa(resultLib);
            gameRunInfo.setScrollType(resultLib.getRollerMode());
            log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
            fuMa(gameRunInfo, playerGameData, betValue);
        } else {
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
            gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
            gameRunInfo.setIconArr(resultLib.getIconArr());
            gameRunInfo.setResultLib(resultLib);
            gameRunInfo.setStake(betValue);
        }
    }

    /**
     * 获取奖池信息
     *
     * @param playerController
     * @param stake
     * @param
     * @return
     */
    public PegasusUnbridleGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        PegasusUnbridleGameRunInfo gameRunInfo = new PegasusUnbridleGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMajor(getPoolValueByRoomCfgId(playerController.getPlayer().getRoomCfgId()));
            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.PEGASUS_UNBRIDLE;
    }

    @Override
    protected void offlineSaveGameDataDto(PegasusUnbridlePlayerGameData gameData) {
        try {
            PegasusUnbridlePlayerGameDataDTO dto = gameData.converToDto(PegasusUnbridlePlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    protected PegasusUnbridleResultLibDao getResultLibDao() {
        return this.PegasusUnbridleResultLibDao;
    }

    @Override
    protected PegasusUnbridleGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected PegasusUnbridleGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭PEGASUS_UNBRIDLE游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
