package com.jjg.game.slots.game.tigerbringsriches.manager;

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
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesGameDataDao;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesPlayerGameDataDTO;
import com.jjg.game.slots.game.tigerbringsriches.dao.TigerBringsRichesResultLibDao;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesAwardLineInfo;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesGameRunInfo;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesPlayerGameData;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesResultLib;
import com.jjg.game.slots.game.tigerbringsriches.pb.bean.TigerBringsRichesWinIconInfo;
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
public abstract class AbstractTigerBringsRichesGameManager extends AbstractSlotsGameManager<TigerBringsRichesPlayerGameData, TigerBringsRichesResultLib> {
    private final TigerBringsRichesGameGenerateManager gameGenerateManager;
    private final TigerBringsRichesGameDataDao gameDataDao;
    private final TigerBringsRichesResultLibDao TigerBringsRichesResultLibDao;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;

    public AbstractTigerBringsRichesGameManager(TigerBringsRichesGameGenerateManager gameGenerateManager,
                                                TigerBringsRichesGameDataDao gameDataDao, TigerBringsRichesResultLibDao TigerBringsRichesResultLibDao) {
        super(TigerBringsRichesPlayerGameData.class, TigerBringsRichesResultLib.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.TigerBringsRichesResultLibDao = TigerBringsRichesResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动虎虎生财游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }
    
    @Override
    public TigerBringsRichesGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        TigerBringsRichesPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new TigerBringsRichesGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        TigerBringsRichesGameRunInfo gameRunInfo = new TigerBringsRichesGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     */
    public TigerBringsRichesGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        TigerBringsRichesPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new TigerBringsRichesGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    @Override
    protected void onAutoExitAction(TigerBringsRichesPlayerGameData gameData, int eventId) {
        if (gameData.getStatus() == TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES) {
            TigerBringsRichesResultLib resultLib = gameData.getSpecialLib();
            for (int i = gameData.getCurrentRandomIndex(); i < resultLib.getSpecialResult().size(); i++) {
                log.info("虎虎生财自动旋转 playerId = {},currentRandomIndex = {}", gameData.playerId(), gameData.getCurrentRandomIndex());
                startGame(new PlayerController(null, null), gameData, gameData.getOneBetScore(), true);
            }
        }
    }

    /**
     * 将库里面的中将线信息转化为消息
     *
     * @param infoList
     * @param oneBetScore 单线押分值
     * @return
     */
    private List<TigerBringsRichesWinIconInfo> transAwardLinePbInfo(List<TigerBringsRichesAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }
        List<TigerBringsRichesWinIconInfo> list = new ArrayList<>(infoList.size());
        for (TigerBringsRichesAwardLineInfo lineInfo : infoList) {
            TigerBringsRichesWinIconInfo resultLineInfo = new TigerBringsRichesWinIconInfo();
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
    public TigerBringsRichesGameRunInfo startGame(PlayerController playerController, TigerBringsRichesPlayerGameData playerGameData, long betValue, boolean auto) {
        TigerBringsRichesGameRunInfo gameRunInfo = new TigerBringsRichesGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == TigerBringsRichesConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES) {
                specialMode(gameRunInfo, playerGameData, betValue);
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

    private void specialMode(TigerBringsRichesGameRunInfo gameRunInfo, TigerBringsRichesPlayerGameData playerGameData, long betValue) {
        TigerBringsRichesResultLib fuMaResultLib = playerGameData.getSpecialLib();
        if (fuMaResultLib == null || CollectionUtil.isEmpty(fuMaResultLib.getSpecialResult())) {
            playerGameData.setStatus(TigerBringsRichesConstant.Status.NORMAL);
            playerGameData.setSpecialLib(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        int currentRandomIndex = playerGameData.getCurrentRandomIndex();
        List<TigerBringsRichesResultLib> randomResult = fuMaResultLib.getSpecialResult();
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
            playerGameData.setStatus(TigerBringsRichesConstant.Status.NORMAL);
            playerGameData.setSpecialLib(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setBigShowId(fuMaResultLib.getJackpotId());
            gameRunInfo.setSpecialModeEnd(true);
            gameRunInfo.setBigPoolTimes(fuMaResultLib.getTimes());
            gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(fuMaResultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
            gameRunInfo.setIconArr(fuMaResultLib.getIconArr());
            gameRunInfo.setStatus(TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES);
            return;
        }
        TigerBringsRichesResultLib resultLib = randomResult.get(currentRandomIndex);
        playerGameData.setCurrentRandomIndex(currentRandomIndex + 1);
        gameRunInfo.setStatus(TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES);
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
    }

    /**
     * 普通正常流程
     *
     */
    protected void normal(TigerBringsRichesGameRunInfo gameRunInfo, TigerBringsRichesPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<TigerBringsRichesResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, TigerBringsRichesConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        TigerBringsRichesResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        Set<Integer> typeSet = resultLib.getLibTypeSet();
        //检查是否触发假福马
        if (gameGenerateManager.getModelRandom() != null) {
            if (typeSet != null && typeSet.size() == 1 && typeSet.contains(gameGenerateManager.getModelRandom().getFirst())) {
                if (RandomUtil.randomInt(10000) < gameGenerateManager.getModelRandom().getSecond()) {
                    gameRunInfo.setStatus(TigerBringsRichesConstant.Status.FAKE_TIGER_BRINGS_RICHES);
                }
            }
        }
        if (typeSet != null && !typeSet.contains(TigerBringsRichesConstant.SpecialMode.NORMAL)) {
            gameRunInfo.setStatus(TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES);
            playerGameData.setStatus(TigerBringsRichesConstant.Status.REAL_TIGER_BRINGS_RICHES);
            playerGameData.setSpecialLib(resultLib);
            gameRunInfo.setScrollType(resultLib.getRollerMode());
            log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
            specialMode(gameRunInfo, playerGameData, betValue);
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
    public TigerBringsRichesGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        TigerBringsRichesGameRunInfo gameRunInfo = new TigerBringsRichesGameRunInfo(Code.SUCCESS, playerController.playerId());
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
        return CoreConst.GameType.TIGER_BRINGS_RICHES;
    }

    @Override
    protected TigerBringsRichesResultLibDao getResultLibDao() {
        return this.TigerBringsRichesResultLibDao;
    }

    @Override
    protected TigerBringsRichesGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected TigerBringsRichesGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<TigerBringsRichesPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return TigerBringsRichesPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭虎虎生财游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
