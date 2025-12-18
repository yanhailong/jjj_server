package com.jjg.game.slots.game.mahjiongwin.manager;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinGameRunInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MahjiongWinRoomGameManager extends AbstractMahjiongWinGameManager{
    public MahjiongWinRoomGameManager() {
        super();
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public MahjiongWinGameRunInfo startGame(PlayerController playerController, MahjiongWinPlayerGameData playerGameData, long betValue, boolean auto) {
        MahjiongWinGameRunInfo gameRunInfo = new MahjiongWinGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);

            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setBeforeGold(player.getDiamond());

            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == MahjiongWinConstant.Status.NORMAL) {
                gameRunInfo = normal(gameRunInfo, playerGameData, betValue);
            } else if (status == MahjiongWinConstant.Status.FREE) {
                gameRunInfo = free(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }

            if(!gameRunInfo.success()){
                return gameRunInfo;
            }

            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                if (addGold > 0) {
                    CommonResult<Player> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), player.getRoomId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }

                //如果是免费模式，要累计记录中奖金额
                if(status == MahjiongWinConstant.Status.FREE) {
                    playerGameData.setFreeAllWin(playerGameData.getFreeAllWin() + addGold);
                }else {
                    playerGameData.setFreeAllWin(0);
                }
            }

            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());

            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(),gameRunInfo.getAllWinGold(),betValue);

            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);

            gameRunInfo.setAfterGold(player.getDiamond());

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData, betValue);
            log.debug("计算出获奖倍数 times = {}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            //系统自动玩的游戏，不会走跑马灯
            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }
}
