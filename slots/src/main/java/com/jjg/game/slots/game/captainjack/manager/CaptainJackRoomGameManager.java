package com.jjg.game.slots.game.captainjack.manager;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackGameDataDao;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackResultLibDao;
import com.jjg.game.slots.game.captainjack.data.CaptainJackGameRunInfo;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CaptainJackRoomGameManager extends AbstractCaptainJackGameManager{
    public CaptainJackRoomGameManager(CaptainJackGameGenerateManager gameGenerateManager,
                                  CaptainJackGameDataDao gameDataDao, CaptainJackResultLibDao captainJackResultLibDao) {
        super(gameGenerateManager, gameDataDao, captainJackResultLibDao);
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public CaptainJackGameRunInfo startGame(PlayerController playerController, CaptainJackPlayerGameData playerGameData, long betValue, boolean auto) {
        CaptainJackGameRunInfo gameRunInfo = new CaptainJackGameRunInfo(Code.SUCCESS, playerGameData.playerId());
        try {
            gameRunInfo.setAuto(auto);
            //玩家当前金币
            Player player = slotsPlayerService.get(playerGameData.playerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(player.getDiamond());
            //获取当前处于哪种状态
            int status = playerGameData.getStatus();
            if (status == CaptainJackConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == CaptainJackConstant.Status.FREE) {
                free(gameRunInfo, playerGameData);
            } else if (status == CaptainJackConstant.Status.TREASURE_CHEST) {
                treasureChest(gameRunInfo, playerGameData);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},gameType = {}", playerController.playerId(), playerController.getPlayer().getGameType());
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //标准池
            if (gameRunInfo.getBigPoolTimes() > 0) {
                long addGold = playerGameData.getOneBetScore() * gameRunInfo.getBigPoolTimes();
                //如果是免费模式，要累计记录中奖金额
                if (status == CaptainJackConstant.Status.FREE) {
                    if (gameRunInfo.getRemainFreeCount() == 0) {
                        playerGameData.setFreeAllWin(addGold);
                    } else {
                        addGold = 0;
                    }
                }
                if (addGold > 0) {
                    CommonResult<Player> result = roomSlotsPoolDao.rewardFromBigPool(playerGameData.playerId(), player.getRoomId(), addGold, AddType.SLOTS_BET_REWARD);
                    if (!result.success()) {
                        log.warn("给玩家添加金币失败 gameType = {},addValue = {}", this.gameType, addGold);
                        gameRunInfo.setCode(result.code);
                        return gameRunInfo;
                    }
                    gameRunInfo.setAllWinGold(addGold);
                }
            }
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo.getAllWinGold(), betValue);

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
