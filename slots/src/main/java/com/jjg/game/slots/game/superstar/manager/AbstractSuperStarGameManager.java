package com.jjg.game.slots.game.superstar.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.superstar.dao.SuperStarGameDataDao;
import com.jjg.game.slots.game.superstar.dao.SuperStarResultLibDao;
import com.jjg.game.slots.game.superstar.data.*;
import com.jjg.game.slots.game.superstar.pb.SuperStarResultLineInfo;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractSuperStarGameManager extends AbstractSlotsGameManager<SuperStarPlayerGameData, SuperStarResultLib, SuperStarGameRunInfo> {

    @Autowired
    protected SuperStarResultLibDao superStarResultLibDao;
    @Autowired
    protected SuperStarGameDataDao superStarGameDataDao;
    @Autowired
    protected SuperStarGenerateManager superStarGenerateManager;
    @Autowired
    protected SuperStarGameDataDao gameDataDao;

    public AbstractSuperStarGameManager() {
        super(SuperStarPlayerGameData.class, SuperStarResultLib.class, SuperStarGameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("SuperStarGameManager");
        super.init();
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

    @Override
    protected Class<? extends SlotsPlayerGameDataDTO> getSlotsPlayerGameDataDTOCla() {
        return SuperStarPlayerGameDataDTO.class;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.SUPER_STAR;
    }

    /**
     * 开始游戏
     */
    @Override
    public SuperStarGameRunInfo startGame(PlayerController playerController, SuperStarPlayerGameData playerGameData, long betValue, boolean auto) {
        SuperStarGameRunInfo gameRunInfo = new SuperStarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());

            gameRunInfo = normal(gameRunInfo, playerGameData, betValue);

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            //奖池中奖
            rewardFromSmallPool(gameRunInfo, playerGameData, gameRunInfo.getResultLib().getJackpotIds());
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), playerGameData.getAllBetScore(), warehouseCfg.getTransactionItemId());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    protected SuperStarGameRunInfo normal(SuperStarGameRunInfo gameRunInfo, SuperStarPlayerGameData playerGameData, long betValue, SuperStarResultLib resultLib) {
        gameRunInfo.setStake(betValue);
        //记录spin数据
        SuperStarSpinInfo spinInfo = spinAnalysis(resultLib, playerGameData.getOneBetScore());
        //记录奖池id
        spinInfo.setJackpotId(resultLib.firstJackpotId());
        gameRunInfo.setSpinInfo(spinInfo);
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        gameRunInfo.setResultLib(resultLib);
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
            spinInfo.setJackpotId(resultLib.firstJackpotId());
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
