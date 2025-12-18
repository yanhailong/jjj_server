package com.jjg.game.slots.game.wealthbank.manager;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;
import com.jjg.game.slots.game.wealthbank.data.WealthBankGameRunInfo;
import com.jjg.game.slots.game.wealthbank.data.WealthBankPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WealthBankRoomGameManager extends AbstractWealthBankGameManager {
    public WealthBankRoomGameManager() {
        super();
        this.log =  LoggerFactory.getLogger(getClass());
    }

    @Override
    public WealthBankGameRunInfo startGame(PlayerController playerController, WealthBankPlayerGameData playerGameData, long betValue, boolean auto) {
        WealthBankGameRunInfo gameRunInfo = new WealthBankGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getDiamond());

            boolean allAreaUnlock = playerGameData.getAllUnLock().compareAndSet(true, false);
            if (allAreaUnlock) {
                gameRunInfo = areaAllUnlockGoldTrain(gameRunInfo, playerGameData);
            } else {
                //获取当前处于哪种状态
                int status = playerGameData.getStatus();
                if (status == WealthBankConstant.Status.NORMAL) {  //正常
                    gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
                } else if (status == WealthBankConstant.Status.NOTMAL_ALL_BOARD || status == WealthBankConstant.Status.GOLD_ALL_BOARD) {  //二选一
                    gameRunInfo.setCode(Code.FORBID);
                    log.debug("[Wealth Bank] 当前正处于二选一状态，禁止开始游戏操作 playerId = {},gameType = {},roomCfgId = {}, status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                } else if (status == WealthBankConstant.Status.ALL_BOARD_TRAIN) {  //二选一之拉火车
                    gameRunInfo = allBoardTrain(gameRunInfo, playerGameData);
                } else if (status == WealthBankConstant.Status.ALL_BOARD_GOLD_TRAIN) {  //二选一之拉黄金火车
                    gameRunInfo = allBoardGoldTrain(gameRunInfo, playerGameData);
                } else if (status == WealthBankConstant.Status.ALL_BOARD_FREE) {  //二选一之免费模式
                    gameRunInfo = allBoardFree(gameRunInfo, playerGameData);
                } else {
                    gameRunInfo.setCode(Code.FAIL);
                    log.debug("[Wealth Bank] 开始游戏失败，检测到错误状态 playerId = {},gameType = {},roomCfgId = {},status = {}", playerGameData.playerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                    return gameRunInfo;
                }
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            //检查是否触发投资游戏
            gameRunInfo = checkInvers(playerGameData, gameRunInfo);

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), player.getRoomId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("[Wealth Bank] 给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue);

            //添加美元收集进度
            if (gameRunInfo.getTotalDollars() < 1) {
                gameRunInfo.setTotalDollars(playerGameData.getTotalDollars());
            }

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getDiamond());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("[Wealth Bank] 计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
            return gameRunInfo;
        } catch (Exception e) {
            log.error("[Wealth Bank] ", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SLOTS_TEAM_UP_ROOM;
    }
}
