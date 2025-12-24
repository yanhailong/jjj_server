package com.jjg.game.slots.game.superstar.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.dao.SuperStarGameDataDao;
import com.jjg.game.slots.game.superstar.dao.SuperStarResultLibDao;
import com.jjg.game.slots.game.superstar.data.*;
import com.jjg.game.slots.game.superstar.pb.SuperStarResultLineInfo;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

public class AbstractSuperStarGameManager extends AbstractSlotsGameManager<SuperStarPlayerGameData, SuperStarResultLib> {

    @Autowired
    protected SuperStarResultLibDao superStarResultLibDao;
    @Autowired
    protected SuperStarGameDataDao superStarGameDataDao;
    @Autowired
    protected SuperStarGenerateManager superStarGenerateManager;
    @Autowired
    protected SuperStarGameDataDao gameDataDao;

    public AbstractSuperStarGameManager() {
        super(SuperStarPlayerGameData.class, SuperStarResultLib.class);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init() {
        log.info("SuperStarGameManager");
        super.init();
//        Map<Integer, Integer> countMap = new HashMap<>();
//        countMap.put(1, 5000);
//        countMap.put(2, 5000);
//        countMap.put(3, 5000);
//        addGenerateLibEvent(countMap);
    }

    @Override
    protected SuperStarResultLibDao getResultLibDao() {
        return superStarResultLibDao;
    }

    @Override
    protected SuperStarGameDataDao getGameDataDao() {
        return superStarGameDataDao;
    }

    @Override
    protected SuperStarGenerateManager getGenerateManager() {
        return superStarGenerateManager;
    }

    /**
     * 玩家离线保存gameDataDto
     *
     * @param gameData
     */
    @Override
    protected void offlineSaveGameDataDto(SuperStarPlayerGameData gameData) {
        try {
            SuperStarPlayerGameDataDTO dto = gameData.converToDto(SuperStarPlayerGameDataDTO.class);
            gameDataDao.saveGameData(dto);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.SUPER_STAR;
    }

    /**
     * 开始游戏
     */
    public SuperStarGameRunInfo playerStartGame(PlayerController playerController, long betValue) {
        //获取玩家游戏数据
        SuperStarPlayerGameData playerGameData = getPlayerGameData(playerController);
        if (playerGameData == null) {
            log.debug("获取玩家游戏数据失败，开始游戏失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            return new SuperStarGameRunInfo(Code.NOT_FOUND, playerController.playerId());
        }
        playerGameData.setLastActiveTime(TimeHelper.nowInt());
        return startGame(playerController, playerGameData, betValue);
    }

    /**
     * 开始游戏
     */
    public SuperStarGameRunInfo startGame(PlayerController playerController, SuperStarPlayerGameData playerGameData, long betValue) {
        SuperStarGameRunInfo gameRunInfo = new SuperStarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            CommonResult<Pair<SuperStarResultLib, BetDivideInfo>> commonResult = normalGetLib(playerGameData, betValue, SuperStarConstant.Common.SPECIAL_MODE_TYPE_NORMAL);
            if (!commonResult.success()) {
                gameRunInfo.setCode(commonResult.code);
                return gameRunInfo;
            }

            SuperStarResultLib resultLib = commonResult.data.getFirst();
            gameRunInfo.setBetDivideInfo(commonResult.data.getSecond());

            gameRunInfo.setStake(betValue);
            //记录spin数据
            SuperStarSpinInfo spinInfo = spinAnalysis(resultLib, playerGameData.getOneBetScore());
            //记录奖池id
            spinInfo.setJackpotId(resultLib.getJackpotId());
            gameRunInfo.setSpinInfo(spinInfo);
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());

            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            //奖池中奖
            rewardFromSmallPool(gameRunInfo, playerGameData, gameRunInfo.getSpinInfo().getJackpotId(), false);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue, warehouseCfg.getTransactionItemId());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));
            gameRunInfo.setResultLib(resultLib);

            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 旋转数据解析
     */
    public SuperStarSpinInfo spinAnalysis(SuperStarResultLib resultLib, long oneBetScore) {
        SuperStarSpinInfo spinInfo = new SuperStarSpinInfo();
        //记录中奖信息
        List<SuperStarAwardLineInfo> awardLineInfoList = resultLib.getAwardLineInfoList();
        if (awardLineInfoList != null && !awardLineInfoList.isEmpty()) {
            List<SuperStarResultLineInfo> resultLineInfos = awardLineInfoList.stream().map(lineInfo -> {
                SuperStarResultLineInfo resultLineInfo = new SuperStarResultLineInfo();
                resultLineInfo.id = lineInfo.getLineId();
                resultLineInfo.iconIndex = getIconIndexsByLineId(lineInfo.getLineId()).subList(0, lineInfo.getSameCount());
                resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
                resultLineInfo.times = lineInfo.getBaseTimes();
                return resultLineInfo;
            }).toList();
            spinInfo.setResultLineInfoList(resultLineInfos);
            spinInfo.setJackpotId(resultLib.getJackpotId());
        }
        List<Integer> iconList = Arrays.stream(resultLib.getIconArr())
                .filter(v -> v != 0)
                .boxed()
                .toList();
        //记录图标信息
        spinInfo.setIconList(iconList);
        return spinInfo;
    }
}
