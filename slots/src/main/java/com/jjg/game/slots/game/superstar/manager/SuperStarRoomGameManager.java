package com.jjg.game.slots.game.superstar.manager;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.data.SuperStarGameRunInfo;
import com.jjg.game.slots.game.superstar.data.SuperStarPlayerGameData;
import com.jjg.game.slots.game.superstar.data.SuperStarResultLib;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 超级明星游戏管理器
 */
@Component
public class SuperStarRoomGameManager extends AbstractSuperStarGameManager {
    public SuperStarRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public SuperStarGameRunInfo startGame(PlayerController playerController, SuperStarPlayerGameData playerGameData, long betValue) {
        SuperStarGameRunInfo gameRunInfo = new SuperStarGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            CommonResult<Pair<SuperStarResultLib,Long>> commonResult = normalGetLib(playerGameData, betValue, SuperStarConstant.Common.SPECIAL_MODE_TYPE_NORMAL);
            if (!commonResult.success()) {
                gameRunInfo.setCode(commonResult.code);
                return gameRunInfo;
            }

            SuperStarResultLib resultLib = commonResult.data.getFirst();
            gameRunInfo.setTax(commonResult.data.getSecond());

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
                    CommonResult<Player> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), playerGameData.getSlotsRoomController().getRoom().getId(), addGold, AddType.SLOTS_BET_REWARD);
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
                    roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(),  playerGameData.getSlotsRoomController().getRoom().getId(), pool, AddType.SLOTS_JACKPOT_REWARD);
                    //记录发奖金额
                    gameRunInfo.getSpinInfo().jackpotValue = pool;
                }
            }
            if (addGold > 0) {
                gameRunInfo.addAllWinGold(addGold);
            }

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(),gameRunInfo.getAllWinGold(),betValue);

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));
            checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getDiamond());
            gameRunInfo.setResultLib(resultLib);

            return gameRunInfo;
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
