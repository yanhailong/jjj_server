package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

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
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullGameDataDao;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullPlayerGameDataDTO;
import com.jjg.game.slots.game.tenfoldgoldenbull.dao.TenFoldGoldenBullResultLibDao;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullAwardLineInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullGameRunInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullPlayerGameData;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import com.jjg.game.slots.game.tenfoldgoldenbull.pb.bean.TenFoldGoldenBullWinIconInfo;
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
public class AbstractTenFoldGoldenBullGameManager extends AbstractSlotsGameManager<TenFoldGoldenBullPlayerGameData, TenFoldGoldenBullResultLib> {
    private final TenFoldGoldenBullGameGenerateManager gameGenerateManager;
    private final TenFoldGoldenBullGameDataDao gameDataDao;
    private final TenFoldGoldenBullResultLibDao TenFoldGoldenBullResultLibDao;
    @Autowired
    protected SlotsPoolDao slotsPoolDao;

    public AbstractTenFoldGoldenBullGameManager(TenFoldGoldenBullGameGenerateManager gameGenerateManager,
                                                TenFoldGoldenBullGameDataDao gameDataDao, TenFoldGoldenBullResultLibDao TenFoldGoldenBullResultLibDao) {
        super(TenFoldGoldenBullPlayerGameData.class, TenFoldGoldenBullResultLib.class);
        this.gameGenerateManager = gameGenerateManager;
        this.gameDataDao = gameDataDao;
        this.TenFoldGoldenBullResultLibDao = TenFoldGoldenBullResultLibDao;
    }


    @Override
    public void init() {
        log.info("启动十倍金牛游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }

    @Override
    public TenFoldGoldenBullGameRunInfo enterGame(PlayerController playerController) {
        //获取玩家游戏数据
        TenFoldGoldenBullPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，进入游戏获取获取数据失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new TenFoldGoldenBullGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        TenFoldGoldenBullGameRunInfo gameRunInfo = new TenFoldGoldenBullGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        gameRunInfo.setData(playerGameData);
        return gameRunInfo;
    }

    /**
     * 玩家开始游戏
     *
     */
    public TenFoldGoldenBullGameRunInfo playerStartGame(PlayerController playerController, long stake) {
        //获取玩家游戏数据
        TenFoldGoldenBullPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new TenFoldGoldenBullGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, stake, false);
    }

    @Override
    protected void onAutoExitAction(TenFoldGoldenBullPlayerGameData gameData, int eventId) {
        if (gameData.getStatus() == TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL) {
            TenFoldGoldenBullResultLib resultLib = gameData.getLuckyBull();
            for (int i = gameData.getCurrentRandomIndex(); i < resultLib.getRandomResult().size(); i++) {
                log.info("福牛模式自动旋转 playerId = {},currentRandomIndex = {}", gameData.playerId(), gameData.getCurrentRandomIndex());
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
    private List<TenFoldGoldenBullWinIconInfo> transAwardLinePbInfo(List<TenFoldGoldenBullAwardLineInfo> infoList, long oneBetScore) {
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }
        List<TenFoldGoldenBullWinIconInfo> list = new ArrayList<>(infoList.size());
        for (TenFoldGoldenBullAwardLineInfo lineInfo : infoList) {
            TenFoldGoldenBullWinIconInfo resultLineInfo = new TenFoldGoldenBullWinIconInfo();
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
    public TenFoldGoldenBullGameRunInfo startGame(PlayerController playerController, TenFoldGoldenBullPlayerGameData playerGameData, long betValue, boolean auto) {
        TenFoldGoldenBullGameRunInfo gameRunInfo = new TenFoldGoldenBullGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == TenFoldGoldenBullConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL) {
                luckyBull(gameRunInfo, playerGameData, betValue);
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

    private void luckyBull(TenFoldGoldenBullGameRunInfo gameRunInfo, TenFoldGoldenBullPlayerGameData playerGameData, long betValue) {
        TenFoldGoldenBullResultLib luckyBullResult = playerGameData.getLuckyBull();
        if (luckyBullResult == null || CollectionUtil.isEmpty(luckyBullResult.getRandomResult())) {
            playerGameData.setStatus(TenFoldGoldenBullConstant.Status.NORMAL);
            playerGameData.setLuckyBull(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        int currentRandomIndex = playerGameData.getCurrentRandomIndex();
        List<TenFoldGoldenBullResultLib> randomResult = luckyBullResult.getRandomResult();
        if (currentRandomIndex >= randomResult.size()) {
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        gameRunInfo.setScrollType(luckyBullResult.getRollerMode());
        if (currentRandomIndex == randomResult.size() - 1) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(luckyBullResult.getJackpotId());
            if (poolCfg != null) {
                //检查是否中大奖
                CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(),
                        poolCfg.getTruePool(), AddType.SLOTS_JACKPOT_REWARD);
                if (result.success()) {
                    gameRunInfo.addSmallPoolGold(result.data);
                }
            }
            //最后一次
            playerGameData.setStatus(TenFoldGoldenBullConstant.Status.NORMAL);
            playerGameData.setLuckyBull(null);
            playerGameData.setCurrentRandomIndex(0);
            gameRunInfo.setBigShowId(luckyBullResult.getJackpotId());
            gameRunInfo.setFuMaEnd(true);
            gameRunInfo.setBigPoolTimes(luckyBullResult.getTimes());
            gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(luckyBullResult.getAwardLineInfoList(), playerGameData.getOneBetScore()));
            gameRunInfo.setIconArr(luckyBullResult.getIconArr());
            gameRunInfo.setStatus(TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL);
            return;
        }
        TenFoldGoldenBullResultLib resultLib = randomResult.get(currentRandomIndex);
        playerGameData.setCurrentRandomIndex(currentRandomIndex + 1);
        gameRunInfo.setStatus(TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL);
        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
    }

    /**
     * 普通正常流程
     *
     */
    protected void normal(TenFoldGoldenBullGameRunInfo gameRunInfo, TenFoldGoldenBullPlayerGameData playerGameData, long betValue) {
        CommonResult<Pair<TenFoldGoldenBullResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, TenFoldGoldenBullConstant.SpecialMode.NORMAL);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return;
        }
        TenFoldGoldenBullResultLib resultLib = libResult.data.getFirst();
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        Set<Integer> typeSet = resultLib.getLibTypeSet();
        //检查是否触发假福牛
        if (gameGenerateManager.getModelRandom() != null) {
            if (typeSet != null && typeSet.size() == 1 && typeSet.contains(gameGenerateManager.getModelRandom().getFirst())) {
                if (RandomUtil.randomInt(10000) < gameGenerateManager.getModelRandom().getSecond()) {
                    gameRunInfo.setStatus(TenFoldGoldenBullConstant.Status.FAKE_LUCKY_BULL);
                }
            }
        }
        if (typeSet != null && !typeSet.contains(TenFoldGoldenBullConstant.SpecialMode.NORMAL)) {
            gameRunInfo.setStatus(TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL);
            playerGameData.setStatus(TenFoldGoldenBullConstant.Status.REAL_LUCKY_BULL);
            playerGameData.setLuckyBull(resultLib);
            gameRunInfo.setScrollType(resultLib.getRollerMode());
            log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
            luckyBull(gameRunInfo, playerGameData, betValue);
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
    public TenFoldGoldenBullGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        TenFoldGoldenBullGameRunInfo gameRunInfo = new TenFoldGoldenBullGameRunInfo(Code.SUCCESS, playerController.playerId());
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
        return CoreConst.GameType.TENFOLD_GOLDEN_BULL;
    }

    @Override
    protected TenFoldGoldenBullResultLibDao getResultLibDao() {
        return this.TenFoldGoldenBullResultLibDao;
    }

    @Override
    protected TenFoldGoldenBullGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    protected TenFoldGoldenBullGameDataDao getGameDataDao() {
        return this.gameDataDao;
    }

    @Override
    protected Class<TenFoldGoldenBullPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return TenFoldGoldenBullPlayerGameDataDTO.class;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭十倍金牛游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}
