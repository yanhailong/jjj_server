package com.jjg.game.slots.game.superstar.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.slots.constant.SlotsConst;
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
            //旋转一次
            spin(gameRunInfo, playerGameData, betValue);
            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = slotsPoolDao.rewardFromBigPool(playerGameData.playerId(), this.gameType, playerGameData.getRoomCfgId(), addGold, "SLOTS_BET_REWARD");
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
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
                gameRunInfo.setAllWinGold(addGold);
            }
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
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
     * 普通正常流程
     */
    private void spin(SuperStarGameRunInfo gameRunInfo, SuperStarPlayerGameData playerGameData, long betValue) {
        log.debug("开始正常流程 playerId = {},betValue = {},", playerGameData.playerId(), betValue);
        //获取倍场配置
        BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
        if (baseRoomCfg == null) {
            log.warn("获取倍场配置失败 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            gameRunInfo.setCode(Code.NOT_FOUND);
            return;
        }
        //检查押分是否合法
        long[] betScoreArr = this.allStakeMap.get(playerGameData.getRoomCfgId()).stream().filter(arr -> arr[1] == betValue).findFirst().orElse(null);
        if (betScoreArr == null) {
            log.warn("押分值不合法 playerId = {},gameType = {},roomCfgId = {},betValue = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue);
            gameRunInfo.setCode(Code.PARAM_ERROR);
            return;
        }
        Player player = slotsPlayerService.get(playerGameData.playerId());
        if (player.getGold() < betValue) {
            log.debug("玩家余额不足，无法快乐的玩游戏 playerId = {},gameType = {},roomCfgId = {},betValue = {},currentGold = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), betValue, player.getGold());
            gameRunInfo.setCode(Code.NOT_ENOUGH);
            return;
        }
        CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
        if (!libCfgResult.success()) {
            gameRunInfo.setCode(libCfgResult.code);
            return;
        }
        int libType;
        //先去获取测试数据
//        TestLibData testLibData = playerGameData.pollTestLibData();
//        if (testLibData != null) {
//            libType = testLibData.getLibType();
//            log.debug("获取到测试数据 playerId = {},libType = {}", playerGameData.playerId(), libType);
//        }

        //获取 specialResultLib 中的type
        CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData.getGameType(), libCfgResult.data.getModelId());
        if (!resultLibTypeResult.success()) {
            gameRunInfo.setCode(libCfgResult.code);
            return;
        }
        libType = resultLibTypeResult.data;
        log.debug("获取到结果库类型 playerId = {},libType = {}", playerGameData.playerId(), libType);

        int sectionIndex = -1;
        SuperStarResultLib resultLib = null;

        for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
            //获取倍数区间
            CommonResult<Integer> resultLibSectionResult = getResultLibSection(libCfgResult.data.getModelId(), libType);
            if (!resultLibSectionResult.success()) {
                continue;
            }

            //根据倍数区间从结果库里面随机获取一条
            resultLib = superStarResultLibDao.getLibBySectionIndex(libType, resultLibSectionResult.data);
            if (resultLib == null) {
                log.debug("获取结果库失败 gameType = {},modelId = {},libType = {},sectionIndex = {},retry = {}", this.gameType, libCfgResult.data.getModelId(), libType, resultLibSectionResult.data, i);
                continue;
            }
            sectionIndex = resultLibSectionResult.data;
            log.debug("成功获取结果库  playerId = {},libId = {}", playerGameData.playerId(), resultLib.getId());
            break;
        }
        //如果前面没有获取到lib，则获取一个无奖励的结果
        if (resultLib == null) {
            sectionIndex = this.defaultRewardSectionIndex;
            resultLib = superStarResultLibDao.getLibBySectionIndex(SuperStarConstant.Common.SPECIAL_MODE_TYPE_NORMAL, this.defaultRewardSectionIndex);
            log.debug("前面获取结果库失败，所以找一个不中奖的结果返回 gameType = {},libType = {}", this.gameType, libType);
        }
        if (resultLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            log.debug("获取结果库失败 gameType = {},libType = {}", this.gameType, libType);
            return;
        }
        //给池子加钱
        CommonResult<Player> result = goldToPool(playerGameData, betValue, baseRoomCfg);
        if (!result.success()) {
            gameRunInfo.setCode(result.code);
            return;
        }
        //记录押分值
        playerGameData.setOneBetScore(betScoreArr[0]);
        playerGameData.setAllBetScore(betScoreArr[1]);
        gameRunInfo.setStake(betValue);
        //记录spin数据
        SuperStarSpinInfo spinInfo = spinAnalysis(resultLib, playerGameData.getOneBetScore());
        //记录奖池id
        spinInfo.setJackpotId(resultLib.getJackpotId());
        gameRunInfo.setSpinInfo(spinInfo);
        if (sectionIndex > 0) {
            playerGameData.setLastSectionIndex(sectionIndex);
        }
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        playerGameData.setLastModelId(libCfgResult.data.getModelId());
        log.debug("id = {},data = {}", resultLib.getId(), JSON.toJSONString(resultLib));
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
