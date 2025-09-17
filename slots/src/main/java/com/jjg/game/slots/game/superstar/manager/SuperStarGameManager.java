package com.jjg.game.slots.game.superstar.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.dao.SuperStarGameDataDao;
import com.jjg.game.slots.game.superstar.dao.SuperStarResultLibDao;
import com.jjg.game.slots.game.superstar.data.SuperStarAwardLineInfo;
import com.jjg.game.slots.game.superstar.data.SuperStarGameRunInfo;
import com.jjg.game.slots.game.superstar.data.SuperStarPlayerGameData;
import com.jjg.game.slots.game.superstar.data.SuperStarResultLib;
import com.jjg.game.slots.game.superstar.pb.SuperStarResultLineInfo;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 超级明星游戏管理器
 */
@Component
public class SuperStarGameManager extends AbstractSlotsGameManager<SuperStarPlayerGameData, SuperStarResultLib> {

    @Autowired
    private SuperStarResultLibDao superStarResultLibDao;
    @Autowired
    private SuperStarGameDataDao superStarGameDataDao;
    @Autowired
    private SuperStarGenerateManager superStarGenerateManager;

    public SuperStarGameManager() {
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

    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.SUPER_STAR;
    }

    /**
     * 获取奖池
     */
    public SuperStarGameRunInfo getPoolValue(PlayerController playerController, long stake) {
        SuperStarGameRunInfo gameRunInfo = new SuperStarGameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            gameRunInfo.setMini(getPoolValueByPoolId(SuperStarConstant.Common.MINI_POOL_ID, stake));
            gameRunInfo.setMinor(getPoolValueByPoolId(SuperStarConstant.Common.MINOR_POOL_ID, stake));
            gameRunInfo.setMajor(getPoolValueByPoolId(SuperStarConstant.Common.MAJOR_POOL_ID, stake));
            gameRunInfo.setGrand(getPoolValueByPoolId(SuperStarConstant.Common.GRAND_POOL_ID, stake));
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
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
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            gameRunInfo.setAfterGold(player.getGold());
            if (playerController != null) {
                playerController.setPlayer(player);
            }
            CommonResult<SuperStarResultLib> commonResult = normalGetLib(playerGameData, betValue, SuperStarConstant.Common.SPECIAL_MODE_TYPE_NORMAL);
            SuperStarResultLib resultLib = commonResult.data;
            if (resultLib == null) {
                gameRunInfo.setCode(Code.EXCEPTION);
                return gameRunInfo;
            }
            gameRunInfo.setStake(betValue);
            //记录spin数据
            SuperStarSpinInfo spinInfo = spinAnalysis(resultLib, playerGameData.getOneBetScore());
            //记录奖池id
            spinInfo.setJackpotId(resultLib.getJackpotId());
            gameRunInfo.setSpinInfo(spinInfo);
            gameRunInfo.addBigPoolTimes(resultLib.getTimes());
            //标准池
            long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
            if (gameRunInfo.getBigPoolTimes() > 0) {
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                }
            }
            int jackpotId = gameRunInfo.getSpinInfo().getJackpotId();
            //检测奖池奖励
            if (jackpotId > 0) {
                long pool = getPoolValueByPoolId(jackpotId, betValue);
                if (pool > 0) {
                    addGold += pool;
                    slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), pool, "SLOTS_JACKPOT_REWARD");
                    //记录发奖金额
                    gameRunInfo.getSpinInfo().jackpotValue = pool;
                }
            }
            if (addGold > 0) {
                gameRunInfo.addAllWinGold(addGold);
            }
            //添加大奖展示id
            int times = (int) (gameRunInfo.getAllWinGold() / betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
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
